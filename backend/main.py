import asyncio
import logging
import os
import re
from contextlib import asynccontextmanager, suppress
from datetime import date, datetime, timedelta
from zoneinfo import ZoneInfo

import httpx
from dotenv import load_dotenv
from fastapi import FastAPI, HTTPException


load_dotenv()

LOGGER = logging.getLogger("uvicorn.error")

ISTANBUL_TIMEZONE = ZoneInfo("Europe/Istanbul")

NOBETECZA_API_KEY = os.getenv(
    "NOBETECZA_API_KEY",
    "",
).strip()

NOBETECZA_BASE_URL = os.getenv(
    "NOBETECZA_BASE_URL",
    "https://api.nobetecza.com",
).strip().rstrip("/")

REFRESH_TIMES = (
    (0, 5),
    (8, 0),
    (18, 5),
)

TURKISH_MONTHS = {
    1: "Ocak",
    2: "Şubat",
    3: "Mart",
    4: "Nisan",
    5: "Mayıs",
    6: "Haziran",
    7: "Temmuz",
    8: "Ağustos",
    9: "Eylül",
    10: "Ekim",
    11: "Kasım",
    12: "Aralık",
}

TURKISH_DAYS = {
    0: "Pazartesi",
    1: "Salı",
    2: "Çarşamba",
    3: "Perşembe",
    4: "Cuma",
    5: "Cumartesi",
    6: "Pazar",
}

TURKISH_TO_ASCII = str.maketrans(
    {
        "ç": "c",
        "Ç": "c",
        "ğ": "g",
        "Ğ": "g",
        "ı": "i",
        "İ": "i",
        "ö": "o",
        "Ö": "o",
        "ş": "s",
        "Ş": "s",
        "ü": "u",
        "Ü": "u",
    }
)

cache: dict[str, dict] = {}
cache_lock = asyncio.Lock()

city_locks: dict[str, asyncio.Lock] = {}

http_client: httpx.AsyncClient | None = None


class ProviderConfigurationError(Exception):
    pass


class ProviderAuthenticationError(Exception):
    pass


class ProviderRateLimitError(Exception):
    pass


class ProviderCityNotFoundError(Exception):
    pass


class ProviderUnavailableError(Exception):
    pass


class ProviderResponseError(Exception):
    pass


def normalize_city_slug(value: str) -> str:
    translated = value.strip().translate(TURKISH_TO_ASCII).lower()

    return re.sub(pattern=r"[^a-z0-9]+",repl="-",string=translated,).strip("-")


def format_turkish_date(value: date) -> str:
    return (
        f"{value.day} "
        f"{TURKISH_MONTHS[value.month]} "
        f"{value.year} "
        f"{TURKISH_DAYS[value.weekday()]}"
    )


def build_duty_date_label(duty_date_text: str,) -> str:
    try:
        start_date = date.fromisoformat(duty_date_text)
    except ValueError:
        return ""

    end_date = start_date + timedelta(days=1)

    return (
        f"{format_turkish_date(start_date)} "
        f"akşamından "
        f"{format_turkish_date(end_date)} "
        f"sabahına kadar"
    )


def get_refresh_slot(now: datetime) -> str:
    current_minutes = (now.hour * 60+ now.minute)

    first_refresh_minutes = 5
    second_refresh_minutes = 8 * 60
    third_refresh_minutes = 18 * 60 + 5

    if current_minutes < first_refresh_minutes:
        previous_day = now - timedelta(days=1)

        return (f"{previous_day.date().isoformat()}-2")

    if current_minutes < second_refresh_minutes:
        return f"{now.date().isoformat()}-0"

    if current_minutes < third_refresh_minutes:
        return f"{now.date().isoformat()}-1"

    return f"{now.date().isoformat()}-2"


def get_next_refresh_time(now: datetime,) -> datetime:
    for hour, minute in REFRESH_TIMES:
        candidate = now.replace(
            hour=hour,
            minute=minute,
            second=0,
            microsecond=0,
        )

        if candidate > now:
            return candidate

    tomorrow = now + timedelta(days=1)
    first_hour, first_minute = REFRESH_TIMES[0]

    return tomorrow.replace(
        hour=first_hour,
        minute=first_minute,
        second=0,
        microsecond=0,
    )


def get_city_lock(city_slug: str,) -> asyncio.Lock:
    city_lock = city_locks.get(city_slug)

    if city_lock is None:
        city_lock = asyncio.Lock()
        city_locks[city_slug] = city_lock

    return city_lock


def to_optional_float( value,) -> float | None:
    if value is None:
        return None

    try:
        return float(value)
    except (TypeError, ValueError):
        return None


async def read_cache(city_slug: str,) -> dict | None:
    async with cache_lock:
        cached_entry = cache.get(city_slug)

        if cached_entry is None:
            return None

        return cached_entry.copy()


async def write_cache(city_slug: str,cache_entry: dict,) -> None:
    async with cache_lock:
        cache[city_slug] = cache_entry


def build_cache_entry(provider_result: dict,checked_at: datetime,) -> dict:
    return {
        "city": provider_result["city"],
        "checked_at": checked_at,
        "refresh_slot": get_refresh_slot(
            checked_at
        ),
        "duty_date": provider_result[
            "duty_date"
        ],
        "duty_date_label": provider_result[
            "duty_date_label"
        ],
        "is_previous_day": provider_result[
            "is_previous_day"
        ],
        "pharmacies": provider_result[
            "pharmacies"
        ],
    }


def build_client_response(cache_entry: dict,source: str,) -> dict:
    return {
        "city": cache_entry["city"],
        "source": source,
        "checked_at": cache_entry[
            "checked_at"
        ].strftime("%H:%M"),
        "duty_date": cache_entry[
            "duty_date"
        ],
        "duty_date_label": cache_entry[
            "duty_date_label"
        ],
        "is_previous_day": cache_entry[
            "is_previous_day"
        ],
        "pharmacies": cache_entry[
            "pharmacies"
        ],
    }


async def fetch_provider_city(city_slug: str,) -> dict:
    if http_client is None:
        raise ProviderConfigurationError(
            "HTTP client is not initialized."
        )

    response = await http_client.get(
        "/v1/nobetci",
        params={
            "il": city_slug,
        },
    )

    LOGGER.info(
        (
            "NobetEcza response city=%s "
            "status=%s length=%s"
        ),
        city_slug,
        response.status_code,
        len(response.content),
    )

    if response.status_code in (401, 403):
        raise ProviderAuthenticationError(
            "NobetEcza authentication failed."
        )

    if response.status_code == 404:
        raise ProviderCityNotFoundError(
            f"City not found. city={city_slug}"
        )

    if response.status_code == 429:
        raise ProviderRateLimitError(
            "NobetEcza request limit exceeded."
        )

    if response.status_code >= 500:
        raise ProviderUnavailableError(
            (
                "NobetEcza service unavailable. "
                f"status={response.status_code}"
            )
        )

    if response.status_code != 200:
        LOGGER.warning(
            (
                "Unexpected NobetEcza response. "
                "city=%s status=%s body=%r"
            ),
            city_slug,
            response.status_code,
            response.text[:500],
        )

        raise ProviderResponseError(
            (
                "Unexpected provider response. "
                f"status={response.status_code}"
            )
        )

    try:
        payload = response.json()
    except ValueError as exception:
        raise ProviderResponseError("Provider returned invalid JSON.") from exception

    if payload.get("success") is not True:
        LOGGER.warning(
            (
                "NobetEcza returned unsuccessful result. "
                "city=%s payload=%r"
            ),
            city_slug,
            payload,
        )

        raise ProviderResponseError("Provider returned unsuccessful result.")

    provider_items = payload.get("data")

    if not isinstance(provider_items, list):
        raise ProviderResponseError("Provider data field is not a list.")

    provider_city = payload.get("il")
    city_name = ""

    if isinstance(provider_city, dict):
        city_name = str(
            provider_city.get("ad") or "").strip()

    if not city_name and provider_items:
        first_item = provider_items[0]

        if isinstance(first_item, dict):
            city_name = str(first_item.get("il") or "").strip()

    if not city_name:
        city_name = city_slug.replace("-", " ").title()

    duty_date = str(payload.get("tarih") or "").strip()

    if not duty_date:
        duty_date = datetime.now(ISTANBUL_TIMEZONE).date().isoformat()

    pharmacies: list[dict] = []

    for item in provider_items:
        if not isinstance(item, dict):
            continue

        location = item.get("konum")

        if not isinstance(location, dict):
            location = {}

        pharmacy_name = str(
            item.get("ad") or ""
        ).strip()

        if not pharmacy_name:
            continue

        pharmacies.append(
            {
                "district": str(
                    item.get("ilce") or ""
                ).strip(),
                "name": pharmacy_name,
                "address": str(
                    item.get("adres") or ""
                ).strip(),
                "phone": str(
                    item.get("telefon") or ""
                ).strip(),

                "provider_id": item.get("id"),
                "city_slug": str(
                    item.get("il_slug") or ""
                ).strip(),
                "district_slug": str(
                    item.get("ilce_slug") or ""
                ).strip(),
                "directions": str(
                    item.get("tarif") or ""
                ).strip(),
                "latitude": to_optional_float(
                    location.get("lat")
                ),
                "longitude": to_optional_float(
                    location.get("lng")
                ),
            }
        )

    return {
        "city": city_name,
        "duty_date": duty_date,
        "duty_date_label": (
            build_duty_date_label(duty_date)
        ),
        "is_previous_day": bool(
            payload.get("onceki_gun", False)
        ),
        "pharmacies": pharmacies,
    }


async def refresh_city(city_slug: str,) -> dict:
    city_lock = get_city_lock(city_slug)

    async with city_lock:
        now = datetime.now(
            ISTANBUL_TIMEZONE
        )

        provider_result = await fetch_provider_city(
            city_slug
        )

        cache_entry = build_cache_entry(
            provider_result=provider_result,
            checked_at=now,
        )

        await write_cache(
            city_slug=city_slug,
            cache_entry=cache_entry,
        )

        return cache_entry


async def refresh_cached_cities() -> None:
    async with cache_lock:
        cached_cities = list(cache.keys())

    if not cached_cities:
        LOGGER.info(
            "Scheduled refresh skipped because cache is empty."
        )
        return

    LOGGER.info(
        "Scheduled refresh started. city_count=%s",
        len(cached_cities),
    )

    for city_slug in cached_cities:
        try:
            cache_entry = await refresh_city(city_slug)

            LOGGER.info(
                (
                    "Scheduled refresh completed. "
                    "city=%s pharmacy_count=%s"
                ),
                city_slug,
                len(cache_entry["pharmacies"]),
            )

        except ProviderRateLimitError:
            LOGGER.error(
                (
                    "Scheduled refresh stopped because "
                    "provider request limit was exceeded."
                )
            )
            break

        except Exception:
            LOGGER.exception(
                (
                    "Scheduled refresh failed. "
                    "city=%s"
                ),
                city_slug,
            )

        await asyncio.sleep(0.25)


async def scheduled_cache_refresh_loop() -> None:
    while True:
        now = datetime.now(
            ISTANBUL_TIMEZONE
        )

        next_refresh = get_next_refresh_time(
            now
        )

        wait_seconds = max(
            0,
            (next_refresh - now).total_seconds(),
        )

        LOGGER.info(
            "Next scheduled refresh: %s",
            next_refresh.isoformat(),
        )

        await asyncio.sleep(
            wait_seconds
        )

        await refresh_cached_cities()


@asynccontextmanager
async def lifespan(_app: FastAPI):
    global http_client

    if not NOBETECZA_API_KEY:
        raise RuntimeError(
            (
                "NOBETECZA_API_KEY environment "
                "variable is not configured."
            )
        )

    http_client = httpx.AsyncClient(
        base_url=NOBETECZA_BASE_URL,
        headers={
            "X-API-Key": NOBETECZA_API_KEY,
            "Accept": "application/json",
        },
        timeout=httpx.Timeout(30.0),
        follow_redirects=True,
    )

    LOGGER.info(
        "NobetEcza provider client started."
    )

    refresh_task = asyncio.create_task(
        scheduled_cache_refresh_loop()
    )

    try:
        yield

    finally:
        LOGGER.info(
            "Pharmacy cache scheduler stopping."
        )

        refresh_task.cancel()

        with suppress(asyncio.CancelledError):
            await refresh_task

        await http_client.aclose()
        http_client = None


app = FastAPI(
    title="PharmacyTrack API",
    version="2.0.0",
    lifespan=lifespan,
)


@app.get("/")
async def health_check():
    return {
        "status": "ok",
        "service": "pharmacy-track-api",
    }


@app.get("/pharmacies/{city}")
async def get_pharmacies(city: str):
    city_slug = normalize_city_slug(city)

    if not city_slug:
        raise HTTPException(
            status_code=400,
            detail="City must not be empty.",
        )

    now = datetime.now(
        ISTANBUL_TIMEZONE
    )

    current_refresh_slot = get_refresh_slot(
        now
    )

    cached_entry = await read_cache(
        city_slug
    )

    if (
        cached_entry is not None
        and cached_entry.get(
            "refresh_slot"
        ) == current_refresh_slot
    ):
        return build_client_response(
            cache_entry=cached_entry,
            source="cache",
        )

    city_lock = get_city_lock(
        city_slug
    )

    async with city_lock:
        # Aynı şehre eş zamanlı istek geldiyse
        # ilk isteğin yazdığı cache'i yeniden kontrol et.
        cached_entry = await read_cache(
            city_slug
        )

        if (
            cached_entry is not None
            and cached_entry.get(
                "refresh_slot"
            ) == current_refresh_slot
        ):
            return build_client_response(
                cache_entry=cached_entry,
                source="cache",
            )

        try:
            provider_result = (
                await fetch_provider_city(
                    city_slug
                )
            )

        except ProviderCityNotFoundError as exception:
            raise HTTPException(
                status_code=404,
                detail=(
                    f"City '{city_slug}' "
                    "was not found."
                ),
            ) from exception

        except ProviderAuthenticationError as exception:
            LOGGER.exception(
                "NobetEcza authentication failed."
            )

            raise HTTPException(
                status_code=503,
                detail=(
                    "Pharmacy data service "
                    "is not configured correctly."
                ),
            ) from exception

        except ProviderRateLimitError as exception:
            LOGGER.warning(
                "NobetEcza request limit exceeded."
            )

            raise HTTPException(
                status_code=503,
                detail=(
                    "Pharmacy data request "
                    "limit was exceeded."
                ),
            ) from exception

        except httpx.TimeoutException as exception:
            raise HTTPException(
                status_code=504,
                detail=(
                    "Pharmacy data provider "
                    "timed out."
                ),
            ) from exception

        except httpx.RequestError as exception:
            LOGGER.exception(
                "Could not connect to NobetEcza."
            )

            raise HTTPException(
                status_code=502,
                detail=(
                    "Pharmacy data provider "
                    "could not be reached."
                ),
            ) from exception

        except (
            ProviderUnavailableError,
            ProviderResponseError,
            ProviderConfigurationError,
        ) as exception:
            LOGGER.exception(
                "NobetEcza provider error."
            )

            raise HTTPException(
                status_code=502,
                detail=(
                    "Pharmacy data provider "
                    "returned an invalid response."
                ),
            ) from exception

        cache_entry = build_cache_entry(
            provider_result=provider_result,
            checked_at=now,
        )

        await write_cache(
            city_slug=city_slug,
            cache_entry=cache_entry,
        )

        return build_client_response(
            cache_entry=cache_entry,
            source="live",
        )
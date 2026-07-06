import asyncio
import logging
from contextlib import asynccontextmanager, suppress
from datetime import datetime, timedelta
from zoneinfo import ZoneInfo

import httpx
from bs4 import BeautifulSoup, NavigableString
from fastapi import FastAPI, HTTPException


LOGGER = logging.getLogger("uvicorn.error")

ISTANBUL_TIMEZONE = ZoneInfo("Europe/Istanbul")

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

cache: dict[str, dict] = {}


class SourceAccessBlockedError(Exception):
    """Kaynak site isteği Cloudflare veya benzeri bir koruma tarafından engellendi."""


class SourceCityNotFoundError(Exception):
    """Kaynak sitede istenen şehre ait sayfa bulunamadı."""


def normalize_text(value: str) -> str:
    return " ".join(value.split()).strip()


def format_turkish_date(value) -> str:
    return (
        f"{value.day} "
        f"{TURKISH_MONTHS[value.month]} "
        f"{value.year} "
        f"{TURKISH_DAYS[value.weekday()]}"
    )


def build_duty_date_label(now: datetime) -> str:
    today = now.date()
    tomorrow = today + timedelta(days=1)

    return (
        f"{format_turkish_date(today)} mesai bitiminden "
        f"{format_turkish_date(tomorrow)} sabahına kadar"
    )


def get_today_marker(now: datetime) -> str:
    return f"{now.day} {TURKISH_MONTHS[now.month]}"


def find_today_container(
    soup: BeautifulSoup,
    now: datetime,
):
    active_panes = soup.select(
        ".tab-pane.show.active, .tab-pane.active"
    )

    for pane in active_panes:
        if pane.select_one("td.border-bottom"):
            return pane

    today_marker = get_today_marker(now)

    for text_node in soup.find_all(string=True):
        text = normalize_text(str(text_node))

        if not text:
            continue

        is_today_label = (
            today_marker in text
            and "kadar" in text.lower()
            and not text.lower().startswith("bu sayfada")
        )

        if not is_today_label:
            continue

        parent = text_node.parent

        if parent is None:
            continue

        next_table = parent.find_next("table")

        if (
            next_table is not None
            and next_table.select_one("td.border-bottom")
        ):
            return next_table

    pharmacy_tables = [
        table
        for table in soup.find_all("table")
        if table.select_one("td.border-bottom")
    ]

    if len(pharmacy_tables) == 1:
        return pharmacy_tables[0]

    return None


def parse_pharmacies(container) -> list[dict]:
    pharmacy_list: list[dict] = []
    seen_pharmacies: set[tuple[str, str, str, str]] = set()

    for box in container.select("td.border-bottom"):
        name_tag = box.find("span", class_="isim")

        if not name_tag:
            continue

        name = normalize_text(
            name_tag.get_text(" ", strip=True)
        )

        district_tag = box.find("span", class_="bg-info")
        district = (
            normalize_text(
                district_tag.get_text(" ", strip=True)
            )
            if district_tag
            else ""
        )

        phone_divs = box.find_all(
            "div",
            class_="col-lg-3",
        )

        phone = (
            normalize_text(
                phone_divs[-1].get_text(" ", strip=True)
            )
            if len(phone_divs) > 1
            else ""
        )

        address = ""
        address_div = box.find(
            "div",
            class_="col-lg-6",
        )

        if address_div:
            direct_texts = [
                normalize_text(str(content))
                for content in address_div.contents
                if isinstance(content, NavigableString)
                and normalize_text(str(content))
            ]

            if direct_texts:
                address = direct_texts[0]
            else:
                address = normalize_text(
                    address_div.get_text(" ", strip=True)
                )

        pharmacy_key = (
            district.casefold(),
            name.casefold(),
            address.casefold(),
            "".join(
                character
                for character in phone
                if character.isdigit()
            ),
        )

        if pharmacy_key in seen_pharmacies:
            continue

        seen_pharmacies.add(pharmacy_key)

        pharmacy_list.append(
            {
                "district": district,
                "name": name,
                "address": address,
                "phone": phone,
            }
        )

    return pharmacy_list


async def fetch_city_data(
    city: str,
    now: datetime,
) -> dict | None:
    target_url = (
        f"https://www.eczaneler.gen.tr/nobetci-{city}"
    )

    headers = {
        "User-Agent": (
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
            "AppleWebKit/537.36 (KHTML, like Gecko) "
            "Chrome/131.0.0.0 Safari/537.36"
        ),
        "Accept": (
            "text/html,application/xhtml+xml,"
            "application/xml;q=0.9,"
            "image/avif,image/webp,*/*;q=0.8"
        ),
        "Accept-Language": (
            "tr-TR,tr;q=0.9,en-US;q=0.8,en;q=0.7"
        ),
        "Cache-Control": "no-cache",
        "Pragma": "no-cache",
        "Referer": "https://www.eczaneler.gen.tr/",
        "Upgrade-Insecure-Requests": "1",
    }

    async with httpx.AsyncClient(
        timeout=20.0,
        follow_redirects=True,
    ) as client:
        response = await client.get(
            target_url,
            headers=headers,
        )

    LOGGER.info(
        (
            "Source response city=%s status=%s "
            "url=%s content_type=%s length=%s"
        ),
        city,
        response.status_code,
        response.url,
        response.headers.get("content-type"),
        len(response.text),
    )

    if response.status_code == 404:
        raise SourceCityNotFoundError(
            f"Source city page not found. city={city}"
        )

    cloudflare_challenge = (
        response.status_code == 403
        and (
            "Just a moment" in response.text
            or "challenges.cloudflare.com" in response.text
        )
    )

    if cloudflare_challenge:
        LOGGER.warning(
            (
                "Source access blocked by Cloudflare. "
                "city=%s status=%s"
            ),
            city,
            response.status_code,
        )

        raise SourceAccessBlockedError(
            f"Source access blocked. city={city}"
        )

    if response.status_code != 200:
        LOGGER.warning(
            (
                "Source request failed. "
                "city=%s status=%s body=%r"
            ),
            city,
            response.status_code,
            response.text[:500],
        )

        return None

    soup = BeautifulSoup(
        response.text,
        "html.parser",
    )

    today_container = find_today_container(
        soup=soup,
        now=now,
    )

    if today_container is None:
        LOGGER.warning(
            (
                "Today container not found. "
                "city=%s title=%r "
                "pharmacy_cell_count=%s "
                "active_pane_count=%s"
            ),
            city,
            (
                soup.title.get_text(" ", strip=True)
                if soup.title
                else None
            ),
            len(soup.select("td.border-bottom")),
            len(
                soup.select(
                    ".tab-pane.show.active, .tab-pane.active"
                )
            ),
        )

        return None

    pharmacies = parse_pharmacies(
        today_container
    )

    if not pharmacies:
        LOGGER.warning(
            (
                "No pharmacies parsed. "
                "city=%s container_length=%s"
            ),
            city,
            len(str(today_container)),
        )

        return None

    return {
        "duty_date": now.date().isoformat(),
        "duty_date_label": build_duty_date_label(now),
        "pharmacies": pharmacies,
    }


async def refresh_cached_cities() -> None:
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

    for city in cached_cities:
        try:
            now = datetime.now(
                ISTANBUL_TIMEZONE
            )

            result = await fetch_city_data(
                city=city,
                now=now,
            )

            if result is None:
                LOGGER.warning(
                    (
                        "Scheduled refresh returned no data. "
                        "city=%s"
                    ),
                    city,
                )
                continue

            cache[city] = {
                "checked_at": now,
                "duty_date": result["duty_date"],
                "duty_date_label": result[
                    "duty_date_label"
                ],
                "pharmacies": result["pharmacies"],
            }

            LOGGER.info(
                (
                    "Scheduled refresh completed. "
                    "city=%s pharmacy_count=%s"
                ),
                city,
                len(result["pharmacies"]),
            )

        except SourceAccessBlockedError:
            LOGGER.warning(
                (
                    "Scheduled refresh blocked by source. "
                    "city=%s"
                ),
                city,
            )

        except SourceCityNotFoundError:
            LOGGER.warning(
                (
                    "Scheduled refresh city not found. "
                    "city=%s"
                ),
                city,
            )

        except Exception:
            LOGGER.exception(
                (
                    "Unexpected scheduled refresh error. "
                    "city=%s"
                ),
                city,
            )

        await asyncio.sleep(1)


def get_next_refresh_time(
    now: datetime,
) -> datetime:
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
    LOGGER.info(
        "Pharmacy cache scheduler started."
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


app = FastAPI(
    title="PharmacyTrack API",
    version="1.0.0",
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
    normalized_city = city.strip().lower()

    if not normalized_city:
        raise HTTPException(
            status_code=400,
            detail="City must not be empty.",
        )

    now = datetime.now(
        ISTANBUL_TIMEZONE
    )

    today = now.date().isoformat()
    cached_entry = cache.get(
        normalized_city
    )

    cache_is_valid = (
        cached_entry is not None
        and cached_entry["duty_date"] == today
    )

    if cache_is_valid:
        return {
            "city": normalized_city.capitalize(),
            "source": "cache",
            "checked_at": cached_entry[
                "checked_at"
            ].strftime("%H:%M"),
            "duty_date": cached_entry[
                "duty_date"
            ],
            "duty_date_label": cached_entry[
                "duty_date_label"
            ],
            "pharmacies": cached_entry[
                "pharmacies"
            ],
        }

    try:
        result = await fetch_city_data(
            city=normalized_city,
            now=now,
        )

    except SourceCityNotFoundError as exception:
        raise HTTPException(
            status_code=404,
            detail=(
                f"City '{normalized_city.capitalize()}' "
                "was not found in the pharmacy data source."
            ),
        ) from exception

    except SourceAccessBlockedError as exception:
        LOGGER.warning(
            (
                "Pharmacy source blocked the server request. "
                "city=%s"
            ),
            normalized_city,
        )

        raise HTTPException(
            status_code=502,
            detail=(
                "Pharmacy data source temporarily "
                "rejected the server request."
            ),
        ) from exception

    if result is None:
        raise HTTPException(
            status_code=502,
            detail=(
                "Pharmacy data source could not "
                "be reached or parsed."
            ),
        )

    cache[normalized_city] = {
        "checked_at": now,
        "duty_date": result["duty_date"],
        "duty_date_label": result[
            "duty_date_label"
        ],
        "pharmacies": result["pharmacies"],
    }

    return {
        "city": normalized_city.capitalize(),
        "source": "live",
        "checked_at": now.strftime("%H:%M"),
        "duty_date": result["duty_date"],
        "duty_date_label": result[
            "duty_date_label"
        ],
        "pharmacies": result["pharmacies"],
    }
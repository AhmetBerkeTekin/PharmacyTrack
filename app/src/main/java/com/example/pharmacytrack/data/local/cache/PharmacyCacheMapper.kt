package com.example.pharmacytrack.data.local.cache

import com.example.pharmacytrack.data.model.Pharmacy
import com.example.pharmacytrack.data.model.PharmacyResponse

fun PharmacyResponse.toCacheEntities(
    citySlug: String,
    cachedAtMillis: Long
): List<CachedPharmacyEntity> {
    val cachedCityName = city.orEmpty().ifBlank {
        citySlug
    }

    val cachedDutyDate = dutyDate.orEmpty()
    val cachedDutyDateLabel = dutyDateLabel.orEmpty()

    return pharmacies
        .orEmpty()
        .filterNotNull()
        .map { pharmacy ->
            CachedPharmacyEntity(
                citySlug = citySlug,
                cityName = cachedCityName,
                dutyDate = cachedDutyDate,
                dutyDateLabel = cachedDutyDateLabel,
                cachedAtMillis = cachedAtMillis,

                district = pharmacy.district.orEmpty(),
                name = pharmacy.name.orEmpty(),
                address = pharmacy.address.orEmpty(),
                phone = pharmacy.phone.orEmpty(),

                providerId = pharmacy.providerId,
                districtSlug = pharmacy.districtSlug.orEmpty(),
                directions = pharmacy.directions.orEmpty(),
                latitude = pharmacy.latitude,
                longitude = pharmacy.longitude
            )
        }
}

fun List<CachedPharmacyEntity>.toOfflineResponse(): PharmacyResponse? {
    val firstItem = firstOrNull() ?: return null

    return PharmacyResponse(
        city = firstItem.cityName,
        source = "offline",
        checkedAt = null,
        dutyDate = firstItem.dutyDate,
        dutyDateLabel = firstItem.dutyDateLabel,
        pharmacies = map { entity ->
            Pharmacy(
                providerId = entity.providerId,
                district = entity.district,
                districtSlug = entity.districtSlug,
                name = entity.name,
                address = entity.address,
                phone = entity.phone,
                directions = entity.directions,
                latitude = entity.latitude,
                longitude = entity.longitude
            )
        }
    )
}
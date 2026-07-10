package com.example.pharmacytrack.data.mapper

import com.example.pharmacytrack.data.model.Pharmacy
import com.example.pharmacytrack.data.model.PharmacyResponse

fun PharmacyResponse.toSafePharmacyList(): List<Pharmacy> {
    return pharmacies
        .orEmpty()
        .filterNotNull()
        .map { pharmacy ->
            pharmacy.toSafePharmacy()
        }
}

fun PharmacyResponse.resolveCityName(fallbackCity: String): String {
    return fallbackCity.ifBlank {
        city.orEmpty()
    }
}

fun PharmacyResponse.resolveSource(): String {
    return source.orEmpty()
}

fun PharmacyResponse.resolveCheckedAt(): String {
    return checkedAt.orEmpty().trim()
}

fun PharmacyResponse.resolveDutyDate(): String {
    return dutyDate.orEmpty().trim()
}

fun PharmacyResponse.resolveDutyDateLabel(): String {
    return dutyDateLabel.orEmpty().trim()
}

private fun Pharmacy.toSafePharmacy(): Pharmacy {
    return Pharmacy(
        providerId = providerId,
        city = city.orEmpty().trim(),
        citySlug = citySlug.orEmpty().trim(),
        district = district.orEmpty().trim(),
        districtSlug = districtSlug.orEmpty().trim(),
        name = name.orEmpty().trim(),
        address = address.orEmpty().trim(),
        phone = phone.orEmpty().trim(),
        directions = directions.orEmpty().trim(),
        latitude = latitude,
        longitude = longitude,
        distanceMeters = distanceMeters
    )
}
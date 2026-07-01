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

fun PharmacyResponse.resolveLastUpdated(): String {
    return lastUpdated.orEmpty()
}

private fun Pharmacy.toSafePharmacy(): Pharmacy {
    return Pharmacy(
        district = district.orEmpty().trim(),
        name = name.orEmpty().trim(),
        address = address.orEmpty().trim(),
        phone = phone.orEmpty().trim()
    )
}

private fun String.toDisplayCityName(): String {
    return trim()
        .lowercase()
        .replaceFirstChar { firstChar ->
            firstChar.uppercase()
        }
}
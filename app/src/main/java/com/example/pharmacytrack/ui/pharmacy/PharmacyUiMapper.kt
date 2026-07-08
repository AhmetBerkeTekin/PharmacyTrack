package com.example.pharmacytrack.ui.pharmacy

import com.example.pharmacytrack.core.text.toTurkishSearchKey
import com.example.pharmacytrack.data.model.Pharmacy

fun List<Pharmacy>.toUiModels(
    city: String,
    favoriteKeys: Set<String>
): List<PharmacyUiModel> {
    return map { pharmacy ->
        pharmacy.toUiModel(
            city = city,
            favoriteKeys = favoriteKeys
        )
    }
}

private fun Pharmacy.toUiModel(
    city: String,
    favoriteKeys: Set<String>
): PharmacyUiModel {
    val cityValue = city.trim()
    val districtValue = district.orEmpty().trim()
    val nameValue = name.orEmpty().trim()
    val addressValue = address.orEmpty().trim()
    val phoneValue = phone.orEmpty().trim()

    val favoriteKey = buildFavoriteKey(
        city = cityValue,
        district = districtValue,
        name = nameValue,
        address = addressValue,
        phone = phoneValue
    )

    return PharmacyUiModel(
        city = cityValue,
        district = districtValue,
        name = nameValue,
        address = addressValue,
        phone = phoneValue,
        favoriteKey = favoriteKey,
        isFavorite = favoriteKey in favoriteKeys,

        providerId = providerId,
        districtSlug = districtSlug.orEmpty().trim(),
        directions = directions.orEmpty().trim(),
        latitude = latitude,
        longitude = longitude
    )
}

private fun buildFavoriteKey(
    city: String,
    district: String,
    name: String,
    address: String,
    phone: String
): String {
    return listOf(
        city.toTurkishSearchKey(),
        district.toTurkishSearchKey(),
        name.toTurkishSearchKey(),
        address.toTurkishSearchKey(),
        phone.filter { it.isDigit() }
    ).joinToString("|")
}
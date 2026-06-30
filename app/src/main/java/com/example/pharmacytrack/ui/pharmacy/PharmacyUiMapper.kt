package com.example.pharmacytrack.ui.pharmacy

import com.example.pharmacytrack.core.text.toTurkishSearchKey
import com.example.pharmacytrack.data.model.Pharmacy

fun List<Pharmacy>.toUiModels(favoriteKeys: Set<String>): List<PharmacyUiModel> {
    return map { pharmacy ->
        pharmacy.toUiModel(favoriteKeys)
    }
}

private fun Pharmacy.toUiModel(favoriteKeys: Set<String>): PharmacyUiModel {
    val districtValue = district.orEmpty().trim()
    val nameValue = name.orEmpty().trim()
    val addressValue = address.orEmpty().trim()
    val phoneValue = phone.orEmpty().trim()

    val favoriteKey = buildFavoriteKey(
        district = districtValue,
        name = nameValue,
        address = addressValue,
        phone = phoneValue
    )

    return PharmacyUiModel(
        district = districtValue,
        name = nameValue,
        address = addressValue,
        phone = phoneValue,
        favoriteKey = favoriteKey,
        isFavorite = favoriteKey in favoriteKeys
    )
}

private fun buildFavoriteKey(
    district: String,
    name: String,
    address: String,
    phone: String
): String {
    return listOf(
        district.toTurkishSearchKey(),
        name.toTurkishSearchKey(),
        address.toTurkishSearchKey(),
        phone.filter { it.isDigit() }
    ).joinToString("|")
}
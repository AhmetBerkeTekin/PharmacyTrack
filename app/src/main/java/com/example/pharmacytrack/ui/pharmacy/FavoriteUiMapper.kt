package com.example.pharmacytrack.ui.pharmacy

import com.example.pharmacytrack.data.local.FavoritePharmacy

fun PharmacyUiModel.toFavoritePharmacy(): FavoritePharmacy {
    return FavoritePharmacy(
        favoriteKey = favoriteKey,
        district = district,
        name = name,
        address = address,
        phone = phone
    )
}

fun FavoritePharmacy.toPharmacyUiModel(): PharmacyUiModel {
    return PharmacyUiModel(
        district = district,
        name = name,
        address = address,
        phone = phone,
        favoriteKey = favoriteKey,
        isFavorite = true
    )
}

fun List<FavoritePharmacy>.toPharmacyUiModels(): List<PharmacyUiModel> {
    return map { favoritePharmacy ->
        favoritePharmacy.toPharmacyUiModel()
    }
}
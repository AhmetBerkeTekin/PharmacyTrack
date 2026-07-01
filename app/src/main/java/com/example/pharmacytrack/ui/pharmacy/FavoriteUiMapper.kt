package com.example.pharmacytrack.ui.pharmacy

import com.example.pharmacytrack.data.local.FavoritePharmacy

fun PharmacyUiModel.toFavoritePharmacy(): FavoritePharmacy {
    return FavoritePharmacy(
        favoriteKey = favoriteKey,
        city = city,
        district = district,
        name = name,
        address = address,
        phone = phone
    )
}

fun FavoritePharmacy.toPharmacyUiModel(): PharmacyUiModel {
    val favoriteKeyValue = favoriteKey.orEmpty()

    return PharmacyUiModel(
        city = city.orEmpty(),
        district = district.orEmpty(),
        name = name.orEmpty(),
        address = address.orEmpty(),
        phone = phone.orEmpty(),
        favoriteKey = favoriteKeyValue,
        isFavorite = true
    )
}

fun List<FavoritePharmacy>.toPharmacyUiModels(): List<PharmacyUiModel> {
    return map { favoritePharmacy ->
        favoritePharmacy.toPharmacyUiModel()
    }
}
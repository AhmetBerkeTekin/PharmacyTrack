package com.example.pharmacytrack.ui.pharmacy

import com.example.pharmacytrack.data.local.FavoritePharmacy

fun PharmacyUiModel.toFavoritePharmacy(): FavoritePharmacy {
    return FavoritePharmacy(
        favoriteKey = favoriteKey,
        city = city,
        district = district,
        name = name,
        address = address,
        phone = phone,
        providerId = providerId,
        districtSlug = districtSlug,
        directions = directions,
        latitude = latitude,
        longitude = longitude
    )
}

fun FavoritePharmacy.toPharmacyUiModel(): PharmacyUiModel {
    return PharmacyUiModel(
        city = city.orEmpty(),
        district = district.orEmpty(),
        name = name.orEmpty(),
        address = address.orEmpty(),
        phone = phone.orEmpty(),
        favoriteKey = favoriteKey.orEmpty(),
        isFavorite = true,
        providerId = providerId,
        districtSlug = districtSlug.orEmpty(),
        directions = directions.orEmpty(),
        latitude = latitude,
        longitude = longitude
    )
}

fun List<FavoritePharmacy>.toPharmacyUiModels(): List<PharmacyUiModel> {
    return map { favoritePharmacy ->
        favoritePharmacy.toPharmacyUiModel()
    }
}
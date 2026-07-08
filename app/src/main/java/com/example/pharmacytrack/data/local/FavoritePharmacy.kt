package com.example.pharmacytrack.data.local

data class FavoritePharmacy(
    val favoriteKey: String?,
    val city: String?,
    val district: String?,
    val name: String?,
    val address: String?,
    val phone: String?,

    val providerId: Long? = null,
    val districtSlug: String? = null,
    val directions: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)
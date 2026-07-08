package com.example.pharmacytrack.ui.pharmacy

data class PharmacyUiModel(
    val city: String,
    val district: String,
    val name: String,
    val address: String,
    val phone: String,
    val favoriteKey: String,
    val isFavorite: Boolean = false,

    val providerId: Long? = null,
    val districtSlug: String = "",
    val directions: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null
)
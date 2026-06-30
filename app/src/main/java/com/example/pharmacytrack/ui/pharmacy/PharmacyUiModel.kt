package com.example.pharmacytrack.ui.pharmacy

data class PharmacyUiModel(
    val district: String,
    val name: String,
    val address: String,
    val phone: String,
    val favoriteKey: String,
    val isFavorite: Boolean = false
)
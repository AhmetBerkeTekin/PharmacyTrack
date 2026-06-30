package com.example.pharmacytrack.ui.favorites

import com.example.pharmacytrack.ui.pharmacy.PharmacyUiModel

sealed interface FavoritesUiState {

    data object Empty : FavoritesUiState

    data class Success(
        val pharmacies: List<PharmacyUiModel>
    ) : FavoritesUiState
}
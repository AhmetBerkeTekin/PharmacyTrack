package com.example.pharmacytrack.ui.pharmacy

import com.example.pharmacytrack.core.ui.UiText

sealed interface PharmacyUiState {
    data object Idle : PharmacyUiState
    data object Loading : PharmacyUiState

    data class Success(
        val city: String,
        val dutyDate: String,
        val isOffline: Boolean,
        val isStale: Boolean,
        val pharmacies: List<PharmacyUiModel>,
        val districtOptions: List<String>,
        val selectedDistrict: String?
    ) : PharmacyUiState

    data class Error(
        val message: UiText
    ) : PharmacyUiState
}
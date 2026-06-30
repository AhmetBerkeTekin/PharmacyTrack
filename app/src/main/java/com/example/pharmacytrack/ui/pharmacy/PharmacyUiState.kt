package com.example.pharmacytrack.ui.pharmacy

import com.example.pharmacytrack.core.ui.UiText
import com.example.pharmacytrack.data.model.Pharmacy

sealed interface PharmacyUiState {
    data object Idle : PharmacyUiState
    data object Loading : PharmacyUiState

    data class Success(
        val city: String,
        val source: String,
        val pharmacies: List<PharmacyUiModel>,
        val districtOptions: List<String>,
        val selectedDistrict: String?
    ) : PharmacyUiState

    data class Error(
        val message: UiText
    ) : PharmacyUiState
}
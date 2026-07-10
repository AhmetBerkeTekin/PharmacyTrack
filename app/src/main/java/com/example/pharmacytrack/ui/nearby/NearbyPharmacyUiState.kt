package com.example.pharmacytrack.ui.nearby

import com.example.pharmacytrack.core.ui.UiText
import com.example.pharmacytrack.ui.pharmacy.PharmacyUiModel

sealed interface NearbyPharmacyUiState {

    data object Idle : NearbyPharmacyUiState

    data object Locating : NearbyPharmacyUiState

    data object Loading : NearbyPharmacyUiState

    data class PermissionRequired(
        val openSettings: Boolean
    ) : NearbyPharmacyUiState

    data class Success(
        val dutyDate: String,
        val pharmacies: List<PharmacyUiModel>
    ) : NearbyPharmacyUiState

    data object Empty : NearbyPharmacyUiState

    data class Error(
        val message: UiText
    ) : NearbyPharmacyUiState
}
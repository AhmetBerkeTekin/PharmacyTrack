package com.example.pharmacytrack.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pharmacytrack.data.local.FavoriteStore
import com.example.pharmacytrack.ui.pharmacy.PharmacyUiModel
import com.example.pharmacytrack.ui.pharmacy.toFavoritePharmacy
import com.example.pharmacytrack.ui.pharmacy.toPharmacyUiModels
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val favoriteStore: FavoriteStore
) : ViewModel() {

    val uiState: StateFlow<FavoritesUiState> =
        favoriteStore.favoritePharmaciesFlow
            .map { favoritePharmacies ->
                val pharmacies = favoritePharmacies.toPharmacyUiModels()

                if (pharmacies.isEmpty()) {
                    FavoritesUiState.Empty
                } else {
                    FavoritesUiState.Success(pharmacies)
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = FavoritesUiState.Empty
            )

    fun toggleFavorite(pharmacy: PharmacyUiModel) {
        viewModelScope.launch {
            favoriteStore.toggleFavorite(
                pharmacy.toFavoritePharmacy()
            )
        }
    }
}
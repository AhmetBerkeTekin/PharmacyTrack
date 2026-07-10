package com.example.pharmacytrack.ui.nearby

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pharmacytrack.R
import com.example.pharmacytrack.core.ui.UiText
import com.example.pharmacytrack.data.local.FavoriteStore
import com.example.pharmacytrack.data.model.Pharmacy
import com.example.pharmacytrack.data.repository.PharmacyRepository
import com.example.pharmacytrack.ui.pharmacy.PharmacyUiModel
import com.example.pharmacytrack.ui.pharmacy.toFavoritePharmacy
import com.example.pharmacytrack.ui.pharmacy.toNearbyUiModels
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class NearbyPharmacyViewModel @Inject constructor(
    private val pharmacyRepository: PharmacyRepository,
    private val favoriteStore: FavoriteStore
) : ViewModel() {

    companion object {
        private const val DEFAULT_RADIUS_METERS = 10_000
    }

    private val _uiState =
        MutableStateFlow<NearbyPharmacyUiState>(
            NearbyPharmacyUiState.Idle
        )

    val uiState: StateFlow<NearbyPharmacyUiState> =
        _uiState.asStateFlow()

    private var latestPharmacies: List<Pharmacy> =
        emptyList()

    private var latestDutyDate: String = ""

    init {
        observeFavoriteChanges()
    }

    fun showLocating() {
        _uiState.value = NearbyPharmacyUiState.Locating
    }

    fun showPermissionRequired(openSettings: Boolean) {
        _uiState.value = NearbyPharmacyUiState.PermissionRequired(
                openSettings = openSettings
            )
    }

    fun showLocationUnavailable() {
        showError(
            R.string.error_location_unavailable
        )
    }

    fun loadNearbyPharmacies(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            _uiState.value = NearbyPharmacyUiState.Loading

            try {
                val response =
                    pharmacyRepository.getNearbyPharmacies(
                        latitude = latitude,
                        longitude = longitude,
                        radius = DEFAULT_RADIUS_METERS
                    )

                latestPharmacies = response.pharmacies
                    .orEmpty()
                    .filterNotNull()

                latestDutyDate = response.dutyDate
                    .orEmpty()
                    .trim()

                if (latestPharmacies.isEmpty()) {
                    _uiState.value = NearbyPharmacyUiState.Empty

                    return@launch
                }

                publishSuccessState()

            } catch (exception: CancellationException) {
                throw exception

            } catch (exception: HttpException) {
                showError(
                    resolveHttpErrorMessage(
                        exception.code()
                    )
                )

            } catch (exception: IOException) {
                showError(
                    R.string.error_network_connection
                )

            } catch (exception: Exception) {
                showError(
                    R.string.error_unexpected
                )
            }
        }
    }

    fun toggleFavorite(pharmacy: PharmacyUiModel) {
        viewModelScope.launch {
            favoriteStore.toggleFavorite(
                pharmacy.toFavoritePharmacy()
            )
        }
    }

    private suspend fun publishSuccessState() {
        val favoriteKeys = favoriteStore.favoriteKeysFlow.first()

        val uiModels = latestPharmacies.toNearbyUiModels(favoriteKeys = favoriteKeys)

        _uiState.value = NearbyPharmacyUiState.Success(
                dutyDate = latestDutyDate,
                pharmacies = uiModels
            )
    }

    private fun observeFavoriteChanges() {
        viewModelScope.launch {
            favoriteStore.favoriteKeysFlow.collect { favoriteKeys ->

                val currentState = _uiState.value
                if (currentState !is NearbyPharmacyUiState.Success) {
                    return@collect
                }

                _uiState.value =
                    currentState.copy(
                        pharmacies = latestPharmacies.toNearbyUiModels(favoriteKeys = favoriteKeys)
                    )
            }
        }
    }

    private fun resolveHttpErrorMessage(statusCode: Int): Int {
        return when (statusCode) {
            429, 503 ->
                R.string.error_pharmacy_service_busy

            502, 504 ->
                R.string.error_pharmacy_service_unavailable

            else ->
                R.string.error_nearby_pharmacies_failed
        }
    }

    private fun showError(@StringRes messageResId: Int) {
        _uiState.value =
            NearbyPharmacyUiState.Error(
                message = UiText.StringResource(
                    messageResId
                )
            )
    }
}
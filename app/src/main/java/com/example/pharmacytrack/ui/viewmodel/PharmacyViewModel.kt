package com.example.pharmacytrack.ui.pharmacy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pharmacytrack.R
import com.example.pharmacytrack.core.result.AppResult
import com.example.pharmacytrack.core.result.toUiText
import com.example.pharmacytrack.core.text.toApiCitySlug
import com.example.pharmacytrack.core.ui.UiText
import com.example.pharmacytrack.data.local.FavoriteStore
import com.example.pharmacytrack.data.local.UserPreferences
import com.example.pharmacytrack.data.model.Pharmacy
import com.example.pharmacytrack.data.repository.PharmacyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.example.pharmacytrack.data.mapper.resolveCityName
import com.example.pharmacytrack.data.mapper.resolveLastUpdated
import com.example.pharmacytrack.data.mapper.resolveSource
import com.example.pharmacytrack.data.mapper.toSafePharmacyList

@HiltViewModel
class PharmacyViewModel @Inject constructor(
    private val repository: PharmacyRepository,
    private val userPreferences: UserPreferences,
    private val favoriteStore: FavoriteStore
) : ViewModel() {

    private val _uiState = MutableStateFlow<PharmacyUiState>(PharmacyUiState.Idle)
    val uiState: StateFlow<PharmacyUiState> = _uiState.asStateFlow()

    private var currentCity: String = ""
    private var currentSource: String = ""
    private var currentLastUpdated: String = ""
    private var hasTriedAutoLoadLastCity = false
    private var allPharmacies: List<Pharmacy> = emptyList()
    private var selectedDistrict: String? = null
    val lastCityFlow = userPreferences.lastCityFlow
    private var favoriteKeys: Set<String> = emptySet()

    init {
        observeFavoriteKeys()
    }

    private fun observeFavoriteKeys() {
        viewModelScope.launch {
            favoriteStore.favoriteKeysFlow.collect { keys ->
                favoriteKeys = keys

                if (allPharmacies.isNotEmpty()) {
                    emitSuccessState()
                }
            }
        }
    }

    fun getPharmacies(city: String, forceRefresh: Boolean = false) {
        val normalizedCity = city.trim()

        if (normalizedCity.isBlank()) {
            _uiState.value = PharmacyUiState.Error(
                UiText.StringResource(R.string.error_invalid_city)
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = PharmacyUiState.Loading

            val apiCitySlug = normalizedCity.toApiCitySlug()
            val result = repository.getPharmacies(apiCitySlug, forceRefresh)

            when (result) {
                is AppResult.Success -> {
                    val response = result.data

                    currentCity = response.resolveCityName(
                        fallbackCity = normalizedCity
                    )

                    currentSource = response.resolveSource()
                    currentLastUpdated = response.resolveLastUpdated()

                    allPharmacies = response.toSafePharmacyList()

                    val rememberedDistrict = userPreferences.getLastDistrictForCity(currentCity)
                    val districtOptions = getDistrictOptions(allPharmacies)

                    selectedDistrict = if (rememberedDistrict in districtOptions) {
                        rememberedDistrict
                    } else {
                        null
                    }

                    viewModelScope.launch {
                        userPreferences.saveLastCity(currentCity)
                    }

                    emitSuccessState()
                }

                is AppResult.Error -> {
                    _uiState.value = PharmacyUiState.Error(
                        message = result.error.toUiText()
                    )
                }
            }
        }
    }

    fun refreshCurrentCity() {
        if (currentCity.isBlank()) {
            return
        }

        getPharmacies(
            city = currentCity,
            forceRefresh = true
        )
    }

    fun selectDistrict(district: String?) {
        selectedDistrict = district

        if (currentCity.isNotBlank()) {
            viewModelScope.launch {
                userPreferences.saveLastDistrictForCity(
                    city = currentCity,
                    district = district
                )
            }
        }

        emitSuccessState()
    }

    private fun getDistrictOptions(pharmacies: List<Pharmacy>): List<String> { return pharmacies
            .map { it.district.orEmpty().trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }

    fun loadLastCityIfNeeded(city: String): Boolean {
        if (hasTriedAutoLoadLastCity) {
            return false
        }

        if (city.isBlank()) {
            return false
        }

        if (currentCity.isNotBlank()) {
            return false
        }

        hasTriedAutoLoadLastCity = true

        getPharmacies(
            city = city,
            forceRefresh = false
        )

        return true
    }

    private fun emitSuccessState() {
        val districtOptions = getDistrictOptions(allPharmacies)

        val filteredPharmacies = selectedDistrict?.let { selected ->
            allPharmacies.filter { pharmacy ->
                pharmacy.district.orEmpty().trim() == selected
            }
        } ?: allPharmacies

        _uiState.value = PharmacyUiState.Success(
            city = currentCity,
            source = currentSource,
            lastUpdatedAt = currentLastUpdated,
            pharmacies = filteredPharmacies.toUiModels(
                city = currentCity,
                favoriteKeys = favoriteKeys
            ),
            districtOptions = districtOptions,
            selectedDistrict = selectedDistrict
        )
    }

    fun toggleFavorite(pharmacy: PharmacyUiModel) {
        viewModelScope.launch {
            favoriteStore.toggleFavorite(
                pharmacy.toFavoritePharmacy()
            )
        }
    }
}
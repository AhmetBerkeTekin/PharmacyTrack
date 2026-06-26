package com.example.pharmacytrack.ui.pharmacy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pharmacytrack.core.result.AppResult
import com.example.pharmacytrack.core.result.toUserMessage
import com.example.pharmacytrack.data.local.UserPreferences
import com.example.pharmacytrack.data.model.Pharmacy
import com.example.pharmacytrack.data.repository.PharmacyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PharmacyViewModel @Inject constructor(
    private val repository: PharmacyRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow<PharmacyUiState>(PharmacyUiState.Idle)
    val uiState: StateFlow<PharmacyUiState> = _uiState.asStateFlow()

    private var currentCity: String = ""
    private var currentSource: String = ""
    private var allPharmacies: List<Pharmacy> = emptyList()
    private var selectedDistrict: String? = null
    val lastCityFlow = userPreferences.lastCityFlow

    fun getPharmacies(city: String) {
        val normalizedCity = city.trim()

        if (normalizedCity.isBlank()) {
            _uiState.value = PharmacyUiState.Error("Şehir boş olamaz.")
            return
        }

        viewModelScope.launch {
            _uiState.value = PharmacyUiState.Loading

            val result = repository.getPharmacies(normalizedCity)

            when (result) {
                is AppResult.Success -> {
                    val response = result.data

                    currentCity = response.city
                        .orEmpty()
                        .ifBlank { normalizedCity }
                        .let { formatCityName(it) }

                    currentSource = response.source.orEmpty()

                    allPharmacies = response.pharmacies
                        .orEmpty()
                        .filterNotNull()
                        .map { pharmacy ->
                            Pharmacy(
                                district = pharmacy.district.orEmpty(),
                                name = pharmacy.name.orEmpty(),
                                address = pharmacy.address.orEmpty(),
                                phone = pharmacy.phone.orEmpty()
                            )
                        }

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
                        message = result.error.toUserMessage()
                    )
                }
            }
        }
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
            pharmacies = filteredPharmacies,
            districtOptions = districtOptions,
            selectedDistrict = selectedDistrict
        )
    }

    private fun formatCityName(city: String): String {
        return city.trim()
            .lowercase()
            .replaceFirstChar { it.uppercase() }
    }
}
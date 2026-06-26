package com.example.pharmacytrack

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pharmacytrack.ui.pharmacy.PharmacyAdapter
import com.example.pharmacytrack.ui.pharmacy.PharmacyUiState
import com.example.pharmacytrack.ui.pharmacy.PharmacyViewModel
import com.google.android.material.button.MaterialButton
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.widget.ArrayAdapter
import com.example.pharmacytrack.core.city.CityProvider
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.card.MaterialCardView

import com.example.pharmacytrack.util.PharmacyActionHelper

import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: PharmacyViewModel by viewModels()

    private lateinit var pharmacyAdapter: PharmacyAdapter

    private lateinit var cityEditText: MaterialAutoCompleteTextView
    private lateinit var searchButton: MaterialButton
    private lateinit var statusTextView: TextView
    private lateinit var loadingProgressBar: ProgressBar
    private lateinit var pharmacyRecyclerView: RecyclerView
    private lateinit var districtChipGroup: ChipGroup
    private lateinit var stateCardView: MaterialCardView
    private lateinit var stateTitleTextView: TextView
    private lateinit var stateMessageTextView: TextView
    private lateinit var retryButton: MaterialButton

    @Inject
    lateinit var cityProvider: CityProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        bindViews()
        setupCityDropdown()
        setupRecyclerView()
        setupClickListeners()
        observeUiState()
        observeLastCity()
    }

    private fun bindViews() {
        cityEditText = findViewById(R.id.cityEditText)
        searchButton = findViewById(R.id.searchButton)
        statusTextView = findViewById(R.id.statusTextView)
        loadingProgressBar = findViewById(R.id.loadingProgressBar)
        pharmacyRecyclerView = findViewById(R.id.pharmacyRecyclerView)
        districtChipGroup = findViewById(R.id.districtChipGroup)
        stateCardView = findViewById(R.id.stateCardView)
        stateTitleTextView = findViewById(R.id.stateTitleTextView)
        stateMessageTextView = findViewById(R.id.stateMessageTextView)
        retryButton = findViewById(R.id.retryButton)
    }

    private fun setupCityDropdown() {
        val cityAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            cityProvider.cities
        )

        cityEditText.setAdapter(cityAdapter)
        cityEditText.threshold = 1

        cityEditText.setOnClickListener {
            cityEditText.showDropDown()
        }

        cityEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                cityEditText.showDropDown()
            }
        }

        cityEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                actionId == EditorInfo.IME_ACTION_DONE
            ) {
                searchPharmacies()
                true
            } else {
                false
            }
        }
    }

    private fun setupRecyclerView() {
        pharmacyAdapter = PharmacyAdapter(
            onCallClicked = { pharmacy ->
                PharmacyActionHelper.openDialer(
                    context = this,
                    phone = pharmacy.phone
                )
            },
            onMapClicked = { pharmacy ->
                PharmacyActionHelper.openMap(
                    context = this,
                    pharmacy = pharmacy
                )
            }
        )

        pharmacyRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = pharmacyAdapter
            setHasFixedSize(false)
        }
    }

    private fun setupClickListeners() {
        searchButton.setOnClickListener {
            searchPharmacies()
        }

        retryButton.setOnClickListener {
            searchPharmacies()
        }
    }

    private fun searchPharmacies() {
        val inputCity = cityEditText.text?.toString().orEmpty()
        val matchedCity = cityProvider.findValidCity(inputCity)

        if (matchedCity == null) {
            cityEditText.error = getString(R.string.error_invalid_city)
            return
        }

        cityEditText.error = null
        hideKeyboard()
        cityEditText.clearFocus()

        viewModel.getPharmacies(matchedCity)
    }

    private fun observeUiState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    renderUiState(state)
                }
            }
        }
    }

    private fun observeLastCity() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.lastCityFlow.collect { lastCity ->
                    if (lastCity.isNotBlank() && cityEditText.text.isNullOrBlank()) {
                        cityEditText.setText(lastCity, false)
                    }
                }
            }
        }
    }

    private fun renderUiState(state: PharmacyUiState) {
        when (state) {
            PharmacyUiState.Idle -> {
                showIdle()
            }

            PharmacyUiState.Loading -> {
                showLoading()
            }

            is PharmacyUiState.Success -> {
                showSuccess(state)
            }

            is PharmacyUiState.Error -> {
                showError(state.message)
            }
        }
    }

    private fun showIdle() {
        hideLoadingIndicator()
        hideRecyclerView()
        hideStatusText()
        hideDistrictFilters()
        resetSearchButton()
        clearPharmacyList()

        showStateCard(
            title = getString(R.string.state_idle_title),
            message = getString(R.string.state_idle_message),
            showRetry = false
        )
    }

    private fun showLoading() {
        showLoadingIndicator()
        hideRecyclerView()
        hideStatusText()
        hideDistrictFilters()
        setSearchButtonLoading()
        clearPharmacyList()

        showStateCard(
            title = getString(R.string.state_loading_title),
            message = getString(R.string.state_loading_message),
            showRetry = false
        )
    }

    private fun showSuccess(state: PharmacyUiState.Success) {
        hideContentState()
        resetSearchButton()
        showDistrictFilters(state)

        val locationText = buildLocationText(
            city = state.city,
            district = state.selectedDistrict
        )

        if (state.pharmacies.isEmpty()) {
            clearPharmacyList()

            showOnlyStateCard(
                title = getString(R.string.state_empty_title),
                message = getString(R.string.state_empty_message, locationText),
                showRetry = true
            )

            return
        }

        hideStateCard()
        showRecyclerView()

        showStatusText(
            getString(
                R.string.status_found_pharmacies,
                locationText,
                state.pharmacies.size
            )
        )

        pharmacyAdapter.submitList(state.pharmacies) {
            pharmacyRecyclerView.scrollToPosition(0)
        }
    }

    private fun showError(message: String) {
        hideLoadingIndicator()
        hideRecyclerView()
        hideStatusText()
        hideDistrictFilters()
        resetSearchButton()
        clearPharmacyList()

        showStateCard(
            title = getString(R.string.state_error_title),
            message = message.ifBlank {
                getString(R.string.state_error_default_message)
            },
            showRetry = true
        )
    }

    private fun showStateCard(title: String, message: String, showRetry: Boolean) {
        stateCardView.visibility = View.VISIBLE
        stateTitleTextView.text = title
        stateMessageTextView.text = message
        retryButton.visibility = if (showRetry) View.VISIBLE else View.GONE
    }

    private fun hideStateCard() {
        stateCardView.visibility = View.GONE
        retryButton.visibility = View.GONE
    }

    private fun setupDistrictChips(state: PharmacyUiState.Success) {
        districtChipGroup.setOnCheckedStateChangeListener(null)
        districtChipGroup.removeAllViews()

        val allChip = createDistrictChip(
            text = "Tümü",
            district = null,
            checked = state.selectedDistrict == null
        )

        districtChipGroup.addView(allChip)

        state.districtOptions.forEach { district ->
            val chip = createDistrictChip(
                text = district,
                district = district,
                checked = state.selectedDistrict == district
            )

            districtChipGroup.addView(chip)
        }

        districtChipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            val checkedId = checkedIds.firstOrNull() ?: return@setOnCheckedStateChangeListener
            val checkedChip = group.findViewById<Chip>(checkedId)
            val district = checkedChip.tag as? String

            viewModel.selectDistrict(district)
        }
    }

    private fun createDistrictChip(text: String, district: String?, checked: Boolean): Chip {
        return Chip(this).apply {
            id = View.generateViewId()
            this.text = text
            tag = district
            isCheckable = true
            isChecked = checked
        }
    }

    private fun hideKeyboard() {
        val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val currentFocusedView = currentFocus

        if (currentFocusedView != null) {
            inputMethodManager.hideSoftInputFromWindow(
                currentFocusedView.windowToken,
                0
            )
        }
    }

    // Helpers

    private fun buildLocationText(city: String, district: String?): String {
        return if (district.isNullOrBlank()) {
            city
        } else {
            "$city / $district"
        }
    }

    private fun showLoadingIndicator() {
        loadingProgressBar.visibility = View.VISIBLE
    }

    private fun hideLoadingIndicator() {
        loadingProgressBar.visibility = View.GONE
    }

    private fun showRecyclerView() {
        pharmacyRecyclerView.visibility = View.VISIBLE
    }

    private fun hideRecyclerView() {
        pharmacyRecyclerView.visibility = View.GONE
    }

    private fun showStatusText(message: String) {
        statusTextView.visibility = View.VISIBLE
        statusTextView.text = message
    }

    private fun hideStatusText() {
        statusTextView.visibility = View.GONE
    }

    private fun resetSearchButton() {
        searchButton.isEnabled = true
        searchButton.text = getString(R.string.button_search_pharmacies)
    }

    private fun setSearchButtonLoading() {
        searchButton.isEnabled = false
        searchButton.text = getString(R.string.button_loading)
    }

    private fun clearPharmacyList() {
        pharmacyAdapter.submitList(emptyList())
    }

    private fun hideDistrictFilters() {
        districtChipGroup.visibility = View.GONE
        districtChipGroup.removeAllViews()
    }

    private fun showDistrictFilters(state: PharmacyUiState.Success) {
        if (state.districtOptions.isEmpty()) {
            hideDistrictFilters()
            return
        }

        districtChipGroup.visibility = View.VISIBLE
        setupDistrictChips(state)
    }

    private fun showOnlyStateCard(title: String, message: String, showRetry: Boolean) {
        hideRecyclerView()
        hideStatusText()

        showStateCard(
            title = title,
            message = message,
            showRetry = showRetry
        )
    }

    private fun hideContentState() {
        hideStateCard()
        hideLoadingIndicator()
    }
}
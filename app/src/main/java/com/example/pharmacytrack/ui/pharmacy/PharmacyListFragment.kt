package com.example.pharmacytrack.ui.pharmacy

import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pharmacytrack.R
import com.example.pharmacytrack.core.city.CityProvider
import com.example.pharmacytrack.util.PharmacyActionHelper
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class PharmacyListFragment : Fragment(R.layout.fragment_pharmacy_list) {

    private val viewModel: PharmacyViewModel by viewModels()

    private lateinit var pharmacyAdapter: PharmacyAdapter
    private lateinit var cityInputLayout: TextInputLayout
    private lateinit var cityEditText: MaterialAutoCompleteTextView
    private lateinit var searchButton: MaterialButton
    private lateinit var resultSummaryCardView: MaterialCardView
    private lateinit var statusTextView: TextView
    private lateinit var loadingProgressBar: ProgressBar
    private lateinit var pharmacyRecyclerView: RecyclerView
    private lateinit var districtInputLayout: TextInputLayout
    private lateinit var districtAutoCompleteTextView: MaterialAutoCompleteTextView
    private lateinit var stateCardView: MaterialCardView
    private lateinit var stateTitleTextView: TextView
    private lateinit var stateMessageTextView: TextView
    private lateinit var dutyDateTextView: TextView
    private lateinit var resultLocationTextView: TextView
    private lateinit var retryButton: MaterialButton

    private var isUpdatingDistrictDropdown = false
    private var shouldScroll = false // Consider using a scroll event instead

    @Inject
    lateinit var cityProvider: CityProvider

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bindViews(view)
        setupCityDropdown()
        setupDistrictDropdown()
        setupRecyclerView()
        setupClickListeners()
        observeUiState()
        observeLastCity()
    }

    private fun bindViews(view: View) {
        cityInputLayout = view.findViewById(R.id.cityInputLayout)
        cityEditText = view.findViewById(R.id.cityEditText)
        searchButton = view.findViewById(R.id.searchButton)
        resultSummaryCardView = view.findViewById(R.id.resultSummaryCardView)
        statusTextView = view.findViewById(R.id.statusTextView)
        loadingProgressBar = view.findViewById(R.id.loadingProgressBar)
        pharmacyRecyclerView = view.findViewById(R.id.pharmacyRecyclerView)
        districtInputLayout = view.findViewById(R.id.districtInputLayout)
        districtAutoCompleteTextView = view.findViewById(R.id.districtAutoCompleteTextView)
        stateCardView = view.findViewById(R.id.stateCardView)
        stateTitleTextView = view.findViewById(R.id.stateTitleTextView)
        stateMessageTextView = view.findViewById(R.id.stateMessageTextView)
        dutyDateTextView = view.findViewById(R.id.dutyDateTextView)
        resultLocationTextView = view.findViewById(R.id.resultLocationTextView)
        retryButton = view.findViewById(R.id.retryButton)
    }

    private fun setupCityDropdown() {
        val cityAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_1,
            cityProvider.cities
        )

        cityEditText.apply {
            setAdapter(cityAdapter)

            threshold = 0
            keyListener = null
            isCursorVisible = false

            setOnClickListener {
                requestFocus()
                showDropDown()
            }

            setOnItemClickListener { parent, _, position, _ ->
                val selectedCity = parent
                    .getItemAtPosition(position)
                    .toString()

                setText(selectedCity, false)
                dismissDropDown()
                clearFocus()

                cityInputLayout.error = null
            }
        }

        cityInputLayout.setEndIconOnClickListener {
            cityEditText.requestFocus()
            cityEditText.showDropDown()
        }
    }

    private fun setupDistrictDropdown() {
        districtAutoCompleteTextView.apply {
            keyListener = null
            isCursorVisible = false

            setOnClickListener {
                requestFocus()
                showDropDown()
            }

            setOnItemClickListener { parent, _, position, _ ->
                if (isUpdatingDistrictDropdown) {
                    return@setOnItemClickListener
                }

                val selectedText = parent
                    .getItemAtPosition(position)
                    .toString()

                setText(selectedText, false)
                dismissDropDown()
                clearFocus()

                val selectedDistrict =
                    if (selectedText == getString(R.string.district_filter_all)) {
                        null
                    } else {
                        selectedText
                    }

                shouldScroll = true
                viewModel.selectDistrict(selectedDistrict)
            }
        }

        districtInputLayout.setEndIconOnClickListener {
            districtAutoCompleteTextView.requestFocus()
            districtAutoCompleteTextView.showDropDown()
        }
    }

    private fun setupRecyclerView() {
        pharmacyAdapter = PharmacyAdapter(
            onCallClicked = { pharmacy ->
                PharmacyActionHelper.openDialer(
                    context = requireContext(),
                    phone = pharmacy.phone
                )
            },
            onMapClicked = { pharmacy ->
                PharmacyActionHelper.openMap(
                    context = requireContext(),
                    name = pharmacy.name,
                    address = pharmacy.address,
                    district = pharmacy.district,
                    city = pharmacy.city
                )
            },
            onFavoriteClicked = { pharmacy ->
                viewModel.toggleFavorite(pharmacy)
            }
        )

        pharmacyRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = pharmacyAdapter
            setHasFixedSize(false)
            itemAnimator = null
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
        val inputCity = cityEditText.text
            ?.toString()
            .orEmpty()

        val matchedCity = cityProvider.findValidCity(inputCity)

        if (matchedCity == null) {
            cityInputLayout.error = getString(R.string.error_invalid_city)
            return
        }

        cityInputLayout.error = null

        cityEditText.dismissDropDown()
        cityEditText.clearFocus()
        districtAutoCompleteTextView.clearFocus()

        shouldScroll = true
        viewModel.getPharmacies(matchedCity)
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    renderUiState(state)
                }
            }
        }
    }

    private fun observeLastCity() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.lastCityFlow.collect { city ->
                    if (city.isBlank()) {
                        return@collect
                    }

                    if (cityEditText.text.isNullOrBlank()) {
                        cityEditText.setText(city, false)
                    }

                    val didStartAutoLoad = viewModel.loadLastCityIfNeeded(city)

                    if (didStartAutoLoad) {
                        shouldScroll = true
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
                showError(state.message.asString(requireContext()))
            }
        }
    }

    private fun showIdle() {
        resultSummaryCardView.visibility = View.GONE
        hideLoadingIndicator()
        hideRecyclerView()
        hideStatusText()
        hideDutyDateInfo()
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
        resultSummaryCardView.visibility = View.GONE
        showLoadingIndicator()
        hideRecyclerView()
        hideStatusText()
        hideDutyDateInfo()
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
        resultSummaryCardView.visibility = View.VISIBLE
        stateCardView.visibility = View.GONE
        hideContentState()
        resetSearchButton()
        showDistrictFilters(state)

        val locationText = buildLocationText(
            city = state.city,
            district = state.selectedDistrict
        )

        if (state.pharmacies.isEmpty()) {
            shouldScroll = false
            clearPharmacyList()

            showOnlyStateCard(
                title = getString(R.string.state_empty_title),
                message = getString(
                    R.string.state_empty_message,
                    locationText
                ),
                showRetry = true
            )

            return
        }

        hideStateCard()
        showRecyclerView()

        resultLocationTextView.text = locationText

        showStatusText(
            getString(
                R.string.status_found_pharmacies_compact,
                state.pharmacies.size
            )
        )

        showDutyDateInfo(
            dutyDate = state.dutyDate
        )

        pharmacyAdapter.submitList(state.pharmacies) {
            if (shouldScroll) {
                pharmacyRecyclerView.scrollToPosition(0)
                shouldScroll = false
            }
        }
    }

    private fun showError(message: String) {
        resultSummaryCardView.visibility = View.GONE
        hideLoadingIndicator()
        hideRecyclerView()
        hideStatusText()
        hideDutyDateInfo()
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
        searchButton.text = getString(R.string.button_search_short)
    }

    private fun setSearchButtonLoading() {
        searchButton.isEnabled = false
        searchButton.text = getString(R.string.button_loading_short)
    }

    private fun clearPharmacyList() {
        pharmacyAdapter.submitList(emptyList())
    }

    private fun hideDistrictFilters() {
        districtInputLayout.visibility = View.GONE
        districtAutoCompleteTextView.setAdapter(null)
        districtAutoCompleteTextView.setText("", false)
    }

    private fun showDistrictFilters(state: PharmacyUiState.Success) {
        if (state.districtOptions.isEmpty()) {
            hideDistrictFilters()
            return
        }

        districtInputLayout.visibility = View.VISIBLE

        val allDistrictsText = getString(R.string.district_filter_all)

        val districtItems = listOf(allDistrictsText) + state.districtOptions

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_1,
            districtItems
        )

        isUpdatingDistrictDropdown = true

        districtAutoCompleteTextView.setAdapter(adapter)

        val selectedText = state.selectedDistrict ?: allDistrictsText
        districtAutoCompleteTextView.setText(selectedText, false)

        isUpdatingDistrictDropdown = false
    }

    private fun showOnlyStateCard(title: String, message: String, showRetry: Boolean) {
        resultSummaryCardView.visibility = View.GONE
        hideRecyclerView()
        hideStatusText()
        hideDutyDateInfo()

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

    private fun showDutyDateInfo(dutyDate: String) {
        if (dutyDate.isBlank()) {
            hideDutyDateInfo()
            return
        }

        dutyDateTextView.visibility = View.VISIBLE
        dutyDateTextView.text = formatDutyPeriod(dutyDate)
    }

    private fun formatDutyPeriod(dutyDate: String): String {
        return try {
            val locale = getCurrentLocale()

            val inputFormatter = SimpleDateFormat(
                "yyyy-MM-dd",
                Locale.US
            ).apply {
                isLenient = false
            }

            val outputFormatter = SimpleDateFormat(
                "d MMMM EEEE",
                locale
            )

            val startDate = requireNotNull(
                inputFormatter.parse(dutyDate)
            )

            val calendar = Calendar.getInstance().apply {
                time = startDate
            }

            val formattedStartDate = outputFormatter.format(
                calendar.time
            )

            calendar.add(Calendar.DAY_OF_MONTH, 1)

            val formattedEndDate = outputFormatter.format(
                calendar.time
            )

            getString(
                R.string.duty_period_value,
                formattedStartDate,
                formattedEndDate
            )
        } catch (_: Exception) {
            dutyDate
        }
    }

    private fun getCurrentLocale(): Locale {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            resources.configuration.locales[0]
        } else {
            @Suppress("DEPRECATION")
            resources.configuration.locale
        }
    }

    private fun hideDutyDateInfo() {
        dutyDateTextView.visibility = View.GONE
    }
}
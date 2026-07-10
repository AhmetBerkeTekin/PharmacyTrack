package com.example.pharmacytrack.ui.nearby

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pharmacytrack.R
import com.example.pharmacytrack.ui.pharmacy.PharmacyAdapter
import com.example.pharmacytrack.util.PharmacyActionHelper
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class NearbyPharmacyFragment :
    Fragment(R.layout.fragment_nearby_pharmacy) {

    private val viewModel: NearbyPharmacyViewModel by viewModels()

    private val fusedLocationClient by lazy {
        LocationServices.getFusedLocationProviderClient(
            requireActivity()
        )
    }

    private lateinit var pharmacyAdapter: PharmacyAdapter

    private lateinit var resultSummaryCardView: MaterialCardView
    private lateinit var statusTextView: TextView
    private lateinit var dutyDateTextView: TextView

    private lateinit var stateCardView: MaterialCardView
    private lateinit var stateProgressBar: ProgressBar
    private lateinit var stateTitleTextView: TextView
    private lateinit var stateMessageTextView: TextView
    private lateinit var stateActionButton: MaterialButton

    private lateinit var pharmacyRecyclerView: RecyclerView

    private var cancellationTokenSource: CancellationTokenSource? = null

    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->

            val isGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                        permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

            if (isGranted) {
                requestCurrentLocation()
                return@registerForActivityResult
            }

            val canRequestAgain =
                ActivityCompat.shouldShowRequestPermissionRationale(
                    requireActivity(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) ||
                        ActivityCompat.shouldShowRequestPermissionRationale(
                            requireActivity(),
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )

            viewModel.showPermissionRequired(
                openSettings = !canRequestAgain
            )
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bindViews(view)
        setupRecyclerView()
        setupStateAction()
        observeUiState()

        startNearbyFlow()
    }

    override fun onResume() {
        super.onResume()

        val currentState = viewModel.uiState.value

        if (currentState is NearbyPharmacyUiState.PermissionRequired &&
            currentState.openSettings && hasLocationPermission()) {
            requestCurrentLocation()
        }
    }

    override fun onDestroyView() {
        cancellationTokenSource?.cancel()
        cancellationTokenSource = null

        super.onDestroyView()
    }

    private fun bindViews(view: View) {
        resultSummaryCardView = view.findViewById(R.id.nearbyResultSummaryCardView)
        statusTextView = view.findViewById(R.id.nearbyStatusTextView)
        dutyDateTextView = view.findViewById(R.id.nearbyDutyDateTextView)
        stateCardView = view.findViewById(R.id.nearbyStateCardView)
        stateProgressBar = view.findViewById(R.id.nearbyStateProgressBar)
        stateTitleTextView = view.findViewById(R.id.nearbyStateTitleTextView)
        stateMessageTextView = view.findViewById(R.id.nearbyStateMessageTextView)
        stateActionButton = view.findViewById(R.id.nearbyStateActionButton)
        pharmacyRecyclerView = view.findViewById(R.id.nearbyPharmacyRecyclerView)
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
                    city = pharmacy.city,
                    latitude = pharmacy.latitude,
                    longitude = pharmacy.longitude
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

    private fun setupStateAction() {
        stateActionButton.setOnClickListener {
            when (val state = viewModel.uiState.value) {
                is NearbyPharmacyUiState.PermissionRequired -> {
                    if (state.openSettings) {
                        openApplicationSettings()
                    } else {
                        requestLocationPermission()
                    }
                }

                NearbyPharmacyUiState.Empty,
                is NearbyPharmacyUiState.Error -> {
                    startNearbyFlow(force = true)
                }

                else -> Unit
            }
        }
    }

    private fun startNearbyFlow(
        force: Boolean = false
    ) {
        val currentState = viewModel.uiState.value

        if (!force && (currentState is NearbyPharmacyUiState.Success ||
                            currentState is NearbyPharmacyUiState.Loading ||
                            currentState is NearbyPharmacyUiState.Locating))
        {
            return
        }

        if (hasLocationPermission()) {
            requestCurrentLocation()
        } else {
            requestLocationPermission()
        }
    }

    private fun requestLocationPermission() {
        viewModel.showPermissionRequired(
            openSettings = false
        )

        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    private fun hasLocationPermission(): Boolean {
        val fineGranted = ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        return fineGranted || coarseGranted
    }

    @SuppressLint("MissingPermission")
    private fun requestCurrentLocation() {
        if (!hasLocationPermission()) {
            requestLocationPermission()
            return
        }

        viewModel.showLocating()

        cancellationTokenSource?.cancel()

        val tokenSource = CancellationTokenSource()
        cancellationTokenSource = tokenSource

        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            tokenSource.token
        ).addOnSuccessListener { location ->

            if (location == null) {
                viewModel.showLocationUnavailable()
                return@addOnSuccessListener
            }

            viewModel.loadNearbyPharmacies(
                latitude = location.latitude,
                longitude = location.longitude
            )

        }.addOnFailureListener {
            viewModel.showLocationUnavailable()
        }
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

    private fun renderUiState(state: NearbyPharmacyUiState) {
        when (state) {
            NearbyPharmacyUiState.Idle -> {
                showLoadingState(
                    title = getString(R.string.nearby_preparing_title),
                    message = getString(R.string.nearby_preparing_message)
                )
            }

            NearbyPharmacyUiState.Locating -> {
                showLoadingState(
                    title = getString(R.string.nearby_locating_title),
                    message = getString(R.string.nearby_locating_message)
                )
            }

            NearbyPharmacyUiState.Loading -> {
                showLoadingState(
                    title = getString(R.string.nearby_loading_title),
                    message = getString(R.string.nearby_loading_message)
                )
            }

            is NearbyPharmacyUiState.PermissionRequired -> {
                showPermissionState(
                    openSettings = state.openSettings
                )
            }

            is NearbyPharmacyUiState.Success -> {
                showSuccess(state)
            }

            NearbyPharmacyUiState.Empty -> {
                showActionState(
                    title = getString(R.string.nearby_empty_title),
                    message = getString(R.string.nearby_empty_message),
                    actionText = getString(R.string.nearby_retry)
                )
            }

            is NearbyPharmacyUiState.Error -> {
                showActionState(
                    title = getString(R.string.nearby_error_title),
                    message = state.message.asString(requireContext()),
                    actionText = getString(R.string.nearby_retry)
                )
            }
        }
    }

    private fun showLoadingState(title: String, message: String) {
        resultSummaryCardView.visibility = View.GONE
        pharmacyRecyclerView.visibility = View.GONE
        stateCardView.visibility = View.VISIBLE
        stateProgressBar.visibility = View.VISIBLE
        stateActionButton.visibility = View.GONE

        stateTitleTextView.text = title
        stateMessageTextView.text = message

        pharmacyAdapter.submitList(emptyList())
    }

    private fun showPermissionState(openSettings: Boolean) {
        resultSummaryCardView.visibility = View.GONE
        pharmacyRecyclerView.visibility = View.GONE
        stateCardView.visibility = View.VISIBLE
        stateProgressBar.visibility = View.GONE
        stateActionButton.visibility = View.VISIBLE

        stateTitleTextView.text =
            getString(R.string.nearby_permission_title)

        stateMessageTextView.text =
            getString(
                if (openSettings) {
                    R.string.nearby_permission_settings_message
                } else {
                    R.string.nearby_permission_message
                }
            )

        stateActionButton.text =
            getString(if (openSettings) {
                    R.string.nearby_open_settings
                } else {
                    R.string.nearby_give_permission
                }
            )

        pharmacyAdapter.submitList(emptyList())
    }

    private fun showSuccess(state: NearbyPharmacyUiState.Success) {
        stateCardView.visibility = View.GONE
        resultSummaryCardView.visibility = View.VISIBLE
        pharmacyRecyclerView.visibility = View.VISIBLE

        statusTextView.text =
            getString(R.string.nearby_found_pharmacies,
                state.pharmacies.size
            )

        if (state.dutyDate.isBlank()) {
            dutyDateTextView.visibility = View.GONE
        } else {
            dutyDateTextView.visibility = View.VISIBLE
            dutyDateTextView.text = state.dutyDate
        }

        pharmacyAdapter.submitList(state.pharmacies) {
            pharmacyRecyclerView.scrollToPosition(0)
        }
    }

    private fun showActionState(
        title: String,
        message: String,
        actionText: String
    ) {
        resultSummaryCardView.visibility = View.GONE
        pharmacyRecyclerView.visibility = View.GONE
        stateCardView.visibility = View.VISIBLE
        stateProgressBar.visibility = View.GONE
        stateActionButton.visibility = View.VISIBLE

        stateTitleTextView.text = title
        stateMessageTextView.text = message
        stateActionButton.text = actionText

        pharmacyAdapter.submitList(emptyList())
    }

    private fun openApplicationSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts(
                "package",
                requireContext().packageName,
                null
            )
        }

        startActivity(intent)
    }
}
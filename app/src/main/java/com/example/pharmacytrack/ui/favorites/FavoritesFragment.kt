package com.example.pharmacytrack.ui.favorites

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pharmacytrack.R
import com.example.pharmacytrack.ui.pharmacy.PharmacyAdapter
import com.example.pharmacytrack.ui.pharmacy.PharmacyUiModel
import com.example.pharmacytrack.util.PharmacyActionHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FavoritesFragment : Fragment(R.layout.fragment_favorites) {

    private val viewModel: FavoritesViewModel by viewModels()

    private lateinit var favoritesRecyclerView: RecyclerView
    private lateinit var emptyFavoritesLayout: View
    private lateinit var pharmacyAdapter: PharmacyAdapter

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        bindViews(view)
        setupRecyclerView()
        observeUiState()
    }

    private fun bindViews(view: View) {
        favoritesRecyclerView = view.findViewById(R.id.favoritesRecyclerView)
        emptyFavoritesLayout = view.findViewById(R.id.emptyFavoritesLayout)
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

        favoritesRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = pharmacyAdapter
            setHasFixedSize(false)
            itemAnimator = null
            overScrollMode = View.OVER_SCROLL_NEVER
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

    private fun renderUiState(state: FavoritesUiState) {
        when (state) {
            FavoritesUiState.Empty -> showEmptyState()

            is FavoritesUiState.Success -> showFavorites(
                pharmacies = state.pharmacies
            )
        }
    }

    private fun showEmptyState() {
        favoritesRecyclerView.visibility = View.GONE
        emptyFavoritesLayout.visibility = View.VISIBLE
        pharmacyAdapter.submitList(emptyList())
    }

    private fun showFavorites(pharmacies: List<PharmacyUiModel>) {
        if (pharmacies.isEmpty()) {
            showEmptyState()
            return
        }

        emptyFavoritesLayout.visibility = View.GONE
        favoritesRecyclerView.visibility = View.VISIBLE

        pharmacyAdapter.submitList(pharmacies)
    }
}
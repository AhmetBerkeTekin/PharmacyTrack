package com.example.pharmacytrack.ui.pharmacy

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.pharmacytrack.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip

class PharmacyAdapter(
    private val onCallClicked: (PharmacyUiModel) -> Unit,
    private val onMapClicked: (PharmacyUiModel) -> Unit,
    private val onFavoriteClicked: (PharmacyUiModel) -> Unit
) : ListAdapter<PharmacyUiModel, PharmacyAdapter.PharmacyViewHolder>(
    PharmacyDiffCallback()
) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): PharmacyViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pharmacy, parent, false)

        return PharmacyViewHolder(
            itemView = view,
            onCallClicked = onCallClicked,
            onMapClicked = onMapClicked,
            onFavoriteClicked = onFavoriteClicked
        )
    }

    override fun onBindViewHolder(
        holder: PharmacyViewHolder,
        position: Int
    ) {
        holder.bind(getItem(position))
    }

    class PharmacyViewHolder(
        itemView: View,
        private val onCallClicked: (PharmacyUiModel) -> Unit,
        private val onMapClicked: (PharmacyUiModel) -> Unit,
        private val onFavoriteClicked: (PharmacyUiModel) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val pharmacyNameTextView: TextView =
            itemView.findViewById(R.id.pharmacyNameTextView)

        private val districtChip: Chip =
            itemView.findViewById(R.id.districtChip)

        private val favoriteButton: ImageButton =
            itemView.findViewById(R.id.favoriteButton)

        private val addressTextView: TextView =
            itemView.findViewById(R.id.addressTextView)

        private val phoneTextView: TextView =
            itemView.findViewById(R.id.phoneTextView)

        private val callButton: MaterialButton =
            itemView.findViewById(R.id.callButton)

        private val mapButton: MaterialButton =
            itemView.findViewById(R.id.mapButton)

        fun bind(pharmacy: PharmacyUiModel) {
            val context = itemView.context

            pharmacyNameTextView.text = pharmacy.name.ifBlank {
                context.getString(R.string.pharmacy_unknown_name)
            }

            districtChip.text = pharmacy.district.ifBlank {
                context.getString(R.string.pharmacy_unknown_district)
            }

            addressTextView.text = pharmacy.address.ifBlank {
                context.getString(R.string.pharmacy_unknown_address)
            }

            phoneTextView.text = pharmacy.phone.ifBlank {
                context.getString(R.string.pharmacy_unknown_phone)
            }

            val hasPhone = pharmacy.phone.isNotBlank()
            val hasAddress = pharmacy.address.isNotBlank()

            callButton.isEnabled = hasPhone
            callButton.alpha = if (hasPhone) 1f else 0.5f

            mapButton.isEnabled = hasAddress
            mapButton.alpha = if (hasAddress) 1f else 0.5f

            favoriteButton.setImageResource(
                if (pharmacy.isFavorite) {
                    R.drawable.ic_star_filled_24
                } else {
                    R.drawable.ic_star_border_24
                }
            )

            favoriteButton.contentDescription = context.getString(
                if (pharmacy.isFavorite) {
                    R.string.favorite_remove
                } else {
                    R.string.favorite_add
                }
            )

            favoriteButton.setOnClickListener {
                onFavoriteClicked(pharmacy)
            }

            callButton.setOnClickListener {
                onCallClicked(pharmacy)
            }

            mapButton.setOnClickListener {
                onMapClicked(pharmacy)
            }
        }
    }
}

private class PharmacyDiffCallback : DiffUtil.ItemCallback<PharmacyUiModel>() {

    override fun areItemsTheSame(oldItem: PharmacyUiModel, newItem: PharmacyUiModel): Boolean {
        return oldItem.favoriteKey == newItem.favoriteKey
    }

    override fun areContentsTheSame(oldItem: PharmacyUiModel, newItem: PharmacyUiModel): Boolean {
        return oldItem == newItem
    }
}
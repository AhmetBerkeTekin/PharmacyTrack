package com.example.pharmacytrack.ui.pharmacy

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.pharmacytrack.R
import com.example.pharmacytrack.data.model.Pharmacy
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip

class PharmacyAdapter(
    private val onCallClicked: (Pharmacy) -> Unit,
    private val onMapClicked: (Pharmacy) -> Unit
) : ListAdapter<Pharmacy, PharmacyAdapter.PharmacyViewHolder>(PharmacyDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PharmacyViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_pharmacy, parent, false)

        return PharmacyViewHolder(parent = this, itemView = view)
    }

    override fun onBindViewHolder(holder: PharmacyViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class PharmacyViewHolder(
        private val parent: PharmacyAdapter,
        itemView: android.view.View
    ) : RecyclerView.ViewHolder(itemView) {

        private val pharmacyNameTextView: TextView = itemView.findViewById(R.id.pharmacyNameTextView)
        private val districtChip: Chip = itemView.findViewById(R.id.districtChip)
        private val addressTextView: TextView = itemView.findViewById(R.id.addressTextView)
        private val phoneTextView: TextView = itemView.findViewById(R.id.phoneTextView)
        private val callButton: MaterialButton = itemView.findViewById(R.id.callButton)
        private val mapButton: MaterialButton = itemView.findViewById(R.id.mapButton)

        fun bind(pharmacy: Pharmacy) {
            val context = itemView.context

            val name = pharmacy.name.orEmpty()
            val district = pharmacy.district.orEmpty()
            val address = pharmacy.address.orEmpty()
            val phone = pharmacy.phone.orEmpty()

            pharmacyNameTextView.text = name.ifBlank {
                context.getString(R.string.pharmacy_unknown_name)
            }

            districtChip.text = district.ifBlank {
                context.getString(R.string.pharmacy_unknown_district)
            }

            addressTextView.text = address.ifBlank {
                context.getString(R.string.pharmacy_unknown_address)
            }

            phoneTextView.text = phone.ifBlank {
                context.getString(R.string.pharmacy_unknown_phone)
            }

            callButton.isEnabled = phone.isNotBlank()
            mapButton.isEnabled = address.isNotBlank()

            callButton.setOnClickListener {
                parent.onCallClicked(pharmacy)
            }

            mapButton.setOnClickListener {
                parent.onMapClicked(pharmacy)
            }
        }
    }

    private class PharmacyDiffCallback : DiffUtil.ItemCallback<Pharmacy>() {

        override fun areItemsTheSame(oldItem: Pharmacy, newItem: Pharmacy): Boolean {

            return oldItem.name.orEmpty() == newItem.name.orEmpty() && oldItem.address.orEmpty() == newItem.address.orEmpty()
        }

        override fun areContentsTheSame(oldItem: Pharmacy, newItem: Pharmacy): Boolean {

            return oldItem == newItem
        }
    }
}
package com.example.pharmacytrack.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.pharmacytrack.R
import com.example.pharmacytrack.data.model.Pharmacy
import androidx.core.net.toUri
import com.example.pharmacytrack.core.logger.Logger

object PharmacyActionHelper {

    private const val TAG = "PharmacyActionHelper"

    fun openDialer(
        context: Context,
        phone: String?
    ) {
        val cleanPhone = phone
            .orEmpty()
            .trim()
            .filter { it.isDigit() || it == '+' }

        if (cleanPhone.isBlank()) {
            showToast(context, R.string.error_phone_not_found)
            return
        }

        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = "tel:$cleanPhone".toUri()
        }

        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            showToast(context, R.string.error_phone_app_not_found)
        }
    }

    fun openMap(
        context: Context,
        pharmacy: Pharmacy
    ) {
        val query = buildString {
            append(pharmacy.name.orEmpty())

            if (pharmacy.address.orEmpty().isNotBlank()) {
                append(", ")
                append(pharmacy.address.orEmpty())
            }
        }.trim()

        if (query.isBlank()) {
            showToast(context, R.string.error_address_not_found)
            return
        }

        val geoIntent = Intent(Intent.ACTION_VIEW).apply {
            data = "geo:0,0?q=${Uri.encode(query)}".toUri()
        }

        val webIntent = Intent(Intent.ACTION_VIEW).apply {
            data = "https://www.google.com/maps/search/?api=1&query=${Uri.encode(query)}".toUri()
        }

        try {
            context.startActivity(geoIntent)
        } catch (e: ActivityNotFoundException) {
            Logger.W(TAG, "Geo map app not found! R: " + e.message)
            try {
                context.startActivity(webIntent)
            } catch (e: ActivityNotFoundException) {
                Logger.E(TAG, "Map fallback also failed! R: " + e.message)
                showToast(context, R.string.error_map_app_not_found)
            }
        }
    }

    private fun showToast(
        context: Context,
        messageResId: Int
    ) {
        Toast.makeText(
            context,
            context.getString(messageResId),
            Toast.LENGTH_SHORT
        ).show()
    }
}
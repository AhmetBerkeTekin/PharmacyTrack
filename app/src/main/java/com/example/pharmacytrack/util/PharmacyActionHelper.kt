package com.example.pharmacytrack.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.pharmacytrack.R
import androidx.core.net.toUri
import com.example.pharmacytrack.core.logger.Logger

object PharmacyActionHelper {

    private const val TAG = "PharmacyActionHelper"

    fun openDialer(context: Context, phone: String?) {
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
        name: String?,
        address: String?,
        district: String?,
        city: String?,
        latitude: Double? = null,
        longitude: Double? = null
    ) {
        val hasCoordinates = latitude != null && longitude != null

        val addressQuery = listOfNotNull(
            name?.takeIf { it.isNotBlank() },
            address?.takeIf { it.isNotBlank() },
            district?.takeIf { it.isNotBlank() },
            city?.takeIf { it.isNotBlank() }
        ).joinToString(" ")

        if (!hasCoordinates && addressQuery.isBlank()) {
            showToast(context, R.string.error_address_not_found)
            return
        }

        val mapUri = if (hasCoordinates) {
            val label = name.orEmpty().trim()

            if (label.isNotBlank()) {
                Uri.parse(
                    "geo:$latitude,$longitude" +
                            "?q=$latitude,$longitude(${Uri.encode(label)})"
                )
            } else {
                Uri.parse(
                    "geo:$latitude,$longitude?q=$latitude,$longitude"
                )
            }
        } else {
            Uri.parse(
                "geo:0,0?q=${Uri.encode(addressQuery)}"
            )
        }

        val mapIntent = Intent(
            Intent.ACTION_VIEW,
            mapUri
        )

        try {
            context.startActivity(mapIntent)
        } catch (e: ActivityNotFoundException) {
            Logger.E(
                TAG,
                "Map app not found. ${e.message}"
            )

            val webQuery = if (hasCoordinates) {
                "$latitude,$longitude"
            } else {
                addressQuery
            }

            val webIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse(
                    "https://www.google.com/maps/search/" +
                            "?api=1&query=${Uri.encode(webQuery)}"
                )
            )

            try {
                context.startActivity(webIntent)
            } catch (webException: ActivityNotFoundException) {
                Logger.E(
                    TAG,
                    "Browser app not found for map fallback! " +
                            webException.message
                )

                showToast(
                    context,
                    R.string.error_map_app_not_found
                )
            }
        }
    }

    private fun showToast(context: Context, messageResId: Int) {
        Toast.makeText(context, context.getString(messageResId), Toast.LENGTH_SHORT).show()
    }
}
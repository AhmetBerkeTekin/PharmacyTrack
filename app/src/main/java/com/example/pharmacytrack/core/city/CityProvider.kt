package com.example.pharmacytrack.core.city

import android.content.Context
import com.example.pharmacytrack.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CityProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {

    val cities: List<String> by lazy {
        context.resources
            .getStringArray(R.array.turkey_cities)
            .toList()
    }

    fun findValidCity(input: String): String? {
        val normalizedInput = input.trim()

        if (normalizedInput.isBlank()) {
            return null
        }

        return cities.firstOrNull { city ->
            city.equals(normalizedInput, ignoreCase = true)
        }
    }
}
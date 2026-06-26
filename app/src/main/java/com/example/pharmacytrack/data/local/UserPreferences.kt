package com.example.pharmacytrack.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

private val Context.userDataStore by preferencesDataStore(
    name = "user_preferences"
)

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private object Keys {
        val LAST_CITY = stringPreferencesKey("last_city")
    }

    val lastCityFlow: Flow<String> =
        context.userDataStore.data.map { preferences ->
            preferences[Keys.LAST_CITY].orEmpty()
        }

    suspend fun saveLastCity(city: String) {
        context.userDataStore.edit { preferences ->
            preferences[Keys.LAST_CITY] = city
        }
    }

    suspend fun getLastDistrictForCity(city: String): String? {
        val key = getLastDistrictKey(city)

        return context.userDataStore.data
            .map { preferences ->
                preferences[key]
            }
            .first()
            ?.takeIf { it.isNotBlank() }
    }

    suspend fun saveLastDistrictForCity(city: String, district: String?) {
        val key = getLastDistrictKey(city)

        context.userDataStore.edit { preferences ->
            preferences[key] = district.orEmpty()
        }
    }

    private fun getLastDistrictKey(city: String) =
        stringPreferencesKey(
            "last_district_${city.toPreferenceKey()}"
        )

    private fun String.toPreferenceKey(): String {
        return trim()
            .lowercase(Locale.ROOT)
            .replace(" ", "_")
    }
}
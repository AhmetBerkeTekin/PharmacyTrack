package com.example.pharmacytrack.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.pharmacytrack.core.logger.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.favoriteDataStore by preferencesDataStore(
    name = "favorite_preferences"
)

@Singleton
class FavoriteStore @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG = "FavoriteStore"
    }

    private object Keys {
        val FAVORITE_PHARMACY_KEYS = stringSetPreferencesKey("favorite_pharmacy_keys")
    }

    val favoriteKeysFlow: Flow<Set<String>> =
        context.favoriteDataStore.data.map { preferences ->
            preferences[Keys.FAVORITE_PHARMACY_KEYS].orEmpty()
        }

    suspend fun toggleFavorite(key: String) {
        if (key.isBlank()) {
            Logger.W(TAG, "Favorite key is blank. Toggle ignored.")
            return
        }

        context.favoriteDataStore.edit { preferences ->
            val currentFavorites = preferences[Keys.FAVORITE_PHARMACY_KEYS].orEmpty()

            val updatedFavorites = if (key in currentFavorites) {
                Logger.I(TAG, "Favorite removed. key=$key")
                currentFavorites - key
            } else {
                Logger.I(TAG, "Favorite added. key=$key")
                currentFavorites + key
            }

            preferences[Keys.FAVORITE_PHARMACY_KEYS] = updatedFavorites
        }
    }

    suspend fun isFavorite(key: String): Boolean {
        return false
    }
}
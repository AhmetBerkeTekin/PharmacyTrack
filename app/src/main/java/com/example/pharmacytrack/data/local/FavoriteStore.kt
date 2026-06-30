package com.example.pharmacytrack.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.pharmacytrack.core.logger.Logger
import com.google.gson.Gson
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
        val FAVORITE_PHARMACIES = stringSetPreferencesKey("favorite_pharmacies")
    }

    private val gson = Gson()

    val favoritePharmaciesFlow: Flow<List<FavoritePharmacy>> =
        context.favoriteDataStore.data.map { preferences ->
            preferences[Keys.FAVORITE_PHARMACIES]
                .orEmpty()
                .mapNotNull { favoriteJson ->
                    parseFavoritePharmacy(favoriteJson)
                }
                .sortedBy { favoritePharmacy ->
                    favoritePharmacy.name
                }
        }

    val favoriteKeysFlow: Flow<Set<String>> =
        favoritePharmaciesFlow.map { favoritePharmacies ->
            favoritePharmacies
                .map { favoritePharmacy -> favoritePharmacy.favoriteKey }
                .toSet()
        }

    suspend fun toggleFavorite(favoritePharmacy: FavoritePharmacy) {
        if (favoritePharmacy.favoriteKey.isBlank()) {
            Logger.W(TAG, "Favorite key is blank. Toggle ignored.")
            return
        }

        context.favoriteDataStore.edit { preferences ->
            val currentFavorites = preferences[Keys.FAVORITE_PHARMACIES]
                .orEmpty()
                .mapNotNull { favoriteJson ->
                    parseFavoritePharmacy(favoriteJson)
                }

            val isAlreadyFavorite = currentFavorites.any { currentFavorite ->
                currentFavorite.favoriteKey == favoritePharmacy.favoriteKey
            }

            val updatedFavorites = if (isAlreadyFavorite) {
                Logger.I(TAG, "Favorite removed. key=${favoritePharmacy.favoriteKey}")

                currentFavorites.filterNot { currentFavorite ->
                    currentFavorite.favoriteKey == favoritePharmacy.favoriteKey
                }
            } else {
                Logger.I(TAG, "Favorite added. key=${favoritePharmacy.favoriteKey}")

                currentFavorites + favoritePharmacy
            }

            preferences[Keys.FAVORITE_PHARMACIES] = updatedFavorites
                .map { updatedFavorite ->
                    gson.toJson(updatedFavorite)
                }
                .toSet()
        }
    }

    private fun parseFavoritePharmacy(
        favoriteJson: String
    ): FavoritePharmacy? {
        return try {
            gson.fromJson(favoriteJson, FavoritePharmacy::class.java)
        } catch (e: Exception) {
            Logger.E(TAG, "Favorite pharmacy parse failed! R: " + e.message)
            null
        }
    }
}
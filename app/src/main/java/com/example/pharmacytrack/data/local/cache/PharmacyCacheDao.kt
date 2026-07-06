package com.example.pharmacytrack.data.local.cache

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
abstract class PharmacyCacheDao {

    @Query("""
        SELECT * FROM cached_pharmacies
        WHERE citySlug = :citySlug
        ORDER BY district COLLATE NOCASE, name COLLATE NOCASE
        """
    )
    abstract suspend fun getPharmaciesByCity(
        citySlug: String
    ): List<CachedPharmacyEntity>

    @Query(
        """
        DELETE FROM cached_pharmacies
        WHERE citySlug = :citySlug
        """
    )
    protected abstract suspend fun deleteByCity(
        citySlug: String
    )

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertAll(
        pharmacies: List<CachedPharmacyEntity>
    )

    @Transaction
    open suspend fun replaceCityCache(
        citySlug: String,
        pharmacies: List<CachedPharmacyEntity>
    ) {
        deleteByCity(citySlug)

        if (pharmacies.isNotEmpty()) {
            insertAll(pharmacies)
        }
    }
}
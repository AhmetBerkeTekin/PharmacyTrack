package com.example.pharmacytrack.data.local.cache

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        CachedPharmacyEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class PharmacyDatabase : RoomDatabase() {

    abstract fun pharmacyCacheDao(): PharmacyCacheDao
}
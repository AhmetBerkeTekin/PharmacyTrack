package com.example.pharmacytrack.data.local.cache

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "cached_pharmacies",
    indices = [
        Index(value = ["citySlug"])
    ]
)
data class CachedPharmacyEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val citySlug: String,
    val cityName: String,
    val dutyDate: String,
    val dutyDateLabel: String,
    val cachedAtMillis: Long,

    val district: String,
    val name: String,
    val address: String,
    val phone: String
)
package com.example.pharmacytrack.data.local.cache

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        CachedPharmacyEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class PharmacyDatabase : RoomDatabase() {

    abstract fun pharmacyCacheDao(): PharmacyCacheDao

    companion object {

        val MIGRATION_1_2 = object : Migration(1, 2) {

            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    ALTER TABLE cached_pharmacies
                    ADD COLUMN providerId INTEGER
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    ALTER TABLE cached_pharmacies
                    ADD COLUMN districtSlug TEXT NOT NULL DEFAULT ''
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    ALTER TABLE cached_pharmacies
                    ADD COLUMN directions TEXT NOT NULL DEFAULT ''
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    ALTER TABLE cached_pharmacies
                    ADD COLUMN latitude REAL
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    ALTER TABLE cached_pharmacies
                    ADD COLUMN longitude REAL
                    """.trimIndent()
                )
            }
        }
    }
}
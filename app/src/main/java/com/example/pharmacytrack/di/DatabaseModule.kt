package com.example.pharmacytrack.di

import android.content.Context
import androidx.room.Room
import com.example.pharmacytrack.data.local.cache.PharmacyCacheDao
import com.example.pharmacytrack.data.local.cache.PharmacyDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun providePharmacyDatabase(
        @ApplicationContext context: Context
    ): PharmacyDatabase {
        return Room.databaseBuilder(
            context,
            PharmacyDatabase::class.java,
            "pharmacy_track.db"
        ).build()
    }

    @Provides
    fun providePharmacyCacheDao(
        database: PharmacyDatabase
    ): PharmacyCacheDao {
        return database.pharmacyCacheDao()
    }
}
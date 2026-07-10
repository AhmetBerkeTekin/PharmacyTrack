package com.example.pharmacytrack.data.remote

import com.example.pharmacytrack.data.model.PharmacyResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface PharmacyApiService {

    @GET("pharmacies/{city}")
    suspend fun getPharmaciesByCity(
        @Path("city") city: String,
    ): Response<PharmacyResponse>

    @GET("pharmacies/nearby")
    suspend fun getNearbyPharmacies(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("radius") radius: Int = 10_000
    ): PharmacyResponse
}
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
        @Query("force_refresh") forceRefresh: Boolean = false
    ): Response<PharmacyResponse>
}
package com.example.pharmacytrack.data.remote

import com.example.pharmacytrack.data.model.PharmacyResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface PharmacyApiService {

    @GET("pharmacies/{city}")
    suspend fun getPharmaciesByCity(
        @Path("city") city: String,
    ): Response<PharmacyResponse>
}
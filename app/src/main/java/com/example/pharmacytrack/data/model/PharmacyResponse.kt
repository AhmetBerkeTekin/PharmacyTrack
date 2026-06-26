package com.example.pharmacytrack.data.model

import com.google.gson.annotations.SerializedName

data class PharmacyResponse(
    @SerializedName("city")
    val city: String?,

    @SerializedName("source")
    val source: String?,

    @SerializedName("pharmacies")
    val pharmacies: List<Pharmacy?>?
)
package com.example.pharmacytrack.data.model

import com.google.gson.annotations.SerializedName

data class Pharmacy(
    @SerializedName("district")
    val district: String?,

    @SerializedName("name")
    val name: String?,

    @SerializedName("address")
    val address: String?,

    @SerializedName("phone")
    val phone: String?
)
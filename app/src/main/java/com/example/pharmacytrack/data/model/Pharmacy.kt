package com.example.pharmacytrack.data.model

import com.google.gson.annotations.SerializedName

data class Pharmacy(
    @SerializedName("provider_id")
    val providerId: Long? = null,

    @SerializedName("city")
    val city: String? = null,

    @SerializedName("city_slug")
    val citySlug: String? = null,

    @SerializedName("district")
    val district: String?,

    @SerializedName("district_slug")
    val districtSlug: String? = null,

    @SerializedName("name")
    val name: String?,

    @SerializedName("address")
    val address: String?,

    @SerializedName("phone")
    val phone: String?,

    @SerializedName("directions")
    val directions: String? = null,

    @SerializedName("latitude")
    val latitude: Double? = null,

    @SerializedName("longitude")
    val longitude: Double? = null,

    @SerializedName("distance_meters")
    val distanceMeters: Int? = null
)
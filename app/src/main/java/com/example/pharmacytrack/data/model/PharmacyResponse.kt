package com.example.pharmacytrack.data.model

import com.google.gson.annotations.SerializedName

data class PharmacyResponse(
    @SerializedName("city")
    val city: String?,
    @SerializedName("source")
    val source: String?,
    @SerializedName(value = "checked_at", alternate = ["last_updated"])
    val checkedAt: String?,
    @SerializedName("duty_date")
    val dutyDate: String?,
    @SerializedName("duty_date_label")
    val dutyDateLabel: String?,
    @SerializedName("pharmacies")
    val pharmacies: List<Pharmacy?>?
)
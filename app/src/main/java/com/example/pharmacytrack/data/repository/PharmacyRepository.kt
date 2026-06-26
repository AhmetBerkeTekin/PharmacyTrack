package com.example.pharmacytrack.data.repository

import com.example.pharmacytrack.core.logger.Logger
import com.example.pharmacytrack.core.result.AppError
import com.example.pharmacytrack.core.result.AppResult
import com.example.pharmacytrack.data.model.PharmacyResponse
import com.example.pharmacytrack.data.remote.PharmacyApiService
import java.io.IOException
import javax.inject.Inject

class PharmacyRepository @Inject constructor(
    private val apiService: PharmacyApiService,
) {

    companion object {
        private const val TAG = "PharmacyRepository"
    }

    suspend fun getPharmacies(
        city: String
    ): AppResult<PharmacyResponse> {
        return try {
            val response = apiService.getPharmaciesByCity(city.trim())

            if (response.isSuccessful) {
                val body = response.body()

                if (body != null) {
                    AppResult.Success(body)
                } else {
                    AppResult.Error(AppError.EmptyResponse)
                }
            } else {
                AppResult.Error(
                    AppError.Http(
                        code = response.code(),
                        message = response.message()
                    )
                )
            }
        } catch (e: IOException) {
            Logger.E(TAG, "Unknown error while getting pharmacies. city=$city! R: " + e.message)
            AppResult.Error(AppError.Network)
        } catch (e: Exception) {
            Logger.E(TAG, "Unknown error while getting pharmacies. city=$city! R: " + e.message)
            AppResult.Error(AppError.Unknown(e))
        }
    }
}
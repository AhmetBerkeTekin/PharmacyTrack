package com.example.pharmacytrack.data.repository

import com.example.pharmacytrack.core.logger.Logger
import com.example.pharmacytrack.core.result.AppError
import com.example.pharmacytrack.core.result.AppResult
import com.example.pharmacytrack.data.model.PharmacyResponse
import com.example.pharmacytrack.data.remote.PharmacyApiService
import java.io.IOException
import java.net.SocketTimeoutException
import javax.inject.Inject

class PharmacyRepository @Inject constructor(
    private val apiService: PharmacyApiService
) {

    companion object {
        private const val TAG = "PharmacyRepository"
    }

    suspend fun getPharmacies(city: String): AppResult<PharmacyResponse> {
        return try {
            val response = apiService.getPharmaciesByCity(
                city = city.trim(),
            )

            if (response.isSuccessful) {
                val body = response.body()

                if (body != null) {
                    AppResult.Success(body)
                } else {
                    AppResult.Error(AppError.EmptyResponse)
                }
            } else {
                AppResult.Error(
                    mapHttpError(
                        code = response.code(),
                        message = response.message()
                    )
                )
            }
        } catch (exception: SocketTimeoutException) {
            Logger.E(
                TAG,
                "Timeout while getting pharmacies. city=$city R: ${exception.message}"
            )

            AppResult.Error(AppError.Timeout)
        } catch (exception: IOException) {
            Logger.E(
                TAG,
                "Network error while getting pharmacies. city=$city R: ${exception.message}"
            )

            AppResult.Error(AppError.Network)
        } catch (exception: Exception) {
            Logger.E(
                TAG,
                "Unknown error while getting pharmacies. city=$city R: ${exception.message}"
            )

            AppResult.Error(
                AppError.Unknown(exception)
            )
        }
    }

    private fun mapHttpError(
        code: Int,
        message: String?
    ): AppError {
        return when (code) {
            404 -> AppError.NotFound

            408,
            504 -> AppError.Timeout

            429 -> AppError.TooManyRequests

            in 500..599 -> AppError.Server

            else -> AppError.Http(
                code = code,
                message = message
            )
        }
    }
}
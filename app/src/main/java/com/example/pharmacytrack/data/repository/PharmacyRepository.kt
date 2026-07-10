package com.example.pharmacytrack.data.repository

import com.example.pharmacytrack.core.logger.Logger
import com.example.pharmacytrack.core.result.AppError
import com.example.pharmacytrack.core.result.AppResult
import com.example.pharmacytrack.data.local.cache.PharmacyCacheDao
import com.example.pharmacytrack.data.local.cache.toCacheEntities
import com.example.pharmacytrack.data.local.cache.toOfflineResponse
import com.example.pharmacytrack.data.model.PharmacyResponse
import com.example.pharmacytrack.data.remote.PharmacyApiService
import java.io.IOException
import java.net.SocketTimeoutException
import javax.inject.Inject

class PharmacyRepository @Inject constructor(
    private val apiService: PharmacyApiService,
    private val pharmacyCacheDao: PharmacyCacheDao
) {

    companion object {
        private const val TAG = "PharmacyRepository"
    }

    suspend fun getNearbyPharmacies(
        latitude: Double,
        longitude: Double,
        radius: Int = 10_000
    ): PharmacyResponse {
        return apiService.getNearbyPharmacies(
            latitude = latitude,
            longitude = longitude,
            radius = radius
        )
    }

    suspend fun getPharmacies(
        city: String
    ): AppResult<PharmacyResponse> {
        val normalizedCity = city.trim()

        return try {
            val response = apiService.getPharmaciesByCity(
                city = normalizedCity
            )

            if (response.isSuccessful) {
                val body = response.body()

                if (body == null) {
                    return getCachedDataOrError(
                        city = normalizedCity,
                        error = AppError.EmptyResponse
                    )
                }

                saveResponseToCache(
                    city = normalizedCity,
                    response = body
                )

                AppResult.Success(body)
            } else {
                getCachedDataOrError(
                    city = normalizedCity,
                    error = mapHttpError(
                        code = response.code(),
                        message = response.message()
                    )
                )
            }
        } catch (exception: SocketTimeoutException) {
            Logger.E(
                TAG,
                "Timeout while getting pharmacies. city=$normalizedCity R: ${exception.message}"
            )

            getCachedDataOrError(
                city = normalizedCity,
                error = AppError.Timeout
            )
        } catch (exception: IOException) {
            Logger.E(
                TAG,
                "Network error while getting pharmacies. city=$normalizedCity R: ${exception.message}"
            )

            getCachedDataOrError(
                city = normalizedCity,
                error = AppError.Network
            )
        } catch (exception: Exception) {
            Logger.E(
                TAG,
                "Unknown error while getting pharmacies. city=$normalizedCity R: ${exception.message}"
            )

            getCachedDataOrError(
                city = normalizedCity,
                error = AppError.Unknown(exception)
            )
        }
    }

    private suspend fun saveResponseToCache(
        city: String,
        response: PharmacyResponse
    ) {
        try {
            val cachedPharmacies = response.toCacheEntities(
                citySlug = city,
                cachedAtMillis = System.currentTimeMillis()
            )

            if (cachedPharmacies.isEmpty()) {
                return
            }

            pharmacyCacheDao.replaceCityCache(
                citySlug = city,
                pharmacies = cachedPharmacies
            )
        } catch (exception: Exception) {
            Logger.E(
                TAG,
                "Could not save pharmacy cache. city=$city R: ${exception.message}"
            )
        }
    }

    private suspend fun getCachedDataOrError(
        city: String,
        error: AppError
    ): AppResult<PharmacyResponse> {
        return try {
            val cachedPharmacies =
                pharmacyCacheDao.getPharmaciesByCity(city)

            val cachedResponse =
                cachedPharmacies.toOfflineResponse()

            if (cachedResponse != null) {
                AppResult.Success(cachedResponse)
            } else {
                AppResult.Error(error)
            }
        } catch (exception: Exception) {
            Logger.E(
                TAG,
                "Could not read pharmacy cache. city=$city R: ${exception.message}"
            )

            AppResult.Error(error)
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
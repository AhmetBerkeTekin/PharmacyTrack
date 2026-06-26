package com.example.pharmacytrack.core.result

sealed class AppError {

    data object Network : AppError()

    data object EmptyResponse : AppError()

    data class Http(
        val code: Int,
        val message: String?
    ) : AppError()

    data class Unknown(
        val throwable: Throwable?
    ) : AppError()
}
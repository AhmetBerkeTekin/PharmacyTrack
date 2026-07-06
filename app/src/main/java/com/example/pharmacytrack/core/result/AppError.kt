package com.example.pharmacytrack.core.result

sealed interface AppError {

    data object Network : AppError

    data object Timeout : AppError

    data object EmptyResponse : AppError

    data object NotFound : AppError

    data object TooManyRequests : AppError

    data object Server : AppError

    data class Http(
        val code: Int,
        val message: String?
    ) : AppError

    data class Unknown(
        val throwable: Throwable
    ) : AppError
}
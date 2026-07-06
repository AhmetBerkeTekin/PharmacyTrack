package com.example.pharmacytrack.core.result

import com.example.pharmacytrack.R
import com.example.pharmacytrack.core.ui.UiText

fun AppError.toUiText(): UiText {
    return when (this) {
        AppError.Network -> {
            UiText.StringResource(R.string.error_network)
        }

        AppError.Timeout -> {
            UiText.StringResource(R.string.error_timeout)
        }

        AppError.EmptyResponse -> {
            UiText.StringResource(R.string.error_empty_response)
        }

        AppError.NotFound -> {
            UiText.StringResource(R.string.error_not_found)
        }

        AppError.TooManyRequests -> {
            UiText.StringResource(R.string.error_too_many_requests)
        }

        AppError.Server -> {
            UiText.StringResource(R.string.error_server)
        }

        is AppError.Http -> {
            UiText.StringResource(
                resId = R.string.error_request_failed,
                args = listOf(code)
            )
        }

        is AppError.Unknown -> {
            UiText.StringResource(R.string.error_unknown)
        }
    }
}
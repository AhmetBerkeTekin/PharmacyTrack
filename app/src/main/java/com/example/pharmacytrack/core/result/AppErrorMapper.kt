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

        is AppError.Http -> {
            when (code) {
                404 -> UiText.StringResource(R.string.error_city_not_found)

                in 500..599 -> UiText.StringResource(R.string.error_server)

                else -> UiText.StringResource(
                    resId = R.string.error_request_failed,
                    args = listOf(code)
                )
            }
        }

        is AppError.Unknown -> {
            UiText.StringResource(R.string.error_unknown)
        }
    }
}
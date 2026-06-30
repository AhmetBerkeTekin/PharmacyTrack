package com.example.pharmacytrack.core.text

import java.util.Locale

private val TURKISH_LOCALE = Locale("tr", "TR")

fun String.toTurkishSearchKey(): String {
    return trim()
        .lowercase(TURKISH_LOCALE)
        .replace("ç", "c")
        .replace("ğ", "g")
        .replace("ı", "i")
        .replace("ö", "o")
        .replace("ş", "s")
        .replace("ü", "u")
        .replace(Regex("\\s+"), " ")
}

fun String.toApiCitySlug(): String {
    return toTurkishSearchKey()
        .replace(" ", "-")
}
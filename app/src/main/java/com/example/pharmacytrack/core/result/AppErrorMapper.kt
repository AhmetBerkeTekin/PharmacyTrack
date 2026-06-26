package com.example.pharmacytrack.core.result

fun AppError.toUserMessage(): String {
    return when (this) {
        AppError.Network -> {
            "Sunucuya ulaşılamadı. İnternet bağlantını veya servis adresini kontrol et."
        }

        AppError.EmptyResponse -> {
            "Sunucu boş cevap döndürdü. Lütfen tekrar dene."
        }

        is AppError.Http -> {
            when (code) {
                404 -> "Bu şehir için eczane bilgisi bulunamadı."
                in 500..599 -> "Sunucuda geçici bir sorun var. Lütfen tekrar dene."
                else -> "İstek tamamlanamadı. Hata kodu: $code"
            }
        }

        is AppError.Unknown -> {
            "Beklenmeyen bir hata oluştu. Lütfen tekrar dene."
        }
    }
}
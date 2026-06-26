package com.example.pharmacytrack.core.logger

import android.util.Log
import com.example.pharmacytrack.BuildConfig

object Logger {

    fun D(tag: String, message: String) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, message)
        }
    }

    fun I(tag: String, message: String) {
        if (BuildConfig.DEBUG) {
            Log.i(tag, message)
        }
    }

    fun W(tag: String, message: String) {
        if (BuildConfig.DEBUG) {
            Log.w(tag, message)
        }
    }

    fun E(
        tag: String,
        message: String, ) {
        if (BuildConfig.DEBUG) {
                Log.e(tag, message)
        }
    }
}
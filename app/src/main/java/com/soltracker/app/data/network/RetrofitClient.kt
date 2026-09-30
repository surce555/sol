package com.soltracker.app.data.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private const val DEFAULT_BASE_URL = "https://api.binance.com/"
    private var currentBaseUrl = DEFAULT_BASE_URL

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    @Volatile
    private var _apiService: BinanceApiService? = null

    val apiService: BinanceApiService
        get() = _apiService ?: synchronized(this) {
            _apiService ?: buildApiService(currentBaseUrl).also { _apiService = it }
        }

    fun setBaseUrl(newUrl: String?) {
        val normalized = if (newUrl.isNullOrBlank()) {
            DEFAULT_BASE_URL
        } else {
            val trimmed = newUrl.trim()
            if (trimmed.endsWith("/")) trimmed else "$trimmed/"
        }
        if (normalized != currentBaseUrl) {
            currentBaseUrl = normalized
            synchronized(this) {
                _apiService = buildApiService(currentBaseUrl)
            }
        }
    }

    private fun buildApiService(baseUrl: String): BinanceApiService {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BinanceApiService::class.java)
    }
}

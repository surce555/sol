package com.soltracker.app.data.network

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private const val DEFAULT_BASE_URL = "https://data-api.binance.vision/"
    private var currentBaseUrl = DEFAULT_BASE_URL

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val userAgentInterceptor = Interceptor { chain ->
        val request = chain.request().newBuilder()
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36")
            .build()
        chain.proceed(request)
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(userAgentInterceptor)
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

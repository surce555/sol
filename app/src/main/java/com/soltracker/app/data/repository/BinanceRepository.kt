package com.soltracker.app.data.repository

import com.google.gson.JsonArray
import com.soltracker.app.data.model.Kline
import com.soltracker.app.data.model.OrderBook
import com.soltracker.app.data.model.Ticker24h
import com.soltracker.app.data.model.TickerPrice
import com.soltracker.app.data.network.BinanceApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BinanceRepository(private val apiService: BinanceApiService) {

    suspend fun getCurrentPrice(): Result<TickerPrice> = withContext(Dispatchers.IO) {
        runCatching { apiService.getPrice() }
    }

    suspend fun get24hTicker(): Result<Ticker24h> = withContext(Dispatchers.IO) {
        runCatching { apiService.get24hTicker() }
    }

    suspend fun getDailyKlines(limit: Int = 90): Result<List<Kline>> = withContext(Dispatchers.IO) {
        runCatching {
            val json = apiService.getKlines(interval = "1d", limit = limit)
            parseKlines(json)
        }
    }

    suspend fun getMonthlyKlines(limit: Int = 24): Result<List<Kline>> = withContext(Dispatchers.IO) {
        runCatching {
            val json = apiService.getKlines(interval = "1M", limit = limit)
            parseKlines(json)
        }
    }

    suspend fun getHourlyKlines(limit: Int = 48): Result<List<Kline>> = withContext(Dispatchers.IO) {
        runCatching {
            val json = apiService.getKlines(interval = "1h", limit = limit)
            parseKlines(json)
        }
    }

    suspend fun get4hKlines(limit: Int = 60): Result<List<Kline>> = withContext(Dispatchers.IO) {
        runCatching {
            val json = apiService.getKlines(interval = "4h", limit = limit)
            parseKlines(json)
        }
    }

    suspend fun getWeeklyKlines(limit: Int = 52): Result<List<Kline>> = withContext(Dispatchers.IO) {
        runCatching {
            val json = apiService.getKlines(interval = "1w", limit = limit)
            parseKlines(json)
        }
    }

    suspend fun getOrderBook(): Result<OrderBook> = withContext(Dispatchers.IO) {
        runCatching { apiService.getOrderBook() }
    }

    private fun parseKlines(jsonArray: JsonArray): List<Kline> {
        return jsonArray.map { element ->
            val arr = element.asJsonArray
            Kline(
                openTime = arr[0].asLong,
                open = arr[1].asDouble,
                high = arr[2].asDouble,
                low = arr[3].asDouble,
                close = arr[4].asDouble,
                volume = arr[5].asDouble,
                closeTime = arr[6].asLong,
                quoteVolume = arr[7].asDouble,
                trades = arr[8].asLong
            )
        }
    }
}

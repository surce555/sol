package com.soltracker.app.data.network

import com.soltracker.app.data.model.OrderBook
import com.soltracker.app.data.model.Ticker24h
import com.soltracker.app.data.model.TickerPrice
import com.google.gson.JsonArray
import retrofit2.http.GET
import retrofit2.http.Query

interface BinanceApiService {

    /**
     * Get current price of a symbol
     */
    @GET("api/v3/ticker/price")
    suspend fun getPrice(
        @Query("symbol") symbol: String = "SOLUSDT"
    ): TickerPrice

    /**
     * Get 24hr ticker stats
     */
    @GET("api/v3/ticker/24hr")
    suspend fun get24hTicker(
        @Query("symbol") symbol: String = "SOLUSDT"
    ): Ticker24h

    /**
     * Get Kline / candlestick data
     * @param interval 1m, 3m, 5m, 15m, 30m, 1h, 2h, 4h, 6h, 8h, 12h, 1d, 3d, 1w, 1M
     */
    @GET("api/v3/klines")
    suspend fun getKlines(
        @Query("symbol") symbol: String = "SOLUSDT",
        @Query("interval") interval: String,
        @Query("limit") limit: Int = 100
    ): JsonArray

    /**
     * Get order book depth
     */
    @GET("api/v3/depth")
    suspend fun getOrderBook(
        @Query("symbol") symbol: String = "SOLUSDT",
        @Query("limit") limit: Int = 10
    ): OrderBook
}

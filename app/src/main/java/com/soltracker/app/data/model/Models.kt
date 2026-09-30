package com.soltracker.app.data.model

import com.google.gson.annotations.SerializedName

/**
 * Binance ticker price response
 */
data class TickerPrice(
    @SerializedName("symbol") val symbol: String,
    @SerializedName("price") val price: String
)

/**
 * Binance 24hr ticker statistics
 */
data class Ticker24h(
    @SerializedName("symbol") val symbol: String,
    @SerializedName("priceChange") val priceChange: String,
    @SerializedName("priceChangePercent") val priceChangePercent: String,
    @SerializedName("weightedAvgPrice") val weightedAvgPrice: String,
    @SerializedName("prevClosePrice") val prevClosePrice: String,
    @SerializedName("lastPrice") val lastPrice: String,
    @SerializedName("lastQty") val lastQty: String,
    @SerializedName("bidPrice") val bidPrice: String,
    @SerializedName("askPrice") val askPrice: String,
    @SerializedName("openPrice") val openPrice: String,
    @SerializedName("highPrice") val highPrice: String,
    @SerializedName("lowPrice") val lowPrice: String,
    @SerializedName("volume") val volume: String,
    @SerializedName("quoteVolume") val quoteVolume: String,
    @SerializedName("openTime") val openTime: Long,
    @SerializedName("closeTime") val closeTime: Long,
    @SerializedName("count") val count: Long
)

/**
 * Binance Kline / Candlestick data
 * Array format: [openTime, open, high, low, close, volume, closeTime, quoteAssetVolume, trades, takerBuyBaseVol, takerBuyQuoteVol, ignore]
 */
data class Kline(
    val openTime: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double,
    val closeTime: Long,
    val quoteVolume: Double,
    val trades: Long
)

/**
 * Binance Order Book depth
 */
data class OrderBook(
    @SerializedName("lastUpdateId") val lastUpdateId: Long,
    @SerializedName("bids") val bids: List<List<String>>,
    @SerializedName("asks") val asks: List<List<String>>
)

/**
 * Price alert model stored in Room DB
 */
data class PriceAlert(
    val id: Long = 0,
    val price: Double,
    val isAbove: Boolean, // true = alert when price goes above, false = below
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * WebSocket ticker stream message
 */
data class WsTickerMessage(
    @SerializedName("e") val eventType: String,
    @SerializedName("E") val eventTime: Long,
    @SerializedName("s") val symbol: String,
    @SerializedName("p") val priceChange: String,
    @SerializedName("P") val priceChangePercent: String,
    @SerializedName("w") val weightedAvgPrice: String,
    @SerializedName("c") val lastPrice: String,
    @SerializedName("Q") val lastQty: String,
    @SerializedName("b") val bestBidPrice: String,
    @SerializedName("a") val bestAskPrice: String,
    @SerializedName("o") val openPrice: String,
    @SerializedName("h") val highPrice: String,
    @SerializedName("l") val lowPrice: String,
    @SerializedName("v") val totalTradedBaseVol: String,
    @SerializedName("q") val totalTradedQuoteVol: String,
    @SerializedName("O") val statisticsOpenTime: Long,
    @SerializedName("C") val statisticsCloseTime: Long,
    @SerializedName("n") val totalTrades: Long
)

/**
 * UI state for price display
 */
data class PriceUiState(
    val currentPrice: Double = 0.0,
    val priceChange: Double = 0.0,
    val priceChangePercent: Double = 0.0,
    val highPrice: Double = 0.0,
    val lowPrice: Double = 0.0,
    val volume: Double = 0.0,
    val quoteVolume: Double = 0.0,
    val bidPrice: Double = 0.0,
    val askPrice: Double = 0.0,
    val isConnected: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null
)

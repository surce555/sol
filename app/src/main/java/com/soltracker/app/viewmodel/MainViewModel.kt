package com.soltracker.app.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.soltracker.app.data.db.AlertEntity
import com.soltracker.app.data.db.AppDatabase
import com.soltracker.app.data.model.Kline
import com.soltracker.app.data.model.OrderBook
import com.soltracker.app.data.model.PriceUiState
import com.soltracker.app.data.network.BinanceWebSocketClient
import com.soltracker.app.data.network.RetrofitClient
import com.soltracker.app.data.pref.AppPreferences
import com.soltracker.app.data.repository.BinanceRepository
import com.soltracker.app.notification.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

enum class KlineInterval(val label: String, val apiValue: String) {
    HOURLY("1H", "1h"),
    FOUR_HOUR("4H", "4h"),
    DAILY("日K", "1d"),
    WEEKLY("周K", "1w"),
    MONTHLY("月K", "1M")
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "MainViewModel"

    val prefs = AppPreferences(application)
    private val repository = BinanceRepository()
    private val webSocketClient = BinanceWebSocketClient()
    private val db = AppDatabase.getInstance(application)
    private val notificationHelper = NotificationHelper(application)

    private val _priceState = MutableStateFlow(PriceUiState())
    val priceState: StateFlow<PriceUiState> = _priceState.asStateFlow()

    private val _klines = MutableStateFlow<List<Kline>>(emptyList())
    val klines: StateFlow<List<Kline>> = _klines.asStateFlow()

    private val _selectedInterval = MutableStateFlow(KlineInterval.DAILY)
    val selectedInterval: StateFlow<KlineInterval> = _selectedInterval.asStateFlow()

    private val _orderBook = MutableStateFlow<OrderBook?>(null)
    val orderBook: StateFlow<OrderBook?> = _orderBook.asStateFlow()

    // 人民币汇率 (USDT -> CNY)
    private val _cnyRate = MutableStateFlow(prefs.cnyRate)
    val cnyRate: StateFlow<Double> = _cnyRate.asStateFlow()

    // K线光标悬停选中的点位
    private val _selectedKline = MutableStateFlow<Kline?>(null)
    val selectedKline: StateFlow<Kline?> = _selectedKline.asStateFlow()

    // 代理状态
    private val _isProxyEnabled = MutableStateFlow(prefs.isProxyEnabled)
    val isProxyEnabled: StateFlow<Boolean> = _isProxyEnabled.asStateFlow()

    private val _proxyUrl = MutableStateFlow(prefs.proxyUrl)
    val proxyUrl: StateFlow<String> = _proxyUrl.asStateFlow()

    val alerts = db.alertDao().getAllAlerts()

    private var klineRefreshJob: Job? = null

    init {
        applyProxySettings()
        startWebSocket()
        loadKlines(KlineInterval.DAILY)
        loadOrderBook()
        startPeriodicRefresh()
        fetchCnyExchangeRate()
    }

    private fun applyProxySettings() {
        val proxy = if (prefs.isProxyEnabled && prefs.proxyUrl.isNotBlank()) prefs.proxyUrl else null
        RetrofitClient.setBaseUrl(proxy)
        webSocketClient.setProxyUrl(proxy)
    }

    fun updateProxy(enabled: Boolean, url: String) {
        prefs.isProxyEnabled = enabled
        prefs.proxyUrl = url
        _isProxyEnabled.value = enabled
        _proxyUrl.value = url
        applyProxySettings()

        // 重新连接并刷新
        webSocketClient.disconnect()
        webSocketClient.connect()
        loadKlines(_selectedInterval.value)
        loadOrderBook()
    }

    fun setCnyRate(rate: Double) {
        prefs.cnyRate = rate
        _cnyRate.value = rate
    }

    fun selectKline(kline: Kline?) {
        _selectedKline.value = kline
    }

    private fun startWebSocket() {
        viewModelScope.launch {
            webSocketClient.connectionFlow.collect { connected ->
                _priceState.update { it.copy(isConnected = connected, isLoading = !connected) }
            }
        }
        viewModelScope.launch {
            webSocketClient.tickerFlow.collect { ticker ->
                val price = ticker.lastPrice.toDoubleOrNull() ?: return@collect
                val change = ticker.priceChange.toDoubleOrNull() ?: 0.0
                val changePercent = ticker.priceChangePercent.toDoubleOrNull() ?: 0.0
                _priceState.update {
                    it.copy(
                        currentPrice = price,
                        priceChange = change,
                        priceChangePercent = changePercent,
                        highPrice = ticker.highPrice.toDoubleOrNull() ?: it.highPrice,
                        lowPrice = ticker.lowPrice.toDoubleOrNull() ?: it.lowPrice,
                        volume = ticker.totalTradedBaseVol.toDoubleOrNull() ?: it.volume,
                        quoteVolume = ticker.totalTradedQuoteVol.toDoubleOrNull() ?: it.quoteVolume,
                        bidPrice = ticker.bestBidPrice.toDoubleOrNull() ?: it.bidPrice,
                        askPrice = ticker.bestAskPrice.toDoubleOrNull() ?: it.askPrice,
                        isLoading = false
                    )
                }
                checkPriceAlerts(price)
            }
        }
        webSocketClient.connect()
    }

    fun loadKlines(interval: KlineInterval) {
        _selectedInterval.value = interval
        _selectedKline.value = null
        klineRefreshJob?.cancel()
        klineRefreshJob = viewModelScope.launch {
            val result = when (interval) {
                KlineInterval.HOURLY -> repository.getHourlyKlines()
                KlineInterval.FOUR_HOUR -> repository.get4hKlines()
                KlineInterval.DAILY -> repository.getDailyKlines()
                KlineInterval.WEEKLY -> repository.getWeeklyKlines()
                KlineInterval.MONTHLY -> repository.getMonthlyKlines()
            }
            result.onSuccess { klines ->
                _klines.value = klines
            }.onFailure { e ->
                Log.e(TAG, "Failed to load klines: ${e.message}")
                _priceState.update { it.copy(error = "加载K线失败: ${e.message}") }
            }
        }
    }

    private fun loadOrderBook() {
        viewModelScope.launch {
            repository.getOrderBook().onSuccess { book ->
                _orderBook.value = book
            }
        }
    }

    private fun startPeriodicRefresh() {
        viewModelScope.launch {
            while (true) {
                delay(30_000) // Refresh order book every 30s
                loadOrderBook()
            }
        }
    }

    private fun fetchCnyExchangeRate() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val client = OkHttpClient.Builder()
                    .connectTimeout(5, TimeUnit.SECONDS)
                    .readTimeout(5, TimeUnit.SECONDS)
                    .build()
                val request = Request.Builder()
                    .url("https://open.er-api.com/v6/latest/USD")
                    .build()
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (body != null) {
                        val json = JSONObject(body)
                        val rates = json.optJSONObject("rates")
                        val cny = rates?.optDouble("CNY", 0.0) ?: 0.0
                        if (cny > 5.0 && cny < 15.0) {
                            setCnyRate(cny)
                            Log.d(TAG, "Fetched live USD/CNY rate: $cny")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not fetch dynamic exchange rate, using default: ${e.message}")
            }
        }
    }

    private suspend fun checkPriceAlerts(currentPrice: Double) {
        val enabledAlerts = db.alertDao().getEnabledAlerts()
        val rate = _cnyRate.value
        for (alert in enabledAlerts) {
            val triggered = if (alert.isAbove) {
                currentPrice >= alert.price
            } else {
                currentPrice <= alert.price
            }
            if (triggered) {
                val direction = if (alert.isAbove) "突破" else "跌破"
                val usdStr = String.format("%.2f", alert.price)
                val cnyStr = String.format("%.2f", alert.price * rate)
                val curUsd = String.format("%.2f", currentPrice)
                val curCny = String.format("%.2f", currentPrice * rate)
                notificationHelper.sendPriceAlert(
                    "SOL 价格提醒",
                    "SOL/USDT 已${direction} \$$usdStr (≈ ¥$cnyStr)，当前价格: \$$curUsd (≈ ¥$curCny)"
                )
                db.alertDao().setAlertEnabled(alert.id, false)
            }
        }
    }

    fun addAlert(price: Double, isAbove: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            db.alertDao().insertAlert(AlertEntity(price = price, isAbove = isAbove))
        }
    }

    fun deleteAlert(alert: AlertEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            db.alertDao().deleteAlert(alert)
        }
    }

    fun toggleAlert(alert: AlertEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            db.alertDao().setAlertEnabled(alert.id, !alert.isEnabled)
        }
    }

    fun clearError() {
        _priceState.update { it.copy(error = null) }
    }

    override fun onCleared() {
        super.onCleared()
        webSocketClient.disconnect()
    }
}

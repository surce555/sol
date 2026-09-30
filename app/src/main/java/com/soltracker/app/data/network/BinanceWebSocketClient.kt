package com.soltracker.app.data.network

import android.util.Log
import com.google.gson.Gson
import com.soltracker.app.data.model.WsTickerMessage
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

class BinanceWebSocketClient {

    private val TAG = "BinanceWS"
    private val WS_BASE = "wss://stream.binance.com:9443/ws"
    private val SYMBOL = "solusdt"

    private var webSocket: WebSocket? = null
    private val gson = Gson()

    private val _tickerChannel = Channel<WsTickerMessage>(Channel.CONFLATED)
    val tickerFlow: Flow<WsTickerMessage> = _tickerChannel.receiveAsFlow()

    private val _connectionChannel = Channel<Boolean>(Channel.CONFLATED)
    val connectionFlow: Flow<Boolean> = _connectionChannel.receiveAsFlow()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // No timeout for WebSocket
        .writeTimeout(10, TimeUnit.SECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    fun connect() {
        val request = Request.Builder()
            .url("$WS_BASE/${SYMBOL}@ticker")
            .build()

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {

            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket connected")
                _connectionChannel.trySend(true)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val ticker = gson.fromJson(text, WsTickerMessage::class.java)
                    _tickerChannel.trySend(ticker)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing message: ${e.message}")
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closing: $code $reason")
                _connectionChannel.trySend(false)
                webSocket.close(1000, null)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket failure: ${t.message}")
                _connectionChannel.trySend(false)
                // Auto-reconnect after 5 seconds
                Thread.sleep(5000)
                connect()
            }
        })
    }

    fun disconnect() {
        webSocket?.close(1000, "User disconnected")
        webSocket = null
    }
}

package com.soltracker.app.data.pref

import android.content.Context
import android.content.SharedPreferences

class AppPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("sol_tracker_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_PROXY_ENABLED = "key_proxy_enabled"
        private const val KEY_PROXY_URL = "key_proxy_url"
        private const val KEY_CNY_RATE = "key_cny_rate"
        const val DEFAULT_CNY_RATE = 7.25
    }

    var isProxyEnabled: Boolean
        get() = prefs.getBoolean(KEY_PROXY_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_PROXY_ENABLED, value).apply()

    var proxyUrl: String
        get() = prefs.getString(KEY_PROXY_URL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_PROXY_URL, value.trim()).apply()

    var cnyRate: Double
        get() = prefs.getFloat(KEY_CNY_RATE, DEFAULT_CNY_RATE.toFloat()).toDouble()
        set(value) = prefs.edit().putFloat(KEY_CNY_RATE, value.toFloat()).apply()
}

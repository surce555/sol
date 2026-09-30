package com.soltracker.app.ui.screen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soltracker.app.data.model.OrderBook
import com.soltracker.app.data.model.PriceUiState
import com.soltracker.app.ui.theme.BearRed
import com.soltracker.app.ui.theme.BullGreen
import com.soltracker.app.ui.theme.SolGreen
import com.soltracker.app.ui.theme.SolPurple
import com.soltracker.app.viewmodel.MainViewModel

@Composable
fun HomeScreen(viewModel: MainViewModel) {
    val priceState by viewModel.priceState.collectAsState()
    val orderBook by viewModel.orderBook.collectAsState()
    val cnyRate by viewModel.cnyRate.collectAsState()
    val isProxyEnabled by viewModel.isProxyEnabled.collectAsState()
    val proxyUrl by viewModel.proxyUrl.collectAsState()

    var showSettingsDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header with status & settings icon
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "SOL / USDT",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Solana · Binance",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Connection indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (priceState.isConnected) SolGreen else BearRed)
                    )
                    Text(
                        text = if (isProxyEnabled) "代理" else (if (priceState.isConnected) "直连" else "断开"),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (priceState.isConnected) SolGreen else BearRed
                    )
                }

                // Settings icon button
                IconButton(onClick = { showSettingsDialog = true }) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "加速设置",
                        tint = SolPurple
                    )
                }
            }
        }

        // Main price card with USD & CNY
        PriceCard(priceState, cnyRate)

        // Stats row with USD & CNY
        StatsRow(priceState, cnyRate)

        // Order book
        if (orderBook != null) {
            OrderBookCard(orderBook!!, cnyRate)
        }

        // Error card
        priceState.error?.let { error ->
            Card(
                colors = CardDefaults.cardColors(containerColor = BearRed.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = error,
                    modifier = Modifier.padding(12.dp),
                    color = BearRed
                )
            }
        }
    }

    if (showSettingsDialog) {
        ProxySettingsDialog(
            currentProxyEnabled = isProxyEnabled,
            currentProxyUrl = proxyUrl,
            currentCnyRate = cnyRate,
            onSave = { enabled, url, rate ->
                viewModel.setCnyRate(rate)
                viewModel.updateProxy(enabled, url)
                showSettingsDialog = false
            },
            onDismiss = { showSettingsDialog = false }
        )
    }
}

@Composable
fun PriceCard(state: PriceUiState, cnyRate: Double) {
    val isPositive = state.priceChangePercent >= 0
    val priceColor by animateColorAsState(
        targetValue = when {
            state.isLoading -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            isPositive -> BullGreen
            else -> BearRed
        },
        animationSpec = tween(500),
        label = "priceColor"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    color = SolPurple,
                    modifier = Modifier.size(40.dp)
                )
                Text("正在连接行情...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            } else {
                AnimatedContent(
                    targetState = state.currentPrice,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(200))
                    },
                    label = "price"
                ) { price ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "\$${formatPrice(price)}",
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 42.sp
                            ),
                            color = priceColor,
                            textAlign = TextAlign.Center
                        )
                        // 人民币换算显示
                        Text(
                            text = "≈ ¥${formatPrice(price * cnyRate)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = priceColor.copy(alpha = 0.85f),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Icon(
                        imageVector = if (isPositive) Icons.Filled.TrendingUp else Icons.Filled.TrendingDown,
                        contentDescription = null,
                        tint = priceColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "${if (isPositive) "+" else ""}${String.format("%.2f", state.priceChangePercent)}%",
                        style = MaterialTheme.typography.titleMedium,
                        color = priceColor,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "(${if (isPositive) "+" else ""}\$${String.format("%.2f", state.priceChange)})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = priceColor.copy(alpha = 0.75f)
                    )
                }
            }
        }
    }
}

@Composable
fun StatsRow(state: PriceUiState, cnyRate: Double) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatCard(
            modifier = Modifier.weight(1f),
            label = "24H 最高",
            usdValue = state.highPrice,
            cnyRate = cnyRate,
            valueColor = BullGreen
        )
        StatCard(
            modifier = Modifier.weight(1f),
            label = "24H 最低",
            usdValue = state.lowPrice,
            cnyRate = cnyRate,
            valueColor = BearRed
        )
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatCard(
            modifier = Modifier.weight(1f),
            label = "买一价",
            usdValue = state.bidPrice,
            cnyRate = cnyRate,
            valueColor = BullGreen
        )
        StatCard(
            modifier = Modifier.weight(1f),
            label = "卖一价",
            usdValue = state.askPrice,
            cnyRate = cnyRate,
            valueColor = BearRed
        )
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        TextStatCard(
            modifier = Modifier.weight(1f),
            label = "24H 成交量(SOL)",
            value = formatVolume(state.volume)
        )
        TextStatCard(
            modifier = Modifier.weight(1f),
            label = "24H 成交额(USDT)",
            value = formatVolume(state.quoteVolume)
        )
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    label: String,
    usdValue: Double,
    cnyRate: Double,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Text(
                text = "\$${formatPrice(usdValue)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
            Text(
                text = "≈ ¥${formatPrice(usdValue * cnyRate)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun TextStatCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}

@Composable
fun OrderBookCard(orderBook: OrderBook, cnyRate: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "深度盘口 (Top 5)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "汇率 1 USDT ≈ ¥${String.format("%.2f", cnyRate)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "买盘 (Bids)",
                        style = MaterialTheme.typography.labelSmall,
                        color = BullGreen,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    orderBook.bids.take(5).forEach { bid ->
                        val priceUsd = bid[0].toDoubleOrNull() ?: 0.0
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "\$${bid[0]}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = BullGreen,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "¥${String.format("%.1f", priceUsd * cnyRate)}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                            Text(
                                text = String.format("%.2f", bid[1].toDoubleOrNull() ?: 0.0),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "卖盘 (Asks)",
                        style = MaterialTheme.typography.labelSmall,
                        color = BearRed,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    orderBook.asks.take(5).forEach { ask ->
                        val priceUsd = ask[0].toDoubleOrNull() ?: 0.0
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "\$${ask[0]}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = BearRed,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "¥${String.format("%.1f", priceUsd * cnyRate)}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                            Text(
                                text = String.format("%.2f", ask[1].toDoubleOrNull() ?: 0.0),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 代理设置与人民币汇率设置弹窗
 */
@Composable
fun ProxySettingsDialog(
    currentProxyEnabled: Boolean,
    currentProxyUrl: String,
    currentCnyRate: Double,
    onSave: (Boolean, String, Double) -> Unit,
    onDismiss: () -> Unit
) {
    var enabled by remember { mutableStateOf(currentProxyEnabled) }
    var proxyInput by remember { mutableStateOf(currentProxyUrl) }
    var rateInput by remember { mutableStateOf(String.format("%.2f", currentCnyRate)) }
    var showCodeDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("⚙️ 代理加速与换算设置", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // 代理开关
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Cloudflare 代理加速", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            "国内免翻墙高速直连币安",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                    Switch(
                        checked = enabled,
                        onCheckedChange = { enabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = SolPurple, checkedTrackColor = SolPurple.copy(alpha = 0.4f))
                    )
                }

                if (enabled) {
                    OutlinedTextField(
                        value = proxyInput,
                        onValueChange = { proxyInput = it },
                        label = { Text("Worker 代理地址") },
                        placeholder = { Text("https://xxx.workers.dev") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = { showCodeDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = SolPurple.copy(alpha = 0.15f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("📋 查看并复制代码 (用于 Cloudflare)", color = SolPurple, style = MaterialTheme.typography.labelSmall)
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // 人民币汇率设置
                OutlinedTextField(
                    value = rateInput,
                    onValueChange = { rateInput = it },
                    label = { Text("USDT 兑 人民币汇率") },
                    placeholder = { Text("7.25") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val rate = rateInput.toDoubleOrNull() ?: currentCnyRate
                    onSave(enabled, proxyInput.trim(), rate)
                    Toast.makeText(context, "设置已保存并生效", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = SolPurple)
            ) {
                Text("保存生效")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )

    if (showCodeDialog) {
        CloudflareWorkerCodeDialog(onDismiss = { showCodeDialog = false })
    }
}

/**
 * 方便用户直接在手机上一键复制 Cloudflare Worker 代码的弹窗
 */
@Composable
fun CloudflareWorkerCodeDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val workerCode = """
const BINANCE_REST_HOST = 'api.binance.com';
const BINANCE_WS_HOST = 'stream.binance.com:9443';

export default {
  async fetch(request) {
    const url = new URL(request.url);
    if (request.method === 'OPTIONS') {
      return new Response(null, {
        headers: {
          'Access-Control-Allow-Origin': '*',
          'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
          'Access-Control-Allow-Headers': '*',
        },
      });
    }
    const upgradeHeader = request.headers.get('Upgrade');
    if (upgradeHeader && upgradeHeader.toLowerCase() === 'websocket') {
      const targetWsUrl = `https://${'$'}{BINANCE_WS_HOST}${'$'}{url.pathname}${'$'}{url.search}`;
      return fetch(targetWsUrl, { headers: request.headers });
    }
    const targetApiUrl = `https://${'$'}{BINANCE_REST_HOST}${'$'}{url.pathname}${'$'}{url.search}`;
    const newRequest = new Request(targetApiUrl, {
      method: request.method,
      headers: request.headers,
      body: request.body,
    });
    const response = await fetch(newRequest);
    const newResponse = new Response(response.body, response);
    newResponse.headers.set('Access-Control-Allow-Origin', '*');
    return newResponse;
  },
};
    """.trimIndent()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("⚡ Cloudflare Worker 部署代码") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "在 Cloudflare 创建 Worker，清空内容后粘贴此代码并部署：",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
                ) {
                    Text(
                        text = workerCode,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        modifier = Modifier
                            .padding(8.dp)
                            .verticalScroll(rememberScrollState())
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("CloudflareWorkerCode", workerCode))
                    Toast.makeText(context, "代码已复制到剪贴板！", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = SolPurple)
            ) {
                Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("一键复制代码")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

private fun formatPrice(price: Double): String {
    return if (price == 0.0) "--" else String.format("%.2f", price)
}

private fun formatVolume(volume: Double): String {
    return when {
        volume >= 1_000_000_000 -> String.format("%.2fB", volume / 1_000_000_000)
        volume >= 1_000_000 -> String.format("%.2fM", volume / 1_000_000)
        volume >= 1_000 -> String.format("%.2fK", volume / 1_000)
        else -> String.format("%.2f", volume)
    }
}

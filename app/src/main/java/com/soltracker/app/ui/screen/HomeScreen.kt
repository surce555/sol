package com.soltracker.app.ui.screen

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
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import java.text.NumberFormat
import java.util.Locale

@Composable
fun HomeScreen(viewModel: MainViewModel) {
    val priceState by viewModel.priceState.collectAsState()
    val orderBook by viewModel.orderBook.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "SOL / USDT",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    text = "Solana · Binance",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
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
                    text = if (priceState.isConnected) "实时" else "断开",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (priceState.isConnected) SolGreen else BearRed
                )
            }
        }

        // Main price card
        PriceCard(priceState)

        // Stats row
        StatsRow(priceState)

        // Order book
        if (orderBook != null) {
            OrderBookCard(orderBook!!)
        }

        // Error snackbar
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
}

@Composable
fun PriceCard(state: PriceUiState) {
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    color = SolPurple,
                    modifier = Modifier.size(40.dp)
                )
                Text("正在连接...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            } else {
                AnimatedContent(
                    targetState = state.currentPrice,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(200))
                    },
                    label = "price"
                ) { price ->
                    Text(
                        text = "\$${formatPrice(price)}",
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 42.sp
                        ),
                        color = priceColor,
                        textAlign = TextAlign.Center
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
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
                        text = "(${if (isPositive) "+" else ""}${String.format("%.4f", state.priceChange)})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = priceColor.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
fun StatsRow(state: PriceUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(
            modifier = Modifier.weight(1f),
            label = "24H 高",
            value = "\$${formatPrice(state.highPrice)}",
            valueColor = BullGreen
        )
        StatCard(
            modifier = Modifier.weight(1f),
            label = "24H 低",
            value = "\$${formatPrice(state.lowPrice)}",
            valueColor = BearRed
        )
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(
            modifier = Modifier.weight(1f),
            label = "买一价",
            value = "\$${formatPrice(state.bidPrice)}",
            valueColor = BullGreen
        )
        StatCard(
            modifier = Modifier.weight(1f),
            label = "卖一价",
            value = "\$${formatPrice(state.askPrice)}",
            valueColor = BearRed
        )
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(
            modifier = Modifier.weight(1f),
            label = "24H 量(SOL)",
            value = formatVolume(state.volume)
        )
        StatCard(
            modifier = Modifier.weight(1f),
            label = "24H 额(USDT)",
            value = formatVolume(state.quoteVolume)
        )
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
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
                color = valueColor
            )
        }
    }
}

@Composable
fun OrderBookCard(orderBook: OrderBook) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "深度盘口 (Top 5)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "买单 (Bids)",
                        style = MaterialTheme.typography.labelSmall,
                        color = BullGreen,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    orderBook.bids.take(5).forEach { bid ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = bid[0],
                                style = MaterialTheme.typography.bodySmall,
                                color = BullGreen
                            )
                            Text(
                                text = String.format("%.3f", bid[1].toDoubleOrNull() ?: 0.0),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "卖单 (Asks)",
                        style = MaterialTheme.typography.labelSmall,
                        color = BearRed,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    orderBook.asks.take(5).forEach { ask ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = ask[0],
                                style = MaterialTheme.typography.bodySmall,
                                color = BearRed
                            )
                            Text(
                                text = String.format("%.3f", ask[1].toDoubleOrNull() ?: 0.0),
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

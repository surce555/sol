package com.soltracker.app.ui.screen

import android.graphics.Color as AndroidColor
import android.graphics.Paint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.CandleStickChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.soltracker.app.data.model.Kline
import com.soltracker.app.ui.theme.BearRed
import com.soltracker.app.ui.theme.BullGreen
import com.soltracker.app.ui.theme.SolPurple
import com.soltracker.app.viewmodel.KlineInterval
import com.soltracker.app.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChartScreen(viewModel: MainViewModel) {
    val klines by viewModel.klines.collectAsState()
    val selectedInterval by viewModel.selectedInterval.collectAsState()
    val priceState by viewModel.priceState.collectAsState()
    val cnyRate by viewModel.cnyRate.collectAsState()
    val selectedKline by viewModel.selectedKline.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Price header with USD & CNY
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "SOL / USDT",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            "\$${String.format("%.2f", priceState.currentPrice)}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (priceState.priceChangePercent >= 0) BullGreen else BearRed
                        )
                        Text(
                            "≈ ¥${String.format("%.2f", priceState.currentPrice * cnyRate)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = (if (priceState.priceChangePercent >= 0) BullGreen else BearRed).copy(alpha = 0.85f),
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                }
                Text(
                    "${if (priceState.priceChangePercent >= 0) "+" else ""}${String.format("%.2f", priceState.priceChangePercent)}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (priceState.priceChangePercent >= 0) BullGreen else BearRed
                )
            }
        }

        // Interval selector tabs
        ScrollableTabRow(
            selectedTabIndex = KlineInterval.values().indexOf(selectedInterval),
            modifier = Modifier.padding(horizontal = 4.dp),
            edgePadding = 0.dp,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = SolPurple
        ) {
            KlineInterval.values().forEachIndexed { index, interval ->
                Tab(
                    selected = selectedInterval == interval,
                    onClick = { viewModel.loadKlines(interval) },
                    text = {
                        Text(
                            interval.label,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                )
            }
        }

        // 光标十字线选中点位信息条 (Cursor Point Details Panel)
        KlineDetailBar(
            kline = selectedKline ?: klines.lastOrNull(),
            isInspecting = selectedKline != null,
            interval = selectedInterval,
            cnyRate = cnyRate
        )

        // Candlestick chart
        if (klines.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = SolPurple)
            }
        } else {
            CandlestickChartView(
                klines = klines,
                interval = selectedInterval,
                onSelectKline = { viewModel.selectKline(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.62f)
                    .padding(horizontal = 2.dp)
            )
            VolumeBarChartView(
                klines = klines,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.22f)
                    .padding(horizontal = 2.dp)
            )
            // MA stats with USD & CNY
            if (klines.isNotEmpty()) {
                MAStatsCard(klines, cnyRate)
            }
        }
    }
}

/**
 * 跟着光标移动显示点位详情的面板 (OHLC + 双币种换算)
 */
@Composable
fun KlineDetailBar(
    kline: Kline?,
    isInspecting: Boolean,
    interval: KlineInterval,
    cnyRate: Double
) {
    if (kline == null) return

    val isUp = kline.close >= kline.open
    val color = if (isUp) BullGreen else BearRed
    val changePercent = if (kline.open > 0) ((kline.close - kline.open) / kline.open) * 100 else 0.0

    val dateFormat = remember(interval) {
        when (interval) {
            KlineInterval.HOURLY, KlineInterval.FOUR_HOUR -> SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
            KlineInterval.DAILY -> SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
            KlineInterval.WEEKLY, KlineInterval.MONTHLY -> SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isInspecting)
                MaterialTheme.colorScheme.surfaceVariant
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        if (isInspecting) "🎯 光标点位:" else "最新K线:",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isInspecting) SolPurple else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        dateFormat.format(Date(kline.openTime)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
                Text(
                    "${if (changePercent >= 0) "+" else ""}${String.format("%.2f", changePercent)}%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }

            // OHLC 行：开、高、低、收
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                PriceItem("开", kline.open, cnyRate)
                PriceItem("高", kline.high, cnyRate)
                PriceItem("低", kline.low, cnyRate)
                PriceItem("收", kline.close, cnyRate, highlightColor = color)
                Column(horizontalAlignment = Alignment.End) {
                    Text("量", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                    Text(
                        formatShortVolume(kline.volume),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun PriceItem(label: String, usdPrice: Double, cnyRate: Double, highlightColor: Color? = null) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
        Text(
            "\$${String.format("%.2f", usdPrice)}",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            fontWeight = FontWeight.SemiBold,
            color = highlightColor ?: MaterialTheme.colorScheme.onSurface
        )
        Text(
            "¥${String.format("%.1f", usdPrice * cnyRate)}",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
        )
    }
}

@Composable
fun CandlestickChartView(
    klines: List<Kline>,
    interval: KlineInterval,
    onSelectKline: (Kline?) -> Unit,
    modifier: Modifier = Modifier
) {
    val bullColor = BullGreen.toArgb()
    val bearColor = BearRed.toArgb()
    val chartGridColor = AndroidColor.argb(35, 180, 180, 220)
    val chartTextColor = AndroidColor.argb(160, 200, 200, 240)
    val crosshairColor = AndroidColor.argb(220, 20, 241, 149) // Solana green crosshair

    val dateFormat = remember(interval) {
        when (interval) {
            KlineInterval.HOURLY, KlineInterval.FOUR_HOUR -> SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())
            KlineInterval.DAILY -> SimpleDateFormat("MM/dd", Locale.getDefault())
            KlineInterval.WEEKLY, KlineInterval.MONTHLY -> SimpleDateFormat("yyyy/MM", Locale.getDefault())
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            CandleStickChart(context).apply {
                description.isEnabled = false
                setBackgroundColor(AndroidColor.TRANSPARENT)
                setDrawGridBackground(false)
                setDrawBorders(false)
                setTouchEnabled(true)
                isDragEnabled = true
                setScaleEnabled(true)
                setPinchZoom(true)
                isAutoScaleMinMaxEnabled = true
                legend.isEnabled = false

                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    setDrawGridLines(true)
                    gridColor = chartGridColor
                    textColor = chartTextColor
                    textSize = 9f
                    labelRotationAngle = -25f
                    granularity = 1f
                }
                axisRight.apply {
                    setDrawGridLines(true)
                    gridColor = chartGridColor
                    textColor = chartTextColor
                    textSize = 9f
                }
                axisLeft.isEnabled = false

                // 监听光标十字线移动，实时更新点位数据
                setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                    override fun onValueSelected(e: Entry?, h: Highlight?) {
                        val index = e?.x?.toInt() ?: return
                        if (index in klines.indices) {
                            onSelectKline(klines[index])
                        }
                    }

                    override fun onNothingSelected() {
                        onSelectKline(null)
                    }
                })
            }
        },
        update = { chart ->
            val entries = klines.mapIndexed { i, k ->
                CandleEntry(
                    i.toFloat(),
                    k.high.toFloat(),
                    k.low.toFloat(),
                    k.open.toFloat(),
                    k.close.toFloat()
                )
            }
            val labels = klines.map { dateFormat.format(Date(it.openTime)) }

            val dataSet = CandleDataSet(entries, "SOL/USDT").apply {
                setDrawIcons(false)
                shadowColor = AndroidColor.LTGRAY
                shadowWidth = 0.8f
                decreasingColor = bearColor
                decreasingPaintStyle = Paint.Style.FILL
                increasingColor = bullColor
                increasingPaintStyle = Paint.Style.FILL
                neutralColor = bullColor
                setDrawValues(false)

                // 启用光标十字线指示器
                isHighlightEnabled = true
                highLightColor = crosshairColor
                enableDashedHighlightLine(10f, 5f, 0f)
            }

            chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            chart.data = CandleData(dataSet)
            chart.invalidate()
        }
    )
}

@Composable
fun VolumeBarChartView(
    klines: List<Kline>,
    modifier: Modifier = Modifier
) {
    val bullColor = BullGreen.copy(alpha = 0.7f).toArgb()
    val bearColor = BearRed.copy(alpha = 0.7f).toArgb()
    val chartGridColor = AndroidColor.argb(25, 180, 180, 220)
    val chartTextColor = AndroidColor.argb(120, 200, 200, 240)

    AndroidView(
        modifier = modifier,
        factory = { context ->
            BarChart(context).apply {
                description.isEnabled = false
                setBackgroundColor(AndroidColor.TRANSPARENT)
                setDrawGridBackground(false)
                setDrawBorders(false)
                setTouchEnabled(false)
                legend.isEnabled = false
                isAutoScaleMinMaxEnabled = true

                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    setDrawGridLines(false)
                    setDrawLabels(false)
                    textColor = chartTextColor
                }
                axisRight.apply {
                    setDrawGridLines(true)
                    gridColor = chartGridColor
                    textColor = chartTextColor
                    textSize = 8f
                    setLabelCount(3, true)
                    setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART)
                }
                axisLeft.isEnabled = false
            }
        },
        update = { chart ->
            val entries = klines.mapIndexed { i, k ->
                BarEntry(i.toFloat(), k.volume.toFloat())
            }
            val colors = klines.map { k -> if (k.close >= k.open) bullColor else bearColor }

            val dataSet = BarDataSet(entries, "Volume").apply {
                setColors(colors)
                setDrawValues(false)
            }
            chart.data = BarData(dataSet).apply {
                barWidth = 0.8f
            }
            chart.invalidate()
        }
    )
}

@Composable
fun MAStatsCard(klines: List<Kline>, cnyRate: Double) {
    val ma7 = calculateMA(klines, 7)
    val ma25 = calculateMA(klines, 25)
    val ma99 = calculateMA(klines, 99)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            MALabel("MA7", ma7, cnyRate, Color(0xFFE6B800))
            MALabel("MA25", ma25, cnyRate, Color(0xFF00B4D8))
            MALabel("MA99", ma99, cnyRate, Color(0xFFFF6B6B))
        }
    }
}

@Composable
fun MALabel(label: String, value: Double?, cnyRate: Double, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
        if (value != null) {
            Text(
                "\$${String.format("%.2f", value)} (¥${String.format("%.1f", value * cnyRate)})",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
        } else {
            Text("N/A", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
        }
    }
}

private fun calculateMA(klines: List<Kline>, period: Int): Double? {
    if (klines.size < period) return null
    return klines.takeLast(period).map { it.close }.average()
}

private fun formatShortVolume(volume: Double): String {
    return when {
        volume >= 1_000_000 -> String.format("%.2fM", volume / 1_000_000)
        volume >= 1_000 -> String.format("%.2fK", volume / 1_000)
        else -> String.format("%.1f", volume)
    }
}

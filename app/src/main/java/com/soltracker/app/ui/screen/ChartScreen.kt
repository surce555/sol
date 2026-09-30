package com.soltracker.app.ui.screen

import android.graphics.Color as AndroidColor
import android.graphics.Paint
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.CandleStickChart
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Price header
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "SOL/USDT",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        "\$${String.format("%.2f", priceState.currentPrice)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = if (priceState.priceChangePercent >= 0) BullGreen else BearRed
                    )
                }
                Text(
                    "${if (priceState.priceChangePercent >= 0) "+" else ""}${String.format("%.2f", priceState.priceChangePercent)}%",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (priceState.priceChangePercent >= 0) BullGreen else BearRed
                )
            }
        }

        // Interval selector tabs
        ScrollableTabRow(
            selectedTabIndex = KlineInterval.values().indexOf(selectedInterval),
            modifier = Modifier.padding(horizontal = 8.dp),
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
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.65f)
                    .padding(horizontal = 4.dp)
            )
            VolumeBarChartView(
                klines = klines,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.25f)
                    .padding(horizontal = 4.dp)
            )
            // MA stats
            if (klines.isNotEmpty()) {
                MAStatsCard(klines)
            }
        }
    }
}

@Composable
fun CandlestickChartView(
    klines: List<Kline>,
    interval: KlineInterval,
    modifier: Modifier = Modifier
) {
    val bullColor = BullGreen.toArgb()
    val bearColor = BearRed.toArgb()
    val chartGridColor = AndroidColor.argb(40, 180, 180, 220)
    val chartTextColor = AndroidColor.argb(160, 200, 200, 240)

    val dateFormat = when (interval) {
        KlineInterval.HOURLY, KlineInterval.FOUR_HOUR -> SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())
        KlineInterval.DAILY -> SimpleDateFormat("MM/dd", Locale.getDefault())
        KlineInterval.WEEKLY -> SimpleDateFormat("yyyy/MM", Locale.getDefault())
        KlineInterval.MONTHLY -> SimpleDateFormat("yyyy/MM", Locale.getDefault())
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
                    labelRotationAngle = -30f
                    granularity = 1f
                }
                axisRight.apply {
                    setDrawGridLines(true)
                    gridColor = chartGridColor
                    textColor = chartTextColor
                    textSize = 9f
                }
                axisLeft.isEnabled = false
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
                shadowWidth = 0.7f
                decreasingColor = bearColor
                decreasingPaintStyle = Paint.Style.FILL
                increasingColor = bullColor
                increasingPaintStyle = Paint.Style.FILL
                neutralColor = bullColor
                setDrawValues(false)
                highLightColor = AndroidColor.argb(100, 153, 69, 255)
            }

            chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            chart.data = CandleData(dataSet)
            chart.moveViewToX(entries.size.toFloat())
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
    val chartGridColor = AndroidColor.argb(30, 180, 180, 220)
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
fun MAStatsCard(klines: List<Kline>) {
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
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            MALabel("MA7", ma7, Color(0xFFE6B800))
            MALabel("MA25", ma25, Color(0xFF00B4D8))
            MALabel("MA99", ma99, Color(0xFFFF6B6B))
        }
    }
}

@Composable
fun MALabel(label: String, value: Double?, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = color)
        Text(
            value?.let { "\$${String.format("%.2f", it)}" } ?: "N/A",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )
    }
}

private fun calculateMA(klines: List<Kline>, period: Int): Double? {
    if (klines.size < period) return null
    return klines.takeLast(period).map { it.close }.average()
}

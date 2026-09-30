package com.soltracker.app.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.soltracker.app.data.db.AlertEntity
import com.soltracker.app.ui.theme.BearRed
import com.soltracker.app.ui.theme.BullGreen
import com.soltracker.app.ui.theme.SolPurple
import com.soltracker.app.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertScreen(viewModel: MainViewModel) {
    val alerts by viewModel.alerts.collectAsState(initial = emptyList())
    val priceState by viewModel.priceState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = SolPurple
            ) {
                Icon(Icons.Filled.Add, "添加提醒", tint = androidx.compose.ui.graphics.Color.White)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "价格提醒",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("SOL 当前价格", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "\$${String.format("%.2f", priceState.currentPrice)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (priceState.priceChangePercent >= 0) BullGreen else BearRed
                    )
                }
            }

            if (alerts.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("🔔", style = MaterialTheme.typography.displayMedium)
                        Text(
                            "暂无价格提醒",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Text(
                            "点击右下角 + 按钮添加提醒",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(alerts, key = { it.id }) { alert ->
                        AlertCard(
                            alert = alert,
                            onDelete = { viewModel.deleteAlert(it) },
                            onToggle = { viewModel.toggleAlert(it) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddAlertDialog(
            currentPrice = priceState.currentPrice,
            onConfirm = { price, isAbove ->
                viewModel.addAlert(price, isAbove)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }
}

@Composable
fun AlertCard(
    alert: AlertEntity,
    onDelete: (AlertEntity) -> Unit,
    onToggle: (AlertEntity) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()) }
    val directionColor = if (alert.isAbove) BullGreen else BearRed

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (alert.isEnabled)
                MaterialTheme.colorScheme.surfaceVariant
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = if (alert.isAbove) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
                    contentDescription = null,
                    tint = directionColor,
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        text = "${if (alert.isAbove) "突破" else "跌破"} \$${String.format("%.2f", alert.price)}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = if (alert.isEnabled) MaterialTheme.colorScheme.onSurface
                               else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                    Text(
                        text = "创建于 ${dateFormat.format(Date(alert.createdAt))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Switch(
                    checked = alert.isEnabled,
                    onCheckedChange = { onToggle(alert) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = SolPurple,
                        checkedTrackColor = SolPurple.copy(alpha = 0.4f)
                    )
                )
                IconButton(onClick = { onDelete(alert) }) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "删除",
                        tint = BearRed.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAlertDialog(
    currentPrice: Double,
    onConfirm: (Double, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var priceText by remember { mutableStateOf(String.format("%.2f", currentPrice)) }
    var isAbove by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加价格提醒") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = priceText,
                    onValueChange = {
                        priceText = it
                        error = null
                    },
                    label = { Text("目标价格 (USDT)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = error != null,
                    supportingText = error?.let { { Text(it, color = BearRed) } },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("提醒方向", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = isAbove,
                        onClick = { isAbove = true },
                        label = { Text("价格上涨突破 ↑") },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BullGreen.copy(alpha = 0.2f),
                            selectedLabelColor = BullGreen
                        )
                    )
                    FilterChip(
                        selected = !isAbove,
                        onClick = { isAbove = false },
                        label = { Text("价格下跌跌破 ↓") },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BearRed.copy(alpha = 0.2f),
                            selectedLabelColor = BearRed
                        )
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val price = priceText.toDoubleOrNull()
                if (price == null || price <= 0) {
                    error = "请输入有效价格"
                } else {
                    onConfirm(price, isAbove)
                }
            }) {
                Text("确认", color = SolPurple)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

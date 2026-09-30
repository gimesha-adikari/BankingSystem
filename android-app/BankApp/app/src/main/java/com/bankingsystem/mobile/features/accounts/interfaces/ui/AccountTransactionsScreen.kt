package com.bankingsystem.mobile.features.accounts.interfaces.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bankingsystem.mobile.features.accounts.domain.model.Transaction
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

@Composable
fun AccountTransactionsScreen(
    accountId: String,
    accountNumber: String?,
) {
    val vm: AccountTransactionsViewModel = hiltViewModel()
    LaunchedEffect(accountId) { vm.load(accountId) }

    val ui by vm.ui.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        GlassPanel {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Activity", style = MaterialTheme.typography.titleLarge, color = Color.White)
                TextButton(onClick = { vm.refresh() }, enabled = !ui.loading) {
                    Text(if (ui.loading) "Refreshing…" else "Refresh")
                }
            }
            Spacer(Modifier.height(8.dp))

            when {
                ui.loading -> repeat(5) { SkeletonRow() }
                ui.error != null -> Text(ui.error!!, color = MaterialTheme.colorScheme.error)
                ui.items.isEmpty() -> Text("No transactions yet.", color = Color.White.copy(0.9f))
                else -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(ui.items, key = { it.transactionId }) { tx -> TxRow(tx) }
                    }
                }
            }
        }
    }
}

/* ---- Rows & helpers ---- */

@Composable
private fun TxRow(tx: Transaction) {
    val (badgeText, badgeBg, amountColor) = when (tx.type) {
        "DEPOSIT"       -> Triple("Deposit",      Color(0xFF065F46), Color(0xFF34D399))
        "WITHDRAWAL"    -> Triple("Withdrawal",   Color(0xFF7F1D1D), Color(0xFFFCA5A5))
        "TRANSFER_IN"   -> Triple("Transfer In",  Color(0xFF0C4A6E), Color(0xFF93C5FD))
        "TRANSFER_OUT"  -> Triple("Transfer Out", Color(0xFF78350F), Color(0xFFFCD34D))
        else            -> Triple(tx.type,        Color(0xFF334155), Color(0xFFCBD5E1))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                @Suppress("DEPRECATION")
                Surface(
                    color = badgeBg.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(8.dp),
                    border = ButtonDefaults.outlinedButtonBorder
                ) {
                    Text(
                        badgeText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                Text(
                    humanDate(tx.createdAt),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(0.75f)
                )
            }
            if (!tx.description.isNullOrBlank()) {
                Text(
                    tx.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                formatAmount(tx.amount),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = amountColor
            )
            Text(
                "Bal ${formatAmount(tx.balanceAfter)}",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(0.7f)
            )
        }
    }
}

@Composable
private fun SkeletonRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Surface(color = Color.White.copy(0.08f), shape = RoundedCornerShape(8.dp)) {
            Spacer(Modifier.height(18.dp).width(180.dp))
        }
        Surface(color = Color.White.copy(0.08f), shape = RoundedCornerShape(8.dp)) {
            Spacer(Modifier.height(18.dp).width(90.dp))
        }
    }
}

private fun humanDate(value: String?): String {
    if (value.isNullOrBlank()) return "—"
    val parsed = runCatching {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.parse(value)
    }.getOrNull() ?: return value
    return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(parsed)
}

private fun formatAmount(v: Double): String =
    (if (v >= 0) "+ " else "− ") + "%,.2f".format(abs(v))

@Composable
private fun GlassPanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = cs.surface.copy(alpha = 0.10f),
        tonalElevation = 0.dp,
        shadowElevation = 12.dp
    ) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

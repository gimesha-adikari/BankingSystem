package com.bankingsystem.mobile.features.wallet.interfaces.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.SwapVerticalCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun WalletHomeScreen(
    modifier: Modifier = Modifier,
    state: WalletUiState,
    onOpenCards: () -> Unit,
    onAddCard: () -> Unit,
    onScanQr: () -> Unit,
    onReload: () -> Unit,
    onBiller: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // My Cards summary
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = cs.primary.copy(alpha = 0.28f)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("My Cards", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    val defaultCard = state.cards.firstOrNull { it.isDefault }
                    Text(
                        defaultCard?.let { "Default: ${it.brand} •••• ${it.last4}" } ?: "No default card",
                        color = Color.White.copy(0.9f)
                    )
                }
                Button(onClick = onOpenCards) { Text("Manage") }
            }
        }

        // Quick actions
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            QuickActionCard("Scan QR", Icons.Filled.QrCodeScanner, onScanQr, Modifier.weight(1f))
            QuickActionCard("Mobile Reload", Icons.Filled.SwapVerticalCircle, onReload, Modifier.weight(1f))
            QuickActionCard("Pay a Bill", Icons.Filled.ReceiptLong, onBiller, Modifier.weight(1f))
        }

        // Add card CTA
        ElevatedCard(shape = RoundedCornerShape(16.dp), colors = CardDefaults.elevatedCardColors(containerColor = cs.surface.copy(alpha = 0.22f))) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.CreditCard, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Add a card", color = Color.White, style = MaterialTheme.typography.titleMedium)
                    Text("Save a debit or credit card for faster checkout.", color = Color.White.copy(0.85f))
                }
                OutlinedButton(onClick = onAddCard) { Text("Add") }
            }
        }
    }
}

@Composable
private fun QuickActionCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    ElevatedCard(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = cs.surface.copy(alpha = 0.22f))
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.Start) {
            Icon(icon, contentDescription = null, tint = Color.White)
            Text(title, style = MaterialTheme.typography.titleMedium, color = Color.White)
            Text("Use your saved card to pay.", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.85f))
        }
    }
}

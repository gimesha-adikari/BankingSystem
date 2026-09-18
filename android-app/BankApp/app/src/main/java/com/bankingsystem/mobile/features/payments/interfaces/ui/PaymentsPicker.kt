package com.bankingsystem.mobile.features.payments.interfaces.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun PaymentsPicker(
    modifier: Modifier = Modifier,
    onTransfer: () -> Unit,
    onDeposit: () -> Unit,
    onWithdraw: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header card – a bit darker for contrast
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = cs.primary.copy(alpha = 0.28f),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Quick Payments",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Transfer, deposit, or withdraw using your linked accounts.",
                        color = Color.White.copy(0.9f)
                    )
                }
            }
        }

        // Action cards – force dark container + white content for readability
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ActionCard(
                title = "Transfer",
                icon = Icons.AutoMirrored.Filled.Send,
                onClick = onTransfer,
                modifier = Modifier.weight(1f)
            )
            ActionCard(
                title = "Deposit",
                icon = Icons.Filled.CallReceived,
                onClick = onDeposit,
                modifier = Modifier.weight(1f)
            )
            ActionCard(
                title = "Withdraw",
                icon = Icons.Filled.CallMade,
                onClick = onWithdraw,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ActionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    ElevatedCard(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = cs.surface.copy(alpha = 0.22f),
            contentColor = Color.White
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Icon(icon, contentDescription = null, tint = Color.White)
            Text(title, style = MaterialTheme.typography.titleMedium, color = Color.White)
            Text(
                "Send money, top up, or cash out.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(0.9f)
            )
        }
    }
}

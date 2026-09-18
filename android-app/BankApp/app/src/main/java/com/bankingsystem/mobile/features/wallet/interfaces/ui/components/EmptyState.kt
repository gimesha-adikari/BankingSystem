package com.bankingsystem.mobile.features.wallet.interfaces.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun EmptyState(
    title: String,
    subtitle: String,
    cta: String,
    onCta: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(Icons.Filled.CreditCard, contentDescription = null, tint = Color.White)
        Text(title, color = Color.White, style = MaterialTheme.typography.titleMedium)
        Text(subtitle, color = Color.White.copy(0.85f))
        Spacer(Modifier.height(8.dp))
        Button(onClick = onCta) { Text(cta) }
    }
}

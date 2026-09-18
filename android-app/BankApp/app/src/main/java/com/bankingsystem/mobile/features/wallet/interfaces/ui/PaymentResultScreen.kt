package com.bankingsystem.mobile.features.wallet.interfaces.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun PaymentResultScreen(
    modifier: Modifier = Modifier,
    onDone: () -> Unit
) {
    val vm = hiltViewModel<PaymentFlowViewModel>()
    val state by vm.state.collectAsState()

    val (icon, title) = when (state.stage) {
        PaymentStage.SUCCEEDED -> Icons.Rounded.CheckCircle to "Payment succeeded"
        PaymentStage.FAILED -> Icons.Rounded.Error to (state.error ?: state.intent?.description ?: "Payment failed")
        PaymentStage.CANCELED -> Icons.Rounded.Cancel to "Payment canceled"
        else -> Icons.Rounded.CheckCircle to ""
    }

    val cardColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f)

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        ElevatedCard(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = cardColor)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
                Text(title, style = MaterialTheme.typography.titleLarge)
                FilledTonalButton(onClick = onDone) { Text("Done") }
            }
        }
    }
}

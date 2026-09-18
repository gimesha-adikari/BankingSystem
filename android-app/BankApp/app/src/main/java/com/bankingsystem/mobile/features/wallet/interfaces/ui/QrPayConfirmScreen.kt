package com.bankingsystem.mobile.features.wallet.interfaces.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun QrPayConfirmScreen(
    modifier: Modifier = Modifier,
    state: WalletUiState,
    onChangeCard: () -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    val p = state.qrPreview
    val cardColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f)
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Confirm Payment", style = MaterialTheme.typography.headlineSmall)
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = androidx.compose.material3.CardDefaults.elevatedCardColors(containerColor = cardColor)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Merchant: ${p.merchantName}")
                Text("Amount: ${p.amount}")
                if (p.memo.isNotBlank()) Text("Memo: ${p.memo}")
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilledTonalButton(onClick = onConfirm) { Text("Pay") }
                    TextButton(onClick = onChangeCard) { Text("Change card") }
                    TextButton(onClick = onCancel) { Text("Cancel") }
                }
            }
        }
    }
}

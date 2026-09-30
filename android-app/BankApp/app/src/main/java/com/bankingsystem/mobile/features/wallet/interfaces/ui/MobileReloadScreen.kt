package com.bankingsystem.mobile.features.wallet.interfaces.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.PhoneIphone
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun MobileReloadScreen(
    modifier: Modifier = Modifier,
    error: String? = null,
    onDismissError: () -> Unit = {},
    onConfirm: (msisdn: String, amount: String) -> Unit,
    onCancel: () -> Unit
) {
    var msisdn by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var touchedMsisdn by remember { mutableStateOf(false) }
    var touchedAmount by remember { mutableStateOf(false) }

    val msisdnErrRaw = WalletValidators.msisdnError(msisdn)
    val amountErrRaw = WalletValidators.amountError(amount)
    val showMsisdnErr = touchedMsisdn && msisdnErrRaw != null
    val showAmountErr = touchedAmount && amountErrRaw != null
    val enabled = msisdnErrRaw == null && amountErrRaw == null && msisdn.isNotBlank() && amount.isNotBlank()

    val host = remember { SnackbarHostState() }
    val cardColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f)
    val tfColors = OutlinedTextFieldDefaults.colors(
        unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.06f),
        focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.10f),
        disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.06f),
        errorContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.06f),
        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
        focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
        errorBorderColor = MaterialTheme.colorScheme.error
    )

    LaunchedEffect(error) {
        if (error != null) {
            host.showSnackbar(error)
            onDismissError()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(host) }
    ) { pad ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(pad)
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Mobile Reload", style = MaterialTheme.typography.headlineSmall)
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = androidx.compose.material3.CardDefaults.elevatedCardColors(containerColor = cardColor)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    OutlinedTextField(
                        value = msisdn,
                        onValueChange = { v -> msisdn = v; if (!touchedMsisdn) touchedMsisdn = true },
                        label = { Text("Mobile number") },
                        leadingIcon = { Icon(Icons.Outlined.PhoneIphone, null) },
                        isError = showMsisdnErr,
                        supportingText = { if (showMsisdnErr) Text(msisdnErrRaw!!) },
                        singleLine = true,
                        colors = tfColors,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { v -> amount = WalletValidators.formatAmountInput(v); if (!touchedAmount) touchedAmount = true },
                        label = { Text("Amount") },
                        leadingIcon = { Icon(Icons.Outlined.AttachMoney, null) },
                        isError = showAmountErr,
                        supportingText = { if (showAmountErr) Text(amountErrRaw!!) },
                        singleLine = true,
                        colors = tfColors,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FilledTonalButton(enabled = enabled, onClick = { onConfirm(msisdn, amount) }) { Text("Confirm") }
                        TextButton(onClick = onCancel) { Text("Cancel") }
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

package com.bankingsystem.mobile.features.payments.interfaces.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun TransferScreen(
    modifier: Modifier = Modifier,
    onReview: (PaymentDraft) -> Unit,
) {
    val cs = MaterialTheme.colorScheme

    var source by remember { mutableStateOf("") }
    var destAcc by remember { mutableStateOf("") }
    var destBank by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(shape = RoundedCornerShape(16.dp), color = cs.primary.copy(alpha = 0.15f)) {
            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.Send, null, tint = Color.White)
                Spacer(Modifier.width(10.dp))
                Text("Transfer between accounts", color = Color.White, style = MaterialTheme.typography.titleMedium)
            }
        }

        AccountSelector(
            label = "From account",
            value = source,
            onValueChange = { source = it }
        )

        OutlinedTextField(
            value = destAcc,
            onValueChange = { destAcc = it },
            label = { Text("To account number") },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = destBank,
            onValueChange = { destBank = it },
            label = { Text("Destination bank") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it },
            label = { Text("Amount") },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = desc,
            onValueChange = { desc = it },
            label = { Text("Description (optional)") },
            singleLine = false,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(4.dp))
        Button(modifier = Modifier.fillMaxWidth(), onClick = {
            onReview(
                PaymentDraft(
                    kind = PaymentKind.TRANSFER,
                    sourceAccount = source,
                    destinationAccount = destAcc,
                    destinationBank = destBank,
                    amount = amount,
                    description = desc,
                )
            )
        }) {
            Text("Review transfer")
        }
    }
}

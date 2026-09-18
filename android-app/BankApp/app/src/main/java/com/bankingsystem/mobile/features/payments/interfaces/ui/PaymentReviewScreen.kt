package com.bankingsystem.mobile.features.payments.interfaces.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun PaymentReviewScreen(
    modifier: Modifier = Modifier,
    draft: PaymentDraft,
    onEdit: () -> Unit,
    onConfirm: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Review", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.SemiBold)

        Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LabeledValue("Type", draft.kind.name.lowercase().replaceFirstChar { it.uppercase() })
                if (draft.sourceAccount.isNotBlank()) LabeledValue("From", draft.sourceAccount)
                if (draft.destinationAccount.isNotBlank()) LabeledValue("To", draft.destinationAccount)
                if (draft.destinationBank.isNotBlank()) LabeledValue("Bank", draft.destinationBank)
                LabeledValue("Amount", draft.amount)
                if (draft.description.isNotBlank()) LabeledValue("Description", draft.description)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onEdit) { Text("Edit") }
            Button(onClick = onConfirm) { Text("Confirm") }
        }
    }
}

@Composable
private fun LabeledValue(label: String, value: String) {
    Column { Text(label, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(0.75f)); Text(value, color = Color.White) }
}

package com.bankingsystem.mobile.features.kyc.interfaces.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
internal fun KycPanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = cs.surface.copy(alpha = 0.10f),
        tonalElevation = 0.dp,
        shadowElevation = 12.dp,
        border = BorderStroke(1.dp, cs.outline.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

@Composable
internal fun StepHeaderRow(current: KycStep, modifier: Modifier = Modifier) {
    val steps = listOf(
        Triple(Icons.Filled.CreditCard, "ID", KycStep.Document),
        Triple(Icons.Filled.Face, "Selfie", KycStep.Selfie),
        Triple(Icons.Filled.Home, "Address", KycStep.Address),
        Triple(Icons.Filled.CheckCircle, "Review", KycStep.Review)
    )
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        steps.forEach { (icon, label, step) ->
            val active = step == current
            AssistChip(
                onClick = { },
                label = {
                    Text(
                        label,
                        color = if (active) MaterialTheme.colorScheme.primary
                        else Color.White.copy(0.70f)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (active) MaterialTheme.colorScheme.primary
                        else Color.White.copy(0.70f)
                    )
                },
                border = BorderStroke(
                    1.dp,
                    (if (active) MaterialTheme.colorScheme.primary else Color.White).copy(alpha = 0.45f)
                ),
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = Color.White.copy(alpha = if (active) 0.10f else 0.06f)
                )
            )
        }
    }
}

@Composable
internal fun SourceChooserDialog(
    onDismiss: () -> Unit,
    onCamera: () -> Unit,
    onGallery: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add photo from") },
        text = { Text("Choose a source") },
        confirmButton = { TextButton(onClick = onCamera) { Text("Camera") } },
        dismissButton = { TextButton(onClick = onGallery) { Text("Gallery") } }
    )
}

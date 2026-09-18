package com.bankingsystem.mobile.features.wallet.interfaces.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun SavedCardChip(
    brand: String,
    last4: String,
    exp: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(100))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(text = "$brand •••• $last4  ·  $exp", color = Color.White)
    }
}

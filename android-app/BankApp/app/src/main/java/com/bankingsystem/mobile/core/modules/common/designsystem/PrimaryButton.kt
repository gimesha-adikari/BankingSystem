package com.bankingsystem.mobile.core.modules.common.designsystem

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PrimaryButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier,
    text: String,
    loading: Boolean = false,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    val cs = MaterialTheme.colorScheme
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 2.dp,
            pressedElevation = 6.dp,
            disabledElevation = 0.dp
        ),
        colors = ButtonDefaults.buttonColors(
            containerColor = cs.primary,
            contentColor = cs.onPrimary,
            disabledContainerColor = cs.primary.copy(alpha = 0.5f),
            disabledContentColor = cs.onPrimary.copy(alpha = 0.7f)
        ),
        content = {
            if (loading) {
                CircularProgressIndicator(strokeWidth = 2.dp)
            } else {
                Row(horizontalArrangement = Arrangement.Center) {
                    if (leadingIcon != null) {
                        leadingIcon()
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(text = text, style = MaterialTheme.typography.labelLarge)
                    if (trailingIcon != null) {
                        Spacer(Modifier.width(8.dp))
                        trailingIcon()
                    }
                }
            }
        }
    )
}

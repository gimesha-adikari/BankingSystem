package com.bankingsystem.mobile.features.profile.interfaces.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun PasswordVerificationDialog(
    onConfirm: (password: String) -> Unit,
    onDismiss: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var show by remember { mutableStateOf(false) }

    val cs = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.large

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = shape,
        containerColor = cs.surface.copy(alpha = 0.90f),
        tonalElevation = 0.dp,

        title = {
            Text(
                "Verify Password",
                color = Color.Black,
                style = MaterialTheme.typography.titleLarge
            )
        },

        text = {
            Column {
                Text(
                    "Enter your current password to confirm username change.",
                    color = cs.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    singleLine = true,
                    label = { Text("Current password") },
                    visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { show = !show }) {
                            Icon(
                                imageVector = if (show) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = if (show) "Hide password" else "Show password"
                            )
                        }
                    },
                    shape = MaterialTheme.shapes.medium
                )
            }
        },

        confirmButton = {
            Button(
                onClick = { onConfirm(password) },
                enabled = password.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = cs.primary),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Verify", color = cs.onPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = cs.onSurfaceVariant)
            }
        }
    )
}

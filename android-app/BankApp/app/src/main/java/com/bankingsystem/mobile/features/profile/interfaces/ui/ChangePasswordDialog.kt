package com.bankingsystem.mobile.features.profile.interfaces.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bankingsystem.mobile.core.modules.common.designsystem.PasswordTextField
import com.bankingsystem.mobile.core.modules.common.util.checkPasswordStrength
import com.bankingsystem.mobile.core.modules.common.util.doPasswordsMatch

@Composable
fun ChangePasswordDialog(
    onConfirm: (current: String, new: String, confirm: String) -> Unit,
    onDismiss: () -> Unit
) {
    var current by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }

    val strength = remember(newPass) { checkPasswordStrength(newPass) }
    val matches = remember(newPass, confirm) { doPasswordsMatch(newPass, confirm) }

    val canSubmit = current.isNotBlank() && strength.score >= 4 && matches
    val cs = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.large

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = shape,
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        tonalElevation = 0.dp,
        title = {
            Text(
                "Change Password",
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
            )
        },
        text = {
            Column {
                PasswordTextField(
                    value = current,
                    onValueChange = { current = it },
                    label = "Current password",
                    isError = current.isBlank(),
                    supportingText = {
                        if (current.isBlank()) {
                            Text(
                                "Enter your current password",
                                color = cs.onSurfaceVariant
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                PasswordTextField(
                    value = newPass,
                    onValueChange = { newPass = it },
                    label = "New password",
                    isError = newPass.isNotBlank() && strength.score < 4,
                    supportingText = {
                        StrengthMeter(strengthScore = strength.score, issues = strength.issues)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                )

                PasswordTextField(
                    value = confirm,
                    onValueChange = { confirm = it },
                    label = "Confirm new password",
                    isError = confirm.isNotBlank() && !matches,
                    supportingText = {
                        when {
                            confirm.isBlank() -> Text("Re-enter new password", color = cs.onSurfaceVariant)
                            matches -> Text("Passwords match", color = cs.tertiary)
                            else -> Text("Passwords do not match", color = cs.error)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                enabled = canSubmit,
                onClick = { onConfirm(current, newPass, confirm) },
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(containerColor = cs.primary)
            ) {
                Text("Change", color = cs.onPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = cs.onSurfaceVariant)
            }
        }
    )
}

@Composable
private fun StrengthMeter(strengthScore: Int, issues: List<String>) {
    val cs = MaterialTheme.colorScheme
    val scoreClamped = strengthScore.coerceIn(0, 4)
    val progress = scoreClamped / 4f

    val barColor = when (scoreClamped) {
        0, 1 -> cs.error
        2 -> cs.secondary
        else -> cs.primary
    }
    val label = when (scoreClamped) {
        0, 1 -> "Weak"
        2 -> "Okay"
        3 -> "Strong"
        else -> "Excellent"
    }

    Column {
        LinearProgressIndicator(
            progress = { progress },
            color = barColor,
            trackColor = cs.onSurfaceVariant.copy(alpha = 0.25f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = cs.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
        if (issues.isNotEmpty()) {
            Column(Modifier.padding(top = 4.dp)) {
                issues.forEach {
                    Text("• $it", style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
                }
            }
        }
    }
}

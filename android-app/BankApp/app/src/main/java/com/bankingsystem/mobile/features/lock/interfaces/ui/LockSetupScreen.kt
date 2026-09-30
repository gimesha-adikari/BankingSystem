package com.bankingsystem.mobile.features.lock.interfaces.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.bankingsystem.mobile.core.modules.common.designsystem.FadingAppBackground

@Composable
fun LockSetupScreen(
    initialPin: String?,
    onSavePin: (String) -> Unit,
    onLockEnabledChange: (Boolean) -> Unit,
    onCancel: () -> Unit
) {
    var pin by remember { mutableStateOf(initialPin ?: "") }
    var confirmPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Box(Modifier.fillMaxSize()) {
        FadingAppBackground()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = MaterialTheme.shapes.large,
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                        shape = CircleShape,
                        tonalElevation = 0.dp
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(64.dp)
                                .padding(12.dp)
                                .clip(CircleShape)
                        )
                    }

                    Text(
                        "Set App Lock PIN",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    OutlinedTextField(
                        value = pin,
                        onValueChange = { pin = it.filter(Char::isDigit) },
                        label = { Text("Enter PIN") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = confirmPin,
                        onValueChange = { confirmPin = it.filter(Char::isDigit) },
                        label = { Text("Confirm PIN") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (error != null) {
                        Text(
                            error!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Button(
                        onClick = {
                            when {
                                pin.length < 4 -> error = "PIN must be at least 4 digits"
                                pin != confirmPin -> error = "PINs do not match"
                                else -> {
                                    error = null
                                    onSavePin(pin)
                                    onLockEnabledChange(true)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save PIN & Enable Lock")
                    }

                    OutlinedButton(
                        onClick = {
                            onLockEnabledChange(false)
                            onCancel()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Disable Lock")
                    }
                }
            }
        }
    }
}

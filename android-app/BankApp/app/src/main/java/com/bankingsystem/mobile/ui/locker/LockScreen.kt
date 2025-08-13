package com.bankingsystem.mobile.ui.locker

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.bankingsystem.mobile.util.showBiometricPrompt

@Composable
fun LockScreen(
    onAuthenticated: () -> Unit,
    onFallbackToPin: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        activity?.let {
            showBiometricPrompt(
                activity = it,
                onSuccess = { onAuthenticated() },
                onFailure = {
                    errorMessage = "Biometric failed. Try PIN."
                    onFallbackToPin()
                },
                onError = { error ->
                    errorMessage = error
                    onFallbackToPin()
                }
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = MaterialTheme.shapes.extraLarge,
            elevation = CardDefaults.cardElevation(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = "Biometric",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp)
                )
                Text(
                    "Authenticate to Continue",
                    style = MaterialTheme.typography.titleLarge
                )
                if (errorMessage != null) {
                    Text(errorMessage!!, color = Color.Red)
                }
                OutlinedButton(
                    onClick = onFallbackToPin,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Use PIN Instead")
                }
            }
        }
    }
}

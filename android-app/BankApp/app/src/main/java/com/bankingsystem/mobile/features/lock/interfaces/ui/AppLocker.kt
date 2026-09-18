package com.bankingsystem.mobile.features.lock.interfaces.ui

import androidx.compose.runtime.*

@Composable
fun AppLocker(
    correctPin: String,
    onAuthenticated: () -> Unit
) {
    var usePin by remember { mutableStateOf(false) }
    var isAuthenticated by remember { mutableStateOf(false) }

    if (isAuthenticated) {
        onAuthenticated()
    } else {
        if (usePin) {
            PinUnlockScreen(correctPin = correctPin) {
                isAuthenticated = true
            }
        } else {
            LockScreen(
                onAuthenticated = { isAuthenticated = true },
                onFallbackToPin = { usePin = true }
            )
        }
    }
}

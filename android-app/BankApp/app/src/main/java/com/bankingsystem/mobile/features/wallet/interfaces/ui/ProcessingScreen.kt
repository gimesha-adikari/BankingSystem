package com.bankingsystem.mobile.features.wallet.interfaces.ui

import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun ProcessingScreen(
    modifier: Modifier = Modifier,
    onDone: () -> Unit,
    onBackHome: () -> Unit
) {
    val vm = hiltViewModel<PaymentFlowViewModel>()
    val state by vm.state.collectAsState()
    val ctx = LocalContext.current

    LaunchedEffect(state.stage) {
        if (state.stage == PaymentStage.SUCCEEDED || state.stage == PaymentStage.FAILED || state.stage == PaymentStage.CANCELED) onDone()
    }
    LaunchedEffect(state.actionUrl) {
        val url = state.actionUrl ?: return@LaunchedEffect
        try {
            CustomTabsIntent.Builder().build().launchUrl(ctx, Uri.parse(url))
        } finally {
            vm.clearActionUrl()
        }
    }

    val cardColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f)

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        ElevatedCard(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = cardColor)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(modifier = Modifier.size(44.dp))
                Text(
                    when (state.stage) {
                        PaymentStage.CREATING -> "Creating payment"
                        PaymentStage.PROCESSING -> if (state.actionUrl == null) "Processing payment" else "Continue in browser"
                        else -> ""
                    },
                    style = MaterialTheme.typography.titleMedium
                )
                if (state.actionUrl != null) {
                    FilledTonalButton(onClick = {
                        CustomTabsIntent.Builder().build().launchUrl(ctx, Uri.parse(state.actionUrl))
                        vm.clearActionUrl()
                    }) { Text("Open Secure Page") }
                    TextButton(onClick = onBackHome) { Text("Back to home") }
                }
            }
        }
    }
}

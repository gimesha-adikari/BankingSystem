package com.bankingsystem.mobile.features.accounts.interfaces.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bankingsystem.mobile.features.accounts.interfaces.ui.models.AccountUi

@Composable
fun MyAccountsScreen(
    modifier: Modifier = Modifier,
    accounts: List<AccountUi>,
    loading: Boolean,
    error: String?,
    onRefresh: () -> Unit,
    onOpenAccount: () -> Unit,
    onAccountClick: (String, String) -> Unit,
    defaultAccountId: String?,
    onSetDefault: (String) -> Unit,
    customerMissing: Boolean = false,
    customerMessage: String? = null,
    onStartKyc: () -> Unit = {},
    onFixCustomer: () -> Unit = {},
    kycApproved: Boolean? = null,
    onRegisterCustomer: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.AccountBalance, null, tint = Color.White)
            Spacer(Modifier.width(10.dp))
            Text("My Accounts", style = MaterialTheme.typography.titleLarge, color = Color.White)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onRefresh, enabled = !loading) {
                Text(if (loading) "Refreshing…" else "Refresh")
            }
        }

        OpenAnotherAccountCTA(onOpenAccount = onOpenAccount)

        when {
            loading -> repeat(3) { AccountSkeleton() }
            error != null -> ErrorPanel(error, onRefresh)

            customerMissing -> CustomerMissingPanel(
                message = customerMessage
                    ?: "We need to verify your identity and create your profile before you can view or open accounts.",
                kycApproved = kycApproved,
                onStartKyc = onStartKyc,
                onGoRegister = onRegisterCustomer
            )

            accounts.isEmpty() -> EmptyAccounts(onOpenAccount)
            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 72.dp)
                ) {
                    items(accounts) { acc ->
                        AccountRow(
                            ui = acc,
                            isDefault = acc.accountId == defaultAccountId,
                            onClick = { onAccountClick(acc.accountId, acc.accountNumber) },
                            onSetDefault = { onSetDefault(acc.accountId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OpenAnotherAccountCTA(onOpenAccount: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0x14222A2E),
        shadowElevation = 0.dp,
        border = ButtonDefaults.outlinedButtonBorder(true).copy(width = 1.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ExtendedFloatingActionButton(
                onClick = onOpenAccount,
                containerColor = Color(0xFF7C3AED),
                contentColor = Color.White,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Open Account") }
            )
        }
    }
}

@Composable
private fun AccountRow(
    ui: AccountUi,
    isDefault: Boolean,
    onClick: () -> Unit,
    onSetDefault: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0x14222A2E),
        border = ButtonDefaults.outlinedButtonBorder(true).copy(width = 1.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    ui.accountNumber,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val subtitle = buildString {
                    append(ui.type.name.lowercase().replaceFirstChar { it.uppercase() })
                    append(" • ")
                    append(ui.status.name.lowercase().replaceFirstChar { it.uppercase() })
                    ui.branchName?.let { append(" • ").append(it) }
                }
                Text(subtitle, color = Color.White.copy(alpha = 0.8f))
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isDefault) {
                    OutlinedButton(onClick = onSetDefault, shape = RoundedCornerShape(12.dp)) {
                        Icon(Icons.Outlined.Star, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Make Default")
                    }
                } else {
                    FilledTonalButton(onClick = onClick, shape = RoundedCornerShape(12.dp)) {
                        Icon(Icons.Filled.Star, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Default")
                    }
                }
                Button(onClick = onClick, shape = RoundedCornerShape(12.dp)) { Text("View") }
            }
        }
    }
}

@Composable
private fun AccountSkeleton() {
    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
}

@Composable
private fun ErrorPanel(message: String, onRetry: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0x33FF6B6B),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                "Something went wrong",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))
            Text(message, color = Color.White.copy(alpha = 0.9f))
            Spacer(Modifier.height(12.dp))
            Button(onClick = onRetry, shape = RoundedCornerShape(12.dp)) { Text("Retry") }
        }
    }
}

@Composable
private fun EmptyAccounts(onOpenAccount: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0x14222A2E)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                "No accounts yet",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))
            Text("Open your first account to get started.", color = Color.White.copy(alpha = 0.9f))
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onOpenAccount,
                shape = RoundedCornerShape(12.dp)
            ) { Text("Open Account") }
        }
    }
}

/**
 * If [kycApproved] == true, show "Register as Customer".
 * Otherwise show "Start Verification".
 * (No "Go to Profile" link anymore.)
 */
@Composable
fun CustomerMissingPanel(
    message: String,
    kycApproved: Boolean?,
    onStartKyc: () -> Unit,
    onGoRegister: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color(0xFF7C3AED).copy(alpha = 0.18f)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Almost there!", color = Color.White, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text(message, color = Color.White.copy(alpha = 0.9f))
            Spacer(Modifier.height(12.dp))

            if (kycApproved == true) {
                Button(
                    onClick = onGoRegister,
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Register as Customer") }
            } else {
                Button(
                    onClick = onStartKyc,
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Start Verification") }
            }
        }
    }
}

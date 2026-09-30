package com.bankingsystem.mobile.features.accounts.interfaces.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Business
import androidx.compose.material3.*
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bankingsystem.mobile.features.accounts.interfaces.ui.models.AccountType
import com.bankingsystem.mobile.features.accounts.interfaces.ui.models.BranchOption

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenAccountScreen(
    accountType: AccountType,
    onAccountTypeChange: (AccountType) -> Unit,
    initialDeposit: String,
    onInitialDepositChange: (String) -> Unit,
    selectedBranchId: String?,
    onBranchChange: (String?) -> Unit,

    branches: List<BranchOption>,
    loadingBranches: Boolean,
    depositError: String?,
    branchError: String?,

    submitting: Boolean,
    onCancel: () -> Unit,
    onCreateClick: () -> Unit,

    kycRequired: Boolean = false,
    onStartKyc: () -> Unit = {},

    modifier: Modifier = Modifier,
) {
    var accountTypeExpanded by remember { mutableStateOf(false) }
    var branchExpanded by remember { mutableStateOf(false) }
    val scroll = rememberScrollState()
    val cs = MaterialTheme.colorScheme

    val selectedBranchName = remember(branches, selectedBranchId) {
        branches.firstOrNull { it.id == selectedBranchId }?.name ?: ""
    }

    val canCreate = !submitting &&
            selectedBranchId != null &&
            depositError == null &&
            initialDeposit.isNotBlank()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Column {
            Text(
                text = "Open a New Account",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White
            )
            Text(
                text = "Choose type, deposit, and branch. Your account number will be generated automatically.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        /* 🔹 KYC banner (only if kycRequired = true) */
        if (kycRequired) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF7C3AED).copy(alpha = 0.18f),
                tonalElevation = 0.dp,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Identity verification required",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Verify your identity to open an account.",
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    TextButton(onClick = onStartKyc) { Text("Start KYC") }
                }
            }
        }

        GlassPanel {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.AccountBalance, null, tint = Color.White)
                Spacer(Modifier.width(10.dp))
                Text("Account Details", style = MaterialTheme.typography.titleLarge, color = Color.White)
            }
            Spacer(Modifier.height(14.dp))

            ExposedDropdownMenuBox(
                expanded = accountTypeExpanded,
                onExpandedChange = { accountTypeExpanded = !accountTypeExpanded }
            ) {
                OutlinedTextField(
                    value = accountType.name.replace('_',' ').lowercase().replaceFirstChar { it.uppercase() },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Account Type") },
                    leadingIcon = { Icon(Icons.Filled.AccountBalance, null) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(accountTypeExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = lightFieldColors()
                )
                ExposedDropdownMenu(
                    expanded = accountTypeExpanded,
                    onDismissRequest = { accountTypeExpanded = false }
                ) {
                    AccountType.entries.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.name.replace('_',' ').lowercase().replaceFirstChar { it.uppercase() }) },
                            onClick = { onAccountTypeChange(type); accountTypeExpanded = false }
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = initialDeposit,
                onValueChange = onInitialDepositChange,
                label = { Text("Initial Deposit") },
                placeholder = { Text("0.00") },
                singleLine = true,
                isError = depositError != null,
                supportingText = { depositError?.let { Text(it, color = MaterialTheme.colorScheme.error) } },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { /* no-op */ }),
                leadingIcon = { Icon(Icons.Filled.AddCircle, null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = lightFieldColors()
            )
            if (depositError == null) {
                Text(
                    text = "Minimum deposit for ${accountType.pretty()}: ${accountType.minDeposit()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFCBD5E1)
                )
            }
        }

        GlassPanel {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Business, null, tint = Color.White)
                Spacer(Modifier.width(10.dp))
                Text("Branch", style = MaterialTheme.typography.titleLarge, color = Color.White)
            }
            Spacer(Modifier.height(14.dp))

            ExposedDropdownMenuBox(
                expanded = branchExpanded,
                onExpandedChange = { branchExpanded = !branchExpanded }
            ) {
                OutlinedTextField(
                    value = selectedBranchName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Branch") },
                    placeholder = { Text(if (loadingBranches) "Loading…" else "Select a branch") },
                    isError = branchError != null,
                    supportingText = { branchError?.let { Text(it, color = MaterialTheme.colorScheme.error) } },
                    leadingIcon = { Icon(Icons.Filled.Business, null) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(branchExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = lightFieldColors()
                )
                ExposedDropdownMenu(
                    expanded = branchExpanded,
                    onDismissRequest = { branchExpanded = false }
                ) {
                    when {
                        loadingBranches -> DropdownMenuItem(text = { Text("Loading…") }, onClick = { }, enabled = false)
                        branches.isEmpty() -> DropdownMenuItem(text = { Text("No branches available") }, onClick = { }, enabled = false)
                        else -> branches.forEach { b ->
                            DropdownMenuItem(
                                text = { Text(b.name) },
                                onClick = { onBranchChange(b.id); branchExpanded = false }
                            )
                        }
                    }
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onCancel, enabled = !submitting) { Text("Cancel") }
            Spacer(Modifier.width(12.dp))
            Button(
                onClick = onCreateClick,
                enabled = canCreate,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = cs.primary, contentColor = cs.onPrimary),
                modifier = Modifier.height(48.dp)
            ) { Text(if (submitting) "Creating…" else "Create Account") }
        }
    }
}

/* Shared visuals */

@Composable
private fun GlassPanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = cs.surface.copy(alpha = 0.10f),
        tonalElevation = 0.dp,
        shadowElevation = 12.dp
    ) {
        Column(
            modifier = Modifier
                .border(1.dp, cs.outline.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
                .padding(16.dp),
            content = content
        )
    }
}

private fun AccountType.pretty() =
    name.replace('_',' ').lowercase().replaceFirstChar { it.uppercase() }

private fun AccountType.minDeposit() = when (this) {
    AccountType.SAVINGS -> "1000.00"
    AccountType.CHECKING -> "0.00"
    AccountType.FIXED_DEPOSIT -> "5000.00"
}

@Composable
private fun lightFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = Color(0xFFF0F4FF),
    unfocusedContainerColor = Color(0xFFEFF3FB),
    disabledContainerColor = Color(0xFFE5E7EB),
    focusedTextColor = Color(0xFF0F172A),
    unfocusedTextColor = Color(0xFF111827),
    focusedBorderColor = Color(0xFF4F46E5),
    unfocusedBorderColor = Color(0xFFCBD5E1),
    focusedLabelColor = Color(0xFF4F46E5),
    unfocusedLabelColor = Color(0xFF475569),
    cursorColor = Color(0xFF4F46E5),
)

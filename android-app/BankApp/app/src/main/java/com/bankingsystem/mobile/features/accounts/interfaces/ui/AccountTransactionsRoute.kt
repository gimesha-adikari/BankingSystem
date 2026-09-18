package com.bankingsystem.mobile.features.accounts.interfaces.ui

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bankingsystem.mobile.core.modules.common.designsystem.FadingAppBackground
import com.bankingsystem.mobile.core.modules.common.designsystem.Sidebar
import kotlinx.coroutines.launch

@SuppressLint("ConfigurationScreenWidthHeight")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountTransactionsRoute(
    userName: String,
    accountId: String,
    accountNumber: String?,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit = {}
) {
    val config = LocalConfiguration.current
    val isCompact = config.screenWidthDp < 600

    val vm: AccountTransactionsViewModel = hiltViewModel()

    LaunchedEffect(accountId) { vm.load(accountId) }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var showFull by rememberSaveable { mutableStateOf(false) }

    val secondaryTitle = remember(accountId, accountNumber, showFull) {
        accountNumber?.let { no ->
            "Account • " + if (showFull) formatAccNo(no) else maskAccNo(no)
        } ?: "Account • ${accountId.take(8)}…${accountId.takeLast(6)}"
    }

    val topBar: @Composable () -> Unit = {
        TopAppBar(
            title = {
                Column {
                    Text("Transactions", color = Color.White)
                    Text(
                        secondaryTitle,
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            },
            actions = {
                if (!accountNumber.isNullOrBlank()) {
                    IconButton(onClick = { showFull = !showFull }) {
                        Icon(
                            if (showFull) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (showFull) "Hide full number" else "Show full number",
                            tint = Color.White
                        )
                    }
                }
                if (isCompact) {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                        Icon(Icons.Filled.Menu, contentDescription = "Menu", tint = Color.White)
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent,
                titleContentColor = Color.White,
                navigationIconContentColor = Color.White,
                actionIconContentColor = Color.White
            )
        )
    }

    val content: @Composable (PaddingValues) -> Unit = { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            AccountTransactionsScreen(
                accountId = accountId,
                accountNumber = accountNumber,
            )
        }
    }

    if (isCompact) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(drawerContainerColor = Color(0xFF0B0B12)) {
                    Sidebar(
                        selectedItem = "My Accounts",
                        onItemClick = { label ->
                            scope.launch { drawerState.close() }
                            onNavigate(label)
                        },
                        userName = userName
                    )
                }
            }
        ) {
            Box(Modifier.fillMaxSize()) {
                FadingAppBackground()
                Scaffold(containerColor = Color.Transparent, topBar = topBar, content = content)
            }
        }
    } else {
        Row(Modifier.fillMaxSize()) {
            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(300.dp),
                tonalElevation = 0.dp,
                shadowElevation = 16.dp
            ) {
                Sidebar(
                    selectedItem = "My Accounts",
                    onItemClick = onNavigate,
                    userName = userName
                )
            }
            Box(Modifier.weight(1f).fillMaxHeight()) {
                FadingAppBackground()
                Scaffold(containerColor = Color.Transparent, topBar = topBar, content = content)
            }
        }
    }
}

/* ---- helpers ---- */

private fun formatAccNo(no: String): String =
    no.filter { it.isDigit() }.chunked(4).joinToString(" ")

private fun maskAccNo(no: String): String {
    val last4 = no.takeLast(4)
    return "•••• $last4"
}

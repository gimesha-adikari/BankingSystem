package com.bankingsystem.mobile.features.accounts.interfaces.ui

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel   // <-- add this
import com.bankingsystem.mobile.core.modules.common.designsystem.FadingAppBackground
import com.bankingsystem.mobile.core.modules.common.designsystem.Sidebar
import com.bankingsystem.mobile.app.navigation.Routes
import kotlinx.coroutines.launch

@SuppressLint("ConfigurationScreenWidthHeight")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsRoute(
    userName: String,
    onNavigate: (String) -> Unit
) {
    val config = LocalConfiguration.current
    val isCompact = config.screenWidthDp < 600

    val vm: OpenAccountViewModel = hiltViewModel()
    val ui by vm.ui.collectAsState()
    val events = vm.events
    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(events) {
        events.collect { ev ->
            when (ev) {
                is OpenAccountEvent.Created -> {
                    scope.launch {
                        snackbarHost.showSnackbar(
                            message = "Account ${ev.accountNumber} created",
                            duration = SnackbarDuration.Short
                        )
                    }
                    onNavigate(Routes.accountTx(ev.accountId, ev.accountNumber))
                }
                is OpenAccountEvent.NeedsCustomerProfile -> {
                    val result = snackbarHost.showSnackbar(
                        message = "We need to verify your identity before opening an account.",
                        actionLabel = "Start KYC",
                        withDismissAction = true,
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) onNavigate(Routes.KYC)
                }
                is OpenAccountEvent.Error -> {
                    scope.launch {
                        snackbarHost.showSnackbar(
                            message = ev.message,
                            duration = SnackbarDuration.Short
                        )
                    }
                }
            }
        }
    }

    if (isCompact) {
        val drawerState = rememberDrawerState(DrawerValue.Closed)
        val scope = rememberCoroutineScope()

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(drawerContainerColor = Color(0xFF0B0B12)) {
                    Sidebar(
                        selectedItem = "Open Accounts",
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
                Scaffold(
                    containerColor = Color.Transparent,
                    topBar = {
                        TopAppBar(
                            title = { Text("Open Account") },
                            navigationIcon = {
                                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                    Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.Transparent,
                                scrolledContainerColor = Color.Transparent,
                                titleContentColor = Color.White,
                                navigationIconContentColor = Color.White
                            )
                        )
                    },
                    snackbarHost = { SnackbarHost(snackbarHost) }
                ) { padding ->
                    OpenAccountScreen(
                        accountType = ui.accountType,
                        onAccountTypeChange = vm::onAccountTypeChange,
                        initialDeposit = ui.initialDeposit,
                        onInitialDepositChange = vm::onInitialDepositChange,
                        selectedBranchId = ui.selectedBranchId,
                        onBranchChange = vm::onBranchChange,
                        branches = ui.branches,
                        loadingBranches = ui.loadingBranches,
                        depositError = ui.depositError,
                        branchError = ui.branchError,
                        submitting = ui.submitting,
                        onCancel = { onNavigate("Home") },
                        onCreateClick = { vm.submit() },
                        modifier = Modifier.padding(padding)
                    )
                }
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
                    selectedItem = "Open Accounts",
                    onItemClick = onNavigate,
                    userName = userName
                )
            }
            Box(Modifier.weight(1f).fillMaxHeight()) {
                FadingAppBackground()
                Scaffold(
                    containerColor = Color.Transparent,
                    topBar = {
                        TopAppBar(
                            title = { Text("Open Account") },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.Transparent,
                                titleContentColor = Color.White
                            )
                        )
                    },
                    snackbarHost = { SnackbarHost(snackbarHost) }
                ) { padding ->
                    OpenAccountScreen(
                        accountType = ui.accountType,
                        onAccountTypeChange = vm::onAccountTypeChange,
                        initialDeposit = ui.initialDeposit,
                        onInitialDepositChange = vm::onInitialDepositChange,
                        selectedBranchId = ui.selectedBranchId,
                        onBranchChange = vm::onBranchChange,
                        branches = ui.branches,
                        loadingBranches = ui.loadingBranches,
                        depositError = ui.depositError,
                        branchError = ui.branchError,
                        submitting = ui.submitting,
                        onCancel = { onNavigate("Home") },
                        onCreateClick = { vm.submit() },
                        modifier = Modifier.padding(padding)
                    )
                }
            }
        }
    }
}

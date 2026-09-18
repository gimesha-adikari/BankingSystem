package com.bankingsystem.mobile.features.accounts.interfaces.ui

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bankingsystem.mobile.app.navigation.Routes
import com.bankingsystem.mobile.core.modules.common.designsystem.FadingAppBackground
import com.bankingsystem.mobile.core.modules.common.designsystem.Sidebar
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun MyAccountsRoute(
    userName: String,
    onNavigate: (String) -> Unit
) {
    val vm: MyAccountsViewModel = hiltViewModel()
    val ui by vm.ui.collectAsState()

    val config = LocalConfiguration.current
    val isCompact = config.screenWidthDp < 900
    val drawerState: DrawerState = rememberDrawerState(initialValue = androidx.compose.material3.DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val topBar: @Composable () -> Unit = {
        TopAppBar(
            title = { Text("Accounts", color = Color.White) },
            navigationIcon = {
                if (isCompact) {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                        Icon(Icons.Filled.Menu, contentDescription = "Menu", tint = Color.White)
                    }
                }
            },
            actions = {
                TextButton(onClick = vm::refresh, enabled = !ui.loading) {
                    Text(if (ui.loading) "Refreshing…" else "Refresh")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                titleContentColor = Color.White,
                navigationIconContentColor = Color.White,
                actionIconContentColor = Color.White
            ),
            scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
        )
    }

    val content: @Composable (PaddingValues) -> Unit = { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            MyAccountsScreen(
                accounts = ui.accounts,
                loading = ui.loading,
                error = ui.error,
                onRefresh = vm::refresh,
                onOpenAccount = { onNavigate(Routes.ACCOUNTS_OPEN) },
                onAccountClick = { id, number -> onNavigate(Routes.accountTx(id, number)) },
                defaultAccountId = ui.defaultAccountId,
                onSetDefault = vm::setDefault,
                customerMissing = ui.customerMissing,
                customerMessage = ui.customerMessage,
                onStartKyc = { onNavigate(Routes.KYC) },
                onFixCustomer = { onNavigate(Routes.PROFILE) },
                // NEW ↓
                kycApproved = ui.kycApproved,
                onRegisterCustomer = { onNavigate(Routes.CUSTOMER_REG) }
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
                        onItemClick = onNavigate,
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
        androidx.compose.foundation.layout.Row(Modifier.fillMaxSize()) {
            ModalDrawerSheet(
                drawerContainerColor = Color(0xFF0B0B12),
                modifier = Modifier.weight(0.28f)
            ) {
                Sidebar(
                    selectedItem = "My Accounts",
                    onItemClick = onNavigate,
                    userName = userName
                )
            }
            Box(Modifier.weight(0.72f).fillMaxSize()) {
                FadingAppBackground()
                Scaffold(containerColor = Color.Transparent, topBar = topBar, content = content)
            }
        }
    }
}

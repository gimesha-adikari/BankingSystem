package com.bankingsystem.mobile.features.home.interfaces.ui

import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun BankHomeRoute(
    userName: String,
    selectedItem: String = "Home",
    onNavigate: (String) -> Unit
) {
    val vm: HomeViewModel = hiltViewModel()
    val ui by vm.ui.collectAsState()

    BankHomeScreen(
        userName = userName,
        balance = ui.balance,
        selectedItem = selectedItem,
        onNavigate = onNavigate
    )
}

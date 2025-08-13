package com.bankingsystem.mobile.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bankingsystem.mobile.ui.home.BankHomeScreen
import com.bankingsystem.mobile.ui.profile.ProfileScreen
import com.bankingsystem.mobile.ui.settings.SettingsScreen

@Composable
fun AppNavHost(
    userName: String,
    onLogout: () -> Unit,
    nav: NavHostController = rememberNavController()
) {
    NavHost(navController = nav, startDestination = Routes.HOME) {

        composable(Routes.HOME) {
            BankHomeScreen(
                userName = userName,
                selectedItem = "Home",
                onNavigate = { label -> navigateByLabel(nav, label, onLogout) }
            )
        }

        composable(Routes.PROFILE) {
            ProfileScreen(
                selectedItem = "Profile",
                onNavigate = { label -> navigateByLabel(nav, label, onLogout) }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onLogout = onLogout,
                onBack = { nav.navigateUp() }
            )
        }

        // Optional placeholders for future screens
        composable(Routes.ACCOUNTS) {
            // AccountsScreen(... pass onNavigate = { label -> navigateByLabel(nav, label, onLogout) })
        }
        composable(Routes.PAYMENTS) {
            // PaymentsScreen(...)
        }
    }
}

/* ---------------- helpers ---------------- */

private fun navigateByLabel(
    nav: NavHostController,
    label: String,
    onLogout: () -> Unit
) {
    val route = when (label) {
        "Home" -> Routes.HOME
        "Profile" -> Routes.PROFILE
        "Settings" -> Routes.SETTINGS
        "Accounts" -> Routes.ACCOUNTS
        "Payments" -> Routes.PAYMENTS
        "Logout" -> {
            onLogout()
            return
        }
        else -> return
    }
    nav.navigate(route) {
        popUpTo(nav.graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

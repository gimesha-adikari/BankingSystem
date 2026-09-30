package com.bankingsystem.mobile.app.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bankingsystem.mobile.features.accounts.interfaces.ui.AccountTransactionsRoute
import com.bankingsystem.mobile.features.accounts.interfaces.ui.AccountsRoute
import com.bankingsystem.mobile.features.accounts.interfaces.ui.MyAccountsRoute
import com.bankingsystem.mobile.features.customer.interfaces.ui.CustomerRegistrationScreen
import com.bankingsystem.mobile.features.home.interfaces.ui.BankHomeRoute
import com.bankingsystem.mobile.features.home.interfaces.ui.BankHomeScreen
import com.bankingsystem.mobile.features.kyc.interfaces.ui.KycRoute
import com.bankingsystem.mobile.features.kyc.interfaces.ui.KycStatusRoute
import com.bankingsystem.mobile.features.payments.interfaces.ui.PaymentsRoute
import com.bankingsystem.mobile.features.profile.interfaces.ui.ProfileRoute
import com.bankingsystem.mobile.features.settings.interfaces.ui.SettingsScreen
import com.bankingsystem.mobile.features.wallet.interfaces.ui.WalletRoute

@RequiresApi(Build.VERSION_CODES.O)
@ExperimentalMaterial3Api
@Composable
fun AppNavHost(
    userName: String,
    onLogout: () -> Unit,
    nav: NavHostController = rememberNavController()
) {
    NavHost(navController = nav, startDestination = Routes.HOME) {

        /* ---------- Home ---------- */
        composable(Routes.HOME) {
            BankHomeRoute(
                userName = userName,
                selectedItem = "Home",
                onNavigate = { label -> navigateByLabel(nav, label, onLogout) }
            )
        }

        /* ---------- Profile ---------- */
        composable(Routes.PROFILE) {
            ProfileRoute(
                onNavigate = { label -> navigateByLabel(nav, label, onLogout) }
            )
        }

        /* ---------- Settings ---------- */
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onLogout = onLogout,
                onBack = { nav.navigateUp() }
            )
        }

        /* ---------- Accounts: My list ---------- */
        composable(Routes.ACCOUNTS_MY) {
            MyAccountsRoute(
                userName = userName,
                onNavigate = { label -> navigateByLabel(nav, label, onLogout) }
            )
        }

        /* ---------- Accounts: Open account ---------- */
        composable(Routes.ACCOUNTS_OPEN) {
            AccountsRoute(
                userName = userName,
                onNavigate = { label -> navigateByLabel(nav, label, onLogout) }
            )
        }

        /* ---------- Accounts: Transactions ---------- */
        composable(
            route = Routes.ACCOUNT_TX,
            arguments = listOf(
                navArgument("accountId") { type = NavType.StringType },
                navArgument("accNo") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStack ->
            val id = backStack.arguments?.getString("accountId").orEmpty()
            val accNo = backStack.arguments?.getString("accNo").orEmpty()

            AccountTransactionsRoute(
                userName = userName,
                accountId = id,
                accountNumber = accNo.ifBlank { null },
                onNavigate = { label -> navigateByLabel(nav, label, onLogout) },
                onBack = { nav.navigateUp() }
            )
        }

        /* ---------- KYC ---------- */
        composable(Routes.KYC) {
            KycRoute(
                userName = userName,
                onNavigate = { label -> navigateByLabel(nav, label, onLogout) }
            )
        }

        /* ---------- KYC STATUS---------- */
        composable(Routes.KYC_STATUS) {
            KycStatusRoute(
                userName = userName,
                selectedItem = "KYC Status",
                onNavigate = { label -> navigateByLabel(nav, label, onLogout) },
            )
        }


        /* ---------- Payments ---------- */
        composable(Routes.PAYMENTS) {
            PaymentsRoute(
                userName = userName,
                selectedItem = "Payments",
                onNavigate = { label -> navigateByLabel(nav, label, onLogout) }
            )
        }

        /* ---------- Customer Registration ---------- */
        composable(Routes.CUSTOMER_REG) {
            CustomerRegistrationScreen(
                onBack = { nav.navigateUp() },
                onDone = { nav.popBackStack(Routes.HOME, inclusive = false) }
            )
        }

        /* ---------- Wallet ---------- */
        composable(WalletRoutes.WALLET_HOME){
            WalletRoute(
                userName = userName,
                selectedItem = "Wallet",
                onNavigate = { label -> navigateByLabel(nav, label, onLogout) }
            )
        }

    }
}

/* ---------------- helpers ---------------- */

private fun navigateByLabel(
    nav: NavHostController,
    label: String,
    onLogout: () -> Unit
) {
    if (label.contains('/')) {
        if (label.startsWith("accounts/tx/")) {
            nav.navigate(label)
        } else {
            nav.navigate(label) {
                popUpTo(nav.graph.startDestinationId) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
        return
    }

    val route = when (label) {
        "Home" -> Routes.HOME
        "Wallet" -> WalletRoutes.WALLET_HOME
        "Profile" -> Routes.PROFILE
        "Settings" -> Routes.SETTINGS
        "Payments" -> Routes.PAYMENTS
        "My Accounts" -> Routes.ACCOUNTS_MY
        "Open Account" -> Routes.ACCOUNTS_OPEN
        "Verify Identity", Routes.KYC -> Routes.KYC
        "KYC Status" -> Routes.KYC_STATUS
        "Logout" -> { onLogout(); return }
        else -> return
    }

    nav.navigate(route) {
        popUpTo(nav.graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

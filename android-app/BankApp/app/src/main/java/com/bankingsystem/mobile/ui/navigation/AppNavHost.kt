package com.bankingsystem.mobile.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bankingsystem.mobile.App
import com.bankingsystem.mobile.data.config.RetrofitClient
import com.bankingsystem.mobile.data.local.AuthStore
import com.bankingsystem.mobile.data.local.AuthStoreImpl
import com.bankingsystem.mobile.data.remote.AuthApiImpl
import com.bankingsystem.mobile.data.storage.TokenManager
import com.bankingsystem.mobile.ui.home.BankHomeScreen
import com.bankingsystem.mobile.ui.profile.ProfileRoute
import com.bankingsystem.mobile.ui.settings.SettingsScreen

@Composable
fun AppNavHost(
    userName: String,
    onLogout: () -> Unit,
    nav: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val app = context.applicationContext as? App

    // Prefer app-scoped store; fall back to local if needed
    val authStore: AuthStore = app?.authStore ?: remember { AuthStoreImpl(TokenManager(context)) }

    // Track whether Retrofit is ready
    var retrofitReady by remember { mutableStateOf(RetrofitClient.isInitialized()) }

    // Initialize Retrofit if needed (runs after first composition)
    LaunchedEffect(authStore) {
        if (!retrofitReady) {
            RetrofitClient.init(authStore = authStore)
            retrofitReady = true
        }
    }

    NavHost(navController = nav, startDestination = Routes.HOME) {

        composable(Routes.HOME) {
            BankHomeScreen(
                userName = userName,
                selectedItem = "Home",
                onNavigate = { label -> navigateByLabel(nav, label, onLogout) }
            )
        }

        composable(Routes.PROFILE) {
            if (!retrofitReady) {
                // Show a quick loader until Retrofit is initialized
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                // Only read apiService AFTER ready is true
                val apiService = remember { RetrofitClient.apiService }
                val authApi = remember { AuthApiImpl(apiService) }

                ProfileRoute(
                    api = authApi,
                    store = authStore,
                    onNavigate = { label -> navigateByLabel(nav, label, onLogout) }
                )
            }
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onLogout = onLogout,
                onBack = { nav.navigateUp() }
            )
        }

        composable(Routes.ACCOUNTS) { /* TODO */ }
        composable(Routes.PAYMENTS) { /* TODO */ }
    }
}

/* ---------------- helpers ---------------- */

private fun navigateByLabel(
    nav: NavHostController,
    label: String,
    onLogout: () -> Unit
) {
    val route = when (label) {
        "Home"     -> Routes.HOME
        "Profile"  -> Routes.PROFILE
        "Settings" -> Routes.SETTINGS
        "Accounts" -> Routes.ACCOUNTS
        "Payments" -> Routes.PAYMENTS
        "Logout"   -> {
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

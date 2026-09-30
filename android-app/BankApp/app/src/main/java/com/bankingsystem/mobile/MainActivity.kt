package com.bankingsystem.mobile

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import com.bankingsystem.mobile.core.modules.common.storage.LockPreferences
import com.bankingsystem.mobile.features.lock.interfaces.ui.AppLocker
import com.bankingsystem.mobile.features.auth.interfaces.ui.LoginScreen
import com.bankingsystem.mobile.features.auth.interfaces.ui.LoginState
import com.bankingsystem.mobile.features.auth.interfaces.ui.LoginViewModel
import com.bankingsystem.mobile.app.navigation.AppNavHost
import com.bankingsystem.mobile.features.auth.interfaces.ui.RegisterScreen
import com.bankingsystem.mobile.core.modules.common.designsystem.theme.BankAppTheme
import com.bankingsystem.mobile.features.wallet.interfaces.ui.CardsViewModel

@ExperimentalMaterial3Api
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private lateinit var lockPreferences: LockPreferences

    private val cardsVm: CardsViewModel by viewModels()

    private var walletReturnPending: Boolean = false

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        lockPreferences = LockPreferences(this)

        handleWalletReturnDeepLink(intent?.data)

        setContent {
            BankAppTheme {
                val loginViewModel: LoginViewModel = hiltViewModel()
                val loginState by loginViewModel.loginState.collectAsState()

                var lockerAuthenticated by remember { mutableStateOf(false) }
                var lockEnabled by remember { mutableStateOf(false) }
                var storedPin by remember { mutableStateOf("") }
                var showRegister by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) { loginViewModel.autoLogin() }

                LaunchedEffect(loginState) {
                    if (loginState is LoginState.Success) {
                        lockEnabled = lockPreferences.isLockEnabled()
                        storedPin = lockPreferences.getPin() ?: ""
                        lockerAuthenticated = !lockEnabled
                        showRegister = false

                        if (walletReturnPending) {
                            cardsVm.refresh()
                            walletReturnPending = false
                        }
                    } else {
                        lockerAuthenticated = false
                        lockEnabled = false
                        storedPin = ""
                    }
                }

                val doLogout: () -> Unit = {
                    lockerAuthenticated = false
                    lockEnabled = false
                    storedPin = ""
                    loginViewModel.logout()
                }

                when (val state = loginState) {
                    is LoginState.Success -> {
                        if (lockEnabled && !lockerAuthenticated) {
                            AppLocker(
                                correctPin = storedPin,
                                onAuthenticated = {
                                    lockerAuthenticated = true
                                    if (walletReturnPending) {
                                        cardsVm.refresh()
                                        walletReturnPending = false
                                    }
                                }
                            )
                        } else {
                            AppNavHost(
                                userName = state.username,
                                onLogout = doLogout
                            )
                        }
                    }
                    is LoginState.Error, LoginState.Idle, is LoginState.Loading -> {
                        if (showRegister) {
                            RegisterScreen(
                                onRegisterSuccess = { showRegister = false },
                                onNavigateToLogin = { showRegister = false }
                            )
                        } else {
                            LoginScreen(
                                onNavigate = { },
                                onNavigateToRegister = { showRegister = true }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleWalletReturnDeepLink(intent.data)
    }

    private fun handleWalletReturnDeepLink(uri: Uri?) {
        if (uri == null) return
        val isWalletReturn = uri.scheme == "mybank" &&
                uri.host == "wallet" &&
                (uri.path?.startsWith("/return") == true)
        if (!isWalletReturn) return

        // Optional: read params if needed later
        // val status = uri.getQueryParameter("status")
        // val pi = uri.getQueryParameter("pi")

        walletReturnPending = true
        runCatching { cardsVm.refresh() }
    }
}

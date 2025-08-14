package com.bankingsystem.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bankingsystem.mobile.data.storage.LockPreferences
import com.bankingsystem.mobile.ui.login.LoginScreen
import com.bankingsystem.mobile.ui.login.LoginState
import com.bankingsystem.mobile.ui.login.LoginViewModel
import com.bankingsystem.mobile.ui.login.LoginViewModelFactory
import com.bankingsystem.mobile.ui.navigation.AppNavHost
import com.bankingsystem.mobile.ui.splash.AnimatedSplashScreen
import com.bankingsystem.mobile.ui.theme.BankAppTheme
import com.bankingsystem.mobile.ui.register.RegisterScreen
import com.bankingsystem.mobile.ui.locker.AppLocker

class MainActivity : ComponentActivity() {
    private lateinit var lockPreferences: LockPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lockPreferences = LockPreferences(this)

        setContent {
            BankAppTheme {
                val loginViewModel: LoginViewModel = viewModel(factory = LoginViewModelFactory(this))
                val loginState by loginViewModel.loginState.collectAsState()

                var showSplash by remember { mutableStateOf(true) }
                var lockerAuthenticated by remember { mutableStateOf(false) }
                var lockEnabled by remember { mutableStateOf(false) }
                var storedPin by remember { mutableStateOf("") }
                var showRegister by remember { mutableStateOf(false) } // simple toggle for login/register

                // kick off auto-login once
                LaunchedEffect(Unit) { loginViewModel.autoLogin() }

                // react to login state
                LaunchedEffect(loginState) {
                    if (loginState is LoginState.Success) {
                        lockEnabled = lockPreferences.isLockEnabled()
                        storedPin = lockPreferences.getPin() ?: ""
                        lockerAuthenticated = !lockEnabled
                        // once logged in, hide register form if it was shown
                        showRegister = false
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

                if (showSplash) {
                    AnimatedSplashScreen { showSplash = false }
                } else {
                    when (val state = loginState) {
                        is LoginState.Success -> {
                            if (lockEnabled && !lockerAuthenticated) {
                                AppLocker(
                                    correctPin = storedPin,
                                    onAuthenticated = { lockerAuthenticated = true }
                                )
                            } else {
                                // ✅ Logged-in main app with Navigation Compose
                                AppNavHost(
                                    userName = state.username,
                                    onLogout = doLogout
                                )
                            }
                        }

                        is LoginState.Error, LoginState.Idle, is LoginState.Loading -> {
                            if (showRegister) {
                                RegisterScreen(
                                    onRegisterSuccess = {
                                        // after successful register, you might want to go to login
                                        showRegister = false
                                    },
                                    onNavigateToLogin = { showRegister = false }
                                )
                            } else {
                                LoginScreen(
                                    onNavigate = { /* optional */ },
                                    onNavigateToRegister = { showRegister = true }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

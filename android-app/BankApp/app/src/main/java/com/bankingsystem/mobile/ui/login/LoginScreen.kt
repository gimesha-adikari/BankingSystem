package com.bankingsystem.mobile.ui.login

import com.bankingsystem.mobile.ui.components.ButtonPrimary
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bankingsystem.mobile.ui.components.InputField
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onNavigate: (String) -> Unit,
    onNavigateToRegister: () -> Unit
) {

    val context = LocalContext.current
    val factory = remember { LoginViewModelFactory(context) }
    val viewModel: LoginViewModel = viewModel(factory = factory)

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var forgotOpen by remember { mutableStateOf(false) }
    var forgotEmail by remember { mutableStateOf("") }

    val loginState by viewModel.loginState.collectAsState()
    val forgotPasswordState by viewModel.forgotPasswordState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(loginState) {
        when (loginState) {
            is LoginState.Success -> {
                val role = (loginState as LoginState.Success).role
                onNavigate(
                    when (role) {
                        "CUSTOMER" -> "/customer/home"
                        "ADMIN" -> "/admin/home"
                        "TELLER" -> "/teller/home"
                        "MANAGER" -> "/manager/home"
                        else -> "/"
                    }
                )
                viewModel.resetLoginState()
            }
            is LoginState.Error -> {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar((loginState as LoginState.Error).error)
                    viewModel.resetLoginState()
                }
            }
            else -> {}
        }
    }

    // Handle forgot password state changes
    LaunchedEffect(forgotPasswordState) {
        when (forgotPasswordState) {
            is ForgotPasswordState.Success -> {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar((forgotPasswordState as ForgotPasswordState.Success).message)
                }
                forgotOpen = false
                forgotEmail = ""
                viewModel.resetForgotPasswordState()
            }
            is ForgotPasswordState.Error -> {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar((forgotPasswordState as ForgotPasswordState.Error).error)
                    viewModel.resetForgotPasswordState()
                }
            }
            else -> {}
        }
    }

    fun validate() = username.trim().isNotEmpty() && password.trim().isNotEmpty()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Log In",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            InputField(
                label = "User Name",
                value = username,
                onValueChange = { username = it },
                placeholder = "ex: Gimesha_13",
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            InputField(
                label = "Password",
                value = password,
                onValueChange = { password = it },
                placeholder = "••••••••",
                isPassword = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            TextButton(
                onClick = { forgotOpen = true },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Forgot Password?", color = MaterialTheme.colorScheme.primary)
            }

            Spacer(Modifier.height(24.dp))

            ButtonPrimary(
                onClick = { viewModel.loginUser(username.trim(), password) },
                enabled = validate() && loginState != LoginState.Loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (loginState == LoginState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Log In")
                }
            }

            Spacer(Modifier.height(12.dp))

            TextButton(
                onClick = onNavigateToRegister,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Don't have an account? Register")
            }
        }

        if (forgotOpen) {
            ForgotPasswordDialog(
                email = forgotEmail,
                onEmailChange = { forgotEmail = it },
                onSubmit = { viewModel.forgotPassword(forgotEmail) },
                onDismiss = {
                    forgotOpen = false
                    forgotEmail = ""
                    viewModel.resetForgotPasswordState()
                },
                isLoading = forgotPasswordState == ForgotPasswordState.Loading
            )
        }
    }
}


@Composable
fun ForgotPasswordDialog(
    email: String,
    onEmailChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
    isLoading: Boolean = false
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Forgot Password") },
        text = {
            Column {
                Text("Enter your email address to reset your password.")
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = onEmailChange,
                    label = { Text("Email") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onSubmit,
                enabled = !isLoading && email.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Submit")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("Cancel")
            }
        }
    )
}

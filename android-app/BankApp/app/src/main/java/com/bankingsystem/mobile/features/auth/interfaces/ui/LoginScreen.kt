package com.bankingsystem.mobile.features.auth.interfaces.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bankingsystem.mobile.core.modules.common.designsystem.ButtonPrimary
import com.bankingsystem.mobile.core.modules.common.designsystem.FadingAppBackground
import com.bankingsystem.mobile.core.modules.common.designsystem.InputField
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onNavigate: (String) -> Unit,
    onNavigateToRegister: () -> Unit
) {
    val viewModel: LoginViewModel = hiltViewModel()

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var forgotOpen by remember { mutableStateOf(false) }
    var forgotEmail by remember { mutableStateOf("") }

    val loginState by viewModel.loginState.collectAsState()
    val forgotPasswordState by viewModel.forgotPasswordState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val cs = MaterialTheme.colorScheme

    LaunchedEffect(loginState) {
        when (loginState) {
            is LoginState.Success -> {
                val role = (loginState as LoginState.Success).role
                onNavigate(
                    when (role) {
                        "CUSTOMER" -> "/customer/home"
                        "ADMIN"    -> "/admin/home"
                        "TELLER"   -> "/teller/home"
                        "MANAGER"  -> "/manager/home"
                        else       -> "/"
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

    Box(Modifier.fillMaxSize()) {
        FadingAppBackground()

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = Color.Transparent
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                GlassPanel(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 420.dp)
                        .padding(24.dp)
                ) {
                    Text(
                        "Log In",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White
                    )

                    Spacer(Modifier.height(14.dp))

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
                    ) { Text("Forgot Password?", color = cs.primary) }

                    Spacer(Modifier.height(20.dp))

                    ButtonPrimary(
                        onClick = { viewModel.loginUser(username.trim(), password) },
                        enabled = validate() && loginState != LoginState.Loading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (loginState == LoginState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = cs.onPrimary,
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
                    ) { Text("Don't have an account? Register") }
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
}

@Composable
fun ForgotPasswordDialog(
    email: String,
    onEmailChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
    isLoading: Boolean = false
) {
    val cs = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Forgot Password", color = cs.onSurface) },
        text = {
            Column {
                Text("Enter your email address to reset your password.", color = cs.onSurface)
                Spacer(Modifier.height(8.dp))
                InputField(
                    label = "Email",
                    value = email,
                    onValueChange = onEmailChange,
                    placeholder = "you@example.com",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onSubmit, enabled = !isLoading && email.isNotBlank()) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("Submit")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) { Text("Cancel") }
        }
    )
}

@Composable
private fun GlassPanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = cs.surface.copy(alpha = 0.20f),
        tonalElevation = 0.dp,
        shadowElevation = 12.dp
    ) {
        Box(
            Modifier
                .border(1.dp, cs.outline.copy(alpha = 0.15f), MaterialTheme.shapes.medium)
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                content = content
            )
        }
    }
}

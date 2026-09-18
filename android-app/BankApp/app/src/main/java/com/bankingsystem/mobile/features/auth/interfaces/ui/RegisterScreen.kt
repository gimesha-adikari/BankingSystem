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
import com.bankingsystem.mobile.core.modules.common.designsystem.FadingAppBackground
import com.bankingsystem.mobile.core.modules.common.designsystem.PasswordTextField
import com.bankingsystem.mobile.core.modules.common.designsystem.ValidatedTextField
import com.bankingsystem.mobile.core.modules.common.util.checkPasswordStrength
import com.bankingsystem.mobile.core.modules.common.util.doPasswordsMatch
import com.bankingsystem.mobile.core.modules.common.util.isValidEmail

@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
) {
    val viewModel: RegisterViewModel = hiltViewModel()

    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    val usernameAvailable by viewModel.usernameAvailable.collectAsState()
    val registerState by viewModel.registerState.collectAsState()

    val passwordStrength = checkPasswordStrength(password)
    val passwordsMatch = doPasswordsMatch(password, confirmPassword)
    val emailValid = isValidEmail(email)
    val cs = MaterialTheme.colorScheme

    LaunchedEffect(username) { viewModel.checkUsernameAvailability(username) }

    LaunchedEffect(registerState) {
        if (registerState is RegisterState.Success) {
            onRegisterSuccess()
            viewModel.resetRegisterState()
        }
    }

    fun validate(): Boolean =
        username.isNotBlank() &&
                usernameAvailable == true &&
                emailValid &&
                passwordStrength.score >= 4 &&
                passwordsMatch

    Box(Modifier.fillMaxSize()) {
        FadingAppBackground()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            GlassPanel(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp)
            ) {
                Text(
                    text = "Create Your Account",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(16.dp))

                ValidatedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = "Username",
                    isError = username.isNotBlank() && usernameAvailable == false,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    supportingText = {
                        when {
                            username.isBlank() -> Text("Username is required", color = Color.White.copy(alpha = 0.7f))
                            usernameAvailable == null -> Text("Checking availability...", color = Color.White.copy(alpha = 0.7f))
                            usernameAvailable == true -> Text("Username is available", color = Color.Green)
                            else -> Text("Username is taken or too short", color = cs.error)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                ValidatedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Email",
                    isError = email.isNotBlank() && !emailValid,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    supportingText = {
                        if (email.isNotBlank() && !emailValid) {
                            Text("Invalid email address", color = cs.error)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                PasswordTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = "Password",
                    isError = password.isNotBlank() && passwordStrength.score < 4,
                    supportingText = {
                        if (password.isNotBlank()) {
                            if (passwordStrength.issues.isEmpty()) {
                                Text("Strong password!", color = Color.Green)
                            } else {
                                Column { passwordStrength.issues.forEach { Text(it, color = cs.error) } }
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                PasswordTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = "Confirm Password",
                    isError = confirmPassword.isNotBlank() && !passwordsMatch,
                    supportingText = {
                        if (confirmPassword.isNotBlank()) {
                            if (passwordsMatch) Text("Passwords match", color = cs.primary)
                            else Text("Passwords do not match", color = cs.error)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(22.dp))

                Button(
                    onClick = { viewModel.registerUser(username.trim(), email.trim(), password) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = validate() && registerState != RegisterState.Loading
                ) {
                    if (registerState == RegisterState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = cs.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Register")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(onClick = onNavigateToLogin, modifier = Modifier.fillMaxWidth()) {
                    Text("Already have an account? Log In")
                }

                Spacer(modifier = Modifier.height(8.dp))

                when (registerState) {
                    is RegisterState.Error -> Text(
                        text = (registerState as RegisterState.Error).error,
                        color = cs.error
                    )
                    is RegisterState.Success -> Text(
                        text = (registerState as RegisterState.Success).message,
                        color = cs.primary
                    )
                    else -> {}
                }
            }
        }
    }
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
        Column(
            modifier = Modifier
                .border(1.dp, cs.outline.copy(alpha = 0.35f), MaterialTheme.shapes.large)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content
        )
    }
}

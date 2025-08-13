package com.bankingsystem.mobile.ui.register

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.unit.dp
import com.bankingsystem.mobile.ui.components.PasswordTextField
import com.bankingsystem.mobile.ui.components.ValidatedTextField
import com.bankingsystem.mobile.util.checkPasswordStrength
import com.bankingsystem.mobile.util.doPasswordsMatch
import com.bankingsystem.mobile.util.isValidEmail

/**
 * Composable function for the Register Screen.
 *
 * This screen allows users to create a new account by providing a username, email, and password.
 * It includes input validation for each field and checks for username availability.
 *
 * @param onRegisterSuccess Callback invoked when the user successfully registers.
 *                          Typically used for navigating to the next screen (e.g., Login or Home).
 * @param onNavigateToLogin Callback invoked when the user clicks the "Log In" button.
 *                          Typically used for navigating to the Login screen.
 */
@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
) {
    val context = LocalContext.current
    val factory = remember { RegisterViewModelFactory(context) }
    val viewModel: RegisterViewModel = viewModel(factory = factory)

    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    val usernameAvailable by viewModel.usernameAvailable.collectAsState()
    val registerState by viewModel.registerState.collectAsState()

    val passwordStrength = checkPasswordStrength(password)
    val passwordsMatch = doPasswordsMatch(password, confirmPassword)
    val emailValid = isValidEmail(email)

    LaunchedEffect(username) {
        viewModel.checkUsernameAvailability(username)
    }

    LaunchedEffect(registerState) {
        if (registerState is RegisterState.Success) {
            onRegisterSuccess()
            viewModel.resetRegisterState()
        }
    }

    fun validate(): Boolean {
        return (username.isNotBlank() && usernameAvailable == true
                && emailValid
                && passwordStrength.score >= 4
                && passwordsMatch)
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            tonalElevation = 6.dp,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .widthIn(max = 400.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Create Your Account",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                ValidatedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = "Username",
                    isError = usernameAvailable == false,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    supportingText = {
                        when {
                            username.isBlank() -> Text("Username is required", color = MaterialTheme.colorScheme.error)
                            usernameAvailable == null -> Text("Checking availability...")
                            usernameAvailable == true -> Text("Username is available", color = MaterialTheme.colorScheme.primary)
                            usernameAvailable == false -> Text("Username is taken or too short", color = MaterialTheme.colorScheme.error)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                ValidatedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Email",
                    isError = !emailValid && email.isNotBlank(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    supportingText = {
                        if (!emailValid && email.isNotBlank()) {
                            Text("Invalid email address", color = MaterialTheme.colorScheme.error)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                PasswordTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = "Password",
                    isError = passwordStrength.score < 4 && password.isNotBlank(),
                    supportingText = {
                        if (password.isNotBlank()) {
                            if (passwordStrength.issues.isEmpty()) {
                                Text("Strong password!", color = MaterialTheme.colorScheme.primary)
                            } else {
                                Column {
                                    passwordStrength.issues.forEach {
                                        Text(it, color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                PasswordTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = "Confirm Password",
                    isError = !passwordsMatch && confirmPassword.isNotBlank(),
                    supportingText = {
                        if (confirmPassword.isNotBlank()) {
                            if (passwordsMatch) {
                                Text("Passwords match", color = MaterialTheme.colorScheme.primary)
                            } else {
                                Text("Passwords do not match", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        viewModel.registerUser(username.trim(), email.trim(), password)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = validate() && registerState != RegisterState.Loading
                ) {
                    if (registerState == RegisterState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Register")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(
                    onClick = onNavigateToLogin,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Already have an account? Log In")
                }

                Spacer(modifier = Modifier.height(8.dp))

                when (registerState) {
                    is RegisterState.Error -> Text(
                        text = (registerState as RegisterState.Error).error,
                        color = MaterialTheme.colorScheme.error
                    )
                    is RegisterState.Success -> Text(
                        text = (registerState as RegisterState.Success).message,
                        color = MaterialTheme.colorScheme.primary
                    )
                    else -> {}
                }
            }
        }
    }
}

package com.bankingsystem.mobile.features.profile.interfaces.ui

import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun ProfileRoute(
    onNavigate: (String) -> Unit
) {
    val vm: ProfileViewModel = hiltViewModel()
    val ui by vm.ui.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(ui.snackbarMessage) {
        ui.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            vm.snackbarShown()
        }
    }

    ProfileScreen(
        profile = ui.profile,
        isLoading = ui.isLoading,
        errorMessage = ui.errorMessage,

        editingField = ui.editingField,

        tempValue = ui.tempValue,

        onEditClicked = vm::startEditing,
        onCancelEditing = vm::cancelEditing,
        onSaveEditing = vm::saveEditing,
        onValueChange = vm::onTempValueChange,
        onChangePasswordClick = vm::openChangePassword,

        selectedItem = "Profile",
        onNavigate = onNavigate,

        isCheckingUsername = ui.isCheckingUsername,
        isUsernameAvailable = ui.isUsernameAvailable
    )

    if (ui.showPasswordDialog) {
        PasswordVerificationDialog(
            onConfirm = { vm.onPasswordVerified(it) },
            onDismiss = { vm.dismissPasswordDialog() }
        )
    }

    if (ui.showChangePasswordDialog) {
        ChangePasswordDialog(
            onConfirm = { current, newPass, confirm ->
                vm.changePassword(current, newPass, confirm)
            },
            onDismiss = { vm.dismissChangePassword() }
        )
    }

    SnackbarHost(hostState = snackbarHostState)
}

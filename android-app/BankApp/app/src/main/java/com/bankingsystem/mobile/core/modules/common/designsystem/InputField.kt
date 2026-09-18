package com.bankingsystem.mobile.core.modules.common.designsystem

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation

@Composable
fun InputField(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    isPassword: Boolean = false,
    isError: Boolean = false,
    supportingText: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    var passwordVisible by remember { mutableStateOf(false) }
    val cs = MaterialTheme.colorScheme

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        singleLine = true,
        modifier = modifier,
        keyboardOptions = keyboardOptions,
        isError = isError,
        enabled = enabled,
        readOnly = readOnly,
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        leadingIcon = leadingIcon,
        trailingIcon = if (isPassword) {
            {
                val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = image,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password"
                    )
                }
            }
        } else trailingIcon,
        supportingText = supportingText,
        shape = MaterialTheme.shapes.medium,
        colors = TextFieldDefaults.colors(
            focusedTextColor = cs.onSurface,
            unfocusedTextColor = cs.onSurface,
            disabledTextColor = cs.onSurface.copy(alpha = 0.5f),
            errorTextColor = cs.onErrorContainer,
            focusedContainerColor = cs.surface,
            unfocusedContainerColor = cs.surface,
            disabledContainerColor = cs.surface.copy(alpha = 0.6f),
            errorContainerColor = cs.surface,
            cursorColor = cs.primary,
            focusedIndicatorColor = cs.primary,
            unfocusedIndicatorColor = cs.outline.copy(alpha = 0.6f),
            disabledIndicatorColor = cs.outline.copy(alpha = 0.3f),
            errorIndicatorColor = cs.error,
            focusedLabelColor = cs.primary,
            unfocusedLabelColor = cs.onSurfaceVariant,
            disabledLabelColor = cs.onSurfaceVariant.copy(alpha = 0.6f),
            errorLabelColor = cs.error,
            focusedPlaceholderColor = cs.onSurfaceVariant,
            unfocusedPlaceholderColor = cs.onSurfaceVariant,
            focusedLeadingIconColor = cs.onSurfaceVariant,
            unfocusedLeadingIconColor = cs.onSurfaceVariant,
            disabledLeadingIconColor = cs.onSurfaceVariant.copy(alpha = 0.5f),
            errorLeadingIconColor = cs.error,
            focusedTrailingIconColor = cs.onSurfaceVariant,
            unfocusedTrailingIconColor = cs.onSurfaceVariant,
            disabledTrailingIconColor = cs.onSurfaceVariant.copy(alpha = 0.5f),
            errorTrailingIconColor = cs.error
        )
    )
}

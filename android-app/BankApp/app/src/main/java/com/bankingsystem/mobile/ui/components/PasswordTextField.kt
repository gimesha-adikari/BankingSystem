package com.bankingsystem.mobile.ui.components

import android.annotation.SuppressLint
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation

/**
 * A composable function that creates a password text field with a visibility toggle.
 *
 * @param value The current value of the password text field.
 * @param onValueChange A callback that is invoked when the value of the password text field changes.
 * @param label The label to display for the password text field.
 * @param isError A boolean indicating whether the password text field is in an error state.
 * @param supportingText A composable function that displays supporting text for the password text field.
 * @param modifier A [Modifier] to apply to the password text field.
 */
@Composable
fun PasswordTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isError: Boolean,
    // This parameter allows for custom supporting text to be displayed below the text field.
    // It is nullable, so if no supporting text is needed, it can be omitted.
    supportingText: @Composable (() -> Unit)? = null,
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier
) {
    var passwordVisible by remember { mutableStateOf(false) }

    ValidatedTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        isError = isError,
        modifier = modifier,
        // Configures the keyboard to be suitable for password input.
        // This typically means that the keyboard will not show suggestions and may have a different layout.
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                Icon(imageVector = image, contentDescription = if (passwordVisible) "Hide password" else "Show password")
            }
        },
        supportingText = supportingText
    )
}

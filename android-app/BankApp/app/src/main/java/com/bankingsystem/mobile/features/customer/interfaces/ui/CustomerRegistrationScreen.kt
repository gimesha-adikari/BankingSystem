package com.bankingsystem.mobile.features.customer.interfaces.ui

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bankingsystem.mobile.core.modules.common.designsystem.FadingAppBackground

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerRegistrationScreen(
    onBack: () -> Unit,
    onDone: () -> Unit,
    vm: CustomerRegistrationViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsState()

    var genderMenu by remember { mutableStateOf(false) }
    var showDobPicker by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        FadingAppBackground()

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Customer Registration", color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    )
                )
            },
            bottomBar = {
                Column(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .imePadding()
                ) {
                    BottomEdgeScrim()
                    Button(
                        onClick = { vm.submit() },
                        enabled = ui.canSubmit && !ui.submitting,
                        modifier = Modifier
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) { Text(if (ui.submitting) "Submitting…" else "Submit") }
                }
            }
        ) { pv ->
            Column(
                modifier = Modifier
                    .padding(pv)
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                GlassPanel {
                    Text(
                        "My Profile",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Spacer(Modifier.height(8.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        IconTextField(
                            value = ui.form.firstName,
                            onValueChange = { s -> vm.update { it.copy(firstName = s) } },
                            label = "First name*",
                            leading = Icons.Filled.Person,
                            keyboardType = KeyboardType.Text
                        )

                        IconTextField(
                            value = ui.form.lastName,
                            onValueChange = { s -> vm.update { it.copy(lastName = s) } },
                            label = "Last name*",
                            leading = Icons.Filled.Person,
                            keyboardType = KeyboardType.Text
                        )

                        ExposedDropdownMenuBox(
                            expanded = genderMenu,
                            onExpandedChange = { genderMenu = !genderMenu }
                        ) {
                            OutlinedTextField(
                                value = ui.form.gender,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Gender*") },
                                leadingIcon = { Icon(Icons.Filled.Badge, null) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(genderMenu) },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                colors = fieldColors()
                            )
                            ExposedDropdownMenu(
                                expanded = genderMenu,
                                onDismissRequest = { genderMenu = false }
                            ) {
                                listOf("MALE", "FEMALE", "OTHER").forEach { opt ->
                                    DropdownMenuItem(
                                        text = { Text(opt.lowercase().replaceFirstChar { it.titlecase() }) },
                                        onClick = {
                                            vm.update { it.copy(gender = opt) }
                                            genderMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        IconTextField(
                            value = ui.form.email,
                            onValueChange = { s -> vm.update { it.copy(email = s) } },
                            label = "Email*",
                            leading = Icons.Filled.Email,
                            keyboardType = KeyboardType.Email
                        )

                        IconTextField(
                            value = ui.form.phone,
                            onValueChange = { s -> vm.update { it.copy(phone = s) } },
                            label = "Phone*",
                            leading = Icons.Filled.Phone,
                            keyboardType = KeyboardType.Phone
                        )

                        IconTextField(
                            value = ui.form.address,
                            onValueChange = { s -> vm.update { it.copy(address = s) } },
                            label = "Address",
                            leading = Icons.Filled.Home,
                            keyboardType = KeyboardType.Text
                        )

                        OutlinedTextField(
                            value = ui.form.dateOfBirth,
                            onValueChange = { /* read-only */ },
                            readOnly = true,
                            label = { Text("Date of birth*") },
                            leadingIcon = { Icon(Icons.Filled.CalendarToday, null) },
                            trailingIcon = {
                                IconButton(onClick = { showDobPicker = true }) {
                                    Icon(Icons.Filled.CalendarToday, contentDescription = "Pick date")
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = fieldColors(),
                            placeholder = { Text("YYYY-MM-DD") }
                        )

                        // Status (read-only)
                        OutlinedTextField(
                            value = ui.form.status,
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("Status") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = fieldColors()
                        )

                        Text(
                            "* Required fields",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Title contrast
        TopEdgeScrim()
    }

    // Date picker
    if (showDobPicker) {
        val state = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDobPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { ms ->
                        val local = java.time.Instant.ofEpochMilli(ms)
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate()
                        vm.update { it.copy(dateOfBirth = local.toString()) }
                    }
                    showDobPicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDobPicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = state) }
    }

    // Observe success / error
    LaunchedEffect(ui.success) { if (ui.success) onDone() }
    if (ui.error != null) {
        AlertDialog(
            onDismissRequest = vm::clearError,
            confirmButton = { TextButton(onClick = vm::clearError) { Text("OK") } },
            title = { Text("Submission failed") },
            text = { Text(ui.error ?: "") }
        )
    }
}

/* ===== visuals (same helpers you already use) ===== */

@Composable private fun GlassPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val cs = MaterialTheme.colorScheme
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(width = 1.dp,
                brush = Brush.linearGradient(listOf(cs.outline.copy(0.45f), cs.outline.copy(0.15f))),
                shape = RoundedCornerShape(24.dp)
            ),
        shape = RoundedCornerShape(24.dp),
        color = cs.surface.copy(alpha = 0.10f),
        tonalElevation = 0.dp,
        shadowElevation = 10.dp
    ) {
        Column(
            modifier = Modifier
                .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.06f), Color.Transparent)))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            content = content
        )
    }
}

@Composable private fun IconTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leading: androidx.compose.ui.graphics.vector.ImageVector,
    keyboardType: KeyboardType
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        label = { Text(label) },
        leadingIcon = { Icon(leading, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = fieldColors()
    )
}

@Composable private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = Color.White.copy(alpha = 0.10f),
    unfocusedContainerColor = Color.White.copy(alpha = 0.08f),
    disabledContainerColor = Color.White.copy(alpha = 0.06f),
    focusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    cursorColor = MaterialTheme.colorScheme.primary
)

@Composable private fun BoxScope.TopEdgeScrim(height: Int = 56) {
    Box(
        modifier = Modifier
            .align(Alignment.TopCenter)
            .fillMaxWidth()
            .height(height.dp)
            .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.28f), Color.Transparent)))
    )
}
@Composable private fun BottomEdgeScrim(height: Int = 16) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height.dp)
            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.22f))))
    )
}

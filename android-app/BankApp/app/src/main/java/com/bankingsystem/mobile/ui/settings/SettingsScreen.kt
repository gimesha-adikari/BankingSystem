package com.bankingsystem.mobile.ui.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bankingsystem.mobile.BuildConfig
import com.bankingsystem.mobile.data.storage.LockPreferences
import com.bankingsystem.mobile.ui.components.FadingAppBackground
import com.bankingsystem.mobile.ui.components.SectionCard
import com.bankingsystem.mobile.ui.locker.LockSetupScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onLogout: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lockPrefs = remember { LockPreferences(context) }

    var lockEnabled by remember { mutableStateOf(false) }
    var currentPin by remember { mutableStateOf<String?>(null) }
    var pendingEnable by remember { mutableStateOf(false) }
    var showLockSetup by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    // read persisted values once
    LaunchedEffect(Unit) {
        val enabled = withContext(Dispatchers.IO) { lockPrefs.isLockEnabled() }
        val pin = withContext(Dispatchers.IO) { lockPrefs.getPin() }
        lockEnabled = enabled
        currentPin = pin
        pendingEnable = enabled
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Box(Modifier.fillMaxSize()) {
        // Background behind everything
        FadingAppBackground()

        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            containerColor = Color.Transparent,
            topBar = {
                LargeTopAppBar(
                    title = { Text("Settings") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.largeTopAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    scrollBehavior = scrollBehavior
                )
            }
        ) { padding ->

            if (showLockSetup) {
                // Full-screen lock setup flow
                LockSetupScreen(
                    initialPin = currentPin,
                    onSavePin = { pin ->
                        lockPrefs.savePin(pin)
                        lockPrefs.setLockEnabled(true)
                        currentPin = pin
                        lockEnabled = true
                        pendingEnable = true
                        showLockSetup = false
                        Toast.makeText(context, "App lock enabled", Toast.LENGTH_SHORT).show()
                    },
                    onLockEnabledChange = { enabled ->
                        lockPrefs.setLockEnabled(enabled)
                        lockEnabled = enabled
                        pendingEnable = enabled
                        if (!enabled) {
                            lockPrefs.clearLock()
                            currentPin = null
                        }
                        showLockSetup = false
                    },
                    onCancel = {
                        // Revert UI toggle if user cancels
                        pendingEnable = lockEnabled
                        showLockSetup = false
                    }
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // --------- Hero header ----------
                    item {
                        SettingsHeader()
                    }

                    // --------- Security section ----------
                    item {
                        SectionCard(title = "Security", modifier = Modifier.fillMaxWidth()) {
                            // App Lock toggle
                            SettingRow(
                                icon = Icons.Filled.Lock,
                                title = "App Lock",
                                subtitle = if (pendingEnable) "Enabled" else "Disabled",
                                trailing = {
                                    Switch(
                                        checked = pendingEnable,
                                        onCheckedChange = { enabled ->
                                            if (enabled) {
                                                // Ask user to set/confirm PIN
                                                showLockSetup = true
                                                pendingEnable = true
                                            } else {
                                                // Disable immediately
                                                lockPrefs.setLockEnabled(false)
                                                lockPrefs.clearLock()
                                                lockEnabled = false
                                                currentPin = null
                                                pendingEnable = false
                                                Toast
                                                    .makeText(context, "App lock disabled", Toast.LENGTH_SHORT)
                                                    .show()
                                            }
                                        }
                                    )
                                }
                            )

                            // Change PIN action (only when enabled)
                            if (lockEnabled) {
                                Spacer(Modifier.height(6.dp))
                                SettingRow(
                                    icon = Icons.Filled.Key,
                                    title = "Change PIN",
                                    subtitle = "Update your lock code",
                                    onClick = { showLockSetup = true }
                                )
                            }
                        }
                    }

                    // --------- About section ----------
                    item {
                        SectionCard(title = "About", modifier = Modifier.fillMaxWidth()) {
                            SettingRow(
                                icon = Icons.Filled.Info,
                                title = "Version",
                                subtitle = BuildConfig.VERSION_NAME
                            )
                        }
                    }

                    // --------- Danger zone ----------
                    item {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            tonalElevation = 0.dp,
                            shadowElevation = 8.dp
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(
                                    "Danger Zone",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(Modifier.height(10.dp))
                                Button(
                                    onClick = { showLogoutDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Logout", color = MaterialTheme.colorScheme.onError)
                                }
                            }
                        }
                    }

                    item { Spacer(Modifier.height(8.dp)) }
                }
            }
        }

        // ------- Logout confirmation dialog -------
        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                title = { Text("Log out?") },
                text = { Text("You’ll need to log in again to access your account.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showLogoutDialog = false
                            onLogout()
                        }
                    ) { Text("Log out") }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutDialog = false }) { Text("Cancel") }
                }
            )
        }
    }
}

/* ---------------------------- Pieces ---------------------------- */

@Composable
private fun SettingsHeader() {
    val cs = MaterialTheme.colorScheme
    val gradient = Brush.linearGradient(listOf(cs.primary, cs.primaryContainer))

    Surface(
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 0.dp,
        shadowElevation = 8.dp
    ) {
        Box(
            Modifier
                .background(gradient)
                .padding(20.dp)
                .fillMaxWidth()
        ) {
            Column {
                Text(
                    "Settings",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = cs.onPrimary
                    )
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Manage app lock and app preferences",
                    style = MaterialTheme.typography.bodyMedium,
                    color = cs.onPrimary.copy(alpha = 0.85f)
                )
            }
        }
    }
}

/** A tidy row used inside SectionCard items */
@Composable
private fun SettingRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String? = null,
    trailing: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    ListItem(
        leadingContent = { Icon(icon, contentDescription = null) },
        headlineContent = { Text(title) },
        supportingContent = { if (!subtitle.isNullOrBlank()) Text(subtitle) },
        trailingContent = trailing,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent
        )
    )
}

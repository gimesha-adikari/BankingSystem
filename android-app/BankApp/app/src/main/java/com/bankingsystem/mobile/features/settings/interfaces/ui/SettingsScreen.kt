package com.bankingsystem.mobile.features.settings.interfaces.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bankingsystem.mobile.BuildConfig
import com.bankingsystem.mobile.core.modules.common.storage.LockPreferences
import com.bankingsystem.mobile.core.modules.common.designsystem.FadingAppBackground
import com.bankingsystem.mobile.features.lock.interfaces.ui.LockSetupScreen
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

    LaunchedEffect(Unit) {
        val enabled = withContext(Dispatchers.IO) { lockPrefs.isLockEnabled() }
        val pin = withContext(Dispatchers.IO) { lockPrefs.getPin() }
        lockEnabled = enabled
        currentPin = pin
        pendingEnable = enabled
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val cs = MaterialTheme.colorScheme

    Box(Modifier.fillMaxSize()) {
        FadingAppBackground()

        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            containerColor = Color.Transparent,
            topBar = {
                LargeTopAppBar(
                    title = {
                        Text(
                            "Settings",
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.largeTopAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                        actionIconContentColor = Color.White
                    ),
                    scrollBehavior = scrollBehavior
                )
            }
        ) { padding ->

            if (showLockSetup) {
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
                    item {
                        GlassPanel {
                            Text(
                                "Settings",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Manage app lock and app preferences",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }

                    item {
                        GlassGroup(title = "Security") {
                            SettingRow(
                                icon = Icons.Filled.Lock,
                                title = "App Lock",
                                subtitle = if (pendingEnable) "Enabled" else "Disabled",
                                trailing = {
                                    Switch(
                                        checked = pendingEnable,
                                        onCheckedChange = { enabled ->
                                            if (enabled) {
                                                showLockSetup = true
                                                pendingEnable = true
                                            } else {
                                                lockPrefs.setLockEnabled(false)
                                                lockPrefs.clearLock()
                                                lockEnabled = false
                                                currentPin = null
                                                pendingEnable = false
                                                Toast.makeText(context, "App lock disabled", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = cs.onPrimary,
                                            checkedTrackColor = cs.primary,
                                            uncheckedThumbColor = cs.onSurfaceVariant,
                                            uncheckedTrackColor = cs.onSurfaceVariant.copy(alpha = 0.4f)
                                        )
                                    )
                                }
                            )

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

                    item {
                        GlassGroup(title = "About") {
                            SettingRow(
                                icon = Icons.Filled.Info,
                                title = "Version",
                                subtitle = BuildConfig.VERSION_NAME
                            )
                        }
                    }

                    item {
                        GlassPanel(
                            accent = cs.error
                        ) {
                            Text(
                                "Danger Zone",
                                style = MaterialTheme.typography.titleMedium,
                                color = cs.error,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(10.dp))
                            Button(
                                onClick = { showLogoutDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = cs.error),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = cs.onError)
                                Spacer(Modifier.width(8.dp))
                                Text("Logout", color = cs.onError)
                            }
                        }
                    }

                    item { Spacer(Modifier.height(8.dp)) }
                }
            }
        }

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


@Composable
private fun GlassPanel(
    modifier: Modifier = Modifier,
    accent: Color? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val panelColor = cs.surface.copy(alpha = 0.10f)

    val borderBrush = if (accent == null) {
        Brush.linearGradient(listOf(cs.outline.copy(0.45f), cs.outline.copy(0.15f)))
    } else {
        Brush.linearGradient(listOf(accent.copy(0.50f), accent.copy(0.20f)))
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(width = 1.dp, brush = borderBrush, shape = RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        color = panelColor,
        tonalElevation = 0.dp,
        shadowElevation = 10.dp
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.06f), Color.Transparent)
                    )
                )
                .padding(20.dp),
            content = content
        )
    }
}

@Composable
private fun GlassGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    GlassPanel {
        Text(
            title,
            color = Color.White,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
        )
        Spacer(Modifier.height(10.dp))
        Column(content = content)
    }
}


@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    trailing: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val cs = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val pressed = interaction.collectIsPressedAsState().value
    val scale = if (pressed) 0.98f else 1f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = onClick != null, interactionSource = interaction, indication = null) {
                onClick?.invoke()
            }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(cs.primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = cs.primary)
        }

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, style = MaterialTheme.typography.bodyLarge)
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, color = Color.White.copy(alpha = 0.75f), style = MaterialTheme.typography.bodyMedium)
            }
        }

        if (trailing != null) {
            Spacer(Modifier.width(12.dp))
            trailing()
        }
    }
}

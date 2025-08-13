package com.bankingsystem.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun Sidebar(
    menuItems: List<Pair<String, ImageVector>> = listOf(
        "Home" to Icons.Filled.Home,
        "Accounts" to Icons.Filled.AccountBalance,
        "Payments" to Icons.Filled.Payment,
        "Profile" to Icons.Filled.Person,   // better icon for profile
        "Settings" to Icons.Filled.Settings,
        "Logout" to Icons.AutoMirrored.Filled.ExitToApp
    ),
    selectedItem: String = "Home",
    onItemClick: (String) -> Unit = {},
    userName: String? = null               // optional: shows header avatar/initial
) {
    val cs = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 18.dp)
    ) {
        // ---------- Header (gradient + avatar) ----------
        SidebarHeader(
            title = "Navigation",
            userName = userName
        )

        Spacer(Modifier.height(12.dp))

        // ---------- Items ----------
        val itemColors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = cs.primary.copy(alpha = 0.10f),
            unselectedContainerColor = Color.Transparent,
            selectedIconColor = cs.primary,
            unselectedIconColor = cs.onSurfaceVariant,
            selectedTextColor = cs.onSurface,
            unselectedTextColor = cs.onSurface
        )

        val itemShape = RoundedCornerShape(12.dp)

        menuItems.forEach { (title, icon) ->
            val selected = title == selectedItem
            // subtle border only when selected
            val border = if (selected) cs.primary.copy(alpha = 0.25f) else Color.Transparent

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = itemShape,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                color = Color.Transparent
            ) {
                NavigationDrawerItem(
                    label = { Text(title, style = MaterialTheme.typography.bodyLarge) },
                    selected = selected,
                    onClick = { onItemClick(title) },
                    icon = { androidx.compose.material3.Icon(icon, contentDescription = title) },
                    colors = itemColors,
                    shape = itemShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                        .then(
                            Modifier
                                .background(
                                    color = if (selected) cs.primary.copy(alpha = 0.10f) else Color.Transparent,
                                    shape = itemShape
                                )
                        )
                        .padding(horizontal = 6.dp)
                )
            }
        }

        Spacer(Modifier.weight(1f))

        // ---------- Footer (optional small print) ----------
        Text(
            text = "v1.0",
            color = cs.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun SidebarHeader(
    title: String,
    userName: String?
) {
    val cs = MaterialTheme.colorScheme
    val gradient = Brush.linearGradient(listOf(cs.primary, cs.primaryContainer))

    Surface(
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 0.dp,
        shadowElevation = 6.dp
    ) {
        Box(
            Modifier
                .background(gradient)
                .padding(horizontal = 14.dp, vertical = 16.dp)
                .fillMaxWidth()
        ) {
            Column {
                // avatar with initial (optional)
                if (!userName.isNullOrBlank()) {
                    val initial = remember(userName) {
                        userName.trim().firstOrNull()?.uppercase() ?: "?"
                    }
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(cs.onPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initial,
                            color = cs.onPrimary,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                }

                Text(
                    title,
                    color = cs.onPrimary,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                if (!userName.isNullOrBlank()) {
                    Text(
                        "Hi, $userName",
                        color = cs.onPrimary.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

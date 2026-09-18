package com.bankingsystem.mobile.core.modules.common.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun Sidebar(
    menuItems: List<Pair<String, ImageVector>> = listOf(
        "Home" to Icons.Filled.Home,
        "Wallet" to Icons.Filled.CreditCard,
        "My Accounts" to Icons.Filled.AccountBalance,
        "Open Account" to Icons.Filled.AddCircle,
        "Payments" to Icons.Filled.Payment,
        "Verify Identity" to Icons.Filled.Verified,
        "KYC Status" to Icons.Filled.Verified,
        "Profile" to Icons.Filled.Person,
        "Settings" to Icons.Filled.Settings,
        "Logout" to Icons.AutoMirrored.Filled.ExitToApp
    ),
    selectedItem: String = "Home",
    onItemClick: (String) -> Unit = {},
    userName: String? = null
) {
    val cs = MaterialTheme.colorScheme
    val panelGradient = Brush.verticalGradient(listOf(Color(0xFF0E1020), Color(0xFF0C0F1A)))
    val lightIndigo = Color(0xFFA5B4FC)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(panelGradient)
            .padding(horizontal = 14.dp, vertical = 18.dp)
    ) {
        SidebarHeader(userName = userName)
        Spacer(Modifier.height(12.dp))

        val itemShape = RoundedCornerShape(12.dp)
        val colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = Color.Transparent,
            unselectedContainerColor = Color.Transparent,
            selectedIconColor = Color.White,
            unselectedIconColor = lightIndigo.copy(alpha = 0.95f),
            selectedTextColor = Color.White,
            unselectedTextColor = lightIndigo
        )

        menuItems.forEach { (title, icon) ->
            val selected = title == selectedItem
            val interaction = remember { MutableInteractionSource() }
            val pressed = interaction.collectIsPressedAsState().value

            val selectedBrush = Brush.linearGradient(
                listOf(cs.primary.copy(alpha = 0.16f), cs.primary.copy(alpha = 0.08f))
            )
            val border = if (selected)
                BorderStroke(1.dp, Brush.linearGradient(listOf(cs.primary.copy(0.35f), cs.primary.copy(0.12f))))
            else null

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        val s = if (pressed) 0.98f else 1f
                        scaleX = s; scaleY = s
                    }
                    .padding(vertical = 3.dp),
                shape = itemShape,
                tonalElevation = 0.dp,
                shadowElevation = if (selected) 10.dp else 0.dp,
                border = border,
                color = Color.Transparent
            ) {
                Box(
                    modifier = Modifier.background(
                        brush = if (selected) selectedBrush else SolidColor(Color.Transparent),
                        shape = itemShape
                    )
                ) {
                    if (selected) {
                        Box(
                            Modifier
                                .padding(vertical = 10.dp)
                                .width(3.dp)
                                .height(24.dp)
                                .align(Alignment.CenterStart)
                                .background(cs.primary)
                        )
                    }
                    NavigationDrawerItem(
                        label = { Text(title, style = MaterialTheme.typography.bodyLarge) },
                        selected = selected,
                        onClick = { onItemClick(title) },
                        icon = {
                            val isLogout = title.equals("Logout", true)
                            val iconBg =
                                if (isLogout) cs.tertiary.copy(alpha = 0.15f)
                                else if (selected) cs.primary.copy(alpha = 0.12f)
                                else Color.Transparent
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(iconBg),
                                contentAlignment = Alignment.Center
                            ) {
                                val tint =
                                    if (isLogout) cs.tertiary
                                    else if (selected) Color.White
                                    else lightIndigo
                                Icon(icon, contentDescription = title, tint = tint)
                            }
                        },
                        colors = colors,
                        shape = itemShape,
                        interactionSource = interaction,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .padding(horizontal = 6.dp)
                    )
                }
            }
        }

        Spacer(Modifier.weight(1f))
        Text("© 2025 MyBank Inc.", color = cs.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun SidebarHeader(userName: String?) {
    val cs = MaterialTheme.colorScheme
    val gradient = Brush.linearGradient(listOf(cs.primary, cs.primaryContainer))

    Surface(shape = RoundedCornerShape(16.dp), tonalElevation = 0.dp, shadowElevation = 8.dp) {
        Row(
            Modifier
                .background(gradient)
                .padding(horizontal = 14.dp, vertical = 16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(cs.onPrimary.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.AccountBalance, contentDescription = null, tint = cs.onPrimary)
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    "MyBank",
                    color = cs.onPrimary,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    if (userName.isNullOrBlank()) "Secure Banking Panel" else "Hi, $userName",
                    color = cs.onPrimary.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

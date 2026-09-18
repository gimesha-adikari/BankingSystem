package com.bankingsystem.mobile.features.kyc.interfaces.ui

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bankingsystem.mobile.app.navigation.Routes
import com.bankingsystem.mobile.core.modules.common.designsystem.FadingAppBackground
import com.bankingsystem.mobile.core.modules.common.designsystem.Sidebar
import com.bankingsystem.mobile.features.kyc.domain.model.KycCheck

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun KycStatusRoute(
    userName: String,
    selectedItem: String = "KYC Status",
    onNavigate: (String) -> Unit = {}
) {
    val vm: KycViewModel = hiltViewModel()
    val status by vm.status.collectAsState()
    val checks by vm.checks.collectAsState()

    val isApproved = status?.status?.equals("APPROVED", ignoreCase = true) == true

    DisposableEffect(Unit) {
        vm.startPollingStatus()
        onDispose { vm.stopPollingStatus() }
    }

    val cs = MaterialTheme.colorScheme
    var isSidebarOpen by remember { mutableStateOf(false) }
    val sidebarWidth by animateDpAsState(
        targetValue = if (isSidebarOpen) 280.dp else 0.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "sidebar"
    )
    val scrimAlpha by animateFloatAsState(if (isSidebarOpen) 0.5f else 0f, label = "scrim")

    Box(Modifier.fillMaxSize()) {
        FadingAppBackground()

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                LargeTopAppBar(
                    title = { Text("KYC Status", color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = { isSidebarOpen = !isSidebarOpen }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Open navigation", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.largeTopAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                        actionIconContentColor = Color.White
                    )
                )
            },
            // NEW: show CTA only when approved
            bottomBar = {
                if (isApproved) {
                    Surface(tonalElevation = 1.dp) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { onNavigate(Routes.CUSTOMER_REG) },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Proceed to Customer Registration") }
                        }
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                StatusCard(
                    statusText = status?.status,
                    reason = status?.decisionReason
                )

                ChecksCard(checks = checks)
            }
        }

        // Sidebar + scrim
        if (isSidebarOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = scrimAlpha))
                    .clickable { isSidebarOpen = false }
            )
        }
        if (isSidebarOpen) {
            Sidebar(
                selectedItem = selectedItem,
                onItemClick = {
                    isSidebarOpen = false
                    onNavigate(it)
                },
                userName = userName
            )
        }
    }
}

/* ---------- Status card ---------- */

@Composable
private fun StatusCard(
    statusText: String?,
    reason: String?
) {
    val st = (statusText ?: "UNDER_REVIEW").uppercase()
    val terminal = st == "APPROVED" || st == "REJECTED"

    KycPanel {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Current status",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            StatusPill(text = st)
        }

        if (!terminal) {
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(4.dp))
            Text(
                "We’re reviewing your documents. You can keep using the app.",
                color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.bodyMedium
            )
        }

        ReasonList(reason)
    }
}

/* ---------- Checks card ---------- */

@Composable
private fun ChecksCard(checks: List<KycCheck>?) {
    if (checks.isNullOrEmpty()) return
    KycPanel {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Checks", color = Color.White, style = MaterialTheme.typography.titleMedium)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(checks) { c -> CheckRow(c) }
            }
        }
    }
}

@Composable
private fun CheckRow(c: KycCheck) {
    val icon = when (c.type.uppercase()) {
        "FACE_MATCH" -> Icons.Filled.Face
        "OCR_ID", "DOC_CLASS" -> Icons.Filled.CreditCard
        else -> Icons.Filled.Badge
    }
    val color = if (c.passed == true) Color(0xFF4CAF50) else Color(0xFFE53935)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = color)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(c.type.prettyLabel(), color = Color.White)
            Text(
                "Score: ${"%.2f".format(c.score)}",
                color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.bodySmall
            )
        }
        StatusPill(if (c.passed == true) "PASS" else "FAIL")
    }
}

/* ---------- small components ---------- */

@Composable
private fun StatusPill(text: String) {
    val color = when (text.uppercase()) {
        "APPROVED", "PASS" -> Color(0xFF2E7D32)
        "REJECTED", "FAIL" -> Color(0xFFC62828)
        else -> Color(0xFF1976D2)
    }
    Surface(
        color = color.copy(alpha = 0.18f),
        contentColor = Color.White,
        border = BorderStroke(1.dp, color),
        shape = RoundedCornerShape(999.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
private fun KycPanel(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White.copy(alpha = 0.07f),
        contentColor = Color.White,
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun ReasonList(reason: String?) {
    val items = reason
        ?.split(';')
        ?.map { it.trim() }
        ?.filter { it.isNotBlank() }
        ?.map { it.replace('_', ' ').lowercase().replaceFirstChar { ch -> ch.titlecase() } }
        .orEmpty()

    if (items.isNotEmpty()) {
        Spacer(Modifier.height(12.dp))
        Text("Reason:", color = Color.White, style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(6.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items.forEach {
                Text("• $it", color = Color.White.copy(alpha = 0.92f), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

/* ---------- tiny helpers ---------- */

private fun String.prettyLabel(): String =
    trim().replace('_', ' ').lowercase().replaceFirstChar { it.titlecase() }

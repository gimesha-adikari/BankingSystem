package com.bankingsystem.mobile.features.home.interfaces.ui

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bankingsystem.mobile.core.modules.common.designsystem.FadingAppBackground
import com.bankingsystem.mobile.core.modules.common.designsystem.QuickAction
import com.bankingsystem.mobile.core.modules.common.designsystem.Sidebar
import com.bankingsystem.mobile.features.home.interfaces.ui.model.UiTransaction

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class, ExperimentalLayoutApi::class)
@Composable
fun BankHomeScreen(
    userName: String = "Gimesha",
    balance: Double = 3500.0,
    transactions: List<UiTransaction> = listOf(
        UiTransaction ("1", "Starbucks", -5.75, "Aug 12"),
        UiTransaction("2", "Salary", 2000.00, "Aug 10"),
        UiTransaction("3", "Electricity Bill", -120.0, "Aug 08")
    ),
    selectedItem: String = "Home",
    onTransferClick: () -> Unit = {},
    onPayBillClick: () -> Unit = {},
    onDepositClick: () -> Unit = {},
    onNavigate: (String) -> Unit = {}
) {
    val cs = MaterialTheme.colorScheme
    var isSidebarOpen by remember { mutableStateOf(false) }
    val sidebarOffset by animateDpAsState(
        targetValue = if (isSidebarOpen) 0.dp else (-300).dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "sidebar"
    )
    val scrimAlpha by animateFloatAsState(if (isSidebarOpen) 0.5f else 0f, label = "scrim")
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Box(Modifier.fillMaxSize()) {
        FadingAppBackground()

        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            containerColor = Color.Transparent,
            topBar = {
                LargeTopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.AccountBalance, null, tint = Color.White)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "MyBank",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { isSidebarOpen = !isSidebarOpen }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Open navigation", tint = Color.White)
                        }
                    },
                    actions = {
                        IconButton(onClick = {}) {
                            Icon(Icons.Filled.Notifications, contentDescription = "Notifications", tint = Color.White)
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
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(cs.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = userName.firstOrNull()?.uppercase() ?: "?",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = cs.onPrimaryContainer
                            )
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "Welcome back,",
                            style = MaterialTheme.typography.bodyMedium.copy(color = Color.White.copy(alpha = 0.85f))
                        )
                        Text(
                            text = userName,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))

                BalanceCard(balance = balance, modifier = Modifier.fillMaxWidth())

                Spacer(Modifier.height(22.dp))

                GlassPanel {
                    Text(
                        text = "Quick Actions",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    )
                    Spacer(Modifier.height(12.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        maxItemsInEachRow = 2
                    ) {
                        QuickAction("Withdraw", Icons.Filled.RemoveCircle, onDepositClick)
                        QuickAction("Pay Bill", Icons.Filled.Lightbulb, onPayBillClick)
                        QuickAction("Transfer", Icons.AutoMirrored.Filled.Send, onTransferClick)
                        QuickAction("Deposit", Icons.Filled.AddCircle, onDepositClick)
                    }
                }

                Spacer(Modifier.height(22.dp))

                GlassPanel {
                    Text(
                        text = "Recent Transactions",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    )
                    Spacer(Modifier.height(8.dp))
                    TransactionListSwipe(transactions = transactions, snackbarHostState = snackbarHostState)
                }
            }
        }

        if (scrimAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(cs.scrim.copy(alpha = scrimAlpha))
                    .clickable { isSidebarOpen = false }
            )
        }

        Surface(
            modifier = Modifier
                .fillMaxHeight()
                .width(300.dp)
                .offset(x = sidebarOffset),
            tonalElevation = 0.dp,
            shadowElevation = 16.dp,
            shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
        ) {
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

@Composable
private fun GlassPanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = cs.surface.copy(alpha = 0.10f),
        tonalElevation = 0.dp,
        shadowElevation = 12.dp
    ) {
        Column(
            modifier = Modifier
                .border(1.dp, cs.outline.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
                .padding(16.dp),
            content = content
        )
    }
}

package com.bankingsystem.mobile.features.wallet.interfaces.ui

import android.annotation.SuppressLint
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import com.bankingsystem.mobile.core.modules.common.designsystem.FadingAppBackground
import com.bankingsystem.mobile.core.modules.common.designsystem.Sidebar
import kotlinx.coroutines.launch
import java.util.UUID

@SuppressLint("ConfigurationScreenWidthHeight")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletRoute(
    userName: String,
    selectedItem: String = "Wallet",
    onNavigate: (String) -> Unit
) {
    val config = LocalConfiguration.current
    val isCompact = config.screenWidthDp < 600

    var screen by remember { mutableStateOf(WalletScreen.HOME) }
    var uiState by remember { mutableStateOf(WalletUiState()) }

    val cardsVm = hiltViewModel<CardsViewModel>()
    val billersVm = hiltViewModel<BillersViewModel>()
    val flowVm = hiltViewModel<PaymentFlowViewModel>()

    val flowState by flowVm.state.collectAsState()
    val cardsState by cardsVm.state.collectAsState()
    val billersState by billersVm.state.collectAsState()

    LaunchedEffect(cardsState.loading, cardsState.error, cardsState.cards) {
        uiState = uiState.copy(
            isLoadingCards = cardsState.loading,
            lastError = cardsState.error,
            cards = cardsState.cards.map { it.toWalletUi() }
        )
    }

    LaunchedEffect(flowState.stage) {
        if (flowState.stage == PaymentStage.SUCCEEDED || flowState.stage == PaymentStage.FAILED || flowState.stage == PaymentStage.CANCELED) {
            screen = WalletScreen.RESULT
        }
    }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current

    val topBar: @Composable () -> Unit = {
        TopAppBar(
            title = { Text(selectedItem, color = Color.White) },
            navigationIcon = {
                if (screen != WalletScreen.HOME) {
                    IconButton(onClick = { screen = WalletScreen.HOME }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                }
            },
            actions = {
                if (isCompact) {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                        Icon(Icons.Filled.Menu, contentDescription = "Menu", tint = Color.White)
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent,
                titleContentColor = Color.White,
                navigationIconContentColor = Color.White,
                actionIconContentColor = Color.White
            )
        )
    }

    val content: @Composable (PaddingValues) -> Unit = { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (screen) {
                WalletScreen.HOME -> WalletHomeScreen(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                    state = uiState,
                    onOpenCards = { screen = WalletScreen.CARDS },
                    onAddCard = { screen = WalletScreen.ADD_CARD },
                    onScanQr = { screen = WalletScreen.QR_SCAN },
                    onReload = { screen = WalletScreen.RELOAD },
                    onBiller = { screen = WalletScreen.BILLERS }
                )
                WalletScreen.CARDS -> CardListScreen(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                    state = uiState,
                    onAddCard = { screen = WalletScreen.ADD_CARD },
                    onBack = { screen = WalletScreen.HOME },
                    onMakeDefault = { id -> cardsVm.makeDefault(id) },
                    onDelete = { id -> cardsVm.delete(id) }
                )
                WalletScreen.ADD_CARD -> AddCardScreen(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                    onCancel = { screen = WalletScreen.CARDS },
                    onContinueSecurely = {
                        cardsVm.launchAddCardSession(
                            onUrl = { url ->
                                CustomTabsIntent.Builder().build().launchUrl(ctx, url.toUri())
                            },
                            onError = {}
                        )
                    }
                )
                WalletScreen.QR_SCAN -> QrPayScanScreen(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                    onCancel = { screen = WalletScreen.HOME },
                    onScanned = { parsed ->
                        uiState = uiState.copy(qrPreview = parsed)
                        screen = WalletScreen.QR_CONFIRM
                    }
                )
                WalletScreen.QR_CONFIRM -> QrPayConfirmScreen(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                    state = uiState,
                    onChangeCard = { screen = WalletScreen.CARDS },
                    onConfirm = {
                        val p = uiState.qrPreview
                        val idem = UUID.randomUUID().toString()
                        flowVm.startQr(p, idem)
                        screen = WalletScreen.PROCESSING
                    },
                    onCancel = { screen = WalletScreen.HOME }
                )
                WalletScreen.RELOAD -> MobileReloadScreen(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                    error = flowState.error,
                    onDismissError = flowVm::clearError,
                    onConfirm = { msisdn, amount ->
                        val idem = UUID.randomUUID().toString()
                        flowVm.startReload(msisdn, amount, idem)
                        screen = WalletScreen.PROCESSING
                    },
                    onCancel = { screen = WalletScreen.HOME }
                )
                WalletScreen.BILLERS -> BillersListScreen(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                    state = billersState,
                    onQueryChange = billersVm::setQuery,
                    onPick = { screen = WalletScreen.BILL_PAY },
                    onCancel = { screen = WalletScreen.HOME }
                )
                WalletScreen.BILL_PAY -> BillerPayScreen(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                    error = flowState.error,
                    onDismissError = flowVm::clearError,
                    onConfirm = { billerId, reference, amount ->
                        val idem = UUID.randomUUID().toString()
                        flowVm.startBill(billerId, reference, amount, idem)
                        screen = WalletScreen.PROCESSING
                    },
                    onCancel = { screen = WalletScreen.HOME }
                )
                WalletScreen.PROCESSING -> ProcessingScreen(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                    onDone = { screen = WalletScreen.RESULT },
                    onBackHome = { screen = WalletScreen.HOME }
                )
                WalletScreen.RESULT -> PaymentResultScreen(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                    onDone = { screen = WalletScreen.HOME }
                )
            }
        }
    }

    if (isCompact) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(drawerContainerColor = Color(0xFF0B0B12)) {
                    Sidebar(
                        selectedItem = selectedItem,
                        onItemClick = { label ->
                            scope.launch { drawerState.close() }
                            onNavigate(label)
                        },
                        userName = userName
                    )
                }
            }
        ) {
            Box(Modifier.fillMaxSize()) {
                FadingAppBackground()
                Scaffold(containerColor = Color.Transparent, topBar = topBar, content = content)
            }
        }
    } else {
        Row(Modifier.fillMaxSize()) {
            Surface(
                modifier = Modifier.fillMaxHeight().width(300.dp)
            ) {
                Sidebar(
                    selectedItem = selectedItem,
                    onItemClick = onNavigate,
                    userName = userName
                )
            }
            Box(Modifier.weight(1f).fillMaxHeight()) {
                FadingAppBackground()
                Scaffold(containerColor = Color.Transparent, topBar = topBar, content = content)
            }
        }
    }
}

private enum class WalletScreen {
    HOME, CARDS, ADD_CARD, QR_SCAN, QR_CONFIRM, RELOAD, BILLERS, BILL_PAY, PROCESSING, RESULT
}

private fun com.bankingsystem.mobile.features.wallet.domain.model.Card.toWalletUi() =
    WalletCardUi(
        id = id,
        brand = brand,
        last4 = last4,
        expMonth = 0,
        expYear = 0,
        isDefault = isDefault
    )

package com.bankingsystem.mobile.features.payments.interfaces.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.bankingsystem.mobile.core.modules.common.designsystem.FadingAppBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentsRoute(
    userName: String,
    selectedItem: String = "Payments",
    onNavigate: (String) -> Unit
) {
    var screen by remember { mutableStateOf(PaymentScreen.PICKER) }
    var draft by remember { mutableStateOf(PaymentDraft()) }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Box(Modifier.fillMaxSize()) {
        FadingAppBackground()

        Scaffold(
            containerColor = Color.Transparent,
            modifier = Modifier,
            topBar = {
                LargeTopAppBar(
                    title = { Text(selectedItem, color = Color.White) },
                    navigationIcon = {
                        if (screen != PaymentScreen.PICKER) {
                            IconButton(onClick = { screen = PaymentScreen.PICKER }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.largeTopAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent,
                        titleContentColor = Color.White
                    ),
                    scrollBehavior = scrollBehavior
                )
            }
        ) { padding ->
            when (screen) {
                PaymentScreen.PICKER -> PaymentsPicker(
                    modifier = Modifier.padding(padding),
                    onTransfer = { screen = PaymentScreen.TRANSFER },
                    onDeposit = { screen = PaymentScreen.DEPOSIT },
                    onWithdraw = { screen = PaymentScreen.WITHDRAW }
                )
                PaymentScreen.TRANSFER -> TransferScreen(
                    modifier = Modifier.padding(padding).verticalScroll(rememberScrollState()),
                    onReview = {
                        draft = it
                        screen = PaymentScreen.REVIEW
                    }
                )
                PaymentScreen.DEPOSIT -> DepositScreen(
                    modifier = Modifier.padding(padding).verticalScroll(rememberScrollState()),
                    onReview = {
                        draft = it
                        screen = PaymentScreen.REVIEW
                    }
                )
                PaymentScreen.WITHDRAW -> WithdrawScreen(
                    modifier = Modifier.padding(padding).verticalScroll(rememberScrollState()),
                    onReview = {
                        draft = it
                        screen = PaymentScreen.REVIEW
                    }
                )
                PaymentScreen.REVIEW -> PaymentReviewScreen(
                    modifier = Modifier.padding(padding),
                    draft = draft,
                    onEdit = {
                        screen = when (draft.kind) {
                            PaymentKind.TRANSFER -> PaymentScreen.TRANSFER
                            PaymentKind.DEPOSIT -> PaymentScreen.DEPOSIT
                            PaymentKind.WITHDRAW -> PaymentScreen.WITHDRAW
                            else -> PaymentScreen.PICKER
                        }
                    },
                    onConfirm = { screen = PaymentScreen.RECEIPT }
                )
                PaymentScreen.RECEIPT -> PaymentReceiptScreen(
                    modifier = Modifier.padding(padding),
                    draft = draft,
                    onDone = { screen = PaymentScreen.PICKER }
                )
            }
        }
    }
}

private enum class PaymentScreen { PICKER, TRANSFER, DEPOSIT, WITHDRAW, REVIEW, RECEIPT }

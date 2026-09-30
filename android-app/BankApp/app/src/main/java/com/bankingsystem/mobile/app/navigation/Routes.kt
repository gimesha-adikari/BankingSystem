package com.bankingsystem.mobile.app.navigation

import android.net.Uri

object Routes {
    const val HOME = "home"
    const val PROFILE = "profile"
    const val PAYMENTS = "payments"
    const val SETTINGS = "settings"
    const val ACCOUNTS_MY = "accounts/my"
    const val ACCOUNTS_OPEN = "accounts/open"
    const val ACCOUNT_TX    = "accounts/tx/{accountId}?accNo={accNo}"
    const val KYC = "kyc"
    const val KYC_STATUS = "kyc/status"
    const val CUSTOMER_REG = "customer/register"

    fun accountTx(accountId: String, accountNumber: String? = null): String {
        val accNo = accountNumber?.let { Uri.encode(it) } ?: ""
        return "accounts/tx/$accountId?accNo=$accNo"
    }
}

object WalletRoutes {
    const val WALLET_HOME = "wallet"
    const val WALLET_CARDS = "wallet/cards"
    const val WALLET_ADD_CARD = "wallet/cards/add"
    const val WALLET_QR_SCAN = "wallet/qr/scan"
    const val WALLET_QR_CONFIRM = "wallet/qr/confirm"
    const val WALLET_RELOAD = "wallet/reload"
    const val WALLET_BILLERS = "wallet/billers"
    const val WALLET_BILL_PAY = "wallet/bill/pay"
    const val WALLET_PROCESSING = "wallet/processing"
    const val WALLET_RESULT = "wallet/result"
}

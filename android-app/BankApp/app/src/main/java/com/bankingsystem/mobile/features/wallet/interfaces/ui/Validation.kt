package com.bankingsystem.mobile.features.wallet.interfaces.ui

object WalletValidators {
    fun msisdnError(text: String): String? {
        val digits = text.filter { it.isDigit() }
        if (digits.isEmpty()) return "Required"
        if (digits.length !in 9..12) return "Enter 9–12 digits"
        return null
    }

    fun amountError(text: String): String? {
        val norm = text.trim().replace(",", "")
        if (norm.isEmpty()) return "Required"
        val v = norm.toDoubleOrNull() ?: return "Invalid amount"
        if (v <= 0.0) return "Must be greater than 0"
        if (v > 1_000_000) return "Too large"
        val decimals = norm.substringAfter('.', "").length
        if (decimals > 2) return "Max 2 decimals"
        return null
    }

    fun billerIdError(text: String): String? = if (text.isBlank()) "Required" else null
    fun referenceError(text: String): String? = if (text.isBlank()) "Required" else null

    fun formatAmountInput(input: String): String {
        if (input.isEmpty()) return ""
        val cleaned = buildString {
            var dot = false
            for (ch in input) {
                if (ch.isDigit()) append(ch)
                else if (ch == '.' && !dot) { append('.'); dot = true }
            }
        }
        val withZero = if (cleaned.startsWith(".")) "0$cleaned" else cleaned
        val idx = withZero.indexOf('.')
        if (idx >= 0) {
            val max = (idx + 3).coerceAtMost(withZero.length)
            return withZero.substring(0, max)
        }
        return withZero
    }
}

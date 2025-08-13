package com.bankingsystem.mobile.data.storage

import android.content.Context
import androidx.core.content.edit

class LockPreferences(private val context: Context) {

    private val prefs = context.getSharedPreferences("app_lock_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_LOCK_ENABLED = "lock_enabled"
        private const val KEY_LOCK_PIN = "lock_pin"
    }

    fun isLockEnabled(): Boolean = prefs.getBoolean(KEY_LOCK_ENABLED, false)

    fun setLockEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_LOCK_ENABLED, enabled) }
    }

    // Optional: Save and get PIN (not recommended in plain text, use encryption in real app)
    fun savePin(pin: String) {
        prefs.edit { putString(KEY_LOCK_PIN, pin) }
        setLockEnabled(true)
    }

    fun getPin(): String? = prefs.getString(KEY_LOCK_PIN, null)

    fun clearLock() {
        prefs.edit { remove(KEY_LOCK_ENABLED).remove(KEY_LOCK_PIN) }
    }
}

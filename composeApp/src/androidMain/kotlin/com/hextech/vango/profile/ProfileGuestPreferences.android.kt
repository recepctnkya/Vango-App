package com.hextech.vango.profile

import android.content.Context
import com.hextech.vango.getAppContext

actual object ProfileGuestPreferences {
    private const val PREFS = "vango_profile_guest"
    private const val KEY = "allow_direct_profile"
    private const val KEY_GUEST_BLE_WARN_DISMISSED = "guest_ble_warn_dismissed"

    private fun prefs() =
        (getAppContext().get() as Context).getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    actual fun getAllowDirectProfile(): Boolean = prefs().getBoolean(KEY, false)

    actual fun setAllowDirectProfile(value: Boolean) {
        prefs().edit().putBoolean(KEY, value).apply()
    }

    actual fun getGuestBleWarnDismissed(): Boolean =
        prefs().getBoolean(KEY_GUEST_BLE_WARN_DISMISSED, false)

    actual fun setGuestBleWarnDismissed(value: Boolean) {
        prefs().edit().putBoolean(KEY_GUEST_BLE_WARN_DISMISSED, value).apply()
    }
}

package com.hextech.vango.profile

actual object ProfileGuestPreferences {
    private var allow = false
    private var guestBleWarnDismissed = false

    actual fun getAllowDirectProfile(): Boolean = allow
    actual fun setAllowDirectProfile(value: Boolean) {
        allow = value
    }

    actual fun getGuestBleWarnDismissed(): Boolean = guestBleWarnDismissed
    actual fun setGuestBleWarnDismissed(value: Boolean) {
        guestBleWarnDismissed = value
    }
}

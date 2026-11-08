package com.hextech.vango.profile

/**
 * When true, opening Profile from settings main skips the login screen (misafir akışı).
 * Cleared on credential sign-in / sign-out from profile.
 *
 * [getGuestBleWarnDismissed] hides the guest-BLE inbox notice until the next BLE link.
 */
expect object ProfileGuestPreferences {
    fun getAllowDirectProfile(): Boolean
    fun setAllowDirectProfile(value: Boolean)
    fun getGuestBleWarnDismissed(): Boolean
    fun setGuestBleWarnDismissed(value: Boolean)
}

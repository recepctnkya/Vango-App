package com.hextech.vango

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.hextech.vango.profile.LoginPreferences
import com.hextech.vango.profile.ProfileGuestPreferences
import com.hextech.vango.profile.hasUsableAuthToken

/** Inbox / badge id for guest-mode BLE control notice. */
const val GUEST_BLE_WARN_RULE_ID = "guest_ble"
const val GUEST_BLE_WARN_LIVE_KEY = "live_guest_ble"

/**
 * Guest BLE warning as a dismissible inbox notification (not a sticky dashboard banner).
 * Reappears on the next BLE link after dismiss / logout.
 */
object GuestBleWarn {
    var dismissedTick by mutableStateOf(0)
        private set

    fun isGuestSession(): Boolean =
        !LoginPreferences.hasUsableAuthToken()

    fun isBleLinked(): Boolean =
        isBleGattLinkReady() || appConnectionState.value == AppConnectionState.BLE_CONNECTED

    fun shouldShow(): Boolean {
        if (!isGuestSession() || !isBleLinked()) return false
        return !ProfileGuestPreferences.getGuestBleWarnDismissed()
    }

    fun isUnread(): Boolean =
        shouldShow() && !AlertReadStore.isRead(GUEST_BLE_WARN_LIVE_KEY)

    fun dismiss() {
        ProfileGuestPreferences.setGuestBleWarnDismissed(true)
        AlertReadStore.markRead(GUEST_BLE_WARN_LIVE_KEY)
        dismissedTick++
    }

    fun markRead() {
        AlertReadStore.markRead(GUEST_BLE_WARN_LIVE_KEY)
        dismissedTick++
    }

    /** Call when BLE link drops so the notice can fire again on reconnect. */
    fun onBleLinkLost() {
        if (ProfileGuestPreferences.getGuestBleWarnDismissed()) {
            ProfileGuestPreferences.setGuestBleWarnDismissed(false)
            AlertReadStore.clear(GUEST_BLE_WARN_LIVE_KEY)
            dismissedTick++
        }
    }

    /** Rising edge: guest + BLE just became linked → ensure unread again. */
    fun onGuestBleLinked() {
        if (!isGuestSession()) return
        ProfileGuestPreferences.setGuestBleWarnDismissed(false)
        AlertReadStore.clear(GUEST_BLE_WARN_LIVE_KEY)
        dismissedTick++
    }
}

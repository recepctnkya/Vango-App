package com.hextech.vango

import com.hextech.vango.profile.normalizeMacForDisplay

/** UI otomasyonu / ADB testleri için Compose testTag sabitleri. */
object VanGoTestTags {
    const val FOOTER_MENU = "footer_menu"
    const val MENU_CONNECTION = "menu_connection"
    const val CONNECTION_BLE_PAIR_OPEN = "connection_ble_pair_open"
    const val CONNECTION_REMOTE_MODE = "connection_remote_mode"
    const val CONNECTION_REMOTE_MANAGE = "connection_remote_manage"
    const val BLE_PAIR_SEARCH = "ble_pair_search"
    const val REMOTE_CONNECT = "remote_connect"
    const val BLE_PAIR_REMOVE_CONFIRM = "ble_pair_remove_confirm"

    fun blePairConnect(mac: String): String =
        "ble_pair_connect:${normalizeMacForDisplay(mac)}"

    fun blePairRemove(mac: String): String =
        "ble_pair_remove:${normalizeMacForDisplay(mac)}"
}

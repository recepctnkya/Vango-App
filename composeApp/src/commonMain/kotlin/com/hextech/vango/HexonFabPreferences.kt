package com.hextech.vango

/**
 * Persist whether the Hexon FAB is shown on the phone home screen.
 * Default: visible (true).
 */
expect object HexonFabPreferences {
    fun isFabVisibleOnHome(): Boolean
    fun setFabVisibleOnHome(visible: Boolean)
}

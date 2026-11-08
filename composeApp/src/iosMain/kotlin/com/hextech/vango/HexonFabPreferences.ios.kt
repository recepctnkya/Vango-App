package com.hextech.vango

import platform.Foundation.NSUserDefaults

actual object HexonFabPreferences {
    private const val KEY_FAB_HOME = "fab_visible_on_home"

    actual fun isFabVisibleOnHome(): Boolean {
        val defaults = NSUserDefaults.standardUserDefaults
        if (defaults.objectForKey(KEY_FAB_HOME) == null) return true
        return defaults.boolForKey(KEY_FAB_HOME)
    }

    actual fun setFabVisibleOnHome(visible: Boolean) {
        NSUserDefaults.standardUserDefaults.setBool(visible, KEY_FAB_HOME)
    }
}

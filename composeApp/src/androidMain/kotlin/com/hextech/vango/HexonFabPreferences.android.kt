package com.hextech.vango

import android.content.Context
import android.content.SharedPreferences

actual object HexonFabPreferences {
    private const val PREFS = "vango_hexon_prefs"
    private const val KEY_FAB_HOME = "fab_visible_on_home"

    private fun prefs(): SharedPreferences {
        val ctx = getAppContext().get() as Context
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    actual fun isFabVisibleOnHome(): Boolean =
        prefs().getBoolean(KEY_FAB_HOME, true)

    actual fun setFabVisibleOnHome(visible: Boolean) {
        prefs().edit().putBoolean(KEY_FAB_HOME, visible).apply()
    }
}

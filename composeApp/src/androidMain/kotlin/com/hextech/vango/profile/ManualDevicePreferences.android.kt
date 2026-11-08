package com.hextech.vango.profile

import android.content.Context
import com.hextech.vango.getAppContext

actual object ManualDevicePreferences {
    private const val PREFS = "vango_manual_devices_prefs"
    private const val KEY_JSON = "manual_devices_json"

    private fun prefs() =
        (getAppContext().get() as Context).getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    actual fun getJson(): String? = prefs().getString(KEY_JSON, null)

    actual fun setJson(value: String?) {
        prefs().edit().putString(KEY_JSON, value).apply()
    }
}

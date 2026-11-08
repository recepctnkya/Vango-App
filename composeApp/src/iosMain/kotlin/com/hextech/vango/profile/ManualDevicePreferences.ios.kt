package com.hextech.vango.profile

import platform.Foundation.NSUserDefaults

actual object ManualDevicePreferences {
    private const val KEY_JSON = "manual_devices_json"

    actual fun getJson(): String? = NSUserDefaults.standardUserDefaults.stringForKey(KEY_JSON)

    actual fun setJson(value: String?) {
        val defaults = NSUserDefaults.standardUserDefaults
        if (value == null) {
            defaults.removeObjectForKey(KEY_JSON)
        } else {
            defaults.setObject(value, KEY_JSON)
        }
        defaults.synchronize()
    }
}

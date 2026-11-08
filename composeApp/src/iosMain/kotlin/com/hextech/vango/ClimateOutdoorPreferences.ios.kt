package com.hextech.vango

import platform.Foundation.NSUserDefaults

actual object ClimateOutdoorPreferences {
    private const val KEY = "climate_outdoor_source"

    actual fun getSource(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(KEY)

    actual fun setSource(source: String) {
        NSUserDefaults.standardUserDefaults.setObject(source, KEY)
    }
}

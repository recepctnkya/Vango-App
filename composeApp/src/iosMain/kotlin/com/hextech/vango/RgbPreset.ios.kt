package com.hextech.vango

import platform.Foundation.NSUserDefaults

actual object RgbPresetStore {
    private const val KEY = "rgb_presets_json"

    actual fun getJson(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(KEY)

    actual fun setJson(json: String) {
        NSUserDefaults.standardUserDefaults.setObject(json, KEY)
    }
}

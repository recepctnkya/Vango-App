package com.hextech.vango

import platform.Foundation.NSNumber
import platform.Foundation.NSUserDefaults

actual object LocalePreferences {
    private const val KEY = "language_tag"
    private const val KEY_UPDATED = "language_updated_at_ms"

    actual fun getLanguageTag(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(KEY)

    actual fun setLanguageTag(tag: String?) {
        if (tag == null) {
            NSUserDefaults.standardUserDefaults.removeObjectForKey(KEY)
        } else {
            NSUserDefaults.standardUserDefaults.setObject(tag, KEY)
        }
    }

    actual fun getLanguageUpdatedAtEpochMs(): Long {
        val raw = NSUserDefaults.standardUserDefaults.objectForKey(KEY_UPDATED) as? NSNumber
        return raw?.longLongValue ?: 0L
    }

    actual fun setLanguageUpdatedAtEpochMs(ms: Long) {
        NSUserDefaults.standardUserDefaults.setObject(NSNumber(longLong = ms.coerceAtLeast(0L)), KEY_UPDATED)
    }
}

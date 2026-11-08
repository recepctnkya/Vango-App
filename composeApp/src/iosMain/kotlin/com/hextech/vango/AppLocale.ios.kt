package com.hextech.vango

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.staticCompositionLocalOf
import platform.Foundation.NSBundle
import platform.Foundation.NSUserDefaults

actual object LocalAppLocale {
    private const val LANG_KEY = "AppleLanguages"
    private val defaultLang = NSBundle.mainBundle.preferredLocalizations.firstOrNull() as? String ?: "en"
    private val LocalLang = staticCompositionLocalOf { defaultLang }

    actual val current: String
        @Composable get() = LocalLang.current

    @Composable
    actual infix fun provides(value: String?): ProvidedValue<*> {
        val new = when (value) {
            null -> defaultLang
            "en" -> "en"
            "tr" -> "tr"
            else -> value
        }
        if (value == null) {
            NSUserDefaults.standardUserDefaults.removeObjectForKey(LANG_KEY)
        } else {
            NSUserDefaults.standardUserDefaults.setObject(arrayListOf(new), LANG_KEY)
        }
        return LocalLang.provides(new)
    }
}

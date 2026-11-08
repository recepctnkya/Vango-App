package com.hextech.vango

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

actual object LocalAppLocale {
    private var defaultLocale: Locale? = null

    actual val current: String
        @Composable get() = Locale.getDefault().toLanguageTag()

    @Composable
    actual infix fun provides(value: String?): ProvidedValue<*> {
        val configuration = LocalConfiguration.current
        val context = LocalContext.current
        if (defaultLocale == null) {
            defaultLocale = Locale.getDefault()
        }
        val newLocale = when (value) {
            null -> defaultLocale!!
            else -> Locale.forLanguageTag(value)
        }
        Locale.setDefault(newLocale)
        configuration.setLocale(newLocale)
        @Suppress("DEPRECATION")
        context.resources.updateConfiguration(configuration, context.resources.displayMetrics)
        return LocalConfiguration.provides(configuration)
    }
}

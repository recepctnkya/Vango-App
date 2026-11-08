package com.hextech.vango

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

var customAppLocale by mutableStateOf<String?>(null)

expect object LocalAppLocale {
    val current: String @Composable get
    @Composable infix fun provides(value: String?): ProvidedValue<*>
}

@Composable
fun AppLocaleProvider(content: @Composable () -> Unit) {
    LaunchedEffect(Unit) {
        // İlk açılışta kayıtlı dili yükle; yoksa Türkçe varsayılan.
        if (customAppLocale == null) {
            customAppLocale = LocalePreferences.getLanguageTag() ?: "tr"
        }
    }
    // CompositionLocal + key: dil değişince tüm stringResource ağacı yeniden yüklenir.
    CompositionLocalProvider(
        LocalAppLocale provides customAppLocale,
    ) {
        key(customAppLocale) {
            content()
        }
    }
}

fun applyAppLanguage(tag: String?, fromServer: Boolean = false) {
    val normalized = when (tag?.trim()?.lowercase()) {
        "en" -> "en"
        "tr" -> "tr"
        null, "", "system" -> null
        else -> tag.trim().lowercase()
    }
    LocalePreferences.setLanguageTag(normalized)
    customAppLocale = normalized
    if (!fromServer) {
        LocalePreferences.setLanguageUpdatedAtEpochMs(
            kotlinx.datetime.Clock.System.now().toEpochMilliseconds(),
        )
        com.hextech.vango.profile.LocaleLocalOverride.noteLocalWrite()
        com.hextech.vango.profile.LocaleServerPush.scheduleCurrentLocale()
    }
}

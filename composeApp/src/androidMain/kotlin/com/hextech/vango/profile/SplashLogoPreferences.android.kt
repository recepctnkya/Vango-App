package com.hextech.vango.profile

import android.content.Context
import android.util.Base64
import com.hextech.vango.getAppContext

actual object SplashLogoPreferences {
    private const val PREFS = "vango_splash_prefs"
    private const val KEY_LOGO = "custom_logo_b64"

    private fun prefs() =
        (getAppContext().get() as Context).getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    actual fun getCustomLogoBytes(): ByteArray? {
        val b64 = prefs().getString(KEY_LOGO, null) ?: return null
        return runCatching { Base64.decode(b64, Base64.DEFAULT) }.getOrNull()
    }

    actual fun setCustomLogoBytes(bytes: ByteArray?) {
        if (bytes == null) {
            prefs().edit().remove(KEY_LOGO).apply()
        } else {
            val capped = if (bytes.size > 800_000) bytes.copyOf(800_000) else bytes
            prefs().edit().putString(KEY_LOGO, Base64.encodeToString(capped, Base64.DEFAULT)).apply()
        }
    }
}

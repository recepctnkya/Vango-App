package com.hextech.vango.profile

/**
 * Hexnet panel ile aynı API.
 *
 * HTTPS vhost (myhexnet.com) DocumentRoot = panel, API alias = `/api`.
 * Eski APK’lar HTTP IP + `/myhexnet/api` kullanıyordu; Android 9+ cleartext’i
 * kestiği için giriş / auth/me sessizce başarısız oluyordu.
 */
object HexnetApi {
    const val BASE_HOST = "https://myhexnet.com"
    const val API_BASE = "$BASE_HOST/api"
    const val PANEL_BASE = "$BASE_HOST/"

    const val LEGACY_HOST = "http://185.33.234.10"
    const val LEGACY_API_BASE = "$LEGACY_HOST/myhexnet/api"
    const val LEGACY_PANEL_BASE = "$LEGACY_HOST/myhexnet/panel/"

    fun api(path: String): String {
        val p = if (path.startsWith("/")) path else "/$path"
        return "$API_BASE$p"
    }

    fun legacyApi(path: String): String {
        val p = if (path.startsWith("/")) path else "/$path"
        return "$LEGACY_API_BASE$p"
    }

    fun apiCandidates(path: String): List<String> = listOf(api(path), legacyApi(path))

    object Auth {
        val login get() = api("/auth/login.php")
        val me get() = api("/auth/me.php")
        val profile get() = api("/auth/profile.php")
        val avatarUpload get() = api("/auth/avatar-upload.php")
        val registerRequest get() = api("/auth/register-request.php")
        val themePreference get() = api("/auth/theme-preference.php")
        val localePreference get() = api("/auth/locale-preference.php")
        val changePassword get() = api("/auth/change-password.php")
    }

    object Devices {
        val list get() = api("/devices/list.php")
        val lookupByMacPublic get() = api("/device/lookup-by-mac.php")
        val lookupRegistration get() = api("/devices/lookup-registration.php")
        val resolveByMac get() = api("/devices/resolve-by-mac.php")
        val mqttCredentials get() = api("/devices/mqtt-credentials.php")
        val mqttCommand get() = api("/devices/mqtt-command.php")
        val latestTelemetry get() = api("/devices/latest-telemetry.php")
        val panelConfigGet get() = api("/devices/panel-config-get.php")
        val panelConfigSave get() = api("/devices/panel-config-save.php")
        fun command(panelDeviceId: Int) = api("/devices/command.php?id=$panelDeviceId")
    }

    object System {
        val mqttInfo get() = api("/system/mqtt-info.php")
    }

    /** Optional cloud Hexon chat (may be unavailable until server ships the route). */
    object Hexon {
        val chat get() = api("/hexon/chat.php")
    }
}

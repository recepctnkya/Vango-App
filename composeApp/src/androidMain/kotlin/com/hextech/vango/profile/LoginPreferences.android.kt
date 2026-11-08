package com.hextech.vango.profile

import android.content.Context
import com.hextech.vango.getAppContext

actual object LoginPreferences {
    private const val PREFS = "vango_login_prefs"
    private const val KEY_EMAIL = "remembered_email"
    private const val KEY_ACCOUNT_EMAIL = "cached_account_email"
    private const val KEY_REMEMBER = "remember_me"
    private const val KEY_TOKEN = "auth_token"
    private const val KEY_EXPIRES_AT = "auth_expires_at"
    private const val KEY_FULL_NAME = "cached_full_name"
    private const val KEY_USERNAME = "cached_username"
    private const val KEY_PHONE = "cached_phone"
    private const val KEY_CARAVAN_TYPE = "cached_caravan_type"
    private const val KEY_ROLE = "cached_role"
    private const val KEY_AVATAR_URL = "cached_avatar_url"
    private const val KEY_DEVICE_MAC = "cached_paired_device_mac"
    private const val KEY_LAST_BLE_ADAPTER_MAC = "cached_last_ble_adapter_mac"
    private const val KEY_DEVICE_UID = "cached_device_uid"
    private const val KEY_DEVICE_HW = "cached_device_hardware"
    private const val KEY_DEVICE_SW = "cached_device_software"
    private const val KEY_DEVICE_MECH = "cached_device_mechanic"
    private const val KEY_MQTT_COMPANY_ID = "cached_mqtt_company_id"
    private const val KEY_MQTT_DEVICE_ID = "cached_mqtt_device_id"
    private const val KEY_MQTT_HOST = "cached_mqtt_host"
    private const val KEY_MQTT_PORT = "cached_mqtt_port"
    private const val KEY_MQTT_USERNAME = "cached_mqtt_username"
    private const val KEY_MQTT_PASSWORD = "cached_mqtt_password"
    private const val KEY_MQTT_CLIENT_ID = "cached_mqtt_client_id"
    private const val KEY_REMOTE_AUTO_RECONNECT = "remote_mqtt_auto_reconnect"
    private const val KEY_REMOTE_CHECK_INTERVAL = "remote_mqtt_check_interval_sec"
    private const val KEY_IO_PROFILE_DEVICE_ID = "cached_io_profile_device_id"
    private const val KEY_IO_PROFILE_JSON = "cached_io_profile_json"
    private const val KEY_PANEL_CONFIG_DEVICE_ID = "cached_panel_config_device_id"
    private const val KEY_PANEL_CONFIG_JSON = "cached_panel_config_json"
    private const val KEY_PANEL_CONFIG_PENDING_BLE = "panel_config_pending_ble_push"
    private const val KEY_PANEL_PROFILE_KEY = "cached_panel_profile_key"
    private const val KEY_FIRST_USE_WIZARD = "first_use_wizard_done"
    private const val KEY_PREFERRED_CONN_MODE = "preferred_connection_mode"

    private fun prefs() =
        (getAppContext().get() as Context).getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    actual fun getRememberedEmail(): String? = prefs().getString(KEY_EMAIL, null)

    actual fun setRememberedEmail(email: String?) {
        prefs().edit().putString(KEY_EMAIL, email).apply()
    }

    actual fun getCachedAccountEmail(): String? = prefs().getString(KEY_ACCOUNT_EMAIL, null)
    actual fun setCachedAccountEmail(email: String?) {
        prefs().edit().putString(KEY_ACCOUNT_EMAIL, email).apply()
    }

    actual fun isRememberMeEnabled(): Boolean = prefs().getBoolean(KEY_REMEMBER, false)

    actual fun setRememberMe(enabled: Boolean) {
        prefs().edit().putBoolean(KEY_REMEMBER, enabled).apply()
    }

    actual fun getAuthToken(): String? = prefs().getString(KEY_TOKEN, null)
    actual fun setAuthToken(token: String?) {
        prefs().edit().putString(KEY_TOKEN, token).apply()
    }

    actual fun getAuthExpiresAt(): String? = prefs().getString(KEY_EXPIRES_AT, null)
    actual fun setAuthExpiresAt(expiresAt: String?) {
        prefs().edit().putString(KEY_EXPIRES_AT, expiresAt).apply()
    }

    actual fun getCachedFullName(): String? = prefs().getString(KEY_FULL_NAME, null)
    actual fun setCachedFullName(fullName: String?) {
        prefs().edit().putString(KEY_FULL_NAME, fullName).apply()
    }

    actual fun getCachedUsername(): String? = prefs().getString(KEY_USERNAME, null)
    actual fun setCachedUsername(username: String?) {
        prefs().edit().putString(KEY_USERNAME, username).apply()
    }

    actual fun getCachedPhone(): String? = prefs().getString(KEY_PHONE, null)
    actual fun setCachedPhone(phone: String?) {
        prefs().edit().putString(KEY_PHONE, phone).apply()
    }

    actual fun getCachedCaravanType(): String? = prefs().getString(KEY_CARAVAN_TYPE, null)
    actual fun setCachedCaravanType(type: String?) {
        prefs().edit().putString(KEY_CARAVAN_TYPE, type).apply()
    }

    actual fun getCachedRole(): String? = prefs().getString(KEY_ROLE, null)
    actual fun setCachedRole(role: String?) {
        prefs().edit().putString(KEY_ROLE, role).apply()
    }

    actual fun getCachedAvatarUrl(): String? = prefs().getString(KEY_AVATAR_URL, null)
    actual fun setCachedAvatarUrl(url: String?) {
        prefs().edit().putString(KEY_AVATAR_URL, url).apply()
    }

    actual fun getCachedPairedDeviceMac(): String? = prefs().getString(KEY_DEVICE_MAC, null)
    actual fun setCachedPairedDeviceMac(mac: String?) {
        prefs().edit().putString(KEY_DEVICE_MAC, mac).apply()
    }

    actual fun getCachedLastBleAdapterMac(): String? =
        prefs().getString(KEY_LAST_BLE_ADAPTER_MAC, null)?.trim()?.takeIf { it.isNotEmpty() }

    actual fun setCachedLastBleAdapterMac(mac: String?) {
        prefs().edit().apply {
            if (mac.isNullOrBlank()) remove(KEY_LAST_BLE_ADAPTER_MAC)
            else putString(KEY_LAST_BLE_ADAPTER_MAC, mac.trim())
        }.apply()
    }

    actual fun getCachedDeviceUid(): String? = prefs().getString(KEY_DEVICE_UID, null)
    actual fun setCachedDeviceUid(uid: String?) {
        prefs().edit().putString(KEY_DEVICE_UID, uid).apply()
    }

    actual fun getCachedDeviceHardware(): String? = prefs().getString(KEY_DEVICE_HW, null)
    actual fun setCachedDeviceHardware(value: String?) {
        prefs().edit().putString(KEY_DEVICE_HW, value).apply()
    }

    actual fun getCachedDeviceSoftware(): String? = prefs().getString(KEY_DEVICE_SW, null)
    actual fun setCachedDeviceSoftware(value: String?) {
        prefs().edit().putString(KEY_DEVICE_SW, value).apply()
    }

    actual fun getCachedDeviceMechanic(): String? = prefs().getString(KEY_DEVICE_MECH, null)
    actual fun setCachedDeviceMechanic(value: String?) {
        prefs().edit().putString(KEY_DEVICE_MECH, value).apply()
    }

    actual fun getCachedMqttCompanyId(): Int? {
        val v = prefs().getInt(KEY_MQTT_COMPANY_ID, -1)
        return v.takeIf { it > 0 }
    }

    actual fun setCachedMqttCompanyId(id: Int?) {
        prefs().edit().apply {
            if (id == null || id <= 0) remove(KEY_MQTT_COMPANY_ID) else putInt(KEY_MQTT_COMPANY_ID, id)
        }.apply()
    }

    actual fun getCachedMqttDeviceId(): Int? {
        val v = prefs().getInt(KEY_MQTT_DEVICE_ID, -1)
        return v.takeIf { it > 0 }
    }

    actual fun setCachedMqttDeviceId(id: Int?) {
        prefs().edit().apply {
            if (id == null || id <= 0) remove(KEY_MQTT_DEVICE_ID) else putInt(KEY_MQTT_DEVICE_ID, id)
        }.apply()
    }

    actual fun getCachedMqttHost(): String? = prefs().getString(KEY_MQTT_HOST, null)?.trim()?.takeIf { it.isNotEmpty() }
    actual fun setCachedMqttHost(host: String?) {
        prefs().edit().apply {
            if (host.isNullOrBlank()) remove(KEY_MQTT_HOST) else putString(KEY_MQTT_HOST, host.trim())
        }.apply()
    }

    actual fun getCachedMqttPort(): Int? {
        val v = prefs().getInt(KEY_MQTT_PORT, -1)
        return v.takeIf { it in 1..65535 }
    }

    actual fun setCachedMqttPort(port: Int?) {
        prefs().edit().apply {
            if (port == null || port !in 1..65535) remove(KEY_MQTT_PORT) else putInt(KEY_MQTT_PORT, port)
        }.apply()
    }

    actual fun getCachedMqttUsername(): String? =
        prefs().getString(KEY_MQTT_USERNAME, null)?.trim()?.takeIf { it.isNotEmpty() }

    actual fun setCachedMqttUsername(username: String?) {
        prefs().edit().apply {
            if (username.isNullOrBlank()) remove(KEY_MQTT_USERNAME) else putString(KEY_MQTT_USERNAME, username.trim())
        }.apply()
    }

    actual fun getCachedMqttPassword(): String? =
        prefs().getString(KEY_MQTT_PASSWORD, null)?.trim()?.takeIf { it.isNotEmpty() }

    actual fun setCachedMqttPassword(password: String?) {
        prefs().edit().apply {
            if (password.isNullOrBlank()) remove(KEY_MQTT_PASSWORD) else putString(KEY_MQTT_PASSWORD, password.trim())
        }.apply()
    }

    actual fun getCachedMqttClientId(): String? =
        prefs().getString(KEY_MQTT_CLIENT_ID, null)?.trim()?.takeIf { it.isNotEmpty() }

    actual fun setCachedMqttClientId(clientId: String?) {
        prefs().edit().apply {
            if (clientId.isNullOrBlank()) remove(KEY_MQTT_CLIENT_ID) else putString(KEY_MQTT_CLIENT_ID, clientId.trim())
        }.apply()
    }

    actual fun getRemoteAutoReconnect(): Boolean =
        prefs().getBoolean(KEY_REMOTE_AUTO_RECONNECT, true)

    actual fun setRemoteAutoReconnect(enabled: Boolean) {
        prefs().edit().putBoolean(KEY_REMOTE_AUTO_RECONNECT, enabled).apply()
    }

    actual fun getRemoteCheckIntervalSec(): Int {
        val v = prefs().getInt(KEY_REMOTE_CHECK_INTERVAL, 30)
        return v.takeIf { it in 15..300 } ?: 30
    }

    actual fun setRemoteCheckIntervalSec(seconds: Int) {
        prefs().edit().putInt(KEY_REMOTE_CHECK_INTERVAL, seconds.coerceIn(15, 300)).apply()
    }

    actual fun getCachedIoProfileDeviceId(): Int? {
        val v = prefs().getInt(KEY_IO_PROFILE_DEVICE_ID, -1)
        return v.takeIf { it > 0 }
    }

    actual fun setCachedIoProfileDeviceId(id: Int?) {
        prefs().edit().apply {
            if (id == null || id <= 0) remove(KEY_IO_PROFILE_DEVICE_ID) else putInt(KEY_IO_PROFILE_DEVICE_ID, id)
        }.apply()
    }

    actual fun getCachedIoProfileJson(): String? = prefs().getString(KEY_IO_PROFILE_JSON, null)

    actual fun setCachedIoProfileJson(json: String?) {
        prefs().edit().apply {
            if (json.isNullOrBlank()) remove(KEY_IO_PROFILE_JSON) else putString(KEY_IO_PROFILE_JSON, json)
        }.apply()
    }

    actual fun getCachedPanelConfigDeviceId(): Int? {
        val v = prefs().getInt(KEY_PANEL_CONFIG_DEVICE_ID, -1)
        return v.takeIf { it > 0 }
    }

    actual fun setCachedPanelConfigDeviceId(id: Int?) {
        prefs().edit().apply {
            if (id == null || id <= 0) remove(KEY_PANEL_CONFIG_DEVICE_ID) else putInt(KEY_PANEL_CONFIG_DEVICE_ID, id)
        }.apply()
    }

    actual fun getCachedPanelConfigJson(): String? = prefs().getString(KEY_PANEL_CONFIG_JSON, null)

    actual fun setCachedPanelConfigJson(json: String?) {
        prefs().edit().apply {
            if (json.isNullOrBlank()) remove(KEY_PANEL_CONFIG_JSON) else putString(KEY_PANEL_CONFIG_JSON, json)
        }.apply()
    }

    actual fun getCachedPanelProfileKey(): String? =
        prefs().getString(KEY_PANEL_PROFILE_KEY, null)?.trim()?.takeIf { it.isNotEmpty() }

    actual fun setCachedPanelProfileKey(key: String?) {
        prefs().edit().apply {
            if (key.isNullOrBlank()) remove(KEY_PANEL_PROFILE_KEY) else putString(KEY_PANEL_PROFILE_KEY, key)
        }.apply()
    }

    actual fun isPanelConfigPendingBlePush(): Boolean =
        prefs().getBoolean(KEY_PANEL_CONFIG_PENDING_BLE, false)

    actual fun setPanelConfigPendingBlePush(pending: Boolean) {
        prefs().edit().putBoolean(KEY_PANEL_CONFIG_PENDING_BLE, pending).apply()
    }

    actual fun hasCompletedFirstUseWizard(): Boolean =
        prefs().getBoolean(KEY_FIRST_USE_WIZARD, false)

    actual fun setCompletedFirstUseWizard(done: Boolean) {
        prefs().edit().putBoolean(KEY_FIRST_USE_WIZARD, done).apply()
    }

    actual fun getPreferredConnectionMode(): String? =
        prefs().getString(KEY_PREFERRED_CONN_MODE, null)?.trim()?.takeIf { it.isNotEmpty() }

    actual fun setPreferredConnectionMode(mode: String?) {
        prefs().edit().apply {
            if (mode.isNullOrBlank()) remove(KEY_PREFERRED_CONN_MODE) else putString(KEY_PREFERRED_CONN_MODE, mode)
        }.apply()
    }

    actual fun clearAuthSession() {
        prefs().edit()
            .remove(KEY_TOKEN)
            .remove(KEY_EXPIRES_AT)
            .remove(KEY_FULL_NAME)
            .remove(KEY_USERNAME)
            .remove(KEY_PHONE)
            .remove(KEY_CARAVAN_TYPE)
            .remove(KEY_ROLE)
            .remove(KEY_AVATAR_URL)
            .remove(KEY_ACCOUNT_EMAIL)
            .remove(KEY_MQTT_HOST)
            .remove(KEY_MQTT_PORT)
            .remove(KEY_MQTT_USERNAME)
            .remove(KEY_MQTT_PASSWORD)
            .remove(KEY_MQTT_CLIENT_ID)
            .apply()
    }
}

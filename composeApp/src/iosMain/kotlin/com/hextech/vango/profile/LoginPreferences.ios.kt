package com.hextech.vango.profile

import platform.Foundation.NSUserDefaults

actual object LoginPreferences {
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

    private var accountEmail: String? = null
    private var token: String? = null
    private var expiresAt: String? = null
    private var fullName: String? = null
    private var username: String? = null
    private var phone: String? = null
    private var role: String? = null
    private var avatarUrl: String? = null

    private fun defaults(): NSUserDefaults = NSUserDefaults.standardUserDefaults

    private fun getPersistedString(key: String): String? =
        defaults().stringForKey(key)?.trim()?.takeIf { it.isNotEmpty() }

    private fun setPersistedString(key: String, value: String?) {
        val v = value?.trim().orEmpty()
        if (v.isEmpty()) {
            defaults().removeObjectForKey(key)
        } else {
            defaults().setObject(v, key)
        }
    }

    actual fun getRememberedEmail(): String? = null
    actual fun setRememberedEmail(email: String?) {}

    actual fun getCachedAccountEmail(): String? = accountEmail
    actual fun setCachedAccountEmail(email: String?) {
        accountEmail = email
    }
    actual fun isRememberMeEnabled(): Boolean = false
    actual fun setRememberMe(enabled: Boolean) {}
    actual fun getAuthToken(): String? = token
    actual fun setAuthToken(token: String?) {
        this.token = token
    }
    actual fun getAuthExpiresAt(): String? = expiresAt
    actual fun setAuthExpiresAt(expiresAt: String?) {
        this.expiresAt = expiresAt
    }
    actual fun getCachedFullName(): String? = fullName
    actual fun setCachedFullName(fullName: String?) {
        this.fullName = fullName
    }
    actual fun getCachedUsername(): String? = username
    actual fun setCachedUsername(username: String?) {
        this.username = username
    }
    actual fun getCachedPhone(): String? = phone
    actual fun setCachedPhone(phone: String?) {
        this.phone = phone
    }
    actual fun getCachedRole(): String? = role
    actual fun setCachedRole(role: String?) {
        this.role = role
    }
    actual fun getCachedAvatarUrl(): String? = avatarUrl
    actual fun setCachedAvatarUrl(url: String?) {
        this.avatarUrl = url
    }

    actual fun getCachedPairedDeviceMac(): String? = getPersistedString(KEY_DEVICE_MAC)
    actual fun setCachedPairedDeviceMac(mac: String?) = setPersistedString(KEY_DEVICE_MAC, mac)

    actual fun getCachedLastBleAdapterMac(): String? = getPersistedString(KEY_LAST_BLE_ADAPTER_MAC)
    actual fun setCachedLastBleAdapterMac(mac: String?) = setPersistedString(KEY_LAST_BLE_ADAPTER_MAC, mac)

    actual fun getCachedDeviceUid(): String? = getPersistedString(KEY_DEVICE_UID)
    actual fun setCachedDeviceUid(uid: String?) = setPersistedString(KEY_DEVICE_UID, uid)

    actual fun getCachedDeviceHardware(): String? = getPersistedString(KEY_DEVICE_HW)
    actual fun setCachedDeviceHardware(value: String?) = setPersistedString(KEY_DEVICE_HW, value)

    actual fun getCachedDeviceSoftware(): String? = getPersistedString(KEY_DEVICE_SW)
    actual fun setCachedDeviceSoftware(value: String?) = setPersistedString(KEY_DEVICE_SW, value)

    actual fun getCachedDeviceMechanic(): String? = getPersistedString(KEY_DEVICE_MECH)
    actual fun setCachedDeviceMechanic(value: String?) = setPersistedString(KEY_DEVICE_MECH, value)

    actual fun getCachedMqttCompanyId(): Int? =
        defaults().objectForKey(KEY_MQTT_COMPANY_ID)?.let { (it as? Number)?.toInt() }?.takeIf { it > 0 }

    actual fun setCachedMqttCompanyId(id: Int?) {
        if (id == null || id <= 0) defaults().removeObjectForKey(KEY_MQTT_COMPANY_ID)
        else defaults().setInteger(id.toLong(), KEY_MQTT_COMPANY_ID)
    }

    actual fun getCachedMqttDeviceId(): Int? =
        defaults().objectForKey(KEY_MQTT_DEVICE_ID)?.let { (it as? Number)?.toInt() }?.takeIf { it > 0 }

    actual fun setCachedMqttDeviceId(id: Int?) {
        if (id == null || id <= 0) defaults().removeObjectForKey(KEY_MQTT_DEVICE_ID)
        else defaults().setInteger(id.toLong(), KEY_MQTT_DEVICE_ID)
    }

    actual fun getCachedMqttHost(): String? = getPersistedString(KEY_MQTT_HOST)
    actual fun setCachedMqttHost(host: String?) = setPersistedString(KEY_MQTT_HOST, host)

    actual fun getCachedMqttPort(): Int? =
        defaults().objectForKey(KEY_MQTT_PORT)?.let { (it as? Number)?.toInt() }?.takeIf { it in 1..65535 }

    actual fun setCachedMqttPort(port: Int?) {
        if (port == null || port !in 1..65535) defaults().removeObjectForKey(KEY_MQTT_PORT)
        else defaults().setInteger(port.toLong(), KEY_MQTT_PORT)
    }

    actual fun getCachedMqttUsername(): String? = getPersistedString(KEY_MQTT_USERNAME)
    actual fun setCachedMqttUsername(username: String?) = setPersistedString(KEY_MQTT_USERNAME, username)

    actual fun getCachedMqttPassword(): String? = getPersistedString(KEY_MQTT_PASSWORD)
    actual fun setCachedMqttPassword(password: String?) = setPersistedString(KEY_MQTT_PASSWORD, password)

    actual fun getCachedMqttClientId(): String? = getPersistedString(KEY_MQTT_CLIENT_ID)
    actual fun setCachedMqttClientId(clientId: String?) = setPersistedString(KEY_MQTT_CLIENT_ID, clientId)

    actual fun getRemoteAutoReconnect(): Boolean {
        if (!defaults().objectForKey(KEY_REMOTE_AUTO_RECONNECT).let { it != null }) return true
        return defaults().boolForKey(KEY_REMOTE_AUTO_RECONNECT)
    }

    actual fun setRemoteAutoReconnect(enabled: Boolean) {
        defaults().setBool(enabled, KEY_REMOTE_AUTO_RECONNECT)
    }

    actual fun getRemoteCheckIntervalSec(): Int {
        val v = defaults().integerForKey(KEY_REMOTE_CHECK_INTERVAL).toInt()
        return v.takeIf { it in 15..300 } ?: 30
    }

    actual fun setRemoteCheckIntervalSec(seconds: Int) {
        defaults().setInteger(seconds.coerceIn(15, 300).toLong(), KEY_REMOTE_CHECK_INTERVAL)
    }

    actual fun getCachedIoProfileDeviceId(): Int? {
        val v = defaults().integerForKey(KEY_IO_PROFILE_DEVICE_ID).toInt()
        return v.takeIf { it > 0 }
    }

    actual fun setCachedIoProfileDeviceId(id: Int?) {
        if (id == null || id <= 0) defaults().removeObjectForKey(KEY_IO_PROFILE_DEVICE_ID)
        else defaults().setInteger(id.toLong(), KEY_IO_PROFILE_DEVICE_ID)
    }

    actual fun getCachedIoProfileJson(): String? = getPersistedString(KEY_IO_PROFILE_JSON)

    actual fun setCachedIoProfileJson(json: String?) = setPersistedString(KEY_IO_PROFILE_JSON, json)

    actual fun getCachedPanelConfigDeviceId(): Int? {
        val v = defaults().integerForKey(KEY_PANEL_CONFIG_DEVICE_ID).toInt()
        return v.takeIf { it > 0 }
    }

    actual fun setCachedPanelConfigDeviceId(id: Int?) {
        if (id == null || id <= 0) defaults().removeObjectForKey(KEY_PANEL_CONFIG_DEVICE_ID)
        else defaults().setInteger(id.toLong(), KEY_PANEL_CONFIG_DEVICE_ID)
    }

    actual fun getCachedPanelConfigJson(): String? = getPersistedString(KEY_PANEL_CONFIG_JSON)

    actual fun setCachedPanelConfigJson(json: String?) = setPersistedString(KEY_PANEL_CONFIG_JSON, json)

    actual fun getCachedPanelProfileKey(): String? =
        getPersistedString(KEY_PANEL_PROFILE_KEY)?.trim()?.takeIf { it.isNotEmpty() }

    actual fun setCachedPanelProfileKey(key: String?) = setPersistedString(KEY_PANEL_PROFILE_KEY, key)

    actual fun isPanelConfigPendingBlePush(): Boolean =
        defaults().boolForKey(KEY_PANEL_CONFIG_PENDING_BLE)

    actual fun setPanelConfigPendingBlePush(pending: Boolean) {
        defaults().setBool(pending, KEY_PANEL_CONFIG_PENDING_BLE)
    }

    actual fun hasCompletedFirstUseWizard(): Boolean =
        defaults().boolForKey(KEY_FIRST_USE_WIZARD)

    actual fun setCompletedFirstUseWizard(done: Boolean) {
        defaults().setBool(done, KEY_FIRST_USE_WIZARD)
    }

    actual fun getPreferredConnectionMode(): String? =
        getPersistedString(KEY_PREFERRED_CONN_MODE)

    actual fun setPreferredConnectionMode(mode: String?) =
        setPersistedString(KEY_PREFERRED_CONN_MODE, mode)

    actual fun clearAuthSession() {
        accountEmail = null
        token = null
        expiresAt = null
        fullName = null
        username = null
        phone = null
        role = null
        avatarUrl = null
        setCachedMqttHost(null)
        setCachedMqttPort(null)
        setCachedMqttUsername(null)
        setCachedMqttPassword(null)
        setCachedMqttClientId(null)
    }
}

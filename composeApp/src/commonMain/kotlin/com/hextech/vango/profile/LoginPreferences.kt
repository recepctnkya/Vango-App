package com.hextech.vango.profile

expect object LoginPreferences {
    fun getRememberedEmail(): String?
    fun setRememberedEmail(email: String?)
    /** Sunucudaki hesap e-postası (profil / oturum; hatırla kutusundan bağımsız). */
    fun getCachedAccountEmail(): String?
    fun setCachedAccountEmail(email: String?)
    fun isRememberMeEnabled(): Boolean
    fun setRememberMe(enabled: Boolean)
    fun getAuthToken(): String?
    fun setAuthToken(token: String?)
    fun getAuthExpiresAt(): String?
    fun setAuthExpiresAt(expiresAt: String?)
    fun getCachedFullName(): String?
    fun setCachedFullName(fullName: String?)
    fun getCachedUsername(): String?
    fun setCachedUsername(username: String?)
    fun getCachedPhone(): String?
    fun setCachedPhone(phone: String?)
    fun getCachedCaravanType(): String?
    fun setCachedCaravanType(type: String?)
    fun getCachedRole(): String?
    fun setCachedRole(role: String?)
    fun getCachedAvatarUrl(): String?
    fun setCachedAvatarUrl(url: String?)
    /**
     * Son başarılı cihaz eşleştirme / sunucu sorgusu (çıkış yapsa da korunur; kullanıcı cihazını hatırlasın).
     * [clearAuthSession] bunları silmez.
     */
    fun getCachedPairedDeviceMac(): String?
    fun setCachedPairedDeviceMac(mac: String?)
    /** Son başarılı BLE IO modül adaptör MAC (otomatik yeniden bağlanma). */
    fun getCachedLastBleAdapterMac(): String?
    fun setCachedLastBleAdapterMac(mac: String?)
    fun getCachedDeviceUid(): String?
    fun setCachedDeviceUid(uid: String?)
    fun getCachedDeviceHardware(): String?
    fun setCachedDeviceHardware(value: String?)
    fun getCachedDeviceSoftware(): String?
    fun setCachedDeviceSoftware(value: String?)
    fun getCachedDeviceMechanic(): String?
    fun setCachedDeviceMechanic(value: String?)
    /** MQTT topic: hexnet/v1/{company}/{device}/… */
    fun getCachedMqttCompanyId(): Int?
    fun setCachedMqttCompanyId(id: Int?)
    fun getCachedMqttDeviceId(): Int?
    fun setCachedMqttDeviceId(id: Int?)
    fun getCachedMqttHost(): String?
    fun setCachedMqttHost(host: String?)
    fun getCachedMqttPort(): Int?
    fun setCachedMqttPort(port: Int?)
    fun getCachedMqttUsername(): String?
    fun setCachedMqttUsername(username: String?)
    fun getCachedMqttPassword(): String?
    fun setCachedMqttPassword(password: String?)
    fun getCachedMqttClientId(): String?
    fun setCachedMqttClientId(clientId: String?)
    fun getRemoteAutoReconnect(): Boolean
    fun setRemoteAutoReconnect(enabled: Boolean)
    fun getRemoteCheckIntervalSec(): Int
    fun setRemoteCheckIntervalSec(seconds: Int)
    fun getCachedIoProfileDeviceId(): Int?
    fun setCachedIoProfileDeviceId(id: Int?)
    fun getCachedIoProfileJson(): String?
    fun setCachedIoProfileJson(json: String?)
    fun getCachedPanelConfigDeviceId(): Int?
    fun setCachedPanelConfigDeviceId(id: Int?)
    fun getCachedPanelConfigJson(): String?
    fun setCachedPanelConfigJson(json: String?)
    fun getCachedPanelProfileKey(): String?
    fun setCachedPanelProfileKey(key: String?)
    fun isPanelConfigPendingBlePush(): Boolean
    fun setPanelConfigPendingBlePush(pending: Boolean)
    fun hasCompletedFirstUseWizard(): Boolean
    fun setCompletedFirstUseWizard(done: Boolean)
    /** "ble" | "remote" — last wizard / user preference for connection mode. */
    fun getPreferredConnectionMode(): String?
    fun setPreferredConnectionMode(mode: String?)
    fun clearAuthSession()
}

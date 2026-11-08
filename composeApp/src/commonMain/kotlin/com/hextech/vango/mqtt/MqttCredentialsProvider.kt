package com.hextech.vango.mqtt

import com.hextech.vango.profile.HexnetSystemApi
import com.hextech.vango.profile.LoginPreferences
import com.hextech.vango.profile.LoginUserDto
import com.hextech.vango.profile.MqttCredentialsApi
import com.hextech.vango.profile.toMqttBrokerCredentials
import com.hextech.vango.remoteSessionError

object MqttCredentialsProvider {

    fun cacheFromLoginUser(user: LoginUserDto?) {
        clearCache()
        user?.toMqttBrokerCredentials()?.let { persist(it) }
    }

    /**
     * myhexnet: broker adresi [HexnetSystemApi], kimlik [MqttCredentialsApi] + panel `device_id`.
     */
    suspend fun resolve(panelDeviceId: Int): MqttBrokerCredentials {
        readCached(panelDeviceId)?.let { return it }

        val token = LoginPreferences.getAuthToken()?.trim().orEmpty()
        if (token.isEmpty()) {
            remoteSessionError.value = "Uzaktan kontrol için myhexnet oturumu gerekli."
            return MqttBrokerCredentials.anonymous()
        }

        if (panelDeviceId <= 0) {
            remoteSessionError.value = "Panel cihaz kimliği (device_id) bulunamadı."
            return MqttBrokerCredentials.anonymous()
        }

        val mqttInfo = HexnetSystemApi.fetchMqttInfo(token)
        MqttCredentialsApi.fetch(token, panelDeviceId)?.let { creds ->
            val merged = creds.copy(
                host = mqttInfo?.brokerHost ?: creds.host,
                port = mqttInfo?.brokerPort ?: creds.port,
            )
            persist(merged, panelDeviceId)
            remoteSessionError.value = null
            return merged
        }

        // Kimlik API’den gelmese de (broker anonim) host/port ile devam et.
        remoteSessionError.value = null
        val fallback = MqttBrokerCredentials(
            host = mqttInfo?.brokerHost ?: MqttBrokerCredentials.DEFAULT_HOST,
            port = mqttInfo?.brokerPort ?: MqttBrokerCredentials.DEFAULT_PORT,
            username = null,
            password = null,
            clientId = "hexnet-vango-$panelDeviceId",
        )
        persist(fallback, panelDeviceId)
        return fallback
    }

    private fun readCached(panelDeviceId: Int): MqttBrokerCredentials? {
        val cachedDeviceId = LoginPreferences.getCachedMqttDeviceId()
        if (cachedDeviceId != null && cachedDeviceId != panelDeviceId) return null

        val host = LoginPreferences.getCachedMqttHost()?.trim().orEmpty()
        val port = LoginPreferences.getCachedMqttPort() ?: return null
        val clientId = LoginPreferences.getCachedMqttClientId()?.trim().orEmpty()
        if (host.isEmpty() || port <= 0 || clientId.isEmpty()) return null
        return MqttBrokerCredentials(
            host = host,
            port = port,
            username = LoginPreferences.getCachedMqttUsername()?.trim()?.takeIf { it.isNotEmpty() },
            password = LoginPreferences.getCachedMqttPassword()?.trim()?.takeIf { it.isNotEmpty() },
            clientId = clientId,
        )
    }

    private fun persist(creds: MqttBrokerCredentials, panelDeviceId: Int? = null) {
        if (panelDeviceId != null && panelDeviceId > 0) {
            LoginPreferences.setCachedMqttDeviceId(panelDeviceId)
        }
        LoginPreferences.setCachedMqttHost(creds.host)
        LoginPreferences.setCachedMqttPort(creds.port)
        LoginPreferences.setCachedMqttUsername(creds.username)
        LoginPreferences.setCachedMqttPassword(creds.password)
        LoginPreferences.setCachedMqttClientId(creds.clientId)
    }

    fun clearCache() {
        LoginPreferences.setCachedMqttHost(null)
        LoginPreferences.setCachedMqttPort(null)
        LoginPreferences.setCachedMqttUsername(null)
        LoginPreferences.setCachedMqttPassword(null)
        LoginPreferences.setCachedMqttClientId(null)
    }

    /**
     * Kullanıcının girdiği MQTT kullanıcı adı/şifresini kaydeder; broker host/port [HexnetSystemApi] üzerinden alınır.
     */
    suspend fun saveUserCredentials(username: String, password: String, panelDeviceId: Int) {
        val token = LoginPreferences.getAuthToken()?.trim().orEmpty()
        if (token.isEmpty()) {
            remoteSessionError.value = "Uzaktan kontrol için myhexnet oturumu gerekli."
            return
        }
        val deviceId = panelDeviceId.takeIf { it > 0 }
            ?: LoginPreferences.getCachedMqttDeviceId()
            ?: 0
        val mqttInfo = HexnetSystemApi.fetchMqttInfo(token)
        val host = mqttInfo?.brokerHost ?: MqttBrokerCredentials.DEFAULT_HOST
        val port = mqttInfo?.brokerPort ?: MqttBrokerCredentials.DEFAULT_PORT
        val clientId = when {
            deviceId > 0 -> "hexnet-vango-$deviceId"
            else -> LoginPreferences.getCachedMqttClientId()?.trim()?.takeIf { it.isNotEmpty() }
                ?: "hexnet-vango-mobile"
        }
        persist(
            MqttBrokerCredentials(
                host = host,
                port = port,
                username = username.trim(),
                password = password,
                clientId = clientId,
            ),
            deviceId.takeIf { it > 0 },
        )
        remoteSessionError.value = null
    }
}

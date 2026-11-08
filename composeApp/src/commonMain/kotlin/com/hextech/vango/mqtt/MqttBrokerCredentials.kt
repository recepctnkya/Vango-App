package com.hextech.vango.mqtt

data class MqttBrokerCredentials(
    val host: String,
    val port: Int,
    val username: String?,
    val password: String?,
    val clientId: String,
) {
    companion object {
        const val DEFAULT_HOST = "185.33.234.10"
        const val DEFAULT_PORT = 1883

        fun anonymous(clientId: String = "hexnet-remote-guest"): MqttBrokerCredentials =
            MqttBrokerCredentials(
                host = DEFAULT_HOST,
                port = DEFAULT_PORT,
                username = null,
                password = null,
                clientId = clientId,
            )
    }
}

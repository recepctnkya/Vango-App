package com.hextech.vango.mqtt

import com.hextech.vango.HeroLinkBus
import com.hextech.vango.AppConnectionState
import com.hextech.vango.RemoteMqttRoute
import com.hextech.vango.RemoteTelemetry
import com.hextech.vango.RemoteTelemetryParser
import com.hextech.vango.appConnectionState
import com.hextech.vango.applyRemoteTelemetryToUiState
import com.hextech.vango.disconnectBleLink
import com.hextech.vango.hexnetPanelDevice
import com.hextech.vango.isAppRemoteMqttConnected
import com.hextech.vango.profile.LoginPreferences
import com.hextech.vango.remoteModeEnabled
import com.hextech.vango.remoteMqttRoute
import com.hextech.vango.remoteSessionError
import com.hextech.vango.remoteTelemetry
import io.github.davidepianca98.MQTTClient
import io.github.davidepianca98.mqtt.MQTTVersion
import io.github.davidepianca98.mqtt.Subscription
import io.github.davidepianca98.mqtt.packets.Qos
import io.github.davidepianca98.mqtt.packets.mqttv5.SubscriptionOptions
import io.github.davidepianca98.mqtt.packets.mqtt.MQTTPublish
import io.github.davidepianca98.mqtt.packets.mqttv5.ReasonCode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

/** Geriye dönük test topic sabiti. */
val REMOTE_TELEMETRY_TEST_TOPIC: String
    get() = RemoteMqttRoute.defaultTest.telemetryTopic

val mqttTelemetryManagerRef = MutableStateFlow<MqttTelemetryManager?>(null)

class MqttTelemetryManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private var mqttClient: MQTTClient? = null
    private var mqttRunJob: Job? = null
    private var telemetryTopic: String? = null
    private var commandTopic: String = RemoteMqttRoute.defaultTest.commandTopic
    private var connected = false

    fun connect(panelDeviceId: Int? = null) {
        scope.launch {
            try {
                disconnectInternal(clearState = false)
                val deviceId = panelDeviceId?.takeIf { it > 0 }
                    ?: hexnetPanelDevice.value?.panelDeviceId?.takeIf { it > 0 }
                    ?: LoginPreferences.getCachedMqttDeviceId()?.takeIf { it > 0 }
                    ?: 0
                if (deviceId <= 0) {
                    appConnectionState.value = AppConnectionState.OFFLINE
                    remoteSessionError.value =
                        "Panel cihaz kimliği (device_id) bulunamadı. Önce cihazı myhexnet paneline kaydedin."
                    return@launch
                }
                val creds = MqttCredentialsProvider.resolve(deviceId)
                val hasAuth = !creds.username.isNullOrBlank() && !creds.password.isNullOrBlank()
                // Broker şu an anonime açık (IO modül de anonim bağlanıyor). Kimlik yoksa anonim dene.
                if (!hasAuth) {
                    println("MQTT credentials missing; trying anonymous ${creds.host}:${creds.port}")
                } else {
                    println("MQTT connecting ${creds.host}:${creds.port} user=${creds.username}")
                }

                val client = MQTTClient(
                    MQTTVersion.MQTT3_1_1,
                    creds.host,
                    creds.port,
                    null,
                    keepAlive = 20,
                    webSocket = null,
                    cleanStart = true,
                    clientId = creds.clientId.ifBlank { "hexnet-vango-$deviceId" },
                    userName = if (hasAuth) creds.username else null,
                    password = if (hasAuth) creds.password.toMqttPassword() else null,
                    connackTimeout = 15,
                    connectTimeout = 15,
                    onConnected = {
                        connected = true
                        println("MQTT connected")
                        disconnectBleLink()
                        appConnectionState.value = AppConnectionState.REMOTE_MQTT_CONNECTED
                        remoteSessionError.value = null
                        telemetryTopic?.let { subscribeTelemetry(it) }
                    },
                    onDisconnected = {
                        connected = false
                        if (remoteModeEnabled.value) {
                            appConnectionState.value = AppConnectionState.OFFLINE
                        }
                    },
                ) { publish -> handlePublish(publish) }
                mqttClient = client
                // KMQTT 1.0.0: CONNACK ve PUBLISH için step() döngüsü şart (yalnızca ctor yetmez).
                mqttRunJob = scope.launch(Dispatchers.Default) {
                    try {
                        while (isActive && client.isRunning()) {
                            client.step()
                            delay(5)
                        }
                    } catch (throwable: Exception) {
                        println("MQTT run loop failed: ${throwable.message}")
                        connected = false
                        appConnectionState.value = AppConnectionState.OFFLINE
                        remoteSessionError.value =
                            "MQTT bağlantısı kesildi: ${throwable.message ?: "bilinmeyen hata"}"
                    }
                }
            } catch (e: Exception) {
                println("MQTT connect failed: ${e.message}")
                connected = false
                appConnectionState.value = AppConnectionState.OFFLINE
                remoteSessionError.value = "MQTT bağlantısı kurulamadı: ${e.message ?: "bilinmeyen hata"}"
            }
        }
    }

    private fun resolveCommandTopic(telemetryTopic: String): String {
        hexnetPanelDevice.value?.commandTopic?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
        if (telemetryTopic.endsWith("/telemetry")) {
            return telemetryTopic.removeSuffix("/telemetry") + "/command"
        }
        return remoteMqttRoute.value?.commandTopic ?: RemoteMqttRoute.defaultTest.commandTopic
    }

    fun subscribeTelemetry(topic: String) {
        val previousTopic = telemetryTopic
        telemetryTopic = topic
        commandTopic = resolveCommandTopic(topic)
        scope.launch {
            try {
                val client = mqttClient ?: return@launch
                if (!connected) return@launch
                if (previousTopic != null && previousTopic != topic) {
                    kotlin.runCatching { client.unsubscribe(listOf(previousTopic)) }
                }
                client.subscribe(
                    listOf(
                        Subscription(
                            topicFilter = topic,
                            options = SubscriptionOptions(qos = Qos.AT_LEAST_ONCE),
                        ),
                    ),
                )
                println("MQTT subscribed: $topic (cmd=$commandTopic)")
            } catch (e: Exception) {
                println("MQTT subscribe failed: ${e.message}")
                appConnectionState.value = AppConnectionState.OFFLINE
                remoteSessionError.value = "Telemetri kanalına abone olunamadı."
            }
        }
    }

    fun publishRelayCommand(index: Int, state: Boolean): Boolean {
        val safeIndex = index.coerceIn(0, 15)
        val payload = MqttCommandBuilder.relaySet(safeIndex, state)
        println("MQTT relay publish idx=$safeIndex state=$state topic=$commandTopic")
        return publishCommandPayload(payload)
    }

    fun publishDimmerCommand(index: Int, value: Int): Boolean {
        val payload = MqttCommandBuilder.dimmerSet(index, value)
        println("MQTT dimmer publish idx=$index pct=$value topic=$commandTopic payload=$payload")
        return publishCommandPayload(payload)
    }

    fun publishRgbCommand(r: Int, g: Int, b: Int, isLight: Int): Boolean {
        val payload = MqttCommandBuilder.rgbSet(r, g, b, isLight)
        println("MQTT rgb publish r=$r g=$g b=$b")
        return publishCommandPayload(payload)
    }

    fun publishConfigCommand(configJson: String): Boolean {
        println("MQTT config_set publish")
        return publishCommandPayload(MqttCommandBuilder.configSet(configJson))
    }

    fun disconnect() {
        scope.launch {
            disconnectInternal(clearState = true)
        }
    }

    fun onTelemetryReceived(payload: String) {
        println("MQTT telemetry received")
        val parsed = parseTelemetryJson(payload)
        if (parsed != null) {
            println("telemetry parsed success")
            remoteTelemetry.value = parsed
            applyRemoteTelemetryToUiState(parsed)
        } else {
            println("telemetry parsed error")
        }
    }

    fun parseTelemetryJson(payload: String): RemoteTelemetry? =
        RemoteTelemetryParser.parse(payload)

    private fun handlePublish(publish: MQTTPublish) {
        val topic = publish.topicName
        val telemetry = telemetryTopic
        if (telemetry != null && topic != telemetry && !topic.endsWith("/telemetry")) {
            return
        }
        val text = publish.payload.decodeToPayloadString() ?: return
        onTelemetryReceived(text)
    }

    private fun publishCommandPayload(payload: String): Boolean {
        return try {
            val client = mqttClient
            if (client == null) {
                println("MQTT publish skipped: no client")
                return false
            }
            if (!connected && !isAppRemoteMqttConnected()) {
                println("MQTT publish skipped: not connected (topic=$commandTopic)")
                return false
            }
            client.publish(
                retain = false,
                qos = Qos.AT_LEAST_ONCE,
                topic = commandTopic,
                payload = payload.toMqttPayload(),
            )
            println("MQTT publish ok -> $commandTopic")
            HeroLinkBus.notifyOutboundTx()
            true
        } catch (e: Exception) {
            println("MQTT publish failed ($commandTopic): ${e.message}")
            false
        }
    }

    private fun disconnectInternal(clearState: Boolean) {
        try {
            mqttRunJob?.cancel()
            mqttRunJob = null
            mqttClient?.let { client ->
                kotlin.runCatching { client.disconnect(ReasonCode.SUCCESS) }
            }
        } catch (_: Exception) {
            // no-op
        } finally {
            mqttClient = null
            connected = false
            if (clearState) {
                telemetryTopic = null
                if (!remoteModeEnabled.value) {
                    appConnectionState.value = AppConnectionState.OFFLINE
                }
            }
        }
    }
}

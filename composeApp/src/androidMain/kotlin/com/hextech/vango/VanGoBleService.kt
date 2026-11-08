package com.hextech.vango

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import kotlinx.coroutines.*

/**
 * Foreground service to maintain persistent BLE connection with VanGO IO modules.
 * Keeps the connection alive when app goes to background or screen is locked.
 */
class VanGoBleService : Service() {
    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "vango_ble_service"
        private const val CHANNEL_NAME = "VanGO BLE Service"

        const val ACTION_START_SERVICE = "com.hextech.vango.START_BLE_SERVICE"
        const val ACTION_STOP_SERVICE = "com.hextech.vango.STOP_BLE_SERVICE"
    }

    private val binder = VanGoBinder()
    private var wakeLock: PowerManager.WakeLock? = null
    private var serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var connectionMonitorJob: Job? = null
    private var weatherUpdateJob: Job? = null

    inner class VanGoBinder : Binder() {
        fun getService(): VanGoBleService = this@VanGoBleService
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        acquireWakeLock()
        println("🔄 VanGoBleService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        println("onStartCommand !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! ${intent?.action}")
        when (intent?.action) {
            ACTION_START_SERVICE -> {
                startForegroundService()
                startBackgroundOperations()
            }
            ACTION_STOP_SERVICE -> {
                stopForegroundService()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder = binder

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Maintains VanGO device connection in background"
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun startForegroundService() {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("VanGO Connected")
            .setContentText("Maintaining connection to VanGO IO module")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceCompat.startForeground(
                    this,
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE,
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            println("🔄 VanGoBleService started as foreground service")
        } catch (e: Exception) {
            println("❌ VanGoBleService startForeground failed: ${e.message}")
            try {
                startForeground(NOTIFICATION_ID, notification)
            } catch (e2: Exception) {
                println("❌ VanGoBleService fallback startForeground failed: ${e2.message}")
            }
        }
    }

    private fun startBackgroundOperations() {
        connectionMonitorJob?.cancel()
        connectionMonitorJob = serviceScope.launch {
            var reconnectionAttempts = 0
            var lastReconnectionTime = 0L
            val baseDelayMs = 2_000L
            val maxDelayMs = 20_000L

            while (isActive) {
                try {
                    ensureWakeLockHeld()
                    val isConnected = getAppContext().vanGO.isConnected()
                    val currentTime = getCurrentTimeInSeconds()

                    if (isConnected) {
                        reconnectionAttempts = 0
                        lastReconnectionTime = 0L
                        getAppContext().ble().connectedDeviceMac()?.let(::rememberLastBleAdapterMac)
                        updateNotification("Connected", "VanGO IO module connected")

                        val globalState = vanGoGlobal.value
                        if (globalState.connectionState == BleGattConnectionChain.ChainEnded &&
                            globalState.operation() == Operations.Idle
                        ) {
                            if (globalState.currentMtu == 20) {
                                getAppContext().vanGO.configureMtu()
                            } else if (!globalState.isCharacteristicNotificationOn) {
                                getAppContext().vanGO.enableCharacteristicNotification()
                            }
                        }
                    } else if (shouldAttemptBleAutoReconnect()) {
                        val reconnectMac = resolveLastBleReconnectMac()
                        val delayMs = minOf(baseDelayMs * (1L shl reconnectionAttempts.coerceAtMost(4)), maxDelayMs)
                        val timeSinceLastAttempt = currentTime - lastReconnectionTime

                        if (reconnectMac != null && timeSinceLastAttempt >= delayMs) {
                            reconnectionAttempts++
                            lastReconnectionTime = currentTime
                            updateNotification(
                                "Reconnecting",
                                "IO module reconnect #${reconnectionAttempts}",
                            )
                            println("🔄 BLE service reconnect attempt #$reconnectionAttempts → $reconnectMac")
                            try {
                                getVanGO().connectByMac(reconnectMac)
                                delay(3_000)
                                if (getAppContext().vanGO.isConnected()) {
                                    println("✅ BLE service reconnection successful")
                                    reconnectionAttempts = 0
                                }
                            } catch (e: Exception) {
                                println("❌ BLE service reconnect failed: ${e.message}")
                            }
                        }
                    } else {
                        updateNotification("Disconnected", "BLE auto-reconnect paused")
                    }
                } catch (e: Exception) {
                    println("❌ Error in connection monitoring: ${e.message}")
                }
                delay(4_000)
            }
        }

        weatherUpdateJob?.cancel()
        weatherUpdateJob = serviceScope.launch {
            var lastWeatherUpdate = 0L
            while (isActive) {
                try {
                    val currentTime = getCurrentTimeInSeconds()
                    if (getAppContext().vanGO.isConnected() &&
                        (currentTime - lastWeatherUpdate) > 10
                    ) {
                        getAppContext().vanGO.sendWeatherInfo()
                        lastWeatherUpdate = currentTime
                    }
                } catch (e: Exception) {
                    println("❌ Error in weather update: ${e.message}")
                }
                delay(10_000)
            }
        }
    }

    private fun updateNotification(title: String, text: String) {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "VanGO::BleServiceWakeLock"
        ).apply {
            setReferenceCounted(false)
            acquire(60 * 60 * 1000L) // 1 hour; refreshed by monitor loop
        }
        println("🔋 Wake lock acquired for BLE service")
    }

    private fun ensureWakeLockHeld() {
        val lock = wakeLock
        if (lock == null || !lock.isHeld) {
            acquireWakeLock()
        } else {
            try {
                lock.acquire(60 * 60 * 1000L)
            } catch (_: Exception) {
                acquireWakeLock()
            }
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
                println("🔋 Wake lock released")
            }
        }
        wakeLock = null
    }

    private fun stopForegroundService() {
        connectionMonitorJob?.cancel()
        weatherUpdateJob?.cancel()
        serviceScope.cancel()
        serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        releaseWakeLock()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        println("🔄 VanGoBleService stopped")
    }

    override fun onDestroy() {
        super.onDestroy()
        connectionMonitorJob?.cancel()
        weatherUpdateJob?.cancel()
        releaseWakeLock()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        println("🔄 App task removed, service continues running")
    }
}

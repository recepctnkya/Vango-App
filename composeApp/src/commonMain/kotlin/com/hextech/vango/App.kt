package com.hextech.vango

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.Typography
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import com.hextech.vango.mqtt.MqttTelemetryManager
import com.hextech.vango.mqtt.mqttTelemetryManagerRef
import com.hextech.vango.profile.LoginPreferences
import com.hextech.vango.profile.SplashLogoPreferences
import com.hextech.vango.profile.syncPreferencesFromServerIfLoggedIn
import com.hextech.vango.profile.SplashLogoState
import com.hextech.vango.profile.decodeProfileImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.Clock
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import testvango.composeapp.generated.resources.Res
import testvango.composeapp.generated.resources.hexnet
import testvango.composeapp.generated.resources.dialog_devices_found_subtitle
import testvango.composeapp.generated.resources.dialog_devices_found_title
import testvango.composeapp.generated.resources.dialog_discovering
import testvango.composeapp.generated.resources.no_device_message
import testvango.composeapp.generated.resources.no_device_ok
import testvango.composeapp.generated.resources.no_device_title
import testvango.composeapp.generated.resources.perm_bt_disabled
import testvango.composeapp.generated.resources.perm_bt_unsupported
import testvango.composeapp.generated.resources.perm_denied_body
import testvango.composeapp.generated.resources.perm_denied_howto
import testvango.composeapp.generated.resources.perm_denied_step1
import testvango.composeapp.generated.resources.perm_denied_step2
import testvango.composeapp.generated.resources.perm_denied_step3
import testvango.composeapp.generated.resources.perm_denied_step4
import testvango.composeapp.generated.resources.perm_denied_step5
import testvango.composeapp.generated.resources.perm_denied_title
import testvango.composeapp.generated.resources.perm_open_settings
import testvango.composeapp.generated.resources.vango_legacy_logo

/** Page backdrop — soft gray (not harsh white) in light mode */
private val SoftLightBackground = Color(0xFFEEF2F7)
private val SoftLightSurface = Color(0xFFFFFFFF)
private val SoftLightOnSurface = Color(0xFF1A2332)

/** Dark mode: soft charcoal / blue-gray — not pure black, easier on the eyes */
private val SoftDarkBackground = Color(0xFF1A1D23)
private val SoftDarkSurface = Color(0xFF252A33)
private val SoftDarkOnSurface = Color(0xFFE8EAEF)
private val SoftBlueBackground = Color(0xFF0B1526)
private val SoftBlueSurface = Color(0xFF13233A)
private val SoftBlueOnSurface = Color(0xFFE8F1FF)

private fun vangoLightColors(primary: Color) = lightColors(
    primary = primary,
    primaryVariant = primary.copy(alpha = 0.85f),
    secondary = primary,
    background = SoftLightBackground,
    surface = SoftLightSurface,
    // Match redesign chrome ink (dark-on-accent), same as dark/blue themes.
    onPrimary = Color(0xFF061018),
    onSecondary = Color(0xFF061018),
    onBackground = SoftLightOnSurface,
    onSurface = SoftLightOnSurface,
)

private fun vangoDarkColors(primary: Color) = darkColors(
    primary = primary,
    primaryVariant = primary.copy(alpha = 0.9f),
    secondary = primary.copy(alpha = 0.95f),
    background = SoftDarkBackground,
    surface = SoftDarkSurface,
    onPrimary = Color(0xFF0D1117),
    onSecondary = Color(0xFF0D1117),
    onBackground = SoftDarkOnSurface,
    onSurface = SoftDarkOnSurface,
)

private fun vangoBlueColors(primary: Color) = darkColors(
    primary = primary,
    primaryVariant = primary.copy(alpha = 0.9f),
    secondary = primary.copy(alpha = 0.95f),
    background = SoftBlueBackground,
    surface = SoftBlueSurface,
    onPrimary = Color(0xFF061018),
    onSecondary = Color(0xFF061018),
    onBackground = SoftBlueOnSurface,
    onSurface = SoftBlueOnSurface,
)

/** Wizard içinde izin adımı tamamlandı / atlandı (BLE keşif kapısı). */
private var sessionConnectionPermGateFinished = false

private enum class LaunchPhase {
    Splash,
    Wizard,
    Main,
}

/** Splash sonrası faz. Kurulum bitmemişse Wizard zorunlu. */
private fun nextLaunchPhaseAfterSplash(): LaunchPhase {
    // Yedekten gelmiş "tamamlandı" bayrağı ama kurulum yarım → yeniden göster
    if (LoginPreferences.hasCompletedFirstUseWizard() &&
        LoginPreferences.getPreferredConnectionMode().isNullOrBlank()
    ) {
        LoginPreferences.setCompletedFirstUseWizard(false)
    }
    if (!LoginPreferences.hasCompletedFirstUseWizard()) {
        return LaunchPhase.Wizard
    }
    return LaunchPhase.Main
}

/** İlk açılışta bir kez cihaz seçim popup'ı (yalnızca ortam sekmesinde). */
private var startupDevicePickerAllowed = true
private var initialAutoDiscoveryLaunched = false
/** Uzaktan mod teklif diyaloğu oturumda bir kez; kullanıcı kapattıysa tekrar gösterilmez. */
private var remoteModeOfferHandled = false

internal fun isOnBleConnectionFlowPage(): Boolean {
    val ui = uiState.value
    return ui.selectedTab == SelectedPage.Menu &&
        (ui.settingsPage == SettingsPage.BlePair ||
            ui.settingsPage == SettingsPage.ManualDeviceAdd)
}

/**
 * Menü alt sayfaları, profil ve bağlantı akışında global BLE popup'ları gösterme.
 * Ortam (UserInfo) sekmesinde yalnızca kullanıcının başlattığı bağlantı durumu gösterilebilir.
 */
internal fun shouldSuppressGlobalBlePopups(): Boolean {
    if (isOnTabletSettingsShell()) return true
    val ui = uiState.value
    if (isOnBleConnectionFlowPage()) return true
    if (ui.selectedTab == SelectedPage.Menu && ui.settingsPage != SettingsPage.Main) return true
    if (ui.selectedTab == SelectedPage.Profile) return true
    return false
}

@Composable
private fun AppSplashOverlay() {
    val custom = SplashLogoState.customLogoBytes
    val bmp = remember(custom) { custom?.let { decodeProfileImage(it) } }
    val transition = rememberInfiniteTransition(label = "splash_pulse")
    val pulse by transition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(520, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "scale",
    )
    val alphaPulse by transition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(520, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "alpha",
    )
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background),
        contentAlignment = Alignment.Center
    ) {
        val isLandscape = maxWidth > maxHeight
        val logoWidth = if (isLandscape) maxWidth * 0.54f else maxWidth * 0.74f
        val logoHeight = if (isLandscape) maxHeight * 0.78f else maxHeight * 0.46f
        Box(
            modifier = Modifier
                .width(logoWidth.coerceIn(180.dp, 620.dp))
                .height(logoHeight.coerceIn(180.dp, 620.dp))
                .scale(pulse)
                .alpha(alphaPulse),
            contentAlignment = Alignment.Center
        ) {
            if (bmp != null) {
                Image(
                    painter = BitmapPainter(bmp),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Image(
                    painter = painterResource(Res.drawable.vango_legacy_logo),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun App() {
    LaunchedEffect(Unit) {
        val storedTheme = ThemePreferences.getThemeMode()
        if (storedTheme.isNullOrBlank()) {
            applyAppThemeMode(AppThemeMode.DARK, fromServer = true)
        } else {
            customAppThemeMode = appThemeModeFromStorage(storedTheme)
        }
        loadAppAccentFromStorage()
        loadAppCardStyleFromStorage()
        loadAppBrightnessFromStorage()
        loadClimateOutdoorSourceFromStorage()
        remoteMqttAutoReconnect.value = LoginPreferences.getRemoteAutoReconnect()
        remoteMqttCheckIntervalSec.value = LoginPreferences.getRemoteCheckIntervalSec()
        syncPreferencesFromServerIfLoggedIn()
    }
    // Keep web -> mobile preference sync fresh while app stays open.
    // Without this loop, updates are only pulled on startup / foreground transitions.
    LaunchedEffect(Unit) {
        while (isActive) {
            delay(5000)
            runCatching { syncPreferencesFromServerIfLoggedIn() }
        }
    }

    var phase by remember { mutableStateOf(LaunchPhase.Splash) }
    LaunchedEffect(Unit) {
        SplashLogoState.customLogoBytes = SplashLogoPreferences.getCustomLogoBytes()
        delay(2800)
        phase = nextLaunchPhaseAfterSplash()
        println("Launch after splash → $phase (wizardDone=${LoginPreferences.hasCompletedFirstUseWizard()})")
    }

    val systemDark = isSystemInDarkTheme()
    val colors = when (customAppThemeMode) {
        AppThemeMode.SYSTEM -> if (systemDark) vangoDarkColors(accentColorFromId(customAppAccentId))
        else vangoLightColors(accentColorFromId(customAppAccentId))
        AppThemeMode.DARK -> vangoDarkColors(accentColorFromId(customAppAccentId))
        AppThemeMode.BLUE -> vangoBlueColors(accentColorFromId(customAppAccentId))
        AppThemeMode.LIGHT -> vangoLightColors(accentColorFromId(customAppAccentId))
    }

    val typography = Typography(
        h1 = TextStyle(fontWeight = FontWeight.W100, fontSize = 96.sp),
        button = TextStyle(fontWeight = FontWeight.W600, fontSize = 14.sp)
    )

    MaterialTheme(colors = colors, typography = typography) {
        ProvideAppChrome {
            Box(Modifier.fillMaxSize()) {
                AppLocaleProvider {
                    when (phase) {
                        LaunchPhase.Splash -> { /* splash overlay below */ }
                        LaunchPhase.Wizard -> FirstUseWizardScreen(
                            onFinished = {
                                sessionConnectionPermGateFinished = true
                                println("First-use wizard finished → Main")
                                phase = LaunchPhase.Main
                            },
                        )
                        LaunchPhase.Main -> MainAppWithAutoDiscovery()
                    }
                }
                if (phase == LaunchPhase.Splash) {
                    AppSplashOverlay()
                }
            }
        }
    }
}

@Composable
fun MainAppWithAutoDiscovery() {
    val coroutineScope = rememberCoroutineScope()
    val state by vanGoGlobal.collectAsState()
    val deviceCount by vangoDeviceListSize.collectAsState()
    val remoteModeOn by remoteModeEnabled.collectAsState()
    val appConnState by appConnectionState.collectAsState()
    val mqttTelemetryManager = remember { MqttTelemetryManager() }
    val ui by uiState.collectAsState()
    var showDeviceDialog by remember { mutableStateOf(false) }
    var showRemoteModeDialog by remember { mutableStateOf(false) }
    var discoveryStarted by remember { mutableStateOf(false) }
    var isConnecting by remember { mutableStateOf(false) }
    var waitingForUserAction by remember { mutableStateOf(false) } // Flag to prevent auto-reconnection when waiting for user

    // Start device discovery automatically when app opens
    LaunchedEffect(true) {
        // First check permissions and Bluetooth status
        if(!getVanGO().checkPermissions()) {
            if (!sessionConnectionPermGateFinished) {
                vanGoGlobal.value = VanGoGlobal(connectionState = BleGattConnectionChain.PermissionDenied)
                return@LaunchedEffect
            }
            // İzin kapısı atlandı / tamamlandı — BLE olmadan ana uygulamaya devam
            println("Permissions incomplete but gate finished — continue without BLE discovery")
            return@LaunchedEffect
        }
        
        if(!getAppContext().ble().bluetoothEnabled()) {
            vanGoGlobal.value = VanGoGlobal(connectionState = BleGattConnectionChain.BluetoothNotEnabled)
            return@LaunchedEffect
        }

        if(!getAppContext().ble().bluetoothSupport()) {
            vanGoGlobal.value = VanGoGlobal(connectionState = BleGattConnectionChain.BluetoothNotSupported)
            return@LaunchedEffect
        }

        // Start automatic discovery / reconnect once per app process
        if (!initialAutoDiscoveryLaunched) {
            initialAutoDiscoveryLaunched = true
            waitingForUserAction = false
            bleAutoReconnectEnabled.value = true
            BleAutoConnect.resetThrottle()
            seedIoModuleFromPlatformRegistration()
            println("Starting automatic VanGO IO module discovery + auto-connect...")
            // Önce tara (cihaz reklamda görünür); ardından bilinen MAC varsa bağlanmayı dene
            getVanGO().discoverVangos()
            delay(1_500L)
            if (!isBleGattLinkReady() && !remoteModeEnabled.value) {
                BleAutoConnect.tryAutoConnect("startup")
            }
        }

        var previousConnectionState = vanGoGlobal.value.connectionState
        var wasConnected = false
        var lastReconnectionAttempt = 0L

        // Note: Background operations (MTU config, notifications, weather updates) 
        // are now handled by VanGoBleService when app goes to background
        // Only handle immediate connection setup here
        while(true) {
            delay(100L)

            val currentState = vanGoGlobal.value.connectionState
            val currentTime = Clock.System.now().toEpochMilliseconds()

            if (appConnectionState.value == AppConnectionState.BLE_CONNECTED &&
                !isBleGattLinkReady() &&
                !isBleHandshakeInProgress()
            ) {
                appConnectionState.value = AppConnectionState.OFFLINE
            }
            
            // Track if we were previously connected
            if (currentState == BleGattConnectionChain.ChainEnded) {
                wasConnected = true
                waitingForUserAction = false // Reset when connected
            }

            // Check for disconnection: if we were connected and now we're not, or if we're in a failed state
            // Don't treat DiscoverFinished as disconnection - it's a normal end state that should show the dialog
            // Also don't treat as disconnection if we're waiting for user action
            val isDisconnected = when {
                currentState == BleGattConnectionChain.ChainEnded -> false
                currentState == BleGattConnectionChain.DiscoverFinished -> false // Don't treat as disconnection
                waitingForUserAction -> false // Don't treat as disconnection when waiting for user
                currentState == BleGattConnectionChain.ConnectingGattServerFailed -> wasConnected || previousConnectionState == BleGattConnectionChain.ChainEnded
                currentState == BleGattConnectionChain.ServicesDiscoveringFailed -> wasConnected || previousConnectionState == BleGattConnectionChain.ChainEnded
                currentState == BleGattConnectionChain.UnexpectedError -> wasConnected || previousConnectionState == BleGattConnectionChain.ChainEnded
                else -> {
                    // If state changed from ChainEnded to something else, it's a disconnection
                    // But don't treat DiscoverFinished as disconnection
                    previousConnectionState == BleGattConnectionChain.ChainEnded && 
                    currentState != BleGattConnectionChain.DiscoverFinished
                }
            }

            // Cihaz eşleştir / manuel ekleme: kullanıcı taramayı kendisi yönetir.
            // Tablet Ayarlar → Bağlantı otomatik BLE bağlanmaya izin verir.
            if (isOnBleConnectionFlowPage()) {
                previousConnectionState = currentState
                continue
            }

            // Unexpected drop: reconnect last IO module by MAC (avoid wiping list with rescan)
            if (isDisconnected && !waitingForUserAction &&
                !isBleGattLinkReady() &&
                !remoteModeOn &&
                shouldAttemptBleAutoReconnect() &&
                currentState != BleGattConnectionChain.DiscoveringDevice &&
                currentState != BleGattConnectionChain.DiscoverFinished &&
                currentState != BleGattConnectionChain.ConnectingGattServer &&
                currentState != BleGattConnectionChain.DiscoveringServices &&
                (currentTime - lastReconnectionAttempt) > 8_000L
            ) {
                val reconnectMac = resolveLastBleReconnectMac()
                println("🔄 BLE connection lost. Reconnecting to last IO module: $reconnectMac")

                vanGoGlobal.value = vanGoGlobal.value.copy(
                    currentMtu = 20,
                    isCharacteristicNotificationOn = false,
                )

                wasConnected = false
                discoveryStarted = false
                lastReconnectionAttempt = currentTime

                delay(400L)
                if (reconnectMac != null && shouldAttemptBleAutoReconnect() && !remoteModeEnabled.value) {
                    BleAutoConnect.resetThrottle()
                    getVanGO().connectByMac(reconnectMac)
                }
            }

            // Check if device is connected and handle initial setup
            if(currentState == BleGattConnectionChain.ChainEnded) {
                if(vanGoGlobal.value.operation() == Operations.Idle) {
                    if(vanGoGlobal.value.currentMtu == 20) {
                        getAppContext().vanGO.configureMtu()
                    }
                    // Notification setup is triggered from OnMtuConfigured after a short delay.
                }
            }

            previousConnectionState = currentState
        }
    }

    LaunchedEffect(remoteModeOn) {
        if (remoteModeOn) {
            println("Remote mode accepted")
            val started = RemoteMqttSession.startRemotePipeline(
                mqttTelemetryManager,
                currentPlatformDeviceMac(),
            )
            if (!started) {
                remoteModeEnabled.value = false
            }
        } else {
            mqttTelemetryManager.disconnect()
            remoteMqttRoute.value = null
            hexnetPanelDevice.value = null
        }
    }

    val autoReconnect by remoteMqttAutoReconnect.collectAsState()
    val reconnectIntervalSec by remoteMqttCheckIntervalSec.collectAsState()
    LaunchedEffect(remoteModeOn, autoReconnect, reconnectIntervalSec) {
        if (!remoteModeOn || !autoReconnect) return@LaunchedEffect
        while (remoteModeEnabled.value) {
            delay(reconnectIntervalSec.coerceAtLeast(15) * 1000L)
            if (!remoteModeEnabled.value) break
            if (isAppRemoteMqttConnected() || resolvedPanelDeviceId() == null) continue
            println("Remote MQTT auto-reconnect attempt...")
            RemoteMqttSession.startRemotePipeline(
                mqttTelemetryManager,
                currentPlatformDeviceMac(),
            )
        }
    }

    LaunchedEffect(mqttTelemetryManager) {
        mqttTelemetryManagerRef.value = mqttTelemetryManager
    }

    LaunchedEffect(appConnState) {
        if (appConnState != AppConnectionState.BLE_CONNECTED) return@LaunchedEffect
        repeat(3) { attempt ->
            if (PanelSettingsPlatformSync.refreshEnvSensorsFromPlatformWhileBle()) return@LaunchedEffect
            if (attempt < 2) delay(2_000L)
        }
        while (appConnectionState.value == AppConnectionState.BLE_CONNECTED) {
            delay(12_000L)
            PanelSettingsPlatformSync.refreshEnvSensorsFromPlatformWhileBle()
        }
    }

    LaunchedEffect(state.connectionState) {
        when (state.connectionState) {
            BleGattConnectionChain.ChainEnded -> {
                syncBleAppConnectionStateIfLinked()
                if (isBleGattLinkReady()) {
                    rememberLastBleAdapterMac(getAppContext().ble().connectedDeviceMac())
                    activateBleModeExclusive()
                    showRemoteModeDialog = false
                    remoteModeOfferHandled = false
                    repeat(3) { attempt ->
                        if (PanelSettingsPlatformSync.refreshEnvSensorsFromPlatformWhileBle()) return@LaunchedEffect
                        if (attempt < 2) delay(2_000L)
                    }
                }
            }
            BleGattConnectionChain.ConnectingGattServerFailed,
            BleGattConnectionChain.ServicesDiscoveringFailed,
            BleGattConnectionChain.UnexpectedError,
            BleGattConnectionChain.BluetoothNotEnabled -> {
                // Geçici hata: donanım hâlâ bağlıysa OFFLINE yazma
                if (!remoteModeEnabled.value && !isBleGattLinkReady() &&
                    !getAppContext().vanGO.isConnected()
                ) {
                    appConnectionState.value = AppConnectionState.OFFLINE
                }
            }
            else -> Unit
        }
    }

    LaunchedEffect(ui.selectedTab, ui.settingsPage) {
        if (shouldSuppressGlobalBlePopups()) {
            showDeviceDialog = false
            showRemoteModeDialog = false
        }
        if (isOnBleConnectionFlowPage()) {
            startupDevicePickerAllowed = false
        }
    }

    val supportedBleDevices = remember(deviceCount) {
        preferBleAdapterDevices(
            vangoDeviceList.filter { isSupportedBleDeviceName(it.name) },
        )
    }

    // Compose tarafı: cihaz listesi değişince merkezi auto-connect'i tetikle
    LaunchedEffect(deviceCount, state.connectionState, remoteModeOn) {
        if (remoteModeOn || isBleGattLinkReady()) return@LaunchedEffect
        if (!bleAutoReconnectEnabled.value) return@LaunchedEffect
        if (isOnBleConnectionFlowPage()) return@LaunchedEffect
        if (state.connectionState == BleGattConnectionChain.ConnectingGattServer ||
            state.connectionState == BleGattConnectionChain.DiscoveringServices ||
            state.connectionState == BleGattConnectionChain.ChainEnded
        ) {
            return@LaunchedEffect
        }
        waitingForUserAction = false
        isConnecting = false
        BleAutoConnect.tryAutoConnect("ui-list")

        val live = preferBleAdapterDevices(
            vangoDeviceList.filter { isSupportedBleDeviceName(it.name) && it.rssi != null },
        )
        if (live.size > 1 && startupDevicePickerAllowed &&
            ui.selectedTab == SelectedPage.UserInfo &&
            !isBleGattLinkReady()
        ) {
            showDeviceDialog = true
            startupDevicePickerAllowed = false
        }
    }

    // BLE fail → tekrar dene
    LaunchedEffect(state.connectionState) {
        if (remoteModeOn || isOnBleConnectionFlowPage()) return@LaunchedEffect
        if (state.connectionState != BleGattConnectionChain.ConnectingGattServerFailed &&
            state.connectionState != BleGattConnectionChain.ServicesDiscoveringFailed
        ) {
            return@LaunchedEffect
        }
        waitingForUserAction = false
        isConnecting = false
        showRemoteModeDialog = false
        delay(1_500L)
        if (isBleGattLinkReady() || remoteModeEnabled.value) return@LaunchedEffect
        if (!bleAutoReconnectEnabled.value) return@LaunchedEffect
        BleAutoConnect.resetThrottle()
        BleAutoConnect.tryAutoConnect("retry-failed")
        if (!isBleGattLinkReady() &&
            vanGoGlobal.value.connectionState != BleGattConnectionChain.ConnectingGattServer &&
            vanGoGlobal.value.connectionState != BleGattConnectionChain.DiscoveringServices
        ) {
            getVanGO().discoverVangos()
        }
    }

    LaunchedEffect(state.connectionState, deviceCount, remoteModeOn, ui.selectedTab, ui.settingsPage) {
        // BLE bağlı / bağlanıyor / tarıyorsa Remote teklifini asla gösterme
        if (isBleGattLinkReady() ||
            appConnectionState.value == AppConnectionState.BLE_CONNECTED ||
            state.connectionState == BleGattConnectionChain.ChainEnded ||
            state.connectionState == BleGattConnectionChain.ConnectingGattServer ||
            state.connectionState == BleGattConnectionChain.DiscoveringServices ||
            state.connectionState == BleGattConnectionChain.DiscoveringDevice ||
            state.connectionState == BleGattConnectionChain.ConnectingGattServerFailed ||
            state.connectionState == BleGattConnectionChain.ServicesDiscoveringFailed
        ) {
            // Geçici BLE hatalarında Remote popup gösterme — yeniden dene
            showRemoteModeDialog = false
            if (state.connectionState == BleGattConnectionChain.ConnectingGattServerFailed ||
                state.connectionState == BleGattConnectionChain.ServicesDiscoveringFailed
            ) {
                // waitingForUserAction BLE yeniden denemesini kilitlemesin
                waitingForUserAction = false
            }
            return@LaunchedEffect
        }

        // Yalnızca tarama bitti ve ortamda hiç IO yoksa (veya BT kapalıysa) Remote teklif et
        val bleUnavailable =
            state.connectionState == BleGattConnectionChain.BluetoothNotEnabled ||
            (state.connectionState == BleGattConnectionChain.DiscoverFinished && deviceCount == 0)

        if (
            bleUnavailable &&
            !remoteModeOn &&
            !remoteModeOfferHandled &&
            !shouldSuppressGlobalBlePopups() &&
            ui.selectedTab == SelectedPage.UserInfo &&
            ui.settingsPage == SettingsPage.Main
        ) {
            // Kısa gecikme: tek cihaz geç gelebilir
            delay(2_500L)
            if (isBleGattLinkReady() || deviceCount > 0 ||
                vanGoGlobal.value.connectionState != state.connectionState
            ) {
                return@LaunchedEffect
            }
            waitingForUserAction = true
            showRemoteModeDialog = true
        } else if (shouldSuppressGlobalBlePopups()) {
            showRemoteModeDialog = false
        }
    }

    // Reset connecting flag when discovery finishes
    LaunchedEffect(state.connectionState, deviceCount) {
        if (state.connectionState == BleGattConnectionChain.DiscoverFinished) {
            isConnecting = false
            discoveryStarted = false
            waitingForUserAction = false
            BleAutoConnect.tryAutoConnect("discover-finished-ui")
        }
    }

    // Handle fatal BLE states; keep MainApp() mounted for all other states so dashboard
    // settings (e.g. Bağlantı) are not reset when connectionState toggles during scan.
    when (state.connectionState) {
        BleGattConnectionChain.BluetoothNotSupported -> UserInfoOnly { BluetoothNotSupported() }
        BleGattConnectionChain.PermissionDenied -> {
            // Wizard sonrası yedek kapı — tamamlanınca ana uygulamaya dön
            PermissionDenied(
                onEnterApp = { startDiscovery ->
                    sessionConnectionPermGateFinished = true
                    vanGoGlobal.value = vanGoGlobal.value.copy(
                        connectionState = BleGattConnectionChain.Idle,
                    )
                    if (startDiscovery && getVanGO().checkPermissions()) {
                        initialAutoDiscoveryLaunched = true
                        bleAutoReconnectEnabled.value = true
                        BleAutoConnect.resetThrottle()
                        seedIoModuleFromPlatformRegistration()
                        println("Permissions ready — starting discovery...")
                        getVanGO().discoverVangos()
                        coroutineScope.launch {
                            delay(1_500L)
                            if (!isBleGattLinkReady() && !remoteModeEnabled.value) {
                                BleAutoConnect.tryAutoConnect("permissions-ready")
                            }
                        }
                    }
                },
            )
        }
        BleGattConnectionChain.UnexpectedError -> UserInfoOnly { UnexpectedError() }
        else -> MainApp()
    }

    if (state.connectionState != BleGattConnectionChain.BluetoothNotSupported &&
        state.connectionState != BleGattConnectionChain.PermissionDenied &&
        state.connectionState != BleGattConnectionChain.UnexpectedError &&
        !shouldSuppressGlobalBlePopups() &&
        isConnecting &&
        (state.connectionState == BleGattConnectionChain.ConnectingGattServer ||
            state.connectionState == BleGattConnectionChain.ConnectingGattServerFailed ||
            state.connectionState == BleGattConnectionChain.DiscoveringServices ||
            state.connectionState == BleGattConnectionChain.ServicesDiscoveringFailed)
    ) {
        ConnectionStatusDialog(state.connectionState)
    }

    if (showDeviceDialog && supportedBleDevices.isNotEmpty() && ui.selectedTab == SelectedPage.UserInfo) {
        DeviceSelectionDialog(
            devices = supportedBleDevices,
            onDeviceSelected = { device ->
                showDeviceDialog = false
                isConnecting = true
                waitingForUserAction = false
                bleAutoReconnectEnabled.value = true
                getVanGO().connectByMac(device.mac)
            },
            onDismiss = {
                showDeviceDialog = false
                waitingForUserAction = false
            },
            onRetryDiscovery = {
                showDeviceDialog = false
                waitingForUserAction = false
                getVanGO().discoverVangos()
            }
        )
    }

        if (
        showRemoteModeDialog &&
        appConnState != AppConnectionState.BLE_CONNECTED &&
        !isBleGattLinkReady() &&
        !remoteModeOn &&
        !shouldSuppressGlobalBlePopups() &&
        state.connectionState != BleGattConnectionChain.ConnectingGattServer &&
        state.connectionState != BleGattConnectionChain.DiscoveringServices &&
        state.connectionState != BleGattConnectionChain.DiscoveringDevice &&
        state.connectionState != BleGattConnectionChain.ConnectingGattServerFailed &&
        state.connectionState != BleGattConnectionChain.ServicesDiscoveringFailed &&
        state.connectionState != BleGattConnectionChain.ChainEnded
    ) {
        RemoteModeOfferDialog(
            onAccept = {
                remoteModeEnabled.value = true
                showRemoteModeDialog = false
                remoteModeOfferHandled = true
                waitingForUserAction = true
                openRemoteConnectionPage()
            },
            onCancel = {
                showRemoteModeDialog = false
                remoteModeOfferHandled = true
                waitingForUserAction = false
                bleAutoReconnectEnabled.value = true
                // Kullanıcı iptal etti — BLE taramasına dön
                getVanGO().discoverVangos()
            }
        )
    }
}

@Composable
fun RemoteModeOfferDialog(
    onAccept: () -> Unit,
    onCancel: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .background(
                    color = MaterialTheme.colors.surface,
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "BLE bağlantısı yok",
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colors.onSurface
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Cihaza BLE ile bağlanılamadı. İnternet üzerinden Remote moda geçmek ister misiniz?",
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onAccept,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Remote Bağlan")
                }
                Button(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("İptal")
                }
            }
        }
    }
}

@Composable
fun DiscoveryIndicator() {
    // Tarama göstergesi yalnızca Bağlantı > Cihaz eşleştir ekranında; global overlay kullanılmaz.
}

@Composable
fun DeviceSelectionDialog(
    devices: List<BleDevice>,
    onDeviceSelected: (BleDevice) -> Unit,
    onDismiss: () -> Unit,
    onRetryDiscovery: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .background(
                    color = MaterialTheme.colors.surface,
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(20.dp)
                .clickable(enabled = false) { /* Prevent dismissing when clicking inside dialog */ },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(Res.string.dialog_devices_found_title),
                style = MaterialTheme.typography.h6,
                color = MaterialTheme.colors.onSurface,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(Modifier.height(5.dp))
            Text(
                text = stringResource(Res.string.dialog_devices_found_subtitle),
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(5.dp))
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                devices.forEachIndexed { index, device ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .background(
                                color = MaterialTheme.colors.primary.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { onDeviceSelected(device) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = device.name.ifEmpty { "Unknown Device" },
                                style = MaterialTheme.typography.body1,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colors.onSurface
                            )
                            Text(
                                text = device.mac,
                                style = MaterialTheme.typography.caption,
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                    
                    if (index < devices.size - 1) {
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ConnectionStatusDialog(connectionState: BleGattConnectionChain) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .background(
                    color = MaterialTheme.colors.surface,
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SpiralWaitingIcon(Modifier.size(48.dp))
            
            Spacer(Modifier.height(16.dp))
            
            val statusText = when(connectionState) {
                BleGattConnectionChain.ConnectingGattServer -> "Connecting to device..."
                BleGattConnectionChain.ConnectingGattServerFailed -> "Connection failed"
                BleGattConnectionChain.DiscoveringServices -> "Discovering services..."
                BleGattConnectionChain.ServicesDiscoveringFailed -> "Service discovery failed"
                else -> "Processing..."
            }
            
            Text(
                text = statusText,
                style = MaterialTheme.typography.body1,
                color = MaterialTheme.colors.onSurface,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun NoDevicesFoundDialog(onOk: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .background(
                    color = MaterialTheme.colors.surface,
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(Res.string.no_device_title),
                style = MaterialTheme.typography.h6,
                color = MaterialTheme.colors.onSurface,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(Res.string.no_device_message),
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.75f),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))

            Button(onClick = onOk) {
                Text(stringResource(Res.string.no_device_ok))
            }
        }
    }
}

/**
 * Runs the provided [action] lambda at a specified [interval] until the [timeout] is reached.
 *
 * @param interval The delay between each execution of the action.
 * @param timeout The total duration after which the execution stops.
 * @param action The lambda or function to execute periodically.
 * @return A Job that can be used to cancel the execution manually.
 */
fun doInTimeout(
    timeout: Long,
    interval: Long,
    action: suspend (routine: CoroutineScope) -> Unit,
    onTimeout: suspend () -> Unit
): Job {
    val scope = CoroutineScope(Dispatchers.Default) // Use Default for background execution

    return scope.launch {
        try {
            withTimeout(timeout) {
                while(true) {
                    action(this) // Execute the provided lambda
                    delay(interval)
                }
            }
        } catch (e: TimeoutCancellationException) {
            onTimeout()
        }
    }
}

@Composable
fun BluetoothNotSupported() {
    Text(stringResource(Res.string.perm_bt_unsupported))
}

@Composable
fun BluetoothNotEnabled() {
    Text(stringResource(Res.string.perm_bt_disabled))
}

@Composable
fun PermissionDenied(
    onEnterApp: (startDiscovery: Boolean) -> Unit = {},
) {
    ConnectionPermissionsScreen(onEnterApp = onEnterApp)
}

@Composable
fun DeviceNotFound() {
    Text("")
    Spacer(Modifier.height(10.dp))
    Button(
        modifier = Modifier.wrapContentSize(),
        onClick = {
//            getAppContext().vanGO.connect()
        }
    ) {
        Text("Re try")
    }
}

@Composable
fun UnexpectedError() {
    Text("Unexpected Error")
}

@Composable
fun CommunicationState() {
    Text("CommunicationState")
}

@Composable
fun ValueUpdating() {
    Text("ValueUpdating")
}

@Composable
fun ConnectDevice(
    onReset:()->Unit
) {
    var stateButton by remember { mutableStateOf(0) }
    var connect by remember { mutableStateOf(false) }

    LaunchedEffect(connect) {
        if (connect) {
//            getPlatform().launchBTController()
            stateButton = 1
        }
    }

    when(stateButton) {
        0 -> {
            Text("No device is connected!")
            Spacer(Modifier.height(10.dp))
            Button(
                modifier = Modifier.wrapContentSize(),
                onClick = {connect = true}
            ) {
                Text("Connect")
            }
        }
        1 -> {
            Text("Re try to search for a VanGO device!")
            Spacer(Modifier.height(10.dp))
            Button(
                modifier = Modifier.wrapContentSize(),
                onClick = onReset
            ) {
                Text("Re try")
            }
        }
    }
}

/**
 * text typography could be arranged for better user experience
 */
@Composable
fun UserInfoOnly(
    content: @Composable() () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally

    ) {
        content()
    }
}

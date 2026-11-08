package com.hextech.vango

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hextech.vango.mqtt.MqttCredentialsProvider
import com.hextech.vango.profile.LoginPreferences
import com.hextech.vango.profile.clearExpiredAuthTokenIfNeeded
import com.hextech.vango.profile.hasUsableAuthToken
import com.hextech.vango.profile.profileRepositorySingleton
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import testvango.composeapp.generated.resources.Res
import testvango.composeapp.generated.resources.connection_copied
import testvango.composeapp.generated.resources.connection_device_name_label
import testvango.composeapp.generated.resources.connection_disconnect
import testvango.composeapp.generated.resources.connection_io_disconnected
import testvango.composeapp.generated.resources.connection_io_display_name
import testvango.composeapp.generated.resources.connection_io_idle
import testvango.composeapp.generated.resources.connection_io_section_desc
import testvango.composeapp.generated.resources.connection_io_section_title
import testvango.composeapp.generated.resources.connection_io_success
import testvango.composeapp.generated.resources.connection_last_link_label
import testvango.composeapp.generated.resources.connection_mac_label
import testvango.composeapp.generated.resources.connection_mode_ble_title
import testvango.composeapp.generated.resources.connection_mode_pick_label
import testvango.composeapp.generated.resources.connection_mode_remote_title
import testvango.composeapp.generated.resources.connection_refresh
import testvango.composeapp.generated.resources.connection_remote_active
import testvango.composeapp.generated.resources.connection_remote_inactive
import testvango.composeapp.generated.resources.connection_remote_internet_hint
import testvango.composeapp.generated.resources.connection_remote_save
import testvango.composeapp.generated.resources.connection_remote_section_desc
import testvango.composeapp.generated.resources.connection_remote_section_title
import testvango.composeapp.generated.resources.connection_remote_state_label
import testvango.composeapp.generated.resources.connection_remote_test
import testvango.composeapp.generated.resources.connection_remote_test_ok
import testvango.composeapp.generated.resources.connection_signal_label
import testvango.composeapp.generated.resources.connection_signal_medium_short
import testvango.composeapp.generated.resources.connection_signal_strong_short
import testvango.composeapp.generated.resources.connection_signal_weak_short
import testvango.composeapp.generated.resources.connection_status_connected
import testvango.composeapp.generated.resources.connection_status_disconnected
import testvango.composeapp.generated.resources.connection_status_label
import testvango.composeapp.generated.resources.connection_status_searching
import testvango.composeapp.generated.resources.connection_uid_label
import testvango.composeapp.generated.resources.ios_ble_remote_only_hint
import testvango.composeapp.generated.resources.login_error_invalid
import testvango.composeapp.generated.resources.phone_caravan_hero
import testvango.composeapp.generated.resources.remote_err_credentials
import testvango.composeapp.generated.resources.remote_password_label
import testvango.composeapp.generated.resources.remote_username_label

private val ConnCyan = Color(0xFF2EC5FF)
private val ConnText: Color @Composable get() = LocalAppChrome.current.text
private val ConnMuted: Color @Composable get() = LocalAppChrome.current.muted
private val ConnBorder: Color @Composable get() = LocalAppChrome.current.border
private val ConnGreen = Color(0xFF22C55E)
private val ConnTile: Color @Composable get() = LocalAppChrome.current.tile
private val ConnCard: Color @Composable get() = LocalAppChrome.current.card
private val ConnInk: Color @Composable get() = LocalAppChrome.current.ink

@Composable
fun ConnectionRedesignPage() {
    val global by vanGoGlobal.collectAsState()
    val appConn by appConnectionState.collectAsState()
    val remoteModeOn by remoteModeEnabled.collectAsState()
    val sessionErr by remoteSessionError.collectAsState()
    val lastTelemetryAt by lastSuccessfulTelemetryEpochSec.collectAsState()
    val deviceCount by vangoDeviceListSize.collectAsState()
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var copiedFlash by remember { mutableStateOf(false) }
    val bleSupported = platformSupportsBle()
    var preferRemote by remember {
        mutableStateOf(
            !bleSupported ||
                (remoteModeOn && appConn != AppConnectionState.BLE_CONNECTED) ||
                LoginPreferences.getPreferredConnectionMode() == "remote",
        )
    }
    // Prefill myhexnet account identity only — never MQTT broker creds (those fail /auth/login).
    var remoteUsername by remember {
        mutableStateOf(
            LoginPreferences.getRememberedEmail()?.trim().orEmpty()
                .ifEmpty { LoginPreferences.getCachedAccountEmail()?.trim().orEmpty() }
                .ifEmpty { LoginPreferences.getCachedUsername()?.trim().orEmpty() },
        )
    }
    var remotePassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var remoteBusy by remember { mutableStateOf(false) }
    var remoteLocalError by remember { mutableStateOf<String?>(null) }
    val credentialsErrorText = stringResource(Res.string.remote_err_credentials)
    val invalidLoginText = stringResource(Res.string.login_error_invalid)

    var linkClock by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            linkClock++
        }
    }

    val bleLinked = remember(global, lastTelemetryAt, linkClock) { isBleGattLinkReady() }
    val remoteLinked = appConn == AppConnectionState.REMOTE_MQTT_CONNECTED
    val bleDiscovering = !bleLinked && isBleHandshakeInProgress()
    val bleMode = bleSupported && !preferRemote

    val connectedMac = currentBleAdapterMac()
        ?: LoginPreferences.getCachedLastBleAdapterMac()
    val displayMac = (if (bleLinked) {
        currentPlatformDeviceMac() ?: connectedMac
    } else {
        currentPlatformDeviceMac() ?: connectedMac
    })?.takeIf { it.isNotBlank() } ?: "-"
    val ioName = stringResource(Res.string.connection_io_display_name)
    val liveDevice = remember(deviceCount, connectedMac, bleLinked) {
        val mac = connectedMac
        if (mac.isNullOrBlank()) {
            vangoDeviceList.firstOrNull { it.rssi != null && isSupportedBleDeviceName(it.name) }
        } else {
            vangoDeviceList.firstOrNull { macsEqual(it.mac, mac) }
        }
    }
    val deviceName = liveDevice?.name?.takeIf { it.isNotBlank() } ?: ioName
    val rssi = liveDevice?.rssi
    val lastLinkEpoch = when {
        lastTelemetryAt > 0L -> lastTelemetryAt
        global.receivedPackageTime > 0L -> global.receivedPackageTime
        else -> 0L
    }
    val lastLinkText = formatConnectionTimestamp(lastLinkEpoch)
    val deviceUid = LoginPreferences.getCachedDeviceUid()?.trim()
        ?.takeUnless { it.isEmpty() || it == "-" || it == "\u2014" }
        ?: "—"
    val testOkText = stringResource(Res.string.connection_remote_test_ok)

    LaunchedEffect(bleMode) {
        if (bleMode) {
            activateBleModeExclusive()
        } else {
            disconnectBleLink(suppressAutoReconnect = true)
        }
    }
    LaunchedEffect(bleMode, bleLinked, remoteModeOn, global.connectionState) {
        if (!bleMode) return@LaunchedEffect
        if (bleLinked || isBleHardwareConnected()) return@LaunchedEffect
        val st = vanGoGlobal.value.connectionState
        if (st == BleGattConnectionChain.ConnectingGattServer ||
            st == BleGattConnectionChain.DiscoveringServices
        ) {
            return@LaunchedEffect
        }
        bleAutoReconnectEnabled.value = true
        BleAutoConnect.resetThrottle()
        getVanGO().discoverVangos()
        BleAutoConnect.tryAutoConnect("connection-page")
    }
    LaunchedEffect(copiedFlash) {
        if (!copiedFlash) return@LaunchedEffect
        kotlinx.coroutines.delay(1400)
        copiedFlash = false
    }

    fun copyText(value: String) {
        if (value.isBlank() || value == "-") return
        clipboard.setText(AnnotatedString(value))
        copiedFlash = true
    }
    fun selectBle() {
        preferRemote = false
        remoteLocalError = null
        activateBleModeExclusive()
        if (!isBleGattLinkReady()) {
            getVanGO().discoverVangos()
            BleAutoConnect.tryAutoConnect("mode-ble")
        }
    }
    fun selectRemote() {
        preferRemote = true
        remoteLocalError = null
        disconnectBleLink(suppressAutoReconnect = true)
    }
    fun runRemoteConnect(activate: Boolean) {
        if (remoteBusy) return
        LoginPreferences.clearExpiredAuthTokenIfNeeded()
        val tokenUsable = LoginPreferences.hasUsableAuthToken()
        val user = remoteUsername.trim()
        val pass = remotePassword
        val mustSignIn = !tokenUsable || pass.isNotEmpty()
        if (mustSignIn && (user.isEmpty() || pass.isEmpty())) {
            remoteLocalError = credentialsErrorText
            return
        }
        remoteBusy = true
        remoteLocalError = null
        scope.launch {
            try {
                if (mustSignIn) {
                    val signedIn = profileRepositorySingleton.signInWithCredentials(user, pass)
                    if (signedIn.isFailure) {
                        remoteLocalError = signedIn.exceptionOrNull()?.message ?: invalidLoginText
                        return@launch
                    }
                }
                val mqttUser = LoginPreferences.getCachedMqttUsername()?.trim().orEmpty()
                val mqttPass = LoginPreferences.getCachedMqttPassword()?.takeIf { it.isNotEmpty() }
                val accountUser = LoginPreferences.getCachedUsername()?.trim().orEmpty()
                    .ifEmpty { LoginPreferences.getCachedAccountEmail()?.trim().orEmpty() }
                val mqttLooksLikeAccount = mqttUser.isNotEmpty() &&
                    accountUser.isNotEmpty() &&
                    mqttUser.equals(accountUser, ignoreCase = true) &&
                    mqttPass == pass
                if (mqttUser.isNotEmpty() && mqttPass != null && !mqttLooksLikeAccount) {
                    MqttCredentialsProvider.saveUserCredentials(
                        mqttUser,
                        mqttPass,
                        LoginPreferences.getCachedMqttDeviceId() ?: 0,
                    )
                } else if (mqttLooksLikeAccount) {
                    MqttCredentialsProvider.clearCache()
                }
                val mac = currentPlatformDeviceMac()
                    ?: LoginPreferences.getCachedPairedDeviceMac()
                    ?: connectedMac
                val session = RemoteMqttSession.prepareSession(mac, LoginPreferences.getCachedDeviceUid())
                if (session?.panelDevice == null) {
                    return@launch
                }
                if (activate) {
                    activateRemoteModeExclusive()
                    preferRemote = true
                    LoginPreferences.setPreferredConnectionMode("remote")
                } else {
                    remoteLocalError = null
                    remoteSessionError.value = null
                    // Geçici başarı mesajı error alanına yeşil değil; sessionErr temiz, toast yok —
                    // kullanıcıya kısa bilgi için localError yerine sessiz başarı:
                    remoteLocalError = testOkText
                }
            } finally {
                remoteBusy = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .padding(
                horizontal = if (isCompactLayout()) 14.dp else 24.dp,
                vertical = if (isCompactLayout()) 12.dp else 20.dp,
            )
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(if (isCompactLayout()) 12.dp else 14.dp),
    ) {
        ConnectionHeroBanner(Modifier.fillMaxWidth().height(148.dp))

        Text(
            stringResource(Res.string.connection_mode_pick_label),
            color = ConnMuted,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (bleSupported) {
                ModeSelectCard(
                    modifier = Modifier.weight(1f),
                    selected = bleMode,
                    icon = Icons.Filled.Bluetooth,
                    title = stringResource(Res.string.connection_mode_ble_title),
                    onClick = { selectBle() },
                )
            }
            ModeSelectCard(
                modifier = Modifier.weight(1f),
                selected = !bleMode,
                icon = Icons.Filled.Cloud,
                title = stringResource(Res.string.connection_mode_remote_title),
                onClick = { selectRemote() },
            )
        }
        if (!bleSupported) {
            Text(
                stringResource(Res.string.ios_ble_remote_only_hint),
                color = ConnMuted,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // Selected mode drives the page body (BLE ↔ Remote), like phone connection flow.
        if (bleMode) {
            IoModuleCard(
                interactive = true,
                connected = bleLinked,
                discovering = bleDiscovering && !bleLinked,
                deviceName = deviceName,
                mac = displayMac,
                rssi = rssi,
                lastLinkText = lastLinkText,
                copiedFlash = copiedFlash,
                onCopyMac = { copyText(displayMac) },
                onDisconnect = { disconnectBleLink(suppressAutoReconnect = true) },
                onRefresh = {
                    activateBleModeExclusive()
                    getVanGO().discoverVangos()
                    BleAutoConnect.resetThrottle()
                    BleAutoConnect.tryAutoConnect("refresh")
                },
            )
        } else {
            RemoteModuleCard(
                interactive = true,
                active = remoteLinked || remoteModeOn,
                mqttConnected = remoteLinked,
                credentialsEditable = !remoteLinked,
                deviceUid = deviceUid,
                error = remoteLocalError ?: sessionErr,
                username = remoteUsername,
                password = remotePassword,
                passwordVisible = passwordVisible,
                busy = remoteBusy,
                onUsernameChange = { remoteUsername = it; remoteLocalError = null },
                onPasswordChange = { remotePassword = it; remoteLocalError = null },
                onClearUsername = { remoteUsername = ""; remoteLocalError = null },
                onTogglePasswordVisible = { passwordVisible = !passwordVisible },
                onCopyUid = { copyText(deviceUid) },
                onTest = { runRemoteConnect(activate = false) },
                onSave = { runRemoteConnect(activate = true) },
            )
        }

        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun ConnectionHeroBanner(bannerModifier: Modifier) {
    Box(
        modifier = bannerModifier
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, ConnBorder, RoundedCornerShape(18.dp)),
    ) {
        Image(
            painter = painterResource(Res.drawable.phone_caravan_hero),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0x66070B12), Color(0x22070B12), Color(0x99070B12)),
                    ),
                ),
        )
        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 28.dp),
            horizontalArrangement = Arrangement.spacedBy(56.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ConnectionHeroOrb(Icons.Filled.Bluetooth)
            ConnectionHeroOrb(Icons.Filled.Cloud)
        }
    }
}

@Composable
private fun ConnectionHeroOrb(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .background(ConnCyan.copy(alpha = 0.18f), CircleShape)
            .border(1.5.dp, ConnCyan.copy(alpha = 0.85f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = ConnCyan, modifier = Modifier.size(26.dp))
    }
}

@Composable
private fun ModeSelectCard(
    modifier: Modifier,
    selected: Boolean,
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
) {
    val border = if (selected) ConnCyan else ConnBorder
    val bg = if (selected) ConnCyan.copy(alpha = 0.08f) else ConnTile
    val tint = if (selected) ConnCyan else ConnMuted
    Row(
        modifier = modifier
            .height(72.dp)
            .background(bg, RoundedCornerShape(16.dp))
            .border(1.5.dp, border, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            title,
            color = if (selected) ConnCyan else ConnText,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (selected) {
            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = ConnCyan,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun IoModuleCard(
    interactive: Boolean,
    connected: Boolean,
    discovering: Boolean,
    deviceName: String,
    mac: String,
    rssi: Int?,
    lastLinkText: String,
    copiedFlash: Boolean,
    onCopyMac: () -> Unit,
    @Suppress("UNUSED_PARAMETER") onDisconnect: () -> Unit,
    @Suppress("UNUSED_PARAMETER") onRefresh: () -> Unit,
) {
    val statusText = when {
        connected -> stringResource(Res.string.connection_status_connected)
        discovering -> stringResource(Res.string.connection_status_searching)
        else -> stringResource(Res.string.connection_status_disconnected)
    }
    val statusColor = when {
        connected -> ConnGreen
        discovering -> ConnCyan
        else -> ConnMuted
    }
    ConnSectionCard(passive = !interactive) {
        SectionTitleRow(
            icon = Icons.Filled.Bluetooth,
            title = stringResource(Res.string.connection_io_section_title),
            desc = stringResource(Res.string.connection_io_section_desc),
            statusText = statusText,
            statusColor = statusColor,
            accent = interactive,
        )
        Spacer(Modifier.height(14.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            MiniInfoCard(
                Modifier.weight(1f),
                stringResource(Res.string.connection_device_name_label),
                deviceName,
                trailing = {
                    Icon(Icons.Filled.Person, null, tint = ConnMuted, modifier = Modifier.size(16.dp))
                },
            )
            MiniInfoCard(
                Modifier.weight(1f),
                stringResource(Res.string.connection_mac_label),
                mac,
                trailing = {
                    Icon(
                        Icons.Filled.ContentCopy,
                        contentDescription = stringResource(Res.string.connection_copied),
                        tint = ConnMuted,
                        modifier = Modifier.size(15.dp).clickable(onClick = onCopyMac),
                    )
                },
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MiniSignalCard(Modifier.weight(1f), rssi, connected || discovering)
            MiniInfoCard(
                Modifier.weight(1f),
                stringResource(Res.string.connection_last_link_label),
                lastLinkText,
                trailing = {
                    Icon(Icons.Filled.AccessTime, null, tint = ConnMuted, modifier = Modifier.size(16.dp))
                },
            )
        }
        if (copiedFlash) {
            Text(
                stringResource(Res.string.connection_copied),
                color = ConnGreen,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun RemoteModuleCard(
    interactive: Boolean,
    active: Boolean,
    mqttConnected: Boolean,
    credentialsEditable: Boolean,
    deviceUid: String,
    error: String?,
    username: String,
    password: String,
    passwordVisible: Boolean,
    busy: Boolean,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onClearUsername: () -> Unit,
    onTogglePasswordVisible: () -> Unit,
    onCopyUid: () -> Unit,
    onTest: () -> Unit,
    onSave: () -> Unit,
) {
    val fieldsEnabled = credentialsEditable && !busy
    val statusText = if (active) {
        stringResource(Res.string.connection_remote_active)
    } else {
        stringResource(Res.string.connection_remote_inactive)
    }
    val linkText = if (mqttConnected) {
        stringResource(Res.string.connection_status_connected)
    } else {
        stringResource(Res.string.connection_status_disconnected)
    }
    ConnSectionCard(passive = !interactive) {
        SectionTitleRow(
            icon = Icons.Filled.Cloud,
            title = stringResource(Res.string.connection_remote_section_title),
            desc = stringResource(Res.string.connection_remote_section_desc),
            statusText = statusText,
            statusColor = if (active) ConnGreen else ConnMuted,
            accent = interactive,
        )
        if (!interactive) return@ConnSectionCard
        Spacer(Modifier.height(14.dp))
        ConnLabeledRow(
            label = stringResource(Res.string.connection_remote_state_label),
            leading = {
                Icon(
                    if (mqttConnected) Icons.Filled.Link else Icons.Filled.LinkOff,
                    null,
                    tint = if (mqttConnected) ConnGreen else ConnMuted,
                    modifier = Modifier.size(18.dp),
                )
            },
            value = linkText,
            valueColor = if (mqttConnected) ConnGreen else ConnMuted,
        )
        Spacer(Modifier.height(10.dp))
        ConnLabeledRow(
            label = stringResource(Res.string.connection_uid_label),
            value = deviceUid,
            trailing = {
                Icon(
                    Icons.Filled.ContentCopy,
                    contentDescription = stringResource(Res.string.connection_copied),
                    tint = ConnMuted,
                    modifier = Modifier.size(16.dp).clickable(onClick = onCopyUid),
                )
            },
        )
        Spacer(Modifier.height(10.dp))
        Text(stringResource(Res.string.remote_username_label), color = ConnMuted, fontSize = 12.sp)
        Spacer(Modifier.height(6.dp))
        ConnCredentialField(
            modifier = Modifier.fillMaxWidth(),
            value = username,
            onValueChange = onUsernameChange,
            placeholder = stringResource(Res.string.remote_username_label),
            leading = Icons.Filled.Person,
            enabled = fieldsEnabled,
            imeAction = ImeAction.Next,
            trailing = if (username.isNotEmpty() && fieldsEnabled) {
                {
                    IconButton(onClick = onClearUsername) {
                        Icon(Icons.Filled.Clear, null, tint = ConnMuted, modifier = Modifier.size(18.dp))
                    }
                }
            } else null,
        )
        Spacer(Modifier.height(10.dp))
        Text(stringResource(Res.string.remote_password_label), color = ConnMuted, fontSize = 12.sp)
        Spacer(Modifier.height(6.dp))
        ConnCredentialField(
            modifier = Modifier.fillMaxWidth(),
            value = password,
            onValueChange = onPasswordChange,
            placeholder = stringResource(Res.string.remote_password_label),
            leading = Icons.Filled.Lock,
            enabled = fieldsEnabled,
            isPassword = true,
            passwordVisible = passwordVisible,
            onTogglePasswordVisible = onTogglePasswordVisible,
            imeAction = ImeAction.Done,
            onDone = onSave,
        )
        if (!error.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            val okMsg = stringResource(Res.string.connection_remote_test_ok)
            Text(
                error,
                color = if (error == okMsg) ConnGreen else Color(0xFFF87171),
                fontSize = 12.sp,
            )
        }
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            GhostButton(
                modifier = Modifier.weight(1f),
                enabled = fieldsEnabled,
                filled = false,
                icon = Icons.Filled.Refresh,
                label = stringResource(Res.string.connection_remote_test),
                onClick = onTest,
            )
            GhostButton(
                modifier = Modifier.weight(1f),
                enabled = fieldsEnabled,
                filled = true,
                icon = Icons.Filled.Cloud,
                label = stringResource(Res.string.connection_remote_save),
                onClick = onSave,
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ConnCyan.copy(alpha = 0.10f), RoundedCornerShape(12.dp))
                .border(1.dp, ConnCyan.copy(alpha = 0.28f), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Info, null, tint = ConnCyan, modifier = Modifier.size(18.dp))
            Text(
                stringResource(Res.string.connection_remote_internet_hint),
                color = ConnMuted,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ConnLabeledRow(
    label: String,
    value: String,
    valueColor: Color = ConnText,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, color = ConnMuted, fontSize = 12.sp)
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .background(ConnTile, RoundedCornerShape(14.dp))
                .border(1.dp, ConnBorder, RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (leading != null) leading()
            Text(
                value,
                color = valueColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (trailing != null) trailing()
        }
    }
}

@Composable
private fun ConnCredentialField(
    modifier: Modifier,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leading: ImageVector,
    enabled: Boolean,
    imeAction: ImeAction,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onTogglePasswordVisible: (() -> Unit)? = null,
    onDone: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.height(58.dp),
        singleLine = true,
        enabled = enabled,
        placeholder = { Text(placeholder, color = ConnMuted, fontSize = 13.sp) },
        leadingIcon = { Icon(leading, null, tint = ConnMuted, modifier = Modifier.size(18.dp)) },
        trailingIcon = when {
            trailing != null -> trailing
            isPassword && onTogglePasswordVisible != null -> {
                {
                    IconButton(onClick = onTogglePasswordVisible, enabled = enabled) {
                        Icon(
                            if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = null,
                            tint = ConnMuted,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
            else -> null
        },
        visualTransformation = if (isPassword && !passwordVisible) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
        keyboardOptions = KeyboardOptions(imeAction = imeAction),
        keyboardActions = KeyboardActions(onDone = { onDone?.invoke() }),
        shape = RoundedCornerShape(14.dp),
        colors = TextFieldDefaults.outlinedTextFieldColors(
            textColor = ConnText,
            backgroundColor = ConnTile,
            cursorColor = ConnCyan,
            focusedBorderColor = ConnCyan.copy(alpha = 0.7f),
            unfocusedBorderColor = ConnBorder,
            disabledTextColor = ConnMuted,
            disabledBorderColor = ConnBorder.copy(alpha = 0.5f),
            placeholderColor = ConnMuted,
        ),
    )
}

@Composable
private fun ConnSectionCard(
    passive: Boolean = false,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (passive) 0.46f else 1f)
            .background(ConnCard, RoundedCornerShape(18.dp))
            .border(1.dp, ConnBorder, RoundedCornerShape(18.dp))
            .padding(18.dp),
    ) {
        content()
    }
}

@Composable
private fun SectionTitleRow(
    icon: ImageVector,
    title: String,
    desc: String,
    statusText: String,
    statusColor: Color,
    accent: Boolean = true,
) {
    val iconTint = if (accent) ConnCyan else ConnMuted
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(iconTint.copy(alpha = 0.12f), CircleShape)
                    .border(1.dp, iconTint.copy(alpha = 0.45f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = iconTint, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.padding(end = 8.dp)) {
                Text(
                    title.uppercase(),
                    color = ConnText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp,
                )
                Spacer(Modifier.height(4.dp))
                Text(desc, color = ConnMuted, fontSize = 12.sp, lineHeight = 16.sp)
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(top = 2.dp),
        ) {
            Text(stringResource(Res.string.connection_status_label), color = ConnMuted, fontSize = 12.sp)
            Box(Modifier.size(8.dp).background(statusColor, CircleShape))
            Text(statusText, color = statusColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun MiniInfoCard(
    modifier: Modifier,
    label: String,
    value: String,
    trailing: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .height(74.dp)
            .background(ConnTile, RoundedCornerShape(14.dp))
            .border(1.dp, ConnBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = ConnMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                value,
                color = ConnText,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            trailing?.invoke()
        }
    }
}

@Composable
private fun MiniSignalCard(modifier: Modifier, rssi: Int?, connected: Boolean) {
    val bars = rssiToBars(rssi, connected)
    val label = when {
        bars <= 0 -> "-"
        bars >= 4 -> stringResource(Res.string.connection_signal_strong_short)
        bars == 3 -> stringResource(Res.string.connection_signal_medium_short)
        else -> stringResource(Res.string.connection_signal_weak_short)
    }
    val color = when {
        bars >= 4 -> ConnCyan
        bars == 3 -> Color(0xFFFBBF24)
        bars > 0 -> Color(0xFFF87171)
        else -> ConnMuted
    }
    Column(
        modifier = modifier
            .height(74.dp)
            .background(ConnTile, RoundedCornerShape(14.dp))
            .border(1.dp, ConnBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(stringResource(Res.string.connection_signal_label), color = ConnMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SignalBars(filled = bars, tint = if (bars > 0) ConnCyan else ConnMuted)
            Text(label, color = color, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SignalBars(filled: Int, tint: Color) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.height(18.dp),
    ) {
        repeat(4) { i ->
            Box(
                Modifier
                    .width(5.dp)
                    .height((8 + i * 3f).dp)
                    .background(
                        if (i < filled.coerceAtMost(4)) tint else ConnMuted.copy(alpha = 0.28f),
                        RoundedCornerShape(1.dp),
                    ),
            )
        }
    }
}

@Composable
private fun GhostButton(
    modifier: Modifier,
    enabled: Boolean,
    filled: Boolean,
    accent: Boolean = false,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    val borderColor = when {
        filled -> Color.Transparent
        accent && enabled -> ConnCyan
        enabled -> ConnBorder
        else -> ConnBorder.copy(alpha = 0.5f)
    }
    val bg = when {
        filled && enabled -> ConnCyan
        filled -> ConnCyan.copy(alpha = 0.35f)
        else -> Color.Transparent
    }
    val fg = when {
        filled -> ConnInk
        accent && enabled -> ConnCyan
        enabled -> ConnText
        else -> ConnMuted
    }
    Row(
        modifier = modifier
            .height(46.dp)
            .background(bg, shape)
            .border(1.dp, borderColor, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, tint = fg, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, color = fg, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

private fun rssiToBars(rssi: Int?, connected: Boolean): Int {
    if (!connected) return 0
    val value = rssi ?: return 4
    return when {
        value >= -55 -> 4
        value >= -65 -> 4
        value >= -75 -> 3
        value >= -85 -> 2
        else -> 1
    }
}

private fun formatConnectionTimestamp(epochSec: Long): String {
    if (epochSec <= 0L) return "-"
    val dt = Instant.fromEpochSeconds(epochSec).toLocalDateTime(TimeZone.currentSystemDefault())
    val d = dt.dayOfMonth.toString().padStart(2, '0')
    val m = dt.monthNumber.toString().padStart(2, '0')
    val y = dt.year.toString().padStart(4, '0')
    val h = dt.hour.toString().padStart(2, '0')
    val min = dt.minute.toString().padStart(2, '0')
    return "$d.$m.$y $h:$min"
}

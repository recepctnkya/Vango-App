package com.hextech.vango

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hextech.vango.mqtt.MqttCredentialsProvider
import com.hextech.vango.profile.DeviceUidLookupApi
import com.hextech.vango.profile.LoginPreferences
import com.hextech.vango.profile.isValidMacFormat
import com.hextech.vango.profile.normalizeMacForDisplay
import com.hextech.vango.profile.persistDeviceLookupCache
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.compose.resources.stringResource
import testvango.composeapp.generated.resources.Res
import testvango.composeapp.generated.resources.connection_device_label
import testvango.composeapp.generated.resources.remote_auto_reconnect
import testvango.composeapp.generated.resources.remote_auto_reconnect_desc
import testvango.composeapp.generated.resources.remote_check_interval
import testvango.composeapp.generated.resources.remote_check_interval_desc
import testvango.composeapp.generated.resources.remote_connect_button
import testvango.composeapp.generated.resources.remote_connect_failed
import testvango.composeapp.generated.resources.remote_credentials_subtitle
import testvango.composeapp.generated.resources.remote_credentials_title
import testvango.composeapp.generated.resources.remote_err_credentials
import testvango.composeapp.generated.resources.remote_err_login
import testvango.composeapp.generated.resources.remote_extra_title
import testvango.composeapp.generated.resources.remote_internet_label
import testvango.composeapp.generated.resources.remote_interval_120
import testvango.composeapp.generated.resources.remote_interval_15
import testvango.composeapp.generated.resources.remote_interval_30
import testvango.composeapp.generated.resources.remote_interval_60
import testvango.composeapp.generated.resources.remote_login_button
import testvango.composeapp.generated.resources.remote_password_label
import testvango.composeapp.generated.resources.remote_phone_label
import testvango.composeapp.generated.resources.remote_save
import testvango.composeapp.generated.resources.remote_save_hint
import testvango.composeapp.generated.resources.remote_secure_link
import testvango.composeapp.generated.resources.remote_settings_subtitle
import testvango.composeapp.generated.resources.remote_settings_title
import testvango.composeapp.generated.resources.remote_status_active
import testvango.composeapp.generated.resources.remote_status_inactive
import testvango.composeapp.generated.resources.remote_status_title
import testvango.composeapp.generated.resources.remote_username_label
import testvango.composeapp.generated.resources.remote_device_query_button
import testvango.composeapp.generated.resources.remote_device_query_login
import testvango.composeapp.generated.resources.remote_device_query_mac_invalid
import testvango.composeapp.generated.resources.remote_device_query_not_found
import testvango.composeapp.generated.resources.remote_device_query_registered
import testvango.composeapp.generated.resources.remote_device_query_required
import testvango.composeapp.generated.resources.remote_device_query_uid_only
import testvango.composeapp.generated.resources.remote_device_querying
import testvango.composeapp.generated.resources.remote_device_uid_label
import testvango.composeapp.generated.resources.remote_wifi_mac_hint
import testvango.composeapp.generated.resources.remote_wifi_mac_label
import testvango.composeapp.generated.resources.remote_err_mac
import testvango.composeapp.generated.resources.remote_err_mac_required
import testvango.composeapp.generated.resources.remote_mqtt_connecting

private val RemoteGreen = Color(0xFF22C55E)
private val RemoteGreenSoft = Color(0xFFDCFCE7)
private val RemoteBlueSoft = Color(0xFFE8F4FD)
private val RemotePrimarySoft = Color(0x1A00A8F3)

private data class CheckIntervalOption(val seconds: Int, val labelRes: org.jetbrains.compose.resources.StringResource)

private enum class RemoteDeviceQueryState {
    Idle,
    Registered,
    UidOnly,
    NotFound,
}

@Composable
fun RemoteConnectionSetting() {
    val scope = rememberCoroutineScope()
    val appConn by appConnectionState.collectAsState()
    val remoteModeOn by remoteModeEnabled.collectAsState()
    val isRemoteActive = isAppRemoteMqttConnected() || isRemoteUserModeActive()
    val mqttConnected = isAppRemoteMqttConnected()
    val sessionErr by remoteSessionError.collectAsState()
    val panelDeviceId = LoginPreferences.getCachedMqttDeviceId() ?: 0

    var wifiMac by remember {
        mutableStateOf(
            currentPlatformDeviceMac()
                ?: LoginPreferences.getCachedPairedDeviceMac().orEmpty(),
        )
    }
    var queriedMac by remember { mutableStateOf<String?>(null) }
    var queriedUid by remember { mutableStateOf(LoginPreferences.getCachedDeviceUid().orEmpty()) }
    var deviceQueryState by remember { mutableStateOf(RemoteDeviceQueryState.Idle) }
    var deviceQueryMessage by remember { mutableStateOf<String?>(null) }
    var deviceQuerying by remember { mutableStateOf(false) }
    val deviceUid = queriedUid.takeIf { it.isNotBlank() }
        ?: LoginPreferences.getCachedDeviceUid()?.takeIf { it.isNotBlank() }
        ?: "—"
    var username by remember { mutableStateOf(LoginPreferences.getCachedMqttUsername().orEmpty()) }
    var password by remember { mutableStateOf(LoginPreferences.getCachedMqttPassword().orEmpty()) }
    var passwordVisible by remember { mutableStateOf(false) }
    var autoReconnect by remember { mutableStateOf(LoginPreferences.getRemoteAutoReconnect()) }
    var checkIntervalSec by remember { mutableStateOf(LoginPreferences.getRemoteCheckIntervalSec()) }
    var intervalMenuExpanded by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }

    val loggedIn = !LoginPreferences.getAuthToken().isNullOrBlank()
    val credentialsErrorText = stringResource(Res.string.remote_err_credentials)
    val connectFailedText = stringResource(Res.string.remote_connect_failed)

    val intervalOptions = listOf(
        CheckIntervalOption(15, Res.string.remote_interval_15),
        CheckIntervalOption(30, Res.string.remote_interval_30),
        CheckIntervalOption(60, Res.string.remote_interval_60),
        CheckIntervalOption(120, Res.string.remote_interval_120),
    )
    val selectedIntervalLabel = intervalOptions
        .firstOrNull { it.seconds == checkIntervalSec }
        ?.let { stringResource(it.labelRes) }
        ?: stringResource(Res.string.remote_interval_30)

    val macRequiredText = stringResource(Res.string.remote_err_mac_required)
    val macInvalidText = stringResource(Res.string.remote_err_mac)
    val queryLoginText = stringResource(Res.string.remote_device_query_login)
    val queryNotFoundText = stringResource(Res.string.remote_device_query_not_found)
    val queryMacInvalidText = stringResource(Res.string.remote_device_query_mac_invalid)
    val queryRegisteredText = stringResource(Res.string.remote_device_query_registered)
    val queryUidOnlyText = stringResource(Res.string.remote_device_query_uid_only)
    val queryRequiredText = stringResource(Res.string.remote_device_query_required)

    LaunchedEffect(loggedIn) {
        if (!loggedIn) return@LaunchedEffect
        username = LoginPreferences.getCachedMqttUsername().orEmpty()
        queriedUid = LoginPreferences.getCachedDeviceUid().orEmpty()
        password = LoginPreferences.getCachedMqttPassword().orEmpty()
        autoReconnect = LoginPreferences.getRemoteAutoReconnect()
        checkIntervalSec = LoginPreferences.getRemoteCheckIntervalSec()
        currentPlatformDeviceMac()?.let { wifiMac = it }
        val cachedMac = LoginPreferences.getCachedPairedDeviceMac()?.trim()?.let { normalizeMacForDisplay(it) }
        val currentMac = wifiMac.trim().let { if (it.isEmpty()) null else normalizeMacForDisplay(it) }
        if (cachedMac != null && currentMac == cachedMac && queriedUid.isNotBlank()) {
            queriedMac = cachedMac
            val hasRemoteIds = LoginPreferences.getCachedMqttCompanyId() != null &&
                LoginPreferences.getCachedMqttDeviceId() != null
            deviceQueryState = if (hasRemoteIds) RemoteDeviceQueryState.Registered else RemoteDeviceQueryState.UidOnly
            deviceQueryMessage = if (hasRemoteIds) queryRegisteredText else queryUidOnlyText
        }
    }

    fun resetDeviceQuery() {
        queriedMac = null
        queriedUid = ""
        deviceQueryState = RemoteDeviceQueryState.Idle
        deviceQueryMessage = null
    }

    fun queryDevice() {
        if (!loggedIn) {
            openRemoteConnectionPage()
            return
        }
        val macInput = wifiMac.trim()
        if (macInput.isEmpty()) {
            deviceQueryMessage = macRequiredText
            deviceQueryState = RemoteDeviceQueryState.NotFound
            return
        }
        val normalizedMac = normalizeMacForDisplay(macInput)
        if (normalizedMac.length != 17 || !isValidMacFormat(normalizedMac)) {
            deviceQueryMessage = queryMacInvalidText
            deviceQueryState = RemoteDeviceQueryState.NotFound
            return
        }
        if (LoginPreferences.getAuthToken().isNullOrBlank()) {
            deviceQueryMessage = queryLoginText
            deviceQueryState = RemoteDeviceQueryState.NotFound
            return
        }
        deviceQuerying = true
        deviceQueryMessage = null
        localError = null
        scope.launch {
            val panelInfo = DeviceUidLookupApi.resolvePanelDeviceForRemote(normalizedMac, null).getOrNull()
                ?: DeviceUidLookupApi.lookupDeviceServerInfoByMac(normalizedMac).getOrNull()
            deviceQuerying = false
            if (panelInfo?.deviceUid.isNullOrBlank()) {
                resetDeviceQuery()
                deviceQueryState = RemoteDeviceQueryState.NotFound
                deviceQueryMessage = queryNotFoundText
                return@launch
            }
            normalizeAndCachePlatformWifiMac(normalizedMac)
            persistDeviceLookupCache(normalizedMac, panelInfo)
            queriedMac = normalizedMac
            queriedUid = panelInfo.deviceUid.orEmpty()
            val remoteReady = panelInfo.mqttCompanyId != null && panelInfo.mqttDeviceId != null
            deviceQueryState = if (remoteReady) {
                RemoteDeviceQueryState.Registered
            } else {
                RemoteDeviceQueryState.UidOnly
            }
            deviceQueryMessage = if (remoteReady) queryRegisteredText else queryUidOnlyText
        }
    }

    fun persistExtras() {
        LoginPreferences.setRemoteAutoReconnect(autoReconnect)
        LoginPreferences.setRemoteCheckIntervalSec(checkIntervalSec)
        remoteMqttAutoReconnect.value = autoReconnect
        remoteMqttCheckIntervalSec.value = checkIntervalSec
    }

    fun saveExtrasOnly() {
        if (!loggedIn) return
        persistExtras()
    }

    fun connectRemote() {
        if (!loggedIn) {
            openRemoteConnectionPage()
            return
        }
        val user = username.trim()
        val pass = password
        if (user.isEmpty() || pass.isEmpty()) {
            localError = credentialsErrorText
            return
        }
        saving = true
        localError = null
        scope.launch {
            persistExtras()
            MqttCredentialsProvider.saveUserCredentials(user, pass, panelDeviceId)

            val macInput = wifiMac.trim()
            if (macInput.isEmpty()) {
                localError = macRequiredText
                saving = false
                return@launch
            }
            val normalizedMac = normalizeMacForDisplay(macInput)
            if (normalizedMac.length != 17 || !isValidMacFormat(normalizedMac)) {
                localError = macInvalidText
                saving = false
                return@launch
            }
            if (queriedMac != normalizedMac || deviceQueryState == RemoteDeviceQueryState.Idle) {
                localError = queryRequiredText
                saving = false
                return@launch
            }
            normalizeAndCachePlatformWifiMac(normalizedMac)

            val syncedInfo = DeviceUidLookupApi.resolvePanelDeviceForRemote(
                normalizedMac,
                queriedUid.takeIf { it.isNotBlank() },
            ).getOrNull()
            if (syncedInfo != null) {
                persistDeviceLookupCache(normalizedMac, syncedInfo)
                syncedInfo.deviceUid?.takeIf { it.isNotBlank() }?.let { queriedUid = it }
            }

            val session = RemoteMqttSession.prepareSession(normalizedMac, queriedUid.takeIf { it.isNotBlank() })
            if (session?.panelDevice == null) {
                saving = false
                return@launch
            }
            RemoteMqttSession.applyResolvedRoute(session.route)
            remoteSessionError.value = null
            activateRemoteModeExclusive()

            var connected = false
            withTimeoutOrNull(25_000) {
                while (true) {
                    when {
                        appConnectionState.value == AppConnectionState.REMOTE_MQTT_CONNECTED -> {
                            connected = true
                            return@withTimeoutOrNull
                        }
                        remoteSessionError.value != null -> return@withTimeoutOrNull
                        !remoteModeEnabled.value -> return@withTimeoutOrNull
                    }
                    delay(250)
                }
            }
            saving = false
            if (connected) {
                navigateToMenuPage(SettingsPage.Connection)
            } else if (remoteSessionError.value == null) {
                localError = connectFailedText
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        RemoteSettingsHeader(
            onBack = { navigateToMenuPage(SettingsPage.Connection) },
            onSave = { saveExtrasOnly() },
            saving = saving,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (!loggedIn) {
                RemoteLoginRequiredCard(onLogin = { openRemoteConnectionPage() })
            } else {
                RemoteConnectionStatusCard(
                    active = isRemoteActive,
                    mqttConnected = mqttConnected,
                    deviceUid = deviceUid,
                )
                RemoteDeviceMacCard(
                    wifiMac = wifiMac,
                    onWifiMacChange = { newMac ->
                        wifiMac = newMac
                        localError = null
                        val normalized = normalizeMacForDisplay(newMac.trim())
                        if (queriedMac != null && normalized != queriedMac) {
                            resetDeviceQuery()
                        }
                    },
                    queriedUid = queriedUid,
                    queryState = deviceQueryState,
                    queryMessage = deviceQueryMessage,
                    querying = deviceQuerying,
                    onQueryDevice = { queryDevice() },
                    enabled = !saving && !deviceQuerying,
                )
                RemoteCredentialsCard(
                    username = username,
                    onUsernameChange = { username = it; localError = null },
                    password = password,
                    onPasswordChange = { password = it; localError = null },
                    passwordVisible = passwordVisible,
                    onTogglePasswordVisible = { passwordVisible = !passwordVisible },
                    enabled = !saving,
                    localError = localError,
                    sessionError = sessionErr,
                    onConnect = { connectRemote() },
                    connecting = saving,
                )
                RemoteExtraSettingsCard(
                    autoReconnect = autoReconnect,
                    onAutoReconnectChange = { autoReconnect = it },
                    intervalLabel = selectedIntervalLabel,
                    intervalMenuExpanded = intervalMenuExpanded,
                    onIntervalMenuToggle = { intervalMenuExpanded = it },
                    intervalOptions = intervalOptions,
                    onIntervalSelected = { sec ->
                        checkIntervalSec = sec
                        intervalMenuExpanded = false
                    },
                )
                RemoteSaveHintBanner()
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun RemoteSettingsHeader(
    onBack: () -> Unit,
    onSave: () -> Unit,
    saving: Boolean,
) {
    SettingsBackHeader(
        backLabel = settingsBackLabelFor(SettingsPage.Remote),
        pageTitle = settingsHeaderTitleFor(SettingsPage.Remote),
        onBack = onBack,
        subtitle = stringResource(Res.string.remote_settings_subtitle),
        titleColor = MaterialTheme.colors.primary,
        trailing = {
            Button(
                onClick = onSave,
                enabled = !saving,
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary),
                contentPadding = ButtonDefaults.ContentPadding,
            ) {
                Icon(Icons.Filled.Check, null, tint = MaterialTheme.colors.onPrimary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(Res.string.remote_save), color = MaterialTheme.colors.onPrimary, fontSize = 13.sp)
            }
        },
    )
}

@Composable
private fun RemoteLoginRequiredCard(onLogin: () -> Unit) {
    Card(shape = RoundedCornerShape(16.dp), elevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(Res.string.remote_err_login), style = MaterialTheme.typography.body2)
            Button(onClick = onLogin, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.remote_login_button))
            }
        }
    }
}

@Composable
private fun RemoteConnectionStatusCard(active: Boolean, mqttConnected: Boolean, deviceUid: String) {
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = 4.dp,
        backgroundColor = if (active) RemoteGreen.copy(alpha = 0.06f) else MaterialTheme.colors.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (active) RemoteGreen else MaterialTheme.colors.onSurface.copy(0.35f)),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(Res.string.remote_status_title),
                    style = MaterialTheme.typography.subtitle1,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                text = when {
                    mqttConnected -> stringResource(Res.string.remote_status_active)
                    active -> stringResource(Res.string.remote_mqtt_connecting)
                    else -> stringResource(Res.string.remote_status_inactive)
                },
                style = MaterialTheme.typography.body2,
                color = if (active) RemoteGreen else MaterialTheme.colors.onSurface.copy(0.6f),
                modifier = Modifier.padding(start = 18.dp, top = 4.dp, bottom = 12.dp),
            )
            RemoteConnectionDiagram(active = active, deviceUid = deviceUid)
        }
    }
}

@Composable
private fun RemoteConnectionDiagram(active: Boolean, deviceUid: String) {
    val lineColor = if (active) RemoteGreen else MaterialTheme.colors.onSurface.copy(0.25f)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RemoteDiagramNode(
            icon = Icons.Filled.PhoneAndroid,
            label = stringResource(Res.string.remote_phone_label),
            highlighted = active,
        )
        RemoteDashedConnector(color = lineColor, modifier = Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(88.dp)) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(if (active) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface.copy(0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Public, null, tint = Color.White, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(Res.string.remote_internet_label),
                style = MaterialTheme.typography.caption,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                fontSize = 10.sp,
            )
            if (active) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp),
                ) {
                    Icon(Icons.Filled.Lock, null, tint = RemoteGreen, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(2.dp))
                    Text(
                        stringResource(Res.string.remote_secure_link),
                        style = MaterialTheme.typography.caption,
                        color = RemoteGreen,
                        fontSize = 9.sp,
                    )
                }
            }
        }
        RemoteDashedConnector(color = lineColor, modifier = Modifier.weight(1f))
        RemoteDiagramNode(
            icon = Icons.Filled.Devices,
            label = deviceUid,
            sublabel = stringResource(Res.string.connection_device_label),
            highlighted = active,
        )
    }
}

@Composable
private fun RemoteDashedConnector(color: Color, modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier
            .height(3.dp)
            .padding(horizontal = 4.dp),
    ) {
        drawLine(
            color = color,
            start = Offset(0f, size.height / 2f),
            end = Offset(size.width, size.height / 2f),
            strokeWidth = 3f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f),
        )
    }
}

@Composable
private fun RemoteDiagramNode(
    icon: ImageVector,
    label: String,
    sublabel: String? = null,
    highlighted: Boolean,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(72.dp)) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (highlighted) RemotePrimarySoft else MaterialTheme.colors.background)
                .border(
                    1.dp,
                    if (highlighted) MaterialTheme.colors.primary.copy(0.35f) else MaterialTheme.colors.onSurface.copy(0.12f),
                    RoundedCornerShape(12.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                null,
                tint = if (highlighted) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface.copy(0.45f),
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(Modifier.height(4.dp))
        sublabel?.let {
            Text(it, style = MaterialTheme.typography.caption, fontSize = 9.sp, textAlign = TextAlign.Center)
        }
        Text(
            label,
            style = MaterialTheme.typography.caption,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            fontSize = 10.sp,
            maxLines = 2,
        )
    }
}

@Composable
private fun RemoteDeviceMacCard(
    wifiMac: String,
    onWifiMacChange: (String) -> Unit,
    queriedUid: String,
    queryState: RemoteDeviceQueryState,
    queryMessage: String?,
    querying: Boolean,
    onQueryDevice: () -> Unit,
    enabled: Boolean,
) {
    val fieldColors = TextFieldDefaults.outlinedTextFieldColors(
        focusedBorderColor = MaterialTheme.colors.primary,
        unfocusedBorderColor = MaterialTheme.colors.onSurface.copy(0.35f),
    )
    Card(shape = RoundedCornerShape(16.dp), elevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            RemoteSectionTitle(Icons.Filled.Devices, stringResource(Res.string.remote_wifi_mac_label))
            Text(
                stringResource(Res.string.remote_wifi_mac_hint),
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(0.65f),
            )
            OutlinedTextField(
                value = wifiMac,
                onValueChange = onWifiMacChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(Res.string.remote_wifi_mac_label)) },
                singleLine = true,
                enabled = enabled,
                colors = fieldColors,
            )
            OutlinedButton(
                onClick = onQueryDevice,
                enabled = enabled && wifiMac.trim().isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (querying) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colors.primary,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(Res.string.remote_device_querying))
                } else {
                    Icon(Icons.Filled.Search, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(Res.string.remote_device_query_button))
                }
            }
            OutlinedTextField(
                value = queriedUid.ifBlank { "—" },
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(Res.string.remote_device_uid_label)) },
                singleLine = true,
                enabled = false,
                readOnly = true,
                colors = fieldColors,
            )
            queryMessage?.let { message ->
                val messageColor = when (queryState) {
                    RemoteDeviceQueryState.Registered -> RemoteGreen
                    RemoteDeviceQueryState.UidOnly -> MaterialTheme.colors.primary
                    RemoteDeviceQueryState.NotFound -> MaterialTheme.colors.error
                    RemoteDeviceQueryState.Idle -> MaterialTheme.colors.onSurface.copy(0.65f)
                }
                Text(
                    message,
                    style = MaterialTheme.typography.caption,
                    color = messageColor,
                )
            }
        }
    }
}

@Composable
private fun RemoteCredentialsCard(
    username: String,
    onUsernameChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    passwordVisible: Boolean,
    onTogglePasswordVisible: () -> Unit,
    enabled: Boolean,
    localError: String?,
    sessionError: String?,
    onConnect: () -> Unit,
    connecting: Boolean,
) {
    Card(shape = RoundedCornerShape(16.dp), elevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            RemoteSectionTitle(Icons.Filled.Person, stringResource(Res.string.remote_credentials_title))
            Text(
                stringResource(Res.string.remote_credentials_subtitle),
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(0.65f),
            )
            val fieldColors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = MaterialTheme.colors.primary,
                unfocusedBorderColor = MaterialTheme.colors.onSurface.copy(0.35f),
            )
            OutlinedTextField(
                value = username,
                onValueChange = onUsernameChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(Res.string.remote_username_label)) },
                singleLine = true,
                enabled = enabled,
                colors = fieldColors,
            )
            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(Res.string.remote_password_label)) },
                singleLine = true,
                enabled = enabled,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = onTogglePasswordVisible) {
                        Icon(
                            if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = null,
                        )
                    }
                },
                colors = fieldColors,
            )
            localError?.let {
                Text(it, style = MaterialTheme.typography.caption, color = MaterialTheme.colors.error)
            }
            sessionError?.let {
                Text(it, style = MaterialTheme.typography.caption, color = MaterialTheme.colors.error)
            }
            Button(
                onClick = onConnect,
                enabled = enabled && !connecting,
                modifier = Modifier
                    .vangoTestTag(VanGoTestTags.REMOTE_CONNECT)
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary),
            ) {
                if (connecting) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colors.onPrimary,
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(
                        stringResource(Res.string.remote_connect_button),
                        color = MaterialTheme.colors.onPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun RemoteExtraSettingsCard(
    autoReconnect: Boolean,
    onAutoReconnectChange: (Boolean) -> Unit,
    intervalLabel: String,
    intervalMenuExpanded: Boolean,
    onIntervalMenuToggle: (Boolean) -> Unit,
    intervalOptions: List<CheckIntervalOption>,
    onIntervalSelected: (Int) -> Unit,
) {
    Card(shape = RoundedCornerShape(16.dp), elevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            RemoteSectionTitle(Icons.Filled.Settings, stringResource(Res.string.remote_extra_title))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(Res.string.remote_auto_reconnect),
                        style = MaterialTheme.typography.body2,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        stringResource(Res.string.remote_auto_reconnect_desc),
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(0.65f),
                    )
                }
                Switch(
                    checked = autoReconnect,
                    onCheckedChange = onAutoReconnectChange,
                    colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colors.primary),
                )
            }
            Column {
                Text(
                    stringResource(Res.string.remote_check_interval),
                    style = MaterialTheme.typography.body2,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    stringResource(Res.string.remote_check_interval_desc),
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(0.65f),
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                Box {
                    OutlinedButton(
                        onClick = { onIntervalMenuToggle(true) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(intervalLabel)
                            Icon(Icons.Filled.ExpandMore, null)
                        }
                    }
                    DropdownMenu(
                        expanded = intervalMenuExpanded,
                        onDismissRequest = { onIntervalMenuToggle(false) },
                    ) {
                        intervalOptions.forEach { opt ->
                            DropdownMenuItem(onClick = { onIntervalSelected(opt.seconds) }) {
                                Text(stringResource(opt.labelRes))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RemoteSectionTitle(icon: ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colors.primary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun RemoteSaveHintBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(RemoteBlueSoft)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icons.Filled.Info, null, tint = MaterialTheme.colors.primary, modifier = Modifier.size(22.dp))
        Text(
            stringResource(Res.string.remote_save_hint),
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(0.85f),
        )
    }
}

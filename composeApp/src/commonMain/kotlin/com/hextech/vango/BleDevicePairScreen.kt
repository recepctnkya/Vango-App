package com.hextech.vango

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hextech.vango.profile.ManualPairedDeviceStore
import com.hextech.vango.profile.isValidMacFormat
import com.hextech.vango.profile.normalizeMacForDisplay
import com.hextech.vango.profile.toBleDevice
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import testvango.composeapp.generated.resources.Res
import testvango.composeapp.generated.resources.ble_pair_connect
import testvango.composeapp.generated.resources.ble_pair_connected_devices_title
import testvango.composeapp.generated.resources.ble_pair_connecting
import testvango.composeapp.generated.resources.ble_pair_status_connected
import testvango.composeapp.generated.resources.ble_pair_device_id
import testvango.composeapp.generated.resources.connection_device_not_found
import testvango.composeapp.generated.resources.ble_pair_help
import testvango.composeapp.generated.resources.ble_pair_help_body
import testvango.composeapp.generated.resources.ble_pair_manual_subtitle
import testvango.composeapp.generated.resources.ble_pair_manual_title
import testvango.composeapp.generated.resources.ble_pair_added_connect_hint
import testvango.composeapp.generated.resources.ble_pair_added_title
import testvango.composeapp.generated.resources.ble_pair_discovered_title
import testvango.composeapp.generated.resources.ble_pair_open_settings
import testvango.composeapp.generated.resources.ble_pair_location_warning
import testvango.composeapp.generated.resources.ble_pair_location_warning_body
import testvango.composeapp.generated.resources.ble_pair_not_found_subtitle
import testvango.composeapp.generated.resources.ble_pair_not_found_title
import testvango.composeapp.generated.resources.ble_pair_pairing_hint
import testvango.composeapp.generated.resources.ble_pair_no_devices_after_scan
import testvango.composeapp.generated.resources.ble_pair_remove_confirm
import testvango.composeapp.generated.resources.ble_pair_remove_device
import testvango.composeapp.generated.resources.ble_pair_scan_done_title
import testvango.composeapp.generated.resources.ble_pair_search_devices
import testvango.composeapp.generated.resources.ble_pair_tap_search
import testvango.composeapp.generated.resources.ble_pair_scan_active
import testvango.composeapp.generated.resources.ble_pair_scanning_subtitle
import testvango.composeapp.generated.resources.ble_pair_scanning_title
import testvango.composeapp.generated.resources.ble_pair_subtitle
import testvango.composeapp.generated.resources.ble_pair_title
import testvango.composeapp.generated.resources.color_picker_close
import testvango.composeapp.generated.resources.connection_disconnect

private val PairGreen = Color(0xFF22C55E)
private val PairGreenSoft = Color(0xFFDCFCE7)
private val PairYellow = Color(0xFFEAB308)

private data class PairUiColors(
    val accent: Color,
    val pageBg: Color,
    val cardBg: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val border: Color,
    val scanBg: Color,
    val green: Color,
    val greenSoft: Color,
    val yellow: Color,
)

@Composable
private fun pairUiColors(): PairUiColors {
    val theme = MaterialTheme.colors
    val light = theme.isLight
    return PairUiColors(
        accent = theme.primary,
        pageBg = theme.background,
        cardBg = theme.surface,
        textPrimary = theme.onBackground,
        textSecondary = theme.onBackground.copy(alpha = 0.62f),
        border = theme.onBackground.copy(alpha = if (light) 0.1f else 0.2f),
        scanBg = theme.primary.copy(alpha = if (light) 0.09f else 0.18f),
        green = PairGreen,
        greenSoft = if (light) PairGreenSoft else Color(0xFF1B3D2A),
        yellow = PairYellow,
    )
}

@Composable
fun BleDevicePairSetting() {
    BleDevicePairScreen(onBack = { navigateToMenuPage(SettingsPage.Connection) })
}

private fun mergeBleDevicesByMac(
    existing: List<BleDevice>,
    found: List<BleDevice>,
): List<BleDevice> {
    if (found.isEmpty()) return existing
    val byMac = existing.associateBy { it.mac.uppercase() }.toMutableMap()
    for (device in found) {
        byMac[device.mac.uppercase()] = device
    }
    return byMac.values.sortedByDescending { it.rssi ?: Int.MIN_VALUE }
}

private fun connectedBleDeviceDisplayName(mac: String?): String {
    if (mac.isNullOrBlank()) return bleIoModuleName
    return vangoDeviceList.firstOrNull { it.mac.equals(mac, ignoreCase = true) }
        ?.name
        ?.takeIf { it.isNotBlank() }
        ?: bleIoModuleName
}

private fun resolveBleDeviceForMac(
    mac: String,
    nearby: List<BleDevice>,
    added: List<BleDevice>,
): BleDevice {
    return nearby.firstOrNull { it.mac.equals(mac, ignoreCase = true) }
        ?: added.firstOrNull { it.mac.equals(mac, ignoreCase = true) }
        ?: BleDevice(name = connectedBleDeviceDisplayName(mac), mac = mac, rssi = null)
}

private fun filterOutConnectedMac(devices: List<BleDevice>, connectedMac: String?): List<BleDevice> {
    val mac = connectedMac?.trim()?.takeIf { it.isNotEmpty() } ?: return devices
    return devices.filterNot { it.mac.equals(mac, ignoreCase = true) }
}

/** Eşleşen listede desteklenen VanGO adları veya manuel eklenen IO modül kayıtları. */
private fun isScannedVanGoBleDevice(device: BleDevice): Boolean {
    val mac = normalizeMacForDisplay(device.mac)
    if (!isValidMacFormat(mac)) return false
    if (isSupportedBleDeviceName(device.name)) return true
    return ManualPairedDeviceStore.getAll().any { it.mac.equals(mac, ignoreCase = true) }
}

private fun filterScannedVanGoDevices(devices: List<BleDevice>): List<BleDevice> =
    preferBleAdapterDevices(devices.filter { isScannedVanGoBleDevice(it) })

@Composable
fun BleDevicePairContent(
    startScanOnEnter: Boolean = false,
    showManualActions: Boolean = true,
    showHelpDialog: Boolean = false,
    onDismissHelp: () -> Unit = {},
) {
    val global by vanGoGlobal.collectAsState()
    val deviceCount by vangoDeviceListSize.collectAsState()
    val connectionState = global.connectionState
    val isConnecting = connectionState == BleGattConnectionChain.ConnectingGattServer ||
        connectionState == BleGattConnectionChain.DiscoveringServices
    val isConnected = connectionState == BleGattConnectionChain.ChainEnded
    val bleLinked = getAppContext().vanGO.isConnected() && isConnected
    val connectedMac = currentBleAdapterMac()
    var nearbyDevices by remember { mutableStateOf<List<BleDevice>>(emptyList()) }
    var addedDevices by remember { mutableStateOf<List<BleDevice>>(emptyList()) }
    var scanComplete by remember { mutableStateOf(false) }
    var pairScanActive by remember { mutableStateOf(false) }
    var scanEverStarted by remember { mutableStateOf(false) }
    var scanGeneration by remember { mutableIntStateOf(0) }
    var showHelp by remember { mutableStateOf(false) }
    var connectingMac by remember { mutableStateOf<String?>(null) }
    var pendingConnectMac by remember { mutableStateOf<String?>(null) }
    var connectError by remember { mutableStateOf<String?>(null) }
    var macPendingRemove by remember { mutableStateOf<String?>(null) }
    var locationBlocked by remember {
        mutableStateOf(bleScanRequiresLocationEnabled() && (!hasLocationPermissions() || !isLocationServicesEnabled()))
    }
    val deviceNotFoundMsg = stringResource(Res.string.connection_device_not_found)
    val tapSearchMsg = stringResource(Res.string.ble_pair_tap_search)
    val noDevicesAfterScanMsg = stringResource(Res.string.ble_pair_no_devices_after_scan)
    val isScanning = pairScanActive && !scanComplete
    val discoveredEmptyMessage = when {
        !scanEverStarted -> tapSearchMsg
        scanComplete -> noDevicesAfterScanMsg
        else -> ""
    }

    fun refreshAddedDevices() {
        addedDevices = ManualPairedDeviceStore.getAll()
            .map { it.toBleDevice() }
            .filter { isValidMacFormat(normalizeMacForDisplay(it.mac)) }
    }

    fun syncNearbyFromGlobal() {
        val scannedOnly = filterScannedVanGoDevices(vangoDeviceList)
        if (scannedOnly.isEmpty()) return
        nearbyDevices = mergeBleDevicesByMac(nearbyDevices, scannedOnly)
    }

    fun stopPairScan(stopBleDiscovery: Boolean = true) {
        pairScanActive = false
        scanComplete = true
        if (stopBleDiscovery) {
            getAppContext().ble().stopDiscovering()
        }
    }

    LaunchedEffect(Unit) {
        refreshAddedDevices()
        seedIoModuleFromPlatformRegistration()
        refreshAddedDevices()
        locationBlocked = bleScanRequiresLocationEnabled() && (!hasLocationPermissions() || !isLocationServicesEnabled())
    }

    fun startPairScan() {
        if (connectingMac != null || isConnecting) return
        locationBlocked = bleScanRequiresLocationEnabled() && (!hasLocationPermissions() || !isLocationServicesEnabled())
        if (locationBlocked) {
            connectError = null
            return
        }
        scanGeneration++
        scanEverStarted = true
        pairScanActive = true
        scanComplete = false
        connectError = null
        syncNearbyFromGlobal()
        seedIoModuleFromPlatformRegistration()
        ManualPairedDeviceStore.syncAllToVangoList()
        getVanGO().discoverVangos()
    }

    LaunchedEffect(bleLinked) {
        if (bleLinked) {
            syncBleAppConnectionStateIfLinked()
        }
    }

    LaunchedEffect(uiState.value.settingsPage) {
        if (uiState.value.settingsPage != SettingsPage.BlePair) return@LaunchedEffect
        refreshAddedDevices()
        syncNearbyFromGlobal()
        val globalScanActive = connectionState == BleGattConnectionChain.DiscoveringDevice
        pairScanActive = globalScanActive
        scanComplete = !globalScanActive && nearbyDevices.isNotEmpty()
        if (globalScanActive || nearbyDevices.isNotEmpty()) {
            scanEverStarted = true
        }
        if (startScanOnEnter && !globalScanActive) {
            startPairScan()
        }
    }

    LaunchedEffect(scanGeneration) {
        val generation = scanGeneration
        delay(60_000)
        if (generation == scanGeneration && pairScanActive) {
            syncNearbyFromGlobal()
            stopPairScan()
        }
    }

    LaunchedEffect(connectionState, deviceCount, pairScanActive) {
        if (!pairScanActive) return@LaunchedEffect
        when (connectionState) {
            BleGattConnectionChain.DiscoveringDevice -> syncNearbyFromGlobal()
            BleGattConnectionChain.DiscoverFinished -> {
                syncNearbyFromGlobal()
            }
            else -> Unit
        }
    }

    LaunchedEffect(deviceCount, pairScanActive) {
        if (pairScanActive && deviceCount > 0) {
            syncNearbyFromGlobal()
        }
    }

    LaunchedEffect(isConnected) {
        if (isConnected) {
            connectingMac = null
            pendingConnectMac = null
            connectError = null
            syncBleAppConnectionStateIfLinked()
        }
    }

    fun removeAddedDevice(mac: String) {
        val normalized = normalizeMacForDisplay(mac)
        if (bleLinked && connectedMac.equals(normalized, ignoreCase = true)) {
            bleAutoReconnectEnabled.value = false
            getAppContext().ble().disconnect()
            appConnectionState.value = AppConnectionState.OFFLINE
        }
        if (ManualPairedDeviceStore.remove(normalized)) {
            refreshAddedDevices()
            nearbyDevices = nearbyDevices.filterNot { it.mac.equals(normalized, ignoreCase = true) }
            if (connectingMac.equals(normalized, ignoreCase = true)) {
                connectingMac = null
                pendingConnectMac = null
            }
        }
        macPendingRemove = null
    }

    fun connectToMac(mac: String) {
        connectError = null
        activateBleModeExclusive()
        val normalized = normalizeMacForDisplay(mac)
        if (!isValidMacFormat(normalized)) {
            connectError = deviceNotFoundMsg
            return
        }
        stopPairScan()
        val switchingDevice = bleLinked &&
            !connectedMac.isNullOrBlank() &&
            !connectedMac.equals(normalized, ignoreCase = true)
        connectingMac = normalized
        if (switchingDevice) {
            pendingConnectMac = normalized
            // Geçici kopma — aynı oturumda yeni MAC'e bağlanacağız
            bleAutoReconnectEnabled.value = true
            getAppContext().ble().disconnect()
            appConnectionState.value = AppConnectionState.OFFLINE
            return
        }
        pendingConnectMac = null
        if (!getVanGO().connectByMac(normalized)) {
            connectError = deviceNotFoundMsg
            connectingMac = null
        }
    }

    fun disconnectConnectedDevice() {
        stopPairScan()
        connectingMac = null
        pendingConnectMac = null
        bleAutoReconnectEnabled.value = false
        getAppContext().ble().disconnect()
        appConnectionState.value = AppConnectionState.OFFLINE
    }

    LaunchedEffect(bleLinked, pendingConnectMac) {
        val target = pendingConnectMac ?: return@LaunchedEffect
        if (bleLinked) return@LaunchedEffect
        if (connectingMac?.equals(target, ignoreCase = true) != true) return@LaunchedEffect
        delay(450)
        if (getAppContext().vanGO.isConnected()) return@LaunchedEffect
        if (!getVanGO().connectByMac(target)) {
            connectError = deviceNotFoundMsg
            connectingMac = null
            pendingConnectMac = null
        }
    }

    val colors = pairUiColors()
    val connectedDevice = if (bleLinked && !connectedMac.isNullOrBlank()) {
        val mac = connectedMac
        vangoDeviceList.firstOrNull { it.mac.equals(mac, ignoreCase = true) && isScannedVanGoBleDevice(it) }
            ?: resolveBleDeviceForMac(mac, nearbyDevices, addedDevices).let { dev ->
                if (isSupportedBleDeviceName(dev.name)) dev
                else dev.copy(name = connectedBleDeviceDisplayName(mac))
            }
    } else {
        null
    }
    val discoveredDevices = filterOutConnectedMac(
        filterScannedVanGoDevices(nearbyDevices),
        connectedMac,
    )
    val otherAddedDevices = filterOutConnectedMac(addedDevices, connectedMac)
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (locationBlocked) {
            PairLocationWarningCard(
                colors = colors,
                onOpenSettings = { openAppSettings() },
            )
        }
        PairScanStatusCard(
            colors = colors,
            isScanning = isScanning,
            radarActive = isScanning || pairScanActive,
            scanComplete = scanComplete && scanEverStarted,
        )
        if (connectedDevice != null) {
            PairDevicesListSection(
                colors = colors,
                title = stringResource(Res.string.ble_pair_connected_devices_title),
                devices = listOf(connectedDevice),
                isScanning = false,
                scanComplete = true,
                connectingMac = null,
                isConnecting = false,
                emptyMessage = "",
                showSearchAction = false,
                showRemove = false,
                showSignal = false,
                connectedMac = connectedMac,
                onSearch = {},
                onConnect = {},
                onDisconnect = { disconnectConnectedDevice() },
                onRemove = { macPendingRemove = it },
            )
        }
        PairDevicesListSection(
            colors = colors,
            title = stringResource(Res.string.ble_pair_discovered_title),
            devices = discoveredDevices,
            isScanning = isScanning,
            scanComplete = scanComplete || !scanEverStarted,
            connectingMac = connectingMac,
            isConnecting = isConnecting,
            emptyMessage = discoveredEmptyMessage,
            showSearchAction = true,
            searchEnabled = !isScanning,
            showSignal = true,
            connectedMac = connectedMac,
            onSearch = { startPairScan() },
            onConnect = { connectToMac(it) },
            onDisconnect = { disconnectConnectedDevice() },
        )
        if (addedDevices.isNotEmpty()) {
            PairDevicesListSection(
                colors = colors,
                title = stringResource(Res.string.ble_pair_added_title),
                devices = otherAddedDevices,
                isScanning = false,
                scanComplete = true,
                connectingMac = connectingMac,
                isConnecting = isConnecting,
                emptyMessage = "",
                showSearchAction = false,
                showRemove = true,
                showSignal = false,
                sectionHint = stringResource(Res.string.ble_pair_added_connect_hint),
                connectedMac = connectedMac,
                onSearch = {},
                onConnect = { connectToMac(it) },
                onDisconnect = { disconnectConnectedDevice() },
                onRemove = { macPendingRemove = it },
            )
        }
        connectError?.let {
            Text(it, color = MaterialTheme.colors.error, fontSize = 13.sp)
        }
        if (showManualActions) {
            PairActionRow(
                colors = colors,
                title = stringResource(Res.string.ble_pair_manual_title),
                subtitle = stringResource(Res.string.ble_pair_manual_subtitle),
                icon = Icons.Filled.Add,
                onClick = { navigateToMenuPage(SettingsPage.ManualDeviceAdd) },
            )
            PairActionRow(
                colors = colors,
                title = stringResource(Res.string.ble_pair_not_found_title),
                subtitle = stringResource(Res.string.ble_pair_not_found_subtitle),
                icon = Icons.Filled.Lightbulb,
                onClick = { showHelp = true },
            )
        }
    }

    macPendingRemove?.let { macToRemove ->
        val label = addedDevices.firstOrNull { it.mac.equals(macToRemove, ignoreCase = true) }
            ?.let { if (it.name.isNotBlank()) "${it.name} (${it.mac})" else it.mac }
            ?: macToRemove
        AlertDialog(
            onDismissRequest = { macPendingRemove = null },
            title = { Text(stringResource(Res.string.ble_pair_remove_device)) },
            text = { Text(stringResource(Res.string.ble_pair_remove_confirm, label)) },
            confirmButton = {
                TextButton(
                    onClick = { removeAddedDevice(macToRemove) },
                    modifier = Modifier.vangoTestTag(VanGoTestTags.BLE_PAIR_REMOVE_CONFIRM),
                ) {
                    Text(stringResource(Res.string.ble_pair_remove_device), color = MaterialTheme.colors.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { macPendingRemove = null }) {
                    Text(stringResource(Res.string.color_picker_close))
                }
            },
        )
    }

    val helpVisible = showHelp || showHelpDialog
    if (helpVisible) {
        AlertDialog(
            onDismissRequest = {
                showHelp = false
                onDismissHelp()
            },
            title = { Text(stringResource(Res.string.ble_pair_help)) },
            text = { Text(stringResource(Res.string.ble_pair_help_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showHelp = false
                    onDismissHelp()
                }) {
                    Text(stringResource(Res.string.color_picker_close))
                }
            },
        )
    }

}

@Composable
fun BleDevicePairScreen(onBack: () -> Unit) {
    var headerHelp by remember { mutableStateOf(false) }
    val colors = pairUiColors()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.pageBg),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            PairHeader(colors = colors, onBack = onBack, onHelp = { headerHelp = true })
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                BleDevicePairContent(
                    startScanOnEnter = true,
                    showHelpDialog = headerHelp,
                    onDismissHelp = { headerHelp = false },
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun PairHeader(colors: PairUiColors, onBack: () -> Unit, onHelp: () -> Unit) {
    SettingsBackHeader(
        backLabel = settingsBackLabelFor(SettingsPage.BlePair),
        pageTitle = settingsHeaderTitleFor(SettingsPage.BlePair),
        onBack = onBack,
        subtitle = stringResource(Res.string.ble_pair_subtitle),
        titleColor = colors.textPrimary,
        subtitleColor = colors.textSecondary,
        trailing = {
            OutlinedButton(
                onClick = onHelp,
                contentPadding = ButtonDefaults.ContentPadding,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textPrimary),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
            ) {
                Icon(Icons.Filled.HelpOutline, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(Res.string.ble_pair_help), fontSize = 12.sp)
            }
        },
    )
}

@Composable
private fun PairLocationWarningCard(
    colors: PairUiColors,
    onOpenSettings: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = 2.dp,
        backgroundColor = PairYellow.copy(alpha = 0.12f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                stringResource(Res.string.ble_pair_location_warning),
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary,
                style = MaterialTheme.typography.subtitle2,
            )
            Text(
                stringResource(Res.string.ble_pair_location_warning_body),
                style = MaterialTheme.typography.caption,
                color = colors.textSecondary,
            )
            OutlinedButton(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.ble_pair_open_settings))
            }
        }
    }
}

@Composable
private fun PairScanStatusCard(
    colors: PairUiColors,
    isScanning: Boolean,
    radarActive: Boolean,
    scanComplete: Boolean,
) {
    val scanningTitle = stringResource(Res.string.ble_pair_scanning_title)
    val scanningSubtitle = stringResource(Res.string.ble_pair_scanning_subtitle)
    val doneTitle = stringResource(Res.string.ble_pair_scan_done_title)

    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = 2.dp,
        backgroundColor = colors.scanBg,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, colors.accent.copy(alpha = 0.12f), RoundedCornerShape(16.dp)),
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            BleRadarIndicator(
                modifier = Modifier.size(88.dp),
                accent = colors.accent,
                active = radarActive,
                centerIcon = {
                    Icon(Icons.Filled.Memory, null, tint = colors.accent, modifier = Modifier.size(26.dp))
                },
            )
            Column(Modifier.weight(1f)) {
                Text(
                    when {
                        isScanning -> scanningTitle
                        scanComplete -> doneTitle
                        else -> stringResource(Res.string.ble_pair_tap_search)
                    },
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    style = MaterialTheme.typography.subtitle1,
                )
                if (isScanning) {
                    Text(
                        scanningSubtitle,
                        style = MaterialTheme.typography.caption,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    Row(
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .background(colors.greenSoft, RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(
                            Modifier
                                .size(8.dp)
                                .background(colors.green, CircleShape),
                        )
                        Text(
                            stringResource(Res.string.ble_pair_scan_active),
                            color = colors.green,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                Text(
                    stringResource(Res.string.ble_pair_pairing_hint),
                    style = MaterialTheme.typography.caption,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(top = 8.dp),
                    lineHeight = 16.sp,
                )
            }
        }
    }
}

@Composable
private fun PairDevicesListSection(
    colors: PairUiColors,
    title: String,
    devices: List<BleDevice>,
    isScanning: Boolean,
    scanComplete: Boolean,
    connectingMac: String?,
    isConnecting: Boolean,
    emptyMessage: String,
    showSearchAction: Boolean = false,
    searchEnabled: Boolean = true,
    showSignal: Boolean,
    sectionHint: String? = null,
    connectedMac: String? = null,
    showRemove: Boolean = false,
    onSearch: () -> Unit = {},
    onConnect: (String) -> Unit,
    onDisconnect: (String) -> Unit = {},
    onRemove: (String) -> Unit = {},
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                title,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.subtitle1,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
            if (showSearchAction) {
                TextButton(
                    onClick = onSearch,
                    enabled = searchEnabled,
                    modifier = Modifier.vangoTestTag(VanGoTestTags.BLE_PAIR_SEARCH),
                ) {
                    if (!searchEnabled) {
                        CircularProgressIndicator(
                            color = colors.accent,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(Icons.Filled.Search, null, tint = colors.accent, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(
                        stringResource(Res.string.ble_pair_search_devices),
                        color = colors.accent,
                        fontSize = 13.sp,
                    )
                }
            }
        }
        sectionHint?.let {
            Text(
                it,
                fontSize = 12.sp,
                color = colors.textSecondary,
                lineHeight = 16.sp,
            )
        }
        if (devices.isEmpty() && isScanning) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = colors.accent, modifier = Modifier.size(36.dp))
            }
        } else if (devices.isEmpty() && scanComplete && emptyMessage.isNotEmpty()) {
            Text(
                emptyMessage,
                color = colors.textSecondary,
                modifier = Modifier.padding(vertical = 12.dp),
            )
        } else {
            devices.forEach { device ->
                val isDeviceConnected = connectedMac?.equals(device.mac, ignoreCase = true) == true
                val isConnectingThis = !isDeviceConnected &&
                    connectingMac.equals(device.mac, ignoreCase = true)
                PairDeviceRow(
                    colors = colors,
                    device = device,
                    showSignal = showSignal,
                    showRemove = showRemove,
                    connecting = isConnectingThis,
                    connected = isDeviceConnected,
                    onConnect = { onConnect(device.mac) },
                    onDisconnect = { onDisconnect(device.mac) },
                    onRemove = { onRemove(device.mac) },
                )
            }
        }
    }
}

@Composable
private fun PairDeviceRow(
    colors: PairUiColors,
    device: BleDevice,
    showSignal: Boolean,
    showRemove: Boolean = false,
    connecting: Boolean,
    connected: Boolean = false,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit = {},
    onRemove: () -> Unit = {},
) {
    val (bars, barColor) = rssiToSignal(device.rssi, colors)
    Card(
        shape = RoundedCornerShape(14.dp),
        elevation = 1.dp,
        backgroundColor = if (connected) colors.greenSoft else colors.cardBg,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (connected) colors.green.copy(alpha = 0.4f) else colors.border,
                RoundedCornerShape(14.dp),
            ),
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        if (connected) colors.green.copy(alpha = 0.15f) else colors.pageBg,
                        RoundedCornerShape(10.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (connected) Icons.Filled.Check else deviceIconFor(device.name),
                    null,
                    tint = if (connected) colors.green else colors.accent,
                    modifier = Modifier.size(24.dp),
                )
            }
            Column(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp),
            ) {
                Text(
                    device.name.ifBlank { "VanGO" },
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    stringResource(Res.string.ble_pair_device_id, device.mac),
                    fontSize = 11.sp,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (showSignal) {
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(end = 8.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        repeat(3) { i ->
                            Box(
                                Modifier
                                    .width(4.dp)
                                    .height((8 + i * 4).dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(
                                        if (i < bars) barColor else colors.border,
                                    ),
                            )
                        }
                    }
                    device.rssi?.let {
                        Text(
                            "$it dBm",
                            fontSize = 10.sp,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
            }
            if (showRemove) {
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.vangoTestTag(VanGoTestTags.blePairRemove(device.mac)),
                ) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = stringResource(Res.string.ble_pair_remove_device),
                        tint = MaterialTheme.colors.error.copy(alpha = 0.85f),
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            when {
                connected -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(
                            modifier = Modifier
                                .background(colors.green.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(Icons.Filled.Check, null, tint = colors.green, modifier = Modifier.size(16.dp))
                            Text(
                                stringResource(Res.string.ble_pair_status_connected),
                                color = colors.green,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        TextButton(
                            onClick = onDisconnect,
                            shape = RoundedCornerShape(20.dp),
                        ) {
                            Text(
                                stringResource(Res.string.connection_disconnect),
                                color = colors.accent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
                connecting -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        strokeWidth = 2.dp,
                        color = colors.accent,
                    )
                }
                else -> {
                    Button(
                        onClick = onConnect,
                        modifier = Modifier.vangoTestTag(VanGoTestTags.blePairConnect(device.mac)),
                        colors = ButtonDefaults.buttonColors(backgroundColor = colors.accent),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = ButtonDefaults.ContentPadding,
                    ) {
                        Text(stringResource(Res.string.ble_pair_connect), color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun PairActionRow(
    colors: PairUiColors,
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        elevation = 1.dp,
        backgroundColor = colors.cardBg,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, colors.border, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .border(1.dp, colors.border, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = colors.accent, modifier = Modifier.size(20.dp))
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(title, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.caption,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(top = 2.dp),
                    lineHeight = 16.sp,
                )
            }
            Icon(Icons.Filled.ChevronRight, null, tint = colors.textSecondary)
        }
    }
}

private fun deviceIconFor(name: String): ImageVector = when {
    name.contains("IO", ignoreCase = true) -> Icons.Filled.Memory
    name.contains("DISPLAY", ignoreCase = true) -> Icons.Filled.Devices
    else -> Icons.Filled.Bluetooth
}

private fun rssiToSignal(rssi: Int?, colors: PairUiColors): Pair<Int, Color> {
    if (rssi == null) return 2 to colors.yellow
    return when {
        rssi >= -55 -> 3 to colors.green
        rssi >= -70 -> 3 to colors.green
        rssi >= -78 -> 2 to colors.yellow
        else -> 1 to Color(0xFFEF4444)
    }
}

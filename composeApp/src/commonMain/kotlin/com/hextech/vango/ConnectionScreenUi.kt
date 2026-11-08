package com.hextech.vango

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
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import testvango.composeapp.generated.resources.Res
import testvango.composeapp.generated.resources.connection_ble_mac_label
import testvango.composeapp.generated.resources.connection_device_info_title
import testvango.composeapp.generated.resources.connection_device_label
import testvango.composeapp.generated.resources.connection_device_not_found
import testvango.composeapp.generated.resources.connection_disconnect
import testvango.composeapp.generated.resources.connection_hw_label
import testvango.composeapp.generated.resources.connection_last_data
import testvango.composeapp.generated.resources.connection_last_status_title
import testvango.composeapp.generated.resources.connection_latency_unknown
import testvango.composeapp.generated.resources.connection_local_link
import testvango.composeapp.generated.resources.connection_mac_label
import testvango.composeapp.generated.resources.connection_mech_label
import testvango.composeapp.generated.resources.connection_mode_bluetooth
import testvango.composeapp.generated.resources.connection_mode_remote
import testvango.composeapp.generated.resources.connection_not_connected_hint
import testvango.composeapp.generated.resources.connection_phone_label
import testvango.composeapp.generated.resources.connection_remote_active
import testvango.composeapp.generated.resources.connection_remote_disconnect
import testvango.composeapp.generated.resources.connection_remote_inactive
import testvango.composeapp.generated.resources.connection_remote_manage
import testvango.composeapp.generated.resources.connection_remote_mqtt_active
import testvango.composeapp.generated.resources.connection_remote_mqtt_idle
import testvango.composeapp.generated.resources.connection_remote_subtitle
import testvango.composeapp.generated.resources.connection_remote_title
import testvango.composeapp.generated.resources.connection_bluetooth_search_hint
import testvango.composeapp.generated.resources.connection_search_button
import testvango.composeapp.generated.resources.connection_search_hint
import testvango.composeapp.generated.resources.connection_searching
import testvango.composeapp.generated.resources.connection_signal_strong
import testvango.composeapp.generated.resources.connection_signal_weak
import testvango.composeapp.generated.resources.connection_status_connected
import testvango.composeapp.generated.resources.connection_status_disconnected
import testvango.composeapp.generated.resources.connection_status_subtitle
import testvango.composeapp.generated.resources.connection_status_title
import testvango.composeapp.generated.resources.connection_sw_label
import testvango.composeapp.generated.resources.connection_uid_label
import testvango.composeapp.generated.resources.ble_pair_subtitle
import testvango.composeapp.generated.resources.ble_pair_title
import testvango.composeapp.generated.resources.connection_internet_link
import testvango.composeapp.generated.resources.connection_open_ble_pair
import testvango.composeapp.generated.resources.connection_via_bluetooth
import testvango.composeapp.generated.resources.connection_via_remote
import testvango.composeapp.generated.resources.connection_view_history
import testvango.composeapp.generated.resources.dialog_discovering
import testvango.composeapp.generated.resources.settings_connection_title

enum class ConnectionUiMode {
    Bluetooth,
    Remote,
}

private val ConnGreen = Color(0xFF22C55E)
private val ConnGreenSoft = Color(0xFFDCFCE7)
private val ConnBlueSoft = Color(0xFFE8F4FD)
private val ConnPrimarySoft = Color(0x1A00A8F3)

@Composable
fun ConnectionSettingScreen(
    backLabel: String,
    pageTitle: String,
    onBack: () -> Unit,
    selectedMode: ConnectionUiMode,
    bleActive: Boolean,
    isConnected: Boolean,
    remoteMqttActive: Boolean,
    remoteLinkFromDevice: Boolean,
    onSelectBluetoothMode: () -> Unit,
    onSelectRemoteMode: () -> Unit,
    onOpenBlePair: () -> Unit,
    onOpenRemoteManage: () -> Unit,
    onRemoteDisconnect: () -> Unit,
    remoteChannelLabel: String?,
    remoteSessionError: String?,
    uidValue: String,
    wifiMac: String,
    bleMac: String,
    hardwareValue: String,
    softwareValue: String,
    mechanicValue: String,
    lastPacketText: String,
    activeModeLabel: String,
    bleDiscovering: Boolean = false,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        SettingsBackHeader(
            backLabel = backLabel,
            pageTitle = pageTitle,
            onBack = onBack,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ConnectionStatusSection(
                selectedMode = selectedMode,
                isConnected = isConnected,
                bleDiscovering = bleDiscovering,
                remoteMqttActive = remoteMqttActive,
                remoteLinkFromDevice = remoteLinkFromDevice,
                onSelectBluetoothMode = onSelectBluetoothMode,
                onSelectRemoteMode = onSelectRemoteMode,
            )
            when (selectedMode) {
                ConnectionUiMode.Bluetooth -> ConnectionBlePairSection(onOpenBlePair = onOpenBlePair)
                ConnectionUiMode.Remote -> ConnectionRemoteSection(
                    remoteMqttActive = remoteMqttActive,
                    remoteLinkFromDevice = remoteLinkFromDevice,
                    remoteChannelLabel = remoteChannelLabel,
                    remoteSessionError = remoteSessionError,
                    onOpenRemoteManage = onOpenRemoteManage,
                    onRemoteDisconnect = onRemoteDisconnect,
                )
            }
            ConnectionDeviceInfoSection(
                uidValue = uidValue,
                wifiMac = wifiMac,
                bleMac = bleMac,
                hardwareValue = hardwareValue,
                softwareValue = softwareValue,
                mechanicValue = mechanicValue,
            )
            ConnectionLastStatusSection(
                activeModeLabel = activeModeLabel,
                lastPacketText = lastPacketText,
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ConnectionBlePairSection(onOpenBlePair: () -> Unit) {
    ConnectionSectionCard {
        ConnectionSectionHeader(
            icon = Icons.Filled.Bluetooth,
            title = stringResource(Res.string.ble_pair_title),
            subtitle = stringResource(Res.string.ble_pair_subtitle),
            badgeText = stringResource(Res.string.connection_mode_bluetooth),
            badgeColor = MaterialTheme.colors.primary,
        )
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onOpenBlePair,
            modifier = Modifier
                .vangoTestTag(VanGoTestTags.CONNECTION_BLE_PAIR_OPEN)
                .fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary),
        ) {
            Icon(
                Icons.Filled.Bluetooth,
                contentDescription = null,
                tint = MaterialTheme.colors.onPrimary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(Res.string.connection_open_ble_pair),
                color = MaterialTheme.colors.onPrimary,
            )
        }
    }
}

@Composable
private fun ConnectionStatusSection(
    selectedMode: ConnectionUiMode,
    isConnected: Boolean,
    bleDiscovering: Boolean,
    remoteMqttActive: Boolean,
    remoteLinkFromDevice: Boolean,
    onSelectBluetoothMode: () -> Unit,
    onSelectRemoteMode: () -> Unit,
) {
    val bluetoothMode = selectedMode == ConnectionUiMode.Bluetooth
    val remoteMode = selectedMode == ConnectionUiMode.Remote
    // isConnected = GATT hazır; appConn bayrağı gecikse bile bağlı say
    val bleHardwareLinked = isConnected
    val bleLinked = bleHardwareLinked && bluetoothMode
    val remoteLinked = (remoteMqttActive || remoteLinkFromDevice) && remoteMode
    val anyLinked = bleHardwareLinked || remoteMqttActive || remoteLinkFromDevice

    ConnectionSectionCard(
        backgroundColor = when {
            anyLinked -> ConnGreen.copy(alpha = 0.06f)
            else -> MaterialTheme.colors.surface
        },
    ) {
        ConnectionSectionHeader(
            icon = Icons.Filled.Link,
            title = stringResource(Res.string.connection_status_title),
            subtitle = stringResource(Res.string.connection_status_subtitle),
            badgeText = when {
                anyLinked -> stringResource(Res.string.connection_status_connected)
                else -> stringResource(Res.string.connection_status_disconnected)
            },
            badgeColor = when {
                anyLinked -> ConnGreen
                else -> MaterialTheme.colors.error
            },
        )
        Spacer(Modifier.height(14.dp))
        ConnectionDiagram(
            showRemotePath = remoteMode && !bleHardwareLinked,
            linked = anyLinked,
            isDiscovering = bleDiscovering && bluetoothMode && !bleHardwareLinked,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = when {
                bleHardwareLinked -> stringResource(Res.string.connection_via_bluetooth)
                remoteLinked -> stringResource(Res.string.connection_via_remote)
                bluetoothMode -> stringResource(Res.string.connection_bluetooth_search_hint)
                else -> stringResource(Res.string.connection_not_connected_hint)
            },
            style = MaterialTheme.typography.subtitle1,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colors.onSurface,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        if (bleHardwareLinked) {
            Text(
                text = stringResource(Res.string.connection_local_link),
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.65f),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        } else if (remoteLinked) {
            Text(
                text = stringResource(Res.string.connection_internet_link),
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.65f),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(12.dp))
        ConnectionSignalBanner(strong = anyLinked)
        Spacer(Modifier.height(12.dp))
        ConnectionModeChips(
            bluetoothSelected = bluetoothMode,
            remoteSelected = remoteMode,
            onBluetoothClick = onSelectBluetoothMode,
            onRemoteClick = onSelectRemoteMode,
        )
    }
}

@Composable
private fun ConnectionRemoteSection(
    remoteMqttActive: Boolean,
    remoteLinkFromDevice: Boolean,
    remoteChannelLabel: String?,
    remoteSessionError: String?,
    onOpenRemoteManage: () -> Unit,
    onRemoteDisconnect: () -> Unit,
) {
    val remoteActive = remoteMqttActive || remoteLinkFromDevice
    ConnectionSectionCard(
        backgroundColor = if (remoteActive) ConnGreen.copy(alpha = 0.08f) else MaterialTheme.colors.surface,
    ) {
        ConnectionSectionHeader(
            icon = Icons.Filled.Cloud,
            title = stringResource(Res.string.connection_remote_title),
            subtitle = stringResource(Res.string.connection_remote_subtitle),
            badgeText = if (remoteActive) {
                stringResource(Res.string.connection_remote_active)
            } else {
                stringResource(Res.string.connection_remote_inactive)
            },
            badgeColor = if (remoteActive) ConnGreen else MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(if (remoteActive) ConnGreenSoft else MaterialTheme.colors.background)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Public,
                contentDescription = null,
                tint = if (remoteActive) ConnGreen else MaterialTheme.colors.onSurface.copy(alpha = 0.45f),
                modifier = Modifier.size(28.dp),
            )
            Column(Modifier.weight(1f)) {
                Text(
                    text = if (remoteActive) {
                        stringResource(Res.string.connection_remote_mqtt_active)
                    } else {
                        stringResource(Res.string.connection_remote_mqtt_idle)
                    },
                    style = MaterialTheme.typography.body2,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colors.onSurface,
                )
                remoteChannelLabel?.let { channel ->
                    Text(
                        text = channel,
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.65f),
                    )
                }
            }
            Text(
                text = stringResource(Res.string.connection_latency_unknown),
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.55f),
                textAlign = TextAlign.End,
            )
        }
        remoteSessionError?.let { err ->
            Spacer(Modifier.height(8.dp))
            Text(
                text = err,
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.error,
            )
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = if (remoteMqttActive) onRemoteDisconnect else onOpenRemoteManage,
            modifier = Modifier
                .vangoTestTag(VanGoTestTags.CONNECTION_REMOTE_MANAGE)
                .fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colors.primary,
            ),
        ) {
            Icon(
                imageVector = Icons.Filled.Public,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                if (remoteMqttActive) {
                    stringResource(Res.string.connection_remote_disconnect)
                } else {
                    stringResource(Res.string.connection_remote_manage)
                }
            )
        }
    }
}

@Composable
private fun ConnectionDeviceInfoSection(
    uidValue: String,
    wifiMac: String,
    bleMac: String,
    hardwareValue: String,
    softwareValue: String,
    mechanicValue: String,
) {
    ConnectionSectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = null,
                tint = MaterialTheme.colors.primary,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(Res.string.connection_device_info_title),
                style = MaterialTheme.typography.subtitle1,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.height(8.dp))
        ConnectionDeviceInfoRow(Icons.Filled.Storage, stringResource(Res.string.connection_uid_label), uidValue)
        ConnectionDeviceInfoRow(Icons.Filled.Wifi, stringResource(Res.string.connection_mac_label), wifiMac)
        ConnectionDeviceInfoRow(Icons.Filled.Bluetooth, stringResource(Res.string.connection_ble_mac_label), bleMac)
        ConnectionDeviceInfoRow(Icons.Filled.Memory, stringResource(Res.string.connection_hw_label), hardwareValue)
        ConnectionDeviceInfoRow(Icons.Filled.Settings, stringResource(Res.string.connection_sw_label), softwareValue)
        ConnectionDeviceInfoRow(Icons.Filled.Devices, stringResource(Res.string.connection_mech_label), mechanicValue)
    }
}

@Composable
private fun ConnectionLastStatusSection(
    activeModeLabel: String,
    lastPacketText: String,
) {
    ConnectionSectionCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(
                    imageVector = Icons.Filled.History,
                    contentDescription = null,
                    tint = MaterialTheme.colors.primary,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        text = stringResource(Res.string.connection_last_status_title),
                        style = MaterialTheme.typography.subtitle1,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(Res.string.connection_last_data, lastPacketText),
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.72f),
                    )
                    Text(
                        text = activeModeLabel,
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.55f),
                    )
                }
            }
            OutlinedButton(
                onClick = { /* geçmiş ekranı — sonraki aşama */ },
                enabled = false,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colors.primary.copy(alpha = 0.45f),
                ),
            ) {
                Text(stringResource(Res.string.connection_view_history), fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun ConnectionSectionCard(
    backgroundColor: Color = MaterialTheme.colors.surface,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = 4.dp,
        backgroundColor = backgroundColor,
    ) {
        Column(Modifier.padding(16.dp)) {
            content()
        }
    }
}

@Composable
private fun ConnectionSectionHeader(
    icon: ImageVector,
    title: String,
    subtitle: String,
    badgeText: String,
    badgeColor: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.Top) {
            Icon(icon, null, tint = MaterialTheme.colors.primary, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(10.dp))
            Column {
                Text(title, style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.Bold)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.65f),
                )
            }
        }
        ConnectionStatusBadge(badgeText, badgeColor)
    }
}

@Composable
private fun ConnectionStatusBadge(text: String, color: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(Icons.Filled.Check, null, tint = color, modifier = Modifier.size(14.dp))
        Text(text, style = MaterialTheme.typography.caption, fontWeight = FontWeight.SemiBold, color = color)
    }
}

@Composable
private fun ConnectionDiagram(
    showRemotePath: Boolean,
    linked: Boolean,
    isDiscovering: Boolean,
) {
    val lineColor = when {
        linked -> MaterialTheme.colors.primary
        isDiscovering -> MaterialTheme.colors.primary.copy(alpha = 0.55f)
        else -> MaterialTheme.colors.onSurface.copy(alpha = 0.2f)
    }
    val hubBg = when {
        linked -> MaterialTheme.colors.primary
        isDiscovering -> MaterialTheme.colors.primary.copy(alpha = 0.75f)
        else -> MaterialTheme.colors.onSurface.copy(alpha = 0.22f)
    }
    val hubIcon = if (showRemotePath) Icons.Filled.Cloud else Icons.Filled.Bluetooth
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ConnectionDiagramNode(
            icon = Icons.Filled.PhoneAndroid,
            label = stringResource(Res.string.connection_phone_label),
            highlighted = linked || isDiscovering,
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(3.dp)
                .padding(horizontal = 6.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(lineColor),
        )
        if (isDiscovering) {
            BleRadarIndicator(
                modifier = Modifier.size(60.dp),
                accent = MaterialTheme.colors.primary,
                active = true,
                centerIcon = {
                    Icon(hubIcon, null, tint = MaterialTheme.colors.primary, modifier = Modifier.size(24.dp))
                },
            )
        } else {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(hubBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(hubIcon, null, tint = Color.White, modifier = Modifier.size(30.dp))
            }
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .height(3.dp)
                .padding(horizontal = 6.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(lineColor),
        )
        ConnectionDiagramNode(
            icon = Icons.Filled.Devices,
            label = stringResource(Res.string.connection_device_label),
            highlighted = linked || isDiscovering,
        )
    }
}

@Composable
private fun ConnectionDiagramNode(
    icon: ImageVector,
    label: String,
    highlighted: Boolean,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (highlighted) ConnPrimarySoft else MaterialTheme.colors.background,
                )
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
                modifier = Modifier.size(26.dp),
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.caption,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ConnectionSignalBanner(strong: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (strong) ConnBlueSoft else MaterialTheme.colors.background)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.Security,
            null,
            tint = MaterialTheme.colors.primary,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = if (strong) {
                stringResource(Res.string.connection_signal_strong)
            } else {
                stringResource(Res.string.connection_signal_weak)
            },
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.85f),
            modifier = Modifier.weight(1f),
        )
        Icon(
            Icons.Filled.SignalCellularAlt,
            null,
            tint = MaterialTheme.colors.primary,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun ConnectionModeChips(
    bluetoothSelected: Boolean,
    remoteSelected: Boolean,
    onBluetoothClick: () -> Unit,
    onRemoteClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ConnectionModeChip(
            label = stringResource(Res.string.connection_mode_bluetooth),
            icon = Icons.Filled.Bluetooth,
            selected = bluetoothSelected,
            onClick = onBluetoothClick,
            modifier = Modifier.weight(1f),
        )
        ConnectionModeChip(
            label = stringResource(Res.string.connection_mode_remote),
            icon = Icons.Filled.Cloud,
            selected = remoteSelected,
            onClick = onRemoteClick,
            modifier = Modifier
                .weight(1f)
                .vangoTestTag(VanGoTestTags.CONNECTION_REMOTE_MODE),
        )
    }
}

@Composable
private fun ConnectionModeChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = if (selected) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface.copy(0.2f)
    val bg = if (selected) ConnPrimarySoft else MaterialTheme.colors.surface
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = if (selected) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface.copy(0.45f))
        Spacer(Modifier.width(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.body2,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface.copy(0.65f),
        )
    }
}

@Composable
private fun ConnectionDiscoveringRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colors.primary.copy(alpha = 0.12f))
            .border(1.dp, MaterialTheme.colors.primary.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        SpiralWaitingIcon(Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(Res.string.dialog_discovering),
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.primary,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun ConnectionDeviceInfoRow(
    icon: ImageVector,
    label: String,
    value: String,
) {
    val display = value.ifBlank { "\u2014" }
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, null, tint = MaterialTheme.colors.primary.copy(0.85f), modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text(
                label,
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface.copy(0.75f),
                modifier = Modifier.weight(1f),
            )
            Text(
                display,
                style = MaterialTheme.typography.body2,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colors.onSurface,
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                Icons.Filled.ChevronRight,
                null,
                tint = MaterialTheme.colors.onSurface.copy(0.25f),
                modifier = Modifier.size(18.dp),
            )
        }
        Divider(color = MaterialTheme.colors.onSurface.copy(alpha = 0.08f))
    }
}

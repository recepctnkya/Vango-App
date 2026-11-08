package com.hextech.vango

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import testvango.composeapp.generated.resources.Res
import testvango.composeapp.generated.resources.hexnet_mark
import testvango.composeapp.generated.resources.perm_conn_action_enable
import testvango.composeapp.generated.resources.perm_conn_action_grant
import testvango.composeapp.generated.resources.perm_conn_bt_desc
import testvango.composeapp.generated.resources.perm_conn_bt_title
import testvango.composeapp.generated.resources.perm_conn_find_devices
import testvango.composeapp.generated.resources.perm_conn_hint_incomplete
import testvango.composeapp.generated.resources.perm_conn_hint_ready
import testvango.composeapp.generated.resources.perm_conn_info_location
import testvango.composeapp.generated.resources.perm_conn_info_usage
import testvango.composeapp.generated.resources.perm_conn_loc_desc
import testvango.composeapp.generated.resources.perm_conn_loc_title
import testvango.composeapp.generated.resources.perm_conn_nearby_desc
import testvango.composeapp.generated.resources.perm_conn_nearby_title
import testvango.composeapp.generated.resources.perm_conn_skip
import testvango.composeapp.generated.resources.perm_conn_status_granted
import testvango.composeapp.generated.resources.perm_conn_status_on
import testvango.composeapp.generated.resources.perm_denied_body
import testvango.composeapp.generated.resources.perm_denied_title

private val PermOkGreen = Color(0xFF2ECC71)

@Composable
fun ConnectionPermissionsScreen(
    onEnterApp: (startDiscovery: Boolean) -> Unit,
    onBack: (() -> Unit)? = null,
) {
    val accent = accentColorFromId(customAppAccentId)
    val chrome = LocalAppChrome.current
    val bg = chrome.bg
    val permText = chrome.text
    val permMuted = chrome.muted
    val permCardBg = chrome.card
    val permBorder = chrome.border
    val permInk = chrome.ink
    val permDisabledBg = chrome.track
    val scope = rememberCoroutineScope()

    var nearbyOk by remember { mutableStateOf(hasNearbyDevicesPermissions()) }
    var bluetoothOn by remember { mutableStateOf(isBluetoothAdapterEnabled()) }
    var locationOk by remember { mutableStateOf(hasLocationPermissions()) }
    var entered by remember { mutableStateOf(false) }

    fun refresh() {
        nearbyOk = hasNearbyDevicesPermissions()
        bluetoothOn = isBluetoothAdapterEnabled()
        locationOk = hasLocationPermissions()
    }

    val allReady = nearbyOk && bluetoothOn && locationOk

    LaunchedEffect(Unit) {
        while (!entered) {
            val n = hasNearbyDevicesPermissions()
            val b = isBluetoothAdapterEnabled()
            val l = hasLocationPermissions()
            nearbyOk = n
            bluetoothOn = b
            locationOk = l
            if (n && b && l) {
                entered = true
                getVanGO().checkPermissions()
                onEnterApp(true)
                return@LaunchedEffect
            }
            delay(600)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = accent,
                    )
                }
            } else {
                Spacer(Modifier.width(48.dp))
            }
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(Res.drawable.hexnet_mark),
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "VanGO",
                    color = accent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                )
            }
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.width(48.dp))
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(8.dp))
            Icon(
                imageVector = Icons.Default.Bluetooth,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(56.dp),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(Res.string.perm_denied_title),
                color = permText,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(Res.string.perm_denied_body),
                color = permMuted,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            Spacer(Modifier.height(20.dp))

            PermissionCheckRow(
                icon = Icons.Default.Sensors,
                title = stringResource(Res.string.perm_conn_nearby_title),
                subtitle = stringResource(Res.string.perm_conn_nearby_desc),
                done = nearbyOk,
                doneLabel = stringResource(Res.string.perm_conn_status_granted),
                actionLabel = stringResource(Res.string.perm_conn_action_grant),
                accent = accent,
                textColor = permText,
                mutedColor = permMuted,
                cardBg = permCardBg,
                borderColor = permBorder,
                inkColor = permInk,
                onAction = {
                    requestNearbyDevicesPermissions()
                    scope.launch {
                        delay(400)
                        refresh()
                    }
                },
            )
            Spacer(Modifier.height(10.dp))
            PermissionCheckRow(
                icon = Icons.Default.Bluetooth,
                title = stringResource(Res.string.perm_conn_bt_title),
                subtitle = stringResource(Res.string.perm_conn_bt_desc),
                done = bluetoothOn,
                doneLabel = stringResource(Res.string.perm_conn_status_on),
                actionLabel = stringResource(Res.string.perm_conn_action_enable),
                accent = accent,
                textColor = permText,
                mutedColor = permMuted,
                cardBg = permCardBg,
                borderColor = permBorder,
                inkColor = permInk,
                onAction = {
                    requestEnableBluetooth()
                    scope.launch {
                        delay(400)
                        refresh()
                    }
                },
            )
            Spacer(Modifier.height(10.dp))
            PermissionCheckRow(
                icon = Icons.Default.LocationOn,
                title = stringResource(Res.string.perm_conn_loc_title),
                subtitle = stringResource(Res.string.perm_conn_loc_desc),
                done = locationOk,
                doneLabel = stringResource(Res.string.perm_conn_status_granted),
                actionLabel = stringResource(Res.string.perm_conn_action_grant),
                accent = accent,
                textColor = permText,
                mutedColor = permMuted,
                cardBg = permCardBg,
                borderColor = permBorder,
                inkColor = permInk,
                onAction = {
                    requestLocationPermissions()
                    scope.launch {
                        delay(400)
                        refresh()
                    }
                },
            )

            Spacer(Modifier.height(16.dp))
            InfoLine(
                icon = Icons.Default.Info,
                text = stringResource(Res.string.perm_conn_info_location),
                mutedColor = permMuted,
            )
            Spacer(Modifier.height(6.dp))
            InfoLine(
                icon = Icons.Default.Security,
                text = stringResource(Res.string.perm_conn_info_usage),
                mutedColor = permMuted,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = if (allReady) {
                    stringResource(Res.string.perm_conn_hint_ready)
                } else {
                    stringResource(Res.string.perm_conn_hint_incomplete)
                },
                color = permMuted,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
        }

        Button(
            onClick = {
                if (!allReady || entered) return@Button
                entered = true
                getVanGO().checkPermissions()
                onEnterApp(true)
            },
            enabled = allReady && !entered,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = accent,
                contentColor = permInk,
                disabledBackgroundColor = permDisabledBg,
                disabledContentColor = permMuted,
            ),
        ) {
            Text(
                text = stringResource(Res.string.perm_conn_find_devices),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
            )
        }
        TextButton(
            onClick = {
                if (entered) return@TextButton
                entered = true
                onEnterApp(false)
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(Res.string.perm_conn_skip),
                color = accent,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
            )
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun PermissionCheckRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    done: Boolean,
    doneLabel: String,
    actionLabel: String,
    accent: Color,
    textColor: Color,
    mutedColor: Color,
    cardBg: Color,
    borderColor: Color,
    inkColor: Color,
    onAction: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(cardBg, RoundedCornerShape(14.dp))
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(accent.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = textColor, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(text = subtitle, color = mutedColor, fontSize = 12.sp)
        }
        Spacer(Modifier.width(8.dp))
        if (done) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .background(PermOkGreen, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp),
                    )
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    text = doneLabel,
                    color = PermOkGreen,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                )
            }
        } else {
            Button(
                onClick = onAction,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = accent,
                    contentColor = inkColor,
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(text = actionLabel, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun InfoLine(icon: ImageVector, text: String, mutedColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.Start,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = mutedColor,
            modifier = Modifier.size(16.dp).padding(top = 2.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(text = text, color = mutedColor, fontSize = 12.sp)
    }
}

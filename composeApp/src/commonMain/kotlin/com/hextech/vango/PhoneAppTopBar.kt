package com.hextech.vango

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Wifi
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import testvango.composeapp.generated.resources.Res
import testvango.composeapp.generated.resources.dashboard_conn_ble
import testvango.composeapp.generated.resources.dashboard_conn_remote
import testvango.composeapp.generated.resources.hexnet_mark

private val PhoneLinkGreen = Color(0xFF3DDC84)

/**
 * Phone-only top app bar: menu, brand, link icons, notifications.
 * Tablet uses [DashboardHeaderBar] instead.
 */
@Composable
fun PhoneAppTopBar(
    accent: Color,
    hasAlerts: Boolean,
    onMenu: () -> Unit,
    onNotifications: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val chrome = LocalAppChrome.current
    val fg = chrome.text
    val linkMuted = chrome.muted
    var wifiOk by remember { mutableStateOf(false) }
    var linkClock by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            wifiOk = isWifiConnected()
            delay(2_000)
        }
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            linkClock++
        }
    }
    val appConn by appConnectionState.collectAsState()
    val bleGlobal by vanGoGlobal.collectAsState()
    val lastTel by lastSuccessfulTelemetryEpochSec.collectAsState()
    val bleOk = remember(bleGlobal, lastTel, linkClock) { isBleGattLinkReady() }
    val remoteOk = appConn == AppConnectionState.REMOTE_MQTT_CONNECTED || isAppRemoteMqttConnected()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .padding(top = 14.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.Menu,
            contentDescription = null,
            tint = fg,
            modifier = Modifier
                .size(28.dp)
                .clickable(onClick = onMenu),
        )
        Spacer(Modifier.width(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(Res.drawable.hexnet_mark),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(6.dp)),
            )
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = fg, fontWeight = FontWeight.Bold)) { append("Van") }
                    withStyle(SpanStyle(color = accent, fontWeight = FontWeight.Bold)) { append("GO") }
                },
                fontSize = 18.sp,
                maxLines = 1,
                modifier = Modifier.offset(x = (-3).dp),
            )
        }
        Spacer(Modifier.weight(1f))
        PhoneLinkIcon(
            icon = Icons.Filled.Bluetooth,
            active = bleOk,
            activeTint = accent,
            mutedTint = linkMuted,
            label = stringResource(Res.string.dashboard_conn_ble),
        )
        Spacer(Modifier.width(6.dp))
        PhoneLinkIcon(
            icon = Icons.Filled.Wifi,
            active = wifiOk,
            activeTint = accent,
            mutedTint = linkMuted,
            label = "WiFi",
        )
        Spacer(Modifier.width(6.dp))
        PhoneLinkIcon(
            icon = Icons.Filled.Cloud,
            active = remoteOk,
            activeTint = PhoneLinkGreen,
            mutedTint = linkMuted,
            label = stringResource(Res.string.dashboard_conn_remote),
        )
        Spacer(Modifier.width(6.dp))
        Box(
            modifier = Modifier
                .size(36.dp)
                .clickable(onClick = onNotifications),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Notifications,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(24.dp),
            )
            if (hasAlerts) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 4.dp, end = 4.dp)
                        .size(8.dp)
                        .background(accent, CircleShape),
                )
            }
        }
    }
}

@Composable
private fun PhoneLinkIcon(
    icon: ImageVector,
    active: Boolean,
    activeTint: Color,
    mutedTint: Color,
    label: String,
) {
    Icon(
        icon,
        contentDescription = label,
        tint = if (active) activeTint else mutedTint.copy(alpha = 0.55f),
        modifier = Modifier.size(20.dp),
    )
}

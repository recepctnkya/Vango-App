package com.hextech.vango

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hextech.vango.profile.CaravanType
import com.hextech.vango.profile.LoginPreferences
import org.jetbrains.compose.resources.painterResource
import testvango.composeapp.generated.resources.Res
import testvango.composeapp.generated.resources.caravan_motor as caravan_motor_drawable
import testvango.composeapp.generated.resources.caravan_tow as caravan_tow_drawable
import kotlin.math.max

@Composable
fun DeviceOverviewCard(
    modifier: Modifier = Modifier,
    onPowerClick: (() -> Unit)? = null,
) {
    val global by vanGoGlobal.collectAsState()
    val appConn by appConnectionState.collectAsState()
    val telemetry by remoteTelemetry.collectAsState()
    val lastUpdate by lastSuccessfulTelemetryEpochSec.collectAsState()

    val bleConnected = isBleGattLinkReady()
    val online = bleConnected || isRemoteSessionUsable()
    val rssi = telemetry?.status?.wifiRssi

    val cardBg = if (MaterialTheme.colors.isLight) Color(0xFFFDFDFE) else Color(0xFF161B24)
    val border = if (MaterialTheme.colors.isLight) Color(0xFFE9EDF6) else Color(0xFF283244)
    val softBlue = if (MaterialTheme.colors.isLight) Color(0xFFEFF5FF) else Color(0xFF1B2940)
    val titleColor = MaterialTheme.colors.onSurface
    val bodyColor = MaterialTheme.colors.onSurface.copy(alpha = 0.74f)
    val onlineColor = Color(0xFF22C55E)
    val accent = MaterialTheme.colors.primary
    val caravanType = cachedCaravanType()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, border, RoundedCornerShape(18.dp)),
        backgroundColor = cardBg,
        shape = RoundedCornerShape(18.dp),
        elevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .weight(1.06f)
                    .aspectRatio(1.42f)
                    .background(
                        brush = Brush.linearGradient(
                            listOf(
                                if (MaterialTheme.colors.isLight) Color(0xFFFFFFFF) else Color(0xFF1A2230),
                                if (MaterialTheme.colors.isLight) Color(0xFFF5F8FF) else Color(0xFF111827),
                            ),
                        ),
                        shape = RoundedCornerShape(16.dp),
                    )
                    .clip(RoundedCornerShape(16.dp))
            ) {
                Image(
                    painter = painterResource(caravanImage(caravanType)),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth(0.94f)
                        .align(Alignment.CenterStart)
                        .offset(x = (-52).dp)
                        .graphicsLayer {
                            scaleX = 1.5f
                            scaleY = 1.5f
                        }
                        .padding(vertical = 0.dp),
                    contentScale = ContentScale.Fit,
                )
            }

            Column(
                modifier = Modifier.weight(0.96f),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "VANGO Karavan",
                    color = titleColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(if (online) onlineColor else bodyColor.copy(alpha = 0.45f), CircleShape),
                    )
                    Text(
                        text = if (online) "Online" else "Offline",
                        color = if (online) onlineColor else bodyColor,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                    )
                }
                Spacer(Modifier.height(6.dp))
                OverviewInfoRow(
                    icon = {
                        Icon(
                            Icons.Filled.Wifi,
                            contentDescription = null,
                            tint = bodyColor.copy(alpha = 0.72f),
                            modifier = Modifier.size(16.dp),
                        )
                    },
                    text = when {
                        rssi != null -> "Guc: $rssi dBm"
                        online -> "Baglanti aktif"
                        else -> "Bagli cihaz yok"
                    },
                    color = bodyColor,
                )
                Spacer(Modifier.height(3.dp))
                OverviewInfoRow(
                    icon = {
                        Icon(
                            Icons.Filled.AccessTime,
                            contentDescription = null,
                            tint = bodyColor.copy(alpha = 0.72f),
                            modifier = Modifier.size(16.dp),
                        )
                    },
                    text = "Son guncelleme: ${formatLastUpdate(lastUpdate)}",
                    color = bodyColor,
                )
            }

            Column(
                modifier = Modifier.width(74.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .background(softBlue, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    IconButton(
                        onClick = { onPowerClick?.invoke() },
                        modifier = Modifier
                            .size(46.dp)
                            .background(Color.White.copy(alpha = if (MaterialTheme.colors.isLight) 0.88f else 0.1f), CircleShape),
                    ) {
                        Icon(
                            Icons.Filled.PowerSettingsNew,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(25.dp),
                        )
                    }
                }
                Text(
                    text = "TUM SISTEMI\nKAPAT",
                    color = titleColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    lineHeight = 13.sp,
                )
            }
        }
    }
}

@Composable
private fun OverviewInfoRow(
    icon: @Composable () -> Unit,
    text: String,
    color: Color,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        icon()
        Text(
            text = text,
            color = color,
            fontSize = 12.sp,
        )
    }
}

private fun formatLastUpdate(epochSec: Long): String {
    if (epochSec <= 0L) return "-"
    val diff = max(0L, getCurrentTimeInSeconds() - epochSec)
    return when {
        diff < 10L -> "simdi"
        diff < 60L -> "${diff.toInt()} sn once"
        diff < 3600L -> "${(diff / 60L).toInt()} dk once"
        else -> "${(diff / 3600L).toInt()} sa once"
    }
}

private fun cachedCaravanType(): CaravanType {
    caravanTypeRevision // read for recomposition
    return when (LoginPreferences.getCachedCaravanType()) {
        CaravanType.MotorCaravan.name -> CaravanType.MotorCaravan
        CaravanType.TowCaravan.name -> CaravanType.TowCaravan
        else -> CaravanType.TowCaravan
    }
}

private fun caravanImage(type: CaravanType) = when (type) {
    CaravanType.TowCaravan -> Res.drawable.caravan_tow_drawable
    CaravanType.MotorCaravan -> Res.drawable.caravan_motor_drawable
}

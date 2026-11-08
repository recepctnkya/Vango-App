package com.hextech.vango

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Hotel
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import org.jetbrains.compose.resources.stringResource
import testvango.composeapp.generated.resources.Res
import testvango.composeapp.generated.resources.climate_active_time
import testvango.composeapp.generated.resources.climate_comfort_normal
import testvango.composeapp.generated.resources.climate_fan_control
import testvango.composeapp.generated.resources.climate_fan_ceiling
import testvango.composeapp.generated.resources.climate_fan_tap_hint
import testvango.composeapp.generated.resources.climate_feedback
import testvango.composeapp.generated.resources.climate_feedback_normal
import testvango.composeapp.generated.resources.climate_humidity
import testvango.composeapp.generated.resources.climate_no_fan
import testvango.composeapp.generated.resources.climate_no_sensor
import testvango.composeapp.generated.resources.climate_speed
import testvango.composeapp.generated.resources.climate_status
import testvango.composeapp.generated.resources.climate_status_off
import testvango.composeapp.generated.resources.climate_status_on
import testvango.composeapp.generated.resources.climate_temp
import testvango.composeapp.generated.resources.dashboard_nav_climate
import testvango.composeapp.generated.resources.metric_no_data
import kotlin.math.roundToInt

private val CliBg: Color @Composable get() = LocalAppChrome.current.bg
private val CliPanel: Color @Composable get() = LocalAppChrome.current.panel
private val CliCard: Color @Composable get() = LocalAppChrome.current.card
private val CliCyan = Color(0xFF2EC5FF)
private val CliText: Color @Composable get() = LocalAppChrome.current.text
private val CliMuted: Color @Composable get() = LocalAppChrome.current.muted
private val CliTrack: Color @Composable get() = LocalAppChrome.current.track
private val CliGreen = Color(0xFF3DDC84)
private val CliPurple = Color(0xFFB794F6)
private val CliBorder: Color @Composable get() = LocalAppChrome.current.border

@Composable
fun ClimateControlScreen(
    data: VanGoData,
    env: BleEnvSensors,
    modifier: Modifier = Modifier,
) {
    val channels by panelPlatformChannels.collectAsState()
    val outdoorSource = climateOutdoorSource
    val weather by climateWeatherCache.collectAsState()
    val zones = remember(data, env, channels, outdoorSource, weather) {
        discoverClimateTempZones(data, env, channels, outdoorSource, weather)
    }
    val fans = remember(data.outNB.contentHashCode(), data.dimNB.contentHashCode(), data.numOfO, data.numOfD, channels) {
        discoverClimateFans(data, channels)
    }

    LaunchedEffect(outdoorSource) {
        if (outdoorSource != ClimateOutdoorSource.Location) return@LaunchedEffect
        while (true) {
            runCatching {
                if (!hasLocationPermissions()) return@runCatching
                val coords = getLocationUpdates() ?: return@runCatching
                val info = WeatherService(WeatherApiKey).getWeather(coords.first, coords.second)
                if (info != null) updateClimateWeatherCache(info)
            }
            delay(120_000)
        }
    }

    val compact = isCompactLayout()
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CliBg)
            .padding(horizontal = if (isPhoneUi()) 0.dp else 14.dp, vertical = if (isPhoneUi()) 0.dp else 10.dp)
            .then(if (compact) Modifier.verticalScroll(rememberScrollState()) else Modifier),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (!isPhoneUi()) {
            ClimatePageHeader()
        }

        // Mockup: zone cards in a horizontal row (tablet) / stacked (phone)
        if (zones.isEmpty()) {
            ClimateEmptyCard(
                message = stringResource(Res.string.climate_no_sensor),
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (compact) Modifier.height(160.dp) else Modifier.weight(0.55f)),
            )
        } else if (compact) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                zones.forEach { zone ->
                    ClimateZoneCard(
                        zone = zone,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                    )
                }
            }
        } else {
            // Keep zone card width fixed to a 2-column slot so 1–2 sensors don't stretch oddly.
            BoxWithConstraints(
                modifier = Modifier
                    .weight(0.50f)
                    .fillMaxWidth(),
            ) {
                val gap = 12.dp
                val slots = maxOf(2, zones.size)
                val cardWidth = (maxWidth - gap * (slots - 1)) / slots
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(gap),
                ) {
                    zones.forEach { zone ->
                        ClimateZoneCard(
                            zone = zone,
                            modifier = Modifier
                                .width(cardWidth)
                                .fillMaxHeight(),
                        )
                    }
                }
            }
        }

        Text(
            stringResource(Res.string.climate_fan_control).uppercase(),
            color = CliText,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
            modifier = Modifier.padding(start = 2.dp, top = 2.dp),
        )

        if (fans.isEmpty()) {
            ClimateEmptyCard(
                message = stringResource(Res.string.climate_no_fan),
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (compact) Modifier.height(120.dp) else Modifier.weight(0.42f)),
            )
        } else {
            Column(
                modifier = Modifier
                    .then(if (compact) Modifier else Modifier.weight(0.42f))
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                fans.forEach { fan ->
                    ClimateFanPanel(
                        data = data,
                        fan = fan,
                        modifier = Modifier
                            .then(if (compact) Modifier.height(140.dp) else Modifier.weight(1f))
                            .fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun ClimatePageHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CliPanel, RoundedCornerShape(14.dp))
            .border(1.dp, CliBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 18.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Filled.AcUnit, null, tint = CliCyan, modifier = Modifier.size(26.dp))
        Text(
            stringResource(Res.string.dashboard_nav_climate).uppercase(),
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.8.sp,
            maxLines = 1,
        )
    }
}

@Composable
private fun ClimateEmptyCard(message: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(CliCard, RoundedCornerShape(16.dp))
            .border(1.dp, CliBorder, RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(message, color = CliMuted, fontSize = 14.sp)
    }
}

@Composable
private fun ClimateZoneCard(
    zone: ClimateZoneCard,
    modifier: Modifier = Modifier,
) {
    val accent = when (zone.kind) {
        ClimateZoneKind.Indoor -> CliCyan
        ClimateZoneKind.Outdoor -> CliGreen
        ClimateZoneKind.Bedroom -> CliPurple
        ClimateZoneKind.Other -> CliCyan
    }
    val zoneIcon: ImageVector = when (zone.kind) {
        ClimateZoneKind.Indoor -> Icons.Outlined.Home
        ClimateZoneKind.Outdoor -> Icons.Filled.Park
        ClimateZoneKind.Bedroom -> Icons.Outlined.Hotel
        ClimateZoneKind.Other -> Icons.Filled.Thermostat
    }
    val comfort = if (zone.hasReading) climateComfortLabel(zone.tempC, zone.humidityPct) else "—"
    val frac = if (zone.hasReading) climateComfortFraction(zone.tempC, zone.humidityPct) else 0f
    val noData = stringResource(Res.string.metric_no_data)
    val tempText = if (zone.hasReading) {
        "${(zone.tempC * 10).roundToInt() / 10.0}°C"
    } else {
        noData
    }
    val humText = if (zone.hasReading) "%${zone.humidityPct}" else noData
    val comfortNormal = stringResource(Res.string.climate_comfort_normal)

    Column(
        modifier = modifier
            .background(CliCard, RoundedCornerShape(16.dp))
            .border(1.dp, CliBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        // Header: title + small corner icon (mockup proportions)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                zone.title,
                color = CliText,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.6.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Icon(
                zoneIcon,
                null,
                tint = accent.copy(alpha = 0.9f),
                modifier = Modifier.size(32.dp),
            )
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                ClimateLiveTempRow(
                    tempC = zone.tempC,
                    label = stringResource(Res.string.climate_temp),
                    value = tempText,
                )
                ClimateLiveHumidityRow(
                    humidityPct = zone.humidityPct,
                    label = stringResource(Res.string.climate_humidity),
                    value = humText,
                )
            }

            ClimateComfortRing(
                fraction = frac,
                line1 = if (comfort == "Normal Konfor" || comfort == comfortNormal) "Normal" else comfort,
                line2 = if (comfort == "Normal Konfor" || comfort == comfortNormal) "Konfor" else "",
                color = accent,
                modifier = Modifier.size(104.dp),
            )
        }
    }
}

/** Home-page style live thermometer: mercury fill + glow by temperature. */
@Composable
private fun ClimateLiveTempRow(
    tempC: Float,
    label: String,
    value: String,
) {
    val tempTint = when {
        tempC <= 5f -> Color(0xFF60A5FA)
        tempC <= 15f -> Color(0xFF38BDF8)
        tempC <= 22f -> CliCyan
        tempC <= 28f -> Color(0xFF4ADE80)
        tempC <= 34f -> Color(0xFFFBBF24)
        else -> Color(0xFFF87171)
    }
    val transition = rememberInfiniteTransition(label = "cli-temp-live")
    val glow by transition.animateFloat(
        initialValue = 0.70f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "cli-temp-glow",
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        LiveThermometerIcon(
            tempC = tempC,
            tint = tempTint,
            glow = glow,
            modifier = Modifier.size(width = 52.dp, height = 96.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                value,
                color = CliText,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                lineHeight = 34.sp,
            )
            Text(label, color = CliMuted, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1)
        }
    }
}

/** Animated droplet fill level matches humidity % (same live feel as home thermometer). */
@Composable
private fun ClimateLiveHumidityRow(
    humidityPct: Int,
    label: String,
    value: String,
) {
    val tint = CliCyan
    val transition = rememberInfiniteTransition(label = "cli-hum-live")
    val glow by transition.animateFloat(
        initialValue = 0.72f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "cli-hum-glow",
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        LiveHumidityDropIcon(
            humidityPct = humidityPct,
            tint = tint,
            glow = glow,
            modifier = Modifier.size(width = 58.dp, height = 82.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                value,
                color = CliText,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                lineHeight = 34.sp,
            )
            Text(label, color = CliMuted, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1)
        }
    }
}

@Composable
private fun LiveThermometerIcon(
    tempC: Float,
    tint: Color,
    glow: Float,
    modifier: Modifier = Modifier,
) {
    val mercury = ((tempC - 0f) / 45f).coerceIn(0.12f, 1f)
    val strokeTint = tint.copy(alpha = 0.75f + glow * 0.25f)
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val tubeW = w * 0.28f
        val bulbR = w * 0.36f
        val cx = w / 2f
        val tubeLeft = cx - tubeW / 2f
        val tubeTop = h * 0.05f
        val bulbCy = h - bulbR - h * 0.03f
        val tubeBottom = bulbCy - bulbR * 0.42f
        val stroke = (w * 0.09f).coerceIn(1.6f, 2.8f)

        val fillTop = tubeBottom - (tubeBottom - tubeTop) * mercury
        drawRoundRect(
            color = tint.copy(alpha = 0.85f),
            topLeft = Offset(tubeLeft + stroke * 0.6f, fillTop),
            size = Size(tubeW - stroke * 1.2f, tubeBottom - fillTop + 2f),
            cornerRadius = CornerRadius(tubeW / 2f, tubeW / 2f),
        )
        drawCircle(
            color = tint.copy(alpha = 0.90f),
            radius = bulbR - stroke * 1.1f,
            center = Offset(cx, bulbCy),
        )
        drawRoundRect(
            color = strokeTint,
            topLeft = Offset(tubeLeft, tubeTop),
            size = Size(tubeW, tubeBottom - tubeTop + bulbR * 0.35f),
            cornerRadius = CornerRadius(tubeW / 2f, tubeW / 2f),
            style = Stroke(width = stroke),
        )
        drawCircle(
            color = strokeTint,
            radius = bulbR,
            center = Offset(cx, bulbCy),
            style = Stroke(width = stroke),
        )
        for (i in 1..4) {
            val ty = tubeTop + (tubeBottom - tubeTop) * (i / 5f)
            drawLine(
                color = strokeTint.copy(alpha = 0.55f),
                start = Offset(tubeLeft + tubeW + 1.5f, ty),
                end = Offset(tubeLeft + tubeW + w * 0.22f, ty),
                strokeWidth = stroke * 0.7f,
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun LiveHumidityDropIcon(
    humidityPct: Int,
    tint: Color,
    glow: Float,
    modifier: Modifier = Modifier,
) {
    val fillFrac = (humidityPct / 100f).coerceIn(0.12f, 1f)
    val strokeTint = tint.copy(alpha = 0.70f + glow * 0.30f)
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val tipY = h * 0.06f
        val bottomY = h * 0.92f
        val bodyR = w * 0.38f
        val drop = Path().apply {
            moveTo(cx, tipY)
            cubicTo(
                cx + w * 0.08f, tipY + h * 0.18f,
                cx + bodyR, bottomY - bodyR * 1.15f,
                cx + bodyR, bottomY - bodyR * 0.55f,
            )
            cubicTo(
                cx + bodyR, bottomY + bodyR * 0.15f,
                cx - bodyR, bottomY + bodyR * 0.15f,
                cx - bodyR, bottomY - bodyR * 0.55f,
            )
            cubicTo(
                cx - bodyR, bottomY - bodyR * 1.15f,
                cx - w * 0.08f, tipY + h * 0.18f,
                cx, tipY,
            )
            close()
        }
        clipPath(drop) {
            val fillTop = tipY + (bottomY - tipY) * (1f - fillFrac)
            drawRect(
                color = tint.copy(alpha = 0.82f),
                topLeft = Offset(0f, fillTop),
                size = Size(w, h - fillTop),
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.18f * glow),
                radius = bodyR * 0.35f,
                center = Offset(cx - bodyR * 0.25f, fillTop + (bottomY - fillTop) * 0.35f),
            )
        }
        drawPath(drop, color = strokeTint, style = Stroke(width = (w * 0.08f).coerceIn(1.8f, 2.8f)))
    }
}

@Composable
private fun ClimateComfortRing(
    fraction: Float,
    line1: String,
    line2: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        val track = CliTrack
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 7.dp.toPx()
            val pad = stroke / 2f + 1f
            drawArc(
                color = track,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(pad, pad),
                size = Size(size.width - pad * 2f, size.height - pad * 2f),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * fraction.coerceIn(0.08f, 1f),
                useCenter = false,
                topLeft = Offset(pad, pad),
                size = Size(size.width - pad * 2f, size.height - pad * 2f),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                line1,
                color = CliText,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            if (line2.isNotBlank()) {
                Text(
                    line2,
                    color = CliMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
        }
    }
}

/**
 * Live 4-blade fan (mockup silhouette). Rotation °/s scales with [speedPct].
 */
@Composable
private fun LiveFanIcon(
    speedPct: Int,
    active: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    var angle by remember { mutableStateOf(0f) }
    val spinning = active && speedPct > 0
    val degPerSec = if (spinning) (100f + (speedPct.coerceIn(1, 100) / 100f) * 680f) else 0f

    LaunchedEffect(spinning, degPerSec) {
        if (!spinning) return@LaunchedEffect
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                if (last != 0L) {
                    val dt = ((now - last).coerceIn(0L, 32_000_000L)) / 1_000_000_000f
                    angle = (angle + degPerSec * dt) % 360f
                }
                last = now
            }
        }
    }

    val muted = CliMuted
    val card = CliCard
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r = size.minDimension * 0.48f
        val hubOuter = size.minDimension * 0.13f
        val hubInner = hubOuter * 0.42f
        val fill = if (active) accent else muted.copy(alpha = 0.5f)
        val strokeCol = if (active) accent.copy(alpha = 0.95f) else muted.copy(alpha = 0.7f)

        if (spinning) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accent.copy(alpha = 0.22f + speedPct / 100f * 0.18f),
                        Color.Transparent,
                    ),
                    center = Offset(cx, cy),
                    radius = r * 1.15f,
                ),
                radius = r * 1.12f,
                center = Offset(cx, cy),
            )
        }

        rotate(degrees = angle, pivot = Offset(cx, cy)) {
            repeat(4) { i ->
                rotate(degrees = i * 90f, pivot = Offset(cx, cy)) {
                    // Classic ceiling-fan blade: wide tip, narrow root, slight curve
                    val blade = Path().apply {
                        moveTo(cx + hubOuter * 0.55f, cy - hubOuter * 0.55f)
                        cubicTo(
                            cx + r * 0.28f, cy - r * 0.78f,
                            cx + r * 0.72f, cy - r * 0.55f,
                            cx + r * 0.96f, cy - r * 0.10f,
                        )
                        cubicTo(
                            cx + r * 0.98f, cy + r * 0.08f,
                            cx + r * 0.70f, cy + r * 0.22f,
                            cx + hubOuter * 0.70f, cy + hubOuter * 0.35f,
                        )
                        cubicTo(
                            cx + hubOuter * 0.95f, cy + hubOuter * 0.05f,
                            cx + hubOuter * 0.85f, cy - hubOuter * 0.25f,
                            cx + hubOuter * 0.55f, cy - hubOuter * 0.55f,
                        )
                        close()
                    }
                    drawPath(blade, color = fill.copy(alpha = 0.88f))
                    drawPath(
                        blade,
                        color = strokeCol,
                        style = Stroke(width = (size.minDimension * 0.028f).coerceIn(2f, 3.5f), cap = StrokeCap.Round),
                    )
                }
            }
        }

        // Hub rings (mockup-like center)
        drawCircle(color = card, radius = hubOuter * 1.15f, center = Offset(cx, cy))
        drawCircle(
            color = strokeCol,
            radius = hubOuter,
            center = Offset(cx, cy),
            style = Stroke(width = (size.minDimension * 0.035f).coerceIn(2.5f, 4f)),
        )
        drawCircle(color = fill.copy(alpha = 0.35f), radius = hubOuter * 0.85f, center = Offset(cx, cy))
        drawCircle(color = if (active) Color.White.copy(alpha = 0.9f) else muted, radius = hubInner, center = Offset(cx, cy))
        drawCircle(color = fill, radius = hubInner * 0.45f, center = Offset(cx, cy))
    }
}

@Composable
private fun ClimateFanPanel(
    data: VanGoData,
    fan: ClimateFanTarget,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val speedActive = fan.kind == ClimateFanKind.Dimmer
    val devicePct = when {
        fan.demoOnly -> 0
        speedActive && fan.index < data.numOfD ->
            (data.getSafeDimsData(fan.index).toInt() and 0xFF).coerceIn(0, 100)
        else -> 0
    }
    val relayOn = !fan.demoOnly && !speedActive && fan.index < data.numOfO &&
        data.outDB.getOrElse(fan.index) { 0 }.toInt() != 0

    var localPct by remember(fan.index, fan.kind) {
        mutableStateOf(devicePct.coerceIn(0, 100).coerceAtLeast(if (fan.demoOnly) 60 else 0))
    }
    var lineOn by remember(fan.index, fan.kind) {
        mutableStateOf(if (fan.demoOnly) true else if (speedActive) devicePct > 0 else relayOn)
    }
    var stash by remember(fan.index) { mutableStateOf(if (devicePct > 0) devicePct else 60) }
    var onSinceMs by remember(fan.index) {
        mutableStateOf<Long?>(if (lineOn) Clock.System.now().toEpochMilliseconds() else null)
    }
    var activeLabel by remember { mutableStateOf(if (fan.demoOnly) "3h 25m" else "0h 00m") }

    LaunchedEffect(devicePct, speedActive, lineOn, fan.demoOnly) {
        if (fan.demoOnly) return@LaunchedEffect
        if (speedActive && lineOn) localPct = devicePct
    }
    LaunchedEffect(relayOn, speedActive, fan.demoOnly) {
        if (fan.demoOnly) return@LaunchedEffect
        if (!speedActive) lineOn = relayOn
    }
    LaunchedEffect(lineOn, onSinceMs, fan.demoOnly) {
        if (fan.demoOnly) {
            activeLabel = "3h 25m"
            return@LaunchedEffect
        }
        while (lineOn) {
            val start = onSinceMs ?: Clock.System.now().toEpochMilliseconds().also { onSinceMs = it }
            val mins = ((Clock.System.now().toEpochMilliseconds() - start) / 60_000L).coerceAtLeast(0)
            val h = mins / 60
            val m = mins % 60
            activeLabel = "${h}h ${m.toString().padStart(2, '0')}m"
            delay(15_000)
        }
    }

    fun writeDim(pct: Int) {
        if (fan.demoOnly) return
        scope.launch {
            runCatching { getAppContext().vanGO.writeDimmerPort(fan.index, pct.coerceIn(0, 100)) }
        }
    }

    fun writeRelay(on: Boolean) {
        if (fan.demoOnly) return
        if (vanGoGlobal.value.lockOperation(Operations.WriteDataDevice)) {
            scope.launch {
                try {
                    getAppContext().vanGO.writeDigitalPort(fan.index, on)
                } finally {
                    vanGoGlobal.value.unlockOperation()
                }
            }
        }
    }

    fun setSpeed(next: Int) {
        if (!speedActive || !lineOn) return
        val stepped = ((next.coerceIn(0, 100) + 5) / 10) * 10
        if (stepped == 0) {
            // Turn off: keep last non-zero speed for restore, UI shows 0% while passive
            stash = localPct.coerceAtLeast(10)
            localPct = 0
            lineOn = false
            onSinceMs = null
            writeDim(0)
        } else {
            localPct = stepped
            stash = stepped
            writeDim(stepped)
        }
    }

    fun togglePower() {
        if (speedActive) {
            if (lineOn) {
                stash = localPct.coerceAtLeast(10)
                localPct = 0
                lineOn = false
                onSinceMs = null
                writeDim(0)
            } else {
                val restore = stash.coerceIn(10, 100)
                lineOn = true
                localPct = restore
                onSinceMs = Clock.System.now().toEpochMilliseconds()
                writeDim(restore)
            }
        } else {
            val next = !lineOn
            lineOn = next
            onSinceMs = if (next) Clock.System.now().toEpochMilliseconds() else null
            writeRelay(next)
        }
    }

    val spinPct = when {
        !lineOn -> 0
        speedActive -> localPct.coerceIn(0, 100)
        else -> 70
    }
    // Passive (off) always shows 0%; stash is only used when turning back on
    val shownPct = if (speedActive) {
        if (lineOn) localPct else 0
    } else {
        if (lineOn) 100 else 0
    }
    val barsLit = if (speedActive) {
        if (lineOn) (shownPct / 10).coerceIn(0, 10) else 0
    } else {
        if (lineOn) 10 else 0
    }

    Row(
        modifier = modifier
            .background(CliCard, RoundedCornerShape(16.dp))
            .border(1.dp, CliBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        // LEFT: larger fan tile + tap hint under icon + labels
        Row(
            modifier = Modifier.width(270.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(148.dp)
                        .background(
                            brush = Brush.verticalGradient(
                                listOf(Color(0xFF152033), Color(0xFF0C121E)),
                            ),
                            shape = RoundedCornerShape(18.dp),
                        )
                        .border(
                            1.5.dp,
                            if (lineOn) CliCyan.copy(alpha = 0.55f) else CliBorder,
                            RoundedCornerShape(18.dp),
                        )
                        .clickable { togglePower() },
                    contentAlignment = Alignment.Center,
                ) {
                    LiveFanIcon(
                        speedPct = spinPct,
                        active = lineOn,
                        accent = CliCyan,
                        modifier = Modifier.size(128.dp),
                    )
                }
                Text(
                    stringResource(Res.string.climate_fan_tap_hint),
                    color = CliMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    lineHeight = 15.sp,
                    modifier = Modifier.width(148.dp),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    fan.name.uppercase(),
                    color = CliText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
                Text(
                    stringResource(Res.string.climate_fan_ceiling),
                    color = CliMuted,
                    fontSize = 13.sp,
                    maxLines = 1,
                )
            }
        }

        // CENTER: HIZ control
        Column(
            modifier = Modifier.weight(1.15f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                stringResource(Res.string.climate_speed).uppercase(),
                color = if (speedActive) CliMuted else CliMuted.copy(alpha = 0.4f),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.4.sp,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                ClimateRoundBtn(
                    icon = Icons.Filled.Remove,
                    enabled = speedActive && lineOn,
                    onClick = { setSpeed(localPct - 10) },
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.Bottom,
                        modifier = Modifier.height(78.dp),
                    ) {
                        repeat(10) { i ->
                            val on = i < barsLit
                            val h = (24 + i * 5.0f).dp
                            Box(
                                modifier = Modifier
                                    .width(18.dp)
                                    .height(h)
                                    .background(
                                        brush = when {
                                            !speedActive -> Brush.verticalGradient(
                                                listOf(CliTrack.copy(alpha = 0.4f), CliTrack.copy(alpha = 0.4f)),
                                            )
                                            on -> Brush.verticalGradient(
                                                listOf(CliCyan.copy(alpha = 0.45f), CliCyan, Color(0xFF7DE8FF)),
                                            )
                                            else -> Brush.verticalGradient(listOf(CliTrack, CliTrack))
                                        },
                                        shape = RoundedCornerShape(3.dp),
                                    ),
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "%$shownPct",
                        color = if (speedActive && lineOn) CliCyan else if (speedActive) CliText else CliMuted.copy(alpha = 0.45f),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                ClimateRoundBtn(
                    icon = Icons.Filled.Add,
                    enabled = speedActive && lineOn,
                    onClick = { setSpeed(localPct + 10) },
                )
            }
        }

        // RIGHT: status columns with dividers (mockup)
        Row(
            modifier = Modifier
                .width(360.dp)
                .fillMaxHeight()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ClimateStatusBlock(
                label = stringResource(Res.string.climate_status),
                value = if (lineOn) stringResource(Res.string.climate_status_on)
                else stringResource(Res.string.climate_status_off),
                dot = if (lineOn) CliGreen else CliMuted,
                modifier = Modifier.weight(1f),
            )
            Box(
                Modifier
                    .width(1.dp)
                    .fillMaxHeight(0.7f)
                    .background(CliBorder),
            )
            ClimateStatusBlock(
                label = stringResource(Res.string.climate_feedback),
                value = stringResource(Res.string.climate_feedback_normal),
                icon = Icons.Filled.Refresh,
                modifier = Modifier.weight(1f),
            )
            Box(
                Modifier
                    .width(1.dp)
                    .fillMaxHeight(0.7f)
                    .background(CliBorder),
            )
            ClimateStatusBlock(
                label = stringResource(Res.string.climate_active_time),
                value = if (lineOn) activeLabel else "—",
                icon = Icons.Filled.AccessTime,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ClimateRoundBtn(
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .background(if (enabled) CliTrack else CliTrack.copy(alpha = 0.4f), CircleShape)
            .border(1.5.dp, if (enabled) CliCyan.copy(alpha = 0.4f) else CliBorder, CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            null,
            tint = if (enabled) CliCyan else CliMuted.copy(alpha = 0.35f),
            modifier = Modifier.size(34.dp),
        )
    }
}

@Composable
private fun ClimateStatusBlock(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    dot: Color? = null,
    icon: ImageVector? = null,
) {
    Column(
        modifier = modifier.padding(horizontal = 10.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            label.uppercase(),
            color = CliMuted,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.8.sp,
            maxLines = 1,
        )
        Spacer(Modifier.height(10.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (dot != null) {
                Box(Modifier.size(12.dp).background(dot, CircleShape))
            }
            if (icon != null) {
                Icon(icon, null, tint = CliCyan, modifier = Modifier.size(22.dp))
            }
            Text(
                value,
                color = CliText,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

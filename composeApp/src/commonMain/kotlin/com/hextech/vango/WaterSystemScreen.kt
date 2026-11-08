package com.hextech.vango

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.WaterDrop
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import testvango.composeapp.generated.resources.Res
import testvango.composeapp.generated.resources.dashboard_nav_water
import testvango.composeapp.generated.resources.metric_no_data
import testvango.composeapp.generated.resources.water_info_title
import testvango.composeapp.generated.resources.water_cap_total
import testvango.composeapp.generated.resources.water_cap_clean
import testvango.composeapp.generated.resources.water_cap_gray
import testvango.composeapp.generated.resources.water_cap_dirty
import testvango.composeapp.generated.resources.water_pump_title
import testvango.composeapp.generated.resources.water_pump_on
import testvango.composeapp.generated.resources.water_pump_off
import testvango.composeapp.generated.resources.water_status_normal
import testvango.composeapp.generated.resources.water_status_low
import testvango.composeapp.generated.resources.water_status_high
import testvango.composeapp.generated.resources.water_tank_clean
import testvango.composeapp.generated.resources.water_tank_gray
import testvango.composeapp.generated.resources.water_tank_dirty
import kotlin.math.PI
import kotlin.math.sin

private val WaterBg: Color @Composable get() = LocalAppChrome.current.bg
private val WaterPanel: Color @Composable get() = LocalAppChrome.current.panel
private val WaterCard: Color @Composable get() = LocalAppChrome.current.card
private val WaterCyan = Color(0xFF2EC5FF)
private val WaterText: Color @Composable get() = LocalAppChrome.current.text
private val WaterMuted: Color @Composable get() = LocalAppChrome.current.muted
private val WaterTrack: Color @Composable get() = LocalAppChrome.current.track
private val WaterClean = Color(0xFF3B82F6)
private val WaterGray = Color(0xFF94A3B8)
private val WaterDirty = Color(0xFFD97706)
private val WaterGreen = Color(0xFF3DDC84)
private val WaterBorder: Color @Composable get() = LocalAppChrome.current.border

private const val WATER_PUMP_TYPE_ID = 10

@Composable
fun WaterSystemScreen(
    data: VanGoData,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(WaterBg)
            .padding(
                horizontal = if (isPhoneUi()) 0.dp else 12.dp,
                vertical = if (isPhoneUi()) 0.dp else 8.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (!isPhoneUi()) {
            WaterSystemPageHeader()
        }
        WaterLevelsContent(
            data = data,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        )
    }
}

@Composable
private fun WaterSystemPageHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(WaterPanel, RoundedCornerShape(14.dp))
            .border(1.dp, WaterBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 18.dp, vertical = if (isCompactLayout()) 10.dp else 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.WaterDrop,
            contentDescription = null,
            tint = WaterCyan,
            modifier = Modifier.size(if (isCompactLayout()) 28.dp else 36.dp),
        )
        Text(
            text = stringResource(Res.string.dashboard_nav_water).uppercase(),
            color = Color.White,
            fontSize = if (isCompactLayout()) 18.sp else 22.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.6.sp,
            maxLines = 1,
        )
    }
}

@Composable
private fun WaterLevelsContent(
    data: VanGoData,
    modifier: Modifier = Modifier,
) {
    val channels by panelPlatformChannels.collectAsState()
    LaunchedEffect(Unit) { ensurePanelChannelsLoadedFromCache() }
    val linked = isBleGattLinkReady() || isAppRemoteMqttConnected()
    val cleanOn = isWaterTankInstalled(0, channels)
    val grayOn = isWaterTankInstalled(1, channels)
    val dirtyOn = isWaterTankInstalled(2, channels)
    val cleanPct = if (cleanOn && linked) data.getSafeSensorsDataUnsigned(2).coerceIn(0, 100) else null
    val grayPct = if (grayOn && linked) data.getSafeSensorsDataUnsigned(3).coerceIn(0, 100) else null
    val dirtyPct = if (dirtyOn && linked) data.getSafeSensorsDataUnsigned(4).coerceIn(0, 100) else null
    val cleanCap = if (cleanOn) waterTankCapacityLiters(0, channels) else 0
    val grayCap = if (grayOn) waterTankCapacityLiters(1, channels) else 0
    val dirtyCap = if (dirtyOn) waterTankCapacityLiters(2, channels) else 0
    val cleanRemain = cleanPct?.let { waterRemainingLiters(it, cleanCap) } ?: 0
    val grayRemain = grayPct?.let { waterRemainingLiters(it, grayCap) } ?: 0
    val dirtyRemain = dirtyPct?.let { waterRemainingLiters(it, dirtyCap) } ?: 0
    val compact = isCompactLayout()
    val totalCap = cleanCap + grayCap + dirtyCap

    if (compact) {
        Column(
            modifier = modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Phone: stack tank cards vertically (same pattern as climate zone cards)
            if (cleanOn) {
                WaterTankCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    title = stringResource(Res.string.water_tank_clean),
                    pct = cleanPct,
                    remainingLiters = cleanRemain,
                    capacityLiters = cleanCap,
                    fill = WaterClean,
                    accent = WaterCyan,
                )
            }
            if (grayOn) {
                WaterTankCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    title = stringResource(Res.string.water_tank_gray),
                    pct = grayPct,
                    remainingLiters = grayRemain,
                    capacityLiters = grayCap,
                    fill = WaterGray,
                    accent = WaterGray,
                )
            }
            if (dirtyOn) {
                WaterTankCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    title = stringResource(Res.string.water_tank_dirty),
                    pct = dirtyPct,
                    remainingLiters = dirtyRemain,
                    capacityLiters = dirtyCap,
                    fill = WaterDirty,
                    accent = WaterDirty,
                )
            }
            WaterInfoPanel(
                modifier = Modifier.fillMaxWidth().height(160.dp),
                totalL = totalCap,
                cleanL = cleanCap,
                grayL = grayCap,
                dirtyL = dirtyCap,
                showGray = grayOn,
                showDirty = dirtyOn,
            )
            WaterPumpCard(
                modifier = Modifier.fillMaxWidth().height(110.dp),
                data = data,
            )
        }
        return
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier
                .weight(1.65f)
                .fillMaxHeight(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (cleanOn) {
                WaterTankCard(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    title = stringResource(Res.string.water_tank_clean),
                    pct = cleanPct,
                    remainingLiters = cleanRemain,
                    capacityLiters = cleanCap,
                    fill = WaterClean,
                    accent = WaterCyan,
                )
            }
            if (grayOn) {
                WaterTankCard(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    title = stringResource(Res.string.water_tank_gray),
                    pct = grayPct,
                    remainingLiters = grayRemain,
                    capacityLiters = grayCap,
                    fill = WaterGray,
                    accent = WaterGray,
                )
            }
            if (dirtyOn) {
                WaterTankCard(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    title = stringResource(Res.string.water_tank_dirty),
                    pct = dirtyPct,
                    remainingLiters = dirtyRemain,
                    capacityLiters = dirtyCap,
                    fill = WaterDirty,
                    accent = WaterDirty,
                )
            }
        }
        Column(
            modifier = Modifier
                .weight(0.68f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            WaterInfoPanel(
                modifier = Modifier.weight(1.25f).fillMaxWidth(),
                totalL = totalCap,
                cleanL = cleanCap,
                grayL = grayCap,
                dirtyL = dirtyCap,
                showGray = grayOn,
                showDirty = dirtyOn,
            )
            WaterPumpCard(
                modifier = Modifier.weight(0.65f).fillMaxWidth(),
                data = data,
            )
        }
    }
}

@Composable
private fun WaterTankCard(
    modifier: Modifier,
    title: String,
    pct: Int?,
    remainingLiters: Int,
    capacityLiters: Int,
    fill: Color,
    accent: Color,
) {
    val noData = stringResource(Res.string.metric_no_data)
    val status = waterStatusFor(title, pct ?: -1)
    val litersLabel = if (pct != null && capacityLiters > 0) {
        "$remainingLiters / $capacityLiters L"
    } else {
        null
    }
    BoxWithConstraints(
        modifier = modifier
            .background(WaterCard, RoundedCornerShape(18.dp))
            .border(1.dp, WaterBorder, RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        // Wide phone cards (stacked) → climate-style horizontal layout
        // Tall tablet cards (side-by-side) → keep vertical layout
        val stacked = maxWidth > maxHeight * 1.15f
        if (stacked) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        title.uppercase(),
                        color = WaterText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.6.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        Icons.Filled.WaterDrop,
                        contentDescription = null,
                        tint = accent.copy(alpha = 0.9f),
                        modifier = Modifier.size(28.dp),
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            pct?.let { "%$it" } ?: noData,
                            color = accent,
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )
                        if (litersLabel != null) {
                            Text(
                                litersLabel,
                                color = WaterMuted,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                            )
                        }
                        WaterStatusChip(status = status)
                    }
                    WaterTankGauge(
                        pct = pct ?: 0,
                        fill = fill,
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(0.48f),
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    title.uppercase(),
                    color = WaterText,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Icon(
                    Icons.Filled.WaterDrop,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(36.dp),
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    WaterTankGauge(
                        pct = pct ?: 0,
                        fill = fill,
                        modifier = Modifier
                            .fillMaxHeight(0.98f)
                            .fillMaxWidth(0.92f),
                    )
                }
                Text(
                    pct?.let { "%$it" } ?: noData,
                    color = accent,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                )
                if (litersLabel != null) {
                    Text(
                        litersLabel,
                        color = WaterMuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                    )
                }
                WaterStatusChip(status = status)
            }
        }
    }
}

private enum class WaterLevelStatus { Normal, Low, High }

private fun waterStatusFor(title: String, pct: Int): WaterLevelStatus {
    if (pct < 0) return WaterLevelStatus.Normal
    val dirty = title.contains("kirli", ignoreCase = true) || title.contains("dirty", ignoreCase = true)
    return when {
        dirty && pct >= 85 -> WaterLevelStatus.High
        !dirty && pct <= 15 -> WaterLevelStatus.Low
        else -> WaterLevelStatus.Normal
    }
}

@Composable
private fun WaterStatusChip(status: WaterLevelStatus) {
    val (label, color) = when (status) {
        WaterLevelStatus.Normal -> stringResource(Res.string.water_status_normal) to WaterGreen
        WaterLevelStatus.Low -> stringResource(Res.string.water_status_low) to WaterDirty
        WaterLevelStatus.High -> stringResource(Res.string.water_status_high) to WaterDirty
    }
    Row(
        modifier = Modifier
            .background(Color.Transparent, RoundedCornerShape(22.dp))
            .border(1.5.dp, WaterCyan.copy(alpha = 0.85f), RoundedCornerShape(22.dp))
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(Icons.Filled.CheckCircle, null, tint = color, modifier = Modifier.size(18.dp))
        Text(label, color = WaterText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun WaterTankGauge(
    pct: Int,
    fill: Color,
    modifier: Modifier = Modifier,
) {
    val target = (pct.coerceIn(0, 100) / 100f).coerceIn(0.04f, 1f)
    val animatedPct by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "water-level",
    )
    // Continuous time — never restarts, so wave loops stay seamless.
    var timeSec by remember { mutableStateOf(0f) }
    LaunchedEffect(Unit) {
        var lastNanos = 0L
        while (true) {
            withFrameNanos { now ->
                if (lastNanos != 0L) {
                    val dt = ((now - lastNanos).coerceIn(0L, 32_000_000L)) / 1_000_000_000f
                    timeSec += dt
                }
                lastNanos = now
            }
        }
    }
    val twoPi = (2f * PI).toFloat()
    val phase = timeSec * (twoPi / 2.4f)
    val phase2 = -timeSec * (twoPi / 3.2f)
    val bubbleT = timeSec / 2.8f
    val glass = LocalAppChrome.current.glass
    val muted = WaterMuted

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Canvas(modifier = Modifier.weight(1f).fillMaxHeight()) {
            val w = size.width
            val h = size.height
            val stroke = 3.2f
            val inset = stroke + 1.5f
            // Ana sayfa gibi: üst hafif yuvarlak, alt köşeler dik
            val topR = CornerRadius(w * 0.16f, w * 0.16f)
            val bottomR = CornerRadius.Zero
            val body = RoundRect(
                left = inset,
                top = inset,
                right = w - inset,
                bottom = h - inset,
                topLeftCornerRadius = topR,
                topRightCornerRadius = topR,
                bottomRightCornerRadius = bottomR,
                bottomLeftCornerRadius = bottomR,
            )
            val bodyPath = Path().apply { addRoundRect(body) }
            drawPath(bodyPath, color = glass)

            val innerTop = inset + 2f
            val innerBottom = h - inset - 2f
            val fillH = (innerBottom - innerTop) * animatedPct
            val waterTopY = innerBottom - fillH
            val waveAmp = (h * 0.034f).coerceIn(3.5f, 8f)

            clipPath(bodyPath) {
                // Secondary harmonic uses integer phase multiples so motion stays soft.
                fun waveY(x: Float, amp: Float, ph: Float, freq: Float): Float =
                    waterTopY +
                        sin((x / w) * freq * PI.toFloat() + ph).toFloat() * amp +
                        sin((x / w) * (freq + 2f) * PI.toFloat() - ph).toFloat() * (amp * 0.35f)

                fun buildWave(amp: Float, ph: Float, freq: Float): Path = Path().apply {
                    moveTo(0f, h)
                    lineTo(0f, waterTopY)
                    var x = 0f
                    while (x <= w + 3f) {
                        lineTo(x, waveY(x, amp, ph, freq))
                        x += 2.2f
                    }
                    lineTo(w + 3f, h)
                    close()
                }

                // Back wave (darker, offset)
                drawPath(
                    buildWave(waveAmp * 0.85f, phase2, 1.8f),
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            fill.copy(alpha = 0.28f),
                            fill.copy(alpha = 0.55f),
                            fill.copy(alpha = 0.72f),
                        ),
                        startY = waterTopY - waveAmp,
                        endY = innerBottom,
                    ),
                )
                // Front wave
                drawPath(
                    buildWave(waveAmp, phase, 2.2f),
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            fill.copy(alpha = 0.55f),
                            fill.copy(alpha = 0.88f),
                            fill,
                        ),
                        startY = waterTopY - waveAmp,
                        endY = innerBottom,
                    ),
                )
                // Surface highlight
                val highlight = Path().apply {
                    var x = inset
                    moveTo(x, waveY(x, waveAmp, phase, 2.2f))
                    while (x <= w - inset) {
                        lineTo(x, waveY(x, waveAmp, phase, 2.2f))
                        x += 2.2f
                    }
                }
                drawPath(
                    highlight,
                    color = Color.White.copy(alpha = 0.55f),
                    style = Stroke(width = 2.2f, cap = StrokeCap.Round),
                )
                // Rising bubbles (soft fade near surface via alpha)
                val bubbles = listOf(
                    Triple(0.28f, 0.22f, 2.6f),
                    Triple(0.48f, 0.50f, 3.4f),
                    Triple(0.68f, 0.32f, 2.3f),
                    Triple(0.40f, 0.74f, 1.9f),
                    Triple(0.58f, 0.62f, 2.1f),
                )
                bubbles.forEachIndexed { i, (xr, yr, r) ->
                    val rise = ((bubbleT + i * 0.19f) % 1f)
                    val fade = (kotlin.math.sin(rise * PI).toFloat()).coerceIn(0f, 1f)
                    val bx = w * xr + sin(phase + i * 1.3f).toFloat() * 2.5f
                    val by = innerBottom - fillH * (0.10f + yr * 0.72f) - rise * fillH * 0.55f
                    if (by in (waterTopY + 4f)..(innerBottom - 3f)) {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.28f * fade),
                            radius = r,
                            center = Offset(bx, by),
                        )
                    }
                }
                // Glass sheen
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.14f),
                            Color.Transparent,
                        ),
                    ),
                    topLeft = Offset(inset + 2f, inset + 2f),
                    size = Size(w * 0.24f, h - inset * 2f - 4f),
                )
            }

            drawPath(
                Path().apply {
                    addRoundRect(
                        RoundRect(
                            left = inset * 0.4f,
                            top = inset * 0.4f,
                            right = w - inset * 0.4f,
                            bottom = h - inset * 0.4f,
                            topLeftCornerRadius = CornerRadius(w * 0.18f, w * 0.18f),
                            topRightCornerRadius = CornerRadius(w * 0.18f, w * 0.18f),
                            bottomRightCornerRadius = CornerRadius.Zero,
                            bottomLeftCornerRadius = CornerRadius.Zero,
                        ),
                    )
                },
                color = fill.copy(alpha = 0.95f),
                style = Stroke(width = stroke),
            )
        }

        // Scale with tick marks aligned to tank height
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .width(42.dp)
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Canvas(
                modifier = Modifier
                    .width(8.dp)
                    .fillMaxHeight(),
            ) {
                val marks = listOf(0f, 0.25f, 0.50f, 0.75f, 1f)
                marks.forEach { frac ->
                    val y = size.height * (1f - frac)
                    drawLine(
                        color = muted.copy(alpha = 0.55f),
                        start = Offset(0f, y),
                        end = Offset(size.width * 0.85f, y),
                        strokeWidth = 1.5f,
                        cap = StrokeCap.Round,
                    )
                }
                val liveY = size.height * (1f - animatedPct)
                drawLine(
                    color = fill.copy(alpha = 0.95f),
                    start = Offset(0f, liveY),
                    end = Offset(size.width, liveY),
                    strokeWidth = 2.6f,
                    cap = StrokeCap.Round,
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(start = 2.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.Start,
            ) {
                listOf("100%", "75%", "50%", "25%", "0%").forEach { mark ->
                    Text(mark, color = WaterMuted, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun WaterInfoPanel(
    modifier: Modifier,
    totalL: Int,
    cleanL: Int,
    grayL: Int,
    dirtyL: Int,
    showGray: Boolean,
    showDirty: Boolean,
) {
    Column(
        modifier = modifier
            .background(WaterCard, RoundedCornerShape(18.dp))
            .border(1.dp, WaterBorder, RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.SpaceEvenly,
    ) {
        Text(
            stringResource(Res.string.water_info_title).uppercase(),
            color = WaterText,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        WaterInfoRow(Icons.Filled.WaterDrop, WaterCyan, stringResource(Res.string.water_cap_total), "$totalL L")
        WaterInfoRow(Icons.Filled.WaterDrop, WaterClean, stringResource(Res.string.water_cap_clean), "$cleanL L")
        if (showGray) {
            WaterInfoRow(Icons.Filled.WaterDrop, WaterGray, stringResource(Res.string.water_cap_gray), "$grayL L")
        }
        if (showDirty) {
            WaterInfoRow(Icons.Filled.WaterDrop, WaterDirty, stringResource(Res.string.water_cap_dirty), "$dirtyL L")
        }
    }
}

@Composable
private fun WaterInfoRow(icon: ImageVector, tint: Color, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(26.dp))
        Text(
            label,
            color = WaterMuted,
            fontSize = 15.sp,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(value, color = WaterText, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
    }
}

/**
 * Home-style pump toggle: same typeId 10 icon (`water.png`), clickable tile,
 * writes [writeDigitalPort] into shared [VanGoData.outDB] so home relay stays in sync.
 */
@Composable
private fun WaterPumpCard(
    modifier: Modifier,
    data: VanGoData,
) {
    val scope = rememberCoroutineScope()
    val pumpIndex = remember(data.outNB.contentHashCode(), data.numOfO) {
        data.outNB.indices.firstOrNull { i ->
            i < data.numOfO && (data.outNB.getOrElse(i) { 0 }.toInt() and 0xFF) == WATER_PUMP_TYPE_ID
        }
    }
    val pumpOn = pumpIndex != null && data.outDB.getOrElse(pumpIndex) { 0 }.toInt() != 0
    val enabled = pumpIndex != null
    val iconRes = remember {
        runCatching {
            getAppContext().vanGO.typeOfIcon(WATER_PUMP_TYPE_ID) as DrawableResource
        }.getOrNull()
    }
    val pumpName = runCatching {
        stringResource(getAppContext().vanGO.typeOfString(WATER_PUMP_TYPE_ID) as StringResource)
    }.getOrElse { stringResource(Res.string.water_pump_title) }

    Row(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.45f)
            .background(
                if (pumpOn) WaterGreen.copy(alpha = 0.28f) else WaterCard,
                RoundedCornerShape(18.dp),
            )
            .border(
                1.5.dp,
                if (pumpOn) WaterGreen.copy(alpha = 0.85f) else WaterBorder,
                RoundedCornerShape(18.dp),
            )
            .clickable(enabled = enabled) {
                val idx = pumpIndex ?: return@clickable
                if (vanGoGlobal.value.lockOperation(Operations.WriteDataDevice)) {
                    scope.launch {
                        try {
                            getAppContext().vanGO.writeDigitalPort(idx, !pumpOn)
                        } finally {
                            vanGoGlobal.value.unlockOperation()
                        }
                    }
                }
            }
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Icon left (same drawable as home Kontroller tile)
        if (iconRes != null) {
            Image(
                painter = painterResource(iconRes),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                colorFilter = ColorFilter.tint(
                    if (pumpOn) WaterCyan else Color(0xFFE8EEF7),
                ),
                modifier = Modifier
                    .size(56.dp)
                    .alpha(if (pumpOn) 1f else 0.82f),
            )
        } else {
            Icon(
                Icons.Filled.WaterDrop,
                null,
                tint = if (pumpOn) WaterCyan else Color(0xFFE8EEF7),
                modifier = Modifier.size(56.dp),
            )
        }
        // Text right
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                pumpName.uppercase(),
                color = if (pumpOn) WaterText else WaterMuted,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                if (pumpOn) stringResource(Res.string.water_pump_on)
                else stringResource(Res.string.water_pump_off),
                color = if (pumpOn) WaterCyan else WaterMuted,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
        }
    }
}

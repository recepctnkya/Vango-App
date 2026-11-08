package com.hextech.vango

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import com.hextech.vango.profile.CaravanType
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

enum class HeroLinkVisual {
    Connected,
    Connecting,
    Disconnected,
}

enum class HeroDayPeriod {
    Day,
    Evening,
    Night,
}

/** Active home-link medium; only one should be active at a time. */
enum class HeroConnectionKind {
    Ble,
    Remote,
}

/** Vehicle drawn in the hero slot (from profile [CaravanType] or explicit override). */
enum class HeroVehicleType {
    /** Çekme karavan — hitch, jockey wheel, single axle. */
    Towable,
    /** Motokaravan — cab, windshield, mirror, dual axle; no hitch. */
    Motorhome,
}

fun heroVehicleTypeFromCaravan(type: CaravanType): HeroVehicleType =
    when (type) {
        CaravanType.TowCaravan -> HeroVehicleType.Towable
        CaravanType.MotorCaravan -> HeroVehicleType.Motorhome
    }

/**
 * Home top card scene: procedural landscape + vehicle (no bitmaps).
 * Sky follows [sky] (solar time + outdoor weather) — not app light/dark theme.
 * Layout: phone left, vehicle right (~42% card width), status overlays as Compose text.
 */
@Composable
fun PhoneHomeHeroSceneCard(
    clockLabel: String,
    temperatureLabel: String?,
    weatherLabel: String?,
    statusLabel: String,
    linkVisual: HeroLinkVisual,
    sky: HeroSkySnapshot,
    isLightTheme: Boolean,
    onClick: () -> Unit,
    vehicleType: HeroVehicleType = HeroVehicleType.Towable,
    connectionKind: HeroConnectionKind = HeroConnectionKind.Ble,
    modifier: Modifier = Modifier,
    /** @deprecated Use [sky]; kept for older call sites. */
    dayPeriod: HeroDayPeriod = sky.period.toHeroDayPeriod(),
    weatherBadge: HeroWeatherBadgeUi? = null,
    onWeatherBadgeClick: (() -> Unit)? = null,
    /**
     * When set (e.g. scene simulation hour tick), overrides light/weather cross-fade ms.
     * Keep ≤ virtual hour step so eases don't stack.
     */
    sceneEaseOverrideMs: Int? = null,
    /** Longer ease for sim month/season jumps (3–5s). Null → derive from [sceneEaseOverrideMs]. */
    sceneSeasonEaseOverrideMs: Int? = null,
) {
    @Suppress("UNUSED_PARAMETER")
    val unusedKind = connectionKind
    @Suppress("UNUSED_PARAMETER")
    val unusedDayPeriod = dayPeriod

    val linkColor = when (linkVisual) {
        HeroLinkVisual.Connected -> Color(0xFF2ECC71)
        HeroLinkVisual.Connecting -> Color(0xFFF5A623)
        HeroLinkVisual.Disconnected -> Color(0xFF8B95A5)
    }
    val windowInfo = LocalWindowInfo.current
    val windowFocused = windowInfo.isWindowFocused
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val appResumed = lifecycleState.isAtLeast(Lifecycle.State.RESUMED)
    val prefersReducedMotion = rememberPrefersReducedMotion()
    var cardVisible by remember { mutableStateOf(true) }
    val animateScene = windowFocused && appResumed && cardVisible && !prefersReducedMotion

    val animateLink =
        linkVisual == HeroLinkVisual.Connected || linkVisual == HeroLinkVisual.Connecting
    // Single shared scene clock. Advances only while visible/resumed; freezes last
    // value on pause so resume never teleports from a large wall-clock jump.
    var sceneTSec by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(animateScene) {
        if (!animateScene) return@LaunchedEffect
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                if (last != 0L) {
                    val dt = (now - last).coerceIn(0L, 33_000_000L) / 1_000_000_000f
                    sceneTSec = (sceneTSec + dt) % HeroSceneSettings.CloudDriftSec
                }
                last = now
            }
        }
    }
    val cloudT = sceneTSec
    val fxT = cloudT
    val waterT = ((cloudT / (HeroLakeMasterMs / 1000f)) % 1f)
    // Link motion derived from the same clock (clearer than ambient nature)
    val flow = if (linkVisual == HeroLinkVisual.Connected) {
        ((cloudT / 2.35f) % 1f)
    } else {
        0f
    }
    val pulse = if (animateLink) {
        0.90f + 0.10f * (0.5f + 0.5f * sin((cloudT * (PI / 1.45)).toFloat()))
    } else {
        1f
    }

    // Target = [sky]; rendered floats ease toward it (retarget mid-flight, no rewind).
    // Live: light follows continuous clock (short settle). Sim: hour vs season durations.
    val simDriven = sceneEaseOverrideMs != null
    val easePos = when {
        prefersReducedMotion -> 0
        simDriven -> sceneEaseOverrideMs!!
        else -> HeroSceneTransitionConfig.LiveCelestialEaseMs
    }
    val easeWx = when {
        prefersReducedMotion -> 0
        simDriven -> sceneEaseOverrideMs!!
        else -> HeroSceneTransitionConfig.WeatherEaseMs
    }
    val easeSeason = when {
        prefersReducedMotion -> 0
        sceneSeasonEaseOverrideMs != null -> sceneSeasonEaseOverrideMs
        simDriven -> HeroSceneTransitionConfig.SeasonEaseMs
        else -> HeroSceneTransitionConfig.LiveSeasonEaseMs
    }
    // Celestial: wrap-aware (23:00→00:00 snaps, no full-sky tour)
    val sunX by animateHeroFloatAsState(sky.sun?.xFrac ?: 0.5f, easePos, "sunX")
    val sunY by animateHeroFloatAsState(sky.sun?.yFrac ?: 0.7f, easePos, "sunY", wrapThreshold = 0.70f)
    val moonX by animateHeroFloatAsState(sky.moon?.pos?.xFrac ?: 0.6f, easePos, "moonX")
    val moonY by animateHeroFloatAsState(sky.moon?.pos?.yFrac ?: 0.7f, easePos, "moonY", wrapThreshold = 0.70f)
    val starVis by animateHeroScalarAsState(sky.starVisibility, easeWx, "starVis")
    val bodyX by animateHeroFloatAsState(sky.bodyXFrac, easePos, "bodyX")
    val reflectStr by animateHeroScalarAsState(sky.reflectionStrength, easeWx, "refl")
    val diskA by animateHeroScalarAsState(sky.celestialDiskAlpha, easeWx, "diskA")
    val glowA by animateHeroScalarAsState(sky.celestialGlowAlpha, easeWx, "glowA")
    val lightMul by animateHeroScalarAsState(sky.lightStrengthMul, easeWx, "lightMul")
    val shadowSoft by animateHeroScalarAsState(sky.shadowSoftness, easeWx, "shSoft")
    val farHaze by animateHeroScalarAsState(sky.farHaze, easeWx, "farHaze")
    val cloudDens by animateHeroScalarAsState(sky.cloudDensity, easeWx, "cloudDens")
    val partlyCloudA by animateHeroScalarAsState(
        if (sky.weather == HeroSkyWeather.PartlyCloudy) 1f else 0f,
        easeWx,
        "partlyCloudA",
    )
    val overcastCloudA by animateHeroScalarAsState(
        when (sky.weather) {
            HeroSkyWeather.Overcast, HeroSkyWeather.Rain, HeroSkyWeather.Snow -> 1f
            else -> 0f
        },
        easeWx,
        "overcastCloudA",
    )
    val rainFxA by animateHeroScalarAsState(
        if (sky.weather == HeroSkyWeather.Rain) 1f else 0f,
        easeWx,
        "rainFxA",
    )
    val snowFxA by animateHeroScalarAsState(
        sky.snowfallIntensity.coerceIn(0f, 1f),
        easeWx,
        "snowFxA",
    )
    val iceCoverA by animateHeroScalarAsState(sky.iceCoverage, easeSeason, "iceCover")
    val fogFxA by animateHeroScalarAsState(
        if (sky.weather == HeroSkyWeather.Fog) 1f else 0f,
        easeWx,
        "fogFxA",
    )
    // Shared light axes — night↔dawn↔day↔dusk blend together
    val nightAmt by animateHeroScalarAsState(
        sceneNightAmount(sky.period),
        easeWx,
        "nightAmt",
    )
    val warmthAmt by animateHeroScalarAsState(
        sceneLightWarmth(sky.period, sky.weather, sky.cloudDensity),
        easeWx,
        "warmthAmt",
    )
    val windowsLitA by animateHeroScalarAsState(
        if (sky.nightWindowsLit) 1f else 0f,
        easeWx,
        "windowsLit",
    )
    val windDirA by animateHeroScalarAsState(sky.wind.dirX, easeWx, "windDir")
    val windSpdA by animateHeroScalarAsState(sky.wind.speedNorm, easeWx, "windSpd")
    val seasonProgA by animateHeroScalarAsState(sky.seasonProgress, easeSeason, "seasonProg")
    val foliageA by animateHeroScalarAsState(sky.foliageAmount, easeSeason, "foliage")
    // Ground pack builds while flakes fall (slow rise); holds / melts toward target when clear
    val snowCoverTarget = sky.snowCover
    val snowingNow = sky.snowfallIntensity > 0.05f
    val snowCoverEaseMs = when {
        prefersReducedMotion -> 0
        snowingNow -> (easeSeason.coerceAtLeast(2_500) * 2).coerceAtMost(12_000) // visible build-up
        else -> easeSeason.coerceAtLeast(4_000) // slower melt / hold
    }
    val snowCoverA by animateHeroScalarAsState(snowCoverTarget, snowCoverEaseMs, "snowCover")
    val mountainSnowA by animateHeroScalarAsState(sky.mountainSnowCover, easeSeason, "mtnSnow")

    // Extra accumulation while actively snowing: nudge cover upward as flakes land
    var landedAccum by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(animateScene, snowingNow) {
        if (!animateScene) return@LaunchedEffect
        while (true) {
            if (snowingNow) {
                val intens = sky.snowfallIntensity
                val ceiling = (sky.snowCover + intens * 0.15f).coerceIn(0.18f, 0.75f)
                landedAccum = (landedAccum + intens * 0.022f).coerceAtMost(ceiling)
            } else if (landedAccum > sky.snowCover + 0.01f) {
                // After flakes stop, hold then slowly settle to weather residual
                landedAccum = lerpFloat(landedAccum, sky.snowCover, 0.035f)
            }
            kotlinx.coroutines.delay(180L)
        }
    }
    val snowCoverDrawn = maxOf(snowCoverA, landedAccum).coerceIn(0f, 0.85f)

    val palette = rememberHeroScenePalette(sky, easeWx)
    val overlayScrim = if (isLightTheme) Color.Black.copy(alpha = 0.30f) else Color.Black.copy(alpha = 0.40f)
    val overlayText = Color.White
    val overlayMuted = Color.White.copy(alpha = 0.72f)
    // Weather badge: semi-transparent dark navy (Compose overlay → above trees/clouds)
    val weatherBadgeBg = Color(0xFF0B1C33).copy(alpha = 0.72f)
    val badge = weatherBadge ?: HeroWeatherBadgeUi(
        primary = sky.weatherLabel?.takeIf { it.isNotBlank() }
            ?: weatherLabel?.takeIf { it.isNotBlank() }
            ?: "Hava bilgisi yok",
        windy = sky.hasWeatherData && sky.windy,
        icon = sky.weatherBadgeIcon,
    )
    val weatherPrimary = badge.primary
    val weatherWindy = badge.windy && sky.hasWeatherData
    val weatherIcon = heroWeatherBadgeImageVector(badge.icon)
    val weatherClickable = onWeatherBadgeClick != null && badge.action != HeroWeatherBadgeAction.None
    val lakeClipPath = remember { Path() }
    val skyClipPath = remember { Path() }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(168.dp)
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, LocalAppChrome.current.border, RoundedCornerShape(14.dp))
            .onGloballyPositioned { coords ->
                val b = coords.boundsInWindow()
                val root = coords.findRootCoordinates().size
                val view = Rect(0f, 0f, root.width.toFloat(), root.height.toFloat())
                cardVisible = b.width > 1f && b.height > 1f && b.overlaps(view)
            }
            .clickable(onClick = onClick),
    ) {
        val drawSky = sky.copy(
            starVisibility = starVis,
            reflectionStrength = reflectStr,
            celestialDiskAlpha = diskA,
            celestialGlowAlpha = glowA,
            lightStrengthMul = lightMul,
            shadowSoftness = shadowSoft,
            farHaze = farHaze,
            cloudDensity = cloudDens,
            seasonProgress = seasonProgA,
            wind = sky.wind.copy(dirX = windDirA, speedNorm = windSpdA),
            foliageAmount = foliageA,
            snowCover = snowCoverDrawn,
            mountainSnowCover = mountainSnowA,
            iceCoverage = iceCoverA,
            snowfallIntensity = snowFxA,
            windy = windSpdA > 0.48f || sky.windy,
            // Soft threshold — glow fades via nightAmt; bool only for discrete window fill
            nightWindowsLit = windowsLitA > 0.42f,
        )
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawHeroScene(
                palette = palette,
                sky = drawSky,
                vehicleType = vehicleType,
                linkColor = linkColor,
                linkVisual = linkVisual,
                flowT = flow,
                pulse = pulse,
                waterT = waterT,
                fxT = fxT,
                cloudT = cloudT,
                animateMotion = animateScene,
                lakeClipPath = lakeClipPath,
                skyClipPath = skyClipPath,
                sunX = sunX,
                sunY = sunY,
                moonX = moonX,
                moonY = moonY,
                starVis = starVis,
                bodyXFrac = bodyX,
                reflectionStrength = reflectStr,
                diskAlpha = diskA,
                glowAlpha = glowA,
                lightMul = lightMul,
                shadowSoft = shadowSoft,
                farHaze = farHaze,
                nightAmount = nightAmt,
                warmth = warmthAmt,
                partlyCloudA = partlyCloudA,
                overcastCloudA = overcastCloudA,
                rainFxA = rainFxA,
                snowFxA = snowFxA,
                fogFxA = fogFxA,
            )
        }

        // Clock + temperature (top-left); keep clear of sky / mountains
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 12.dp, top = 10.dp)
                .background(overlayScrim, RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Text(
                text = clockLabel,
                color = overlayText,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            if (!temperatureLabel.isNullOrBlank()) {
                Text(
                    text = "  ·  ",
                    color = overlayMuted,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = temperatureLabel,
                    color = overlayText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
            }
        }

        // Weather badge — same top inset as clock (12/10); above Canvas trees/clouds
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 12.dp, top = 10.dp)
                .widthIn(max = maxWidth * 0.46f)
                .background(weatherBadgeBg, RoundedCornerShape(8.dp))
                .then(
                    if (weatherClickable) {
                        Modifier.clickable { onWeatherBadgeClick?.invoke() }
                    } else {
                        Modifier
                    },
                )
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            // No wind/air icon when there is no weather sample
            if (badge.icon != HeroWeatherBadgeIcon.Unknown || sky.hasWeatherData) {
                Icon(
                    imageVector = weatherIcon,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.92f),
                    modifier = Modifier
                        .padding(end = 5.dp)
                        .size(14.dp),
                )
            }
            Column {
                Text(
                    text = weatherPrimary,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (weatherWindy) {
                    Text(
                        text = "Rüzgarlı",
                        color = Color.White.copy(alpha = 0.78f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        // Status sits above the link, in the gap between phone and hitch
        val statusStart = maxWidth * 0.18f
        val statusWidth = (maxWidth * 0.48f - statusStart).coerceAtLeast(48.dp)
        Text(
            text = statusLabel,
            color = linkColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = statusStart, bottom = maxHeight * 0.24f)
                .width(statusWidth),
        )
    }
}

/**
 * Main scene orchestrator — layout, shared light, draw order, clips, layer flags.
 * Each landscape module owns its own drawing; this file does not paint trees/water/etc.
 */
private fun DrawScope.drawHeroScene(
    palette: HeroScenePalette,
    sky: HeroSkySnapshot,
    vehicleType: HeroVehicleType,
    linkColor: Color,
    linkVisual: HeroLinkVisual,
    flowT: Float,
    pulse: Float,
    waterT: Float,
    fxT: Float,
    cloudT: Float,
    animateMotion: Boolean,
    lakeClipPath: Path,
    skyClipPath: Path,
    sunX: Float,
    sunY: Float,
    moonX: Float,
    moonY: Float,
    starVis: Float,
    bodyXFrac: Float,
    reflectionStrength: Float,
    diskAlpha: Float,
    glowAlpha: Float,
    lightMul: Float,
    shadowSoft: Float,
    farHaze: Float,
    nightAmount: Float,
    warmth: Float,
    partlyCloudA: Float,
    overcastCloudA: Float,
    rainFxA: Float,
    snowFxA: Float,
    fogFxA: Float,
) {
    val w = size.width
    val h = size.height
    val layout = computeHeroLayout(w, h, vehicleType)
    val light = heroSceneLight(
        sky = sky,
        sunX = sunX,
        sunY = sunY,
        moonX = moonX,
        moonY = moonY,
        lightMul = lightMul,
        diskAlpha = diskAlpha,
        shadowSoft = shadowSoft,
        farHaze = farHaze,
        nightAmount = nightAmount,
        warmth = warmth,
    )
    val layers = HeroSceneDebugLayers.flags
    val state = HeroSceneDrawState(
        w = w,
        h = h,
        palette = palette,
        sky = sky,
        light = light,
        layout = layout,
        vehicleType = vehicleType,
        waterT = waterT,
        cloudT = cloudT,
        fxT = fxT,
        animateMotion = animateMotion,
        sunX = sunX,
        sunY = sunY,
        moonX = moonX,
        moonY = moonY,
        starVis = starVis,
        bodyXFrac = bodyXFrac,
        reflectionStrength = reflectionStrength,
        diskAlpha = diskAlpha,
        glowAlpha = glowAlpha,
        partlyCloudA = partlyCloudA,
        overcastCloudA = overcastCloudA,
        rainFxA = rainFxA,
        snowFxA = snowFxA,
        fogFxA = fogFxA,
        linkColor = linkColor,
        linkVisual = linkVisual,
        flowT = flowT,
        pulse = pulse,
        lakeClipPath = lakeClipPath,
        skyClipPath = skyClipPath,
        layers = layers,
        season = sky.season,
        seasonProgress = sky.seasonProgress,
        wind = sky.wind,
        foliageAmount = sky.foliageAmount,
        snowCover = sky.snowCover,
        mountainSnowCover = sky.mountainSnowCover,
        iceCoverage = sky.iceCoverage,
        nightAmount = nightAmount,
    )
    drawHeroSceneLayers(state)
}

/** Fixed draw order — modules share [HeroSceneDrawState]. */
private fun DrawScope.drawHeroSceneLayers(state: HeroSceneDrawState) {
    val layers = state.layers
    val palette = state.palette
    val light = state.light
    val w = state.w
    val h = state.h

    if (layers.sky) {
        drawHeroSkyGradient(palette, light)
    }

    if (layers.celestial) {
        drawHeroCelestial(
            palette = palette,
            sky = state.sky,
            w = w,
            h = h,
            sunX = state.sunX,
            sunY = state.sunY,
            moonX = state.moonX,
            moonY = state.moonY,
            starVis = state.starVis,
            fxT = state.fxT,
            animateMotion = state.animateMotion,
            diskAlpha = state.diskAlpha,
            glowAlpha = state.glowAlpha,
        )
    }

    if (layers.clouds) {
        state.skyClipPath.rewind()
        state.skyClipPath.addRect(Rect(0f, 0f, w, h * HeroSceneSettings.SkyClipHeightFrac))
        clipPath(state.skyClipPath) {
            drawHeroSkyWeatherFx(
                weather = state.sky.weather,
                windy = state.sky.windy,
                cloudDensity = state.sky.cloudDensity,
                // Always pass frozen clock — pause keeps last pose (no snap to 0)
                timeSec = state.cloudT,
                skyDim = palette.mountainFar,
                partlyAlpha = state.partlyCloudA,
                overcastAlpha = state.overcastCloudA,
                rainAlpha = state.rainFxA,
                snowAlpha = 0f, // full-card flakes drawn after park (must reach ground)
                fogAlpha = state.fogFxA,
                cloudTint = palette.cloud,
                wind = state.wind,
                nightAmount = state.nightAmount,
            )
        }
    }

    if (layers.mountains) {
        drawHeroMountains(
            w = w,
            h = h,
            palette = palette,
            light = light,
            farHaze = light.haze,
            shadowSoft = light.soft,
            season = state.season,
            mountainSnowCover = state.mountainSnowCover,
        )
    }
    // Distance fog after mountains (far strong → near weak)
    if (state.fogFxA > 0.02f || light.haze > 0.08f) {
        drawHeroDistanceFog(w, h, state.fogFxA, light.haze, state.cloudT)
    }

    // Pose uses continuous clock; animate flags stay true so pause freezes mid-pose
    val poseLive = state.cloudT > 0.001f || state.animateMotion
    if (layers.lake) {
        drawHeroLake(
            w = w,
            h = h,
            palette = palette,
            sky = state.sky,
            waterT = state.waterT,
            sceneTSec = state.cloudT,
            animateWater = poseLive,
            lakeClipPath = state.lakeClipPath,
            bodyXFrac = state.bodyXFrac,
            reflectionStrength = state.reflectionStrength,
            light = light,
            drawWaterLife = layers.waterLife,
            iceCoverage = state.iceCoverage,
        )
        drawHeroRainLakeRings(
            w = w,
            h = h,
            timeSec = state.cloudT,
            rainAlpha = state.rainFxA,
            lakeClipPath = state.lakeClipPath,
            nightAmount = state.nightAmount,
        )
    }
    if (layers.wetShore && state.iceCoverage < 0.06f && state.snowCover < 0.20f) {
        drawHeroWetShoreAndFoam(
            w = w,
            h = h,
            light = light,
            sceneTSec = state.cloudT,
            animate = poseLive,
        )
    }
    if (layers.farShore) {
        drawHeroFarShore(w, h, palette)
    }
    if (layers.treesFar) {
        drawHeroTreeClusters(
            w, h, palette, light, farOnly = true,
            season = state.season,
            seasonProgress = state.seasonProgress,
            foliageAmount = state.foliageAmount,
            wind = state.wind,
            timeSec = state.cloudT,
            snowCover = state.snowCover,
        )
    }
    if (layers.treesNear) {
        drawHeroTreeClusters(
            w, h, palette, light, farOnly = false,
            season = state.season,
            seasonProgress = state.seasonProgress,
            foliageAmount = state.foliageAmount,
            wind = state.wind,
            timeSec = state.cloudT,
            snowCover = state.snowCover,
        )
    }
    if (layers.park) {
        drawHeroParkShore(
            w = w,
            h = h,
            palette = palette,
            waterT = state.waterT,
            animateMotion = poseLive,
            light = light,
            season = state.season,
            seasonProgress = state.seasonProgress,
            wind = state.wind,
            timeSec = state.cloudT,
            snowCover = state.snowCover,
            foliageAmount = state.foliageAmount,
        )
        drawHeroRainWetGround(w, h, state.rainFxA, state.nightAmount)
    }

    // Falling snow after park so flakes land on the pack and settle (not clipped by sky)
    if (state.snowFxA > 0.02f || state.snowCover > 0.06f) {
        drawHeroSnow(
            timeSec = state.cloudT,
            wind = state.wind,
            alpha = if (state.animateMotion) state.snowFxA else 0f,
            snowCover = state.snowCover,
        )
    }

    // Autumn leaves under UI chrome
    drawHeroAutumnLeaves(
        w = w,
        h = h,
        timeSec = state.cloudT,
        wind = state.wind,
        foliageAmount = state.foliageAmount,
        season = state.season,
        animate = poseLive,
        nightAmount = state.nightAmount,
    )

    if (layers.vehicle) {
        val nightLit = state.sky.nightWindowsLit
        when (state.vehicleType) {
            HeroVehicleType.Towable -> drawTowableCaravan(
                origin = Offset(state.layout.vehicleOriginX, state.layout.vehicleOriginY),
                scale = state.layout.vehicleScale,
                nightWindowsLit = nightLit,
                shadow = palette.shadow,
                light = light,
                skyTint = palette.skyBottom,
                snowCover = state.snowCover,
            )
            HeroVehicleType.Motorhome -> drawMotorhome(
                origin = Offset(state.layout.vehicleOriginX, state.layout.vehicleOriginY),
                scale = state.layout.vehicleScale,
                nightWindowsLit = nightLit,
                shadow = palette.shadow,
                light = light,
                skyTint = palette.skyBottom,
                snowCover = state.snowCover,
            )
        }
    }

    if (layers.phoneLink) {
        val linkFrom = Offset(
            state.layout.phoneCenter.x + state.layout.phoneSize * 0.36f,
            state.layout.phoneCenter.y,
        )
        drawPhoneIcon(state.layout.phoneCenter, state.layout.phoneSize)
        drawConnectionLine(
            from = linkFrom,
            to = state.layout.linkTarget,
            color = state.linkColor,
            visual = state.linkVisual,
            flowT = state.flowT,
            pulse = state.pulse,
        )
    }
}

/** Sky wash only — celestial / clouds are separate modules. */
private fun DrawScope.drawHeroSkyGradient(palette: HeroScenePalette, light: HeroSceneLight) {
    val horizon = if (light.warmth > 0.05f) {
        lerpColor(palette.skyBottom, HeroWarmLit, light.warmth * 0.35f * (1f - light.soft))
    } else {
        light.litColor(palette.skyBottom, 0.35f)
    }
    val zenith = light.shadeColor(palette.skyTop, 0.25f)
    drawRect(
        brush = Brush.verticalGradient(
            colorStops = arrayOf(
                0.00f to zenith,
                0.40f to lerpColor(zenith, horizon, 0.35f),
                0.72f to lerpColor(zenith, horizon, 0.78f),
                1.00f to horizon,
            ),
        ),
        size = size,
    )
}
fun capitalizeWeatherLabel(raw: String?): String? {
    val t = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    return t.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}

private fun heroWeatherBadgeImageVector(kind: HeroWeatherBadgeIcon): ImageVector = when (kind) {
    HeroWeatherBadgeIcon.Sun -> Icons.Filled.WbSunny
    HeroWeatherBadgeIcon.Moon -> Icons.Filled.NightsStay
    HeroWeatherBadgeIcon.PartlyCloudy -> Icons.Filled.WbCloudy
    HeroWeatherBadgeIcon.Cloudy -> Icons.Filled.Cloud
    HeroWeatherBadgeIcon.Rain, HeroWeatherBadgeIcon.Shower -> Icons.Filled.WaterDrop
    HeroWeatherBadgeIcon.Snow -> Icons.Filled.AcUnit
    HeroWeatherBadgeIcon.Fog -> Icons.Filled.Cloud
    HeroWeatherBadgeIcon.Storm -> Icons.Filled.Bolt
    HeroWeatherBadgeIcon.Loading -> Icons.Filled.Cloud
    HeroWeatherBadgeIcon.NeedsLocation -> Icons.Filled.Air
    HeroWeatherBadgeIcon.Unknown -> Icons.Filled.Cloud
}


package com.hextech.vango

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

internal data class HeroScenePalette(
    val skyTop: Color,
    val skyBottom: Color,
    val mountainFar: Color,
    val mountainMid: Color,
    val mountainNear: Color,
    val lake: Color,
    val shore: Color,
    val shoreDeep: Color,
    val tree: Color,
    val sun: Color,
    val moon: Color,
    val cloud: Color,
    val star: Color,
    val caravanBody: Color,
    val caravanStripe: Color,
    val caravanDoor: Color,
    val caravanWindow: Color,
    val caravanWindowLit: Color,
    val hitch: Color,
    val wheel: Color,
    val wheelRim: Color,
    val shadow: Color,
)

@Composable
internal fun rememberHeroScenePalette(sky: HeroSkySnapshot, easeMs: Int): HeroScenePalette {
    val target = remember(sky.period, sky.weather, sky.cloudDensity, sky.hasWeatherData) {
        heroScenePalette(sky.period, sky.weather, sky.cloudDensity)
    }
    val spec = tween<Color>(easeMs.coerceAtLeast(0), easing = HeroSceneSmoothEase)
    val skyTop by animateColorAsState(target.skyTop, spec, label = "skyTop")
    val skyBottom by animateColorAsState(target.skyBottom, spec, label = "skyBot")
    val mountainFar by animateColorAsState(target.mountainFar, spec, label = "mtFar")
    val mountainMid by animateColorAsState(target.mountainMid, spec, label = "mtMid")
    val mountainNear by animateColorAsState(target.mountainNear, spec, label = "mtNear")
    val lake by animateColorAsState(target.lake, spec, label = "lake")
    val shore by animateColorAsState(target.shore, spec, label = "shore")
    val shoreDeep by animateColorAsState(target.shoreDeep, spec, label = "shoreD")
    val tree by animateColorAsState(target.tree, spec, label = "tree")
    val sun by animateColorAsState(target.sun, spec, label = "sunCol")
    val moon by animateColorAsState(target.moon, spec, label = "moonCol")
    val cloud by animateColorAsState(target.cloud, spec, label = "cloudCol")
    val star by animateColorAsState(target.star, spec, label = "starCol")
    val shadow by animateColorAsState(target.shadow, spec, label = "shCol")
    return target.copy(
        skyTop = skyTop,
        skyBottom = skyBottom,
        mountainFar = mountainFar,
        mountainMid = mountainMid,
        mountainNear = mountainNear,
        lake = lake,
        shore = shore,
        shoreDeep = shoreDeep,
        tree = tree,
        sun = sun,
        moon = moon,
        cloud = cloud,
        star = star,
        shadow = shadow,
    )
}

/**
 * Landscape colors follow solar period + outdoor weather — not app light/dark theme.
 * Vertical sky: darker top → lighter near horizon. Cloud cover desaturates & cools.
 */
internal fun heroScenePalette(
    period: HeroSkyPeriod,
    weather: HeroSkyWeather,
    cloudDensity: Float,
): HeroScenePalette {
    val base = heroScenePalettePeriod(period)
    return tintPaletteForWeather(base, period, weather, cloudDensity)
}

internal fun heroScenePalettePeriod(period: HeroSkyPeriod): HeroScenePalette {
    return when (period) {
        HeroSkyPeriod.Dawn -> HeroScenePalette(
            // Warmth stays near horizon; mid-ground stays cooler so scene isn't orange-washed
            skyTop = Color(0xFF4A5A8A),
            skyBottom = Color(0xFFE8A878),
            mountainFar = Color(0xFF8A90B0),
            mountainMid = Color(0xFF5A6888),
            mountainNear = Color(0xFF3A4868),
            lake = Color(0xFF527F9F),
            shore = Color(0xFFB0A898),
            shoreDeep = Color(0xFF9A9284),
            tree = Color(0xFF1A2A28),
            sun = Color(0xFFFFE0B0),
            moon = Color(0xFFFFF3D0),
            cloud = Color(0xFFFFE0C0).copy(alpha = 0.35f),
            star = Color.White.copy(alpha = 0.25f),
            caravanBody = Color(0xFFEAF4FB),
            caravanStripe = Color(0xFF8EC8E8),
            caravanDoor = Color(0xFFB8CDDC),
            caravanWindow = Color(0xFF2A3A4A),
            caravanWindowLit = Color(0xFFFFE0A3),
            hitch = Color(0xFF9AA8B5),
            wheel = Color(0xFF1C222A),
            wheelRim = Color(0xFFC5CCD4),
            shadow = Color.Black.copy(alpha = 0.11f),
        )
        HeroSkyPeriod.Day -> HeroScenePalette(
            // Soft saturated blue → lighter bluish near horizon
            skyTop = Color(0xFF2E6FA8),
            skyBottom = Color(0xFF7BB8DC),
            mountainFar = Color(0xFF5A84A8),
            mountainMid = Color(0xFF3D6588),
            mountainNear = Color(0xFF254860),
            lake = Color(0xFF527F9F),
            shore = Color(0xFFB9AE98),
            shoreDeep = Color(0xFFA79B85),
            tree = Color(0xFF163028),
            sun = Color(0xFFFFF0C8),
            moon = Color(0xFFFFF3D0),
            cloud = Color.White.copy(alpha = 0.50f),
            star = Color.Transparent,
            caravanBody = Color(0xFFEAF4FB),
            caravanStripe = Color(0xFF8EC8E8),
            caravanDoor = Color(0xFFB8CDDC),
            caravanWindow = Color(0xFF2A3A4A),
            caravanWindowLit = Color(0xFFFFE0A3),
            hitch = Color(0xFF9AA8B5),
            wheel = Color(0xFF1C222A),
            wheelRim = Color(0xFFC5CCD4),
            shadow = Color.Black.copy(alpha = 0.10f),
        )
        HeroSkyPeriod.Dusk -> HeroScenePalette(
            // Horizon warm; mountains/shore stay cooler blues so only lit edges pick up warmth
            skyTop = Color(0xFF2A1F4A),
            skyBottom = Color(0xFFC4744A),
            mountainFar = Color(0xFF4A4A68),
            mountainMid = Color(0xFF343850),
            mountainNear = Color(0xFF1E2438),
            lake = Color(0xFF1D3C58),
            shore = Color(0xFF6A6860),
            shoreDeep = Color(0xFF524E48),
            tree = Color(0xFF12141C),
            sun = Color(0xFFFFCC80),
            moon = Color(0xFFFFF3D0),
            cloud = Color(0xFFFFCCBC).copy(alpha = 0.32f),
            star = Color.White.copy(alpha = 0.35f),
            caravanBody = Color(0xFFD8E4EE),
            caravanStripe = Color(0xFF90CAF9),
            caravanDoor = Color(0xFFA8B8C8),
            caravanWindow = Color(0xFF2A3A4A),
            caravanWindowLit = Color(0xFFFFCC80),
            hitch = Color(0xFF9AA8B5),
            wheel = Color(0xFF1C222A),
            wheelRim = Color(0xFFC5CCD4),
            shadow = Color.Black.copy(alpha = 0.12f),
        )
        HeroSkyPeriod.Night -> HeroScenePalette(
            skyTop = Color(0xFF0C2240),
            skyBottom = Color(0xFF315D83),
            mountainFar = Color(0xFF4A7698),
            mountainMid = Color(0xFF2A5270),
            mountainNear = Color(0xFF102838),
            lake = Color(0xFF1D3C58),
            shore = Color(0xFF5A5E58),
            shoreDeep = Color(0xFF454942),
            tree = Color(0xFF0D2736),
            sun = Color(0xFFFFE082),
            moon = Color(0xFFFFF3D0),
            cloud = Color(0xFF6B8CB0).copy(alpha = 0.28f),
            star = Color.White.copy(alpha = 0.78f),
            // Cooler / dimmer body — not as bright as day white
            caravanBody = Color(0xFFC8D8E4),
            caravanStripe = Color(0xFF81D4FA),
            caravanDoor = Color(0xFF98A8B8),
            caravanWindow = Color(0xFF1A2533),
            caravanWindowLit = Color(0xFFFFE0A3),
            hitch = Color(0xFF9AA8B5),
            wheel = Color(0xFF1C222A),
            wheelRim = Color(0xFFC5CCD4),
            shadow = Color.Black.copy(alpha = 0.10f),
        )
    }
}

/** Desaturate / cool sky & lake under cloud; suppress dawn/dusk warmth when covered. */
internal fun tintPaletteForWeather(
    base: HeroScenePalette,
    period: HeroSkyPeriod,
    weather: HeroSkyWeather,
    cloudDensity: Float,
): HeroScenePalette {
    val c = cloudDensity.coerceIn(0f, 1f)
    val warmPeriod = period == HeroSkyPeriod.Dawn || period == HeroSkyPeriod.Dusk
    val warmSuppress = when (weather) {
        HeroSkyWeather.Clear -> 0f
        HeroSkyWeather.PartlyCloudy -> 0.40f * c
        HeroSkyWeather.Fog -> 0.75f
        HeroSkyWeather.Overcast, HeroSkyWeather.Rain, HeroSkyWeather.Snow -> 0.90f
    }
    // Cloudy day: grey-blue sky (not muddy grey), still daytime-bright
    val grayTop = when (period) {
        HeroSkyPeriod.Night -> Color(0xFF1A2838)
        else -> Color(0xFF6A7E92)
    }
    val grayBot = when (period) {
        HeroSkyPeriod.Night -> Color(0xFF3A5068)
        else -> Color(0xFFA8B8C4)
    }
    val rainTop = when (period) {
        HeroSkyPeriod.Night -> Color(0xFF152030)
        else -> Color(0xFF4A5C6E)
    }
    val rainBot = when (period) {
        HeroSkyPeriod.Night -> Color(0xFF2E4254)
        else -> Color(0xFF7A90A0)
    }
    val fogBot = Color(0xFFC5CED6)
    val (skyT, skyB) = when (weather) {
        HeroSkyWeather.Clear -> base.skyTop to base.skyBottom
        HeroSkyWeather.PartlyCloudy ->
            lerpColor(base.skyTop, grayTop, 0.18f * c) to
                lerpColor(base.skyBottom, grayBot, 0.12f * c)
        HeroSkyWeather.Overcast ->
            lerpColor(base.skyTop, grayTop, 0.72f) to
                lerpColor(base.skyBottom, grayBot, 0.65f)
        HeroSkyWeather.Rain, HeroSkyWeather.Snow ->
            lerpColor(base.skyTop, rainTop, 0.80f) to
                lerpColor(base.skyBottom, rainBot, 0.75f)
        HeroSkyWeather.Fog ->
            lerpColor(base.skyTop, grayTop, 0.45f) to
                lerpColor(base.skyBottom, fogBot, 0.70f)
    }
    // Bastır dawn/dusk sıcak ufuk tonlarını yoğun bulutta
    val skyBottomFinal = if (warmPeriod && warmSuppress > 0.01f) {
        lerpColor(skyB, grayBot, warmSuppress)
    } else {
        skyB
    }
    val lakeCool = when (weather) {
        HeroSkyWeather.Clear -> base.lake
        HeroSkyWeather.PartlyCloudy -> lerpColor(base.lake, Color(0xFF5A7A90), 0.20f * c)
        HeroSkyWeather.Fog -> lerpColor(base.lake, Color(0xFF8A9AA8), 0.45f)
        HeroSkyWeather.Overcast -> lerpColor(base.lake, Color(0xFF5A7080), 0.40f)
        HeroSkyWeather.Rain, HeroSkyWeather.Snow -> lerpColor(base.lake, Color(0xFF4A6070), 0.50f)
    }
    val shoreNeutral = when (weather) {
        HeroSkyWeather.Overcast, HeroSkyWeather.Rain, HeroSkyWeather.Snow ->
            lerpColor(base.shore, Color(0xFF9A9A92), 0.42f)
        HeroSkyWeather.Fog -> lerpColor(base.shore, Color(0xFFB0B0A8), 0.28f)
        HeroSkyWeather.PartlyCloudy -> lerpColor(base.shore, Color(0xFFA8A498), 0.18f * c)
        else -> base.shore
    }
    val treeMuted = when (weather) {
        HeroSkyWeather.Overcast, HeroSkyWeather.Rain, HeroSkyWeather.Snow ->
            lerpColor(base.tree, Color(0xFF3A4A48), 0.35f)
        HeroSkyWeather.Fog -> lerpColor(base.tree, Color(0xFF4A5A58), 0.25f)
        HeroSkyWeather.PartlyCloudy -> lerpColor(base.tree, Color(0xFF2A3A34), 0.12f * c)
        else -> base.tree
    }
    // Soften ridge contrast under cloud (wash mid toward far)
    val farWash = when (weather) {
        HeroSkyWeather.Fog -> lerpColor(base.mountainFar, Color(0xFFC8D0D8), 0.55f)
        HeroSkyWeather.Overcast, HeroSkyWeather.Rain, HeroSkyWeather.Snow ->
            lerpColor(base.mountainFar, grayBot, 0.40f)
        else -> base.mountainFar
    }
    val midWash = when (weather) {
        HeroSkyWeather.Fog -> lerpColor(base.mountainMid, Color(0xFFA8B4C0), 0.40f)
        HeroSkyWeather.Overcast, HeroSkyWeather.Rain, HeroSkyWeather.Snow ->
            lerpColor(base.mountainMid, farWash, 0.42f)
        else -> base.mountainMid
    }
    val nearWash = when (weather) {
        HeroSkyWeather.Overcast, HeroSkyWeather.Rain, HeroSkyWeather.Snow ->
            lerpColor(base.mountainNear, midWash, 0.28f)
        HeroSkyWeather.Fog -> lerpColor(base.mountainNear, midWash, 0.35f)
        else -> base.mountainNear
    }
    // Cool white body under cloudy cover
    val bodyCool = when (weather) {
        HeroSkyWeather.Overcast, HeroSkyWeather.Rain, HeroSkyWeather.Snow ->
            lerpColor(base.caravanBody, Color(0xFFD0DCE6), 0.45f)
        HeroSkyWeather.PartlyCloudy -> lerpColor(base.caravanBody, Color(0xFFE0EAF0), 0.20f * c)
        HeroSkyWeather.Fog -> lerpColor(base.caravanBody, Color(0xFFD8E2EA), 0.30f)
        else -> base.caravanBody
    }
    return base.copy(
        skyTop = skyT,
        skyBottom = skyBottomFinal,
        mountainFar = farWash,
        mountainMid = midWash,
        mountainNear = nearWash,
        lake = lakeCool,
        shore = shoreNeutral,
        shoreDeep = lerpColor(base.shoreDeep, shoreNeutral, 0.3f),
        tree = treeMuted,
        caravanBody = bodyCool,
        caravanDoor = lerpColor(base.caravanDoor, bodyCool, 0.25f),
        shadow = base.shadow.copy(
            alpha = when (weather) {
                HeroSkyWeather.Clear -> base.shadow.alpha
                HeroSkyWeather.PartlyCloudy -> base.shadow.alpha * 0.70f
                else -> base.shadow.alpha * 0.38f
            },
        ),
    )
}

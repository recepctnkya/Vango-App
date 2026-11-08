package com.hextech.vango

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import com.hextech.vango.profile.CaravanType
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import testvango.composeapp.generated.resources.Res
import testvango.composeapp.generated.resources.phone_hero_00
import testvango.composeapp.generated.resources.phone_hero_01
import testvango.composeapp.generated.resources.phone_hero_02
import testvango.composeapp.generated.resources.phone_hero_03
import testvango.composeapp.generated.resources.phone_hero_04
import testvango.composeapp.generated.resources.phone_hero_05
import testvango.composeapp.generated.resources.phone_hero_06
import testvango.composeapp.generated.resources.phone_hero_07
import testvango.composeapp.generated.resources.phone_hero_08
import testvango.composeapp.generated.resources.phone_hero_09
import testvango.composeapp.generated.resources.phone_hero_10
import testvango.composeapp.generated.resources.phone_hero_11
import testvango.composeapp.generated.resources.phone_hero_12
import testvango.composeapp.generated.resources.phone_hero_13
import testvango.composeapp.generated.resources.phone_hero_14
import testvango.composeapp.generated.resources.phone_hero_15
import testvango.composeapp.generated.resources.phone_hero_16
import testvango.composeapp.generated.resources.phone_hero_17
import testvango.composeapp.generated.resources.phone_hero_18
import testvango.composeapp.generated.resources.phone_hero_19
import testvango.composeapp.generated.resources.phone_hero_20
import testvango.composeapp.generated.resources.phone_hero_21
import testvango.composeapp.generated.resources.phone_hero_22
import testvango.composeapp.generated.resources.phone_hero_23
import testvango.composeapp.generated.resources.phone_hero_tow_00
import testvango.composeapp.generated.resources.phone_hero_tow_01
import testvango.composeapp.generated.resources.phone_hero_tow_02
import testvango.composeapp.generated.resources.phone_hero_tow_03
import testvango.composeapp.generated.resources.phone_hero_tow_04
import testvango.composeapp.generated.resources.phone_hero_tow_05
import testvango.composeapp.generated.resources.phone_hero_tow_06
import testvango.composeapp.generated.resources.phone_hero_tow_07
import testvango.composeapp.generated.resources.phone_hero_tow_08
import testvango.composeapp.generated.resources.phone_hero_tow_09
import testvango.composeapp.generated.resources.phone_hero_tow_10
import testvango.composeapp.generated.resources.phone_hero_tow_11
import testvango.composeapp.generated.resources.phone_hero_tow_12
import testvango.composeapp.generated.resources.phone_hero_tow_13
import testvango.composeapp.generated.resources.phone_hero_tow_14
import testvango.composeapp.generated.resources.phone_hero_tow_15
import testvango.composeapp.generated.resources.phone_hero_tow_16
import testvango.composeapp.generated.resources.phone_hero_tow_17
import testvango.composeapp.generated.resources.phone_hero_tow_18
import testvango.composeapp.generated.resources.phone_hero_tow_19
import testvango.composeapp.generated.resources.phone_hero_tow_20
import testvango.composeapp.generated.resources.phone_hero_tow_21
import testvango.composeapp.generated.resources.phone_hero_tow_22
import testvango.composeapp.generated.resources.phone_hero_tow_23

/**
 * Caravan hero card background: one full scene per clock hour (00–23),
 * soft cross-fade driven by the live local clock.
 *
 * Motokaravan → [phone_hero_00]…[phone_hero_23]
 * Çekme karavan → [phone_hero_tow_00]…[phone_hero_tow_23]
 *
 * Do not overlay separate vehicle cutouts; the vehicle is baked into each frame.
 */
@Composable
fun PhoneTimedSkyBackground(
    caravanType: CaravanType = resolvedCaravanType(),
    modifier: Modifier = Modifier,
) {
    var blend by remember { mutableStateOf(heroBlendNow()) }
    LaunchedEffect(Unit) {
        while (true) {
            blend = heroBlendNow()
            delay(1_000L)
        }
    }
    val frames = remember(caravanType) { phoneHeroFramesFor(caravanType) }
    PhoneHeroCrossfade(
        frames = frames,
        baseIndex = blend.baseIndex,
        nextIndex = blend.nextIndex,
        fraction = blend.fraction,
        modifier = modifier,
    )
}

@Composable
private fun PhoneHeroCrossfade(
    frames: List<DrawableResource>,
    baseIndex: Int,
    nextIndex: Int,
    fraction: Float,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Image(
            painter = painterResource(frames[baseIndex]),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .alpha(1f - fraction),
        )
        if (nextIndex != baseIndex && fraction > 0.001f) {
            Image(
                painter = painterResource(frames[nextIndex]),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(fraction),
            )
        }
    }
}

private data class HeroBlend(
    val baseIndex: Int,
    val nextIndex: Int,
    val fraction: Float,
)

private const val HeroFrameCount = 24

fun phoneHeroFramesFor(type: CaravanType): List<DrawableResource> = when (type) {
    CaravanType.TowCaravan -> PhoneHeroTowFrames
    CaravanType.MotorCaravan -> PhoneHeroMotorFrames
}

/**
 * Index = clock hour.
 * phone_hero_00 ← source `24` (midnight)
 * phone_hero_01 ← source `01` … phone_hero_23 ← source `23`
 */
private val PhoneHeroMotorFrames: List<DrawableResource> = listOf(
    Res.drawable.phone_hero_00,
    Res.drawable.phone_hero_01,
    Res.drawable.phone_hero_02,
    Res.drawable.phone_hero_03,
    Res.drawable.phone_hero_04,
    Res.drawable.phone_hero_05,
    Res.drawable.phone_hero_06,
    Res.drawable.phone_hero_07,
    Res.drawable.phone_hero_08,
    Res.drawable.phone_hero_09,
    Res.drawable.phone_hero_10,
    Res.drawable.phone_hero_11,
    Res.drawable.phone_hero_12,
    Res.drawable.phone_hero_13,
    Res.drawable.phone_hero_14,
    Res.drawable.phone_hero_15,
    Res.drawable.phone_hero_16,
    Res.drawable.phone_hero_17,
    Res.drawable.phone_hero_18,
    Res.drawable.phone_hero_19,
    Res.drawable.phone_hero_20,
    Res.drawable.phone_hero_21,
    Res.drawable.phone_hero_22,
    Res.drawable.phone_hero_23,
)

/** Same hour mapping as motor; vehicle in frame is tow caravan. */
private val PhoneHeroTowFrames: List<DrawableResource> = listOf(
    Res.drawable.phone_hero_tow_00,
    Res.drawable.phone_hero_tow_01,
    Res.drawable.phone_hero_tow_02,
    Res.drawable.phone_hero_tow_03,
    Res.drawable.phone_hero_tow_04,
    Res.drawable.phone_hero_tow_05,
    Res.drawable.phone_hero_tow_06,
    Res.drawable.phone_hero_tow_07,
    Res.drawable.phone_hero_tow_08,
    Res.drawable.phone_hero_tow_09,
    Res.drawable.phone_hero_tow_10,
    Res.drawable.phone_hero_tow_11,
    Res.drawable.phone_hero_tow_12,
    Res.drawable.phone_hero_tow_13,
    Res.drawable.phone_hero_tow_14,
    Res.drawable.phone_hero_tow_15,
    Res.drawable.phone_hero_tow_16,
    Res.drawable.phone_hero_tow_17,
    Res.drawable.phone_hero_tow_18,
    Res.drawable.phone_hero_tow_19,
    Res.drawable.phone_hero_tow_20,
    Res.drawable.phone_hero_tow_21,
    Res.drawable.phone_hero_tow_22,
    Res.drawable.phone_hero_tow_23,
)

/** Maps local clock to hour frames 0..23 with wrap-around soft blend. */
private fun heroBlendNow(
    nowMs: Long = Clock.System.now().toEpochMilliseconds(),
): HeroBlend {
    val local = Instant.fromEpochMilliseconds(nowMs)
        .toLocalDateTime(TimeZone.currentSystemDefault())
    val hourFrac = local.hour + (local.minute * 60 + local.second) / 3600f
    val mapped = hourFrac.coerceIn(0f, 23.999f)
    val base = mapped.toInt().coerceIn(0, HeroFrameCount - 1)
    val next = (base + 1) % HeroFrameCount
    val fraction = (mapped - base).coerceIn(0f, 1f)
    return HeroBlend(baseIndex = base, nextIndex = next, fraction = fraction)
}

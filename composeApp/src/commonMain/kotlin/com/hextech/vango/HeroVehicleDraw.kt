package com.hextech.vango

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
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

internal object HeroVehicleColors {
    val body = Color(0xFFEAF4F8)
    val bodyBot = Color(0xFFC8DFEB)
    val stripe = Color(0xFF75C9E9)
    val tire = Color(0xFF17212B)
    val rim = Color(0xFFAABBC8)
    val dayGlass = Color(0xFF16455B)
    val nightLivingGlass = Color(0xFFFFE0A0)
    val door = Color(0xFFB8D4E4)
    val frame = Color(0xFF5A6A78).copy(alpha = 0.55f)
    val hitch = Color(0xFF9AA8B5)
    val handle = Color(0xFF2A3340)
    val stop = Color(0xFFE53935)
    val amber = Color(0xFFFF9800)
    val headlight = Color(0xFFFFF3C4)
}

/** Design-space metrics returned by each vehicle draw function. */
data class HeroVehicleMetrics(
    val contentLeft: Float,
    val contentRight: Float,
    val contentTop: Float,
    val groundY: Float,
    val linkX: Float,
    val linkY: Float,
)
/** Narrow dark contact under a tire — stays readable even when body shadow is soft/cloudy. */
internal fun DrawScope.drawHeroWheelContactShadow(center: Offset, tireR: Float, shadow: Color) {
    val sw = tireR * 1.35f
    val sh = tireR * 0.16f
    drawOval(
        color = Color.Black.copy(alpha = (0.22f + shadow.alpha * 0.55f).coerceIn(0.18f, 0.34f)),
        topLeft = Offset(center.x - sw * 0.5f, center.y + tireR * 0.62f),
        size = Size(sw, sh),
    )
}

/**
 * Snow sit: thin contact, pressed hollow, tiny edge mounds — tire stays mostly visible.
 * Never paints a pale disc over the lower half of the tire.
 */
internal fun DrawScope.drawHeroWheelSnowSit(center: Offset, tireR: Float, snowCover: Float) {
    if (snowCover < 0.12f) return
    val a = (0.35f + snowCover * 0.50f).coerceIn(0.35f, 0.85f)
    val groundY = center.y + tireR * 0.72f
    // Pressed hollow
    drawOval(
        HeroSnowContact.copy(alpha = 0.35f * a),
        topLeft = Offset(center.x - tireR * 0.85f, groundY - tireR * 0.08f),
        size = Size(tireR * 1.70f, tireR * 0.22f),
    )
    // Tiny side mounds (not covering tire)
    drawOval(
        HeroSnowLit.copy(alpha = 0.55f * a),
        topLeft = Offset(center.x - tireR * 1.15f, groundY - tireR * 0.18f),
        size = Size(tireR * 0.42f, tireR * 0.28f),
    )
    drawOval(
        HeroSnowMain.copy(alpha = 0.50f * a),
        topLeft = Offset(center.x + tireR * 0.72f, groundY - tireR * 0.16f),
        size = Size(tireR * 0.40f, tireR * 0.26f),
    )
}

/** Irregular roof snow strip — top plane only; sides/windows stay clear. */
internal fun DrawScope.drawHeroRoofSnow(
    left: Float,
    right: Float,
    topY: Float,
    thickness: Float,
    snowCover: Float,
    light: HeroSceneLight,
    equipment: List<Rect> = emptyList(),
) {
    if (snowCover < 0.12f) return
    val a = (0.45f + snowCover * 0.45f).coerceIn(0.45f, 0.92f)
    val shade = light.shadeColor(HeroSnowHollow, 0.35f).copy(alpha = 0.40f * a)
    val lit = light.litColor(HeroSnowLit, 0.45f).copy(alpha = 0.85f * a)
    val w = right - left
    // Soft thickness shadow under snow edge
    drawRect(
        shade,
        topLeft = Offset(left, topY + thickness * 0.55f),
        size = Size(w, thickness * 0.55f),
    )
    val n = 8
    val path = Path().apply {
        moveTo(left, topY + thickness * 0.35f)
        for (i in 0..n) {
            val t = i / n.toFloat()
            val x = left + w * t
            val bump = thickness * (0.55f + 0.35f * sin(i * 1.7f + 0.4f))
            lineTo(x, topY - bump * 0.15f)
        }
        for (i in n downTo 0) {
            val t = i / n.toFloat()
            val x = left + w * t
            val dip = thickness * (0.15f + 0.20f * abs(sin(i * 2.1f)))
            lineTo(x, topY + thickness * 0.55f + dip)
        }
        close()
    }
    drawPath(path, color = lit)
    equipment.forEach { eq ->
        drawRoundRect(
            lit,
            topLeft = Offset(eq.left - thickness * 0.15f, eq.top - thickness * 0.55f),
            size = Size(eq.width + thickness * 0.30f, thickness * 0.85f),
            cornerRadius = CornerRadius(thickness * 0.2f, thickness * 0.2f),
        )
        drawRect(
            shade,
            topLeft = Offset(eq.left, eq.top + eq.height * 0.15f),
            size = Size(eq.width, thickness * 0.35f),
        )
    }
}

/** Shared wheel: dark tire, lit rim top / darker rim bottom, separate hub. */
internal fun DrawScope.drawHeroWheel(
    center: Offset,
    tireR: Float,
    rimR: Float = tireR * (HeroRimR / HeroWheelR),
    hubR: Float = tireR * (HeroHubR / HeroWheelR),
    tire: Color = HeroVehicleColors.tire,
    rim: Color = HeroVehicleColors.rim,
) {
    drawCircle(tire, radius = tireR, center = center)
    drawCircle(
        color = Color.Black.copy(alpha = 0.40f),
        radius = tireR * 0.92f,
        center = center,
        style = Stroke(width = tireR * 0.07f),
    )
    // Rim with soft top light / bottom shade (not a flat disk)
    drawCircle(
        brush = Brush.verticalGradient(
            colorStops = arrayOf(
                0.00f to lerpColor(rim, Color.White, 0.28f),
                0.45f to rim,
                1.00f to lerpColor(rim, Color.Black, 0.22f),
            ),
            startY = center.y - rimR,
            endY = center.y + rimR,
        ),
        radius = rimR,
        center = center,
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.28f),
        radius = rimR * 0.78f,
        center = Offset(center.x - tireR * 0.06f, center.y - tireR * 0.12f),
        style = Stroke(width = tireR * 0.05f),
    )
    drawCircle(tire.copy(alpha = 0.95f), radius = hubR, center = center)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(lerpColor(rim, Color.White, 0.35f), rim.copy(alpha = 0.7f)),
            center = Offset(center.x - hubR * 0.2f, center.y - hubR * 0.25f),
            radius = hubR,
        ),
        radius = hubR * 0.48f,
        center = center,
    )
}

/** Dark inner shade in the wheel well (upper arc only — never a pale semicircle under the tire). */
internal fun DrawScope.drawHeroWheelWellInnerShadow(
    center: Offset,
    wellR: Float,
    bodyBottomY: Float,
) {
    val d = (center.y - bodyBottomY).coerceIn(-wellR + 0.5f, wellR - 0.5f)
    val startAngle = -asin(d / wellR) * (180f / PI.toFloat())
    val sweep = -(180f - 2f * abs(startAngle))
    val innerR = wellR * 0.92f
    drawArc(
        color = Color.Black.copy(alpha = 0.28f),
        startAngle = startAngle,
        sweepAngle = sweep,
        useCenter = false,
        topLeft = Offset(center.x - innerR, center.y - innerR),
        size = Size(innerR * 2f, innerR * 2f),
        style = Stroke(width = wellR * 0.22f, cap = StrokeCap.Round),
    )
}

/**
 * While tracing the body underside right→left, cut an upper semicircle wheel well
 * at [center] so the tire is not covered by body fill (silhouette arc, not paint-over).
 */
internal fun Path.arcWheelWellRightToLeft(
    center: Offset,
    wellR: Float,
    bodyBottomY: Float,
) {
    val d = (center.y - bodyBottomY).coerceIn(-wellR + 0.5f, wellR - 0.5f)
    val half = sqrt(wellR * wellR - d * d)
    lineTo(center.x + half, bodyBottomY)
    val startAngle = -asin(d / wellR) * (180f / PI.toFloat())
    val sweep = -(180f - 2f * abs(startAngle))
    arcTo(
        Rect(center.x - wellR, center.y - wellR, center.x + wellR, center.y + wellR),
        startAngleDegrees = startAngle,
        sweepAngleDegrees = sweep,
        forceMoveTo = false,
    )
}

/**
 * Light-blue curved fender band along the wheel-well rim (integrated arch, not a separate flap).
 * Follows the same upper arc as the body cutout, just outside the well edge.
 */
internal fun DrawScope.drawHeroWheelArchBand(
    center: Offset,
    wellR: Float,
    band: Float,
    color: Color,
    bodyBottomY: Float,
) {
    val d = (center.y - bodyBottomY).coerceIn(-wellR + 0.5f, wellR - 0.5f)
    val startAngle = -asin(d / wellR) * (180f / PI.toFloat())
    val sweep = -(180f - 2f * abs(startAngle))
    val midR = wellR + band * 0.55f
    drawArc(
        color = color,
        startAngle = startAngle,
        sweepAngle = sweep,
        useCenter = false,
        topLeft = Offset(center.x - midR, center.y - midR),
        size = Size(midR * 2f, midR * 2f),
        style = Stroke(width = band, cap = StrokeCap.Round),
    )
}

/**
 * Living window — darker top / lighter bottom glass, sky+mountain reflection clipped inside,
 * thin dark frame + small upper inner shadow.
 */
internal fun DrawScope.drawHeroLivingWindow(
    topLeft: Offset,
    size: Size,
    corner: Float,
    glass: Color,
    nightLit: Boolean,
    skyTint: Color = Color(0xFF6AA9D8),
) {
    val cr = CornerRadius(corner, corner)
    if (nightLit) {
        drawRoundRect(
            color = glass.copy(alpha = 0.22f),
            topLeft = Offset(topLeft.x - corner * 0.28f, topLeft.y - corner * 0.28f),
            size = Size(size.width + corner * 0.56f, size.height + corner * 0.56f),
            cornerRadius = CornerRadius(corner * 1.15f, corner * 1.15f),
        )
    }
    val glassTop = if (nightLit) glass else lerpColor(glass, Color.Black, 0.18f)
    val glassBot = if (nightLit) lerpColor(glass, Color.White, 0.12f) else lerpColor(glass, Color.White, 0.16f)
    drawRoundRect(
        brush = Brush.verticalGradient(
            colorStops = arrayOf(
                0.00f to glassTop,
                0.45f to glass,
                1.00f to glassBot,
            ),
            startY = topLeft.y,
            endY = topLeft.y + size.height,
        ),
        topLeft = topLeft,
        size = size,
        cornerRadius = cr,
    )
    if (!nightLit) {
        val glassClip = Path().apply {
            addRoundRect(
                RoundRect(
                    left = topLeft.x,
                    top = topLeft.y,
                    right = topLeft.x + size.width,
                    bottom = topLeft.y + size.height,
                    cornerRadius = cr,
                ),
            )
        }
        clipPath(glassClip) {
            // Soft sky + distant ridge reflection (stays inside frame)
            drawRect(
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.00f to skyTint.copy(alpha = 0.28f),
                        0.35f to skyTint.copy(alpha = 0.10f),
                        0.55f to Color(0xFF3A5A70).copy(alpha = 0.12f),
                        1.00f to Color.Transparent,
                    ),
                    startY = topLeft.y,
                    endY = topLeft.y + size.height,
                ),
                topLeft = topLeft,
                size = size,
            )
            // Tiny broken mountain hint
            drawPath(
                Path().apply {
                    val y0 = topLeft.y + size.height * 0.42f
                    moveTo(topLeft.x + size.width * 0.12f, y0 + size.height * 0.12f)
                    lineTo(topLeft.x + size.width * 0.28f, y0)
                    lineTo(topLeft.x + size.width * 0.42f, y0 + size.height * 0.10f)
                    lineTo(topLeft.x + size.width * 0.58f, y0 - size.height * 0.02f)
                    lineTo(topLeft.x + size.width * 0.78f, y0 + size.height * 0.08f)
                    lineTo(topLeft.x + size.width * 0.90f, y0 + size.height * 0.14f)
                    lineTo(topLeft.x + size.width * 0.90f, topLeft.y + size.height * 0.92f)
                    lineTo(topLeft.x + size.width * 0.12f, topLeft.y + size.height * 0.92f)
                    close()
                },
                color = Color(0xFF2A4A5C).copy(alpha = 0.14f),
            )
        }
    }
    // Upper inner shade
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.20f),
        topLeft = Offset(topLeft.x + corner * 0.15f, topLeft.y + corner * 0.12f),
        size = Size(size.width - corner * 0.30f, size.height * 0.12f),
        cornerRadius = CornerRadius(corner * 0.5f, corner * 0.5f),
    )
    // Thin dark frame
    drawRoundRect(
        color = Color(0xFF2A3340).copy(alpha = 0.72f),
        topLeft = topLeft,
        size = size,
        cornerRadius = cr,
        style = Stroke(width = (corner * 0.14f).coerceAtLeast(1.2f)),
    )
}

/** Flush door — low-contrast seam (lit one side / shade other) + handle contact shadow. */
internal fun DrawScope.drawHeroVehicleDoor(
    topLeft: Offset,
    size: Size,
    corner: Float,
    lightFromLeft: Boolean,
    handle: Offset,
    handleR: Float,
) {
    val cr = CornerRadius(corner, corner)
    drawRoundRect(color = HeroVehicleColors.door, topLeft = topLeft, size = size, cornerRadius = cr)
    // Soft volume on door panel
    drawRoundRect(
        brush = Brush.horizontalGradient(
            colorStops = arrayOf(
                0.00f to (if (lightFromLeft) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.08f)),
                0.50f to Color.Transparent,
                1.00f to (if (lightFromLeft) Color.Black.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.08f)),
            ),
            startX = topLeft.x,
            endX = topLeft.x + size.width,
        ),
        topLeft = topLeft,
        size = size,
        cornerRadius = cr,
    )
    val litEdge = Color.White.copy(alpha = 0.22f)
    val shadeEdge = Color.Black.copy(alpha = 0.16f)
    val stroke = (size.width * 0.018f).coerceIn(1.1f, 2.4f)
    // Left seam
    drawLine(
        color = if (lightFromLeft) litEdge else shadeEdge,
        start = Offset(topLeft.x + stroke * 0.5f, topLeft.y + corner * 0.4f),
        end = Offset(topLeft.x + stroke * 0.5f, topLeft.y + size.height - corner * 0.4f),
        strokeWidth = stroke,
        cap = StrokeCap.Round,
    )
    // Right seam
    drawLine(
        color = if (lightFromLeft) shadeEdge else litEdge,
        start = Offset(topLeft.x + size.width - stroke * 0.5f, topLeft.y + corner * 0.4f),
        end = Offset(topLeft.x + size.width - stroke * 0.5f, topLeft.y + size.height - corner * 0.4f),
        strokeWidth = stroke,
        cap = StrokeCap.Round,
    )
    // Outer flush outline (very soft)
    drawRoundRect(
        color = Color(0xFF3A4550).copy(alpha = 0.28f),
        topLeft = topLeft,
        size = size,
        cornerRadius = cr,
        style = Stroke(width = stroke * 0.85f),
    )
    // Handle contact shadow + handle
    drawOval(
        color = Color.Black.copy(alpha = 0.22f),
        topLeft = Offset(handle.x - handleR * 0.9f, handle.y + handleR * 0.35f),
        size = Size(handleR * 1.8f, handleR * 0.55f),
    )
    drawCircle(HeroVehicleColors.handle, radius = handleR, center = handle)
}

/** Body fill colors from shared light (cool under cloud, dim at night, warm only on lit faces). */
internal fun heroVehicleBodyGradientColors(
    light: HeroSceneLight,
    nightDim: Boolean,
): Triple<Color, Color, Color> {
    val mul = (if (nightDim) 0.70f else 1f) *
        (0.72f + 0.18f * light.ambient + 0.10f * light.direct)
    fun tone(c: Color): Color {
        var t = lerpColor(c, Color(0xFF1E2834), 1f - mul)
        // Cool bias when overcast / night
        if (light.warmth < 0f) {
            t = lerpColor(t, HeroCoolShade, (-light.warmth) * 0.35f)
        }
        t = light.litColor(t, 0.45f)
        return t
    }
    val top = tone(Color(0xFFF5F8FA))
    val mid = tone(HeroVehicleColors.body)
    val bot = tone(Color(0xFFB4C8D6))
    return Triple(top, mid, bot)
}

/** Soft roof highlight + lower-edge shade clipped to body (no thick outline). */
internal fun DrawScope.drawHeroBodyVolumePass(
    body: Path,
    bodyTopY: Float,
    bodyBotY: Float,
    light: HeroSceneLight,
    nightDim: Boolean,
    /** Optional horizontal band for nose/cab turn wash (design-space already scaled). */
    turnWash: Rect? = null,
) {
    val contrast = (0.55f + 0.45f * light.strength) * (if (nightDim) 0.75f else 1f)
    clipPath(body) {
        // Roof highlight strip
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color.White.copy(alpha = 0.22f * contrast),
                0.55f to Color.White.copy(alpha = 0.06f * contrast),
                1f to Color.Transparent,
            ),
            topLeft = Offset(0f, bodyTopY),
            size = Size(size.width, (bodyBotY - bodyTopY) * 0.12f),
        )
        // Lower edge shade
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color.Transparent,
                0.45f to Color.Black.copy(alpha = 0.06f * contrast),
                1f to Color.Black.copy(alpha = 0.16f * contrast),
            ),
            topLeft = Offset(0f, bodyBotY - (bodyBotY - bodyTopY) * 0.14f),
            size = Size(size.width, (bodyBotY - bodyTopY) * 0.14f),
        )
        // Soft turn / nose wash (keeps side view — no new front face)
        if (turnWash != null) {
            val fromLeft = light.dirX >= 0f
            drawRect(
                brush = Brush.horizontalGradient(
                    colorStops = arrayOf(
                        0.00f to (if (fromLeft) Color.White.copy(alpha = 0.12f * contrast) else Color.Black.copy(alpha = 0.10f * contrast)),
                        0.55f to Color.Transparent,
                        1.00f to Color.Transparent,
                    ),
                    startX = turnWash.left,
                    endX = turnWash.right,
                ),
                topLeft = Offset(turnWash.left, turnWash.top),
                size = Size(turnWash.width, turnWash.height),
            )
        }
    }
}
/**
 * TOWABLE travel trailer — independent of [drawMotorhome].
 * Local design 1000×500; fixed path geometry; uniform [scale].
 * No cab, windshield, mirror, headlight, or second main axle.
 */
internal fun DrawScope.drawTowableCaravan(
    origin: Offset,
    scale: Float,
    nightWindowsLit: Boolean,
    shadow: Color,
    light: HeroSceneLight = HeroSceneLight(0.25f, 0.5f, 0.8f),
    skyTint: Color = Color(0xFF6AA9D8),
    snowCover: Float = 0f,
): HeroVehicleMetrics {
    fun p(x: Float, y: Float) = Offset(origin.x + x * scale, origin.y + y * scale)
    fun sx(v: Float) = v * scale
    val livingGlass = if (nightWindowsLit) HeroVehicleColors.nightLivingGlass else HeroVehicleColors.dayGlass
    val shadowLen = (1.2f - light.elev * 0.55f).coerceIn(0.55f, 1.25f)
    val shadowDx = light.dirX * sx(28f) * shadowLen
    // Cloudy → pale, wider body wash; tire contacts stay darker separately
    val spread = 1f + (1f - light.strength) * 0.65f
    val bodyA = shadow.alpha * (0.14f + 0.16f * light.strength)

    drawOval(
        color = shadow.copy(alpha = bodyA * 0.55f),
        topLeft = Offset(p(240f, 428f).x + shadowDx * 0.25f, p(240f, 428f).y),
        size = Size(sx(640f) * spread, sx(22f) * spread),
    )
    drawOval(
        color = shadow.copy(alpha = bodyA * 0.40f),
        topLeft = Offset(p(400f, 434f).x + shadowDx * 0.15f, p(400f, 434f).y),
        size = Size(sx(280f) * spread, sx(16f) * spread),
    )
    drawOval(
        color = shadow.copy(alpha = bodyA * 0.50f),
        topLeft = Offset(p(600f, 432f).x + shadowDx * 0.2f, p(600f, 432f).y),
        size = Size(sx(130f), sx(18f)),
    )

    // Hitch A-frame (~1.5× thicker arms) + prominent coupler
    val hitch = HeroVehicleColors.hitch
    val tip = p(TowLinkX, TowLinkY)
    val armW = sx(16f)
    drawLine(hitch, tip, p(225f, 352f), strokeWidth = armW, cap = StrokeCap.Round)
    drawLine(hitch, tip, p(225f, 378f), strokeWidth = armW, cap = StrokeCap.Round)
    drawRoundRect(
        color = hitch,
        topLeft = p(200f, 344f),
        size = Size(sx(52f), sx(40f)),
        cornerRadius = CornerRadius(sx(6f), sx(6f)),
    )
    drawCircle(hitch, radius = sx(24f), center = tip)
    drawCircle(Color(0xFF5A6570), radius = sx(13f), center = tip)
    drawCircle(hitch.copy(alpha = 0.85f), radius = sx(6f), center = tip)

    // Jockey: clear vertical post + ground contact
    val jockeyC = p(125f, 447f)
    // Jockey post attachment shade near A-frame
    drawOval(
        color = Color.Black.copy(alpha = 0.18f),
        topLeft = Offset(p(118f, 362f).x, p(118f, 362f).y),
        size = Size(sx(14f), sx(8f)),
    )
    drawLine(
        hitch,
        start = p(125f, 368f),
        end = Offset(jockeyC.x, jockeyC.y - sx(17f)),
        strokeWidth = sx(11f),
        cap = StrokeCap.Round,
    )
    drawRoundRect(
        hitch,
        topLeft = Offset(jockeyC.x - sx(8f), jockeyC.y - sx(24f)),
        size = Size(sx(16f), sx(12f)),
        cornerRadius = CornerRadius(sx(2f), sx(2f)),
    )
    drawHeroWheelContactShadow(jockeyC, sx(18f), shadow)
    drawHeroWheel(jockeyC, sx(18f), rimR = sx(9f), hubR = sx(4f))

    // Main wheel before body (shows through well cutout)
    val wheelC = p(665f, HeroWheelCy)
    val tireR = sx(HeroWheelR)
    val wellR = sx(HeroWellR)
    val bodyBottomY = p(0f, 395f).y
    drawHeroWheelSnowSit(wheelC, tireR, snowCover)
    drawHeroWheelContactShadow(wheelC, tireR, shadow)
    drawHeroWheel(wheelC, tireR, rimR = sx(HeroRimR), hubR = sx(HeroHubR))

    // Body silhouette with wheel well cut into underside (arc, not paint-over)
    val body = Path().apply {
        moveTo(p(280f, 65f).x, p(280f, 65f).y)
        lineTo(p(900f, 65f).x, p(900f, 65f).y)
        cubicTo(p(935f, 65f).x, p(935f, 65f).y, p(950f, 85f).x, p(950f, 85f).y, p(950f, 120f).x, p(950f, 120f).y)
        lineTo(p(950f, 355f).x, p(950f, 355f).y)
        cubicTo(p(950f, 382f).x, p(950f, 382f).y, p(935f, 395f).x, p(935f, 395f).y, p(905f, 395f).x, p(905f, 395f).y)
        arcWheelWellRightToLeft(wheelC, wellR, bodyBottomY)
        lineTo(p(250f, 395f).x, p(250f, 395f).y)
        cubicTo(p(210f, 395f).x, p(210f, 395f).y, p(190f, 375f).x, p(190f, 375f).y, p(190f, 340f).x, p(190f, 340f).y)
        cubicTo(p(190f, 265f).x, p(190f, 265f).y, p(220f, 145f).x, p(220f, 145f).y, p(250f, 90f).x, p(250f, 90f).y)
        cubicTo(p(258f, 74f).x, p(258f, 74f).y, p(268f, 65f).x, p(268f, 65f).y, p(280f, 65f).x, p(280f, 65f).y)
        close()
    }
    val (bodyTop, bodyMid, bodyBot) = heroVehicleBodyGradientColors(light, nightWindowsLit)
    val bodyY0 = origin.y + 65f * scale
    val bodyY1 = origin.y + 395f * scale
    drawPath(
        body,
        brush = Brush.verticalGradient(
            colorStops = arrayOf(
                0.00f to bodyTop,
                0.42f to bodyMid,
                1.00f to bodyBot,
            ),
            startY = bodyY0,
            endY = bodyY1,
        ),
    )
    drawHeroBodyVolumePass(
        body = body,
        bodyTopY = bodyY0,
        bodyBotY = bodyY1,
        light = light,
        nightDim = nightWindowsLit,
        turnWash = Rect(p(190f, 70f).x, bodyY0, p(310f, 70f).x, bodyY1),
    )
    // Very soft edge only — no thick plastic outline
    drawPath(body, color = Color(0xFF4A5A68).copy(alpha = 0.18f), style = Stroke(width = sx(1.4f)))
    drawHeroWheelWellInnerShadow(wheelC, wellR, bodyBottomY)
    drawHeroWheelArchBand(wheelC, wellR, sx(HeroFenderBand), HeroVehicleColors.stripe, bodyBottomY)

    // Hitch / jockey join shadows on body face (after fill so they stay visible)
    drawOval(
        color = Color.Black.copy(alpha = 0.18f),
        topLeft = Offset(p(198f, 346f).x, p(198f, 346f).y),
        size = Size(sx(28f), sx(14f)),
    )
    drawOval(
        color = Color.Black.copy(alpha = 0.15f),
        topLeft = Offset(p(198f, 370f).x, p(198f, 370f).y),
        size = Size(sx(28f), sx(12f)),
    )
    drawOval(
        color = Color.Black.copy(alpha = 0.14f),
        topLeft = Offset(p(188f, 328f).x, p(188f, 328f).y),
        size = Size(sx(16f), sx(10f)),
    )

    drawRoundRect(
        color = HeroVehicleColors.stripe,
        topLeft = p(240f, 278f),
        size = Size(sx(680f), sx(16f)),
        cornerRadius = CornerRadius(sx(5f), sx(5f)),
    )

    val acRect = Rect(p(520f, 48f).x, p(520f, 48f).y, p(570f, 48f).x, p(520f, 62f).y)
    drawRoundRect(
        color = Color(0xFFB8C8D4),
        topLeft = p(520f, 48f),
        size = Size(sx(50f), sx(14f)),
        cornerRadius = CornerRadius(sx(3f), sx(3f)),
    )

    drawHeroLivingWindow(p(310f, 145f), Size(sx(185f), sx(105f)), sx(15f), livingGlass, nightWindowsLit, skyTint)
    drawHeroLivingWindow(p(770f, 150f), Size(sx(115f), sx(95f)), sx(13f), livingGlass, nightWindowsLit, skyTint)

    drawHeroVehicleDoor(
        topLeft = p(585f, 125f),
        size = Size(sx(110f), sx(188f)),
        corner = sx(15f),
        lightFromLeft = light.dirX >= 0f,
        handle = p(670f, 250f),
        handleR = sx(7f),
    )

    drawRoundRect(HeroVehicleColors.amber, p(255f, 368f), Size(sx(26f), sx(11f)), CornerRadius(sx(2f), sx(2f)))
    drawRoundRect(HeroVehicleColors.amber, p(880f, 368f), Size(sx(26f), sx(11f)), CornerRadius(sx(2f), sx(2f)))
    drawRoundRect(HeroVehicleColors.stop, p(938f, 205f), Size(sx(10f), sx(38f)), CornerRadius(sx(2f), sx(2f)))

    // Roof snow last so it sits on top plane / AC without painting sides
    drawHeroRoofSnow(
        left = p(280f, 65f).x,
        right = p(900f, 65f).x,
        topY = p(280f, 65f).y,
        thickness = sx(10f),
        snowCover = snowCover,
        light = light,
        equipment = listOf(acRect),
    )

    return HeroVehicleMetrics(
        contentLeft = TowContentLeft,
        contentRight = TowContentRight,
        contentTop = 48f,
        groundY = HeroGroundY,
        linkX = TowLinkX,
        linkY = TowLinkY,
    )
}

/**
 * MOTORHOME — alcove cab + living cell (independent of [drawTowableCaravan]).
 * Local design 1000×500; fixed path geometry; uniform [scale]. Dual axle; no hitch.
 */
internal fun DrawScope.drawMotorhome(
    origin: Offset,
    scale: Float,
    nightWindowsLit: Boolean,
    shadow: Color,
    light: HeroSceneLight = HeroSceneLight(0.25f, 0.5f, 0.8f),
    skyTint: Color = Color(0xFF6AA9D8),
    snowCover: Float = 0f,
): HeroVehicleMetrics {
    fun p(x: Float, y: Float) = Offset(origin.x + x * scale, origin.y + y * scale)
    fun sx(v: Float) = v * scale
    val livingGlass = if (nightWindowsLit) HeroVehicleColors.nightLivingGlass else HeroVehicleColors.dayGlass
    val cabGlass = HeroVehicleColors.dayGlass // always dark petrol
    val shadowLen = (1.2f - light.elev * 0.55f).coerceIn(0.55f, 1.25f)
    val shadowDx = light.dirX * sx(28f) * shadowLen
    val spread = 1f + (1f - light.strength) * 0.65f
    val bodyA = shadow.alpha * (0.14f + 0.16f * light.strength)
    // Wide low-opacity cool under-body shadow when sitting on snow
    val underCol = if (snowCover >= 0.12f) {
        Color(0xFF4A5A6A).copy(alpha = (bodyA * 0.55f + snowCover * 0.08f).coerceIn(0.10f, 0.28f))
    } else {
        shadow.copy(alpha = bodyA * 0.50f)
    }

    drawOval(
        color = underCol,
        topLeft = Offset(p(90f, 428f).x + shadowDx * 0.25f, p(90f, 428f).y),
        size = Size(sx(820f) * spread, sx(22f) * spread),
    )
    drawOval(
        color = shadow.copy(alpha = bodyA * 0.45f),
        topLeft = Offset(p(160f, 433f).x + shadowDx * 0.2f, p(160f, 433f).y),
        size = Size(sx(110f), sx(16f)),
    )
    drawOval(
        color = shadow.copy(alpha = bodyA * 0.45f),
        topLeft = Offset(p(740f, 433f).x + shadowDx * 0.2f, p(740f, 433f).y),
        size = Size(sx(110f), sx(16f)),
    )

    // Dual axle before body (visible through well cutouts)
    val frontC = p(210f, HeroWheelCy)
    val rearC = p(800f, HeroWheelCy)
    val tireR = sx(HeroWheelR)
    val wellR = sx(HeroWellR)
    val bodyBottomY = p(0f, 395f).y
    drawHeroWheelSnowSit(frontC, tireR, snowCover)
    drawHeroWheelContactShadow(frontC, tireR, shadow)
    drawHeroWheel(frontC, tireR, rimR = sx(HeroRimR), hubR = sx(HeroHubR))
    drawHeroWheelSnowSit(rearC, tireR, snowCover)
    drawHeroWheelContactShadow(rearC, tireR, shadow)
    drawHeroWheel(rearC, tireR, rimR = sx(HeroRimR), hubR = sx(HeroHubR))

    // Body silhouette with front+rear wheel wells cut into underside (R→L)
    val body = Path().apply {
        moveTo(p(340f, 65f).x, p(340f, 65f).y)
        lineTo(p(905f, 65f).x, p(905f, 65f).y)
        cubicTo(p(935f, 65f).x, p(935f, 65f).y, p(950f, 85f).x, p(950f, 85f).y, p(950f, 115f).x, p(950f, 115f).y)
        lineTo(p(950f, 365f).x, p(950f, 365f).y)
        cubicTo(p(950f, 385f).x, p(950f, 385f).y, p(935f, 395f).x, p(935f, 395f).y, p(915f, 395f).x, p(915f, 395f).y)
        arcWheelWellRightToLeft(rearC, wellR, bodyBottomY)
        arcWheelWellRightToLeft(frontC, wellR, bodyBottomY)
        lineTo(p(85f, 395f).x, p(85f, 395f).y)
        cubicTo(p(58f, 395f).x, p(58f, 395f).y, p(45f, 382f).x, p(45f, 382f).y, p(45f, 355f).x, p(45f, 355f).y)
        lineTo(p(55f, 310f).x, p(55f, 310f).y)
        cubicTo(p(60f, 292f).x, p(60f, 292f).y, p(75f, 278f).x, p(75f, 278f).y, p(105f, 265f).x, p(105f, 265f).y)
        lineTo(p(155f, 245f).x, p(155f, 245f).y)
        lineTo(p(245f, 125f).x, p(245f, 125f).y)
        lineTo(p(210f, 125f).x, p(210f, 125f).y)
        cubicTo(p(215f, 90f).x, p(215f, 90f).y, p(265f, 65f).x, p(265f, 65f).y, p(340f, 65f).x, p(340f, 65f).y)
        close()
    }
    val (bodyTop, bodyMid, bodyBot) = heroVehicleBodyGradientColors(light, nightWindowsLit)
    val bodyY0 = origin.y + 65f * scale
    val bodyY1 = origin.y + 395f * scale
    drawPath(
        body,
        brush = Brush.verticalGradient(
            colorStops = arrayOf(
                0.00f to bodyTop,
                0.42f to bodyMid,
                1.00f to bodyBot,
            ),
            startY = bodyY0,
            endY = bodyY1,
        ),
    )
    drawHeroBodyVolumePass(
        body = body,
        bodyTopY = bodyY0,
        bodyBotY = bodyY1,
        light = light,
        nightDim = nightWindowsLit,
        turnWash = Rect(p(45f, 70f).x, bodyY0, p(280f, 70f).x, bodyY1),
    )
    drawPath(body, color = Color(0xFF4A5A68).copy(alpha = 0.18f), style = Stroke(width = sx(1.4f)))
    drawHeroWheelWellInnerShadow(frontC, wellR, bodyBottomY)
    drawHeroWheelWellInnerShadow(rearC, wellR, bodyBottomY)
    drawHeroWheelArchBand(frontC, wellR, sx(HeroFenderBand), HeroVehicleColors.stripe, bodyBottomY)
    drawHeroWheelArchBand(rearC, wellR, sx(HeroFenderBand), HeroVehicleColors.stripe, bodyBottomY)

    // Cab / living divider
    drawLine(
        color = HeroVehicleColors.frame,
        start = p(340f, 70f),
        end = p(340f, 390f),
        strokeWidth = sx(1.8f),
    )

    drawRoundRect(
        color = HeroVehicleColors.stripe,
        topLeft = p(160f, 285f),
        size = Size(sx(760f), sx(15f)),
        cornerRadius = CornerRadius(sx(5f), sx(5f)),
    )

    drawRoundRect(
        color = Color(0xFFB8C8D4),
        topLeft = p(600f, 48f),
        size = Size(sx(50f), sx(14f)),
        cornerRadius = CornerRadius(sx(3f), sx(3f)),
    )

    // Cab side window — darker top / lighter bottom + sky reflection clipped inside
    val cabWin = Path().apply {
        val r = sx(10f)
        moveTo(p(250f, 140f).x + r, p(250f, 140f).y)
        lineTo(p(315f, 140f).x - r, p(315f, 140f).y)
        quadraticBezierTo(p(315f, 140f).x, p(315f, 140f).y, p(315f, 140f).x, p(315f, 140f).y + r)
        lineTo(p(315f, 245f).x, p(315f, 245f).y - r)
        quadraticBezierTo(p(315f, 245f).x, p(315f, 245f).y, p(315f, 245f).x - r, p(315f, 245f).y)
        lineTo(p(170f, 245f).x + r, p(170f, 245f).y)
        quadraticBezierTo(p(170f, 245f).x, p(170f, 245f).y, p(178f, 235f).x, p(178f, 235f).y)
        lineTo(p(242f, 148f).x, p(242f, 148f).y)
        quadraticBezierTo(p(250f, 140f).x, p(250f, 140f).y, p(250f, 140f).x + r, p(250f, 140f).y)
        close()
    }
    val cabTop = lerpColor(cabGlass, Color.Black, 0.18f)
    val cabBot = lerpColor(cabGlass, Color.White, 0.14f)
    drawPath(
        cabWin,
        brush = Brush.verticalGradient(
            colorStops = arrayOf(
                0.00f to cabTop,
                0.45f to cabGlass,
                1.00f to cabBot,
            ),
            startY = p(250f, 140f).y,
            endY = p(170f, 245f).y,
        ),
    )
    clipPath(cabWin) {
        drawRect(
            brush = Brush.verticalGradient(
                colorStops = arrayOf(
                    0.00f to skyTint.copy(alpha = 0.26f),
                    0.40f to skyTint.copy(alpha = 0.08f),
                    0.58f to Color(0xFF2A4A5C).copy(alpha = 0.12f),
                    1.00f to Color.Transparent,
                ),
                startY = p(250f, 140f).y,
                endY = p(170f, 245f).y,
            ),
            topLeft = p(165f, 135f),
            size = Size(sx(160f), sx(120f)),
        )
        drawRect(
            color = Color.Black.copy(alpha = 0.18f),
            topLeft = p(175f, 142f),
            size = Size(sx(135f), sx(12f)),
        )
    }
    drawPath(cabWin, color = Color(0xFF2A3340).copy(alpha = 0.72f), style = Stroke(width = sx(1.8f)))

    // Side mirror — attachment contact shadow + arm + housing
    drawOval(
        color = Color.Black.copy(alpha = 0.20f),
        topLeft = Offset(p(165f, 228f).x, p(165f, 228f).y),
        size = Size(sx(14f), sx(8f)),
    )
    drawLine(
        color = Color(0xFF6E7A88),
        start = p(172f, 230f),
        end = p(138f, 222f),
        strokeWidth = sx(4f),
        cap = StrokeCap.Round,
    )
    drawRoundRect(
        color = HeroVehicleColors.handle,
        topLeft = p(118f, 205f),
        size = Size(sx(24f), sx(34f)),
        cornerRadius = CornerRadius(sx(4f), sx(4f)),
    )
    drawOval(
        color = Color.Black.copy(alpha = 0.18f),
        topLeft = Offset(p(120f, 236f).x, p(120f, 236f).y),
        size = Size(sx(20f), sx(6f)),
    )

    // Driver door seam: soft lit/shade edges + handle contact
    val cabDoorLit = light.dirX >= 0f
    drawLine(
        color = if (cabDoorLit) Color.White.copy(alpha = 0.20f) else Color.Black.copy(alpha = 0.14f),
        start = p(298f, 248f),
        end = p(298f, 392f),
        strokeWidth = sx(1.8f),
        cap = StrokeCap.Round,
    )
    drawLine(
        color = if (cabDoorLit) Color.Black.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.16f),
        start = p(302f, 248f),
        end = p(302f, 392f),
        strokeWidth = sx(1.6f),
        cap = StrokeCap.Round,
    )
    drawOval(
        color = Color.Black.copy(alpha = 0.20f),
        topLeft = Offset(p(278f, 305f).x, p(278f, 305f).y),
        size = Size(sx(14f), sx(5f)),
    )
    drawCircle(HeroVehicleColors.handle, radius = sx(7f), center = p(285f, 300f))

    drawOval(
        color = HeroVehicleColors.headlight,
        topLeft = p(58f, 298f),
        size = Size(sx(28f), sx(22f)),
    )
    // Bumper — contact shade at body join
    drawOval(
        color = Color.Black.copy(alpha = 0.16f),
        topLeft = Offset(p(48f, 350f).x, p(48f, 350f).y),
        size = Size(sx(58f), sx(8f)),
    )
    drawRoundRect(
        color = bodyBot,
        topLeft = p(45f, 355f),
        size = Size(sx(65f), sx(30f)),
        cornerRadius = CornerRadius(sx(6f), sx(6f)),
    )

    drawHeroLivingWindow(p(410f, 145f), Size(sx(165f), sx(110f)), sx(14f), livingGlass, nightWindowsLit, skyTint)
    drawHeroLivingWindow(p(780f, 150f), Size(sx(115f), sx(100f)), sx(13f), livingGlass, nightWindowsLit, skyTint)

    drawHeroVehicleDoor(
        topLeft = p(625f, 135f),
        size = Size(sx(110f), sx(178f)),
        corner = sx(14f),
        lightFromLeft = light.dirX >= 0f,
        handle = p(710f, 255f),
        handleR = sx(7f),
    )

    drawRoundRect(HeroVehicleColors.stop, p(938f, 200f), Size(sx(10f), sx(36f)), CornerRadius(sx(2f), sx(2f)))
    drawRoundRect(HeroVehicleColors.amber, p(938f, 245f), Size(sx(10f), sx(16f)), CornerRadius(sx(2f), sx(2f)))

    val acRect = Rect(p(600f, 48f).x, p(600f, 48f).y, p(650f, 48f).x, p(600f, 62f).y)
    drawHeroRoofSnow(
        left = p(340f, 65f).x,
        right = p(905f, 65f).x,
        topY = p(340f, 65f).y,
        thickness = sx(10f),
        snowCover = snowCover,
        light = light,
        equipment = listOf(acRect),
    )
    // Thin ledge snow above windshield brow if present
    if (snowCover >= 0.12f) {
        drawLine(
            light.litColor(HeroSnowLit, 0.40f).copy(alpha = 0.55f + snowCover * 0.25f),
            p(210f, 125f),
            p(245f, 125f),
            strokeWidth = sx(4f),
            cap = StrokeCap.Round,
        )
    }

    return HeroVehicleMetrics(
        contentLeft = MotorContentLeft,
        contentRight = MotorContentRight,
        contentTop = 48f,
        groundY = HeroGroundY,
        linkX = MotorLinkX,
        linkY = MotorLinkY,
    )
}

/**
 * Close-up vehicle-only canvas for checking wheel wells / hitch / cab details.
 * Keeps the same design geometry and uniform scale; no card chrome.
 */
@Composable
fun HeroVehicleCloseupPreview(
    vehicleType: HeroVehicleType,
    nightWindowsLit: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .background(Color(0xFF3D5A68)),
    ) {
        val contentLeft = when (vehicleType) {
            HeroVehicleType.Towable -> TowContentLeft
            HeroVehicleType.Motorhome -> MotorContentLeft
        }
        val contentRight = when (vehicleType) {
            HeroVehicleType.Towable -> TowContentRight
            HeroVehicleType.Motorhome -> MotorContentRight
        }
        val contentW = contentRight - contentLeft
        val scale = size.width / contentW
        val origin = Offset(-contentLeft * scale, size.height - HeroGroundY * scale)
        when (vehicleType) {
            HeroVehicleType.Towable -> drawTowableCaravan(
                origin = origin,
                scale = scale,
                nightWindowsLit = nightWindowsLit,
                shadow = Color.Black.copy(alpha = 0.10f),
            )
            HeroVehicleType.Motorhome -> drawMotorhome(
                origin = origin,
                scale = scale,
                nightWindowsLit = nightWindowsLit,
                shadow = Color.Black.copy(alpha = 0.10f),
            )
        }
    }
}

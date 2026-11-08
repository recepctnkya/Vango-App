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

/** Layout anchors shared by Canvas drawing and status text placement. */
internal data class HeroLayout(
    val phoneCenter: Offset,
    val phoneSize: Float,
    /** Phone↔vehicle link ends here (tow hitch tip or motorhome bumper tip). */
    val linkTarget: Offset,
    /** Top-left of vehicle local design canvas. */
    val vehicleOriginX: Float,
    val vehicleOriginY: Float,
    /** Uniform px-per-design-unit. */
    val vehicleScale: Float,
    val baselineY: Float,
)

/** Shared 1000×500 design canvas (uniform scale into hero slot). */
internal const val HeroDesignW = 1000f
internal const val HeroDesignH = 500f
/** Main tire ground contact in design space. */
internal const val HeroGroundY = 465f
internal const val HeroWheelCy = 400f
internal const val HeroWheelR = 65f
internal const val HeroWellR = 72f
internal const val HeroFenderBand = 9f
internal const val HeroRimR = 34f
internal const val HeroHubR = 11f

/** Towable hitch coupler — phone link endpoint. */
internal const val TowLinkX = 40f
internal const val TowLinkY = 395f
internal const val TowContentLeft = 25f
internal const val TowContentRight = 955f

/** Motorhome front bumper tip — phone link endpoint. */
internal const val MotorLinkX = 45f
internal const val MotorLinkY = 370f
internal const val MotorContentLeft = 40f
internal const val MotorContentRight = 955f

/** Target vehicle width as fraction of card (~40–43%). */
internal const val HeroVehicleWidthFrac = 0.425f

internal fun computeHeroLayout(w: Float, h: Float, vehicleType: HeroVehicleType): HeroLayout {
    val slotRight = w * 0.95f
    val wheelBottomY = h * 0.90f
    val targetW = w * HeroVehicleWidthFrac
    val phoneSize = min(w, h) * 0.22f

    return when (vehicleType) {
        HeroVehicleType.Towable -> {
            val contentW = TowContentRight - TowContentLeft
            val scale = targetW / contentW
            val originX = slotRight - TowContentRight * scale
            val originY = wheelBottomY - HeroGroundY * scale
            val linkTarget = Offset(originX + TowLinkX * scale, originY + TowLinkY * scale)
            val phoneCenter = Offset(w * 0.10f, linkTarget.y)
            HeroLayout(phoneCenter, phoneSize, linkTarget, originX, originY, scale, linkTarget.y)
        }
        HeroVehicleType.Motorhome -> {
            val contentW = MotorContentRight - MotorContentLeft
            val scale = targetW / contentW
            val originX = slotRight - MotorContentRight * scale
            val originY = wheelBottomY - HeroGroundY * scale
            val linkTarget = Offset(originX + MotorLinkX * scale, originY + MotorLinkY * scale)
            val phoneCenter = Offset(w * 0.10f, linkTarget.y)
            HeroLayout(phoneCenter, phoneSize, linkTarget, originX, originY, scale, linkTarget.y)
        }
    }
}

package com.hextech.vango

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Electric motor (side view) for dashboard footer — drawn to match the neon motor mockup:
 * shaft left, finned body, junction box on top, rounded fan cover right, two-tier base.
 */
val MotorEngineIcon: ImageVector
    get() {
        _motorEngineIcon?.let { return it }
        _motorEngineIcon = ImageVector.Builder(
            name = "ElectricMotor",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            val stroke = SolidColor(Color.White)
            val outer = 1.7f
            val inner = 1.35f

            // Lower wide base
            path(
                fill = null,
                stroke = stroke,
                strokeLineWidth = outer,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(4.0f, 19.4f)
                horizontalLineTo(20.0f)
                verticalLineTo(21.0f)
                horizontalLineTo(4.0f)
                close()
            }
            // Upper base plate
            path(
                fill = null,
                stroke = stroke,
                strokeLineWidth = outer,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(5.2f, 17.6f)
                horizontalLineTo(18.8f)
                verticalLineTo(19.4f)
                horizontalLineTo(5.2f)
                close()
            }

            // Fan cover (right, rounded)
            path(
                fill = null,
                stroke = stroke,
                strokeLineWidth = outer,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(16.0f, 6.6f)
                horizontalLineTo(18.2f)
                curveTo(19.5f, 6.6f, 20.5f, 7.6f, 20.5f, 8.9f)
                verticalLineTo(15.3f)
                curveTo(20.5f, 16.6f, 19.5f, 17.6f, 18.2f, 17.6f)
                horizontalLineTo(16.0f)
                close()
            }
            // Fan cover inner arc hint
            path(
                fill = null,
                stroke = stroke,
                strokeLineWidth = inner,
                strokeLineCap = StrokeCap.Round,
            ) {
                moveTo(18.0f, 8.2f)
                curveTo(18.7f, 8.6f, 19.2f, 9.5f, 19.2f, 10.6f)
                verticalLineTo(13.6f)
                curveTo(19.2f, 14.7f, 18.7f, 15.6f, 18.0f, 16.0f)
            }

            // Main body
            path(
                fill = null,
                stroke = stroke,
                strokeLineWidth = outer,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(7.0f, 6.6f)
                horizontalLineTo(16.0f)
                verticalLineTo(17.6f)
                horizontalLineTo(7.0f)
                close()
            }

            // Cooling fins
            path(
                fill = null,
                stroke = stroke,
                strokeLineWidth = 1.25f,
                strokeLineCap = StrokeCap.Round,
            ) {
                moveTo(8.1f, 8.3f); horizontalLineTo(14.9f)
                moveTo(8.1f, 9.5f); horizontalLineTo(14.9f)
                moveTo(8.1f, 10.7f); horizontalLineTo(14.9f)
                moveTo(8.1f, 11.9f); horizontalLineTo(14.9f)
                moveTo(8.1f, 13.1f); horizontalLineTo(14.9f)
                moveTo(8.1f, 14.3f); horizontalLineTo(14.9f)
                moveTo(8.1f, 15.5f); horizontalLineTo(14.9f)
            }

            // Junction / terminal box
            path(
                fill = null,
                stroke = stroke,
                strokeLineWidth = outer,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(10.0f, 3.8f)
                horizontalLineTo(13.0f)
                verticalLineTo(6.6f)
                horizontalLineTo(10.0f)
                close()
            }

            // Drive shaft (left, stepped)
            path(
                fill = null,
                stroke = stroke,
                strokeLineWidth = outer,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(7.0f, 10.0f)
                horizontalLineTo(5.2f)
                verticalLineTo(14.2f)
                horizontalLineTo(7.0f)
                moveTo(5.2f, 10.8f)
                horizontalLineTo(3.4f)
                verticalLineTo(13.4f)
                horizontalLineTo(5.2f)
            }
        }.build()
        return _motorEngineIcon!!
    }

private var _motorEngineIcon: ImageVector? = null

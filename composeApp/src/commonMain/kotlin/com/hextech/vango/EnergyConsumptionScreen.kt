package com.hextech.vango

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import testvango.composeapp.generated.resources.Res
import testvango.composeapp.generated.resources.energy_avg
import testvango.composeapp.generated.resources.energy_avg_hint
import testvango.composeapp.generated.resources.energy_cat_climate
import testvango.composeapp.generated.resources.energy_cat_lighting
import testvango.composeapp.generated.resources.energy_cat_other
import testvango.composeapp.generated.resources.energy_cat_outlets
import testvango.composeapp.generated.resources.energy_cat_water
import testvango.composeapp.generated.resources.energy_chart_title
import testvango.composeapp.generated.resources.energy_dist_title
import testvango.composeapp.generated.resources.energy_est_remaining
import testvango.composeapp.generated.resources.energy_est_remaining_hint
import testvango.composeapp.generated.resources.energy_instant
import testvango.composeapp.generated.resources.energy_max
import testvango.composeapp.generated.resources.energy_max_hint
import testvango.composeapp.generated.resources.energy_metering_unavailable_hint
import testvango.composeapp.generated.resources.energy_metering_unavailable_title
import testvango.composeapp.generated.resources.energy_battery_from_voltage
import testvango.composeapp.generated.resources.metric_not_measured
import testvango.composeapp.generated.resources.energy_period_daily
import testvango.composeapp.generated.resources.energy_period_monthly
import testvango.composeapp.generated.resources.energy_period_weekly
import testvango.composeapp.generated.resources.energy_period_yearly
import testvango.composeapp.generated.resources.energy_remaining
import testvango.composeapp.generated.resources.energy_runtime
import testvango.composeapp.generated.resources.energy_runtime_hint
import testvango.composeapp.generated.resources.energy_today
import testvango.composeapp.generated.resources.energy_today_total

private val EnBg: Color @Composable get() = LocalAppChrome.current.bg
private val EnPanel: Color @Composable get() = LocalAppChrome.current.panel
private val EnCard: Color @Composable get() = LocalAppChrome.current.card
private val EnCyan = Color(0xFF2EC5FF)
private val EnText: Color @Composable get() = LocalAppChrome.current.text
private val EnMuted: Color @Composable get() = LocalAppChrome.current.muted
private val EnBorder: Color @Composable get() = LocalAppChrome.current.border
private val EnTrack: Color @Composable get() = LocalAppChrome.current.track
private val EnYellow = Color(0xFFF5C542)
private val EnGreen = Color(0xFF3DDC84)
private val EnPurple = Color(0xFFB794F6)
private val EnBlue = Color(0xFF4DA3FF)
private val EnGray = Color(0xFF9AA5B5)

private enum class EnergyPeriod { Daily, Weekly, Monthly, Yearly }

@Composable
fun EnergyConsumptionScreen(
    data: VanGoData,
    modifier: Modifier = Modifier,
) {
    val hasVolt = data.volt > 0f
    val batPct = if (hasVolt) {
        (((data.volt - 11.5f) / (13.0f - 11.5f)) * 100f).roundToInt().coerceIn(0, 100)
    } else {
        null
    }
    val notMeasured = stringResource(Res.string.metric_not_measured)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(EnBg)
            .padding(
                horizontal = if (isPhoneUi()) 0.dp else 10.dp,
                vertical = if (isPhoneUi()) 0.dp else 6.dp,
            )
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(EnCard, RoundedCornerShape(16.dp))
                .border(1.dp, EnBorder, RoundedCornerShape(16.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                stringResource(Res.string.energy_metering_unavailable_title),
                color = EnCyan,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                stringResource(Res.string.energy_metering_unavailable_hint),
                color = EnMuted,
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )
        }

        EnergyMetricCard(
            modifier = Modifier.fillMaxWidth().height(120.dp),
            title = stringResource(Res.string.energy_remaining),
            value = batPct?.let { "$it %" } ?: "—",
            subtitle = if (hasVolt) {
                stringResource(Res.string.energy_battery_from_voltage)
            } else {
                notMeasured
            },
        ) {
            EnergyRingGauge(
                progress = (batPct ?: 0) / 100f,
                centerLabel = batPct?.toString() ?: "—",
                centerUnit = "%",
                accent = EnCyan,
                modifier = Modifier.size(72.dp),
            )
        }

        EnergyMetricCard(
            modifier = Modifier.fillMaxWidth().height(96.dp),
            title = stringResource(Res.string.energy_instant),
            value = notMeasured,
            subtitle = stringResource(Res.string.energy_metering_unavailable_title),
        ) { }

        EnergyMetricCard(
            modifier = Modifier.fillMaxWidth().height(96.dp),
            title = stringResource(Res.string.energy_today),
            value = notMeasured,
            subtitle = stringResource(Res.string.energy_today_total),
        ) { }

        Row(
            modifier = Modifier.fillMaxWidth().height(88.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            EnergyStatCard(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                title = stringResource(Res.string.energy_avg),
                value = notMeasured,
                hint = stringResource(Res.string.energy_avg_hint),
                icon = Icons.Filled.ShowChart,
            )
            EnergyStatCard(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                title = stringResource(Res.string.energy_max),
                value = notMeasured,
                hint = stringResource(Res.string.energy_max_hint),
                icon = Icons.Filled.TrendingUp,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().height(88.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            EnergyStatCard(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                title = stringResource(Res.string.energy_runtime),
                value = notMeasured,
                hint = stringResource(Res.string.energy_runtime_hint),
                icon = Icons.Filled.AccessTime,
            )
            EnergyStatCard(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                title = stringResource(Res.string.energy_est_remaining),
                value = notMeasured,
                hint = stringResource(Res.string.energy_est_remaining_hint),
                icon = Icons.Filled.HourglassBottom,
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}


private data class EnergySlice(
    val nameRes: StringResource,
    val kwh: Float,
    val fraction: Float,
    val color: Color,
    val icon: ImageVector,
)

@Composable
private fun EnergyMetricCard(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    trailingTop: (@Composable () -> Unit)? = null,
    visual: @Composable () -> Unit,
) {
    Row(
        modifier = modifier
            .background(EnCard, RoundedCornerShape(16.dp))
            .border(1.dp, EnBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    title.uppercase(),
                    color = EnMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.6.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                trailingTop?.invoke()
            }
            Text(
                value,
                color = EnText,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            Text(
                subtitle,
                color = EnCyan,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
        }
        visual()
    }
}

@Composable
private fun EnergyStatCard(
    title: String,
    value: String,
    hint: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(EnCard, RoundedCornerShape(14.dp))
            .border(1.dp, EnBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                title.uppercase(),
                color = EnMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Icon(icon, null, tint = EnCyan, modifier = Modifier.size(20.dp))
        }
        Text(
            value,
            color = EnText,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
        Text(
            hint,
            color = EnCyan,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun EnergyChartCard(
    period: EnergyPeriod,
    onPeriod: (EnergyPeriod) -> Unit,
    series: List<Float>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(EnCard, RoundedCornerShape(16.dp))
            .border(1.dp, EnBorder, RoundedCornerShape(16.dp))
            .padding(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(Res.string.energy_chart_title).uppercase(),
                color = EnMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.6.sp,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                EnergyPeriodChip(stringResource(Res.string.energy_period_daily), period == EnergyPeriod.Daily) {
                    onPeriod(EnergyPeriod.Daily)
                }
                EnergyPeriodChip(stringResource(Res.string.energy_period_weekly), period == EnergyPeriod.Weekly) {
                    onPeriod(EnergyPeriod.Weekly)
                }
                EnergyPeriodChip(stringResource(Res.string.energy_period_monthly), period == EnergyPeriod.Monthly) {
                    onPeriod(EnergyPeriod.Monthly)
                }
                EnergyPeriodChip(stringResource(Res.string.energy_period_yearly), period == EnergyPeriod.Yearly) {
                    onPeriod(EnergyPeriod.Yearly)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        EnergyAreaChart(
            values = series,
            yMax = 2f,
            xLabels = when (period) {
                EnergyPeriod.Daily -> listOf("00:00", "06:00", "12:00", "18:00", "24:00")
                EnergyPeriod.Weekly -> listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz")
                EnergyPeriod.Monthly -> listOf("1", "8", "15", "22", "30")
                EnergyPeriod.Yearly -> listOf("Oca", "Mar", "May", "Tem", "Eyl", "Kas")
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        )
    }
}

@Composable
private fun EnergyPeriodChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Text(
        label,
        color = if (selected) EnCyan else EnMuted,
        fontSize = 12.sp,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        modifier = Modifier
            .background(
                if (selected) EnCyan.copy(alpha = 0.12f) else Color.Transparent,
                RoundedCornerShape(8.dp),
            )
            .border(
                1.dp,
                if (selected) EnCyan.copy(alpha = 0.7f) else EnBorder,
                RoundedCornerShape(8.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

@Composable
private fun EnergyDistributionCard(
    totalKwh: Float,
    slices: List<EnergySlice>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(EnCard, RoundedCornerShape(16.dp))
            .border(1.dp, EnBorder, RoundedCornerShape(16.dp))
            .padding(12.dp),
    ) {
        Text(
            stringResource(Res.string.energy_dist_title).uppercase(),
            color = EnMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.6.sp,
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .weight(0.95f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center,
            ) {
                EnergyDonut(
                    slices = slices.map { it.color to it.fraction },
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .fillMaxHeight(0.92f),
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        energyOneDecimal(totalKwh),
                        color = EnText,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text("kWh", color = EnMuted, fontSize = 12.sp)
                }
            }
            Column(
                modifier = Modifier
                    .weight(1.15f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceEvenly,
            ) {
                slices.forEach { slice ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .background(slice.color.copy(alpha = 0.18f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(slice.icon, null, tint = slice.color, modifier = Modifier.size(15.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(slice.nameRes),
                                color = EnText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                "${energyOneDecimal(slice.kwh)} kWh  %${(slice.fraction * 100).roundToInt()}",
                                color = EnMuted,
                                fontSize = 11.sp,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EnergyRingGauge(
    progress: Float,
    centerLabel: String,
    centerUnit: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        val track = EnTrack
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 7.dp.toPx()
            val pad = stroke / 2f
            drawArc(
                color = track,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(pad, pad),
                size = Size(size.width - stroke, size.height - stroke),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            drawArc(
                color = accent,
                startAngle = -90f,
                sweepAngle = 360f * progress.coerceIn(0f, 1f),
                useCenter = false,
                topLeft = Offset(pad, pad),
                size = Size(size.width - stroke, size.height - stroke),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(centerLabel, color = EnText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(centerUnit, color = EnMuted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun EnergyMiniBars(
    values: List<Float>,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val gap = 4.dp.toPx()
        val barW = (size.width - gap * (values.size - 1)) / values.size
        values.forEachIndexed { i, v ->
            val h = size.height * v.coerceIn(0f, 1f)
            val x = i * (barW + gap)
            drawRoundRect(
                color = accent.copy(alpha = 0.35f + v * 0.55f),
                topLeft = Offset(x, size.height - h),
                size = Size(barW, h),
                cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
            )
        }
    }
}

@Composable
private fun EnergyAreaChart(
    values: List<Float>,
    yMax: Float,
    xLabels: List<String>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            val grid = EnBorder
            Canvas(modifier = Modifier.fillMaxSize().padding(start = 28.dp, end = 6.dp, top = 4.dp, bottom = 4.dp)) {
                val maxY = max(yMax, values.maxOrNull() ?: yMax)
                val n = values.size.coerceAtLeast(2)
                fun pt(i: Int, v: Float): Offset {
                    val x = size.width * (i / (n - 1f))
                    val y = size.height * (1f - (v / maxY).coerceIn(0f, 1f))
                    return Offset(x, y)
                }
                // Grid
                for (g in 0..4) {
                    val y = size.height * (g / 4f)
                    drawLine(
                        color = grid,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.dp.toPx(),
                    )
                }
                val line = Path()
                val fill = Path()
                values.forEachIndexed { i, v ->
                    val p = pt(i, v)
                    if (i == 0) {
                        line.moveTo(p.x, p.y)
                        fill.moveTo(p.x, size.height)
                        fill.lineTo(p.x, p.y)
                    } else {
                        line.lineTo(p.x, p.y)
                        fill.lineTo(p.x, p.y)
                    }
                }
                fill.lineTo(size.width, size.height)
                fill.close()
                drawPath(
                    path = fill,
                    brush = Brush.verticalGradient(
                        colors = listOf(EnCyan.copy(alpha = 0.35f), EnCyan.copy(alpha = 0.02f)),
                    ),
                )
                drawPath(
                    path = line,
                    color = EnCyan,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(28.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                listOf("2.0", "1.5", "1.0", "0.5", "0").forEach {
                    Text(it, color = EnMuted, fontSize = 9.sp, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 28.dp, end = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            xLabels.forEach {
                Text(it, color = EnMuted, fontSize = 10.sp)
            }
        }
        Text(
            "kW",
            color = EnMuted,
            fontSize = 10.sp,
            modifier = Modifier.padding(start = 2.dp, top = 2.dp),
        )
    }
}

@Composable
private fun EnergyDonut(
    slices: List<Pair<Color, Float>>,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val stroke = min(size.minDimension * 0.18f, 18.dp.toPx())
        val pad = stroke / 2f
        var start = -90f
        slices.forEach { (color, frac) ->
            val sweep = 360f * frac.coerceAtLeast(0f)
            drawArc(
                color = color,
                startAngle = start,
                sweepAngle = sweep - 1.5f,
                useCenter = false,
                topLeft = Offset(pad, pad),
                size = Size(size.width - stroke, size.height - stroke),
                style = Stroke(width = stroke, cap = StrokeCap.Butt),
            )
            start += sweep
        }
    }
}

private fun energySeriesFor(period: EnergyPeriod): List<Float> = when (period) {
    EnergyPeriod.Daily -> List(24) { 0f }
    EnergyPeriod.Weekly -> List(7) { 0f }
    EnergyPeriod.Monthly -> List(8) { 0f }
    EnergyPeriod.Yearly -> List(12) { 0f }
}

private fun formatEnergyHours(hours: Float): String {
    val h = hours.toInt().coerceAtLeast(0)
    val m = ((hours - h) * 60f).roundToInt().coerceIn(0, 59)
    return "$h sa ${m.toString().padStart(2, '0')} dk"
}

private fun energyOneDecimal(v: Float): String {
    val t = (v * 10f).roundToInt() / 10f
    val whole = t.toInt()
    val frac = ((t - whole) * 10f).roundToInt().coerceIn(0, 9)
    return "$whole.$frac"
}

private fun energyTwoDecimal(v: Float): String {
    val t = (v * 100f).roundToInt() / 100f
    val whole = t.toInt()
    val frac = ((t - whole) * 100f).roundToInt().coerceIn(0, 99)
    return "$whole.${frac.toString().padStart(2, '0')}"
}

private fun energyKw(v: Float): String = "${energyTwoDecimal(v)} kW"

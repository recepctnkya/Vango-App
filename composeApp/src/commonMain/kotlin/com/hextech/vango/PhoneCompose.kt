package com.hextech.vango

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Slider
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.PI
import kotlin.math.roundToInt
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.delay
import testvango.composeapp.generated.resources.Res
import testvango.composeapp.generated.resources.panel_sensor_humidity
import testvango.composeapp.generated.resources.panel_sensor_hum_label
import testvango.composeapp.generated.resources.panel_sensor_in_temp
import testvango.composeapp.generated.resources.panel_sensor_temp_humidity_title
import testvango.composeapp.generated.resources.panel_sensor_temp_label
import testvango.composeapp.generated.resources.color_picker_apply
import testvango.composeapp.generated.resources.color_picker_close
import testvango.composeapp.generated.resources.color_picker_dimmer
import testvango.composeapp.generated.resources.color_picker_saved_button
import testvango.composeapp.generated.resources.color_picker_title
import testvango.composeapp.generated.resources.rgb_icon

/** Soft live dimmer writes: UI updates every frame; device gets throttled percent updates. */
private const val DIMMER_LIVE_SEND_INTERVAL_MS = 90L

expect fun createColorWheelBitmap(diameter: Int): androidx.compose.ui.graphics.ImageBitmap

/** Opaque sRGB color from 8-bit components (reliable in Canvas / backgrounds). */
private fun composeColorRgb(r: Int, g: Int, b: Int): Color {
    val rr = r and 0xFF
    val gg = g and 0xFF
    val bb = b and 0xFF
    return Color((0xFF shl 24) or (rr shl 16) or (gg shl 8) or bb)
}

/** Readable label on top of RGB preview (center “UYGULA” text). */
private fun contrastLabelColorForRgb(r: Int, g: Int, b: Int): Color {
    val lum = (r * 299 + g * 587 + b * 114) / 1000
    return if (lum > 186) Color(0xFF1A1A1A) else Color.White
}

fun toRadians(deg: Double): Double = deg / 180.0 * PI

fun toDegrees(rad: Double): Double = rad * 180.0 / PI

/** HSV: h 0–360°, s/v 0–1 → RGB 0–255 */
private fun hsvToRgb(h: Float, s: Float, v: Float): Triple<Int, Int, Int> {
    val hh = ((h % 360f) + 360f) % 360f
    val c = v * s
    val x = c * (1 - kotlin.math.abs((hh / 60f % 2f) - 1f))
    val m = v - c
    val (rp, gp, bp) = when {
        hh < 60f -> Triple(c, x, 0f)
        hh < 120f -> Triple(x, c, 0f)
        hh < 180f -> Triple(0f, c, x)
        hh < 240f -> Triple(0f, x, c)
        hh < 300f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    return Triple(
        ((rp + m) * 255).toInt().coerceIn(0, 255),
        ((gp + m) * 255).toInt().coerceIn(0, 255),
        ((bp + m) * 255).toInt().coerceIn(0, 255)
    )
}

private fun rgbToHsv(r: Int, g: Int, b: Int): Triple<Float, Float, Float> {
    val rf = r / 255f
    val gf = g / 255f
    val bf = b / 255f
    val max = maxOf(rf, gf, bf)
    val min = minOf(rf, gf, bf)
    val delta = max - min
    val h = when {
        delta < 1e-5f -> 0f
        max == rf -> ((60f * ((gf - bf) / delta) + 360f) % 360f)
        max == gf -> (60f * ((bf - rf) / delta) + 120f)
        else -> (60f * ((rf - gf) / delta) + 240f)
    }
    val s = if (max <= 1e-5f) 0f else delta / max
    val v = max
    return Triple(h, s, v)
}

@Composable
fun VerticalSliderController(
    index: Int
) {
    val data by vanGoData.collectAsState()
    
    val enabled = data.numOfD > index
    if(enabled) {
        val safeIndex = data.getSafeDimsName(index).toInt()
        val stringRes = getAppContext().vanGO.typeOfStringDim(safeIndex)
        val iconRes = getAppContext().vanGO.typeOfIcon(safeIndex) as DrawableResource
        
        // Get the actual device value
        val deviceValue = data.getSafeDimsData(index) / 100.0f
        
        // State for user interaction (temporary override)
        var userValue by remember { mutableStateOf<Float?>(null) }
        var isUserInteractingDrag by remember { mutableStateOf(false) }

        // --- NEW: Reset Timer Logic ---
        LaunchedEffect(isUserInteractingDrag) {
            // Only start the timer if the user is NO LONGER dragging
            // and there is a temporary userValue stored
            if (!isUserInteractingDrag && userValue != null) {
                delay(2000) // Wait for 2 seconds
                userValue = null // Reset to null so displayValue switches back to deviceValue
            }
        }
        // Level before line was turned off (so we can restore after toggle back on)
        var stashedDimBeforeOff by remember(index) { mutableStateOf<Float?>(null) }
        var dimmerLineOn by remember(index) { mutableStateOf(true) }

        // Use user value if interacting; while line is off, show stashed level; else device
        val displayValue = when {
            !dimmerLineOn && stashedDimBeforeOff != null -> stashedDimBeforeOff!!
            userValue != null -> userValue!!
            else -> deviceValue
        }
        val label = stringResource(stringRes as StringResource)
        val scope = rememberCoroutineScope()

        VerticalSliderWithReset(
            label = label,
            iconRes = iconRes,
            value = displayValue,
            portNo = index,
            dimmerLineEnabled = dimmerLineOn,
            onDimmerLineEnabledChange = { on ->
                if (!on) {
                    val level = userValue ?: deviceValue
                    stashedDimBeforeOff = level.coerceIn(0f, 1f)
                    dimmerLineOn = false
                    scope.launch {
                        try {
                            getAppContext().vanGO.writeDimmerPort(index, 0)
                        } catch (e: Exception) {
                            println("Dimmer off: ${e.message}")
                        }
                    }
                } else {
                    val restore = (stashedDimBeforeOff ?: (userValue ?: deviceValue)).coerceIn(0f, 1f)
                    stashedDimBeforeOff = null
                    dimmerLineOn = true
                    userValue = restore
                    scope.launch {
                        try {
                            val intValue = (restore * 100).toInt()
                            getAppContext().vanGO.writeDimmerPort(index, intValue)
                        } catch (e: Exception) {
                            println("Dimmer on restore: ${e.message}")
                        }
                    }
                }
            },
            onUserStart = {
                isUserInteractingDrag = true
            },
            onUserChange = { newValue ->
                userValue = newValue
            },
            onUserEnd = {
                isUserInteractingDrag = false
            }
        )
        Spacer(Modifier.height(5.dp))
    }
}

@Composable
fun VerticalSliderControllerConfig(
    index: Int
) {
    val caps by devicePanelProfile.collectAsState()
    val enabled = caps.caps.dimmers > index
    VerticalSliderConfig(enabled, index)
    Spacer(Modifier.height(5.dp))
}

@Composable
fun ColorPalette(
    enabled: Boolean,
    lightning: Boolean,
    deviceR: Int,
    deviceG: Int,
    deviceB: Int,
    onColorChange: (Int, Int, Int) -> Unit
) {
    if (!enabled) return
    var showDialog by remember { mutableStateOf(false) }

    val tileBg =
        if (lightning) composeColorRgb(deviceR, deviceG, deviceB)
        else MaterialTheme.colors.surface
    Box(
        modifier = Modifier
            .width(120.dp)
            .height(120.dp)
            .background(
                color = tileBg,
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.12f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { showDialog = true }
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(Res.drawable.rgb_icon),
            contentDescription = stringResource(Res.string.color_picker_title),
            modifier = Modifier.fillMaxSize()
        )
    }

    if (showDialog) {
        ColorPaletteDialog(
            lightning = lightning,
            deviceR = deviceR,
            deviceG = deviceG,
            deviceB = deviceB,
            onColorChange = onColorChange,
            onDismiss = { showDialog = false }
        )
    }
}

@Composable
internal fun ColorPaletteDialog(
    lightning: Boolean,
    deviceR: Int,
    deviceG: Int,
    deviceB: Int,
    onColorChange: (Int, Int, Int) -> Unit,
    onDismiss: () -> Unit,
    onLiveRgb: ((Int, Int, Int) -> Unit)? = null
) {
    var showSavedPicker by remember { mutableStateOf(false) }
    var pickVersion by remember { mutableStateOf(0) }
    var pickedTriple by remember { mutableStateOf<Triple<Int, Int, Int>?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .background(
                        color = MaterialTheme.colors.surface,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable(enabled = false) { }
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(Res.string.color_picker_title),
                        style = MaterialTheme.typography.h6,
                        color = MaterialTheme.colors.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { showSavedPicker = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(2.dp, MaterialTheme.colors.primary),
                        colors = ButtonDefaults.outlinedButtonColors(
                            backgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.14f),
                            contentColor = MaterialTheme.colors.primary
                        ),
                        contentPadding = PaddingValues(vertical = 12.dp, horizontal = 16.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.color_picker_saved_button),
                            style = MaterialTheme.typography.subtitle1,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                ColorPaletteWheelPanel(
                    lightning = lightning,
                    deviceR = deviceR,
                    deviceG = deviceG,
                    deviceB = deviceB,
                    pickVersion = pickVersion,
                    pickedTriple = pickedTriple,
                    onColorChange = onColorChange,
                    onLiveRgb = onLiveRgb
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = MaterialTheme.colors.primary,
                        contentColor = MaterialTheme.colors.onPrimary
                    ),
                    elevation = ButtonDefaults.elevation(
                        defaultElevation = 8.dp,
                        pressedElevation = 10.dp
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.color_picker_close),
                        style = MaterialTheme.typography.subtitle1,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }

    if (showSavedPicker) {
        SavedRgbColorsPickerDialog(
            onDismiss = { showSavedPicker = false },
            onPick = { preset ->
                pickedTriple = Triple(preset.r, preset.g, preset.b)
                pickVersion++
            }
        )
    }
}

@Composable
internal fun ColorPaletteWheelPanel(
    lightning: Boolean,
    deviceR: Int,
    deviceG: Int,
    deviceB: Int,
    pickVersion: Int,
    pickedTriple: Triple<Int, Int, Int>?,
    onColorChange: (Int, Int, Int) -> Unit,
    onLiveRgb: ((Int, Int, Int) -> Unit)? = null
) {
    var hue by remember { mutableStateOf(0f) }
    /** Must match device color; was fixed at 1f before → wrong preview vs selection. */
    var saturation by remember { mutableStateOf(1f) }
    var dimmer by remember { mutableStateOf(1f) }
    /** Last committed color (wheel position); restored when turning Active on again. */
    var storedRgb by remember { mutableStateOf(Triple(deviceR, deviceG, deviceB)) }
    var pickerEnabled by remember { mutableStateOf(lightning) }

    // Do not sync from BLE on every packet — that was shifting hue after Apply and stuttering the wheel.
    LaunchedEffect(Unit) {
        storedRgb = Triple(deviceR, deviceG, deviceB)
        val (h, s, v) = rgbToHsv(storedRgb.first, storedRgb.second, storedRgb.third)
        hue = h
        saturation = s
        dimmer = v
    }
    LaunchedEffect(pickVersion, pickedTriple) {
        pickedTriple?.let { (r, g, b) ->
            storedRgb = Triple(r, g, b)
            val (h, s, v) = rgbToHsv(r, g, b)
            hue = h
            saturation = s
            dimmer = v
        }
    }

    val rgb = remember(hue, saturation, dimmer) {
        hsvToRgb(
            hue,
            saturation.coerceIn(0f, 1f),
            dimmer.coerceIn(0f, 1f)
        )
    }
    val userR = rgb.first
    val userG = rgb.second
    val userB = rgb.third

    LaunchedEffect(userR, userG, userB, pickerEnabled) {
        if (pickerEnabled) {
            onLiveRgb?.invoke(userR, userG, userB)
        }
    }

    var layoutSize by remember { mutableStateOf(IntSize.Zero) }
    val layoutSizeState = rememberUpdatedState(layoutSize)
    val coroutineScope = rememberCoroutineScope()
    val passiveRingColor = MaterialTheme.colors.onSurface.copy(alpha = 0.38f)
    val passiveApplyBg = MaterialTheme.colors.onSurface.copy(alpha = 0.32f)
    val passiveApplyLabel = MaterialTheme.colors.onSurface.copy(alpha = 0.55f)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Switch(
                checked = pickerEnabled,
                onCheckedChange = { enabled ->
                    if (!enabled) {
                        storedRgb = Triple(userR, userG, userB)
                        pickerEnabled = false
                        coroutineScope.launch {
                            try {
                                getAppContext().vanGO.writeRGBPort(userR, userG, userB, 0)
                            } catch (e: Exception) {
                                println("RGB passive off: ${e.message}")
                            }
                        }
                    } else {
                        val (r, g, b) = storedRgb
                        val (h, s, v) = rgbToHsv(r, g, b)
                        hue = h
                        saturation = s
                        dimmer = v
                        pickerEnabled = true
                    }
                },
                modifier = Modifier.scale(1.45f),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colors.primary,
                    checkedTrackColor = MaterialTheme.colors.primary.copy(alpha = 0.45f)
                )
            )
        }

        Box(
            modifier = Modifier
                .size(220.dp)
                .onGloballyPositioned { layoutSize = it.size }
        ) {
            if (pickerEnabled) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(pickerEnabled) {
                            detectDragGestures(
                                onDrag = { change, _ ->
                                    val sz = layoutSizeState.value
                                    if (sz.width == 0 || sz.height == 0) return@detectDragGestures
                                    change.consume()
                                    val w = sz.width.toFloat()
                                    val h = sz.height.toFloat()
                                    val c = Offset(w / 2f, h / 2f)
                                    val pos = change.position
                                    val dx = pos.x - c.x
                                    val dy = pos.y - c.y
                                    // Same convention as knob (cos/sin on Canvas with y+ down): atan2(dy, dx).
                                    val angleDeg =
                                        Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                    hue = (angleDeg + 360f) % 360f
                                    // Ring is drawn at full saturation; keep S=1 so knob hue matches UYGULA preview
                                    saturation = 1f
                                }
                            )
                        }
                ) {
                    val c = Offset(size.width / 2f, size.height / 2f)
                    val outerR = minOf(size.width, size.height) / 2f * 0.98f
                    val strokeW = outerR * 0.22f
                    val innerR = outerR - strokeW
                    val trackMidR = (innerR + outerR) / 2f
                    // Fixed rainbow list ≠ HSV hue — green/blue on ring didn’t match hsvToRgb(hue,…).
                    // Evenly spaced hues on the ring so pixel under the knob matches UYGULA preview.
                    val ringHueSteps = 48
                    val sweepColors = List(ringHueSteps + 1) { i ->
                        val deg = (i * 360f / ringHueSteps) % 360f
                        val (rr, gg, bb) = hsvToRgb(deg, 1f, 1f)
                        composeColorRgb(rr, gg, bb)
                    }
                    drawArc(
                        brush = Brush.sweepGradient(
                            sweepColors,
                            center = c
                        ),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = Offset(c.x - outerR, c.y - outerR),
                        size = Size(outerR * 2f, outerR * 2f),
                        style = Stroke(width = strokeW)
                    )
                    val rad = Math.toRadians(hue.toDouble())
                    val ix = c.x + trackMidR * cos(rad).toFloat()
                    val iy = c.y + trackMidR * sin(rad).toFloat()
                    drawCircle(Color.White, 16f, Offset(ix, iy))
                    drawCircle(Color(0xFF555555), 11f, Offset(ix, iy))
                }
            } else {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val c = Offset(size.width / 2f, size.height / 2f)
                    val outerR = minOf(size.width, size.height) / 2f * 0.98f
                    val strokeW = outerR * 0.22f
                    val innerR = outerR - strokeW
                    val trackMidR = (innerR + outerR) / 2f
                    val sweepGrays = List(49) { passiveRingColor }
                    drawArc(
                        brush = Brush.sweepGradient(sweepGrays, center = c),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = Offset(c.x - outerR, c.y - outerR),
                        size = Size(outerR * 2f, outerR * 2f),
                        style = Stroke(width = strokeW)
                    )
                    val rad = Math.toRadians(hue.toDouble())
                    val ix = c.x + trackMidR * cos(rad).toFloat()
                    val iy = c.y + trackMidR * sin(rad).toFloat()
                    drawCircle(Color(0xFFE0E0E0), 16f, Offset(ix, iy))
                    drawCircle(Color(0xFF9E9E9E), 11f, Offset(ix, iy))
                }
            }
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(92.dp)
                    .background(
                        if (pickerEnabled) composeColorRgb(userR, userG, userB) else passiveApplyBg,
                        CircleShape
                    )
                    .border(
                        width = 1.5.dp,
                        color = if (pickerEnabled) {
                            Color.White.copy(alpha = 0.35f)
                        } else {
                            MaterialTheme.colors.onSurface.copy(alpha = 0.2f)
                        },
                        shape = CircleShape
                    )
                    .clickable(enabled = pickerEnabled) {
                        coroutineScope.launch {
                            try {
                                val success = getAppContext().vanGO.writeRGBPort(
                                    userR,
                                    userG,
                                    userB,
                                    1
                                )
                                if (success) {
                                    storedRgb = Triple(userR, userG, userB)
                                    onColorChange(userR, userG, userB)
                                }
                            } catch (e: Exception) {
                                println("RGB apply center: ${e.message}")
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(Res.string.color_picker_apply).uppercase(),
                    style = MaterialTheme.typography.button,
                    fontWeight = FontWeight.Bold,
                    color = if (pickerEnabled) {
                        contrastLabelColorForRgb(userR, userG, userB)
                    } else {
                        passiveApplyLabel
                    },
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(Res.string.color_picker_dimmer),
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.85f),
            modifier = Modifier.fillMaxWidth()
        )
        Slider(
            value = dimmer,
            onValueChange = { dimmer = it },
            valueRange = 0f..1f,
            enabled = pickerEnabled,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun PhoneUI() {
    val data by vanGoData.collectAsState()
    val scrollPortraitTop = rememberScrollState()
    val scrollPortraitIo = rememberScrollState()
    val scrollYellowLand = rememberScrollState()
    val scrollRedLand = rememberScrollState()

    @Composable
    fun PhoneYellowSection() {
        val env by bleEnvSensors.collectAsState()
        fun formatTempC(v: Int): String = "$v °C"
        fun formatHumidity(v: Int): String = "$v %"
        fun formatSensorOrDash(v: Int, fmt: (Int) -> String): String = if (v <= 0) "—" else fmt(v)
        val (rgbR, rgbG, rgbB) = data.rgb888ForUi()
        val tempVal = BleEnvSensorParser.resolveDisplayTempC(env, data)
        val humidityVal = BleEnvSensorParser.resolveDisplayHumidityPct(env, data)
        val platformChannels by panelPlatformChannels.collectAsState()
        val tempHumEnabled = shouldShowCombinedTempHumidityCard(data, env, platformChannels)
        DeviceOverviewCard()
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CombinedTempHumidityMetricCard(
                enabled = tempHumEnabled,
                tempText = formatSensorOrDash(tempVal, ::formatTempC),
                humidityText = formatSensorOrDash(humidityVal, ::formatHumidity),
            )
            ColorPalette(
                data.RGBE == "yes",
                data.getSafeRGBData(3) > 0,
                rgbR,
                rgbG,
                rgbB
            ) { r, g, b ->
                println("RGB color updated: R=$r, G=$g, B=$b")
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val channels by panelPlatformChannels.collectAsState()
            if (isWaterTankInstalled(0, channels)) {
                CircularProgressBox(
                    true,
                    "Clean Water",
                    data.getSafeSensorsDataUnsigned(2).toFloat() / 100.0f
                )
            }
            if (isWaterTankInstalled(1, channels)) {
                CircularProgressBox(
                    true,
                    "Gray Water",
                    data.getSafeSensorsDataUnsigned(3).toFloat() / 100.0f
                )
            }
            if (isWaterTankInstalled(2, channels)) {
                CircularProgressBox(
                    true,
                    "Dirty Water",
                    data.getSafeSensorsDataUnsigned(4).toFloat() / 100.0f
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        VerticalSliderController(0)
        VerticalSliderController(1)
        VerticalSliderController(2)
        VerticalSliderController(3)
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isLandscape = maxWidth > maxHeight
        val maxH = maxHeight

        if (isLandscape) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(scrollYellowLand)
                ) {
                    PhoneYellowSection()
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(scrollRedLand)
                ) {
                    InputOutputPanel(data.outDB, data.outNB)
                }
            }
        } else {
            // Portrait: top block first; I/O below in remaining space with its own vertical scroll
            Column(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = maxH * 0.62f)
                        .verticalScroll(scrollPortraitTop)
                ) {
                    PhoneYellowSection()
                }
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollPortraitIo)
                    ) {
                        InputOutputPanel(data.outDB, data.outNB)
                    }
                }
            }
        }
    }
}

@Composable
fun CircularProgressBox(
    enabled:Boolean,
    label: String = "label",
    progress: Float = 0.9f
) {
    if (enabled.not()) return

    Box(
        modifier = Modifier
            .width(120.dp)
            .height(120.dp)
            .background(
                color = MaterialTheme.colors.surface,
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.12f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(8.dp)
    ) {
        Column(
            Modifier.matchParentSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxHeight(0.8f),
            ) {
                Box(
                    modifier = Modifier.border(6.dp, Color.Gray, CircleShape).size(75.dp, 75.dp)
                )
                CircularProgressIndicator(
                    progress = progress,
                    modifier = Modifier
                        .fillMaxSize()
                        .wrapContentSize(Alignment.Center)
                        .size(75.dp),
                    color = MaterialTheme.colors.primary,
                    strokeWidth = 6.dp,
                    strokeCap = StrokeCap.Round
                )
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.h6,
                    color = MaterialTheme.colors.primary
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.primary
            )
        }
    }
}

@Composable
fun CombinedTempHumidityMetricCard(
    enabled: Boolean,
    tempText: String,
    humidityText: String,
    modifier: Modifier = Modifier,
) {
    if (!enabled) return

    Box(
        modifier = modifier
            .width(120.dp)
            .height(120.dp)
            .background(
                color = MaterialTheme.colors.surface,
                shape = RoundedCornerShape(12.dp),
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.12f),
                shape = RoundedCornerShape(12.dp),
            )
            .padding(8.dp),
    ) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.72f),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.2f)
                        .padding(top = 8.dp),
                    text = "🌡️",
                    style = MaterialTheme.typography.h5,
                    color = MaterialTheme.colors.primary,
                )
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = tempText,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.subtitle2,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.primary,
                        maxLines = 1,
                    )
                    Text(
                        text = humidityText,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.subtitle2,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.primary,
                        maxLines = 1,
                    )
                }
            }
            Text(
                text = stringResource(Res.string.panel_sensor_temp_humidity_title),
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.primary,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun InformationBox(
    enabled:Boolean,
    label: String = "label",
    value: String,
    imageVector: ImageVector = Icons.Default.Info,
) {
    if (enabled.not()) return

    Box(
        modifier = Modifier
            .width(120.dp)
            .height(120.dp)
            .background(
                color = MaterialTheme.colors.surface,
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.12f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(8.dp)
    ) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.7f),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isTemp = label.contains("Temp", ignoreCase = true) || label.contains("sıcak", ignoreCase = true)
                val isHumidity = label.contains("hum", ignoreCase = true) || label.contains("nem", ignoreCase = true)
                if (isTemp) {
                    Text(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(0.2f)
                            .padding(top = 8.dp),
                        text = "🌡️",
                        style = MaterialTheme.typography.h5,
                        color = MaterialTheme.colors.primary
                    )
                } else if (isHumidity) {
                    Text(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(0.2f)
                            .padding(top = 8.dp),
                        text = "💧",
                        style = MaterialTheme.typography.h5,
                        color = MaterialTheme.colors.primary
                    )
                } else {
                    Icon(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(0.2f),
                        imageVector = imageVector,
                        contentDescription = "",
                        tint = MaterialTheme.colors.primary
                    )
                }
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    text = value,
                    style = MaterialTheme.typography.h6,
                    color = MaterialTheme.colors.primary
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.h6,
                color = MaterialTheme.colors.primary
            )
        }
    }
}

@Composable
fun InformationBoxConfig(
    index: Int,
    enable: Boolean = false,
    label: String = "label",
    imageVector: ImageVector = Icons.Default.Info,
) {
    var enabled by remember(index) { mutableStateOf(enable) }
    LaunchedEffect(enable, index) {
        enabled = enable
    }
    LaunchedEffect(enabled, index) {
        val cfg = uiState.value.devConfig
        if (index < 5) {
            val buf = cfg.SensorsNameBuffer.copyOf()
            buf[index] = if (enabled) 1 else 0
            uiState.value = uiState.value.copy(devConfig = cfg.copy(SensorsNameBuffer = buf))
        } else {
            uiState.value = uiState.value.copy(
                devConfig = cfg.copy(RGBEnables = if (enabled) "yes" else "no"),
            )
        }
        markPanelConfigTouched()
    }

    Box(
        modifier = Modifier
            .width(120.dp)
            .height(90.dp)
            .background(
                color = MaterialTheme.colors.surface,
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.12f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(8.dp)
    ) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.7f),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (label.contains("Temp", ignoreCase = true)) {
                    Text(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(0.2f)
                            .padding(top = 8.dp),
                        text = "🌡️", // "°C", "🌡️",
                        style = MaterialTheme.typography.h5,
                        color = MaterialTheme.colors.primary
                    )
                } else {
                    Icon(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(0.2f),
                        imageVector = imageVector,
                        contentDescription = "",
                        tint = MaterialTheme.colors.primary
                    )
                }
                SelectSwitch(enabled, {enabled = !enabled})
            }
            Text(
                text = label,
                style = MaterialTheme.typography.h6,
                color = MaterialTheme.colors.primary
            )
        }
    }
}

@Composable
fun VerticalSliderWithReset(
    label: String = "Identity",
    iconRes: DrawableResource? = null,
    value: Float = .3f,
    portNo: Int = 0,
    dimmerLineEnabled: Boolean = true,
    onDimmerLineEnabledChange: (Boolean) -> Unit = {},
    onUserStart: () -> Unit = {},
    onUserChange: (Float) -> Unit = {},
    onUserEnd: () -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    var sliderValue by remember { mutableStateOf(value) }
    var isDragging by remember { mutableStateOf(false) }
    var lastSentPercent by remember { mutableStateOf<Int?>(null) }
    var lastSendAtMs by remember { mutableStateOf(0L) }
    var pendingLiveSend by remember { mutableStateOf<Job?>(null) }
    val isLightTheme = MaterialTheme.colors.isLight
    val cardBg = if (isLightTheme) Color(0xFFF9FBFF) else MaterialTheme.colors.surface
    val cardBorder = if (isLightTheme) Color(0xFFD9E4F3) else MaterialTheme.colors.onSurface.copy(alpha = 0.12f)
    val iconChipBg = if (isLightTheme) Color(0xFFF0F5FF) else MaterialTheme.colors.primary.copy(alpha = 0.12f)
    val sliderActive = if (isLightTheme) Color(0xFF1F7AE0) else MaterialTheme.colors.primary
    val sliderInactive = if (isLightTheme) Color(0xFFCDD8EA) else MaterialTheme.colors.onSurface.copy(alpha = 0.18f)
    val percent = "${(sliderValue * 100).roundToInt()}%"

    fun writeDimmerPercent(percent0to100: Int) {
        val clamped = percent0to100.coerceIn(0, 100)
        coroutineScope.launch {
            try {
                val success = getAppContext().vanGO.writeDimmerPort(portNo, clamped)
                if (success) {
                    println("Dimmer live/write port=$portNo pct=$clamped")
                } else {
                    println("Dimmer write failed port=$portNo pct=$clamped")
                }
            } catch (e: Exception) {
                println("Error sending dimmer value: ${e.message}")
            }
        }
    }

    fun sendDimmerLive(percent0to100: Int, force: Boolean) {
        if (!dimmerLineEnabled) return
        val clamped = percent0to100.coerceIn(0, 100)
        if (!force && lastSentPercent == clamped) return
        if (force && lastSentPercent == clamped) return
        val now = Clock.System.now().toEpochMilliseconds()
        val elapsed = now - lastSendAtMs
        if (!force && lastSendAtMs > 0L && elapsed < DIMMER_LIVE_SEND_INTERVAL_MS) {
            pendingLiveSend?.cancel()
            pendingLiveSend = coroutineScope.launch {
                delay(DIMMER_LIVE_SEND_INTERVAL_MS - elapsed)
                if (lastSentPercent != clamped) {
                    lastSentPercent = clamped
                    lastSendAtMs = Clock.System.now().toEpochMilliseconds()
                    writeDimmerPercent(clamped)
                }
            }
            return
        }
        pendingLiveSend?.cancel()
        pendingLiveSend = null
        lastSentPercent = clamped
        lastSendAtMs = now
        writeDimmerPercent(clamped)
    }

    // Sync from device only when user is not dragging (avoids fighting live UI).
    LaunchedEffect(value) {
        if (!isDragging) {
            sliderValue = value
        }
    }

    Column(
        modifier = Modifier
            .alpha(if (dimmerLineEnabled) 1f else 0.55f)
            .background(cardBg, RoundedCornerShape(14.dp))
            .border(
                1.dp,
                cardBorder,
                shape = RoundedCornerShape(14.dp)
            )
            .padding(14.dp, 12.dp, 14.dp, 10.dp)
            .fillMaxWidth(1f),
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(iconChipBg, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (iconRes != null) {
                    Image(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Text(
                text = label,
                color = MaterialTheme.colors.onSurface,
                textAlign = TextAlign.Start,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.subtitle2,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = dimmerLineEnabled,
                onCheckedChange = onDimmerLineEnabledChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = sliderActive,
                    uncheckedThumbColor = if (isLightTheme) Color.White else MaterialTheme.colors.onSurface,
                    uncheckedTrackColor = sliderInactive
                )
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Slider(
                modifier = Modifier.weight(1f),
                enabled = dimmerLineEnabled,
                value = sliderValue,
                colors = androidx.compose.material.SliderDefaults.colors(
                    thumbColor = if (dimmerLineEnabled) Color.White else sliderInactive,
                    activeTrackColor = sliderActive,
                    inactiveTrackColor = sliderInactive,
                    disabledThumbColor = sliderInactive,
                    disabledActiveTrackColor = sliderInactive,
                    disabledInactiveTrackColor = sliderInactive.copy(alpha = 0.8f),
                ),
                onValueChange = { newValue ->
                    if (!isDragging) {
                        isDragging = true
                        onUserStart()
                    }
                    sliderValue = newValue
                    onUserChange(newValue)
                    sendDimmerLive((newValue * 100f).roundToInt(), force = false)
                },
                onValueChangeFinished = {
                    pendingLiveSend?.cancel()
                    pendingLiveSend = null
                    if (dimmerLineEnabled) {
                        sendDimmerLive((sliderValue * 100f).roundToInt(), force = true)
                    }
                    isDragging = false
                    onUserEnd()
                }
            )
            Text(
                text = percent,
                color = sliderActive,
                style = MaterialTheme.typography.caption,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
fun VerticalSlider(
    identity: String = "Identity",
    startValue: Float = .3f,
    portNo: Int = 0
) {
    val coroutineScope = rememberCoroutineScope()
    var newDimVAl by remember { mutableStateOf(startValue) }
    var lastSentPercent by remember { mutableStateOf<Int?>(null) }
    var lastSendAtMs by remember { mutableStateOf(0L) }
    var pendingLiveSend by remember { mutableStateOf<Job?>(null) }

    fun sendLive(percent0to100: Int, force: Boolean) {
        val clamped = percent0to100.coerceIn(0, 100)
        if (lastSentPercent == clamped) return
        val now = Clock.System.now().toEpochMilliseconds()
        val elapsed = now - lastSendAtMs
        if (!force && lastSendAtMs > 0L && elapsed < DIMMER_LIVE_SEND_INTERVAL_MS) {
            pendingLiveSend?.cancel()
            pendingLiveSend = coroutineScope.launch {
                delay(DIMMER_LIVE_SEND_INTERVAL_MS - elapsed)
                if (lastSentPercent != clamped) {
                    lastSentPercent = clamped
                    lastSendAtMs = Clock.System.now().toEpochMilliseconds()
                    runCatching { getAppContext().vanGO.writeDimmerPort(portNo, clamped) }
                }
            }
            return
        }
        pendingLiveSend?.cancel()
        pendingLiveSend = null
        lastSentPercent = clamped
        lastSendAtMs = now
        coroutineScope.launch {
            runCatching { getAppContext().vanGO.writeDimmerPort(portNo, clamped) }
        }
    }

    Column(
        modifier = Modifier
            .border(2.dp, Color.Gray, shape = RoundedCornerShape(10.dp))
            .padding(10.dp, 0.dp, 10.dp, 0.dp)
            .fillMaxWidth(1f),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                modifier = Modifier.fillMaxWidth(.2f),
                text = identity
            )
            Slider(
                modifier = Modifier.fillMaxWidth(),
                value = newDimVAl,
                onValueChange = { newValue ->
                    newDimVAl = newValue
                    sendLive((newValue * 100f).roundToInt(), force = false)
                },
                onValueChangeFinished = {
                    pendingLiveSend?.cancel()
                    pendingLiveSend = null
                    sendLive((newDimVAl * 100f).roundToInt(), force = true)
                },
            )
        }
    }
}

@Composable
fun VerticalSliderConfig(
    enabled: Boolean,
    index: Int
) {

    var swipeDirection by remember { mutableStateOf<String?>(null) }
    var dragAmount by remember { mutableStateOf(0f) }
    var boxWidth by remember { mutableStateOf(0f) }
    var cardWidth by remember { mutableStateOf(0f) }
    var showPresetDialog by remember(index) { mutableStateOf(false) }

    val ui by uiState.collectAsState()
    val profile by devicePanelProfile.collectAsState()
    val maxDims = profile.caps.dimmers
    val slotByte = ui.devConfig.DimsNameBuffer.getOrElse(index) { 0 }
    var state by remember(index) {
        mutableStateOf(
            IOStateConfig(
                enabled = slotByte != 0.toByte(),
                ioName = if (slotByte != 0.toByte()) slotByte.toInt().coerceIn(1, 18) else 1,
            ),
        )
    }
    LaunchedEffect(slotByte, index) {
        val en = slotByte != 0.toByte()
        val nm = if (en) slotByte.toInt().coerceIn(1, 18) else 1
        state = IOStateConfig(enabled = en, ioName = nm)
    }
    LaunchedEffect(state.enabled, state.ioName, index) {
        syncDimBufferToUiState(index, state.enabled, state.ioName)
    }
    val stringRes = getAppContext().vanGO.typeOfStringDim(state.ioName)
    val label = stringResource(stringRes as StringResource)
    val iconRes = getAppContext().vanGO.typeOfIcon(state.ioName) as DrawableResource
    val isLightTheme = MaterialTheme.colors.isLight
    val cardBg = if (isLightTheme) Color(0xFFF9FBFF) else MaterialTheme.colors.surface
    val cardBorder = if (isLightTheme) Color(0xFFD9E4F3) else MaterialTheme.colors.onSurface.copy(alpha = 0.12f)
    val iconChipBg = if (isLightTheme) Color(0xFFF0F5FF) else MaterialTheme.colors.primary.copy(alpha = 0.12f)
    val sliderActive = if (isLightTheme) Color(0xFF1F7AE0) else MaterialTheme.colors.primary

    Column(
        modifier = Modifier
            .background(cardBg, RoundedCornerShape(14.dp))
            .border(
                1.dp,
                cardBorder,
                shape = RoundedCornerShape(14.dp)
            )
            .padding(14.dp, 12.dp, 14.dp, 10.dp)
            .fillMaxWidth(1f)
            .onGloballyPositioned { coordinates ->
                boxWidth = coordinates.size.width.toFloat() // Capture width in pixels
                cardWidth = coordinates.size.width.toFloat()
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        swipeDirection = null }, // Reset on start
                    onDragEnd = {
                        val halfWidth = boxWidth * 0.2f
                        swipeDirection = when {
                            dragAmount > halfWidth -> "R"
                            dragAmount < -halfWidth -> "L"
                            else -> null
                        }
                        dragAmount = 0f
                    },
                    onHorizontalDrag = { change, dragAmountDelta ->
                        dragAmount += dragAmountDelta // Accumulate drag distance
                        change.consume() // Consume the event
                    }
                )
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        swipeDirection?.let {
            var cur = state.ioName

            if(it.equals("R")) {
                --cur
                if(cur < 1) {
                    cur = 8 // todo: Buraya ioMAx?
                }
            } else {
                ++cur
                if(cur > 8) {
                    cur = 1
                }
            }
            state = state.copy(ioName = cur)

            swipeDirection = null
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.subtitle2,
                color = MaterialTheme.colors.onSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            SelectSwitch(state.enabled, {
                val buf = uiState.value.devConfig.DimsNameBuffer
                if (canToggleDimmerSlot(index, state.enabled, buf, maxDims)) {
                    state = state.copy(enabled = !state.enabled)
                }
            })
        }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coordinates ->
                    boxWidth = coordinates.size.width.toFloat()
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            swipeDirection = null
                        },
                        onDragEnd = {
                            val halfWidth = cardWidth * 0.18f
                            swipeDirection = when {
                                dragAmount > halfWidth -> "R"
                                dragAmount < -halfWidth -> "L"
                                else -> null
                            }
                            dragAmount = 0f
                        },
                        onHorizontalDrag = { change, dragAmountDelta ->
                            dragAmount += dragAmountDelta
                            change.consume()
                        }
                    )
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.ArrowBack,
                contentDescription = "Previous dim icon",
                tint = MaterialTheme.colors.onSurface.copy(alpha = 0.45f),
                modifier = Modifier
                    .size(18.dp)
                    .clickable { swipeDirection = "R" }
            )
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .offset { IntOffset(dragAmount.roundToInt(), 0) }
                    .background(iconChipBg, CircleShape)
                    .clickable { showPresetDialog = true },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(iconRes),
                    contentDescription = "Dim icon",
                    modifier = Modifier.size(22.dp)
                )
            }
            Slider(
                modifier = Modifier.weight(1f),
                value = (index + 1) / 5f,
                onValueChange = {},
                enabled = false,
                colors = androidx.compose.material.SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = sliderActive,
                    inactiveTrackColor = if (isLightTheme) Color(0xFFCDD8EA) else MaterialTheme.colors.onSurface.copy(alpha = 0.18f),
                    disabledThumbColor = Color.White,
                    disabledActiveTrackColor = sliderActive.copy(alpha = 0.8f),
                    disabledInactiveTrackColor = if (isLightTheme) Color(0xFFCDD8EA) else MaterialTheme.colors.onSurface.copy(alpha = 0.18f),
                )
            )
            Icon(
                imageVector = Icons.Filled.ArrowForward,
                contentDescription = "Next dim icon",
                tint = MaterialTheme.colors.onSurface.copy(alpha = 0.45f),
                modifier = Modifier
                    .size(18.dp)
                    .clickable { swipeDirection = "L" }
            )
        }
        if (showPresetDialog) {
            OutputPresetPickerDialog(
                currentPresetId = state.ioName.coerceIn(1, 8),
                onDismiss = { showPresetDialog = false },
                onConfirm = { newId ->
                    state = state.copy(ioName = newId.coerceIn(1, 8))
                    showPresetDialog = false
                }
            )
        }
    }
}

@Composable
fun SpiralWaitingIcon(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition()
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = modifier
    ) {
        val icon = getAppContext().vanGO.getHexnetIcon()
        Image(
            painter = painterResource(icon),
            contentDescription = "Rotating and scaling image",
            modifier = Modifier
                .matchParentSize()
                .rotate(rotation) // Apply rotation
                .scale(scale) // Apply scaling
        )
    }

//    Canvas(modifier = modifier) {
//        val center = Offset(size.width / 2, size.height / 2)
//        val radius = size.minDimension / 2 * scale
//        val strokeWidth = 4.dp.toPx()
//
//        // Draw spiral effect
//        drawArc(
//            color = Color.Blue,
//            startAngle = 0f + rotation,
//            sweepAngle = 270f, // Partial arc for spiral effect
//            useCenter = false,
//            topLeft = Offset(center.x - radius, center.y - radius),
//            size = Size(radius * 2, radius * 2),
//            style = Stroke(width = strokeWidth)
//        )
//        // Second arc for layered spiral
//        drawArc(
//            color = Color.Blue.copy(alpha = 0.5f),
//            startAngle = 180f + rotation,
//            sweepAngle = 270f,
//            useCenter = false,
//            topLeft = Offset(center.x - radius * 0.7f, center.y - radius * 0.7f),
//            size = Size(radius * 1.4f, radius * 1.4f),
//            style = Stroke(width = strokeWidth)
//        )
//    }
}

@Composable
fun PanelPlatformSensorsSection(
    cfg: DevConfig,
    maxSensors: Int,
) {
    val platformChannels by panelPlatformChannels.collectAsState()
    val sensors = platformChannels?.sensors.orEmpty().take(maxSensors)
    if (sensors.isEmpty()) return

    Text(
        text = "Sensörler",
        style = MaterialTheme.typography.subtitle2,
        color = MaterialTheme.colors.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 8.dp, top = 4.dp),
    )
    sensors.forEach { sensor ->
        val enabled = isPlatformSensorEnabled(sensor, cfg.SensorsNameBuffer)
        val type = normalizePanelSensorType(sensor.sensorType)
        if (sensor.id == 0 && type == "temp_humidity") {
            CombinedTempHumidityConfigCard(
                enabled = enabled,
                label = sensor.name.ifBlank { sensorDefaultName(type, sensor.id) },
                onEnabledChange = { setPanelSensorEnabled(sensor, it) },
                onTypeChange = { setPanelSensorType(sensor.id, it) },
            )
        } else {
            PlatformSensorConfigCard(
                sensor = sensor,
                enabled = enabled,
                onEnabledChange = { setPanelSensorEnabled(sensor, it) },
                onTypeChange = { setPanelSensorType(sensor.id, it) },
            )
        }
        Box(Modifier.height(8.dp))
    }
}

@Composable
fun CombinedTempHumidityConfigCard(
    enabled: Boolean,
    label: String,
    onEnabledChange: (Boolean) -> Unit,
    onTypeChange: (String) -> Unit,
) {
    var typeMenuOpen by remember { mutableStateOf(false) }
    PanelSensorCardShell(
        enabled = enabled,
        onEnabledChange = onEnabledChange,
        leading = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("🌡️", style = MaterialTheme.typography.h5)
                Text("💧", style = MaterialTheme.typography.h5)
            }
        },
        label = label,
        footer = {
            Box {
                TextButton(onClick = { typeMenuOpen = true }) {
                    Text("Sıcaklık / Nem", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
                DropdownMenu(expanded = typeMenuOpen, onDismissRequest = { typeMenuOpen = false }) {
                    PanelSensorTypes.forEach { option ->
                        DropdownMenuItem(onClick = {
                            typeMenuOpen = false
                            onTypeChange(option)
                        }) {
                            Text(sensorTypeLabel(option))
                        }
                    }
                }
            }
        },
    )
}

@Composable
fun PlatformSensorConfigCard(
    sensor: PanelPlatformChannel,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onTypeChange: (String) -> Unit,
) {
    val type = normalizePanelSensorType(sensor.sensorType)
    var typeMenuOpen by remember(sensor.id, type) { mutableStateOf(false) }
    PanelSensorCardShell(
        enabled = enabled,
        onEnabledChange = onEnabledChange,
        leading = {
            Icon(
                imageVector = if (type == "voltage_current") Icons.Default.Bolt else Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colors.primary,
            )
        },
        label = sensor.name.ifBlank { sensorDefaultName(type, sensor.id) },
        footer = {
            Box {
                TextButton(onClick = { typeMenuOpen = true }) {
                    Text(
                        text = sensorTypeLabel(type),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
                DropdownMenu(expanded = typeMenuOpen, onDismissRequest = { typeMenuOpen = false }) {
                    PanelSensorTypes.forEach { option ->
                        DropdownMenuItem(onClick = {
                            typeMenuOpen = false
                            onTypeChange(option)
                        }) {
                            Text(sensorTypeLabel(option))
                        }
                    }
                }
            }
        },
    )
}

@Composable
private fun PanelSensorCardShell(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    leading: @Composable () -> Unit,
    label: String,
    footer: @Composable () -> Unit = {},
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
            .background(
                color = MaterialTheme.colors.surface,
                shape = RoundedCornerShape(12.dp),
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.12f),
                shape = RoundedCornerShape(12.dp),
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                leading()
                SelectSwitch(enabled, { onEnabledChange(!enabled) })
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.subtitle1,
                    color = MaterialTheme.colors.primary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                footer()
            }
        }
    }
}

@Composable
fun PanelWaterTanksRow(
    cfg: DevConfig,
    maxWater: Int,
) {
    if (maxWater <= 0) return
    val platformChannels by panelPlatformChannels.collectAsState()
    val waterChannels = platformChannels?.water.orEmpty()
    val fallbackLabels = listOf("Temiz su", "Gri su", "Kirli su")
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (wi in 0 until maxWater) {
            val bufferIndex = wi + 2
            val channelName = waterChannels.getOrNull(wi)?.name?.trim().orEmpty()
            val label = channelName.ifEmpty { fallbackLabels.getOrElse(wi) { "Su ${wi + 1}" } }
            InformationBoxConfig(
                bufferIndex,
                cfg.SensorsNameBuffer.getOrElse(bufferIndex) { 0 } != 0.toByte(),
                label,
            )
        }
    }
}

@Composable
expect fun getDeviceType(): String

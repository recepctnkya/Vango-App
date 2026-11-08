package com.hextech.vango

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import testvango.composeapp.generated.resources.Res
import testvango.composeapp.generated.resources.motor_dir
import testvango.composeapp.generated.resources.motor_dir_closing
import testvango.composeapp.generated.resources.motor_dir_down
import testvango.composeapp.generated.resources.motor_dir_opening
import testvango.composeapp.generated.resources.motor_dir_up
import testvango.composeapp.generated.resources.motor_locked
import testvango.composeapp.generated.resources.motor_no_motor
import testvango.composeapp.generated.resources.motor_unlocked
import testvango.composeapp.generated.resources.motor_page_hint
import testvango.composeapp.generated.resources.motor_page_title
import testvango.composeapp.generated.resources.motor_speed
import testvango.composeapp.generated.resources.motor_status_active
import testvango.composeapp.generated.resources.motor_status_blocked
import testvango.composeapp.generated.resources.motor_status_hold_hint
import testvango.composeapp.generated.resources.motor_status_ready
import testvango.composeapp.generated.resources.motor_telemetry_incomplete
import testvango.composeapp.generated.resources.settings_rd_panel_motor_label
import testvango.composeapp.generated.resources.settings_rd_panel_status_active
import testvango.composeapp.generated.resources.settings_rd_panel_status_passive

private val MotBg: Color @Composable get() = LocalAppChrome.current.bg
private val MotCard: Color @Composable get() = LocalAppChrome.current.card
private val MotCyan = Color(0xFF2EC5FF)
private val MotPurple = Color(0xFFB794F6)
private val MotText: Color @Composable get() = LocalAppChrome.current.text
private val MotMuted: Color @Composable get() = LocalAppChrome.current.muted
private val MotTrack: Color @Composable get() = LocalAppChrome.current.track
private val MotGreen = Color(0xFF3DDC84)
private val MotBorder: Color @Composable get() = LocalAppChrome.current.border

/** Per-motor padlock. Absent means locked. Not part of the motor CAN/BLE packet. */
internal object MotorLockState {
    val byId = mutableStateMapOf<Int, Boolean>()

    fun isLocked(id: Int): Boolean = byId[id] ?: true

    fun setLocked(id: Int, locked: Boolean) {
        byId[id] = locked
    }
}

@Composable
fun MotorControlScreen(
    data: VanGoData,
    modifier: Modifier = Modifier,
) {
    if (isPhoneUi()) {
        // Phone keeps its own mockup; tablet uses the gear-dial board below.
        PhoneMotorControlScreen(data = data, modifier = modifier)
        return
    }
    TabletMotorControlScreen(data = data, modifier = modifier)
}

@Composable
private fun TabletMotorControlScreen(
    data: VanGoData,
    modifier: Modifier = Modifier,
) {
    val channels by panelPlatformChannels.collectAsState()
    val motors = remember(data.numOfO, data.motorEn, data.io?.motor, channels) {
        discoverMotors(data, channels)
    }
    // Only one motor may run at a time.
    var runningMotorId by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MotBg)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MotorPageHeader()

        if (isBleGattLinkReady()) {
            Text(
                stringResource(Res.string.motor_telemetry_incomplete),
                color = MotMuted,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MotCard, RoundedCornerShape(10.dp))
                    .border(1.dp, MotBorder, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }

        if (motors.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(MotCard, RoundedCornerShape(16.dp))
                    .border(1.dp, MotBorder, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(Res.string.motor_no_motor),
                    color = MotMuted,
                    fontSize = 16.sp,
                )
            }
        } else {
            // Keep card width fixed to a 2-column slot so 1 motor does not stretch full-width.
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.TopStart,
            ) {
                val gap = 14.dp
                val slots = maxOf(2, motors.size)
                val cardWidth = (maxWidth - gap * (slots - 1)) / slots
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(gap, Alignment.Start),
                ) {
                    motors.forEach { motor ->
                        MotorControlCard(
                            motor = motor,
                            data = data,
                            peerRunning = runningMotorId != null && runningMotorId != motor.id,
                            onRunningChanged = { running ->
                                runningMotorId = when {
                                    running -> motor.id
                                    runningMotorId == motor.id -> null
                                    else -> runningMotorId
                                }
                            },
                            modifier = Modifier
                                .width(cardWidth)
                                .fillMaxHeight(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MotorPageHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = null,
            tint = MotText,
            modifier = Modifier
                .size(28.dp)
                .clickable {
                    dashboardNavigateToSection = DashboardSection.Home
                },
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                stringResource(Res.string.motor_page_title),
                color = MotText,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                stringResource(Res.string.motor_page_hint),
                color = MotMuted,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 17.sp,
            )
        }
        Icon(
            Icons.Filled.Settings,
            contentDescription = null,
            tint = MotMuted,
            modifier = Modifier
                .size(26.dp)
                .clickable {
                    dashboardSettingsNav = DashboardSettingsNav.Panel
                    dashboardSettingsDetailOpen = true
                    dashboardShowSettings = true
                },
        )
    }
}

@Composable
private fun MotorControlCard(
    motor: MotorCardModel,
    data: VanGoData,
    peerRunning: Boolean,
    onRunningChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = if (motor.accentIndex % 2 == 0) MotCyan else MotPurple
    val scope = rememberCoroutineScope()
    var motorActive by remember(motor.id) { mutableStateOf(true) }
    var holding by remember(motor.id) { mutableStateOf(false) }
    var moveDir by remember(motor.id) { mutableStateOf(MotorMoveDir.Idle) }
    var speedPct by remember(motor.id) { mutableStateOf(70) }
    val liveDir = motorLiveDir(data, motor)
    LaunchedEffect(liveDir, holding) {
        if (holding) return@LaunchedEffect
        moveDir = liveDir
        onRunningChanged(liveDir != MotorMoveDir.Idle)
    }

    val forwardLabel = when (motor.kind) {
        MotorKind.Awning -> stringResource(Res.string.motor_dir_opening)
        else -> stringResource(Res.string.motor_dir_up)
    }
    val reverseLabel = when (motor.kind) {
        MotorKind.Awning -> stringResource(Res.string.motor_dir_closing)
        else -> stringResource(Res.string.motor_dir_down)
    }
    val motorNumber = Regex("""(\d+)\s*$""").find(motor.title)?.groupValues?.getOrNull(1)?.toIntOrNull()
        ?: (motor.id + 1)
    val displayName = stringResource(Res.string.settings_rd_panel_motor_label, motorNumber)
    val activeLabel = stringResource(Res.string.settings_rd_panel_status_active)
    val passiveLabel = stringResource(Res.string.settings_rd_panel_status_passive)

    val motorLocked = MotorLockState.isLocked(motor.id)
    val running = moveDir != MotorMoveDir.Idle
    val spinning = running && motorActive
    val canDrive = motorActive && !peerRunning && !motorLocked
    val statusTitle = when {
        !motorActive -> passiveLabel
        running -> stringResource(Res.string.motor_status_active)
        peerRunning -> stringResource(Res.string.motor_status_blocked)
        else -> stringResource(Res.string.motor_status_hold_hint)
    }
    val statusColor = when {
        !motorActive -> MotMuted
        running -> MotGreen
        peerRunning -> Color(0xFFFFB74D)
        else -> MotGreen
    }

    fun writePair(fwdOn: Boolean, revOn: Boolean) {
        val fwd = motor.forwardRelay
        val rev = motor.reverseRelay
        if (fwd == null && rev == null) return
        if (vanGoGlobal.value.lockOperation(Operations.WriteDataDevice)) {
            scope.launch {
                try {
                    val van = getAppContext().vanGO
                    if (fwd != null) van.writeDigitalPort(fwd, fwdOn)
                    if (rev != null) van.writeDigitalPort(rev, revOn)
                } finally {
                    vanGoGlobal.value.unlockOperation()
                }
            }
        }
    }

    fun startMove(dir: MotorMoveDir) {
        if (MotorLockState.isLocked(motor.id)) return
        if (!canDrive || moveDir != MotorMoveDir.Idle) return
        holding = true
        moveDir = dir
        onRunningChanged(true)
        when (dir) {
            MotorMoveDir.Forward -> writePair(fwdOn = true, revOn = false)
            MotorMoveDir.Reverse -> writePair(fwdOn = false, revOn = true)
            MotorMoveDir.Idle -> Unit
        }
    }

    fun stopMove() {
        if (moveDir == MotorMoveDir.Idle) return
        writePair(fwdOn = false, revOn = false)
        holding = false
        moveDir = MotorMoveDir.Idle
        onRunningChanged(false)
    }

    fun openPanelSettings() {
        dashboardSettingsNav = DashboardSettingsNav.Panel
        dashboardSettingsDetailOpen = true
        dashboardShowSettings = true
    }

    Column(
        modifier = modifier
            .background(MotCard, RoundedCornerShape(18.dp))
            .border(1.dp, MotBorder, RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(accent.copy(alpha = 0.14f), RoundedCornerShape(12.dp))
                    .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(MotorEngineIcon, null, tint = accent, modifier = Modifier.size(26.dp))
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        displayName,
                        color = MotText,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Icon(
                        Icons.Filled.Edit,
                        null,
                        tint = MotMuted,
                        modifier = Modifier
                            .size(16.dp)
                            .clickable(onClick = ::openPanelSettings),
                    )
                    Box(
                        Modifier
                            .size(8.dp)
                            .background(statusColor, CircleShape),
                    )
                    Text(
                        statusTitle,
                        color = statusColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                    )
                }
                Text(
                    motor.subtitle,
                    color = MotMuted,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Switch(
                    checked = motorActive,
                    onCheckedChange = { on ->
                        if (!on && running) stopMove()
                        motorActive = on
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = MotGreen,
                        uncheckedThumbColor = Color.White.copy(alpha = 0.85f),
                        uncheckedTrackColor = MotMuted.copy(alpha = 0.4f),
                    ),
                )
                Icon(
                    Icons.Filled.Settings,
                    null,
                    tint = MotMuted,
                    modifier = Modifier
                        .size(22.dp)
                        .clickable(onClick = ::openPanelSettings),
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                MotorDirControl(
                    label = stringResource(Res.string.motor_dir),
                    value = forwardLabel,
                    up = true,
                    accent = accent,
                    lit = moveDir == MotorMoveDir.Forward,
                    enabled = canDrive,
                    onPress = { startMove(MotorMoveDir.Forward) },
                    onRelease = { stopMove() },
                )

                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(horizontal = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    val dial = minOf(maxWidth.value, maxHeight.value)
                        .coerceIn(200f, 360f).dp
                    MotorGearDial(
                        accent = accent,
                        locked = !motorActive,
                        spinning = spinning,
                        forward = moveDir == MotorMoveDir.Forward,
                        speedPct = speedPct,
                        modifier = Modifier.size(dial),
                    )
                }

                MotorDirControl(
                    label = stringResource(Res.string.motor_dir),
                    value = reverseLabel,
                    up = false,
                    accent = accent,
                    lit = moveDir == MotorMoveDir.Reverse,
                    enabled = canDrive,
                    onPress = { startMove(MotorMoveDir.Reverse) },
                    onRelease = { stopMove() },
                )
            }
            Spacer(Modifier.height(10.dp))
            MotorLockButton(
                locked = motorLocked,
                accent = accent,
                onClick = {
                    if (running) return@MotorLockButton
                    MotorLockState.setLocked(motor.id, !motorLocked)
                },
            )
        }

        Spacer(Modifier.height(10.dp))
        MotorSpeedControl(
            speedPct = speedPct,
            accent = accent,
            enabled = motorActive,
            onSpeedChange = { speedPct = it.coerceIn(0, 100) },
        )
    }
}

@Composable
private fun MotorSpeedControl(
    speedPct: Int,
    accent: Color,
    enabled: Boolean,
    onSpeedChange: (Int) -> Unit,
) {
    var sliding by remember { mutableStateOf(false) }
    var speedNorm by remember { mutableStateOf((speedPct / 100f).coerceIn(0f, 1f)) }
    LaunchedEffect(speedPct) {
        if (!sliding) speedNorm = (speedPct / 100f).coerceIn(0f, 1f)
    }
    val shownPct = (speedNorm * 100f).roundToInt().coerceIn(0, 100)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Filled.Speed, null, tint = accent, modifier = Modifier.size(20.dp))
            Text(
                stringResource(Res.string.motor_speed),
                color = MotText,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            MotorStepButton(
                enabled = enabled && shownPct > 0,
                onClick = { onSpeedChange(shownPct - 5) },
            ) {
                Icon(Icons.Filled.Remove, null, tint = MotText, modifier = Modifier.size(16.dp))
            }
            Box(
                modifier = Modifier
                    .width(64.dp)
                    .height(34.dp)
                    .background(MotTrack, RoundedCornerShape(10.dp))
                    .border(1.dp, MotBorder, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "%$shownPct",
                    color = accent,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            MotorStepButton(
                enabled = enabled && shownPct < 100,
                onClick = { onSpeedChange(shownPct + 5) },
            ) {
                Icon(Icons.Filled.Add, null, tint = MotText, modifier = Modifier.size(16.dp))
            }
        }
        // Match home dimmer: continuous 0..1 float, compact touch height, no discrete steps.
        Slider(
            value = speedNorm,
            onValueChange = {
                if (!enabled) return@Slider
                sliding = true
                speedNorm = it.coerceIn(0f, 1f)
                onSpeedChange((it * 100f).roundToInt().coerceIn(0, 100))
            },
            onValueChangeFinished = {
                onSpeedChange((speedNorm * 100f).roundToInt().coerceIn(0, 100))
                sliding = false
            },
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(26.dp),
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = accent,
                inactiveTrackColor = MotTrack,
                disabledThumbColor = MotMuted.copy(alpha = 0.4f),
                disabledActiveTrackColor = MotMuted.copy(alpha = 0.25f),
                disabledInactiveTrackColor = MotTrack,
            ),
        )
    }
}

@Composable
private fun MotorStepButton(
    enabled: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .background(MotTrack, RoundedCornerShape(10.dp))
            .border(1.dp, MotBorder, RoundedCornerShape(10.dp))
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun MotorDirControl(
    label: String,
    value: String,
    up: Boolean,
    accent: Color,
    lit: Boolean,
    enabled: Boolean,
    onPress: () -> Unit,
    onRelease: () -> Unit,
) {
    val arrowTint = when {
        lit -> accent
        enabled -> MotText
        else -> MotMuted.copy(alpha = 0.4f)
    }
    val borderColor = when {
        lit -> accent
        enabled -> accent.copy(alpha = 0.40f)
        else -> MotBorder
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.width(96.dp),
    ) {
        Text(
            label.uppercase(),
            color = MotMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.0.sp,
        )
        Box(
            modifier = Modifier
                .size(78.dp)
                .background(
                    if (lit) accent.copy(alpha = 0.22f) else MotTrack,
                    CircleShape,
                )
                .border(
                    width = if (lit) 2.5.dp else 2.dp,
                    color = borderColor,
                    shape = CircleShape,
                )
                .then(
                    if (enabled) {
                        Modifier.pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    onPress()
                                    try {
                                        tryAwaitRelease()
                                    } finally {
                                        onRelease()
                                    }
                                },
                            )
                        }
                    } else {
                        Modifier
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            // Double chevron (mockup)
            Canvas(modifier = Modifier.size(34.dp)) {
                val stroke = Stroke(width = 3.4.dp.toPx(), cap = StrokeCap.Round)
                val w = size.width
                val h = size.height
                fun chevron(yOffset: Float) {
                    val path = Path()
                    if (up) {
                        path.moveTo(w * 0.18f, h * (0.58f + yOffset))
                        path.lineTo(w * 0.50f, h * (0.28f + yOffset))
                        path.lineTo(w * 0.82f, h * (0.58f + yOffset))
                    } else {
                        path.moveTo(w * 0.18f, h * (0.42f + yOffset))
                        path.lineTo(w * 0.50f, h * (0.72f + yOffset))
                        path.lineTo(w * 0.82f, h * (0.42f + yOffset))
                    }
                    drawPath(path = path, color = arrowTint, style = stroke)
                }
                chevron(-0.12f)
                chevron(0.12f)
            }
        }
        Text(
            value,
            color = if (lit) accent else MotText,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
internal fun MotorLockButton(
    locked: Boolean,
    accent: Color,
    onClick: () -> Unit,
) {
    val label = stringResource(if (locked) Res.string.motor_locked else Res.string.motor_unlocked)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .background(MotTrack, CircleShape)
                .border(2.dp, accent, CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (locked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                contentDescription = label,
                tint = accent,
                modifier = Modifier.size(30.dp),
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            label,
            color = accent,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.4.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Composable
internal fun MotorGearDial(
    accent: Color,
    locked: Boolean,
    spinning: Boolean,
    forward: Boolean,
    speedPct: Int,
    modifier: Modifier = Modifier,
) {
    var angle by remember { mutableStateOf(0f) }
    var orbit by remember { mutableStateOf(0f) }
    val motorActive = !locked

    LaunchedEffect(spinning, forward, speedPct) {
        if (!spinning) return@LaunchedEffect
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                if (last != 0L) {
                    val dt = ((now - last).coerceIn(0L, 32_000_000L)) / 1_000_000_000f
                    val degPerSec = (35f + speedPct.coerceIn(0, 100) * 2.4f) * if (forward) 1f else -1f
                    angle = (angle + degPerSec * dt) % 360f
                    // Arrow orbit follows rotation direction (slightly faster for visible motion)
                    orbit = (orbit + degPerSec * 1.15f * dt) % 360f
                }
                last = now
            }
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        val track = MotTrack
        val muted = MotMuted
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height
            val cx = w / 2f
            val cy = h / 2f
            val r = minOf(w, h) / 2f

            // Gear + thicker outer ring (ticks / arrows) around it
            val gearOuter = r * 0.86f
            val tickOuter = r - 0.5.dp.toPx()
            val tickInner = gearOuter + 4.dp.toPx()
            val arcR = (gearOuter + tickOuter) * 0.52f
            val arcStroke = 6.5.dp.toPx()

            // Surroundings active when unlocked
            val tickAlphaMajor = if (motorActive) 0.50f else 0.16f
            val tickAlphaMinor = if (motorActive) 0.28f else 0.08f
            val tickColor = if (motorActive) accent else muted

            val tickCount = 60
            for (i in 0 until tickCount) {
                val a = (i * (360.0 / tickCount) - 90.0) * PI / 180.0
                val cosA = cos(a).toFloat()
                val sinA = sin(a).toFloat()
                val major = i % 5 == 0
                val inner = if (major) tickInner else tickInner + 1.5.dp.toPx()
                drawLine(
                    color = tickColor.copy(alpha = if (major) tickAlphaMajor else tickAlphaMinor),
                    start = Offset(cx + cosA * inner, cy + sinA * inner),
                    end = Offset(cx + cosA * tickOuter, cy + sinA * tickOuter),
                    strokeWidth = if (major) 2.4f else 1.5f,
                    cap = StrokeCap.Round,
                )
            }

            drawArc(
                color = if (motorActive) track else track.copy(alpha = 0.45f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(cx - arcR, cy - arcR),
                size = Size(arcR * 2f, arcR * 2f),
                style = Stroke(width = arcStroke * 0.4f, cap = StrokeCap.Round),
            )

            // Direction arrows: only while pressing; orbit shows rotation movement
            if (spinning) {
                fun drawDirArrow(startDeg: Float, sweepDeg: Float, color: Color, thick: Float) {
                    val box = Size(arcR * 2f, arcR * 2f)
                    val topLeft = Offset(cx - arcR, cy - arcR)
                    drawArc(
                        color = color,
                        startAngle = startDeg,
                        sweepAngle = sweepDeg,
                        useCenter = false,
                        topLeft = topLeft,
                        size = box,
                        style = Stroke(width = thick, cap = StrokeCap.Round),
                    )
                    val endDeg = startDeg + sweepDeg
                    val endRad = endDeg * PI / 180.0
                    val tipX = cx + cos(endRad).toFloat() * arcR
                    val tipY = cy + sin(endRad).toFloat() * arcR
                    val cw = sweepDeg >= 0f
                    val tangent = endRad + if (cw) PI / 2.0 else -PI / 2.0
                    val ah = 14.dp.toPx()
                    val aw = 9.dp.toPx()
                    val bx = tipX - cos(tangent).toFloat() * ah
                    val by = tipY - sin(tangent).toFloat() * ah
                    val arrow = Path().apply {
                        moveTo(tipX, tipY)
                        lineTo(
                            bx + cos(tangent + PI / 2.0).toFloat() * aw,
                            by + sin(tangent + PI / 2.0).toFloat() * aw,
                        )
                        lineTo(
                            bx + cos(tangent - PI / 2.0).toFloat() * aw,
                            by + sin(tangent - PI / 2.0).toFloat() * aw,
                        )
                        close()
                    }
                    drawPath(arrow, color = color)
                }

                val o = orbit
                if (forward) {
                    drawDirArrow(startDeg = -50f + o, sweepDeg = 95f, color = accent, thick = arcStroke)
                    drawDirArrow(startDeg = 130f + o, sweepDeg = 95f, color = accent.copy(alpha = 0.40f), thick = arcStroke * 0.85f)
                } else {
                    drawDirArrow(startDeg = 50f + o, sweepDeg = -95f, color = accent, thick = arcStroke)
                    drawDirArrow(startDeg = -130f + o, sweepDeg = -95f, color = accent.copy(alpha = 0.40f), thick = arcStroke * 0.85f)
                }
            }

            rotate(degrees = angle, pivot = Offset(cx, cy)) {
                drawMetallicGear(
                    cx = cx,
                    cy = cy,
                    outerR = gearOuter,
                    teeth = 12,
                    passive = locked,
                )
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMetallicGear(
    cx: Float,
    cy: Float,
    outerR: Float,
    teeth: Int,
    passive: Boolean,
) {
    val toothDepth = outerR * 0.20f
    val rootR = outerR - toothDepth
    val valleyHalf = 360f / teeth / 2f
    val tipHalf = valleyHalf * 0.42f
    val rootHalf = valleyHalf * 0.78f

    val gearPath = Path()
    for (i in 0 until teeth) {
        val base = i * (360f / teeth) - 90f
        val aTip0 = (base - tipHalf) * PI / 180.0
        val aTip1 = (base + tipHalf) * PI / 180.0
        val aRoot0 = (base - rootHalf) * PI / 180.0
        val aRoot1 = (base + rootHalf) * PI / 180.0
        val nextBase = (i + 1) * (360f / teeth) - 90f
        val aNextRoot0 = (nextBase - rootHalf) * PI / 180.0

        fun pt(rad: Double, radius: Float) =
            Offset(cx + cos(rad).toFloat() * radius, cy + sin(rad).toFloat() * radius)

        if (i == 0) {
            val p = pt(aRoot0, rootR)
            gearPath.moveTo(p.x, p.y)
        }
        val pRoot0 = pt(aRoot0, rootR)
        if (i != 0) gearPath.lineTo(pRoot0.x, pRoot0.y)
        val pTip0 = pt(aTip0, outerR)
        val pTip1 = pt(aTip1, outerR)
        val pRoot1 = pt(aRoot1, rootR)
        gearPath.lineTo(pTip0.x, pTip0.y)
        gearPath.lineTo(pTip1.x, pTip1.y)
        gearPath.lineTo(pRoot1.x, pRoot1.y)
        val mid = pt(((aRoot1 + aNextRoot0) / 2.0), rootR * 0.98f)
        gearPath.quadraticTo(
            mid.x, mid.y,
            (cx + cos(aNextRoot0).toFloat() * rootR),
            (cy + sin(aNextRoot0).toFloat() * rootR),
        )
    }
    gearPath.close()

    val bodyColors = if (passive) {
        listOf(
            Color(0xFF6B7380).copy(alpha = 0.55f),
            Color(0xFF4A5260).copy(alpha = 0.55f),
            Color(0xFF3A4250).copy(alpha = 0.50f),
            Color(0xFF5A6270).copy(alpha = 0.55f),
        )
    } else {
        listOf(
            Color(0xFFE8EEF5),
            Color(0xFFB8C2CF),
            Color(0xFF8A96A8),
            Color(0xFFD5DCE6),
        )
    }

    drawPath(
        gearPath,
        brush = Brush.radialGradient(
            colors = listOf(
                Color.Black.copy(alpha = if (passive) 0.18f else 0.35f),
                Color.Transparent,
            ),
            center = Offset(cx + outerR * 0.08f, cy + outerR * 0.12f),
            radius = outerR * 1.15f,
        ),
    )
    drawPath(
        gearPath,
        brush = Brush.linearGradient(
            colors = bodyColors,
            start = Offset(cx - outerR, cy - outerR),
            end = Offset(cx + outerR, cy + outerR),
        ),
    )
    drawPath(
        gearPath,
        color = if (passive) Color(0xFF3A4250).copy(alpha = 0.55f) else Color(0xFF5C6777),
        style = Stroke(width = 2.2.dp.toPx()),
    )

    val faceR = rootR * 0.78f
    drawCircle(
        brush = Brush.radialGradient(
            colors = if (passive) {
                listOf(Color(0xFF1A222E).copy(alpha = 0.55f), Color(0xFF121820).copy(alpha = 0.50f))
            } else {
                listOf(Color(0xFF2A3342), Color(0xFF1A222E))
            },
            center = Offset(cx, cy),
            radius = faceR,
        ),
        radius = faceR,
        center = Offset(cx, cy),
    )
    drawCircle(
        color = if (passive) Color(0xFF4A5564).copy(alpha = 0.40f) else Color(0xFF6B7788),
        radius = faceR,
        center = Offset(cx, cy),
        style = Stroke(width = 2.dp.toPx()),
    )

    val hubR = outerR * 0.30f
    drawCircle(
        brush = Brush.radialGradient(
            colors = if (passive) {
                listOf(
                    Color(0xFF7A8492).copy(alpha = 0.45f),
                    Color(0xFF4A5564).copy(alpha = 0.40f),
                    Color(0xFF3A4250).copy(alpha = 0.40f),
                )
            } else {
                listOf(Color(0xFFF2F5F9), Color(0xFF9AA6B6), Color(0xFF6E7A8B))
            },
            center = Offset(cx - hubR * 0.25f, cy - hubR * 0.30f),
            radius = hubR * 1.2f,
        ),
        radius = hubR,
        center = Offset(cx, cy),
    )
    drawCircle(
        color = if (passive) Color(0xFF3A4250).copy(alpha = 0.45f) else Color(0xFF4A5564),
        radius = hubR,
        center = Offset(cx, cy),
        style = Stroke(width = 1.8.dp.toPx()),
    )

    val boreR = outerR * 0.11f
    drawCircle(
        color = Color(0xFF0E141D).copy(alpha = if (passive) 0.55f else 1f),
        radius = boreR,
        center = Offset(cx, cy),
    )
    if (!passive) {
        drawCircle(
            color = Color(0xFF7E8A99).copy(alpha = 0.7f),
            radius = boreR,
            center = Offset(cx, cy),
            style = Stroke(width = 1.4.dp.toPx()),
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.22f), Color.Transparent),
                center = Offset(cx - outerR * 0.25f, cy - outerR * 0.30f),
                radius = outerR * 0.55f,
            ),
            radius = outerR * 0.55f,
            center = Offset(cx - outerR * 0.15f, cy - outerR * 0.18f),
        )
    }
}


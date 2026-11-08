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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import testvango.composeapp.generated.resources.Res
import testvango.composeapp.generated.resources.motor_dir_closing
import testvango.composeapp.generated.resources.motor_dir_down
import testvango.composeapp.generated.resources.motor_dir_opening
import testvango.composeapp.generated.resources.motor_dir_up
import testvango.composeapp.generated.resources.motor_no_motor
import testvango.composeapp.generated.resources.motor_page_hint
import testvango.composeapp.generated.resources.motor_page_title
import testvango.composeapp.generated.resources.motor_speed
import testvango.composeapp.generated.resources.motor_status_active
import testvango.composeapp.generated.resources.motor_status_blocked
import testvango.composeapp.generated.resources.motor_status_hold_hint
import testvango.composeapp.generated.resources.motor_status_ready
import testvango.composeapp.generated.resources.motor_status_ready_hint
import testvango.composeapp.generated.resources.motor_telemetry_incomplete
import testvango.composeapp.generated.resources.settings_rd_panel_motor_label
import testvango.composeapp.generated.resources.settings_rd_panel_status_active
import testvango.composeapp.generated.resources.settings_rd_panel_status_passive

private val MotBg: Color @Composable get() = LocalAppChrome.current.bg
private val MotCard: Color @Composable get() = LocalAppChrome.current.card
private val MotCyan = Color(0xFF2EC5FF)
private val MotText: Color @Composable get() = LocalAppChrome.current.text
private val MotMuted: Color @Composable get() = LocalAppChrome.current.muted
private val MotTrack: Color @Composable get() = LocalAppChrome.current.track
private val MotGreen = Color(0xFF21C47E)
private val MotBorder: Color @Composable get() = LocalAppChrome.current.border
private val MotField: Color @Composable get() = LocalAppChrome.current.field

@Composable
fun PhoneMotorControlScreen(
    data: VanGoData,
    modifier: Modifier = Modifier,
) {
    val channels by panelPlatformChannels.collectAsState()
    val motors = remember(data.numOfO, data.motorEn, data.io?.motor, channels) {
        discoverMotors(data, channels)
    }
    var runningMotorId by remember { mutableStateOf<Int?>(null) }
    val phoneUi = isPhoneUi()
    val compact = isCompactLayout()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MotBg)
            .padding(
                horizontal = if (phoneUi) 14.dp else 12.dp,
                vertical = if (phoneUi) 8.dp else 8.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PhoneMotorPageHeader()

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
        } else if (phoneUi && motors.size <= 2) {
            // Fixed 2-slot height: 1 motor stays half-height; 2 motors fill the page.
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                val gap = 10.dp
                val slots = 2
                val cardHeight = (maxHeight - gap * (slots - 1)) / slots
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(gap),
                ) {
                    motors.forEach { motor ->
                        PhoneMotorControlCard(
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
                                .fillMaxWidth()
                                .height(cardHeight),
                        )
                    }
                }
            }
        } else if (compact || motors.size == 1) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                motors.forEach { motor ->
                    PhoneMotorControlCard(
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
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                motors.forEach { motor ->
                    PhoneMotorControlCard(
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
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                }
            }
        }
    }
}

@Composable
private fun PhoneMotorPageHeader() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
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
            lineHeight = 17.sp,
        )
    }
}

@Composable
private fun PhoneMotorControlCard(
    motor: MotorCardModel,
    data: VanGoData,
    peerRunning: Boolean,
    onRunningChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val radius = appCardCornerRadius()
    var motorActive by remember(motor.id) { mutableStateOf(true) }
    var holding by remember(motor.id) { mutableStateOf(false) }
    var moveDir by remember(motor.id) { mutableStateOf(MotorMoveDir.Idle) }
    // Same continuous 0..1 model as home dimmer sliders.
    var speedNorm by remember(motor.id) { mutableStateOf(0.70f) }
    val speedPct = (speedNorm * 100f).roundToInt().coerceIn(0, 100)
    val liveDir = motorLiveDir(data, motor)
    val iconTint = if (motor.accentIndex % 2 == 0) MotCyan else MotGreen

    // BLE live packets often omit motor H-bridge pins; don't snap UI Idle mid-hold.
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
    val fallbackName = stringResource(Res.string.settings_rd_panel_motor_label, motor.id + 1)
    val displayName = motor.subtitle.trim().ifEmpty { fallbackName }
    val activeLabel = stringResource(Res.string.settings_rd_panel_status_active)
    val passiveLabel = stringResource(Res.string.settings_rd_panel_status_passive)

    val motorLocked = MotorLockState.isLocked(motor.id)
    val running = moveDir != MotorMoveDir.Idle
    val spinning = running && motorActive
    val canDrive = motorActive && !peerRunning && !motorLocked
    val accent = if (motor.accentIndex % 2 == 0) MotCyan else MotGreen

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
        if (!canDrive) return
        if (moveDir == dir) return
        if (moveDir != MotorMoveDir.Idle) {
            writePair(fwdOn = false, revOn = false)
        }
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
        holding = false
        if (moveDir == MotorMoveDir.Idle) {
            writePair(fwdOn = false, revOn = false)
            return
        }
        writePair(fwdOn = false, revOn = false)
        moveDir = MotorMoveDir.Idle
        onRunningChanged(false)
    }

    fun openPanelSettings() {
        dashboardSettingsNav = DashboardSettingsNav.Panel
        dashboardSettingsDetailOpen = true
        dashboardShowSettings = true
    }

    val statusTitle = when {
        !motorActive -> passiveLabel
        running -> stringResource(Res.string.motor_status_active)
        peerRunning -> stringResource(Res.string.motor_status_blocked)
        else -> activeLabel
    }
    val statusHint = when {
        !motorActive -> passiveLabel
        running -> when (moveDir) {
            MotorMoveDir.Forward -> forwardLabel
            MotorMoveDir.Reverse -> reverseLabel
            MotorMoveDir.Idle -> stringResource(Res.string.motor_status_hold_hint)
        }
        peerRunning -> stringResource(Res.string.motor_status_blocked)
        else -> stringResource(Res.string.motor_status_hold_hint)
    }
    val statusColor = when {
        !motorActive -> MotMuted
        running -> MotGreen
        peerRunning -> Color(0xFFFFB74D)
        else -> MotGreen
    }

    Column(
        modifier = modifier
            .background(MotCard, RoundedCornerShape(radius))
            .border(1.dp, MotBorder, RoundedCornerShape(radius))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(iconTint.copy(alpha = 0.14f), RoundedCornerShape(12.dp))
                    .border(1.dp, iconTint.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    MotorEngineIcon,
                    null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    displayName,
                    color = MotText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .background(statusColor, CircleShape),
                    )
                    Column {
                        Text(
                            statusTitle,
                            color = statusColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                        )
                        Text(
                            statusHint,
                            color = MotMuted,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
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
                        .size(18.dp)
                        .clickable(onClick = ::openPanelSettings),
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f, fill = true)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MotorRoundAction(
                    label = forwardLabel,
                    ring = accent,
                    lit = moveDir == MotorMoveDir.Forward,
                    enabled = canDrive,
                    size = 64.dp,
                    holdToRun = true,
                    onPress = { startMove(MotorMoveDir.Forward) },
                    onRelease = { stopMove() },
                ) {
                    MotorDoubleChevron(
                        up = true,
                        color = if (moveDir == MotorMoveDir.Forward) accent else MotText,
                    )
                }

                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    val dial = minOf(maxWidth.value, maxHeight.value)
                        .coerceIn(170f, 320f).dp
                    MotorGearDial(
                        accent = accent,
                        locked = !motorActive,
                        spinning = spinning,
                        forward = moveDir == MotorMoveDir.Forward,
                        speedPct = speedPct,
                        modifier = Modifier.size(dial),
                    )
                }

                MotorRoundAction(
                    label = reverseLabel,
                    ring = accent,
                    lit = moveDir == MotorMoveDir.Reverse,
                    enabled = canDrive,
                    size = 64.dp,
                    holdToRun = true,
                    onPress = { startMove(MotorMoveDir.Reverse) },
                    onRelease = { stopMove() },
                ) {
                    MotorDoubleChevron(
                        up = false,
                        color = if (moveDir == MotorMoveDir.Reverse) accent else MotText,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            MotorLockButton(
                locked = motorLocked,
                accent = accent,
                onClick = {
                    if (running) return@MotorLockButton
                    MotorLockState.setLocked(motor.id, !motorLocked)
                },
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Filled.Speed, null, tint = MotCyan, modifier = Modifier.size(18.dp))
                Text(
                    stringResource(Res.string.motor_speed),
                    color = MotText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                MotorStepButton(
                    enabled = motorActive && speedPct > 0,
                    onClick = {
                        speedNorm = ((speedPct - 5) / 100f).coerceIn(0f, 1f)
                    },
                ) {
                    Icon(Icons.Filled.Remove, null, tint = MotText, modifier = Modifier.size(16.dp))
                }
                Text(
                    "%$speedPct",
                    color = MotCyan,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(44.dp),
                )
                MotorStepButton(
                    enabled = motorActive && speedPct < 100,
                    onClick = {
                        speedNorm = ((speedPct + 5) / 100f).coerceIn(0f, 1f)
                    },
                ) {
                    Icon(Icons.Filled.Add, null, tint = MotText, modifier = Modifier.size(16.dp))
                }
            }
            Slider(
                value = speedNorm,
                onValueChange = { if (motorActive) speedNorm = it.coerceIn(0f, 1f) },
                enabled = motorActive,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(26.dp),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = MotCyan,
                    inactiveTrackColor = MotTrack,
                    disabledThumbColor = MotMuted.copy(alpha = 0.4f),
                    disabledActiveTrackColor = MotMuted.copy(alpha = 0.25f),
                    disabledInactiveTrackColor = MotTrack,
                ),
            )
        }
    }
}

@Composable
private fun MotorRoundAction(
    label: String,
    ring: Color,
    lit: Boolean,
    enabled: Boolean,
    onPress: () -> Unit,
    onRelease: () -> Unit = {},
    holdToRun: Boolean = false,
    onClick: (() -> Unit)? = null,
    size: Dp = 78.dp,
    content: @Composable () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .background(
                    if (lit) ring.copy(alpha = 0.16f) else MotField,
                    CircleShape,
                )
                .border(
                    width = if (lit) 2.5.dp else 2.dp,
                    color = when {
                        lit -> ring
                        enabled -> ring.copy(alpha = 0.55f)
                        else -> MotBorder
                    },
                    shape = CircleShape,
                )
                .then(
                    when {
                        !enabled -> Modifier
                        holdToRun -> Modifier.pointerInput(Unit) {
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
                        onClick != null -> Modifier.clickable(onClick = onClick)
                        else -> Modifier.clickable(onClick = onPress)
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
        Text(
            label,
            color = when {
                lit -> ring
                enabled -> MotText
                else -> MotMuted.copy(alpha = 0.5f)
            },
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

@Composable
private fun MotorDoubleChevron(up: Boolean, color: Color) {
    Canvas(modifier = Modifier.size(32.dp)) {
        val stroke = Stroke(width = 3.2.dp.toPx(), cap = StrokeCap.Round)
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
            drawPath(path = path, color = color, style = stroke)
        }
        chevron(-0.12f)
        chevron(0.12f)
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
            .size(30.dp)
            .background(MotField, RoundedCornerShape(8.dp))
            .border(1.dp, MotBorder, RoundedCornerShape(8.dp))
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

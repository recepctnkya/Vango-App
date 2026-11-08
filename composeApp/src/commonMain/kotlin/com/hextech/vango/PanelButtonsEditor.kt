package com.hextech.vango

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Monitor
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import testvango.composeapp.generated.resources.settings_rd_panel_tank_capacity
import testvango.composeapp.generated.resources.settings_rd_panel_tank_capacity_hint
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import testvango.composeapp.generated.resources.Res
import testvango.composeapp.generated.resources.panel_preset_cancel
import testvango.composeapp.generated.resources.panel_preset_confirm
import testvango.composeapp.generated.resources.settings_rd_lock_again
import testvango.composeapp.generated.resources.settings_rd_panel_buttons
import testvango.composeapp.generated.resources.settings_rd_panel_buttons_hint
import testvango.composeapp.generated.resources.settings_rd_panel_climate
import testvango.composeapp.generated.resources.settings_rd_panel_climate_hint
import testvango.composeapp.generated.resources.settings_rd_panel_climate_move_hint
import testvango.composeapp.generated.resources.settings_rd_panel_dimmer_label
import testvango.composeapp.generated.resources.settings_rd_panel_edit_light
import testvango.composeapp.generated.resources.settings_rd_panel_edit_mode
import testvango.composeapp.generated.resources.settings_rd_panel_edit_sensor
import testvango.composeapp.generated.resources.settings_rd_panel_edit_tank
import testvango.composeapp.generated.resources.settings_rd_panel_edit_title
import testvango.composeapp.generated.resources.settings_rd_panel_icon
import testvango.composeapp.generated.resources.settings_rd_panel_lights
import testvango.composeapp.generated.resources.settings_rd_panel_add_motor
import testvango.composeapp.generated.resources.settings_rd_panel_add_rgb
import testvango.composeapp.generated.resources.settings_rd_panel_edit_motor
import testvango.composeapp.generated.resources.settings_rd_panel_lights_add_hint
import testvango.composeapp.generated.resources.settings_rd_panel_lights_hint
import testvango.composeapp.generated.resources.settings_rd_panel_lights_move_hint
import testvango.composeapp.generated.resources.settings_rd_panel_motor_label
import testvango.composeapp.generated.resources.settings_rd_panel_motors
import testvango.composeapp.generated.resources.settings_rd_panel_motors_hint
import testvango.composeapp.generated.resources.settings_rd_panel_motors_move_hint
import testvango.composeapp.generated.resources.settings_rd_panel_motor_home_active
import testvango.composeapp.generated.resources.settings_rd_panel_motor_home_passive
import testvango.composeapp.generated.resources.settings_rd_panel_motor_icon
import testvango.composeapp.generated.resources.settings_rd_panel_motor_name
import testvango.composeapp.generated.resources.settings_rd_panel_motor_preview
import testvango.composeapp.generated.resources.settings_rd_panel_motor_status
import testvango.composeapp.generated.resources.settings_rd_panel_motor_type
import testvango.composeapp.generated.resources.settings_rd_panel_motor_type_awning
import testvango.composeapp.generated.resources.settings_rd_panel_motor_type_lift
import testvango.composeapp.generated.resources.settings_rd_panel_motor_type_shutter
import testvango.composeapp.generated.resources.settings_rd_panel_motor_type_vent
import testvango.composeapp.generated.resources.settings_rd_panel_move_hint
import testvango.composeapp.generated.resources.settings_rd_panel_name
import testvango.composeapp.generated.resources.settings_rd_panel_page_title
import testvango.composeapp.generated.resources.settings_rd_panel_relay_label
import testvango.composeapp.generated.resources.settings_rd_panel_remove
import testvango.composeapp.generated.resources.settings_rd_panel_reset
import testvango.composeapp.generated.resources.settings_rd_panel_rgb_label
import testvango.composeapp.generated.resources.settings_rd_panel_save
import testvango.composeapp.generated.resources.settings_rd_panel_sensor_label
import testvango.composeapp.generated.resources.settings_rd_panel_sensor_type
import testvango.composeapp.generated.resources.settings_rd_panel_tank_label
import testvango.composeapp.generated.resources.settings_rd_panel_water
import testvango.composeapp.generated.resources.settings_rd_panel_water_hint
import testvango.composeapp.generated.resources.settings_rd_panel_water_move_hint
import testvango.composeapp.generated.resources.settings_rd_panel_button_slot
import testvango.composeapp.generated.resources.settings_rd_panel_phone_buttons_desc
import testvango.composeapp.generated.resources.settings_rd_panel_phone_buttons_list_hint
import testvango.composeapp.generated.resources.settings_rd_panel_phone_climate_desc
import testvango.composeapp.generated.resources.settings_rd_panel_phone_lights_desc
import testvango.composeapp.generated.resources.settings_rd_panel_phone_motors_desc
import testvango.composeapp.generated.resources.settings_rd_panel_phone_passive_info
import testvango.composeapp.generated.resources.settings_rd_panel_phone_water_desc
import testvango.composeapp.generated.resources.settings_rd_panel_phone_water_list_hint
import testvango.composeapp.generated.resources.settings_rd_panel_phone_water_passive_info
import testvango.composeapp.generated.resources.settings_rd_panel_water_show_home
import testvango.composeapp.generated.resources.settings_rd_panel_water_hide_home
import testvango.composeapp.generated.resources.settings_rd_panel_status_active
import testvango.composeapp.generated.resources.settings_rd_panel_status_passive
import testvango.composeapp.generated.resources.settings_rd_panel_tab_buttons_short
import testvango.composeapp.generated.resources.settings_rd_panel_tab_climate_short
import testvango.composeapp.generated.resources.settings_rd_panel_tab_dimmer
import testvango.composeapp.generated.resources.settings_rd_panel_tab_motor_short
import testvango.composeapp.generated.resources.settings_rd_panel_tab_water_short

private val PbBg: Color @Composable get() = LocalAppChrome.current.panel
private val PbCard: Color @Composable get() = LocalAppChrome.current.tile
private val PbCyan = Color(0xFF2EC5FF)
private val PbText: Color @Composable get() = LocalAppChrome.current.text
private val PbMuted: Color @Composable get() = LocalAppChrome.current.muted
private val PbBorder: Color @Composable get() = LocalAppChrome.current.border
private val PbField: Color @Composable get() = LocalAppChrome.current.field

private const val MAX_BUTTONS = 12

private enum class PanelEditorTab {
    Buttons, Lights, Water, Climate, Motors
}

private enum class PanelEditKind {
    Relay, Dimmer, Rgb, Water, Sensor, Motor
}

private fun padPanelChannels(
    source: List<PanelPlatformChannel>,
    count: Int,
    nameFor: (Int) -> String,
    iconFor: (Int) -> String,
    enabledDefault: (Int) -> Boolean = { true },
    sensorType: String = "temp_humidity",
): List<PanelPlatformChannel> = (0 until count).map { id ->
    source.firstOrNull { it.id == id } ?: PanelPlatformChannel(
        id = id,
        enabled = enabledDefault(id),
        name = nameFor(id),
        icon = iconFor(id),
        sensorType = sensorType,
        sortOrder = id,
    )
}

@Composable
fun PanelButtonsEditorContent(
    onLock: () -> Unit,
) {
    val session by panelSettingsSession.collectAsState()
    val channels by panelPlatformChannels.collectAsState()
    val profile by devicePanelProfile.collectAsState()
    var tab by remember { mutableStateOf(PanelEditorTab.Buttons) }
    var editMode by remember { mutableStateOf(false) }
    var moveSourceId by remember { mutableStateOf<Int?>(null) }
    var editingId by remember { mutableStateOf<Int?>(null) }
    var editingKind by remember { mutableStateOf<PanelEditKind?>(null) }

    LaunchedEffect(Unit) {
        beginPanelSettingsSessionIfNeeded()
    }
    LaunchedEffect(tab) {
        moveSourceId = null
        editingId = null
        editingKind = null
    }

    val defaults = remember(profile.profileKey) {
        defaultPanelPlatformConfig(profile.profileKey).channels
    }
    val relays = remember(channels, defaults) {
        padPanelChannels(channels?.relays.orEmpty(), MAX_BUTTONS, { "Röle ${it + 1}" }, { "lightbulb" })
    }
    val dimmers = remember(channels, defaults, profile.caps.dimmers) {
        padPanelChannels(
            source = channels?.dimmers.orEmpty().ifEmpty { defaults.dimmers },
            count = profile.caps.dimmers,
            nameFor = { "Dimmer ${it + 1}" },
            iconFor = { "spot_aydinlatma" },
        )
    }
    val rgbChannels = channels?.rgb ?: defaults.rgb
    val motorChannels = channels?.motors ?: defaults.motors
    val rgbCap = profile.caps.rgb.coerceAtLeast(1)
    val motorCap = profile.caps.motors.coerceAtLeast(1)
    val waterTanks = remember(channels, defaults, profile.caps.water) {
        sanitizeWaterChannels(
            water = padPanelChannels(
                source = channels?.water.orEmpty().ifEmpty { defaults.water },
                count = profile.caps.water,
                nameFor = { listOf("Temiz Su", "Gri Su", "Kirli Su").getOrElse(it) { "Su ${it + 1}" } },
                iconFor = { defaultWaterIcon(it) },
                enabledDefault = { it == 0 },
            ),
            count = profile.caps.water,
        )
    }
    val sensors = remember(channels, defaults, profile.caps.sensors) {
        padPanelChannels(
            source = channels?.sensors.orEmpty().ifEmpty { defaults.sensors },
            count = profile.caps.sensors,
            nameFor = { sensorDefaultName("temp_humidity", it) },
            iconFor = { sensorDefaultIcon("temp_humidity") },
            enabledDefault = { it == 0 },
        )
    }
    val editingChannel = when (editingKind) {
        PanelEditKind.Relay -> relays.firstOrNull { it.id == editingId }
        PanelEditKind.Dimmer -> dimmers.firstOrNull { it.id == editingId }
        PanelEditKind.Rgb -> rgbChannels.firstOrNull { it.id == editingId }
        PanelEditKind.Water -> waterTanks.firstOrNull { it.id == editingId }
        PanelEditKind.Sensor -> sensors.firstOrNull { it.id == editingId }
        PanelEditKind.Motor -> motorChannels.firstOrNull { it.id == editingId }
        null -> null
    }

    fun clearEditSelection() {
        moveSourceId = null
        editingId = null
        editingKind = null
    }

    if (isPhoneUi()) {
        PhonePanelEditorPhoneLayout(
            tab = tab,
            onTabChange = { tab = it },
            editMode = editMode,
            onEditModeChange = { enabled ->
                editMode = enabled
                if (!enabled) clearEditSelection()
            },
            onLock = onLock,
            defaults = defaults,
            relays = relays,
            dimmers = dimmers,
            waterTanks = waterTanks,
            sensors = sensors,
            motorChannels = motorChannels,
            motorCap = motorCap,
            onEditRelay = { id ->
                editingKind = PanelEditKind.Relay
                editingId = id
            },
            onEditDimmer = { id ->
                editingKind = PanelEditKind.Dimmer
                editingId = id
            },
            onEditRgb = { id ->
                editingKind = PanelEditKind.Rgb
                editingId = id
            },
            onEditWater = { id ->
                editingKind = PanelEditKind.Water
                editingId = id
            },
            onEditSensor = { id ->
                editingKind = PanelEditKind.Sensor
                editingId = id
            },
            onEditMotor = { id ->
                editingKind = PanelEditKind.Motor
                editingId = id
            },
            rgbChannels = rgbChannels,
            rgbCap = rgbCap,
            hasChanges = session.hasChanges,
            isSaving = session.isSaving,
            onSave = { savePanelSettings() },
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 14.dp),
        ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(Icons.Filled.Monitor, null, tint = PbCyan, modifier = Modifier.size(28.dp))
                Text(
                    stringResource(Res.string.settings_rd_panel_page_title).uppercase(),
                    color = PbText,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                )
                Text(
                    stringResource(Res.string.settings_rd_lock_again),
                    color = PbCyan.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clickable(onClick = onLock)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TextButton(
                    onClick = {
                        val defaults = defaultPanelPlatformConfig(devicePanelProfile.value.profileKey).channels.relays
                        replacePanelRelayChannels(defaults)
                        editMode = false
                        moveSourceId = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = PbMuted),
                ) {
                    Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(Res.string.settings_rd_panel_reset), fontSize = 13.sp)
                }
                Button(
                    onClick = { savePanelSettings() },
                    enabled = session.hasChanges && !session.isSaving,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = PbCyan,
                        contentColor = Color(0xFF061018),
                        disabledBackgroundColor = PbCyan.copy(alpha = 0.35f),
                        disabledContentColor = Color(0xFF061018).copy(alpha = 0.5f),
                    ),
                    elevation = ButtonDefaults.elevation(0.dp, 0.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Icon(Icons.Filled.Check, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        stringResource(Res.string.settings_rd_panel_save),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        PanelEditorTabRow(selected = tab, onSelect = { tab = it })
        Spacer(Modifier.height(12.dp))

        when (tab) {
            PanelEditorTab.Buttons -> {
                ChannelGridBody(
                    title = "${stringResource(Res.string.settings_rd_panel_buttons).uppercase()} (${relays.count { it.enabled }}/$MAX_BUTTONS)",
                    hint = if (editMode) {
                        stringResource(Res.string.settings_rd_panel_move_hint)
                    } else {
                        stringResource(Res.string.settings_rd_panel_buttons_hint)
                    },
                    channels = relays,
                    columnsPerRow = if (isTablet()) 6 else if (isPhoneUi()) 2 else 3,
                    minRows = 2,
                    slotLabel = { ch -> stringResource(Res.string.settings_rd_panel_relay_label, ch.id + 1) },
                    editMode = editMode,
                    moveSourceId = moveSourceId,
                    onEditModeChange = {
                        editMode = it
                        if (!it) clearEditSelection()
                    },
                    onTapCard = { id ->
                        if (!editMode) return@ChannelGridBody
                        editingKind = PanelEditKind.Relay
                        editingId = id
                        moveSourceId = null
                    },
                    onSwap = { from, to ->
                        if (!editMode) return@ChannelGridBody
                        swapPanelRelayContent(from, to)
                        moveSourceId = null
                    },
                    onToggleEnabled = { id, enabled ->
                        if (!editMode) return@ChannelGridBody
                        updatePanelRelayChannel(id) { it.copy(enabled = enabled) }
                    },
                )
            }
            PanelEditorTab.Lights -> {
                LightsEditorBody(
                    dimmers = dimmers,
                    rgbChannels = rgbChannels,
                    rgbCap = rgbCap,
                    editMode = editMode,
                    moveSourceId = moveSourceId,
                    onEditModeChange = {
                        editMode = it
                        if (!it) clearEditSelection()
                    },
                    onTapDimmer = { id ->
                        if (!editMode) return@LightsEditorBody
                        editingKind = PanelEditKind.Dimmer
                        editingId = id
                        moveSourceId = null
                    },
                    onTapRgb = { id ->
                        if (!editMode) return@LightsEditorBody
                        editingKind = PanelEditKind.Rgb
                        editingId = id
                        moveSourceId = null
                    },
                    onSwapDimmers = { from, to ->
                        if (!editMode) return@LightsEditorBody
                        swapPanelDimmerContent(from, to)
                        moveSourceId = null
                    },
                    onToggleDimmer = { id, enabled ->
                        if (!editMode) return@LightsEditorBody
                        updatePanelDimmerChannel(id) { it.copy(enabled = enabled) }
                    },
                    onToggleRgb = { id, enabled ->
                        if (!editMode) return@LightsEditorBody
                        updatePanelRgbChannel(id) { it.copy(enabled = enabled) }
                    },
                    onAddRgb = {
                        if (!editMode) return@LightsEditorBody
                        addPanelRgbChannel()
                    },
                    onRemoveRgb = { id ->
                        if (!editMode) return@LightsEditorBody
                        if (editingKind == PanelEditKind.Rgb && editingId == id) {
                            editingId = null
                            editingKind = null
                        }
                        removePanelRgbChannel(id)
                    },
                )
            }
            PanelEditorTab.Water -> {
                ChannelGridBody(
                    title = "${stringResource(Res.string.settings_rd_panel_water).uppercase()} (${waterTanks.count { it.enabled }}/${waterTanks.size})",
                    hint = if (editMode) {
                        stringResource(Res.string.settings_rd_panel_water_move_hint)
                    } else {
                        stringResource(Res.string.settings_rd_panel_water_hint)
                    },
                    channels = waterTanks,
                    columnsPerRow = waterTanks.size.coerceAtLeast(1),
                    minRows = 1,
                    slotLabel = { ch -> stringResource(Res.string.settings_rd_panel_tank_label, ch.id + 1) },
                    editMode = editMode,
                    moveSourceId = moveSourceId,
                    onEditModeChange = {
                        editMode = it
                        if (!it) clearEditSelection()
                    },
                    onTapCard = { id ->
                        if (!editMode) return@ChannelGridBody
                        editingKind = PanelEditKind.Water
                        editingId = id
                        moveSourceId = null
                    },
                    onSwap = { from, to ->
                        if (!editMode) return@ChannelGridBody
                        swapPanelWaterContent(from, to)
                        moveSourceId = null
                    },
                    onToggleEnabled = { id, enabled ->
                        if (!editMode) return@ChannelGridBody
                        updatePanelWaterChannel(id) { it.copy(enabled = enabled) }
                    },
                )
            }
            PanelEditorTab.Climate -> {
                ChannelGridBody(
                    title = "${stringResource(Res.string.settings_rd_panel_climate).uppercase()} (${sensors.count { it.enabled }}/${sensors.size})",
                    hint = if (editMode) {
                        stringResource(Res.string.settings_rd_panel_climate_move_hint)
                    } else {
                        stringResource(Res.string.settings_rd_panel_climate_hint)
                    },
                    channels = sensors,
                    columnsPerRow = if (isTablet()) maxOf(2, sensors.size) else 2,
                    minRows = 1,
                    slotLabel = { ch -> stringResource(Res.string.settings_rd_panel_sensor_label, ch.id + 1) },
                    editMode = editMode,
                    moveSourceId = moveSourceId,
                    onEditModeChange = {
                        editMode = it
                        if (!it) clearEditSelection()
                    },
                    onTapCard = { id ->
                        if (!editMode) return@ChannelGridBody
                        editingKind = PanelEditKind.Sensor
                        editingId = id
                        moveSourceId = null
                    },
                    onSwap = { from, to ->
                        if (!editMode) return@ChannelGridBody
                        swapPanelSensorContent(from, to)
                        moveSourceId = null
                    },
                    onToggleEnabled = { id, enabled ->
                        if (!editMode) return@ChannelGridBody
                        val sensor = sensors.firstOrNull { it.id == id } ?: return@ChannelGridBody
                        setPanelSensorEnabled(sensor, enabled)
                    },
                )
            }
            PanelEditorTab.Motors -> {
                TabletPanelMotorGrid(
                    sectionTitle = "${stringResource(Res.string.settings_rd_panel_motors).uppercase()} (${motorChannels.count { it.enabled }}/$motorCap)",
                    channels = motorChannels,
                    motorCap = motorCap,
                    modifier = Modifier.fillMaxSize(),
                    onToggle = { id, enabled -> updatePanelMotorChannel(id) { it.copy(enabled = enabled) } },
                    onNameChange = { id, name -> updatePanelMotorChannel(id) { it.copy(name = name) } },
                    onIconChange = { id, icon -> updatePanelMotorChannel(id) { it.copy(icon = icon) } },
                    onAdd = { addPanelMotorChannel() },
                    onRemove = { id ->
                        if (editingKind == PanelEditKind.Motor && editingId == id) {
                            editingId = null
                            editingKind = null
                        }
                        removePanelMotorChannel(id)
                    },
                    onPreview = {
                        dashboardSettingsDetailOpen = false
                        dashboardShowSettings = false
                        dashboardNavigateToSection = DashboardSection.Motor
                    },
                )
            }
        }
        }
    }

    if (editingChannel != null && editMode && editingKind != null) {
        val dialogTitle = when (editingKind) {
            PanelEditKind.Relay -> stringResource(Res.string.settings_rd_panel_edit_title)
            PanelEditKind.Dimmer, PanelEditKind.Rgb -> stringResource(Res.string.settings_rd_panel_edit_light)
            PanelEditKind.Water -> stringResource(Res.string.settings_rd_panel_edit_tank)
            PanelEditKind.Sensor -> stringResource(Res.string.settings_rd_panel_edit_sensor)
            PanelEditKind.Motor -> stringResource(Res.string.settings_rd_panel_edit_motor)
            null -> stringResource(Res.string.settings_rd_panel_edit_title)
        }
        ButtonEditDialog(
            channel = editingChannel,
            title = dialogTitle,
            showSensorType = editingKind == PanelEditKind.Sensor,
            showIconPicker = editingKind == PanelEditKind.Relay || editingKind == PanelEditKind.Rgb,
            showCapacityLiters = editingKind == PanelEditKind.Water,
            onDismiss = { editingId = null; editingKind = null },
            onSave = { name, iconKey, sensorType, capacityLiters ->
                val trimmed = name.trim().ifEmpty { editingChannel.name }
                when (editingKind) {
                    PanelEditKind.Relay -> updatePanelRelayChannel(editingChannel.id) {
                        it.copy(name = trimmed, icon = iconKey)
                    }
                    PanelEditKind.Dimmer -> updatePanelDimmerChannel(editingChannel.id) {
                        it.copy(name = trimmed)
                    }
                    PanelEditKind.Rgb -> updatePanelRgbChannel(editingChannel.id) {
                        it.copy(name = trimmed, icon = iconKey)
                    }
                    PanelEditKind.Water -> updatePanelWaterChannel(editingChannel.id) {
                        it.copy(
                            name = trimmed,
                            capacityLiters = (capacityLiters ?: it.capacityLiters).coerceIn(0, 10_000),
                        )
                    }
                    PanelEditKind.Motor -> updatePanelMotorChannel(editingChannel.id) {
                        it.copy(name = trimmed)
                    }
                    PanelEditKind.Sensor -> {
                        val nextType = sensorType ?: editingChannel.sensorType
                        if (sensorType != null) {
                            setPanelSensorType(editingChannel.id, sensorType, updateName = false)
                        }
                        updatePanelSensorChannel(editingChannel.id) {
                            it.copy(
                                name = trimmed,
                                sensorType = normalizePanelSensorType(nextType),
                            )
                        }
                    }
                    null -> {}
                }
                editingId = null
                editingKind = null
            },
        )
    }
}

@Composable
private fun PanelEditorTabRow(
    selected: PanelEditorTab,
    onSelect: (PanelEditorTab) -> Unit,
) {
    val tabs = listOf(
        PanelEditorTab.Buttons to (Icons.Filled.GridView to Res.string.settings_rd_panel_buttons),
        PanelEditorTab.Lights to (Icons.Filled.WbSunny to Res.string.settings_rd_panel_lights),
        PanelEditorTab.Water to (Icons.Filled.WaterDrop to Res.string.settings_rd_panel_water),
        PanelEditorTab.Climate to (Icons.Filled.Thermostat to Res.string.settings_rd_panel_climate),
        PanelEditorTab.Motors to (Icons.Filled.Settings to Res.string.settings_rd_panel_motors),
    )
    val phoneUi = isPhoneUi()
    val scroll = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (phoneUi) Modifier.horizontalScroll(scroll) else Modifier)
            .background(PbCard.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
            .border(
                width = 1.dp,
                color = PbCyan.copy(alpha = 0.28f),
                shape = RoundedCornerShape(10.dp),
            )
            .padding(horizontal = if (phoneUi) 6.dp else 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(if (phoneUi) 6.dp else 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tabs.forEach { (tab, pair) ->
            val (icon, labelRes) = pair
            val active = selected == tab
            Row(
                modifier = Modifier
                    .then(if (phoneUi) Modifier else Modifier.weight(1f))
                    .clickable { onSelect(tab) }
                    .background(
                        if (active) PbCyan.copy(alpha = 0.12f) else Color.Transparent,
                        RoundedCornerShape(8.dp),
                    )
                    .border(
                        width = 1.dp,
                        color = if (active) PbCyan.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(8.dp),
                    )
                    .padding(horizontal = if (phoneUi) 10.dp else 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    icon,
                    null,
                    tint = if (active) PbCyan else PbMuted,
                    modifier = Modifier.size(if (phoneUi) 26.dp else 24.dp),
                )
                if (!phoneUi) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(labelRes),
                        color = if (active) PbText else PbMuted,
                        fontSize = 12.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun ChannelGridBody(
    title: String,
    hint: String,
    channels: List<PanelPlatformChannel>,
    columnsPerRow: Int,
    minRows: Int,
    slotLabel: @Composable (PanelPlatformChannel) -> String,
    editMode: Boolean,
    moveSourceId: Int?,
    onEditModeChange: (Boolean) -> Unit,
    onTapCard: (Int) -> Unit,
    onSwap: (Int, Int) -> Unit,
    onToggleEnabled: (Int, Boolean) -> Unit,
    onRemove: ((Int) -> Unit)? = null,
    showChannelIcon: Boolean = true,
    extraBelow: (@Composable ColumnScope.() -> Unit)? = null,
) {
    var cardCenters by remember { mutableStateOf(mapOf<Int, Offset>()) }
    var draggingId by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var hoverTargetId by remember { mutableStateOf<Int?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        ChannelEditorHeader(
            title = title,
            hint = hint,
            editMode = editMode,
            onEditModeChange = onEditModeChange,
        )
        Spacer(Modifier.height(10.dp))

        val rows = remember(channels, columnsPerRow) { channels.chunked(columnsPerRow.coerceAtLeast(1)) }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            rows.forEach { rowChannels ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    rowChannels.forEach { channel ->
                        val isDragging = draggingId == channel.id
                        val isMoveSource = moveSourceId == channel.id || hoverTargetId == channel.id
                        PanelButtonCard(
                            channel = channel,
                            slotLabel = slotLabel(channel),
                            editMode = editMode,
                            highlighted = isMoveSource || isDragging,
                            dragOffset = if (isDragging) dragOffset else Offset.Zero,
                            showChannelIcon = showChannelIcon,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .zIndex(if (isDragging) 2f else 1f)
                                .onGloballyPositioned { coords ->
                                    val pos = coords.positionInRoot()
                                    val center = Offset(
                                        pos.x + coords.size.width / 2f,
                                        pos.y + coords.size.height / 2f,
                                    )
                                    cardCenters = cardCenters + (channel.id to center)
                                }
                                .then(
                                    if (editMode) {
                                        Modifier
                                            .clickable { onTapCard(channel.id) }
                                            .pointerInput(channel.id) {
                                                detectDragGesturesAfterLongPress(
                                                    onDragStart = {
                                                        draggingId = channel.id
                                                        dragOffset = Offset.Zero
                                                        hoverTargetId = null
                                                    },
                                                    onDrag = { change, dragAmount ->
                                                        dragOffset += dragAmount
                                                        val origin = cardCenters[channel.id]
                                                        if (origin != null) {
                                                            val finger = origin + dragOffset
                                                            hoverTargetId = cardCenters
                                                                .filterKeys { it != channel.id }
                                                                .minByOrNull { (_, c) -> (c - finger).getDistance() }
                                                                ?.takeIf { (it.value - finger).getDistance() < 160f }
                                                                ?.key
                                                        }
                                                        change.consume()
                                                    },
                                                    onDragEnd = {
                                                        val target = hoverTargetId
                                                        if (target != null && target != channel.id) {
                                                            onSwap(channel.id, target)
                                                        }
                                                        draggingId = null
                                                        dragOffset = Offset.Zero
                                                        hoverTargetId = null
                                                    },
                                                    onDragCancel = {
                                                        draggingId = null
                                                        dragOffset = Offset.Zero
                                                        hoverTargetId = null
                                                    },
                                                )
                                            }
                                    } else {
                                        Modifier
                                    },
                                ),
                            onToggleEnabled = { onToggleEnabled(channel.id, it) },
                            onRemove = onRemove?.let { rm -> { rm(channel.id) } },
                        )
                    }
                    repeat((columnsPerRow - rowChannels.size).coerceAtLeast(0)) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
            extraBelow?.let { it() }
            repeat((minRows - rows.size).coerceAtLeast(0)) {
                Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ChannelEditorHeader(
    title: String,
    hint: String,
    editMode: Boolean,
    onEditModeChange: (Boolean) -> Unit,
) {
    val phoneUi = isPhoneUi()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = if (phoneUi) Alignment.CenterVertically else Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                color = PbText,
                fontSize = if (phoneUi) 13.sp else 15.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = if (phoneUi) 0.2.sp else 0.4.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!phoneUi) {
                Spacer(Modifier.height(4.dp))
                Text(hint, color = PbMuted, fontSize = 12.sp)
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (phoneUi) 4.dp else 8.dp),
        ) {
            if (!phoneUi) {
                Text(
                    stringResource(Res.string.settings_rd_panel_edit_mode),
                    color = PbMuted,
                    fontSize = 12.sp,
                )
            }
            Switch(
                checked = editMode,
                onCheckedChange = onEditModeChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = PbCyan,
                    uncheckedThumbColor = Color.White.copy(alpha = 0.85f),
                    uncheckedTrackColor = PbMuted.copy(alpha = 0.45f),
                ),
            )
        }
    }
    if (phoneUi) {
        Spacer(Modifier.height(4.dp))
        Text(hint, color = PbMuted, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun LightsEditorBody(
    dimmers: List<PanelPlatformChannel>,
    rgbChannels: List<PanelPlatformChannel>,
    rgbCap: Int,
    editMode: Boolean,
    moveSourceId: Int?,
    onEditModeChange: (Boolean) -> Unit,
    onTapDimmer: (Int) -> Unit,
    onTapRgb: (Int) -> Unit,
    onSwapDimmers: (Int, Int) -> Unit,
    onToggleDimmer: (Int, Boolean) -> Unit,
    onToggleRgb: (Int, Boolean) -> Unit,
    onAddRgb: () -> Unit,
    onRemoveRgb: (Int) -> Unit,
) {
    val rgbLabel = stringResource(Res.string.settings_rd_panel_rgb_label)
    ChannelGridBody(
        title = "${stringResource(Res.string.settings_rd_panel_lights).uppercase()} (${(dimmers + rgbChannels).count { it.enabled }}/${dimmers.size + rgbCap})",
        hint = if (editMode) {
            stringResource(Res.string.settings_rd_panel_lights_add_hint)
        } else {
            stringResource(Res.string.settings_rd_panel_lights_hint)
        },
        channels = dimmers,
        columnsPerRow = if (isTablet()) 4 else 2,
        minRows = 2,
        slotLabel = { ch -> stringResource(Res.string.settings_rd_panel_dimmer_label, ch.id + 1) },
        editMode = editMode,
        moveSourceId = moveSourceId,
        showChannelIcon = false,
        onEditModeChange = onEditModeChange,
        onTapCard = onTapDimmer,
        onSwap = onSwapDimmers,
        onToggleEnabled = onToggleDimmer,
        extraBelow = {
            val canAddRgb = editMode && rgbChannels.size < rgbCap
            if (rgbChannels.isNotEmpty() || canAddRgb) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.85f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    rgbChannels.forEach { channel ->
                        PanelButtonCard(
                            channel = channel,
                            slotLabel = rgbLabel,
                            editMode = editMode,
                            highlighted = false,
                            dragOffset = Offset.Zero,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .then(
                                    if (editMode) {
                                        Modifier.clickable { onTapRgb(channel.id) }
                                    } else {
                                        Modifier
                                    },
                                ),
                            onToggleEnabled = { onToggleRgb(channel.id, it) },
                            onRemove = if (editMode) ({ onRemoveRgb(channel.id) }) else null,
                        )
                    }
                    if (canAddRgb) {
                        AddChannelSlot(
                            label = stringResource(Res.string.settings_rd_panel_add_rgb),
                            modifier = Modifier.weight(1f).fillMaxSize(),
                            onClick = onAddRgb,
                        )
                    }
                    val fillers = (4 - rgbChannels.size - if (canAddRgb) 1 else 0).coerceAtLeast(0)
                    repeat(fillers) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        },
    )
}

@Composable
private fun PanelButtonCard(
    channel: PanelPlatformChannel,
    slotLabel: String,
    editMode: Boolean,
    highlighted: Boolean,
    dragOffset: Offset,
    modifier: Modifier = Modifier,
    showChannelIcon: Boolean = true,
    onToggleEnabled: (Boolean) -> Unit,
    onRemove: (() -> Unit)? = null,
) {
    val catalogIcon = PanelIconCatalog.find(channel.icon)
    val preset = PanelConfigMapper.iconToPreset(channel.icon)
    val legacyIconRes = if (catalogIcon == null) {
        runCatching {
            getAppContext().vanGO.typeOfIcon(preset) as DrawableResource
        }.getOrNull()
    } else {
        null
    }
    val tint = catalogIcon?.tint ?: iconTintForPreset(preset)

    val phoneUi = isPhoneUi()
    Column(
        modifier = modifier
            .offset { IntOffset(dragOffset.x.roundToInt(), dragOffset.y.roundToInt()) }
            .alpha(if (channel.enabled) 1f else 0.55f)
            .background(PbCard, RoundedCornerShape(if (phoneUi) 12.dp else 14.dp))
            .border(
                width = if (highlighted) 2.dp else 1.dp,
                color = if (highlighted) PbCyan else PbBorder,
                shape = RoundedCornerShape(if (phoneUi) 12.dp else 14.dp),
            )
            .padding(
                horizontal = if (phoneUi) 6.dp else 8.dp,
                vertical = if (phoneUi) 8.dp else 10.dp,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(18.dp)) {
            if (editMode && onRemove != null) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = stringResource(Res.string.settings_rd_panel_remove),
                    tint = Color(0xFFFF6B6B),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .size(18.dp)
                        .clickable(onClick = onRemove),
                )
            }
            if (editMode) {
                Icon(
                    Icons.Filled.DragIndicator,
                    null,
                    tint = PbMuted,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(18.dp),
                )
            }
        }
        Text(
            slotLabel,
            color = PbMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
        Spacer(Modifier.height(2.dp))
        if (showChannelIcon) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.2f, fill = true),
                contentAlignment = Alignment.Center,
            ) {
                val iconSize = minOf(maxWidth.value * 0.82f, maxHeight.value * 0.84f)
                    .coerceIn(72f, 112f)
                    .dp
                if (catalogIcon != null) {
                    PanelCatalogIcon(def = catalogIcon, size = iconSize, tint = tint)
                } else if (legacyIconRes != null) {
                    Image(
                        painter = painterResource(legacyIconRes),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(tint),
                        modifier = Modifier.size(iconSize),
                    )
                } else {
                    Icon(Icons.Filled.WbSunny, null, tint = tint, modifier = Modifier.size(iconSize))
                }
            }
        } else {
            Spacer(Modifier.weight(1.2f, fill = true))
        }
        Text(
            channel.name.ifBlank { "CH ${channel.id + 1}" },
            color = PbText,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            lineHeight = 17.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp, bottom = 4.dp),
        )
        Spacer(modifier.weight(0.55f))
        Switch(
            checked = channel.enabled,
            onCheckedChange = onToggleEnabled,
            enabled = editMode,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = PbCyan,
                uncheckedThumbColor = Color.White.copy(alpha = 0.85f),
                uncheckedTrackColor = PbMuted.copy(alpha = 0.4f),
            ),
        )
    }
}

@Composable
private fun AddChannelSlot(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .background(PbCard, RoundedCornerShape(14.dp))
            .border(1.dp, PbCyan.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Filled.Add, null, tint = PbCyan, modifier = Modifier.size(36.dp))
        Spacer(Modifier.height(8.dp))
        Text(
            label,
            color = PbCyan,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ButtonEditDialog(
    channel: PanelPlatformChannel,
    title: String,
    showSensorType: Boolean,
    showIconPicker: Boolean = true,
    showCapacityLiters: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (name: String, iconKey: String, sensorType: String?, capacityLiters: Int?) -> Unit,
) {
    var name by remember(channel.id) { mutableStateOf(channel.name) }
    var iconKey by remember(channel.id) {
        mutableStateOf(
            PanelIconCatalog.find(channel.icon)?.key
                ?: PanelIconCatalog.resolveKeyFromLegacyPreset(
                    PanelConfigMapper.iconToPreset(channel.icon),
                ),
        )
    }
    var sensorType by remember(channel.id) {
        mutableStateOf(normalizePanelSensorType(channel.sensorType))
    }
    var capacityText by remember(channel.id) {
        mutableStateOf(
            channel.capacityLiters.takeIf { it > 0 }?.toString().orEmpty(),
        )
    }
    val categories = remember { PanelIconCategory.entries.toList() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .heightIn(max = 720.dp)
                    .background(PbBg, RoundedCornerShape(16.dp))
                    .border(1.dp, PbBorder, RoundedCornerShape(16.dp))
                    .clickable(enabled = false) {}
                    .padding(horizontal = 16.dp, vertical = 18.dp),
            ) {
                Text(
                    title,
                    color = PbText,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    stringResource(Res.string.settings_rd_panel_name),
                    color = PbMuted,
                    fontSize = 12.sp,
                )
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        textColor = PbText,
                        backgroundColor = PbField,
                        cursorColor = PbCyan,
                        focusedBorderColor = PbCyan.copy(alpha = 0.7f),
                        unfocusedBorderColor = PbBorder,
                    ),
                )
                if (showCapacityLiters) {
                    Spacer(Modifier.height(14.dp))
                    Text(
                        stringResource(Res.string.settings_rd_panel_tank_capacity),
                        color = PbMuted,
                        fontSize = 12.sp,
                    )
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = capacityText,
                        onValueChange = { raw ->
                            capacityText = raw.filter { it.isDigit() }.take(5)
                        },
                        singleLine = true,
                        placeholder = {
                            Text(
                                stringResource(Res.string.settings_rd_panel_tank_capacity_hint),
                                color = PbMuted.copy(alpha = 0.7f),
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            textColor = PbText,
                            backgroundColor = PbField,
                            cursorColor = PbCyan,
                            focusedBorderColor = PbCyan.copy(alpha = 0.7f),
                            unfocusedBorderColor = PbBorder,
                        ),
                    )
                }
                Spacer(Modifier.height(14.dp))
                if (showSensorType) {
                    Text(
                        stringResource(Res.string.settings_rd_panel_sensor_type),
                        color = PbMuted,
                        fontSize = 12.sp,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        PanelSensorTypes.forEach { type ->
                            val selected = sensorType == type
                            Text(
                                sensorTypeLabel(type),
                                color = if (selected) Color(0xFF061018) else PbText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (selected) PbCyan else PbField,
                                        RoundedCornerShape(8.dp),
                                    )
                                    .border(
                                        1.dp,
                                        if (selected) PbCyan else PbBorder,
                                        RoundedCornerShape(8.dp),
                                    )
                                    .clickable {
                                        sensorType = type
                                        name = sensorDefaultName(type, channel.id)
                                    }
                                    .padding(horizontal = 6.dp, vertical = 8.dp),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                }
                if (showIconPicker) {
                Text(
                    stringResource(Res.string.settings_rd_panel_icon),
                    color = PbMuted,
                    fontSize = 12.sp,
                )
                Spacer(Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    categories.forEach { category ->
                        val icons = PanelIconCatalog.all.filter { it.category == category }
                        Text(
                            panelIconCategoryTitle(category),
                            color = PbCyan,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        icons.chunked(4).forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                row.forEach { def ->
                                    val selected = iconKey == def.key
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(
                                                if (selected) PbCyan.copy(alpha = 0.14f) else PbField,
                                                RoundedCornerShape(10.dp),
                                            )
                                            .border(
                                                1.dp,
                                                if (selected) PbCyan.copy(alpha = 0.7f) else PbBorder,
                                                RoundedCornerShape(10.dp),
                                            )
                                            .clickable {
                                                iconKey = def.key
                                                name = def.labelTr
                                            }
                                            .padding(vertical = 10.dp, horizontal = 6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                    ) {
                                        PanelCatalogIcon(
                                            def = def,
                                            size = 40.dp,
                                            tint = def.tint,
                                        )
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                            def.labelTr,
                                            color = PbText,
                                            fontSize = 11.sp,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center,
                                            lineHeight = 13.sp,
                                        )
                                    }
                                }
                                repeat((4 - row.size).coerceAtLeast(0)) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
                }
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(Res.string.panel_preset_cancel), color = PbMuted)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val capacity = if (showCapacityLiters) {
                                capacityText.toIntOrNull()?.coerceIn(0, 10_000)
                            } else {
                                null
                            }
                            onSave(
                                name,
                                iconKey,
                                if (showSensorType) sensorType else null,
                                capacity,
                            )
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = PbCyan,
                            contentColor = Color(0xFF061018),
                        ),
                        elevation = ButtonDefaults.elevation(0.dp, 0.dp),
                    ) {
                        Text(
                            stringResource(Res.string.panel_preset_confirm),
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

private val PbPhoneGreen = Color(0xFF3DDC84)

private data class PhonePanelTabMeta(
    val tab: PanelEditorTab,
    val icon: ImageVector,
    val labelRes: org.jetbrains.compose.resources.StringResource,
    val titleRes: org.jetbrains.compose.resources.StringResource,
    val descRes: org.jetbrains.compose.resources.StringResource,
)

@Composable
private fun phonePanelTabMeta(): List<PhonePanelTabMeta> = listOf(
    PhonePanelTabMeta(
        PanelEditorTab.Buttons,
        Icons.Filled.GridView,
        Res.string.settings_rd_panel_tab_buttons_short,
        Res.string.settings_rd_panel_buttons,
        Res.string.settings_rd_panel_phone_buttons_desc,
    ),
    PhonePanelTabMeta(
        PanelEditorTab.Lights,
        Icons.Filled.WbSunny,
        Res.string.settings_rd_panel_tab_dimmer,
        Res.string.settings_rd_panel_tab_dimmer,
        Res.string.settings_rd_panel_phone_lights_desc,
    ),
    PhonePanelTabMeta(
        PanelEditorTab.Water,
        Icons.Filled.WaterDrop,
        Res.string.settings_rd_panel_tab_water_short,
        Res.string.settings_rd_panel_tab_water_short,
        Res.string.settings_rd_panel_phone_water_desc,
    ),
    PhonePanelTabMeta(
        PanelEditorTab.Climate,
        Icons.Filled.Thermostat,
        Res.string.settings_rd_panel_tab_climate_short,
        Res.string.settings_rd_panel_tab_climate_short,
        Res.string.settings_rd_panel_phone_climate_desc,
    ),
    PhonePanelTabMeta(
        PanelEditorTab.Motors,
        Icons.Filled.Settings,
        Res.string.settings_rd_panel_tab_motor_short,
        Res.string.settings_rd_panel_tab_motor_short,
        Res.string.settings_rd_panel_phone_motors_desc,
    ),
)

@Composable
private fun PhonePanelEditorPhoneLayout(
    tab: PanelEditorTab,
    onTabChange: (PanelEditorTab) -> Unit,
    editMode: Boolean,
    onEditModeChange: (Boolean) -> Unit,
    onLock: () -> Unit,
    defaults: PanelPlatformChannels,
    relays: List<PanelPlatformChannel>,
    dimmers: List<PanelPlatformChannel>,
    rgbChannels: List<PanelPlatformChannel>,
    rgbCap: Int,
    waterTanks: List<PanelPlatformChannel>,
    sensors: List<PanelPlatformChannel>,
    motorChannels: List<PanelPlatformChannel>,
    motorCap: Int,
    onEditRelay: (Int) -> Unit,
    onEditDimmer: (Int) -> Unit,
    onEditRgb: (Int) -> Unit,
    onEditWater: (Int) -> Unit,
    onEditSensor: (Int) -> Unit,
    onEditMotor: (Int) -> Unit,
    hasChanges: Boolean,
    isSaving: Boolean,
    onSave: () -> Unit,
) {
    val scroll = rememberScrollState()
    val tabMeta = phonePanelTabMeta()
    val activeMeta = tabMeta.first { it.tab == tab }

    fun resetTab() {
        when (tab) {
            PanelEditorTab.Buttons -> replacePanelRelayChannels(defaults.relays)
            PanelEditorTab.Lights -> {
                replacePanelDimmerChannels(defaults.dimmers)
                replacePanelRgbChannels(defaults.rgb)
            }
            PanelEditorTab.Water -> replacePanelWaterChannels(defaults.water)
            PanelEditorTab.Climate -> replacePanelSensorChannels(defaults.sensors)
            PanelEditorTab.Motors -> replacePanelMotorChannels(defaults.motors)
        }
        onEditModeChange(false)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll),
    ) {
        PhonePanelCategoryHeader(
            icon = activeMeta.icon,
            title = stringResource(activeMeta.titleRes),
            description = stringResource(activeMeta.descRes),
            onLock = onLock,
            onRefresh = { resetTab() },
        )
        Spacer(Modifier.height(if (tab == PanelEditorTab.Water) 8.dp else 12.dp))
        PhonePanelSquareTabRow(selected = tab, tabs = tabMeta, onSelect = onTabChange)
        Spacer(Modifier.height(if (tab == PanelEditorTab.Water) 8.dp else 14.dp))

        when (tab) {
            PanelEditorTab.Buttons -> PhonePanelToggleGrid(
                sectionTitle = "${stringResource(Res.string.settings_rd_panel_buttons).uppercase()} (${relays.count { it.enabled }}/$MAX_BUTTONS)",
                channels = relays,
                editMode = editMode,
                onEditModeChange = onEditModeChange,
                displayName = { ch ->
                    ch.name.trim().ifEmpty { stringResource(Res.string.settings_rd_panel_button_slot, ch.id + 1) }
                },
                onToggle = { id, enabled -> updatePanelRelayChannel(id) { it.copy(enabled = enabled) } },
                onTap = { id -> if (editMode) onEditRelay(id) },
            )
            PanelEditorTab.Lights -> {
                PhonePanelToggleGrid(
                    sectionTitle = "${stringResource(Res.string.settings_rd_panel_tab_dimmer).uppercase()} (${dimmers.count { it.enabled }}/${dimmers.size})",
                    channels = dimmers,
                    editMode = editMode,
                    showChannelIcon = false,
                    statusBelowSwitch = true,
                    nameMaxLines = 2,
                    onEditModeChange = onEditModeChange,
                    displayName = { ch ->
                        ch.name.trim().ifEmpty { stringResource(Res.string.settings_rd_panel_dimmer_label, ch.id + 1) }
                    },
                    onToggle = { id, enabled -> updatePanelDimmerChannel(id) { it.copy(enabled = enabled) } },
                    onTap = { id -> if (editMode) onEditDimmer(id) },
                )
                Spacer(Modifier.height(14.dp))
                PhonePanelRgbSection(
                    rgbChannels = rgbChannels,
                    rgbCap = rgbCap,
                    editMode = editMode,
                    onToggle = { id, enabled -> updatePanelRgbChannel(id) { it.copy(enabled = enabled) } },
                    onTap = { id -> if (editMode) onEditRgb(id) },
                    onAdd = { addPanelRgbChannel() },
                    onRemove = { id ->
                        if (editMode) removePanelRgbChannel(id)
                    },
                )
            }
            PanelEditorTab.Water -> PhonePanelWaterGrid(
                sectionTitle = "${stringResource(Res.string.settings_rd_panel_water)} (${waterTanks.count { it.enabled }}/${waterTanks.size})",
                hint = if (editMode) {
                    stringResource(Res.string.settings_rd_panel_water_move_hint)
                } else {
                    stringResource(Res.string.settings_rd_panel_phone_water_list_hint)
                },
                channels = waterTanks,
                editMode = editMode,
                onEditModeChange = onEditModeChange,
                onToggle = { id, enabled -> updatePanelWaterChannel(id) { it.copy(enabled = enabled) } },
                onToggleShowOnHome = { id, show ->
                    updatePanelWaterChannel(id) { it.copy(showOnHome = show) }
                },
                onTap = { id -> if (editMode) onEditWater(id) },
            )
            PanelEditorTab.Climate -> PhonePanelToggleGrid(
                sectionTitle = "${stringResource(Res.string.settings_rd_panel_tab_climate_short).uppercase()} (${sensors.count { it.enabled }}/${sensors.size})",
                channels = sensors,
                editMode = editMode,
                onEditModeChange = onEditModeChange,
                displayName = { ch ->
                    ch.name.trim().ifEmpty { stringResource(Res.string.settings_rd_panel_sensor_label, ch.id + 1) }
                },
                onToggle = { id, enabled ->
                    sensors.firstOrNull { it.id == id }?.let { setPanelSensorEnabled(it, enabled) }
                },
                onTap = { id -> if (editMode) onEditSensor(id) },
            )
            PanelEditorTab.Motors -> PhonePanelMotorGrid(
                sectionTitle = "${stringResource(Res.string.settings_rd_panel_motors).uppercase()} (${motorChannels.count { it.enabled }}/$motorCap)",
                channels = motorChannels,
                onToggle = { id, enabled -> updatePanelMotorChannel(id) { it.copy(enabled = enabled) } },
                onNameChange = { id, name -> updatePanelMotorChannel(id) { it.copy(name = name) } },
                onTypeChange = { id, typeKey, iconKey ->
                    updatePanelMotorChannel(id) { it.withPanelMotorType(typeKey, iconKey) }
                },
                onIconChange = { id, icon -> updatePanelMotorChannel(id) { it.copy(icon = icon) } },
                onPreview = {
                    dashboardSettingsDetailOpen = false
                    dashboardShowSettings = false
                    dashboardNavigateToSection = DashboardSection.Motor
                },
            )
        }

        Spacer(Modifier.height(if (tab == PanelEditorTab.Water) 8.dp else 14.dp))
        if (tab == PanelEditorTab.Water) {
            PhonePanelInfoBanner(text = stringResource(Res.string.settings_rd_panel_phone_water_passive_info))
        } else {
            PhonePanelInfoBanner()
        }
        if (hasChanges) {
            Spacer(Modifier.height(if (tab == PanelEditorTab.Water) 8.dp else 12.dp))
            PhonePanelSaveBar(
                isSaving = isSaving,
                onSave = onSave,
            )
        }
        Spacer(Modifier.height(if (tab == PanelEditorTab.Water) 4.dp else 8.dp))
    }
}

@Composable
private fun PhonePanelCategoryHeader(
    icon: ImageVector,
    title: String,
    description: String,
    onLock: () -> Unit,
    onRefresh: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(icon, null, tint = PbCyan, modifier = Modifier.size(26.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    color = PbText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    description,
                    color = PbMuted,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.clickable(onClick = onLock),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    stringResource(Res.string.settings_rd_lock_again),
                    color = PbCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Icon(Icons.Filled.LockOpen, null, tint = PbCyan, modifier = Modifier.size(16.dp))
            }
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(PbCard, RoundedCornerShape(16.dp))
                    .border(1.dp, PbBorder, RoundedCornerShape(16.dp))
                    .clickable(onClick = onRefresh),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Refresh, null, tint = PbMuted, modifier = Modifier.size(17.dp))
            }
        }
    }
}

@Composable
private fun PhonePanelSquareTabRow(
    selected: PanelEditorTab,
    tabs: List<PhonePanelTabMeta>,
    onSelect: (PanelEditorTab) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PbCard, RoundedCornerShape(12.dp))
            .border(1.dp, PbBorder, RoundedCornerShape(12.dp))
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tabs.forEachIndexed { index, meta ->
            val active = selected == meta.tab
            if (index > 0) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(44.dp)
                        .background(PbBorder.copy(alpha = 0.85f)),
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelect(meta.tab) }
                    .then(
                        if (active) {
                            Modifier
                                .background(PbCyan.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                                .border(1.5.dp, PbCyan, RoundedCornerShape(10.dp))
                        } else {
                            Modifier
                        },
                    )
                    .padding(vertical = 8.dp, horizontal = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    meta.icon,
                    null,
                    tint = if (active) PbCyan else PbText.copy(alpha = 0.85f),
                    modifier = Modifier.size(28.dp),
                )
                Text(
                    stringResource(meta.labelRes),
                    color = if (active) PbCyan else PbText.copy(alpha = 0.8f),
                    fontSize = 9.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun PhonePanelRgbSection(
    rgbChannels: List<PanelPlatformChannel>,
    rgbCap: Int,
    editMode: Boolean,
    onToggle: (Int, Boolean) -> Unit,
    onTap: (Int) -> Unit,
    onAdd: () -> Unit,
    onRemove: (Int) -> Unit,
) {
    val canAdd = editMode && rgbChannels.size < rgbCap
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "${stringResource(Res.string.settings_rd_panel_rgb_label).uppercase()} (${rgbChannels.count { it.enabled }}/$rgbCap)",
            color = PbText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.4.sp,
        )
        rgbChannels.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                row.forEach { channel ->
                    Box(modifier = Modifier.weight(1f)) {
                        PhonePanelToggleRow(
                            channel = channel,
                            label = channel.name.trim().ifEmpty {
                                stringResource(Res.string.settings_rd_panel_rgb_label)
                            },
                            editMode = editMode,
                            showChannelIcon = true,
                            statusBelowSwitch = true,
                            nameMaxLines = 2,
                            onToggle = { onToggle(channel.id, it) },
                            onTap = { onTap(channel.id) },
                        )
                        if (editMode) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = stringResource(Res.string.settings_rd_panel_remove),
                                tint = Color(0xFFFF6B6B),
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(6.dp)
                                    .size(18.dp)
                                    .clickable { onRemove(channel.id) },
                            )
                        }
                    }
                }
                if (row.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
        if (canAdd) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(PbCard, RoundedCornerShape(14.dp))
                    .border(1.dp, PbCyan.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                    .clickable(onClick = onAdd)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(Icons.Filled.Add, null, tint = PbCyan, modifier = Modifier.size(20.dp))
                Text(
                    stringResource(Res.string.settings_rd_panel_add_rgb),
                    color = PbCyan,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun PhonePanelWaterGrid(
    sectionTitle: String,
    hint: String,
    channels: List<PanelPlatformChannel>,
    editMode: Boolean,
    onEditModeChange: (Boolean) -> Unit,
    onToggle: (Int, Boolean) -> Unit,
    onToggleShowOnHome: (Int, Boolean) -> Unit,
    onTap: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                sectionTitle,
                color = PbText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    stringResource(Res.string.settings_rd_panel_edit_mode),
                    color = PbMuted,
                    fontSize = 11.sp,
                    maxLines = 1,
                )
                Switch(
                    checked = editMode,
                    onCheckedChange = onEditModeChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = PbCyan,
                        uncheckedThumbColor = Color.White.copy(alpha = 0.85f),
                        uncheckedTrackColor = PbMuted.copy(alpha = 0.45f),
                    ),
                )
            }
        }
        Text(
            hint,
            color = PbMuted,
            fontSize = 10.sp,
            lineHeight = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            channels.forEach { channel ->
                PhonePanelWaterCard(
                    channel = channel,
                    editMode = editMode,
                    onToggle = { onToggle(channel.id, it) },
                    onToggleShowOnHome = { onToggleShowOnHome(channel.id, it) },
                    onTap = { onTap(channel.id) },
                )
            }
        }
    }
}

@Composable
private fun PhonePanelWaterCard(
    channel: PanelPlatformChannel,
    editMode: Boolean,
    onToggle: (Boolean) -> Unit,
    onToggleShowOnHome: (Boolean) -> Unit,
    onTap: () -> Unit,
) {
    val activeLabel = stringResource(Res.string.settings_rd_panel_status_active)
    val passiveLabel = stringResource(Res.string.settings_rd_panel_status_passive)
    val showLabel = stringResource(Res.string.settings_rd_panel_water_show_home)
    val hideLabel = stringResource(Res.string.settings_rd_panel_water_hide_home)
    val enabled = channel.enabled
    val showOnHome = channel.showOnHome
    val label = channel.name.trim().ifEmpty {
        stringResource(Res.string.settings_rd_panel_tank_label, channel.id + 1)
    }
    val capacity = channel.capacityLiters.takeIf { it > 0 }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.72f)
            .background(
                if (enabled) PbCard else PbField.copy(alpha = 0.85f),
                RoundedCornerShape(14.dp),
            )
            .border(
                1.dp,
                if (enabled) PbPhoneGreen.copy(alpha = 0.35f) else PbBorder.copy(alpha = 0.55f),
                RoundedCornerShape(14.dp),
            )
            .then(if (editMode) Modifier.clickable(onClick = onTap) else Modifier)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                label,
                color = if (enabled) PbText else PbMuted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (capacity != null) {
                Text(
                    "$capacity L",
                    color = PbCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(PbField, RoundedCornerShape(26.dp))
                    .border(
                        1.5.dp,
                        if (enabled) PbCyan.copy(alpha = 0.55f) else PbBorder,
                        RoundedCornerShape(26.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Box(modifier = Modifier.alpha(if (enabled) 1f else 0.4f)) {
                    PanelChannelIconSmall(channel = channel, size = 28.dp)
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                PhonePanelWaterSwitchRow(
                    title = if (enabled) activeLabel else passiveLabel,
                    checked = enabled,
                    enabled = true,
                    checkedColor = PbPhoneGreen,
                    onCheckedChange = onToggle,
                    compact = true,
                )
                PhonePanelWaterSwitchRow(
                    title = if (showOnHome) showLabel else hideLabel,
                    checked = showOnHome,
                    enabled = enabled,
                    checkedColor = PbCyan,
                    onCheckedChange = onToggleShowOnHome,
                    compact = true,
                )
            }
        }
    }
}

@Composable
private fun PhonePanelWaterSwitchRow(
    title: String,
    checked: Boolean,
    enabled: Boolean,
    checkedColor: Color,
    onCheckedChange: (Boolean) -> Unit,
    compact: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PbField.copy(alpha = 0.9f), RoundedCornerShape(10.dp))
            .border(1.dp, PbBorder.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
            .padding(
                horizontal = if (compact) 10.dp else 12.dp,
                vertical = if (compact) 4.dp else 8.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            title,
            color = when {
                !enabled -> PbMuted.copy(alpha = 0.55f)
                checked -> checkedColor
                else -> PbMuted
            },
            fontSize = if (compact) 12.sp else 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = checkedColor,
                uncheckedThumbColor = Color.White.copy(alpha = 0.85f),
                uncheckedTrackColor = PbMuted.copy(alpha = 0.4f),
                disabledCheckedThumbColor = Color.White.copy(alpha = 0.7f),
                disabledCheckedTrackColor = checkedColor.copy(alpha = 0.35f),
                disabledUncheckedThumbColor = Color.White.copy(alpha = 0.55f),
                disabledUncheckedTrackColor = PbMuted.copy(alpha = 0.25f),
            ),
        )
    }
}

@Composable
private fun PhonePanelMotorGrid(
    sectionTitle: String,
    channels: List<PanelPlatformChannel>,
    onToggle: (Int, Boolean) -> Unit,
    onNameChange: (Int, String) -> Unit,
    onTypeChange: (Int, typeKey: String, iconKey: String) -> Unit,
    onIconChange: (Int, String) -> Unit,
    onPreview: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                sectionTitle,
                color = PbText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                modifier = Modifier.weight(1f),
            )
            Icon(
                Icons.Filled.Info,
                contentDescription = null,
                tint = PbCyan,
                modifier = Modifier.size(18.dp),
            )
        }
        channels.forEach { channel ->
            PhonePanelMotorCard(
                channel = channel,
                onToggle = { onToggle(channel.id, it) },
                onNameChange = { onNameChange(channel.id, it) },
                onTypeChange = { type, icon -> onTypeChange(channel.id, type, icon) },
                onIconChange = { onIconChange(channel.id, it) },
                onPreview = onPreview,
            )
        }
    }
}

@Composable
private fun TabletPanelMotorGrid(
    sectionTitle: String,
    channels: List<PanelPlatformChannel>,
    motorCap: Int,
    onToggle: (Int, Boolean) -> Unit,
    onNameChange: (Int, String) -> Unit,
    onIconChange: (Int, String) -> Unit,
    onAdd: () -> Unit,
    onRemove: (Int) -> Unit,
    onPreview: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                sectionTitle,
                color = PbText,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                modifier = Modifier.weight(1f),
            )
            Icon(
                Icons.Filled.Info,
                contentDescription = null,
                tint = PbCyan,
                modifier = Modifier.size(18.dp),
            )
        }
        Text(
            stringResource(Res.string.settings_rd_panel_motors_hint),
            color = PbMuted,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            val gap = 14.dp
            val addSlot = channels.size < motorCap
            val slots = maxOf(2, channels.size + if (addSlot) 1 else 0)
            val cardWidth = (maxWidth - gap * (slots - 1)) / slots
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(gap),
            ) {
                channels.forEach { channel ->
                    TabletPanelMotorCard(
                        channel = channel,
                        onToggle = { onToggle(channel.id, it) },
                        onNameChange = { onNameChange(channel.id, it) },
                        onIconChange = { onIconChange(channel.id, it) },
                        onPreview = onPreview,
                        onRemove = { onRemove(channel.id) },
                        modifier = Modifier
                            .width(cardWidth)
                            .fillMaxHeight(),
                    )
                }
                if (addSlot) {
                    AddChannelSlot(
                        label = stringResource(Res.string.settings_rd_panel_add_motor),
                        modifier = Modifier
                            .width(cardWidth)
                            .fillMaxHeight(),
                        onClick = onAdd,
                    )
                }
            }
        }
    }
}

private data class PanelMotorTypeOption(
    val key: String,
    val labelRes: org.jetbrains.compose.resources.StringResource,
    val defaultIcon: String,
)

private val PanelMotorTypeOptions = listOf(
    PanelMotorTypeOption("vent", Res.string.settings_rd_panel_motor_type_vent, "fan"),
    PanelMotorTypeOption("shutter", Res.string.settings_rd_panel_motor_type_shutter, "perde"),
    PanelMotorTypeOption("lift", Res.string.settings_rd_panel_motor_type_lift, "yatak_basi"),
    PanelMotorTypeOption("awning", Res.string.settings_rd_panel_motor_type_awning, "tente_alti_aydinlatma"),
)

private val PanelMotorQuickIcons = listOf("fan", "perde", "pencere")

private val TabletPanelMotorQuickIcons = listOf(
    "fan",
    "perde",
    "pencere",
    "yatak_basi",
    "tente_alti_aydinlatma",
    "havalandirma",
)

private fun normalizePanelMotorType(raw: String, icon: String, id: Int): String {
    val key = raw.trim().lowercase()
    if (PanelMotorTypeOptions.any { it.key == key }) return key
    return when {
        icon.contains("perde") || icon.contains("curtain") -> "shutter"
        icon.contains("pencere") || icon.contains("window") -> "shutter"
        icon.contains("fan") || icon.contains("hava") -> "vent"
        icon.contains("tente") || icon.contains("awning") -> "awning"
        icon.contains("yatak") || icon.contains("lift") -> "lift"
        id == 1 -> "shutter"
        else -> "vent"
    }
}

private fun defaultPanelMotorName(typeKey: String, id: Int): String {
    return when (typeKey) {
        "vent" -> "Tavan Fanı"
        "shutter" -> "Perde"
        "lift" -> "Yatak Kaldırma"
        "awning" -> "Tente"
        else -> "Motor ${id + 1}"
    }
}

private fun PanelPlatformChannel.withPanelMotorType(typeKey: String, iconKey: String): PanelPlatformChannel {
    return copy(
        sensorType = typeKey,
        icon = iconKey,
        name = name.ifBlank { defaultPanelMotorName(typeKey, id) },
    )
}

@Composable
private fun PhonePanelMotorCard(
    channel: PanelPlatformChannel,
    onToggle: (Boolean) -> Unit,
    onNameChange: (String) -> Unit,
    onTypeChange: (typeKey: String, iconKey: String) -> Unit,
    onIconChange: (String) -> Unit,
    onPreview: () -> Unit,
) {
    val enabled = channel.enabled
    val activeLabel = stringResource(Res.string.settings_rd_panel_status_active)
    val passiveLabel = stringResource(Res.string.settings_rd_panel_status_passive)
    val slotTitle = stringResource(Res.string.settings_rd_panel_motor_label, channel.id + 1)
    var name by remember(channel.id, channel.name) {
        mutableStateOf(channel.name.ifBlank { slotTitle })
    }
    var typeMenu by remember { mutableStateOf(false) }
    var moreIcons by remember { mutableStateOf(false) }
    val typeKey = normalizePanelMotorType(channel.sensorType, channel.icon, channel.id)
    val typeOption = PanelMotorTypeOptions.firstOrNull { it.key == typeKey } ?: PanelMotorTypeOptions.first()
    val selectedIcon = channel.icon.trim().ifEmpty { typeOption.defaultIcon }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.88f)
            .background(PbCard, RoundedCornerShape(16.dp))
            .border(
                1.dp,
                if (enabled) PbCyan.copy(alpha = 0.35f) else PbBorder.copy(alpha = 0.6f),
                RoundedCornerShape(16.dp),
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(PbField, CircleShape)
                    .border(1.dp, PbBorder, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "${channel.id + 1}",
                    color = PbCyan,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                slotTitle,
                color = PbText,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(if (enabled) PbPhoneGreen else PbMuted, CircleShape),
                )
                Text(
                    "${stringResource(Res.string.settings_rd_panel_motor_status)}: ${if (enabled) activeLabel else passiveLabel}",
                    color = if (enabled) PbPhoneGreen else PbMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Switch(
                    checked = enabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = PbPhoneGreen,
                        uncheckedThumbColor = Color.White.copy(alpha = 0.85f),
                        uncheckedTrackColor = PbMuted.copy(alpha = 0.4f),
                    ),
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                stringResource(Res.string.settings_rd_panel_motor_name),
                color = PbMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
            )
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    onNameChange(it)
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    Icon(Icons.Filled.Edit, null, tint = PbCyan, modifier = Modifier.size(18.dp))
                },
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    textColor = PbText,
                    backgroundColor = PbField,
                    cursorColor = PbCyan,
                    focusedBorderColor = PbCyan.copy(alpha = 0.7f),
                    unfocusedBorderColor = PbBorder,
                ),
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                stringResource(Res.string.settings_rd_panel_motor_type),
                color = PbMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Box {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PbField, RoundedCornerShape(12.dp))
                        .border(1.dp, PbBorder, RoundedCornerShape(12.dp))
                        .clickable { typeMenu = true }
                        .padding(horizontal = 12.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        stringResource(typeOption.labelRes),
                        color = PbText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Icon(
                        Icons.Filled.KeyboardArrowDown,
                        null,
                        tint = PbMuted,
                        modifier = Modifier.size(20.dp),
                    )
                }
                DropdownMenu(expanded = typeMenu, onDismissRequest = { typeMenu = false }) {
                    PanelMotorTypeOptions.forEach { opt ->
                        DropdownMenuItem(onClick = {
                            typeMenu = false
                            onTypeChange(opt.key, opt.defaultIcon)
                        }) {
                            Text(stringResource(opt.labelRes))
                        }
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(Res.string.settings_rd_panel_motor_icon),
                color = PbMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                val iconsToShow = if (selectedIcon in PanelMotorQuickIcons) {
                    PanelMotorQuickIcons
                } else {
                    listOf(selectedIcon) + PanelMotorQuickIcons.take(2)
                }
                iconsToShow.forEach { iconKey ->
                    val selected = selectedIcon == iconKey
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(PbField, RoundedCornerShape(12.dp))
                            .border(
                                width = if (selected) 2.dp else 1.dp,
                                color = if (selected) PbCyan else PbBorder,
                                shape = RoundedCornerShape(12.dp),
                            )
                            .clickable { onIconChange(iconKey) },
                        contentAlignment = Alignment.Center,
                    ) {
                        PanelChannelIconSmall(
                            channel = channel.copy(icon = iconKey),
                            size = 28.dp,
                        )
                        if (selected) {
                            Icon(
                                Icons.Filled.Check,
                                null,
                                tint = PbCyan,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .size(14.dp),
                            )
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(PbField, RoundedCornerShape(12.dp))
                        .border(1.dp, PbBorder, RoundedCornerShape(12.dp))
                        .clickable { moreIcons = true },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.MoreHoriz, null, tint = PbMuted, modifier = Modifier.size(26.dp))
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (enabled) PbCyan.copy(alpha = 0.10f) else PbField.copy(alpha = 0.85f),
                    RoundedCornerShape(12.dp),
                )
                .border(
                    1.dp,
                    if (enabled) PbCyan.copy(alpha = 0.35f) else PbBorder,
                    RoundedCornerShape(12.dp),
                )
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                if (enabled) {
                    stringResource(Res.string.settings_rd_panel_motor_home_active)
                } else {
                    stringResource(Res.string.settings_rd_panel_motor_home_passive)
                },
                color = if (enabled) PbText else PbMuted,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                modifier = Modifier.weight(1f),
            )
            Row(
                modifier = Modifier
                    .background(Color.Transparent, RoundedCornerShape(10.dp))
                    .border(1.dp, PbCyan.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
                    .clickable(onClick = onPreview)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    stringResource(Res.string.settings_rd_panel_motor_preview),
                    color = PbCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }

    if (moreIcons) {
        ButtonEditDialog(
            channel = channel.copy(icon = selectedIcon),
            title = stringResource(Res.string.settings_rd_panel_edit_motor),
            showSensorType = false,
            showIconPicker = true,
            onDismiss = { moreIcons = false },
            onSave = { newName, iconKey, _, _ ->
                onNameChange(newName)
                onIconChange(iconKey)
                moreIcons = false
            },
        )
    }
}

@Composable
private fun TabletPanelMotorCard(
    channel: PanelPlatformChannel,
    onToggle: (Boolean) -> Unit,
    onNameChange: (String) -> Unit,
    onIconChange: (String) -> Unit,
    onPreview: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val enabled = channel.enabled
    val activeLabel = stringResource(Res.string.settings_rd_panel_status_active)
    val passiveLabel = stringResource(Res.string.settings_rd_panel_status_passive)
    val slotTitle = stringResource(Res.string.settings_rd_panel_motor_label, channel.id + 1)
    var name by remember(channel.id, channel.name) {
        mutableStateOf(channel.name.ifBlank { slotTitle })
    }
    var moreIcons by remember { mutableStateOf(false) }
    val selectedIcon = channel.icon.trim().ifEmpty { "fan" }
    val iconsToShow = remember(selectedIcon) {
        if (selectedIcon in TabletPanelMotorQuickIcons) {
            TabletPanelMotorQuickIcons
        } else {
            listOf(selectedIcon) + TabletPanelMotorQuickIcons.take(5)
        }
    }

    Column(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.88f)
            .background(PbCard, RoundedCornerShape(16.dp))
            .border(
                1.dp,
                if (enabled) PbCyan.copy(alpha = 0.35f) else PbBorder.copy(alpha = 0.6f),
                RoundedCornerShape(16.dp),
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(PbField, CircleShape)
                    .border(1.dp, PbBorder, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "${channel.id + 1}",
                    color = PbCyan,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                slotTitle,
                color = PbText,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(if (enabled) PbPhoneGreen else PbMuted, CircleShape),
                )
                Text(
                    "${stringResource(Res.string.settings_rd_panel_motor_status)}: ${if (enabled) activeLabel else passiveLabel}",
                    color = if (enabled) PbPhoneGreen else PbMuted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Switch(
                    checked = enabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = PbPhoneGreen,
                        uncheckedThumbColor = Color.White.copy(alpha = 0.85f),
                        uncheckedTrackColor = PbMuted.copy(alpha = 0.4f),
                    ),
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                stringResource(Res.string.settings_rd_panel_motor_name),
                color = PbMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    onNameChange(it)
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    Icon(Icons.Filled.Edit, null, tint = PbCyan, modifier = Modifier.size(20.dp))
                },
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    textColor = PbText,
                    backgroundColor = PbField,
                    cursorColor = PbCyan,
                    focusedBorderColor = PbCyan.copy(alpha = 0.7f),
                    unfocusedBorderColor = PbBorder,
                ),
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                stringResource(Res.string.settings_rd_panel_motor_icon),
                color = PbMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                iconsToShow.take(3).forEach { iconKey ->
                    val selected = selectedIcon == iconKey
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(96.dp)
                            .background(PbField, RoundedCornerShape(16.dp))
                            .border(
                                width = if (selected) 2.dp else 1.dp,
                                color = if (selected) PbCyan else PbBorder,
                                shape = RoundedCornerShape(16.dp),
                            )
                            .clickable { onIconChange(iconKey) },
                        contentAlignment = Alignment.Center,
                    ) {
                        PanelChannelIconSmall(
                            channel = channel.copy(icon = iconKey),
                            size = 52.dp,
                        )
                        if (selected) {
                            Icon(
                                Icons.Filled.Check,
                                null,
                                tint = PbCyan,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .size(18.dp),
                            )
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                iconsToShow.drop(3).take(2).forEach { iconKey ->
                    val selected = selectedIcon == iconKey
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(96.dp)
                            .background(PbField, RoundedCornerShape(16.dp))
                            .border(
                                width = if (selected) 2.dp else 1.dp,
                                color = if (selected) PbCyan else PbBorder,
                                shape = RoundedCornerShape(16.dp),
                            )
                            .clickable { onIconChange(iconKey) },
                        contentAlignment = Alignment.Center,
                    ) {
                        PanelChannelIconSmall(
                            channel = channel.copy(icon = iconKey),
                            size = 52.dp,
                        )
                        if (selected) {
                            Icon(
                                Icons.Filled.Check,
                                null,
                                tint = PbCyan,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .size(18.dp),
                            )
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(96.dp)
                        .background(PbField, RoundedCornerShape(16.dp))
                        .border(1.dp, PbBorder, RoundedCornerShape(16.dp))
                        .clickable { moreIcons = true },
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.MoreHoriz, null, tint = PbMuted, modifier = Modifier.size(36.dp))
                        Text(
                            stringResource(Res.string.settings_rd_panel_icon),
                            color = PbMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
            Spacer(Modifier.weight(1f))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (enabled) PbCyan.copy(alpha = 0.10f) else PbField.copy(alpha = 0.85f),
                    RoundedCornerShape(12.dp),
                )
                .border(
                    1.dp,
                    if (enabled) PbCyan.copy(alpha = 0.35f) else PbBorder,
                    RoundedCornerShape(12.dp),
                )
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                stringResource(Res.string.settings_rd_panel_remove),
                color = Color(0xFFFF6B6B),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable(onClick = onRemove)
                    .padding(horizontal = 6.dp, vertical = 4.dp),
            )
            Spacer(Modifier.weight(1f))
            Row(
                modifier = Modifier
                    .background(Color.Transparent, RoundedCornerShape(10.dp))
                    .border(1.dp, PbCyan.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
                    .clickable(onClick = onPreview)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    stringResource(Res.string.settings_rd_panel_motor_preview),
                    color = PbCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }

    if (moreIcons) {
        ButtonEditDialog(
            channel = channel.copy(icon = selectedIcon),
            title = stringResource(Res.string.settings_rd_panel_edit_motor),
            showSensorType = false,
            showIconPicker = true,
            onDismiss = { moreIcons = false },
            onSave = { newName, iconKey, _, _ ->
                onNameChange(newName)
                onIconChange(iconKey)
                moreIcons = false
            },
        )
    }
}

@Composable
private fun PhonePanelToggleGrid(
    sectionTitle: String,
    channels: List<PanelPlatformChannel>,
    editMode: Boolean,
    onEditModeChange: (Boolean) -> Unit,
    displayName: @Composable (PanelPlatformChannel) -> String,
    onToggle: (Int, Boolean) -> Unit,
    onTap: (Int) -> Unit,
    showChannelIcon: Boolean = true,
    statusBelowSwitch: Boolean = false,
    nameMaxLines: Int = 1,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                sectionTitle,
                color = PbText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.4.sp,
                modifier = Modifier.weight(1f),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    stringResource(Res.string.settings_rd_panel_edit_mode),
                    color = PbMuted,
                    fontSize = 11.sp,
                    maxLines = 1,
                )
                Switch(
                    checked = editMode,
                    onCheckedChange = onEditModeChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = PbCyan,
                        uncheckedThumbColor = Color.White.copy(alpha = 0.85f),
                        uncheckedTrackColor = PbMuted.copy(alpha = 0.45f),
                    ),
                )
            }
        }
        channels.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                row.forEach { channel ->
                    PhonePanelToggleRow(
                        channel = channel,
                        label = displayName(channel),
                        editMode = editMode,
                        showChannelIcon = showChannelIcon,
                        statusBelowSwitch = statusBelowSwitch,
                        nameMaxLines = nameMaxLines,
                        modifier = Modifier.weight(1f),
                        onToggle = { onToggle(channel.id, it) },
                        onTap = { onTap(channel.id) },
                    )
                }
                if (row.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun PhonePanelToggleRow(
    channel: PanelPlatformChannel,
    label: String,
    editMode: Boolean,
    modifier: Modifier = Modifier,
    showChannelIcon: Boolean = true,
    statusBelowSwitch: Boolean = false,
    nameMaxLines: Int = 1,
    onToggle: (Boolean) -> Unit,
    onTap: () -> Unit,
) {
    val activeLabel = stringResource(Res.string.settings_rd_panel_status_active)
    val passiveLabel = stringResource(Res.string.settings_rd_panel_status_passive)
    val statusLabel = if (channel.enabled) activeLabel else passiveLabel
    val enabled = channel.enabled
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = if (showChannelIcon) 86.dp else 72.dp)
            .alpha(if (enabled) 1f else 0.72f)
            .background(
                if (enabled) PbCard else PbField.copy(alpha = 0.85f),
                RoundedCornerShape(14.dp),
            )
            .border(
                width = if (enabled) 1.dp else 1.dp,
                color = if (enabled) PbPhoneGreen.copy(alpha = 0.35f) else PbBorder.copy(alpha = 0.55f),
                shape = RoundedCornerShape(14.dp),
            )
            .then(if (editMode) Modifier.clickable(onClick = onTap) else Modifier)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (showChannelIcon) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        if (enabled) PbField else PbCard.copy(alpha = 0.6f),
                        RoundedCornerShape(10.dp),
                    )
                    .border(
                        1.dp,
                        if (enabled) PbPhoneGreen.copy(alpha = 0.25f) else PbBorder.copy(alpha = 0.4f),
                        RoundedCornerShape(10.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Box(modifier = Modifier.alpha(if (enabled) 1f else 0.4f)) {
                    PanelChannelIconSmall(channel = channel, size = 28.dp)
                }
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                label,
                color = if (enabled) PbText else PbMuted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = nameMaxLines,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 16.sp,
            )
            if (!statusBelowSwitch) {
                Spacer(Modifier.height(2.dp))
                Text(
                    statusLabel,
                    color = if (enabled) PbPhoneGreen else PbMuted.copy(alpha = 0.75f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        if (statusBelowSwitch) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Switch(
                    checked = channel.enabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = PbCyan,
                        uncheckedThumbColor = Color.White.copy(alpha = 0.85f),
                        uncheckedTrackColor = PbMuted.copy(alpha = 0.4f),
                    ),
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    statusLabel,
                    color = if (enabled) PbPhoneGreen else PbMuted.copy(alpha = 0.75f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
            }
        } else {
            Switch(
                checked = channel.enabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = PbCyan,
                    uncheckedThumbColor = Color.White.copy(alpha = 0.85f),
                    uncheckedTrackColor = PbMuted.copy(alpha = 0.4f),
                ),
            )
        }
    }
}

@Composable
private fun PanelChannelIconSmall(
    channel: PanelPlatformChannel,
    size: androidx.compose.ui.unit.Dp,
) {
    val catalogIcon = PanelIconCatalog.find(channel.icon)
    val preset = PanelConfigMapper.iconToPreset(channel.icon)
    val legacyIconRes = if (catalogIcon == null) {
        runCatching {
            getAppContext().vanGO.typeOfIcon(preset) as DrawableResource
        }.getOrNull()
    } else {
        null
    }
    val tint = catalogIcon?.tint ?: iconTintForPreset(preset)
    if (catalogIcon != null) {
        PanelCatalogIcon(def = catalogIcon, size = size, tint = tint)
    } else if (legacyIconRes != null) {
        Image(
            painter = painterResource(legacyIconRes),
            contentDescription = null,
            colorFilter = ColorFilter.tint(tint),
            modifier = Modifier.size(size),
        )
    } else {
        Icon(Icons.Filled.WbSunny, null, tint = tint, modifier = Modifier.size(size))
    }
}

@Composable
private fun PhonePanelSaveBar(
    isSaving: Boolean,
    onSave: () -> Unit,
) {
    Button(
        onClick = onSave,
        enabled = !isSaving,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = PbCyan,
            contentColor = Color(0xFF061018),
            disabledBackgroundColor = PbCyan.copy(alpha = 0.35f),
            disabledContentColor = Color(0xFF061018).copy(alpha = 0.5f),
        ),
        elevation = ButtonDefaults.elevation(0.dp, 0.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
    ) {
        Icon(Icons.Filled.Check, null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            stringResource(Res.string.settings_rd_panel_save),
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
        )
    }
}

@Composable
private fun PhonePanelInfoBanner(
    text: String = stringResource(Res.string.settings_rd_panel_phone_passive_info),
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PbCyan.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .border(1.dp, PbCyan.copy(alpha = 0.22f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(Icons.Filled.Info, null, tint = PbCyan, modifier = Modifier.size(18.dp))
        Text(
            text,
            color = PbMuted,
            fontSize = 11.sp,
            lineHeight = 15.sp,
        )
    }
}

private fun iconTintForPreset(preset: Int): Color = when (preset) {
    7, 10 -> Color(0xFF4EA8FF) // ac / pump
    9, 11, 8, 15 -> Color(0xFF7CC4FF)
    16 -> Color(0xFFFF6B6B) // heater
    else -> Color(0xFFFFC857) // lights
}

package com.hextech.vango

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButton
import androidx.compose.material.RadioButtonDefaults
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toIntRect
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.toSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt
import testvango.composeapp.generated.resources.Res
import testvango.composeapp.generated.resources.panel_preset_cancel
import testvango.composeapp.generated.resources.panel_preset_confirm
import testvango.composeapp.generated.resources.panel_preset_picker_hint
import testvango.composeapp.generated.resources.panel_preset_picker_title

/**
 * Writes panel output names to [DevConfig.OutputsNameBuffer].
 * - Disabling slot [index] clears this slot and all higher indices (device expects a contiguous prefix).
 * - Enabling slot [index] fills any earlier zero slots with preset 1 so the prefix stays valid.
 */
private fun syncOutputBufferToUiState(index: Int, enabled: Boolean, ioName: Int) {
    val cfg = uiState.value.devConfig
    val buf = cfg.OutputsNameBuffer
    if (index !in buf.indices) return
    val newBuf = buf.copyOf()

    if (!enabled) {
        for (j in index until newBuf.size) {
            newBuf[j] = 0
        }
    } else {
        for (j in 0 until index) {
            if (newBuf[j] == 0.toByte()) {
                newBuf[j] = 1
            }
        }
        newBuf[index] = ioName.coerceIn(1, 18).toByte()
    }
    uiState.value = uiState.value.copy(devConfig = cfg.copy(OutputsNameBuffer = newBuf))
    markPanelConfigTouched()
}

/** Writes panel dimmer preset slots to [DevConfig.DimsNameBuffer]. */
internal fun syncDimBufferToUiState(index: Int, enabled: Boolean, ioName: Int) {
    val cfg = uiState.value.devConfig
    val buf = cfg.DimsNameBuffer
    if (index !in buf.indices) return
    val newBuf = buf.copyOf()

    if (!enabled) {
        for (j in index until newBuf.size) {
            newBuf[j] = 0
        }
    } else {
        for (j in 0 until index) {
            if (newBuf[j] == 0.toByte()) {
                newBuf[j] = 1
            }
        }
        newBuf[index] = ioName.coerceIn(1, 18).toByte()
    }
    uiState.value = uiState.value.copy(devConfig = cfg.copy(DimsNameBuffer = newBuf))
    markPanelConfigTouched()
}

internal fun syncTempHumiditySensorToDevConfig(enabled: Boolean, iconPreset: Int = 1) {
    val cfg = uiState.value.devConfig
    val buf = cfg.SensorsNameBuffer.copyOf()
    val preset = if (enabled) iconPreset.coerceIn(1, 18).toByte() else 0
    buf[0] = preset
    buf[1] = preset
    uiState.value = uiState.value.copy(devConfig = cfg.copy(SensorsNameBuffer = buf))
    markPanelConfigTouched()
}

internal fun syncSensorSlotToDevConfig(index: Int, enabled: Boolean, iconPreset: Int = 1) {
    if (index !in 0..1) return
    val cfg = uiState.value.devConfig
    val buf = cfg.SensorsNameBuffer.copyOf()
    buf[index] = if (enabled) iconPreset.coerceIn(1, 18).toByte() else 0
    if (index == 0 && enabled) {
        // temp_humidity disindaki tipler yalnizca slot 0 kullanir
        buf[1] = 0
    }
    uiState.value = uiState.value.copy(devConfig = cfg.copy(SensorsNameBuffer = buf))
    markPanelConfigTouched()
}

internal fun canToggleDimmerSlot(
    index: Int,
    currentlyEnabled: Boolean,
    buffer: ByteArray,
    maxDims: Int,
): Boolean {
    if (index !in 0 until maxDims || index !in buffer.indices) return false
    if (currentlyEnabled) {
        val lastEnabled = (0 until maxDims).lastOrNull { buffer.getOrElse(it) { 0 } != 0.toByte() }
        return lastEnabled == index
    }
    return index == 0 || buffer.getOrElse(index - 1) { 0 } != 0.toByte()
}

@Composable
internal fun OutputPresetPickerDialog(
    currentPresetId: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var pickedId by remember(currentPresetId) {
        mutableStateOf(currentPresetId.coerceIn(1, 18))
    }
    val vanGo = getAppContext().vanGO

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .heightIn(max = 520.dp)
                    .background(
                        color = MaterialTheme.colors.surface,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable(enabled = false) { }
                    .padding(16.dp)
            ) {
                Text(
                    text = stringResource(Res.string.panel_preset_picker_title),
                    style = MaterialTheme.typography.h6,
                    color = MaterialTheme.colors.onSurface
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(Res.string.panel_preset_picker_hint),
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.75f)
                )
                Spacer(Modifier.height(12.dp))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                ) {
                    items((1..18).toList()) { id ->
                        val icon = vanGo.typeOfIcon(id)
                        val label = vanGo.typeOfString(id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    pickedId = id
                                }
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RadioButton(
                                selected = pickedId == id,
                                onClick = { pickedId = id },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = MaterialTheme.colors.primary
                                )
                            )
                            Image(
                                painter = painterResource(icon as DrawableResource),
                                contentDescription = null,
                                modifier = Modifier.size(40.dp)
                            )
                            Text(
                                text = stringResource(label as StringResource),
                                style = MaterialTheme.typography.body1,
                                color = MaterialTheme.colors.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(Res.string.panel_preset_cancel))
                    }
                    TextButton(
                        onClick = { onConfirm(pickedId) }
                    ) {
                        Text(stringResource(Res.string.panel_preset_confirm))
                    }
                }
            }
        }
    }
}

@Composable
fun InputOutputPanel(ioDataBuf: ByteArray, ioNameBuf: ByteArray) {
    val slotCount = maxOf(ioDataBuf.size, ioNameBuf.size, 1)
    val rowCount = (slotCount + 3) / 4
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val w = maxWidth.value.coerceAtLeast(280f)
        val scale = (w / 520f).coerceIn(0.85f, 1.45f)
        val rowH = (170f * scale).dp
        val gap = (6f * scale).dp.coerceIn(2.dp, 18.dp)
        val verticalGapCount = (rowCount - 1).coerceAtLeast(0)
        val colH = rowH * rowCount + gap * verticalGapCount

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(colH),
            verticalArrangement = Arrangement.spacedBy(gap)
        ) {
            for (row in 0 until rowCount) {
                val base = row * 4
                IORow(
                    base,
                    ioDataBuf.drop(base).take(4),
                    ioNameBuf.drop(base).take(4),
                    rowH,
                    gap,
                )
            }
        }
    }
}

@Composable
fun IORow(
    rowIndex: Int,
    ioDataBuf: List<Byte>,
    ioNameBuf: List<Byte>,
    rowHeight: Dp = 170.dp,
    horizontalGap: Dp = 1.dp
) {
    if(ioNameBuf.isEmpty().not()) {
        Row(
            modifier = Modifier
                .height(rowHeight)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(horizontalGap),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IO_UI_Manager(rowIndex + 0, ioNameBuf.getOrElse(0){0}, ioDataBuf.getOrElse(0){0}, Modifier.weight(1f))
            IO_UI_Manager(rowIndex + 1, ioNameBuf.getOrElse(1){0}, ioDataBuf.getOrElse(1){0}, Modifier.weight(1f))
            IO_UI_Manager(rowIndex + 2, ioNameBuf.getOrElse(2){0}, ioDataBuf.getOrElse(2){0}, Modifier.weight(1f))
            IO_UI_Manager(rowIndex + 3, ioNameBuf.getOrElse(3){0}, ioDataBuf.getOrElse(3){0}, Modifier.weight(1f))
        }
    }
}

@Composable
fun IO_UI_Manager(idIndex:Int, id: Byte, connected: Byte, modifier: Modifier) {
    val enabled = id.toInt() != 0
    val icon = getAppContext().vanGO.typeOfIcon(id.toInt())
    val label = getAppContext().vanGO.typeOfString(id.toInt())
    if (uiState.value.selectedTab == SelectedPage.Menu || uiState.value.selectedTab == SelectedPage.Profile) {
        IOConfigUI(
            idIndex,
            enabled,
            if(id.toInt() == 0) 1 else id.toInt(),
            modifier)
    } else {
        IO_UI(
            painterResource(icon as DrawableResource),
            stringResource(label as StringResource),
            idIndex,
            enabled,
            connected.toInt() != 0,
            modifier
        )
    }
}

@Composable
fun SelectSwitch(checked:Boolean, onCheckedChange:(Boolean)->Unit){
    Row(modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.Top) {
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

data class IOStateInfo (
    var clicked: Boolean = false
)

@Composable
private fun IOPanelInfo(modifier: Modifier, index: Int, visible: Boolean, enabled:Boolean, content: @Composable()(IOStateInfo.() -> Unit)):Unit {
    println("TTTTTT indec $index $visible $enabled")
    var state by remember { mutableStateOf(IOStateInfo()) }
    val color = if(enabled) Color(0xFF37C600).copy(alpha = .7f) else Color(0xFF5A5A5A).copy(alpha = .7f)
    Column(
        modifier = modifier.alpha(if(visible) 1f else 0f)
            .background(color = color, shape = RoundedCornerShape(10.dp))
            .fillMaxHeight()
            .border(
                .5.dp,
                MaterialTheme.colors.onSurface.copy(alpha = 0.12f),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(
                enabled = visible,
                onClick = {
                    CoroutineScope(Dispatchers.Default).launch {
                        state = state.copy(clicked = true)
                        val targetOn = !enabled
                        println("writing digital port $index -> ${if (targetOn) "ON" else "OFF"} (ui=$enabled)")
                        val result = getAppContext().vanGO.writeDigitalPort(index, targetOn)
                        if (!result) {
                            println("Failed to send relay command idx=$index on=$targetOn")
                        }
                        state = state.copy(clicked = false)
                    }
                }
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        content.invoke(state)
    }
}

data class IOStateConfig (
    var enabled: Boolean = false,
    var ioName: Int = 0,
)

@Composable
private fun IOPanelConfig(modifier: Modifier, index: Int, visible: Boolean, enabled:Boolean, content: @Composable()(IOStateConfig.() -> Unit)):Unit {
    val color = if(enabled) MaterialTheme.colors.secondary.copy(alpha = .7f) else Color.Gray.copy(alpha = .7f)
    Column(
        modifier = modifier.alpha(if(visible) 1f else 0f)
            .background(
                color = color, shape = RoundedCornerShape(10.dp)
            )
            .fillMaxSize()
            .fillMaxHeight()
            .border(
                .5.dp,
                MaterialTheme.colors.onSurface.copy(alpha = 0.12f),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(
                enabled = visible,
                onClick = {}
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        content
    }
}

@Composable
private fun IOButtonInfo(icon: Painter, label: String = "label", clicked: Boolean) {
    val labelFontSize = when {
        label.length >= 16 -> 11.sp
        label.length >= 12 -> 12.sp
        label.length >= 9 -> 13.sp
        else -> 14.sp
    }

    Box(
        modifier = Modifier
            .fillMaxSize(0.75f),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = icon,
            contentDescription = "",
            modifier = Modifier
                .fillMaxSize(0.6f),  // Icon takes 60% of the button size
//            colorFilter = ColorFilter.tint(Color.White.copy(alpha = .7f)) // Apply the tint
        )
        if(clicked) {
            SpiralWaitingIcon(Modifier.matchParentSize())
        }
    }
    Text(
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .padding(horizontal = 2.dp),
        text = label,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
        fontSize = labelFontSize
    )
}

@Composable
private fun IOButtonConfig(
    icon: Painter,
    label: String = "label",
    onSwipe: (String) -> Unit,
    onIconClick: () -> Unit
) {
    var swipeDirection by remember { mutableStateOf<String?>(null) }
    var dragAmount by remember { mutableStateOf(0f) }
    var boxWidth by remember { mutableStateOf(0f) }

    Box(
        modifier = Modifier
//            .border(.5.dp, Color.Red)
            .fillMaxSize(0.75f)
            .onGloballyPositioned { coordinates ->
                boxWidth = coordinates.size.width.toFloat() // Capture width in pixels
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
        contentAlignment = Alignment.Center,
    ) {
        // Left arrow indicator (clickable - goes to previous icon)
        Icon(
            imageVector = Icons.Filled.ArrowBack,
            contentDescription = "Previous icon",
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 4.dp)
                .size(16.dp)
                .alpha(0.6f)
                .clickable {
                    onSwipe("R") // "R" decrements (previous) in the existing logic
                },
            tint = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
        )
        
        // Main icon (centered, clickable - cycles forward)
        Image(
            painter = icon,
            contentDescription = "Change icon",
            modifier = Modifier
                .align(Alignment.Center)
                .offset { IntOffset(dragAmount.roundToInt(), 0) } // Move with swipe
                .fillMaxSize(0.6f)  // Icon takes 60% of the button size
                .clickable { onIconClick() },
//            colorFilter = ColorFilter.tint(MaterialTheme.colors.primary.copy(alpha = .7f)) // Apply the tint
        )
        
        // Right arrow indicator (clickable - goes to next icon)
        Icon(
            imageVector = Icons.Filled.ArrowForward,
            contentDescription = "Next icon",
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 4.dp)
                .size(16.dp)
                .alpha(0.6f)
                .clickable {
                    onSwipe("L") // "L" increments (next) in the existing logic
                },
            tint = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
        )
        // Display swipe result
        swipeDirection?.let {
            onSwipe(it)
            swipeDirection = null
        }
    }
    Text(modifier = Modifier
        .offset { IntOffset(dragAmount.roundToInt(), 0) },
        text = label
    )
}

@Composable
private fun IOConfigUI(
    index: Int, // todo global bi data degiskeni olusturulacak olup her degisimde buraya gore setlencek!
    enabled: Boolean = true,
    ioName: Int = 0,
    modifier: Modifier = Modifier,
) {
    val ui by uiState.collectAsState()
    val slotByte = ui.devConfig.OutputsNameBuffer.getOrElse(index) { 0 }

    var state by remember(index) { mutableStateOf(IOStateConfig(enabled = enabled, ioName = ioName)) }
    var showPresetDialog by remember(index) { mutableStateOf(false) }

    // Keep local UI in sync when buffer changes (e.g. cascade after another slot toggles off)
    LaunchedEffect(slotByte, index) {
        val en = slotByte != 0.toByte()
        val nm = if (en) slotByte.toInt().coerceIn(1, 18) else 1
        state = IOStateConfig(enabled = en, ioName = nm)
    }

    LaunchedEffect(state.enabled, state.ioName, index) {
        syncOutputBufferToUiState(index, state.enabled, state.ioName)
    }

    val color = if(state.enabled) Color(0xFF37C600).copy(alpha = .7f) else Color(0xFF5A5A5A).copy(alpha = .7f)
    val icon = getAppContext().vanGO.typeOfIcon(state.ioName)
    val label = getAppContext().vanGO.typeOfString(state.ioName)

    println(" WEEEEE DDFF F ------------ $index --?? ${state.enabled}  -- ${if (index < uiState.value.devConfig.OutputsNameBuffer.size) uiState.value.devConfig.OutputsNameBuffer[index] else "OUT_OF_BOUNDS"}")
    Column(
        modifier = modifier
            .background( color = color )
            .clip(RectangleShape)
            .fillMaxSize()
            .fillMaxHeight()
            .border(.5.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        SelectSwitch(
            checked = state.enabled,
            onCheckedChange = {
                state = state.copy(enabled = !state.enabled)
            }
        )
        IOButtonConfig(
            painterResource(icon as DrawableResource),
            stringResource(label as StringResource),
            {
                var cur = state.ioName

                if(it.equals("R")) {
                    --cur
                    if(cur < 1) {
                        cur = 18 // todo: Buraya ioMAx?
                    }
                } else {
                    ++cur
                    if(cur > 18) {
                        cur = 1
                    }
                }
                state = state.copy(ioName = cur)
            },
            onIconClick = { showPresetDialog = true }
        )
    }

    if (showPresetDialog) {
        OutputPresetPickerDialog(
            currentPresetId = state.ioName,
            onDismiss = { showPresetDialog = false },
            onConfirm = { newId ->
                state = state.copy(ioName = newId.coerceIn(1, 18))
                showPresetDialog = false
            }
        )
    }
}

@Composable
private fun IO_UI(
    icon: Painter,
    label: String = "label",
    index: Int,
    enabled: Boolean = true,
    connected: Boolean = false,
    modifier: Modifier = Modifier,
) {
    IOPanelInfo(modifier, index, enabled, connected) {
        IOButtonInfo(icon, label, clicked)
    }
}

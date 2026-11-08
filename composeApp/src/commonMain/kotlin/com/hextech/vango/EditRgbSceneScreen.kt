package com.hextech.vango

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import testvango.composeapp.generated.resources.Res
import testvango.composeapp.generated.resources.color_picker_close
import testvango.composeapp.generated.resources.rgb_scene_add_color
import testvango.composeapp.generated.resources.rgb_scene_brightness_pct
import testvango.composeapp.generated.resources.rgb_scene_colors_hint
import testvango.composeapp.generated.resources.rgb_scene_colors_title
import testvango.composeapp.generated.resources.rgb_scene_delete
import testvango.composeapp.generated.resources.rgb_scene_delete_confirm
import testvango.composeapp.generated.resources.rgb_scene_duration_sec
import testvango.composeapp.generated.resources.rgb_scene_edit_hint
import testvango.composeapp.generated.resources.rgb_scene_edit_step_title
import testvango.composeapp.generated.resources.rgb_scene_edit_subtitle
import testvango.composeapp.generated.resources.rgb_scene_edit_title
import testvango.composeapp.generated.resources.rgb_scene_effect_bounce
import testvango.composeapp.generated.resources.rgb_scene_effect_fade
import testvango.composeapp.generated.resources.rgb_scene_effect_slide
import testvango.composeapp.generated.resources.rgb_scene_effect_smooth
import testvango.composeapp.generated.resources.rgb_scene_empty_slot
import testvango.composeapp.generated.resources.rgb_scene_group_label
import testvango.composeapp.generated.resources.rgb_scene_info_summary
import testvango.composeapp.generated.resources.rgb_scene_loop_continuous
import testvango.composeapp.generated.resources.rgb_scene_loop_label
import testvango.composeapp.generated.resources.rgb_scene_loop_once
import testvango.composeapp.generated.resources.rgb_scene_loop_pingpong
import testvango.composeapp.generated.resources.rgb_scene_name_label
import testvango.composeapp.generated.resources.rgb_scene_pick_color_title
import testvango.composeapp.generated.resources.rgb_scene_preview_title
import testvango.composeapp.generated.resources.rgb_scene_save
import testvango.composeapp.generated.resources.rgb_scene_step_brightness
import testvango.composeapp.generated.resources.rgb_scene_step_duration
import testvango.composeapp.generated.resources.rgb_scene_step_move_down
import testvango.composeapp.generated.resources.rgb_scene_step_move_up
import testvango.composeapp.generated.resources.rgb_scene_step_remove
import testvango.composeapp.generated.resources.rgb_scene_transition_duration
import testvango.composeapp.generated.resources.rgb_scene_transition_duration_value
import testvango.composeapp.generated.resources.rgb_scene_transition_title
import testvango.composeapp.generated.resources.rgb_saved_scene_cinema
import testvango.composeapp.generated.resources.rgb_saved_scene_party
import testvango.composeapp.generated.resources.rgb_saved_scene_rest

private val SceneAccent = Color(0xFF00A8F3)
private val ScenePurple = Color(0xFF7C3AED)
private val ScenePurpleSoft = Color(0xFFF3E8FF)
private val ScenePageBg = Color(0xFFF1F5F9)
private val SceneCardBg = Color.White
private val SceneTextPrimary = Color(0xFF0F172A)
private val SceneTextSecondary = Color(0xFF64748B)
private val SceneBorder = Color(0xFFE2E8F0)
private val SceneInfoSoft = Color(0xFFE8F4FD)
private val SceneDanger = Color(0xFFEF4444)

private data class SceneGroupOption(val key: String, val emoji: String, val labelRes: org.jetbrains.compose.resources.StringResource)

private val sceneGroupOptions = listOf(
    SceneGroupOption(RGB_SCENE_REST, "🌙", Res.string.rgb_saved_scene_rest),
    SceneGroupOption(RGB_SCENE_CINEMA, "🎬", Res.string.rgb_saved_scene_cinema),
    SceneGroupOption(RGB_SCENE_PARTY, "🪩", Res.string.rgb_saved_scene_party),
)

private val transitionDurationOptions = listOf(1f, 1.5f, 2f, 2.5f, 3f)
private val loopModeOptions = listOf("continuous", "once", "pingpong")

private fun rgbPresetsLightTheme() = lightColors(
    primary = SceneAccent,
    primaryVariant = SceneAccent,
    secondary = SceneAccent,
    background = ScenePageBg,
    surface = SceneCardBg,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = SceneTextPrimary,
    onSurface = SceneTextPrimary,
)

@Composable
fun EditRgbSceneScreen(sceneId: String, onBack: () -> Unit) {
    MaterialTheme(colors = rgbPresetsLightTheme()) {
        EditRgbSceneScreenBody(sceneId = sceneId, onBack = onBack)
    }
}

@Composable
private fun EditRgbSceneScreenBody(sceneId: String, onBack: () -> Unit) {
    val slots = remember { ensureRgbSlotsInitialized() }
    var draft by remember(sceneId) {
        mutableStateOf(ensureRgbScenes()[sceneId] ?: seedDefaultRgbScenes()[sceneId]!!)
    }
    val playback by RgbScenePlayer.state.collectAsState()
    val previewPlaying = playback.isPlaying && playback.sceneId == sceneId

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showPickColor by remember { mutableStateOf(false) }
    var editStepIndex by remember { mutableStateOf<Int?>(null) }
    var stepMenuIndex by remember { mutableStateOf<Int?>(null) }
    var groupMenuExpanded by remember { mutableStateOf(false) }
    var durationMenuExpanded by remember { mutableStateOf(false) }
    var loopMenuExpanded by remember { mutableStateOf(false) }

    val tag = draft.displayNumber.toString().padStart(2, '0')
    val totalLoopSec = sceneTotalDurationSec(draft)

    DisposableEffect(sceneId) {
        onDispose {
            if (RgbScenePlayer.isPlayingScene(sceneId)) {
                RgbScenePlayer.stop()
            }
        }
    }

    fun persistAndBack() {
        RgbScenePlayer.stop()
        saveRgbScene(draft)
        onBack()
    }

    fun handleBack() {
        if (RgbScenePlayer.isPlayingScene(sceneId)) RgbScenePlayer.stop()
        onBack()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScenePageBg),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            SceneEditorHeader(
                sceneName = draft.name,
                tag = tag,
                onBack = { handleBack() },
                onDelete = { showDeleteConfirm = true },
                onSave = { persistAndBack() },
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                SceneInfoCard(
                    name = draft.name,
                    emoji = draft.emoji,
                    groupKey = draft.groupKey,
                    groupMenuExpanded = groupMenuExpanded,
                    onNameChange = { draft = draft.copy(name = it) },
                    onGroupMenuToggle = { groupMenuExpanded = it },
                    onGroupSelect = { opt ->
                        draft = draft.copy(groupKey = opt.key, emoji = opt.emoji)
                        groupMenuExpanded = false
                    },
                )
                SceneColorsCard(
                    colors = draft.colors,
                    slots = slots,
                    onAddColor = { showPickColor = true },
                    onStepMenu = { stepMenuIndex = it },
                )
                SceneTransitionCard(
                    effect = draft.transitionEffect,
                    durationSec = draft.transitionDurationSec,
                    loopMode = draft.loopMode,
                    durationMenuExpanded = durationMenuExpanded,
                    loopMenuExpanded = loopMenuExpanded,
                    onEffectSelect = { draft = draft.copy(transitionEffect = it) },
                    onDurationMenuToggle = { durationMenuExpanded = it },
                    onDurationSelect = { draft = draft.copy(transitionDurationSec = it); durationMenuExpanded = false },
                    onLoopMenuToggle = { loopMenuExpanded = it },
                    onLoopSelect = { draft = draft.copy(loopMode = it); loopMenuExpanded = false },
                )
                ScenePreviewCard(
                    colors = draft.colors,
                    slots = slots,
                    playing = previewPlaying,
                    onPlayToggle = {
                        if (previewPlaying) {
                            RgbScenePlayer.stop()
                        } else {
                            RgbScenePlayer.play(draft, slots)
                        }
                    },
                    onStop = { RgbScenePlayer.stop() },
                    onRewind = {
                        RgbScenePlayer.stop()
                        RgbScenePlayer.play(draft, slots)
                    },
                )
                SceneSummaryBanner(colorCount = draft.colors.size, loopSec = totalLoopSec)
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(Res.string.rgb_scene_delete)) },
            text = { Text(stringResource(Res.string.rgb_scene_delete_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    resetRgbSceneToDefault(sceneId)
                    onBack()
                }) {
                    Text(stringResource(Res.string.rgb_scene_delete), color = SceneDanger)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(Res.string.color_picker_close))
                }
            },
        )
    }

    if (showPickColor) {
        AlertDialog(
            onDismissRequest = { showPickColor = false },
            title = { Text(stringResource(Res.string.rgb_scene_pick_color_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    slots.forEachIndexed { index, preset ->
                        if (preset == null) return@forEachIndexed
                        TextButton(
                            onClick = {
                                draft = draft.copy(
                                    colors = draft.colors + RgbSceneColorStep(index),
                                )
                                showPickColor = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Box(
                                    Modifier
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(rgb888ComposeColor(preset.r, preset.g, preset.b)),
                                )
                                Text(preset.name, color = SceneTextPrimary)
                                Text(preset.hex(), color = SceneTextSecondary, fontSize = 12.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPickColor = false }) {
                    Text(stringResource(Res.string.color_picker_close))
                }
            },
        )
    }

    val menuIdx = stepMenuIndex
    if (menuIdx != null) {
        AlertDialog(
            onDismissRequest = { stepMenuIndex = null },
            title = { Text(stringResource(Res.string.rgb_scene_edit_step_title)) },
            text = {
                Column {
                    if (menuIdx > 0) {
                        TextButton(onClick = {
                            val list = draft.colors.toMutableList()
                            list[menuIdx] = list[menuIdx - 1].also { list[menuIdx - 1] = list[menuIdx] }
                            draft = draft.copy(colors = list)
                            stepMenuIndex = null
                        }) { Text(stringResource(Res.string.rgb_scene_step_move_up)) }
                    }
                    if (menuIdx < draft.colors.lastIndex) {
                        TextButton(onClick = {
                            val list = draft.colors.toMutableList()
                            list[menuIdx] = list[menuIdx + 1].also { list[menuIdx + 1] = list[menuIdx] }
                            draft = draft.copy(colors = list)
                            stepMenuIndex = null
                        }) { Text(stringResource(Res.string.rgb_scene_step_move_down)) }
                    }
                    TextButton(onClick = {
                        editStepIndex = menuIdx
                        stepMenuIndex = null
                    }) { Text(stringResource(Res.string.rgb_scene_edit_step_title)) }
                    TextButton(onClick = {
                        draft = draft.copy(colors = draft.colors.filterIndexed { i, _ -> i != menuIdx })
                        stepMenuIndex = null
                    }) {
                        Text(stringResource(Res.string.rgb_scene_step_remove), color = SceneDanger)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { stepMenuIndex = null }) {
                    Text(stringResource(Res.string.color_picker_close))
                }
            },
        )
    }

    val editIdx = editStepIndex
    if (editIdx != null && editIdx < draft.colors.size) {
        var stepDraft by remember(editIdx, draft.colors) { mutableStateOf(draft.colors[editIdx]) }
        AlertDialog(
            onDismissRequest = { editStepIndex = null },
            title = { Text(stringResource(Res.string.rgb_scene_edit_step_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(Res.string.rgb_scene_step_duration), fontWeight = FontWeight.Medium)
                    Text("${stepDraft.durationSec} s", color = SceneAccent)
                    Slider(
                        value = stepDraft.durationSec.toFloat(),
                        onValueChange = { stepDraft = stepDraft.copy(durationSec = it.toInt().coerceIn(1, 30)) },
                        valueRange = 1f..30f,
                        steps = 28,
                    )
                    Text(stringResource(Res.string.rgb_scene_step_brightness), fontWeight = FontWeight.Medium)
                    Text(stringResource(Res.string.rgb_scene_brightness_pct, stepDraft.brightnessPercent))
                    Slider(
                        value = stepDraft.brightnessPercent.toFloat(),
                        onValueChange = {
                            stepDraft = stepDraft.copy(brightnessPercent = it.toInt().coerceIn(1, 100))
                        },
                        valueRange = 1f..100f,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val list = draft.colors.toMutableList()
                    list[editIdx] = stepDraft
                    draft = draft.copy(colors = list)
                    editStepIndex = null
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { editStepIndex = null }) {
                    Text(stringResource(Res.string.color_picker_close))
                }
            },
        )
    }
}

@Composable
private fun SceneEditorHeader(
    sceneName: String,
    tag: String,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    onSave: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 120.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                rgbSceneEditorHeaderTitle(),
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold,
                color = SceneTextPrimary,
                textAlign = TextAlign.Center,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 4.dp),
            ) {
                Text(
                    sceneName,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.subtitle1,
                    color = SceneTextPrimary,
                )
                Text(
                    "#$tag",
                    modifier = Modifier
                        .background(ScenePurpleSoft, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    color = ScenePurple,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                stringResource(Res.string.rgb_scene_edit_hint),
                style = MaterialTheme.typography.caption,
                color = SceneTextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp),
                lineHeight = 16.sp,
            )
        }
        Row(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .clickable(onClick = onBack),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.ArrowBack,
                contentDescription = null,
                tint = SceneAccent,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                rgbSceneEditorBackLabel(),
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.SemiBold,
                color = SceneTextPrimary,
                maxLines = 1,
            )
        }
        Column(
            modifier = Modifier.align(Alignment.CenterEnd),
            horizontalAlignment = Alignment.End,
        ) {
            OutlinedButton(
                onClick = onDelete,
                contentPadding = ButtonDefaults.ContentPadding,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = SceneDanger),
                border = androidx.compose.foundation.BorderStroke(1.dp, SceneDanger.copy(alpha = 0.35f)),
            ) {
                Icon(Icons.Filled.Delete, null, tint = SceneDanger, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(Res.string.rgb_scene_delete), fontSize = 12.sp)
            }
            Button(
                onClick = onSave,
                modifier = Modifier.padding(top = 6.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = SceneAccent),
            ) {
                Icon(Icons.Filled.Save, null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(Res.string.rgb_scene_save), color = Color.White, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun SceneSectionCard(
    title: String,
    icon: @Composable () -> Unit,
    headerAction: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = 2.dp,
        backgroundColor = SceneCardBg,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SceneBorder, RoundedCornerShape(16.dp)),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                icon()
                Text(
                    title,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp),
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.subtitle1,
                    color = SceneTextPrimary,
                )
                headerAction?.invoke()
            }
            content()
        }
    }
}

@Composable
private fun SceneInfoCard(
    name: String,
    emoji: String,
    groupKey: String,
    groupMenuExpanded: Boolean,
    onNameChange: (String) -> Unit,
    onGroupMenuToggle: (Boolean) -> Unit,
    onGroupSelect: (SceneGroupOption) -> Unit,
) {
    val selectedGroup = sceneGroupOptions.find { it.key == groupKey } ?: sceneGroupOptions.first()
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = 2.dp,
        backgroundColor = SceneCardBg,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SceneBorder, RoundedCornerShape(16.dp)),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(Res.string.rgb_scene_name_label), fontSize = 12.sp, color = SceneTextSecondary)
        TextField(
            value = name,
            onValueChange = onNameChange,
            singleLine = true,
            leadingIcon = { Text(emoji, fontSize = 20.sp) },
            colors = TextFieldDefaults.textFieldColors(
                backgroundColor = ScenePageBg,
                focusedIndicatorColor = SceneAccent,
                unfocusedIndicatorColor = SceneBorder,
                textColor = SceneTextPrimary,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            stringResource(Res.string.rgb_scene_group_label),
            fontSize = 12.sp,
            color = SceneTextSecondary,
            modifier = Modifier.padding(top = 4.dp),
        )
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, SceneBorder, RoundedCornerShape(12.dp))
                    .background(ScenePageBg)
                    .clickable { onGroupMenuToggle(true) }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(selectedGroup.emoji, fontSize = 18.sp)
                Text(
                    stringResource(selectedGroup.labelRes),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 10.dp),
                    color = SceneTextPrimary,
                )
                Icon(Icons.Filled.ExpandMore, null, tint = SceneTextSecondary)
            }
            DropdownMenu(expanded = groupMenuExpanded, onDismissRequest = { onGroupMenuToggle(false) }) {
                sceneGroupOptions.forEach { opt ->
                    DropdownMenuItem(onClick = { onGroupSelect(opt) }) {
                        Text("${opt.emoji}  ${stringResource(opt.labelRes)}")
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun SceneColorsCard(
    colors: List<RgbSceneColorStep>,
    slots: Array<RgbPreset?>,
    onAddColor: () -> Unit,
    onStepMenu: (Int) -> Unit,
) {
    SceneSectionCard(
        title = stringResource(Res.string.rgb_scene_colors_title),
        icon = { Icon(Icons.Filled.ColorLens, null, tint = SceneAccent, modifier = Modifier.size(22.dp)) },
        headerAction = {
            OutlinedButton(
                onClick = onAddColor,
                contentPadding = ButtonDefaults.ContentPadding,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = SceneAccent),
                border = androidx.compose.foundation.BorderStroke(1.dp, SceneAccent.copy(alpha = 0.35f)),
            ) {
                Icon(Icons.Filled.Add, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(Res.string.rgb_scene_add_color), fontSize = 12.sp)
            }
        },
    ) {
        Text(stringResource(Res.string.rgb_scene_colors_hint), style = MaterialTheme.typography.caption, color = SceneTextSecondary)
        colors.forEachIndexed { index, step ->
            SceneColorStepRow(
                order = index + 1,
                step = step,
                preset = slots.getOrNull(step.presetSlotIndex),
                onMenu = { onStepMenu(index) },
            )
            if (index < colors.lastIndex) {
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun SceneColorStepRow(
    order: Int,
    step: RgbSceneColorStep,
    preset: RgbPreset?,
    onMenu: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, SceneBorder, RoundedCornerShape(12.dp))
            .background(ScenePageBg)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.DragHandle, null, tint = SceneTextSecondary.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
        Box(
            modifier = Modifier
                .padding(horizontal = 6.dp)
                .size(28.dp)
                .background(SceneAccent.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text("$order", color = SceneAccent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (preset != null) rgb888ComposeColor(preset.r, preset.g, preset.b)
                    else SceneBorder,
                )
                .border(1.dp, SceneBorder, RoundedCornerShape(8.dp)),
        )
        Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
            Text(
                preset?.name ?: stringResource(Res.string.rgb_scene_empty_slot),
                fontWeight = FontWeight.SemiBold,
                color = SceneTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                preset?.hex() ?: "",
                fontSize = 11.sp,
                color = SceneTextSecondary,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Icon(Icons.Filled.Schedule, null, tint = SceneTextSecondary, modifier = Modifier.size(14.dp))
                Text(
                    stringResource(Res.string.rgb_scene_duration_sec, step.durationSec),
                    fontSize = 11.sp,
                    color = SceneTextSecondary,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Icon(Icons.Filled.WbSunny, null, tint = SceneTextSecondary, modifier = Modifier.size(14.dp))
                Text(
                    stringResource(Res.string.rgb_scene_brightness_pct, step.brightnessPercent),
                    fontSize = 11.sp,
                    color = SceneTextSecondary,
                )
            }
        }
        IconButton(onClick = onMenu, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Filled.MoreVert, null, tint = SceneTextSecondary, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun SceneTransitionCard(
    effect: RgbTransitionEffect,
    durationSec: Float,
    loopMode: String,
    durationMenuExpanded: Boolean,
    loopMenuExpanded: Boolean,
    onEffectSelect: (RgbTransitionEffect) -> Unit,
    onDurationMenuToggle: (Boolean) -> Unit,
    onDurationSelect: (Float) -> Unit,
    onLoopMenuToggle: (Boolean) -> Unit,
    onLoopSelect: (String) -> Unit,
) {
    val effects = listOf(
        RgbTransitionEffect.Smooth to Res.string.rgb_scene_effect_smooth,
        RgbTransitionEffect.Fade to Res.string.rgb_scene_effect_fade,
        RgbTransitionEffect.Slide to Res.string.rgb_scene_effect_slide,
        RgbTransitionEffect.Bounce to Res.string.rgb_scene_effect_bounce,
    )
    SceneSectionCard(
        title = stringResource(Res.string.rgb_scene_transition_title),
        icon = {
            Text("〜", color = SceneAccent, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            effects.forEach { (fx, labelRes) ->
                val selected = effect == fx
                Column(
                    modifier = Modifier
                        .width(100.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(
                            width = if (selected) 2.dp else 1.dp,
                            color = if (selected) SceneAccent else SceneBorder,
                            shape = RoundedCornerShape(12.dp),
                        )
                        .background(if (selected) SceneAccent.copy(alpha = 0.06f) else ScenePageBg)
                        .clickable { onEffectSelect(fx) }
                        .padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SceneBorder.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("●━━●", fontSize = 10.sp, color = SceneTextSecondary)
                    }
                    Text(
                        stringResource(labelRes),
                        fontSize = 11.sp,
                        color = if (selected) SceneAccent else SceneTextSecondary,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier.padding(top = 6.dp),
                        maxLines = 2,
                    )
                    if (selected) {
                        Icon(
                            Icons.Filled.Check,
                            null,
                            tint = SceneAccent,
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .size(16.dp),
                        )
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SceneDropdownField(
                label = stringResource(Res.string.rgb_scene_transition_duration),
                value = stringResource(Res.string.rgb_scene_transition_duration_value, durationSec),
                expanded = durationMenuExpanded,
                onToggle = onDurationMenuToggle,
                modifier = Modifier.weight(1f),
            ) {
                transitionDurationOptions.forEach { sec ->
                    DropdownMenuItem(onClick = { onDurationSelect(sec) }) {
                        Text(stringResource(Res.string.rgb_scene_transition_duration_value, sec))
                    }
                }
            }
            SceneDropdownField(
                label = stringResource(Res.string.rgb_scene_loop_label),
                value = loopModeLabel(loopMode),
                expanded = loopMenuExpanded,
                onToggle = onLoopMenuToggle,
                modifier = Modifier.weight(1f),
            ) {
                loopModeOptions.forEach { mode ->
                    DropdownMenuItem(onClick = { onLoopSelect(mode) }) {
                        Text(loopModeLabel(mode))
                    }
                }
            }
        }
    }
}

@Composable
private fun loopModeLabel(mode: String): String = when (mode) {
    "once" -> stringResource(Res.string.rgb_scene_loop_once)
    "pingpong" -> stringResource(Res.string.rgb_scene_loop_pingpong)
    else -> stringResource(Res.string.rgb_scene_loop_continuous)
}

@Composable
private fun SceneDropdownField(
    label: String,
    value: String,
    expanded: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    menuContent: @Composable () -> Unit,
) {
    Column(modifier) {
        Text(label, fontSize = 11.sp, color = SceneTextSecondary)
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, SceneBorder, RoundedCornerShape(12.dp))
                    .background(ScenePageBg)
                    .clickable { onToggle(true) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(value, modifier = Modifier.weight(1f), color = SceneTextPrimary, fontSize = 13.sp)
                Icon(Icons.Filled.ExpandMore, null, tint = SceneTextSecondary, modifier = Modifier.size(18.dp))
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { onToggle(false) }) {
                menuContent()
            }
        }
    }
}

@Composable
private fun ScenePreviewCard(
    colors: List<RgbSceneColorStep>,
    slots: Array<RgbPreset?>,
    playing: Boolean,
    onPlayToggle: () -> Unit,
    onStop: () -> Unit,
    onRewind: () -> Unit,
) {
    val gradientColors = colors.mapNotNull { step ->
        slots.getOrNull(step.presetSlotIndex)?.let { p ->
            val (r, g, b) = applyRgbBrightness(p.r, p.g, p.b, step.brightnessPercent)
            rgb888ComposeColor(r, g, b)
        }
    }
    SceneSectionCard(
        title = stringResource(Res.string.rgb_scene_preview_title),
        icon = { Icon(Icons.Filled.Visibility, null, tint = SceneAccent, modifier = Modifier.size(22.dp)) },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .then(
                        if (gradientColors.size >= 2) {
                            Modifier.background(Brush.horizontalGradient(gradientColors))
                        } else if (gradientColors.size == 1) {
                            Modifier.background(gradientColors.first())
                        } else {
                            Modifier.background(SceneBorder)
                        },
                    ),
            )
            IconButton(onClick = onRewind) {
                Icon(Icons.Filled.SkipPrevious, null, tint = SceneTextSecondary)
            }
            IconButton(
                onClick = onPlayToggle,
                modifier = Modifier
                    .size(48.dp)
                    .background(SceneAccent, CircleShape),
            ) {
                Icon(
                    if (playing) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                    null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp),
                )
            }
            IconButton(onClick = onStop) {
                Icon(Icons.Filled.Stop, null, tint = SceneTextSecondary)
            }
        }
    }
}

@Composable
private fun SceneSummaryBanner(colorCount: Int, loopSec: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SceneInfoSoft)
            .border(1.dp, SceneAccent.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Filled.Info, null, tint = SceneAccent, modifier = Modifier.size(22.dp))
        Text(
            stringResource(Res.string.rgb_scene_info_summary, colorCount, loopSec),
            style = MaterialTheme.typography.body2,
            color = SceneTextSecondary,
            lineHeight = 18.sp,
        )
    }
}

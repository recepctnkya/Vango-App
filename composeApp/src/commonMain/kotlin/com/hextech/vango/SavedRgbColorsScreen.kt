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
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import testvango.composeapp.generated.resources.Res
import testvango.composeapp.generated.resources.rgb_saved_active_badge
import testvango.composeapp.generated.resources.rgb_saved_active_title
import testvango.composeapp.generated.resources.rgb_saved_add_color
import testvango.composeapp.generated.resources.rgb_saved_delete
import testvango.composeapp.generated.resources.rgb_saved_edit_name
import testvango.composeapp.generated.resources.rgb_saved_filter_all
import testvango.composeapp.generated.resources.rgb_saved_filter_favorites
import testvango.composeapp.generated.resources.rgb_saved_filter_scenes
import testvango.composeapp.generated.resources.rgb_saved_groups_title
import testvango.composeapp.generated.resources.rgb_saved_info_body
import testvango.composeapp.generated.resources.rgb_saved_info_title
import testvango.composeapp.generated.resources.rgb_saved_new_color
import testvango.composeapp.generated.resources.rgb_saved_page_subtitle
import testvango.composeapp.generated.resources.rgb_saved_scene_colors_count
import testvango.composeapp.generated.resources.rgb_saved_scene_cinema
import testvango.composeapp.generated.resources.rgb_saved_scene_cinema_desc
import testvango.composeapp.generated.resources.rgb_saved_scene_party
import testvango.composeapp.generated.resources.rgb_saved_scene_party_desc
import testvango.composeapp.generated.resources.rgb_saved_scene_rest
import testvango.composeapp.generated.resources.rgb_saved_scene_rest_desc
import testvango.composeapp.generated.resources.rgb_saved_sort
import testvango.composeapp.generated.resources.rgb_saved_sort_name
import testvango.composeapp.generated.resources.rgb_saved_sort_slot
import testvango.composeapp.generated.resources.color_picker_close
import testvango.composeapp.generated.resources.rgb_saved_custom_color
import testvango.composeapp.generated.resources.rgb_scene_edit_short
import testvango.composeapp.generated.resources.rgb_scene_play
import testvango.composeapp.generated.resources.rgb_scene_stop_playback
import testvango.composeapp.generated.resources.settings_rgb_presets_title

/** Aktif / başarı rozeti — her iki temada okunaklı sabit yeşil. */
private val RgbSuccessGreen = Color(0xFF22C55E)

@Composable
private fun rgbBorderColor(): Color = MaterialTheme.colors.onSurface.copy(alpha = 0.12f)

@Composable
private fun rgbMutedText(): Color = MaterialTheme.colors.onSurface.copy(alpha = 0.65f)

@Composable
private fun rgbPrimarySoftBg(): Color = MaterialTheme.colors.primary.copy(alpha = 0.12f)

@Composable
private fun rgbSelectedChipBg(): Color = MaterialTheme.colors.primary.copy(alpha = 0.08f)

@Composable
private fun rgbInfoBannerBg(): Color = MaterialTheme.colors.primary.copy(alpha = 0.12f)

@Composable
private fun rgbCardFooterBg(): Color = MaterialTheme.colors.onSurface.copy(alpha = 0.04f)

@Composable
private fun rgbSuccessSoftBg(): Color = RgbSuccessGreen.copy(alpha = 0.15f)

private enum class RgbColorFilter { All, Favorites, Scenes }

private enum class RgbSortOrder { Slot, Name }

private data class RgbGridItem(val index: Int, val preset: RgbPreset?)

@Composable
fun RgbPresetsSetting() {
    SavedRgbColorsScreen()
}

@Composable
fun SavedRgbColorsScreen() {
    SavedRgbColorsScreenBody()
}

@Composable
private fun SavedRgbColorsScreenBody() {
    var editingSceneId by remember { mutableStateOf<String?>(null) }
    if (editingSceneId != null) {
        EditRgbSceneScreen(sceneId = editingSceneId!!, onBack = { editingSceneId = null })
        return
    }

    DisposableEffect(Unit) {
        onDispose { RgbScenePlayer.stop() }
    }

    val data by vanGoData.collectAsState()
    val scope = rememberCoroutineScope()
    var slots by remember { mutableStateOf(ensureRgbSlotsInitialized()) }
    var filter by remember { mutableStateOf(RgbColorFilter.All) }
    var sortOrder by remember { mutableStateOf(RgbSortOrder.Slot) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    var editSlotIndex by remember { mutableStateOf<Int?>(null) }
    var menuSlotIndex by remember { mutableStateOf<Int?>(null) }
    var renameSlotIndex by remember { mutableStateOf<Int?>(null) }
    var renameDraft by remember { mutableStateOf("") }
    var showRenameDialog by remember { mutableStateOf(false) }
    var draftRgb by remember { mutableStateOf<Triple<Int, Int, Int>?>(null) }
    val rgbEnabled = data.RGBE == "yes"

    val (curR, curG, curB) = data.rgb888ForUi()
    val activeIndex = slots.indexOfFirst { p ->
        p != null && colorsMatch(p.r, p.g, p.b, curR, curG, curB)
    }

    fun persist(updated: Array<RgbPreset?>) {
        slots = updated
        saveRgbSlots(updated)
    }

    fun applyPreset(preset: RgbPreset) {
        scope.launch(Dispatchers.Default) {
            getAppContext().vanGO.writeRGBPort(preset.r, preset.g, preset.b, 1)
        }
    }

    val filledCount = slots.count { it != null }
    val favoriteCount = slots.count { it?.favorite == true }

    val gridItems = remember(slots, filter, sortOrder) {
        val indices = slots.indices.map { RgbGridItem(it, slots[it]) }
        val filtered = when (filter) {
            RgbColorFilter.All -> indices
            RgbColorFilter.Favorites -> indices.filter { it.preset?.favorite == true }
            RgbColorFilter.Scenes -> indices.filter { it.preset != null }
        }
        when (sortOrder) {
            RgbSortOrder.Slot -> filtered.sortedBy { it.index }
            RgbSortOrder.Name -> filtered.sortedBy { it.preset?.name?.lowercase() ?: "zzz" }
        }
    }

    LaunchedEffect(showColorPicker) {
        if (showColorPicker) {
            draftRgb = vanGoData.value.rgb888ForUi()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            SavedRgbHeader(
                onBack = { backToMenuGrid() },
                onNewColor = {
                    editSlotIndex = slots.indexOfFirst { it == null }.takeIf { it >= 0 }
                        ?: slots.indices.maxByOrNull { slots[it]?.name ?: "" } ?: 0
                    showColorPicker = true
                },
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                SavedRgbActiveCard(
                    preset = activeIndex.takeIf { it >= 0 }?.let { slots[it] },
                    slotIndex = activeIndex,
                    r = curR,
                    g = curG,
                    b = curB,
                )

                SavedRgbFilterRow(
                    filter = filter,
                    allCount = filledCount,
                    favoriteCount = favoriteCount,
                    sceneCount = 3,
                    onFilterChange = { filter = it },
                    sortMenuExpanded = sortMenuExpanded,
                    onSortMenuToggle = { sortMenuExpanded = it },
                    sortOrder = sortOrder,
                    onSortOrderChange = { sortOrder = it; sortMenuExpanded = false },
                )

                if (filter != RgbColorFilter.Scenes) {
                    SavedRgbColorGrid(
                        items = gridItems,
                        activeIndex = activeIndex,
                        onApply = { item -> item.preset?.let { applyPreset(it) } },
                        onToggleFavorite = { item ->
                            val p = slots[item.index] ?: return@SavedRgbColorGrid
                            val copy = slots.copyOf()
                            copy[item.index] = p.copy(favorite = !p.favorite)
                            persist(copy)
                        },
                        onMenu = { menuSlotIndex = it },
                        onAdd = { index ->
                            editSlotIndex = index
                            showColorPicker = true
                        },
                    )
                }

                if (filter != RgbColorFilter.Favorites) {
                    SavedRgbScenesSection(
                        rgbEnabled = rgbEnabled,
                        onEditScene = { editingSceneId = it },
                    )
                }

                SavedRgbInfoBanner()
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    if (showColorPicker && rgbEnabled) {
        val (dr, dg, db) = data.rgb888ForUi()
        ColorPaletteDialog(
            lightning = data.getSafeRGBData(3) > 0,
            deviceR = dr,
            deviceG = dg,
            deviceB = db,
            onColorChange = { r, g, b -> draftRgb = Triple(r, g, b) },
            onDismiss = {
                val idx = editSlotIndex
                val rgb = draftRgb
                if (idx != null && rgb != null) {
                    val existing = slots[idx]
                    val copy = slots.copyOf()
                    copy[idx] = RgbPreset(
                        name = existing?.name ?: defaultRgbPresetNameStatic(idx),
                        r = rgb.first,
                        g = rgb.second,
                        b = rgb.third,
                        favorite = existing?.favorite == true,
                    )
                    persist(copy)
                }
                showColorPicker = false
                editSlotIndex = null
                draftRgb = null
            },
            onLiveRgb = { r, g, b ->
                draftRgb = Triple(r, g, b)
                scope.launch(Dispatchers.Default) {
                    getAppContext().vanGO.writeRGBPort(r, g, b, 1)
                }
            },
        )
    }

    val menuIndex = menuSlotIndex
    if (menuIndex != null) {
        AlertDialog(
            onDismissRequest = { menuSlotIndex = null },
            title = { Text(slots[menuIndex]?.name ?: "") },
            text = {
                Column {
                    TextButton(onClick = {
                        renameSlotIndex = menuIndex
                        renameDraft = slots[menuIndex]?.name.orEmpty()
                        showRenameDialog = true
                        menuSlotIndex = null
                    }) { Text(stringResource(Res.string.rgb_saved_edit_name)) }
                    TextButton(onClick = {
                        editSlotIndex = menuIndex
                        draftRgb = slots[menuIndex]?.let { Triple(it.r, it.g, it.b) }
                        showColorPicker = true
                        menuSlotIndex = null
                    }) { Text(stringResource(Res.string.rgb_saved_add_color)) }
                    TextButton(onClick = {
                        val copy = slots.copyOf()
                        copy[menuIndex] = null
                        persist(copy)
                        menuSlotIndex = null
                    }) {
                        Text(
                            stringResource(Res.string.rgb_saved_delete),
                            color = MaterialTheme.colors.error,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { menuSlotIndex = null }) {
                    Text(stringResource(Res.string.color_picker_close))
                }
            },
        )
    }

    val renameIdx = renameSlotIndex
    if (showRenameDialog && renameIdx != null) {
        AlertDialog(
            onDismissRequest = {
                showRenameDialog = false
                renameSlotIndex = null
            },
            title = { Text(stringResource(Res.string.rgb_saved_edit_name)) },
            text = {
                TextField(
                    value = renameDraft,
                    onValueChange = { renameDraft = it },
                    singleLine = true,
                    colors = TextFieldDefaults.textFieldColors(
                        focusedIndicatorColor = MaterialTheme.colors.primary,
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val p = slots[renameIdx] ?: return@TextButton
                    val copy = slots.copyOf()
                    copy[renameIdx] = p.copy(name = renameDraft.trim().ifBlank { p.name })
                    persist(copy)
                    showRenameDialog = false
                    renameSlotIndex = null
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRenameDialog = false
                    renameSlotIndex = null
                }) {
                    Text(stringResource(Res.string.color_picker_close))
                }
            },
        )
    }
}

private fun defaultRgbPresetNameStatic(index: Int): String = when (index) {
    0 -> "Mor Rüya"
    1 -> "Okyanus"
    2 -> "Doğa Yeşili"
    3 -> "Gün Batımı"
    4 -> "Pembe Şeker"
    5 -> "Buz Mavisi"
    6 -> "Sıcak Beyaz"
    7 -> "Soğuk Beyaz"
    8 -> "Ateş Kırmızısı"
    9 -> "Gece Moru"
    10 -> "Nane Yeşili"
    else -> "Parti Modu"
}

private fun colorsMatch(r1: Int, g1: Int, b1: Int, r2: Int, g2: Int, b2: Int): Boolean =
    kotlin.math.abs(r1 - r2) <= 3 &&
        kotlin.math.abs(g1 - g2) <= 3 &&
        kotlin.math.abs(b1 - b2) <= 3

@Composable
private fun SavedRgbColorGrid(
    items: List<RgbGridItem>,
    activeIndex: Int,
    onApply: (RgbGridItem) -> Unit,
    onToggleFavorite: (RgbGridItem) -> Unit,
    onMenu: (Int) -> Unit,
    onAdd: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.chunked(3).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                row.forEach { item ->
                    SavedRgbColorCard(
                        modifier = Modifier.weight(1f),
                        index = item.index,
                        preset = item.preset,
                        isActive = item.index == activeIndex,
                        isPartyGradient = item.preset?.name?.contains("Parti", ignoreCase = true) == true,
                        onApply = { onApply(item) },
                        onToggleFavorite = { onToggleFavorite(item) },
                        onMenu = { onMenu(item.index) },
                        onAdd = { onAdd(item.index) },
                    )
                }
                repeat((3 - row.size).coerceAtLeast(0)) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SavedRgbHeader(onBack: () -> Unit, onNewColor: () -> Unit) {
    SettingsBackHeader(
        backLabel = settingsBackLabelFor(SettingsPage.RgbPresets),
        pageTitle = settingsHeaderTitleFor(SettingsPage.RgbPresets),
        onBack = onBack,
        subtitle = stringResource(Res.string.rgb_saved_page_subtitle),
        subtitleColor = rgbMutedText(),
        trailing = {
            Button(
                onClick = onNewColor,
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary),
                contentPadding = ButtonDefaults.ContentPadding,
                elevation = ButtonDefaults.elevation(defaultElevation = 0.dp),
            ) {
                Icon(Icons.Filled.Add, null, tint = MaterialTheme.colors.onPrimary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    stringResource(Res.string.rgb_saved_new_color),
                    color = MaterialTheme.colors.onPrimary,
                    fontSize = 13.sp,
                )
            }
        },
    )
}

@Composable
private fun SavedRgbActiveCard(
    preset: RgbPreset?,
    slotIndex: Int,
    r: Int,
    g: Int,
    b: Int,
) {
    val displayName = preset?.name ?: stringResource(Res.string.rgb_saved_custom_color)
    val hasLight = (r + g + b) > 24
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = 4.dp,
        backgroundColor = if (hasLight) RgbSuccessGreen.copy(alpha = 0.08f) else MaterialTheme.colors.surface,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, rgbBorderColor(), RoundedCornerShape(16.dp)),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                stringResource(Res.string.rgb_saved_active_title),
                style = MaterialTheme.typography.subtitle1,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colors.onSurface,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(rgbPrimarySoftBg(), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.ColorLens, null, tint = MaterialTheme.colors.primary, modifier = Modifier.size(26.dp))
                }
                Column(Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            displayName,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.subtitle1,
                            color = MaterialTheme.colors.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (slotIndex >= 0) {
                            Text(
                                "#${(slotIndex + 1).toString().padStart(2, '0')}",
                                modifier = Modifier
                                    .background(rgbPrimarySoftBg(), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                                color = MaterialTheme.colors.primary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                    Row(
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .background(rgbSuccessSoftBg(), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(Icons.Filled.Check, null, tint = RgbSuccessGreen, modifier = Modifier.size(14.dp))
                        Text(
                            stringResource(Res.string.rgb_saved_active_badge),
                            color = RgbSuccessGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(rgb888ComposeColor(r, g, b))
                        .border(1.dp, rgbBorderColor(), RoundedCornerShape(12.dp)),
                )
                Column(horizontalAlignment = Alignment.End) {
                    Text("R $r", fontSize = 12.sp, color = rgbMutedText())
                    Text("G $g", fontSize = 12.sp, color = rgbMutedText())
                    Text("B $b", fontSize = 12.sp, color = rgbMutedText())
                    Text(
                        preset?.hex() ?: rgbHex(r, g, b),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colors.onSurface,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

private fun rgbHex(r: Int, g: Int, b: Int): String {
    fun h(n: Int) = n.coerceIn(0, 255).toString(16).uppercase().padStart(2, '0')
    return "#${h(r)}${h(g)}${h(b)}"
}

@Composable
private fun SavedRgbFilterRow(
    filter: RgbColorFilter,
    allCount: Int,
    favoriteCount: Int,
    sceneCount: Int,
    onFilterChange: (RgbColorFilter) -> Unit,
    sortMenuExpanded: Boolean,
    onSortMenuToggle: (Boolean) -> Unit,
    sortOrder: RgbSortOrder,
    onSortOrderChange: (RgbSortOrder) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = filter == RgbColorFilter.All,
                label = stringResource(Res.string.rgb_saved_filter_all, allCount),
                onClick = { onFilterChange(RgbColorFilter.All) },
            )
            FilterChip(
                selected = filter == RgbColorFilter.Favorites,
                label = stringResource(Res.string.rgb_saved_filter_favorites, favoriteCount),
                onClick = { onFilterChange(RgbColorFilter.Favorites) },
            )
            FilterChip(
                selected = filter == RgbColorFilter.Scenes,
                label = stringResource(Res.string.rgb_saved_filter_scenes, sceneCount),
                onClick = { onFilterChange(RgbColorFilter.Scenes) },
            )
        }
        Box {
            OutlinedButton(
                onClick = { onSortMenuToggle(true) },
                contentPadding = ButtonDefaults.ContentPadding,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .background(MaterialTheme.colors.surface, RoundedCornerShape(20.dp)),
                colors = ButtonDefaults.outlinedButtonColors(
                    backgroundColor = MaterialTheme.colors.surface,
                    contentColor = MaterialTheme.colors.onSurface,
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, rgbBorderColor()),
            ) {
                Icon(Icons.Filled.Sort, null, modifier = Modifier.size(16.dp), tint = rgbMutedText())
                Spacer(Modifier.width(4.dp))
                Text(stringResource(Res.string.rgb_saved_sort), fontSize = 12.sp, color = MaterialTheme.colors.onSurface)
            }
            DropdownMenu(expanded = sortMenuExpanded, onDismissRequest = { onSortMenuToggle(false) }) {
                DropdownMenuItem(onClick = { onSortOrderChange(RgbSortOrder.Slot) }) {
                    Text(stringResource(Res.string.rgb_saved_sort_slot))
                }
                DropdownMenuItem(onClick = { onSortOrderChange(RgbSortOrder.Name) }) {
                    Text(stringResource(Res.string.rgb_saved_sort_name))
                }
            }
        }
    }
}

@Composable
private fun FilterChip(selected: Boolean, label: String, onClick: () -> Unit) {
    val borderColor = if (selected) MaterialTheme.colors.primary else rgbBorderColor()
    val bg = if (selected) rgbSelectedChipBg() else MaterialTheme.colors.surface
    Text(
        text = label,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        color = if (selected) MaterialTheme.colors.primary else rgbMutedText(),
        fontSize = 13.sp,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
    )
}

@Composable
private fun SavedRgbColorCard(
    modifier: Modifier = Modifier,
    index: Int,
    preset: RgbPreset?,
    isActive: Boolean,
    isPartyGradient: Boolean,
    onApply: () -> Unit,
    onToggleFavorite: () -> Unit,
    onMenu: () -> Unit,
    onAdd: () -> Unit,
) {
    val cardBorder = if (isActive) MaterialTheme.colors.primary else rgbBorderColor()
    Card(
        shape = RoundedCornerShape(14.dp),
        elevation = if (isActive) 4.dp else 2.dp,
        backgroundColor = MaterialTheme.colors.surface,
        modifier = modifier
            .border(if (isActive) 2.dp else 1.dp, cardBorder, RoundedCornerShape(14.dp))
            .clickable {
                if (preset != null) onApply() else onAdd()
            },
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.05f)
                    .then(
                        if (preset == null) {
                            Modifier.background(rgbCardFooterBg())
                        } else if (isPartyGradient) {
                            Modifier.background(
                                Brush.linearGradient(
                                    listOf(
                                        Color(0xFFFF0080),
                                        Color(0xFF7928CA),
                                        Color(0xFF0070F3),
                                        Color(0xFF00DFD8),
                                    ),
                                ),
                            )
                        } else {
                            Modifier.background(rgb888ComposeColor(preset.r, preset.g, preset.b))
                        },
                    ),
            ) {
                if (preset != null) {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(4.dp)
                            .size(32.dp),
                    ) {
                        Icon(
                            imageVector = if (preset.favorite) Icons.Filled.Star else Icons.Outlined.Star,
                            contentDescription = null,
                            tint = if (preset.favorite) Color(0xFFF59E0B) else Color.White.copy(0.9f),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    if (isActive) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                                .size(22.dp)
                                .background(Color.White, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.Check, null, tint = RgbSuccessGreen, modifier = Modifier.size(14.dp))
                        }
                    }
                } else {
                    Icon(
                        Icons.Filled.Add,
                        null,
                        tint = MaterialTheme.colors.primary.copy(alpha = 0.45f),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(32.dp),
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(rgbCardFooterBg())
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        preset?.name ?: "—",
                        style = MaterialTheme.typography.body2,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colors.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp),
                    ) {
                        Text(
                            "#${(index + 1).toString().padStart(2, '0')}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colors.primary,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .background(MaterialTheme.colors.primary.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 5.dp, vertical = 1.dp),
                        )
                        Text(
                            preset?.hex() ?: "",
                            fontSize = 11.sp,
                            color = rgbMutedText(),
                        )
                    }
                }
                if (preset != null) {
                    IconButton(onClick = onMenu, modifier = Modifier.size(28.dp)) {
                        Icon(
                            Icons.Filled.MoreVert,
                            null,
                            modifier = Modifier.size(18.dp),
                            tint = rgbMutedText(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedRgbScenesSection(
    rgbEnabled: Boolean,
    onEditScene: (String) -> Unit,
) {
    val slots = remember { ensureRgbSlotsInitialized() }
    val storedScenes = remember { ensureRgbScenes() }
    val playback by RgbScenePlayer.state.collectAsState()
    val sceneOrder = listOf(RGB_SCENE_REST, RGB_SCENE_CINEMA, RGB_SCENE_PARTY)
    val descResById = mapOf(
        RGB_SCENE_REST to Res.string.rgb_saved_scene_rest_desc,
        RGB_SCENE_CINEMA to Res.string.rgb_saved_scene_cinema_desc,
        RGB_SCENE_PARTY to Res.string.rgb_saved_scene_party_desc,
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            stringResource(Res.string.rgb_saved_groups_title),
            style = MaterialTheme.typography.subtitle1,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colors.onSurface,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            sceneOrder.forEach { sceneId ->
                val scene = storedScenes[sceneId] ?: return@forEach
                val descRes = descResById[sceneId]
                val isPlaying = playback.isPlaying && playback.sceneId == sceneId
                val borderColor = if (isPlaying) MaterialTheme.colors.primary else rgbBorderColor()
                Card(
                    shape = RoundedCornerShape(14.dp),
                    elevation = if (isPlaying) 4.dp else 2.dp,
                    backgroundColor = MaterialTheme.colors.surface,
                    modifier = Modifier
                        .width(200.dp)
                        .border(if (isPlaying) 2.dp else 1.dp, borderColor, RoundedCornerShape(14.dp)),
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(scene.emoji, fontSize = 28.sp)
                            IconButton(
                                onClick = { onEditScene(sceneId) },
                                modifier = Modifier.size(32.dp),
                            ) {
                                Icon(
                                    Icons.Filled.Edit,
                                    stringResource(Res.string.rgb_scene_edit_short),
                                    tint = rgbMutedText(),
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                        Text(
                            scene.name,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.subtitle1,
                            color = MaterialTheme.colors.onSurface,
                        )
                        if (descRes != null) {
                            Text(
                                stringResource(descRes),
                                style = MaterialTheme.typography.caption,
                                color = rgbMutedText(),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Text(
                            stringResource(Res.string.rgb_saved_scene_colors_count, scene.colors.size),
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.primary,
                            fontWeight = FontWeight.Medium,
                        )
                        OutlinedButton(
                            onClick = {
                                if (rgbEnabled) {
                                    RgbScenePlayer.toggleScene(scene, slots)
                                }
                            },
                            enabled = rgbEnabled && scene.colors.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (isPlaying) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface,
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isPlaying) MaterialTheme.colors.primary else rgbBorderColor(),
                            ),
                        ) {
                            Icon(
                                if (isPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                                null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                if (isPlaying) {
                                    stringResource(Res.string.rgb_scene_stop_playback)
                                } else {
                                    stringResource(Res.string.rgb_scene_play)
                                },
                                fontSize = 12.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedRgbInfoBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(rgbInfoBannerBg())
            .border(1.dp, MaterialTheme.colors.primary.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(Icons.Filled.Info, null, tint = MaterialTheme.colors.primary, modifier = Modifier.size(22.dp))
        Column {
            Text(
                stringResource(Res.string.rgb_saved_info_title),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colors.primary,
                style = MaterialTheme.typography.subtitle2,
            )
            Text(
                stringResource(Res.string.rgb_saved_info_body),
                style = MaterialTheme.typography.body2,
                color = rgbMutedText(),
                modifier = Modifier.padding(top = 4.dp),
                lineHeight = 18.sp,
            )
        }
    }
}

package com.hextech.vango

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.jetbrains.compose.resources.stringResource
import testvango.composeapp.generated.resources.Res
import testvango.composeapp.generated.resources.rgb_preset_clear
import testvango.composeapp.generated.resources.rgb_preset_hint_name
import testvango.composeapp.generated.resources.rgb_preset_save_here
import testvango.composeapp.generated.resources.rgb_preset_slot
import testvango.composeapp.generated.resources.rgb_preset_default_1
import testvango.composeapp.generated.resources.rgb_preset_default_10
import testvango.composeapp.generated.resources.rgb_preset_default_2
import testvango.composeapp.generated.resources.rgb_preset_default_3
import testvango.composeapp.generated.resources.rgb_preset_default_4
import testvango.composeapp.generated.resources.rgb_preset_default_5
import testvango.composeapp.generated.resources.rgb_preset_default_6
import testvango.composeapp.generated.resources.rgb_preset_default_7
import testvango.composeapp.generated.resources.rgb_preset_default_8
import testvango.composeapp.generated.resources.rgb_preset_default_9
import testvango.composeapp.generated.resources.color_picker_close
import testvango.composeapp.generated.resources.color_picker_title
import testvango.composeapp.generated.resources.rgb_saved_picker_empty
import testvango.composeapp.generated.resources.rgb_saved_picker_title
import testvango.composeapp.generated.resources.settings_rgb_presets_desc
import testvango.composeapp.generated.resources.settings_rgb_presets_empty_hint
import testvango.composeapp.generated.resources.settings_rgb_presets_title

/** Opaque sRGB swatch from 8-bit RGB (matches saved preset values). */
internal fun rgb888ComposeColor(r: Int, g: Int, b: Int): Color {
    val rr = r.coerceIn(0, 255)
    val gg = g.coerceIn(0, 255)
    val bb = b.coerceIn(0, 255)
    return Color((0xFF shl 24) or (rr shl 16) or (gg shl 8) or bb)
}

@Composable
fun defaultRgbPresetName(index: Int): String {
    val res = when (index) {
        0 -> Res.string.rgb_preset_default_1
        1 -> Res.string.rgb_preset_default_2
        2 -> Res.string.rgb_preset_default_3
        3 -> Res.string.rgb_preset_default_4
        4 -> Res.string.rgb_preset_default_5
        5 -> Res.string.rgb_preset_default_6
        6 -> Res.string.rgb_preset_default_7
        7 -> Res.string.rgb_preset_default_8
        8 -> Res.string.rgb_preset_default_9
        else -> Res.string.rgb_preset_default_10
    }
    return stringResource(res)
}

@Composable
private fun defaultRgbPresetNames(): List<String> = listOf(
    stringResource(Res.string.rgb_preset_default_1),
    stringResource(Res.string.rgb_preset_default_2),
    stringResource(Res.string.rgb_preset_default_3),
    stringResource(Res.string.rgb_preset_default_4),
    stringResource(Res.string.rgb_preset_default_5),
    stringResource(Res.string.rgb_preset_default_6),
    stringResource(Res.string.rgb_preset_default_7),
    stringResource(Res.string.rgb_preset_default_8),
    stringResource(Res.string.rgb_preset_default_9),
    stringResource(Res.string.rgb_preset_default_10),
)

@Composable
fun SavedRgbColorsPickerDialog(
    onDismiss: () -> Unit,
    onPick: (RgbPreset) -> Unit
) {
    val slots = loadRgbSlots()
    val filled = slots.mapIndexedNotNull { i, p -> p?.let { i to it } }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.onBackground.copy(alpha = 0.55f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .background(MaterialTheme.colors.surface, RoundedCornerShape(16.dp))
                    .clickable(enabled = false) { }
                    .padding(16.dp)
            ) {
                Text(
                    text = stringResource(Res.string.rgb_saved_picker_title),
                    style = MaterialTheme.typography.h6,
                    color = MaterialTheme.colors.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(12.dp))
                if (filled.isEmpty()) {
                    Text(
                        text = stringResource(Res.string.rgb_saved_picker_empty),
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.75f)
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.height(280.dp)
                    ) {
                        items(filled.size) { index ->
                            val (_, preset) = filled[index]
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        MaterialTheme.colors.background,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        onPick(preset)
                                        onDismiss()
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(
                                                rgb888ComposeColor(preset.r, preset.g, preset.b),
                                                CircleShape
                                            )
                                            .border(
                                                1.dp,
                                                MaterialTheme.colors.onSurface.copy(alpha = 0.25f),
                                                CircleShape
                                            )
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            preset.name,
                                            style = MaterialTheme.typography.body1,
                                            color = MaterialTheme.colors.onSurface
                                        )
                                        Text(
                                            "RGB ${preset.r}, ${preset.g}, ${preset.b}",
                                            style = MaterialTheme.typography.caption,
                                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(Res.string.color_picker_close),
                    color = MaterialTheme.colors.primary,
                    style = MaterialTheme.typography.button,
                    modifier = Modifier
                        .clickable { onDismiss() }
                        .padding(8.dp)
                )
            }
        }
    }
}

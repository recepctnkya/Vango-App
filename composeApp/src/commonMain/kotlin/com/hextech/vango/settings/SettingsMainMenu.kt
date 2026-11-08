package com.hextech.vango

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Translate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import testvango.composeapp.generated.resources.Res
import testvango.composeapp.generated.resources.exit_confirm_message
import testvango.composeapp.generated.resources.exit_confirm_no
import testvango.composeapp.generated.resources.exit_confirm_title
import testvango.composeapp.generated.resources.exit_confirm_yes
import testvango.composeapp.generated.resources.profile_title
import testvango.composeapp.generated.resources.settings_about_title
import testvango.composeapp.generated.resources.settings_climate_menu_title
import testvango.composeapp.generated.resources.settings_connection_title
import testvango.composeapp.generated.resources.settings_exit_title
import testvango.composeapp.generated.resources.settings_language_menu_title
import testvango.composeapp.generated.resources.settings_panel_title
import testvango.composeapp.generated.resources.settings_rgb_presets_menu_title
import testvango.composeapp.generated.resources.settings_theme_menu_title

@Composable
fun MainSetting(
    onClickConnection: () -> Unit,
    onClickPanel: () -> Unit,
    onClickTheme: () -> Unit,
    onClickLanguage: () -> Unit,
    onClickRgbPresets: () -> Unit,
    onClickClimate: () -> Unit,
    onClickAbout: () -> Unit,
    onClickProfile: () -> Unit,
) {
    var showExitDialog by remember { mutableStateOf(false) }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text(stringResource(Res.string.exit_confirm_title)) },
            text = { Text(stringResource(Res.string.exit_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showExitDialog = false
                    closeApplication()
                }) {
                    Text(stringResource(Res.string.exit_confirm_yes))
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text(stringResource(Res.string.exit_confirm_no))
                }
            },
        )
    }

    data class MenuTile(
        val title: String,
        val icon: ImageVector,
        val onClick: () -> Unit,
        val accent: Boolean = false,
        val testTag: String? = null,
    )

    val tiles: List<MenuTile> = listOf(
        MenuTile(
            stringResource(Res.string.settings_connection_title),
            Icons.Filled.Bluetooth,
            onClickConnection,
            testTag = VanGoTestTags.MENU_CONNECTION,
        ),
        MenuTile(stringResource(Res.string.settings_panel_title), Icons.Filled.Build, onClickPanel),
        MenuTile(stringResource(Res.string.settings_climate_menu_title), Icons.Filled.AcUnit, onClickClimate),
        MenuTile(stringResource(Res.string.settings_language_menu_title), Icons.Filled.Translate, onClickLanguage),
        MenuTile(stringResource(Res.string.settings_theme_menu_title), Icons.Filled.BrightnessMedium, onClickTheme),
        MenuTile(stringResource(Res.string.settings_rgb_presets_menu_title), Icons.Filled.ColorLens, onClickRgbPresets),
        MenuTile(stringResource(Res.string.profile_title), Icons.Filled.Person, onClickProfile),
        MenuTile(stringResource(Res.string.settings_exit_title), Icons.Filled.ExitToApp, { showExitDialog = true }, accent = true),
        MenuTile(stringResource(Res.string.settings_about_title), Icons.Filled.Info, onClickAbout),
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background)
    ) {
        val isLandscape = maxWidth > maxHeight
        val columns = if (isLandscape) 3 else 2
        val minSide = minOf(maxWidth, maxHeight)
        val scale = (minSide.value / 420f).coerceIn(0.72f, 1.42f)
        val pad = (minSide.value * 0.032f).dp.coerceIn(10.dp, 28.dp)
        val gap = (minSide.value * 0.022f).dp.coerceIn(8.dp, 18.dp)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad),
            verticalArrangement = Arrangement.spacedBy(gap)
        ) {
            tiles.chunked(columns).forEach { rowTiles ->
                Row(
                    modifier = Modifier
                        .weight(1f, fill = true)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(gap)
                ) {
                    rowTiles.forEach { tile ->
                        SettingsGridTile(
                            title = tile.title,
                            icon = tile.icon,
                            onClick = tile.onClick,
                            modifier = Modifier.weight(1f),
                            scale = scale,
                            accent = tile.accent,
                            testTag = tile.testTag,
                        )
                    }
                    repeat(columns - rowTiles.size) {
                        Spacer(Modifier.weight(1f).fillMaxHeight())
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsGridTile(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    scale: Float = 1f,
    accent: Boolean = false,
    testTag: String? = null,
) {
    val iconDp = (40f * scale).dp.coerceIn(26.dp, 58.dp)
    val padH = (14f * scale).dp.coerceIn(8.dp, 22.dp)
    val padV = (12f * scale).dp.coerceIn(8.dp, 20.dp)
    val titleStyle = MaterialTheme.typography.subtitle1.copy(
        fontSize = ((16f * scale).coerceIn(13f, 22f)).sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = ((20f * scale).coerceIn(16f, 26f)).sp,
    )
    val accentColor = MaterialTheme.colors.error
    Card(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        shape = RoundedCornerShape((14f * scale).dp.coerceIn(12.dp, 22.dp)),
        elevation = (4f * scale).dp.coerceIn(2.dp, 8.dp),
        backgroundColor = if (accent) accentColor.copy(alpha = 0.1f) else MaterialTheme.colors.surface,
        contentColor = MaterialTheme.colors.onSurface
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .then(if (testTag != null) Modifier.vangoTestTag(testTag) else Modifier)
                .clickable(onClick = onClick)
                .padding(horizontal = padH, vertical = padV),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = titleStyle,
                color = if (accent) accentColor else MaterialTheme.colors.onSurface,
                modifier = Modifier.weight(1f),
                maxLines = 3
            )
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (accent) accentColor else MaterialTheme.colors.primary,
                modifier = Modifier
                    .padding(start = (6f * scale).dp.coerceIn(4.dp, 12.dp))
                    .size(iconDp)
            )
        }
    }
}

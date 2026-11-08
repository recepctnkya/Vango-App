package com.hextech.vango

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
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButton
import androidx.compose.material.RadioButtonDefaults
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness5
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import testvango.composeapp.generated.resources.Res
import testvango.composeapp.generated.resources.settings_theme_accent_green
import testvango.composeapp.generated.resources.settings_theme_accent_orange
import testvango.composeapp.generated.resources.settings_theme_accent_purple
import testvango.composeapp.generated.resources.settings_theme_accent_red
import testvango.composeapp.generated.resources.settings_theme_accent_vango
import testvango.composeapp.generated.resources.settings_theme_active
import testvango.composeapp.generated.resources.settings_theme_blue
import testvango.composeapp.generated.resources.settings_theme_current
import testvango.composeapp.generated.resources.settings_theme_customize_subtitle
import testvango.composeapp.generated.resources.settings_theme_customize_title
import testvango.composeapp.generated.resources.settings_theme_dark
import testvango.composeapp.generated.resources.settings_theme_dark_desc
import testvango.composeapp.generated.resources.settings_theme_info_body
import testvango.composeapp.generated.resources.settings_theme_info_title
import testvango.composeapp.generated.resources.settings_theme_light
import testvango.composeapp.generated.resources.settings_theme_light_desc
import testvango.composeapp.generated.resources.settings_theme_options_title
import testvango.composeapp.generated.resources.settings_theme_page_hint
import testvango.composeapp.generated.resources.settings_theme_page_title
import testvango.composeapp.generated.resources.settings_theme_save
import testvango.composeapp.generated.resources.settings_theme_system
import testvango.composeapp.generated.resources.settings_theme_system_desc

private val ThemeGreen = Color(0xFF22C55E)
private val ThemeGreenSoft = Color(0xFFDCFCE7)
private val ThemeBlueSoft = Color(0xFFE8F4FD)
private val ThemePreviewLightBg = Color(0xFFF8FAFC)
private val ThemePreviewDarkBg = Color(0xFF1E293B)
private val ThemePreviewBorder = Color(0xFFE2E8F0)
private val ThemeToggleGreen = Color(0xFF22C55E)

private data class ThemeListItem(
    val id: String,
    val titleRes: StringResource,
    val subtitleRes: StringResource,
    val icon: ImageVector,
    val preview: ThemePreviewStyle,
)

private enum class ThemePreviewStyle {
    Light,
    Dark,
    System,
}

private data class AccentOption(
    val id: String,
    val color: Color,
    val labelRes: StringResource,
)

@Composable
fun ThemeSettingScreen(
    activeMode: AppThemeMode,
    selectedId: String,
    onSelectedIdChange: (String) -> Unit,
    selectedAccentId: String,
    onSelectedAccentIdChange: (String) -> Unit,
    onBack: () -> Unit,
    onSave: () -> Unit,
    saving: Boolean,
) {
    val options = rememberThemeOptions()
    val accents = rememberAccentOptions()
    val accentColor = accentColorFromId(selectedAccentId)
    val activeName = themeDisplayName(activeMode)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        ThemeSettingsHeader(
            backLabel = settingsBackLabelFor(SettingsPage.Theme),
            pageTitle = settingsHeaderTitleFor(SettingsPage.Theme),
            onBack = onBack,
            onSave = onSave,
            saving = saving,
        )
        Text(
            text = stringResource(Res.string.settings_theme_page_hint),
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.68f),
            modifier = Modifier.padding(bottom = 12.dp),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ThemeCurrentCard(
                activeName = activeName,
                modeIcon = modeIconFor(activeMode),
            )
            ThemeOptionsSection(
                options = options,
                selectedId = selectedId,
                accentColor = accentColor,
                onSelectedIdChange = onSelectedIdChange,
            )
            ThemeCustomizeSection(
                accents = accents,
                selectedAccentId = selectedAccentId,
                onSelectedAccentIdChange = onSelectedAccentIdChange,
            )
            ThemeInfoBanner()
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun rememberThemeOptions(): List<ThemeListItem> = remember {
    listOf(
        ThemeListItem(
            id = "light",
            titleRes = Res.string.settings_theme_light,
            subtitleRes = Res.string.settings_theme_light_desc,
            icon = Icons.Filled.Brightness5,
            preview = ThemePreviewStyle.Light,
        ),
        ThemeListItem(
            id = "dark",
            titleRes = Res.string.settings_theme_dark,
            subtitleRes = Res.string.settings_theme_dark_desc,
            icon = Icons.Filled.Brightness4,
            preview = ThemePreviewStyle.Dark,
        ),
        ThemeListItem(
            id = "system",
            titleRes = Res.string.settings_theme_system,
            subtitleRes = Res.string.settings_theme_system_desc,
            icon = Icons.Filled.PhoneAndroid,
            preview = ThemePreviewStyle.System,
        ),
    )
}

@Composable
private fun rememberAccentOptions(): List<AccentOption> = remember {
    listOf(
        AccentOption("vango", accentColorFromId("vango"), Res.string.settings_theme_accent_vango),
        AccentOption("green", accentColorFromId("green"), Res.string.settings_theme_accent_green),
        AccentOption("purple", accentColorFromId("purple"), Res.string.settings_theme_accent_purple),
        AccentOption("orange", accentColorFromId("orange"), Res.string.settings_theme_accent_orange),
        AccentOption("red", accentColorFromId("red"), Res.string.settings_theme_accent_red),
    )
}

fun themeSelectionFromMode(mode: AppThemeMode): String = when (mode) {
    AppThemeMode.LIGHT -> "light"
    AppThemeMode.DARK -> "dark"
    AppThemeMode.BLUE -> "blue"
    AppThemeMode.SYSTEM -> "system"
}

fun modeFromThemeSelection(id: String): AppThemeMode = when (id) {
    "light" -> AppThemeMode.LIGHT
    "dark" -> AppThemeMode.DARK
    "blue" -> AppThemeMode.BLUE
    else -> AppThemeMode.SYSTEM
}

private fun modeIconFor(mode: AppThemeMode): ImageVector = when (mode) {
    AppThemeMode.DARK -> Icons.Filled.Brightness4
    AppThemeMode.BLUE, AppThemeMode.SYSTEM -> Icons.Filled.Brightness6
    AppThemeMode.LIGHT -> Icons.Filled.Brightness5
}

@Composable
private fun themeDisplayName(mode: AppThemeMode): String = when (mode) {
    AppThemeMode.LIGHT -> stringResource(Res.string.settings_theme_light)
    AppThemeMode.DARK -> stringResource(Res.string.settings_theme_dark)
    AppThemeMode.BLUE -> stringResource(Res.string.settings_theme_blue)
    AppThemeMode.SYSTEM -> stringResource(Res.string.settings_theme_system)
}

@Composable
private fun ThemeSettingsHeader(
    backLabel: String,
    pageTitle: String,
    onBack: () -> Unit,
    onSave: () -> Unit,
    saving: Boolean,
) {
    SettingsBackHeader(
        backLabel = backLabel,
        pageTitle = pageTitle,
        onBack = onBack,
        trailing = {
            Button(
                onClick = onSave,
                enabled = !saving,
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary),
            ) {
                Icon(Icons.Filled.Check, null, tint = MaterialTheme.colors.onPrimary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    stringResource(Res.string.settings_theme_save),
                    color = MaterialTheme.colors.onPrimary,
                    fontSize = 13.sp,
                )
            }
        },
    )
}

@Composable
private fun ThemeCurrentCard(activeName: String, modeIcon: ImageVector) {
    Card(shape = RoundedCornerShape(16.dp), elevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(ThemeBlueSoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.ColorLens, null, tint = MaterialTheme.colors.primary, modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(Res.string.settings_theme_current),
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface.copy(0.65f),
                )
                Text(
                    activeName,
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colors.primary,
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(ThemeGreenSoft)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Check, null, tint = ThemeGreen, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        stringResource(Res.string.settings_theme_active),
                        style = MaterialTheme.typography.caption,
                        color = ThemeGreen,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, MaterialTheme.colors.primary.copy(0.35f), CircleShape)
                    .background(ThemeBlueSoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(modeIcon, null, tint = MaterialTheme.colors.primary, modifier = Modifier.size(28.dp))
            }
        }
    }
}

@Composable
private fun ThemeOptionsSection(
    options: List<ThemeListItem>,
    selectedId: String,
    accentColor: Color,
    onSelectedIdChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Brightness5, null, tint = MaterialTheme.colors.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(Res.string.settings_theme_options_title),
                style = MaterialTheme.typography.subtitle1,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            options.forEach { item ->
                ThemeOptionCard(
                    item = item,
                    selected = selectedId == item.id,
                    accentColor = accentColor,
                    onSelect = { onSelectedIdChange(item.id) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ThemeOptionCard(
    item: ThemeListItem,
    selected: Boolean,
    accentColor: Color,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = if (selected) MaterialTheme.colors.primary else ThemePreviewBorder
    Card(
        modifier = modifier
            .clickable(onClick = onSelect)
            .border(2.dp, borderColor, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        elevation = if (selected) 6.dp else 2.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                RadioButton(
                    selected = selected,
                    onClick = onSelect,
                    modifier = Modifier.align(Alignment.TopEnd),
                    colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colors.primary),
                )
            }
            ThemeMiniDashboardPreview(
                style = item.preview,
                accentColor = accentColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp),
            )
            Spacer(Modifier.height(8.dp))
            Icon(item.icon, null, tint = MaterialTheme.colors.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(item.titleRes),
                style = MaterialTheme.typography.caption,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            Text(
                stringResource(item.subtitleRes),
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(0.58f),
                textAlign = TextAlign.Center,
                fontSize = 10.sp,
                lineHeight = 12.sp,
                maxLines = 2,
            )
            if (selected) {
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(Res.string.settings_theme_active),
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colors.primary.copy(0.1f))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun ThemeMiniDashboardPreview(
    style: ThemePreviewStyle,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, ThemePreviewBorder, RoundedCornerShape(10.dp)),
    ) {
        when (style) {
            ThemePreviewStyle.Light -> MiniDashboardContent(ThemePreviewLightBg, accentColor)
            ThemePreviewStyle.Dark -> MiniDashboardContent(ThemePreviewDarkBg, accentColor.copy(0.85f))
            ThemePreviewStyle.System -> Row(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    MiniDashboardContent(ThemePreviewLightBg, accentColor)
                }
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    MiniDashboardContent(ThemePreviewDarkBg, accentColor.copy(0.85f))
                }
            }
        }
    }
}

@Composable
private fun MiniDashboardContent(background: Color, accentColor: Color) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(accentColor.copy(alpha = 0.55f)),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(0.65f)
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(accentColor.copy(alpha = 0.35f)),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(ThemeToggleGreen),
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(accentColor.copy(alpha = 0.25f)),
            )
        }
    }
}

@Composable
private fun ThemeCustomizeSection(
    accents: List<AccentOption>,
    selectedAccentId: String,
    onSelectedAccentIdChange: (String) -> Unit,
) {
    Card(shape = RoundedCornerShape(16.dp), elevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Tune, null, tint = MaterialTheme.colors.primary, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(Res.string.settings_theme_customize_title),
                        style = MaterialTheme.typography.subtitle1,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        stringResource(Res.string.settings_theme_customize_subtitle),
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(0.65f),
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                accents.forEach { accent ->
                    AccentColorChip(
                        option = accent,
                        selected = selectedAccentId == accent.id,
                        onSelect = { onSelectedAccentIdChange(accent.id) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun AccentColorChip(
    option: AccentOption,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = if (selected) option.color else ThemePreviewBorder
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(2.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onSelect)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(option.color),
            )
            if (selected) {
                Icon(
                    Icons.Filled.Check,
                    null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(option.labelRes),
            style = MaterialTheme.typography.caption,
            fontSize = 9.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            lineHeight = 11.sp,
        )
    }
}

@Composable
private fun ThemeInfoBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ThemeBlueSoft)
            .border(1.dp, MaterialTheme.colors.primary.copy(0.15f), RoundedCornerShape(12.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icons.Filled.Info, null, tint = MaterialTheme.colors.primary, modifier = Modifier.size(22.dp))
        Column {
            Text(
                stringResource(Res.string.settings_theme_info_title),
                style = MaterialTheme.typography.subtitle2,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colors.primary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(Res.string.settings_theme_info_body),
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(0.8f),
            )
        }
    }
}

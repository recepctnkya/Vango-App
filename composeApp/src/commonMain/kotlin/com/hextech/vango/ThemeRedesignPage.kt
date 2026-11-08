package com.hextech.vango

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import testvango.composeapp.generated.resources.Res
import testvango.composeapp.generated.resources.settings_theme_blue
import testvango.composeapp.generated.resources.settings_theme_dark
import testvango.composeapp.generated.resources.settings_theme_light
import testvango.composeapp.generated.resources.settings_theme_page_hint
import testvango.composeapp.generated.resources.settings_theme_rd_accent_title
import testvango.composeapp.generated.resources.settings_theme_rd_card_medium
import testvango.composeapp.generated.resources.settings_theme_rd_card_medium_desc
import testvango.composeapp.generated.resources.settings_theme_rd_card_soft
import testvango.composeapp.generated.resources.settings_theme_rd_card_soft_desc
import testvango.composeapp.generated.resources.settings_theme_rd_card_standard
import testvango.composeapp.generated.resources.settings_theme_rd_card_standard_desc
import testvango.composeapp.generated.resources.settings_theme_rd_card_title
import testvango.composeapp.generated.resources.settings_theme_rd_mode_title
import testvango.composeapp.generated.resources.settings_theme_rd_title
import testvango.composeapp.generated.resources.settings_theme_system

private val ThemeText: Color @Composable get() = LocalAppChrome.current.text
private val ThemeMuted: Color @Composable get() = LocalAppChrome.current.muted
private val ThemeBorder: Color @Composable get() = LocalAppChrome.current.border
private val ThemeTile: Color @Composable get() = LocalAppChrome.current.tile
private val ThemeCard: Color @Composable get() = LocalAppChrome.current.card
private val ThemeInk: Color @Composable get() = LocalAppChrome.current.ink

private data class ThemeModeOption(
    val mode: AppThemeMode,
    val title: String,
    val preview: ThemePreviewKind,
)

private enum class ThemePreviewKind { Dark, Blue, Light, System }

private data class ThemeAccentSwatch(val id: String, val color: Color)

private data class ThemeCardOption(
    val style: AppCardStyle,
    val titleRes: StringResource,
    val descRes: StringResource,
)

@Composable
fun ThemeRedesignPage() {
    val accent = accentColorFromId(customAppAccentId)
    var draftMode by remember { mutableStateOf(customAppThemeMode) }
    var draftAccent by remember { mutableStateOf(normalizeThemeAccentId(customAppAccentId)) }
    var draftCard by remember { mutableStateOf(customAppCardStyle) }

    LaunchedEffect(customAppThemeMode) { draftMode = customAppThemeMode }
    LaunchedEffect(customAppAccentId) { draftAccent = normalizeThemeAccentId(customAppAccentId) }
    LaunchedEffect(customAppCardStyle) { draftCard = customAppCardStyle }

    val modes = listOf(
        ThemeModeOption(AppThemeMode.DARK, stringResource(Res.string.settings_theme_dark), ThemePreviewKind.Dark),
        ThemeModeOption(AppThemeMode.BLUE, stringResource(Res.string.settings_theme_blue), ThemePreviewKind.Blue),
        ThemeModeOption(AppThemeMode.LIGHT, stringResource(Res.string.settings_theme_light), ThemePreviewKind.Light),
        ThemeModeOption(AppThemeMode.SYSTEM, stringResource(Res.string.settings_theme_system), ThemePreviewKind.System),
    )
    val accents = remember {
        listOf(
            ThemeAccentSwatch("vango", accentColorFromId("vango")),
            ThemeAccentSwatch("blue", accentColorFromId("blue")),
            ThemeAccentSwatch("purple", accentColorFromId("purple")),
            ThemeAccentSwatch("pink", accentColorFromId("pink")),
            ThemeAccentSwatch("orange", accentColorFromId("orange")),
            ThemeAccentSwatch("yellow", accentColorFromId("yellow")),
            ThemeAccentSwatch("lime", accentColorFromId("lime")),
            ThemeAccentSwatch("green", accentColorFromId("green")),
        )
    }
    val cardOptions = listOf(
        ThemeCardOption(
            AppCardStyle.STANDARD,
            Res.string.settings_theme_rd_card_standard,
            Res.string.settings_theme_rd_card_standard_desc,
        ),
        ThemeCardOption(
            AppCardStyle.MEDIUM,
            Res.string.settings_theme_rd_card_medium,
            Res.string.settings_theme_rd_card_medium_desc,
        ),
        ThemeCardOption(
            AppCardStyle.SOFT,
            Res.string.settings_theme_rd_card_soft,
            Res.string.settings_theme_rd_card_soft_desc,
        ),
    )

    fun applyLive(mode: AppThemeMode = draftMode, accentId: String = draftAccent, card: AppCardStyle = draftCard) {
        applyAppThemeMode(mode)
        applyAppAccent(accentId)
        applyAppCardStyle(card)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 14.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    Icons.Filled.SentimentSatisfied,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(34.dp),
                )
                Text(
                    stringResource(Res.string.settings_theme_rd_title).uppercase(),
                    color = ThemeText,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                )
            }
            Text(
                stringResource(Res.string.settings_theme_page_hint),
                color = ThemeMuted,
                fontSize = 13.sp,
                lineHeight = 19.sp,
            )
        }

        ThemeSectionCard {
            ThemeSectionLabel(stringResource(Res.string.settings_theme_rd_mode_title))
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                modes.forEach { option ->
                    ThemeModePreviewCard(
                        modifier = Modifier.weight(1f),
                        title = option.title,
                        kind = option.preview,
                        accent = accentColorFromId(draftAccent),
                        selected = draftMode == option.mode,
                        onClick = {
                            draftMode = option.mode
                            applyLive(mode = option.mode)
                        },
                    )
                }
            }
        }

        ThemeSectionCard {
            ThemeSectionLabel(stringResource(Res.string.settings_theme_rd_accent_title))
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                accents.forEach { swatch ->
                    val selected = draftAccent == swatch.id
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(swatch.color, CircleShape)
                            .border(
                                width = if (selected) 2.5.dp else 1.dp,
                                color = if (selected) swatch.color else ThemeBorder,
                                shape = CircleShape,
                            )
                            .clickable {
                                draftAccent = swatch.id
                                applyLive(accentId = swatch.id)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (selected) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }
        }

        ThemeSectionCard {
            ThemeSectionLabel(stringResource(Res.string.settings_theme_rd_card_title))
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                cardOptions.forEach { option ->
                    CardStyleOption(
                        modifier = Modifier.weight(1f),
                        title = stringResource(option.titleRes),
                        description = stringResource(option.descRes),
                        style = option.style,
                        accent = accent,
                        selected = draftCard == option.style,
                        onClick = {
                            draftCard = option.style
                            applyLive(card = option.style)
                        },
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun ThemeSectionCard(content: @Composable () -> Unit) {
    val radius = appCardCornerRadius()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ThemeCard, RoundedCornerShape(radius))
            .border(1.dp, ThemeBorder, RoundedCornerShape(radius))
            .padding(horizontal = 14.dp, vertical = 14.dp),
    ) {
        content()
    }
}

@Composable
private fun ThemeSectionLabel(text: String) {
    Text(
        text.uppercase(),
        color = ThemeMuted,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp,
    )
}

@Composable
private fun ThemeModePreviewCard(
    modifier: Modifier,
    title: String,
    kind: ThemePreviewKind,
    accent: Color,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = modifier
            .background(ThemeTile, shape)
            .border(1.5.dp, if (selected) accent else ThemeBorder, shape)
            .clickable(onClick = onClick)
            .padding(8.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(88.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    when (kind) {
                        ThemePreviewKind.Dark -> Color(0xFF0A0F18)
                        ThemePreviewKind.Blue -> Color(0xFF0C1C33)
                        ThemePreviewKind.Light -> Color(0xFFE8EEF5)
                        ThemePreviewKind.System -> Color(0xFF1A2333)
                    },
                ),
        ) {
            MiniDashboardPreview(kind = kind, accent = accent)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(20.dp)
                    .background(if (selected) accent else Color.Transparent, CircleShape)
                    .border(
                        1.5.dp,
                        if (selected) accent else ThemeMuted.copy(alpha = 0.55f),
                        CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) {
                    Icon(Icons.Filled.Check, null, tint = ThemeInk, modifier = Modifier.size(12.dp))
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            title,
            color = if (selected) accent else ThemeText,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            lineHeight = 14.sp,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun MiniDashboardPreview(kind: ThemePreviewKind, accent: Color) {
    val panel = when (kind) {
        ThemePreviewKind.Dark -> Color(0xFF141B28)
        ThemePreviewKind.Blue -> Color(0xFF16304F)
        ThemePreviewKind.Light -> Color(0xFFFFFFFF)
        ThemePreviewKind.System -> Color(0xFF141B28)
    }
    val muted = when (kind) {
        ThemePreviewKind.Light -> Color(0xFF94A3B8)
        else -> Color(0xFF64748B)
    }
    Canvas(Modifier.fillMaxSize().padding(6.dp)) {
        val w = size.width
        val h = size.height
        if (kind == ThemePreviewKind.System) {
            drawRoundRect(Color(0xFF141B28), size = Size(w / 2f + 1f, h), cornerRadius = CornerRadius(8f, 8f))
            drawRoundRect(
                color = Color(0xFFFFFFFF),
                topLeft = Offset(w / 2f, 0f),
                size = Size(w / 2f, h),
                cornerRadius = CornerRadius(8f, 8f),
            )
        } else {
            drawRoundRect(panel, cornerRadius = CornerRadius(8f, 8f))
        }
        drawRoundRect(
            color = accent.copy(alpha = 0.85f),
            topLeft = Offset(w * 0.08f, h * 0.12f),
            size = Size(w * 0.28f, h * 0.08f),
            cornerRadius = CornerRadius(4f, 4f),
        )
        drawRoundRect(
            color = muted.copy(alpha = 0.45f),
            topLeft = Offset(w * 0.42f, h * 0.14f),
            size = Size(w * 0.22f, h * 0.05f),
            cornerRadius = CornerRadius(3f, 3f),
        )
        val tileW = w * 0.24f
        val tileH = h * 0.28f
        val y = h * 0.34f
        repeat(3) { i ->
            val x = w * 0.08f + i * (tileW + w * 0.06f)
            val tile = when {
                kind == ThemePreviewKind.Light -> Color(0xFFF1F5F9)
                kind == ThemePreviewKind.System && x + tileW / 2f >= w / 2f -> Color(0xFFF1F5F9)
                else -> Color(0xFF0B1220)
            }
            drawRoundRect(
                color = tile,
                topLeft = Offset(x, y),
                size = Size(tileW, tileH),
                cornerRadius = CornerRadius(6f, 6f),
            )
            drawCircle(
                color = accent,
                radius = 3.5f,
                center = Offset(x + tileW / 2f, y + tileH * 0.38f),
            )
        }
        drawRoundRect(
            color = muted.copy(alpha = 0.25f),
            topLeft = Offset(w * 0.08f, h * 0.78f),
            size = Size(w * 0.84f, h * 0.12f),
            cornerRadius = CornerRadius(6f, 6f),
        )
    }
}

@Composable
private fun CardStyleOption(
    modifier: Modifier,
    title: String,
    description: String,
    style: AppCardStyle,
    accent: Color,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = modifier
            .background(ThemeTile, shape)
            .border(1.5.dp, if (selected) accent else ThemeBorder, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(52.dp), contentAlignment = Alignment.Center) {
            CardFillPreview(style = style, accent = if (selected) accent else ThemeMuted)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(18.dp)
                    .background(if (selected) accent else Color.Transparent, CircleShape)
                    .border(
                        1.5.dp,
                        if (selected) accent else ThemeMuted.copy(alpha = 0.5f),
                        CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) {
                    Icon(Icons.Filled.Check, null, tint = ThemeInk, modifier = Modifier.size(11.dp))
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            title,
            color = if (selected) accent else ThemeText,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            description,
            color = ThemeMuted,
            fontSize = 10.sp,
            lineHeight = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun CardFillPreview(style: AppCardStyle, accent: Color) {
    Canvas(Modifier.size(72.dp, 44.dp)) {
        val barW = size.width * 0.14f
        val gap = size.width * 0.08f
        val startX = size.width * 0.16f
        val baseY = size.height * 0.82f
        repeat(4) { i ->
            val h = size.height * (0.30f + i * 0.12f)
            val topLeft = Offset(startX + i * (barW + gap), baseY - h)
            val barSize = Size(barW, h)
            val radius = CornerRadius(4f, 4f)
            when (style) {
                AppCardStyle.STANDARD -> {
                    drawRoundRect(color = accent.copy(alpha = 0.95f), topLeft = topLeft, size = barSize, cornerRadius = radius)
                }
                AppCardStyle.MEDIUM -> {
                    drawRoundRect(color = accent.copy(alpha = 0.40f), topLeft = topLeft, size = barSize, cornerRadius = radius)
                }
                AppCardStyle.SOFT -> {
                    drawRoundRect(
                        color = accent.copy(alpha = 0.85f),
                        topLeft = topLeft,
                        size = barSize,
                        cornerRadius = radius,
                        style = Stroke(width = 2.2f),
                    )
                }
            }
        }
    }
}

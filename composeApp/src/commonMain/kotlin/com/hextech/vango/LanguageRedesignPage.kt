package com.hextech.vango

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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Update
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hextech.vango.profile.LocalePreferenceApi
import com.hextech.vango.profile.LoginPreferences
import com.hextech.vango.profile.localePreferenceApiValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import testvango.composeapp.generated.resources.Res
import testvango.composeapp.generated.resources.settings_lang_rd_app_language
import testvango.composeapp.generated.resources.settings_lang_rd_current_language
import testvango.composeapp.generated.resources.settings_lang_rd_datetime_format
import testvango.composeapp.generated.resources.settings_lang_rd_default_badge
import testvango.composeapp.generated.resources.settings_lang_rd_hint
import testvango.composeapp.generated.resources.settings_lang_rd_info_body
import testvango.composeapp.generated.resources.settings_lang_rd_info_title
import testvango.composeapp.generated.resources.settings_lang_rd_last_update
import testvango.composeapp.generated.resources.settings_lang_rd_native_ar
import testvango.composeapp.generated.resources.settings_lang_rd_native_de
import testvango.composeapp.generated.resources.settings_lang_rd_native_en
import testvango.composeapp.generated.resources.settings_lang_rd_native_es
import testvango.composeapp.generated.resources.settings_lang_rd_native_fr
import testvango.composeapp.generated.resources.settings_lang_rd_native_ru
import testvango.composeapp.generated.resources.settings_lang_rd_never
import testvango.composeapp.generated.resources.settings_lang_rd_region
import testvango.composeapp.generated.resources.settings_lang_rd_region_en
import testvango.composeapp.generated.resources.settings_lang_rd_region_format
import testvango.composeapp.generated.resources.settings_lang_rd_region_tr
import testvango.composeapp.generated.resources.settings_lang_rd_time_24h
import testvango.composeapp.generated.resources.settings_lang_rd_title
import testvango.composeapp.generated.resources.settings_lang_rd_title_ar
import testvango.composeapp.generated.resources.settings_lang_rd_title_de
import testvango.composeapp.generated.resources.settings_lang_rd_title_en
import testvango.composeapp.generated.resources.settings_lang_rd_title_es
import testvango.composeapp.generated.resources.settings_lang_rd_title_fr
import testvango.composeapp.generated.resources.settings_lang_rd_title_ru
import testvango.composeapp.generated.resources.settings_lang_turkish

private val LangText: Color @Composable get() = LocalAppChrome.current.text
private val LangMuted: Color @Composable get() = LocalAppChrome.current.muted
private val LangBorder: Color @Composable get() = LocalAppChrome.current.border
private val LangTile: Color @Composable get() = LocalAppChrome.current.tile
private val LangCard: Color @Composable get() = LocalAppChrome.current.card
private val LangInk: Color @Composable get() = LocalAppChrome.current.ink
private val LangInfoBg: Color @Composable get() = LocalAppChrome.current.infoBg

private data class LangOption(
    val tag: String,
    val titleRes: StringResource,
    val subtitleRes: StringResource,
    val flag: String,
    val enabled: Boolean,
)

@Composable
fun LanguageRedesignPage() {
    val scope = rememberCoroutineScope()
    val accent = accentColorFromId(customAppAccentId)
    var draftTag by remember { mutableStateOf(languageTagForSelection(customAppLocale)) }
    var lastUpdateMs by remember { mutableStateOf(LocalePreferences.getLanguageUpdatedAtEpochMs()) }

    LaunchedEffect(customAppLocale) {
        draftTag = languageTagForSelection(customAppLocale)
        lastUpdateMs = LocalePreferences.getLanguageUpdatedAtEpochMs()
    }

    val options = remember {
        listOf(
            LangOption("tr", Res.string.settings_lang_turkish, Res.string.settings_lang_rd_default_badge, "🇹🇷", true),
            LangOption("en", Res.string.settings_lang_rd_title_en, Res.string.settings_lang_rd_native_en, "🇬🇧", true),
            LangOption("de", Res.string.settings_lang_rd_title_de, Res.string.settings_lang_rd_native_de, "🇩🇪", false),
            LangOption("fr", Res.string.settings_lang_rd_title_fr, Res.string.settings_lang_rd_native_fr, "🇫🇷", false),
            LangOption("es", Res.string.settings_lang_rd_title_es, Res.string.settings_lang_rd_native_es, "🇪🇸", false),
            LangOption("ru", Res.string.settings_lang_rd_title_ru, Res.string.settings_lang_rd_native_ru, "🇷🇺", false),
            LangOption("ar", Res.string.settings_lang_rd_title_ar, Res.string.settings_lang_rd_native_ar, "🇸🇦", false),
        )
    }

    val activeName = when (languageTagForSelection(customAppLocale)) {
        "en" -> stringResource(Res.string.settings_lang_rd_title_en)
        else -> stringResource(Res.string.settings_lang_turkish)
    }
    val draftIsEn = draftTag == "en"
    val regionValue = stringResource(
        if (draftIsEn) Res.string.settings_lang_rd_region_en else Res.string.settings_lang_rd_region_tr,
    )
    val timeValue = stringResource(Res.string.settings_lang_rd_time_24h)
    val lastUpdateText = formatLanguageUpdatedAt(lastUpdateMs, draftTag)
        ?: stringResource(Res.string.settings_lang_rd_never)

    fun selectLanguage(tag: String) {
        draftTag = tag
        val nextTag = languageTagForSave(tag)
        applyAppLanguage(nextTag)
        lastUpdateMs = LocalePreferences.getLanguageUpdatedAtEpochMs()
        scope.launch {
            val token = LoginPreferences.getAuthToken()?.trim().orEmpty()
            if (token.isNotEmpty()) {
                withContext(Dispatchers.Default) {
                    LocalePreferenceApi.update(token, localePreferenceApiValue(nextTag))
                }
            }
        }
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
                Icon(Icons.Filled.Public, null, tint = accent, modifier = Modifier.size(32.dp))
                Text(
                    stringResource(Res.string.settings_lang_rd_title).uppercase(),
                    color = LangText,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                )
            }
            Text(
                stringResource(Res.string.settings_lang_rd_hint),
                color = LangMuted,
                fontSize = 13.sp,
                lineHeight = 19.sp,
            )
        }

        LangSectionCard {
            LangSectionLabel(stringResource(Res.string.settings_lang_rd_app_language))
            Spacer(Modifier.height(12.dp))
            options.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    row.forEach { option ->
                        LanguageChoiceCard(
                            modifier = Modifier.weight(1f),
                            option = option,
                            accent = accent,
                            selected = draftTag == option.tag,
                            onClick = { if (option.enabled) selectLanguage(option.tag) },
                        )
                    }
                    if (row.size == 1) {
                        Spacer(Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LangInfoBg.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                    .border(1.dp, accent.copy(alpha = 0.28f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(Icons.Filled.Info, null, tint = accent, modifier = Modifier.size(18.dp))
                Text(
                    stringResource(Res.string.settings_lang_rd_info_body),
                    color = LangText.copy(alpha = 0.88f),
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                )
            }
        }

        LangSectionCard {
            LangSectionLabel(stringResource(Res.string.settings_lang_rd_info_title))
            Spacer(Modifier.height(4.dp))
            LangInfoRow(
                icon = Icons.Filled.Translate,
                label = stringResource(Res.string.settings_lang_rd_current_language),
                value = activeName,
                valueColor = accent,
            )
            LangInfoRow(
                icon = Icons.Filled.Update,
                label = stringResource(Res.string.settings_lang_rd_last_update),
                value = lastUpdateText,
            )
        }

        LangSectionCard {
            LangSectionLabel(stringResource(Res.string.settings_lang_rd_region_format))
            Spacer(Modifier.height(4.dp))
            LangFormatRow(
                icon = Icons.Filled.Public,
                label = stringResource(Res.string.settings_lang_rd_region),
                value = regionValue,
                accent = accent,
                valueAccent = true,
            )
            LangFormatRow(
                icon = Icons.Filled.Schedule,
                label = stringResource(Res.string.settings_lang_rd_datetime_format),
                value = timeValue,
                accent = accent,
                valueAccent = false,
            )
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun LanguageChoiceCard(
    modifier: Modifier,
    option: LangOption,
    accent: Color,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    val enabled = option.enabled
    Row(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.72f)
            .background(LangTile, shape)
            .border(1.5.dp, if (selected && enabled) accent else LangBorder, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(option.flag, fontSize = 24.sp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                stringResource(option.titleRes),
                color = if (selected && enabled) accent else LangText,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                stringResource(option.subtitleRes),
                color = if (selected && enabled) accent else LangMuted,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(
            modifier = Modifier
                .size(22.dp)
                .background(if (selected && enabled) accent else Color.Transparent, CircleShape)
                .border(
                    1.5.dp,
                    if (selected && enabled) accent else LangMuted.copy(alpha = 0.45f),
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (selected && enabled) {
                Icon(Icons.Filled.Check, null, tint = LangInk, modifier = Modifier.size(13.dp))
            }
        }
    }
}

@Composable
private fun LangSectionCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val radius = appCardCornerRadius()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(LangCard, RoundedCornerShape(radius))
            .border(1.dp, LangBorder, RoundedCornerShape(radius))
            .padding(horizontal = 14.dp, vertical = 14.dp),
    ) {
        content()
    }
}

@Composable
private fun LangSectionLabel(text: String) {
    Text(
        text.uppercase(),
        color = LangMuted,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp,
    )
}

@Composable
private fun LangInfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    valueColor: Color = LangText,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(icon, null, tint = LangMuted, modifier = Modifier.size(18.dp))
        Text(label, color = LangMuted, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(value, color = valueColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun LangFormatRow(
    icon: ImageVector,
    label: String,
    value: String,
    accent: Color,
    valueAccent: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(icon, null, tint = accent, modifier = Modifier.size(18.dp))
        Text(label, color = LangMuted, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(
            value,
            color = if (valueAccent) accent else LangText,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            null,
            tint = LangMuted.copy(alpha = 0.7f),
            modifier = Modifier.size(20.dp),
        )
    }
}

private fun formatLanguageUpdatedAt(epochMs: Long, tag: String): String? {
    if (epochMs <= 0L) return null
    val local = Instant.fromEpochMilliseconds(epochMs)
        .toLocalDateTime(TimeZone.currentSystemDefault())
    val d = local.dayOfMonth
    val y = local.year
    val hh = local.hour.toString().padStart(2, '0')
    val mm = local.minute.toString().padStart(2, '0')
    return if (tag == "en") {
        val m = local.monthNumber.toString().padStart(2, '0')
        val day = d.toString().padStart(2, '0')
        "$day.$m.$y $hh:$mm"
    } else {
        val month = turkishMonthName(local.monthNumber)
        "$d $month $y $hh:$mm"
    }
}

private fun turkishMonthName(month: Int): String = when (month) {
    1 -> "Ocak"
    2 -> "Şubat"
    3 -> "Mart"
    4 -> "Nisan"
    5 -> "Mayıs"
    6 -> "Haziran"
    7 -> "Temmuz"
    8 -> "Ağustos"
    9 -> "Eylül"
    10 -> "Ekim"
    11 -> "Kasım"
    else -> "Aralık"
}

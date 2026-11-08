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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButton
import androidx.compose.material.RadioButtonDefaults
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Translate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import testvango.composeapp.generated.resources.Res
import testvango.composeapp.generated.resources.settings_lang_arabic
import testvango.composeapp.generated.resources.settings_lang_chinese
import testvango.composeapp.generated.resources.settings_lang_coming_soon
import testvango.composeapp.generated.resources.settings_lang_deutsch
import testvango.composeapp.generated.resources.settings_lang_english
import testvango.composeapp.generated.resources.settings_lang_french
import testvango.composeapp.generated.resources.settings_lang_russian
import testvango.composeapp.generated.resources.settings_lang_spanish
import testvango.composeapp.generated.resources.settings_lang_turkish
import testvango.composeapp.generated.resources.settings_lang_turkish_default
import testvango.composeapp.generated.resources.settings_language_active
import testvango.composeapp.generated.resources.settings_language_current
import testvango.composeapp.generated.resources.settings_language_info_body
import testvango.composeapp.generated.resources.settings_language_info_title
import testvango.composeapp.generated.resources.settings_language_options_title
import testvango.composeapp.generated.resources.settings_language_page_hint
import testvango.composeapp.generated.resources.settings_language_page_title
import testvango.composeapp.generated.resources.settings_language_save

private val LangGreen = Color(0xFF22C55E)
private val LangGreenSoft = Color(0xFFDCFCE7)
private val LangBlueSoft = Color(0xFFE8F4FD)

private data class LanguageListItem(
    val tag: String,
    val nativeNameRes: StringResource,
    val subtitleRes: StringResource,
    val flagEmoji: String,
    val enabled: Boolean,
)

@Composable
fun LanguageSettingScreen(
    activeTag: String?,
    selectedTag: String,
    onSelectedTagChange: (String) -> Unit,
    onBack: () -> Unit,
    onSave: () -> Unit,
    saving: Boolean,
) {
  val options = rememberLanguageOptions()
  val activeDisplayName = languageDisplayName(activeTag ?: selectedTag)

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp, vertical = 8.dp),
  ) {
    LanguageSettingsHeader(
      backLabel = settingsBackLabelFor(SettingsPage.Language),
      pageTitle = settingsHeaderTitleFor(SettingsPage.Language),
      onBack = onBack,
      onSave = onSave,
      saving = saving,
    )
    Text(
      text = stringResource(Res.string.settings_language_page_hint),
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
      LanguageCurrentCard(activeDisplayName = activeDisplayName)
      LanguageOptionsCard(
        options = options,
        selectedTag = selectedTag,
        onSelectedTagChange = onSelectedTagChange,
      )
      LanguageInfoBanner()
      Spacer(Modifier.height(8.dp))
    }
  }
}

@Composable
private fun rememberLanguageOptions(): List<LanguageListItem> = remember {
  listOf(
    LanguageListItem("tr", Res.string.settings_lang_turkish, Res.string.settings_lang_turkish_default, "🇹🇷", true),
    LanguageListItem("en", Res.string.settings_lang_english, Res.string.settings_lang_english, "🇬🇧", true),
    LanguageListItem("de", Res.string.settings_lang_deutsch, Res.string.settings_lang_deutsch, "🇩🇪", false),
    LanguageListItem("ru", Res.string.settings_lang_russian, Res.string.settings_lang_russian, "🇷🇺", false),
    LanguageListItem("es", Res.string.settings_lang_spanish, Res.string.settings_lang_spanish, "🇪🇸", false),
    LanguageListItem("fr", Res.string.settings_lang_french, Res.string.settings_lang_french, "🇫🇷", false),
    LanguageListItem("ar", Res.string.settings_lang_arabic, Res.string.settings_lang_arabic, "🇸🇦", false),
    LanguageListItem("zh", Res.string.settings_lang_chinese, Res.string.settings_lang_chinese, "🇨🇳", false),
  )
}

@Composable
private fun languageDisplayName(tag: String): String = when (tag) {
  "en" -> stringResource(Res.string.settings_lang_english)
  else -> stringResource(Res.string.settings_lang_turkish)
}

@Composable
private fun LanguageSettingsHeader(
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
        stringResource(Res.string.settings_language_save),
        color = MaterialTheme.colors.onPrimary,
        fontSize = 13.sp,
      )
    }
    },
  )
}

@Composable
private fun LanguageCurrentCard(activeDisplayName: String) {
  Card(
    shape = RoundedCornerShape(16.dp),
    elevation = 4.dp,
    modifier = Modifier.fillMaxWidth(),
  ) {
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
          .background(MaterialTheme.colors.primary.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center,
      ) {
        Icon(Icons.Filled.Public, null, tint = MaterialTheme.colors.primary, modifier = Modifier.size(26.dp))
      }
      Spacer(Modifier.width(12.dp))
      Column(Modifier.weight(1f)) {
        Text(
          stringResource(Res.string.settings_language_current),
          style = MaterialTheme.typography.body2,
          color = MaterialTheme.colors.onSurface.copy(0.65f),
        )
        Text(
          activeDisplayName,
          style = MaterialTheme.typography.h6,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colors.primary,
        )
        Spacer(Modifier.height(6.dp))
        Row(
          modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(LangGreenSoft)
            .padding(horizontal = 10.dp, vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(Icons.Filled.Check, null, tint = LangGreen, modifier = Modifier.size(14.dp))
          Spacer(Modifier.width(4.dp))
          Text(
            stringResource(Res.string.settings_language_active),
            style = MaterialTheme.typography.caption,
            color = LangGreen,
            fontWeight = FontWeight.Medium,
          )
        }
      }
      Box(
        modifier = Modifier
          .size(56.dp)
          .clip(CircleShape)
          .background(LangBlueSoft),
        contentAlignment = Alignment.Center,
      ) {
        Icon(Icons.Filled.Translate, null, tint = MaterialTheme.colors.primary.copy(0.7f), modifier = Modifier.size(30.dp))
      }
    }
  }
}

@Composable
private fun LanguageOptionsCard(
  options: List<LanguageListItem>,
  selectedTag: String,
  onSelectedTagChange: (String) -> Unit,
) {
  Card(
    shape = RoundedCornerShape(16.dp),
    elevation = 4.dp,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(Modifier.padding(vertical = 8.dp)) {
      Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(Icons.Filled.Public, null, tint = MaterialTheme.colors.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(
          stringResource(Res.string.settings_language_options_title),
          style = MaterialTheme.typography.subtitle1,
          fontWeight = FontWeight.SemiBold,
        )
      }
      options.forEachIndexed { index, item ->
        LanguageOptionRow(
          item = item,
          selected = selectedTag == item.tag,
          onSelect = {
            if (item.enabled) onSelectedTagChange(item.tag)
          },
        )
        if (index < options.lastIndex) {
          Divider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colors.onSurface.copy(0.08f),
          )
        }
      }
    }
  }
}

@Composable
private fun LanguageOptionRow(
  item: LanguageListItem,
  selected: Boolean,
  onSelect: () -> Unit,
) {
  val subtitle = if (item.enabled) {
    stringResource(item.subtitleRes)
  } else {
    stringResource(Res.string.settings_lang_coming_soon)
  }
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .alpha(if (item.enabled) 1f else 0.55f)
      .clickable(enabled = item.enabled, onClick = onSelect)
      .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(item.flagEmoji, fontSize = 26.sp, modifier = Modifier.width(40.dp))
    Column(Modifier.weight(1f)) {
      Text(
        stringResource(item.nativeNameRes),
        style = MaterialTheme.typography.body1,
        fontWeight = FontWeight.Medium,
      )
      Text(
        subtitle,
        style = MaterialTheme.typography.caption,
        color = MaterialTheme.colors.onSurface.copy(0.6f),
      )
    }
    RadioButton(
      selected = selected,
      onClick = onSelect,
      enabled = item.enabled,
      colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colors.primary),
    )
  }
}

@Composable
private fun LanguageInfoBanner() {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .background(LangBlueSoft)
      .border(1.dp, MaterialTheme.colors.primary.copy(0.15f), RoundedCornerShape(12.dp))
      .padding(14.dp),
    horizontalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Icon(Icons.Filled.Info, null, tint = MaterialTheme.colors.primary, modifier = Modifier.size(22.dp))
    Column {
      Text(
        stringResource(Res.string.settings_language_info_title),
        style = MaterialTheme.typography.subtitle2,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colors.primary,
      )
      Spacer(Modifier.height(4.dp))
      Text(
        stringResource(Res.string.settings_language_info_body),
        style = MaterialTheme.typography.caption,
        color = MaterialTheme.colors.onSurface.copy(0.8f),
      )
    }
  }
}

/** Seçim etiketini kayıtlı locale etiketine dönüştürür (yalnızca tr/en). */
fun languageTagForSave(selectedTag: String): String? = when (selectedTag) {
  "en" -> "en"
  "tr" -> "tr"
  else -> "tr"
}

/** Kayıtlı etiketi liste seçimine dönüştürür. */
fun languageTagForSelection(savedTag: String?): String = when (savedTag?.trim()?.lowercase()) {
  "en" -> "en"
  else -> "tr"
}

package com.hextech.vango

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Card
import androidx.compose.material.AlertDialog
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButton
import androidx.compose.material.RadioButtonDefaults
import androidx.compose.material.Switch
import androidx.compose.material.TextButton
import androidx.compose.material.Text
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Divider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Translate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.juul.kable.Scanner
import com.juul.kable.Advertisement
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.jetbrains.compose.resources.stringResource
import testvango.composeapp.generated.resources.Res
import testvango.composeapp.generated.resources.connection_ble_mac
import testvango.composeapp.generated.resources.connection_ble_mac_label
import testvango.composeapp.generated.resources.connection_device_not_found
import testvango.composeapp.generated.resources.connection_disconnect
import testvango.composeapp.generated.resources.connection_mac
import testvango.composeapp.generated.resources.connection_wifi_mac_label
import testvango.composeapp.generated.resources.connection_search_button
import testvango.composeapp.generated.resources.connection_search_hint
import testvango.composeapp.generated.resources.connection_searching
import testvango.composeapp.generated.resources.dialog_discovering
import testvango.composeapp.generated.resources.footer_settings
import testvango.composeapp.generated.resources.panel_sensor_clean_water
import testvango.composeapp.generated.resources.panel_sensor_dirty_water
import testvango.composeapp.generated.resources.panel_sensor_ex_temp
import testvango.composeapp.generated.resources.panel_sensor_gray_water
import testvango.composeapp.generated.resources.panel_sensor_in_temp
import testvango.composeapp.generated.resources.panel_sensor_rgb
import testvango.composeapp.generated.resources.settings_connection_desc
import testvango.composeapp.generated.resources.settings_connection_title
import testvango.composeapp.generated.resources.settings_lang_english
import testvango.composeapp.generated.resources.settings_lang_system
import testvango.composeapp.generated.resources.settings_lang_turkish
import testvango.composeapp.generated.resources.settings_language_menu_desc
import testvango.composeapp.generated.resources.settings_language_menu_title
import testvango.composeapp.generated.resources.settings_language_page_hint
import testvango.composeapp.generated.resources.settings_language_page_title
import testvango.composeapp.generated.resources.settings_language_save
import testvango.composeapp.generated.resources.settings_panel_desc
import testvango.composeapp.generated.resources.settings_panel_title
import testvango.composeapp.generated.resources.settings_theme_dark
import testvango.composeapp.generated.resources.settings_theme_light
import testvango.composeapp.generated.resources.settings_theme_menu_desc
import testvango.composeapp.generated.resources.settings_theme_menu_title
import testvango.composeapp.generated.resources.settings_theme_page_hint
import testvango.composeapp.generated.resources.settings_theme_page_title
import testvango.composeapp.generated.resources.settings_theme_save
import testvango.composeapp.generated.resources.settings_theme_system
import testvango.composeapp.generated.resources.settings_rgb_presets_menu_desc
import testvango.composeapp.generated.resources.settings_rgb_presets_menu_title
import testvango.composeapp.generated.resources.profile_title
import testvango.composeapp.generated.resources.settings_about_copyright
import testvango.composeapp.generated.resources.settings_about_tagline
import testvango.composeapp.generated.resources.settings_about_title
import testvango.composeapp.generated.resources.settings_about_version_label
import testvango.composeapp.generated.resources.settings_exit_title
import testvango.composeapp.generated.resources.exit_confirm_message
import testvango.composeapp.generated.resources.exit_confirm_no
import testvango.composeapp.generated.resources.exit_confirm_title
import testvango.composeapp.generated.resources.exit_confirm_yes
import com.hextech.vango.profile.DeviceUidLookupApi
import com.hextech.vango.profile.LocalePreferenceApi
import com.hextech.vango.profile.LoginPreferences
import com.hextech.vango.profile.ThemePreferenceApi
import com.hextech.vango.profile.localePreferenceApiValue
import com.hextech.vango.profile.themePreferenceApiValue
import com.hextech.vango.profile.persistDeviceLookupCache
import com.hextech.vango.profile.readCachedDeviceServerInfo
import com.hextech.vango.profile.DeviceServerInfo
import com.hextech.vango.profile.ProfileGuestPreferences
import com.hextech.vango.profile.ProfileLoginScreen
import com.hextech.vango.profile.RegisterRequestScreen
import com.hextech.vango.profile.ProfileScreen
import com.hextech.vango.profile.ProfileViewModel
import com.hextech.vango.profile.rememberProfileViewModel
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.unit.sp
/** Uygulama surumu — composeApp/build.gradle.kts versionName ile senkron tutun. */
const val VANGO_APP_VERSION_NAME = "0.90-001"

enum class SettingsPage {
    Main,
    Language,
    Theme,
    Panel,
    RgbPresets,
    Climate,
    Connection,
    BlePair,
    ManualDeviceAdd,
    Remote,
    Profile,
    About,
    Debug
}

internal fun backToMenuGrid() {
    uiState.value = uiState.value.copy(
        selectedTab = SelectedPage.Menu,
        settingsPage = SettingsPage.Main,
    )
}

internal fun navigateToMenuPage(page: SettingsPage) {
    uiState.value = uiState.value.copy(
        selectedTab = SelectedPage.Menu,
        settingsPage = page,
    )
}

internal fun openRemoteConnectionPage() {
    val token = LoginPreferences.getAuthToken()?.trim().orEmpty()
    if (token.isEmpty()) {
        pendingRemoteConnectAfterLogin = true
        forceProfileLoginFromConnection = true
        navigateToProfileTab()
    } else {
        remoteSessionError.value = null
        navigateToMenuPage(SettingsPage.Remote)
    }
}

private fun navigateToProfileTab() {
    uiState.value = uiState.value.copy(
        selectedTab = SelectedPage.Profile,
        settingsPage = SettingsPage.Profile,
    )
}

private enum class LanguageChoice {
    System,
    Turkish,
    English
}

private fun languageChoiceFromTag(tag: String?): LanguageChoice = when (tag) {
    null -> LanguageChoice.System
    "tr" -> LanguageChoice.Turkish
    "en" -> LanguageChoice.English
    else -> LanguageChoice.System
}

private fun tagFromLanguageChoice(choice: LanguageChoice): String? = when (choice) {
    LanguageChoice.System -> null
    LanguageChoice.Turkish -> "tr"
    LanguageChoice.English -> "en"
}

private var forceProfileLoginFromConnection by mutableStateOf(false)
private var pendingRemoteConnectAfterLogin by mutableStateOf(false)

private enum class ProfileAuthPage {
    Login,
    Register,
}

private var profileAuthPage by mutableStateOf(ProfileAuthPage.Login)

/**
 * Legacy settings UI (grid + subpages). Kept intact for phone / fallback.
 * Tablet uses [SettingsRedesignScreen] instead — do not delete this flow yet.
 */
@Composable
fun Settings() {
    val data by uiState.collectAsState()
    val panelSession by panelSettingsSession.collectAsState()
    val profileViewModel = rememberProfileViewModel()
    val profileState by profileViewModel.state.collectAsState()
    var showProfileLogin by remember { mutableStateOf(false) }
    var previousSettingsPage by remember { mutableStateOf(data.settingsPage) }
    LaunchedEffect(profileState.isLoggedIn, pendingRemoteConnectAfterLogin) {
        if (pendingRemoteConnectAfterLogin && profileState.isLoggedIn) {
            pendingRemoteConnectAfterLogin = false
            remoteSessionError.value = null
            uiState.value = data.copy(settingsPage = SettingsPage.Remote)
        }
    }
    LaunchedEffect(data.settingsPage) {
        val prev = previousSettingsPage
        if (data.settingsPage == SettingsPage.Panel) {
            beginPanelSettingsSessionIfNeeded()
        } else {
            closePanelSettingsSession()
        }
        if (data.settingsPage != SettingsPage.Profile) {
            profileAuthPage = ProfileAuthPage.Login
        }
        if (data.settingsPage == SettingsPage.Profile) {
            ProfileGuestPreferences.setAllowDirectProfile(false)
            if (forceProfileLoginFromConnection) {
                profileAuthPage = ProfileAuthPage.Login
                showProfileLogin = true
                forceProfileLoginFromConnection = false
            }
            if (prev == SettingsPage.Main) {
                val loggedIn = profileViewModel.state.value.isLoggedIn
                showProfileLogin = !loggedIn
                if (showProfileLogin) profileAuthPage = ProfileAuthPage.Login
            }
        }
        previousSettingsPage = data.settingsPage
    }
    if (panelSession.showSavedPopup) {
        LaunchedEffect(panelSession.showSavedPopup) {
            delay(1700)
            dismissPanelSavedPopup()
        }
    }
    LaunchedEffect(data.selectedTab) {
        if (data.selectedTab == SelectedPage.Profile && data.settingsPage != SettingsPage.Profile) {
            uiState.value = data.copy(settingsPage = SettingsPage.Profile)
        }
    }

    Box(Modifier.fillMaxSize()) {
        when {
            data.settingsPage == SettingsPage.Profile -> {
                ProfileSetting(
                    viewModel = profileViewModel,
                    showProfileLogin = showProfileLogin,
                    onShowProfileLoginChange = { showProfileLogin = it },
                )
            }
            data.settingsPage == SettingsPage.Main -> {
                MainSetting(
                    onClickConnection = { navigateToMenuPage(SettingsPage.Connection) },
                    onClickPanel = { navigateToMenuPage(SettingsPage.Panel) },
                    onClickTheme = { navigateToMenuPage(SettingsPage.Theme) },
                    onClickLanguage = { navigateToMenuPage(SettingsPage.Language) },
                    onClickRgbPresets = { navigateToMenuPage(SettingsPage.RgbPresets) },
                    onClickClimate = { navigateToMenuPage(SettingsPage.Climate) },
                    onClickAbout = { navigateToMenuPage(SettingsPage.About) },
                    onClickProfile = { navigateToMenuPage(SettingsPage.Profile) },
                )
            }
            data.settingsPage == SettingsPage.Language -> LanguageSetting()
            data.settingsPage == SettingsPage.Theme -> ThemeSetting()
            data.settingsPage == SettingsPage.Panel -> PanelSetting()
            data.settingsPage == SettingsPage.RgbPresets -> RgbPresetsSetting()
            data.settingsPage == SettingsPage.Climate -> ClimateSetting()
            data.settingsPage == SettingsPage.Connection -> ConnectionSetting()
            data.settingsPage == SettingsPage.BlePair -> BleDevicePairSetting()
            data.settingsPage == SettingsPage.ManualDeviceAdd -> ManualDeviceAddSetting()
            data.settingsPage == SettingsPage.Remote -> RemoteConnectionSetting()
            data.settingsPage == SettingsPage.About -> AboutSetting()
            data.settingsPage == SettingsPage.Debug -> ScrollableContent { }
        }
        if (panelSession.showSavedPopup) {
            AlertDialog(
                onDismissRequest = { dismissPanelSavedPopup() },
                title = { Text(panelSession.savedMessage) },
                text = panelSession.syncError?.let { msg -> { Text(msg) } },
                confirmButton = {
                    TextButton(onClick = { dismissPanelSavedPopup() }) {
                        Text("Tamam")
                    }
                }
            )
        }
        if (panelSession.showDiscardPopup) {
            AlertDialog(
                onDismissRequest = { dismissPanelDiscardPopup() },
                title = { Text("YaptÄ±ÄŸÄ±nÄ±z deÄŸiÅŸiklikler kaydedilmeyecek") },
                confirmButton = {
                    TextButton(onClick = { confirmPanelDiscardAndExit() }) {
                        Text("Geri dÃ¶n")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { dismissPanelDiscardPopup() }) {
                        Text("VazgeÃ§")
                    }
                }
            )
        }
    }
}

@Composable
fun ClickableSettingColon(
    label:String,
    width: Float,
    onClick:()->Unit
)
{
    Column(
        modifier = Modifier
            .fillMaxWidth(width)
            .fillMaxHeight(.5f),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .background(Color.Gray.copy(alpha = .5f))
                .fillMaxSize(.7f)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Text(label, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun ProfileSetting(
    viewModel: ProfileViewModel,
    showProfileLogin: Boolean,
    onShowProfileLoginChange: (Boolean) -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        if (showProfileLogin) {
            when (profileAuthPage) {
                ProfileAuthPage.Login -> ProfileLoginScreen(
                    viewModel = viewModel,
                    onBack = { backToMenuGrid() },
                    onNavigateToRegister = { profileAuthPage = ProfileAuthPage.Register },
                    onLoginSuccess = { onShowProfileLoginChange(false) },
                )
                ProfileAuthPage.Register -> RegisterRequestScreen(
                    viewModel = viewModel,
                    onBack = { profileAuthPage = ProfileAuthPage.Login },
                )
            }
        } else {
            ProfileScreen(
                viewModel = viewModel,
                modifier = Modifier.fillMaxSize(),
                showTopBarBack = true,
                onBack = { backToMenuGrid() },
                onNavigateToDevicePairing = {
                    navigateToMenuPage(SettingsPage.Connection)
                },
                onRequestLogin = {
                    ProfileGuestPreferences.setAllowDirectProfile(false)
                    onShowProfileLoginChange(true)
                },
            )
        }
    }
}

@Composable
fun AboutSetting() = AboutHelpSetting()

@Composable
fun SettingListItem(
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        backgroundColor = MaterialTheme.colors.surface,
        shape = RoundedCornerShape(12.dp),
        elevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.h6,
                    color = MaterialTheme.colors.onSurface,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )
            }
            Text(
                text = "â†’",
                style = MaterialTheme.typography.h6,
                color = MaterialTheme.colors.primary,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Composable
fun SettingsBackHeader(
    backLabel: String,
    pageTitle: String,
    onBack: () -> Unit,
    subtitle: String? = null,
    titleColor: Color = MaterialTheme.colors.onSurface,
    subtitleColor: Color = MaterialTheme.colors.onSurface.copy(alpha = 0.65f),
    trailing: @Composable (() -> Unit)? = null,
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
                .padding(horizontal = 108.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = pageTitle,
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.SemiBold,
                color = titleColor,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.caption,
                    color = subtitleColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp),
                    lineHeight = 16.sp,
                )
            }
        }
        Row(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .clickable(onClick = onBack),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = null,
                tint = MaterialTheme.colors.primary,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = backLabel,
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colors.onSurface,
                maxLines = 1,
            )
        }
        if (trailing != null) {
            Box(Modifier.align(Alignment.CenterEnd)) {
                trailing()
            }
        }
    }
}

@Composable
fun LanguageSetting() {
    val data by uiState.collectAsState()
    val scope = rememberCoroutineScope()
    var selectedTag by remember(data.settingsPage, customAppLocale) {
        mutableStateOf(languageTagForSelection(customAppLocale))
    }
    var saving by remember { mutableStateOf(false) }

    LanguageSettingScreen(
        activeTag = customAppLocale,
        selectedTag = selectedTag,
        onSelectedTagChange = { selectedTag = it },
        onBack = { backToMenuGrid() },
        onSave = {
            val nextTag = languageTagForSave(selectedTag)
            saving = true
            scope.launch {
                val token = LoginPreferences.getAuthToken()?.trim().orEmpty()
                var syncOk = true
                if (token.isNotEmpty()) {
                    syncOk = withContext(Dispatchers.Default) {
                        val target = localePreferenceApiValue(nextTag)
                        var ok = false
                        repeat(2) { attempt ->
                            val result = LocalePreferenceApi.update(token, target)
                            if (result.isSuccess) {
                                ok = true
                                return@repeat
                            }
                            println("Locale sync attempt ${attempt + 1} failed: ${result.exceptionOrNull()?.message}")
                            kotlinx.coroutines.delay(250)
                        }
                        ok
                    }
                }
                withContext(Dispatchers.Main) {
                    applyAppLanguage(nextTag)
                    saving = false
                    if (!syncOk) {
                        println("Locale preference applied locally, but platform sync failed.")
                    }
                    backToMenuGrid()
                }
            }
        },
        saving = saving,
    )
}

@Composable
fun ThemeSetting() {
    val data by uiState.collectAsState()
    val scope = rememberCoroutineScope()
    var selectedId by remember(data.settingsPage, customAppThemeMode) {
        mutableStateOf(themeSelectionFromMode(customAppThemeMode))
    }
    var selectedAccentId by remember(data.settingsPage, customAppAccentId) {
        mutableStateOf(customAppAccentId)
    }
    var saving by remember { mutableStateOf(false) }

    ThemeSettingScreen(
        activeMode = customAppThemeMode,
        selectedId = selectedId,
        onSelectedIdChange = {
            selectedId = it
            applyAppThemeMode(modeFromThemeSelection(it))
        },
        selectedAccentId = selectedAccentId,
        onSelectedAccentIdChange = {
            selectedAccentId = it
            applyAppAccent(it)
        },
        onBack = { backToMenuGrid() },
        onSave = {
            val mode = modeFromThemeSelection(selectedId)
            saving = true
            scope.launch {
                val token = LoginPreferences.getAuthToken()?.trim().orEmpty()
                var syncOk = true
                if (token.isNotEmpty()) {
                    syncOk = withContext(Dispatchers.Default) {
                        val target = themePreferenceApiValue(mode)
                        var ok = false
                        repeat(2) { attempt ->
                            val result = ThemePreferenceApi.update(
                                authToken = token,
                                themePreference = target,
                                themeAccentId = selectedAccentId,
                            )
                            if (result.isSuccess) {
                                ok = true
                                return@repeat
                            }
                            println("Theme sync attempt ${attempt + 1} failed: ${result.exceptionOrNull()?.message}")
                            kotlinx.coroutines.delay(250)
                        }
                        ok
                    }
                }
                withContext(Dispatchers.Main) {
                    applyAppAccent(selectedAccentId)
                    applyAppThemeMode(mode)
                    saving = false
                    if (!syncOk) {
                        println("Theme preference applied locally, but platform sync failed.")
                    }
                    backToMenuGrid()
                }
            }
        },
        saving = saving,
    )
}

/**
 * Panel settings composable for device configuration
 * Allows users to configure:
 * - Vertical sliders for dimmer controls (0-3)
 * - Sensor enable/disable states (temperature, water levels)
 * - RGB lighting configuration
 * - I/O panel setup with digital ports
 */
@Composable
fun PanelSetting() {
    val ui by uiState.collectAsState()
    val panelSession by panelSettingsSession.collectAsState()
    val profileState by devicePanelProfile.collectAsState()
    val caps = profileState.caps
    val cfg = ui.devConfig
    val relaySlots = caps.relays.coerceIn(1, FIRMWARE_RELAY_BUFFER_MAX)
    val valueArray = ByteArray(relaySlots) { 0 }
    val idArray = ByteArray(relaySlots) { 0 }

    vanGoData.value.outDB.copyInto(valueArray, endIndex = minOf(valueArray.size, vanGoData.value.outDB.size))
    val srcNames = cfg.OutputsNameBuffer
    for (i in idArray.indices) {
        idArray[i] = srcNames.getOrElse(i) { 0 }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        SettingsBackHeader(
            backLabel = settingsBackLabelFor(SettingsPage.Panel),
            pageTitle = settingsHeaderTitleFor(SettingsPage.Panel),
            onBack = { requestPanelDiscardPopup() },
            trailing = {
                TextButton(
                    onClick = { savePanelSettings() },
                    enabled = panelSession.hasChanges && !panelSession.isSaving && !panelSession.isSyncing
                ) {
                    Text(
                        text = "KAYDET",
                        fontWeight = FontWeight.Bold,
                        color = if (panelSession.hasChanges && !panelSession.isSaving && !panelSession.isSyncing) {
                            MaterialTheme.colors.primary
                        } else {
                            MaterialTheme.colors.onSurface.copy(alpha = 0.45f)
                        }
                    )
                }
            }
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            if (panelSession.isSyncing) {
                Text(
                    text = "Platform ayarlari yukleniyor...",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.primary,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            } else if (panelSession.syncError != null) {
                Text(
                    text = panelSession.syncError.orEmpty(),
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.error,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            Text(
                text = profileState.displayName,
                style = MaterialTheme.typography.subtitle2,
                color = MaterialTheme.colors.primary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            Text(
                text = "${caps.relays} rÃ¶le Â· ${caps.dimmers} dimmer Â· ${caps.sensors} sensÃ¶r Â· ${caps.water} su",
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.65f),
                modifier = Modifier.padding(bottom = 10.dp),
            )
            PanelPlatformSensorsSection(cfg = cfg, maxSensors = caps.sensors)
            if (caps.rgb > 0) {
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                ) {
                    InformationBoxConfig(
                        9,
                        cfg.RGBEnables.equals("yes", ignoreCase = true),
                        stringResource(Res.string.panel_sensor_rgb),
                    )
                }
            }
            if (caps.water > 0) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Su tanklarÄ±",
                    style = MaterialTheme.typography.subtitle2,
                    color = MaterialTheme.colors.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                PanelWaterTanksRow(cfg = cfg, maxWater = caps.water)
            }
            if (caps.dimmers > 0) {
                Spacer(Modifier.height(12.dp))
                for (dimIndex in 0 until caps.dimmers) {
                    VerticalSliderConfig(
                        cfg.DimsNameBuffer.getOrElse(dimIndex) { 0 } != 0.toByte(),
                        dimIndex,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            InputOutputPanel(valueArray, idArray)
        }
    }
}

@Composable
fun ConnectionSetting() {
    val global by vanGoGlobal.collectAsState()
    val appConn by appConnectionState.collectAsState()
    val lastTelemetryAt by lastSuccessfulTelemetryEpochSec.collectAsState()
    val connectionState = global.connectionState
    val bleEstablished = isBleGattLinkReady()
    val data by vanGoData.collectAsState()
    LaunchedEffect(connectionState, bleEstablished) {
        syncBleAppConnectionStateIfLinked()
    }
    var selectedMode by remember {
        mutableStateOf(
            if (appConn == AppConnectionState.REMOTE_MQTT_CONNECTED && !bleEstablished) {
                ConnectionUiMode.Remote
            } else {
                ConnectionUiMode.Bluetooth
            },
        )
    }
    LaunchedEffect(appConn, bleEstablished) {
        selectedMode = when {
            bleEstablished || appConn == AppConnectionState.BLE_CONNECTED -> ConnectionUiMode.Bluetooth
            appConn == AppConnectionState.REMOTE_MQTT_CONNECTED -> ConnectionUiMode.Remote
            isRemoteUserModeActive() && !bleEstablished -> ConnectionUiMode.Remote
            else -> selectedMode
        }
    }

    var deviceServerInfo by remember { mutableStateOf(readCachedDeviceServerInfo()) }
    var lastResolvedMac by remember {
        mutableStateOf(
            LoginPreferences.getCachedPairedDeviceMac()?.trim()?.takeUnless {
                it.isEmpty() || it == "\u2014" || it == "-"
            }
        )
    }
    val wifiMacLive by vanGoDeviceWifiMac.collectAsState()
    LaunchedEffect(bleEstablished, wifiMacLive) {
        val mac = currentPlatformDeviceMac()
        if (mac.isNullOrBlank()) {
            deviceServerInfo = readCachedDeviceServerInfo()
            return@LaunchedEffect
        }
        val hasLocalInfo = deviceServerInfo != null || readCachedDeviceServerInfo() != null
        if (lastResolvedMac == mac && hasLocalInfo) {
            deviceServerInfo = deviceServerInfo ?: readCachedDeviceServerInfo()
            return@LaunchedEffect
        }
        val fresh = DeviceUidLookupApi.lookupDeviceServerInfoByMac(mac).getOrNull()
        if (fresh != null) {
            deviceServerInfo = fresh
            lastResolvedMac = mac
            persistDeviceLookupCache(mac, fresh)
        } else {
            deviceServerInfo = readCachedDeviceServerInfo()
        }
    }
    fun normalizeInfo(v: String?): String = v?.trim()?.takeIf { it.isNotEmpty() } ?: "-"
    val uidValue = deviceServerInfo?.deviceUid?.takeIf { !it.isNullOrBlank() }
        ?: deviceUidForUiFromSlvConn(data.slvConn)
    val hardwareValue = normalizeInfo(deviceServerInfo?.hardware)
    val softwareValue = normalizeInfo(deviceServerInfo?.software)
    val mechanicValue = normalizeInfo(deviceServerInfo?.mechanic)
    val remoteLinkFromDevice = isRemoteLinkActive(data.slvConn)
    val remoteModeOn by remoteModeEnabled.collectAsState()
    val bleActive = bleEstablished || appConn == AppConnectionState.BLE_CONNECTED
    val remoteMqttActive = appConn == AppConnectionState.REMOTE_MQTT_CONNECTED && !bleEstablished
    val remotePanelActive = isRemoteUserModeActive() && !remoteMqttActive && !bleEstablished
    val remoteRoute by remoteMqttRoute.collectAsState()
    val sessionErr by remoteSessionError.collectAsState()
    val fallbackLastPacketAt = global.receivedPackageTime
    val effectiveLastPacketAt = if (lastTelemetryAt > 0L) lastTelemetryAt else fallbackLastPacketAt
    val secondsSinceLastPacket = (getCurrentTimeInSeconds() - effectiveLastPacketAt).coerceAtLeast(0)
    val lastPacketText = if (effectiveLastPacketAt <= 0L) {
        "-"
    } else if (secondsSinceLastPacket < 60) {
        "$secondsSinceLastPacket sn Ã¶nce"
    } else {
        "${secondsSinceLastPacket / 60} dk Ã¶nce"
    }

    val activeModeLabel = when {
        bleActive -> "Bluetooth aktif"
        remoteMqttActive -> "Remote aktif (MQTT)"
        remotePanelActive -> "Remote aktif (panel)"
        else -> "Ã‡evrimdÄ±ÅŸÄ±"
    }
    val remoteChannel = remoteRoute?.telemetryTopic?.let { "Kanal: $it" }

    ConnectionSettingScreen(
        backLabel = settingsBackLabelFor(SettingsPage.Connection),
        pageTitle = settingsHeaderTitleFor(SettingsPage.Connection),
        onBack = { backToMenuGrid() },
        selectedMode = selectedMode,
        bleActive = bleActive,
        isConnected = bleEstablished,
        remoteMqttActive = remoteMqttActive || remotePanelActive,
        remoteLinkFromDevice = remoteLinkFromDevice,
        onSelectBluetoothMode = {
            selectedMode = ConnectionUiMode.Bluetooth
            activateBleModeExclusive()
            if (!bleEstablished) {
                getVanGO().discoverVangos()
                BleAutoConnect.tryAutoConnect("settings-ble")
            }
        },
        onSelectRemoteMode = {
            selectedMode = ConnectionUiMode.Remote
            val token = LoginPreferences.getAuthToken()?.trim().orEmpty()
            if (token.isEmpty()) {
                openRemoteConnectionPage()
            } else {
                activateRemoteModeExclusive()
            }
        },
        onOpenBlePair = { navigateToMenuPage(SettingsPage.BlePair) },
        onOpenRemoteManage = { openRemoteConnectionPage() },
        onRemoteDisconnect = {
            disconnectRemoteLink()
        },
        remoteChannelLabel = remoteChannel,
        remoteSessionError = sessionErr,
        uidValue = uidValue ?: "-",
        wifiMac = currentPlatformDeviceMac() ?: "-",
        bleMac = currentBleAdapterMac() ?: "-",
        hardwareValue = hardwareValue,
        softwareValue = softwareValue,
        mechanicValue = mechanicValue,
        lastPacketText = lastPacketText,
        activeModeLabel = activeModeLabel,
        bleDiscovering = connectionState == BleGattConnectionChain.DiscoveringDevice,
    )
}

// Data class to hold BLE device information from Kable
data class KableBleDevice(
    val address: String,
    val name: String?,
    val rssi: Int?,
    val advertisement: Advertisement
)

@Composable
fun BleDeviceScannerSection() {
    val coroutineScope = rememberCoroutineScope()
    var isScanning by remember { mutableStateOf(false) }
    var discoveredDevices by remember { mutableStateOf<List<KableBleDevice>>(emptyList()) }
    var scanner: Scanner? by remember { mutableStateOf(null) }
    
    // Start/stop scanning using Kable Scanner
    LaunchedEffect(isScanning) {
        if (isScanning) {
            try {
                val newScanner = Scanner()
                scanner = newScanner
                
                // Collect advertisements from the scanner
                // The scanner automatically stops when the flow collection is cancelled
                newScanner.advertisements.collect { advertisement ->
                    // Check if device already exists
                    val existingIndex = discoveredDevices.indexOfFirst { it.address == advertisement.identifier.toString() }
                    val device = KableBleDevice(
                        address = advertisement.identifier.toString(),
                        name = advertisement.name,
                        rssi = advertisement.rssi,
                        advertisement = advertisement
                    )
                    
                    if (existingIndex >= 0) {
                        // Update existing device
                        discoveredDevices = discoveredDevices.toMutableList().apply {
                            set(existingIndex, device)
                        }
                    } else {
                        // Add new device
                        discoveredDevices = discoveredDevices + device
                    }
                }
            } catch (e: Exception) {
                println("Error scanning for BLE devices: ${e.message}")
                e.printStackTrace()
                isScanning = false
            }
        } else {
            // Scanner will stop automatically when LaunchedEffect is cancelled
            scanner = null
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colors.surface,
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.12f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ğŸ” BLE Device Scanner",
                    style = MaterialTheme.typography.h6,
                    color = MaterialTheme.colors.onSurface
                )
                Text(
                    text = "${discoveredDevices.size} device${if (discoveredDevices.size != 1) "s" else ""}",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.primary
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = if (isScanning) "ğŸ”„ Scanning for BLE devices..." else "Tap 'Start Scan' to discover nearby BLE devices",
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        isScanning = true
                    },
                    enabled = !isScanning,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Start Scan")
                }
                
                Button(
                    onClick = {
                        isScanning = false
                    },
                    enabled = isScanning,
                    modifier = Modifier.weight(1f)
                ) {
                    if (isScanning) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SpiralWaitingIcon(Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Stop Scan")
                        }
                    } else {
                        Text("Stop Scan")
                    }
                }
            }
            
            if (discoveredDevices.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                
                // Clear button
                Button(
                    onClick = {
                        discoveredDevices = emptyList()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Clear List")
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Device list
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(discoveredDevices.size) { index ->
                        val device = discoveredDevices[index]
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = MaterialTheme.colors.surface,
                            elevation = 2.dp,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = device.name ?: "Unknown Device",
                                            style = MaterialTheme.typography.body1,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colors.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Address: ${device.address}",
                                            style = MaterialTheme.typography.caption,
                                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                                        )
                                        if (device.rssi != null) {
                                            Text(
                                                text = "RSSI: ${device.rssi} dBm",
                                                style = MaterialTheme.typography.caption,
                                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IconButton(
    label: String = "label",
    index: Int,
    enabled: Boolean = true,
    connected: Boolean = false,
    modifier: Modifier = Modifier,
) {
    var clicked by remember { mutableStateOf(false) }
    val commentsAlpha = if (enabled) 1f else 0f
    val color = if(connected) MaterialTheme.colors.secondary.copy(alpha = .7f) else Color.Gray.copy(alpha = .7f)

    Column(
        modifier = modifier
            .alpha(commentsAlpha)
            .background(
                color = color
            )
            .clickable(
                enabled = enabled,
                onClick = {
                    CoroutineScope(Dispatchers.Default).launch {
                        clicked = true
                        val targetOn = !connected
                        println("writing digital port $index -> ${if (targetOn) "ON" else "OFF"} (ui=$connected)")
                        val result = getAppContext().vanGO.writeDigitalPort(index, targetOn)
                        if (!result) {
                            println("Failed to send relay command idx=$index on=$targetOn")
                        }
                        clicked = false
                    }
                }
            )
            .fillMaxSize()
            .fillMaxHeight()
            .border(.5.dp, MaterialTheme.colors.background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize(0.75f),
            contentAlignment = Alignment.Center,
        ) {
//            Image(
////                painter = icon,
//                contentDescription = "",
//                modifier = Modifier
//                    .fillMaxSize(0.6f),  // Icon takes 60% of the button size
//                colorFilter = ColorFilter.tint(MaterialTheme.colors.primary.copy(alpha = .7f)) // Apply the tint
//            )
            if(clicked) {
                SpiralWaitingIcon(Modifier.matchParentSize())
            }
        }
        Text(
            text = label
        )
    }
}

@Composable
fun DebugSetting() {
    var incomingDebugEnabled by remember { mutableStateOf(BleDebugger.isIncomingDebugEnabled()) }
    var outgoingDataDebugEnabled by remember { mutableStateOf(BleDebugger.isOutgoingDataDebugEnabled()) }
    var outgoingConfigDebugEnabled by remember { mutableStateOf(BleDebugger.isOutgoingConfigDebugEnabled()) }
    var outgoingWeatherDebugEnabled by remember { mutableStateOf(BleDebugger.isOutgoingWeatherDebugEnabled()) }
    val connectionState by vanGoGlobal.collectAsState()
    val deviceList by vangoDeviceListSize.collectAsState()
    val vangoData by vanGoData.collectAsState()
    val isConnected = connectionState.connectionState == BleGattConnectionChain.ChainEnded

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Debug Controls Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = Color(0xFF2A2A2A),
                shape = RoundedCornerShape(12.dp),
                elevation = 4.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "ğŸ› Debug Controls",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Incoming Debug Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "ğŸ“¥ Incoming Messages Debug",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                            Text(
                                text = "Debug notify messages from device",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                        Switch(
                            checked = incomingDebugEnabled,
                            onCheckedChange = { enabled ->
                                incomingDebugEnabled = enabled
                                BleDebugger.enableIncomingDebug(enabled)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Green,
                                uncheckedThumbColor = Color.Gray
                            )
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Outgoing Data Debug Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "ğŸ“¤ Outgoing Data Messages Debug",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                            Text(
                                text = "Debug I/O control and data messages",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                        Switch(
                            checked = outgoingDataDebugEnabled,
                            onCheckedChange = { enabled ->
                                outgoingDataDebugEnabled = enabled
                                BleDebugger.enableOutgoingDataDebug(enabled)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Green,
                                uncheckedThumbColor = Color.Gray
                            )
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Outgoing Config Debug Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "âš™ï¸ Outgoing Config Messages Debug",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                            Text(
                                text = "Debug device configuration messages",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                        Switch(
                            checked = outgoingConfigDebugEnabled,
                            onCheckedChange = { enabled ->
                                outgoingConfigDebugEnabled = enabled
                                BleDebugger.enableOutgoingConfigDebug(enabled)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Green,
                                uncheckedThumbColor = Color.Gray
                            )
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Outgoing Weather Debug Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "ğŸŒ¤ï¸ Outgoing Weather Messages Debug",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                            Text(
                                text = "Debug weather data messages",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                        Switch(
                            checked = outgoingWeatherDebugEnabled,
                            onCheckedChange = { enabled ->
                                outgoingWeatherDebugEnabled = enabled
                                BleDebugger.enableOutgoingWeatherDebug(enabled)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Green,
                                uncheckedThumbColor = Color.Gray
                            )
                        )
                    }
                }
            }
        }

        // Connection Status Section
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colors.surface,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = "ğŸ“Š Connection Status",
                        style = MaterialTheme.typography.h6,
                        color = MaterialTheme.colors.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Device Status: ${if (isConnected) "âœ… Connected" else "âŒ Disconnected"}",
                        style = MaterialTheme.typography.body2,
                        color = if (isConnected) Color.Green else Color.Red
                    )
                    Text(
                        text = "Connection State: ${connectionState.connectionState}",
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "Current Operation: ${connectionState.operation()}",
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "Operation Status: ${connectionState.operationStatus}",
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "MTU Size: ${connectionState.currentMtu} bytes",
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "Notifications: ${if (connectionState.isCharacteristicNotificationOn) "âœ… Enabled" else "âŒ Disabled"}",
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                    )
                    
                    val timeSinceLastPacket = getCurrentTimeInSeconds() - connectionState.receivedPackageTime
                    Text(
                        text = "Last Data: ${timeSinceLastPacket}s ago",
                        style = MaterialTheme.typography.caption,
                        color = if (timeSinceLastPacket < 10) Color.Green else Color.Yellow
                    )
                }
            }
        }

        // Device List Debug Section
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colors.surface,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = "ğŸ“± Discovered Devices",
                        style = MaterialTheme.typography.h6,
                        color = MaterialTheme.colors.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Device Count: $deviceList",
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onSurface
                    )
                    
                    if (deviceList > 0) {
                        vangoDeviceList.forEachIndexed { index, device ->
                            Text(
                                text = "[$index] ${device.name} (${device.mac})",
                                style = MaterialTheme.typography.caption,
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    } else {
                        Text(
                            text = "No devices discovered",
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }

        // VanGO Data Debug Section
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colors.surface,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = "ğŸ” VanGO Data Analysis",
                        style = MaterialTheme.typography.h6,
                        color = MaterialTheme.colors.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val isValid = vangoData.isValid()
                    
                    Text(
                        text = "Data Valid: ${if (isValid) "âœ… Yes" else "âŒ No"}",
                        style = MaterialTheme.typography.body2,
                        color = if (isValid) Color.Green else Color.Red
                    )
                    Text(
                        text = "Slave Connection: \"${vangoData.slvConn}\"",
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "Outputs: ${vangoData.numOfO}, Dims: ${vangoData.numOfD}, Sensors: ${vangoData.numOfS}",
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "RGB Enabled: ${vangoData.RGBE}, Theme: ${vangoData.Th}",
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "Voltage: ${vangoData.volt}V",
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                    )
                    
                    // Buffer sizes
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Buffer Sizes:",
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.primary
                    )
                    Text(
                        text = "â€¢ Outputs: ${vangoData.outNB.size} names, ${vangoData.outDB.size} data",
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "â€¢ Dims: ${vangoData.dimNB.size} names, ${vangoData.dDB.size} data",
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "â€¢ Sensors: ${vangoData.senNB.size} enabled, ${vangoData.sDB.size} data",
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "â€¢ RGB: ${vangoData.RGBDB.size} values",
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }

        // System Info Section
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colors.surface,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = "âš™ï¸ System Information",
                        style = MaterialTheme.typography.h6,
                        color = MaterialTheme.colors.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Platform: ${getPlatform().name}",
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "Current Time: ${getFormattedTime()}",
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "Current Date: ${getFormattedDate()}",
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "Epoch Seconds: ${getCurrentTimeInSeconds()}",
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Background Service:",
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.primary
                    )
                    Text(
                        text = "â€¢ Maintains BLE connection when app is backgrounded",
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "â€¢ Handles automatic reconnection attempts", 
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "â€¢ Sends periodic weather updates",
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "â€¢ Preserves connection during screen lock",
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "ğŸ“„ Debug logs are printed to console/logcat for analysis",
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}


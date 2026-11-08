package com.hextech.vango

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import testvango.composeapp.generated.resources.Res
import testvango.composeapp.generated.resources.ble_pair_title
import testvango.composeapp.generated.resources.footer_menu
import testvango.composeapp.generated.resources.manual_add_title
import testvango.composeapp.generated.resources.profile_title
import testvango.composeapp.generated.resources.remote_settings_title
import testvango.composeapp.generated.resources.rgb_scene_edit_title
import testvango.composeapp.generated.resources.settings_about_page_title
import testvango.composeapp.generated.resources.settings_about_title
import testvango.composeapp.generated.resources.settings_climate_menu_title
import testvango.composeapp.generated.resources.settings_climate_page_title
import testvango.composeapp.generated.resources.settings_connection_title
import testvango.composeapp.generated.resources.settings_language_menu_title
import testvango.composeapp.generated.resources.settings_language_page_title
import testvango.composeapp.generated.resources.settings_panel_title
import testvango.composeapp.generated.resources.settings_rgb_presets_menu_title
import testvango.composeapp.generated.resources.settings_rgb_presets_title
import testvango.composeapp.generated.resources.settings_theme_menu_title
import testvango.composeapp.generated.resources.settings_theme_page_title

fun settingsParentPage(page: SettingsPage): SettingsPage? = when (page) {
    SettingsPage.Main -> null
    SettingsPage.Language, SettingsPage.Theme, SettingsPage.Panel, SettingsPage.RgbPresets,
    SettingsPage.Climate, SettingsPage.Connection, SettingsPage.About, SettingsPage.Profile,
    SettingsPage.Debug -> SettingsPage.Main
    SettingsPage.BlePair, SettingsPage.Remote -> SettingsPage.Connection
    SettingsPage.ManualDeviceAdd -> SettingsPage.BlePair
}

@Composable
fun settingsBackLabelFor(page: SettingsPage): String {
    val parent = settingsParentPage(page) ?: return ""
    return settingsNavTitleFor(parent)
}

@Composable
fun settingsNavTitleFor(page: SettingsPage): String = when (page) {
    SettingsPage.Main -> stringResource(Res.string.footer_menu)
    SettingsPage.Language -> stringResource(Res.string.settings_language_menu_title)
    SettingsPage.Theme -> stringResource(Res.string.settings_theme_menu_title)
    SettingsPage.Panel -> stringResource(Res.string.settings_panel_title)
    SettingsPage.RgbPresets -> stringResource(Res.string.settings_rgb_presets_menu_title)
    SettingsPage.Climate -> stringResource(Res.string.settings_climate_menu_title)
    SettingsPage.Connection -> stringResource(Res.string.settings_connection_title)
    SettingsPage.BlePair -> stringResource(Res.string.ble_pair_title)
    SettingsPage.ManualDeviceAdd -> stringResource(Res.string.manual_add_title)
    SettingsPage.Remote -> stringResource(Res.string.remote_settings_title)
    SettingsPage.About -> stringResource(Res.string.settings_about_title)
    SettingsPage.Profile -> stringResource(Res.string.profile_title)
    SettingsPage.Debug -> "Debug"
}

@Composable
fun settingsHeaderTitleFor(page: SettingsPage): String = when (page) {
    SettingsPage.Language -> stringResource(Res.string.settings_language_page_title)
    SettingsPage.Theme -> stringResource(Res.string.settings_theme_page_title)
    SettingsPage.RgbPresets -> stringResource(Res.string.settings_rgb_presets_title)
    SettingsPage.Climate -> stringResource(Res.string.settings_climate_page_title)
    SettingsPage.Remote -> stringResource(Res.string.remote_settings_title)
    SettingsPage.About -> stringResource(Res.string.settings_about_page_title)
    else -> settingsNavTitleFor(page)
}

@Composable
fun rgbSceneEditorBackLabel(): String =
    stringResource(Res.string.settings_rgb_presets_menu_title)

@Composable
fun rgbSceneEditorHeaderTitle(): String =
    stringResource(Res.string.rgb_scene_edit_title)
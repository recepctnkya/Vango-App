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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hextech.vango.profile.ProfileDisplayPreferences
import org.jetbrains.compose.resources.stringResource
import testvango.composeapp.generated.resources.Res
import testvango.composeapp.generated.resources.notif_rd_action_audio
import testvango.composeapp.generated.resources.notif_rd_action_label
import testvango.composeapp.generated.resources.notif_rd_cat_gas
import testvango.composeapp.generated.resources.notif_rd_cat_other
import testvango.composeapp.generated.resources.notif_rd_cat_power
import testvango.composeapp.generated.resources.notif_rd_cat_temp
import testvango.composeapp.generated.resources.notif_rd_filter_all
import testvango.composeapp.generated.resources.notif_rd_filter_gas
import testvango.composeapp.generated.resources.notif_rd_filter_other
import testvango.composeapp.generated.resources.notif_rd_filter_power
import testvango.composeapp.generated.resources.notif_rd_filter_temp
import testvango.composeapp.generated.resources.notif_rd_filter_water
import testvango.composeapp.generated.resources.notif_rd_metric_alarm
import testvango.composeapp.generated.resources.notif_rd_metric_level
import testvango.composeapp.generated.resources.notif_rd_metric_temp
import testvango.composeapp.generated.resources.notif_rd_metric_voltage
import testvango.composeapp.generated.resources.notif_rd_action_notify
import testvango.composeapp.generated.resources.notif_rd_action_pump_off
import testvango.composeapp.generated.resources.notif_rd_action_pump_on
import testvango.composeapp.generated.resources.notif_rd_active
import testvango.composeapp.generated.resources.notif_rd_active_rules
import testvango.composeapp.generated.resources.notif_rd_add_cancel
import testvango.composeapp.generated.resources.notif_rd_add_condition
import testvango.composeapp.generated.resources.notif_rd_add_name
import testvango.composeapp.generated.resources.notif_rd_add_rule
import testvango.composeapp.generated.resources.notif_rd_add_save
import testvango.composeapp.generated.resources.notif_rd_add_title
import testvango.composeapp.generated.resources.notif_rd_auto_summary
import testvango.composeapp.generated.resources.notif_rd_cat_energy
import testvango.composeapp.generated.resources.notif_rd_cat_security
import testvango.composeapp.generated.resources.notif_rd_cat_sensors
import testvango.composeapp.generated.resources.notif_rd_cat_water
import testvango.composeapp.generated.resources.notif_rd_ch_app
import testvango.composeapp.generated.resources.notif_rd_ch_audio
import testvango.composeapp.generated.resources.notif_rd_ch_email
import testvango.composeapp.generated.resources.notif_rd_ch_push
import testvango.composeapp.generated.resources.notif_rd_channels
import testvango.composeapp.generated.resources.notif_rd_col_action
import testvango.composeapp.generated.resources.notif_rd_col_condition
import testvango.composeapp.generated.resources.notif_rd_col_notif
import testvango.composeapp.generated.resources.notif_rd_col_rule
import testvango.composeapp.generated.resources.notif_rd_cond_co
import testvango.composeapp.generated.resources.notif_rd_cond_detected
import testvango.composeapp.generated.resources.notif_rd_cond_door
import testvango.composeapp.generated.resources.notif_rd_cond_grey
import testvango.composeapp.generated.resources.notif_rd_cond_hum
import testvango.composeapp.generated.resources.notif_rd_cond_inv
import testvango.composeapp.generated.resources.notif_rd_cond_solar
import testvango.composeapp.generated.resources.notif_rd_delete
import testvango.composeapp.generated.resources.notif_rd_hist_empty
import testvango.composeapp.generated.resources.notif_rd_hist_hint
import testvango.composeapp.generated.resources.notif_rd_hist_item_1
import testvango.composeapp.generated.resources.notif_rd_hist_item_2
import testvango.composeapp.generated.resources.notif_rd_hist_item_3
import testvango.composeapp.generated.resources.notif_rd_hist_item_4
import testvango.composeapp.generated.resources.notif_rd_hist_item_5
import testvango.composeapp.generated.resources.notif_rd_hist_item_6
import testvango.composeapp.generated.resources.notif_rd_hist_time_1
import testvango.composeapp.generated.resources.notif_rd_hist_time_2
import testvango.composeapp.generated.resources.notif_rd_hist_time_3
import testvango.composeapp.generated.resources.notif_rd_hist_time_4
import testvango.composeapp.generated.resources.notif_rd_hist_time_5
import testvango.composeapp.generated.resources.notif_rd_hist_time_6
import testvango.composeapp.generated.resources.notif_rd_hydrofor
import testvango.composeapp.generated.resources.notif_rd_page_hint
import testvango.composeapp.generated.resources.notif_rd_rules_local_only_hint
import testvango.composeapp.generated.resources.notif_rule_sensor_unavailable
import testvango.composeapp.generated.resources.notif_ch_email_inactive
import testvango.composeapp.generated.resources.notif_ch_push_inactive
import testvango.composeapp.generated.resources.notif_rd_passive
import testvango.composeapp.generated.resources.notif_rd_passive_rules
import testvango.composeapp.generated.resources.notif_rd_quick_info
import testvango.composeapp.generated.resources.notif_rd_rule_battery_desc
import testvango.composeapp.generated.resources.notif_rd_rule_battery_title
import testvango.composeapp.generated.resources.notif_rd_rule_clean_full_desc
import testvango.composeapp.generated.resources.notif_rd_rule_clean_full_title
import testvango.composeapp.generated.resources.notif_rd_rule_clean_low_desc
import testvango.composeapp.generated.resources.notif_rd_rule_clean_low_title
import testvango.composeapp.generated.resources.notif_rd_rule_co_desc
import testvango.composeapp.generated.resources.notif_rd_rule_co_title
import testvango.composeapp.generated.resources.notif_rd_rule_door_desc
import testvango.composeapp.generated.resources.notif_rd_rule_door_title
import testvango.composeapp.generated.resources.notif_rd_rule_gas_desc
import testvango.composeapp.generated.resources.notif_rd_rule_gas_title
import testvango.composeapp.generated.resources.notif_rd_rule_grey_desc
import testvango.composeapp.generated.resources.notif_rd_rule_grey_title
import testvango.composeapp.generated.resources.notif_rd_rule_hum_desc
import testvango.composeapp.generated.resources.notif_rd_rule_hum_title
import testvango.composeapp.generated.resources.notif_rd_rule_inv_desc
import testvango.composeapp.generated.resources.notif_rd_rule_inv_title
import testvango.composeapp.generated.resources.notif_rd_rule_solar_desc
import testvango.composeapp.generated.resources.notif_rd_rule_solar_title
import testvango.composeapp.generated.resources.notif_rd_rule_temp_desc
import testvango.composeapp.generated.resources.notif_rd_rule_temp_title
import testvango.composeapp.generated.resources.notif_rd_rule_waste_desc
import testvango.composeapp.generated.resources.notif_rd_rule_waste_title
import testvango.composeapp.generated.resources.notif_rd_show_less
import testvango.composeapp.generated.resources.notif_rd_show_more
import testvango.composeapp.generated.resources.notif_rd_tab_history
import testvango.composeapp.generated.resources.notif_rd_tab_rules
import testvango.composeapp.generated.resources.notif_rd_tab_settings
import testvango.composeapp.generated.resources.notif_rd_total_rules
import testvango.composeapp.generated.resources.notif_rd_week_total
import testvango.composeapp.generated.resources.profile_rd_notif_alert
import testvango.composeapp.generated.resources.profile_rd_notif_alert_hint
import testvango.composeapp.generated.resources.profile_rd_notif_device
import testvango.composeapp.generated.resources.profile_rd_notif_device_hint
import testvango.composeapp.generated.resources.profile_rd_notif_system
import testvango.composeapp.generated.resources.profile_rd_notif_system_hint
import testvango.composeapp.generated.resources.settings_rd_notifications_title

private val NotifText: Color @Composable get() = LocalAppChrome.current.text
private val NotifMuted: Color @Composable get() = LocalAppChrome.current.muted
private val NotifBorder: Color @Composable get() = LocalAppChrome.current.border
private val NotifCard: Color @Composable get() = LocalAppChrome.current.card
private val NotifTile: Color @Composable get() = LocalAppChrome.current.tile
private val NotifGreen = Color(0xFF3DDC84)
private val NotifYellow = Color(0xFFFFC14D)
private val NotifPurple = Color(0xFFB388FF)

private enum class NotifPageTab { Rules, History, Settings }

private enum class NotifCategory { Water, Power, Gas, Temp, Other }

private enum class NotifFilter { All, Water, Power, Gas, Temp, Other }

private enum class NotifActionKind { NotifyOnly, PumpOn, PumpOff, AudioWarn }

private data class NotifRuleUi(
    val id: String,
    val category: NotifCategory,
    val title: String,
    val description: String,
    val conditions: List<String>,
    val conditionIndex: Int,
    val metric: String,
    val action: NotifActionKind,
    val enabled: Boolean,
)

private const val CHANNEL_APP = 1
private const val CHANNEL_AUDIO = 2
private const val CHANNEL_EMAIL = 4
private const val CHANNEL_PUSH = 8
private const val DEFAULT_DISABLED = "grey,humidity,inverter,door"

@Composable
fun NotificationsRedesignPage() {
    val accent = accentColorFromId(customAppAccentId)
    val radius = appCardCornerRadius()
    var tab by remember { mutableStateOf(NotifPageTab.Rules) }
    var filter by remember { mutableStateOf(NotifFilter.All) }
    var addOpen by remember { mutableStateOf(false) }
    var channelFlags by remember { mutableStateOf(ProfileDisplayPreferences.getNotifChannelFlags()) }
    val templates = defaultNotifRules()
    val disabledSeed = remember {
        ProfileDisplayPreferences.getNotifDisabledRuleIds().ifBlank { DEFAULT_DISABLED }
            .split(',').map { it.trim() }.filter { it.isNotEmpty() }.toSet() +
            AlertRuleEngine.unsupportedRuleIds
    }
    val rules = remember {
        mutableStateListOf<NotifRuleUi>().also { list ->
            list.addAll(
                templates.map { template ->
                    val unsupported = template.id in AlertRuleEngine.unsupportedRuleIds
                    template.copy(
                        enabled = !unsupported && template.id !in disabledSeed,
                        conditionIndex = AlertRuleEngine.conditionIndex(template.id),
                    )
                },
            )
        }
    }
    val filteredRules = remember(rules.toList(), filter) {
        when (filter) {
            NotifFilter.All -> rules.toList()
            NotifFilter.Water -> rules.filter { it.category == NotifCategory.Water }
            NotifFilter.Power -> rules.filter { it.category == NotifCategory.Power }
            NotifFilter.Gas -> rules.filter { it.category == NotifCategory.Gas }
            NotifFilter.Temp -> rules.filter { it.category == NotifCategory.Temp }
            NotifFilter.Other -> rules.filter { it.category == NotifCategory.Other }
        }
    }

    if (addOpen) {
        AddRuleDialog(
            accent = accent,
            onDismiss = { addOpen = false },
            onSave = { title, condition ->
                rules.add(
                    NotifRuleUi(
                        id = "custom_${rules.size}_${title.hashCode()}",
                        category = NotifCategory.Other,
                        title = title,
                        description = condition,
                        conditions = listOf(condition),
                        conditionIndex = 0,
                        metric = "",
                        action = NotifActionKind.NotifyOnly,
                        enabled = true,
                    ),
                )
                persistDisabled(rules)
                addOpen = false
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .border(1.5.dp, accent.copy(alpha = 0.7f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.Notifications, null, tint = accent, modifier = Modifier.size(22.dp))
                }
                Text(
                    stringResource(Res.string.settings_rd_notifications_title).uppercase(),
                    color = NotifText,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.7.sp,
                )
            }
            Text(
                stringResource(Res.string.notif_rd_page_hint),
                color = NotifMuted,
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(Res.string.notif_rd_rules_local_only_hint),
                color = NotifMuted,
                fontSize = 12.sp,
                lineHeight = 16.sp,
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            NotifInnerTab(
                label = stringResource(Res.string.notif_rd_tab_rules),
                selected = tab == NotifPageTab.Rules,
                accent = accent,
                onClick = { tab = NotifPageTab.Rules },
            )
            NotifInnerTab(
                label = stringResource(Res.string.notif_rd_tab_history),
                selected = tab == NotifPageTab.History,
                accent = accent,
                onClick = { tab = NotifPageTab.History },
            )
            NotifInnerTab(
                label = stringResource(Res.string.notif_rd_tab_settings),
                selected = tab == NotifPageTab.Settings,
                accent = accent,
                onClick = { tab = NotifPageTab.Settings },
            )
        }

        when (tab) {
            NotifPageTab.Rules -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    NotifFilterRow(filter = filter, accent = accent, onFilter = { filter = it })
                    filteredRules.forEach { rule ->
                        RuleCard(
                            rule = rule,
                            accent = accent,
                            onChange = { updated ->
                                val idx = rules.indexOfFirst { it.id == updated.id }
                                if (idx >= 0) rules[idx] = updated
                                persistDisabled(rules)
                            },
                            onDelete = { id ->
                                rules.removeAll { it.id == id }
                                persistDisabled(rules)
                            },
                        )
                    }
                    // Add-rule / automation actions not shipped yet — avoid implying they run.
                    Spacer(Modifier.height(4.dp))
                }
            }
            NotifPageTab.History -> AlarmHistoryPanel(accent = accent, radius = radius)
            NotifPageTab.Settings -> NotificationSettingsPanel(
                accent = accent,
                radius = radius,
                channelFlags = channelFlags,
                onChannel = { bit, on ->
                    channelFlags = if (on) channelFlags or bit else channelFlags and bit.inv()
                    ProfileDisplayPreferences.setNotifChannelFlags(channelFlags)
                },
            )
        }
    }
}

@Composable
private fun defaultNotifRules(): List<NotifRuleUi> {
    val level = stringResource(Res.string.notif_rd_metric_level)
    val voltage = stringResource(Res.string.notif_rd_metric_voltage)
    val tempMetric = stringResource(Res.string.notif_rd_metric_temp)
    val alarm = stringResource(Res.string.notif_rd_metric_alarm)
    return listOf(
        NotifRuleUi(
            "clean_low", NotifCategory.Water,
            stringResource(Res.string.notif_rd_rule_clean_low_title),
            stringResource(Res.string.notif_rd_rule_clean_low_desc),
            listOf("≤ 10%", "≤ 15%", "≤ 20%"), 0, level, NotifActionKind.PumpOn, true,
        ),
        NotifRuleUi(
            "clean_full", NotifCategory.Water,
            stringResource(Res.string.notif_rd_rule_clean_full_title),
            stringResource(Res.string.notif_rd_rule_clean_full_desc),
            listOf("≥ 95%", "≥ 90%", "≥ 85%"), 0, level, NotifActionKind.PumpOff, true,
        ),
        NotifRuleUi(
            "battery", NotifCategory.Power,
            stringResource(Res.string.notif_rd_rule_battery_title),
            stringResource(Res.string.notif_rd_rule_battery_desc),
            listOf("≤ 11.8V", "≤ 12.0V", "≤ 11.5V"), 0, voltage, NotifActionKind.NotifyOnly, true,
        ),
        NotifRuleUi(
            "temp", NotifCategory.Temp,
            stringResource(Res.string.notif_rd_rule_temp_title),
            stringResource(Res.string.notif_rd_rule_temp_desc),
            listOf("≥ 30°C", "≥ 32°C", "≥ 35°C"), 0, tempMetric, NotifActionKind.NotifyOnly, true,
        ),
        NotifRuleUi(
            "gas", NotifCategory.Gas,
            stringResource(Res.string.notif_rd_rule_gas_title),
            stringResource(Res.string.notif_rd_rule_gas_desc),
            listOf(stringResource(Res.string.notif_rd_cond_detected)), 0, alarm, NotifActionKind.AudioWarn, true,
        ),
        NotifRuleUi(
            "waste_full", NotifCategory.Water,
            stringResource(Res.string.notif_rd_rule_waste_title),
            stringResource(Res.string.notif_rd_rule_waste_desc),
            listOf("≥ 90%", "≥ 80%", "≥ 70%"), 0, level, NotifActionKind.NotifyOnly, true,
        ),
        NotifRuleUi(
            "grey", NotifCategory.Water,
            stringResource(Res.string.notif_rd_rule_grey_title),
            stringResource(Res.string.notif_rd_rule_grey_desc),
            listOf(stringResource(Res.string.notif_rd_cond_grey)), 0, level, NotifActionKind.NotifyOnly, true,
        ),
        NotifRuleUi(
            "humidity", NotifCategory.Other,
            stringResource(Res.string.notif_rd_rule_hum_title),
            stringResource(Res.string.notif_rd_rule_hum_desc),
            listOf(stringResource(Res.string.notif_rd_cond_hum)), 0, "", NotifActionKind.NotifyOnly, true,
        ),
        NotifRuleUi(
            "inverter", NotifCategory.Power,
            stringResource(Res.string.notif_rd_rule_inv_title),
            stringResource(Res.string.notif_rd_rule_inv_desc),
            listOf(stringResource(Res.string.notif_rd_cond_inv)), 0, "", NotifActionKind.NotifyOnly, true,
        ),
        NotifRuleUi(
            "door", NotifCategory.Other,
            stringResource(Res.string.notif_rd_rule_door_title),
            stringResource(Res.string.notif_rd_rule_door_desc),
            listOf(stringResource(Res.string.notif_rd_cond_door)), 0, "", NotifActionKind.NotifyOnly, true,
        ),
        NotifRuleUi(
            "solar", NotifCategory.Power,
            stringResource(Res.string.notif_rd_rule_solar_title),
            stringResource(Res.string.notif_rd_rule_solar_desc),
            listOf(stringResource(Res.string.notif_rd_cond_solar)), 0, "", NotifActionKind.NotifyOnly, true,
        ),
        NotifRuleUi(
            "co", NotifCategory.Other,
            stringResource(Res.string.notif_rd_rule_co_title),
            stringResource(Res.string.notif_rd_rule_co_desc),
            listOf(stringResource(Res.string.notif_rd_cond_co)), 0, alarm, NotifActionKind.AudioWarn, true,
        ),
    )
}

private fun persistDisabled(rules: List<NotifRuleUi>) {
    val csv = rules.filter { !it.enabled || it.id in AlertRuleEngine.unsupportedRuleIds }
        .map { it.id }
        .distinct()
        .joinToString(",")
    ProfileDisplayPreferences.setNotifDisabledRuleIds(csv)
    rules.forEach { AlertRuleEngine.setConditionIndex(it.id, it.conditionIndex) }
}

@Composable
private fun NotifInnerTab(label: String, selected: Boolean, accent: Color, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(end = 18.dp, bottom = 2.dp),
    ) {
        Text(
            label,
            color = if (selected) accent else NotifMuted,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        )
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier
                .height(2.dp)
                .width(if (selected) 28.dp else 0.dp)
                .background(accent, RoundedCornerShape(2.dp)),
        )
    }
}

@Composable
private fun NotifFilterRow(
    filter: NotifFilter,
    accent: Color,
    onFilter: (NotifFilter) -> Unit,
) {
    val items = listOf(
        NotifFilter.All to (Icons.Filled.GridView to Res.string.notif_rd_filter_all),
        NotifFilter.Water to (Icons.Filled.WaterDrop to Res.string.notif_rd_filter_water),
        NotifFilter.Power to (Icons.Filled.Bolt to Res.string.notif_rd_filter_power),
        NotifFilter.Gas to (Icons.Filled.LocalFireDepartment to Res.string.notif_rd_filter_gas),
        NotifFilter.Temp to (Icons.Filled.Thermostat to Res.string.notif_rd_filter_temp),
        NotifFilter.Other to (Icons.Filled.MoreHoriz to Res.string.notif_rd_filter_other),
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items.forEach { (value, pair) ->
            val selected = filter == value
            val tint = if (selected) accent else NotifMuted
            Column(
                modifier = Modifier
                    .width(64.dp)
                    .background(NotifTile, RoundedCornerShape(12.dp))
                    .border(1.dp, if (selected) accent else NotifBorder, RoundedCornerShape(12.dp))
                    .clickable { onFilter(value) }
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(pair.first, null, tint = tint, modifier = Modifier.size(20.dp))
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(pair.second),
                    color = tint,
                    fontSize = 11.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun DashedAddRuleButton(accent: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .drawBehind {
                drawRoundRect(
                    color = accent.copy(alpha = 0.85f),
                    cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx()),
                    style = Stroke(
                        width = 1.6.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f),
                    ),
                )
            }
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            stringResource(Res.string.notif_rd_add_rule),
            color = accent,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun RuleCard(
    rule: NotifRuleUi,
    accent: Color,
    onChange: (NotifRuleUi) -> Unit,
    onDelete: (String) -> Unit,
) {
    var condOpen by remember(rule.id) { mutableStateOf(false) }
    var menuOpen by remember(rule.id) { mutableStateOf(false) }
    val catColor = categoryColor(rule.category, accent)
    val catLabel = when (rule.category) {
        NotifCategory.Water -> stringResource(Res.string.notif_rd_cat_water)
        NotifCategory.Power -> stringResource(Res.string.notif_rd_cat_power)
        NotifCategory.Gas -> stringResource(Res.string.notif_rd_cat_gas)
        NotifCategory.Temp -> stringResource(Res.string.notif_rd_cat_temp)
        NotifCategory.Other -> stringResource(Res.string.notif_rd_cat_other)
    }
    val actionLabel = when (rule.action) {
        NotifActionKind.NotifyOnly -> stringResource(Res.string.notif_rd_action_notify)
        NotifActionKind.PumpOn -> stringResource(Res.string.notif_rd_action_pump_on)
        NotifActionKind.PumpOff -> stringResource(Res.string.notif_rd_action_pump_off)
        NotifActionKind.AudioWarn -> stringResource(Res.string.notif_rd_action_audio)
    }
    val unsupported = rule.id in AlertRuleEngine.unsupportedRuleIds
    val condText = buildString {
        append(rule.conditions.getOrElse(rule.conditionIndex) { rule.conditions.firstOrNull().orEmpty() })
        if (rule.metric.isNotBlank()) {
            append(' ')
            append(rule.metric)
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(NotifCard, RoundedCornerShape(14.dp))
            .border(1.dp, NotifBorder, RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(catColor.copy(alpha = 0.16f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(ruleIcon(rule), null, tint = catColor, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(catLabel, color = catColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(
                    rule.title,
                    color = NotifText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    if (unsupported) {
                        stringResource(Res.string.notif_rule_sensor_unavailable)
                    } else {
                        rule.description
                    },
                    color = NotifMuted,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NotifTile, RoundedCornerShape(10.dp))
                        .border(1.dp, NotifBorder, RoundedCornerShape(10.dp))
                        .clickable { condOpen = true }
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        condText,
                        color = NotifText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Icon(Icons.Filled.KeyboardArrowDown, null, tint = NotifMuted, modifier = Modifier.size(18.dp))
                }
                DropdownMenu(expanded = condOpen, onDismissRequest = { condOpen = false }) {
                    rule.conditions.forEachIndexed { index, option ->
                        DropdownMenuItem(onClick = {
                            onChange(rule.copy(conditionIndex = index))
                            condOpen = false
                        }) {
                            Text(if (rule.metric.isBlank()) option else "$option ${rule.metric}")
                        }
                    }
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Filled.PlayArrow, null, tint = accent, modifier = Modifier.size(14.dp))
                    Text(
                        stringResource(Res.string.notif_rd_action_label),
                        color = NotifMuted,
                        fontSize = 11.sp,
                    )
                }
                Text(
                    actionLabel,
                    color = NotifText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Switch(
                checked = rule.enabled && !unsupported,
                enabled = !unsupported,
                onCheckedChange = { onChange(rule.copy(enabled = it)) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = accent,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = NotifMuted.copy(alpha = 0.45f),
                ),
            )
            Box {
                IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Filled.MoreVert, null, tint = NotifMuted, modifier = Modifier.size(18.dp))
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(onClick = {
                        menuOpen = false
                        onDelete(rule.id)
                    }) {
                        Text(stringResource(Res.string.notif_rd_delete))
                    }
                }
            }
        }
    }
}

private fun categoryColor(category: NotifCategory, accent: Color): Color = when (category) {
    NotifCategory.Water -> accent
    NotifCategory.Power -> NotifYellow
    NotifCategory.Gas -> Color(0xFFF59E0B)
    NotifCategory.Temp -> Color(0xFFEF4444)
    NotifCategory.Other -> NotifPurple
}

private fun ruleIcon(rule: NotifRuleUi): ImageVector = when (rule.id) {
    "clean_low", "clean_full", "waste_full", "grey" -> Icons.Filled.WaterDrop
    "temp" -> Icons.Filled.Thermostat
    "gas", "co" -> Icons.Filled.LocalFireDepartment
    "battery", "solar", "inverter" -> Icons.Filled.Bolt
    "humidity" -> Icons.Filled.Sensors
    else -> Icons.Filled.Notifications
}

@Composable
private fun SidePanels(
    rules: List<NotifRuleUi>,
    channelFlags: Int,
    accent: Color,
    radius: androidx.compose.ui.unit.Dp,
    onChannel: (Int, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    scrollable: Boolean = true,
) {
    val active = rules.count { it.enabled }
    val passive = rules.size - active
    val hydroforOn = rules.any { it.enabled && (it.action == NotifActionKind.PumpOn || it.action == NotifActionKind.PumpOff) }
    Column(
        modifier = if (scrollable) {
            modifier.verticalScroll(rememberScrollState())
        } else {
            modifier
        },
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SideCard(stringResource(Res.string.notif_rd_quick_info), radius) {
            InfoStat(stringResource(Res.string.notif_rd_active_rules), "$active", NotifGreen)
            InfoStat(stringResource(Res.string.notif_rd_passive_rules), "$passive", NotifMuted)
            InfoStat(stringResource(Res.string.notif_rd_week_total), "${AlertHistoryStore.load().size}", accent)
        }
        SideCard(stringResource(Res.string.notif_rd_channels), radius) {
            ChannelRow(stringResource(Res.string.notif_rd_ch_app), channelFlags and CHANNEL_APP != 0, accent) {
                onChannel(CHANNEL_APP, it)
            }
            ChannelRow(stringResource(Res.string.notif_rd_ch_audio), channelFlags and CHANNEL_AUDIO != 0, accent) {
                onChannel(CHANNEL_AUDIO, it)
            }
            ChannelRow(stringResource(Res.string.notif_ch_email_inactive), false, accent, enabled = false) {
                /* email channel not wired */
            }
            ChannelRow(stringResource(Res.string.notif_ch_push_inactive), false, accent, enabled = false) {
                /* push / FCM not wired */
            }
        }
        SideCard(stringResource(Res.string.notif_rd_auto_summary), radius) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NotifTile, RoundedCornerShape(10.dp))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(Res.string.notif_rd_hydrofor), color = NotifText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                Text(
                    stringResource(if (hydroforOn) Res.string.notif_rd_active else Res.string.notif_rd_passive),
                    color = if (hydroforOn) NotifGreen else NotifMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun SideCard(title: String, radius: androidx.compose.ui.unit.Dp, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(NotifCard, RoundedCornerShape(radius))
            .border(1.dp, NotifBorder, RoundedCornerShape(radius))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title.uppercase(), color = NotifMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
        content()
    }
}

@Composable
private fun InfoStat(label: String, value: String, color: Color) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = NotifMuted, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Text(value, color = color, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ChannelRow(
    label: String,
    checked: Boolean,
    accent: Color,
    enabled: Boolean = true,
    onChecked: (Boolean) -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = if (enabled) NotifText else NotifMuted, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onChecked,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = accent,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = NotifMuted.copy(alpha = 0.45f),
            ),
        )
    }
}

@Composable
private fun AlarmHistoryPanel(accent: Color, radius: androidx.compose.ui.unit.Dp) {
    val items = remember { AlertHistoryStore.load() }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NotifCard, RoundedCornerShape(radius))
            .border(1.dp, NotifBorder, RoundedCornerShape(radius))
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Filled.History, null, tint = accent, modifier = Modifier.size(18.dp))
            Text(stringResource(Res.string.notif_rd_hist_hint), color = NotifMuted, fontSize = 12.sp)
        }
        if (items.isEmpty()) {
            Text(stringResource(Res.string.notif_rd_hist_empty), color = NotifMuted, fontSize = 13.sp)
        } else {
            items.forEach { entry ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NotifTile, RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(Icons.Filled.Notifications, null, tint = accent, modifier = Modifier.size(18.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(entry.title, color = NotifText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text(
                            AlertHistoryStore.formatTime(entry.epochMs),
                            color = NotifMuted,
                            fontSize = 11.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationSettingsPanel(
    accent: Color,
    radius: androidx.compose.ui.unit.Dp,
    channelFlags: Int = ProfileDisplayPreferences.getNotifChannelFlags(),
    onChannel: (Int, Boolean) -> Unit = { _, _ -> },
) {
    var notifSystem by remember { mutableStateOf(ProfileDisplayPreferences.getNotifSystem()) }
    var notifAlert by remember { mutableStateOf(ProfileDisplayPreferences.getNotifAlert()) }
    var notifDevice by remember { mutableStateOf(ProfileDisplayPreferences.getNotifDevice()) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(NotifCard, RoundedCornerShape(radius))
            .border(1.dp, NotifBorder, RoundedCornerShape(radius))
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 8.dp),
        ) {
            Icon(Icons.Filled.Settings, null, tint = accent, modifier = Modifier.size(18.dp))
            Text(stringResource(Res.string.notif_rd_tab_settings), color = NotifText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
        NotifToggleRow(
            title = stringResource(Res.string.profile_rd_notif_system),
            hint = stringResource(Res.string.profile_rd_notif_system_hint),
            checked = notifSystem,
            accent = accent,
            onChecked = {
                notifSystem = it
                ProfileDisplayPreferences.setNotifSystem(it)
            },
        )
        NotifToggleRow(
            title = stringResource(Res.string.profile_rd_notif_alert),
            hint = stringResource(Res.string.profile_rd_notif_alert_hint),
            checked = notifAlert,
            accent = accent,
            onChecked = {
                notifAlert = it
                ProfileDisplayPreferences.setNotifAlert(it)
            },
        )
        NotifToggleRow(
            title = stringResource(Res.string.profile_rd_notif_device),
            hint = stringResource(Res.string.profile_rd_notif_device_hint),
            checked = notifDevice,
            accent = accent,
            onChecked = {
                notifDevice = it
                ProfileDisplayPreferences.setNotifDevice(it)
            },
        )
        Spacer(Modifier.height(10.dp))
        Text(
            stringResource(Res.string.notif_rd_channels).uppercase(),
            color = NotifMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.6.sp,
        )
        Spacer(Modifier.height(6.dp))
        ChannelRow(stringResource(Res.string.notif_rd_ch_app), channelFlags and CHANNEL_APP != 0, accent) {
            onChannel(CHANNEL_APP, it)
        }
        ChannelRow(stringResource(Res.string.notif_rd_ch_audio), channelFlags and CHANNEL_AUDIO != 0, accent) {
            onChannel(CHANNEL_AUDIO, it)
        }
        ChannelRow(stringResource(Res.string.notif_ch_email_inactive), false, accent, enabled = false) {}
        ChannelRow(stringResource(Res.string.notif_ch_push_inactive), false, accent, enabled = false) {}
    }
}

@Composable
private fun AddRuleDialog(
    accent: Color,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var condition by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        backgroundColor = NotifCard,
        title = {
            Text(stringResource(Res.string.notif_rd_add_title), color = NotifText, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    placeholder = { Text(stringResource(Res.string.notif_rd_add_name), color = NotifMuted) },
                    colors = notifFieldColors(accent),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = condition,
                    onValueChange = { condition = it },
                    singleLine = true,
                    placeholder = { Text(stringResource(Res.string.notif_rd_add_condition), color = NotifMuted) },
                    colors = notifFieldColors(accent),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) onSave(name.trim(), condition.trim().ifBlank { name.trim() })
                },
            ) {
                Text(stringResource(Res.string.notif_rd_add_save), color = accent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.notif_rd_add_cancel), color = NotifMuted)
            }
        },
    )
}

@Composable
private fun notifFieldColors(accent: Color) = TextFieldDefaults.outlinedTextFieldColors(
    textColor = NotifText,
    backgroundColor = NotifTile,
    cursorColor = accent,
    focusedBorderColor = accent.copy(alpha = 0.7f),
    unfocusedBorderColor = NotifBorder,
    placeholderColor = NotifMuted,
)

@Composable
private fun NotifToggleRow(
    title: String,
    hint: String,
    checked: Boolean,
    accent: Color,
    onChecked: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = NotifText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(hint, color = NotifMuted, fontSize = 12.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChecked,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = accent,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = NotifMuted.copy(alpha = 0.45f),
            ),
        )
    }
}

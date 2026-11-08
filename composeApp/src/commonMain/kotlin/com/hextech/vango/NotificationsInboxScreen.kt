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
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import org.jetbrains.compose.resources.stringResource
import testvango.composeapp.generated.resources.Res
import testvango.composeapp.generated.resources.guest_ble_control_sign_in
import testvango.composeapp.generated.resources.guest_ble_control_warning
import testvango.composeapp.generated.resources.guest_ble_notif_dismiss
import testvango.composeapp.generated.resources.guest_ble_notif_title
import testvango.composeapp.generated.resources.notif_inbox_active
import testvango.composeapp.generated.resources.notif_inbox_empty
import testvango.composeapp.generated.resources.notif_inbox_empty_hint
import testvango.composeapp.generated.resources.notif_inbox_live_now
import testvango.composeapp.generated.resources.notif_inbox_recent
import testvango.composeapp.generated.resources.notif_inbox_title
import testvango.composeapp.generated.resources.notif_rd_rule_battery_title
import testvango.composeapp.generated.resources.notif_rd_rule_clean_full_title
import testvango.composeapp.generated.resources.notif_rd_rule_clean_low_title
import testvango.composeapp.generated.resources.notif_rd_rule_grey_title
import testvango.composeapp.generated.resources.notif_rd_rule_temp_title
import testvango.composeapp.generated.resources.notif_rd_rule_waste_title

private val InboxBg = Color(0xFF0B0F14)
private val InboxCard = Color(0xFF14181F)
private val InboxBorder = Color(0xFF2A313C)
private val InboxText = Color(0xFFF2F5F8)
private val InboxMuted = Color(0xFF8B95A5)
private val InboxGreen = Color(0xFF3DDC84)
private val InboxWarn = Color(0xFFFFB020)
private val InboxDanger = Color(0xFFFF6B4A)
private val InboxInfo = Color(0xFF3B82F6)

private enum class InboxSeverity { Info, Warning, Danger }

private data class InboxEvent(
    val id: String,
    val title: String,
    val time: String,
    val icon: ImageVector,
    val severity: InboxSeverity,
    val active: Boolean,
    val unread: Boolean,
    val dismissible: Boolean = false,
    val actionLabel: String? = null,
)

/**
 * Phone inbox: occurred / active alerts (water low, tank full, etc.) — not rule settings.
 */
@Composable
fun NotificationsInboxScreen(
    data: VanGoData,
    onBack: () -> Unit,
    onGuestSignIn: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val linked = isBleGattLinkReady() || isAppRemoteMqttConnected()
    val appConn by appConnectionState.collectAsState()
    val bleLinked = linked || appConn == AppConnectionState.BLE_CONNECTED || isBleGattLinkReady()
    var readTick by remember { mutableIntStateOf(0) }
    val guestTick = GuestBleWarn.dismissedTick
    val sensorAlerts = rememberLiveInboxEvents(data, linked, readTick)
    val guestTitle = stringResource(Res.string.guest_ble_notif_title)
    val guestBody = stringResource(Res.string.guest_ble_control_warning)
    val guestSignIn = stringResource(Res.string.guest_ble_control_sign_in)
    val guestNow = stringResource(Res.string.notif_inbox_live_now)
    val guestEvent = remember(guestTick, bleLinked, guestTitle, guestBody, guestSignIn, guestNow) {
        if (!GuestBleWarn.shouldShow()) {
            null
        } else {
            InboxEvent(
                id = GUEST_BLE_WARN_LIVE_KEY,
                title = "$guestTitle — $guestBody",
                time = guestNow,
                icon = Icons.Filled.Person,
                severity = InboxSeverity.Info,
                active = true,
                unread = GuestBleWarn.isUnread(),
                dismissible = true,
                actionLabel = guestSignIn,
            )
        }
    }
    val active = remember(sensorAlerts, guestEvent) {
        listOfNotNull(guestEvent) + sensorAlerts
    }
    var historyTick by remember { mutableIntStateOf(0) }
    LaunchedEffect(active.map { it.id }) {
        historyTick++
    }
    val recent = remember(historyTick, readTick, guestTick) {
        AlertHistoryStore.load().take(12).map { entry ->
            InboxEvent(
                id = entry.id,
                title = entry.title,
                time = AlertHistoryStore.formatTime(entry.epochMs),
                icon = iconForRule(entry.ruleId),
                severity = InboxSeverity.Info,
                active = false,
                unread = !AlertReadStore.isRead(entry.id),
            )
        }
    }
    val unreadActive = active.filter { it.unread }
    val events = active + recent.filter { r -> active.none { it.id == r.id || it.title == r.title } }

    fun markRead(event: InboxEvent) {
        if (event.id == GUEST_BLE_WARN_LIVE_KEY) {
            GuestBleWarn.markRead()
            readTick++
            historyTick++
            return
        }
        if (!event.unread) return
        AlertReadStore.markRead(event.id)
        readTick++
        historyTick++
    }

    fun dismiss(event: InboxEvent) {
        if (event.id == GUEST_BLE_WARN_LIVE_KEY) {
            GuestBleWarn.dismiss()
            readTick++
            historyTick++
        } else {
            markRead(event)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(InboxBg)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                tint = InboxText,
                modifier = Modifier
                    .size(28.dp)
                    .clickable(onClick = onBack),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                stringResource(Res.string.notif_inbox_title),
                color = InboxText,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            if (unreadActive.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .background(InboxGreen.copy(alpha = 0.18f), RoundedCornerShape(999.dp))
                        .border(1.dp, InboxGreen.copy(alpha = 0.45f), RoundedCornerShape(999.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        stringResource(Res.string.notif_inbox_active, unreadActive.size),
                        color = InboxGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

        if (events.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 40.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.Notifications,
                        contentDescription = null,
                        tint = InboxMuted,
                        modifier = Modifier.size(40.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        stringResource(Res.string.notif_inbox_empty),
                        color = InboxText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stringResource(Res.string.notif_inbox_empty_hint),
                        color = InboxMuted,
                        fontSize = 13.sp,
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (active.isNotEmpty()) {
                    InboxSectionLabel(stringResource(Res.string.notif_inbox_active, unreadActive.size))
                    active.forEach { event ->
                        InboxEventCard(
                            event = event,
                            onClick = { markRead(event) },
                            onDismiss = if (event.dismissible) {
                                { dismiss(event) }
                            } else {
                                null
                            },
                            onAction = if (event.id == GUEST_BLE_WARN_LIVE_KEY && onGuestSignIn != null) {
                                {
                                    GuestBleWarn.markRead()
                                    onGuestSignIn()
                                }
                            } else {
                                null
                            },
                            dismissLabel = stringResource(Res.string.guest_ble_notif_dismiss),
                        )
                    }
                }
                if (recent.isNotEmpty()) {
                    InboxSectionLabel(stringResource(Res.string.notif_inbox_recent))
                    recent.forEach { event ->
                        InboxEventCard(event = event, onClick = { markRead(event) })
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

fun countActiveInboxAlerts(data: VanGoData): Int {
    val linked = isBleGattLinkReady() || isAppRemoteMqttConnected()
    val sensor = AlertRuleEngine.countUnread(data, linked)
    val guest = if (GuestBleWarn.isUnread()) 1 else 0
    return sensor + guest
}

@Composable
private fun rememberLiveInboxEvents(
    data: VanGoData,
    linked: Boolean,
    readTick: Int,
): List<InboxEvent> {
    val now = stringResource(Res.string.notif_inbox_live_now)
    val titleCleanLow = stringResource(Res.string.notif_rd_rule_clean_low_title)
    val titleCleanFull = stringResource(Res.string.notif_rd_rule_clean_full_title)
    val titleGrey = stringResource(Res.string.notif_rd_rule_grey_title)
    val titleWaste = stringResource(Res.string.notif_rd_rule_waste_title)
    val titleTemp = stringResource(Res.string.notif_rd_rule_temp_title)
    val titleBattery = stringResource(Res.string.notif_rd_rule_battery_title)

    val sensorSig = data.sDB.contentHashCode()
    val alerts = remember(data.volt, sensorSig, linked) {
        AlertRuleEngine.evaluateActive(data, linked)
    }
    LaunchedEffect(alerts.map { it.ruleId + it.valueText }) {
        AlertRuleEngine.recordTransitions(alerts) { a ->
            when (a.ruleId) {
                "clean_low" -> "$titleCleanLow · ${a.valueText}"
                "clean_full" -> "$titleCleanFull · ${a.valueText}"
                "grey" -> "$titleGrey · ${a.valueText}"
                "waste_full" -> "$titleWaste · ${a.valueText}"
                "temp" -> "$titleTemp · ${a.valueText}"
                "battery" -> "$titleBattery · ${a.valueText}"
                else -> "${a.ruleId} · ${a.valueText}"
            }
        }
    }

    return remember(
        alerts,
        now,
        readTick,
        titleCleanLow,
        titleCleanFull,
        titleGrey,
        titleWaste,
        titleTemp,
        titleBattery,
    ) {
        alerts.map { a ->
            val key = AlertRuleEngine.liveKey(a.ruleId)
            val title = when (a.ruleId) {
                "clean_low" -> "$titleCleanLow (${a.valueText})"
                "clean_full" -> "$titleCleanFull (${a.valueText})"
                "grey" -> "$titleGrey (${a.valueText})"
                "waste_full" -> "$titleWaste (${a.valueText})"
                "temp" -> "$titleTemp (${a.valueText})"
                "battery" -> "$titleBattery (${a.valueText})"
                else -> "${a.ruleId} ${a.valueText}"
            }
            InboxEvent(
                id = key,
                title = title,
                time = now,
                icon = iconForRule(a.ruleId),
                severity = when (a.severity) {
                    AlertSeverity.Danger -> InboxSeverity.Danger
                    AlertSeverity.Warning -> InboxSeverity.Warning
                    AlertSeverity.Info -> InboxSeverity.Info
                },
                active = true,
                unread = !AlertReadStore.isRead(key),
            )
        }
    }
}

private fun iconForRule(ruleId: String): ImageVector = when (ruleId) {
    "clean_low", "clean_full", "waste_full", "grey" -> Icons.Filled.WaterDrop
    "temp" -> Icons.Filled.Thermostat
    "battery" -> Icons.Filled.BatteryAlert
    GUEST_BLE_WARN_RULE_ID, "guest_ble" -> Icons.Filled.Person
    else -> Icons.Filled.Opacity
}

@Composable
private fun InboxSectionLabel(text: String) {
    Text(
        text.uppercase(),
        color = InboxMuted,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp, start = 2.dp),
    )
}

@Composable
private fun InboxEventCard(
    event: InboxEvent,
    onClick: () -> Unit,
    onDismiss: (() -> Unit)? = null,
    onAction: (() -> Unit)? = null,
    dismissLabel: String = "",
) {
    val accent = when (event.severity) {
        InboxSeverity.Info -> InboxInfo
        InboxSeverity.Warning -> InboxWarn
        InboxSeverity.Danger -> InboxDanger
    }
    val titleWeight = if (event.unread) FontWeight.SemiBold else FontWeight.Medium
    val titleColor = if (event.unread) InboxText else InboxMuted
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (event.unread) InboxCard else InboxCard.copy(alpha = 0.72f),
                RoundedCornerShape(14.dp),
            )
            .border(
                1.dp,
                when {
                    event.unread && event.active -> accent.copy(alpha = 0.45f)
                    event.unread -> InboxBorder.copy(alpha = 0.9f)
                    else -> InboxBorder.copy(alpha = 0.45f)
                },
                RoundedCornerShape(14.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(accent.copy(alpha = if (event.unread) 0.16f else 0.08f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (event.severity == InboxSeverity.Danger && event.unread) {
                        Icons.Filled.Warning
                    } else {
                        event.icon
                    },
                    contentDescription = null,
                    tint = if (event.unread) accent else InboxMuted,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    event.title,
                    color = titleColor,
                    fontSize = 14.sp,
                    fontWeight = titleWeight,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(event.time, color = InboxMuted, fontSize = 12.sp)
            }
            if (onDismiss != null) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = dismissLabel,
                    tint = InboxMuted,
                    modifier = Modifier
                        .size(22.dp)
                        .clickable(onClick = onDismiss),
                )
            } else if (event.unread) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(accent, CircleShape),
                )
            }
        }
        if (onAction != null && !event.actionLabel.isNullOrBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(
                event.actionLabel,
                color = accent,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clickable(onClick = onAction)
                    .padding(start = 54.dp),
            )
        }
    }
}

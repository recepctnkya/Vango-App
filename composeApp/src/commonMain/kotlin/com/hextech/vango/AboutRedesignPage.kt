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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PhoneAndroid
import testvango.composeapp.generated.resources.about_rd_app_version_badge
import testvango.composeapp.generated.resources.about_rd_app_version_row
import testvango.composeapp.generated.resources.about_rd_last_update
import testvango.composeapp.generated.resources.about_rd_manual_quick_sub
import testvango.composeapp.generated.resources.about_rd_manual_user_sub
import testvango.composeapp.generated.resources.about_rd_page_hint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.hextech.vango.profile.LoginPreferences
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import testvango.composeapp.generated.resources.Res
import testvango.composeapp.generated.resources.about_faq_ble_a
import testvango.composeapp.generated.resources.about_faq_ble_q
import testvango.composeapp.generated.resources.about_faq_device_a
import testvango.composeapp.generated.resources.about_faq_device_q
import testvango.composeapp.generated.resources.about_faq_qr_a
import testvango.composeapp.generated.resources.about_faq_qr_q
import testvango.composeapp.generated.resources.about_faq_remote_a
import testvango.composeapp.generated.resources.about_faq_remote_q
import testvango.composeapp.generated.resources.about_rd_app_blurb
import testvango.composeapp.generated.resources.about_rd_app_copyright
import testvango.composeapp.generated.resources.about_rd_app_firmware
import testvango.composeapp.generated.resources.about_rd_app_product
import testvango.composeapp.generated.resources.about_rd_app_release
import testvango.composeapp.generated.resources.about_rd_app_section
import testvango.composeapp.generated.resources.about_rd_app_version
import testvango.composeapp.generated.resources.about_rd_device_name
import testvango.composeapp.generated.resources.about_rd_email_support
import testvango.composeapp.generated.resources.about_rd_faq
import testvango.composeapp.generated.resources.about_rd_last_link
import testvango.composeapp.generated.resources.about_rd_link_ble
import testvango.composeapp.generated.resources.about_rd_link_offline
import testvango.composeapp.generated.resources.about_rd_link_remote
import testvango.composeapp.generated.resources.about_rd_link_type
import testvango.composeapp.generated.resources.about_rd_mac
import testvango.composeapp.generated.resources.about_rd_manual_close
import testvango.composeapp.generated.resources.about_rd_manual_download_all
import testvango.composeapp.generated.resources.about_rd_manual_install
import testvango.composeapp.generated.resources.about_rd_manual_install_body
import testvango.composeapp.generated.resources.about_rd_manual_quick
import testvango.composeapp.generated.resources.about_rd_manual_quick_body
import testvango.composeapp.generated.resources.about_rd_manual_safety
import testvango.composeapp.generated.resources.about_rd_manual_safety_body
import testvango.composeapp.generated.resources.about_rd_manual_user
import testvango.composeapp.generated.resources.about_rd_manual_user_body
import testvango.composeapp.generated.resources.about_rd_manual_view
import testvango.composeapp.generated.resources.about_rd_manuals_section
import testvango.composeapp.generated.resources.about_rd_never
import testvango.composeapp.generated.resources.about_rd_release_value
import testvango.composeapp.generated.resources.about_rd_support_section
import testvango.composeapp.generated.resources.about_rd_system_section
import testvango.composeapp.generated.resources.about_rd_ticket_back
import testvango.composeapp.generated.resources.about_rd_ticket_button
import testvango.composeapp.generated.resources.about_rd_ticket_cancel
import testvango.composeapp.generated.resources.about_rd_ticket_cat_app
import testvango.composeapp.generated.resources.about_rd_ticket_cat_connection
import testvango.composeapp.generated.resources.about_rd_ticket_cat_device
import testvango.composeapp.generated.resources.about_rd_ticket_cat_other
import testvango.composeapp.generated.resources.about_rd_ticket_category
import testvango.composeapp.generated.resources.about_rd_ticket_close
import testvango.composeapp.generated.resources.about_rd_ticket_detail
import testvango.composeapp.generated.resources.about_rd_ticket_detail_ph
import testvango.composeapp.generated.resources.about_rd_ticket_done_body
import testvango.composeapp.generated.resources.about_rd_ticket_done_title
import testvango.composeapp.generated.resources.about_rd_ticket_email
import testvango.composeapp.generated.resources.about_rd_ticket_email_ph
import testvango.composeapp.generated.resources.about_rd_ticket_hint
import testvango.composeapp.generated.resources.about_rd_ticket_mail
import testvango.composeapp.generated.resources.about_rd_ticket_need_fields
import testvango.composeapp.generated.resources.about_rd_ticket_page_sub
import testvango.composeapp.generated.resources.about_rd_ticket_page_title
import testvango.composeapp.generated.resources.about_rd_ticket_send
import testvango.composeapp.generated.resources.about_rd_ticket_subject
import testvango.composeapp.generated.resources.about_rd_ticket_subject_ph
import testvango.composeapp.generated.resources.about_rd_ticket_title
import testvango.composeapp.generated.resources.about_rd_uptime
import testvango.composeapp.generated.resources.about_rd_website
import testvango.composeapp.generated.resources.connection_io_display_name
import testvango.composeapp.generated.resources.settings_about_title
import testvango.composeapp.generated.resources.vango_legacy_logo

private const val SUPPORT_EMAIL = "destek@hexnet.com.tr"
private const val SUPPORT_WEB = "https://www.hexnet.com.tr"
private const val APP_VERSION_CODE = 90

private val AboutCyan = Color(0xFF2EC5FF)
private val AboutInk = Color(0xFF061018)
private val AboutText: Color @Composable get() = LocalAppChrome.current.text
private val AboutMuted: Color @Composable get() = LocalAppChrome.current.muted
private val AboutBorder: Color @Composable get() = LocalAppChrome.current.border
private val AboutTile: Color @Composable get() = LocalAppChrome.current.tile
private val AboutCard: Color @Composable get() = LocalAppChrome.current.card
private val AboutPanel: Color @Composable get() = LocalAppChrome.current.panel

private enum class AboutManualKind { Quick, User, Install, Safety }

private enum class TicketCategory { Connection, Device, App, Other }

private sealed class AboutOverlay {
    data object None : AboutOverlay()
    data object Ticket : AboutOverlay()
    data object Faq : AboutOverlay()
    data class Manual(val kind: AboutManualKind) : AboutOverlay()
}

@Composable
fun AboutRedesignPage() {
    val accent = accentColorFromId(customAppAccentId)
    val radius = appCardCornerRadius()
    val global by vanGoGlobal.collectAsState()
    val lastTelemetryAt by lastSuccessfulTelemetryEpochSec.collectAsState()
    val deviceCount by vangoDeviceListSize.collectAsState()
    var overlay by remember { mutableStateOf<AboutOverlay>(AboutOverlay.None) }

    val bleLinked = remember(global, lastTelemetryAt) { isBleGattLinkReady() }
    val connectedMac = currentBleAdapterMac() ?: LoginPreferences.getCachedLastBleAdapterMac()
    val displayMac = (
        currentPlatformDeviceMac()
            ?: connectedMac
            ?: LoginPreferences.getCachedPairedDeviceMac()
        )?.takeIf { it.isNotBlank() }
        ?: stringResource(Res.string.about_rd_never)
    val ioName = stringResource(Res.string.connection_io_display_name)
    val liveDevice = remember(deviceCount, connectedMac, bleLinked) {
        val mac = connectedMac
        if (mac.isNullOrBlank()) {
            vangoDeviceList.firstOrNull { it.rssi != null && isSupportedBleDeviceName(it.name) }
        } else {
            vangoDeviceList.firstOrNull { macsEqual(it.mac, mac) }
        }
    }
    val deviceName = liveDevice?.name?.takeIf { it.isNotBlank() } ?: ioName
    val lastLinkEpoch = when {
        lastTelemetryAt > 0L -> lastTelemetryAt
        global.receivedPackageTime > 0L -> global.receivedPackageTime
        else -> 0L
    }
    val lastUpdateText = formatAboutTimestamp(lastLinkEpoch).ifBlank {
        stringResource(Res.string.about_rd_never)
    }
    val versionName = VANGO_APP_VERSION_NAME
    val versionLabel = "$versionName ($APP_VERSION_CODE)"

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 14.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(accent.copy(alpha = 0.14f), CircleShape)
                            .border(1.dp, accent.copy(alpha = 0.45f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Info, null, tint = accent, modifier = Modifier.size(20.dp))
                    }
                    Text(
                        stringResource(Res.string.settings_about_title).uppercase(),
                        color = AboutText,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.7.sp,
                    )
                }
                Text(
                    stringResource(Res.string.about_rd_page_hint),
                    color = AboutMuted,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                )
            }

            AboutSectionCard(
                title = stringResource(Res.string.about_rd_app_section),
                showTitle = false,
                radius = radius,
            ) {
                AboutAppBlock(accent = accent, versionName = versionName)
            }
            AboutSectionCard(
                title = stringResource(Res.string.about_rd_manuals_section),
                radius = radius,
            ) {
                AboutManualsBlock(
                    accent = accent,
                    onView = { overlay = AboutOverlay.Manual(it) },
                )
            }
            AboutSectionCard(
                title = stringResource(Res.string.about_rd_support_section),
                radius = radius,
            ) {
                AboutSupportBlock(
                    accent = accent,
                    onTicket = { overlay = AboutOverlay.Ticket },
                    onEmail = {
                        openExternalUrl("mailto:$SUPPORT_EMAIL?subject=${encodeUri("VanGO destek")}")
                    },
                )
            }
            AboutSectionCard(
                title = stringResource(Res.string.about_rd_system_section),
                radius = radius,
            ) {
                AboutSystemBlock(
                    accent = accent,
                    deviceName = deviceName,
                    mac = displayMac,
                    version = versionLabel,
                    lastUpdate = lastUpdateText,
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        when (val pane = overlay) {
            AboutOverlay.None -> Unit
            AboutOverlay.Ticket -> AboutTicketOverlay(
                accent = accent,
                radius = radius,
                deviceName = deviceName,
                mac = displayMac,
                version = versionLabel,
                onClose = { overlay = AboutOverlay.None },
            )
            AboutOverlay.Faq -> AboutFaqOverlay(
                accent = accent,
                radius = radius,
                onClose = { overlay = AboutOverlay.None },
            )
            is AboutOverlay.Manual -> AboutManualOverlay(
                kind = pane.kind,
                accent = accent,
                radius = radius,
                onClose = { overlay = AboutOverlay.None },
            )
        }
    }
}

@Composable
private fun AboutSectionCard(
    title: String,
    modifier: Modifier = Modifier,
    radius: androidx.compose.ui.unit.Dp,
    showTitle: Boolean = true,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(AboutCard, RoundedCornerShape(radius))
            .border(1.dp, AboutBorder, RoundedCornerShape(radius))
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (showTitle) {
            Text(
                title.uppercase(),
                color = AboutMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
            )
        }
        content()
    }
}

@Composable
private fun AboutAppBlock(accent: Color, versionName: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            androidx.compose.foundation.Image(
                painter = painterResource(Res.drawable.vango_legacy_logo),
                contentDescription = null,
                modifier = Modifier.size(72.dp),
            )
            Text(
                "VanGO",
                color = AboutText,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
            )
        }
        Text(
            stringResource(Res.string.about_rd_app_product),
            color = AboutMuted,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
        )
        Text(
            stringResource(Res.string.about_rd_app_blurb),
            color = AboutMuted,
            fontSize = 12.sp,
            lineHeight = 17.sp,
        )
        Box(
            modifier = Modifier
                .background(accent.copy(alpha = 0.16f), RoundedCornerShape(50))
                .border(1.dp, accent.copy(alpha = 0.55f), RoundedCornerShape(50))
                .padding(horizontal = 14.dp, vertical = 6.dp),
        ) {
            Text(
                stringResource(Res.string.about_rd_app_version_badge, versionName),
                color = accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            stringResource(Res.string.about_rd_app_copyright),
            color = AboutMuted,
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun AboutManualsBlock(
    accent: Color,
    onView: (AboutManualKind) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        AboutManualRow(
            icon = Icons.Filled.MenuBook,
            title = stringResource(Res.string.about_rd_manual_quick),
            subtitle = stringResource(Res.string.about_rd_manual_quick_sub),
            accent = accent,
            showDivider = true,
            onView = { onView(AboutManualKind.Quick) },
        )
        AboutManualRow(
            icon = Icons.Filled.Description,
            title = stringResource(Res.string.about_rd_manual_user),
            subtitle = stringResource(Res.string.about_rd_manual_user_sub),
            accent = accent,
            showDivider = false,
            onView = { onView(AboutManualKind.User) },
        )
    }
}

@Composable
private fun AboutManualRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accent: Color,
    showDivider: Boolean,
    onView: () -> Unit,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onView)
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = AboutText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = AboutMuted, fontSize = 12.sp)
            }
            Text(
                stringResource(Res.string.about_rd_manual_view),
                color = accent,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                null,
                tint = accent,
                modifier = Modifier.size(18.dp),
            )
        }
        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(AboutBorder),
            )
        }
    }
}

@Composable
private fun AboutSupportBlock(
    accent: Color,
    onTicket: () -> Unit,
    onEmail: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(accent.copy(alpha = 0.12f), CircleShape)
                    .border(1.dp, accent.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.HeadsetMic, null, tint = accent, modifier = Modifier.size(22.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(Res.string.about_rd_ticket_title),
                    color = AboutText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    stringResource(Res.string.about_rd_ticket_hint),
                    color = AboutMuted,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                )
            }
            Button(
                onClick = onTicket,
                colors = ButtonDefaults.buttonColors(backgroundColor = accent, contentColor = AboutInk),
                shape = RoundedCornerShape(10.dp),
                elevation = ButtonDefaults.elevation(0.dp, 0.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier.height(40.dp),
            ) {
                Text(
                    stringResource(Res.string.about_rd_ticket_button),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    maxLines = 1,
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(AboutBorder),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onEmail)
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Email, null, tint = accent, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(Res.string.about_rd_email_support),
                    color = AboutText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(SUPPORT_EMAIL, color = AboutMuted, fontSize = 12.sp)
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                null,
                tint = AboutMuted,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun AboutSystemBlock(
    accent: Color,
    deviceName: String,
    mac: String,
    version: String,
    lastUpdate: String,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        AboutSystemRow(Icons.Filled.PhoneAndroid, stringResource(Res.string.about_rd_device_name), deviceName, accent, true)
        AboutSystemRow(Icons.Filled.Memory, stringResource(Res.string.about_rd_mac), mac, accent, true)
        AboutSystemRow(Icons.Filled.Info, stringResource(Res.string.about_rd_app_version_row), version, accent, true)
        AboutSystemRow(Icons.Outlined.CalendarToday, stringResource(Res.string.about_rd_last_update), lastUpdate, accent, false)
    }
}

@Composable
private fun AboutSystemRow(
    icon: ImageVector,
    label: String,
    value: String,
    accent: Color,
    showDivider: Boolean,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(20.dp))
            Text(label, color = AboutMuted, fontSize = 13.sp, modifier = Modifier.weight(1f))
            Text(
                value,
                color = AboutText,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(AboutBorder),
            )
        }
    }
}

@Composable
private fun AboutTicketOverlay(
    accent: Color,
    radius: androidx.compose.ui.unit.Dp,
    deviceName: String,
    mac: String,
    version: String,
    onClose: () -> Unit,
) {
    var category by remember { mutableStateOf(TicketCategory.Connection) }
    var subject by remember { mutableStateOf("") }
    var detail by remember { mutableStateOf("") }
    var email by remember { mutableStateOf(LoginPreferences.getRememberedEmail()?.trim().orEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }
    var ticketId by remember { mutableStateOf<String?>(null) }
    val catConnection = stringResource(Res.string.about_rd_ticket_cat_connection)
    val catDevice = stringResource(Res.string.about_rd_ticket_cat_device)
    val catApp = stringResource(Res.string.about_rd_ticket_cat_app)
    val catOther = stringResource(Res.string.about_rd_ticket_cat_other)
    val needFields = stringResource(Res.string.about_rd_ticket_need_fields)
    val categoryLabel = when (category) {
        TicketCategory.Connection -> catConnection
        TicketCategory.Device -> catDevice
        TicketCategory.App -> catApp
        TicketCategory.Other -> catOther
    }

    fun mailUrl(id: String): String {
        val body = buildString {
            appendLine("Etiket: $id")
            appendLine("Kategori: $categoryLabel")
            appendLine("Cihaz: $deviceName")
            appendLine("MAC: $mac")
            appendLine("Uygulama: $version")
            if (email.isNotBlank()) appendLine("İletişim: $email")
            appendLine()
            appendLine(detail.trim())
        }
        return "mailto:$SUPPORT_EMAIL?subject=${encodeUri("[$id] ${subject.trim()}")}&body=${encodeUri(body)}"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AboutCard.copy(alpha = 0.97f))
            .imePadding()
            .padding(18.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxWidth()
                .fillMaxHeight()
                .background(AboutPanel, RoundedCornerShape(radius))
                .border(1.dp, AboutBorder, RoundedCornerShape(radius))
                .padding(20.dp),
        ) {
            val doneId = ticketId
            if (doneId != null) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        stringResource(Res.string.about_rd_ticket_done_title),
                        color = AboutText,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        stringResource(Res.string.about_rd_ticket_done_body, doneId),
                        color = AboutMuted,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                    )
                    Spacer(Modifier.weight(1f))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { openExternalUrl(mailUrl(doneId)) },
                            colors = ButtonDefaults.buttonColors(backgroundColor = accent, contentColor = AboutInk),
                            shape = RoundedCornerShape(10.dp),
                            elevation = ButtonDefaults.elevation(0.dp, 0.dp),
                        ) {
                            Text(stringResource(Res.string.about_rd_ticket_mail), fontWeight = FontWeight.Bold)
                        }
                        TextButton(onClick = onClose) {
                            Text(stringResource(Res.string.about_rd_ticket_close), color = accent)
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        stringResource(Res.string.about_rd_ticket_page_title),
                        color = AboutText,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        stringResource(Res.string.about_rd_ticket_page_sub),
                        color = AboutMuted,
                        fontSize = 13.sp,
                    )
                    Text(
                        stringResource(Res.string.about_rd_ticket_category).uppercase(),
                        color = AboutMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TicketChip(catConnection, category == TicketCategory.Connection, accent) {
                            category = TicketCategory.Connection
                        }
                        TicketChip(catDevice, category == TicketCategory.Device, accent) {
                            category = TicketCategory.Device
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TicketChip(catApp, category == TicketCategory.App, accent) {
                            category = TicketCategory.App
                        }
                        TicketChip(catOther, category == TicketCategory.Other, accent) {
                            category = TicketCategory.Other
                        }
                    }
                    OutlinedTextField(
                        value = subject,
                        onValueChange = {
                            subject = it
                            error = null
                        },
                        singleLine = true,
                        label = { Text(stringResource(Res.string.about_rd_ticket_subject)) },
                        placeholder = { Text(stringResource(Res.string.about_rd_ticket_subject_ph)) },
                        colors = aboutFieldColors(accent),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = detail,
                        onValueChange = {
                            detail = it
                            error = null
                        },
                        label = { Text(stringResource(Res.string.about_rd_ticket_detail)) },
                        placeholder = { Text(stringResource(Res.string.about_rd_ticket_detail_ph)) },
                        colors = aboutFieldColors(accent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 140.dp),
                        maxLines = 8,
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        singleLine = true,
                        label = { Text(stringResource(Res.string.about_rd_ticket_email)) },
                        placeholder = { Text(stringResource(Res.string.about_rd_ticket_email_ph)) },
                        colors = aboutFieldColors(accent),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (error != null) {
                        Text(error.orEmpty(), color = Color(0xFFFF6B6B), fontSize = 12.sp)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                if (subject.isBlank() || detail.isBlank()) {
                                    error = needFields
                                } else {
                                    ticketId = newTicketId()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(backgroundColor = accent, contentColor = AboutInk),
                            shape = RoundedCornerShape(10.dp),
                            elevation = ButtonDefaults.elevation(0.dp, 0.dp),
                        ) {
                            Text(stringResource(Res.string.about_rd_ticket_send), fontWeight = FontWeight.Bold)
                        }
                        TextButton(onClick = onClose) {
                            Text(stringResource(Res.string.about_rd_ticket_cancel), color = AboutMuted)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TicketChip(label: String, selected: Boolean, accent: Color, onClick: () -> Unit) {
    val bg = if (selected) accent.copy(alpha = 0.22f) else AboutTile
    val border = if (selected) accent else AboutBorder
    val fg = if (selected) accent else AboutText
    Box(
        modifier = Modifier
            .background(bg, RoundedCornerShape(20.dp))
            .border(1.dp, border, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(label, color = fg, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun AboutFaqOverlay(
    accent: Color,
    radius: androidx.compose.ui.unit.Dp,
    onClose: () -> Unit,
) {
    val items = listOf(
        stringResource(Res.string.about_faq_device_q) to stringResource(Res.string.about_faq_device_a),
        stringResource(Res.string.about_faq_ble_q) to stringResource(Res.string.about_faq_ble_a),
        stringResource(Res.string.about_faq_remote_q) to stringResource(Res.string.about_faq_remote_a),
        stringResource(Res.string.about_faq_qr_q) to stringResource(Res.string.about_faq_qr_a),
    )
    var openIndex by remember { mutableStateOf(0) }
    OverlaySheet(
        title = stringResource(Res.string.about_rd_faq),
        accent = accent,
        radius = radius,
        onClose = onClose,
    ) {
        items.forEachIndexed { index, (q, a) ->
            val open = openIndex == index
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AboutTile, RoundedCornerShape(10.dp))
                    .clickable { openIndex = if (open) -1 else index }
                    .padding(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        q,
                        color = AboutText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        if (open) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        null,
                        tint = accent,
                    )
                }
                if (open) {
                    Spacer(Modifier.height(8.dp))
                    Text(a, color = AboutMuted, fontSize = 13.sp, lineHeight = 18.sp)
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun AboutManualOverlay(
    kind: AboutManualKind,
    accent: Color,
    radius: androidx.compose.ui.unit.Dp,
    onClose: () -> Unit,
) {
    val title = when (kind) {
        AboutManualKind.Quick -> stringResource(Res.string.about_rd_manual_quick)
        AboutManualKind.User -> stringResource(Res.string.about_rd_manual_user)
        AboutManualKind.Install -> stringResource(Res.string.about_rd_manual_install)
        AboutManualKind.Safety -> stringResource(Res.string.about_rd_manual_safety)
    }
    val body = when (kind) {
        AboutManualKind.Quick -> stringResource(Res.string.about_rd_manual_quick_body)
        AboutManualKind.User -> stringResource(Res.string.about_rd_manual_user_body)
        AboutManualKind.Install -> stringResource(Res.string.about_rd_manual_install_body)
        AboutManualKind.Safety -> stringResource(Res.string.about_rd_manual_safety_body)
    }
    OverlaySheet(title = title, accent = accent, radius = radius, onClose = onClose) {
        Text(body, color = AboutMuted, fontSize = 14.sp, lineHeight = 21.sp)
    }
}

@Composable
private fun OverlaySheet(
    title: String,
    accent: Color,
    radius: androidx.compose.ui.unit.Dp,
    onClose: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AboutCard.copy(alpha = 0.97f))
            .padding(18.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxWidth()
                .fillMaxHeight()
                .background(AboutPanel, RoundedCornerShape(radius))
                .border(1.dp, AboutBorder, RoundedCornerShape(radius))
                .padding(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    title,
                    color = AboutText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onClose) {
                    Text(stringResource(Res.string.about_rd_ticket_back), color = accent)
                }
            }
            Spacer(Modifier.height(12.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                content()
            }
            TextButton(onClick = onClose, modifier = Modifier.align(Alignment.End)) {
                Text(stringResource(Res.string.about_rd_manual_close), color = accent)
            }
        }
    }
}

@Composable
private fun aboutFieldColors(accent: Color) = TextFieldDefaults.outlinedTextFieldColors(
    textColor = AboutText,
    backgroundColor = AboutTile,
    cursorColor = accent,
    focusedBorderColor = accent.copy(alpha = 0.7f),
    unfocusedBorderColor = AboutBorder,
    placeholderColor = AboutMuted,
    focusedLabelColor = accent,
    unfocusedLabelColor = AboutMuted,
)

private fun formatAboutTimestamp(epochSec: Long): String {
    if (epochSec <= 0L) return ""
    val dt = Instant.fromEpochSeconds(epochSec).toLocalDateTime(TimeZone.currentSystemDefault())
    val d = dt.dayOfMonth.toString().padStart(2, '0')
    val m = dt.monthNumber.toString().padStart(2, '0')
    val y = dt.year.toString().padStart(4, '0')
    val h = dt.hour.toString().padStart(2, '0')
    val min = dt.minute.toString().padStart(2, '0')
    return "$d.$m.$y $h:$min"
}

private fun newTicketId(): String {
    val dt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    val stamp = "${dt.year}${dt.monthNumber.toString().padStart(2, '0')}${dt.dayOfMonth.toString().padStart(2, '0')}"
    val seq = (dt.hour * 3600 + dt.minute * 60 + dt.second) % 1000
    return "HX-$stamp-${seq.toString().padStart(3, '0')}"
}

private fun encodeUri(value: String): String = buildString(value.length) {
    for (ch in value) {
        when (ch) {
            ' ' -> append("%20")
            '\n' -> append("%0A")
            '\r' -> Unit
            '&' -> append("%26")
            '?' -> append("%3F")
            '=' -> append("%3D")
            '#' -> append("%23")
            else -> append(ch)
        }
    }
}

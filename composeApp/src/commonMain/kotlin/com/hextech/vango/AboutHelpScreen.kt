package com.hextech.vango

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.AlertDialog
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hextech.vango.profile.HexnetApi
import com.hextech.vango.profile.LoginPreferences
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import testvango.composeapp.generated.resources.Res
import testvango.composeapp.generated.resources.about_brand_subtitle
import testvango.composeapp.generated.resources.about_company_name
import testvango.composeapp.generated.resources.about_company_tagline
import testvango.composeapp.generated.resources.about_faq_ble_a
import testvango.composeapp.generated.resources.about_faq_ble_q
import testvango.composeapp.generated.resources.about_faq_device_a
import testvango.composeapp.generated.resources.about_faq_device_q
import testvango.composeapp.generated.resources.about_faq_qr_a
import testvango.composeapp.generated.resources.about_faq_qr_q
import testvango.composeapp.generated.resources.about_faq_remote_a
import testvango.composeapp.generated.resources.about_faq_remote_q
import testvango.composeapp.generated.resources.about_feedback_subject
import testvango.composeapp.generated.resources.about_help_ble_desc
import testvango.composeapp.generated.resources.about_help_ble_title
import testvango.composeapp.generated.resources.about_help_panel_desc
import testvango.composeapp.generated.resources.about_help_panel_title
import testvango.composeapp.generated.resources.about_help_qr_desc
import testvango.composeapp.generated.resources.about_help_qr_title
import testvango.composeapp.generated.resources.about_help_remote_desc
import testvango.composeapp.generated.resources.about_help_remote_title
import testvango.composeapp.generated.resources.about_hero_intro
import testvango.composeapp.generated.resources.about_info_dialog_message
import testvango.composeapp.generated.resources.about_info_dialog_ok
import testvango.composeapp.generated.resources.about_info_dialog_title
import testvango.composeapp.generated.resources.about_qr_dialog_message
import testvango.composeapp.generated.resources.about_qr_dialog_ok
import testvango.composeapp.generated.resources.about_qr_dialog_title
import testvango.composeapp.generated.resources.about_quick_action_feedback_desc
import testvango.composeapp.generated.resources.about_quick_action_feedback_title
import testvango.composeapp.generated.resources.about_quick_action_rescan_desc
import testvango.composeapp.generated.resources.about_quick_action_rescan_title
import testvango.composeapp.generated.resources.about_quick_action_show_qr_desc
import testvango.composeapp.generated.resources.about_quick_action_show_qr_title
import testvango.composeapp.generated.resources.about_quick_actions_title
import testvango.composeapp.generated.resources.about_section_faq
import testvango.composeapp.generated.resources.about_section_quick_help
import testvango.composeapp.generated.resources.about_section_support
import testvango.composeapp.generated.resources.about_support_email_desc
import testvango.composeapp.generated.resources.about_support_email_title
import testvango.composeapp.generated.resources.about_support_manual_desc
import testvango.composeapp.generated.resources.about_support_manual_title
import testvango.composeapp.generated.resources.about_support_web_desc
import testvango.composeapp.generated.resources.about_support_web_title
import testvango.composeapp.generated.resources.about_support_whatsapp_desc
import testvango.composeapp.generated.resources.about_support_whatsapp_title
import testvango.composeapp.generated.resources.hexnet
import testvango.composeapp.generated.resources.settings_about_page_title
import testvango.composeapp.generated.resources.vango_legacy_logo

private const val SUPPORT_EMAIL = "destek@hexnet.com.tr"
private const val SUPPORT_WEB = "https://www.hexnet.com.tr"
private const val WHATSAPP_URL = "https://wa.me/?text=VanGO%20destek%20talebi"

@Composable
fun AboutHelpSetting() {
    var showInfoDialog by remember { mutableStateOf(false) }
    var showQrDialog by remember { mutableStateOf(false) }
    val feedbackSubject = stringResource(Res.string.about_feedback_subject)
    val mailSubject = encodeUri(feedbackSubject)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        SettingsBackHeader(
            backLabel = settingsBackLabelFor(SettingsPage.About),
            pageTitle = settingsHeaderTitleFor(SettingsPage.About),
            onBack = { backToMenuGrid() },
            titleColor = MaterialTheme.colors.primary,
            trailing = {
                Icon(
                    Icons.Filled.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colors.primary,
                    modifier = Modifier
                        .size(28.dp)
                        .clickable { showInfoDialog = true },
                )
            },
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            AboutHeroSection()
            AboutQuickHelpSection(
                onBle = { navigateToMenuPage(SettingsPage.Connection) },
                onRemote = { navigateToMenuPage(SettingsPage.Remote) },
                onQr = { navigateToMenuPage(SettingsPage.ManualDeviceAdd) },
                onPanel = { navigateToMenuPage(SettingsPage.Panel) },
            )
            AboutFaqSection()
            AboutSupportSection(
                onWhatsApp = { openExternalUrl(WHATSAPP_URL) },
                onEmail = { openExternalUrl("mailto:$SUPPORT_EMAIL?subject=$mailSubject") },
                onManual = { openExternalUrl(HexnetApi.PANEL_BASE) },
                onWeb = { openExternalUrl(SUPPORT_WEB) },
            )
            AboutQuickActionsSection(
                onShowQr = { showQrDialog = true },
                onRescan = {
                    getVanGO().discoverVangos()
                    navigateToMenuPage(SettingsPage.BlePair)
                },
                onFeedback = { openExternalUrl("mailto:$SUPPORT_EMAIL?subject=$mailSubject&body=") },
            )
            AboutFooterSection()
            Spacer(Modifier.height(12.dp))
        }
    }

    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = { Text(stringResource(Res.string.about_info_dialog_title)) },
            text = { Text(stringResource(Res.string.about_info_dialog_message)) },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) {
                    Text(stringResource(Res.string.about_info_dialog_ok))
                }
            },
        )
    }
    if (showQrDialog) {
        val uid = LoginPreferences.getCachedDeviceUid()?.takeIf { it.isNotBlank() } ?: "—"
        val mac = LoginPreferences.getCachedPairedDeviceMac()?.takeIf { it.isNotBlank() }
            ?: currentPlatformDeviceMac()
            ?: "—"
        AlertDialog(
            onDismissRequest = { showQrDialog = false },
            title = { Text(stringResource(Res.string.about_qr_dialog_title)) },
            text = {
                Text(
                    stringResource(Res.string.about_qr_dialog_message, uid, mac),
                )
            },
            confirmButton = {
                TextButton(onClick = { showQrDialog = false }) {
                    Text(stringResource(Res.string.about_qr_dialog_ok))
                }
            },
        )
    }
}

private fun encodeUri(value: String): String =
    value.replace(" ", "%20")

@Composable
private fun AboutHeroSection() {
    val brandBlue = Color(0xFF24A2FF)
    val panelBg = Color(0xFF071426)
    val panelBorder = Color(0xFF112841)
    val textPrimary = Color(0xFFF4F8FF)
    val textSecondary = Color(0xFF7FC4FF)
    val textMuted = Color(0xFFB8C7D9)
    val lineColor = brandBlue.copy(alpha = 0.72f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        elevation = 0.dp,
        backgroundColor = panelBg,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                androidx.compose.foundation.Image(
                    painter = painterResource(Res.drawable.vango_legacy_logo),
                    contentDescription = null,
                    modifier = Modifier.size(68.dp),
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "VanGO",
                        style = MaterialTheme.typography.h4,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary,
                        letterSpacing = 1.sp,
                    )
                    Text(
                        stringResource(Res.string.about_brand_subtitle),
                        style = MaterialTheme.typography.caption,
                        color = textSecondary,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(112.dp)
            ) {
                CaravanLineArt(lineColor = lineColor, modifier = Modifier.fillMaxSize())
            }
            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(Res.string.about_hero_intro),
                style = MaterialTheme.typography.body2,
                textAlign = TextAlign.Start,
                color = textMuted,
                lineHeight = 20.sp,
            )
        }
    }
}

@Composable
private fun CaravanLineArt(lineColor: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = 2.2f
        val horizonY = h * 0.84f

        drawLine(
            color = lineColor.copy(alpha = 0.22f),
            start = Offset(0f, 1f),
            end = Offset(w, 1f),
            strokeWidth = 1f,
        )

        val mountainBack = Path().apply {
            moveTo(0f, horizonY)
            lineTo(w * 0.14f, h * 0.52f)
            lineTo(w * 0.24f, horizonY)
            lineTo(w * 0.35f, h * 0.6f)
            lineTo(w * 0.48f, horizonY)
            lineTo(w * 0.72f, h * 0.5f)
            lineTo(w * 0.86f, horizonY)
            lineTo(w, h * 0.64f)
        }
        drawPath(mountainBack, lineColor.copy(alpha = 0.16f), style = androidx.compose.ui.graphics.drawscope.Stroke(1.5f))

        val ground = Path().apply {
            moveTo(0f, horizonY)
            quadraticBezierTo(w * 0.5f, h * 0.68f, w, horizonY)
        }
        drawPath(ground, lineColor.copy(alpha = 0.4f), style = androidx.compose.ui.graphics.drawscope.Stroke(1.8f))

        fun drawPine(centerX: Float, scale: Float) {
            val trunkBottom = horizonY - h * 0.01f
            val trunkTop = trunkBottom - h * 0.2f * scale
            val tree = Path().apply {
                moveTo(centerX, trunkTop)
                lineTo(centerX - w * 0.035f * scale, trunkBottom)
                lineTo(centerX + w * 0.035f * scale, trunkBottom)
                close()
                moveTo(centerX, trunkTop + h * 0.06f * scale)
                lineTo(centerX - w * 0.05f * scale, trunkBottom - h * 0.03f)
                lineTo(centerX + w * 0.05f * scale, trunkBottom - h * 0.03f)
                close()
            }
            drawPath(tree, lineColor, style = androidx.compose.ui.graphics.drawscope.Stroke(stroke))
            drawLine(
                color = lineColor,
                start = Offset(centerX, trunkBottom),
                end = Offset(centerX, trunkBottom + h * 0.06f * scale),
                strokeWidth = stroke,
            )
        }

        drawPine(w * 0.14f, 1f)
        drawPine(w * 0.82f, 0.95f)

        val bodyLeft = w * 0.31f
        val bodyRight = w * 0.69f
        val bodyTop = h * 0.41f
        val bodyBottom = horizonY - h * 0.065f
        val bodyWidth = bodyRight - bodyLeft
        val bodyHeight = bodyBottom - bodyTop

        val caravanOutline = Path().apply {
            moveTo(bodyLeft + bodyWidth * 0.16f, bodyTop)
            quadraticTo(bodyLeft + bodyWidth * 0.02f, bodyTop + bodyHeight * 0.02f, bodyLeft, bodyTop + bodyHeight * 0.28f)
            lineTo(bodyLeft, bodyBottom - bodyHeight * 0.08f)
            quadraticTo(bodyLeft + bodyWidth * 0.01f, bodyBottom, bodyLeft + bodyWidth * 0.08f, bodyBottom)
            lineTo(bodyRight - bodyWidth * 0.08f, bodyBottom)
            quadraticTo(bodyRight, bodyBottom, bodyRight, bodyBottom - bodyHeight * 0.12f)
            lineTo(bodyRight, bodyTop + bodyHeight * 0.24f)
            quadraticTo(bodyRight - bodyWidth * 0.02f, bodyTop, bodyRight - bodyWidth * 0.13f, bodyTop)
            close()
        }
        drawPath(caravanOutline, lineColor, style = androidx.compose.ui.graphics.drawscope.Stroke(stroke))

        drawRoundRect(
            color = lineColor.copy(alpha = 0.9f),
            topLeft = Offset(bodyLeft + bodyWidth * 0.1f, bodyTop + bodyHeight * 0.2f),
            size = androidx.compose.ui.geometry.Size(bodyWidth * 0.1f, bodyHeight * 0.42f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(stroke * 0.62f),
        )
        drawRoundRect(
            color = lineColor.copy(alpha = 0.9f),
            topLeft = Offset(bodyLeft + bodyWidth * 0.45f, bodyTop + bodyHeight * 0.2f),
            size = androidx.compose.ui.geometry.Size(bodyWidth * 0.28f, bodyHeight * 0.2f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(stroke * 0.62f),
        )
        drawLine(
            color = lineColor.copy(alpha = 0.8f),
            start = Offset(bodyLeft + bodyWidth * 0.1f, bodyBottom - bodyHeight * 0.13f),
            end = Offset(bodyRight - bodyWidth * 0.08f, bodyBottom - bodyHeight * 0.13f),
            strokeWidth = stroke * 0.6f,
        )

        val wheelR = h * 0.066f
        val leftWheel = Offset(bodyLeft + bodyWidth * 0.27f, bodyBottom + wheelR * 0.62f)
        val rightWheel = Offset(bodyLeft + bodyWidth * 0.74f, bodyBottom + wheelR * 0.62f)
        drawCircle(lineColor, wheelR, center = leftWheel, style = androidx.compose.ui.graphics.drawscope.Stroke(stroke))
        drawCircle(lineColor, wheelR, center = rightWheel, style = androidx.compose.ui.graphics.drawscope.Stroke(stroke))
        drawLine(
            lineColor,
            Offset(bodyLeft - bodyWidth * 0.08f, bodyBottom - bodyHeight * 0.03f),
            Offset(bodyLeft, bodyBottom - bodyHeight * 0.03f),
            strokeWidth = stroke * 0.85f,
        )
        drawCircle(lineColor, radius = 3f, center = Offset(w * 0.19f, h * 0.23f))
        drawCircle(lineColor.copy(alpha = 0.7f), radius = 2.2f, center = Offset(w * 0.27f, h * 0.14f))
        drawCircle(lineColor.copy(alpha = 0.55f), radius = 1.8f, center = Offset(w * 0.73f, h * 0.18f))
        drawCircle(lineColor.copy(alpha = 0.45f), radius = 1.6f, center = Offset(w * 0.9f, h * 0.26f))
    }
}

@Composable
private fun AboutQuickHelpSection(
    onBle: () -> Unit,
    onRemote: () -> Unit,
    onQr: () -> Unit,
    onPanel: () -> Unit,
) {
    AboutSectionTitle(stringResource(Res.string.about_section_quick_help))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AboutHelpTile(
            icon = Icons.Filled.Bluetooth,
            title = stringResource(Res.string.about_help_ble_title),
            desc = stringResource(Res.string.about_help_ble_desc),
            onClick = onBle,
        )
        AboutHelpTile(
            icon = Icons.Filled.Router,
            title = stringResource(Res.string.about_help_remote_title),
            desc = stringResource(Res.string.about_help_remote_desc),
            onClick = onRemote,
        )
        AboutHelpTile(
            icon = Icons.Filled.QrCodeScanner,
            title = stringResource(Res.string.about_help_qr_title),
            desc = stringResource(Res.string.about_help_qr_desc),
            onClick = onQr,
        )
        AboutHelpTile(
            icon = Icons.Filled.Settings,
            title = stringResource(Res.string.about_help_panel_title),
            desc = stringResource(Res.string.about_help_panel_desc),
            onClick = onPanel,
        )
    }
}

@Composable
private fun AboutFaqSection() {
    AboutSectionTitle(stringResource(Res.string.about_section_faq))
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AboutFaqItem(
            question = stringResource(Res.string.about_faq_device_q),
            answer = stringResource(Res.string.about_faq_device_a),
        )
        AboutFaqItem(
            question = stringResource(Res.string.about_faq_ble_q),
            answer = stringResource(Res.string.about_faq_ble_a),
        )
        AboutFaqItem(
            question = stringResource(Res.string.about_faq_remote_q),
            answer = stringResource(Res.string.about_faq_remote_a),
            leadingIcon = Icons.Filled.Wifi,
        )
        AboutFaqItem(
            question = stringResource(Res.string.about_faq_qr_q),
            answer = stringResource(Res.string.about_faq_qr_a),
        )
    }
}

@Composable
private fun AboutSupportSection(
    onWhatsApp: () -> Unit,
    onEmail: () -> Unit,
    onManual: () -> Unit,
    onWeb: () -> Unit,
) {
    AboutSectionTitle(stringResource(Res.string.about_section_support))
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        AboutSupportCard(
            icon = Icons.Filled.Chat,
            title = stringResource(Res.string.about_support_whatsapp_title),
            desc = stringResource(Res.string.about_support_whatsapp_desc),
            onClick = onWhatsApp,
        )
        AboutSupportCard(
            icon = Icons.Filled.Email,
            title = stringResource(Res.string.about_support_email_title),
            desc = stringResource(Res.string.about_support_email_desc),
            onClick = onEmail,
        )
        AboutSupportCard(
            icon = Icons.Filled.MenuBook,
            title = stringResource(Res.string.about_support_manual_title),
            desc = stringResource(Res.string.about_support_manual_desc),
            onClick = onManual,
        )
        AboutSupportCard(
            icon = Icons.Filled.Language,
            title = stringResource(Res.string.about_support_web_title),
            desc = stringResource(Res.string.about_support_web_desc),
            onClick = onWeb,
        )
    }
}

@Composable
private fun AboutQuickActionsSection(
    onShowQr: () -> Unit,
    onRescan: () -> Unit,
    onFeedback: () -> Unit,
) {
    AboutSectionTitle(stringResource(Res.string.about_quick_actions_title))
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AboutQuickActionRow(
            icon = Icons.Filled.QrCode,
            title = stringResource(Res.string.about_quick_action_show_qr_title),
            desc = stringResource(Res.string.about_quick_action_show_qr_desc),
            onClick = onShowQr,
        )
        AboutQuickActionRow(
            icon = Icons.Filled.Refresh,
            title = stringResource(Res.string.about_quick_action_rescan_title),
            desc = stringResource(Res.string.about_quick_action_rescan_desc),
            onClick = onRescan,
        )
        AboutQuickActionRow(
            icon = Icons.Filled.Chat,
            title = stringResource(Res.string.about_quick_action_feedback_title),
            desc = stringResource(Res.string.about_quick_action_feedback_desc),
            onClick = onFeedback,
        )
    }
}

@Composable
private fun AboutFooterSection() {
    val versionLabel = remember {
        val v = VANGO_APP_VERSION_NAME
        if (v.startsWith("v", ignoreCase = true)) v else "v$v"
    }
    Card(
        shape = RoundedCornerShape(14.dp),
        elevation = 2.dp,
        backgroundColor = MaterialTheme.colors.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            androidx.compose.foundation.Image(
                painter = painterResource(Res.drawable.hexnet),
                contentDescription = null,
                modifier = Modifier.size(40.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(Res.string.about_company_name),
                    style = MaterialTheme.typography.subtitle2,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    stringResource(Res.string.about_company_tagline),
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.65f),
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colors.primary.copy(alpha = 0.12f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(
                    versionLabel,
                    style = MaterialTheme.typography.caption,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colors.primary,
                )
            }
        }
    }
}

@Composable
private fun AboutSectionTitle(title: String) {
    Text(
        title.uppercase(),
        style = MaterialTheme.typography.overline,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colors.primary,
        letterSpacing = 1.sp,
    )
}

@Composable
private fun AboutHelpTile(
    icon: ImageVector,
    title: String,
    desc: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        elevation = 3.dp,
        backgroundColor = MaterialTheme.colors.surface,
    ) {
        Column(
            Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colors.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = MaterialTheme.colors.primary, modifier = Modifier.size(22.dp))
            }
            Text(title, style = MaterialTheme.typography.subtitle2, fontWeight = FontWeight.SemiBold, maxLines = 2)
            Text(
                desc,
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.65f),
                maxLines = 3,
            )
        }
    }
}

@Composable
private fun AboutFaqItem(
    question: String,
    answer: String,
    leadingIcon: ImageVector? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(12.dp),
        elevation = 2.dp,
        backgroundColor = MaterialTheme.colors.surface,
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (leadingIcon != null) {
                    Icon(
                        leadingIcon,
                        null,
                        tint = MaterialTheme.colors.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    question,
                    style = MaterialTheme.typography.body2,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    null,
                    tint = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                )
            }
            if (expanded) {
                Spacer(Modifier.height(8.dp))
                Text(
                    answer,
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.75f),
                    lineHeight = 18.sp,
                )
            }
        }
    }
}

@Composable
private fun AboutSupportCard(
    icon: ImageVector,
    title: String,
    desc: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        elevation = 3.dp,
        backgroundColor = MaterialTheme.colors.surface,
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colors.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = MaterialTheme.colors.primary)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.subtitle2, fontWeight = FontWeight.SemiBold)
                Text(
                    desc,
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.65f),
                )
            }
            Icon(
                Icons.Filled.ChevronRight,
                null,
                tint = MaterialTheme.colors.onSurface.copy(alpha = 0.35f),
            )
        }
    }
}

@Composable
private fun AboutQuickActionRow(
    icon: ImageVector,
    title: String,
    desc: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = 2.dp,
        backgroundColor = MaterialTheme.colors.surface.copy(alpha = 0.85f),
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, null, tint = MaterialTheme.colors.primary, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.body2, fontWeight = FontWeight.Medium)
                Text(
                    desc,
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                )
            }
            Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colors.onSurface.copy(alpha = 0.35f))
        }
    }
}

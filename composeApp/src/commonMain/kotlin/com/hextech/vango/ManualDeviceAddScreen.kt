package com.hextech.vango

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Tag
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.hextech.vango.profile.DeviceUidLookupApi
import com.hextech.vango.profile.DuplicateManualDeviceMacException
import com.hextech.vango.profile.ManualPairedDeviceStore
import com.hextech.vango.profile.ParsedDeviceQr
import com.hextech.vango.profile.DeviceQrScannerView
import com.hextech.vango.profile.formatMacAddressInputColonsWithCursor
import com.hextech.vango.profile.isValidMacFormat
import com.hextech.vango.profile.normalizeMacForDisplay
import com.hextech.vango.profile.parseDeviceQrContent
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import testvango.composeapp.generated.resources.Res
import testvango.composeapp.generated.resources.manual_add_add_device
import testvango.composeapp.generated.resources.manual_add_button
import testvango.composeapp.generated.resources.manual_add_duplicate_mac
import testvango.composeapp.generated.resources.manual_add_invalid_mac
import testvango.composeapp.generated.resources.manual_add_mac_helper
import testvango.composeapp.generated.resources.manual_add_mac_hint
import testvango.composeapp.generated.resources.manual_add_mac_label
import testvango.composeapp.generated.resources.manual_add_method_enter
import testvango.composeapp.generated.resources.manual_add_method_qr
import testvango.composeapp.generated.resources.manual_add_method_uid
import testvango.composeapp.generated.resources.manual_add_note
import testvango.composeapp.generated.resources.manual_add_qr_result_subtitle
import testvango.composeapp.generated.resources.manual_add_qr_result_title
import testvango.composeapp.generated.resources.manual_add_qr_flash
import testvango.composeapp.generated.resources.manual_add_qr_how_title
import testvango.composeapp.generated.resources.manual_add_qr_manual_link
import testvango.composeapp.generated.resources.manual_add_qr_step1
import testvango.composeapp.generated.resources.manual_add_qr_step2
import testvango.composeapp.generated.resources.manual_add_qr_step3
import testvango.composeapp.generated.resources.manual_add_qr_subtitle
import testvango.composeapp.generated.resources.manual_add_qr_tip
import testvango.composeapp.generated.resources.manual_add_qr_title
import testvango.composeapp.generated.resources.manual_add_subtitle
import testvango.composeapp.generated.resources.manual_add_title
import testvango.composeapp.generated.resources.manual_add_uid_hint
import testvango.composeapp.generated.resources.manual_add_uid_label
import testvango.composeapp.generated.resources.manual_add_where_info_body
import testvango.composeapp.generated.resources.manual_add_where_info_title

private enum class ManualAddTab { Mac, Qr, Uid }

private data class ManualUiColors(
    val accent: Color,
    val pageBg: Color,
    val cardBg: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val border: Color,
    val infoBg: Color,
    val noteBg: Color,
    val noteBorder: Color,
    val noteIcon: Color,
    val cameraPreviewBg: Color,
    val qrTipIconBg: Color,
)

@Composable
private fun manualUiColors(): ManualUiColors {
    val theme = MaterialTheme.colors
    val light = theme.isLight
    return ManualUiColors(
        accent = theme.primary,
        pageBg = theme.background,
        cardBg = theme.surface,
        textPrimary = theme.onBackground,
        textSecondary = theme.onBackground.copy(alpha = 0.62f),
        border = theme.onBackground.copy(alpha = if (light) 0.1f else 0.2f),
        infoBg = theme.primary.copy(alpha = if (light) 0.09f else 0.18f),
        noteBg = if (light) Color(0xFFFEF9C3) else Color(0xFF3D3508),
        noteBorder = if (light) Color(0xFFFDE047) else Color(0xFF854D0E),
        noteIcon = Color(0xFFCA8A04),
        cameraPreviewBg = Color(0xFF0A0A0A),
        qrTipIconBg = if (light) Color(0xFF1E293B) else Color(0xFF334155),
    )
}

@Composable
fun ManualDeviceAddSetting() {
    ManualDeviceAddScreen(
        onBack = { navigateToMenuPage(SettingsPage.BlePair) },
        onDeviceAdded = { navigateToMenuPage(SettingsPage.BlePair) },
    )
}

@Composable
fun ManualDeviceAddScreen(
    onBack: () -> Unit,
    onDeviceAdded: () -> Unit,
) {
    var tab by remember { mutableStateOf(ManualAddTab.Mac) }
    var macInput by remember { mutableStateOf("") }
    var uidInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var qrResult by remember { mutableStateOf<ParsedDeviceQr?>(null) }
    val scope = rememberCoroutineScope()

    val duplicateMsg = stringResource(Res.string.manual_add_duplicate_mac)
    val invalidMacMsg = stringResource(Res.string.manual_add_invalid_mac)

    fun tryAdd(mac: String, uid: String? = null) {
        errorMessage = null
        val normalized = normalizeMacForDisplay(mac)
        if (!isValidMacFormat(normalized)) {
            errorMessage = invalidMacMsg
            return
        }
        isSaving = true
        scope.launch {
            val result = ManualPairedDeviceStore.add(normalized, uid)
            isSaving = false
            result.onSuccess {
                qrResult = null
                onDeviceAdded()
            }.onFailure { e ->
                errorMessage = when (e) {
                    is DuplicateManualDeviceMacException -> duplicateMsg
                    else -> e.message ?: duplicateMsg
                }
            }
        }
    }

    val isQrTab = tab == ManualAddTab.Qr
    val colors = manualUiColors()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.pageBg),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            SettingsBackHeader(
                backLabel = settingsBackLabelFor(SettingsPage.ManualDeviceAdd),
                pageTitle = settingsHeaderTitleFor(SettingsPage.ManualDeviceAdd),
                onBack = onBack,
            )
            if (isQrTab) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ManualMethodRow(
                        colors = colors,
                        selected = tab,
                        onSelect = {
                            tab = it
                            errorMessage = null
                        },
                    )
                    ManualQrSection(
                        colors = colors,
                        modifier = Modifier.weight(1f),
                        onScanned = { raw ->
                            val parsed = parseDeviceQrContent(raw)
                            if (parsed.mac == null) {
                                errorMessage = invalidMacMsg
                                false
                            } else {
                                errorMessage = null
                                qrResult = parsed
                                true
                            }
                        },
                        onCameraError = { errorMessage = it },
                        onSwitchToMac = { tab = ManualAddTab.Mac },
                    )
                    errorMessage?.let {
                        Text(
                            it,
                            color = MaterialTheme.colors.error,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.manual_add_subtitle),
                        fontSize = 14.sp,
                        color = colors.textSecondary,
                    )
                    ManualInfoCard(
                        colors = colors,
                        title = stringResource(Res.string.manual_add_where_info_title),
                        body = stringResource(Res.string.manual_add_where_info_body),
                    )
                    ManualMethodRow(
                        colors = colors,
                        selected = tab,
                        onSelect = {
                            tab = it
                            errorMessage = null
                        },
                    )
                    when (tab) {
                        ManualAddTab.Mac -> ManualMacForm(
                            colors = colors,
                            mac = macInput,
                            onMacChange = {
                                val (formatted, _) = formatMacAddressInputColonsWithCursor(it, it.length)
                                macInput = formatted
                                errorMessage = null
                            },
                            note = stringResource(Res.string.manual_add_note),
                            error = errorMessage,
                            isSaving = isSaving,
                            onAdd = { tryAdd(macInput) },
                        )
                        ManualAddTab.Qr -> Unit
                        ManualAddTab.Uid -> ManualUidForm(
                            colors = colors,
                            uid = uidInput,
                            onUidChange = {
                                uidInput = it.uppercase()
                                errorMessage = null
                            },
                            note = stringResource(Res.string.manual_add_note),
                            error = errorMessage,
                            isSaving = isSaving,
                            onAdd = {
                                val uid = uidInput.trim()
                                if (uid.isEmpty()) {
                                    errorMessage = "UID gerekli."
                                    return@ManualUidForm
                                }
                                isSaving = true
                                scope.launch {
                                    val lookup = DeviceUidLookupApi.lookupMacAndInfoByDeviceUid(uid)
                                    isSaving = false
                                    lookup.onSuccess { (mac, _) ->
                                        tryAdd(mac, uid)
                                    }.onFailure { e ->
                                        errorMessage = e.message
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    qrResult?.let { parsed ->
        DeviceQrResultDialog(
            colors = colors,
            mac = parsed.mac.orEmpty(),
            modelName = parsed.modelName,
            uid = parsed.uid,
            isSaving = isSaving,
            error = errorMessage,
            onDismiss = {
                qrResult = null
                errorMessage = null
            },
            onAdd = {
                val mac = parsed.mac
                if (mac == null) {
                    errorMessage = invalidMacMsg
                } else {
                    tryAdd(mac, parsed.uid)
                }
            },
        )
    }
}

@Composable
private fun ManualInfoCard(colors: ManualUiColors, title: String, body: String) {
    Card(
        backgroundColor = colors.infoBg,
        elevation = 0.dp,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Default.Info, contentDescription = null, tint = colors.accent)
            Column {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = colors.textPrimary)
                Spacer(Modifier.height(4.dp))
                Text(body, fontSize = 13.sp, color = colors.textSecondary, lineHeight = 18.sp)
            }
        }
    }
}

@Composable
private fun ManualMethodRow(
    colors: ManualUiColors,
    selected: ManualAddTab,
    onSelect: (ManualAddTab) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ManualMethodChip(
            colors = colors,
            label = stringResource(Res.string.manual_add_method_enter),
            icon = Icons.Default.Tag,
            selected = selected == ManualAddTab.Mac,
            onClick = { onSelect(ManualAddTab.Mac) },
            modifier = Modifier.weight(1f),
        )
        ManualMethodChip(
            colors = colors,
            label = stringResource(Res.string.manual_add_method_qr),
            icon = Icons.Default.QrCodeScanner,
            selected = selected == ManualAddTab.Qr,
            onClick = { onSelect(ManualAddTab.Qr) },
            modifier = Modifier.weight(1f),
        )
        ManualMethodChip(
            colors = colors,
            label = stringResource(Res.string.manual_add_method_uid),
            icon = Icons.Default.QrCode,
            selected = selected == ManualAddTab.Uid,
            onClick = { onSelect(ManualAddTab.Uid) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ManualMethodChip(
    colors: ManualUiColors,
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = if (selected) colors.accent else colors.border
    val backgroundColor = if (selected) colors.infoBg else colors.cardBg
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(2.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        backgroundColor = backgroundColor,
        elevation = 0.dp,
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (selected) {
                Icon(Icons.Default.Check, contentDescription = null, tint = colors.accent, modifier = Modifier.size(16.dp))
            } else {
                Icon(icon, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(22.dp))
            }
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
        }
    }
}

@Composable
private fun ManualMacForm(
    colors: ManualUiColors,
    mac: String,
    onMacChange: (String) -> Unit,
    note: String,
    error: String?,
    isSaving: Boolean,
    onAdd: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            stringResource(Res.string.manual_add_mac_label),
            fontWeight = FontWeight.SemiBold,
            color = colors.textPrimary,
        )
        TextField(
            value = mac,
            onValueChange = onMacChange,
            placeholder = { Text(stringResource(Res.string.manual_add_mac_hint), color = colors.textSecondary) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                keyboardType = KeyboardType.Ascii,
            ),
            colors = TextFieldDefaults.textFieldColors(
                focusedIndicatorColor = colors.accent,
                backgroundColor = colors.cardBg,
                textColor = colors.textPrimary,
                placeholderColor = colors.textSecondary,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        Text(stringResource(Res.string.manual_add_mac_helper), fontSize = 12.sp, color = colors.textSecondary)
        ManualNoteBox(colors, note)
        error?.let { Text(it, color = MaterialTheme.colors.error, fontSize = 13.sp) }
        ManualAddButton(colors, enabled = !isSaving, loading = isSaving, onClick = onAdd)
    }
}

@Composable
private fun ManualUidForm(
    colors: ManualUiColors,
    uid: String,
    onUidChange: (String) -> Unit,
    note: String,
    error: String?,
    isSaving: Boolean,
    onAdd: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            stringResource(Res.string.manual_add_uid_label),
            fontWeight = FontWeight.SemiBold,
            color = colors.textPrimary,
        )
        TextField(
            value = uid,
            onValueChange = onUidChange,
            placeholder = { Text(stringResource(Res.string.manual_add_uid_hint), color = colors.textSecondary) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                keyboardType = KeyboardType.Ascii,
            ),
            colors = TextFieldDefaults.textFieldColors(
                focusedIndicatorColor = colors.accent,
                backgroundColor = colors.cardBg,
                textColor = colors.textPrimary,
                placeholderColor = colors.textSecondary,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        ManualNoteBox(colors, note)
        error?.let { Text(it, color = MaterialTheme.colors.error, fontSize = 13.sp) }
        ManualAddButton(colors, enabled = !isSaving, loading = isSaving, onClick = onAdd)
    }
}

@Composable
private fun ManualQrSection(
    colors: ManualUiColors,
    modifier: Modifier = Modifier,
    onScanned: (String) -> Boolean,
    onCameraError: (String) -> Unit,
    onSwitchToMac: () -> Unit,
) {
    var flashEnabled by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(Res.string.manual_add_qr_title),
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(Res.string.manual_add_qr_subtitle),
                fontSize = 14.sp,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .heightIn(min = 220.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.cameraPreviewBg),
            ) {
                DeviceQrScannerView(
                    modifier = Modifier.fillMaxSize(),
                    flashEnabled = flashEnabled,
                    onQrScanned = onScanned,
                    onCameraError = onCameraError,
                )
                QrViewfinderOverlay(
                    colors = colors,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth(0.82f)
                        .aspectRatio(1f),
                )
                QrFlashButton(
                    colors = colors,
                    enabled = flashEnabled,
                    onToggle = { flashEnabled = !flashEnabled },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp),
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(colors.qrTipIconBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = colors.noteIcon,
                    modifier = Modifier.size(16.dp),
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = stringResource(Res.string.manual_add_qr_tip),
                fontSize = 13.sp,
                color = colors.textSecondary,
                lineHeight = 18.sp,
                modifier = Modifier.weight(1f),
            )
        }

        QrHowItWorksPanel(colors)

        Text(
            text = stringResource(Res.string.manual_add_qr_manual_link),
            color = colors.accent,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onSwitchToMac)
                .padding(vertical = 4.dp),
        )
    }
}

@Composable
private fun QrFlashButton(
    colors: ManualUiColors,
    enabled: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.clickable(onClick = onToggle),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color(0x66000000)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (enabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                contentDescription = null,
                tint = if (enabled) colors.accent else Color.White,
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(Res.string.manual_add_qr_flash),
            fontSize = 11.sp,
            color = Color.White,
        )
    }
}

@Composable
private fun QrViewfinderOverlay(colors: ManualUiColors, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "qr_scan")
    val scanProgress by transition.animateFloat(
        initialValue = 0.14f,
        targetValue = 0.86f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "scan_line",
    )

    BoxWithConstraints(modifier = modifier) {
        val corner = 32.dp
        val stroke = 4.dp
        Canvas(Modifier.fillMaxSize()) {
            val len = corner.toPx()
            val sw = stroke.toPx()
            val w = size.width
            val h = size.height
            val white = Color.White
            fun cornerLines(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float) {
                drawLine(white, Offset(x1, y1), Offset(x2, y2), sw, StrokeCap.Round)
                drawLine(white, Offset(x1, y1), Offset(x3, y3), sw, StrokeCap.Round)
            }
            cornerLines(0f, 0f, len, 0f, 0f, len)
            cornerLines(w, 0f, w - len, 0f, w, len)
            cornerLines(0f, h, len, h, 0f, h - len)
            cornerLines(w, h, w - len, h, w, h - len)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .height(3.dp)
                .align(Alignment.TopCenter)
                .offset(y = maxHeight * scanProgress)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            colors.accent,
                            colors.accent.copy(alpha = 0.7f),
                            colors.accent,
                            Color.Transparent,
                        ),
                    ),
                    shape = RoundedCornerShape(2.dp),
                ),
        )
    }
}

@Composable
private fun QrHowItWorksPanel(colors: ManualUiColors) {
    Card(
        backgroundColor = colors.cardBg,
        elevation = 0.dp,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, colors.border, RoundedCornerShape(16.dp)),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(Res.string.manual_add_qr_how_title),
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = colors.textPrimary,
            )
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                QrHowItWorksStep(
                    colors = colors,
                    icon = Icons.Default.QrCodeScanner,
                    text = stringResource(Res.string.manual_add_qr_step1),
                )
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = colors.textSecondary,
                    modifier = Modifier.size(18.dp),
                )
                QrHowItWorksStep(
                    colors = colors,
                    icon = Icons.Default.QrCode2,
                    text = stringResource(Res.string.manual_add_qr_step2),
                )
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = colors.textSecondary,
                    modifier = Modifier.size(18.dp),
                )
                QrHowItWorksStep(
                    colors = colors,
                    icon = Icons.Default.Check,
                    text = stringResource(Res.string.manual_add_qr_step3),
                    iconTint = colors.accent,
                )
            }
        }
    }
}

@Composable
private fun QrHowItWorksStep(
    colors: ManualUiColors,
    icon: ImageVector,
    text: String,
    iconTint: Color = colors.textSecondary,
) {
    Column(
        modifier = Modifier.width(96.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(28.dp))
        Text(
            text = text,
            fontSize = 11.sp,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 14.sp,
        )
    }
}

@Composable
private fun ManualNoteBox(colors: ManualUiColors, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(colors.noteBg)
            .border(1.dp, colors.noteBorder, RoundedCornerShape(10.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.Default.Lightbulb, contentDescription = null, tint = colors.noteIcon, modifier = Modifier.size(20.dp))
        Text(text, fontSize = 12.sp, color = colors.textPrimary, lineHeight = 17.sp)
    }
}

@Composable
private fun ManualAddButton(colors: ManualUiColors, enabled: Boolean, loading: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        colors = ButtonDefaults.buttonColors(backgroundColor = colors.accent),
        shape = RoundedCornerShape(12.dp),
    ) {
        if (loading) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
        } else {
            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
            Spacer(Modifier.size(8.dp))
            Text(stringResource(Res.string.manual_add_button), color = Color.White, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun DeviceQrResultDialog(
    colors: ManualUiColors,
    mac: String,
    modelName: String? = null,
    uid: String?,
    isSaving: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onAdd: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            backgroundColor = colors.cardBg,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(Res.string.manual_add_qr_result_title),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = colors.textPrimary,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            stringResource(Res.string.manual_add_qr_result_subtitle),
                            fontSize = 13.sp,
                            color = colors.textSecondary,
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = colors.textSecondary)
                    }
                }
                Spacer(Modifier.height(16.dp))
                modelName?.let {
                    QrInfoRow(colors, "Model", it)
                    Spacer(Modifier.height(8.dp))
                }
                QrInfoRow(colors, "MAC", mac)
                uid?.let {
                    Spacer(Modifier.height(8.dp))
                    QrInfoRow(colors, "UID", it)
                }
                error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colors.error, fontSize = 13.sp)
                }
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onAdd,
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(backgroundColor = colors.accent),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(Res.string.manual_add_add_device), color = Color.White)
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textSecondary),
                ) {
                    Text("İptal")
                }
            }
        }
    }
}

@Composable
private fun QrInfoRow(colors: ManualUiColors, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(colors.pageBg)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, fontWeight = FontWeight.Medium, color = colors.textSecondary, fontSize = 13.sp)
        Text(value, fontWeight = FontWeight.SemiBold, color = colors.textPrimary, fontSize = 13.sp)
    }
}

package com.hextech.vango.profile

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler
import com.hextech.vango.BleGattConnectionChain
import com.hextech.vango.currentPlatformDeviceMac
import com.hextech.vango.getAppContext
import com.hextech.vango.persistCaravanType
import com.hextech.vango.phoneCaravanForeground
import com.hextech.vango.resolvedCaravanType
import com.hextech.vango.vanGoData
import com.hextech.vango.vanGoDeviceWifiMac
import com.hextech.vango.vanGoGlobal
import com.hextech.vango.vangoDeviceList
import com.hextech.vango.vangoDeviceListSize
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import testvango.composeapp.generated.resources.Res
import testvango.composeapp.generated.resources.caravan_type_motor
import testvango.composeapp.generated.resources.caravan_type_tow
import testvango.composeapp.generated.resources.profile_caravan_type
import testvango.composeapp.generated.resources.profile_photo_add
import testvango.composeapp.generated.resources.profile_photo_remove
import testvango.composeapp.generated.resources.register_back_to_login
import testvango.composeapp.generated.resources.login_email
import testvango.composeapp.generated.resources.register_caravan_hint
import testvango.composeapp.generated.resources.register_first_name
import testvango.composeapp.generated.resources.register_last_name
import testvango.composeapp.generated.resources.register_password_confirm
import testvango.composeapp.generated.resources.register_password_min
import testvango.composeapp.generated.resources.register_username
import testvango.composeapp.generated.resources.register_mac_label
import testvango.composeapp.generated.resources.register_nav_back
import testvango.composeapp.generated.resources.register_page_title
import testvango.composeapp.generated.resources.register_submit
import testvango.composeapp.generated.resources.register_success_title
import testvango.composeapp.generated.resources.register_uid_label
import testvango.composeapp.generated.resources.register_uid_lookup_loading
import testvango.composeapp.generated.resources.register_uid_not_found
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.text.style.TextOverflow

private const val REQUIRED_FIELD_MARK = " *"

private enum class RegisterFlowPhase {
    Form,
    Done,
}

@Composable
fun RegisterRequestScreen(
    viewModel: ProfileViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    var phase by remember { mutableStateOf(RegisterFlowPhase.Form) }
    var regFirstName by remember { mutableStateOf("") }
    var regLastName by remember { mutableStateOf("") }
    var regUsername by remember { mutableStateOf("") }
    var regEmail by remember { mutableStateOf("") }
    var regPassword by remember { mutableStateOf("") }
    var regPasswordConfirm by remember { mutableStateOf("") }
    var regPhone by remember { mutableStateOf("") }
    var regAddress by remember { mutableStateOf("") }
    var regDeviceMacValue by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
    val regDeviceMac = regDeviceMacValue.text
    var regDeviceUid by remember { mutableStateOf("") }
    BackHandler(enabled = phase == RegisterFlowPhase.Form) {
        onBack()
    }

    var uidLookupBusy by remember { mutableStateOf(false) }
    var uidLookupHint by remember { mutableStateOf<String?>(null) }
    var regProfileImage by remember { mutableStateOf<ByteArray?>(null) }
    val regProfileImagePicker = rememberAvatarPicker { regProfileImage = it }
    var regCaravanType by remember {
        mutableStateOf(resolvedCaravanType())
    }
    var localError by remember { mutableStateOf<String?>(null) }
    var doneMessage by remember { mutableStateOf("") }

    val isLight = MaterialTheme.colors.isLight
    val cardBg = if (isLight) MaterialTheme.colors.surface else Color(0xFF2B313D)
    val highContrastText = if (isLight) MaterialTheme.colors.onSurface else Color(0xFFF2F5FA)
    val vanGoState by vanGoGlobal.collectAsState()
    val devCount by vangoDeviceListSize.collectAsState()
    val uidNotFoundText = stringResource(Res.string.register_uid_not_found)

    val wifiMacLive by vanGoDeviceWifiMac.collectAsState()
    LaunchedEffect(phase, vanGoState.connectionState, devCount, regDeviceMac, wifiMacLive) {
        if (phase != RegisterFlowPhase.Form) return@LaunchedEffect
        if (vanGoState.connectionState != BleGattConnectionChain.ChainEnded) return@LaunchedEffect
        if (!getAppContext().vanGO.isConnected()) return@LaunchedEffect
        if (regDeviceMac.isNotBlank()) return@LaunchedEffect
        val raw = vanGoDeviceWifiMac.value?.takeIf { it.isNotBlank() }
            ?: vanGoData.value.wifiMac?.takeIf { it.isNotBlank() }
            ?: currentPlatformDeviceMac()
            ?: return@LaunchedEffect
        val norm = normalizeMacForDisplay(raw)
        if (norm.length != 17) return@LaunchedEffect
        regDeviceMacValue = TextFieldValue(norm, TextRange(norm.length))
    }

    LaunchedEffect(phase, regDeviceMac) {
        if (phase != RegisterFlowPhase.Form) return@LaunchedEffect
        val m = regDeviceMac.trim()
        if (m.isEmpty()) {
            uidLookupHint = null
            return@LaunchedEffect
        }
        if (!isValidMacFormat(m)) return@LaunchedEffect
        delay(450)
        if (regDeviceMac.trim() != m) return@LaunchedEffect
        uidLookupBusy = true
        uidLookupHint = null
        viewModel.resolveDeviceUidByMac(m) { result ->
            uidLookupBusy = false
            result.onSuccess { uid ->
                if (uid != null) {
                    regDeviceUid = uid
                    uidLookupHint = null
                } else {
                    uidLookupHint = uidNotFoundText
                }
            }.onFailure { e ->
                val msg = e.message?.trim().orEmpty()
                val transientNetworkOrParse =
                    msg.isEmpty() ||
                        msg.contains("unexpected eof", ignoreCase = true) ||
                        msg.contains("parse http response", ignoreCase = true) ||
                        msg.contains("sunucu yanıtı okunamadı", ignoreCase = true)
                uidLookupHint = if (transientNetworkOrParse) uidNotFoundText else msg
            }
        }
    }

    val fieldColors = TextFieldDefaults.outlinedTextFieldColors(
        textColor = highContrastText,
        backgroundColor = cardBg,
        cursorColor = MaterialTheme.colors.primary,
        focusedBorderColor = MaterialTheme.colors.primary,
        unfocusedBorderColor = highContrastText.copy(alpha = 0.45f),
        focusedLabelColor = MaterialTheme.colors.primary,
        unfocusedLabelColor = highContrastText.copy(alpha = 0.88f),
    )

    fun buildInput() = RegisterRequestInput(
        firstName = regFirstName,
        lastName = regLastName,
        username = regUsername,
        password = regPassword,
        email = regEmail,
        phone = regPhone.ifBlank { null },
        address = regAddress.ifBlank { null },
        deviceMac = regDeviceMac.ifBlank { null },
        deviceUid = regDeviceUid.ifBlank { null },
        profileImage = regProfileImage,
    )

    val displayName = "${regFirstName.trim()} ${regLastName.trim()}".trim().ifBlank { "—" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .background(MaterialTheme.colors.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 4.dp)
                .clickable(enabled = phase != RegisterFlowPhase.Done) { onBack() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = null, tint = MaterialTheme.colors.primary)
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(Res.string.register_nav_back),
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colors.onBackground,
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = stringResource(Res.string.register_page_title),
                style = MaterialTheme.typography.h5,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colors.onBackground,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )

            when (phase) {
                RegisterFlowPhase.Form -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        elevation = 2.dp,
                        backgroundColor = cardBg,
                        contentColor = highContrastText,
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = stringResource(Res.string.profile_photo_add),
                                style = MaterialTheme.typography.caption,
                                color = highContrastText.copy(alpha = 0.9f),
                                fontWeight = FontWeight.Medium,
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(CircleShape)
                                        .border(2.dp, MaterialTheme.colors.primary, CircleShape)
                                        .background(MaterialTheme.colors.onSurface.copy(alpha = 0.08f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    val bmp = regProfileImage?.let { decodeProfileImage(it) }
                                    if (bmp != null) {
                                        Image(
                                            bitmap = bmp,
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop,
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.AccountCircle,
                                            contentDescription = null,
                                            modifier = Modifier.size(84.dp),
                                            tint = MaterialTheme.colors.onSurface.copy(alpha = 0.35f),
                                        )
                                    }
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                TextButton(
                                    onClick = { regProfileImagePicker.launch("image/*") },
                                    enabled = !state.isLoading,
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AddAPhoto,
                                        contentDescription = null,
                                        tint = MaterialTheme.colors.primary,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(Res.string.profile_photo_add),
                                        color = MaterialTheme.colors.primary,
                                        fontWeight = FontWeight.Medium,
                                    )
                                }
                            }
                            if (regProfileImage != null) {
                                TextButton(
                                    onClick = { regProfileImage = null },
                                    enabled = !state.isLoading,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(
                                        stringResource(Res.string.profile_photo_remove),
                                        color = MaterialTheme.colors.error,
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = regFirstName,
                                onValueChange = { regFirstName = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(stringResource(Res.string.register_first_name) + REQUIRED_FIELD_MARK) },
                                singleLine = true,
                                colors = fieldColors,
                            )
                            OutlinedTextField(
                                value = regLastName,
                                onValueChange = { regLastName = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(stringResource(Res.string.register_last_name) + REQUIRED_FIELD_MARK) },
                                singleLine = true,
                                colors = fieldColors,
                            )
                            OutlinedTextField(
                                value = regUsername,
                                onValueChange = { regUsername = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(stringResource(Res.string.register_username) + REQUIRED_FIELD_MARK) },
                                singleLine = true,
                                colors = fieldColors,
                            )
                            OutlinedTextField(
                                value = regEmail,
                                onValueChange = { regEmail = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(stringResource(Res.string.login_email) + REQUIRED_FIELD_MARK) },
                                singleLine = true,
                                colors = fieldColors,
                            )
                            OutlinedTextField(
                                value = regPassword,
                                onValueChange = { regPassword = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(stringResource(Res.string.register_password_min) + REQUIRED_FIELD_MARK) },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                colors = fieldColors,
                            )
                            OutlinedTextField(
                                value = regPasswordConfirm,
                                onValueChange = { regPasswordConfirm = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(stringResource(Res.string.register_password_confirm) + REQUIRED_FIELD_MARK) },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                colors = fieldColors,
                            )
                            OutlinedTextField(
                                value = regPhone,
                                onValueChange = { regPhone = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Telefon (opsiyonel)") },
                                singleLine = true,
                                colors = fieldColors,
                            )
                            OutlinedTextField(
                                value = regAddress,
                                onValueChange = { regAddress = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Adres (opsiyonel)") },
                                singleLine = true,
                                colors = fieldColors,
                            )
                            OutlinedTextField(
                                value = regDeviceMacValue,
                                onValueChange = { new ->
                                    val (formatted, cursorPos) =
                                        formatMacAddressInputColonsWithCursor(new.text, new.selection.end)
                                    regDeviceMacValue = TextFieldValue(formatted, TextRange(cursorPos))
                                },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(stringResource(Res.string.register_mac_label)) },
                                singleLine = true,
                                placeholder = { Text("AA:BB:CC:DD:EE:FF") },
                                colors = fieldColors,
                            )
                            OutlinedTextField(
                                value = regDeviceUid,
                                onValueChange = { regDeviceUid = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(stringResource(Res.string.register_uid_label)) },
                                singleLine = true,
                                colors = fieldColors,
                            )
                            if (uidLookupBusy) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colors.primary,
                                    )
                                    Text(
                                        stringResource(Res.string.register_uid_lookup_loading),
                                        style = MaterialTheme.typography.caption,
                                        color = highContrastText.copy(alpha = 0.85f),
                                    )
                                }
                            }
                            uidLookupHint?.let { hint ->
                                Text(
                                    hint,
                                    style = MaterialTheme.typography.caption,
                                    color = MaterialTheme.colors.error.copy(alpha = 0.9f),
                                )
                            }

                            Text(
                                text = stringResource(Res.string.profile_caravan_type) + REQUIRED_FIELD_MARK,
                                style = MaterialTheme.typography.subtitle1,
                                fontWeight = FontWeight.SemiBold,
                                color = highContrastText,
                            )
                            Text(
                                text = stringResource(Res.string.register_caravan_hint),
                                style = MaterialTheme.typography.caption,
                                color = highContrastText.copy(alpha = 0.75f),
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                RegisterCaravanTypeCard(
                                    modifier = Modifier.weight(1f),
                                    type = CaravanType.TowCaravan,
                                    selected = regCaravanType == CaravanType.TowCaravan,
                                    label = stringResource(Res.string.caravan_type_tow),
                                    accent = MaterialTheme.colors.primary,
                                    enabled = !state.isLoading,
                                    onClick = {
                                        regCaravanType = CaravanType.TowCaravan
                                        persistCaravanType(CaravanType.TowCaravan)
                                    },
                                )
                                RegisterCaravanTypeCard(
                                    modifier = Modifier.weight(1f),
                                    type = CaravanType.MotorCaravan,
                                    selected = regCaravanType == CaravanType.MotorCaravan,
                                    label = stringResource(Res.string.caravan_type_motor),
                                    accent = MaterialTheme.colors.primary,
                                    enabled = !state.isLoading,
                                    onClick = {
                                        regCaravanType = CaravanType.MotorCaravan
                                        persistCaravanType(CaravanType.MotorCaravan)
                                    },
                                )
                            }

                            localError?.let {
                                Text(it, color = MaterialTheme.colors.error, style = MaterialTheme.typography.caption)
                            }
                            Button(
                                onClick = {
                                    localError = null
                                    persistCaravanType(regCaravanType)
                                    viewModel.submitRegisterRequest(buildInput(), regPasswordConfirm) { result ->
                                        result.onSuccess {
                                            persistCaravanType(regCaravanType)
                                            doneMessage = it.message.ifBlank {
                                                "Başvurunuz alındı. Yönetici onayı bekleniyor."
                                            }
                                            phase = RegisterFlowPhase.Done
                                        }.onFailure {
                                            val msg = it.message?.trim().orEmpty()
                                            localError = if (
                                                msg.contains("unexpected eof", ignoreCase = true) ||
                                                msg.contains("failed to parse http response", ignoreCase = true)
                                            ) {
                                                "Sunucu yanıtı tamamlanamadı. Lütfen tekrar deneyin."
                                            } else {
                                                msg.ifBlank { "Kayıt başvurusu sırasında hata oluştu." }
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !state.isLoading,
                                colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary),
                            ) {
                                if (state.isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colors.onPrimary,
                                    )
                                } else {
                                    Text(
                                        stringResource(Res.string.register_submit),
                                        color = MaterialTheme.colors.onPrimary,
                                    )
                                }
                            }
                            Spacer(Modifier.height(120.dp))
                        }
                    }
                }

                RegisterFlowPhase.Done -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        elevation = 2.dp,
                        backgroundColor = cardBg,
                        contentColor = highContrastText,
                    ) {
                        Column(
                            Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = stringResource(Res.string.register_success_title),
                                style = MaterialTheme.typography.h6,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF22C55E),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Text(
                                text = displayName,
                                style = MaterialTheme.typography.subtitle1,
                                fontWeight = FontWeight.SemiBold,
                                color = highContrastText,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Text(
                                text = doneMessage,
                                style = MaterialTheme.typography.body1,
                                color = highContrastText,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = onBack,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary),
                            ) {
                                Text(
                                    stringResource(Res.string.register_back_to_login),
                                    color = MaterialTheme.colors.onPrimary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RegisterCaravanTypeCard(
    modifier: Modifier,
    type: CaravanType,
    selected: Boolean,
    label: String,
    accent: Color,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val border = if (selected) accent else MaterialTheme.colors.onSurface.copy(alpha = 0.22f)
    Column(
        modifier = modifier
            .heightIn(min = 150.dp)
            .clip(RoundedCornerShape(14.dp))
            .border(2.dp, border, RoundedCornerShape(14.dp))
            .background(
                if (selected) accent.copy(alpha = 0.12f)
                else MaterialTheme.colors.onSurface.copy(alpha = 0.04f),
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(phoneCaravanForeground(type)),
                contentDescription = label,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp),
            )
        }
        Text(
            label,
            color = if (selected) accent else MaterialTheme.colors.onSurface,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

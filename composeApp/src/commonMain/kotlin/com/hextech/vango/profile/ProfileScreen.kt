package com.hextech.vango.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.AlertDialog
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hextech.vango.SettingsBackHeader
import com.hextech.vango.SettingsPage
import com.hextech.vango.settingsBackLabelFor
import com.hextech.vango.settingsHeaderTitleFor
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import testvango.composeapp.generated.resources.Res
import testvango.composeapp.generated.resources.caravan_type_motor
import testvango.composeapp.generated.resources.caravan_type_tow
import testvango.composeapp.generated.resources.profile_account_status
import testvango.composeapp.generated.resources.profile_address
import testvango.composeapp.generated.resources.profile_btn_cancel_edit
import testvango.composeapp.generated.resources.profile_btn_edit
import testvango.composeapp.generated.resources.profile_btn_save
import testvango.composeapp.generated.resources.profile_btn_sign_out
import testvango.composeapp.generated.resources.profile_email
import testvango.composeapp.generated.resources.profile_err_email
import testvango.composeapp.generated.resources.profile_err_full_name
import testvango.composeapp.generated.resources.profile_err_phone
import testvango.composeapp.generated.resources.profile_full_name
import testvango.composeapp.generated.resources.profile_phone
import testvango.composeapp.generated.resources.profile_photo_add
import testvango.composeapp.generated.resources.profile_photo_change
import testvango.composeapp.generated.resources.profile_photo_remove
import testvango.composeapp.generated.resources.profile_photo_update
import testvango.composeapp.generated.resources.profile_caravan_type
import testvango.composeapp.generated.resources.profile_section_info
import testvango.composeapp.generated.resources.profile_title
import testvango.composeapp.generated.resources.profile_user_type
import testvango.composeapp.generated.resources.profile_username
import testvango.composeapp.generated.resources.status_approved
import testvango.composeapp.generated.resources.status_guest
import testvango.composeapp.generated.resources.status_passive
import testvango.composeapp.generated.resources.status_pending
import testvango.composeapp.generated.resources.status_rejected
import testvango.composeapp.generated.resources.profile_admin_branding_desc
import testvango.composeapp.generated.resources.profile_admin_branding_pick
import testvango.composeapp.generated.resources.profile_admin_branding_reset
import testvango.composeapp.generated.resources.profile_admin_branding_title
import testvango.composeapp.generated.resources.profile_save_error_prefix
import testvango.composeapp.generated.resources.user_type_admin
import testvango.composeapp.generated.resources.user_type_customer
import testvango.composeapp.generated.resources.user_type_guest
import testvango.composeapp.generated.resources.user_type_premium
import testvango.composeapp.generated.resources.user_type_service
import testvango.composeapp.generated.resources.user_type_standard

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onNavigateToDevicePairing: () -> Unit,
    modifier: Modifier = Modifier,
    showTopBarBack: Boolean = false,
    onBack: () -> Unit = {},
    onRequestLogin: () -> Unit = {},
) {
    val state by viewModel.state.collectAsState()

    val avatarPicker = rememberAvatarPicker { bytes ->
        viewModel.updateAvatarBytes(bytes)
    }

    state.saveInfoMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissSaveInfoMessage() },
            title = { Text("Bilgilendirme") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissSaveInfoMessage() }) {
                    Text("Tamam")
                }
            }
        )
    }
    val merged = state.draft

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background)
    ) {
        if (showTopBarBack) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                SettingsBackHeader(
                    backLabel = settingsBackLabelFor(SettingsPage.Profile),
                    pageTitle = settingsHeaderTitleFor(SettingsPage.Profile),
                    onBack = onBack,
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            if (!showTopBarBack) {
                Text(
                    text = stringResource(Res.string.profile_title),
                    style = MaterialTheme.typography.h5,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.onBackground,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            if (state.isLoading) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colors.primary)
                }
                return@Column
            }

            if (!state.isLoggedIn) {
                LaunchedEffect(Unit) {
                    onRequestLogin()
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colors.primary)
                }
                return@Column
            }

            ProfileHeaderCard(
                merged = merged,
                accountStatus = state.draft.accountStatus,
                avatarPicker = avatarPicker,
                isAvatarUploading = state.isAvatarUploading,
                onUploadAvatar = { viewModel.uploadProfileAvatar { } },
                onClearAvatar = { viewModel.updateAvatarBytes(null) },
            )

            Spacer(Modifier.height(16.dp))

            if (state.draft.userType == UserType.Admin) {
                AdminBrandingSection()
            }

            Text(
                text = stringResource(Res.string.profile_section_info),
                style = MaterialTheme.typography.subtitle2,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colors.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            ProfileSectionCard {
                ProfilePersonalFields(
                    merged = merged,
                    isEditing = state.isEditing,
                    fieldErrors = state.fieldErrors,
                    onFullName = { v -> viewModel.updateDraft { it.copy(fullName = v) } },
                    onUsername = { v -> viewModel.updateDraft { it.copy(username = v) } },
                    onEmail = { v -> viewModel.updateDraft { it.copy(email = v) } },
                    onPhone = { v -> viewModel.updateDraft { it.copy(phone = v) } },
                    onAddress = { v -> viewModel.updateDraft { it.copy(address = v) } },
                    onCaravanType = { v -> viewModel.updateDraft { it.copy(caravanType = v) } },
                    onBeginEdit = { if (!state.isEditing) viewModel.setEditing(true) },
                )
            }

            Spacer(Modifier.height(20.dp))

            state.saveError?.let { err ->
                Text(
                    text = "${stringResource(Res.string.profile_save_error_prefix)}: $err",
                    color = MaterialTheme.colors.error,
                    style = MaterialTheme.typography.caption,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (state.isEditing) {
                    Button(
                        onClick = { viewModel.save() },
                        modifier = Modifier.weight(1f),
                        enabled = !state.isSaving,
                        colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary)
                    ) {
                        if (state.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colors.onPrimary
                            )
                        } else {
                            Text(
                                stringResource(Res.string.profile_btn_save),
                                color = MaterialTheme.colors.onPrimary
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = { viewModel.cancelEdit() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colors.primary
                        )
                    ) {
                        Text(stringResource(Res.string.profile_btn_cancel_edit))
                    }
                } else {
                    OutlinedButton(
                        onClick = { viewModel.setEditing(true) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colors.primary
                        )
                    ) {
                        Text(stringResource(Res.string.profile_btn_edit))
                    }
                }
            }

            TextButton(
                onClick = { viewModel.signOut() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(Res.string.profile_btn_sign_out),
                    color = MaterialTheme.colors.error
                )
            }
        }
    }
}

@Composable
private fun profileOutlinedFieldColors() = TextFieldDefaults.outlinedTextFieldColors(
    textColor = MaterialTheme.colors.onSurface,
    disabledTextColor = MaterialTheme.colors.onSurface.copy(alpha = 0.88f),
    backgroundColor = MaterialTheme.colors.surface,
    cursorColor = MaterialTheme.colors.primary,
    errorCursorColor = MaterialTheme.colors.error,
    focusedBorderColor = MaterialTheme.colors.primary,
    unfocusedBorderColor = MaterialTheme.colors.onSurface.copy(alpha = 0.45f),
    disabledBorderColor = MaterialTheme.colors.onSurface.copy(alpha = 0.35f),
    errorBorderColor = MaterialTheme.colors.error,
    focusedLabelColor = MaterialTheme.colors.primary,
    unfocusedLabelColor = MaterialTheme.colors.onSurface.copy(alpha = 0.78f),
    disabledLabelColor = MaterialTheme.colors.onSurface.copy(alpha = 0.55f),
    errorLabelColor = MaterialTheme.colors.error,
    placeholderColor = MaterialTheme.colors.onSurface.copy(alpha = 0.45f),
)

@Composable
private fun ProfileSectionCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = 2.dp,
        backgroundColor = MaterialTheme.colors.surface,
        contentColor = MaterialTheme.colors.onSurface,
    ) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun ProfileHeaderCard(
    merged: UserProfile,
    accountStatus: AccountStatus,
    avatarPicker: AvatarPickerLauncher,
    isAvatarUploading: Boolean,
    onUploadAvatar: () -> Unit,
    onClearAvatar: () -> Unit,
) {
    val avatarBitmap = remember(merged.avatarBytes) {
        merged.avatarBytes?.let { decodeProfileImage(it) }
    }
    val fullNameParts = remember(merged.fullName) {
        val normalized = merged.fullName.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        val first = normalized.firstOrNull().orEmpty()
        val last = normalized.drop(1).joinToString(" ")
        first to last
    }
    val statusLabel = when (accountStatus) {
        AccountStatus.Guest -> stringResource(Res.string.status_guest)
        AccountStatus.PendingApproval -> stringResource(Res.string.status_pending)
        AccountStatus.Approved -> stringResource(Res.string.status_approved)
        AccountStatus.Rejected -> stringResource(Res.string.status_rejected)
        AccountStatus.Passive -> stringResource(Res.string.status_passive)
    }
    val statusColor = when (accountStatus) {
        AccountStatus.Guest -> Color(0xFF6B7280)
        AccountStatus.PendingApproval -> Color(0xFFF59E0B)
        AccountStatus.Approved -> Color(0xFF22C55E)
        AccountStatus.Rejected -> Color(0xFFEF4444)
        AccountStatus.Passive -> Color(0xFF52525B)
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = 3.dp,
        backgroundColor = MaterialTheme.colors.surface,
        contentColor = MaterialTheme.colors.onSurface,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = statusColor.copy(alpha = 0.16f),
                contentColor = statusColor,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(Res.string.profile_account_status),
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.9f)
                    )
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.subtitle2,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(112.dp)
                            .clip(CircleShape)
                            .border(2.dp, MaterialTheme.colors.primary.copy(alpha = 0.45f), CircleShape)
                            .background(MaterialTheme.colors.surface)
                            .clickable { avatarPicker.launch() },
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            avatarBitmap != null -> {
                                Image(
                                    painter = BitmapPainter(avatarBitmap),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            else -> {
                                Icon(
                                    imageVector = Icons.Filled.AccountCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(88.dp),
                                    tint = MaterialTheme.colors.onSurface.copy(alpha = 0.38f)
                                )
                            }
                        }
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = fullNameParts.first.ifBlank { "\u2014" },
                                style = MaterialTheme.typography.h6,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colors.onSurface,
                                maxLines = 1
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = fullNameParts.second.ifBlank { "\u2014" },
                                style = MaterialTheme.typography.h6,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colors.onSurface,
                                maxLines = 1
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { avatarPicker.launch() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isAvatarUploading,
                    ) {
                        Icon(
                            Icons.Filled.AddAPhoto,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colors.primary
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (merged.avatarBytes != null) {
                                stringResource(Res.string.profile_photo_change)
                            } else {
                                stringResource(Res.string.profile_photo_add)
                            },
                            color = MaterialTheme.colors.primary,
                            style = MaterialTheme.typography.body2,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onUploadAvatar,
                            enabled = merged.avatarBytes != null && !isAvatarUploading,
                            modifier = Modifier.weight(1f),
                        ) {
                            if (isAvatarUploading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colors.primary,
                                )
                            } else {
                                Text(
                                    stringResource(Res.string.profile_photo_update),
                                    color = MaterialTheme.colors.primary,
                                    style = MaterialTheme.typography.body2,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        TextButton(
                            onClick = onClearAvatar,
                            enabled = merged.avatarBytes != null && !isAvatarUploading,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                stringResource(Res.string.profile_photo_remove),
                                color = if (merged.avatarBytes != null) MaterialTheme.colors.error else MaterialTheme.colors.onSurface.copy(alpha = 0.35f),
                                style = MaterialTheme.typography.body2,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfilePersonalFields(
    merged: UserProfile,
    isEditing: Boolean,
    fieldErrors: Map<String, String>,
    onFullName: (String) -> Unit,
    onUsername: (String) -> Unit,
    onEmail: (String) -> Unit,
    onPhone: (String) -> Unit,
    onAddress: (String) -> Unit,
    onCaravanType: (CaravanType) -> Unit,
    onBeginEdit: () -> Unit,
) {
    val colors = profileOutlinedFieldColors()
    val caravanMenuExpanded = remember { androidx.compose.runtime.mutableStateOf(false) }
    OutlinedTextField(
        value = merged.fullName,
        onValueChange = onFullName,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(Res.string.profile_full_name)) },
        enabled = isEditing,
        singleLine = true,
        isError = fieldErrors.containsKey(ProfileValidator.ERR_FULL_NAME),
        colors = colors,
    )
    if (fieldErrors.containsKey(ProfileValidator.ERR_FULL_NAME)) {
        Text(
            text = stringResource(Res.string.profile_err_full_name),
            color = MaterialTheme.colors.error,
            style = MaterialTheme.typography.caption,
            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
        )
    }
    Spacer(Modifier.height(8.dp))

    OutlinedTextField(
        value = merged.username,
        onValueChange = onUsername,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(Res.string.profile_username)) },
        enabled = isEditing,
        singleLine = true,
        colors = colors,
    )
    Spacer(Modifier.height(8.dp))

    OutlinedTextField(
        value = merged.email,
        onValueChange = onEmail,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(Res.string.profile_email)) },
        enabled = isEditing,
        singleLine = true,
        isError = fieldErrors.containsKey(ProfileValidator.ERR_EMAIL),
        colors = colors,
    )
    if (fieldErrors.containsKey(ProfileValidator.ERR_EMAIL)) {
        Text(
            text = stringResource(Res.string.profile_err_email),
            color = MaterialTheme.colors.error,
            style = MaterialTheme.typography.caption,
            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
        )
    }
    Spacer(Modifier.height(8.dp))

    OutlinedTextField(
        value = merged.phone,
        onValueChange = onPhone,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(Res.string.profile_phone)) },
        enabled = isEditing,
        singleLine = true,
        isError = fieldErrors.containsKey(ProfileValidator.ERR_PHONE),
        colors = colors,
    )
    if (fieldErrors.containsKey(ProfileValidator.ERR_PHONE)) {
        Text(
            text = stringResource(Res.string.profile_err_phone),
            color = MaterialTheme.colors.error,
            style = MaterialTheme.typography.caption,
            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
        )
    }
    Spacer(Modifier.height(8.dp))

    OutlinedTextField(
        value = merged.address,
        onValueChange = onAddress,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(Res.string.profile_address)) },
        enabled = isEditing,
        minLines = 2,
        colors = colors,
    )
    Spacer(Modifier.height(12.dp))

    Text(
        text = stringResource(Res.string.profile_caravan_type),
        color = MaterialTheme.colors.onSurface.copy(alpha = 0.78f),
        style = MaterialTheme.typography.caption,
        modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        Box(modifier = Modifier.width(220.dp)) {
            OutlinedButton(
                onClick = {
                    if (!isEditing) onBeginEdit()
                    caravanMenuExpanded.value = true
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    backgroundColor = MaterialTheme.colors.surface,
                    contentColor = MaterialTheme.colors.onSurface,
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colors.onSurface.copy(alpha = 0.32f),
                ),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(
                    caravanTypeLabel(merged.caravanType),
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colors.onSurface,
                )
                Icon(
                    Icons.Filled.ArrowDropDown,
                    contentDescription = null,
                    tint = MaterialTheme.colors.onSurface.copy(alpha = 0.8f),
                )
            }
            DropdownMenu(
                expanded = caravanMenuExpanded.value,
                onDismissRequest = { caravanMenuExpanded.value = false },
            ) {
                DropdownMenuItem(onClick = {
                    onCaravanType(CaravanType.TowCaravan)
                    com.hextech.vango.persistCaravanType(CaravanType.TowCaravan)
                    caravanMenuExpanded.value = false
                }) {
                    Text(caravanTypeLabel(CaravanType.TowCaravan))
                }
                DropdownMenuItem(onClick = {
                    onCaravanType(CaravanType.MotorCaravan)
                    com.hextech.vango.persistCaravanType(CaravanType.MotorCaravan)
                    caravanMenuExpanded.value = false
                }) {
                    Text(caravanTypeLabel(CaravanType.MotorCaravan))
                }
            }
        }
    }
    Spacer(Modifier.height(12.dp))

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            stringResource(Res.string.profile_user_type),
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.85f),
            style = MaterialTheme.typography.body2,
        )
        Text(
            userTypeLabel(merged.userType),
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colors.onSurface,
            style = MaterialTheme.typography.body2,
        )
    }
}

@Composable
private fun AdminBrandingSection() {
    val logoPicker = rememberAvatarPicker { bytes ->
        SplashLogoPreferences.setCustomLogoBytes(bytes)
        SplashLogoState.customLogoBytes = bytes
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = 2.dp,
        backgroundColor = MaterialTheme.colors.surface,
        contentColor = MaterialTheme.colors.onSurface,
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = stringResource(Res.string.profile_admin_branding_title),
                style = MaterialTheme.typography.subtitle1,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colors.primary,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(Res.string.profile_admin_branding_desc),
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f),
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { logoPicker.launch("image/png") },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colors.primary),
                ) {
                    Text(stringResource(Res.string.profile_admin_branding_pick))
                }
                TextButton(
                    onClick = {
                        SplashLogoPreferences.setCustomLogoBytes(null)
                        SplashLogoState.customLogoBytes = null
                    }
                ) {
                    Text(
                        stringResource(Res.string.profile_admin_branding_reset),
                        color = MaterialTheme.colors.error,
                    )
                }
            }
        }
    }
    Spacer(Modifier.height(16.dp))
}

@Composable
private fun AccountStatusBadge(status: AccountStatus) {
    val label = when (status) {
        AccountStatus.Guest -> stringResource(Res.string.status_guest)
        AccountStatus.PendingApproval -> stringResource(Res.string.status_pending)
        AccountStatus.Approved -> stringResource(Res.string.status_approved)
        AccountStatus.Rejected -> stringResource(Res.string.status_rejected)
        AccountStatus.Passive -> stringResource(Res.string.status_passive)
    }
    val bg = when (status) {
        AccountStatus.Guest -> Color(0xFF6B7280)
        AccountStatus.PendingApproval -> Color(0xFFF59E0B)
        AccountStatus.Approved -> Color(0xFF22C55E)
        AccountStatus.Rejected -> Color(0xFFEF4444)
        AccountStatus.Passive -> Color(0xFF52525B)
    }
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = bg.copy(alpha = 0.2f),
        contentColor = bg,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(Res.string.profile_account_status),
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.9f)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.subtitle2,
                fontWeight = FontWeight.Bold,
                color = bg
            )
        }
    }
}

@Composable
private fun userTypeLabel(type: UserType): String = when (type) {
    UserType.Guest -> stringResource(Res.string.user_type_guest)
    UserType.Standard -> stringResource(Res.string.user_type_standard)
    UserType.Customer -> stringResource(Res.string.user_type_customer)
    UserType.Premium -> stringResource(Res.string.user_type_premium)
    UserType.Service -> stringResource(Res.string.user_type_service)
    UserType.Admin -> stringResource(Res.string.user_type_admin)
}

@Composable
private fun caravanTypeLabel(type: CaravanType): String = when (type) {
    CaravanType.TowCaravan -> stringResource(Res.string.caravan_type_tow)
    CaravanType.MotorCaravan -> stringResource(Res.string.caravan_type_motor)
}

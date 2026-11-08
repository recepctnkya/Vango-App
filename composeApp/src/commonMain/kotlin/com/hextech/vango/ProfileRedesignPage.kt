package com.hextech.vango

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Language
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hextech.vango.profile.ChangePasswordApi
import com.hextech.vango.profile.LoginPreferences
import com.hextech.vango.profile.ProfileDisplayPreferences
import com.hextech.vango.profile.UserType
import com.hextech.vango.profile.decodeProfileImage
import com.hextech.vango.profile.rememberAvatarPicker
import com.hextech.vango.profile.rememberProfileViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import testvango.composeapp.generated.resources.Res
import testvango.composeapp.generated.resources.login_email_or_username
import testvango.composeapp.generated.resources.login_error_invalid
import testvango.composeapp.generated.resources.login_password
import testvango.composeapp.generated.resources.login_sign_in
import testvango.composeapp.generated.resources.profile_btn_cancel_edit
import testvango.composeapp.generated.resources.profile_btn_edit
import testvango.composeapp.generated.resources.profile_btn_save
import testvango.composeapp.generated.resources.profile_email
import testvango.composeapp.generated.resources.profile_phone
import testvango.composeapp.generated.resources.profile_rd_account_info
import testvango.composeapp.generated.resources.profile_rd_copied
import testvango.composeapp.generated.resources.profile_rd_default_name
import testvango.composeapp.generated.resources.profile_rd_first_name
import testvango.composeapp.generated.resources.profile_rd_hint
import testvango.composeapp.generated.resources.profile_rd_language
import testvango.composeapp.generated.resources.profile_rd_last_login
import testvango.composeapp.generated.resources.profile_rd_last_name
import testvango.composeapp.generated.resources.profile_rd_login_hint
import testvango.composeapp.generated.resources.profile_rd_logout
import testvango.composeapp.generated.resources.profile_rd_password_change
import testvango.composeapp.generated.resources.profile_rd_password_current
import testvango.composeapp.generated.resources.profile_rd_password_current_ph
import testvango.composeapp.generated.resources.profile_rd_password_medium
import testvango.composeapp.generated.resources.profile_rd_password_mismatch
import testvango.composeapp.generated.resources.profile_rd_password_new
import testvango.composeapp.generated.resources.profile_rd_password_new_ph
import testvango.composeapp.generated.resources.profile_rd_password_ok
import testvango.composeapp.generated.resources.profile_rd_password_repeat
import testvango.composeapp.generated.resources.profile_rd_password_repeat_ph
import testvango.composeapp.generated.resources.profile_rd_password_short
import testvango.composeapp.generated.resources.profile_rd_password_strength
import testvango.composeapp.generated.resources.profile_rd_password_strong
import testvango.composeapp.generated.resources.profile_rd_password_title
import testvango.composeapp.generated.resources.profile_rd_password_update
import testvango.composeapp.generated.resources.profile_rd_password_weak
import testvango.composeapp.generated.resources.profile_rd_personal
import testvango.composeapp.generated.resources.profile_rd_privacy_note
import testvango.composeapp.generated.resources.profile_rd_registered
import testvango.composeapp.generated.resources.profile_rd_security_code
import testvango.composeapp.generated.resources.profile_rd_security_code_ph
import testvango.composeapp.generated.resources.profile_rd_timezone
import testvango.composeapp.generated.resources.profile_rd_title
import testvango.composeapp.generated.resources.profile_rd_tz_istanbul
import testvango.composeapp.generated.resources.profile_rd_tz_london
import testvango.composeapp.generated.resources.profile_rd_tz_utc
import testvango.composeapp.generated.resources.profile_rd_user_id
import testvango.composeapp.generated.resources.settings_lang_english
import testvango.composeapp.generated.resources.settings_lang_rd_never
import testvango.composeapp.generated.resources.settings_lang_turkish
import testvango.composeapp.generated.resources.user_type_admin
import testvango.composeapp.generated.resources.user_type_customer
import testvango.composeapp.generated.resources.user_type_guest
import testvango.composeapp.generated.resources.user_type_premium
import testvango.composeapp.generated.resources.user_type_service
import testvango.composeapp.generated.resources.user_type_standard

private val ProfText: Color @Composable get() = LocalAppChrome.current.text
private val ProfMuted: Color @Composable get() = LocalAppChrome.current.muted
private val ProfBorder: Color @Composable get() = LocalAppChrome.current.border
private val ProfTile: Color @Composable get() = LocalAppChrome.current.tile
private val ProfCard: Color @Composable get() = LocalAppChrome.current.card
private val ProfInk: Color @Composable get() = LocalAppChrome.current.ink
private val ProfInfoBg: Color @Composable get() = LocalAppChrome.current.infoBg
private val ProfError = Color(0xFFE35D5D)

@Composable
fun ProfileRedesignPage() {
    val viewModel = rememberProfileViewModel()
    val state by viewModel.state.collectAsState()
    val accent = accentColorFromId(customAppAccentId)
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val phoneUi = isPhoneUi()
    val avatarPicker = rememberAvatarPicker { bytes ->
        viewModel.updateAvatarBytes(bytes)
        if (bytes != null) viewModel.uploadProfileAvatar { }
    }

    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var copiedFlash by remember { mutableStateOf(false) }
    var langMenu by remember { mutableStateOf(false) }
    var tzMenu by remember { mutableStateOf(false) }
    var timezoneId by remember { mutableStateOf(ProfileDisplayPreferences.getTimezoneId()) }
    var currentPass by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }
    var repeatPass by remember { mutableStateOf("") }
    var securityCode by remember { mutableStateOf("") }
    var passVisible by remember { mutableStateOf(false) }
    var passwordBusy by remember { mutableStateOf(false) }
    var passwordMessage by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf(false) }
    var loginId by remember { mutableStateOf("") }
    var loginPass by remember { mutableStateOf("") }
    var loginError by remember { mutableStateOf<String?>(null) }

    val invalidLogin = stringResource(Res.string.login_error_invalid)
    val passMismatch = stringResource(Res.string.profile_rd_password_mismatch)
    val passShort = stringResource(Res.string.profile_rd_password_short)
    val passOk = stringResource(Res.string.profile_rd_password_ok)
    val copiedText = stringResource(Res.string.profile_rd_copied)
    val defaultName = stringResource(Res.string.profile_rd_default_name)

    LaunchedEffect(state.draft.fullName, state.draft.email, state.draft.phone, state.isLoggedIn) {
        val parts = splitFullName(state.draft.fullName)
        firstName = parts.first
        lastName = parts.second
        email = state.draft.email
        phone = state.draft.phone
    }

    LaunchedEffect(state.isLoggedIn) {
        if (state.isLoggedIn) {
            val now = Clock.System.now().toEpochMilliseconds()
            ProfileDisplayPreferences.setLastLoginAtEpochMs(now)
            if (ProfileDisplayPreferences.getRegisteredAtEpochMs() <= 0L) {
                ProfileDisplayPreferences.setRegisteredAtEpochMs(now)
            }
        }
    }

    LaunchedEffect(copiedFlash) {
        if (copiedFlash) {
            delay(1800)
            copiedFlash = false
        }
    }

    val displayName = state.draft.fullName.trim().ifBlank { defaultName }
    val userId = formatProfileUserId(state.draft.username, state.draft.email)
    val roleLabel = userTypeLabel(state.draft.userType)
    val avatarBitmap = remember(state.draft.avatarBytes) {
        state.draft.avatarBytes?.let { decodeProfileImage(it) }
    }
    val langTag = languageTagForSelection(customAppLocale)
    val langLabel = if (langTag == "en") {
        stringResource(Res.string.settings_lang_english)
    } else {
        stringResource(Res.string.settings_lang_turkish)
    }
    val tzLabel = timezoneLabel(timezoneId)
    val registeredText = formatProfileDateTime(ProfileDisplayPreferences.getRegisteredAtEpochMs())
        ?: stringResource(Res.string.settings_lang_rd_never)
    val lastLoginText = formatProfileDateTime(ProfileDisplayPreferences.getLastLoginAtEpochMs())
        ?: stringResource(Res.string.settings_lang_rd_never)

    fun persistPersonal() {
        viewModel.updateDraft {
            it.copy(
                fullName = joinFullName(firstName, lastName).ifBlank { it.fullName },
                email = email.trim(),
                phone = phone.trim(),
            )
        }
        viewModel.save()
    }

    fun submitPasswordChange() {
        passwordMessage = null
        passwordError = false
        when {
            newPass.length < 6 -> {
                passwordMessage = passShort
                passwordError = true
            }
            newPass != repeatPass -> {
                passwordMessage = passMismatch
                passwordError = true
            }
            currentPass.isBlank() -> {
                passwordMessage = passShort
                passwordError = true
            }
            else -> {
                passwordBusy = true
                scope.launch {
                    val token = LoginPreferences.getAuthToken().orEmpty()
                    val result = withContext(Dispatchers.Default) {
                        ChangePasswordApi.update(token, currentPass, newPass)
                    }
                    passwordBusy = false
                    if (result.isSuccess) {
                        currentPass = ""
                        newPass = ""
                        repeatPass = ""
                        securityCode = ""
                        passwordError = false
                        passwordMessage = passOk
                    } else {
                        passwordError = true
                        passwordMessage = result.exceptionOrNull()?.message
                    }
                }
            }
        }
    }

    @Composable
    fun AccountCard(modifier: Modifier = Modifier) {
        ProfSectionCard(modifier = modifier) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(Modifier.size(118.dp), contentAlignment = Alignment.BottomEnd) {
                    Box(
                        modifier = Modifier
                            .size(118.dp)
                            .clip(CircleShape)
                            .background(ProfTile)
                            .clickable { avatarPicker.launch() },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (avatarBitmap != null) {
                            Image(
                                painter = BitmapPainter(avatarBitmap),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            Icon(
                                Icons.Filled.Person,
                                null,
                                tint = ProfMuted,
                                modifier = Modifier.size(64.dp),
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(accent, CircleShape)
                            .clickable { avatarPicker.launch() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.PhotoCamera,
                            null,
                            tint = ProfInk,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    displayName,
                    color = ProfText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .background(accent.copy(alpha = 0.16f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(roleLabel, color = accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(16.dp))
            Divider(color = ProfBorder)
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(Res.string.profile_rd_account_info).uppercase(),
                color = ProfMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
            )
            Spacer(Modifier.height(8.dp))
            ProfMetaRow(
                label = stringResource(Res.string.profile_rd_user_id),
                value = userId,
                accent = accent,
                trailing = {
                    Icon(
                        Icons.Filled.ContentCopy,
                        null,
                        tint = ProfMuted,
                        modifier = Modifier
                            .size(16.dp)
                            .clickable {
                                clipboard.setText(AnnotatedString(userId))
                                copiedFlash = true
                            },
                    )
                },
            )
            ProfMetaRow(stringResource(Res.string.profile_rd_registered), registeredText, accent)
            ProfMetaRow(stringResource(Res.string.profile_rd_last_login), lastLoginText, accent)
            if (copiedFlash) {
                Text(copiedText, color = accent, fontSize = 12.sp)
            }
            Spacer(Modifier.height(14.dp))
            ProfOutlineAction(
                color = accent,
                icon = Icons.AutoMirrored.Filled.Logout,
                label = stringResource(Res.string.profile_rd_logout),
                onClick = { viewModel.signOut() },
            )
        }
    }

    @Composable
    fun PersonalCard(modifier: Modifier = Modifier) {
        ProfSectionCard(modifier = modifier) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(Res.string.profile_rd_personal).uppercase(),
                    color = ProfMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                )
                if (state.isEditing) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ProfChipButton(
                            accent = ProfMuted,
                            filled = false,
                            icon = Icons.Filled.Edit,
                            label = stringResource(Res.string.profile_btn_cancel_edit),
                            onClick = { viewModel.cancelEdit() },
                        )
                        ProfChipButton(
                            accent = accent,
                            filled = true,
                            icon = Icons.Filled.Edit,
                            label = stringResource(Res.string.profile_btn_save),
                            onClick = { persistPersonal() },
                        )
                    }
                } else {
                    ProfChipButton(
                        accent = accent,
                        filled = false,
                        icon = Icons.Filled.Edit,
                        label = stringResource(Res.string.profile_btn_edit),
                        onClick = { viewModel.setEditing(true) },
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            if (!state.isEditing) {
                ProfInfoRow(
                    icon = Icons.Filled.Person,
                    label = stringResource(Res.string.profile_rd_first_name),
                    value = firstName.ifBlank { "—" },
                )
                ProfInfoRow(
                    icon = Icons.Filled.Person,
                    label = stringResource(Res.string.profile_rd_last_name),
                    value = lastName.ifBlank { "—" },
                )
                ProfInfoRow(
                    icon = Icons.Filled.Email,
                    label = stringResource(Res.string.profile_email),
                    value = email.ifBlank { "—" },
                )
                ProfInfoRow(
                    icon = Icons.Outlined.Language,
                    label = stringResource(Res.string.profile_rd_language),
                    value = langLabel,
                )
                ProfInfoRow(
                    icon = Icons.Filled.Phone,
                    label = stringResource(Res.string.profile_phone),
                    value = phone.ifBlank { "—" },
                )
                ProfInfoRow(
                    icon = Icons.Filled.AccessTime,
                    label = stringResource(Res.string.profile_rd_timezone),
                    value = tzLabel,
                )
            } else {
                ProfField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    label = stringResource(Res.string.profile_rd_first_name),
                    accent = accent,
                )
                Spacer(Modifier.height(10.dp))
                ProfField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    label = stringResource(Res.string.profile_rd_last_name),
                    accent = accent,
                )
                Spacer(Modifier.height(10.dp))
                ProfField(
                    value = email,
                    onValueChange = { email = it },
                    label = stringResource(Res.string.profile_email),
                    accent = accent,
                    leading = Icons.Filled.Email,
                )
                Spacer(Modifier.height(10.dp))
                ProfField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = stringResource(Res.string.profile_phone),
                    accent = accent,
                    leading = Icons.Filled.Phone,
                    keyboardType = KeyboardType.Phone,
                )
                Spacer(Modifier.height(10.dp))
                ProfSelectField(
                    label = stringResource(Res.string.profile_rd_language),
                    value = langLabel,
                    accent = accent,
                    icon = Icons.Outlined.Language,
                    enabled = true,
                    expanded = langMenu,
                    onExpand = { langMenu = true },
                    onDismiss = { langMenu = false },
                ) {
                    DropdownMenuItem(onClick = {
                        langMenu = false
                        applyAppLanguage(languageTagForSave("tr"))
                    }) { Text(stringResource(Res.string.settings_lang_turkish)) }
                    DropdownMenuItem(onClick = {
                        langMenu = false
                        applyAppLanguage(languageTagForSave("en"))
                    }) { Text(stringResource(Res.string.settings_lang_english)) }
                }
                Spacer(Modifier.height(10.dp))
                ProfSelectField(
                    label = stringResource(Res.string.profile_rd_timezone),
                    value = tzLabel,
                    accent = accent,
                    icon = Icons.Filled.AccessTime,
                    enabled = true,
                    expanded = tzMenu,
                    onExpand = { tzMenu = true },
                    onDismiss = { tzMenu = false },
                ) {
                    listOf(
                        "Europe/Istanbul" to stringResource(Res.string.profile_rd_tz_istanbul),
                        "Europe/London" to stringResource(Res.string.profile_rd_tz_london),
                        "UTC" to stringResource(Res.string.profile_rd_tz_utc),
                    ).forEach { (id, label) ->
                        DropdownMenuItem(onClick = {
                            tzMenu = false
                            timezoneId = id
                            ProfileDisplayPreferences.setTimezoneId(id)
                        }) { Text(label) }
                    }
                }
            }
            state.saveError?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = ProfError, fontSize = 12.sp)
            }
            state.saveInfoMessage?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = accent, fontSize = 12.sp)
            }
        }
    }

    @Composable
    fun PasswordCard(modifier: Modifier = Modifier) {
        ProfSectionCard(modifier = modifier) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(Res.string.profile_rd_password_title).uppercase(),
                    color = ProfMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                )
                ProfChipButton(
                    accent = accent,
                    filled = false,
                    icon = Icons.Filled.Lock,
                    label = stringResource(Res.string.profile_rd_password_change),
                    onClick = { submitPasswordChange() },
                )
            }
            Spacer(Modifier.height(12.dp))
            ProfField(
                value = currentPass,
                onValueChange = { currentPass = it; passwordMessage = null },
                label = stringResource(Res.string.profile_rd_password_current),
                placeholder = stringResource(Res.string.profile_rd_password_current_ph),
                accent = accent,
                isPassword = true,
                passwordVisible = passVisible,
                onTogglePasswordVisible = { passVisible = !passVisible },
            )
            Spacer(Modifier.height(10.dp))
            ProfField(
                value = newPass,
                onValueChange = { newPass = it; passwordMessage = null },
                label = stringResource(Res.string.profile_rd_password_new),
                placeholder = stringResource(Res.string.profile_rd_password_new_ph),
                accent = accent,
                isPassword = true,
                passwordVisible = passVisible,
                onTogglePasswordVisible = { passVisible = !passVisible },
            )
            Spacer(Modifier.height(8.dp))
            ProfPasswordStrength(password = newPass)
            Spacer(Modifier.height(10.dp))
            ProfField(
                value = repeatPass,
                onValueChange = { repeatPass = it; passwordMessage = null },
                label = stringResource(Res.string.profile_rd_password_repeat),
                placeholder = stringResource(Res.string.profile_rd_password_repeat_ph),
                accent = accent,
                isPassword = true,
                passwordVisible = passVisible,
                onTogglePasswordVisible = { passVisible = !passVisible },
            )
            Spacer(Modifier.height(10.dp))
            ProfField(
                value = securityCode,
                onValueChange = { securityCode = it },
                label = stringResource(Res.string.profile_rd_security_code),
                placeholder = stringResource(Res.string.profile_rd_security_code_ph),
                accent = accent,
                leading = Icons.Filled.Security,
            )
            Spacer(Modifier.height(14.dp))
            ProfActionButton(
                modifier = Modifier.fillMaxWidth(),
                filled = true,
                accent = accent,
                icon = Icons.Filled.Lock,
                label = stringResource(Res.string.profile_rd_password_update),
                enabled = !passwordBusy,
                onClick = { submitPasswordChange() },
            )
            if (passwordBusy) {
                Spacer(Modifier.height(8.dp))
                CircularProgressIndicator(Modifier.size(18.dp), color = accent, strokeWidth = 2.dp)
            }
            passwordMessage?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = if (passwordError) ProfError else accent, fontSize = 12.sp)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 22.dp, vertical = 18.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(Icons.Filled.Person, null, tint = accent, modifier = Modifier.size(26.dp))
                Text(
                    stringResource(Res.string.profile_rd_title).uppercase(),
                    color = ProfText,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                )
            }
            Text(
                stringResource(Res.string.profile_rd_hint),
                color = ProfMuted,
                fontSize = 13.sp,
                lineHeight = 19.sp,
            )
        }

        if (state.isLoading) {
            Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = accent)
            }
            return@Column
        }

        if (!state.isLoggedIn) {
            ProfSectionCard {
                Text(
                    stringResource(Res.string.profile_rd_login_hint),
                    color = ProfMuted,
                    fontSize = 13.sp,
                )
                Spacer(Modifier.height(12.dp))
                ProfField(
                    value = loginId,
                    onValueChange = { loginId = it; loginError = null },
                    label = stringResource(Res.string.login_email_or_username),
                    accent = accent,
                    leading = Icons.Filled.Person,
                )
                Spacer(Modifier.height(10.dp))
                ProfField(
                    value = loginPass,
                    onValueChange = { loginPass = it; loginError = null },
                    label = stringResource(Res.string.login_password),
                    accent = accent,
                    leading = Icons.Filled.Lock,
                    isPassword = true,
                    passwordVisible = passVisible,
                    onTogglePasswordVisible = { passVisible = !passVisible },
                )
                loginError?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = ProfError, fontSize = 12.sp)
                }
                Spacer(Modifier.height(14.dp))
                ProfActionButton(
                    filled = true,
                    accent = accent,
                    icon = Icons.Filled.Lock,
                    label = stringResource(Res.string.login_sign_in),
                    enabled = loginId.isNotBlank() && loginPass.isNotBlank(),
                    onClick = {
                        viewModel.signInWithCredentials(loginId.trim(), loginPass) { result ->
                            loginError = if (result.isFailure) {
                                result.exceptionOrNull()?.message ?: invalidLogin
                            } else null
                        }
                    },
                )
            }
            return@Column
        }

        if (phoneUi) {
            AccountCard()
            PersonalCard()
            PasswordCard()
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                AccountCard(modifier = Modifier.weight(0.38f))
                Column(
                    modifier = Modifier.weight(0.62f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    PersonalCard()
                    PasswordCard()
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ProfInfoBg.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                .border(1.dp, accent.copy(alpha = 0.28f), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(Icons.Filled.Info, null, tint = accent, modifier = Modifier.size(18.dp))
            Text(
                stringResource(Res.string.profile_rd_privacy_note),
                color = ProfText.copy(alpha = 0.88f),
                fontSize = 12.sp,
                lineHeight = 18.sp,
            )
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun userTypeLabel(type: UserType): String = when (type) {
    UserType.Admin -> stringResource(Res.string.user_type_admin)
    UserType.Premium -> stringResource(Res.string.user_type_premium)
    UserType.Service -> stringResource(Res.string.user_type_service)
    UserType.Customer -> stringResource(Res.string.user_type_customer)
    UserType.Standard -> stringResource(Res.string.user_type_standard)
    UserType.Guest -> stringResource(Res.string.user_type_guest)
}

@Composable
private fun timezoneLabel(id: String): String = when (id) {
    "Europe/London" -> stringResource(Res.string.profile_rd_tz_london)
    "UTC" -> stringResource(Res.string.profile_rd_tz_utc)
    else -> stringResource(Res.string.profile_rd_tz_istanbul)
}

@Composable
private fun ProfSectionCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val radius = appCardCornerRadius()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(ProfCard, RoundedCornerShape(radius))
            .border(1.dp, ProfBorder, RoundedCornerShape(radius))
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        content()
    }
}

@Composable
private fun ProfMetaRow(
    label: String,
    value: String,
    accent: Color,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(label, color = ProfMuted, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Text(
            value,
            color = ProfText,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        trailing?.invoke()
    }
}

@Composable
private fun ProfInfoRow(
    icon: ImageVector,
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(icon, null, tint = ProfMuted, modifier = Modifier.size(18.dp))
        Text(label, color = ProfMuted, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(
            value,
            color = ProfText,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ProfPasswordStrength(password: String) {
    val score = remember(password) { passwordStrengthScore(password) }
    val label = when (score) {
        0, 1 -> stringResource(Res.string.profile_rd_password_weak)
        2 -> stringResource(Res.string.profile_rd_password_medium)
        else -> stringResource(Res.string.profile_rd_password_strong)
    }
    val barColor = when (score) {
        0, 1 -> Color(0xFFE35D5D)
        2 -> Color(0xFFF0A020)
        else -> Color(0xFF2ECC71)
    }
    val progress = when (score) {
        0 -> 0.08f
        1 -> 0.33f
        2 -> 0.66f
        else -> 1f
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "${stringResource(Res.string.profile_rd_password_strength)}: $label",
            color = ProfMuted,
            fontSize = 12.sp,
        )
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(ProfBorder, RoundedCornerShape(2.dp)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(4.dp)
                    .background(barColor, RoundedCornerShape(2.dp)),
            )
        }
    }
}

private fun passwordStrengthScore(password: String): Int {
    if (password.isEmpty()) return 0
    var score = 0
    if (password.length >= 6) score++
    if (password.length >= 10) score++
    val hasLower = password.any { it.isLowerCase() }
    val hasUpper = password.any { it.isUpperCase() }
    val hasDigit = password.any { it.isDigit() }
    val hasSymbol = password.any { !it.isLetterOrDigit() }
    if ((hasLower && hasUpper) || (hasDigit && (hasLower || hasUpper))) score++
    if (hasSymbol && password.length >= 8) score++
    return score.coerceIn(0, 3)
}

@Composable
private fun ProfField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    accent: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leading: ImageVector? = null,
    placeholder: String? = null,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onTogglePasswordVisible: (() -> Unit)? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth().heightIn(min = 64.dp),
        enabled = enabled,
        singleLine = true,
        textStyle = TextStyle(
            color = ProfText,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Medium,
        ),
        label = { Text(label, fontSize = 11.sp) },
        placeholder = placeholder?.let { { Text(it, color = ProfMuted, fontSize = 12.sp) } },
        leadingIcon = leading?.let { { Icon(it, null, tint = ProfMuted, modifier = Modifier.size(16.dp)) } },
        trailingIcon = if (isPassword && onTogglePasswordVisible != null) {
            {
                IconButton(onClick = onTogglePasswordVisible) {
                    Icon(
                        if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        null,
                        tint = ProfMuted,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        } else null,
        visualTransformation = if (isPassword && !passwordVisible) {
            PasswordVisualTransformation()
        } else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(12.dp),
        colors = TextFieldDefaults.outlinedTextFieldColors(
            textColor = ProfText,
            backgroundColor = ProfTile,
            cursorColor = accent,
            focusedBorderColor = accent.copy(alpha = 0.7f),
            unfocusedBorderColor = ProfBorder,
            disabledTextColor = ProfText.copy(alpha = 0.85f),
            disabledBorderColor = ProfBorder,
            focusedLabelColor = accent,
            unfocusedLabelColor = ProfMuted,
            disabledLabelColor = ProfMuted,
            placeholderColor = ProfMuted,
        ),
    )
}

@Composable
private fun ProfSelectField(
    label: String,
    value: String,
    accent: Color,
    icon: ImageVector,
    enabled: Boolean,
    expanded: Boolean,
    onExpand: () -> Unit,
    onDismiss: () -> Unit,
    menu: @Composable () -> Unit,
) {
    Box {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .clickable(enabled = enabled, onClick = onExpand),
            enabled = false,
            readOnly = true,
            singleLine = true,
            textStyle = TextStyle(
                color = ProfText,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Medium,
            ),
            label = { Text(label, fontSize = 11.sp) },
            leadingIcon = { Icon(icon, null, tint = ProfMuted, modifier = Modifier.size(16.dp)) },
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                disabledTextColor = ProfText,
                disabledBorderColor = ProfBorder,
                disabledLabelColor = ProfMuted,
                backgroundColor = ProfTile,
            ),
        )
        DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) { menu() }
    }
}

@Composable
private fun ProfChipButton(
    accent: Color,
    filled: Boolean,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    val bg = if (filled) accent else Color.Transparent
    val fg = if (filled) ProfInk else accent
    Row(
        modifier = Modifier
            .height(34.dp)
            .background(bg, RoundedCornerShape(10.dp))
            .border(1.dp, accent.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, null, tint = fg, modifier = Modifier.size(14.dp))
        Text(label, color = fg, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ProfOutlineAction(
    color: Color,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .border(1.dp, color.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, color = color, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ProfActionButton(
    filled: Boolean,
    accent: Color,
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(12.dp)
    val bg = if (filled && enabled) accent else accent.copy(alpha = 0.35f)
    Row(
        modifier = modifier
            .widthIn(min = 140.dp)
            .height(46.dp)
            .background(bg, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, tint = ProfInk, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, color = ProfInk, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

private fun splitFullName(fullName: String): Pair<String, String> {
    val parts = fullName.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    if (parts.isEmpty()) return "" to ""
    if (parts.size == 1) return parts.first() to ""
    return parts.first() to parts.drop(1).joinToString(" ")
}

private fun joinFullName(first: String, last: String): String =
    listOf(first.trim(), last.trim()).filter { it.isNotEmpty() }.joinToString(" ")

private fun formatProfileUserId(username: String, email: String): String {
    val raw = username.trim().ifBlank { email.substringBefore("@") }.ifBlank { "00012345" }
    val digits = raw.filter { it.isDigit() }
    val id = if (digits.length >= 4) digits.takeLast(8).padStart(8, '0') else raw.uppercase().take(8).padStart(8, '0')
    return "VG-$id"
}

private fun formatProfileDateTime(epochMs: Long): String? {
    if (epochMs <= 0L) return null
    val local = Instant.fromEpochMilliseconds(epochMs).toLocalDateTime(TimeZone.currentSystemDefault())
    val d = local.dayOfMonth.toString().padStart(2, '0')
    val m = local.monthNumber.toString().padStart(2, '0')
    val y = local.year
    val hh = local.hour.toString().padStart(2, '0')
    val mm = local.minute.toString().padStart(2, '0')
    return "$d.$m.$y $hh:$mm"
}

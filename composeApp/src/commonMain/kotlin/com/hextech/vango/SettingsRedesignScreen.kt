package com.hextech.vango

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Monitor
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import testvango.composeapp.generated.resources.Res
import testvango.composeapp.generated.resources.profile_title
import testvango.composeapp.generated.resources.settings_about_title
import testvango.composeapp.generated.resources.settings_connection_title
import testvango.composeapp.generated.resources.settings_language_menu_title
import testvango.composeapp.generated.resources.settings_panel_title
import testvango.composeapp.generated.resources.settings_rd_about_desc
import testvango.composeapp.generated.resources.settings_rd_coming_soon
import testvango.composeapp.generated.resources.settings_rd_connection_desc
import testvango.composeapp.generated.resources.settings_rd_header_subtitle
import testvango.composeapp.generated.resources.settings_rd_header_title
import testvango.composeapp.generated.resources.settings_rd_language_desc
import testvango.composeapp.generated.resources.settings_rd_lock_again
import testvango.composeapp.generated.resources.settings_rd_login
import testvango.composeapp.generated.resources.settings_rd_notifications_desc
import testvango.composeapp.generated.resources.settings_rd_notifications_title
import testvango.composeapp.generated.resources.settings_rd_panel_access_body
import testvango.composeapp.generated.resources.settings_rd_panel_access_title
import testvango.composeapp.generated.resources.settings_rd_panel_desc
import testvango.composeapp.generated.resources.settings_rd_panel_page_subtitle
import testvango.composeapp.generated.resources.settings_rd_panel_page_title
import testvango.composeapp.generated.resources.settings_rd_password
import testvango.composeapp.generated.resources.settings_rd_password_error
import testvango.composeapp.generated.resources.settings_rd_profile_desc
import testvango.composeapp.generated.resources.settings_rd_theme_desc
import testvango.composeapp.generated.resources.settings_rd_unauthorized_hint
import testvango.composeapp.generated.resources.settings_rd_username
import testvango.composeapp.generated.resources.settings_theme_menu_title

private val SetBg: Color @Composable get() = LocalAppChrome.current.bg
private val SetPanel: Color @Composable get() = LocalAppChrome.current.panel
private val SetCard: Color @Composable get() = LocalAppChrome.current.card
private val SetCyan = Color(0xFF2EC5FF)
private val SetText: Color @Composable get() = LocalAppChrome.current.text
private val SetMuted: Color @Composable get() = LocalAppChrome.current.muted
private val SetBorder: Color @Composable get() = LocalAppChrome.current.border
private val SetField: Color @Composable get() = LocalAppChrome.current.field

private const val PANEL_USER = "admin"
private const val PANEL_PASS = "1234"

/**
 * New tablet settings shell (sidebar + content).
 * Legacy [Settings] remains in the project but is unused on tablet.
 */
@Composable
fun SettingsRedesignScreen() {
    val accent = accentColorFromId(customAppAccentId)
    val chrome = LocalAppChrome.current
    val shellBg = chrome.bg
    val shellPanel = chrome.panel
    var selected by remember { mutableStateOf(dashboardSettingsNav) }
    // Panel editor requires login (default locked).
    var panelUnlocked by remember { mutableStateOf(false) }
    val phoneUi = isPhoneUi()
    var detailOpen by remember { mutableStateOf(dashboardSettingsDetailOpen || !phoneUi) }

    LaunchedEffect(selected) {
        dashboardSettingsNav = selected
    }
    LaunchedEffect(dashboardSettingsNav) {
        if (dashboardSettingsNav != selected) {
            selected = dashboardSettingsNav
        }
    }
    LaunchedEffect(dashboardSettingsDetailOpen, phoneUi) {
        if (phoneUi && dashboardSettingsDetailOpen != detailOpen) {
            detailOpen = dashboardSettingsDetailOpen
        }
    }
    LaunchedEffect(detailOpen) {
        dashboardSettingsDetailOpen = detailOpen
    }
    LaunchedEffect(phoneUi) {
        if (!phoneUi) detailOpen = true
    }

    @Composable
    fun DetailPane() {
        when (selected) {
            DashboardSettingsNav.Panel -> {
                if (panelUnlocked) {
                    PanelSettingsContentUnlocked(
                        onLock = { panelUnlocked = false },
                    )
                } else {
                    PanelAccessLoginScreen(
                        onSuccess = { panelUnlocked = true },
                    )
                }
            }
            DashboardSettingsNav.Connection -> ConnectionRedesignPage()
            DashboardSettingsNav.Theme -> ThemeRedesignPage()
            DashboardSettingsNav.Language -> LanguageRedesignPage()
            DashboardSettingsNav.Profile -> ProfileRedesignPage()
            DashboardSettingsNav.Notifications -> NotificationsRedesignPage()
            DashboardSettingsNav.About -> AboutRedesignPage()
        }
    }

    if (phoneUi) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(shellBg),
        ) {
            if (!detailOpen) {
                SettingsSidebar(
                    selected = selected,
                    accent = accent,
                    panelBg = shellPanel,
                    onSelect = { item ->
                        selected = item
                        detailOpen = true
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { detailOpen = false },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text("‹", color = accent, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                            Text(
                                settingsNavTitle(selected),
                                color = SetText,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (selected == DashboardSettingsNav.Panel) {
                            Icon(
                                if (panelUnlocked) Icons.Filled.LockOpen else Icons.Filled.Lock,
                                contentDescription = stringResource(Res.string.settings_rd_lock_again),
                                tint = if (panelUnlocked) accent else SetMuted,
                                modifier = Modifier
                                    .size(22.dp)
                                    .clickable {
                                        if (panelUnlocked) {
                                            panelUnlocked = false
                                        }
                                    },
                            )
                        }
                    }
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        DetailPane()
                    }
                }
            }
        }
        return
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(shellBg)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SettingsSidebar(
            selected = selected,
            accent = accent,
            panelBg = shellPanel,
            onSelect = { item ->
                selected = item
            },
            modifier = Modifier
                .width(280.dp)
                .fillMaxHeight(),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(SetCard, RoundedCornerShape(appCardCornerRadius()))
                .border(1.dp, SetBorder, RoundedCornerShape(appCardCornerRadius())),
        ) {
            DetailPane()
        }
    }
}

@Composable
private fun settingsNavTitle(nav: DashboardSettingsNav): String = when (nav) {
    DashboardSettingsNav.Panel -> stringResource(Res.string.settings_panel_title)
    DashboardSettingsNav.Connection -> stringResource(Res.string.settings_connection_title)
    DashboardSettingsNav.Theme -> stringResource(Res.string.settings_theme_menu_title)
    DashboardSettingsNav.Language -> stringResource(Res.string.settings_language_menu_title)
    DashboardSettingsNav.Profile -> stringResource(Res.string.profile_title)
    DashboardSettingsNav.Notifications -> stringResource(Res.string.settings_rd_notifications_title)
    DashboardSettingsNav.About -> stringResource(Res.string.settings_about_title)
}

@Composable
private fun SettingsSidebar(
    selected: DashboardSettingsNav,
    accent: Color,
    panelBg: Color,
    onSelect: (DashboardSettingsNav) -> Unit,
    modifier: Modifier = Modifier,
) {
    val phoneUi = isPhoneUi()
    val radius = appCardCornerRadius()
    val cardBg = LocalAppChrome.current.card
    val items = listOf(
        SettingsNavSpec(
            DashboardSettingsNav.Panel,
            Icons.Filled.Monitor,
            Color(0xFF4DA3FF),
            stringResource(Res.string.settings_panel_title),
            stringResource(Res.string.settings_rd_panel_desc),
        ),
        SettingsNavSpec(
            DashboardSettingsNav.Connection,
            Icons.Filled.Wifi,
            Color(0xFF21C47E),
            stringResource(Res.string.settings_connection_title),
            stringResource(Res.string.settings_rd_connection_desc),
        ),
        SettingsNavSpec(
            DashboardSettingsNav.Theme,
            Icons.Filled.ColorLens,
            Color(0xFFB388FF),
            stringResource(Res.string.settings_theme_menu_title),
            stringResource(Res.string.settings_rd_theme_desc),
        ),
        SettingsNavSpec(
            DashboardSettingsNav.Language,
            Icons.Outlined.Language,
            Color(0xFFF5C542),
            stringResource(Res.string.settings_language_menu_title),
            stringResource(Res.string.settings_rd_language_desc),
        ),
        SettingsNavSpec(
            DashboardSettingsNav.Profile,
            Icons.Filled.Person,
            Color(0xFF2EC5C0),
            stringResource(Res.string.profile_title),
            stringResource(Res.string.settings_rd_profile_desc),
        ),
        SettingsNavSpec(
            DashboardSettingsNav.Notifications,
            Icons.Filled.Notifications,
            Color(0xFFFF5A5A),
            stringResource(Res.string.settings_rd_notifications_title),
            stringResource(Res.string.settings_rd_notifications_desc),
        ),
        SettingsNavSpec(
            DashboardSettingsNav.About,
            Icons.Filled.HelpOutline,
            Color(0xFF4DA3FF),
            stringResource(Res.string.settings_about_title),
            stringResource(Res.string.settings_rd_about_desc),
        ),
    )

    Column(
        modifier = modifier
            .then(
                if (phoneUi) {
                    Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                } else {
                    Modifier
                        .background(panelBg, RoundedCornerShape(18.dp))
                        .border(1.dp, SetBorder, RoundedCornerShape(18.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                },
            ),
        verticalArrangement = Arrangement.spacedBy(if (phoneUi) 6.dp else 6.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(if (phoneUi) 34.dp else 38.dp)
                        .border(1.5.dp, accent.copy(alpha = 0.7f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Settings,
                        null,
                        tint = accent,
                        modifier = Modifier.size(if (phoneUi) 18.dp else 22.dp),
                    )
                }
                Text(
                    stringResource(Res.string.settings_rd_header_title).uppercase(),
                    color = SetText,
                    fontSize = if (phoneUi) 20.sp else 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.7.sp,
                )
            }
            Text(
                stringResource(Res.string.settings_rd_header_subtitle),
                color = SetMuted,
                fontSize = if (phoneUi) 12.sp else 11.sp,
                lineHeight = if (phoneUi) 15.sp else 15.sp,
                maxLines = if (phoneUi) 2 else 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        items.forEach { item ->
            SettingsNavRow(
                spec = item,
                selected = selected == item.id,
                accent = accent,
                cardBg = cardBg,
                radius = radius,
                compact = phoneUi,
                onClick = { onSelect(item.id) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

private data class SettingsNavSpec(
    val id: DashboardSettingsNav,
    val icon: ImageVector,
    val iconTint: Color,
    val title: String,
    val desc: String,
)

@Composable
private fun SettingsNavRow(
    spec: SettingsNavSpec,
    selected: Boolean,
    accent: Color,
    cardBg: Color,
    radius: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val shape = RoundedCornerShape(radius)
    val padV = if (compact) 8.dp else 12.dp
    val padH = if (compact) 12.dp else 14.dp
    val iconBox = if (compact) 40.dp else 44.dp
    val iconSize = if (compact) 22.dp else 24.dp
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(cardBg, shape)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) accent else SetBorder,
                shape = shape,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = padH, vertical = padV),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(iconBox)
                .background(spec.iconTint.copy(alpha = 0.16f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                spec.icon,
                null,
                tint = spec.iconTint,
                modifier = Modifier.size(iconSize),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                spec.title,
                color = SetText,
                fontSize = if (compact) 14.sp else 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(1.dp))
            Text(
                spec.desc,
                color = SetMuted,
                fontSize = if (compact) 11.sp else 12.sp,
                lineHeight = if (compact) 14.sp else 16.sp,
                maxLines = if (compact) 1 else 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            null,
            tint = SetMuted.copy(alpha = 0.7f),
            modifier = Modifier.size(if (compact) 18.dp else 20.dp),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PanelAccessLoginScreen(
    onSuccess: () -> Unit,
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPass by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val passwordError = stringResource(Res.string.settings_rd_password_error)
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val passwordFocus = remember { FocusRequester() }
    val passwordBringIntoView = remember { BringIntoViewRequester() }

    fun tryLogin() {
        if (username.trim() == PANEL_USER && password == PANEL_PASS) {
            error = null
            focusManager.clearFocus()
            onSuccess()
        } else {
            error = passwordError
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(scrollState)
            .padding(horizontal = 22.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(Icons.Filled.Monitor, null, tint = SetCyan, modifier = Modifier.size(30.dp))
                Text(
                    stringResource(Res.string.settings_rd_panel_page_title).uppercase(),
                    color = SetText,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp,
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(Res.string.settings_rd_panel_page_subtitle),
                color = SetMuted,
                fontSize = 14.sp,
            )
        }

        Spacer(Modifier.height(18.dp))

        Column(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth(0.92f)
                .background(SetPanel, RoundedCornerShape(18.dp))
                .border(1.dp, SetBorder, RoundedCornerShape(18.dp))
                .padding(horizontal = 28.dp, vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(SetCyan.copy(alpha = 0.12f), CircleShape)
                    .border(1.5.dp, SetCyan.copy(alpha = 0.45f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Lock, null, tint = SetCyan, modifier = Modifier.size(34.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(Res.string.settings_rd_panel_access_title).uppercase(),
                color = SetText,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(Res.string.settings_rd_panel_access_body),
                color = SetMuted,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))

            SettingsLoginField(
                value = username,
                onValueChange = { username = it; error = null },
                placeholder = stringResource(Res.string.settings_rd_username),
                leading = Icons.Filled.Person,
                imeAction = ImeAction.Next,
                onNext = { passwordFocus.requestFocus() },
            )
            Spacer(Modifier.height(12.dp))
            SettingsLoginField(
                value = password,
                onValueChange = { password = it; error = null },
                placeholder = stringResource(Res.string.settings_rd_password),
                leading = Icons.Filled.Lock,
                isPassword = true,
                showPassword = showPass,
                onTogglePassword = { showPass = !showPass },
                imeAction = ImeAction.Done,
                onDone = { tryLogin() },
                modifier = Modifier
                    .focusRequester(passwordFocus)
                    .bringIntoViewRequester(passwordBringIntoView)
                    .onFocusChanged { state ->
                        if (state.isFocused) {
                            scope.launch {
                                passwordBringIntoView.bringIntoView()
                                scrollState.animateScrollTo(scrollState.maxValue)
                            }
                        }
                    },
            )

            if (error != null) {
                Spacer(Modifier.height(10.dp))
                Text(error!!, color = Color(0xFFFF6B6B), fontSize = 13.sp)
            }

            Spacer(Modifier.height(18.dp))
            Button(
                onClick = { tryLogin() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = SetCyan,
                    contentColor = Color(0xFF061018),
                ),
                elevation = ButtonDefaults.elevation(0.dp, 0.dp),
            ) {
                Text(
                    stringResource(Res.string.settings_rd_login).uppercase(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    letterSpacing = 0.6.sp,
                )
            }

            Spacer(Modifier.height(18.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Filled.Info, null, tint = SetMuted, modifier = Modifier.size(16.dp))
                Text(
                    stringResource(Res.string.settings_rd_unauthorized_hint),
                    color = SetMuted,
                    fontSize = 12.sp,
                )
            }
        }

        // Extra room so keyboard never permanently covers password / login button
        Spacer(Modifier.height(120.dp))
    }
}

@Composable
private fun SettingsLoginField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leading: ImageVector,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    showPassword: Boolean = false,
    onTogglePassword: (() -> Unit)? = null,
    imeAction: ImeAction = ImeAction.Next,
    onNext: (() -> Unit)? = null,
    onDone: (() -> Unit)? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        placeholder = { Text(placeholder, color = SetMuted) },
        leadingIcon = { Icon(leading, null, tint = SetMuted) },
        trailingIcon = if (isPassword && onTogglePassword != null) {
            {
                IconButton(onClick = onTogglePassword) {
                    Icon(
                        if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        null,
                        tint = SetMuted,
                    )
                }
            }
        } else null,
        visualTransformation = if (isPassword && !showPassword) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
        keyboardOptions = KeyboardOptions(imeAction = imeAction),
        keyboardActions = KeyboardActions(
            onNext = { onNext?.invoke() },
            onDone = { onDone?.invoke() },
        ),
        shape = RoundedCornerShape(12.dp),
        colors = TextFieldDefaults.outlinedTextFieldColors(
            textColor = SetText,
            backgroundColor = SetField,
            cursorColor = SetCyan,
            focusedBorderColor = SetCyan.copy(alpha = 0.7f),
            unfocusedBorderColor = SetBorder,
            placeholderColor = SetMuted,
        ),
    )
}

@Composable
private fun PanelSettingsContentUnlocked(
    onLock: () -> Unit,
) {
    PanelButtonsEditorContent(onLock = onLock)
}

@Composable
private fun SettingsComingSoon(title: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, color = SetText, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(Res.string.settings_rd_coming_soon),
                color = SetMuted,
                fontSize = 14.sp,
            )
        }
    }
}

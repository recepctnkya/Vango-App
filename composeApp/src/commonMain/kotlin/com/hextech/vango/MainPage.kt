package com.hextech.vango

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Typography
import androidx.compose.material.darkColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.lightColors
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.hextech.vango.profile.SplashLogoState
import com.hextech.vango.profile.decodeProfileImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import testvango.composeapp.generated.resources.Res
import testvango.composeapp.generated.resources.footer_cancel
import testvango.composeapp.generated.resources.footer_home
import testvango.composeapp.generated.resources.footer_save
import testvango.composeapp.generated.resources.footer_menu
import testvango.composeapp.generated.resources.footer_profile
import androidx.compose.material.icons.filled.ArrowBack


enum class SelectedPage {
    UserInfo,
    Rules,
    /** Ayarlar ızgarası (eski Settings sekmesi). */
    Menu,
    Profile,
}

data class UIState (
    val selectedTab: SelectedPage,
    val settingsPage: SettingsPage,
    val devConfig: DevConfig
)

var uiState = MutableStateFlow(UIState(
    selectedTab = SelectedPage.UserInfo,
    settingsPage = SettingsPage.Main,
    devConfig = DevConfig("", 0, ByteArray(16){0}, 0, ByteArray(4){0}, 0, ByteArray(5){0}, "0", "0")
))

/** Ensure first paint after process start uses the new dashboard home once. */
private var forcedDashboardHomeOnLaunch = false

@Composable
fun MainApp() {
    LaunchedEffect(Unit) {
        if (!forcedDashboardHomeOnLaunch) {
            forcedDashboardHomeOnLaunch = true
            uiState.value = uiState.value.copy(
                selectedTab = SelectedPage.UserInfo,
                settingsPage = SettingsPage.Main,
            )
        }
    }

    // One APK — phone vs tablet UI is chosen via isPhoneUi() inside DashboardHomeScreen.
    VanGoAppShell()
}

@Composable
fun PhoneLayout() {
    val data by uiState.collectAsState()
    val hasInternalBack =
        (data.selectedTab == SelectedPage.Menu && data.settingsPage != SettingsPage.Main) ||
            data.selectedTab == SelectedPage.Profile
    PlatformBackHandler(enabled = hasInternalBack) {
        when {
            data.selectedTab == SelectedPage.Menu && data.settingsPage == SettingsPage.Remote ->
                uiState.value = data.copy(settingsPage = SettingsPage.Connection)
            data.selectedTab == SelectedPage.Menu && data.settingsPage == SettingsPage.BlePair ->
                uiState.value = data.copy(settingsPage = SettingsPage.Connection)
            data.selectedTab == SelectedPage.Menu && data.settingsPage != SettingsPage.Main ->
                uiState.value = data.copy(settingsPage = SettingsPage.Main)
            data.selectedTab == SelectedPage.Profile ->
                uiState.value = data.copy(selectedTab = SelectedPage.Menu, settingsPage = SettingsPage.Main)
            data.selectedTab == SelectedPage.Menu ->
                uiState.value = data.copy(selectedTab = SelectedPage.UserInfo, settingsPage = SettingsPage.Main)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Fixed Header
            Header()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)  // Takes remaining space
            ) {
                // Show different content based on selected tab
                when (uiState.value.selectedTab) {
                    SelectedPage.UserInfo -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(5.dp)
                                .background(MaterialTheme.colors.background)
                        ) {
                            PhoneUI()
                        }
                    }
                    SelectedPage.Rules -> { /* Temporarily disabled Rules tab */ } // RuleContent()
                    SelectedPage.Menu, SelectedPage.Profile -> Settings()
                }
            }
            Box(
//                    modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Footer(
                    selectedTab = data.selectedTab,
                    onTabSelected = { tab ->
                        when (tab) {
                            SelectedPage.Menu ->
                                uiState.value = data.copy(
                                    selectedTab = SelectedPage.Menu,
                                    settingsPage = SettingsPage.Main,
                                )
                            SelectedPage.Profile ->
                                uiState.value = data.copy(
                                    selectedTab = SelectedPage.Profile,
                                    settingsPage = SettingsPage.Profile,
                                )
                            else ->
                                uiState.value = data.copy(
                                    selectedTab = tab,
                                    settingsPage = SettingsPage.Main,
                                )
                        }
                    }
                )
            }
        }
    }
}


@Composable
fun AppTopBar(
    modifier: Modifier = Modifier,
    barHeight: Dp = 62.dp,
    logoSize: Dp = 56.dp,
    statusIconSize: Dp = 24.dp,
) {
    val global by vanGoGlobal.collectAsState()
    val appConn by appConnectionState.collectAsState()
    val bleConnected = isBleGattLinkReady()
    val vanData by vanGoData.collectAsState()
    var wifiOk by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            wifiOk = isWifiConnected()
            delay(1500)
        }
    }
    val remoteOk = isRemoteControlActive(vanData.slvConn)
    val activeTint = Color(0xFF43A047)
    val inactiveTint = Color(0xFF9E9E9E)

    val customLogoBytes = SplashLogoState.customLogoBytes
    val customLogoBitmap = remember(customLogoBytes) { customLogoBytes?.let { decodeProfileImage(it) } }
    val customLogoSize = logoSize * 1.22f

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(barHeight)
            .background(MaterialTheme.colors.surface)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (customLogoBitmap != null) {
                Image(
                    painter = BitmapPainter(customLogoBitmap),
                    contentDescription = null,
                    modifier = Modifier.size(customLogoSize)
                )
            } else {
                Image(
                    painter = painterResource(getAppContext().vanGO.getHexnetIcon()),
                    contentDescription = null,
                    modifier = Modifier.size(logoSize)
                )
            }
            Text(
                text = "VANGO",
                color = MaterialTheme.colors.onSurface,
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold
            )
        }
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.Center
        ) {
            if (appConn == AppConnectionState.REMOTE_MQTT_CONNECTED || isRemoteUserModeActive()) {
                Text(
                    text = if (appConn == AppConnectionState.REMOTE_MQTT_CONNECTED) "Remote" else "Remote…",
                    color = activeTint,
                    style = MaterialTheme.typography.caption,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 1.dp)
                )
            }
            Row(
                modifier = Modifier.offset(y = (-2).dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Bluetooth,
                    contentDescription = "Bluetooth",
                    modifier = Modifier.size(statusIconSize),
                    tint = if (bleConnected) activeTint else inactiveTint
                )
                Icon(
                    imageVector = Icons.Filled.Wifi,
                    contentDescription = "Wi‑Fi",
                    modifier = Modifier.size(statusIconSize),
                    tint = if (wifiOk) activeTint else inactiveTint
                )
                Icon(
                    imageVector = Icons.Filled.Cloud,
                    contentDescription = "Remote",
                    modifier = Modifier.size(statusIconSize),
                    tint = if (remoteOk) activeTint else inactiveTint
                )
            }
        }
    }
}

@Composable
fun Header() {
    AppTopBar(
        barHeight = 62.dp,
        logoSize = 56.dp,
        statusIconSize = 24.dp
    )
}

@Composable
fun ScrollableContent(content: @Composable ()->Unit) {
    Column(
        modifier = Modifier
            .padding(5.dp)
            .fillMaxWidth()
            .fillMaxSize()
            .background(MaterialTheme.colors.background)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        content()
    }
}

@Composable
fun Footer(
    selectedTab: SelectedPage,
    onTabSelected: (SelectedPage) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val w = maxWidth.value.coerceAtLeast(280f)
        val deviceBoost = if (isTablet()) 1.12f else 1f
        val scale = (w / 380f).coerceIn(0.92f, 1.38f) * deviceBoost
        val barH = (60f * scale).dp.coerceIn(52.dp, 88.dp)
        val iconSz = (26f * scale).dp.coerceIn(22.dp, 34.dp)
        val padRow = (25f * scale).dp.coerceIn(16.dp, 36.dp)
        val padIcon = (8f * scale).dp.coerceIn(6.dp, 14.dp)
        val labelSp = (11f * scale).coerceIn(10f, 14f).sp

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(barH)
                .background(MaterialTheme.colors.primary)
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FooterIcon(
                    Icons.Default.Home,
                    stringResource(Res.string.footer_home),
                    isSelected = selectedTab == SelectedPage.UserInfo,
                    onClick = { onTabSelected(SelectedPage.UserInfo) },
                    iconSize = iconSz,
                    contentPadding = padIcon,
                    labelFontSize = labelSp,
                    modifier = Modifier.weight(1f),
                )
                FooterIcon(
                    Icons.Default.Menu,
                    stringResource(Res.string.footer_menu),
                    isSelected = selectedTab == SelectedPage.Menu,
                    onClick = { onTabSelected(SelectedPage.Menu) },
                    iconSize = iconSz,
                    contentPadding = padIcon,
                    labelFontSize = labelSp,
                    modifier = Modifier.weight(1f),
                    testTag = VanGoTestTags.FOOTER_MENU,
                )
                FooterIcon(
                    Icons.Default.Person,
                    stringResource(Res.string.footer_profile),
                    isSelected = selectedTab == SelectedPage.Profile,
                    onClick = { onTabSelected(SelectedPage.Profile) },
                    iconSize = iconSz,
                    contentPadding = padIcon,
                    labelFontSize = labelSp,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
fun FooterIcon(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    iconSize: Dp = 24.dp,
    contentPadding: Dp = 8.dp,
    labelFontSize: androidx.compose.ui.unit.TextUnit? = null,
    modifier: Modifier = Modifier,
    testTag: String? = null,
) {
    val captionStyle = if (labelFontSize != null) {
        MaterialTheme.typography.caption.copy(fontSize = labelFontSize)
    } else {
        MaterialTheme.typography.caption
    }
    Column(
        modifier = modifier
            .then(if (testTag != null) Modifier.vangoTestTag(testTag) else Modifier)
            .clickable(onClick = onClick)
            .padding(contentPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(iconSize),
            tint = if (isSelected)
                MaterialTheme.colors.onPrimary
            else
                MaterialTheme.colors.onPrimary.copy(alpha = 0.6f)
        )
        Text(
            text = label,
            style = captionStyle,
            color = if (isSelected)
                MaterialTheme.colors.onPrimary
            else
                MaterialTheme.colors.onPrimary.copy(alpha = 0.6f)
        )
    }
}

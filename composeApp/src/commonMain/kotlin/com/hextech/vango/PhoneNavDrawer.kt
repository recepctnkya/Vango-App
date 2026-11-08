package com.hextech.vango

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbIncandescent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import testvango.composeapp.generated.resources.Res
import testvango.composeapp.generated.resources.dashboard_brand_tagline
import testvango.composeapp.generated.resources.dashboard_drawer_logout
import testvango.composeapp.generated.resources.dashboard_drawer_system
import testvango.composeapp.generated.resources.dashboard_nav_climate
import testvango.composeapp.generated.resources.dashboard_nav_dim
import testvango.composeapp.generated.resources.dashboard_nav_energy_monitor
import testvango.composeapp.generated.resources.dashboard_nav_home
import testvango.composeapp.generated.resources.dashboard_nav_motor
import testvango.composeapp.generated.resources.dashboard_nav_settings
import testvango.composeapp.generated.resources.dashboard_nav_water
import testvango.composeapp.generated.resources.hexnet_mark
import testvango.composeapp.generated.resources.hexon_avatar
import testvango.composeapp.generated.resources.hexon_drawer_show_on_home
import testvango.composeapp.generated.resources.hexon_drawer_title
import testvango.composeapp.generated.resources.settings_about_title
import testvango.composeapp.generated.resources.settings_rd_notifications_title

private val DrawerBg = Color(0xFF0E1218)
private val DrawerBorder = Color(0xFF2A313C)
private val DrawerGreen = Color(0xFF3DDC84)
private val DrawerMuted = Color(0xFF8B95A5)
private val DrawerActiveBg = Color(0xFF143528)
private val DrawerActiveBorder = Color(0xFF2A6B4A)

enum class PhoneDrawerDestination {
    Home, Dim, Water, Climate, Motor, Energy, Settings, Notifications, About, Logout
}

/**
 * Phone-only left hamburger drawer (mockup): brand header, nav items, system block, logout.
 */
@Composable
fun PhoneNavDrawerOverlay(
    open: Boolean,
    selected: PhoneDrawerDestination,
    onClose: () -> Unit,
    onNavigate: (PhoneDrawerDestination) -> Unit,
    hexonFabVisible: Boolean = true,
    onHexonFabVisibleChange: (Boolean) -> Unit = {},
) {
    val scrim by animateFloatAsState(
        targetValue = if (open) 0.55f else 0f,
        animationSpec = tween(280, easing = FastOutSlowInEasing),
        label = "phone-drawer-scrim",
    )
    val panelOffset by animateDpAsState(
        targetValue = if (open) 0.dp else (-320).dp,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "phone-drawer-slide",
    )
    if (!open && scrim < 0.01f && panelOffset.value < -300f) return

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = scrim))
                .then(
                    if (open) {
                        Modifier.clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = onClose,
                        )
                    } else {
                        Modifier
                    },
                ),
        )
        PhoneNavDrawerPanel(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .fillMaxWidth(0.78f)
                .offset(x = panelOffset),
            selected = selected,
            onClose = onClose,
            onNavigate = onNavigate,
            hexonFabVisible = hexonFabVisible,
            onHexonFabVisibleChange = onHexonFabVisibleChange,
        )
    }
}

@Composable
private fun PhoneNavDrawerPanel(
    modifier: Modifier,
    selected: PhoneDrawerDestination,
    onClose: () -> Unit,
    onNavigate: (PhoneDrawerDestination) -> Unit,
    hexonFabVisible: Boolean,
    onHexonFabVisibleChange: (Boolean) -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(topEnd = 22.dp, bottomEnd = 22.dp))
            .background(DrawerBg)
            .border(
                width = 1.dp,
                color = DrawerBorder,
                shape = RoundedCornerShape(topEnd = 22.dp, bottomEnd = 22.dp),
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            Image(
                painter = painterResource(Res.drawable.hexnet_mark),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp)),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp),
            ) {
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(color = Color.White, fontWeight = FontWeight.Bold)) { append("Van") }
                        withStyle(SpanStyle(color = DrawerGreen, fontWeight = FontWeight.Bold)) { append("GO") }
                    },
                    fontSize = 20.sp,
                )
                Text(
                    stringResource(Res.string.dashboard_brand_tagline),
                    color = DrawerMuted,
                    fontSize = 11.sp,
                    maxLines = 2,
                )
            }
            Icon(
                Icons.Filled.Close,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .size(26.dp)
                    .clickable(onClick = onClose),
            )
        }

        Spacer(Modifier.height(22.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            DrawerNavItem(
                icon = Icons.Filled.Home,
                label = stringResource(Res.string.dashboard_nav_home),
                selected = selected == PhoneDrawerDestination.Home,
                onClick = { onNavigate(PhoneDrawerDestination.Home) },
            )
            DrawerNavItem(
                icon = Icons.Filled.WbIncandescent,
                label = stringResource(Res.string.dashboard_nav_dim),
                selected = selected == PhoneDrawerDestination.Dim,
                onClick = { onNavigate(PhoneDrawerDestination.Dim) },
            )
            DrawerNavItem(
                icon = Icons.Filled.WaterDrop,
                label = stringResource(Res.string.dashboard_nav_water),
                selected = selected == PhoneDrawerDestination.Water,
                onClick = { onNavigate(PhoneDrawerDestination.Water) },
            )
            DrawerNavItem(
                icon = Icons.Filled.Thermostat,
                label = stringResource(Res.string.dashboard_nav_climate),
                selected = selected == PhoneDrawerDestination.Climate,
                onClick = { onNavigate(PhoneDrawerDestination.Climate) },
            )
            DrawerNavItem(
                icon = MotorEngineIcon,
                label = stringResource(Res.string.dashboard_nav_motor),
                selected = selected == PhoneDrawerDestination.Motor,
                onClick = { onNavigate(PhoneDrawerDestination.Motor) },
            )
            DrawerNavItem(
                icon = Icons.Filled.Bolt,
                label = stringResource(Res.string.dashboard_nav_energy_monitor),
                selected = selected == PhoneDrawerDestination.Energy,
                onClick = { onNavigate(PhoneDrawerDestination.Energy) },
            )

            Spacer(Modifier.height(14.dp))
            Text(
                stringResource(Res.string.dashboard_drawer_system),
                color = DrawerMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                modifier = Modifier.padding(start = 10.dp, bottom = 4.dp),
            )
            DrawerNavItem(
                icon = Icons.Filled.Settings,
                label = stringResource(Res.string.dashboard_nav_settings),
                selected = selected == PhoneDrawerDestination.Settings,
                onClick = { onNavigate(PhoneDrawerDestination.Settings) },
            )
            DrawerNavItem(
                icon = Icons.Filled.Notifications,
                label = stringResource(Res.string.settings_rd_notifications_title),
                selected = selected == PhoneDrawerDestination.Notifications,
                onClick = { onNavigate(PhoneDrawerDestination.Notifications) },
            )
            DrawerNavItem(
                icon = Icons.Filled.Info,
                label = stringResource(Res.string.settings_about_title),
                selected = selected == PhoneDrawerDestination.About,
                onClick = { onNavigate(PhoneDrawerDestination.About) },
            )

            Spacer(Modifier.height(12.dp))
            DrawerHexonToggleRow(
                checked = hexonFabVisible,
                onCheckedChange = onHexonFabVisibleChange,
            )
        }

        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, DrawerBorder, RoundedCornerShape(14.dp))
                .clickable { onNavigate(PhoneDrawerDestination.Logout) }
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ExitToApp,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp),
            )
            Text(
                stringResource(Res.string.dashboard_drawer_logout),
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.height(6.dp))
    }
}

@Composable
private fun DrawerHexonToggleRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Image(
            painter = painterResource(Res.drawable.hexon_avatar),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .border(1.dp, DrawerGreen.copy(alpha = 0.45f), CircleShape),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(Res.string.hexon_drawer_title),
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(Res.string.hexon_drawer_show_on_home),
                color = DrawerMuted,
                fontSize = 12.sp,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = DrawerGreen,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = DrawerBorder,
            ),
        )
    }
}

@Composable
private fun DrawerNavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .then(
                if (selected) {
                    Modifier
                        .background(DrawerActiveBg, shape)
                        .border(1.dp, DrawerActiveBorder, shape)
                } else {
                    Modifier
                },
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (selected) DrawerGreen else Color.White,
            modifier = Modifier.size(22.dp),
        )
        Text(
            label,
            color = if (selected) DrawerGreen else Color.White,
            fontSize = 15.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        )
    }
}

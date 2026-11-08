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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hextech.vango.profile.CaravanType
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import testvango.composeapp.generated.resources.Res
import testvango.composeapp.generated.resources.caravan_type_motor
import testvango.composeapp.generated.resources.caravan_type_tow
import testvango.composeapp.generated.resources.dashboard_caravan_page_hint
import testvango.composeapp.generated.resources.dashboard_caravan_page_title
import testvango.composeapp.generated.resources.profile_caravan_type
import testvango.composeapp.generated.resources.register_caravan_hint

private val PageBg: Color @Composable get() = LocalAppChrome.current.bg
private val PageText: Color @Composable get() = LocalAppChrome.current.text
private val PageMuted: Color @Composable get() = LocalAppChrome.current.muted
private val PageBorder: Color @Composable get() = LocalAppChrome.current.border
private val PageCard: Color @Composable get() = LocalAppChrome.current.card
private val PageAccent = Color(0xFF3DDC84)

/**
 * Phone-only: pick Motokaravan vs Çekme Karavan for the home hero visual.
 */
@Composable
fun PhoneCaravanTypeScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val revision = caravanTypeRevision
    var selected by remember(revision) { mutableStateOf(resolvedCaravanType()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PageBg)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                tint = PageText,
                modifier = Modifier
                    .size(28.dp)
                    .clickable(onClick = onBack),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(Res.string.dashboard_caravan_page_title),
                    color = PageText,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    stringResource(Res.string.profile_caravan_type),
                    color = PageMuted,
                    fontSize = 13.sp,
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                stringResource(Res.string.dashboard_caravan_page_hint),
                color = PageMuted,
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )
            Text(
                stringResource(Res.string.register_caravan_hint),
                color = PageMuted,
                fontSize = 12.sp,
                lineHeight = 17.sp,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PhoneCaravanTypeCard(
                    modifier = Modifier.weight(1f),
                    type = CaravanType.MotorCaravan,
                    selected = selected == CaravanType.MotorCaravan,
                    label = stringResource(Res.string.caravan_type_motor),
                    onClick = {
                        selected = CaravanType.MotorCaravan
                        persistCaravanType(CaravanType.MotorCaravan)
                    },
                )
                PhoneCaravanTypeCard(
                    modifier = Modifier.weight(1f),
                    type = CaravanType.TowCaravan,
                    selected = selected == CaravanType.TowCaravan,
                    label = stringResource(Res.string.caravan_type_tow),
                    onClick = {
                        selected = CaravanType.TowCaravan
                        persistCaravanType(CaravanType.TowCaravan)
                    },
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun PhoneCaravanTypeCard(
    modifier: Modifier,
    type: CaravanType,
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
) {
    val border = if (selected) PageAccent else PageBorder
    Column(
        modifier = modifier
            .heightIn(min = 200.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(2.dp, border, RoundedCornerShape(16.dp))
            .background(
                if (selected) PageAccent.copy(alpha = 0.12f) else PageCard,
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(phoneCaravanForeground(type)),
                contentDescription = label,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
            )
        }
        Text(
            label,
            color = if (selected) PageAccent else PageText,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

package com.hextech.vango

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hextech.vango.profile.LoginPreferences
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import testvango.composeapp.generated.resources.Res
import testvango.composeapp.generated.resources.dashboard_home_system_offline
import testvango.composeapp.generated.resources.hexon_avatar
import testvango.composeapp.generated.resources.hexon_chip_connection
import testvango.composeapp.generated.resources.hexon_chip_connection_q
import testvango.composeapp.generated.resources.hexon_chip_lights
import testvango.composeapp.generated.resources.hexon_chip_lights_q
import testvango.composeapp.generated.resources.hexon_chip_relays
import testvango.composeapp.generated.resources.hexon_chip_relays_q
import testvango.composeapp.generated.resources.hexon_chip_water
import testvango.composeapp.generated.resources.hexon_chip_water_q
import testvango.composeapp.generated.resources.hexon_chips_title
import testvango.composeapp.generated.resources.hexon_connect_cta
import testvango.composeapp.generated.resources.hexon_connection_status
import testvango.composeapp.generated.resources.hexon_input_hint
import testvango.composeapp.generated.resources.hexon_subtitle
import testvango.composeapp.generated.resources.hexon_title
import testvango.composeapp.generated.resources.hexon_typing
import kotlin.random.Random

/** Soft dark palette matching Hexon mockup */
private val HxBg = Color(0xFF0C1219)
private val HxSurface = Color(0xFF151C26)
private val HxSurfaceSoft = Color(0xFF1A222E)
private val HxBorder = Color(0xFF2A3544).copy(alpha = 0.7f)
private val HxTeal = Color(0xFF3DDCB0)
private val HxTealDeep = Color(0xFF249E82)
private val HxMuted = Color(0xFF8E99A8)
private val HxBot = Color(0xFF1B2430)
private val HxUser = Color(0xFF1F6B58)
private val HxDanger = Color(0xFFD45B5B)
private val HxDangerSoft = Color(0xFF3A1F24)

private data class HexonChatMessage(
    val id: Long,
    val fromUser: Boolean,
    val text: String,
    val timeLabel: String,
    val navigateTo: HexonDestination? = null,
    val ctaLabel: String? = null,
    val card: HexonResultCard? = null,
    val followUps: List<HexonFollowUp> = emptyList(),
)

@Composable
fun HexonAssistantScreen(
    onBack: () -> Unit,
    onNavigate: (HexonDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    var linked by remember {
        mutableStateOf(isBleGattLinkReady() || isAppRemoteMqttConnected())
    }
    LaunchedEffect(Unit) {
        while (true) {
            linked = isBleGattLinkReady() || isAppRemoteMqttConnected()
            delay(800)
        }
    }

    val userFirst = remember {
        LoginPreferences.getCachedFullName()
            ?.trim()
            ?.split(Regex("\\s+"))
            ?.firstOrNull()
            ?.takeIf { it.isNotBlank() }
    }

    val messages = remember { mutableStateListOf<HexonChatMessage>() }
    var draft by remember { mutableStateOf("") }
    var isTyping by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var idSeq by remember { mutableStateOf(1L) }

    fun nextId(): Long {
        idSeq += 1
        return idSeq
    }

    fun nowLabel(): String {
        val t = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        return "${t.hour.toString().padStart(2, '0')}:${t.minute.toString().padStart(2, '0')}"
    }

    fun pushBot(reply: HexonReply) {
        messages.add(
            HexonChatMessage(
                id = nextId(),
                fromUser = false,
                text = reply.text,
                timeLabel = nowLabel(),
                navigateTo = reply.navigateTo,
                ctaLabel = reply.ctaLabel,
                card = reply.card,
                followUps = reply.followUps,
            ),
        )
    }

    fun liveContext(): HexonLiveContext {
        val ble = isBleGattLinkReady()
        val remote = isAppRemoteMqttConnected()
        return HexonLiveContext(
            linked = ble || remote,
            bleReady = ble,
            remoteReady = remote,
            data = vanGoData.value,
            env = bleEnvSensors.value,
            channels = panelPlatformChannels.value,
            profile = devicePanelProfile.value,
            alertCount = countActiveInboxAlerts(vanGoData.value),
            userFirstName = userFirst,
        )
    }

    fun submit(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || isTyping) return
        messages.add(
            HexonChatMessage(
                id = nextId(),
                fromUser = true,
                text = trimmed,
                timeLabel = nowLabel(),
            ),
        )
        draft = ""
        scope.launch {
            isTyping = true
            delay(650L + Random.nextLong(0, 550))
            isTyping = false
            pushBot(hexonRespond(trimmed, liveContext()))
        }
    }

    LaunchedEffect(Unit) {
        HexonIssueGuide.clear()
        if (messages.isEmpty()) {
            isTyping = true
            delay(800)
            isTyping = false
            val hour = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).hour
            pushBot(HexonReply(hexonGreeting(userFirst, hour)))
        }
    }

    LaunchedEffect(messages.size, isTyping) {
        val target = when {
            isTyping -> messages.size
            messages.isNotEmpty() -> messages.lastIndex
            else -> return@LaunchedEffect
        }
        listState.animateScrollToItem(target)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .navigationBarsPadding()
            .background(
                Brush.verticalGradient(
                    0f to Color(0xFF0E1620),
                    0.45f to HxBg,
                    1f to Color(0xFF0A1016),
                ),
            ),
    ) {
        HexonTopBar(onBack = onBack)
        if (!linked) {
            SoftOfflineBanner(onClick = { onNavigate(HexonDestination.SettingsConnection) })
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(messages, key = { it.id }) { msg ->
                HexonBubble(
                    message = msg,
                    onCta = { dest -> onNavigate(dest) },
                    onFollowUp = { query -> submit(query) },
                )
            }
            if (isTyping) {
                item(key = "typing") {
                    HexonTypingIndicator()
                }
            }

            if (messages.size <= 2 && !isTyping) {
                item(key = "chips") {
                    val chips = listOf(
                        HexonQuickChip(
                            stringResource(Res.string.hexon_chip_connection),
                            stringResource(Res.string.hexon_chip_connection_q),
                        ),
                        HexonQuickChip(
                            stringResource(Res.string.hexon_chip_water),
                            stringResource(Res.string.hexon_chip_water_q),
                        ),
                        HexonQuickChip(
                            stringResource(Res.string.hexon_chip_relays),
                            stringResource(Res.string.hexon_chip_relays_q),
                        ),
                        HexonQuickChip(
                            stringResource(Res.string.hexon_chip_lights),
                            stringResource(Res.string.hexon_chip_lights_q),
                        ),
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(Res.string.hexon_chips_title),
                            color = HxMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            chips.forEach { chip ->
                                HexonTagChip(
                                    label = chip.label,
                                    onClick = { submit(chip.seedQuery) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
                if (!linked) {
                    item(key = "status-card") {
                        SoftConnectionCard(
                            onConnect = { onNavigate(HexonDestination.SettingsConnection) },
                        )
                    }
                }
            }
        }

        SoftInputBar(
            value = draft,
            onValueChange = { draft = it },
            onSend = { submit(draft) },
        )
    }
}

@Composable
private fun HexonTopBar(onBack: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
    ) {
        IconButton(onClick = onBack) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.92f),
            )
        }
        Image(
            painter = painterResource(Res.drawable.hexon_avatar),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(42.dp)
                .shadow(6.dp, CircleShape, clip = false)
                .clip(CircleShape)
                .border(1.5.dp, HxTeal.copy(alpha = 0.55f), CircleShape),
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(Res.string.hexon_title),
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(Res.string.hexon_subtitle),
                color = HxMuted,
                fontSize = 12.sp,
            )
        }
        Icon(
            Icons.Filled.Info,
            contentDescription = null,
            tint = HxMuted,
            modifier = Modifier
                .padding(end = 6.dp)
                .size(20.dp),
        )
    }
}

@Composable
private fun SoftOfflineBanner(onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(HxDangerSoft.copy(alpha = 0.85f))
            .border(1.dp, HxDanger.copy(alpha = 0.28f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Icon(Icons.Filled.WifiOff, contentDescription = null, tint = HxDanger, modifier = Modifier.size(18.dp))
        Text(
            text = stringResource(Res.string.dashboard_home_system_offline),
            color = Color(0xFFFFB4B4),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = HxDanger.copy(alpha = 0.8f),
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun HexonBubble(
    message: HexonChatMessage,
    onCta: (HexonDestination) -> Unit,
    onFollowUp: (String) -> Unit,
) {
    if (message.fromUser) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.widthIn(max = 300.dp),
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            color = HxUser,
                            shape = RoundedCornerShape(
                                topStart = 18.dp,
                                topEnd = 18.dp,
                                bottomStart = 18.dp,
                                bottomEnd = 6.dp,
                            ),
                        )
                        .padding(horizontal = 14.dp, vertical = 11.dp),
                ) {
                    Text(
                        message.text,
                        color = Color.White.copy(alpha = 0.95f),
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                    )
                }
                Text(
                    text = message.timeLabel,
                    color = HxMuted.copy(alpha = 0.75f),
                    fontSize = 10.sp,
                    modifier = Modifier.padding(top = 5.dp, end = 6.dp),
                )
            }
        }
        return
    }

    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Image(
            painter = painterResource(Res.drawable.hexon_avatar),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(28.dp)
                .clip(CircleShape)
                .border(1.dp, HxTeal.copy(alpha = 0.4f), CircleShape),
        )
        Column(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier
                .weight(1f, fill = false)
                .widthIn(max = 320.dp)
                .fillMaxWidth(),
        ) {
            Box(
                modifier = Modifier
                    .shadow(2.dp, RoundedCornerShape(18.dp), clip = false)
                    .background(
                        color = HxBot,
                        shape = RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 18.dp,
                            bottomStart = 6.dp,
                            bottomEnd = 18.dp,
                        ),
                    )
                    .border(1.dp, HxBorder.copy(alpha = 0.65f), RoundedCornerShape(18.dp))
                    .padding(horizontal = 14.dp, vertical = 11.dp),
            ) {
                Text(
                    message.text,
                    color = Color.White.copy(alpha = 0.95f),
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                )
            }
            Text(
                text = message.timeLabel,
                color = HxMuted.copy(alpha = 0.75f),
                fontSize = 10.sp,
                modifier = Modifier.padding(top = 5.dp, start = 6.dp),
            )

            val card = message.card
            if (card != null) {
                Spacer(Modifier.height(10.dp))
                HexonResultCardView(
                    card = card,
                    onCta = onCta,
                )
            } else {
                val dest = message.navigateTo
                val cta = message.ctaLabel
                if (dest != null && cta != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = cta,
                        color = HxTeal,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(HxTeal.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                            .border(1.dp, HxTeal.copy(alpha = 0.28f), RoundedCornerShape(20.dp))
                            .clickable { onCta(dest) }
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                    )
                }
            }

            if (message.followUps.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    message.followUps.forEach { follow ->
                        HexonTagChip(
                            label = follow.label,
                            onClick = { onFollowUp(follow.seedQuery) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HexonResultCardView(
    card: HexonResultCard,
    onCta: (HexonDestination) -> Unit,
) {
    when (card) {
        is HexonResultCard.Metrics -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(HxSurface)
                    .border(1.dp, HxBorder, RoundedCornerShape(18.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    HexonMetricIcon(card.headerIconKey, size = 18.dp)
                    Text(
                        text = card.title,
                        color = Color(0xFF8EC8FF),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        Icons.Filled.Info,
                        contentDescription = null,
                        tint = HxMuted.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp),
                    )
                }
                Spacer(Modifier.height(8.dp))
                card.rows.forEachIndexed { index, row ->
                    if (index > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(HxBorder.copy(alpha = 0.55f)),
                        )
                    }
                    val rowDest = card.rowsNavigateTo
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (rowDest != null) {
                                    Modifier.clickable { onCta(rowDest) }
                                } else {
                                    Modifier
                                },
                            )
                            .padding(vertical = 11.dp),
                    ) {
                        HexonMetricIcon(row.iconKey ?: card.headerIconKey, size = 22.dp)
                        Text(
                            text = row.label,
                            color = Color.White.copy(alpha = 0.92f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f),
                        )
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = row.value,
                                color = if (row.value.equals("Veri yok", ignoreCase = true)) {
                                    HxMuted
                                } else {
                                    Color.White.copy(alpha = 0.9f)
                                },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                            if (!row.detail.isNullOrBlank()) {
                                Text(
                                    text = row.detail,
                                    color = HxMuted.copy(alpha = 0.85f),
                                    fontSize = 11.sp,
                                )
                            }
                        }
                        if (rowDest != null) {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = HxMuted.copy(alpha = 0.8f),
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
                val dest = card.navigateTo
                val cta = card.ctaLabel
                if (dest != null && cta != null) {
                    Spacer(Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, HxTeal.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
                            .clickable { onCta(dest) }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                    ) {
                        Icon(
                            Icons.Filled.Link,
                            contentDescription = null,
                            tint = HxTeal,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = cta,
                            color = HxTeal,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                val footer = card.footer
                if (!footer.isNullOrBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            Icons.Filled.Info,
                            contentDescription = null,
                            tint = HxMuted.copy(alpha = 0.75f),
                            modifier = Modifier
                                .padding(top = 1.dp)
                                .size(13.dp),
                        )
                        Text(
                            text = footer,
                            color = HxMuted,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HexonMetricIcon(
    iconKey: String?,
    size: Dp,
) {
    val def = iconKey?.let { PanelIconCatalog.find(it) }
    if (def != null) {
        PanelCatalogIcon(def = def, size = size, tint = def.tint)
    } else {
        Icon(
            Icons.Filled.WaterDrop,
            contentDescription = null,
            tint = Color(0xFF4EA8FF),
            modifier = Modifier.size(size),
        )
    }
}

@Composable
private fun HexonTypingIndicator() {
    val transition = rememberInfiniteTransition(label = "hexon-typing")
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Image(
            painter = painterResource(Res.drawable.hexon_avatar),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .border(1.dp, HxTeal.copy(alpha = 0.45f), CircleShape),
        )
        Column(horizontalAlignment = Alignment.Start) {
            Text(
                text = stringResource(Res.string.hexon_typing),
                color = HxMuted.copy(alpha = 0.9f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .shadow(2.dp, RoundedCornerShape(16.dp), clip = false)
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = 6.dp,
                            bottomEnd = 16.dp,
                        ),
                    )
                    .background(HxBot)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                repeat(3) { index ->
                    val phase by transition.animateFloat(
                        initialValue = 0.28f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(
                                durationMillis = 380,
                                delayMillis = index * 160,
                                easing = FastOutSlowInEasing,
                            ),
                            repeatMode = RepeatMode.Reverse,
                        ),
                        label = "dot-$index",
                    )
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .graphicsLayer {
                                alpha = phase
                                translationY = -(1f - phase) * 4f
                                scaleX = 0.85f + phase * 0.15f
                                scaleY = 0.85f + phase * 0.15f
                            }
                            .background(Color(0xFF5B9FD4).copy(alpha = 0.95f), CircleShape),
                    )
                }
            }
        }
    }
}

@Composable
private fun HexonTagChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        text = label,
        color = Color.White.copy(alpha = 0.92f),
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, HxTeal.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
            .background(Color.Transparent, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp),
    )
}

@Composable
private fun SoftConnectionCard(onConnect: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(HxSurface)
            .border(1.dp, HxBorder, RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(Res.string.hexon_connection_status),
            color = HxMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.6.sp,
        )
        SoftStatusRow(label = "BLE", ok = isBleGattLinkReady())
        SoftStatusRow(label = "Uzaktan", ok = isAppRemoteMqttConnected())
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.horizontalGradient(listOf(HxTealDeep, HxTeal.copy(alpha = 0.85f))),
                    RoundedCornerShape(14.dp),
                )
                .clickable(onClick = onConnect)
                .padding(vertical = 13.dp),
        ) {
            Icon(Icons.Filled.Link, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(Res.string.hexon_connect_cta),
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
            )
        }
    }
}

@Composable
private fun SoftStatusRow(label: String, ok: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .background(if (ok) HxTeal else HxDanger.copy(alpha = 0.9f), CircleShape),
        )
        Text(label, color = Color.White.copy(alpha = 0.92f), fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(
            text = if (ok) "Bağlı" else "Bağlı değil",
            color = if (ok) HxTeal else HxDanger.copy(alpha = 0.9f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun SoftInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(HxSurface.copy(alpha = 0.95f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(stringResource(Res.string.hexon_input_hint), color = HxMuted, fontSize = 14.sp)
            },
            singleLine = true,
            colors = TextFieldDefaults.textFieldColors(
                textColor = Color.White,
                backgroundColor = HxBg,
                cursorColor = HxTeal,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 46.dp)
                .border(1.dp, HxBorder, RoundedCornerShape(24.dp)),
        )
        Spacer(Modifier.width(10.dp))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(46.dp)
                .shadow(if (value.isBlank()) 0.dp else 6.dp, CircleShape, clip = false)
                .clip(CircleShape)
                .background(if (value.isBlank()) HxSurfaceSoft else HxTealDeep)
                .clickable(enabled = value.isNotBlank(), onClick = onSend),
        ) {
            Icon(
                Icons.AutoMirrored.Filled.Send,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/** Home FAB: avatar only (no “Hexon’a Sor” label), soft speech-bubble cue. */
@Composable
fun HexonAskFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(64.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(Res.drawable.hexon_avatar),
            contentDescription = stringResource(Res.string.hexon_title),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(56.dp)
                .shadow(10.dp, CircleShape, clip = false)
                .clip(CircleShape)
                .border(2.dp, HxTeal.copy(alpha = 0.7f), CircleShape),
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 2.dp, y = (-2).dp)
                .size(22.dp)
                .shadow(4.dp, CircleShape, clip = false)
                .clip(CircleShape)
                .background(HxTeal)
                .border(2.dp, LocalAppChrome.current.bg, CircleShape),
        ) {
            Icon(
                Icons.Filled.ChatBubble,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(11.dp),
            )
        }
    }
}

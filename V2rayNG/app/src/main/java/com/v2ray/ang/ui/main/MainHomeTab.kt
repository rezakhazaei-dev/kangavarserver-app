package com.v2ray.ang.ui.main

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.ConfigurationCompat
import com.v2ray.ang.AppConfig
import com.v2ray.ang.R
import com.v2ray.ang.dto.GroupMapItem
import kotlinx.coroutines.delay
import java.util.Locale

private const val TIMER_TICK_MS = 1000L

/** Number formatting with the digits of the app language (Persian digits in Persian). */
internal class BzNumbers(val locale: Locale) {
    fun int(value: Long): String = String.format(locale, "%d", value)
    fun dec(value: Double, digits: Int = 1): String = formatDecimal(value, locale, digits)
}

@Composable
internal fun rememberBzNumbers(): BzNumbers {
    val configuration = LocalConfiguration.current
    val locale = ConfigurationCompat.getLocales(configuration)[0] ?: Locale.getDefault()
    return remember(locale) { BzNumbers(locale) }
}

internal val TopInsets: WindowInsets
    @Composable get() = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)

/** Home tab: welcome, connection, error or expired screen, chosen by [homeScreenMode]. */
@Composable
internal fun MainHomeTab(
    uiState: MainUiState,
    catalog: ServerCatalogUiModel,
    liveTraffic: LiveTrafficUiModel,
    layout: MainLayoutInfo,
    onAction: (MainAction) -> Unit,
    onSelectTab: (MainTab) -> Unit,
    onOpenAddSheet: () -> Unit,
) {
    val subscription = remember(uiState.groups, uiState.selectedServer) {
        subscriptionForHome(uiState.groups, uiState.selectedServer?.subscriptionId)
    }
    val now = remember(subscription) { System.currentTimeMillis() }
    val summary = remember(subscription, now) { subscription?.usageInfo?.let { buildSubscriptionSummary(it, now) } }
    val mode = homeScreenMode(
        catalogLoaded = catalog.loaded,
        hasServers = catalog.rows.isNotEmpty(),
        isRunning = uiState.isRunning,
        isConnecting = uiState.isConnecting,
        hasConnectionError = uiState.connectionError != null,
        subscriptionBlocked = summary?.isBlocked() == true,
    )
    val openBot = { onAction(MainAction.OpenSubscriptionLink(AppConfig.BRAND_TELEGRAM_BOT_URL)) }
    when (mode) {
        HomeScreenMode.Welcome -> WelcomeScreen(layout, onAction, onOpenAddSheet, openBot)
        HomeScreenMode.Error -> ErrorScreen(uiState.connectionError, layout, onAction, onOpenAddSheet, openBot)
        HomeScreenMode.Expired -> ExpiredScreen(subscription, summary, layout, onAction, onOpenAddSheet, openBot)
        HomeScreenMode.Main -> HomeMain(uiState, catalog, liveTraffic, summary, layout, onAction, onSelectTab, onOpenAddSheet, openBot)
    }
}

/** Top bar of the home screens: wordmark, add-subscription and support buttons. */
@Composable
private fun HomeTopBar(layout: MainLayoutInfo, onOpenAddSheet: () -> Unit, openBot: () -> Unit) {
    BzTopBar(
        height = if (layout.shortHeight) 44.dp else 52.dp,
        start = { BzWordmark(small = layout.small) },
        actions = {
            BzIconButton(R.drawable.ic_ms_add_link, stringResource(R.string.bz_add_subscription), onOpenAddSheet, small = layout.small)
            BzIconButton(R.drawable.ic_ms_support_agent, stringResource(R.string.bz_support), openBot, small = layout.small)
        },
    )
}

/**
 * Scrollable screen column whose content is at least one screen tall, so [Arrangement.SpaceBetween]
 * can push the bottom block down while short screens still scroll.
 */
@Composable
private fun FullHeightScroll(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        val viewport = maxHeight
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = viewport),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally,
                content = content,
            )
        }
    }
}

@Composable
private fun HomeMain(
    uiState: MainUiState,
    catalog: ServerCatalogUiModel,
    liveTraffic: LiveTrafficUiModel,
    summary: SubscriptionSummaryUiModel?,
    layout: MainLayoutInfo,
    onAction: (MainAction) -> Unit,
    onSelectTab: (MainTab) -> Unit,
    onOpenAddSheet: () -> Unit,
    openBot: () -> Unit,
) {
    val phase = homeConnectionPhase(uiState.isRunning, uiState.isConnecting)
    val backdrop = if (phase == HomeConnectionPhase.Connected) BzBackdrop.Connected else BzBackdrop.Default
    Box(
        Modifier
            .fillMaxSize()
            .bzBackdrop(backdrop)
            .windowInsetsPadding(TopInsets)
            .padding(horizontal = if (layout.useRail) 28.dp else if (layout.small) 14.dp else 18.dp),
    ) {
        if (layout.twoColumns) {
            Column(Modifier.fillMaxSize()) {
                HomeTopBar(layout, onOpenAddSheet, openBot)
                Row(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(if (layout.shortHeight) 20.dp else 32.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1.1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        HeroBlock(phase, uiState, catalog, liveTraffic, layout, onAction)
                    }
                    Column(Modifier.weight(1f)) {
                        CardsBlock(phase, uiState, catalog, liveTraffic, summary, layout, onSelectTab, wide = true)
                    }
                }
            }
        } else {
            FullHeightScroll {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    HomeTopBar(layout, onOpenAddSheet, openBot)
                    HeroBlock(phase, uiState, catalog, liveTraffic, layout, onAction)
                }
                Column(Modifier.padding(top = 8.dp)) {
                    CardsBlock(phase, uiState, catalog, liveTraffic, summary, layout, onSelectTab, wide = false)
                }
            }
        }
    }
}

/** State pill, power button, big state text and the phase-specific block under it. */
@Composable
private fun HeroBlock(
    phase: HomeConnectionPhase,
    uiState: MainUiState,
    catalog: ServerCatalogUiModel,
    liveTraffic: LiveTrafficUiModel,
    layout: MainLayoutInfo,
    onAction: (MainAction) -> Unit,
) {
    val numbers = rememberBzNumbers()
    if (!layout.shortHeight) {
        Spacer(Modifier.height(if (layout.small) 2.dp else 6.dp))
        when (phase) {
            HomeConnectionPhase.Connected -> BzStatePill(
                stringResource(R.string.home_status_connected), Bz.Accent, Bz.Accent, Bz.Accent.copy(alpha = 0.35f),
            )

            HomeConnectionPhase.Connecting -> BzStatePill(
                stringResource(R.string.bz_status_handshake), Bz.Warm, Bz.Muted, Bz.Line, blinking = true,
            )

            HomeConnectionPhase.Disconnected -> BzStatePill(
                stringResource(R.string.home_status_disconnected), Bz.Faint, Bz.Muted, Bz.Line,
            )
        }
    }
    val ringSize = when {
        layout.shortHeight -> 150.dp
        layout.small -> 160.dp
        else -> 212.dp
    }
    PowerControl(
        phase = phase,
        ringSize = ringSize,
        modifier = Modifier.padding(top = if (layout.small || layout.shortHeight) 6.dp else 22.dp),
        onClick = { onAction(MainAction.ToggleService) },
    )
    Text(
        text = stringResource(
            when (phase) {
                HomeConnectionPhase.Connected -> R.string.home_title_connected
                HomeConnectionPhase.Connecting -> R.string.bz_title_connecting
                HomeConnectionPhase.Disconnected -> R.string.home_title_disconnected
            }
        ),
        style = bzText(
            if (layout.shortHeight) 22.sp else if (layout.small) 25.sp else 32.sp,
            FontWeight.Black,
            if (phase == HomeConnectionPhase.Connected) Bz.Accent else Bz.Text,
        ),
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = if (layout.small || layout.shortHeight) 6.dp else 14.dp),
    )
    Box(Modifier.heightIn(min = if (layout.shortHeight) 0.dp else 24.dp), contentAlignment = Alignment.Center) {
        when (phase) {
            HomeConnectionPhase.Connected -> uiState.connectedSinceMillis?.let { ConnectionTimer(it, numbers) }
            HomeConnectionPhase.Connecting -> SubState(stringResource(R.string.bz_hint_wait), layout)
            HomeConnectionPhase.Disconnected -> SubState(stringResource(R.string.home_hint_disconnected), layout)
        }
    }
    when (phase) {
        HomeConnectionPhase.Connecting -> ConnectingSteps(uiState.smartSelect)
        HomeConnectionPhase.Disconnected -> if (!layout.shortHeight) QuickConnect(catalog, layout, numbers, onAction)
        HomeConnectionPhase.Connected -> StatsRow(liveTraffic, uiState, numbers, layout)
    }
}

@Composable
private fun SubState(text: String, layout: MainLayoutInfo) {
    Text(text, style = bzText(if (layout.small) 13.sp else 14.sp, color = Bz.Muted), textAlign = TextAlign.Center)
}

@Composable
private fun ConnectionTimer(sinceMillis: Long, numbers: BzNumbers) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(sinceMillis) {
        while (true) {
            now = System.currentTimeMillis()
            delay(TIMER_TICK_MS)
        }
    }
    Text(
        text = formatConnectionDuration(now - sinceMillis, numbers.locale),
        style = bzText(15.sp, FontWeight.Bold).copy(letterSpacing = 0.6.sp),
        textAlign = TextAlign.Center,
    )
}

/**
 * The 212dp power control: dashed ring when off, spinning arc while connecting, solid ring with a
 * glowing mint button and two pulses when connected. Animations only change the draw phase.
 */
@Composable
private fun PowerControl(
    phase: HomeConnectionPhase,
    ringSize: Dp,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val scale = ringSize / 212.dp
    val buttonSize = ringSize * (150f / 212f)
    val transition = rememberInfiniteTransition(label = "power")
    val spin = if (phase == HomeConnectionPhase.Connecting) {
        transition.animateFloat(0f, 360f, infiniteRepeatable(tween(1100, easing = LinearEasing)), label = "powerSpin")
    } else {
        null
    }
    val pulse = if (phase == HomeConnectionPhase.Connected) {
        transition.animateFloat(0f, 1f, infiniteRepeatable(tween(2600, easing = LinearEasing)), label = "powerPulse")
    } else {
        null
    }
    val label = stringResource(if (phase == HomeConnectionPhase.Disconnected) R.string.bz_connect else R.string.bz_disconnect)
    val state = stringResource(
        when (phase) {
            HomeConnectionPhase.Connected -> R.string.home_title_connected
            HomeConnectionPhase.Connecting -> R.string.bz_title_connecting
            HomeConnectionPhase.Disconnected -> R.string.home_status_disconnected
        }
    )
    Box(
        modifier
            .size(ringSize)
            .drawBehind {
                val r = 96.dp.toPx() * scale
                when (phase) {
                    HomeConnectionPhase.Disconnected -> drawCircle(
                        Color.White.copy(alpha = 0.09f),
                        radius = r,
                        style = Stroke(2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 9.dp.toPx()))),
                    )

                    HomeConnectionPhase.Connecting -> {
                        drawCircle(Color.White.copy(alpha = 0.07f), radius = r, style = Stroke(5.dp.toPx()))
                        rotate(spin?.value ?: 0f) {
                            drawArc(
                                brush = Brush.linearGradient(listOf(Bz.Accent2, Bz.Accent)),
                                startAngle = -90f,
                                sweepAngle = 360f * 0.28f,
                                useCenter = false,
                                topLeft = Offset(center.x - r, center.y - r),
                                size = androidx.compose.ui.geometry.Size(r * 2, r * 2),
                                style = Stroke(5.dp.toPx(), cap = StrokeCap.Round),
                            )
                        }
                    }

                    HomeConnectionPhase.Connected -> {
                        val buttonRadius = buttonSize.toPx() / 2f
                        drawCircle(
                            Brush.radialGradient(
                                0f to Bz.Accent.copy(alpha = 0.38f),
                                1f to Color.Transparent,
                                center = center,
                                radius = buttonRadius + 60.dp.toPx() * scale,
                            ),
                            radius = buttonRadius + 60.dp.toPx() * scale,
                        )
                        drawCircle(Bz.Accent.copy(alpha = 0.10f), radius = buttonRadius + 10.dp.toPx() * scale)
                        drawCircle(Bz.Accent.copy(alpha = 0.55f), radius = r, style = Stroke(2.dp.toPx()))
                        drawCircle(
                            Bz.Accent.copy(alpha = 0.15f),
                            radius = r + 8.dp.toPx() * scale,
                            style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 7.dp.toPx()))),
                        )
                        val base = (ringSize.toPx() / 2f) - 30.dp.toPx() * scale
                        val p = pulse?.value ?: 0f
                        listOf(p, (p + 0.5f) % 1f).forEach { t ->
                            drawCircle(
                                Bz.Accent.copy(alpha = 0.35f * (0.7f * (1f - t)) / 0.7f * 0.7f),
                                radius = base * (0.92f + 0.30f * t),
                                style = Stroke(1.dp.toPx()),
                            )
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        val on = phase == HomeConnectionPhase.Connected
        Box(
            Modifier
                .size(buttonSize)
                .clip(CircleShape)
                .background(
                    if (on) {
                        Brush.radialGradient(listOf(Bz.Accent2, Bz.Accent), center = Offset.Unspecified)
                    } else {
                        Brush.radialGradient(listOf(Color(0xFF1E3A35), Color(0xFF10221F)))
                    }
                )
                .border(1.dp, if (on) Color.Transparent else Bz.Line2, CircleShape)
                .semantics {
                    contentDescription = label
                    stateDescription = state
                }
                .clickable(role = Role.Button, onClickLabel = label, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(R.drawable.ic_ms_power_settings_new),
                contentDescription = null,
                tint = if (on) Bz.On else Bz.Accent,
                modifier = Modifier.size(buttonSize * (58f / 150f)),
            )
        }
    }
}

@Composable
private fun ConnectingSteps(smart: Boolean) {
    Column(
        Modifier
            .padding(top = 14.dp)
            .widthIn(min = 230.dp)
            .glass()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        StepRow(stringResource(if (smart) R.string.bz_step_find else R.string.bz_step_prepare), StepState.Done)
        StepRow(stringResource(R.string.bz_step_handshake), StepState.Running)
        StepRow(stringResource(R.string.bz_step_tunnel), StepState.Pending)
    }
}

private enum class StepState { Done, Running, Pending }

@Composable
private fun StepRow(text: String, state: StepState) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        when (state) {
            StepState.Done -> Icon(painterResource(R.drawable.ic_ms_check_circle_fill), null, tint = Bz.Accent, modifier = Modifier.size(18.dp))
            StepState.Running -> CircularProgressIndicator(Modifier.size(16.dp), color = Bz.Warm, strokeWidth = 2.dp)
            StepState.Pending -> Icon(painterResource(R.drawable.ic_ms_radio_button_unchecked), null, tint = Bz.Faint, modifier = Modifier.size(18.dp))
        }
        Text(
            text,
            style = bzText(
                13.sp,
                if (state == StepState.Running) FontWeight.Bold else FontWeight.Normal,
                when (state) {
                    StepState.Done -> Bz.Muted
                    StepState.Running -> Bz.Text
                    StepState.Pending -> Bz.Faint
                },
            ),
        )
    }
}

/** "Or connect straight to a country" chips, one per known country, fastest first. */
@Composable
private fun QuickConnect(catalog: ServerCatalogUiModel, layout: MainLayoutInfo, numbers: BzNumbers, onAction: (MainAction) -> Unit) {
    val countries = catalog.countries.filter { it.country != ServerCountry.Other }.take(3)
    if (countries.isEmpty()) return
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = if (layout.small) 10.dp else 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (!layout.compactHeight) {
            Text(stringResource(R.string.bz_quick_connect), style = bzText(12.sp, color = Bz.Faint))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            countries.forEach { group ->
                val name = stringResource(group.country.labelRes)
                val description = stringResource(R.string.bz_quick_connect_to, name)
                Row(
                    Modifier
                        .weight(1f)
                        .height(if (layout.small) 38.dp else 42.dp)
                        .glass(CircleShape)
                        .clickable(role = Role.Button, onClickLabel = description) { onAction(MainAction.QuickConnect(group.country)) }
                        .semantics { contentDescription = description }
                        .padding(horizontal = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(if (layout.small) 4.dp else 7.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BzFlag(group.country, if (layout.small) 18.dp else 22.dp, ring = false)
                    Text(name, style = bzText(if (layout.small) 12.sp else 13.sp, FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (group.bestDelayMillis.isMeasuredDelay()) {
                        Text(
                            numbers.int(group.bestDelayMillis),
                            style = bzText(11.sp, FontWeight.Bold, pingColor(pingQuality(group.bestDelayMillis))),
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsRow(liveTraffic: LiveTrafficUiModel, uiState: MainUiState, numbers: BzNumbers, layout: MainLayoutInfo) {
    val ping = uiState.currentServerTest?.delayMillis ?: 0L
    Row(
        Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(top = if (layout.shortHeight) 8.dp else 16.dp),
    ) {
        StatCell(numbers.dec(megabitsPerSecond(liveTraffic.downloadBytesPerSecond)), "Mb/s", stringResource(R.string.bz_download), layout)
        StatDivider()
        StatCell(numbers.dec(megabitsPerSecond(liveTraffic.uploadBytesPerSecond)), "Mb/s", stringResource(R.string.bz_upload), layout)
        StatDivider()
        StatCell(if (ping.isMeasuredDelay()) numbers.int(ping) else "—", "ms", stringResource(R.string.home_ping), layout)
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.StatCell(value: String, unit: String, label: String, layout: MainLayoutInfo) {
    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            buildAnnotatedString {
                append(value)
                withStyle(SpanStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Bz.Muted)) { append(" $unit") }
            },
            style = bzText(if (layout.small || layout.shortHeight) 18.sp else 21.sp, FontWeight.Black),
            maxLines = 1,
        )
        Text(label, style = bzText(12.sp, color = Bz.Muted))
    }
}

@Composable
private fun StatDivider() {
    Box(
        Modifier
            .padding(vertical = 8.dp)
            .width(1.dp)
            .fillMaxHeight()
            .background(Bz.Line)
    )
}

/** Fixed-IP chip, server card, quota and (wide screens) the download sparkline. */
@Composable
private fun CardsBlock(
    phase: HomeConnectionPhase,
    uiState: MainUiState,
    catalog: ServerCatalogUiModel,
    liveTraffic: LiveTrafficUiModel,
    summary: SubscriptionSummaryUiModel?,
    layout: MainLayoutInfo,
    onSelectTab: (MainTab) -> Unit,
    wide: Boolean,
) {
    val numbers = rememberBzNumbers()
    val ip = uiState.currentServerTest?.ipAddress?.takeIf { phase == HomeConnectionPhase.Connected && it.isNotBlank() }
    if (ip != null) {
        Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.Center) {
            Row(
                Modifier
                    .height(28.dp)
                    .glass(CircleShape, fill = Bz.AccentDim, border = Bz.Accent.copy(alpha = 0.3f))
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(painterResource(R.drawable.ic_ms_verified), null, tint = Bz.Accent, modifier = Modifier.size(15.dp))
                Text(stringResource(R.string.bz_fixed_ip, maskIpAddress(ip)), style = bzText(12.sp, FontWeight.SemiBold, Bz.Accent))
            }
        }
    }
    ServerCard(uiState, catalog, numbers, layout) { onSelectTab(MainTab.Servers) }
    if (phase != HomeConnectionPhase.Connecting && summary != null) {
        QuotaCard(summary, numbers, layout)
    }
    if (wide && phase == HomeConnectionPhase.Connected && layout.wideScreen) {
        Sparkline(liveTraffic, numbers)
    }
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun ServerCard(
    uiState: MainUiState,
    catalog: ServerCatalogUiModel,
    numbers: BzNumbers,
    layout: MainLayoutInfo,
    onClick: () -> Unit,
) {
    val selectedRow = catalog.row(uiState.selectedGuid)
    val connected = uiState.isRunning
    val title: String
    val subtitle: String
    if (uiState.smartSelect) {
        title = stringResource(R.string.bz_smart_select)
        subtitle = if (connected && selectedRow != null) {
            val ping = uiState.currentServerTest?.delayMillis?.takeIf { it.isMeasuredDelay() } ?: selectedRow.testDelayMillis
            if (ping.isMeasuredDelay()) {
                stringResource(R.string.bz_smart_connected_ping, selectedRow.displayName, numbers.int(ping))
            } else {
                stringResource(R.string.bz_smart_connected, selectedRow.displayName)
            }
        } else {
            stringResource(R.string.bz_smart_hint)
        }
    } else {
        title = selectedRow?.displayName ?: stringResource(R.string.home_no_server)
        subtitle = selectedRow?.let { row ->
            if (row.testDelayMillis.isMeasuredDelay()) {
                stringResource(R.string.bz_protocol_ping, row.typeDescription, numbers.int(row.testDelayMillis))
            } else {
                row.typeDescription
            }
        } ?: stringResource(R.string.home_no_server_hint)
    }
    Row(
        Modifier
            .fillMaxWidth()
            .glass()
            .clickable(role = Role.Button, onClickLabel = stringResource(R.string.bz_choose_server), onClick = onClick)
            .padding(horizontal = if (layout.small) 12.dp else 14.dp, vertical = if (layout.small || layout.shortHeight) 9.dp else 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (uiState.smartSelect) BzSmartIcon() else BzFlag(selectedRow?.country ?: ServerCountry.Other, 38.dp)
        Column(Modifier.weight(1f)) {
            Text(title, style = bzText(15.sp, FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = bzText(if (layout.small) 11.sp else 12.sp, color = Bz.Muted), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(painterResource(R.drawable.ic_ms_chevron_right), null, tint = Bz.Muted)
    }
}

@Composable
private fun QuotaCard(summary: SubscriptionSummaryUiModel, numbers: BzNumbers, layout: MainLayoutInfo) {
    Column(
        Modifier
            .padding(top = if (layout.small || layout.shortHeight) 6.dp else 10.dp)
            .fillMaxWidth()
            .glass()
            .padding(horizontal = if (layout.small) 12.dp else 16.dp, vertical = if (layout.small || layout.shortHeight) 8.dp else 12.dp),
        verticalArrangement = Arrangement.spacedBy(if (layout.small) 6.dp else 8.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.home_remaining_traffic), style = bzText(13.sp, color = Bz.Muted), modifier = Modifier.weight(1f))
            Text(quotaText(summary, numbers), style = bzText(13.sp, color = Bz.Muted), maxLines = 1)
        }
        summary.remainingFraction?.let { fraction ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(fraction)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(Bz.AccentGradient)
                )
            }
        }
    }
}

@Composable
private fun quotaText(summary: SubscriptionSummaryUiModel, numbers: BzNumbers) = buildAnnotatedString {
    val remaining = summary.remainingBytes
    if (remaining != null) {
        withStyle(SpanStyle(color = Bz.Text, fontWeight = FontWeight.Bold, fontSize = 14.sp)) {
            append(numbers.dec(gigabytes(remaining)))
        }
        append(" ")
        append(stringResource(R.string.bz_of_gb, numbers.dec(gigabytes(summary.totalBytes))))
    } else {
        withStyle(SpanStyle(color = Bz.Text, fontWeight = FontWeight.Bold, fontSize = 14.sp)) {
            append(stringResource(R.string.sub_info_unlimited))
        }
    }
    summary.daysLeft?.let { days ->
        append(" · ")
        append(pluralStringResource(R.plurals.bz_days, days, numbers.int(days.toLong())))
    }
}

@Composable
private fun Sparkline(liveTraffic: LiveTrafficUiModel, numbers: BzNumbers) {
    Column(
        Modifier
            .padding(top = 12.dp)
            .fillMaxWidth()
            .glass()
            .padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 6.dp)
    ) {
        Row(Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.bz_spark_title), style = bzText(12.sp, color = Bz.Muted), modifier = Modifier.weight(1f))
            Text(
                stringResource(R.string.bz_session_traffic, numbers.dec(megabytes(liveTraffic.sessionBytes), 0)),
                style = bzText(13.sp, FontWeight.Bold),
            )
        }
        val history = liveTraffic.downloadHistory
        Box(
            Modifier
                .padding(top = 4.dp)
                .fillMaxWidth()
                .height(46.dp)
                .drawBehind {
                    if (history.size < 2) return@drawBehind
                    val max = (history.maxOrNull() ?: 0L).coerceAtLeast(1L).toFloat()
                    val step = size.width / (history.size - 1)
                    val line = Path()
                    history.forEachIndexed { index, value ->
                        val x = index * step
                        val y = size.height - (value / max) * (size.height - 6.dp.toPx()) - 2.dp.toPx()
                        if (index == 0) line.moveTo(x, y) else line.lineTo(x, y)
                    }
                    val fill = Path().apply {
                        addPath(line)
                        lineTo(size.width, size.height)
                        lineTo(0f, size.height)
                        close()
                    }
                    drawPath(fill, Brush.verticalGradient(listOf(Bz.Accent.copy(alpha = 0.45f), Color.Transparent)))
                    drawPath(line, Bz.Accent, style = Stroke(2.dp.toPx()))
                }
        )
    }
}

/** First-run screen shown while no server exists. */
@Composable
private fun WelcomeScreen(
    layout: MainLayoutInfo,
    onAction: (MainAction) -> Unit,
    onOpenAddSheet: () -> Unit,
    openBot: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .bzBackdrop(BzBackdrop.Default)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = if (layout.small) 14.dp else 18.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        FullHeightScroll(Modifier.widthIn(max = 460.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.fillMaxWidth().height(52.dp), contentAlignment = Alignment.Center) { BzWordmark(small = layout.small) }
                Spacer(Modifier.height(if (layout.small) 4.dp else 14.dp))
                BzMark(if (layout.small) 150.dp else 200.dp)
                Text(
                    buildAnnotatedString {
                        append(stringResource(R.string.bz_welcome_title_1))
                        append(" ")
                        withStyle(SpanStyle(color = Bz.Accent)) { append(stringResource(R.string.bz_welcome_title_2)) }
                    },
                    style = bzText(if (layout.small) 25.sp else 30.sp, FontWeight.Black),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 10.dp, bottom = 6.dp),
                )
                Text(
                    stringResource(R.string.bz_welcome_body),
                    style = bzText(if (layout.small) 13.sp else 14.sp, color = Bz.Muted),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 6.dp),
                )
                FeatureRow(layout)
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    BzButton(
                        stringResource(R.string.bz_paste_link),
                        onOpenAddSheet,
                        Modifier.fillMaxWidth(),
                        iconRes = R.drawable.ic_ms_content_paste,
                        height = if (layout.small) 48.dp else 52.dp,
                    )
                    BzButton(
                        stringResource(R.string.bz_scan_qr),
                        { onAction(MainAction.ImportQRcode) },
                        Modifier.fillMaxWidth(),
                        style = BzButtonStyle.Outlined,
                        iconRes = R.drawable.ic_ms_qr_code_scanner,
                        height = if (layout.small) 48.dp else 52.dp,
                    )
                    BzButton(
                        stringResource(R.string.bz_free_trial_bot, BOT_HANDLE),
                        openBot,
                        Modifier.fillMaxWidth(),
                        style = BzButtonStyle.Text,
                        fontSize = 14.sp,
                    )
                }
            }
            Text(
                stringResource(R.string.bz_ten_years),
                style = bzText(12.sp, color = Bz.Faint),
                modifier = Modifier.padding(top = 16.dp, bottom = 14.dp),
            )
        }
    }
}

internal const val BOT_HANDLE = "@Kangavarserverbot"
internal const val CHANNEL_HANDLE = "@Kangavarserver"

@Composable
private fun FeatureRow(layout: MainLayoutInfo) {
    Row(
        Modifier
            .padding(top = if (layout.small) 12.dp else 16.dp, bottom = if (layout.small) 14.dp else 18.dp)
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .glass()
            .padding(vertical = 12.dp),
    ) {
        listOf(
            R.string.bz_feat_speed to R.string.bz_feat_speed_sub,
            R.string.bz_feat_ip to R.string.bz_feat_ip_sub,
            R.string.bz_feat_support to R.string.bz_feat_support_sub,
        ).forEachIndexed { index, (title, sub) ->
            if (index > 0) StatDivider()
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(title), style = bzText(if (layout.small) 13.sp else 15.sp, FontWeight.ExtraBold), textAlign = TextAlign.Center)
                Text(stringResource(sub), style = bzText(12.sp, color = Bz.Muted), textAlign = TextAlign.Center)
            }
        }
    }
}

/** Shared frame of the error and expired screens: top bar, pill, toned mark, title and body. */
@Composable
private fun StatusScreen(
    backdrop: BzBackdrop,
    tone: BzMarkTone,
    badgeRes: Int,
    pillText: String,
    title: String,
    body: String,
    layout: MainLayoutInfo,
    onOpenAddSheet: () -> Unit,
    openBot: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val color = if (tone == BzMarkTone.Error) Bz.Error else Bz.Warm
    Box(
        Modifier
            .fillMaxSize()
            .bzBackdrop(backdrop)
            .windowInsetsPadding(TopInsets)
            .padding(horizontal = if (layout.useRail) 28.dp else if (layout.small) 14.dp else 18.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            Modifier
                .widthIn(max = 460.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            HomeTopBar(layout, onOpenAddSheet, openBot)
            Spacer(Modifier.height(10.dp))
            BzStatePill(pillText, color, color, color.copy(alpha = 0.35f))
            Spacer(Modifier.height(14.dp))
            BzMark(if (layout.small) 140.dp else 168.dp, tone, badgeRes, animate = false)
            Text(
                title,
                style = bzText(if (layout.small) 25.sp else 32.sp, FontWeight.Black, color),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 14.dp),
            )
            Text(
                body,
                style = bzText(if (layout.small) 13.sp else 14.sp, color = Bz.Muted),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
            content()
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ErrorScreen(
    error: ConnectionErrorUiModel?,
    layout: MainLayoutInfo,
    onAction: (MainAction) -> Unit,
    onOpenAddSheet: () -> Unit,
    openBot: () -> Unit,
) {
    StatusScreen(
        backdrop = BzBackdrop.Error,
        tone = BzMarkTone.Error,
        badgeRes = R.drawable.ic_ms_priority_high,
        pillText = stringResource(R.string.bz_error_pill),
        title = stringResource(R.string.bz_error_title),
        body = error?.serverName?.let { stringResource(R.string.bz_error_body_server, it) } ?: stringResource(R.string.bz_error_body),
        layout = layout,
        onOpenAddSheet = onOpenAddSheet,
        openBot = openBot,
    ) {
        Row(
            Modifier
                .padding(top = if (layout.small) 12.dp else 16.dp, bottom = if (layout.small) 12.dp else 14.dp)
                .fillMaxWidth()
                .glass(Bz.BoxShape, fill = Bz.WarmDim, border = Bz.Warm.copy(alpha = 0.25f))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(painterResource(R.drawable.ic_ms_tips_and_updates_fill), null, tint = Bz.Warm, modifier = Modifier.size(20.dp))
            Text(stringResource(R.string.bz_error_tip), style = bzText(12.5.sp), modifier = Modifier.weight(1f))
        }
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            BzButton(
                stringResource(R.string.bz_retry_other),
                { onAction(MainAction.RetryOtherServer) },
                Modifier.fillMaxWidth(),
                iconRes = R.drawable.ic_ms_auto_awesome_fill,
                height = if (layout.small) 48.dp else 52.dp,
            )
            BzButton(
                stringResource(R.string.bz_send_report),
                openBot,
                Modifier.fillMaxWidth(),
                style = BzButtonStyle.Outlined,
                iconRes = R.drawable.ic_ms_send,
                height = if (layout.small) 48.dp else 52.dp,
            )
            BzButton(
                stringResource(R.string.bz_back_home),
                { onAction(MainAction.DismissConnectionError) },
                Modifier.fillMaxWidth(),
                style = BzButtonStyle.Text,
                textColor = Bz.Muted,
                fontSize = 14.sp,
            )
        }
    }
}

@Composable
private fun ExpiredScreen(
    subscription: GroupMapItem?,
    summary: SubscriptionSummaryUiModel?,
    layout: MainLayoutInfo,
    onAction: (MainAction) -> Unit,
    onOpenAddSheet: () -> Unit,
    openBot: () -> Unit,
) {
    val numbers = rememberBzNumbers()
    val renewLink = subscription?.usageInfo?.webPageUrl ?: subscription?.usageInfo?.supportUrl
    StatusScreen(
        backdrop = BzBackdrop.Warm,
        tone = BzMarkTone.Warm,
        badgeRes = R.drawable.ic_ms_hourglass_bottom_fill,
        pillText = stringResource(R.string.bz_expired_pill),
        title = stringResource(R.string.bz_expired_title),
        body = stringResource(if (summary?.expired == true) R.string.bz_expired_body_time else R.string.bz_expired_body_traffic),
        layout = layout,
        onOpenAddSheet = onOpenAddSheet,
        openBot = openBot,
    ) {
        if (summary != null && summary.totalBytes > 0L) {
            Row(
                Modifier
                    .padding(top = 16.dp, bottom = 14.dp)
                    .fillMaxWidth()
                    .glass()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                BzRowIcon(R.drawable.ic_ms_data_usage)
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.bz_period_usage), style = bzText(14.sp, FontWeight.Bold))
                    Text(
                        stringResource(
                            R.string.bz_usage_of,
                            numbers.dec(gigabytes(summary.usedBytes)),
                            numbers.dec(gigabytes(summary.totalBytes)),
                        ),
                        style = bzText(12.sp, color = Bz.Muted),
                    )
                }
            }
        } else {
            Spacer(Modifier.height(16.dp))
        }
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            BzButton(
                stringResource(R.string.bz_renew_same),
                { onAction(MainAction.OpenSubscriptionLink(renewLink ?: AppConfig.BRAND_TELEGRAM_BOT_URL)) },
                Modifier.fillMaxWidth(),
                iconRes = R.drawable.ic_ms_bolt,
                height = if (layout.small) 48.dp else 52.dp,
            )
            BzButton(
                stringResource(R.string.bz_buy_from_bot, BOT_HANDLE),
                openBot,
                Modifier.fillMaxWidth(),
                style = BzButtonStyle.Outlined,
                iconRes = R.drawable.ic_ms_send,
                height = if (layout.small) 48.dp else 52.dp,
            )
            BzButton(
                stringResource(R.string.bz_refresh_subscription),
                { onAction(MainAction.UpdateSubscriptions) },
                Modifier.fillMaxWidth(),
                style = BzButtonStyle.Text,
                textColor = Bz.Muted,
                fontSize = 14.sp,
            )
        }
    }
}

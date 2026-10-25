package com.v2ray.ang.ui.main

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.ConfigurationCompat
import com.v2ray.ang.R
import com.v2ray.ang.dto.GroupMapItem
import com.v2ray.ang.extension.toTrafficString
import com.v2ray.ang.ui.compose.BrandLatinFont
import com.v2ray.ang.ui.compose.BrandPersianFont
import com.v2ray.ang.ui.compose.LocalDarkTheme
import kotlinx.coroutines.delay
import java.util.Locale

private const val TIMER_TICK_MS = 1000L
private const val LOW_REMAINING_FRACTION = 0.1f

/** Home tab: connection hero (status, power button, timer), current server and remaining traffic. */
@Composable
internal fun MainHomeTab(
    uiState: MainUiState,
    statusText: String,
    layout: MainLayoutInfo,
    onAction: (MainAction) -> Unit,
    onSelectTab: (MainTab) -> Unit,
) {
    val phase = homeConnectionPhase(uiState.isRunning, uiState.isConnecting)
    val subscription = remember(uiState.groups, uiState.selectedServer) {
        subscriptionForHome(uiState.groups, uiState.selectedServer)
    }
    val scrollState = rememberScrollState()

    Box(
        Modifier
            .fillMaxSize()
            .background(homeBackground(phase))
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            HomeHeader(isRunning = uiState.isRunning, onAction = onAction)
            if (layout.twoColumns) {
                Row(
                    Modifier
                        .widthIn(max = 1040.dp)
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        ConnectionHero(phase, uiState, statusText, buttonSize = 176.dp, onAction = onAction)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        HomeCards(uiState, subscription, onSelectTab)
                    }
                }
            } else {
                Column(
                    Modifier.widthIn(max = 560.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    ConnectionHero(
                        phase,
                        uiState,
                        statusText,
                        buttonSize = if (layout.small) 148.dp else 176.dp,
                        onAction = onAction,
                    )
                    Spacer(Modifier.height(20.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        HomeCards(uiState, subscription, onSelectTab)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

/** Subscription of the selected server, or the first subscription that reports usage. */
private fun subscriptionForHome(groups: List<GroupMapItem>, server: SelectedServerUiModel?): GroupMapItem? =
    groups.firstOrNull { it.usageInfo != null && it.id == server?.subscriptionId }
        ?: groups.firstOrNull { it.usageInfo != null }

@Composable
private fun homeBackground(phase: HomeConnectionPhase): Brush {
    val base = MaterialTheme.colorScheme.background
    val glow by animateColorAsState(
        targetValue = if (phase == HomeConnectionPhase.Connected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
        } else {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
        },
        label = "homeGlow",
    )
    return Brush.verticalGradient(listOf(glow, base, base))
}

@Composable
private fun HomeHeader(isRunning: Boolean, onAction: (MainAction) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BrandName(Modifier.weight(1f))
        IconButton(onClick = { onAction(MainAction.TestCurrentServer) }, enabled = isRunning) {
            Icon(painterResource(R.drawable.ic_speed_24dp), contentDescription = stringResource(R.string.home_test_ping))
        }
        IconButton(onClick = { onAction(MainAction.ImportClipboard) }) {
            Icon(painterResource(R.drawable.ic_content_paste_24dp), contentDescription = stringResource(R.string.home_paste_link))
        }
    }
}

/** Logo mark plus the brand name: Vazirmatn for «باژیکو», Outfit for the Latin BAZHIKO. */
@Composable
internal fun BrandName(modifier: Modifier = Modifier) {
    val name = stringResource(R.string.app_name)
    val persian = isArabicScript(name)
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(if (LocalDarkTheme.current) R.drawable.brand_mark_dark else R.drawable.brand_mark_light),
            contentDescription = null,
            modifier = Modifier.size(30.dp),
        )
        Spacer(Modifier.size(8.dp))
        Text(
            text = name,
            fontFamily = if (persian) BrandPersianFont else BrandLatinFont,
            fontWeight = if (persian) FontWeight.Black else FontWeight.Bold,
            fontSize = 21.sp,
            letterSpacing = if (persian) 0.sp else 1.5.sp,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun ConnectionHero(
    phase: HomeConnectionPhase,
    uiState: MainUiState,
    statusText: String,
    buttonSize: Dp,
    onAction: (MainAction) -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        StatusPill(phase)
        Spacer(Modifier.height(12.dp))
        PowerButton(
            phase = phase,
            size = buttonSize,
            onClick = { onAction(MainAction.ToggleService) },
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(
                when (phase) {
                    HomeConnectionPhase.Disconnected -> R.string.home_title_disconnected
                    HomeConnectionPhase.Connecting -> R.string.home_title_connecting
                    HomeConnectionPhase.Connected -> R.string.home_title_connected
                }
            ),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = if (phase == HomeConnectionPhase.Connected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        when (phase) {
            HomeConnectionPhase.Connected -> {
                uiState.connectedSinceMillis?.let { ConnectionTimer(it) }
                Spacer(Modifier.height(10.dp))
                PingPill(uiState.status, statusText) { onAction(MainAction.TestCurrentServer) }
            }

            HomeConnectionPhase.Connecting -> HintText(stringResource(R.string.home_hint_connecting))
            HomeConnectionPhase.Disconnected -> HintText(stringResource(R.string.home_hint_disconnected))
        }
    }
}

@Composable
private fun HintText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun StatusPill(phase: HomeConnectionPhase) {
    val connected = phase == HomeConnectionPhase.Connected
    val dotColor = when (phase) {
        HomeConnectionPhase.Connected -> MaterialTheme.colorScheme.primary
        HomeConnectionPhase.Connecting -> MaterialTheme.colorScheme.tertiary
        HomeConnectionPhase.Disconnected -> MaterialTheme.colorScheme.outline
    }
    Surface(
        shape = CircleShape,
        color = if (connected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, if (connected) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Spacer(Modifier.size(8.dp))
            Text(
                text = stringResource(
                    when (phase) {
                        HomeConnectionPhase.Disconnected -> R.string.home_status_disconnected
                        HomeConnectionPhase.Connecting -> R.string.home_status_connecting
                        HomeConnectionPhase.Connected -> R.string.home_status_connected
                    }
                ),
                style = MaterialTheme.typography.labelLarge,
                color = if (connected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Big round connect button with a glow while connected and a spinning arc while connecting. */
@Composable
private fun PowerButton(phase: HomeConnectionPhase, size: Dp, onClick: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outlineVariant
    val transition = rememberInfiniteTransition(label = "power")
    val pulse by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(1600), RepeatMode.Reverse),
        label = "pulse",
    )
    val spin by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing)),
        label = "spin",
    )
    val connected = phase == HomeConnectionPhase.Connected
    val label = stringResource(if (connected) R.string.acc_stop else R.string.acc_start)

    Box(Modifier.size(size * 1.4f), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2, this.size.height / 2)
            val radius = size.toPx() / 2
            when (phase) {
                HomeConnectionPhase.Connected -> {
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(primary.copy(alpha = 0.35f), Color.Transparent),
                            center = center,
                            radius = radius * 1.4f * pulse,
                        ),
                        radius = radius * 1.4f * pulse,
                        center = center,
                    )
                    drawCircle(primary.copy(alpha = 0.4f), radius + 10.dp.toPx(), center, style = Stroke(2.dp.toPx()))
                }

                HomeConnectionPhase.Connecting -> {
                    drawCircle(outline, radius + 10.dp.toPx(), center, style = Stroke(2.dp.toPx()))
                    drawArc(
                        color = primary,
                        startAngle = spin,
                        sweepAngle = 100f,
                        useCenter = false,
                        topLeft = Offset(center.x - radius - 10.dp.toPx(), center.y - radius - 10.dp.toPx()),
                        size = androidx.compose.ui.geometry.Size((radius + 10.dp.toPx()) * 2, (radius + 10.dp.toPx()) * 2),
                        style = Stroke(4.dp.toPx(), cap = StrokeCap.Round),
                    )
                }

                HomeConnectionPhase.Disconnected -> drawCircle(
                    color = outline,
                    radius = radius + 14.dp.toPx(),
                    center = center,
                    style = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 7.dp.toPx())),
                        cap = StrokeCap.Round,
                    ),
                )
            }
        }
        Surface(
            onClick = onClick,
            enabled = phase != HomeConnectionPhase.Connecting,
            shape = CircleShape,
            color = if (connected) primary else MaterialTheme.colorScheme.surfaceContainerHigh,
            border = BorderStroke(1.dp, if (connected) primary else outline),
            shadowElevation = if (connected) 12.dp else 4.dp,
            modifier = Modifier
                .size(size)
                .semantics {
                    contentDescription = label
                    role = Role.Button
                },
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(R.drawable.ic_power_24dp),
                    contentDescription = null,
                    tint = if (connected) MaterialTheme.colorScheme.onPrimary else primary,
                    modifier = Modifier.size(size * 0.36f),
                )
            }
        }
    }
}

@Composable
private fun ConnectionTimer(sinceMillis: Long) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(sinceMillis) {
        while (true) {
            now = System.currentTimeMillis()
            delay(TIMER_TICK_MS)
        }
    }
    val locale = ConfigurationCompat.getLocales(LocalConfiguration.current)[0] ?: Locale.getDefault()
    Text(
        text = formatConnectionDuration(now - sinceMillis, locale),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

/** Shows the last ping result; tapping runs a new test of the current server. */
@Composable
private fun PingPill(status: MainStatus, statusText: String, onClick: () -> Unit) {
    val text = if (status == MainStatus.Connected) stringResource(R.string.home_test_ping) else statusText
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painterResource(R.drawable.ic_speed_24dp),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.size(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun HomeCards(
    uiState: MainUiState,
    subscription: GroupMapItem?,
    onSelectTab: (MainTab) -> Unit,
) {
    ServerCard(uiState.selectedServer, onClick = { onSelectTab(MainTab.Servers) })
    subscription?.usageInfo?.let { info ->
        val summary = remember(info) { buildSubscriptionSummary(info, System.currentTimeMillis()) }
        RemainingTrafficCard(summary, onClick = { onSelectTab(MainTab.Subscription) })
    }
}

@Composable
private fun HomeCard(onClick: () -> Unit, content: @Composable () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
    ) {
        content()
    }
}

@Composable
private fun ServerCard(server: SelectedServerUiModel?, onClick: () -> Unit) {
    HomeCard(onClick) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(R.drawable.ic_dns_24dp),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.home_current_server),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = server?.remarks?.ifBlank { null } ?: stringResource(R.string.home_no_server),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = server?.typeDescription ?: stringResource(R.string.home_no_server_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                painterResource(R.drawable.ic_chevron_end_24dp),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RemainingTrafficCard(summary: SubscriptionSummaryUiModel, onClick: () -> Unit) {
    val low = summary.expired || (summary.remainingFraction != null && summary.remainingFraction < LOW_REMAINING_FRACTION)
    val accent = if (low) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    HomeCard(onClick) {
        Column(
            Modifier
                .padding(14.dp)
                .semantics(mergeDescendants = true) {},
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.home_remaining_traffic),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                val daysText = when {
                    summary.expired -> stringResource(R.string.home_expired)
                    summary.daysLeft != null -> pluralStringResource(R.plurals.home_days_left, summary.daysLeft, summary.daysLeft)
                    else -> null
                }
                daysText?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (summary.expired) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (summary.remainingBytes != null) {
                    stringResource(R.string.home_traffic_of, summary.remainingBytes.toTrafficString(), summary.totalBytes.toTrafficString())
                } else {
                    stringResource(R.string.sub_info_unlimited)
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = accent,
            )
            summary.remainingFraction?.let { fraction ->
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape)
                        .clearAndSetSemantics {},
                    color = accent,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    drawStopIndicator = {},
                )
            }
        }
    }
}

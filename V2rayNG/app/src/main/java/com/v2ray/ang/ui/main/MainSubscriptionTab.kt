package com.v2ray.ang.ui.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.os.ConfigurationCompat
import com.v2ray.ang.AppConfig
import com.v2ray.ang.R
import com.v2ray.ang.dto.GroupMapItem
import com.v2ray.ang.dto.entities.SubscriptionUsageInfo
import com.v2ray.ang.extension.toTrafficString
import com.v2ray.ang.util.Utils
import java.text.NumberFormat
import java.util.Locale

private const val EXPIRY_DATE_PATTERN = "yyyy-MM-dd"
private const val LOW_REMAINING_FRACTION = 0.1f

/** Subscription tab: remaining traffic and time of every subscription, renewal and support shortcuts. */
@Composable
internal fun MainSubscriptionTab(
    groups: List<GroupMapItem>,
    layout: MainLayoutInfo,
    onAction: (MainAction) -> Unit,
    onNavigate: (MainDestination) -> Unit,
) {
    val subscriptions = remember(groups) { groups.filter { it.usageInfo != null } }
    val openLink: (String) -> Unit = { url -> onAction(MainAction.OpenSubscriptionLink(url)) }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            Modifier
                .widthIn(max = if (layout.twoColumns) 1040.dp else 560.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.sub_tab_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                if (subscriptions.isNotEmpty()) {
                    IconButton(onClick = { onAction(MainAction.UpdateSubscriptions) }) {
                        Icon(
                            painterResource(R.drawable.ic_refresh_24dp),
                            contentDescription = stringResource(R.string.acc_update_subscriptions),
                        )
                    }
                }
            }

            if (subscriptions.isEmpty()) {
                EmptySubscriptionCard(
                    onAdd = { onNavigate(MainDestination.Subscriptions) },
                    onFreeTrial = { openLink(AppConfig.BRAND_TELEGRAM_BOT_URL) },
                )
            } else {
                val rows = if (layout.twoColumns) subscriptions.chunked(2) else subscriptions.map { listOf(it) }
                rows.forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row.forEach { group ->
                            val info = group.usageInfo ?: return@forEach
                            SubscriptionCard(
                                group = group,
                                info = info,
                                onRenew = { openLink(AppConfig.BRAND_TELEGRAM_BOT_URL) },
                                onSupport = { openLink(info.supportUrl ?: AppConfig.BRAND_TELEGRAM_BOT_URL) },
                                onUpdate = { onAction(MainAction.UpdateGroupSubscription(group.id)) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        // Keeps a lone last card at half width in two-column layouts.
                        if (row.size == 1 && layout.twoColumns) Spacer(Modifier.weight(1f))
                    }
                }
            }

            ResellerCard(onClick = { openLink(AppConfig.BRAND_TELEGRAM_BOT_URL) })
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun TabCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
    ) {
        content()
    }
}

@Composable
private fun SubscriptionCard(
    group: GroupMapItem,
    info: SubscriptionUsageInfo,
    onRenew: () -> Unit,
    onSupport: () -> Unit,
    onUpdate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val summary = remember(info) { buildSubscriptionSummary(info, System.currentTimeMillis()) }
    val low = summary.expired || (summary.remainingFraction != null && summary.remainingFraction < LOW_REMAINING_FRACTION)
    val accent = if (low) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

    TabCard(modifier) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = info.title?.ifBlank { null } ?: group.remarks,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onUpdate) {
                    Icon(painterResource(R.drawable.ic_refresh_24dp), contentDescription = stringResource(R.string.title_sub_update))
                }
            }

            Column(
                Modifier
                    .fillMaxWidth()
                    .semantics(mergeDescendants = true) {},
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(4.dp))
                RemainingRing(summary, accent)
                Spacer(Modifier.height(12.dp))
                Text(
                    text = if (summary.remainingBytes != null) {
                        stringResource(R.string.home_traffic_of, summary.remainingBytes.toTrafficString(), summary.totalBytes.toTrafficString())
                    } else {
                        stringResource(R.string.sub_info_unlimited)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCell(
                        label = stringResource(R.string.sub_tab_used),
                        value = summary.usedBytes.toTrafficString(),
                        modifier = Modifier.weight(1f),
                    )
                    StatCell(
                        label = stringResource(R.string.sub_tab_expires),
                        value = summary.expireMillis?.let { Utils.formatTimestamp(it, EXPIRY_DATE_PATTERN) }
                            ?: stringResource(R.string.sub_info_unlimited),
                        caption = when {
                            summary.expired -> stringResource(R.string.home_expired)
                            summary.daysLeft != null -> pluralStringResource(R.plurals.home_days_left, summary.daysLeft, summary.daysLeft)
                            else -> null
                        },
                        captionIsError = summary.expired,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onRenew, shape = CircleShape, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.sub_tab_renew), maxLines = 1)
                }
                OutlinedButton(onClick = onSupport, shape = CircleShape, modifier = Modifier.weight(1f)) {
                    Icon(painterResource(R.drawable.ic_telegram_24dp), contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text(stringResource(R.string.sub_tab_support), maxLines = 1)
                }
            }
        }
    }
}

/** Circular gauge of the remaining traffic share; a full ring with an infinity sign when unlimited. */
@Composable
private fun RemainingRing(summary: SubscriptionSummaryUiModel, accent: Color) {
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    val fraction = summary.remainingFraction ?: 1f
    val locale = ConfigurationCompat.getLocales(LocalConfiguration.current)[0] ?: Locale.getDefault()
    Box(Modifier.size(136.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
            val inset = stroke.width / 2
            val arcSize = Size(size.width - stroke.width, size.height - stroke.width)
            val topLeft = Offset(inset, inset)
            drawArc(track, startAngle = 0f, sweepAngle = 360f, useCenter = false, topLeft = topLeft, size = arcSize, style = stroke)
            if (fraction > 0f) {
                drawArc(accent, startAngle = -90f, sweepAngle = 360f * fraction, useCenter = false, topLeft = topLeft, size = arcSize, style = stroke)
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (summary.remainingFraction != null) {
                    NumberFormat.getPercentInstance(locale).format(summary.remainingFraction.toDouble())
                } else {
                    "∞"
                },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = accent,
            )
            Text(
                text = stringResource(R.string.sub_tab_remaining),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StatCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    caption: String? = null,
    captionIsError: Boolean = false,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1)
        caption?.let {
            Text(
                it,
                style = MaterialTheme.typography.labelSmall,
                color = if (captionIsError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun EmptySubscriptionCard(onAdd: () -> Unit, onFreeTrial: () -> Unit) {
    TabCard {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(R.drawable.ic_card_membership_24dp),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.sub_tab_empty_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.sub_tab_empty_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
            Button(onClick = onAdd, shape = CircleShape, modifier = Modifier.widthIn(min = 200.dp)) {
                Text(stringResource(R.string.sub_tab_add))
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onFreeTrial, shape = CircleShape, modifier = Modifier.widthIn(min = 200.dp)) {
                Icon(painterResource(R.drawable.ic_telegram_24dp), contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text(stringResource(R.string.more_support_bot))
            }
        }
    }
}

/** Promotes the reseller programme of the brand's Telegram bot. */
@Composable
private fun ResellerCard(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painterResource(R.drawable.ic_bolt_24dp),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.sub_tab_reseller_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    stringResource(R.string.sub_tab_reseller_action),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                )
            }
            Icon(
                painterResource(R.drawable.ic_chevron_end_24dp),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

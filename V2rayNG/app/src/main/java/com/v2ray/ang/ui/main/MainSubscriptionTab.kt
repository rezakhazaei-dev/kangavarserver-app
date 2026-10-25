package com.v2ray.ang.ui.main

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v2ray.ang.AppConfig
import com.v2ray.ang.R
import com.v2ray.ang.dto.GroupMapItem
import java.util.Locale

/** Subscription tab of the design: plan card with the donut, subscription list, reseller promo. */
@Composable
internal fun MainSubscriptionTab(
    uiState: MainUiState,
    catalog: ServerCatalogUiModel,
    layout: MainLayoutInfo,
    onAction: (MainAction) -> Unit,
    onNavigate: (MainDestination) -> Unit,
    onOpenAddSheet: () -> Unit,
) {
    val groups = uiState.groups
    val planGroup = remember(groups, uiState.selectedServer) {
        subscriptionForHome(groups, uiState.selectedServer?.subscriptionId)
    }
    val now = remember(groups) { System.currentTimeMillis() }
    val summary = remember(planGroup, now) { planGroup?.usageInfo?.let { buildSubscriptionSummary(it, now) } }
    val openBot = { onAction(MainAction.OpenSubscriptionLink(AppConfig.BRAND_TELEGRAM_BOT_URL)) }

    Column(
        Modifier
            .fillMaxSize()
            .bzBackdrop(BzBackdrop.Default)
            .windowInsetsPadding(TopInsets)
            .padding(horizontal = if (layout.useRail) 28.dp else if (layout.small) 14.dp else 18.dp),
    ) {
        BzTopBar(
            height = if (layout.shortHeight) 44.dp else 52.dp,
            start = { BzTitle(stringResource(R.string.sub_tab_title)) },
            actions = {
                if (groups.isNotEmpty()) {
                    BzIconButton(R.drawable.ic_ms_sync, stringResource(R.string.bz_refresh_subscription), { onAction(MainAction.UpdateGroupSubscription("")) }, small = layout.small)
                }
                BzIconButton(R.drawable.ic_ms_add, stringResource(R.string.bz_add_subscription), onOpenAddSheet, small = layout.small)
            },
        )
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(top = if (layout.shortHeight) 8.dp else 14.dp, bottom = 16.dp),
        ) {
            if (groups.isEmpty()) {
                EmptySubscription(onOpenAddSheet, openBot)
                return@Column
            }
            if (layout.twoColumns) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(Modifier.weight(1f)) {
                        PlanCard(planGroup, summary, now, onAction)
                    }
                    Column(Modifier.weight(1f)) {
                        SideColumn(groups, catalog, now, wide = true, onAction, onNavigate, openBot)
                    }
                }
            } else {
                Column(Modifier.widthIn(max = 640.dp).align(Alignment.CenterHorizontally)) {
                    PlanCard(planGroup, summary, now, onAction)
                    SideColumn(groups, catalog, now, wide = false, onAction, onNavigate, openBot)
                }
            }
        }
    }
}

@Composable
private fun PlanCard(
    group: GroupMapItem?,
    summary: SubscriptionSummaryUiModel?,
    now: Long,
    onAction: (MainAction) -> Unit,
) {
    if (group == null || summary == null) return
    val numbers = rememberBzNumbers()
    val info = group.usageInfo
    Column(
        Modifier
            .fillMaxWidth()
            .glass(Bz.CardShape)
            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        updatedAgoText(group.lastUpdated, now, numbers)?.let {
            Text(it, style = bzText(13.sp, FontWeight.SemiBold, Bz.Muted), textAlign = TextAlign.Center)
        }
        Text(
            info?.title?.takeIf { it.isNotBlank() } ?: group.remarks,
            style = bzText(18.sp, FontWeight.ExtraBold).copy(textDirection = TextDirection.Content),
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 2.dp),
        )
        Donut(summary, numbers)
        Row(Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 14.dp)) {
            KeyValue(
                value = stringResource(R.string.bz_gb_value, numbers.dec(gigabytes(summary.usedBytes))),
                label = stringResource(R.string.sub_tab_used),
            )
            KeyValue(
                value = summary.expireMillis?.let { formatExpiryDate(it, numbers.locale) } ?: stringResource(R.string.sub_info_unlimited),
                label = summary.daysLeft?.let { pluralStringResource(R.plurals.bz_days_left, it, numbers.int(it.toLong())) }
                    ?: stringResource(R.string.sub_tab_expires),
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BzButton(
                stringResource(R.string.sub_tab_renew),
                { onAction(MainAction.OpenSubscriptionLink(info?.webPageUrl ?: AppConfig.BRAND_TELEGRAM_BOT_URL)) },
                Modifier.weight(1f),
                iconRes = R.drawable.ic_ms_bolt,
                height = 44.dp,
                fontSize = 14.sp,
            )
            BzButton(
                stringResource(R.string.sub_tab_support),
                { onAction(MainAction.OpenSubscriptionLink(info?.supportUrl ?: AppConfig.BRAND_TELEGRAM_BOT_URL)) },
                Modifier.weight(1f),
                style = BzButtonStyle.Outlined,
                iconRes = R.drawable.ic_ms_support_agent,
                height = 44.dp,
                fontSize = 14.sp,
            )
        }
    }
}

@Composable
private fun updatedAgoText(lastUpdated: Long, now: Long, numbers: BzNumbers): String? {
    val (unit, value) = timeAgo(lastUpdated, now) ?: return null
    val count = value.toInt()
    val digits = numbers.int(value)
    return when (unit) {
        AgoUnit.JustNow -> stringResource(R.string.bz_updated_now)
        AgoUnit.Minutes -> pluralStringResource(R.plurals.bz_updated_minutes, count, digits)
        AgoUnit.Hours -> pluralStringResource(R.plurals.bz_updated_hours, count, digits)
        AgoUnit.Days -> pluralStringResource(R.plurals.bz_updated_days, count, digits)
    }
}

/** Expiry date in the app language; Persian uses the Solar Hijri calendar ("۲۵ آبان ۱۴۰۵"). */
private fun formatExpiryDate(millis: Long, locale: Locale): String {
    val uLocale = if (locale.language == "fa") {
        android.icu.util.ULocale("fa_IR@calendar=persian")
    } else {
        android.icu.util.ULocale.forLocale(locale)
    }
    return android.icu.text.SimpleDateFormat("d MMMM y", uLocale).format(java.util.Date(millis))
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.KeyValue(value: String, label: String) {
    Column(
        Modifier
            .weight(1f)
            .semantics(mergeDescendants = true) {},
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, style = bzText(15.sp, FontWeight.ExtraBold), textAlign = TextAlign.Center, maxLines = 1)
        Text(label, style = bzText(12.sp, color = Bz.Muted), textAlign = TextAlign.Center, maxLines = 1)
    }
}

@Composable
private fun Donut(summary: SubscriptionSummaryUiModel, numbers: BzNumbers) {
    // The coloured arc is the used part; the empty track is what is left (Reza, 2026-10-25).
    val fraction = summary.usedFraction ?: 0f
    val remaining = summary.remainingBytes
    val center = if (remaining != null) numbers.dec(gigabytes(remaining)) else "∞"
    val caption = if (remaining != null) stringResource(R.string.bz_gb_remaining) else stringResource(R.string.sub_info_unlimited)
    val description = "$center $caption"
    Box(
        Modifier
            .padding(top = 10.dp, bottom = 6.dp)
            .size(140.dp)
            .clearAndSetSemantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 12.dp.toPx()
            val inset = stroke / 2f + 2.dp.toPx()
            val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
            val topLeft = Offset(inset, inset)
            drawArc(Color.White.copy(alpha = 0.08f), 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
            if (fraction > 0f) {
                drawArc(
                    brush = Brush.sweepGradient(listOf(Bz.Accent2, Bz.Accent, Bz.Accent2)),
                    startAngle = -90f,
                    sweepAngle = 360f * fraction,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(center, style = bzText(34.sp, FontWeight.Black))
            Text(caption, style = bzText(12.sp, color = Bz.Muted))
        }
    }
}

@Composable
private fun SideColumn(
    groups: List<GroupMapItem>,
    catalog: ServerCatalogUiModel,
    now: Long,
    wide: Boolean,
    onAction: (MainAction) -> Unit,
    onNavigate: (MainDestination) -> Unit,
    openBot: () -> Unit,
) {
    val numbers = rememberBzNumbers()
    BzListGroup(Modifier.padding(top = if (wide) 0.dp else 12.dp)) {
        groups.forEachIndexed { index, group ->
            if (index > 0) BzDivider()
            SubscriptionRow(group, catalog.serversPerSubscription[group.id] ?: 0, now, numbers, onAction, onNavigate)
        }
    }
    ResellerPromo(openBot)
    if (wide) {
        BzListGroup(Modifier.padding(top = 12.dp)) {
            TelegramRows(onAction)
        }
    }
}

@Composable
private fun SubscriptionRow(
    group: GroupMapItem,
    serverCount: Int,
    now: Long,
    numbers: BzNumbers,
    onAction: (MainAction) -> Unit,
    onNavigate: (MainDestination) -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }
    val blocked = group.usageInfo?.let { buildSubscriptionSummary(it, now).isBlocked() } == true
    val name = subscriptionDisplayName(group)
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(start = 14.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            Modifier
                .weight(1f)
                .semantics(mergeDescendants = true) {},
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (blocked) {
                BzRowIcon(R.drawable.ic_ms_hourglass_bottom_fill, tint = Bz.Warm)
            } else {
                BzRowIcon(R.drawable.ic_ms_check_circle_fill, tint = Bz.Accent)
            }
            Column(Modifier.weight(1f)) {
                Text(name, style = bzText(14.sp, FontWeight.Bold).copy(textDirection = TextDirection.Content), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    stringResource(
                        R.string.bz_sub_row_state,
                        pluralStringResource(R.plurals.bz_servers_count, serverCount, numbers.int(serverCount.toLong())),
                        stringResource(if (blocked) R.string.bz_sub_state_ended else R.string.bz_sub_state_active),
                    ),
                    style = bzText(12.sp, color = if (blocked) Bz.Warm else Bz.Muted),
                    maxLines = 1,
                )
            }
        }
        Box(Modifier.wrapContentSize(Alignment.TopEnd)) {
            BzIconButton(
                R.drawable.ic_ms_more_vert,
                stringResource(R.string.bz_sub_options, name),
                { showMenu = true },
                small = true,
                tint = Bz.Muted,
            )
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.bz_refresh_subscription)) },
                    onClick = {
                        showMenu = false
                        onAction(MainAction.UpdateGroupSubscription(group.id))
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.bz_manage_subscriptions)) },
                    onClick = {
                        showMenu = false
                        onNavigate(MainDestination.Subscriptions)
                    },
                )
            }
        }
    }
}

/** The panel's service (user) name when it sent one, otherwise the stored remarks. */
internal fun subscriptionDisplayName(group: GroupMapItem): String =
    group.usageInfo?.serviceName?.takeIf { it.isNotBlank() } ?: group.remarks

@Composable
private fun ResellerPromo(openBot: () -> Unit) {
    Row(
        Modifier
            .padding(top = 12.dp)
            .fillMaxWidth()
            .heightIn(min = 108.dp)
            .clip(Bz.CardShape)
            .background(Brush.horizontalGradient(listOf(Bz.Accent.copy(alpha = 0.03f), Bz.Accent.copy(alpha = 0.13f))))
            .border(1.dp, Bz.Accent.copy(alpha = 0.30f), Bz.CardShape)
            .clickable(role = Role.Button, onClick = openBot)
            .semantics(mergeDescendants = true) {}
            .padding(start = 16.dp, end = 14.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.sub_tab_reseller_title), style = bzText(16.sp, FontWeight.ExtraBold))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.sub_tab_reseller_action), style = bzText(13.sp, FontWeight.ExtraBold, Bz.Accent))
                Icon(painterResource(R.drawable.ic_ms_chevron_right), contentDescription = null, tint = Bz.Accent, modifier = Modifier.size(18.dp))
            }
        }
        BzMark(84.dp, animate = false)
    }
}

/** Bot and channel rows, shared with the settings tab. */
@Composable
internal fun TelegramRows(onAction: (MainAction) -> Unit) {
    LinkRow(R.drawable.ic_ms_smart_toy, stringResource(R.string.more_support_bot), BOT_HANDLE) {
        onAction(MainAction.OpenSubscriptionLink(AppConfig.BRAND_TELEGRAM_BOT_URL))
    }
    BzDivider()
    LinkRow(R.drawable.ic_ms_campaign, stringResource(R.string.more_telegram_channel), CHANNEL_HANDLE) {
        onAction(MainAction.OpenSubscriptionLink(AppConfig.BRAND_TELEGRAM_CHANNEL_URL))
    }
}

@Composable
private fun LinkRow(iconRes: Int, title: String, handle: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BzRowIcon(iconRes)
        Column(Modifier.weight(1f)) {
            Text(title, style = bzText(14.sp, FontWeight.Bold))
            LtrStartText(handle, bzText(12.sp, color = Bz.Muted))
        }
        Icon(painterResource(R.drawable.ic_ms_open_in_new), contentDescription = null, tint = Bz.Muted, modifier = Modifier.size(20.dp))
    }
}

/**
 * Latin text such as a Telegram handle: laid out left-to-right so "@" stays in front, but aligned
 * to the start of the row (the right in Persian).
 */
@Composable
internal fun LtrStartText(text: String, style: androidx.compose.ui.text.TextStyle, modifier: Modifier = Modifier) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Text(
        text,
        style = style.copy(textDirection = TextDirection.Ltr),
        textAlign = if (rtl) TextAlign.Right else TextAlign.Left,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun EmptySubscription(onOpenAddSheet: () -> Unit, openBot: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        BzMark(132.dp, animate = false)
        Text(stringResource(R.string.sub_tab_empty_title), style = bzText(20.sp, FontWeight.ExtraBold), textAlign = TextAlign.Center)
        Text(
            stringResource(R.string.sub_tab_empty_hint),
            style = bzText(14.sp, color = Bz.Muted),
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 360.dp),
        )
        BzButton(
            stringResource(R.string.sub_tab_add),
            onOpenAddSheet,
            Modifier
                .padding(top = 8.dp)
                .widthIn(min = 220.dp),
            iconRes = R.drawable.ic_ms_add_link,
            height = 48.dp,
        )
        BzButton(stringResource(R.string.bz_free_trial_bot, BOT_HANDLE), openBot, style = BzButtonStyle.Text, fontSize = 14.sp)
    }
}

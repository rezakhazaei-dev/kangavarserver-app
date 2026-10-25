package com.v2ray.ang.ui.main

import com.v2ray.ang.dto.GroupMapItem
import com.v2ray.ang.dto.entities.ProfileItem
import com.v2ray.ang.dto.entities.SubscriptionUsageInfo
import java.util.Locale

private const val MILLIS_PER_SECOND = 1000L
private const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L

/** Connection phase shown by the home tab's status pill, power button and title. */
internal enum class HomeConnectionPhase { Disconnected, Connecting, Connected }

internal fun homeConnectionPhase(isRunning: Boolean, isConnecting: Boolean): HomeConnectionPhase = when {
    isRunning -> HomeConnectionPhase.Connected
    isConnecting -> HomeConnectionPhase.Connecting
    else -> HomeConnectionPhase.Disconnected
}

/** The selected server as the home tab shows it; resolved again by [guid] after every reload. */
data class SelectedServerUiModel(
    val guid: String,
    val remarks: String,
    val typeDescription: String,
    val subscriptionId: String,
)

internal fun buildSelectedServerUiModel(guid: String, profile: ProfileItem): SelectedServerUiModel =
    SelectedServerUiModel(
        guid = guid,
        remarks = profile.remarks,
        typeDescription = serverProtocolDescription(profile),
        subscriptionId = profile.subscriptionId,
    )

/** Remaining traffic and time of one subscription, as shown on the home and subscription tabs. */
internal data class SubscriptionSummaryUiModel(
    /** Remaining bytes; null when the quota is unlimited. */
    val remainingBytes: Long?,
    /** Traffic quota in bytes; 0 means unlimited. */
    val totalBytes: Long,
    val usedBytes: Long,
    /** Remaining share of the quota in 0..1; null when the quota is unlimited. */
    val remainingFraction: Float?,
    /** Expiry in epoch milliseconds; null when the subscription never expires. */
    val expireMillis: Long?,
    /** Whole days left until expiry, rounded up; null when the subscription never expires. */
    val daysLeft: Int?,
    val expired: Boolean,
) {
    /** Used share of the quota in 0..1, the coloured part of the bars; null when unlimited. */
    val usedFraction: Float? get() = remainingFraction?.let { 1f - it }
}

internal fun buildSubscriptionSummary(info: SubscriptionUsageInfo, nowMillis: Long): SubscriptionSummaryUiModel {
    val unlimited = info.total <= 0L
    val remaining = if (unlimited) null else (info.total - info.used).coerceAtLeast(0L)
    val expireMillis = info.expire.takeIf { it > 0L }?.let { it * MILLIS_PER_SECOND }
    val expired = expireMillis != null && expireMillis <= nowMillis
    val daysLeft = expireMillis?.let { expire ->
        if (expired) 0 else ((expire - nowMillis + MILLIS_PER_DAY - 1) / MILLIS_PER_DAY).toInt()
    }
    return SubscriptionSummaryUiModel(
        remainingBytes = remaining,
        totalBytes = if (unlimited) 0L else info.total,
        usedBytes = info.used,
        remainingFraction = remaining?.let { (it.toDouble() / info.total).toFloat().coerceIn(0f, 1f) },
        expireMillis = expireMillis,
        daysLeft = daysLeft,
        expired = expired,
    )
}

/** Formats a connection duration as HH:MM:SS using the digits of [locale]. */
internal fun formatConnectionDuration(elapsedMillis: Long, locale: Locale): String {
    val totalSeconds = (elapsedMillis / MILLIS_PER_SECOND).coerceAtLeast(0L)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return String.format(locale, "%02d:%02d:%02d", hours, minutes, seconds)
}

/** True when [text] contains Arabic-script letters, so a Persian face must render it. */
internal fun isArabicScript(text: String): Boolean =
    text.any { Character.UnicodeBlock.of(it) == Character.UnicodeBlock.ARABIC }

/** Which full-screen variant the home tab shows. */
internal enum class HomeScreenMode { Welcome, Main, Error, Expired }

internal fun homeScreenMode(
    catalogLoaded: Boolean,
    hasServers: Boolean,
    isRunning: Boolean,
    isConnecting: Boolean,
    hasConnectionError: Boolean,
    subscriptionBlocked: Boolean,
): HomeScreenMode = when {
    catalogLoaded && !hasServers && !isRunning -> HomeScreenMode.Welcome
    isRunning || isConnecting -> HomeScreenMode.Main
    hasConnectionError -> HomeScreenMode.Error
    subscriptionBlocked -> HomeScreenMode.Expired
    else -> HomeScreenMode.Main
}

/** True when the subscription can no longer connect: expired, or a limited quota fully used. */
internal fun SubscriptionSummaryUiModel.isBlocked(): Boolean =
    expired || (totalBytes > 0L && remainingBytes == 0L)

/** Live traffic of the running connection, sampled about once per second from the app's own counters. */
data class LiveTrafficUiModel(
    val downloadBytesPerSecond: Long = 0L,
    val uploadBytesPerSecond: Long = 0L,
    /** Bytes moved in both directions since the connection started. */
    val sessionBytes: Long = 0L,
    /** Recent download rates in bytes per second, oldest first. */
    val downloadHistory: List<Long> = emptyList(),
)

/**
 * Adds one sample of counter deltas to [previous]. Negative deltas (counters reset by the system)
 * count as no traffic, and [elapsedMillis] below one millisecond is treated as one second.
 */
internal fun nextLiveTraffic(
    previous: LiveTrafficUiModel,
    rxDeltaBytes: Long,
    txDeltaBytes: Long,
    elapsedMillis: Long,
    historySize: Int,
): LiveTrafficUiModel {
    val rx = rxDeltaBytes.coerceAtLeast(0L)
    val tx = txDeltaBytes.coerceAtLeast(0L)
    val elapsed = if (elapsedMillis < 1L) MILLIS_PER_SECOND else elapsedMillis
    val down = rx * MILLIS_PER_SECOND / elapsed
    val up = tx * MILLIS_PER_SECOND / elapsed
    return LiveTrafficUiModel(
        downloadBytesPerSecond = down,
        uploadBytesPerSecond = up,
        sessionBytes = previous.sessionBytes + rx + tx,
        downloadHistory = (previous.downloadHistory + down).takeLast(historySize),
    )
}

/** Megabits per second, the unit the stats row shows. */
internal fun megabitsPerSecond(bytesPerSecond: Long): Double = bytesPerSecond * 8.0 / 1_000_000.0

private const val BYTES_PER_GIGABYTE = 1024.0 * 1024.0 * 1024.0
private const val BYTES_PER_MEGABYTE = 1024.0 * 1024.0

internal fun gigabytes(bytes: Long): Double = bytes / BYTES_PER_GIGABYTE

internal fun megabytes(bytes: Long): Double = bytes / BYTES_PER_MEGABYTE

/** Formats [value] with at most [fractionDigits] decimals using the digits of [locale]. */
internal fun formatDecimal(value: Double, locale: Locale, fractionDigits: Int = 1): String =
    java.text.NumberFormat.getNumberInstance(locale).apply {
        maximumFractionDigits = fractionDigits
        minimumFractionDigits = 0
        isGroupingUsed = false
    }.format(value)

/** The quick settings of the settings tab, read from and written to the app settings by the ViewModel. */
data class QuickSettingsUiModel(
    val vpnMode: Boolean = true,
    val iranDirect: Boolean = false,
    val perAppEnabled: Boolean = false,
    /** True when the selected apps bypass the VPN; false when only they use it. */
    val perAppBypass: Boolean = false,
    val perAppCount: Int = 0,
    val antiFilter: Boolean = false,
    val autoConnect: Boolean = false,
    val proMode: Boolean = false,
)

/** A quick setting the settings tab toggles through [MainAction.ChangeQuickSetting]. */
enum class QuickSetting(
    /** The running connection must restart for the change to take effect. */
    val needsRestart: Boolean,
) {
    VpnMode(needsRestart = true),
    IranDirect(needsRestart = true),
    AntiFilter(needsRestart = true),
    AutoConnect(needsRestart = false),
    ProMode(needsRestart = false),
}

/** Subscription of the selected server, or the first subscription that reports usage. */
internal fun subscriptionForHome(groups: List<GroupMapItem>, subscriptionId: String?): GroupMapItem? =
    groups.firstOrNull { it.usageInfo != null && it.id == subscriptionId }
        ?: groups.firstOrNull { it.usageInfo != null }

/** Unit of the "updated … ago" line of the subscription card. */
internal enum class AgoUnit { JustNow, Minutes, Hours, Days }

/** How long ago [thenMillis] was, in the largest whole unit; null when it was never set. */
internal fun timeAgo(thenMillis: Long, nowMillis: Long): Pair<AgoUnit, Long>? {
    if (thenMillis <= 0L) return null
    val minutes = (nowMillis - thenMillis).coerceAtLeast(0L) / 60_000L
    return when {
        minutes < 1L -> AgoUnit.JustNow to 0L
        minutes < 60L -> AgoUnit.Minutes to minutes
        minutes < 24L * 60L -> AgoUnit.Hours to minutes / 60L
        else -> AgoUnit.Days to minutes / (24L * 60L)
    }
}

/** True when the foreground subscription refresh may run: first time, or [intervalMillis] after the last one. */
internal fun shouldRefreshSubscriptionsOnOpen(lastMillis: Long?, nowMillis: Long, intervalMillis: Long): Boolean =
    lastMillis == null || nowMillis - lastMillis >= intervalMillis || nowMillis < lastMillis

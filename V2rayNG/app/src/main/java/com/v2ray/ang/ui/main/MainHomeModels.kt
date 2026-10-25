package com.v2ray.ang.ui.main

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
)

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

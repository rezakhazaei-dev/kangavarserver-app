package com.v2ray.ang.ui.main

import com.v2ray.ang.dto.entities.SubscriptionUsageInfo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Locale

class MainHomeModelsTest {
    private val now = 1_000_000_000_000L // ms
    private val day = 24L * 60L * 60L * 1000L

    @Test
    fun runningWinsOverConnecting() {
        assertEquals(HomeConnectionPhase.Connected, homeConnectionPhase(isRunning = true, isConnecting = true))
        assertEquals(HomeConnectionPhase.Connecting, homeConnectionPhase(isRunning = false, isConnecting = true))
        assertEquals(HomeConnectionPhase.Disconnected, homeConnectionPhase(isRunning = false, isConnecting = false))
    }

    @Test
    fun limitedQuotaReportsRemainingBytesAndFraction() {
        val summary = buildSubscriptionSummary(SubscriptionUsageInfo(upload = 100, download = 150, total = 1000), now)
        assertEquals(750L, summary.remainingBytes)
        assertEquals(250L, summary.usedBytes)
        assertEquals(0.75f, summary.remainingFraction!!, 0.0001f)
    }

    @Test
    fun overUsageClampsRemainingToZero() {
        val summary = buildSubscriptionSummary(SubscriptionUsageInfo(download = 2000, total = 1000), now)
        assertEquals(0L, summary.remainingBytes)
        assertEquals(0f, summary.remainingFraction!!, 0.0001f)
    }

    @Test
    fun unlimitedQuotaHasNoRemainingFigures() {
        val summary = buildSubscriptionSummary(SubscriptionUsageInfo(download = 5), now)
        assertNull(summary.remainingBytes)
        assertNull(summary.remainingFraction)
        assertEquals(0L, summary.totalBytes)
    }

    @Test
    fun daysLeftRoundUpPartialDays() {
        val expire = (now + day + 1000) / 1000
        assertEquals(2, buildSubscriptionSummary(SubscriptionUsageInfo(expire = expire), now).daysLeft)
        val exactlyOneDay = (now + day) / 1000
        assertEquals(1, buildSubscriptionSummary(SubscriptionUsageInfo(expire = exactlyOneDay), now).daysLeft)
    }

    @Test
    fun pastExpiryIsExpiredWithZeroDays() {
        val summary = buildSubscriptionSummary(SubscriptionUsageInfo(expire = now / 1000 - 1), now)
        assertTrue(summary.expired)
        assertEquals(0, summary.daysLeft)
    }

    @Test
    fun zeroExpireNeverExpires() {
        val summary = buildSubscriptionSummary(SubscriptionUsageInfo(expire = 0), now)
        assertNull(summary.expireMillis)
        assertNull(summary.daysLeft)
        assertFalse(summary.expired)
    }

    @Test
    fun durationIsFormattedAsHoursMinutesSeconds() {
        assertEquals("00:00:00", formatConnectionDuration(-5_000, Locale.US))
        assertEquals("01:02:03", formatConnectionDuration((3600 + 120 + 3) * 1000L + 999, Locale.US))
        assertEquals("27:00:00", formatConnectionDuration(27 * 3600 * 1000L, Locale.US))
    }

    @Test
    fun arabicScriptDetection() {
        assertTrue(isArabicScript("باژیکو"))
        assertFalse(isArabicScript("BAZHIKO"))
    }

    @Test
    fun homeScreenModePriorities() {
        fun mode(loaded: Boolean = true, servers: Boolean = true, running: Boolean = false, connecting: Boolean = false, error: Boolean = false, blocked: Boolean = false) =
            homeScreenMode(loaded, servers, running, connecting, error, blocked)
        assertEquals(HomeScreenMode.Welcome, mode(servers = false))
        // Until the list has loaded once, the welcome screen must not flash.
        assertEquals(HomeScreenMode.Main, mode(loaded = false, servers = false))
        assertEquals(HomeScreenMode.Main, mode(running = true, error = true, blocked = true))
        assertEquals(HomeScreenMode.Main, mode(connecting = true, error = true))
        assertEquals(HomeScreenMode.Error, mode(error = true, blocked = true))
        assertEquals(HomeScreenMode.Expired, mode(blocked = true))
        assertEquals(HomeScreenMode.Main, mode())
    }

    @Test
    fun subscriptionBlockedWhenExpiredOrQuotaUsedUp() {
        assertTrue(buildSubscriptionSummary(SubscriptionUsageInfo(download = 1000, total = 1000), now).isBlocked())
        assertTrue(buildSubscriptionSummary(SubscriptionUsageInfo(total = 1000, expire = now / 1000 - 10), now).isBlocked())
        assertFalse(buildSubscriptionSummary(SubscriptionUsageInfo(download = 10, total = 1000), now).isBlocked())
        assertFalse(buildSubscriptionSummary(SubscriptionUsageInfo(download = 10), now).isBlocked())
    }

    @Test
    fun liveTrafficTurnsCounterDeltasIntoRates() {
        var traffic = LiveTrafficUiModel()
        traffic = nextLiveTraffic(traffic, rxDeltaBytes = 2000, txDeltaBytes = 500, elapsedMillis = 500, historySize = 2)
        assertEquals(4000L, traffic.downloadBytesPerSecond)
        assertEquals(1000L, traffic.uploadBytesPerSecond)
        assertEquals(2500L, traffic.sessionBytes)
        traffic = nextLiveTraffic(traffic, rxDeltaBytes = -50, txDeltaBytes = 100, elapsedMillis = 0, historySize = 2)
        assertEquals(0L, traffic.downloadBytesPerSecond)
        assertEquals(100L, traffic.uploadBytesPerSecond)
        assertEquals(2600L, traffic.sessionBytes)
        traffic = nextLiveTraffic(traffic, rxDeltaBytes = 1000, txDeltaBytes = 0, elapsedMillis = 1000, historySize = 2)
        assertEquals(listOf(0L, 1000L), traffic.downloadHistory)
    }

    @Test
    fun decimalsUseTheLocaleDigitsWithoutGrouping() {
        assertEquals("12.4", formatDecimal(12.44, Locale.US))
        assertEquals("1234", formatDecimal(1234.0, Locale.US))
        assertEquals("۳۱٫۶", formatDecimal(31.6, Locale.forLanguageTag("fa")))
        assertEquals(1.0, megabitsPerSecond(125_000), 0.0001)
        assertEquals(1.0, gigabytes(1024L * 1024L * 1024L), 0.0001)
    }

    @Test
    fun timeAgoPicksTheLargestUnit() {
        val minute = 60_000L
        assertNull(timeAgo(-1L, now))
        assertEquals(AgoUnit.JustNow to 0L, timeAgo(now - 30_000L, now))
        assertEquals(AgoUnit.Minutes to 5L, timeAgo(now - 5 * minute, now))
        assertEquals(AgoUnit.Hours to 2L, timeAgo(now - 125 * minute, now))
        assertEquals(AgoUnit.Days to 3L, timeAgo(now - 3 * day - minute, now))
        // A clock that moved backwards reads as just now.
        assertEquals(AgoUnit.JustNow to 0L, timeAgo(now + minute, now))
    }
}

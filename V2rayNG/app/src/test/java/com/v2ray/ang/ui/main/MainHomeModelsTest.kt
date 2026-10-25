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
}

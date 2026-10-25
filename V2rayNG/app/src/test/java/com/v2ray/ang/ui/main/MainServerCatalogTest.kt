package com.v2ray.ang.ui.main

import com.v2ray.ang.dto.entities.ProfileItem
import com.v2ray.ang.enums.EConfigType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MainServerCatalogTest {

    private fun row(guid: String, remarks: String, delay: Long = 0L, subscriptionId: String = "sub") = ServerRowUiModel(
        guid = guid,
        profile = ProfileItem(configType = EConfigType.VLESS, subscriptionId = subscriptionId, remarks = remarks),
        remarks = remarks,
        statistics = "",
        typeDescription = "VLESS",
        testDelayMillis = delay,
        subscriptionBadge = "",
    )

    @Test
    fun countryComesFromFlagEmojiOrWholeWords() {
        assertEquals(ServerCountry.Germany, serverCountryOf("🇩🇪 Frankfurt 1"))
        assertEquals(ServerCountry.Usa, serverCountryOf("🇺🇸 Server"))
        assertEquals(ServerCountry.Turkey, serverCountryOf("ترکیه ۱"))
        assertEquals(ServerCountry.Usa, serverCountryOf("آمریکا ۲ · نیویورک"))
        assertEquals(ServerCountry.Germany, serverCountryOf("DE-2 reality"))
        assertEquals(ServerCountry.Usa, serverCountryOf("New York 3"))
    }

    @Test
    fun countryWordsInsideOtherWordsDoNotMatch() {
        assertEquals(ServerCountry.Other, serverCountryOf("Plus node"))
        assertEquals(ServerCountry.Other, serverCountryOf("trojan backup"))
        assertEquals(ServerCountry.Other, serverCountryOf(""))
    }

    @Test
    fun displayNameDropsFlagEmoji() {
        assertEquals("Frankfurt 1", serverDisplayName("🇩🇪 Frankfurt 1"))
        assertEquals("ترکیه ۱", serverDisplayName("ترکیه ۱"))
        // A name made only of a flag keeps the flag rather than becoming empty.
        assertEquals("🇩🇪", serverDisplayName("🇩🇪"))
    }

    @Test
    fun pingBarsAndColourBands() {
        assertEquals(0, pingLevel(0L))
        assertEquals(0, pingLevel(-1L))
        assertEquals(4, pingLevel(99L))
        assertEquals(3, pingLevel(100L))
        assertEquals(3, pingLevel(150L))
        assertEquals(2, pingLevel(200L))
        assertEquals(1, pingLevel(201L))
        assertEquals(PingQuality.Unknown, pingQuality(0L))
        assertEquals(PingQuality.Good, pingQuality(68L))
        assertEquals(PingQuality.Medium, pingQuality(181L))
        assertEquals(PingQuality.Bad, pingQuality(236L))
    }

    @Test
    fun catalogOrdersCountriesByFastestServerAndUntestedLast() {
        val rows = listOf(
            row("de1", "Germany 1", 86),
            row("us1", "USA 1", 174),
            row("tr1", "Turkey 1", 68),
            row("x1", "Other node"),
            row("de2", "Germany 2", -1),
        )
        val catalog = buildServerCatalog(rows)
        assertTrue(catalog.loaded)
        assertEquals(
            listOf(ServerCountry.Turkey, ServerCountry.Germany, ServerCountry.Usa, ServerCountry.Other),
            catalog.countries.map { it.country },
        )
        assertEquals("tr1", catalog.bestGuid)
        assertEquals(68L, catalog.bestDelayMillis)
        assertEquals("de1", catalog.countries[1].bestGuid)
        assertNull(catalog.countries[3].bestGuid)
        assertEquals(mapOf("sub" to 5), catalog.serversPerSubscription)
        assertEquals("us1", catalog.row("us1")?.guid)
        assertNull(catalog.row(null))
    }

    @Test
    fun smartPickPrefersFastestMeasuredThenFirstLeft() {
        val rows = listOf(row("a", "A", 0), row("b", "B", 120), row("c", "C", 90), row("d", "D", -1))
        assertEquals("c", pickSmartServer(rows))
        assertEquals("b", pickSmartServer(rows, excluding = setOf("c")))
        assertEquals("a", pickSmartServer(rows, excluding = setOf("b", "c")))
        assertNull(pickSmartServer(rows, excluding = setOf("a", "b", "c", "d")))
        assertNull(pickSmartServer(emptyList()))
    }

    @Test
    fun countryPickStaysInsideTheCountry() {
        val rows = listOf(row("de1", "Germany 1", 0), row("de2", "Germany 2", 140), row("tr1", "Turkey 1", 60))
        assertEquals("de2", pickCountryServer(rows, ServerCountry.Germany))
        assertEquals("tr1", pickCountryServer(rows, ServerCountry.Turkey))
        assertNull(pickCountryServer(rows, ServerCountry.Usa))
    }

    @Test
    fun ipMaskHidesTheMiddle() {
        assertEquals("185.230.•.24", maskIpAddress("185.230.11.24"))
        assertEquals("2a01:•:1", maskIpAddress("2a01:4f8:c0c:1::1"))
        assertEquals("not-an-ip", maskIpAddress(" not-an-ip "))
    }

    @Test
    fun clipboardSuggestionOnlyForLinks() {
        assertTrue(isImportableText(" https://sub.example.com/s/abc "))
        assertTrue(isImportableText("vless://uuid@host:443?security=reality#DE"))
        assertTrue(isImportableText("HYSTERIA2://pass@host:443"))
        assertEquals(false, isImportableText("https://"))
        assertEquals(false, isImportableText("hello"))
        assertEquals(false, isImportableText(null))
    }
}

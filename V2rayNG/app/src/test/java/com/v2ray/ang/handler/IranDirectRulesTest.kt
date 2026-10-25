package com.v2ray.ang.handler

import com.v2ray.ang.AppConfig
import com.v2ray.ang.dto.entities.RulesetItem
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class IranDirectRulesTest {
    private val iranDomains = RulesetItem(id = "ir-d", domain = listOf("geosite:category-ir", "domain:ir"), outboundTag = AppConfig.TAG_DIRECT)
    private val iranIps = RulesetItem(id = "ir-ip", ip = listOf("geoip:ir"), outboundTag = AppConfig.TAG_DIRECT)
    private val ads = RulesetItem(id = "ads", domain = listOf("geosite:category-ads-all"), outboundTag = AppConfig.TAG_BLOCKED)
    private val catchAll = RulesetItem(id = "all", port = "0-65535", outboundTag = AppConfig.TAG_PROXY)
    private val preset = listOf(ads, iranDomains, iranIps, catchAll)

    @Test
    fun onlyDirectIranRulesCount() {
        assertTrue(SettingsManager.isIranRule(iranDomains))
        assertTrue(SettingsManager.isIranRule(iranIps))
        assertFalse(SettingsManager.isIranRule(ads))
        assertFalse(SettingsManager.isIranRule(iranIps.copy(outboundTag = AppConfig.TAG_PROXY)))
    }

    @Test
    fun disablingKeepsUserRulesAndOrder() {
        val custom = RulesetItem(id = "mine", domain = listOf("domain:example.com"), outboundTag = AppConfig.TAG_DIRECT)
        val result = SettingsManager.applyIranDirect(listOf(custom, iranDomains, iranIps, catchAll), enabled = false, preset = preset)
        assertEquals(listOf("mine", "ir-d", "ir-ip", "all"), result.map { it.id })
        assertEquals(listOf(true, false, false, true), result.map { it.enabled })
        assertFalse(SettingsManager.iranDirectEnabled(result))
    }

    @Test
    fun enablingReusesExistingRules() {
        val current = listOf(iranDomains.copy(enabled = false), catchAll)
        val result = SettingsManager.applyIranDirect(current, enabled = true, preset = preset)
        assertEquals(listOf("ir-d", "all"), result.map { it.id })
        assertTrue(SettingsManager.iranDirectEnabled(result))
    }

    @Test
    fun enablingInsertsPresetRulesBeforeTheCatchAll() {
        val custom = RulesetItem(id = "mine", domain = listOf("domain:example.com"), outboundTag = AppConfig.TAG_PROXY)
        val result = SettingsManager.applyIranDirect(listOf(custom, catchAll), enabled = true, preset = preset)
        assertEquals(listOf("mine", "ir-d", "ir-ip", "all"), result.map { it.id })
        assertTrue(result.all { it.enabled })
    }

    @Test
    fun enablingAppendsWhenThereIsNoCatchAll() {
        val result = SettingsManager.applyIranDirect(emptyList(), enabled = true, preset = preset)
        assertEquals(listOf("ir-d", "ir-ip"), result.map { it.id })
    }
}

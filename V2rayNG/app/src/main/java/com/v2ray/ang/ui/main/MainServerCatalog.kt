package com.v2ray.ang.ui.main

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.v2ray.ang.R
import java.util.Locale

/** Countries the Bazhiko servers run in; [Other] collects every server whose name names none of them. */
enum class ServerCountry(@StringRes val labelRes: Int, @DrawableRes val flagRes: Int?) {
    Germany(R.string.bz_country_de, R.drawable.ic_flag_de),
    Usa(R.string.bz_country_us, R.drawable.ic_flag_us),
    Turkey(R.string.bz_country_tr, R.drawable.ic_flag_tr),
    Other(R.string.bz_country_other, null),
}

private val countryFlags = mapOf(
    "DE" to ServerCountry.Germany,
    "US" to ServerCountry.Usa,
    "TR" to ServerCountry.Turkey,
)

// Lower-case words matched as whole tokens, so "us" in "plus" or "de" in "node" never count.
private val countryWords = mapOf(
    ServerCountry.Germany to setOf("de", "ger", "germany", "deutschland", "frankfurt", "nuremberg", "آلمان", "المان"),
    ServerCountry.Usa to setOf("us", "usa", "america", "united states", "newyork", "new york", "dallas", "آمریکا", "امریکا", "آمريكا"),
    ServerCountry.Turkey to setOf("tr", "tur", "turkey", "turkiye", "türkiye", "istanbul", "ترکیه", "تركيا"),
)

private const val REGIONAL_INDICATOR_A = 0x1F1E6
private const val REGIONAL_INDICATOR_Z = 0x1F1FF

private fun isRegionalIndicator(codePoint: Int) = codePoint in REGIONAL_INDICATOR_A..REGIONAL_INDICATOR_Z

/** Reads the server's country from a flag emoji or a country name in its remarks. */
internal fun serverCountryOf(remarks: String): ServerCountry {
    val codePoints = remarks.codePoints().toArray()
    for (i in 0 until codePoints.size - 1) {
        if (isRegionalIndicator(codePoints[i]) && isRegionalIndicator(codePoints[i + 1])) {
            val code = buildString {
                append('A' + (codePoints[i] - REGIONAL_INDICATOR_A))
                append('A' + (codePoints[i + 1] - REGIONAL_INDICATOR_A))
            }
            countryFlags[code]?.let { return it }
        }
    }
    val normalized = remarks.lowercase(Locale.ROOT)
    val tokens = normalized.split(Regex("[^\\p{L}\\p{N}]+")).filter { it.isNotEmpty() }.toSet()
    for ((country, words) in countryWords) {
        if (words.any { word -> if (' ' in word) normalized.contains(word) else word in tokens }) return country
    }
    return ServerCountry.Other
}

/** Remarks without flag emoji, since the list already shows a drawn flag next to them. */
internal fun serverDisplayName(remarks: String): String {
    val builder = StringBuilder()
    remarks.codePoints().forEach { codePoint ->
        if (!isRegionalIndicator(codePoint)) builder.appendCodePoint(codePoint)
    }
    return builder.toString().trim().ifEmpty { remarks.trim() }
}

/** A measured delay; 0 means not tested and a negative value means the test failed. */
internal fun Long.isMeasuredDelay(): Boolean = this > 0L

/** Signal strength shown by the four ping bars: 4 under 100 ms down to 1 above 200 ms, 0 when unknown. */
internal fun pingLevel(delayMillis: Long): Int = when {
    !delayMillis.isMeasuredDelay() -> 0
    delayMillis < 100 -> 4
    delayMillis <= 150 -> 3
    delayMillis <= 200 -> 2
    else -> 1
}

/** Colour band of a ping: good under 100 ms, medium up to 200 ms, bad above, unknown otherwise. */
enum class PingQuality { Good, Medium, Bad, Unknown }

internal fun pingQuality(delayMillis: Long): PingQuality = when {
    !delayMillis.isMeasuredDelay() -> PingQuality.Unknown
    delayMillis < 100 -> PingQuality.Good
    delayMillis <= 200 -> PingQuality.Medium
    else -> PingQuality.Bad
}

/** One country of the server list: its servers in list order plus the fastest measured one. */
internal data class CountryGroupUiModel(
    val country: ServerCountry,
    val rows: List<ServerRowUiModel>,
    /** Fastest measured server of this country; null when none was measured. */
    val bestGuid: String?,
    val bestDelayMillis: Long,
)

/** The whole server list grouped the way the servers and home tabs show it. */
internal data class ServerCatalogUiModel(
    /** False until the first load finished, so the welcome screen never flashes on start. */
    val loaded: Boolean = false,
    val rows: List<ServerRowUiModel> = emptyList(),
    /** Countries ordered fastest first; untested countries keep [ServerCountry] order at the end. */
    val countries: List<CountryGroupUiModel> = emptyList(),
    /** Fastest measured server of the whole list. */
    val bestGuid: String? = null,
    val bestDelayMillis: Long = 0L,
    /** Number of servers of each subscription ID. */
    val serversPerSubscription: Map<String, Int> = emptyMap(),
) {
    fun row(guid: String?): ServerRowUiModel? = guid?.let { id -> rows.firstOrNull { it.guid == id } }
}

private fun List<ServerRowUiModel>.fastest(excluding: Set<String> = emptySet()): ServerRowUiModel? =
    filter { it.testDelayMillis.isMeasuredDelay() && it.guid !in excluding }.minByOrNull { it.testDelayMillis }

internal fun buildServerCatalog(rows: List<ServerRowUiModel>): ServerCatalogUiModel {
    val countries = rows.groupBy { it.country }.map { (country, countryRows) ->
        val best = countryRows.fastest()
        CountryGroupUiModel(
            country = country,
            rows = countryRows,
            bestGuid = best?.guid,
            bestDelayMillis = best?.testDelayMillis ?: 0L,
        )
    }.sortedWith(
        compareBy<CountryGroupUiModel> { if (it.bestGuid == null) Long.MAX_VALUE else it.bestDelayMillis }
            .thenBy { it.country.ordinal }
    )
    val best = rows.fastest()
    return ServerCatalogUiModel(
        loaded = true,
        rows = rows,
        countries = countries,
        bestGuid = best?.guid,
        bestDelayMillis = best?.testDelayMillis ?: 0L,
        serversPerSubscription = rows.groupingBy { it.profile.subscriptionId }.eachCount(),
    )
}

/**
 * Server the smart selection connects to: the fastest measured one other than [excluding],
 * else the first server not excluded. Null when nothing is left.
 */
internal fun pickSmartServer(rows: List<ServerRowUiModel>, excluding: Set<String> = emptySet()): String? =
    rows.fastest(excluding)?.guid ?: rows.firstOrNull { it.guid !in excluding }?.guid

/** Server a country chip connects to: that country's fastest measured server, else its first one. */
internal fun pickCountryServer(rows: List<ServerRowUiModel>, country: ServerCountry): String? =
    pickSmartServer(rows.filter { it.country == country })

/** Hides the third group of an IPv4 address ("185.230.•.24") and the middle of an IPv6 one. */
internal fun maskIpAddress(ip: String): String {
    val trimmed = ip.trim()
    val v4 = trimmed.split('.')
    if (v4.size == 4 && v4.all { part -> part.isNotEmpty() && part.all(Char::isDigit) }) {
        return "${v4[0]}.${v4[1]}.•.${v4[3]}"
    }
    val v6 = trimmed.split(':')
    if (v6.size >= 3) return "${v6.first()}:•:${v6.last()}"
    return trimmed
}

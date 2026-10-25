package com.v2ray.ang.handler

import com.v2ray.ang.dto.entities.SubscriptionUsageInfo
import com.v2ray.ang.util.Utils
import java.net.URI
import java.net.URLDecoder
import java.util.Locale

/**
 * Parses the account information subscription panels put in the response headers of the
 * subscription URL (the de-facto standard shared by 3x-ui, Marzban, Remnawave, Clash, v2rayN,
 * Hiddify and others):
 *
 * - `Subscription-Userinfo: upload=<bytes>; download=<bytes>; total=<bytes>; expire=<unix seconds>`
 * - `Profile-Title: <text>` or `base64:<base64 text>`
 * - `Support-Url: <link>` (usually a Telegram link)
 * - `Profile-Web-Page-Url: <link>` (the panel's subscription information page)
 * - `Content-Disposition: attachment; filename="<username>"` (Marzban, PasarGuard)
 *
 * Pure functions only, so the logic is covered by plain JVM tests.
 */
object SubscriptionInfoParser {
    private const val BASE64_PREFIX = "base64:"
    private const val MAX_TITLE_LENGTH = 100
    private const val MAX_URL_LENGTH = 2048

    /** Values at or above this are epoch milliseconds, not seconds (year 5138 in seconds). */
    private const val MILLIS_THRESHOLD = 100_000_000_000L

    /** Remarks given to a subscription imported from a link without a #name. */
    const val DEFAULT_REMARKS = "import sub"

    /** Extensions PasarGuard appends to the user name for client-specific formats. */
    private val FILE_EXTENSIONS = listOf(".txt", ".json", ".yaml", ".yml", ".zip", ".conf")

    private val SAFE_SCHEMES = setOf("http", "https", "tg")
    private val TELEGRAM_HOSTS = setOf("t.me", "telegram.me", "telegram.dog", "telegram.org")

    /**
     * Builds usage info from raw header values.
     *
     * @return null when the panel sent none of the supported headers, so the UI shows nothing.
     */
    fun parse(
        userInfo: String?,
        profileTitle: String?,
        supportUrl: String?,
        webPageUrl: String?,
        contentDisposition: String? = null,
        decodeBase64: (String) -> String = Utils::decode,
    ): SubscriptionUsageInfo? {
        val fields = parseUserInfoFields(userInfo)
        val title = decodeTitle(profileTitle, decodeBase64)
        val support = sanitizeLink(supportUrl)
        val webPage = sanitizeLink(webPageUrl)
        val service = parseServiceName(contentDisposition)

        if (fields == null && title == null && support == null && webPage == null && service == null) return null

        return SubscriptionUsageInfo(
            upload = fields?.get("upload") ?: 0,
            download = fields?.get("download") ?: 0,
            total = fields?.get("total") ?: 0,
            expire = normalizeExpire(fields?.get("expire") ?: 0),
            title = title,
            supportUrl = support,
            webPageUrl = webPage,
            serviceName = service,
        )
    }

    /**
     * Reads the file name of a `Content-Disposition` header, preferring the RFC 5987
     * `filename*=UTF-8''...` form, and drops the format extension PasarGuard adds.
     */
    fun parseServiceName(header: String?): String? {
        if (header.isNullOrBlank()) return null
        var plain: String? = null
        var extended: String? = null
        for (part in header.split(';')) {
            val separator = part.indexOf('=')
            if (separator <= 0) continue
            val key = part.substring(0, separator).trim().lowercase(Locale.ROOT)
            val value = part.substring(separator + 1).trim()
            when (key) {
                "filename" -> plain = value.removeSurrounding("\"")
                "filename*" -> extended = value.substringAfter("''", "").takeIf { it.isNotEmpty() }?.let {
                    runCatching { URLDecoder.decode(it.replace("+", "%2B"), "UTF-8") }.getOrNull()
                }
            }
        }
        var name = (extended ?: plain)?.trim().orEmpty()
        FILE_EXTENSIONS.firstOrNull { name.endsWith(it, ignoreCase = true) }?.let { name = name.dropLast(it.length) }
        name = name.filterNot { it.isISOControl() }.trim()
        return name.take(MAX_TITLE_LENGTH).ifEmpty { null }
    }

    /**
     * The subscription URL itself answers with the panel's information page when it is opened in
     * a browser (3x-ui, Marzban), so it is the fallback target of the "info page" button.
     */
    fun withWebPageFallback(info: SubscriptionUsageInfo, subscriptionUrl: String): SubscriptionUsageInfo {
        if (info.webPageUrl != null) return info
        return info.copy(webPageUrl = sanitizeLink(subscriptionUrl))
    }

    /**
     * Remarks to store after an update: the panel's service name replaces the default name and a
     * name that was itself taken from the panel earlier, but never a name the user typed.
     */
    fun remarksAfterUpdate(currentRemarks: String, previousServiceName: String?, serviceName: String?): String {
        if (serviceName.isNullOrBlank()) return currentRemarks
        val automatic = currentRemarks.isBlank() ||
            currentRemarks == DEFAULT_REMARKS ||
            (previousServiceName != null && currentRemarks == previousServiceName)
        return if (automatic) serviceName else currentRemarks
    }

    /** Returns [raw] when it is an http(s) or tg:// link that is safe to hand to ACTION_VIEW. */
    fun sanitizeLink(raw: String?): String? {
        val value = raw?.trim().orEmpty()
        if (value.isEmpty() || value.length > MAX_URL_LENGTH) return null
        val uri = runCatching { URI(value) }.getOrNull() ?: return null
        val scheme = uri.scheme?.lowercase(Locale.ROOT) ?: return null
        if (scheme !in SAFE_SCHEMES) return null
        if (scheme != "tg" && uri.host.isNullOrEmpty()) return null
        return value
    }

    /** True for links that open in the Telegram app. */
    fun isTelegramLink(url: String?): Boolean {
        val uri = runCatching { URI(url.orEmpty().trim()) }.getOrNull() ?: return false
        val scheme = uri.scheme?.lowercase(Locale.ROOT) ?: return false
        if (scheme == "tg") return true
        return uri.host?.lowercase(Locale.ROOT) in TELEGRAM_HOSTS
    }

    private fun parseUserInfoFields(header: String?): Map<String, Long>? {
        if (header.isNullOrBlank()) return null
        val result = HashMap<String, Long>()
        for (part in header.split(';')) {
            val separator = part.indexOf('=')
            if (separator <= 0) continue
            val key = part.substring(0, separator).trim().lowercase(Locale.ROOT)
            val raw = part.substring(separator + 1).trim()
            val number = raw.toLongOrNull() ?: raw.toDoubleOrNull()?.takeIf { it.isFinite() }?.toLong()
            if (number != null) result[key] = number.coerceAtLeast(0)
        }
        return result.takeIf { it.isNotEmpty() }
    }

    private fun normalizeExpire(value: Long): Long =
        if (value >= MILLIS_THRESHOLD) value / 1000 else value

    private fun decodeTitle(raw: String?, decodeBase64: (String) -> String): String? {
        val value = raw?.trim().orEmpty()
        if (value.isEmpty()) return null
        val decoded = when {
            value.startsWith(BASE64_PREFIX, ignoreCase = true) ->
                decodeBase64(value.substring(BASE64_PREFIX.length).trim())

            value.contains('%') ->
                runCatching { URLDecoder.decode(value, "UTF-8") }.getOrDefault(value)

            else -> value
        }
        return decoded.trim().take(MAX_TITLE_LENGTH).ifEmpty { null }
    }
}

package com.example.dutype.promo

import org.json.JSONArray
import org.json.JSONObject

/**
 * Versioned, backend-controlled config for the launch promo + app-icon feature.
 *
 * Source of truth: Firestore doc `app_config/launch` (see docs/launch-promo-and-icon-config.md).
 * The parsed form is cached locally, so every UI read is a memory/disk-cache read and never
 * touches the network.
 */
data class LaunchConfig(
    val version: Long = 0L,
    /** Global kill switch: false disables every promo AND reverts the icon to default. */
    val enabled: Boolean = false,
    val promos: List<PromoDef> = emptyList(),
    val appIcon: AppIconDef? = null
)

data class PromoFrequency(
    val maxShows: Int = 3,
    val minHoursBetween: Long = 24L
)

data class PromoDef(
    val id: String,
    /** "banner" or "fullscreen". */
    val type: String,
    val startAt: Long,
    val endAt: Long,
    /** lower-case roles ("worker", "employer"); empty = everyone. */
    val roles: Set<String>,
    /** lower-case language codes ("en", "hi", "te"); empty = everyone. */
    val languages: Set<String>,
    val minAppVersion: Long,
    val maxAppVersion: Long,
    val imageUrl: String,
    val title: Map<String, String>,
    val body: Map<String, String>,
    val ctaLabel: Map<String, String>,
    val deepLink: String,
    /** Higher number wins. */
    val priority: Int,
    val frequency: PromoFrequency,
    val dismissible: Boolean
) {
    val isFullscreen: Boolean get() = type == TYPE_FULLSCREEN

    companion object {
        const val TYPE_BANNER = "banner"
        const val TYPE_FULLSCREEN = "fullscreen"
    }
}

data class AppIconDef(
    val enabled: Boolean = false,
    val activeIconId: String = "default",
    val startAt: Long = 0L,
    val endAt: Long = 0L,
    val minAppVersion: Long = 0L,
    val maxAppVersion: Long = 0L
)

/** Per-promo local history used for frequency capping. */
data class PromoHistoryEntry(
    val showCount: Int = 0,
    val lastShownAt: Long = 0L,
    val dismissed: Boolean = false
)

/** A promo with its text already resolved for the user's language. */
data class ResolvedPromo(
    val id: String,
    val type: String,
    val imageUrl: String,
    val title: String,
    val body: String,
    val ctaLabel: String,
    val deepLink: String,
    val dismissible: Boolean
) {
    val isFullscreen: Boolean get() = type == PromoDef.TYPE_FULLSCREEN
}

/**
 * Tolerant parser. Unknown keys are ignored, bad individual promos are dropped, and any
 * structural error returns null so the caller keeps the last good config. Never throws.
 */
object LaunchConfigParser {
    private const val MAX_PROMOS = 20

    fun parse(raw: String?): LaunchConfig? {
        if (raw.isNullOrBlank()) return null
        return try {
            fromJson(JSONObject(raw))
        } catch (_: Throwable) {
            null
        }
    }

    fun fromJson(root: JSONObject): LaunchConfig? {
        return try {
            val promos = ArrayList<PromoDef>()
            val arr = root.optJSONArray("promos")
            if (arr != null) {
                for (i in 0 until minOf(arr.length(), MAX_PROMOS)) {
                    val obj = arr.optJSONObject(i) ?: continue
                    parsePromo(obj)?.let { promos.add(it) }
                }
            }
            LaunchConfig(
                version = root.optLong("version", 0L),
                enabled = root.optBoolean("enabled", false),
                promos = promos,
                appIcon = root.optJSONObject("appIcon")?.let { parseIcon(it) }
            )
        } catch (_: Throwable) {
            null
        }
    }

    private fun parsePromo(o: JSONObject): PromoDef? {
        return try {
            val id = o.optString("id", "").trim()
            if (id.isEmpty()) return null
            val type = o.optString("type", PromoDef.TYPE_BANNER).trim().lowercase()
                .takeIf { it == PromoDef.TYPE_BANNER || it == PromoDef.TYPE_FULLSCREEN }
                ?: PromoDef.TYPE_BANNER
            var imageUrl = o.optString("imageUrl", "").trim()
            // Only https images are accepted (cleartext is blocked by the network security config).
            if (imageUrl.isNotEmpty() && !imageUrl.startsWith("https://", ignoreCase = true)) imageUrl = ""
            // A fullscreen promo is an image; without one there is nothing to show.
            if (type == PromoDef.TYPE_FULLSCREEN && imageUrl.isEmpty()) return null
            val freq = o.optJSONObject("frequency")
            PromoDef(
                id = id,
                type = type,
                startAt = o.optLong("startAt", 0L),
                endAt = o.optLong("endAt", 0L),
                roles = stringSet(o.optJSONArray("roles")),
                languages = stringSet(o.optJSONArray("languages")),
                minAppVersion = o.optLong("minAppVersion", 0L),
                maxAppVersion = o.optLong("maxAppVersion", 0L),
                imageUrl = imageUrl,
                title = localized(o.opt("title")),
                body = localized(o.opt("body")),
                ctaLabel = localized(o.opt("ctaLabel")),
                deepLink = o.optString("deepLink", "").trim(),
                priority = o.optInt("priority", 0),
                frequency = PromoFrequency(
                    maxShows = freq?.optInt("maxShows", 3) ?: 3,
                    minHoursBetween = freq?.optLong("minHoursBetween", 24L) ?: 24L
                ),
                dismissible = o.optBoolean("dismissible", true)
            )
        } catch (_: Throwable) {
            null
        }
    }

    private fun parseIcon(o: JSONObject): AppIconDef = AppIconDef(
        enabled = o.optBoolean("enabled", false),
        activeIconId = o.optString("activeIconId", "default").trim().lowercase().ifEmpty { "default" },
        startAt = o.optLong("startAt", 0L),
        endAt = o.optLong("endAt", 0L),
        minAppVersion = o.optLong("minAppVersion", 0L),
        maxAppVersion = o.optLong("maxAppVersion", 0L)
    )

    private fun stringSet(arr: JSONArray?): Set<String> {
        if (arr == null) return emptySet()
        val out = LinkedHashSet<String>()
        for (i in 0 until arr.length()) {
            val s = arr.optString(i, "").trim().lowercase()
            if (s.isNotEmpty()) out.add(s)
        }
        return out
    }

    /** Accepts either {"en": "...", "te": "..."} or a plain string (treated as English). */
    private fun localized(value: Any?): Map<String, String> {
        return when (value) {
            is JSONObject -> {
                val out = LinkedHashMap<String, String>()
                val keys = value.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val v = value.optString(k, "").trim()
                    if (v.isNotEmpty()) out[k.trim().lowercase()] = v
                }
                out
            }
            is String -> if (value.isNotBlank()) mapOf("en" to value.trim()) else emptyMap()
            else -> emptyMap()
        }
    }
}

/** Pure, side-effect free promo selection. Fully unit-testable. */
object PromoSelector {

    /**
     * @param role "worker" or "employer" (case-insensitive)
     * @param appVersion the app's versionCode
     * @param history per-promo local show history
     * @param isImageReady must return true only when the image is already in the local disk cache
     */
    fun select(
        config: LaunchConfig?,
        now: Long,
        role: String,
        language: String,
        appVersion: Long,
        history: Map<String, PromoHistoryEntry>,
        isImageReady: (String) -> Boolean = { true }
    ): ResolvedPromo? {
        if (config == null || !config.enabled) return null
        val roleKey = role.trim().lowercase()
        val langKey = language.trim().lowercase()

        val best = config.promos
            .asSequence()
            .filter { isEligible(it, now, roleKey, langKey, appVersion, history[it.id]) }
            .filter { it.imageUrl.isEmpty() || isImageReady(it.imageUrl) }
            .sortedWith(compareByDescending<PromoDef> { it.priority }.thenByDescending { it.startAt })
            .firstOrNull()
            ?: return null

        return ResolvedPromo(
            id = best.id,
            type = best.type,
            imageUrl = best.imageUrl,
            title = pick(best.title, langKey),
            body = pick(best.body, langKey),
            ctaLabel = pick(best.ctaLabel, langKey),
            deepLink = best.deepLink,
            dismissible = best.dismissible
        )
    }

    fun isEligible(
        p: PromoDef,
        now: Long,
        role: String,
        language: String,
        appVersion: Long,
        h: PromoHistoryEntry?
    ): Boolean {
        if (p.startAt > 0L && now < p.startAt) return false
        if (p.endAt > 0L && now >= p.endAt) return false
        if (p.roles.isNotEmpty() && role !in p.roles) return false
        if (p.languages.isNotEmpty() && language !in p.languages) return false
        if (p.minAppVersion > 0L && appVersion < p.minAppVersion) return false
        if (p.maxAppVersion > 0L && appVersion > p.maxAppVersion) return false
        // Nothing to show when the promo has neither an image nor any text.
        if (p.imageUrl.isEmpty() && p.title.isEmpty() && p.body.isEmpty()) return false
        if (h != null) {
            if (h.dismissed && p.dismissible) return false
            if (p.frequency.maxShows > 0 && h.showCount >= p.frequency.maxShows) return false
            if (p.frequency.minHoursBetween > 0L &&
                h.lastShownAt > 0L &&
                now - h.lastShownAt < p.frequency.minHoursBetween * 3_600_000L
            ) return false
        }
        return true
    }

    private fun pick(map: Map<String, String>, lang: String): String =
        map[lang] ?: map["en"] ?: map.values.firstOrNull().orEmpty()
}

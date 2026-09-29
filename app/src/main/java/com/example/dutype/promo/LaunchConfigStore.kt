package com.example.dutype.promo

import android.content.Context
import org.json.JSONObject
import timber.log.Timber

/**
 * Local cache for the launch config (parsed once, kept in memory) and the per-promo
 * frequency-cap history. All reads are served from memory after the first load; the first
 * load is a single small SharedPreferences read and must be done off the main thread
 * (callers use Dispatchers.IO).
 */
object LaunchConfigStore {
    private const val PREFS = "launch_config_v1"
    private const val KEY_RAW = "raw"
    private const val KEY_LAST_FETCH_AT = "last_fetch_at"
    private const val KEY_HISTORY = "history"

    @Volatile private var cached: LaunchConfig? = null
    @Volatile private var loaded = false
    private var historyCache: MutableMap<String, PromoHistoryEntry>? = null
    private val lock = Any()

    /** True once a promo has been displayed in this process (max one promo per app session). */
    @Volatile var promoShownThisSession = false

    private fun prefs(ctx: Context) =
        ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Last good parsed config, or null when none was ever fetched. */
    fun current(ctx: Context): LaunchConfig? {
        if (loaded) return cached
        return synchronized(lock) {
            if (!loaded) {
                cached = try {
                    LaunchConfigParser.parse(prefs(ctx).getString(KEY_RAW, null))
                } catch (_: Throwable) {
                    null
                }
                loaded = true
            }
            cached
        }
    }

    /**
     * Validates and persists a raw config. An invalid payload is rejected and the previous
     * good config is kept. Returns true when the payload was accepted.
     */
    fun save(ctx: Context, raw: String): Boolean {
        val parsed = LaunchConfigParser.parse(raw)
        if (parsed == null) {
            Timber.tag("PromoConfig").w("Rejected invalid launch config; keeping last good config")
            return false
        }
        synchronized(lock) {
            prefs(ctx).edit().putString(KEY_RAW, raw).apply()
            cached = parsed
            loaded = true
        }
        Timber.tag("PromoConfig").d(
            "Saved launch config v%d enabled=%s promos=%d icon=%s",
            parsed.version, parsed.enabled, parsed.promos.size, parsed.appIcon?.activeIconId
        )
        return true
    }

    fun lastFetchAt(ctx: Context): Long = prefs(ctx).getLong(KEY_LAST_FETCH_AT, 0L)

    fun markFetched(ctx: Context, at: Long = System.currentTimeMillis()) {
        prefs(ctx).edit().putLong(KEY_LAST_FETCH_AT, at).apply()
    }

    // ---- frequency-cap history -------------------------------------------------------------

    fun history(ctx: Context): Map<String, PromoHistoryEntry> = synchronized(lock) {
        loadHistory(ctx).toMap()
    }

    fun recordShown(ctx: Context, promoId: String, now: Long = System.currentTimeMillis()) {
        update(ctx, promoId) { it.copy(showCount = it.showCount + 1, lastShownAt = now) }
    }

    fun recordDismissed(ctx: Context, promoId: String) {
        update(ctx, promoId) { it.copy(dismissed = true) }
    }

    /** Debug/QA helper: forget all show counts so promos can be seen again. */
    fun clearHistory(ctx: Context) {
        synchronized(lock) {
            historyCache = HashMap()
            prefs(ctx).edit().remove(KEY_HISTORY).apply()
        }
    }

    private fun update(ctx: Context, id: String, change: (PromoHistoryEntry) -> PromoHistoryEntry) {
        synchronized(lock) {
            val map = loadHistory(ctx)
            map[id] = change(map[id] ?: PromoHistoryEntry())
            // Keep the file tiny: retain only the 50 most recently shown entries.
            if (map.size > 50) {
                val keep = map.entries.sortedByDescending { it.value.lastShownAt }.take(50)
                map.clear()
                keep.forEach { map[it.key] = it.value }
            }
            val json = JSONObject()
            map.forEach { (k, v) ->
                json.put(
                    k,
                    JSONObject().put("c", v.showCount).put("t", v.lastShownAt).put("d", v.dismissed)
                )
            }
            prefs(ctx).edit().putString(KEY_HISTORY, json.toString()).apply()
        }
    }

    private fun loadHistory(ctx: Context): MutableMap<String, PromoHistoryEntry> {
        historyCache?.let { return it }
        val out = HashMap<String, PromoHistoryEntry>()
        try {
            val raw = prefs(ctx).getString(KEY_HISTORY, null)
            if (!raw.isNullOrBlank()) {
                val json = JSONObject(raw)
                val keys = json.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val o = json.optJSONObject(k) ?: continue
                    out[k] = PromoHistoryEntry(
                        showCount = o.optInt("c", 0),
                        lastShownAt = o.optLong("t", 0L),
                        dismissed = o.optBoolean("d", false)
                    )
                }
            }
        } catch (_: Throwable) {
            out.clear()
        }
        historyCache = out
        return out
    }
}

package com.igredirect

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

// ─── Data models ────────────────────────────────────────────────────────────

data class FrontendInstance(
    val id: String,
    val name: String,
    val url: String,
    val enabled: Boolean = true,
    val isBuiltIn: Boolean = false
) {
    fun toJson() = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("url", url)
        put("enabled", enabled)
        put("isBuiltIn", isBuiltIn)
    }

    companion object {
        fun fromJson(o: JSONObject) = FrontendInstance(
            id       = o.getString("id"),
            name     = o.getString("name"),
            url      = o.getString("url"),
            enabled  = o.optBoolean("enabled", true),
            isBuiltIn= o.optBoolean("isBuiltIn", false)
        )
    }
}

data class RedirectRule(
    val id: String,
    val name: String,
    /** Regex matched against the full incoming URL string. */
    val pattern: String,
    val enabled: Boolean = true,
    val isBuiltIn: Boolean = false
) {
    fun toJson() = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("pattern", pattern)
        put("enabled", enabled)
        put("isBuiltIn", isBuiltIn)
    }

    companion object {
        fun fromJson(o: JSONObject) = RedirectRule(
            id       = o.getString("id"),
            name     = o.getString("name"),
            pattern  = o.getString("pattern"),
            enabled  = o.optBoolean("enabled", true),
            isBuiltIn= o.optBoolean("isBuiltIn", false)
        )
    }
}

data class AppSettings(
    val activeFrontendId: String?,
    val frontends: MutableList<FrontendInstance>,
    val rules: MutableList<RedirectRule>,
    val preferredBrowserPackage: String?,
    val stripQueryParams: Boolean = true
) {
    /** Returns the selected frontend, falling back to the first enabled one. */
    fun getActiveFrontend(): FrontendInstance? =
        frontends.find { it.id == activeFrontendId && it.enabled }
            ?: frontends.firstOrNull { it.enabled }

    fun toJson(): JSONObject = JSONObject().apply {
        put("version", 1)
        put("activeFrontendId",        if (activeFrontendId.isNullOrBlank()) JSONObject.NULL else activeFrontendId)
        put("preferredBrowserPackage", if (preferredBrowserPackage.isNullOrBlank()) JSONObject.NULL else preferredBrowserPackage)
        put("stripQueryParams", stripQueryParams)
        put("frontends", JSONArray().also { a -> frontends.forEach { a.put(it.toJson()) } })
        put("rules",     JSONArray().also { a -> rules.forEach    { a.put(it.toJson()) } })
    }

    companion object {
        fun fromJson(o: JSONObject): AppSettings {
            val fa = o.getJSONArray("frontends")
            val ra = o.getJSONArray("rules")
            return AppSettings(
                activeFrontendId        = o.optString("activeFrontendId", "").takeIf { it.isNotBlank() && it != "null" },
                preferredBrowserPackage = o.optString("preferredBrowserPackage", "").takeIf { it.isNotBlank() && it != "null" },
                stripQueryParams        = o.optBoolean("stripQueryParams", true),
                frontends = (0 until fa.length()).mapTo(mutableListOf()) { FrontendInstance.fromJson(fa.getJSONObject(it)) },
                rules     = (0 until ra.length()).mapTo(mutableListOf()) { RedirectRule.fromJson(ra.getJSONObject(it)) }
            )
        }
    }
}

// ─── Singleton repository ────────────────────────────────────────────────────

object SettingsManager {

    private const val PREFS_NAME = "igredirect_prefs"
    private const val KEY_SETTINGS = "settings_json"

    // Built-in Kittygram instances from the official instance list
    private val BUILT_IN_FRONTENDS = mutableListOf(
        FrontendInstance("bf-kg-meowing-de",          "kg.meowing.de",              "https://kg.meowing.de",              true,  true),
        FrontendInstance("bf-kittygr-am",             "kittygr.am",                 "https://kittygr.am",                 true,  true),
        FrontendInstance("bf-kittygram-pussthecat",   "kittygram.pussthecat.org",   "https://kittygram.pussthecat.org",   true,  true),
        FrontendInstance("bf-kg-lus-lu",              "kg.lus.lu",                  "https://kg.lus.lu",                  true,  true),
        FrontendInstance("bf-kittygram-kareem",       "kittygram.kareem.one",       "https://kittygram.kareem.one",       true,  true),
        FrontendInstance("bf-kittygram-nadeko",       "kittygram.nadeko.net",       "https://kittygram.nadeko.net",       true,  true),
        FrontendInstance("bf-kittygram-nexussfan",    "kittygram.nexussfan.cz",     "https://kittygram.nexussfan.cz",     true,  true),
        FrontendInstance("bf-gram-utilibre",          "gram.utilibre.org",          "https://gram.utilibre.org",          true,  true),
        FrontendInstance("bf-kittygram-irelephant",   "kittygram.irelephant.net",   "https://kittygram.irelephant.net",   true,  true),
    )

    // Built-in URL detection rules
    private val BUILT_IN_RULES = mutableListOf(
        RedirectRule(
            id        = "br-instagram",
            name      = "Instagram",
            pattern   = """^https?://(www\.)?instagram\.com(/.*)?$""",
            enabled   = true,
            isBuiltIn = true
        )
    )

    fun load(context: Context): AppSettings {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json  = prefs.getString(KEY_SETTINGS, null)
        return if (json != null) {
            try { AppSettings.fromJson(JSONObject(json)) }
            catch (_: Exception) { createDefault() }
        } else {
            createDefault()
        }
    }

    fun save(context: Context, settings: AppSettings) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SETTINGS, settings.toJson().toString())
            .apply()
    }

    fun exportJson(context: Context): String =
        load(context).toJson().toString(2)

    /**
     * Replaces all current settings with the imported JSON.
     * Returns true on success.
     */
    fun importJson(context: Context, json: String): Boolean {
        return try {
            val settings = AppSettings.fromJson(JSONObject(json))
            save(context, settings)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun createDefault() = AppSettings(
        activeFrontendId        = "bf-kg-meowing-de",
        frontends               = BUILT_IN_FRONTENDS.map { it.copy() }.toMutableList(),
        rules                   = BUILT_IN_RULES.map  { it.copy() }.toMutableList(),
        preferredBrowserPackage = null,
        stripQueryParams        = true
    )

    /** Create a new UUID for user-added items. */
    fun newId(): String = UUID.randomUUID().toString()
}

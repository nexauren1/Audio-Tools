package com.nexauren.audiotools.ui

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class UsageEntry(
    val toolId: String,
    val timestamp: Long
)

object UsageStore {
    private const val PREFS = "audio_tools_personal"
    private const val HISTORY = "history_v2"
    private const val FAVORITES = "favorites_v2"
    private const val MAX_HISTORY = 120

    fun record(context: Context, toolId: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val current = history(context).toMutableList()
        current.add(0, UsageEntry(toolId, System.currentTimeMillis()))
        val limited = current.take(MAX_HISTORY)
        val json = JSONArray()
        limited.forEach {
            json.put(
                JSONObject()
                    .put("toolId", it.toolId)
                    .put("timestamp", it.timestamp)
            )
        }
        prefs.edit().putString(HISTORY, json.toString()).apply()
    }

    fun history(context: Context): List<UsageEntry> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(HISTORY, null) ?: return emptyList()

        return runCatching {
            val json = JSONArray(raw)
            buildList {
                for (i in 0 until json.length()) {
                    val item = json.optJSONObject(i) ?: continue
                    val id = item.optString("toolId")
                    if (id.isNotBlank()) {
                        add(
                            UsageEntry(
                                id,
                                item.optLong("timestamp")
                            )
                        )
                    }
                }
            }
        }.getOrDefault(emptyList())
    }

    fun favorites(context: Context): Set<String> =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet(FAVORITES, emptySet())
            ?.toSet()
            ?: emptySet()

    fun isFavorite(context: Context, toolId: String): Boolean =
        favorites(context).contains(toolId)

    fun toggleFavorite(context: Context, toolId: String): Boolean {
        val prefs =
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val next = favorites(context).toMutableSet()

        val nowFavorite = if (next.contains(toolId)) {
            next.remove(toolId)
            false
        } else {
            next.add(toolId)
            true
        }

        prefs.edit()
            .putStringSet(FAVORITES, next)
            .apply()

        return nowFavorite
    }

    fun totalRuns(context: Context): Int =
        history(context).size

    fun uniqueTools(context: Context): Int =
        history(context)
            .map { it.toolId }
            .distinct()
            .size

    fun lastDaysRuns(
        context: Context,
        days: Int
    ): Int {
        val cutoff =
            System.currentTimeMillis() -
                days * 24L * 60L * 60L * 1000L

        return history(context)
            .count { it.timestamp >= cutoff }
    }

    fun usageCount(
        context: Context,
        toolId: String
    ): Int =
        history(context)
            .count { it.toolId == toolId }
}

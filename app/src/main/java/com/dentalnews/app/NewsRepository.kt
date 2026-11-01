package com.dentalnews.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** تخزين محلي بسيط (SharedPreferences) لآخر الأخبار. */
class NewsStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("news", Context.MODE_PRIVATE)

    fun load(): List<NewsItem> {
        val raw = prefs.getString("items", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map {
                val o = arr.getJSONObject(it)
                NewsItem(o.getString("title"), o.getString("source"), o.getString("link"), o.getLong("time"))
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /** يدمج الجديد مع المخزّن ويعيد العناصر الجديدة فقط (غير الموجودة سابقًا). */
    fun merge(fresh: List<NewsItem>): List<NewsItem> {
        val existing = load()
        val known = existing.map { it.link }.toSet()
        val added = fresh.distinctBy { it.link }.filter { it.link !in known }
        val all = (added + existing).sortedByDescending { it.timeMs }.take(MAX_ITEMS)
        val arr = JSONArray()
        all.forEach {
            arr.put(
                JSONObject().put("title", it.title).put("source", it.source)
                    .put("link", it.link).put("time", it.timeMs)
            )
        }
        prefs.edit().putString("items", arr.toString()).apply()
        return added.sortedByDescending { it.timeMs }
    }

    companion object {
        const val MAX_ITEMS = 150
    }
}

data class RefreshResult(val newItems: List<NewsItem>, val okSources: Int, val totalSources: Int)

object NewsRepository {
    /** يجلب كل المصادر (كل مصدر مستقل عن الآخر) ويحدّث التخزين. استدعِه من خيط خلفي. */
    fun refresh(context: Context): RefreshResult {
        val collected = mutableListOf<NewsItem>()
        var ok = 0
        for (source in Sources.all) {
            runCatching { RssFetcher.fetch(source) }
                .onSuccess { collected += it; ok++ }
        }
        val added = NewsStore(context).merge(collected)
        return RefreshResult(added, ok, Sources.all.size)
    }
}

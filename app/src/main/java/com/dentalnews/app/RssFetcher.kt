package com.dentalnews.app

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale

/** يجلب خلاصة RSS/Atom ويحوّلها إلى قائمة NewsItem. */
object RssFetcher {

    fun fetch(source: Source): List<NewsItem> {
        val conn = (URL(source.feedUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 15_000
            setRequestProperty("User-Agent", "Mozilla/5.0 (Android) DentalNews/1.0")
            setRequestProperty(
                "Accept",
                "application/rss+xml, application/atom+xml, application/xml, text/xml, */*"
            )
        }
        try {
            if (conn.responseCode !in 200..299) error("HTTP ${conn.responseCode}")
            return conn.inputStream.use { parse(it, source.name) }
        } finally {
            conn.disconnect()
        }
    }

    fun parse(stream: InputStream, sourceName: String): List<NewsItem> {
        val p = XmlPullParserFactory.newInstance().newPullParser()
        p.setInput(stream, null)

        val items = mutableListOf<NewsItem>()
        var inItem = false
        var title = ""
        var link = ""
        var date = ""

        var ev = p.eventType
        while (ev != XmlPullParser.END_DOCUMENT) {
            when (ev) {
                XmlPullParser.START_TAG -> when (p.name) {
                    "item", "entry" -> {
                        inItem = true; title = ""; link = ""; date = ""
                    }
                    "title" -> if (inItem) title = p.nextText()
                    "link" -> if (inItem) {
                        val href = p.getAttributeValue(null, "href")   // Atom
                        val rel = p.getAttributeValue(null, "rel")
                        val text = p.nextText().trim()                 // RSS
                        if (link.isEmpty()) {
                            if (href != null && (rel == null || rel == "alternate")) link = href
                            else if (href == null) link = text
                        }
                    }
                    "pubDate", "published", "updated", "dc:date" ->
                        if (inItem && date.isEmpty()) date = p.nextText()
                }
                XmlPullParser.END_TAG -> if (p.name == "item" || p.name == "entry") {
                    val t = clean(title)
                    if (t.isNotBlank() && link.isNotBlank()) {
                        items += NewsItem(t, sourceName, link.trim(), parseDate(date))
                    }
                    inItem = false
                }
            }
            ev = p.next()
        }
        return items
    }

    private val dateFormats = listOf(
        "EEE, dd MMM yyyy HH:mm:ss Z",
        "EEE, dd MMM yyyy HH:mm:ss zzz",
        "yyyy-MM-dd'T'HH:mm:ssXXX",
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX"
    )

    private fun parseDate(s: String): Long {
        val str = s.trim()
        if (str.isNotEmpty()) {
            for (f in dateFormats) {
                try {
                    return SimpleDateFormat(f, Locale.US).parse(str)!!.time
                } catch (_: Exception) {
                }
            }
        }
        return System.currentTimeMillis()
    }

    /** يزيل وسوم HTML ويفك ترميز الكيانات الشائعة. */
    private fun clean(s: String): String {
        var r = s.replace(Regex("<[^>]*>"), "")
        r = Regex("&#(\\d+);").replace(r) { it.groupValues[1].toInt().toChar().toString() }
        r = Regex("&#x([0-9a-fA-F]+);").replace(r) { it.groupValues[1].toInt(16).toChar().toString() }
        r = r.replace("&nbsp;", " ").replace("&quot;", "\"").replace("&apos;", "'")
            .replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&")
        return r.replace(Regex("\\s+"), " ").trim()
    }
}

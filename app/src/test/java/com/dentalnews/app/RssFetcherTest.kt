package com.dentalnews.app

import org.junit.Assert.assertEquals
import org.junit.Test

class RssFetcherTest {

    @Test
    fun parsesRss() {
        val xml = """<?xml version="1.0"?><rss version="2.0"><channel><title>X</title>
            <link>https://site.com</link>
            <item><title><![CDATA[New <b>laser</b> &amp; scanner]]></title>
            <link>https://site.com/a</link>
            <pubDate>Sat, 01 Nov 2026 08:00:00 +0000</pubDate></item></channel></rss>"""
        val r = RssFetcher.parse(xml.byteInputStream(), "Site")
        assertEquals(1, r.size)
        assertEquals("New laser & scanner", r[0].title)
        assertEquals("Site", r[0].source)
        assertEquals("https://site.com/a", r[0].link)
    }

    @Test
    fun parsesAtom() {
        val xml = """<?xml version="1.0"?><feed xmlns="http://www.w3.org/2005/Atom">
            <link href="https://site.com/feed" rel="self"/>
            <entry><title>Atom item</title>
            <link rel="alternate" href="https://site.com/b"/>
            <updated>2026-11-01T08:00:00Z</updated></entry></feed>"""
        val r = RssFetcher.parse(xml.byteInputStream(), "AtomSite")
        assertEquals(1, r.size)
        assertEquals("https://site.com/b", r[0].link)
    }
}

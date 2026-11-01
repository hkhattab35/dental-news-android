#!/usr/bin/env python3
"""يجلب عناوين الأخبار من خلاصات RSS/Atom ويكتب docs/news.json (يعمل يوميًا عبر GitHub Actions)."""
import html
import json
import re
import sys
import urllib.request
import xml.etree.ElementTree as ET
from datetime import datetime, timezone
from email.utils import parsedate_to_datetime
from pathlib import Path

# ===== مصادر المحتوى: عدّل هنا لإضافة مصدر أو حذفه =====
SOURCES = [
    ("Dental Tribune", "https://www.dental-tribune.com/news/feed"),
    ("Dentistry.co.uk", "https://www.dentistry.co.uk/feed"),
    ("Dentistry Today", "https://dentistrytoday.com/feed"),
    ("DentalReach", "https://www.dentalreach.today/feed"),
    ("ScienceDaily – Dentistry", "https://www.sciencedaily.com/rss/health_medicine/dentistry.xml"),
]

OUT = Path(__file__).resolve().parent.parent / "docs" / "news.json"
MAX_ITEMS = 200


def local(tag):
    return tag.rsplit("}", 1)[-1]


def clean(text):
    text = html.unescape(re.sub(r"<[^>]*>", "", text or ""))
    return re.sub(r"\s+", " ", text).strip()


def parse_date(s):
    s = (s or "").strip()
    try:
        d = parsedate_to_datetime(s)
    except Exception:
        try:
            d = datetime.fromisoformat(s.replace("Z", "+00:00"))
        except Exception:
            return datetime.now(timezone.utc)
    if d.tzinfo is None:
        d = d.replace(tzinfo=timezone.utc)
    return d


def parse(xml_bytes, source):
    root = ET.fromstring(xml_bytes)
    items = []
    for el in root.iter():
        if local(el.tag) not in ("item", "entry"):
            continue
        title = link = date = ""
        for c in el:
            t = local(c.tag)
            if t == "title":
                title = clean("".join(c.itertext()))
            elif t == "link" and not link:
                href = c.get("href")
                if href:
                    if c.get("rel") in (None, "alternate"):
                        link = href
                else:
                    link = (c.text or "").strip()
            elif t in ("pubDate", "published", "updated", "date") and not date:
                date = c.text or ""
        if title and link:
            items.append({"title": title, "source": source, "link": link,
                          "time": parse_date(date).isoformat()})
    return items


def fetch(url):
    req = urllib.request.Request(url, headers={
        "User-Agent": "Mozilla/5.0 (compatible; DentalNewsBot/1.0)",
        "Accept": "application/rss+xml, application/atom+xml, application/xml, text/xml, */*"})
    with urllib.request.urlopen(req, timeout=30) as r:
        return r.read()


def main():
    old = []
    if OUT.exists():
        try:
            old = json.loads(OUT.read_text(encoding="utf-8")).get("items", [])
        except Exception:
            old = []

    fresh, ok = [], 0
    for name, url in SOURCES:
        try:
            got = parse(fetch(url), name)
            fresh += got
            ok += 1
            print(f"OK   {name}: {len(got)} items")
        except Exception as e:
            print(f"FAIL {name}: {e}", file=sys.stderr)

    if ok == 0:
        print("كل المصادر فشلت؛ لا تغيير على news.json", file=sys.stderr)
        return 0

    merged = {}
    for it in fresh + old:           # الجديد أولًا
        merged.setdefault(it["link"], it)
    items = sorted(merged.values(), key=lambda i: i["time"], reverse=True)[:MAX_ITEMS]

    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(json.dumps(
        {"updated": datetime.now(timezone.utc).isoformat(), "sources_ok": ok,
         "sources_total": len(SOURCES), "items": items},
        ensure_ascii=False, indent=1), encoding="utf-8")
    print(f"تم: {len(items)} خبرًا، {ok}/{len(SOURCES)} مصادر")
    return 0


if __name__ == "__main__":
    sys.exit(main())

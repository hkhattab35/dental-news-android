package com.dentalnews.app

/** عنصر خبر واحد: العنوان والمصدر والرابط الأصلي فقط. */
data class NewsItem(
    val title: String,
    val source: String,
    val link: String,
    val timeMs: Long
)

data class Source(val name: String, val feedUrl: String)

/**
 * ===== مصادر المحتوى =====
 * عدّل هذه القائمة لإضافة مصدر أو حذفه. كل مصدر يجب أن يوفّر خلاصة RSS أو Atom.
 * فشل أي مصدر لا يوقف بقية المصادر.
 *
 * ملاحظة: الروابط مأخوذة من أدلة RSS العامة ولم تُختبر من بيئة التطوير؛
 * تأكد من عملها عند أول تشغيل (يظهر عدد المصادر الناجحة في الشاشة).
 */
object Sources {
    val all = listOf(
        Source("Dental Tribune", "https://www.dental-tribune.com/news/feed"),
        Source("Dentistry.co.uk", "https://www.dentistry.co.uk/feed"),
        Source("Dentistry Today", "https://dentistrytoday.com/feed"),
        Source("DentalReach", "https://www.dentalreach.today/feed"),
        Source("ScienceDaily – Dentistry", "https://www.sciencedaily.com/rss/health_medicine/dentistry.xml")
    )
}

package com.pasiflonet.mobile

import android.os.Bundle
import android.view.Gravity
import android.widget.*
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import org.json.JSONObject
import kotlin.concurrent.thread

class CurrentIntelActivity : BaseActivity() {
    private lateinit var output: TextView
    private lateinit var keywords: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 28, 28, 28)
        }

        val title = TextView(this).apply {
            text = "מידע אקטואלי מהיר"
            textSize = 26f
            gravity = Gravity.CENTER
        }

        keywords = EditText(this).apply {
            hint = "מילות חיפוש: תאונה, שריפה, נתבג, אזעקה"
            minLines = 2
        }

        val btn = Button(this).apply {
            text = "חפש מידע אקטואלי"
        }

        output = TextView(this).apply {
            text = "הכנס מילים ולחץ חיפוש.\nהמידע נמשך ממקורות פתוחים, לא RSS ולא אתר מוטמע."
            textSize = 16f
            setPadding(0, 20, 0, 20)
        }

        btn.setOnClickListener {
            val q = keywords.text?.toString()?.trim().orEmpty()
            if (q.isBlank()) {
                Toast.makeText(this, "כתוב מילת חיפוש", Toast.LENGTH_SHORT).show()
            } else {
                searchIntel(q)
            }
        }

        root.addView(title)
        root.addView(keywords)
        root.addView(btn)
        root.addView(output)

        setContentView(ScrollView(this).apply { addView(root) })
    }

    private fun searchIntel(query: String) {
        output.text = "מחפש מידע אקטואלי..."
        thread {
            try {
                val encoded = URLEncoder.encode(query, "UTF-8")
                val url = URL("https://api.gdeltproject.org/api/v2/doc/doc?query=$encoded&mode=ArtList&format=json&maxrecords=15&sort=HybridRel")
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 12000
                conn.readTimeout = 12000
                conn.requestMethod = "GET"

                val text = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(text)
                val arr = json.optJSONArray("articles")

                val sb = StringBuilder()
                sb.append("תוצאות עבור: ").append(query).append("\n\n")

                if (arr == null || arr.length() == 0) {
                    sb.append("לא נמצאו תוצאות.")
                } else {
                    for (i in 0 until arr.length()) {
                        val item = arr.optJSONObject(i) ?: continue
                        val title = item.optString("title", "ללא כותרת")
                        val source = item.optString("sourceCountry", "")
                        val domain = item.optString("domain", "")
                        val urlItem = item.optString("url", "")
                        val date = item.optString("seendate", "")

                        sb.append("• ").append(title).append("\n")
                        if (domain.isNotBlank()) sb.append("מקור: ").append(domain).append("\n")
                        if (source.isNotBlank()) sb.append("מדינה: ").append(source).append("\n")
                        if (date.isNotBlank()) sb.append("עודכן: ").append(date).append("\n")
                        if (urlItem.isNotBlank()) sb.append(urlItem).append("\n")
                        sb.append("\n")
                    }
                }

                runOnUiThread { output.text = sb.toString() }
            } catch (e: Exception) {
                runOnUiThread { output.text = "שגיאה באיסוף מידע: ${e.message}" }
            }
        }
    }
}

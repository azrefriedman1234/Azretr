package com.pasiflonet.mobile

import android.os.Bundle
import android.view.Gravity
import android.widget.*
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import org.json.JSONObject
import kotlin.concurrent.thread

class VerifyIntelActivity : BaseActivity() {
    private lateinit var input: EditText
    private lateinit var output: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 28, 28, 28)
        }

        val title = TextView(this).apply {
            text = "אימות מידע"
            textSize = 26f
            gravity = Gravity.CENTER
        }

        input = EditText(this).apply {
            hint = "כתוב טענה לבדיקה: למשל שריפה באיילון"
            minLines = 3
        }

        val btn = Button(this).apply {
            text = "בדוק אמינות"
        }

        output = TextView(this).apply {
            text = "האימות בודק כמה מקורות שונים מדווחים על אותו נושא ונותן ציון בסיסי."
            textSize = 16f
            setPadding(0, 20, 0, 20)
        }

        btn.setOnClickListener {
            val q = input.text?.toString()?.trim().orEmpty()
            if (q.isBlank()) {
                Toast.makeText(this, "כתוב משהו לבדיקה", Toast.LENGTH_SHORT).show()
            } else {
                verify(q)
            }
        }

        root.addView(title)
        root.addView(input)
        root.addView(btn)
        root.addView(output)

        setContentView(ScrollView(this).apply { addView(root) })
    }

    private fun verify(query: String) {
        output.text = "בודק מקורות..."
        thread {
            try {
                val encoded = URLEncoder.encode(query, "UTF-8")
                val url = URL("https://api.gdeltproject.org/api/v2/doc/doc?query=$encoded&mode=ArtList&format=json&maxrecords=25&sort=HybridRel")
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 12000
                conn.readTimeout = 12000

                val text = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(text)
                val arr = json.optJSONArray("articles")

                val domains = linkedSetOf<String>()
                val titles = mutableListOf<String>()

                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        val item = arr.optJSONObject(i) ?: continue
                        val d = item.optString("domain", "")
                        val t = item.optString("title", "")
                        if (d.isNotBlank()) domains.add(d)
                        if (t.isNotBlank()) titles.add(t)
                    }
                }

                val sourceCount = domains.size
                var score = 0
                if (sourceCount >= 2) score += 25
                if (sourceCount >= 4) score += 25
                if (sourceCount >= 7) score += 25
                if (titles.size >= 5) score += 15
                if (query.length >= 6) score += 10
                if (score > 100) score = 100

                val level = when {
                    score >= 75 -> "גבוהה"
                    score >= 45 -> "בינונית"
                    score >= 20 -> "נמוכה"
                    else -> "לא מספיק מידע"
                }

                val sb = StringBuilder()
                sb.append("בדיקה עבור:\n").append(query).append("\n\n")
                sb.append("ציון אמינות בסיסי: ").append(score).append("/100\n")
                sb.append("רמת אימות: ").append(level).append("\n")
                sb.append("מספר מקורות שונים: ").append(sourceCount).append("\n\n")

                if (domains.isNotEmpty()) {
                    sb.append("מקורות שנמצאו:\n")
                    domains.take(10).forEach { sb.append("• ").append(it).append("\n") }
                    sb.append("\n")
                }

                if (titles.isNotEmpty()) {
                    sb.append("כותרות לדוגמה:\n")
                    titles.take(8).forEach { sb.append("• ").append(it).append("\n") }
                } else {
                    sb.append("לא נמצאו תוצאות מספיקות.")
                }

                sb.append("\n\nהערה: זה אימות ראשוני בלבד. מידע רגיש צריך להיבדק מול מקור רשמי.")

                runOnUiThread { output.text = sb.toString() }
            } catch (e: Exception) {
                runOnUiThread { output.text = "שגיאה באימות: ${e.message}" }
            }
        }
    }
}

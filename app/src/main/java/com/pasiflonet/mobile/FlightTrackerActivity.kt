package com.pasiflonet.mobile

import android.os.Bundle
import android.view.Gravity
import android.widget.*
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject
import kotlin.concurrent.thread
import kotlin.math.roundToInt

class FlightTrackerActivity : BaseActivity() {
    private lateinit var output: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 28, 28, 28)
        }

        val title = TextView(this).apply {
            text = "מעקב מטוסים סביב ישראל"
            textSize = 26f
            gravity = Gravity.CENTER
        }

        val btn = Button(this).apply {
            text = "רענן מטוסים"
        }

        output = TextView(this).apply {
            text = "לחץ רענון כדי לראות מטוסים באזור ישראל.\nהנתונים מגיעים מ־OpenSky API, בלי הטמעת אתר."
            textSize = 16f
            setPadding(0, 20, 0, 20)
        }

        btn.setOnClickListener { loadFlights() }

        root.addView(title)
        root.addView(btn)
        root.addView(output)

        setContentView(ScrollView(this).apply { addView(root) })
    }

    private fun loadFlights() {
        output.text = "טוען מטוסים באזור ישראל..."
        thread {
            try {
                // Bounding box roughly around Israel / nearby airspace
                val url = URL("https://opensky-network.org/api/states/all?lamin=29.0&lomin=33.0&lamax=34.0&lomax=36.8")
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 12000
                conn.readTimeout = 12000

                val text = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(text)
                val states = json.optJSONArray("states")

                val sb = StringBuilder()
                sb.append("מטוסים שנמצאו באזור ישראל:\n\n")

                if (states == null || states.length() == 0) {
                    sb.append("לא נמצאו מטוסים כרגע או שהשירות חסום זמנית.")
                } else {
                    for (i in 0 until states.length().coerceAtMost(40)) {
                        val a = states.optJSONArray(i) ?: continue
                        val icao = a.optString(0, "")
                        val callsign = a.optString(1, "").trim().ifBlank { "ללא אות קריאה" }
                        val country = a.optString(2, "")
                        val lon = a.optDouble(5, 0.0)
                        val lat = a.optDouble(6, 0.0)
                        val altitudeM = a.optDouble(7, -1.0)
                        val speedMs = a.optDouble(9, -1.0)
                        val heading = a.optDouble(10, -1.0)

                        val altitudeFt = if (altitudeM >= 0) (altitudeM * 3.28084).roundToInt() else -1
                        val speedKt = if (speedMs >= 0) (speedMs * 1.94384).roundToInt() else -1

                        sb.append("✈️ ").append(callsign).append("\n")
                        if (country.isNotBlank()) sb.append("מדינה: ").append(country).append("\n")
                        if (icao.isNotBlank()) sb.append("ICAO: ").append(icao).append("\n")
                        sb.append("מיקום: ").append("%.4f".format(lat)).append(", ").append("%.4f".format(lon)).append("\n")
                        if (altitudeFt >= 0) sb.append("גובה: ").append(altitudeFt).append(" רגל\n")
                        if (speedKt >= 0) sb.append("מהירות: ").append(speedKt).append(" קשר\n")
                        if (heading >= 0) sb.append("כיוון: ").append(heading.roundToInt()).append("°\n")
                        sb.append("\n")
                    }
                }

                runOnUiThread { output.text = sb.toString() }
            } catch (e: Exception) {
                runOnUiThread { output.text = "שגיאה בטעינת מטוסים: ${e.message}" }
            }
        }
    }
}

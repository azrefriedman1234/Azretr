package com.pasiflonet.mobile

import android.os.Bundle
import android.view.Gravity
import android.widget.*
import com.pasiflonet.mobile.utils.MilitaryFlightAlertHelper
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread
import kotlin.math.roundToInt

class MilitaryFlightMonitorActivity : BaseActivity() {
    private lateinit var areaSpinner: Spinner
    private lateinit var output: TextView
    private lateinit var autoCheck: CheckBox

    private val areas = listOf(
        Area("איראן", 24.0, 44.0, 40.5, 63.5),
        Area("תימן", 11.0, 41.0, 19.5, 55.5),
        Area("ישראל והאזור", 29.0, 33.0, 34.0, 36.8),
        Area("סוריה", 32.0, 35.5, 37.7, 42.5),
        Area("עיראק", 28.0, 38.0, 38.0, 49.0),
        Area("המפרץ", 22.0, 47.0, 31.0, 57.0)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 28, 28, 28)
        }

        val title = TextView(this).apply {
            text = "מעקב מטוסים צבאיים"
            textSize = 26f
            gravity = Gravity.CENTER
        }

        val note = TextView(this).apply {
            text = "המעקב משתמש בנתוני ADS-B פתוחים. לא כל מטוס צבאי מופיע, ולכן הזיהוי הוא חשד בלבד."
            textSize = 14f
            setPadding(0, 12, 0, 12)
        }

        areaSpinner = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@MilitaryFlightMonitorActivity,
                android.R.layout.simple_spinner_dropdown_item,
                areas.map { it.name }
            )
        }

        autoCheck = CheckBox(this).apply {
            text = "בדיקה אוטומטית כל 5 דקות בזמן שהמסך פתוח"
            isChecked = false
        }

        val btn = Button(this).apply {
            text = "בדוק עכשיו"
            setOnClickListener { checkNow(showToast = true) }
        }

        output = TextView(this).apply {
            text = "בחר אזור ולחץ בדוק עכשיו."
            textSize = 16f
            setPadding(0, 20, 0, 20)
        }

        root.addView(title)
        root.addView(note)
        root.addView(areaSpinner)
        root.addView(autoCheck)
        root.addView(btn)
        root.addView(output)

        setContentView(ScrollView(this).apply { addView(root) })
    }

    override fun onResume() {
        super.onResume()
        scheduleAuto()
    }

    private fun scheduleAuto() {
        output.postDelayed({
            if (!isFinishing && autoCheck.isChecked) {
                checkNow(showToast = false)
                scheduleAuto()
            }
        }, 5 * 60 * 1000L)
    }

    private fun checkNow(showToast: Boolean) {
        val area = areas[areaSpinner.selectedItemPosition]
        if (showToast) Toast.makeText(this, "בודק ${area.name}", Toast.LENGTH_SHORT).show()
        output.text = "בודק תנועה באזור ${area.name}..."

        thread {
            try {
                val url = URL("https://opensky-network.org/api/states/all?lamin=${area.minLat}&lomin=${area.minLon}&lamax=${area.maxLat}&lomax=${area.maxLon}")
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 15000
                conn.readTimeout = 15000
                conn.requestMethod = "GET"

                val response = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                val states = json.optJSONArray("states")

                val all = mutableListOf<Aircraft>()
                val suspicious = mutableListOf<Aircraft>()

                if (states != null) {
                    for (i in 0 until states.length()) {
                        val row = states.optJSONArray(i) ?: continue
                        val aircraft = Aircraft(
                            icao = row.optString(0, ""),
                            callsign = row.optString(1, "").trim(),
                            country = row.optString(2, ""),
                            lon = row.optDouble(5, 0.0),
                            lat = row.optDouble(6, 0.0),
                            altitudeM = row.optDouble(7, -1.0),
                            speedMs = row.optDouble(9, -1.0),
                            heading = row.optDouble(10, -1.0)
                        )
                        all.add(aircraft)
                        if (isLikelyMilitary(aircraft)) suspicious.add(aircraft)
                    }
                }

                val sb = StringBuilder()
                sb.append("אזור: ${area.name}\n")
                sb.append("סה״כ מטוסים שנקלטו: ${all.size}\n")
                sb.append("חשד לצבאי/מבצעי: ${suspicious.size}\n\n")

                if (suspicious.isEmpty()) {
                    sb.append("לא נמצאה כרגע תנועה חשודה לפי הסינון.\n")
                    sb.append("חשוב: מטוסים צבאיים רבים לא משדרים ADS-B ולכן לא יופיעו.")
                } else {
                    sb.append("התראות/תוצאות:\n\n")
                    suspicious.take(25).forEachIndexed { index, a ->
                        sb.append("✈️ ${a.displayName()}\n")
                        sb.append("מדינה: ${a.country}\n")
                        sb.append("ICAO: ${a.icao}\n")
                        sb.append("מיקום: ${"%.4f".format(a.lat)}, ${"%.4f".format(a.lon)}\n")
                        if (a.altitudeFeet() >= 0) sb.append("גובה: ${a.altitudeFeet()} רגל\n")
                        if (a.speedKnots() >= 0) sb.append("מהירות: ${a.speedKnots()} קשר\n")
                        if (a.heading >= 0) sb.append("כיוון: ${a.heading.roundToInt()}°\n")
                        sb.append("סיבה: ${reason(a)}\n\n")

                        MilitaryFlightAlertHelper.notify(
                            this,
                            "Azretr: תנועה חשודה ליד ${area.name}",
                            "${a.displayName()} | ${reason(a)}",
                            (a.icao + area.name + index).hashCode()
                        )
                    }
                }

                runOnUiThread { output.text = sb.toString() }
            } catch (e: Exception) {
                runOnUiThread {
                    output.text = "שגיאה בבדיקת מטוסים: ${e.message}\n\nייתכן שהשירות עמוס או חסום זמנית."
                }
            }
        }
    }

    private fun isLikelyMilitary(a: Aircraft): Boolean {
        val c = a.callsign.uppercase()
        val country = a.country.uppercase()

        val militaryPrefixes = listOf(
            "RCH", "REACH", "QID", "HKY", "NATO", "ASY", "IAM", "KAF",
            "TUAF", "AF", "VV", "CNV", "FORTE", "JAKE", "DUKE", "LAGR",
            "SHUCK", "SHELL", "TEX2", "TEXAS", "GAF", "BAF", "CAF", "RAF"
        )

        if (militaryPrefixes.any { c.startsWith(it) || c.contains(it) }) return true

        val militaryCountries = listOf(
            "UNITED STATES", "ISRAEL", "TURKEY", "UNITED KINGDOM",
            "FRANCE", "GERMANY", "ITALY", "SAUDI ARABIA",
            "UNITED ARAB EMIRATES", "QATAR"
        )

        if (militaryCountries.any { country.contains(it) } && a.altitudeM > 9000) return true

        if (c.isBlank() && a.altitudeM > 8000 && a.speedMs > 180) return true

        return false
    }

    private fun reason(a: Aircraft): String {
        val c = a.callsign.uppercase()
        return when {
            listOf("RCH", "REACH").any { c.startsWith(it) } -> "זוהה callsign נפוץ של תובלה צבאית אמריקאית"
            c.contains("NATO") -> "callsign מכיל NATO"
            c.startsWith("QID") || c.startsWith("HKY") -> "callsign נפוץ של תדלוק/בקרה"
            c.startsWith("ASY") -> "callsign נפוץ של חיל האוויר האוסטרלי"
            c.startsWith("IAM") -> "callsign נפוץ של חיל האוויר האיטלקי"
            c.startsWith("KAF") -> "callsign נפוץ של חיל האוויר הכוויתי"
            c.startsWith("TUAF") -> "callsign נפוץ של חיל האוויר הטורקי"
            a.callsign.isBlank() -> "ללא callsign ובפרופיל טיסה חריג"
            else -> "התאמה כללית לסינון צבאי/מבצעי"
        }
    }

    data class Area(
        val name: String,
        val minLat: Double,
        val minLon: Double,
        val maxLat: Double,
        val maxLon: Double
    )

    data class Aircraft(
        val icao: String,
        val callsign: String,
        val country: String,
        val lon: Double,
        val lat: Double,
        val altitudeM: Double,
        val speedMs: Double,
        val heading: Double
    ) {
        fun displayName(): String = callsign.ifBlank { "ללא אות קריאה" }
        fun altitudeFeet(): Int = if (altitudeM >= 0) (altitudeM * 3.28084).roundToInt() else -1
        fun speedKnots(): Int = if (speedMs >= 0) (speedMs * 1.94384).roundToInt() else -1
    }
}

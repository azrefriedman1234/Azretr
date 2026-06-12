package com.pasiflonet.mobile.utils

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

class CyberMiddleEastMapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(2, 8, 6) }
    private val grid = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(0, 255, 136); alpha = 45; strokeWidth = 1f }
    private val green = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(0, 255, 136); strokeWidth = 3f; style = Paint.Style.STROKE }
    private val blue = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(0, 191, 255); strokeWidth = 2.5f; style = Paint.Style.STROKE }
    private val red = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(255, 23, 68); style = Paint.Style.FILL }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(215, 255, 233); textSize = 24f; typeface = Typeface.DEFAULT_BOLD }
    private val small = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(114, 191, 160); textSize = 20f }
    private val countPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 20f; typeface = Typeface.DEFAULT_BOLD }

    private var searchText: String = ""

    fun setSearchText(value: String) {
        searchText = value.trim()
        invalidate()
    }

    fun refreshCounters() {
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        canvas.drawRect(0f, 0f, w, h, bg)

        for (i in 1..8) canvas.drawLine(w * i / 9f, 0f, w * i / 9f, h, grid)
        for (i in 1..5) canvas.drawLine(0f, h * i / 6f, w, h * i / 6f, grid)

        drawRegion(canvas, floatArrayOf(.45f,.20f, .56f,.25f, .60f,.43f, .48f,.60f, .39f,.43f), green, w, h)
        drawRegion(canvas, floatArrayOf(.58f,.28f, .86f,.31f, .92f,.54f, .78f,.66f, .60f,.51f), green, w, h)
        drawRegion(canvas, floatArrayOf(.34f,.45f, .63f,.40f, .76f,.62f, .58f,.84f, .34f,.72f), blue, w, h)

        val counts = CyberAlertCounter.getCounts(context)

        marker(canvas, "ישראל", "israel", counts["israel"] ?: 0, w*.39f, h*.38f)
        marker(canvas, "סוריה", "syria", counts["syria"] ?: 0, w*.43f, h*.27f)
        marker(canvas, "עיראק", "iraq", counts["iraq"] ?: 0, w*.58f, h*.36f)
        marker(canvas, "איראן", "iran", counts["iran"] ?: 0, w*.73f, h*.43f)
        marker(canvas, "תימן", "yemen", counts["yemen"] ?: 0, w*.55f, h*.78f)
        marker(canvas, "המפרץ", "gulf", counts["gulf"] ?: 0, w*.66f, h*.60f)

        canvas.drawText("מפת מזרח תיכון - ספירה עד 00:00", 18f, 30f, text)

        if (searchText.isNotBlank()) {
            canvas.drawText("חיפוש: $searchText", 18f, h - 18f, text)
        }
    }

    private fun drawRegion(canvas: Canvas, pts: FloatArray, paint: Paint, w: Float, h: Float) {
        if (pts.size < 4) return
        val p = Path()
        p.moveTo(pts[0] * w, pts[1] * h)
        var i = 2
        while (i < pts.size) {
            p.lineTo(pts[i] * w, pts[i + 1] * h)
            i += 2
        }
        p.close()
        canvas.drawPath(p, paint)
    }

    private fun marker(canvas: Canvas, label: String, key: String, count: Int, x: Float, y: Float) {
        val highlight = searchText.isNotBlank() && (
            label.contains(searchText, true) ||
            key.contains(searchText, true)
        )

        val radius = if (highlight) 34f else 23f
        val circle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (count > 0 || highlight) Color.rgb(255, 23, 68) else Color.rgb(0, 255, 136)
            style = Paint.Style.STROKE
            strokeWidth = if (highlight) 6f else 3f
        }

        canvas.drawCircle(x, y, radius, circle)
        canvas.drawCircle(x, y, 5f, red)
        canvas.drawText(label, x + 12f, y - 10f, small)
        canvas.drawText(count.toString(), x - 7f, y + 7f, countPaint)
    }
}

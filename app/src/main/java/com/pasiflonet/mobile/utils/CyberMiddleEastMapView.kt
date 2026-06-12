package com.pasiflonet.mobile.utils

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.max
import kotlin.math.min

class CyberMiddleEastMapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(2, 8, 6) }
    private val grid = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(0, 255, 136)
        alpha = 55
        strokeWidth = 1f
    }
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(0, 255, 136)
        strokeWidth = 3f
        style = Paint.Style.STROKE
    }
    private val blue = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(0, 191, 255)
        strokeWidth = 2.5f
        style = Paint.Style.STROKE
    }
    private val red = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(255, 23, 68)
        style = Paint.Style.FILL
    }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(215, 255, 233)
        textSize = 26f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }
    private val small = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(114, 191, 160)
        textSize = 22f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    }

    private var highlight: String = ""

    fun setSearchText(value: String) {
        highlight = value.trim()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bg)

        val w = width.toFloat()
        val h = height.toFloat()

        for (i in 1..8) {
            val x = w * i / 9f
            canvas.drawLine(x, 0f, x, h, grid)
        }
        for (i in 1..5) {
            val y = h * i / 6f
            canvas.drawLine(0f, y, w, y, grid)
        }

        val p = Path()
        p.moveTo(w * .48f, h * .12f)
        p.lineTo(w * .56f, h * .22f)
        p.lineTo(w * .61f, h * .36f)
        p.lineTo(w * .57f, h * .55f)
        p.lineTo(w * .50f, h * .71f)
        p.lineTo(w * .45f, h * .62f)
        p.lineTo(w * .40f, h * .44f)
        p.lineTo(w * .43f, h * .27f)
        p.close()
        canvas.drawPath(p, line)

        val arabia = Path()
        arabia.moveTo(w * .38f, h * .42f)
        arabia.lineTo(w * .63f, h * .38f)
        arabia.lineTo(w * .76f, h * .62f)
        arabia.lineTo(w * .58f, h * .84f)
        arabia.lineTo(w * .34f, h * .72f)
        arabia.close()
        canvas.drawPath(arabia, blue)

        val iran = Path()
        iran.moveTo(w * .63f, h * .27f)
        iran.lineTo(w * .86f, h * .30f)
        iran.lineTo(w * .92f, h * .55f)
        iran.lineTo(w * .77f, h * .68f)
        iran.lineTo(w * .61f, h * .50f)
        iran.close()
        canvas.drawPath(iran, line)

        label(canvas, "ישראל", w*.39f, h*.37f)
        label(canvas, "סוריה", w*.43f, h*.26f)
        label(canvas, "עיראק", w*.58f, h*.35f)
        label(canvas, "איראן", w*.73f, h*.42f)
        label(canvas, "תימן", w*.55f, h*.78f)
        label(canvas, "המפרץ", w*.66f, h*.60f)

        point(canvas, w*.73f, h*.42f, "IR")
        point(canvas, w*.55f, h*.78f, "YE")
        point(canvas, w*.39f, h*.37f, "IL")

        canvas.drawText("מפת מזרח תיכון - תצוגה סכמטית", 20f, 34f, text)

        if (highlight.isNotBlank()) {
            val clean = highlight.lowercase()
            val hx = when {
                clean.contains("איראן") || clean.contains("iran") -> w*.73f
                clean.contains("תימן") || clean.contains("yemen") -> w*.55f
                clean.contains("ישראל") || clean.contains("israel") -> w*.39f
                clean.contains("עיראק") || clean.contains("iraq") -> w*.58f
                clean.contains("סוריה") || clean.contains("syria") -> w*.43f
                clean.contains("מפרץ") || clean.contains("gulf") -> w*.66f
                else -> w*.5f
            }
            val hy = when {
                clean.contains("איראן") || clean.contains("iran") -> h*.42f
                clean.contains("תימן") || clean.contains("yemen") -> h*.78f
                clean.contains("ישראל") || clean.contains("israel") -> h*.37f
                clean.contains("עיראק") || clean.contains("iraq") -> h*.35f
                clean.contains("סוריה") || clean.contains("syria") -> h*.26f
                clean.contains("מפרץ") || clean.contains("gulf") -> h*.60f
                else -> h*.5f
            }
            val pulse = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(255, 23, 68)
                strokeWidth = 5f
                style = Paint.Style.STROKE
            }
            canvas.drawCircle(hx, hy, min(w, h) * .09f, pulse)
            canvas.drawText("חיפוש: $highlight", 20f, h - 24f, text)
        }
    }

    private fun label(canvas: Canvas, s: String, x: Float, y: Float) {
        canvas.drawText(s, x, y, small)
    }

    private fun point(canvas: Canvas, x: Float, y: Float, s: String) {
        canvas.drawCircle(x, y, 7f, red)
        canvas.drawText(s, x + 10f, y - 8f, small)
    }
}

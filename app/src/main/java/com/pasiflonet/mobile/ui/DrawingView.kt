package com.pasiflonet.mobile.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.pasiflonet.mobile.utils.BlurRect
import kotlin.math.max
import kotlin.math.min

class DrawingView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private val paint = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 5f
        pathEffect = DashPathEffect(floatArrayOf(10f, 20f), 0f)
    }

    private val blurFill = Paint().apply {
        color = 0x55000000
        style = Paint.Style.FILL
    }

    var isBlurMode = false

    private var startX = 0f
    private var startY = 0f
    private var currentX = 0f
    private var currentY = 0f
    private var isDrawing = false

    private var validBounds: RectF? = null

    val rects = mutableListOf<BlurRect>()

    fun setValidBounds(bounds: RectF) {
        validBounds = RectF(bounds)
        invalidate()
    }

    private fun getActiveBounds(): RectF {
        val b = validBounds
        return if (b != null && b.width() > 0f && b.height() > 0f) {
            RectF(b)
        } else {
            RectF(0f, 0f, width.toFloat(), height.toFloat())
        }
    }

    private fun clampToBounds(x: Float, y: Float, bounds: RectF): Pair<Float, Float> {
        return Pair(
            x.coerceIn(bounds.left, bounds.right),
            y.coerceIn(bounds.top, bounds.bottom)
        )
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isBlurMode) return false

        val bounds = getActiveBounds()
        if (bounds.width() <= 0f || bounds.height() <= 0f) return false

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val (cx, cy) = clampToBounds(event.x, event.y, bounds)
                startX = cx
                startY = cy
                currentX = cx
                currentY = cy
                isDrawing = true
                invalidate()
            }

            MotionEvent.ACTION_MOVE -> {
                val (cx, cy) = clampToBounds(event.x, event.y, bounds)
                currentX = cx
                currentY = cy
                invalidate()
            }

            MotionEvent.ACTION_UP -> {
                val (cx, cy) = clampToBounds(event.x, event.y, bounds)
                currentX = cx
                currentY = cy
                isDrawing = false

                val left = min(startX, currentX)
                val top = min(startY, currentY)
                val right = max(startX, currentX)
                val bottom = max(startY, currentY)

                if ((right - left) > 4f && (bottom - top) > 4f) {
                    rects.add(
                        BlurRect(
                            (left - bounds.left) / bounds.width(),
                            (top - bounds.top) / bounds.height(),
                            (right - bounds.left) / bounds.width(),
                            (bottom - bounds.top) / bounds.height()
                        )
                    )
                }

                invalidate()
            }
        }

        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val bounds = getActiveBounds()

        rects.forEach { r ->
            val left = bounds.left + (r.left * bounds.width())
            val top = bounds.top + (r.top * bounds.height())
            val right = bounds.left + (r.right * bounds.width())
            val bottom = bounds.top + (r.bottom * bounds.height())

            canvas.drawRect(left, top, right, bottom, blurFill)
            canvas.drawRect(left, top, right, bottom, paint)
        }

        if (isDrawing) {
            canvas.drawRect(
                min(startX, currentX),
                min(startY, currentY),
                max(startX, currentX),
                max(startY, currentY),
                paint
            )
        }
    }
}

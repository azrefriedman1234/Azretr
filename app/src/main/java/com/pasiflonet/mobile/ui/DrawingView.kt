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

    private val strokePaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 6f
        pathEffect = DashPathEffect(floatArrayOf(14f, 16f), 0f)
        isAntiAlias = true
    }

    private val fillPaint = Paint().apply {
        color = 0x55000000
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    var isBlurMode = false

    private var validBounds: RectF? = null
    val rects = mutableListOf<BlurRect>()

    private var startX = 0f
    private var startY = 0f
    private var currentX = 0f
    private var currentY = 0f

    private var activeRectIndex = -1
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var isCreating = false

    fun setValidBounds(bounds: RectF) {
        validBounds = RectF(bounds)
        invalidate()
    }

    private fun activeBounds(): RectF {
        val b = validBounds
        return if (b != null && b.width() > 0f && b.height() > 0f) {
            RectF(b)
        } else {
            RectF(0f, 0f, width.toFloat(), height.toFloat())
        }
    }

    private fun clampX(x: Float, b: RectF): Float = x.coerceIn(b.left, b.right)
    private fun clampY(y: Float, b: RectF): Float = y.coerceIn(b.top, b.bottom)

    private fun toPixelRect(r: BlurRect, b: RectF): RectF {
        return RectF(
            b.left + r.left * b.width(),
            b.top + r.top * b.height(),
            b.left + r.right * b.width(),
            b.top + r.bottom * b.height()
        )
    }

    private fun findRectAt(x: Float, y: Float, b: RectF): Int {
        val pad = 34f
        for (i in rects.indices.reversed()) {
            val pr = toPixelRect(rects[i], b)
            pr.inset(-pad, -pad)
            if (pr.contains(x, y)) return i
        }
        return -1
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isBlurMode) return false

        parent?.requestDisallowInterceptTouchEvent(true)

        val b = activeBounds()
        if (b.width() <= 0f || b.height() <= 0f) return false

        val x = clampX(event.x, b)
        val y = clampY(event.y, b)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                activeRectIndex = findRectAt(x, y, b)
                lastTouchX = x
                lastTouchY = y

                if (activeRectIndex >= 0) {
                    isCreating = false
                } else {
                    isCreating = true
                    startX = x
                    startY = y
                    currentX = x
                    currentY = y
                }

                invalidate()
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (activeRectIndex >= 0) {
                    val dx = x - lastTouchX
                    val dy = y - lastTouchY
                    lastTouchX = x
                    lastTouchY = y

                    val bw = b.width()
                    val bh = b.height()
                    if (bw > 0f && bh > 0f) {
                        val r = rects[activeRectIndex]

                        var left = r.left + (dx / bw)
                        var right = r.right + (dx / bw)
                        var top = r.top + (dy / bh)
                        var bottom = r.bottom + (dy / bh)

                        val w = right - left
                        val h = bottom - top

                        if (left < 0f) {
                            left = 0f
                            right = w
                        }
                        if (right > 1f) {
                            right = 1f
                            left = 1f - w
                        }
                        if (top < 0f) {
                            top = 0f
                            bottom = h
                        }
                        if (bottom > 1f) {
                            bottom = 1f
                            top = 1f - h
                        }

                        rects[activeRectIndex] = BlurRect(left, top, right, bottom)
                    }
                } else if (isCreating) {
                    currentX = x
                    currentY = y
                }

                invalidate()
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isCreating) {
                    val left = min(startX, currentX)
                    val top = min(startY, currentY)
                    val right = max(startX, currentX)
                    val bottom = max(startY, currentY)

                    if ((right - left) > 18f && (bottom - top) > 18f) {
                        rects.add(
                            BlurRect(
                                ((left - b.left) / b.width()).coerceIn(0f, 1f),
                                ((top - b.top) / b.height()).coerceIn(0f, 1f),
                                ((right - b.left) / b.width()).coerceIn(0f, 1f),
                                ((bottom - b.top) / b.height()).coerceIn(0f, 1f)
                            )
                        )
                    }
                }

                activeRectIndex = -1
                isCreating = false
                invalidate()
                return true
            }
        }

        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val b = activeBounds()

        rects.forEach { r ->
            val pr = toPixelRect(r, b)
            canvas.drawRect(pr, fillPaint)
            canvas.drawRect(pr, strokePaint)
        }

        if (isCreating) {
            canvas.drawRect(
                min(startX, currentX),
                min(startY, currentY),
                max(startX, currentX),
                max(startY, currentY),
                strokePaint
            )
        }
    }
}

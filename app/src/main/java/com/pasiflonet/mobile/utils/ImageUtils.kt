package com.pasiflonet.mobile.utils

import android.content.Context
import android.graphics.*
import android.net.Uri
import android.util.Log
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object ImageUtils {

    /**
     * Blur אמיתי (לא פיקסלים) + Logo overlay.
     * blur strength כבר מוגבר ~15% (radius=21f).
     */
    fun processImage(
        context: Context,
        inputPath: String,
        outputPath: String,
        blurRects: List<BlurRect>,
        logoUri: Uri?,
        relX: Float,
        relY: Float,
        relW: Float
    ): Boolean {
        return try {
            // decode with downsample to avoid OOM
            val opts = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(inputPath, opts)

            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = calculateInSampleSize(opts, 2500, 2500)
                inJustDecodeBounds = false
                inMutable = true
            }

            val base = BitmapFactory.decodeFile(inputPath, decodeOpts) ?: return false
            val canvas = Canvas(base)

            val w = base.width.toFloat()
            val h = base.height.toFloat()

            // --- BLUR (stronger by ~15%) ---
            if (blurRects.isNotEmpty()) {
                val radius = 21f
                val blurredFull = createBlurredBitmap(base, radius)

                val src = Rect()
                val dst = Rect()

                for (r in blurRects) {
                    val left = (min(r.left, r.right) * w).roundToInt()
                    val right = (max(r.left, r.right) * w).roundToInt()
                    val top = (min(r.top, r.bottom) * h).roundToInt()
                    val bottom = (max(r.top, r.bottom) * h).roundToInt()

                    val l = left.coerceIn(0, base.width)
                    val rr = right.coerceIn(0, base.width)
                    val t = top.coerceIn(0, base.height)
                    val bb = bottom.coerceIn(0, base.height)

                    if (rr > l && bb > t) {
                        src.set(l, t, rr, bb)
                        dst.set(l, t, rr, bb)
                        canvas.drawBitmap(blurredFull, src, dst, null)
                    }
                }

                blurredFull.recycle()
            }

            // --- LOGO overlay ---
            if (logoUri != null) {
                try {
                    val ins = context.contentResolver.openInputStream(logoUri)
                    val logo = BitmapFactory.decodeStream(ins)
                    ins?.close()

                    if (logo != null) {
                        val safeRelW = relW.coerceIn(0.05f, 0.8f)
                        val targetW = (w * safeRelW).roundToInt().coerceAtLeast(2)
                        val ratio = if (logo.width > 0) (logo.height.toFloat() / logo.width.toFloat()) else 1f
                        val targetH = (targetW * ratio).roundToInt().coerceAtLeast(2)

                        val scaled = Bitmap.createScaledBitmap(logo, targetW, targetH, true)

                        // clamp position inside image
                        val x = (w * relX).coerceIn(0f, w - scaled.width)
                        val y = (h * relY).coerceIn(0f, h - scaled.height)

                        canvas.drawBitmap(scaled, x, y, null)

                        if (scaled != logo) scaled.recycle()
                        logo.recycle()
                    }
                } catch (e: Exception) {
                    Log.e("ImageUtils", "Logo error", e)
                }
            }

            FileOutputStream(outputPath).use { out ->
                base.compress(Bitmap.CompressFormat.JPEG, 90, out)
                out.flush()
            }

            base.recycle()
            true
        } catch (t: Throwable) {
            Log.e("ImageUtils", "processImage failed", t)
            false
        }
    }

    private fun createBlurredBitmap(src: Bitmap, radius: Float): Bitmap {
        // Maximum compatibility: StackBlur only (no RenderEffect / RenderScript)
        return stackBlur(src.copy(Bitmap.Config.ARGB_8888, true), radius.toInt().coerceAtLeast(1))
    }

    // Fast StackBlur
    private fun stackBlur(sentBitmap: Bitmap, radius: Int): Bitmap {
        if (radius < 1) return sentBitmap

        val bitmap = sentBitmap
        val w = bitmap.width
        val h = bitmap.height
        val pix = IntArray(w * h)
        bitmap.getPixels(pix, 0, w, 0, 0, w, h)

        val wm = w - 1
        val hm = h - 1
        val wh = w * h
        val div = radius + radius + 1

        val r = IntArray(wh)
        val g = IntArray(wh)
        val b = IntArray(wh)

        var rsum: Int
        var gsum: Int
        var bsum: Int
        var x: Int
        var y: Int
        var i: Int
        var p: Int
        var yp: Int
        var yi: Int

        val vmin = IntArray(max(w, h))

        var divsum = (div + 1) shr 1
        divsum *= divsum
        val dv = IntArray(256 * divsum)
        i = 0
        while (i < 256 * divsum) {
            dv[i] = i / divsum
            i++
        }

        yi = 0
        yp = 0

        val stack = Array(div) { IntArray(3) }
        var stackpointer: Int
        var stackstart: Int
        var sir: IntArray
        var rbs: Int
        val r1 = radius + 1
        var routsum: Int
        var goutsum: Int
        var boutsum: Int
        var rinsum: Int
        var ginsum: Int
        var binsum: Int

        y = 0
        while (y < h) {
            rinsum = 0; ginsum = 0; binsum = 0
            routsum = 0; goutsum = 0; boutsum = 0
            rsum = 0; gsum = 0; bsum = 0

            i = -radius
            while (i <= radius) {
                p = pix[yi + min(wm, max(i, 0))]
                sir = stack[i + radius]
                sir[0] = (p and 0xff0000) shr 16
                sir[1] = (p and 0x00ff00) shr 8
                sir[2] = (p and 0x0000ff)
                rbs = r1 - kotlin.math.abs(i)
                rsum += sir[0] * rbs
                gsum += sir[1] * rbs
                bsum += sir[2] * rbs
                if (i > 0) {
                    rinsum += sir[0]; ginsum += sir[1]; binsum += sir[2]
                } else {
                    routsum += sir[0]; goutsum += sir[1]; boutsum += sir[2]
                }
                i++
            }
            stackpointer = radius

            x = 0
            while (x < w) {
                r[yi] = dv[rsum]
                g[yi] = dv[gsum]
                b[yi] = dv[bsum]

                rsum -= routsum
                gsum -= goutsum
                bsum -= boutsum

                stackstart = stackpointer - radius + div
                sir = stack[stackstart % div]

                routsum -= sir[0]
                goutsum -= sir[1]
                boutsum -= sir[2]

                if (y == 0) vmin[x] = min(x + radius + 1, wm)
                p = pix[yp + vmin[x]]

                sir[0] = (p and 0xff0000) shr 16
                sir[1] = (p and 0x00ff00) shr 8
                sir[2] = (p and 0x0000ff)

                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]

                rsum += rinsum
                gsum += ginsum
                bsum += binsum

                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer]

                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]

                rinsum -= sir[0]
                ginsum -= sir[1]
                binsum -= sir[2]

                yi++
                x++
            }
            yp += w
            y++
        }

        x = 0
        while (x < w) {
            rinsum = 0; ginsum = 0; binsum = 0
            routsum = 0; goutsum = 0; boutsum = 0
            rsum = 0; gsum = 0; bsum = 0
            yp = -radius * w
            i = -radius
            while (i <= radius) {
                yi = max(0, yp) + x

                sir = stack[i + radius]
                sir[0] = r[yi]
                sir[1] = g[yi]
                sir[2] = b[yi]

                rbs = r1 - kotlin.math.abs(i)
                rsum += r[yi] * rbs
                gsum += g[yi] * rbs
                bsum += b[yi] * rbs

                if (i > 0) {
                    rinsum += sir[0]; ginsum += sir[1]; binsum += sir[2]
                } else {
                    routsum += sir[0]; goutsum += sir[1]; boutsum += sir[2]
                }

                if (i < hm) yp += w
                i++
            }
            yi = x
            stackpointer = radius
            y = 0
            while (y < h) {
                pix[yi] = (0xff000000.toInt()
                        or (dv[rsum] shl 16)
                        or (dv[gsum] shl 8)
                        or dv[bsum])

                rsum -= routsum
                gsum -= goutsum
                bsum -= boutsum

                stackstart = stackpointer - radius + div
                sir = stack[stackstart % div]

                routsum -= sir[0]
                goutsum -= sir[1]
                boutsum -= sir[2]

                if (x == 0) vmin[y] = min(y + r1, hm) * w
                p = x + vmin[y]

                sir[0] = r[p]
                sir[1] = g[p]
                sir[2] = b[p]

                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]

                rsum += rinsum
                gsum += ginsum
                bsum += binsum

                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer]

                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]

                rinsum -= sir[0]
                ginsum -= sir[1]
                binsum -= sir[2]

                yi += w
                y++
            }
            x++
        }

        bitmap.setPixels(pix, 0, w, 0, 0, w, h)
        return bitmap
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            var halfHeight = height / 2
            var halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}

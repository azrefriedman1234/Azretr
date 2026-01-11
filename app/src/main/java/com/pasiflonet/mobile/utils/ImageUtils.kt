package com.pasiflonet.mobile.utils

import android.content.ContentResolver
import android.content.Context
import android.graphics.*
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min

object ImageUtils {

    /**
     * מעבד תמונה:
     * - BlurRects (טשטוש “אמיתי” / soft blur, לא פיקסלים)
     * - לוגו לפי יחס X/Y + יחס רוחב (logoRelW)
     * - שומר ל-outPath
     *
     * מחזיר true אם נוצר קובץ תקין.
     */
    fun processImage(
        ctx: Context,
        inPath: String,
        outPath: String,
        rects: List<BlurRect>,
        logoUri: Uri?,
        logoRelX: Float,
        logoRelY: Float,
        logoRelW: Float
    ): Boolean {
        return try {
            val src = BitmapFactory.decodeFile(inPath) ?: return false
            val bmp = src.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(bmp)

            // Blur radius בסיסי + 15% יותר חזק (כמו שביקשת)
            val baseRadius = 22
            val radius = max(1, (baseRadius * 1.15f).toInt())

            // טשטוש לכל מלבן
            rects.forEach { r ->
                val left = (r.left * bmp.width).toInt()
                val top = (r.top * bmp.height).toInt()
                val right = (r.right * bmp.width).toInt()
                val bottom = (r.bottom * bmp.height).toInt()

                val l = max(0, min(left, bmp.width - 1))
                val t = max(0, min(top, bmp.height - 1))
                val rr = max(l + 1, min(right, bmp.width))
                val bb = max(t + 1, min(bottom, bmp.height))

                val w = rr - l
                val h = bb - t
                if (w < 2 || h < 2) return@forEach

                val region = Bitmap.createBitmap(bmp, l, t, w, h)
                val blurred = stackBlur(region, radius)
                canvas.drawBitmap(blurred, l.toFloat(), t.toFloat(), null)

                region.recycle()
                blurred.recycle()
            }

            // לוגו
            if (logoUri != null && logoRelW > 0f) {
                val logo = loadBitmapFromUri(ctx, logoUri)
                if (logo != null) {
                    val targetW = max(1, (bmp.width * logoRelW).toInt())
                    val scale = targetW.toFloat() / max(1, logo.width).toFloat()
                    val targetH = max(1, (logo.height * scale).toInt())
                    val scaled = Bitmap.createScaledBitmap(logo, targetW, targetH, true)

                    val x = ((bmp.width - scaled.width) * logoRelX).toInt().toFloat()
                    val y = ((bmp.height - scaled.height) * logoRelY).toInt().toFloat()

                    canvas.drawBitmap(scaled, x, y, null)

                    scaled.recycle()
                    if (logo != scaled) logo.recycle()
                }
            }

            File(outPath).parentFile?.mkdirs()
            FileOutputStream(outPath).use { fos ->
                // jpg איכות גבוהה
                bmp.compress(Bitmap.CompressFormat.JPEG, 92, fos)
            }
            bmp.recycle()
            val f = File(outPath)
            f.exists() && f.length() > 0
        } catch (_: Exception) {
            false
        }
    }

    private fun loadBitmapFromUri(ctx: Context, uri: Uri): Bitmap? {
        return try {
            val cr: ContentResolver = ctx.contentResolver
            cr.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input)
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * StackBlur פשוט ומהיר (blur רך, לא פיקסלים)
     */
    private fun stackBlur(sentBitmap: Bitmap, radius: Int): Bitmap {
        if (radius < 1) return sentBitmap

        val bitmap = sentBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val w = bitmap.width
        val h = bitmap.height
        val pix = IntArray(w * h)
        bitmap.getPixels(pix, 0, w, 0, 0, w, h)

        val div = radius + radius + 1
        val r = IntArray(w * h)
        val g = IntArray(w * h)
        val b = IntArray(w * h)
        val vmin = IntArray(max(w, h))

        var rsum: Int
        var gsum: Int
        var bsum: Int
        var x: Int
        var y: Int
        var i: Int
        var p: Int
        var yp: Int
        var yi: Int
        val dv = IntArray(256 * div * div)
        i = 0
        while (i < 256 * div * div) {
            dv[i] = (i / (div * div))
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
            rinsum = 0
            ginsum = 0
            binsum = 0
            routsum = 0
            goutsum = 0
            boutsum = 0
            rsum = 0
            gsum = 0
            bsum = 0

            i = -radius
            while (i <= radius) {
                p = pix[yi + min(w - 1, max(i, 0))]
                sir = stack[i + radius]
                sir[0] = (p and 0xff0000) shr 16
                sir[1] = (p and 0x00ff00) shr 8
                sir[2] = (p and 0x0000ff)
                rbs = r1 - kotlin.math.abs(i)
                rsum += sir[0] * rbs
                gsum += sir[1] * rbs
                bsum += sir[2] * rbs
                if (i > 0) {
                    rinsum += sir[0]
                    ginsum += sir[1]
                    binsum += sir[2]
                } else {
                    routsum += sir[0]
                    goutsum += sir[1]
                    boutsum += sir[2]
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

                if (y == 0) {
                    vmin[x] = min(x + radius + 1, w - 1)
                }
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
            rinsum = 0
            ginsum = 0
            binsum = 0
            routsum = 0
            goutsum = 0
            boutsum = 0
            rsum = 0
            gsum = 0
            bsum = 0
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
                    rinsum += sir[0]
                    ginsum += sir[1]
                    binsum += sir[2]
                } else {
                    routsum += sir[0]
                    goutsum += sir[1]
                    boutsum += sir[2]
                }
                if (i < h - 1) {
                    yp += w
                }
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

                if (x == 0) {
                    vmin[y] = min(y + r1, h - 1) * w
                }
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
}

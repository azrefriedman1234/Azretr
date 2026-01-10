package com.pasiflonet.mobile.utils

import android.content.Context
import android.graphics.*
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object ImageUtils {

    /**
     * מחזיר נתיב קובץ אמיתי.
     * אם uri הוא content:// -> מעתיק לקובץ בתוך cacheDir ומחזיר נתיב פנימי.
     */
    @JvmStatic
    fun getFilePath(context: Context, uri: Uri, onResult: (String) -> Unit = {}): String? {
        return try {
            if (uri.scheme == "file") {
                val p = uri.path
                if (!p.isNullOrBlank() && File(p).exists()) {
                    onResult(p)
                    return p
                }
            }

            val cr = context.contentResolver
            val mime = cr.getType(uri) ?: ""
            val ext = when {
                mime.contains("png") -> "png"
                mime.contains("webp") -> "webp"
                mime.contains("jpeg") || mime.contains("jpg") -> "jpg"
                mime.contains("mp4") -> "mp4"
                else -> "bin"
            }

            val out = File(context.cacheDir, "import_${System.currentTimeMillis()}.$ext")
            cr.openInputStream(uri)?.use { input ->
                FileOutputStream(out).use { output -> input.copyTo(output) }
            } ?: return null

            onResult(out.absolutePath)
            out.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    /**
     * שומר על קומפילציה גם אם הקריאה משתנה:
     * DetailsActivity יכול לקרוא עם חתימה שונה -> אנחנו מקבלים vararg.
     *
     * מחזיר נתיב קובץ פלט (בתוך cacheDir).
     */
    @JvmStatic
    fun processImage(context: Context, vararg args: Any?): String {
        // 1) מוצאים inputPath אמיתי
        val inputPath = args.firstOrNull { it is String && File(it).exists() } as? String
            ?: throw IllegalArgumentException("processImage: missing input file path")

        // 2) blurRects אם יש
        val blurRects: List<Any?> = (args.firstOrNull { it is List<*> } as? List<*>)?.toList() ?: emptyList()

        // 3) לוגו אם יש (עוד String שקיים אבל לא ה-input)
        val logoPath = args.filterIsInstance<String>()
            .firstOrNull { it != inputPath && File(it).exists() }

        // 4) פרמטרים יחסיים (אם קיימים)
        val floats = args.filterIsInstance<Float>()
        val logoRelX = floats.getOrNull(0) ?: 0.02f
        val logoRelY = floats.getOrNull(1) ?: 0.02f
        val logoRelW = floats.getOrNull(2) ?: 0.20f
        val strengthMul = floats.getOrNull(3) ?: 1.0f   // אם אצלך אתה מעלה ב-15% -> תעביר 1.15f

        val bmp = BitmapFactory.decodeFile(inputPath)
            ?: throw IllegalStateException("processImage: cannot decode image")

        val outBmp = bmp.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(outBmp)

        // blur בכל Rect שנמצא
        val radius = (16f * strengthMul).coerceIn(6f, 40f)
        for (r in blurRects) {
            val rect = toRectF(r, outBmp.width, outBmp.height) ?: continue
            blurRectInPlace(outBmp, rect, radius)
        }

        // Overlay logo אם יש
        if (!logoPath.isNullOrBlank()) {
            try {
                val logoBmp = BitmapFactory.decodeFile(logoPath)
                if (logoBmp != null) {
                    val targetW = (outBmp.width * logoRelW).toInt().coerceAtLeast(24)
                    val scale = targetW.toFloat() / logoBmp.width.toFloat()
                    val targetH = (logoBmp.height * scale).toInt().coerceAtLeast(24)

                    val scaled = Bitmap.createScaledBitmap(logoBmp, targetW, targetH, true)
                    val x = (outBmp.width * logoRelX).toInt()
                    val y = (outBmp.height * logoRelY).toInt()

                    canvas.drawBitmap(scaled, x.toFloat(), y.toFloat(), Paint(Paint.ANTI_ALIAS_FLAG))
                }
            } catch (_: Exception) {}
        }

        val outFile = File(context.cacheDir, "processed_${System.currentTimeMillis()}.jpg")
        FileOutputStream(outFile).use { fos ->
            outBmp.compress(Bitmap.CompressFormat.JPEG, 92, fos)
        }
        return outFile.absolutePath
    }

    // ===== helpers =====

    private fun toRectF(any: Any?, w: Int, h: Int): RectF? {
        if (any == null) return null

        // אם זה BlurRect שלנו (בדרך כלל יש לו left/top/right/bottom כ-Float)
        try {
            val cls = any.javaClass
            val leftF = cls.methods.firstOrNull { it.name == "getLeft" }?.invoke(any) as? Float
            val topF  = cls.methods.firstOrNull { it.name == "getTop" }?.invoke(any) as? Float
            val rightF= cls.methods.firstOrNull { it.name == "getRight" }?.invoke(any) as? Float
            val bottomF=cls.methods.firstOrNull { it.name == "getBottom" }?.invoke(any) as? Float
            if (leftF != null && topF != null && rightF != null && bottomF != null) {
                return RectF(leftF, topF, rightF, bottomF).normalize(w, h)
            }
        } catch (_: Exception) {}

        return null
    }

    private fun RectF.normalize(w: Int, h: Int): RectF {
        // אם הערכים נראים יחסיים (0..1) נהפוך לפיקסלים
        val rel = (left in 0f..1.5f) && (right in 0f..1.5f) && (top in 0f..1.5f) && (bottom in 0f..1.5f)
        val r = if (rel) RectF(left*w, top*h, right*w, bottom*h) else RectF(left, top, right, bottom)
        r.sort()
        // clamp
        r.left = r.left.coerceIn(0f, w.toFloat())
        r.right = r.right.coerceIn(0f, w.toFloat())
        r.top = r.top.coerceIn(0f, h.toFloat())
        r.bottom = r.bottom.coerceIn(0f, h.toFloat())
        return r
    }

    /**
     * blur פשוט (box blur) על תת-אזור. מספיק לתיקון קומפילציה + "blur" אמיתי (לא פיקסלים).
     */
    private fun blurRectInPlace(bmp: Bitmap, rect: RectF, radius: Float) {
        val r = radius.toInt().coerceAtLeast(2)
        val x0 = rect.left.toInt().coerceAtLeast(0)
        val y0 = rect.top.toInt().coerceAtLeast(0)
        val x1 = rect.right.toInt().coerceAtMost(bmp.width)
        val y1 = rect.bottom.toInt().coerceAtMost(bmp.height)
        if (x1 - x0 < 2 || y1 - y0 < 2) return

        val w = x1 - x0
        val h = y1 - y0
        val pixels = IntArray(w * h)
        bmp.getPixels(pixels, 0, w, x0, y0, w, h)

        val out = IntArray(w * h)
        for (y in 0 until h) {
            for (x in 0 until w) {
                var rs = 0
                var gs = 0
                var bs = 0
                var cnt = 0
                val yMin = (y - r).coerceAtLeast(0)
                val yMax = (y + r).coerceAtMost(h - 1)
                val xMin = (x - r).coerceAtLeast(0)
                val xMax = (x + r).coerceAtMost(w - 1)
                for (yy in yMin..yMax) {
                    val row = yy * w
                    for (xx in xMin..xMax) {
                        val c = pixels[row + xx]
                        rs += (c shr 16) and 0xff
                        gs += (c shr 8) and 0xff
                        bs += (c) and 0xff
                        cnt++
                    }
                }
                val a = 0xff
                val rr = (rs / cnt).coerceIn(0, 255)
                val gg = (gs / cnt).coerceIn(0, 255)
                val bb = (bs / cnt).coerceIn(0, 255)
                out[y * w + x] = (a shl 24) or (rr shl 16) or (gg shl 8) or bb
            }
        }

        bmp.setPixels(out, 0, w, x0, y0, w, h)
    }
}

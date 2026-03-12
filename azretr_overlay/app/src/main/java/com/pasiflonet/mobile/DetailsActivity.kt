package com.pasiflonet.mobile

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import coil.load
import com.pasiflonet.mobile.databinding.ActivityDetailsBinding
import com.pasiflonet.mobile.td.TdLibManager
import com.pasiflonet.mobile.utils.BlurRect
import com.pasiflonet.mobile.utils.ImageUtils
import com.pasiflonet.mobile.utils.MediaProcessor
import com.pasiflonet.mobile.utils.TikTokExportManager
import com.pasiflonet.mobile.utils.TranslationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.drinkless.tdlib.TdApi
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume

class DetailsActivity : BaseActivity() {
    private lateinit var b: ActivityDetailsBinding
    private var rawMediaPath: String? = null
    private var isVideo = false
    private var fileId = 0
    private var thumbId = 0
    private var imageBounds = RectF()
    private var logoRelX = 0.5f
    private var logoRelY = 0.5f
    private var savedLogoRelW = 0.2f

    private val pickLogoLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            try {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) {
            }
            getSharedPreferences("app_prefs", MODE_PRIVATE).edit().putString("logo_uri", uri.toString()).apply()
            loadLogoFromUri(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            b = ActivityDetailsBinding.inflate(layoutInflater)
            setContentView(b.root)
        } catch (e: Exception) {
            Log.e("DETAILS_UI", "Inflation failed", e)
            finish()
            return
        }

        isVideo = intent.getBooleanExtra("IS_VIDEO", false)
        fileId = intent.getIntExtra("FILE_ID", 0)
        thumbId = intent.getIntExtra("THUMB_ID", 0)
        val passedThumbPath = intent.getStringExtra("THUMB_PATH")
        b.etCaption.setText(intent.getStringExtra("CAPTION") ?: "")

        if (passedThumbPath != null && File(passedThumbPath).exists()) {
            loadPreview(passedThumbPath)
        } else if (thumbId != 0) {
            startThumbHunter(thumbId)
        }
        if (fileId != 0) startFullMediaHunter(fileId)

        setupTools()
        setupTikTokUi()
    }

    private fun setupTikTokUi() {
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val cookiesUri = prefs.getString("tiktok_cookies_uri", null)
        val savedTitle = prefs.getString("tiktok_last_title", "") ?: ""
        b.etTikTokTitle.setText(savedTitle)

        b.cbExportTikTok.isEnabled = isVideo
        b.cbExportTikTok.isChecked = isVideo && !savedTitle.isBlank()
        b.etTikTokTitle.visibility = if (b.cbExportTikTok.isChecked) View.VISIBLE else View.GONE
        b.tvTikTokCookieStatus.text = if (cookiesUri.isNullOrBlank()) {
            "TikTok cookies not configured in Settings"
        } else {
            "TikTok cookies ready"
        }

        if (!isVideo) {
            b.cbExportTikTok.isChecked = false
            b.cbExportTikTok.text = "TikTok export available for videos only"
            b.etTikTokTitle.visibility = View.GONE
        }

        b.cbExportTikTok.setOnCheckedChangeListener { _, checked ->
            b.etTikTokTitle.visibility = if (checked && isVideo) View.VISIBLE else View.GONE
        }
    }

    private fun loadPreview(path: String) {
        if (isFinishing || isDestroyed) return
        b.ivPreview.load(File(path)) {
            listener(onSuccess = { _, _ ->
                b.ivPreview.post {
                    if (!isFinishing) {
                        calculateMatrixBounds()
                        if (b.ivDraggableLogo.visibility == View.VISIBLE) restoreLogoPosition()
                    }
                }
            })
        }
    }

    private fun startThumbHunter(tId: Int) {
        TdLibManager.downloadFile(tId)
        lifecycleScope.launch(Dispatchers.IO) {
            repeat(11) {
                if (isFinishing) return@launch
                val path = TdLibManager.getFilePath(tId)
                if (path != null && File(path).exists()) {
                    withContext(Dispatchers.Main) { loadPreview(path) }
                    return@launch
                }
                delay(500)
            }
        }
    }

    private fun startFullMediaHunter(fId: Int) {
        TdLibManager.downloadFile(fId)
        lifecycleScope.launch(Dispatchers.IO) {
            repeat(61) {
                if (isFinishing) return@launch
                val path = TdLibManager.getFilePath(fId)
                if (path != null && File(path).exists()) {
                    val file = File(path)
                    if (file.length() > 50_000 || !isVideo) {
                        rawMediaPath = path
                        if (!isVideo) withContext(Dispatchers.Main) { loadPreview(path) }
                        return@launch
                    }
                }
                delay(1000)
            }
        }
    }

    private fun setupTools() {
        b.btnTranslate.setOnClickListener {
            val originalText = b.etCaption.text.toString()
            if (originalText.isNotBlank()) {
                safeToast("Translating...")
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val translated = TranslationManager.translateToHebrew(originalText)
                        withContext(Dispatchers.Main) {
                            if (!isFinishing) b.etCaption.setText(translated)
                        }
                    } catch (_: Exception) {
                        withContext(Dispatchers.Main) { safeToast("Translation failed") }
                    }
                }
            }
        }

        b.btnModeBlur.setOnClickListener {
            b.drawingView.visibility = View.VISIBLE
            b.drawingView.bringToFront()
            b.drawingView.isBlurMode = true
            b.ivDraggableLogo.alpha = 0.5f
            calculateMatrixBounds()
        }

        b.btnModeLogo.setOnClickListener {
            b.drawingView.isBlurMode = false
            b.ivDraggableLogo.visibility = View.VISIBLE
            b.ivDraggableLogo.alpha = 1.0f
            b.ivDraggableLogo.bringToFront()
            val uriStr = getSharedPreferences("app_prefs", MODE_PRIVATE).getString("logo_uri", null)
            if (uriStr != null) {
                loadLogoFromUri(Uri.parse(uriStr))
            } else {
                pickLogoLauncher.launch("image/*")
            }
        }
        b.btnModeLogo.setOnLongClickListener {
            pickLogoLauncher.launch("image/*")
            true
        }

        var dX = 0f
        var dY = 0f
        b.ivDraggableLogo.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    dX = v.x - event.rawX
                    dY = v.y - event.rawY
                }
                MotionEvent.ACTION_MOVE -> {
                    var newX = event.rawX + dX
                    var newY = event.rawY + dY
                    if (imageBounds.width() > 0) {
                        newX = newX.coerceIn(imageBounds.left, imageBounds.right - v.width)
                        newY = newY.coerceIn(imageBounds.top, imageBounds.bottom - v.height)
                        logoRelX = (newX - imageBounds.left) / imageBounds.width()
                        logoRelY = (newY - imageBounds.top) / imageBounds.height()
                    }
                    v.x = newX
                    v.y = newY
                }
            }
            true
        }

        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        savedLogoRelW = prefs.getFloat("logo_rel_w", 0.2f).coerceIn(0.05f, 0.8f)
        val progress = (((savedLogoRelW - 0.05f) / (0.8f - 0.05f)) * 100f).toInt().coerceIn(0, 100)
        b.sbLogoSize.progress = progress
        b.sbLogoSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val relW = (0.05f + (progress / 100f) * (0.8f - 0.05f)).coerceIn(0.05f, 0.8f)
                savedLogoRelW = relW
                prefs.edit().putFloat("logo_rel_w", relW).apply()
                if (b.ivDraggableLogo.visibility == View.VISIBLE) applyLogoSize(relW)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })

        b.btnSend.setOnClickListener { performStrictSend() }
        b.btnCancel.setOnClickListener { finish() }
    }

    private fun loadLogoFromUri(uri: Uri) {
        if (isFinishing) return
        b.ivDraggableLogo.load(uri) {
            listener(onSuccess = { _, _ ->
                if (!isFinishing) {
                    b.ivDraggableLogo.visibility = View.VISIBLE
                    b.ivDraggableLogo.bringToFront()
                    restoreLogoPosition()
                }
            })
        }
    }

    private fun calculateMatrixBounds() {
        if (isFinishing) return
        val drawable = b.ivPreview.drawable ?: return
        val values = FloatArray(9)
        b.ivPreview.imageMatrix.getValues(values)
        val width = drawable.intrinsicWidth * values[Matrix.MSCALE_X]
        val height = drawable.intrinsicHeight * values[Matrix.MSCALE_Y]
        imageBounds.set(
            values[Matrix.MTRANS_X],
            values[Matrix.MTRANS_Y],
            values[Matrix.MTRANS_X] + width,
            values[Matrix.MTRANS_Y] + height
        )
        b.drawingView.setValidBounds(imageBounds)
    }

    private fun restoreLogoPosition() {
        if (imageBounds.width() > 0) {
            b.ivDraggableLogo.x = imageBounds.left + (logoRelX * imageBounds.width())
            b.ivDraggableLogo.y = imageBounds.top + (logoRelY * imageBounds.height())
        }
    }

    private fun applyLogoSize(relW: Float) {
        if (imageBounds.width() <= 0) calculateMatrixBounds()
        if (imageBounds.width() <= 0) return
        val drawable = b.ivDraggableLogo.drawable ?: return
        val iw = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else b.ivDraggableLogo.width
        val ih = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else b.ivDraggableLogo.height
        val ratio = if (iw > 0) ih.toFloat() / iw.toFloat() else 1f
        val targetW = (imageBounds.width() * relW).toInt().coerceIn(48, 2000)
        val targetH = (targetW * ratio).toInt().coerceIn(48, 2000)
        val lp = b.ivDraggableLogo.layoutParams
        lp.width = targetW
        lp.height = targetH
        b.ivDraggableLogo.layoutParams = lp
        restoreLogoPosition()
    }

    private fun performStrictSend() {
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val target = prefs.getString("target_username", "").orEmpty().trim()
        val exportToTikTok = isVideo && b.cbExportTikTok.isChecked
        val tikTokTitle = b.etTikTokTitle.text?.toString()?.trim().orEmpty()
        val tikTokCookiesUri = prefs.getString("tiktok_cookies_uri", null)
        val includeMediaInTelegram = b.swIncludeMedia.isChecked

        if (exportToTikTok) {
            if (tikTokTitle.isBlank()) {
                safeToast("Fill TikTok title")
                return
            }
            if (tikTokCookiesUri.isNullOrBlank()) {
                safeToast("Select TikTok cookies file in Settings")
                return
            }
            prefs.edit().putString("tiktok_last_title", tikTokTitle).apply()
        }

        if (target.isBlank() && !exportToTikTok) {
            safeToast("No Telegram target and no TikTok export selected")
            return
        }

        val rawPath = rawMediaPath
        if (rawPath == null || !File(rawPath).exists()) {
            if (!isVideo && fileId == 0 && thumbId == 0) {
                val textToSend = b.etCaption.text?.toString()?.trim().orEmpty()
                if (textToSend.isEmpty()) {
                    safeToast("אין טקסט לשליחה")
                    return
                }
                if (target.isBlank()) {
                    safeToast("No Telegram target configured")
                    return
                }
                b.btnSend.isEnabled = false
                safeToast("שולח טקסט…")
                try {
                    startActivity(Intent(this, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    })
                } catch (_: Exception) {
                }
                CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                    TdLibManager.sendFinalMessage(target, textToSend, null, false)
                }
                return
            }
            safeToast("Wait, downloading media...")
            return
        }

        val rects = ArrayList<BlurRect>()
        for (r in b.drawingView.rects) rects.add(BlurRect(r.left, r.top, r.right, r.bottom))
        val caption = b.etCaption.text?.toString().orEmpty()

        var currentLogoRelX = prefs.getFloat("logo_rel_x", 0.02f)
        var currentLogoRelY = prefs.getFloat("logo_rel_y", 0.02f)
        var currentLogoRelW = prefs.getFloat("logo_rel_w", savedLogoRelW).coerceIn(0.05f, 0.8f)

        if (imageBounds.width() <= 0f || imageBounds.height() <= 0f) calculateMatrixBounds()
        if (b.ivDraggableLogo.visibility == View.VISIBLE && imageBounds.width() > 0f && imageBounds.height() > 0f) {
            currentLogoRelX = ((b.ivDraggableLogo.x - imageBounds.left) / imageBounds.width()).coerceIn(0f, 1f)
            currentLogoRelY = ((b.ivDraggableLogo.y - imageBounds.top) / imageBounds.height()).coerceIn(0f, 1f)
            currentLogoRelW = (b.ivDraggableLogo.width.toFloat() / imageBounds.width()).coerceIn(0.05f, 0.8f)
            prefs.edit()
                .putFloat("logo_rel_x", currentLogoRelX)
                .putFloat("logo_rel_y", currentLogoRelY)
                .putFloat("logo_rel_w", currentLogoRelW)
                .apply()
        }

        var logoUri: Uri? = null
        if (b.ivDraggableLogo.visibility == View.VISIBLE) {
            try {
                val drawable = b.ivDraggableLogo.drawable
                if (drawable is BitmapDrawable) {
                    val outFile = File(applicationContext.cacheDir, "temp_logo.png")
                    FileOutputStream(outFile).use { output ->
                        drawable.bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
                    }
                    logoUri = Uri.fromFile(outFile)
                }
            } catch (_: Exception) {
            }
        }

        try {
            b.loadingOverlay.visibility = View.GONE
        } catch (_: Exception) {
        }
        b.btnSend.isEnabled = false
        try {
            safeToast(if (exportToTikTok) "מעבד ושומר חבילת TikTok…" else "שולח ברקע…")
            startActivity(Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            })
        } catch (_: Exception) {
        }

        val appCtx = applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val safeInputFile = File(appCtx.cacheDir, "safe_input.${if (isVideo) "mp4" else "jpg"}")
                File(rawPath).copyTo(safeInputFile, overwrite = true)
                val outPath = File(appCtx.cacheDir, "safe_out_${System.currentTimeMillis()}.${if (isVideo) "mp4" else "jpg"}").absolutePath

                val success = if (isVideo) {
                    try {
                        suspendCancellableCoroutine { cont ->
                            MediaProcessor.processContent(
                                appCtx,
                                safeInputFile.absolutePath,
                                outPath,
                                true,
                                rects,
                                logoUri,
                                currentLogoRelX,
                                currentLogoRelY,
                                currentLogoRelW
                            ) { cont.resume(it) }
                        }
                    } catch (_: Exception) {
                        false
                    }
                } else {
                    ImageUtils.processImage(
                        appCtx,
                        safeInputFile.absolutePath,
                        outPath,
                        rects,
                        logoUri,
                        currentLogoRelX,
                        currentLogoRelY,
                        currentLogoRelW
                    )
                }

                if (success && File(outPath).exists() && File(outPath).length() > 0) {
                    var tiktokExported = false
                    if (exportToTikTok && isVideo && !tikTokCookiesUri.isNullOrBlank()) {
                        tiktokExported = TikTokExportManager.exportVideoPackage(
                            appCtx,
                            outPath,
                            tikTokTitle,
                            caption,
                            tikTokCookiesUri
                        )
                    }

                    if (target.isNotBlank()) {
                        TdLibManager.sendFinalMessage(target, caption, if (includeMediaInTelegram) outPath else null, isVideo)
                    }

                    when {
                        target.isNotBlank() && tiktokExported -> safeToast("✅ Sent + TikTok package saved")
                        target.isNotBlank() -> safeToast("✅ Sent")
                        tiktokExported -> safeToast("TikTok package saved to Downloads")
                        else -> safeToast("Processed, but export failed")
                    }
                    runOnUiThread { try { finish() } catch (_: Exception) {} }
                } else {
                    safeToast("❌ Edit failed")
                    runOnUiThread {
                        if (!isFinishing && !isDestroyed) b.btnSend.isEnabled = true
                    }
                }
            } catch (e: Exception) {
                safeToast("Error: ${e.message}")
                runOnUiThread {
                    if (!isFinishing && !isDestroyed) b.btnSend.isEnabled = true
                }
            }
        }
    }

    private fun safeToast(msg: String) {
        runOnUiThread {
            try {
                Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {
            }
        }
    }
}

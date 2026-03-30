package com.pasiflonet.mobile

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.pasiflonet.mobile.databinding.ActivityMainBinding
import com.pasiflonet.mobile.td.TdLibManager
import com.pasiflonet.mobile.utils.CrashLogger
import com.pasiflonet.mobile.utils.KeepAliveService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.drinkless.tdlib.TdApi
import java.io.File

class MainActivity : BaseActivity() {
    private lateinit var b: ActivityMainBinding
    private lateinit var adapter: ChatAdapter

    private data class TvChannel(
        val name: String,
        val url: String
    )

    private val tvChannels = listOf(
        TvChannel("כאן 11", "https://www.kan.org.il/live/"),
        TvChannel("ערוץ הכנסת", "https://www.knesset.tv/live/"),
        TvChannel("i24NEWS", "https://video.i24news.tv/"),
        TvChannel("ערוץ 13", "https://13tv.co.il/live/"),
        TvChannel("ערוץ 12", "https://www.mako.co.il/mako-vod-live-tv"),
        TvChannel("כאן חינוכית", "https://www.kan.org.il/content/kan/kan-educational/p-12239/")
    )

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CrashLogger.install(application)

        try {
            b = ActivityMainBinding.inflate(layoutInflater)
            setContentView(b.root)
        } catch (e: Exception) {
            Log.e("UI_CRASH", "Layout Inflation Failed", e)
            val errorView = android.widget.TextView(this).apply {
                text = "CRITICAL UI ERROR:\n${e.message}\n\nCheck Logcat for details."
                textSize = 20f
                setTextColor(android.graphics.Color.RED)
                setPadding(50, 50, 50, 50)
            }
            setContentView(errorView)
            return
        }

        try {
            startService(Intent(this, KeepAliveService::class.java))
            b.apiContainer.visibility = View.GONE
            b.loginContainer.visibility = View.GONE
            b.mainContent.visibility = View.GONE
            setupUI()
            setupTvPanel()
            checkPermissions()
            checkApiAndInit()
        } catch (e: Exception) {
            Toast.makeText(this, "Runtime Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            TdLibManager.setOnline(true)
            TdLibManager.refreshRecentMessages()
            if (::adapter.isInitialized) {
                adapter.updateList(TdLibManager.currentMessages.value)
            }
            if (::b.isInitialized) {
                b.tvWebView.onResume()
            }
        } catch (_: Exception) {
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            TdLibManager.setOnline(true)
            if (::b.isInitialized) {
                b.tvWebView.onPause()
            }
        } catch (_: Exception) {
        }
    }

    override fun onStop() {
        super.onStop()
        try {
            TdLibManager.setOnline(true)
        } catch (_: Exception) {
        }
    }

    override fun onDestroy() {
        try {
            if (::b.isInitialized) {
                b.tvWebView.stopLoading()
                b.tvWebView.loadUrl("about:blank")
                b.tvWebView.destroy()
            }
        } catch (_: Exception) {
        }
        super.onDestroy()
    }

    private fun setupUI() {
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)

        b.btnSaveApi.setOnClickListener {
            val id = b.etApiId.text.toString().toIntOrNull()
            val hash = b.etApiHash.text.toString()
            if (id != null && hash.isNotEmpty()) {
                prefs.edit().putInt("api_id", id).putString("api_hash", hash).apply()
                checkApiAndInit()
            }
        }

        b.btnSendCode.setOnClickListener {
            val p = b.etPhone.text.toString()
            if (p.isEmpty()) return@setOnClickListener
            b.btnSendCode.text = "Sending..."
            b.btnSendCode.isEnabled = false
            TdLibManager.sendPhone(p) { e ->
                runOnUiThread {
                    b.btnSendCode.isEnabled = true
                    b.btnSendCode.text = "SEND CODE"
                    Toast.makeText(this, e, Toast.LENGTH_LONG).show()
                }
            }
        }

        b.btnVerify.setOnClickListener {
            val c = b.etCode.text.toString()
            if (c.isNotEmpty()) {
                TdLibManager.sendCode(c) { e ->
                    runOnUiThread {
                        Toast.makeText(this, e, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        b.btnVerifyPassword.setOnClickListener {
            val pa = b.etPassword.text.toString()
            TdLibManager.sendPassword(pa) { e ->
                runOnUiThread {
                    Toast.makeText(this, e, Toast.LENGTH_LONG).show()
                }
            }
        }

        adapter = ChatAdapter(emptyList()) { msg ->
            var thumbPath: String? = null
            var fullId = 0
            var isVideo = false
            var caption = ""
            var thumbId = 0

            when (msg.content) {
                is TdApi.MessagePhoto -> {
                    val c = msg.content as TdApi.MessagePhoto
                    val previewPhoto = c.photo.sizes.find { it.type == "x" }
                        ?: c.photo.sizes.find { it.type == "y" }
                        ?: c.photo.sizes.find { it.type == "w" }
                        ?: c.photo.sizes.lastOrNull()
                        ?: c.photo.sizes.firstOrNull()
                    if (previewPhoto != null) {
                        thumbPath = previewPhoto.photo.local.path
                        thumbId = previewPhoto.photo.id
                    }
                    fullId = if (c.photo.sizes.isNotEmpty()) c.photo.sizes.last().photo.id else 0
                    caption = c.caption.text
                }
                is TdApi.MessageVideo -> {
                    val c = msg.content as TdApi.MessageVideo
                    val thumb = c.video.thumbnail
                    if (thumb != null) {
                        thumbPath = thumb.file.local.path
                        thumbId = thumb.file.id
                    }
                    fullId = c.video.video.id
                    isVideo = true
                    caption = c.caption.text
                }
                is TdApi.MessageText -> {
                    caption = (msg.content as TdApi.MessageText).text.text
                }
            }

            if (thumbId != 0) TdLibManager.downloadFile(thumbId)
            if (fullId != 0) TdLibManager.downloadFile(fullId)

            lifecycleScope.launch(Dispatchers.IO) {
                var resolvedThumbPath = thumbPath
                if ((resolvedThumbPath == null || !File(resolvedThumbPath).exists()) && thumbId != 0) {
                    for (i in 0..20) {
                        val pth = TdLibManager.getFilePath(thumbId)
                        if (pth != null && File(pth).exists()) {
                            resolvedThumbPath = pth
                            break
                        }
                        kotlinx.coroutines.delay(100)
                    }
                }
                withContext(Dispatchers.Main) {
                    val intent = Intent(this@MainActivity, DetailsActivity::class.java)
                    if (resolvedThumbPath != null) intent.putExtra("THUMB_PATH", resolvedThumbPath)
                    intent.putExtra("THUMB_ID", thumbId)
                    intent.putExtra("FILE_ID", fullId)
                    intent.putExtra("IS_VIDEO", isVideo)
                    intent.putExtra("CAPTION", caption)
                    startActivity(intent)
                }
            }
        }

        b.rvMessages.layoutManager = LinearLayoutManager(this)
        b.rvMessages.adapter = adapter

        b.btnClearCache.setOnClickListener {
            val files = cacheDir.listFiles()
            var deletedCount = 0
            if (files != null) {
                for (file in files) {
                    if (file.isFile && file.delete()) deletedCount++
                }
            }
            Toast.makeText(
                this,
                if (deletedCount > 0) "Cleaned $deletedCount files" else "Cache clean",
                Toast.LENGTH_SHORT
            ).show()
        }

        b.btnSettings.setOnClickListener {
            try {
                startActivity(Intent(this, SettingsActivity::class.java))
            } catch (_: Exception) {
                Toast.makeText(this, "Settings Error", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupTvPanel() {
        val names = tvChannels.map { it.name }
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, names)
        b.tvSpinner.adapter = spinnerAdapter

        val webView = b.tvWebView
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.loadsImagesAutomatically = true
        webView.settings.mediaPlaybackRequiresUserGesture = false
        webView.settings.cacheMode = WebSettings.LOAD_DEFAULT
        webView.settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        webView.settings.useWideViewPort = true
        webView.settings.loadWithOverviewMode = true
        webView.settings.builtInZoomControls = false
        webView.settings.displayZoomControls = false
        webView.webChromeClient = WebChromeClient()
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                return false
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                b.tvLoading.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                b.tvLoading.visibility = View.GONE
            }
        }

        b.tvSpinner.setSelection(0, false)
        b.tvSpinner.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                loadChannel(position)
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
        })

        b.btnReloadTv.setOnClickListener {
            b.tvWebView.reload()
        }
    }

    private fun loadChannel(position: Int) {
        val channel = tvChannels.getOrNull(position) ?: tvChannels.first()
        b.tvTitle.text = "שידור חי: ${channel.name}"
        b.tvWebView.loadUrl(channel.url)
    }

    private fun checkApiAndInit() {
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val i = prefs.getInt("api_id", 0)
        val h = prefs.getString("api_hash", "")
        if (i != 0 && !h.isNullOrEmpty()) {
            b.apiContainer.visibility = View.GONE
            TdLibManager.init(this@MainActivity, i, h)
            observeAuth()
        } else {
            b.apiContainer.visibility = View.VISIBLE
            b.mainContent.visibility = View.GONE
        }
    }

    private fun observeAuth() {
        lifecycleScope.launch {
            TdLibManager.authState.collect { s ->
                runOnUiThread {
                    when (s) {
                        is TdApi.AuthorizationStateWaitPhoneNumber -> {
                            b.apiContainer.visibility = View.GONE
                            b.loginContainer.visibility = View.VISIBLE
                            b.phoneLayout.visibility = View.VISIBLE
                            b.codeLayout.visibility = View.GONE
                            b.passwordLayout.visibility = View.GONE
                            b.btnSendCode.isEnabled = true
                            b.btnSendCode.text = "SEND CODE"
                        }
                        is TdApi.AuthorizationStateWaitCode -> {
                            b.loginContainer.visibility = View.VISIBLE
                            b.phoneLayout.visibility = View.GONE
                            b.codeLayout.visibility = View.VISIBLE
                        }
                        is TdApi.AuthorizationStateWaitPassword -> {
                            b.loginContainer.visibility = View.VISIBLE
                            b.codeLayout.visibility = View.GONE
                            b.passwordLayout.visibility = View.VISIBLE
                        }
                        is TdApi.AuthorizationStateReady -> {
                            b.loginContainer.visibility = View.GONE
                            b.mainContent.visibility = View.VISIBLE
                            TdLibManager.setOnline(true)
                            TdLibManager.refreshRecentMessages()
                        }
                    }
                }
            }
        }

        lifecycleScope.launch(Dispatchers.IO) {
            TdLibManager.currentMessages.collect { m ->
                withContext(Dispatchers.Main) {
                    adapter.updateList(m)
                }
                m.forEach { msg ->
                    val previewIdToDownload = when (val c = msg.content) {
                        is TdApi.MessagePhoto -> {
                            val previewPhoto = c.photo.sizes.find { it.type == "x" }
                                ?: c.photo.sizes.find { it.type == "y" }
                                ?: c.photo.sizes.find { it.type == "w" }
                                ?: c.photo.sizes.lastOrNull()
                                ?: c.photo.sizes.firstOrNull()
                            previewPhoto?.photo?.id ?: 0
                        }
                        is TdApi.MessageVideo -> c.video.thumbnail?.file?.id ?: 0
                        else -> 0
                    }
                    if (previewIdToDownload != 0) TdLibManager.downloadFile(previewIdToDownload)
                }
            }
        }
    }

    private fun checkPermissions() {
        val perms = mutableListOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.clear()
            perms.add(Manifest.permission.READ_MEDIA_IMAGES)
            perms.add(Manifest.permission.READ_MEDIA_VIDEO)
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            perms.add(Manifest.permission.FOREGROUND_SERVICE)
        }
        requestPermissionLauncher.launch(perms.toTypedArray())
    }
}

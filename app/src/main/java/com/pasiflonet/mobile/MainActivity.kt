package com.pasiflonet.mobile

import com.pasiflonet.mobile.utils.NotificationHelper

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
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

class MainActivity : BaseActivity() {

    private lateinit var b: ActivityMainBinding
    private lateinit var adapter: ChatAdapter

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        NotificationHelper.createChannels(this)
        NotificationPermission.requestIfNeeded(this)
// Orientation unlocked: portrait and landscape supported

        CrashLogger.install(application)

        try {
            b = ActivityMainBinding.inflate(layoutInflater)
            setContentView(b.root)
        } catch (e: Exception) {
            Log.e("UI_CRASH", "Layout Inflation Failed", e)
            val errorView = android.widget.TextView(this)
            errorView.text = "CRITICAL UI ERROR:\n${e.message}\n\nCheck Logcat for details."
            errorView.textSize = 20f
            errorView.setTextColor(android.graphics.Color.RED)
            errorView.setPadding(50, 50, 50, 50)
            setContentView(errorView)
            return
        }

        try {
            startService(Intent(this, KeepAliveService::class.java))
            b.apiContainer.visibility = View.GONE
            b.loginContainer.visibility = View.GONE
            b.mainContent.visibility = View.GONE
            setupUI()
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
        } catch (_: Exception) {
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            TdLibManager.setOnline(true)
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
        super.onDestroy()
    }

    private fun setupUI() {

        try {
            b.rvMessages.layoutManager = LinearLayoutManager(this)
            adapter = ChatAdapter(emptyList<TdApi.Message>()) { msg ->
                try {
                    val intent = Intent(this, DetailsActivity::class.java)

                    var caption = ""
                    var fileId = 0
                    var thumbId = 0
                    var isVideo = false

                    when (val c = msg.content) {
                        is TdApi.MessageText -> {
                            caption = c.text.text
                        }
                        is TdApi.MessagePhoto -> {
                            caption = c.caption.text
                            val best = c.photo.sizes.maxByOrNull { it.width * it.height }
                            fileId = best?.photo?.id ?: 0
                            thumbId = fileId
                        }
                        is TdApi.MessageVideo -> {
                            caption = c.caption.text
                            fileId = c.video.video.id
                            thumbId = c.video.thumbnail?.file?.id ?: 0
                            isVideo = true
                        }
                        is TdApi.MessageAnimation -> {
                            caption = c.caption.text
                            fileId = c.animation.animation.id
                            thumbId = c.animation.thumbnail?.file?.id ?: 0
                        }
                        is TdApi.MessageDocument -> {
                            caption = c.caption.text
                            fileId = c.document.document.id
                        }
                    }

                    intent.putExtra("CAPTION", caption)
                    intent.putExtra("FILE_ID", fileId)
                    intent.putExtra("THUMB_ID", thumbId)
                    intent.putExtra("IS_VIDEO", isVideo)
                    startActivity(intent)
                } catch (_: Exception) { }
            }
            b.rvMessages.adapter = adapter
            adapter.updateList(TdLibManager.currentMessages.value)
        } catch (_: Exception) { }
 try { com.pasiflonet.mobile.utils.CyberUiHelper.wireMapSearch(b.root); com.pasiflonet.mobile.utils.CyberUiHelper.wireVerifyBox(b.root) } catch (_: Exception) { }
        


        try {
            b.root.findViewById<android.widget.Button>(resources.getIdentifier("btnCurrentIntel", "id", packageName))?.setOnClickListener {
                startActivity(android.content.Intent(this, CurrentIntelActivity::class.java))
            }
            b.root.findViewById<android.widget.Button>(resources.getIdentifier("btnVerifyIntel", "id", packageName))?.setOnClickListener {
                startActivity(android.content.Intent(this, VerifyIntelActivity::class.java))
            }
            b.root.findViewById<android.widget.Button>(resources.getIdentifier("btnFlights", "id", packageName))?.setOnClickListener {
                startActivity(android.content.Intent(this, FlightTrackerActivity::class.java))
            }
            b.root.findViewById<android.widget.Button>(resources.getIdentifier("btnMilitaryFlights", "id", packageName))?.setOnClickListener {
                startActivity(android.content.Intent(this, MilitaryFlightMonitorActivity::class.java))
            }
        } catch (_: Exception) { }

        b.btnSettings.setOnClickListener {
            try {
                startActivity(Intent(this, SettingsActivity::class.java))
            } catch (_: Exception) {
                Toast.makeText(this, "Settings Error", Toast.LENGTH_SHORT).show()
            }
        }
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
                    try { if (::adapter.isInitialized) { adapter.updateList(m); com.pasiflonet.mobile.utils.CyberAlertCounter.updateFromMessages(this@MainActivity, m); com.pasiflonet.mobile.utils.CyberUiHelper.refreshMapCounters(b.root); com.pasiflonet.mobile.utils.CyberUiHelper.flashUpdateButtons(this@MainActivity, b.root) } } catch (_: Exception) { }; com.pasiflonet.mobile.utils.CyberUiHelper.flashUpdateButtons(this@MainActivity, b.root)
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

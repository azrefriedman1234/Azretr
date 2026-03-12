package com.pasiflonet.mobile

import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.pasiflonet.mobile.databinding.ActivitySettingsBinding
import java.io.File
import java.io.FileOutputStream

class SettingsActivity : BaseActivity() {
    private lateinit var b: ActivitySettingsBinding
    private lateinit var prefs: SharedPreferences

    private val pickLogo = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            try {
                val localUri = copyPickedFileToInternal(uri, "channel_logo.png")
                prefs.edit().putString("logo_uri", localUri).apply()
                b.ivCurrentLogo.setImageURI(Uri.parse(localUri))
                Toast.makeText(this, "Logo saved", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Logo error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private val pickTikTokCookies = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            try {
                val localUri = copyPickedFileToInternal(uri, "tiktok_cookies.txt")
                prefs.edit().putString("tiktok_cookies_uri", localUri).apply()
                renderTikTokCookiesPath(localUri)
                Toast.makeText(this, "TikTok cookies saved", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Cookies error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            b = ActivitySettingsBinding.inflate(layoutInflater)
            setContentView(b.root)
            prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)

            val currentTarget = prefs.getString("target_username", "").orEmpty()
            val currentLogo = prefs.getString("logo_uri", "").orEmpty()
            val currentTikTokCookies = prefs.getString("tiktok_cookies_uri", "").orEmpty()

            b.etTargetUsername.setText(currentTarget)
            if (currentLogo.isNotEmpty()) {
                try {
                    b.ivCurrentLogo.setImageURI(Uri.parse(currentLogo))
                } catch (_: Exception) {
                }
            }
            renderTikTokCookiesPath(currentTikTokCookies)

            b.btnSaveSettings.setOnClickListener {
                prefs.edit()
                    .putString("target_username", b.etTargetUsername.text?.toString()?.trim().orEmpty())
                    .apply()
                Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show()
                finish()
            }

            b.btnSelectLogo.setOnClickListener { pickLogo.launch("image/*") }
            b.btnSelectTikTokCookies.setOnClickListener { pickTikTokCookies.launch("*/*") }
            b.btnClearTikTokCookies.setOnClickListener {
                prefs.edit().remove("tiktok_cookies_uri").apply()
                try {
                    File(filesDir, "tiktok_cookies.txt").delete()
                } catch (_: Exception) {
                }
                renderTikTokCookiesPath(null)
                Toast.makeText(this, "TikTok cookies cleared", Toast.LENGTH_SHORT).show()
            }
            b.btnClearCache.setOnClickListener {
                try {
                    cacheDir.deleteRecursively()
                    cacheDir.mkdirs()
                    Toast.makeText(this, "Cache cleared", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this, "Cache clear failed", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error opening settings: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun copyPickedFileToInternal(uri: Uri, fileName: String): String {
        val inputStream = contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Unable to open selected file")
        val localFile = File(filesDir, fileName)
        FileOutputStream(localFile).use { output ->
            inputStream.use { input ->
                input.copyTo(output)
            }
        }
        return Uri.fromFile(localFile).toString()
    }

    private fun renderTikTokCookiesPath(uriString: String?) {
        val value = uriString?.takeIf { it.isNotBlank() }
        b.tvTikTokCookiesPath.text = if (value == null) {
            getString(R.string.tiktok_cookies_empty)
        } else {
            val fileName = try {
                File(Uri.parse(value).path ?: "").name
            } catch (_: Exception) {
                value
            }
            "Saved: $fileName"
        }
    }
}

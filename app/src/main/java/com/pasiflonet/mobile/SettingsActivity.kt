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
                val inputStream = contentResolver.openInputStream(uri)
                val localFile = File(filesDir, "channel_logo.png")
                val outputStream = FileOutputStream(localFile)
                inputStream?.copyTo(outputStream)
                inputStream?.close()
                outputStream.close()
                val localUri = Uri.fromFile(localFile).toString()
                prefs.edit().putString("logo_uri", localUri).apply()
                b.ivCurrentLogo.setImageURI(Uri.parse(localUri))
                Toast.makeText(this, "הלוגו נשמר", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "שגיאה: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            b = ActivitySettingsBinding.inflate(layoutInflater)
            setContentView(b.root)
            prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)

            b.etTargetUsername.setText(prefs.getString("target_username", "") ?: "")
            b.etSignature.setText(prefs.getString("text_signature", "") ?: "")
            b.etKeywords.setText(prefs.getString("alert_keywords", "") ?: "")

            val currentLogo = prefs.getString("logo_uri", "") ?: ""
            if (currentLogo.isNotEmpty()) {
                try { b.ivCurrentLogo.setImageURI(Uri.parse(currentLogo)) } catch (_: Exception) {}
            }

            b.btnSaveSettings.setOnClickListener {
                val target = b.etTargetUsername.text?.toString()?.trim().orEmpty()
                val signature = b.etSignature.text?.toString()?.trim().orEmpty()
                val keywords = b.etKeywords.text?.toString()?.trim().orEmpty()

                prefs.edit()
                    .putString("target_username", target)
                    .putString("text_signature", signature)
                    .putString("alert_keywords", keywords)
                    .apply()

                Toast.makeText(this, "ההגדרות נשמרו", Toast.LENGTH_SHORT).show()
                finish()
            }

            b.btnSelectLogo.setOnClickListener { pickLogo.launch("image/*") }

            b.btnClearCache.setOnClickListener {
                try {
                    cacheDir.deleteRecursively()
                    Toast.makeText(this, "הקאש נוקה", Toast.LENGTH_SHORT).show()
                } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            Toast.makeText(this, "שגיאה בפתיחת הגדרות: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }
}

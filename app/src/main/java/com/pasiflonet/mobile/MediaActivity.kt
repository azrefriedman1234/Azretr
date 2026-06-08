package com.pasiflonet.mobile

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.pasiflonet.mobile.databinding.ActivityMediaBinding

class MediaActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMediaBinding
    private var player: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMediaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mediaUrl = intent.getStringExtra(EXTRA_MEDIA_URL)
        if (!mediaUrl.isNullOrBlank()) {
            startPlayer(Uri.parse(mediaUrl))
        } else {
            binding.mediaTitle.text = "לא נבחר קובץ מדיה"
        }
    }

    private fun startPlayer(uri: Uri) {
        val exoPlayer = ExoPlayer.Builder(this).build()
        binding.playerView.player = exoPlayer
        exoPlayer.setMediaItem(MediaItem.fromUri(uri))
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
        player = exoPlayer
    }

    override fun onStop() {
        super.onStop()
        player?.release()
        player = null
    }

    companion object {
        const val EXTRA_MEDIA_URL = "extra_media_url"
    }
}

package com.simplerapps.videotoaudio.share

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.simplerapps.videotoaudio.databinding.ActivityShareBinding
import com.simplerapps.videotoaudio.service.InfoActivity.Companion.CONTENT_URI

class ShareActivity : AppCompatActivity() {
    private lateinit var viewBinding: ActivityShareBinding
    private lateinit var exoplayer: ExoPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityShareBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        val uri = intent.getStringExtra(CONTENT_URI)

        uri?.let {
            exoplayer = ExoPlayer.Builder(this).build()
            val mediaItem = MediaItem.fromUri(uri)
            exoplayer.addMediaItem(mediaItem)
            viewBinding.audioPlayer.player = exoplayer
            exoplayer.playWhenReady = false
            exoplayer.prepare()
            viewBinding.audioPlayer.controllerShowTimeoutMs = 0
            viewBinding.audioPlayer.controllerHideOnTouch = false
        }
    }
}
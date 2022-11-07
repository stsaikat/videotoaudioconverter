package com.simplerapps.videotoaudio.share

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.simplerapps.videotoaudio.R
import com.simplerapps.videotoaudio.databinding.ActivityShareBinding
import com.simplerapps.videotoaudio.service.InfoActivity.Companion.CONTENT_URI
import com.simplerapps.videotoaudio.servicechooser.ServiceChooserActivity

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

    override fun onStop() {
        super.onStop()
        exoplayer.release()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.share_screen_menu,menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.home -> {
                goToHomeScreen()
                true
            }
            else -> false
        }
    }

    private fun goToHomeScreen() {
        val intent = Intent(this,ServiceChooserActivity::class.java)
        startActivity(intent)
        finishAffinity()
    }
}
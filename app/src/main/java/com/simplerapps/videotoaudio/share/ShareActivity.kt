package com.simplerapps.videotoaudio.share

import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toUri
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.simplerapps.videotoaudio.R
import com.simplerapps.videotoaudio.databinding.ActivityShareBinding
import com.simplerapps.videotoaudio.getDateTimeFromMillis
import com.simplerapps.videotoaudio.service.InfoActivity.Companion.CONTENT_URI
import com.simplerapps.videotoaudio.servicechooser.ServiceChooserActivity
import java.io.File

class ShareActivity : AppCompatActivity() {
    private lateinit var viewBinding: ActivityShareBinding
    private lateinit var exoplayer: ExoPlayer
    private lateinit var uri: String
    private var alreadySaved = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityShareBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        uri = intent.getStringExtra(CONTENT_URI)!!

        uri.let {
            exoplayer = ExoPlayer.Builder(this).build()
            val mediaItem = MediaItem.fromUri(it)
            exoplayer.addMediaItem(mediaItem)
            viewBinding.audioPlayer.player = exoplayer
            exoplayer.playWhenReady = false
            exoplayer.prepare()
            viewBinding.audioPlayer.controllerShowTimeoutMs = 0
            viewBinding.audioPlayer.controllerHideOnTouch = false
        }

        viewBinding.btSaveToStorage.setOnClickListener {
            saveToExternalStorage(uri)
        }
    }

    override fun onStop() {
        super.onStop()
        exoplayer.release()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.share_screen_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.home -> {
                goToHomeScreen()
                true
            }
            else -> false
        }
    }

    private fun goToHomeScreen() {
        val intent = Intent(this, ServiceChooserActivity::class.java)
        startActivity(intent)
        finishAffinity()
    }

    private fun saveToExternalStorage(uri: String?) {
        if (uri == null) {
            showInfoDialog("No files to save!")
            return
        }

        if (alreadySaved) {
            showInfoDialog("Already saved!")
            return
        }

        val externalUri = getExternalOutUri()

        if (externalUri == null) {
            showInfoDialog("Failed to save!")
            return
        }

        contentResolver.openInputStream(Uri.parse(uri)).use { input ->
            contentResolver.openOutputStream(externalUri).use { output ->
                input!!.copyTo(output!!, DEFAULT_BUFFER_SIZE)
            }
        }

        showInfoDialog("Saved Successfully!")
        alreadySaved = true
    }

    private fun getExternalOutUri(): Uri? {

        val fileName =
            "VideoToAudioConverter${getDateTimeFromMillis(System.currentTimeMillis())}.m4a"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "audio/mp4a")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_MUSIC)
            }

            return contentResolver.insert(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues
            )
        } else {
            val file = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), fileName
            )

            return file.toUri()
        }
    }

    private fun showInfoDialog(info: String) {
        val builder = AlertDialog.Builder(this)
        builder.setMessage(info)
        builder.setPositiveButton("Ok", null)
        builder.create().show()
    }
}
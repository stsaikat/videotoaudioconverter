package com.simplerapps.videotoaudio.share

import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuItem
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.simplerapps.videotoaudio.R
import com.simplerapps.videotoaudio.common.FileInfoManager
import com.simplerapps.videotoaudio.databinding.ActivityShareBinding
import com.simplerapps.videotoaudio.getFileNameSerial
import com.simplerapps.videotoaudio.service.InfoActivity.Companion.CONTENT_URI
import com.simplerapps.videotoaudio.servicechooser.ServiceChooserActivity

class ShareActivity : AppCompatActivity() {

    companion object {
        const val ALREADY_SAVED = "already_saved"
    }

    private lateinit var viewBinding: ActivityShareBinding
    private lateinit var exoplayer: ExoPlayer
    private lateinit var uri: String
    private var alreadySaved = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityShareBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        savedInstanceState?.let {
            alreadySaved = it.getBoolean(ALREADY_SAVED)
        }

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
            exoplayer.pause()
            saveToExternalStorage(uri)
        }

        viewBinding.btShare.setOnClickListener {
            exoplayer.pause()
            onShareClick()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(ALREADY_SAVED, alreadySaved)
        super.onSaveInstanceState(outState)
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

        val externalName = getToSaveName()
        val externalUri = getExternalOutUri(externalName)

        if (externalUri == null) {
            showInfoDialog("Failed to save!")
            return
        }

        contentResolver.openInputStream(Uri.parse(uri)).use { input ->
            contentResolver.openOutputStream(externalUri).use { output ->
                input!!.copyTo(output!!, DEFAULT_BUFFER_SIZE)
            }
        }

        FileInfoManager.savedFileUri = externalUri
        FileInfoManager.savedFileName = externalName

        showInfoDialog(
            title = "Saved Successfully!",
            message = "Saved To \"storage/Music/${FileInfoManager.savedFileName}\""
        )
        alreadySaved = true
    }

    private fun getExternalOutUri(name: String): Uri? {

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, FileInfoManager.mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_MUSIC)
        }

        return contentResolver.insert(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues
        )
    }

    private fun onShareClick() {

        if (FileInfoManager.savedFileUri == null) {
            showInfoDialog(
                title = "Please save before share!",
                message = "click on the \"SAVE TO STORAGE\" button to save."
            )

            return
        }

        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "audio/*"
        intent.putExtra(Intent.EXTRA_STREAM, FileInfoManager.savedFileUri)
        startActivity(Intent.createChooser(intent, "Share ${FileInfoManager.savedFileName} using"))
    }

    private fun getToSaveName(): String {
        var name = "Audio_Converter${getFileNameSerial()}.m4a"
        FileInfoManager.fileName?.let {
            name = "${it}_${name}"
        }

        return name
    }

    private fun showInfoDialog(title: String? = null, message: String? = null) {
        val processResultDialog = ProcessResultDialog(title, message)
        processResultDialog.show(supportFragmentManager, null)
    }
}
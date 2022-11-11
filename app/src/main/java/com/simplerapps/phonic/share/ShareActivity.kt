package com.simplerapps.phonic.share

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
import com.simplerapps.phonic.R
import com.simplerapps.phonic.common.FileInfoManager
import com.simplerapps.phonic.databinding.ActivityShareBinding
import com.simplerapps.phonic.getFileNameSerial
import com.simplerapps.phonic.service.InfoActivity.Companion.CONTENT_URI
import com.simplerapps.phonic.servicechooser.ServiceChooserActivity
import com.simplerapps.phonic.shareAudioFile
import com.simplerapps.phonic.showInfoDialog

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

        supportFragmentManager
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
            showInfoDialog(supportFragmentManager,"No files to save!")
            return
        }

        if (alreadySaved) {
            showInfoDialog(supportFragmentManager,"Already saved!")
            return
        }

        val externalName = getToSaveName()
        val externalUri = getExternalOutUri(externalName)

        if (externalUri == null) {
            showInfoDialog(supportFragmentManager,"Failed to save!")
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
            supportFragmentManager,
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
                supportFragmentManager,
                title = "Please save before share!",
                message = "click on the \"SAVE TO STORAGE\" button to save."
            )

            return
        }

        shareAudioFile(uri = FileInfoManager.savedFileUri!!, name = FileInfoManager.savedFileName)
    }

    private fun getToSaveName(): String {
        var name = "Audio_Converter${getFileNameSerial()}.m4a"
        FileInfoManager.fileName?.let {
            name = "${it}_${name}"
        }

        return name
    }

/*    private fun showInfoDialog(title: String? = null, message: String? = null) {
        val processResultDialog = ProcessResultDialog(title, message)
        processResultDialog.show(supportFragmentManager, null)
    }*/
}
package com.simplerapps.phonic.share

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback
import com.simplerapps.phonic.*
import com.simplerapps.phonic.common.FileInfoManager
import com.simplerapps.phonic.databinding.ActivityShareBinding
import com.simplerapps.phonic.repository.AudioFileModel
import com.simplerapps.phonic.repository.MyFolderRepo
import com.simplerapps.phonic.service.InfoActivity
import com.simplerapps.phonic.service.InfoActivity.Companion.CONTENT_URI
import com.simplerapps.phonic.servicechooser.Service
import com.simplerapps.phonic.servicechooser.ServiceChooserActivity
import java.io.File

class ShareActivity : AppCompatActivity() {

    companion object {
        const val ALREADY_SAVED = "already_saved"
        const val PERMISSION_REQUEST_CODE = 111
    }

    private lateinit var viewBinding: ActivityShareBinding
    private lateinit var exoplayer: ExoPlayer
    private lateinit var uri: String
    private var alreadySaved = false
    private lateinit var myFolderRepo: MyFolderRepo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityShareBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        showRewardedAd()
        //showRewardedInterstitialAd()

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
        }

        viewBinding.btSaveToStorage.setOnClickListener {
            exoplayer.pause()
            saveToExternalStorage(uri)
        }

        viewBinding.btShare.setOnClickListener {
            exoplayer.pause()
            onShareClick()
        }

        myFolderRepo = MyFolderRepo(applicationContext)
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
            R.id.my_folder -> {
                goToMyFolderScreen()
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

    private fun goToMyFolderScreen() {
        val intent = Intent(this, InfoActivity::class.java)
        intent.putExtra(InfoActivity.SERVICE_ID, Service.MY_FOLDER.serviceId)
        startActivity(intent)
        finishAffinity()
    }

    private fun saveToExternalStorage(uri: String?) {
        if (uri == null) {
            showInfoDialog(supportFragmentManager, "No files to save!")
            return
        }

        if (alreadySaved) {
            showInfoDialog(supportFragmentManager, "Already saved!")
            return
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (needsPermission()) {
                requestPermission()
                return
            }
        }

        val externalName = getToSaveName()
        val externalUri = getExternalOutUri(externalName)

        if (externalUri == null) {
            showInfoDialog(supportFragmentManager, "Failed to save!")
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

        addToMyFolderList(
            AudioFileModel(
                uri = FileInfoManager.savedFileUri.toString(),
                displayName = FileInfoManager.savedFileName
            )
        )
    }

    private fun addToMyFolderList(audioFileModel: AudioFileModel) {
        myFolderRepo.addToAudioList(
            audioFileModel
        )
    }

    private fun getExternalOutUri(name: String): Uri? {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            val externalDir = File(Environment.getExternalStorageDirectory(), "Music")
            if (!externalDir.exists()) {
                externalDir.mkdir()
            }

            val toSaveFile = File(externalDir.absolutePath, name)
            return Uri.fromFile(toSaveFile)
        }

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
        var name = "${resources.getString(R.string.app_name)}_${getFileCreateTime()}.m4a"
        FileInfoManager.fileName?.let {
            name = "${it}_${name}"
        }

        return name
    }

    private fun needsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_DENIED
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
            PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showInfoDialog(
                    supportFragmentManager,
                    "Ready to save!",
                    "Congratulations! you granted save permission! you can now save your audio! click on the \"SAVE TO STORAGE\" button to save."
                )
            }
        }
    }

    private fun showRewardedAd() {
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(
            this, resources.getString(R.string.share_start_rewarded_ad_id),
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    super.onAdFailedToLoad(error)

                    LogD(error.message)
                }

                override fun onAdLoaded(ad: RewardedAd) {
                    super.onAdLoaded(ad)

                    ad.show(
                        this@ShareActivity,
                        OnUserEarnedRewardListener {
                            LogD(it.amount.toString())
                        }
                    )
                }
            }
        )
    }
}
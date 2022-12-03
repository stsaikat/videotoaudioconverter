package com.simplerapps.phonic.service

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.simplerapps.phonic.Range
import com.simplerapps.phonic.common.FileInfoManager
import com.simplerapps.phonic.common.ProgressListener
import com.simplerapps.phonic.databinding.ActivityInfoBinding
import com.simplerapps.phonic.servicechooser.Service
import com.simplerapps.phonic.share.ShareActivity
import com.simplerapps.phonic.showInfoDialog
import com.simplerapps.phonic.tools.AudioConverter
import java.io.File
import kotlin.concurrent.thread

class InfoActivity : AppCompatActivity(), VideoToAudioFragment.Listener,
    EditAudioFragment.Listener, ProgressListener {
    companion object {
        const val SERVICE_ID = "service_id"
        const val CONTENT_URI = "content_uri"
    }

    private lateinit var viewBinding: ActivityInfoBinding
    private val convertProgressDialog = ConvertProgressDialog()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityInfoBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        val serviceId = intent.getIntExtra(SERVICE_ID, -1)

        when (Service.getServiceById(serviceId)) {
            Service.VIDEO_TO_AUDIO -> {
                FileInfoManager.fileUri?.let {
                    showVideoToAudioFragment(it.toString())
                }
            }
            Service.EDIT_AUDIO -> {
                FileInfoManager.fileUri?.let {
                    showEditAudioFragment(it.toString())
                }
            }
            Service.MERGE_AUDIO -> {

            }
            Service.MY_FOLDER -> {
                showMyFolderFragment()
            }
            null -> {

            }
        }
        convertProgressDialog.isCancelable = false
    }

    private fun showVideoToAudioFragment(uri: String?) {
        uri?.let {
            val videoToAudioFragment = VideoToAudioFragment(it, this)
            showFragment(videoToAudioFragment)
        }
    }

    private fun showEditAudioFragment(uri: String?) {
        uri?.let {
            val editAudioFragment = EditAudioFragment(it, this)
            showFragment(editAudioFragment)
        }
    }

    private fun showMyFolderFragment() {
        val myFolderFragment = MyFolderFragment()
        showFragment(myFolderFragment)
    }

    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace(viewBinding.fragmentContainer.id, fragment)
        }
    }

    override fun convertVideoToAudio(uri: String, trim: Range?, volume: Int?) {
        convertProgressDialog.show(supportFragmentManager, null)

        thread(start = true) {
            val outFile =
                File(applicationContext.cacheDir.absolutePath + FileInfoManager.cacheName)
            val outUri = Uri.fromFile(outFile)

            val audioConverter = AudioConverter(
                this,
                Uri.parse(uri),
                outUri,
                this,
                trim,
                volume
            )

            audioConverter.convert()
        }

    }

    override fun editAudio(uri: String, trim: Range?, volume: Int?) {
        convertProgressDialog.show(supportFragmentManager, null)

        thread(start = true) {
            val outFile =
                File(applicationContext.cacheDir.absolutePath + FileInfoManager.cacheName)
            val outUri = Uri.fromFile(outFile)

            val audioTranscoder = AudioConverter(
                this,
                Uri.parse(uri),
                outUri,
                this,
                trim,
                volume
            )

            audioTranscoder.convert()
        }
    }

    override fun onProgress(progress: Int) {
        convertProgressDialog.setProgress(progress)
    }

    override fun onFinish(uri: String) {
        convertProgressDialog.dismiss()
        val intent = Intent(this@InfoActivity, ShareActivity::class.java)
        intent.putExtra(CONTENT_URI, uri)
        startActivity(intent)
        finish()
    }

    override fun onFailed(message: String) {
        convertProgressDialog.dismiss()
        showInfoDialog(supportFragmentManager, title = "Error!", message = message)
    }
}
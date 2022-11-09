package com.simplerapps.videotoaudio.service

import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.simplerapps.videotoaudio.LogD
import com.simplerapps.videotoaudio.R
import com.simplerapps.videotoaudio.common.FileInfoManager
import com.simplerapps.videotoaudio.databinding.ActivityInfoBinding
import com.simplerapps.videotoaudio.servicechooser.Service
import com.simplerapps.videotoaudio.share.ShareActivity
import java.io.File
import kotlin.concurrent.thread

class InfoActivity : AppCompatActivity(), VideoToAudioInfoFragment.Listener {
    companion object {
        const val SERVICE_ID = "service_id"
        const val CONTENT_URI = "content_uri"
    }

    private lateinit var viewBinding: ActivityInfoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityInfoBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        val serviceId = intent.getIntExtra(SERVICE_ID, -1)
        val service = Service.getServiceById(serviceId)

        when (service) {
            Service.VIDEO_TO_AUDIO -> {
                FileInfoManager.fileUri?.let {
                    showVideoToAudioFragment(it.toString())
                }
            }
            Service.EDIT_AUDIO -> {

            }
            Service.MERGE_AUDIO -> {

            }
            Service.MY_FOLDER -> {

            }
            null -> {

            }
        }
    }

    private fun showVideoToAudioFragment(uri: String?) {
        uri?.let {
            val videoToAudioInfoFragment = VideoToAudioInfoFragment(it, this)
            showFragment(videoToAudioInfoFragment)
        }
    }

    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace(viewBinding.fragmentContainer.id, fragment)
        }
    }

    override fun convertVideoToAudio(uri: String) {
        val convertProgressDialog = ConvertProgressDialog()
        convertProgressDialog.show(supportFragmentManager, null)
        convertProgressDialog.isCancelable = false

        thread(start = true) {
            val outFile =
                File(applicationContext.cacheDir.absolutePath + FileInfoManager.cacheName)
            val outUri = Uri.fromFile(outFile)

            val videoToAudioConverter = VideoToAudioConverter(
                this,
                Uri.parse(uri),
                outUri,
                object : VideoToAudioConverter.Listener {
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
                }
            )

            videoToAudioConverter.convert()
        }

    }
}
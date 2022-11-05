package com.simplerapps.videotoaudio.service

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.simplerapps.videotoaudio.LogD
import com.simplerapps.videotoaudio.R
import com.simplerapps.videotoaudio.databinding.ActivityInfoBinding
import com.simplerapps.videotoaudio.servicechooser.Service

class InfoActivity : AppCompatActivity() {
    companion object {
        const val SERVICE_ID = "service_id"
        const val CONTENT_URI = "content_uri"
    }

    private lateinit var viewBinding: ActivityInfoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityInfoBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        val serviceId = intent.getIntExtra(SERVICE_ID,-1)
        val service = Service.getServiceById(serviceId)

        when(service) {
            Service.VIDEO_TO_AUDIO -> {
                showVideoToAudioFragment(
                    intent.getStringExtra(CONTENT_URI)
                )
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
        LogD("$uri")
        uri?.let {
            val videoToAudioInfoFragment = VideoToAudioInfoFragment(it)
            showFragment(videoToAudioInfoFragment)
        }
    }

    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace(viewBinding.fragmentContainer.id, fragment)
        }
    }
}
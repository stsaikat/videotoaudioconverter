package com.simplerapps.videotoaudio.servicechooser

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.recyclerview.widget.GridLayoutManager
import com.simplerapps.videotoaudio.LogD
import com.simplerapps.videotoaudio.databinding.ActivityServiceChooserBinding
import com.simplerapps.videotoaudio.service.InfoActivity
import com.simplerapps.videotoaudio.service.InfoActivity.Companion.CONTENT_URI
import com.simplerapps.videotoaudio.service.InfoActivity.Companion.SERVICE_ID

class ServiceChooserActivity : AppCompatActivity(), ServicesAdapter.ItemClickListener {
    companion object {
        const val VIDEO_REQUEST_CODE = 0
    }

    private lateinit var viewBinding: ActivityServiceChooserBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityServiceChooserBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        initAllViews()
        initAllListeners()
    }

    private fun initAllViews() {
        initRecyclerView()
    }

    private fun initAllListeners() {

    }

    private fun initRecyclerView() {
        val serviceAdapter = ServicesAdapter(
            getServicesList(),
            this
        )
        viewBinding.rvServices.layoutManager = GridLayoutManager(
            this, 2, GridLayoutManager.VERTICAL, false
        )
        viewBinding.rvServices.adapter = serviceAdapter
    }

    private fun getServicesList() = arrayListOf(
        Service.VIDEO_TO_AUDIO,
/*        Service.EDIT_AUDIO,
        Service.MERGE_AUDIO,
        Service.MY_FOLDER*/
    )

    override fun onItemClick(service: Service) {
        when(service) {
            Service.VIDEO_TO_AUDIO -> {
                pickVideo()
            }
            Service.EDIT_AUDIO -> {

            }
            Service.MERGE_AUDIO -> {

            }
            Service.MY_FOLDER -> {

            }
        }
    }

    private fun pickVideo() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, VIDEO_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            val intent = Intent(this,InfoActivity::class.java)
            intent.putExtra(CONTENT_URI,data!!.data.toString())
            when(requestCode) {
                VIDEO_REQUEST_CODE -> {
                    intent.putExtra(SERVICE_ID,Service.VIDEO_TO_AUDIO.serviceId)
                }
            }

            startActivity(intent)
        }
    }
}
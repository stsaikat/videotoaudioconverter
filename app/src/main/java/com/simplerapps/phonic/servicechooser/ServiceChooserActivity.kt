package com.simplerapps.phonic.servicechooser

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.gms.ads.AdRequest
import com.simplerapps.phonic.common.FileInfoManager
import com.simplerapps.phonic.databinding.ActivityServiceChooserBinding
import com.simplerapps.phonic.service.InfoActivity
import com.simplerapps.phonic.service.InfoActivity.Companion.SERVICE_ID

class ServiceChooserActivity : AppCompatActivity(), ServicesAdapter.ItemClickListener {
    companion object {
        const val VIDEO_REQUEST_CODE = 0
        const val AUDIO_REQUEST_CODE = 1
    }

    private lateinit var viewBinding: ActivityServiceChooserBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityServiceChooserBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        viewBinding.adBannerServiceChooser.loadAd(
            AdRequest.Builder().build()
        )

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
        Service.EDIT_AUDIO,
        Service.MY_FOLDER
    )

    override fun onItemClick(service: Service) {
        when (service) {
            Service.VIDEO_TO_AUDIO -> {
                pickVideo()
            }
            Service.EDIT_AUDIO -> {
                pickAudio()
            }
            Service.MERGE_AUDIO -> {

            }
            Service.MY_FOLDER -> {
                goToInfoActivity(Service.MY_FOLDER)
            }
        }
    }

    private fun pickVideo() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, VIDEO_REQUEST_CODE)
    }

    private fun pickAudio() {
        val intent = Intent()
        intent.type = "audio/*"
        intent.action = Intent.ACTION_OPEN_DOCUMENT
        startActivityForResult(Intent.createChooser(intent,"Select Audio"), AUDIO_REQUEST_CODE)
    }

    @Deprecated("")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when(requestCode) {
                VIDEO_REQUEST_CODE -> {
                    data?.let { intent ->
                        intent.data?.let {
                            processChosenVideoUri(it)
                        }
                    }
                }
                AUDIO_REQUEST_CODE -> {
                    data?.let { intent ->
                        intent.data?.let {
                            processChosenAudioUri(it)
                        }
                    }
                }
            }
        }
    }

    private fun processChosenVideoUri(uri: Uri) {
        FileInfoManager.fileUri = uri
        contentResolver.query(uri, null, null, null, null)?.use {
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)

            it.moveToFirst()

            FileInfoManager.fileName = getMp4RemovedName(it.getString(nameIndex))
            FileInfoManager.fileSize = it.getLong(sizeIndex)
        }

        goToInfoActivity(Service.VIDEO_TO_AUDIO)
    }

    private fun processChosenAudioUri(uri: Uri) {
        FileInfoManager.fileUri = uri
        contentResolver.query(uri, null, null, null, null)?.use {
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)

            it.moveToFirst()

            FileInfoManager.fileName = getMp4RemovedName(it.getString(nameIndex))
            FileInfoManager.fileSize = it.getLong(sizeIndex)
        }

        goToInfoActivity(Service.EDIT_AUDIO)
    }

    private fun getMp4RemovedName(name: String) : String {
        return name.removeSuffix(".mp4").removeSuffix(".m4a").removeSuffix(".mp3")
    }

    private fun goToInfoActivity(withService: Service) {
        val intent = Intent(this, InfoActivity::class.java)
        intent.putExtra(SERVICE_ID, withService.serviceId)
        startActivity(intent)
    }
}
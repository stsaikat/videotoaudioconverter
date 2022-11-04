package com.simplerapps.videotoaudio.servicechooser

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.GridLayoutManager
import com.simplerapps.videotoaudio.LogD
import com.simplerapps.videotoaudio.databinding.ActivityServiceChooserBinding

class ServiceChooserActivity : AppCompatActivity(), ServicesAdapter.ItemClickListener {
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
        Service.EDIT_AUDIO,
        Service.MERGE_AUDIO,
        Service.MY_FOLDER
    )

    override fun onItemClick(service: Service) {
        LogD("$service")
    }
}
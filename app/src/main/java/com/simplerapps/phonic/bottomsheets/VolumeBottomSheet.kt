package com.simplerapps.phonic.bottomsheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.simplerapps.phonic.databinding.BottomSheetVolumeBinding
import com.simplerapps.phonic.view.VolumeProgressBarView

class VolumeBottomSheet(private val initialVolume: Int? = null, val listener: VolumeListener) :
    BottomSheetDialogFragment(), VolumeProgressBarView.OnVolumeChangedListener {

    private lateinit var viewBinding: BottomSheetVolumeBinding
    private var volume: Int? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewBinding = BottomSheetVolumeBinding.inflate(inflater, container, false)
        return viewBinding.root
    }

    override fun onStart() {
        super.onStart()
        initListeners()
        if (initialVolume != null) {
            volume = initialVolume
            viewBinding.cvVolumeProgressBar.currentValue = initialVolume
        }
        else {
            viewBinding.cvVolumeProgressBar.currentValue = 100
            volume = 100
        }
        isCancelable = false
    }

    private fun initListeners() {
        viewBinding.tvbVolumeDone.setOnClickListener {
            listener.onVolumeChange(volume)
            dismiss()
        }

        viewBinding.tvbVolumeCancel.setOnClickListener {
            dismiss()
        }
        viewBinding.cvVolumeProgressBar.setVolumeListener(this)
    }

    interface VolumeListener {
        fun onVolumeChange(volume: Int?)
    }

    override fun onVolumeChange(volume: Int) {
        this.volume = volume
        "$volume%".also { viewBinding.tvChoosenVolume.text = it }
    }
}
package com.simplerapps.phonic.bottomsheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.simplerapps.phonic.databinding.BottomSheetVolumeBinding

class VolumeBottomSheet(val initialVolume: Int? = null, val listener: VolumeListener) :
    BottomSheetDialogFragment(), SeekBar.OnSeekBarChangeListener {

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
            viewBinding.sbVolumeChange.progress = initialVolume
            volume = initialVolume
        }
        else {
            viewBinding.sbVolumeChange.progress = 100
            volume = 100
        }
    }

    private fun initListeners() {
        viewBinding.tvbVolumeDone.setOnClickListener {
            listener.onVolumeChange(volume)
            dismiss()
        }

        viewBinding.tvbVolumeCancel.setOnClickListener {
            dismiss()
        }
        viewBinding.sbVolumeChange.setOnSeekBarChangeListener(this)
    }

    override fun onProgressChanged(seekBar: SeekBar?, volume: Int, fromUser: Boolean) {
        if (fromUser) {
            this.volume = volume
        }
        viewBinding.tvChoosenVolume.text = "$volume%"
    }

    override fun onStartTrackingTouch(p0: SeekBar?) {

    }

    override fun onStopTrackingTouch(p0: SeekBar?) {

    }

    interface VolumeListener {
        fun onVolumeChange(volume: Int?)
    }
}
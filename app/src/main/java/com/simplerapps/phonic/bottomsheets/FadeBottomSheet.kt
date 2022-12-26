package com.simplerapps.phonic.bottomsheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.simplerapps.phonic.databinding.BottomSheetFadeBinding

class FadeBottomSheet(
    private var fadeInMs: Int = 0,
    private var fadeOutMs: Int = 0,
    private val listener: FadeListener
) : BottomSheetDialogFragment(), SeekBar.OnSeekBarChangeListener {
    private lateinit var viewBinding: BottomSheetFadeBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewBinding = BottomSheetFadeBinding.inflate(inflater, container, false)
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewBinding.tvbFadeDone.setOnClickListener {
            listener.onFadeChanged(fadeInMs, fadeOutMs)
            dismiss()
        }

        viewBinding.tvbFadeCancel.setOnClickListener {
            dismiss()
        }

        viewBinding.sbFadeIn.setOnSeekBarChangeListener(this)
        viewBinding.sbFadeOut.setOnSeekBarChangeListener(this)

        viewBinding.sbFadeIn.progress = fadeInMs
        viewBinding.sbFadeOut.progress = fadeOutMs
    }

    private fun getFormattedFadeTime(timeMs: Int) : String {
        return "${String.format("%.1f",timeMs / 1000f)}s"
    }

    interface FadeListener {
        fun onFadeChanged(fadeInMs: Int, fadeOutMs: Int)
    }

    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        seekBar?.let { sb ->
            when(sb.id) {
                viewBinding.sbFadeIn.id -> {
                    fadeInMs = progress
                    viewBinding.tvFadeInDuration.text = getFormattedFadeTime(fadeInMs)
                }
                viewBinding.sbFadeOut.id -> {
                    fadeOutMs = progress
                    viewBinding.tvFadeOutDuration.text = getFormattedFadeTime(fadeOutMs)
                }
                else -> {
                    // ignore
                }
            }
        }
    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) {

    }

    override fun onStopTrackingTouch(seekBar: SeekBar?) {

    }
}
package com.simplerapps.phonic.bottomsheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.innovattic.rangeseekbar.RangeSeekBar
import com.simplerapps.phonic.Range
import com.simplerapps.phonic.databinding.BottomSheetTrimBinding
import com.simplerapps.phonic.getFormattedTrimTimeText
import kotlin.math.max

class TrimBottomSheet(
    private val totalDuration: Long,
    private val currentTrim: Range?,
    private val listener: TrimUpdateListener
) :
    BottomSheetDialogFragment(), RangeSeekBar.SeekBarChangeListener {

    private lateinit var viewBinding: BottomSheetTrimBinding
    private var audioTrimRange: Range? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewBinding = BottomSheetTrimBinding.inflate(inflater, container, false)
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewBinding.trimPointStart.tvTrimPointName.text = "start"
        viewBinding.trimPointEnd.tvTrimPointName.text = "end"
    }

    override fun onStart() {
        super.onStart()
        initValues()
        initButtonListeners()
    }

    private fun initValues() {
        initSlider()
        currentTrim?.let {
            inflateCurrentTrim(it)
        }
    }

    private fun inflateCurrentTrim(trim: Range) {
        viewBinding.rsbTrim.setMinThumbValue(trim.from)
        viewBinding.rsbTrim.setMaxThumbValue(trim.to)
    }

    private fun initButtonListeners() {

        viewBinding.tvbTrimCancel.setOnClickListener {
            dismiss()
        }

        viewBinding.tvbTrimDone.setOnClickListener {
            listener.onTrimUpdate(audioTrimRange)
            dismiss()
        }

        viewBinding.trimPointStart.ibTrimPointMinus.setOnClickListener {
            viewBinding.rsbTrim.setMinThumbValue(
                max(0, viewBinding.rsbTrim.getMinThumbValue() - 100)
            )
        }
        viewBinding.trimPointStart.ibTrimPointPlus.setOnClickListener {
            viewBinding.rsbTrim.setMinThumbValue(
                Integer.min(
                    viewBinding.rsbTrim.getMaxThumbValue(),
                    viewBinding.rsbTrim.getMinThumbValue() + 100
                )
            )
        }
        viewBinding.trimPointEnd.ibTrimPointMinus.setOnClickListener {
            viewBinding.rsbTrim.setMaxThumbValue(
                max(
                    viewBinding.rsbTrim.getMinThumbValue(),
                    viewBinding.rsbTrim.getMaxThumbValue() - 100
                )
            )
        }
        viewBinding.trimPointEnd.ibTrimPointPlus.setOnClickListener {
            viewBinding.rsbTrim.setMaxThumbValue(
                Integer.min(
                    viewBinding.rsbTrim.max,
                    viewBinding.rsbTrim.getMaxThumbValue() + 100
                )
            )
        }
    }

    private fun initSlider() {
        viewBinding.rsbTrim.seekBarChangeListener = this
        viewBinding.rsbTrim.max = (totalDuration / 1000).toInt()
        processTrimTime(0, (totalDuration / 1000).toInt())
    }

    private fun processTrimTime(startMs: Int, endMs: Int) {
        audioTrimRange = Range(startMs, endMs)
        setStartText(startMs)
        setEndText(endMs)
        setDurationText(endMs - startMs)
    }

    private fun setStartText(startMs: Int) {
        viewBinding.trimPointStart.tvTrimPointTime.text = getFormattedTrimTimeText(startMs)
    }

    private fun setEndText(endMs: Int) {
        viewBinding.trimPointEnd.tvTrimPointTime.text = getFormattedTrimTimeText(endMs)
    }

    private fun setDurationText(duration: Int) {
        viewBinding.tvTrimDuration.text = getFormattedTrimTimeText(duration)
    }

    override fun onStartedSeeking() {

    }

    override fun onStoppedSeeking() {

    }

    /**
     * minThumbValue = start time ms
     * maxThumbValue = end time ms
     */
    override fun onValueChanged(minThumbValue: Int, maxThumbValue: Int) {
        processTrimTime(minThumbValue, maxThumbValue)
    }

    interface TrimUpdateListener {
        fun onTrimUpdate(trim: Range?)
    }
}
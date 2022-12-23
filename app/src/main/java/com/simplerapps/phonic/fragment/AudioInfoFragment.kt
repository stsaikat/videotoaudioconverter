package com.simplerapps.phonic.fragment

import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.simplerapps.phonic.R
import com.simplerapps.phonic.TrimRange
import com.simplerapps.phonic.bottomsheets.TrimBottomSheet
import com.simplerapps.phonic.bottomsheets.VolumeBottomSheet
import com.simplerapps.phonic.common.FileInfoManager
import com.simplerapps.phonic.databinding.FragmentAudioInfoBinding
import com.simplerapps.phonic.getFormattedTrimTimeText

class AudioInfoFragment(private val uri: Uri) : Fragment(R.layout.fragment_audio_info),
    TrimBottomSheet.TrimUpdateListener, VolumeBottomSheet.VolumeListener {

    private lateinit var viewBinding: FragmentAudioInfoBinding
    private var trim: TrimRange? = null
    private var volume: Int? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewBinding = FragmentAudioInfoBinding.inflate(inflater, container, false)
        return viewBinding.root
    }

    override fun onStart() {
        super.onStart()
        setButtonListeners()
    }

    private fun setButtonListeners() {
        viewBinding.btTrim.setOnClickListener {
            showTrimBottomSheet()
        }

        viewBinding.btVolume.setOnClickListener {
            showVolumeBottomSheet()
        }
    }

    private fun showTrimBottomSheet() {
        getAudioDuration()?.let {
            val trimBottomSheet = TrimBottomSheet(it, trim, this)
            trimBottomSheet.show(childFragmentManager, null)
        }
    }

    private fun showVolumeBottomSheet() {
        val volumeBottomSheet = VolumeBottomSheet(volume, this)
        volumeBottomSheet.show(childFragmentManager, null)
    }

    private fun getAudioDuration(): Long? {
        var duration: Long? = null

        val extractor = MediaExtractor()
        val pfd = requireContext().contentResolver.openFileDescriptor(uri, "r")!!
        extractor.setDataSource(pfd.fileDescriptor)
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME)!!
            if (mime.startsWith("audio/")) {
                duration = format.getLong(MediaFormat.KEY_DURATION)
            }
        }

        pfd.close()
        extractor.release()
        return duration
    }

    override fun onTrimUpdate(trim: TrimRange?) {
        trim?.let {
            this.trim = it
            viewBinding.tvTrimChoosenDuration.visibility = View.VISIBLE
            viewBinding.tvTrimChoosenDuration.text =
                getFormattedTrimTimeText(it.to - it.from)
            FileInfoManager.trim = it
        }
    }

    override fun onVolumeChange(volume: Int?) {
        volume?.let {
            this.volume = it
            viewBinding.tvTrimChoosenVolume.visibility = View.VISIBLE
            viewBinding.tvTrimChoosenVolume.text = "$it%"
            FileInfoManager.volume = it
        }
    }
}
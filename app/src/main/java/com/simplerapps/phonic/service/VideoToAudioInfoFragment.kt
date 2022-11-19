package com.simplerapps.phonic.service

import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.innovattic.rangeseekbar.RangeSeekBar
import com.simplerapps.phonic.R
import com.simplerapps.phonic.Range
import com.simplerapps.phonic.databinding.FragmentVideoToAudioInfoBinding
import java.lang.Integer.min
import kotlin.math.max

class VideoToAudioInfoFragment(private val uri: String, private val listener: Listener) :
    Fragment(R.layout.fragment_video_to_audio_info), Player.Listener,
    RangeSeekBar.SeekBarChangeListener {

    private lateinit var exoplayer: ExoPlayer
    private lateinit var viewBinding: FragmentVideoToAudioInfoBinding
    private var audioTrimRange: Range? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewBinding = FragmentVideoToAudioInfoBinding.inflate(inflater, container, false)
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewBinding.btConvert.setOnClickListener {
            exoplayer.pause()
            listener.convertVideoToAudio(uri, audioTrimRange)
        }


    }

    override fun onStart() {
        super.onStart()
        initSlider()
        initializePlayer(uri)
        initButtonListeners()
    }

    private fun initButtonListeners() {
        viewBinding.ibStartMinus.setOnClickListener {
            viewBinding.rsTrim.setMinThumbValue(
                max(0, viewBinding.rsTrim.getMinThumbValue() - 100)
            )
        }
        viewBinding.ibStartAdd.setOnClickListener {
            viewBinding.rsTrim.setMinThumbValue(
                min(
                    viewBinding.rsTrim.getMaxThumbValue(),
                    viewBinding.rsTrim.getMinThumbValue() + 100
                )
            )
        }
        viewBinding.ibEndMinus.setOnClickListener {
            viewBinding.rsTrim.setMaxThumbValue(
                max(
                    viewBinding.rsTrim.getMinThumbValue(),
                    viewBinding.rsTrim.getMaxThumbValue() - 100
                )
            )
        }
        viewBinding.ibEndAdd.setOnClickListener {
            viewBinding.rsTrim.setMaxThumbValue(
                min(
                    viewBinding.rsTrim.max,
                    viewBinding.rsTrim.getMaxThumbValue() + 100
                )
            )
        }
    }

    private fun initSlider() {
        val duration = getAudioDuration()
        duration?.let {
            viewBinding.rsTrim.seekBarChangeListener = this
            viewBinding.rsTrim.max = (duration / 1000).toInt()
            processTrimTime(0, (duration / 1000).toInt())
        }

        if (duration == null) {
            viewBinding.clTrimCompo.visibility = View.GONE
        }
    }

    private fun getAudioDuration(): Long? {
        var duration: Long? = null

        val extractor = MediaExtractor()
        val pfd = requireContext().contentResolver.openFileDescriptor(Uri.parse(uri), "r")!!
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

    override fun onStop() {
        super.onStop()
        releasePlayer()
    }

    private fun initializePlayer(uri: String) {
        exoplayer = ExoPlayer.Builder(requireContext()).build()
        val mediaItem = MediaItem.fromUri(uri)
        exoplayer.addMediaItem(mediaItem)
        viewBinding.exoVideoPlayer.player = exoplayer
        exoplayer.playWhenReady = true
        exoplayer.addListener(this)
        exoplayer.prepare()
    }

    private fun releasePlayer() {
        exoplayer.release()
    }

    override fun onPlayerError(error: PlaybackException) {
        super.onPlayerError(error)
        Toast.makeText(requireContext(), "Can't play the video", Toast.LENGTH_SHORT).show()
    }

    interface Listener {
        fun convertVideoToAudio(uri: String, trim: Range?)
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

    private fun processTrimTime(startMs: Int, endMs: Int) {
        audioTrimRange = Range(startMs, endMs)
        setStartText(startMs)
        setEndText(endMs)
        setDurationText(endMs - startMs)
    }

    private fun setStartText(startMs: Int) {
        viewBinding.tvStart.text = getFormattedTimeText(startMs)
    }

    private fun setEndText(endMs: Int) {
        viewBinding.tvEnd.text = getFormattedTimeText(endMs)
    }

    private fun setDurationText(duration: Int) {
        viewBinding.tvDuration.text = "duration : ${getFormattedTimeText(duration)}"
    }

    private fun getFormattedTimeText(time: Int): String {
        val ss = (time % 1000) / 100
        val seconds = time / 1000
        val s = seconds % 60
        val m = (seconds / 60) % 60
        val h = (seconds / (60 * 60)) % 24
        return String.format("%02d:%02d:%02d.%01d", h, m, s, ss)
    }
}
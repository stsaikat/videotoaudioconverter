package com.simplerapps.videotoaudio

import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.simplerapps.videotoaudio.databinding.FragmentConvertProcessBinding

class ConvertProcessFragment(private val uri: Uri, private val listener: Listener) :
    Fragment(R.layout.fragment_convert_process), SeekBar.OnSeekBarChangeListener {

    private val UPDATE_CURRENT_POS = 1
    private val INITIAL_VOLUME = 25

    private lateinit var viewBinding: FragmentConvertProcessBinding
    private val mediaPlayer = MediaPlayer()
    private var beforeMuteVolume = INITIAL_VOLUME
    private val progressUpdateHandler = Handler(Looper.getMainLooper()) {
        Log.d("xyz", "sdf : ${it.what}")
        if (it.what == UPDATE_CURRENT_POS) {
            if (mediaPlayer.isPlaying) {
                viewBinding.sbCurrPos.progress = mediaPlayer.currentPosition
                sendProgressHandlerUpdate()
            }
        }
        return@Handler true
    }

    private fun sendProgressHandlerUpdate() {
        progressUpdateHandler.postDelayed({
            UPDATE_CURRENT_POS
        }, 250)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewBinding = FragmentConvertProcessBinding.inflate(inflater, container, false)
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setInitialCondition()
        setViewListeners()

        setMediaPlayer(uri)
    }

    private fun setViewListeners() {
        viewBinding.sbVolume.setOnSeekBarChangeListener(this)
        viewBinding.sbCurrPos.setOnSeekBarChangeListener(this)
        viewBinding.ibtHome.setOnClickListener {
            listener.onButtonClick(it)
        }
        viewBinding.ibtPlayPause.setOnClickListener {
            if (mediaPlayer.isPlaying) {
                mediaPlayer.pause()
            } else {
                mediaPlayer.start()
            }
            setPlayPauseViews()
        }
        viewBinding.ibtReplay.setOnClickListener {
            mediaPlayer.seekTo(0)
            mediaPlayer.start()
            setPlayPauseViews()
            sendProgressHandlerUpdate()
        }
        viewBinding.ibtMute.setOnClickListener {
            if (viewBinding.sbVolume.progress == 0) {
                viewBinding.sbVolume.progress = beforeMuteVolume
            } else {
                beforeMuteVolume = viewBinding.sbVolume.progress
                viewBinding.sbVolume.progress = 0
            }
        }
    }

    private fun setInitialCondition() {
        viewBinding.pbConversion.isIndeterminate = true
        viewBinding.sbVolume.max = 100
        viewBinding.sbVolume.progress = INITIAL_VOLUME

        setVolumeViews()
        setPlayPauseViews()
    }

    private fun setVolumeViews() {
        if (viewBinding.sbVolume.progress == 0) {
            viewBinding.ibtMute.setImageDrawable(
                ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.ic_baseline_volume_off_24
                )
            )
        } else if (viewBinding.sbVolume.progress < 50) {
            viewBinding.ibtMute.setImageDrawable(
                ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.ic_baseline_volume_down_24
                )
            )
        } else {
            viewBinding.ibtMute.setImageDrawable(
                ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.ic_baseline_volume_up_24
                )
            )
        }
    }

    private fun setPlayPauseViews() {
        if (mediaPlayer.isPlaying) {
            viewBinding.ibtPlayPause.setImageDrawable(
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_baseline_pause_24)
            )
        } else {
            viewBinding.ibtPlayPause.setImageDrawable(
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_baseline_play_arrow_24)
            )
        }
    }

    private fun setMediaPlayer(uri: Uri) {
        mediaPlayer.setDataSource(requireContext(), uri)
        mediaPlayer.setOnPreparedListener {
            it.start()
            viewBinding.sbCurrPos.max = it.duration
            setPlayPauseViews()
            sendProgressHandlerUpdate()
            viewBinding.sbVolume.progress = INITIAL_VOLUME
        }
        mediaPlayer.setScreenOnWhilePlaying(true)
        mediaPlayer.prepare()
    }

    interface Listener {
        fun onButtonClick(view: View)
    }

    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, isUser: Boolean) {
        seekBar?.let {
            when (it.id) {
                viewBinding.sbVolume.id -> {
                    Log.d("xyz", "onProgressChanged: $progress")
                    setVolumeViews()
                    mediaPlayer.setVolume(progress / 100f, progress / 100f)
                }
                viewBinding.sbCurrPos.id -> {
                    Log.d("xyz", "onProgressChanged: $progress")
                    if (isUser) {
                        mediaPlayer.seekTo(progress)
                    }
                }
            }
        }
    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) {

    }

    override fun onStopTrackingTouch(seekBar: SeekBar?) {

    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.stop()
        mediaPlayer.release()
    }
}
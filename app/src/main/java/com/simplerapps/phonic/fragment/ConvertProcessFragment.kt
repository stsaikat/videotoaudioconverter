package com.simplerapps.phonic.fragment

import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.simplerapps.phonic.R
import com.simplerapps.phonic.databinding.FragmentConvertProcessBinding
import com.simplerapps.phonic.utils.convertSecondsToHMmSs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ConvertProcessFragment(private val uri: Uri, private val listener: Listener) :
    Fragment(R.layout.fragment_convert_process), SeekBar.OnSeekBarChangeListener {

    private val UPDATE_CURRENT_POS = 1
    private val INITIAL_VOLUME = 25

    private lateinit var viewBinding: FragmentConvertProcessBinding
    private val mediaPlayer = MediaPlayer()
    private var beforeMuteVolume = INITIAL_VOLUME
    private var observerWaitTime = 500L

    private fun setProgressUpdateObserver() {
        lifecycleScope.launch {
            while (true) {
                if (mediaPlayer.isPlaying) {
                    viewBinding.sbCurrPos.progress = mediaPlayer.currentPosition
                    withContext(Dispatchers.Main) {
                        "${convertSecondsToHMmSs(mediaPlayer.currentPosition / 1000)}/${
                            convertSecondsToHMmSs(
                                mediaPlayer.duration / 1000
                            )
                        }".also { viewBinding.tvPlayProgressTime.text = it }
                    }
                }
                delay(observerWaitTime)
            }
        }
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

        setProgressUpdateObserver()
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
            viewBinding.sbVolume.progress = INITIAL_VOLUME
        }

        mediaPlayer.setOnCompletionListener {
            setPlayPauseViews()
            viewBinding.sbCurrPos.progress = 0
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
                    setVolumeViews()
                    mediaPlayer.setVolume(progress / 100f, progress / 100f)
                }
                viewBinding.sbCurrPos.id -> {
                    if (isUser) {
                        mediaPlayer.seekTo(progress)
                    }
                }
            }
        }
    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) {
        seekBar?.let {
            when (it.id) {
                viewBinding.sbCurrPos.id -> {
                    observerWaitTime = 50L
                }
            }
        }
    }

    override fun onStopTrackingTouch(seekBar: SeekBar?) {
        seekBar?.let {
            when (it.id) {
                viewBinding.sbCurrPos.id -> {
                    observerWaitTime = 500L
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.stop()
        mediaPlayer.release()
    }
}
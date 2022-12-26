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
import androidx.fragment.app.commit
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.simplerapps.phonic.R
import com.simplerapps.phonic.common.FileInfoManager
import com.simplerapps.phonic.databinding.FragmentVideoToAudioInfoBinding
import com.simplerapps.phonic.datamodel.AudioConversionInfo
import com.simplerapps.phonic.fragment.AudioInfoFragment

class VideoToAudioFragment(private val uri: String, private val listener: Listener) :
    Fragment(R.layout.fragment_video_to_audio_info), Player.Listener {

    private lateinit var exoplayer: ExoPlayer
    private lateinit var viewBinding: FragmentVideoToAudioInfoBinding

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
            listener.convertVideoToAudio(
                AudioConversionInfo(
                    uri = Uri.parse(uri),
                    trim = FileInfoManager.trim,
                    volume = FileInfoManager.volume,
                    fadeInMs = FileInfoManager.fadeInMs,
                    fadeOutMs = FileInfoManager.fadeOutMs
                )
            )
        }
    }

    override fun onStart() {
        super.onStart()
        showAudioInfoFragment()
        initializePlayer(uri)
    }

    private fun showAudioInfoFragment() {
        val audioInfoFragment = AudioInfoFragment(Uri.parse(uri))
        showFragment(audioInfoFragment)
    }

    private fun showFragment(fragment: Fragment) {
        childFragmentManager.commit {
            setReorderingAllowed(true)
            replace(viewBinding.fragmentContainerAudioInfo.id, fragment)
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
        fun convertVideoToAudio(audioConversionInfo: AudioConversionInfo)
    }
}
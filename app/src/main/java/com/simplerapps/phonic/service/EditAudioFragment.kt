package com.simplerapps.phonic.service

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
import com.simplerapps.phonic.Range
import com.simplerapps.phonic.common.FileInfoManager
import com.simplerapps.phonic.databinding.FragmentEditAudioBinding
import com.simplerapps.phonic.fragment.AudioInfoFragment

class EditAudioFragment(private val uri: String, private val listener: Listener) :
    Fragment(R.layout.fragment_edit_audio), Player.Listener {

    private lateinit var exoplayer: ExoPlayer
    private lateinit var viewBinding: FragmentEditAudioBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewBinding = FragmentEditAudioBinding.inflate(inflater, container, false)
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewBinding.btProcess.setOnClickListener {
            exoplayer.pause()
            listener.editAudio(uri, FileInfoManager.trim, FileInfoManager.volume)
        }

        viewBinding.exoVideoPlayer.controllerShowTimeoutMs = 0
        viewBinding.exoVideoPlayer.controllerHideOnTouch = false
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
            replace(viewBinding.fragmentAudioInfo.id, fragment)
        }
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
        fun editAudio(uri: String, trim: Range?,volume: Int?)
    }
}
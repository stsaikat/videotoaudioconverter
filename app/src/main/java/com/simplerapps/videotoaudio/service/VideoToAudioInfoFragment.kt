package com.simplerapps.videotoaudio.service

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.simplerapps.videotoaudio.LogD
import com.simplerapps.videotoaudio.R
import java.io.File

class VideoToAudioInfoFragment(private val uri: String) :
    Fragment(R.layout.fragment_video_to_audio_info), Player.Listener {

    private lateinit var exoplayer: ExoPlayer
    private var playbackPosition: Long = 0
    private lateinit var playerView: PlayerView

    private val dataSourceFactory: DataSource.Factory by lazy {
        DefaultDataSourceFactory(requireContext(), "exoplayer-sample")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        playerView = view.findViewById(R.id.exo_video_player)

        view.findViewById<Button>(R.id.bt_convert).setOnClickListener {
            val outFile =
                File(requireContext().applicationContext.cacheDir.absolutePath + "/temp.m4a")
            val outUri = Uri.fromFile(outFile)

            val videoToAudioConverter = VideoToAudioConverter(
                requireContext(),
                Uri.parse(uri),
                outUri,
                object : VideoToAudioConverter.Listener {
                    override fun onProgress(progress: Int) {

                    }

                    override fun onFinish() {
                        initializePlayer(outUri.toString())
                    }
                }
            )

            videoToAudioConverter.convert()
        }
    }

    override fun onStart() {
        super.onStart()
        initializePlayer(uri)
    }

    override fun onStop() {
        super.onStop()
        releasePlayer()
    }

    private fun initializePlayer(uri: String) {
        exoplayer = ExoPlayer.Builder(requireContext()).build()
        val mediaItem = MediaItem.fromUri(uri)
        exoplayer.addMediaItem(mediaItem)
        playerView.player = exoplayer
        exoplayer.playWhenReady = true
        exoplayer.addListener(this)
        exoplayer.prepare()
    }

    private fun releasePlayer() {
        playbackPosition = exoplayer.currentPosition
        exoplayer.release()
    }

    private fun preparePlayer(videoUrl: String, type: String) {
        val uri = Uri.parse(videoUrl)
        val mediaSource = buildMediaSource(uri, type)
        exoplayer.setMediaSource(mediaSource)
        exoplayer.prepare()
    }

    private fun buildMediaSource(uri: Uri, type: String): MediaSource {
        val mediaItem = MediaItem.fromUri(uri)
        return if (type == "dash") {
            DashMediaSource.Factory(dataSourceFactory)
                .createMediaSource(mediaItem)
        } else {
            ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(mediaItem)
        }
    }

    override fun onPlayerError(error: PlaybackException) {
        super.onPlayerError(error)
        LogD("${error.message}")
        Toast.makeText(requireContext(), "Can't play the video", Toast.LENGTH_SHORT).show()
    }

    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        super.onPlayerStateChanged(playWhenReady, playbackState)
    }
}
package com.simplerapps.phonic.service

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import androidx.core.widget.ContentLoadingProgressBar
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerControlView
import com.google.android.exoplayer2.ui.PlayerView
import com.simplerapps.phonic.R
import com.simplerapps.phonic.repository.AudioFileModel
import com.simplerapps.phonic.repository.MyFolderRepo
import com.simplerapps.phonic.shareAudioFile
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MyFolderFragment : Fragment(R.layout.fragment_my_folder),
    MyFolderRecyclerViewAdapter.OnItemClickListener, Player.Listener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var myFolderRepo: MyFolderRepo
    private lateinit var playerView: PlayerView
    private lateinit var exoplayer: ExoPlayer
    private lateinit var rvLoadingProgressBar: ProgressBar

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.rv_my_folder)
        playerView = view.findViewById(R.id.my_folder_player)
        myFolderRepo = MyFolderRepo(requireContext().applicationContext)
        rvLoadingProgressBar = view.findViewById(R.id.pb_rv_loading)

        playerView.visibility = View.GONE
        rvLoadingProgressBar.visibility = View.VISIBLE
    }

    override fun onStart() {
        super.onStart()

        exoplayer = ExoPlayer.Builder(requireContext()).build()
        playerView.player = exoplayer
        exoplayer.playWhenReady = false
        exoplayer.prepare()
        playerView.controllerShowTimeoutMs = 0
        playerView.controllerHideOnTouch = false

        lifecycleScope.launch {
            initRecyclerView()
        }
    }

    override fun onStop() {
        super.onStop()
        exoplayer.release()
    }

    private suspend fun initRecyclerView() {
        val list = ArrayList<AudioFileModel>()
        myFolderRepo.getMyAudioList()?.let {
            list.addAll(it)
        }
        val adapter = MyFolderRecyclerViewAdapter(list, this)
        recyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        recyclerView.adapter = adapter

        rvLoadingProgressBar.visibility = View.GONE
    }

    override fun onItemShareClick(audioFileModel: AudioFileModel) {
        requireActivity().shareAudioFile(
            uri = Uri.parse(audioFileModel.uri),
            name = audioFileModel.displayName
        )
    }

    override fun onItemClick(audioFileModel: AudioFileModel) {
        exoplayer.stop()
        exoplayer.clearMediaItems()
        val mediaItem = MediaItem.fromUri(audioFileModel.uri)
        exoplayer.addMediaItem(mediaItem)
        exoplayer.prepare()
        exoplayer.play()

        playerView.visibility = View.VISIBLE
    }
}
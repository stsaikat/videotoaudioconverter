package com.simplerapps.phonic.service

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
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

class MyFolderFragment : Fragment(R.layout.fragment_my_folder),
    MyFolderRecyclerViewAdapter.OnItemClickListener, Player.Listener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var myFolderRepo: MyFolderRepo
    private lateinit var playerView: PlayerView
    private lateinit var exoplayer: ExoPlayer

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.rv_my_folder)
        playerView = view.findViewById(R.id.my_folder_player)
        myFolderRepo = MyFolderRepo(requireContext().applicationContext)

        playerView.visibility = View.GONE

        initRecyclerView()
    }

    override fun onStart() {
        super.onStart()

        exoplayer = ExoPlayer.Builder(requireContext()).build()
        playerView.player = exoplayer
        exoplayer.playWhenReady = false
        exoplayer.prepare()
        playerView.controllerShowTimeoutMs = 0
        playerView.controllerHideOnTouch = false
    }

    override fun onStop() {
        super.onStop()
        exoplayer.release()
    }

    private fun initRecyclerView() {
        recyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        val list = ArrayList<AudioFileModel>()
        myFolderRepo.getMyAudioList()?.let {
            list.addAll(it)
        }
        val adapter = MyFolderRecyclerViewAdapter(list, this)
        recyclerView.adapter = adapter
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
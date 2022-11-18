package com.simplerapps.phonic.service

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.simplerapps.phonic.databinding.FragmentMyFolderBinding
import com.simplerapps.phonic.repository.AudioFileModel
import com.simplerapps.phonic.repository.MyFolderRepo
import com.simplerapps.phonic.shareAudioFile
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MyFolderFragment : Fragment(R.layout.fragment_my_folder),
    MyFolderRecyclerViewAdapter.OnItemClickListener, Player.Listener {

    private lateinit var myFolderRepo: MyFolderRepo
    private lateinit var exoplayer: ExoPlayer

    private lateinit var viewBinding: FragmentMyFolderBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewBinding = FragmentMyFolderBinding.inflate(inflater, container, false)
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        myFolderRepo = MyFolderRepo(requireContext().applicationContext)

        viewBinding.tvEmptyList.visibility = View.GONE
        //viewBinding.myFolderPlayer.visibility = View.GONE
        viewBinding.pbRvLoading.visibility = View.VISIBLE
    }

    override fun onStart() {
        super.onStart()

        exoplayer = ExoPlayer.Builder(requireContext()).build()
        viewBinding.myFolderPlayer.player = exoplayer
        exoplayer.playWhenReady = false
        exoplayer.prepare()

        lifecycleScope.launch {
            initRecyclerView()
        }
    }

    override fun onStop() {
        super.onStop()
        exoplayer.release()
    }

    private fun initRecyclerView() {
        val list = ArrayList<AudioFileModel>()
        myFolderRepo.getMyAudioList()?.let {
            list.addAll(it)
        }

        if (list.isEmpty()) {
            viewBinding.tvEmptyList.visibility = View.VISIBLE
        }
        val adapter = MyFolderRecyclerViewAdapter(list, this)
        viewBinding.rvMyFolder.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        viewBinding.rvMyFolder.adapter = adapter

        viewBinding.pbRvLoading.visibility = View.GONE
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

        //viewBinding.myFolderPlayer.visibility = View.VISIBLE
    }
}
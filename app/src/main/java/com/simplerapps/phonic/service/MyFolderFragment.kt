package com.simplerapps.phonic.service

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.simplerapps.phonic.R
import com.simplerapps.phonic.repository.AudioFileModel
import com.simplerapps.phonic.repository.MyFolderRepo
import com.simplerapps.phonic.shareAudioFile

class MyFolderFragment : Fragment(R.layout.fragment_my_folder), MyFolderRecyclerViewAdapter.OnItemClickListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var myFolderRepo: MyFolderRepo

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.rv_my_folder)
        myFolderRepo = MyFolderRepo(requireContext().applicationContext)

        initRecyclerView()
    }

    private fun initRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(requireContext(),LinearLayoutManager.VERTICAL,false)
        val list = ArrayList<AudioFileModel>()
        myFolderRepo.getMyAudioList()?.let {
            list.addAll(it)
        }
        val adapter = MyFolderRecyclerViewAdapter(list,this)
        recyclerView.adapter = adapter
    }

    override fun onItemClick(audioFileModel: AudioFileModel) {
        requireActivity().shareAudioFile(
            uri = Uri.parse(audioFileModel.uri),
            name = audioFileModel.displayName
        )
    }
}
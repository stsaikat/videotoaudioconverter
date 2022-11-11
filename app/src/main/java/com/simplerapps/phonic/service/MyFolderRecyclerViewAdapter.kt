package com.simplerapps.phonic.service

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.simplerapps.phonic.R
import com.simplerapps.phonic.repository.AudioFileModel

class MyFolderRecyclerViewAdapter(
    private val list: ArrayList<AudioFileModel>,
    private val listener: OnItemClickListener
) : RecyclerView.Adapter<MyFolderRecyclerViewAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameTextView: TextView = view.findViewById(R.id.tv_audio_item_name)
        val seekbar: SeekBar = view.findViewById(R.id.sb_audio_item)
        val playPauseButton: ImageButton = view.findViewById(R.id.ib_play_audio_item)
        val shareButton: ImageButton = view.findViewById(R.id.ib_share_audio_item)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_my_audio_element, parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val audioFileModel = list[position]
        holder.nameTextView.text = audioFileModel.displayName
        holder.shareButton.setOnClickListener {
            listener.onItemClick(audioFileModel)
        }
    }

    override fun getItemCount(): Int = list.size

    interface OnItemClickListener {
        fun onItemClick(audioFileModel: AudioFileModel)
    }
}
package com.simplerapps.phonic.service

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.simplerapps.phonic.R
import com.simplerapps.phonic.repository.AudioFileModel

class MyFolderRecyclerViewAdapter(
    private val list: ArrayList<AudioFileModel>,
    private val listener: OnItemClickListener
) : RecyclerView.Adapter<MyFolderRecyclerViewAdapter.ViewHolder>() {

    private var currentPlayingPosition = -1

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameTextView: TextView = view.findViewById(R.id.tv_audio_item_name)
        val moreButton: ImageButton = view.findViewById(R.id.ib_more)
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
        holder.nameTextView.setTextColor(
            if (position == currentPlayingPosition) {
                holder.itemView.context.resources.getColor(R.color.color_active, null)
            } else {
                holder.itemView.context.resources.getColor(R.color.text_color_active, null)
            }
        )

        holder.moreButton.setOnClickListener {
            //listener.onItemMoreClick(audioFileModel)

            val popUp = PopupMenu(holder.itemView.context, holder.moreButton)
            popUp.inflate(R.menu.my_folder_options)
            popUp.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.edit -> listener.onEditClick(audioFileModel)
                    R.id.share -> listener.onShareClick(audioFileModel)
                }
                return@setOnMenuItemClickListener true
            }
            popUp.show()
        }
        holder.nameTextView.setOnClickListener {
            listener.onItemClick(audioFileModel)
            if (currentPlayingPosition != -1) {
                notifyItemChanged(currentPlayingPosition)
            }
            currentPlayingPosition = position
            notifyItemChanged(position)
        }
    }

    override fun getItemCount(): Int = list.size

    interface OnItemClickListener {
        //fun onItemMoreClick(audioFileModel: AudioFileModel)
        fun onEditClick(audioFileModel: AudioFileModel)
        fun onShareClick(audioFileModel: AudioFileModel)
        fun onItemClick(audioFileModel: AudioFileModel)
    }
}
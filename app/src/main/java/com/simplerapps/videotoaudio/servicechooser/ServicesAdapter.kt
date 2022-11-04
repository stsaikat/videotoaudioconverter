package com.simplerapps.videotoaudio.servicechooser

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.simplerapps.videotoaudio.R

class ServicesAdapter(private val list: ArrayList<Service>,private val listener: ItemClickListener) :
    RecyclerView.Adapter<ServicesAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.tv_service_name)
        val icon: ImageView = itemView.findViewById(R.id.iv_service_icon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_service, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.name.text = item.serviceName

        holder.itemView.setOnClickListener {
            listener.onItemClick(item)
        }

        holder.icon.setImageResource(
            when(item) {
                Service.VIDEO_TO_AUDIO -> R.drawable.ic_video_to_audio
                Service.EDIT_AUDIO -> R.drawable.ic_edit_audio
                Service.MERGE_AUDIO -> R.drawable.ic_merge_audio
                Service.MY_FOLDER -> R.drawable.ic_baseline_folder_24
            }
        )
    }

    override fun getItemCount(): Int = list.size

    interface ItemClickListener {
        fun onItemClick(service: Service)
    }
}
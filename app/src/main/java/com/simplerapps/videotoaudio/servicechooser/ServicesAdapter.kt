package com.simplerapps.videotoaudio.servicechooser

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.simplerapps.videotoaudio.R

class ServicesAdapter(private val list: ArrayList<Service>,private val listener: ItemClickListener) :
    RecyclerView.Adapter<ServicesAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.tv_service_name)
        val details: TextView = itemView.findViewById(R.id.tv_service_details)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_service, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.name.text = item.serviceName
        if (item.serviceDetails != null) {
            holder.details.text = item.serviceDetails
        }
        else {
            holder.details.text = ""
        }

        holder.itemView.setOnClickListener {
            listener.onItemClick(item)
        }
    }

    override fun getItemCount(): Int = list.size

    interface ItemClickListener {
        fun onItemClick(service: Service)
    }
}
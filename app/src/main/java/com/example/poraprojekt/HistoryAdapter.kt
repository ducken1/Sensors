package com.example.poraprojekt

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class HistoryAdapter : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    private var historyList: List<HistoryItem> = mutableListOf()

    fun setItems(items: List<HistoryItem>) {
        historyList = items
        notifyDataSetChanged()
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val sensorTypeTextView: TextView = itemView.findViewById(R.id.sensorTypeTextView)
        val dataTextView: TextView = itemView.findViewById(R.id.dataTextView)
        val timeTextView: TextView = itemView.findViewById(R.id.timeTextView)
        val locationTextView: TextView = itemView.findViewById(R.id.locationTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.history_item, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentItem = historyList[position]

        holder.sensorTypeTextView.text = "Sensor Type: ${currentItem.sensorType}"
        holder.dataTextView.text = "Data: ${currentItem.sensorData}"
        holder.timeTextView.text = "Time: ${currentItem.absoluteTime}"
        holder.locationTextView.text = "Location: ${currentItem.location}"
    }

    override fun getItemCount(): Int {
        return historyList.size
    }
}
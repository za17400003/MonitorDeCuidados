package com.example.monitordecuidados.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.monitordecuidados.R
import com.example.monitordecuidados.data.local.Event
import java.text.SimpleDateFormat
import java.util.*

class NotificationAlertAdapter : RecyclerView.Adapter<NotificationAlertAdapter.ViewHolder>() {
    private var items = listOf<Event>()

    fun submitList(newItems: List<Event>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_alert_campana, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

        holder.tvTerminalName?.text = item.sourceTerminalName
        holder.tvAlertMessage.text = item.message
        holder.tvAlertTime.text = sdf.format(Date(item.timestamp))
    }

    override fun getItemCount() = items.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTerminalName: TextView? = view.findViewById(R.id.tvTerminalName)
        val tvAlertMessage: TextView = view.findViewById(R.id.tvAlertMessage)
        val tvAlertTime: TextView = view.findViewById(R.id.tvAlertTime)
    }
}

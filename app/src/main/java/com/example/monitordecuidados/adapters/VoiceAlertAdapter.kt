package com.example.monitordecuidados.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.monitordecuidados.data.local.Event
import com.example.monitordecuidados.databinding.ItemAlertVozBinding
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adapter for Voice Detection events.
 */
class VoiceAlertAdapter(private var alerts: List<Event>) : RecyclerView.Adapter<VoiceAlertAdapter.VoiceViewHolder>() {

    class VoiceViewHolder(val binding: ItemAlertVozBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VoiceViewHolder {
        val binding = ItemAlertVozBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VoiceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VoiceViewHolder, position: Int) {
        val alert = alerts[position]
        holder.binding.tvAlertMessage.text = alert.message
        holder.binding.tvAlertTime.text = formatTimestamp(alert.timestamp)
    }

    override fun getItemCount(): Int = alerts.size

    fun updateData(newAlerts: List<Event>) {
        alerts = newAlerts
        notifyDataSetChanged()
    }

    private fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}

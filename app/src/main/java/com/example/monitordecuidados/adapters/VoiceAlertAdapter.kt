package com.example.monitordecuidados.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.monitordecuidados.R
import com.example.monitordecuidados.data.local.Event
import com.example.monitordecuidados.databinding.ItemAlertVozBinding
import java.text.SimpleDateFormat
import java.util.*

interface VoiceAlertListener {
    fun onCallClicked(event: Event)
    fun onMonitorClicked(event: Event)
}

/**
 * Adapter for Voice Detection events.
 */
class VoiceAlertAdapter(
    private var alerts: List<Event>,
    private val listener: VoiceAlertListener? = null
) : RecyclerView.Adapter<VoiceAlertAdapter.VoiceViewHolder>() {

    class VoiceViewHolder(val binding: ItemAlertVozBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VoiceViewHolder {
        val binding = ItemAlertVozBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VoiceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VoiceViewHolder, position: Int) {
        val alert = alerts[position]
        holder.binding.ivAlertIcon.setImageResource(R.drawable.ic_microphone)
        holder.binding.tvTerminalName.text = alert.type
        holder.binding.tvAlertMessage.text = alert.message
        holder.binding.tvAlertTime.text = formatTimestamp(alert.timestamp)
        holder.binding.btnCall.setOnClickListener { listener?.onCallClicked(alert) }
        holder.binding.btnMonitor.setOnClickListener { listener?.onMonitorClicked(alert) }
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
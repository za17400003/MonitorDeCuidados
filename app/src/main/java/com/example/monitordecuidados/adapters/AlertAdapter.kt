package com.example.monitordecuidados.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.monitordecuidados.R
import com.example.monitordecuidados.data.local.Event
import com.example.monitordecuidados.databinding.ItemAlertCampanaBinding
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adapter for Bell (Campanazo) and other alerts.
 * T84: Grouped by terminal, with call/monitor actions.
 */
class AlertAdapter(
    private var alerts: List<Event>,
    private val onCallClick: ((Event) -> Unit)? = null,
    private val onMonitorClick: ((Event) -> Unit)? = null
) : RecyclerView.Adapter<AlertAdapter.AlertViewHolder>() {

    class AlertViewHolder(val binding: ItemAlertCampanaBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlertViewHolder {
        val binding = ItemAlertCampanaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AlertViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AlertViewHolder, position: Int) {
        val alert = alerts[position]
        holder.binding.tvAlertMessage.text = alert.message
        val timeString = formatTimestamp(alert.timestamp)
        holder.binding.tvAlertTime.text = timeString

        // T84: Mostrar nombre del terminal fuente
        holder.binding.tvTerminalName.text = alert.sourceTerminalName.ifBlank { alert.sourceIp }

        // T69: Dynamic icon based on type
        holder.binding.tvAlertIcon.text = when(alert.type) {
            "bell" -> "🔔"
            "shake" -> "⚠️"
            "voice" -> "🎙️"
            "battery_low" -> "🔋"
            "alarm" -> "⏰"
            else -> "🛎️"
        }

        // T84: Acciones por terminal
        holder.binding.btnCall.setOnClickListener { onCallClick?.invoke(alert) }
        holder.binding.btnMonitor.setOnClickListener { onMonitorClick?.invoke(alert) }

        // Accessibility
        holder.itemView.contentDescription = holder.itemView.context.getString(
            R.string.cd_alert_card, alert.type, timeString
        )
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

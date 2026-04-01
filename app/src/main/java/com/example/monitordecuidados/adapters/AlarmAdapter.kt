package com.example.monitordecuidados.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.monitordecuidados.R
import com.example.monitordecuidados.data.local.Alarm
import com.google.android.material.materialswitch.MaterialSwitch

class AlarmAdapter(
    private var alarms: List<Alarm>,
    private val onToggle: (Alarm, Boolean) -> Unit,
    private val onDelete: (Alarm) -> Unit
) : RecyclerView.Adapter<AlarmAdapter.AlarmViewHolder>() {

    inner class AlarmViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTime: TextView = itemView.findViewById(R.id.tvAlarmTime)
        val tvMessage: TextView = itemView.findViewById(R.id.tvAlarmMessage)
        val switchEnabled: MaterialSwitch = itemView.findViewById(R.id.switchAlarmEnabled)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDeleteAlarm)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlarmViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_alarm, parent, false)
        return AlarmViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlarmViewHolder, position: Int) {
        val alarm = alarms[position]
        holder.tvTime.text = String.format("%02d:%02d", alarm.hour, alarm.minute)
        holder.tvMessage.text = alarm.message
        
        holder.switchEnabled.setOnCheckedChangeListener(null)
        holder.switchEnabled.isChecked = alarm.enabled
        holder.switchEnabled.setOnCheckedChangeListener { _, isChecked ->
            onToggle(alarm, isChecked)
        }

        holder.btnDelete.setOnClickListener {
            onDelete(alarm)
        }
    }

    override fun getItemCount(): Int = alarms.size

    fun updateData(newAlarms: List<Alarm>) {
        alarms = newAlarms
        notifyDataSetChanged()
    }
}

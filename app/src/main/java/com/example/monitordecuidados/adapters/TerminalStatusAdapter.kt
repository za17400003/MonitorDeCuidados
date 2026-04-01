package com.example.monitordecuidados.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.monitordecuidados.R
import com.example.monitordecuidados.databinding.ItemTerminalStatusBinding

data class TerminalInfo(
    val name: String,
    val status: String,     // "connected", "reconnecting", "disconnected"
    val batteryPercent: Int,
    val connectionType: String  // "WiFi", "Mobile", "None"
)

class TerminalStatusAdapter(
    private var terminals: List<TerminalInfo>,
    private val onItemClick: ((TerminalInfo) -> Unit)? = null
) : RecyclerView.Adapter<TerminalStatusAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemTerminalStatusBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTerminalStatusBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val terminal = terminals[position]
        holder.binding.tvTerminalName.text = terminal.name
        
        val statusEmoji = when (terminal.status) {
            "connected", "Conectado" -> "🟢 Activo"
            "reconnecting" -> "🟡 Reconectando"
            else -> "🔴 Desconectado"
        }
        holder.binding.tvTerminalStatus.text = statusEmoji
        
        val batteryText = "🔋 ${terminal.batteryPercent}%"
        holder.binding.tvTerminalBattery.text = batteryText
        
        if (terminal.batteryPercent <= 15 && terminal.batteryPercent > 0) {
            holder.binding.tvTerminalBattery.append(" ⚠️")
        }
        
        holder.binding.tvTerminalConnection.text = "📶 ${terminal.connectionType}"

        // Accessibility
        holder.itemView.contentDescription = holder.itemView.context.getString(
            R.string.cd_terminal_card, terminal.name, terminal.status
        )

        holder.itemView.setOnClickListener {
            onItemClick?.invoke(terminal)
        }
    }

    override fun getItemCount(): Int = terminals.size

    fun updateData(newTerminals: List<TerminalInfo>) {
        terminals = newTerminals
        notifyDataSetChanged()
    }

    fun getData(): List<TerminalInfo> = terminals
}
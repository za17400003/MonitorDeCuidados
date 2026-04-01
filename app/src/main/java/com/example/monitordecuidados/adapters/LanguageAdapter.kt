package com.example.monitordecuidados.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.monitordecuidados.models.Language
import com.example.monitordecuidados.R

class LanguageAdapter(
    private val languages: List<Language>,
    private val onLanguageSelected: (Language) -> Unit
) : RecyclerView.Adapter<LanguageAdapter.LanguageViewHolder>() {

    private var selectedPosition = -1

    inner class LanguageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvFlag: TextView = itemView.findViewById(R.id.tvFlag)
        val tvName: TextView = itemView.findViewById(R.id.tvLanguageName)
        val ivCheck: View = itemView.findViewById(R.id.ivCheck)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val oldPosition = selectedPosition
                    selectedPosition = position
                    notifyItemChanged(oldPosition)
                    notifyItemChanged(selectedPosition)
                    onLanguageSelected(languages[position])
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_language, parent, false)
        return LanguageViewHolder(view)
    }

    override fun onBindViewHolder(holder: LanguageViewHolder, position: Int) {
        val language = languages[position]
        holder.tvFlag.text = when (language.code) {
            "es" -> "🇪🇸"
            "en" -> "🇬🇧"
            "fr" -> "🇫🇷"
            "pt" -> "🇧🇷"
            "de" -> "🇩🇪"
            "it" -> "🇮🇹"
            else -> "🌐"
        }
        holder.tvName.text = language.name
        holder.ivCheck.visibility = if (position == selectedPosition) View.VISIBLE else View.GONE
    }

    override fun getItemCount(): Int = languages.size
}

package com.example.monitordecuidados.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.monitordecuidados.R
import com.example.monitordecuidados.data.local.CustomPhrase

class CustomPhrasesAdapter(
    private var phrases: List<CustomPhrase>,
    private val onDelete: (CustomPhrase) -> Unit,
    private val onPlay: (CustomPhrase) -> Unit
) : RecyclerView.Adapter<CustomPhrasesAdapter.PhraseViewHolder>() {

    inner class PhraseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvPhraseName)
        val btnPlay: ImageButton = itemView.findViewById(R.id.btnPlayPhrase)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDeletePhrase)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhraseViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_custom_phrase, parent, false)
        return PhraseViewHolder(view)
    }

    override fun onBindViewHolder(holder: PhraseViewHolder, position: Int) {
        val phrase = phrases[position]
        holder.tvName.text = phrase.name
        
        holder.btnPlay.setOnClickListener { onPlay(phrase) }
        holder.btnDelete.setOnClickListener { onDelete(phrase) }
    }

    override fun getItemCount(): Int = phrases.size

    fun updateData(newPhrases: List<CustomPhrase>) {
        phrases = newPhrases
        notifyDataSetChanged()
    }
}

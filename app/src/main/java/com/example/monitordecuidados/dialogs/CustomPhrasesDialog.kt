package com.example.monitordecuidados.dialogs

import android.app.Dialog
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.monitordecuidados.adapters.CustomPhrasesAdapter
import com.example.monitordecuidados.data.local.AppDatabase
import com.example.monitordecuidados.data.local.CustomPhrase
import com.example.monitordecuidados.databinding.DialogAddCustomPhrasesBinding
import com.example.monitordecuidados.managers.CustomPhrasesManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class CustomPhrasesDialog : DialogFragment() {

    private var _binding: DialogAddCustomPhrasesBinding? = null
    private val binding get() = _binding!!
    private lateinit var manager: CustomPhrasesManager
    private var isRecording = false
    private var lastRecordedAudio: ByteArray? = null
    private lateinit var adapter: CustomPhrasesAdapter

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogAddCustomPhrasesBinding.inflate(layoutInflater)
        manager = CustomPhrasesManager(requireContext())
        
        setupRecyclerView()
        setupListeners()
        loadSensitivity()
        observePhrases()

        return AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .create()
    }

    private fun setupListeners() {
        binding.btnRecordPhrase.setOnClickListener {
            if (!isRecording) {
                val phraseName = binding.etPhrase.text.toString()
                if (phraseName.isBlank()) {
                    Toast.makeText(context, "Escribe la frase antes de grabar", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                manager.startRecording(phraseName)
                binding.btnRecordPhrase.text = "Detener grabación"
                isRecording = true
            } else {
                lastRecordedAudio = manager.stopRecording()
                binding.btnRecordPhrase.text = "Grabar frase (Listo ✓)"
                isRecording = false
                Toast.makeText(context, "Grabación completada", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnSavePhrases.setOnClickListener {
            val phraseName = binding.etPhrase.text.toString()
            if (phraseName.isNotBlank()) {
                lifecycleScope.launch {
                    manager.savePhrase(phraseName, lastRecordedAudio, binding.sliderSensitivity.value / 100f)
                    binding.etPhrase.text?.clear()
                    lastRecordedAudio = null
                    binding.btnRecordPhrase.text = "Grabar frase"
                    Toast.makeText(context, "Frase guardada", Toast.LENGTH_SHORT).show()
                }
            } else {
                dismiss()
            }
        }

        binding.sliderSensitivity.addOnChangeListener { _, value, _ ->
            manager.setSensitivityThreshold(value / 100f)
        }
    }

    private fun loadSensitivity() {
        binding.sliderSensitivity.value = manager.getSensitivityThreshold() * 100f
    }

    private fun setupRecyclerView() {
        adapter = CustomPhrasesAdapter(emptyList(), 
            onDelete = { phrase ->
                lifecycleScope.launch {
                    AppDatabase.getDatabase(requireContext()).customPhraseDao().deletePhrase(phrase)
                }
            },
            onPlay = { phrase ->
                phrase.audioData?.let { manager.playPlayback(it) }
            }
        )
        binding.rvPhrases.layoutManager = LinearLayoutManager(context)
        binding.rvPhrases.adapter = adapter
    }

    private fun observePhrases() {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())
            db.customPhraseDao().getAllPhrases().collectLatest { phrases ->
                adapter.updateData(phrases)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

package com.example.monitordecuidados.dialogs

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.monitordecuidados.R
import com.example.monitordecuidados.databinding.DialogCalibrationStepBinding
import java.util.*

class CalibrationDialog : DialogFragment() {

    private var _binding: DialogCalibrationStepBinding? = null
    private val binding get() = _binding!!
    
    private var speechRecognizer: SpeechRecognizer? = null
    private var currentStep = 0
    private val wordsToCalibrate = arrayOf("AUXILIO", "AYUDA", "SOCORRO")
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogCalibrationStepBinding.inflate(layoutInflater)
        
        setupStep()
        
        binding.btnRecordCalibration.setOnClickListener {
            startListening()
        }

        return AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .create()
    }

    private fun setupStep() {
        if (isAdded) {
            if (currentStep < wordsToCalibrate.size) {
                binding.tvCalibrationWord.text = wordsToCalibrate[currentStep]
                binding.progressCalibration.progress = (currentStep * 100) / wordsToCalibrate.size
                binding.tvCalibrationStatus.text = getString(R.string.calibration_mic_start)
            } else {
                binding.tvCalibrationWord.text = getString(R.string.ready_done)
                binding.tvCalibrationStatus.text = getString(R.string.calibration_success)
                binding.progressCalibration.progress = 100
                binding.btnRecordCalibration.isEnabled = false
                
                Handler(Looper.getMainLooper()).postDelayed({
                    if (isAdded) dismiss()
                }, 2000)
            }
        }
    }

    private fun startListening() {
        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(requireContext())
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                binding.tvCalibrationStatus.text = getString(R.string.listening)
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {
                binding.progressCalibration.progress = ((currentStep * 100 / wordsToCalibrate.size) + (rmsdB.toInt() / 2)).coerceAtMost(100)
            }
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                binding.tvCalibrationStatus.text = getString(R.string.calibration_error)
            }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val detected = matches?.get(0)?.uppercase() ?: ""
                
                if (detected.contains(wordsToCalibrate[currentStep])) {
                    Toast.makeText(context, getString(R.string.well_done), Toast.LENGTH_SHORT).show()
                    currentStep++
                    setupStep()
                } else {
                    binding.tvCalibrationStatus.text = "No entendí. Dijiste: $detected. Prueba de nuevo."
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        speechRecognizer?.startListening(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        speechRecognizer?.destroy()
        _binding = null
    }
}

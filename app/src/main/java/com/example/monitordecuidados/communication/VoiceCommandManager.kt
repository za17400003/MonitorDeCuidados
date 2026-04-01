package com.example.monitordecuidados.communication

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import com.example.monitordecuidados.data.local.AppDatabase
import com.example.monitordecuidados.managers.CustomPhrasesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.*

class VoiceCommandManager(
    private val context: Context,
    private val listener: OnVoiceCommandListener
) {

    interface OnVoiceCommandListener {
        fun onKeywordDetected(keyword: String)
        fun onSpeechError(error: String)
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private val recognizerIntent: Intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
    }

    private var isListening = false
    private val defaultKeywords = setOf("auxilio", "ayuda", "socorro", "necesito ayuda")
    private val customPhrasesManager = CustomPhrasesManager(context)
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    // Sensitivity thresholds (P2-1)
    private var voiceThreshold = 0.80f
    private var minRmsdB = 12.0f

    init {
        updateSensitivityFromPrefs()
    }

    private fun updateSensitivityFromPrefs() {
        val prefs = context.getSharedPreferences("monitordecuidados_prefs", Context.MODE_PRIVATE)
        val speechDifficulty = prefs.getBoolean("speech_difficulty", false)
        if (speechDifficulty) {
            // Weak voice mode: More sensitive
            voiceThreshold = 0.65f
            minRmsdB = 8.0f
        } else {
            // Normal mode
            voiceThreshold = 0.80f
            minRmsdB = 12.0f
        }
    }

    fun startListening() {
        if (isListening) return
        
        updateSensitivityFromPrefs() // Refresh before starting

        scope.launch(Dispatchers.Main) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        isListening = true
                        Log.d("VoiceManager", "Ready for speech (Threshold: $minRmsdB dB)")
                    }

                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {
                        // Detection based on adapted threshold
                        if (rmsdB > minRmsdB) {
                            // listener.onKeywordDetected("¡GRITO DETECTADO!") 
                            // Note: We might want to handle shouts separately or just use it as activation
                        }
                    }
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {
                        isListening = false
                    }

                    override fun onError(error: Int) {
                        val message = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "Error de audio"
                            SpeechRecognizer.ERROR_NO_MATCH -> "No se encontró coincidencia"
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Reconocedor ocupado"
                            else -> "Error: $error"
                        }
                        Log.e("VoiceManager", "Error: $message")
                        isListening = false
                        if (error != SpeechRecognizer.ERROR_RECOGNIZER_BUSY && speechRecognizer != null) {
                            startListening()
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        matches?.forEach { text ->
                            processDetectedText(text)
                        }
                        isListening = false
                        if (speechRecognizer != null) startListening()
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        matches?.forEach { text ->
                            processDetectedText(text)
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
            speechRecognizer?.startListening(recognizerIntent)
        }
    }

    private fun processDetectedText(text: String) {
        val lowercaseText = text.lowercase()
        
        defaultKeywords.forEach { keyword ->
            if (lowercaseText.contains(keyword)) {
                listener.onKeywordDetected(keyword)
                return
            }
        }

        scope.launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val customPhrases = db.customPhraseDao().getAllPhrasesSync()
                
                // Use adapted voiceThreshold instead of fixed manager threshold if higher sensitivity needed
                val finalThreshold = minOf(voiceThreshold, customPhrasesManager.getSensitivityThreshold())

                customPhrases.forEach { phrase ->
                    val similarity = calculateSimilarity(lowercaseText, phrase.name.lowercase())
                    if (similarity >= finalThreshold) {
                        launch(Dispatchers.Main) {
                            listener.onKeywordDetected(phrase.name)
                        }
                        return@launch
                    }
                }
            } catch (e: Exception) {
                Log.e("VoiceManager", "Error processing phrases: ${e.message}")
            }
        }
    }

    private fun calculateSimilarity(s1: String, s2: String): Float {
        if (s1.isEmpty() || s2.isEmpty()) return 0f
        if (s1.contains(s2) || s2.contains(s1)) return 1.0f
        
        val longer = if (s1.length >= s2.length) s1 else s2
        val shorter = if (s1.length < s2.length) s1 else s2
        val longerLength = longer.length
        
        return (longerLength - levenshteinDistance(longer, shorter)) / longerLength.toFloat()
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j

        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        return dp[s1.length][s2.length]
    }

    fun stopListening() {
        isListening = false
        speechRecognizer?.stopListening()
        speechRecognizer?.destroy()
        speechRecognizer = null
        scope.cancel()
    }
}

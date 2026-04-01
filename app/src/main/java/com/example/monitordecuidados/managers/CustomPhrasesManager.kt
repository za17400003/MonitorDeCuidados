package com.example.monitordecuidados.managers

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.util.Log
import com.example.monitordecuidados.data.local.AppDatabase
import com.example.monitordecuidados.data.local.CustomPhrase
import com.example.monitordecuidados.utils.AES256EncryptionHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File

class CustomPhrasesManager(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var isRecording = false
    private var currentRecordingFile: File? = null

    fun startRecording(phraseName: String) {
        try {
            currentRecordingFile = File(context.cacheDir, "${phraseName}_temp.3gp")
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(currentRecordingFile?.absolutePath)
                prepare()
                start()
            }
            isRecording = true
        } catch (e: Exception) {
            Log.e("CustomPhrasesManager", "Start recording failed: ${e.message}")
        }
    }

    fun stopRecording(): ByteArray? {
        if (!isRecording) return null
        return try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            isRecording = false
            currentRecordingFile?.readBytes()
        } catch (e: Exception) {
            Log.e("CustomPhrasesManager", "Stop recording failed: ${e.message}")
            null
        }
    }

    fun playPlayback(audioData: ByteArray) {
        try {
            val tempFile = File(context.cacheDir, "phrase_playback.3gp")
            tempFile.writeBytes(audioData)
            
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(tempFile.absolutePath)
                prepare()
                start()
                setOnCompletionListener { 
                    it.release() 
                    mediaPlayer = null
                }
            }
        } catch (e: Exception) {
            Log.e("CustomPhrasesManager", "Playback failed: ${e.message}")
        }
    }

    suspend fun savePhrase(phraseName: String, audioData: ByteArray?, confidence: Float) {
        val db = AppDatabase.getDatabase(context)
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "local_user"
        
        val encryptedContent = AES256EncryptionHelper.encrypt(phraseName, userId)

        val customPhrase = CustomPhrase(
            name = phraseName,
            audioData = audioData,
            confidence = confidence,
            createdAt = System.currentTimeMillis(),
            encryptedContent = encryptedContent
        )
        
        db.customPhraseDao().insertPhrase(customPhrase)
        
        if (userId != "local_user") {
            val phraseMap = hashMapOf(
                "encryptedContent" to encryptedContent,
                "confidence" to confidence,
                "createdAt" to customPhrase.createdAt,
                "autoAdded" to false
            )
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .collection("custom_phrases")
                .document(phraseName)
                .set(phraseMap)
        }
    }

    /**
     * P3-1: Re-pair Mapping Logic.
     * When Terminal re-pairs, auto-added phrases (like names) must be replaced.
     */
    suspend fun remapPhrasesOnRePair(oldMonitorId: String?, newMonitorId: String, newMonitorName: String) {
        val db = AppDatabase.getDatabase(context)
        
        // 1. Delete old auto-added phrases
        if (oldMonitorId != null) {
            val oldPhrases = db.customPhraseDao().getAllPhrasesSync().filter { 
                it.autoAdded && it.addedBy_monitorId == oldMonitorId 
            }
            oldPhrases.forEach { db.customPhraseDao().deletePhrase(it) }
        }

        // 2. Add new auto-added phrase for the new Monitor
        val phraseName = newMonitorName
        val encrypted = AES256EncryptionHelper.encrypt(phraseName, newMonitorId)
        
        val newPhrase = CustomPhrase(
            name = phraseName,
            audioData = null, // No audio for auto-added names
            confidence = 0.85f,
            autoAdded = true,
            addedBy_monitorId = newMonitorId,
            encryptedContent = encrypted
        )
        db.customPhraseDao().insertPhrase(newPhrase)
    }

    fun getSensitivityThreshold(): Float {
        val prefs = context.getSharedPreferences("monitordecuidados_prefs", Context.MODE_PRIVATE)
        return prefs.getFloat("voice_sensitivity", 0.7f)
    }

    fun setSensitivityThreshold(threshold: Float) {
        val prefs = context.getSharedPreferences("monitordecuidados_prefs", Context.MODE_PRIVATE)
        prefs.edit().putFloat("voice_sensitivity", threshold).apply()
    }
}

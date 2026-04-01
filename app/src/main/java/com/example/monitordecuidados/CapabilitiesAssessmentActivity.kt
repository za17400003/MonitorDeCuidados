package com.example.monitordecuidados

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.monitordecuidados.databinding.ActivityCapabilitiesBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Activity for geriatric capabilities assessment.
 * Implements 5-Capacity Assessment Flow: Audition, Mobility, Cognition, Speech, Vision.
 */
class CapabilitiesAssessmentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCapabilitiesBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var currentQuestionIndex = 0
    private val responses = mutableMapOf<String, Boolean>()

    private val questions = listOf(
        AssessmentQuestion("audition", "¿Puede escuchar una conversación normal sin dificultad?"),
        AssessmentQuestion("mobility", "¿Puede caminar sin ayuda de otra persona?"),
        AssessmentQuestion("cognition", "¿Sabe en qué día y lugar se encuentra actualmente?"),
        AssessmentQuestion("speech", "¿Tiene dificultad para hablar o darse a entender?"),
        AssessmentQuestion("vision", "¿Puede ver televisión o leer sin dificultad?")
    )

    data class AssessmentQuestion(val id: String, val text: String)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCapabilitiesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupMainGate()
        setupAssessmentNavigation()
    }

    private fun setupMainGate() {
        binding.llMainGate.visibility = View.VISIBLE
        binding.clAssessmentContent.visibility = View.GONE

        binding.btnDoAssessment.setOnClickListener {
            startAssessment()
        }

        binding.btnSkipAssessment.setOnClickListener {
            skipAssessment()
        }

        binding.btnCancelAssessment.setOnClickListener {
            finish()
        }
    }

    private fun startAssessment() {
        binding.llMainGate.visibility = View.GONE
        binding.clAssessmentContent.visibility = View.VISIBLE
        currentQuestionIndex = 0
        showQuestion(currentQuestionIndex)
    }

    private fun showQuestion(index: Int) {
        val question = questions[index]
        binding.tvAssessmentTitle.text = question.text
        
        binding.rbOption1.text = "Sí"
        binding.rbOption2.text = "No"
        binding.rbOption3.visibility = View.GONE
        
        binding.rgCapabilities.clearCheck()
        
        // Restore previous response if exists
        val prevResponse = responses[question.id]
        if (prevResponse != null) {
            if (prevResponse) binding.rbOption1.isChecked = true
            else binding.rbOption2.isChecked = true
        }

        binding.btnBack.visibility = if (index == 0) View.GONE else View.VISIBLE
        binding.btnNext.text = if (index == questions.size - 1) "Finalizar" else "Siguiente"
    }

    private fun setupAssessmentNavigation() {
        binding.btnNext.setOnClickListener {
            val checkedId = binding.rgCapabilities.checkedRadioButtonId
            if (checkedId == -1) {
                Toast.makeText(this, "Por favor seleccione una opción", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val isYes = checkedId == binding.rbOption1.id
            responses[questions[currentQuestionIndex].id] = isYes

            if (currentQuestionIndex < questions.size - 1) {
                currentQuestionIndex++
                showQuestion(currentQuestionIndex)
            } else {
                saveResultsAndFinish()
            }
        }

        binding.btnBack.setOnClickListener {
            if (currentQuestionIndex > 0) {
                currentQuestionIndex--
                showQuestion(currentQuestionIndex)
            }
        }
    }

    private fun saveResultsAndFinish() {
        val userId = auth.currentUser?.uid
        val capabilitiesMap = responses.toMap()
        
        if (userId != null) {
            val update = mapOf(
                "capabilities_status" to "completed",
                "capabilities" to capabilitiesMap,
                "speech_difficulty" to (responses["speech"] ?: false)
            )
            db.collection("users").document(userId)
                .collection("capabilities_assessment")
                .document("latest")
                .set(update)
                
            db.collection("users").document(userId)
                .update("capabilities_status", "completed")
        }

        // Save locally for VoiceCommandManager sensitivity
        val prefs = getSharedPreferences("monitordecuidados_prefs", MODE_PRIVATE)
        prefs.edit().apply {
            putString("capabilities_status", "completed")
            putBoolean("speech_difficulty", responses["speech"] ?: false)
            apply()
        }

        Toast.makeText(this, "Evaluación completada", Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, TerminalMainActivity::class.java))
        finish()
    }

    private fun skipAssessment() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            val update = mapOf(
                "capabilities_status" to "omitted",
                "capabilities" to null
            )
            db.collection("users").document(userId)
                .update(update)
        }
        
        // Save locally
        val prefs = getSharedPreferences("monitordecuidados_prefs", MODE_PRIVATE)
        prefs.edit().putString("capabilities_status", "omitted").apply()
        
        // Navigate to Terminal Config
        startActivity(Intent(this, TerminalMainActivity::class.java))
        finish()
    }
}
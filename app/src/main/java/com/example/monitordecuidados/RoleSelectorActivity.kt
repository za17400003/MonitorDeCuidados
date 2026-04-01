package com.example.monitordecuidados

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.monitordecuidados.databinding.ActivityRoleSelectorBinding
import com.example.monitordecuidados.utils.EncryptedPreferencesHelper
import com.example.monitordecuidados.CapabilitiesAssessmentActivity

class RoleSelectorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRoleSelectorBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRoleSelectorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.cardCaregiver.setOnClickListener {
            saveRole("monitor")
            // TASK #1: Reinitialize connections after role switch
            reinitializeConnections()
            val intent = Intent(this, MonitorMainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        binding.cardPatient.setOnClickListener {
            saveRole("terminal")
            // TASK #1: Reinitialize connections after role switch
            reinitializeConnections()

            val capStatus = EncryptedPreferencesHelper.getString(this, "capabilities_status", "")
            val targetClass = if (capStatus == "completed" || capStatus == "omitted") {
                TerminalMainActivity::class.java
            } else {
                CapabilitiesAssessmentActivity::class.java
            }

            val intent = Intent(this, targetClass)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun saveRole(role: String) {
        val prefs = getSharedPreferences("monitordecuidados_prefs", MODE_PRIVATE)
        prefs.edit().putString("user_role", role).apply()
    }

    private fun reinitializeConnections() {
        // Stop current service and restart to apply new role
        CampanaService.stopService(this)
        CampanaService.startService(this)
    }
}
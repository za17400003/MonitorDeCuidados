package com.example.monitordecuidados

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.monitordecuidados.databinding.ActivitySplashBinding
import com.example.monitordecuidados.utils.EncryptedPreferencesHelper
import com.example.monitordecuidados.CapabilitiesAssessmentActivity
import java.util.*

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Handler(Looper.getMainLooper()).postDelayed({
            val lang = EncryptedPreferencesHelper.getString(this, "app_language")
            if (lang == null) {
                startActivity(Intent(this, LanguageSelectorActivity::class.java))
                finish()
                return@postDelayed
            } else {
                updateLocale(lang)
            }

            val prefs = getSharedPreferences("monitordecuidados_prefs", MODE_PRIVATE)
            val onboardingDone = EncryptedPreferencesHelper.getBoolean(this, "onboarding_completed", false)
            
            if (!onboardingDone) {
                startActivity(Intent(this, OnboardingActivity::class.java))
                finish()
                return@postDelayed
            }

            val userRole = EncryptedPreferencesHelper.getString(this, "user_role")
            
            if (userRole != null) {
                CampanaService.startService(this)
            }

            val intent = when (userRole) {
                "monitor" -> Intent(this, MonitorMainActivity::class.java)
                "terminal" -> {
                    val capStatus = EncryptedPreferencesHelper.getString(this, "capabilities_status", "")
                    if (capStatus == "completed" || capStatus == "omitted") {
                        Intent(this, TerminalMainActivity::class.java)
                    } else {
                        Intent(this, CapabilitiesAssessmentActivity::class.java)
                    }
                }
                else -> Intent(this, OnboardingActivity::class.java)
            }
            
            startActivity(intent)
            finish()
        }, 2000)
    }

    private fun updateLocale(langCode: String) {
        val locale = Locale(langCode)
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }
}
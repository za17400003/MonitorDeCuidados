package com.example.monitordecuidados

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.monitordecuidados.adapters.LanguageAdapter
import com.example.monitordecuidados.databinding.ActivityLanguageSelectorBinding
import com.example.monitordecuidados.models.Language
import com.example.monitordecuidados.utils.EncryptedPreferencesHelper
import com.example.monitordecuidados.utils.StringsLocalizationManager
import java.util.*

class LanguageSelectorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLanguageSelectorBinding
    private var selectedLanguage: Language? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLanguageSelectorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val languages = listOf(
            Language("es", "Español", "🇪🇸"),
            Language("en", "English", "🇬🇧"),
            Language("fr", "Français", "🇫🇷"),
            Language("pt", "Português", "🇵🇹"),
            Language("de", "Deutsch", "🇩🇪"),
            Language("it", "Italiano", "🇮🇹")
        )

        val adapter = LanguageAdapter(languages) { language ->
            selectedLanguage = language
            updateLocale(language.code)
            // Trigger remote translation fetch
            StringsLocalizationManager.loadTranslations(language.code)
        }

        binding.rvLanguages.layoutManager = LinearLayoutManager(this)
        binding.rvLanguages.adapter = adapter

        binding.btnContinue.setOnClickListener {
            val fromSettings = intent.getBooleanExtra("fromSettings", false)
            selectedLanguage?.let {
                EncryptedPreferencesHelper.saveString(this, "app_language", it.code)
            } ?: run {
                // Default to Spanish if nothing selected but clicked continue
                EncryptedPreferencesHelper.saveString(this, "app_language", "es")
            }
            
            if (fromSettings) {
                finish() // Regresa a Settings
            } else {
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
        }
    }

    private fun updateLocale(langCode: String) {
        val locale = Locale(langCode)
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
        
        // Refresh UI texts
        binding.btnContinue.text = getString(R.string.next)
    }
}
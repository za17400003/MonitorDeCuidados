package com.example.monitordecuidados.fragments

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.example.monitordecuidados.*
import com.example.monitordecuidados.dialogs.CalibrationDialog
import com.example.monitordecuidados.dialogs.CreateAlarmDialog
import com.example.monitordecuidados.dialogs.CustomPhrasesDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class SettingsFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {

    companion object {
        private const val REQUEST_CODE_RINGTONE = 1001
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.sharedPreferencesName = "monitordecuidados_prefs"
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
        
        setupCommonPreferences()
        setupTerminalPreferences()

        findPreference<Preference>("clear_logs")?.setOnPreferenceClickListener {
            // Logic to clear logs
            true
        }

        findPreference<Preference>("view_alert_log")?.setOnPreferenceClickListener {
            startActivity(Intent(requireContext(), AlertLogActivity::class.java))
            true
        }
        
        // M15: Sync night_mode switch with actual theme state
        val isActuallyDarkMode = (resources.configuration.uiMode and 
            android.content.res.Configuration.UI_MODE_NIGHT_MASK) == 
            android.content.res.Configuration.UI_MODE_NIGHT_YES
        findPreference<SwitchPreferenceCompat>("night_mode")?.isChecked = isActuallyDarkMode
    }

    private fun setupCommonPreferences() {
        findPreference<Preference>("notification_ringtone")?.setOnPreferenceClickListener {
            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, getString(R.string.settings_change_sound))
            }
            startActivityForResult(intent, REQUEST_CODE_RINGTONE)
            true
        }

        findPreference<Preference>("view_alert_history_pref")?.setOnPreferenceClickListener {
            startActivity(Intent(requireContext(), AlertLogActivity::class.java))
            true
        }

        findPreference<Preference>("configure_reminders")?.setOnPreferenceClickListener {
            CreateAlarmDialog().show(parentFragmentManager, "configure_reminder")
            true
        }

        findPreference<Preference>("app_language")?.setOnPreferenceClickListener {
            // M12: Pass fromSettings extra
            startActivity(Intent(requireContext(), LanguageSelectorActivity::class.java).apply {
                putExtra("fromSettings", true)
            })
            true
        }
    }

    private fun setupTerminalPreferences() {
        val role = requireContext()
            .getSharedPreferences("monitordecuidados_prefs", Context.MODE_PRIVATE)
            .getString("user_role", "monitor")

        val terminalCategories = listOf(
            "terminal_device_cat", "terminal_sensors_cat", 
            "terminal_alarms_cat", "terminal_sound_cat", "terminal_extras_cat"
        )
        terminalCategories.forEach { key ->
            findPreference<PreferenceCategory>(key)?.isVisible = (role == "terminal")
        }

        // BUG T3: Ocultar categorías Monitor en Terminal y viceversa
        val monitorOnlyCategories = listOf("notif_cat_monitor")
        monitorOnlyCategories.forEach { key ->
            findPreference<PreferenceCategory>(key)?.isVisible = (role == "monitor")
        }

        val terminalOnlyExtraCategories = listOf("notif_cat_terminal")
        terminalOnlyExtraCategories.forEach { key ->
            findPreference<PreferenceCategory>(key)?.isVisible = (role == "terminal")
        }

        // Ocultar "Mi Dispositivo" en Terminal (se maneja desde toolbar/drawer ahora)
        findPreference<PreferenceCategory>("terminal_device_cat")?.isVisible = false

        findPreference<Preference>("terminal_change_pairing")?.setOnPreferenceClickListener {
            // BUG T7: Terminal muestra QR, no escanea. Navegar a pantalla principal que tiene el QR.
            requireActivity().finish()
            true
        }

        findPreference<Preference>("edit_custom_phrases")?.setOnPreferenceClickListener {
            CustomPhrasesDialog().show(parentFragmentManager, "custom_phrases")
            true
        }

        findPreference<Preference>("calibrate_voice")?.setOnPreferenceClickListener {
            CalibrationDialog().show(parentFragmentManager, "calibration")
            true
        }

        findPreference<Preference>("edit_alarms")?.setOnPreferenceClickListener {
            CreateAlarmDialog().show(parentFragmentManager, "create_alarm")
            true
        }

        findPreference<Preference>("change_bell_sound")?.setOnPreferenceClickListener {
            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, getString(R.string.settings_change_sound))
            }
            startActivityForResult(intent, REQUEST_CODE_RINGTONE)
            true
        }

        findPreference<Preference>("view_tutorial")?.setOnPreferenceClickListener {
            requireContext().getSharedPreferences("monitordecuidados_prefs", Context.MODE_PRIVATE)
                .edit().putBoolean("onboarding_completed", false).apply()
            startActivity(Intent(requireContext(), OnboardingActivity::class.java))
            requireActivity().finish()
            true
        }

        findPreference<Preference>("pause_service")?.setOnPreferenceClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.terminal_pause_service)
                .setMessage(R.string.terminal_pause_confirm)
                .setPositiveButton(R.string.terminal_pause_service) { _, _ ->
                    requireContext().stopService(Intent(requireContext(), CampanaService::class.java))
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
            true
        }

        // BUG T8: Device name dynamic summaries
        findPreference<Preference>("terminal_device_name")?.summary = Build.MODEL
        val monitorName = requireContext()
            .getSharedPreferences("monitordecuidados_prefs", Context.MODE_PRIVATE)
            .getString("paired_monitor_name", getString(R.string.not_linked))
        findPreference<Preference>("terminal_paired_monitor")?.summary = monitorName
    }

    override fun onResume() {
        super.onResume()
        preferenceScreen.sharedPreferences?.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        preferenceScreen.sharedPreferences?.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            "night_mode" -> {
                val nightMode = sharedPreferences?.getBoolean(key, false) ?: false
                AppCompatDelegate.setDefaultNightMode(
                    if (nightMode) AppCompatDelegate.MODE_NIGHT_YES
                    else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                    else AppCompatDelegate.MODE_NIGHT_NO
                )
            }
            "shake_detection_enabled" -> {
                // M10: Shake detection is now integrated in CampanaService
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_RINGTONE && resultCode == Activity.RESULT_OK) {
            val uri = data?.getParcelableExtra<android.net.Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            if (uri != null) {
                requireContext().getSharedPreferences("monitordecuidados_prefs", Context.MODE_PRIVATE)
                    .edit()
                    .putString("bell_ringtone_uri", uri.toString())
                    .apply()
            }
        }
    }
}
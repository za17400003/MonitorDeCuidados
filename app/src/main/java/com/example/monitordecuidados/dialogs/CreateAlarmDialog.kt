package com.example.monitordecuidados.dialogs

import android.app.AlarmManager
import android.app.Dialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.example.monitordecuidados.R
import com.example.monitordecuidados.data.local.Alarm
import com.example.monitordecuidados.data.local.AppDatabase
import com.example.monitordecuidados.databinding.DialogCreateAlarmBinding
import com.example.monitordecuidados.receivers.AlarmReceiver
import com.example.monitordecuidados.utils.EncryptedPreferencesHelper
import kotlinx.coroutines.launch
import java.util.*

class CreateAlarmDialog : DialogFragment() {

    private var _binding: DialogCreateAlarmBinding? = null
    private val binding get() = _binding!!
    private var selectedSoundUri: Uri? = null

    companion object {
        private const val REQUEST_CODE_RINGTONE = 1001
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogCreateAlarmBinding.inflate(layoutInflater)

        val repeatOptions = arrayOf(
            getString(R.string.once),
            getString(R.string.daily),
            getString(R.string.weekdays)
        )
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, repeatOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerRepeat.adapter = adapter

        binding.btnSound.setOnClickListener {
            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, getString(R.string.select_sound))
                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, selectedSoundUri)
            }
            startActivityForResult(intent, REQUEST_CODE_RINGTONE)
        }

        binding.btnCancel.setOnClickListener { dismiss() }

        binding.btnSave.setOnClickListener {
            saveAlarm()
        }

        return AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .create()
    }

    private fun saveAlarm() {
        val hour = binding.timePicker.hour
        val minute = binding.timePicker.minute
        val message = binding.inputAlarmMessage.text.toString().ifEmpty { getString(R.string.app_name) }
        val repeatMode = when (binding.spinnerRepeat.selectedItemPosition) {
            1 -> "daily"
            2 -> "weekdays"
            else -> "once"
        }

        val prefs = requireContext().getSharedPreferences("monitordecuidados_prefs", Context.MODE_PRIVATE)
        val role = prefs.getString("user_role", "terminal")
        val createdBy = if (role == "monitor") "monitorId" else "terminalId"
        val terminalId = if (role == "terminal") "local" else EncryptedPreferencesHelper.getString(requireContext(), "paired_terminal_id", null)
        val terminalName = if (role == "terminal") "Local" else EncryptedPreferencesHelper.getString(requireContext(), "paired_terminal_name", "Terminal") ?: "Terminal"

        val alarm = Alarm(
            hour = hour,
            minute = minute,
            message = message,
            soundUri = selectedSoundUri?.toString(),
            repeatMode = repeatMode,
            createdBy = createdBy,
            terminalId = terminalId
        )

        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())
            val id = db.alarmDao().insertAlarm(alarm)
            val savedAlarm = alarm.copy(id = id.toInt())
            scheduleAlarm(savedAlarm, terminalName)
            dismiss()
        }
    }

    private fun scheduleAlarm(alarm: Alarm, terminalName: String) {
        val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(requireContext(), AlarmReceiver::class.java).apply {
            putExtra("alarm_id", alarm.id)
            putExtra("message", alarm.message)
            putExtra("terminal_name", terminalName)
        }

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
            set(Calendar.SECOND, 0)
            if (before(Calendar.getInstance())) {
                add(Calendar.DATE, 1)
            }
        }

        val pendingIntent = PendingIntent.getBroadcast(
            requireContext(), alarm.id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_RINGTONE && resultCode == android.app.Activity.RESULT_OK) {
            selectedSoundUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }
            binding.btnSound.text = getString(R.string.success)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

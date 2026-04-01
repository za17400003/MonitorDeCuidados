package com.example.monitordecuidados

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.monitordecuidados.databinding.ActivityAlertLogBinding
import com.example.monitordecuidados.models.NotificationState
import com.example.monitordecuidados.viewmodels.NotificationViewModel
import com.example.monitordecuidados.adapters.NotificationAlertAdapter

/**
 * Muestra el historial de alertas con identificación de terminal.
 * Phase 5: Integrated with NotificationViewModel.
 */
class AlertLogActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAlertLogBinding
    private val notificationViewModel: NotificationViewModel by viewModels()
    private lateinit var adapter: NotificationAlertAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAlertLogBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        adapter = NotificationAlertAdapter()
        binding.rvAlerts.layoutManager = LinearLayoutManager(this)
        binding.rvAlerts.adapter = adapter

        setupObservers()
        notificationViewModel.observeRealtimeNotifications()
    }

    private fun setupObservers() {
        notificationViewModel.notificationList.observe(this) { list ->
            adapter.submitList(list)
        }

        notificationViewModel.notificationState.observe(this) { state ->
            when (state) {
                is NotificationState.Loading -> {
                    // Show progress
                }
                is NotificationState.Empty -> {
                    Toast.makeText(this, "No notifications found", Toast.LENGTH_SHORT).show()
                }
                is NotificationState.Error -> {
                    Toast.makeText(this, "Error: ${state.message}", Toast.LENGTH_SHORT).show()
                }
                else -> {}
            }
        }
    }
}
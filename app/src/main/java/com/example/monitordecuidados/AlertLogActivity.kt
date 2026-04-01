package com.example.monitordecuidados

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.monitordecuidados.databinding.ActivityAlertLogBinding
import com.example.monitordecuidados.models.NotificationItem
import com.example.monitordecuidados.models.NotificationState
import com.example.monitordecuidados.viewmodels.NotificationViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * Muestra el historial de alertas con identificación de terminal.
 * Phase 5: Integrated with NotificationViewModel.
 */
class AlertLogActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAlertLogBinding
    private val notificationViewModel: NotificationViewModel by viewModels()
    private lateinit var adapter: AlertAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAlertLogBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        adapter = AlertAdapter()
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

    class AlertAdapter : RecyclerView.Adapter<AlertAdapter.ViewHolder>() {
        private var items = listOf<NotificationItem>()

        fun submitList(newItems: List<NotificationItem>) {
            items = newItems
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_alert_campana, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            
            holder.tvTerminalName?.text = item.title
            holder.tvAlertMessage.text = item.body
            holder.tvAlertTime.text = item.timestamp?.let { sdf.format(it.toDate()) } ?: ""
        }

        override fun getItemCount() = items.size

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvTerminalName: TextView? = view.findViewById(R.id.tvTerminalName)
            val tvAlertMessage: TextView = view.findViewById(R.id.tvAlertMessage)
            val tvAlertTime: TextView = view.findViewById(R.id.tvAlertTime)
        }
    }
}

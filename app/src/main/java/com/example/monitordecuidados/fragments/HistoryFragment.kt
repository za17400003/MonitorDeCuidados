package com.example.monitordecuidados.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.monitordecuidados.R
import com.example.monitordecuidados.databinding.ActivityAlertLogBinding
import com.example.monitordecuidados.models.NotificationItem
import com.example.monitordecuidados.viewmodels.NotificationViewModel
import java.text.SimpleDateFormat
import java.util.*

class HistoryFragment : Fragment(R.layout.activity_alert_log) {

    private var _binding: ActivityAlertLogBinding? = null
    private val binding get() = _binding!!
    
    private val notificationViewModel: NotificationViewModel by viewModels({ requireActivity() })
    private lateinit var adapter: AlertAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = ActivityAlertLogBinding.bind(view)

        // Hide toolbar if embedded in Fragment with its own toolbar
        binding.toolbar.visibility = View.GONE

        adapter = AlertAdapter()
        binding.rvAlerts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAlerts.adapter = adapter

        notificationViewModel.notificationList.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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
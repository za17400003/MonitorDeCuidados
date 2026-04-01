package com.example.monitordecuidados.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.monitordecuidados.R
import com.example.monitordecuidados.databinding.ActivityAlertLogBinding
import com.example.monitordecuidados.viewmodels.NotificationViewModel
import com.example.monitordecuidados.adapters.NotificationAlertAdapter

class HistoryFragment : Fragment(R.layout.activity_alert_log) {

    private var _binding: ActivityAlertLogBinding? = null
    private val binding get() = _binding!!
    
    private val notificationViewModel: NotificationViewModel by viewModels({ requireActivity() })
    private lateinit var adapter: NotificationAlertAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = ActivityAlertLogBinding.bind(view)

        // Hide toolbar if embedded in Fragment with its own toolbar
        binding.toolbar.visibility = View.GONE

        adapter = NotificationAlertAdapter()
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
}
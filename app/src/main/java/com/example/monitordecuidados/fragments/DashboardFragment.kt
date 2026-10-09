package com.example.monitordecuidados.fragments

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.monitordecuidados.QRScannerActivity
import com.example.monitordecuidados.R
import com.example.monitordecuidados.TerminalDetailActivity
import com.example.monitordecuidados.adapters.AlertAdapter
import com.example.monitordecuidados.adapters.TerminalStatusAdapter
import com.example.monitordecuidados.adapters.TerminalInfo
import com.example.monitordecuidados.data.local.Event
import com.example.monitordecuidados.databinding.LayoutMonitorContentBinding
import com.example.monitordecuidados.utils.EncryptedPreferencesHelper
import com.example.monitordecuidados.viewmodels.ConnectionViewModel
import com.example.monitordecuidados.viewmodels.NotificationViewModel
import org.json.JSONObject
import java.net.URL

class DashboardFragment : Fragment(R.layout.layout_monitor_content) {

    private var _binding: LayoutMonitorContentBinding? = null
    private val binding get() = _binding!!

    private val notificationViewModel: NotificationViewModel by viewModels({ requireActivity() })
    private val connectionViewModel: ConnectionViewModel by viewModels({ requireActivity() })

    private lateinit var alertAdapter: AlertAdapter
    private lateinit var terminalAdapter: TerminalStatusAdapter

    // T61: Polling terminals battery/status
    private val pollHandler = Handler(Looper.getMainLooper())
    private var currentTerminals: List<TerminalInfo> = emptyList()
    private val pollRunnable = object : Runnable {
        override fun run() {
            if (currentTerminals.isNotEmpty()) {
                queryTerminalStatuses(currentTerminals)
            }
            pollHandler.postDelayed(this, 10000) // Cada 10 segundos
        }
    }

    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 3001
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = LayoutMonitorContentBinding.bind(view)

        setupRecyclerViews()
        setupObservers()
        setupListeners()
    }

    private fun setupRecyclerViews() {
        // Alertas Activas - T84: Pasar callbacks para LLAMAR/MONITOREAR
        alertAdapter = AlertAdapter(
            alerts = emptyList(),
            onCallClick = { event -> navigateToTerminalDetail(event, autoCall = true) },
            onMonitorClick = { event -> navigateToMonitor(event) }
        )
        binding.rvActiveAlerts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvActiveAlerts.adapter = alertAdapter

        // Mis Terminales - T36: Navegación al detalle
        terminalAdapter = TerminalStatusAdapter(emptyList()) { terminal ->
            val intent = Intent(requireContext(), TerminalDetailActivity::class.java).apply {
                putExtra("terminal_id", terminal.id) // T70: DashboardFragment pase terminal_id
                putExtra("terminal_name", terminal.name)
                putExtra("terminal_status", terminal.status)
            }
            startActivity(intent)
        }
        binding.rvTerminals.layoutManager = LinearLayoutManager(requireContext())
        binding.rvTerminals.adapter = terminalAdapter
    }

    private fun navigateToTerminalDetail(event: Event, autoCall: Boolean = false) {
        startActivity(Intent(requireContext(), TerminalDetailActivity::class.java).apply {
            putExtra("terminal_id", event.sourceIp)
            putExtra("terminal_name", event.sourceTerminalName.ifBlank { "Terminal" })
            putExtra("terminal_status", "connected")
            if (autoCall) putExtra("auto_call", true)
        })
    }

    private fun navigateToMonitor(event: Event) {
        val ip = event.sourceIp
        startActivity(Intent(requireContext(), com.example.monitordecuidados.VideoActivity::class.java).apply {
            putExtra("mode", "monitor")
            putExtra("remote_ip", ip)
        })
    }

    private fun setupObservers() {
        // T69: DashboardFragment LOCAL-FIRST — leer alertas de Room DB
        notificationViewModel.notificationList.observe(viewLifecycleOwner) { events ->
            alertAdapter.updateData(events)
            binding.tvNoAlerts.visibility = if (events.isEmpty()) View.VISIBLE else View.GONE
        }

        connectionViewModel.pairingsList.observe(viewLifecycleOwner) { pairings ->
            var terminals = pairings.map {
                TerminalInfo(
                    id = it.terminalId, // T70 requirement
                    name = it.name ?: "Terminal",
                    status = it.status ?: "disconnected",
                    batteryPercent = it.batteryLevel ?: 0,
                    connectionType = it.connectionType ?: "None"
                )
            }
            
            // T35: Fallback local
            if (terminals.isEmpty()) {
                val localTerminalId = EncryptedPreferencesHelper.getString(requireContext(), "paired_terminal_id")
                if (!localTerminalId.isNullOrEmpty()) {
                    val localName = EncryptedPreferencesHelper.getString(requireContext(), "paired_terminal_name") ?: "Terminal"
                    terminals = listOf(
                        TerminalInfo(
                            id = localTerminalId,
                            name = localName,
                            status = "disconnected",
                            batteryPercent = 0,
                            connectionType = "internet"
                        )
                    )
                }
            }

            currentTerminals = terminals
            terminalAdapter.updateData(terminals)
            binding.tvNoTerminals.visibility = if (terminals.isEmpty()) View.VISIBLE else View.GONE
            
            if (terminals.isNotEmpty()) {
                queryTerminalStatuses(terminals)
            }
        }

        connectionViewModel.getPairingsList()
    }

    private fun queryTerminalStatuses(terminals: List<TerminalInfo>) {
        for (terminal in terminals) {
            val terminalIp = EncryptedPreferencesHelper.getString(requireContext(), "paired_terminal_ip")
            if (terminalIp == null) continue
            
            Thread {
                try {
                    val url = URL("http://$terminalIp:8080/status")
                    val conn = url.openConnection() as java.net.HttpURLConnection
                    conn.requestMethod = "GET"
                    conn.connectTimeout = 2000
                    conn.readTimeout = 2000
                    if (conn.responseCode == 200) {
                        val response = conn.inputStream.bufferedReader().readText()
                        val json = JSONObject(response)
                        val batteryLevel = json.optInt("batteryLevel", 0)
                        val status = json.optString("status", "connected")
                        
                        activity?.runOnUiThread {
                            val updated = terminal.copy(
                                batteryPercent = batteryLevel,
                                status = status,
                                connectionType = "local"
                            )
                            val adapter = binding.rvTerminals.adapter as? TerminalStatusAdapter
                            adapter?.let {
                                val currentList = it.getData().toMutableList()
                                val idx = currentList.indexOfFirst { t -> t.id == terminal.id }
                                if (idx >= 0) {
                                    currentList[idx] = updated
                                    it.updateData(currentList)
                                }
                            }
                        }
                    }
                    conn.disconnect()

                    // T75: Periodic heal
                    try {
                        val myIp = com.example.monitordecuidados.utils.NetworkUtils.getLocalIpAddress()
                        if (myIp != null) {
                            val healUrl = java.net.URL("http://$terminalIp:8080/confirm_pairing")
                            val healConn = healUrl.openConnection() as java.net.HttpURLConnection
                            healConn.requestMethod = "POST"
                            healConn.connectTimeout = 2000
                            healConn.setRequestProperty("Content-Type", "application/json")
                            healConn.doOutput = true
                            val healJson = org.json.JSONObject().apply {
                                put("monitorName", android.os.Build.MODEL)
                                put("monitorIp", myIp)
                            }
                            healConn.outputStream.write(healJson.toString().toByteArray())
                            healConn.responseCode // Consume response
                            healConn.disconnect()
                        }
                    } catch (_: Exception) { }
                } catch (e: Exception) { }
            }.start()
        }
    }

    override fun onResume() {
        super.onResume()
        connectionViewModel.getPairingsList()
        pollHandler.post(pollRunnable)
    }

    override fun onPause() {
        super.onPause()
        pollHandler.removeCallbacks(pollRunnable)
    }

    private fun setupListeners() {
        binding.btnLinkTerminal.setOnClickListener {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) 
                    == PackageManager.PERMISSION_GRANTED) {
                startActivity(Intent(requireContext(), QRScannerActivity::class.java))
            } else {
                requestPermissions(arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startActivity(Intent(requireContext(), QRScannerActivity::class.java))
            } else {
                Toast.makeText(requireContext(), "Se necesita la cámara para escanear el código QR", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        pollHandler.removeCallbacks(pollRunnable)
        super.onDestroyView()
        _binding = null
    }
}

package com.example.monitordecuidados.fragments

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.monitordecuidados.QRScannerActivity
import com.example.monitordecuidados.R
import com.example.monitordecuidados.TerminalDetailActivity
import com.example.monitordecuidados.VideoActivity
import com.example.monitordecuidados.adapters.AlertAdapter
import com.example.monitordecuidados.adapters.TerminalStatusAdapter
import com.example.monitordecuidados.databinding.LayoutMonitorContentBinding
import com.example.monitordecuidados.viewmodels.ConnectionViewModel
import com.example.monitordecuidados.viewmodels.NotificationViewModel

class DashboardFragment : Fragment(R.layout.layout_monitor_content) {

    private var _binding: LayoutMonitorContentBinding? = null
    private val binding get() = _binding!!

    private val notificationViewModel: NotificationViewModel by viewModels({ requireActivity() })
    private val connectionViewModel: ConnectionViewModel by viewModels({ requireActivity() })

    private lateinit var alertAdapter: AlertAdapter
    private lateinit var terminalAdapter: TerminalStatusAdapter

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
        // Alertas Activas
        alertAdapter = AlertAdapter(emptyList())
        binding.rvActiveAlerts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvActiveAlerts.adapter = alertAdapter

        // Mis Terminales - T36: Navegación al detalle
        terminalAdapter = TerminalStatusAdapter(emptyList()) { terminal ->
            val intent = Intent(requireContext(), TerminalDetailActivity::class.java).apply {
                putExtra("terminal_name", terminal.name)
                putExtra("terminal_status", terminal.status)
            }
            startActivity(intent)
        }
        binding.rvTerminals.layoutManager = LinearLayoutManager(requireContext())
        binding.rvTerminals.adapter = terminalAdapter
    }

    private fun setupObservers() {
        notificationViewModel.notificationList.observe(viewLifecycleOwner) { list ->
            val alerts = list.map {
                com.example.monitordecuidados.data.local.Event(
                    timestamp = it.timestamp?.toDate()?.time ?: 0L,
                    type = it.type ?: "unknown",
                    message = it.body ?: "",
                    sourceIp = "",
                    sourceTerminalName = it.title ?: "Desconocido"
                )
            }
            alertAdapter.updateData(alerts)
            binding.tvNoAlerts.visibility = if (alerts.isEmpty()) View.VISIBLE else View.GONE
        }

        connectionViewModel.pairingsList.observe(viewLifecycleOwner) { pairings ->
            var terminals = pairings.map {
                com.example.monitordecuidados.adapters.TerminalInfo(
                    name = it.name ?: "Terminal",
                    status = it.status ?: "disconnected",
                    batteryPercent = it.batteryLevel ?: 0,
                    connectionType = it.connectionType ?: "None"
                )
            }
            
            // T35: Fallback local — si Firestore no devolvió terminales, mostrar la terminal vinculada desde SharedPreferences
            if (terminals.isEmpty()) {
                val localTerminalId = com.example.monitordecuidados.utils.EncryptedPreferencesHelper
                    .getString(requireContext(), "paired_terminal_id")
                if (!localTerminalId.isNullOrEmpty()) {
                    val localName = com.example.monitordecuidados.utils.EncryptedPreferencesHelper
                        .getString(requireContext(), "paired_terminal_name") ?: "Terminal"
                    terminals = listOf(
                        com.example.monitordecuidados.adapters.TerminalInfo(
                            name = localName,
                            status = "disconnected",
                            batteryPercent = 0,
                            connectionType = "internet"
                        )
                    )
                }
            }

            terminalAdapter.updateData(terminals)
            binding.tvNoTerminals.visibility = if (terminals.isEmpty()) View.VISIBLE else View.GONE
            
            // T39-D: Consultar estado real de cada terminal
            if (terminals.isNotEmpty()) {
                queryTerminalStatuses(terminals)
            }
        }

        connectionViewModel.getPairingsList()
    }

    private fun queryTerminalStatuses(terminals: List<com.example.monitordecuidados.adapters.TerminalInfo>) {
        for (terminal in terminals) {
            val terminalIp = com.example.monitordecuidados.utils.EncryptedPreferencesHelper
                .getString(requireContext(), "paired_terminal_ip")
            if (terminalIp == null) continue
            
            Thread {
                try {
                    val url = java.net.URL("http://$terminalIp:8080/status")
                    val conn = url.openConnection() as java.net.HttpURLConnection
                    conn.requestMethod = "GET"
                    conn.connectTimeout = 2000
                    conn.readTimeout = 2000
                    if (conn.responseCode == 200) {
                        val response = conn.inputStream.bufferedReader().readText()
                        val json = org.json.JSONObject(response)
                        val batteryLevel = json.optInt("batteryLevel", 0)
                        val status = json.optString("status", "connected")
                        
                        activity?.runOnUiThread {
                            val updated = terminal.copy(
                                batteryPercent = batteryLevel,
                                status = status,
                                connectionType = "local"
                            )
                            val adapter = binding.rvTerminals.adapter as? com.example.monitordecuidados.adapters.TerminalStatusAdapter
                            adapter?.let {
                                val currentList = it.getData().toMutableList()
                                val idx = currentList.indexOfFirst { t -> t.name == terminal.name }
                                if (idx >= 0) {
                                    currentList[idx] = updated
                                    it.updateData(currentList)
                                }
                            }
                        }
                    }
                    conn.disconnect()
                } catch (e: Exception) {
                    // Silent catch as per T39-D
                }
            }.start()
        }
    }

    override fun onResume() {
        super.onResume()
        connectionViewModel.getPairingsList()
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
                if (!shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                    Toast.makeText(requireContext(), 
                        "Permiso de cámara requerido. Actívalo en Ajustes → Permisos", 
                        Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(requireContext(), 
                        "Se necesita la cámara para escanear el código QR", 
                        Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
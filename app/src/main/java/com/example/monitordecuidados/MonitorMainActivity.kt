package com.example.monitordecuidados

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.monitordecuidados.databinding.ActivityMonitorMainBinding
import com.example.monitordecuidados.fragments.DashboardFragment
import com.example.monitordecuidados.fragments.HistoryFragment
import com.example.monitordecuidados.fragments.SettingsFragment
import com.example.monitordecuidados.viewmodels.NotificationViewModel
import com.example.monitordecuidados.viewmodels.UserViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth

/**
 * Main activity for the Monitor role.
 * Phase 3.6: BottomNav + Fragments integration.
 * Bug M3/M4/M14/M16 refactored.
 */
class MonitorMainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMonitorMainBinding
    
    private val userViewModel: UserViewModel by viewModels()
    private val notificationViewModel: NotificationViewModel by viewModels()

    private val TAG = "MonitorMainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            binding = ActivityMonitorMainBinding.inflate(layoutInflater)
            setContentView(binding.root)

            // T38-C: Ensure CampanaService is running for Monitor
            if (!CampanaService.isRunning) {
                CampanaService.startService(this)
            }

            // T63: Auto-heal pairing — asegurar que Terminal tiene nuestro IP
            healPairingIfNeeded()

            setupUI()
            setupObservers()
            
            // Load default fragment
            if (savedInstanceState == null) {
                loadFragment(DashboardFragment())
            }
            
            notificationViewModel.observeRealtimeNotifications()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate", e)
        }
    }

    private fun setupObservers() {
        notificationViewModel.unreadCount.observe(this) { count ->
            binding.tvBadgeCount.visibility = if (count > 0) View.VISIBLE else View.GONE
            binding.tvBadgeCount.text = count.toString()
        }
    }

    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        
        binding.toolbar.setNavigationOnClickListener {
            binding.drawerLayout.open()
        }

        // M14: Drawer header dynamic info
        val headerView = binding.navigationView.getHeaderView(0)
        val currentUser = FirebaseAuth.getInstance().currentUser
        val userEmail = currentUser?.email
        headerView.findViewById<TextView>(R.id.headerEmail).text = userEmail ?: getString(R.string.app_name)
        
        val displayName = currentUser?.displayName
        headerView.findViewById<TextView>(R.id.headerTitle).text =
            if (!displayName.isNullOrEmpty()) displayName else android.os.Build.MODEL

        binding.navigationView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_current_rol -> {
                    startActivity(Intent(this, RoleSelectorActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
                    finish()
                }
                R.id.nav_settings -> {
                    loadFragment(SettingsFragment())
                }
                R.id.nav_about -> {
                    MaterialAlertDialogBuilder(this)
                        .setTitle(getString(R.string.about))
                        .setMessage(getString(R.string.about_message))
                        .setPositiveButton(android.R.string.ok, null)
                        .show()
                }
                R.id.nav_logout -> {
                    userViewModel.logout()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
            }
            binding.drawerLayout.close()
            true
        }
        
        // M16: Notifications button toggle
        binding.btnNotificationBadge.setOnClickListener {
            val currentFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
            if (currentFragment is HistoryFragment) {
                loadFragment(DashboardFragment())
            } else {
                loadFragment(HistoryFragment())
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    /**
     * T63: Si ya tenemos paired_terminal_ip, enviar /confirm_pairing al Terminal
     * para asegurar que Terminal tiene nuestro paired_monitor_ip.
     * Esto cura emparejamientos rotos del pairing viejo (fire-and-forget).
     */
    private fun healPairingIfNeeded() {
        val terminalIp = com.example.monitordecuidados.utils.EncryptedPreferencesHelper
            .getString(this, "paired_terminal_ip") ?: return
        val myIp = com.example.monitordecuidados.utils.NetworkUtils.getLocalIpAddress() ?: return
        val myName = android.os.Build.MODEL

        Log.d(TAG, "T68-TRACE: healPairingIfNeeded called. terminalIp=$terminalIp, myIp=$myIp")

        Thread {
            try {
                val url = java.net.URL("http://$terminalIp:8080/confirm_pairing")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "POST"
                conn.connectTimeout = 3000
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true
                val json = org.json.JSONObject().apply {
                    put("monitorName", myName)
                    put("monitorIp", myIp)
                }
                conn.outputStream.write(json.toString().toByteArray())
                val code = conn.responseCode
                conn.disconnect()
                Log.d(TAG, "T63 pairing heal: sent to $terminalIp, response=$code")
            } catch (e: Exception) {
                Log.w(TAG, "T63 pairing heal failed (Terminal may be offline): ${e.message}")
            }
        }.start()
    }
}

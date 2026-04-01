package com.example.monitordecuidados

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.monitordecuidados.databinding.ActivityTerminalMainBinding
import com.example.monitordecuidados.fragments.SettingsFragment
import com.example.monitordecuidados.fragments.TerminalQRFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth

class TerminalMainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTerminalMainBinding

    companion object {
        private const val REQUEST_AUDIO_PERMISSION = 2001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTerminalMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupUI()
        if (savedInstanceState == null) loadFragment(TerminalQRFragment())
        
        // T33: Solicitar permisos antes de iniciar el servicio para asegurar que las funciones dependientes se activen correctamente
        requestAudioPermissionIfNeeded()
        CampanaService.startService(this)
    }

    private fun requestAudioPermissionIfNeeded() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_AUDIO_PERMISSION)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_AUDIO_PERMISSION) {
            val prefs = getSharedPreferences("monitordecuidados_prefs", MODE_PRIVATE)
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permiso otorgado — asegurar que el detector de voz esté habilitado si el usuario lo desea
                prefs.edit().putBoolean("voice_detection_enabled", true).apply()
                CampanaService.refreshService(this)
            } else {
                // Permiso denegado — forzar desactivación de la función de voz
                prefs.edit().putBoolean("voice_detection_enabled", false).apply()
                CampanaService.refreshService(this)
                Toast.makeText(this, "El detector de voz requiere permiso de micrófono para funcionar", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        
        // M14: Toolbar title fixed
        binding.toolbar.title = getString(R.string.app_name)
        
        binding.toolbar.setNavigationOnClickListener { binding.drawerLayout.open() }

        // M14: Drawer header dynamic info
        val headerView = binding.navigationView.getHeaderView(0)
        val prefs = getSharedPreferences("monitordecuidados_prefs", MODE_PRIVATE)
        val personName = prefs.getString("terminal_person_name", null)
        headerView.findViewById<TextView>(R.id.headerTitle).text =
            if (!personName.isNullOrEmpty()) personName else Build.MODEL
            
        val userEmail = FirebaseAuth.getInstance().currentUser?.email
        headerView.findViewById<TextView>(R.id.headerEmail).text =
            userEmail ?: getString(R.string.app_name)

        binding.navigationView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_current_rol -> {
                    startActivity(Intent(this, RoleSelectorActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
                    finish()
                }
                R.id.nav_settings -> loadFragment(SettingsFragment())
                R.id.nav_about -> {
                    MaterialAlertDialogBuilder(this)
                        .setTitle(getString(R.string.about))
                        .setMessage(getString(R.string.about_message))
                        .setPositiveButton(android.R.string.ok, null)
                        .show()
                }
                R.id.nav_logout -> {
                    FirebaseAuth.getInstance().signOut()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
            }
            binding.drawerLayout.close()
            true
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}
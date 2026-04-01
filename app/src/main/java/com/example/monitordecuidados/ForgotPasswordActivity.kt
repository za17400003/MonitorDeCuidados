package com.example.monitordecuidados

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.monitordecuidados.databinding.ActivityForgotPasswordBinding
import com.google.firebase.auth.FirebaseAuth

class ForgotPasswordActivity : AppCompatActivity() {
    private lateinit var binding: ActivityForgotPasswordBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        binding.toolbarForgotPassword.setNavigationOnClickListener { finish() }
        binding.tvBackToLogin.setOnClickListener { finish() }
        
        binding.btnSendReset.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            if (email.isEmpty()) {
                binding.tilEmail.error = "Ingresa tu email"
                return@setOnClickListener
            }
            binding.tilEmail.error = null
            binding.btnSendReset.isEnabled = false
            
            FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                .addOnSuccessListener {
                    Toast.makeText(this, "Enlace enviado a $email", Toast.LENGTH_LONG).show()
                    finish()
                }
                .addOnFailureListener {
                    binding.btnSendReset.isEnabled = true
                    binding.tilEmail.error = "Error: ${it.localizedMessage}"
                }
        }
    }
}
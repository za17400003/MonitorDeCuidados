package com.example.monitordecuidados.fragments

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.monitordecuidados.databinding.FragmentRegisterBinding
import com.example.monitordecuidados.utils.EncryptedPreferencesHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.example.monitordecuidados.RoleSelectorActivity
import com.example.monitordecuidados.R

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = Firebase.auth

        binding.tvTermsLink.setOnClickListener {
            showTermsDialog()
        }

        binding.btnRegister.setOnClickListener {
            val fullName = binding.etFullName.text.toString()
            val email = binding.etEmailRegister.text.toString()
            val password = binding.etPasswordRegister.text.toString()
            val confirmPassword = binding.etConfirmPassword.text.toString()

            if (fullName.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty()) {
                if (password == confirmPassword) {
                    if (binding.cbTerms.isChecked) {
                        auth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener(requireActivity()) { task ->
                                if (task.isSuccessful) {
                                    EncryptedPreferencesHelper.saveString(requireContext(), "user_email", email)
                                    
                                    val intent = Intent(requireContext(), RoleSelectorActivity::class.java)
                                    startActivity(intent)
                                    requireActivity().finish()
                                } else {
                                    Toast.makeText(requireContext(), "Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                    } else {
                        Toast.makeText(requireContext(), getString(R.string.accept_terms), Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), getString(R.string.passwords_dont_match), Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), getString(R.string.fill_all_fields), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showTermsDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_terms, null)
        val tvContent = dialogView.findViewById<TextView>(R.id.tvTermsContent)
        val btnClose = dialogView.findViewById<View>(R.id.btnCloseTerms)

        tvContent.text = "TÉRMINOS Y CONDICIONES DE USO - MONITOR DE CUIDADOS v2.0..."

        val dialog = AlertDialog.Builder(requireContext(), android.R.style.Theme_Material_NoActionBar_Fullscreen)
            .setView(dialogView)
            .create()

        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

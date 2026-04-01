package com.example.monitordecuidados.fragments

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.example.monitordecuidados.databinding.FragmentLoginBinding
import com.example.monitordecuidados.utils.EncryptedPreferencesHelper
import com.example.monitordecuidados.RoleSelectorActivity
import com.example.monitordecuidados.ForgotPasswordActivity
import com.example.monitordecuidados.R
import com.example.monitordecuidados.cloud.FirebaseService
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    
    private var auth: FirebaseAuth? = null
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var googleSignInLauncher: ActivityResultLauncher<Intent>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        try {
            if (FirebaseApp.getApps(requireContext()).isNotEmpty()) {
                auth = Firebase.auth
            } else {
                disableFirebaseButtons()
            }
        } catch (e: Exception) {
            disableFirebaseButtons()
        }
        
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .requestProfile()
            .build()
            
        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
        
        googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data = result.data
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                firebaseAuthWithGoogle(account)
            } catch (e: ApiException) {
                Toast.makeText(requireContext(), "Error al iniciar sesión con Google: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()

            val currentAuth = auth
            if (currentAuth != null) {
                if (email.isNotEmpty() && password.isNotEmpty()) {
                    currentAuth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(requireActivity()) { task ->
                            if (task.isSuccessful) {
                                EncryptedPreferencesHelper.saveString(requireContext(), "user_email", email)
                                if (binding.cbRememberMe.isChecked) {
                                    EncryptedPreferencesHelper.saveBoolean(requireContext(), "remember_me", true)
                                }
                                syncUserDataToFirestore()
                                readPreferencesFromCloud()
                                navigateToRoleSelector()
                            } else {
                                Toast.makeText(requireContext(), "Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                } else {
                    Toast.makeText(requireContext(), getString(R.string.fill_all_fields), Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), "Firebase no configurado. Usa el Modo Local.", Toast.LENGTH_LONG).show()
            }
        }
        
        binding.btnGoogleLogin.setOnClickListener {
            if (auth != null) {
                val signInIntent = googleSignInClient.signInIntent
                googleSignInLauncher.launch(signInIntent)
            } else {
                Toast.makeText(requireContext(), "Google Sign-In no disponible sin Firebase.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnLocalMode.setOnClickListener {
            navigateToRoleSelector()
        }

        binding.tvForgotPassword.setOnClickListener {
            startActivity(Intent(requireContext(), ForgotPasswordActivity::class.java))
        }
    }

    private fun syncUserDataToFirestore() {
        val user = auth?.currentUser ?: return
        val prefs = requireContext().getSharedPreferences("monitordecuidados_prefs", Context.MODE_PRIVATE)
        val userRole = prefs.getString("user_role", "UNKNOWN")

        val userData = mapOf(
            "uid" to user.uid,
            "email" to user.email,
            "fullName" to user.displayName,
            "role" to userRole,
            "photoURL" to user.photoUrl?.toString(),
            "lastLogin" to FieldValue.serverTimestamp(),
            "preferences" to mapOf(
                "autoAcceptCalls" to prefs.getBoolean("terminal_auto_answer", true),
                "enableShakeDetection" to prefs.getBoolean("shake_detection_enabled", true),
                "notificationSound" to prefs.getBoolean("notifications_sound", true)
            )
        )
        
        FirebaseService.db.collection("users")
            .document(user.uid)
            .set(userData, SetOptions.merge())
    }

    private fun readPreferencesFromCloud() {
        val userId = FirebaseService.getUserId() ?: return

        FirebaseService.db.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { snapshot ->
                @Suppress("UNCHECKED_CAST")
                val prefs = snapshot.get("preferences") as? Map<String, Any>
                if (prefs != null) {
                    val sharedPref = requireContext().getSharedPreferences("monitordecuidados_prefs", Context.MODE_PRIVATE)
                    val editor = sharedPref.edit()
                    
                    editor.putBoolean("terminal_auto_answer", prefs["autoAcceptCalls"] as? Boolean ?: true)
                    editor.putBoolean("shake_detection_enabled", prefs["enableShakeDetection"] as? Boolean ?: true)
                    editor.putBoolean("notifications_sound", prefs["notificationSound"] as? Boolean ?: true)
                    editor.apply()
                    
                    Log.d("LoginFragment", "Cloud preferences applied locally")
                }
            }
            .addOnFailureListener { exception ->
                Log.e("LoginFragment", "Failed to read preferences: ${exception.message}")
            }
    }

    private fun disableFirebaseButtons() {
        binding.btnLogin.alpha = 0.5f
        binding.btnGoogleLogin.alpha = 0.5f
    }

    private fun firebaseAuthWithGoogle(acct: GoogleSignInAccount) {
        val currentAuth = auth ?: return
        val credential = GoogleAuthProvider.getCredential(acct.idToken, null)
        currentAuth.signInWithCredential(credential)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    EncryptedPreferencesHelper.saveString(requireContext(), "user_email", acct.email)
                    syncUserDataToFirestore()
                    readPreferencesFromCloud()
                    navigateToRoleSelector()
                } else {
                    Toast.makeText(requireContext(), "Error de autenticación: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun navigateToRoleSelector() {
        val intent = Intent(requireContext(), RoleSelectorActivity::class.java)
        startActivity(intent)
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

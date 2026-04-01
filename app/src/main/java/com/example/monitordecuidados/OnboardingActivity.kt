package com.example.monitordecuidados

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.monitordecuidados.databinding.ActivityOnboardingBinding
import com.example.monitordecuidados.utils.EncryptedPreferencesHelper
import com.google.android.material.tabs.TabLayoutMediator

class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    private var userRole: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val adapter = OnboardingPagerAdapter(this)
        binding.viewPagerOnboarding.adapter = adapter
        binding.viewPagerOnboarding.isUserInputEnabled = false // Control navigation via buttons

        // Solo mostrar dots si hay más de 1 página
        binding.tabDots.visibility = if (adapter.itemCount > 1) View.VISIBLE else View.GONE
        TabLayoutMediator(binding.tabDots, binding.viewPagerOnboarding) { _, _ -> }.attach()
    }

    fun nextStep() {
        val nextItem = binding.viewPagerOnboarding.currentItem + 1
        if (nextItem < binding.viewPagerOnboarding.adapter?.itemCount ?: 0) {
            binding.viewPagerOnboarding.currentItem = nextItem
        } else {
            finishOnboarding()
        }
    }

    fun setRole(role: String) {
        this.userRole = role
        EncryptedPreferencesHelper.saveString(this, "user_role", role)
        // T38-A: Sync to regular SharedPrefs for CampanaService
        getSharedPreferences("monitordecuidados_prefs", MODE_PRIVATE).edit()
            .putString("user_role", role)
            .apply()
        nextStep()
    }

    fun finishOnboarding() {
        EncryptedPreferencesHelper.saveBoolean(this, "onboarding_completed", true)
        // T38-B: Start CampanaService immediately after onboarding
        CampanaService.startService(this)
        val targetActivity = if (userRole == "monitor") {
            MonitorMainActivity::class.java
        } else {
            val capStatus = EncryptedPreferencesHelper.getString(this, "capabilities_status", "")
            if (capStatus == "completed" || capStatus == "omitted") {
                TerminalMainActivity::class.java
            } else {
                CapabilitiesAssessmentActivity::class.java
            }
        }
        startActivity(Intent(this, targetActivity))
        finish()
    }

    inner class OnboardingPagerAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {
        override fun getItemCount(): Int = 5

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> WelcomeFragment()
                1 -> RoleSelectionFragment()
                2 -> SetupFragment()
                3 -> CapabilitiesFragment()
                else -> TutorialFragment()
            }
        }
    }

    // Inner Fragments for simplicity in this implementation
    class WelcomeFragment : Fragment(R.layout.fragment_onboarding_welcome) {
        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            view.findViewById<Button>(R.id.btnStartSetup).setOnClickListener {
                (activity as? OnboardingActivity)?.nextStep()
            }
            view.findViewById<TextView>(R.id.tvSkip).setOnClickListener {
                (activity as? OnboardingActivity)?.finishOnboarding()
            }
        }
    }

    class RoleSelectionFragment : Fragment(R.layout.fragment_onboarding_role) {
        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            view.findViewById<View>(R.id.cardCaregiver).setOnClickListener {
                (activity as? OnboardingActivity)?.setRole("monitor")
            }
            view.findViewById<View>(R.id.cardPatient).setOnClickListener {
                (activity as? OnboardingActivity)?.setRole("terminal")
            }
        }
    }

    class SetupFragment : Fragment(R.layout.fragment_onboarding_setup) {
        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            val btnAction = view.findViewById<Button>(R.id.btnAction)
            btnAction.setOnClickListener {
                (activity as? OnboardingActivity)?.nextStep()
            }
        }
    }

    class CapabilitiesFragment : Fragment(R.layout.fragment_onboarding_tutorial) { // Reusing tutorial layout for simple evaluation step
        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            val title = view.findViewById<TextView>(R.id.tvTutorialTitle)
            title.text = "Evaluación Geriátrica"
            val btn = view.findViewById<Button>(R.id.btnStartUsing)
            btn.text = "CONTINUAR"
            btn.setOnClickListener {
                (activity as? OnboardingActivity)?.nextStep()
            }
        }
    }

    class TutorialFragment : Fragment(R.layout.fragment_onboarding_tutorial) {
        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            view.findViewById<Button>(R.id.btnStartUsing).setOnClickListener {
                (activity as? OnboardingActivity)?.finishOnboarding()
            }
        }
    }
}
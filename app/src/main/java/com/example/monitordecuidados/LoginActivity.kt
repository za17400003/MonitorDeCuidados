package com.example.monitordecuidados

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.monitordecuidados.adapters.LoginPagerAdapter
import com.example.monitordecuidados.databinding.ActivityLoginProgrammaticBinding
import com.google.android.material.tabs.TabLayoutMediator

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginProgrammaticBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginProgrammaticBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val adapter = LoginPagerAdapter(this)
        binding.viewPager.adapter = adapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = if (position == 0) getString(R.string.login_tab) else getString(R.string.register_tab)
        }.attach()
    }
}

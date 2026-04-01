package com.example.monitordecuidados.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.monitordecuidados.fragments.LoginFragment
import com.example.monitordecuidados.fragments.RegisterFragment

class LoginPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {
    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> LoginFragment()
            else -> RegisterFragment()
        }
    }
}

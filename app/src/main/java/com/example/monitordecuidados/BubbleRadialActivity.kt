package com.example.monitordecuidados

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.appcompat.app.AppCompatActivity
import com.example.monitordecuidados.databinding.ActivityBubbleRadialBinding
import com.example.monitordecuidados.utils.EncryptedPreferencesHelper

class BubbleRadialActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBubbleRadialBinding
    private var terminalId: String? = null
    private var terminalName: String = "Terminal"
    private var terminalIp: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBubbleRadialBinding.inflate(layoutInflater)
        setContentView(binding.root)

        terminalId = intent.getStringExtra("terminal_id")
        terminalName = intent.getStringExtra("terminal_name") ?: "Terminal"
        terminalIp = intent.getStringExtra("terminal_ip")
            ?: EncryptedPreferencesHelper.getString(this, "paired_terminal_ip")
        val alertTitle = intent.getStringExtra("alert_title") ?: ""
        val alertCount = intent.getIntExtra("alert_count", 1)

        // Hub central
        binding.tvBubbleName.text = terminalName
        binding.tvBubbleAlert.text = alertTitle
        if (alertCount > 1) {
            binding.tvBubbleBadge.text = alertCount.toString()
            binding.tvBubbleBadge.visibility = View.VISIBLE
        } else {
            binding.tvBubbleBadge.visibility = View.GONE
        }

        // Click handlers
        binding.btnRadialMonitor.setOnClickListener { launchMonitor() }
        binding.btnRadialCall.setOnClickListener { launchCall() }
        binding.btnRadialControls.setOnClickListener { launchControls() }

        // Animación de expansión radial
        animateExpansion()
    }

    private fun animateExpansion() {
        val hub = binding.hubContainer
        val radialButtons = listOf(binding.btnRadialMonitor, binding.btnRadialCall, binding.btnRadialControls)
        val labels = listOf(binding.tvLabelMonitor, binding.tvLabelCall, binding.tvLabelControls)

        // Hub fade in + scale
        hub.alpha = 0f
        hub.scaleX = 0.8f
        hub.scaleY = 0.8f
        val hubAnim = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(hub, View.ALPHA, 0f, 1f),
                ObjectAnimator.ofFloat(hub, View.SCALE_X, 0.8f, 1f),
                ObjectAnimator.ofFloat(hub, View.SCALE_Y, 0.8f, 1f)
            )
            duration = 200
        }

        // Botones emergen del centro
        val btnAnims = radialButtons.map { btn ->
            btn.alpha = 0f
            btn.scaleX = 0.3f
            btn.scaleY = 0.3f
            AnimatorSet().apply {
                playTogether(
                    ObjectAnimator.ofFloat(btn, View.ALPHA, 0f, 1f),
                    ObjectAnimator.ofFloat(btn, View.SCALE_X, 0.3f, 1f),
                    ObjectAnimator.ofFloat(btn, View.SCALE_Y, 0.3f, 1f)
                )
                duration = 300
                interpolator = OvershootInterpolator(1.2f)
                startDelay = 100
            }
        }

        // Labels fade in
        val lblAnims = labels.map { lbl ->
            lbl.alpha = 0f
            ObjectAnimator.ofFloat(lbl, View.ALPHA, 0f, 1f).apply {
                duration = 100
                startDelay = 400
            }
        }

        AnimatorSet().apply {
            playTogether(listOf(hubAnim) + btnAnims + lblAnims)
            start()
        }
    }

    private fun animateCollapseAndRun(action: () -> Unit) {
        val allViews = listOf(
            binding.btnRadialMonitor, binding.btnRadialCall, binding.btnRadialControls,
            binding.tvLabelMonitor, binding.tvLabelCall, binding.tvLabelControls,
            binding.hubContainer
        )
        val anims = allViews.map { v ->
            AnimatorSet().apply {
                playTogether(
                    ObjectAnimator.ofFloat(v, View.ALPHA, v.alpha, 0f),
                    ObjectAnimator.ofFloat(v, View.SCALE_X, v.scaleX, 0.3f),
                    ObjectAnimator.ofFloat(v, View.SCALE_Y, v.scaleY, 0.3f)
                )
                duration = 300
                interpolator = AccelerateInterpolator()
            }
        }
        AnimatorSet().apply {
            playTogether(anims)
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    action()
                    finish()
                }
            })
            start()
        }
    }

    private fun launchMonitor() {
        animateCollapseAndRun {
            startActivity(Intent(this, VideoActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra("mode", "monitor")
                putExtra("remote_ip", terminalIp)
            })
        }
    }

    private fun launchCall() {
        animateCollapseAndRun {
            startActivity(Intent(this, TerminalDetailActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra("terminal_id", terminalId)
                putExtra("terminal_name", terminalName)
                putExtra("terminal_status", "connected")
                putExtra("auto_call", true)
            })
        }
    }

    private fun launchControls() {
        animateCollapseAndRun {
            startActivity(Intent(this, TerminalDetailActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra("terminal_id", terminalId)
                putExtra("terminal_name", terminalName)
                putExtra("terminal_status", "connected")
            })
        }
    }
}

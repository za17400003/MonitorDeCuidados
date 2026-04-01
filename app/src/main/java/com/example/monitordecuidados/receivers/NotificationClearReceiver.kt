package com.example.monitordecuidados.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.monitordecuidados.utils.NotificationHelper

class NotificationClearReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        context?.let {
            NotificationHelper.clearNotification(it)
        }
    }
}

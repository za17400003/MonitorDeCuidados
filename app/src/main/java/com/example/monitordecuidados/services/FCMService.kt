package com.example.monitordecuidados.services

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.monitordecuidados.MonitorMainActivity
import com.example.monitordecuidados.R
import com.example.monitordecuidados.utils.NotificationHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FCMService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "From: ${remoteMessage.from}")

        // Check if message contains data payload.
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")
            handleDataMessage(remoteMessage.data)
        }

        // Check if message contains notification payload.
        remoteMessage.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
            sendNotification(it.title ?: "Monitor de Cuidados", it.body ?: "")
        }
    }

    private fun handleDataMessage(data: Map<String, String>) {
        val title = data["title"] ?: "Alerta de Cuidados"
        val message = data["message"] ?: "Nueva notificación recibida"
        sendNotification(title, message)
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")
        sendRegistrationToServer(token)
    }

    private fun sendRegistrationToServer(token: String?) {
        token ?: return
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance()
            .collection("users").document(userId)
            .update("fcmToken", token)
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to update FCM token", e)
            }
    }

    private fun sendNotification(title: String, messageBody: String) {
        val intent = Intent(this, MonitorMainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE)

        val channelId = NotificationHelper.ALERT_CHANNEL_ID
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification_bell)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(0, notificationBuilder.build())
    }

    companion object {
        private const val TAG = "FCMService"
    }
}
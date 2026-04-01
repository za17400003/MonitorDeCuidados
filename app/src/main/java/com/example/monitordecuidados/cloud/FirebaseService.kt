package com.example.monitordecuidados.cloud

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Singleton that manages all Firestore operations.
 */
object FirebaseService {

    val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    fun isConnected(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            else -> false
        }
    }

    fun getUserId(): String? = auth.currentUser?.uid

    fun getConnectionType(context: Context): String {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return "none"
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return "none"
        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "internet"
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "internet"
            else -> "local"
        }
    }
}

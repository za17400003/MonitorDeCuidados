package com.example.monitordecuidados.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.*

object NetworkUtils {
    private var currentConnectionType: String = "local" // "local" | "internet"
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    fun getLocalIpAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address.isSiteLocalAddress) {
                        return address.hostAddress
                    }
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return null
    }

    fun isWifiConnected(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    fun getConnectionType(): String = currentConnectionType

    fun setConnectionType(type: String, context: Context) {
        currentConnectionType = type
        val prefs = context.getSharedPreferences("monitordecuidados_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("connection_type", type).apply()
    }

    fun initConnectionType(context: Context) {
        val prefs = context.getSharedPreferences("monitordecuidados_prefs", Context.MODE_PRIVATE)
        currentConnectionType = prefs.getString("connection_type", "local") ?: "local"
    }

    fun registerNetworkCallback(context: Context, onChanged: (Boolean) -> Unit) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: android.net.Network) {
                onChanged(true)
            }
            override fun onLost(network: android.net.Network) {
                onChanged(false)
            }
        }
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        cm.registerNetworkCallback(request, networkCallback!!)
    }

    fun unregisterNetworkCallback(context: Context) {
        networkCallback?.let {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            cm.unregisterNetworkCallback(it)
            networkCallback = null
        }
    }

    fun isInternetAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
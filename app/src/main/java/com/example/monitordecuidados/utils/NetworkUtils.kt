package com.example.monitordecuidados.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.*

object NetworkUtils {
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
}
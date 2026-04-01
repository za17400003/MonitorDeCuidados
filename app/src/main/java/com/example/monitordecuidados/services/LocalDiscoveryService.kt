package com.example.monitordecuidados.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.monitordecuidados.CampanaService

class LocalDiscoveryService : Service() {
    private val binder = LocalBinder()
    private var nsdManager: NsdManager? = null
    private val discoveredServices = mutableMapOf<String, DeviceInfo>()
    
    companion object {
        const val ACTION_DEVICES_UPDATED = "com.example.monitordecuidados.LOCAL_DEVICES_UPDATED"
        const val EXTRA_DEVICES = "devices"
    }

    inner class LocalBinder : Binder() {
        fun getService(): LocalDiscoveryService = this@LocalDiscoveryService
    }

    private val discoveryListener = object : NsdManager.DiscoveryListener {
        override fun onServiceFound(service: NsdServiceInfo) {
            Log.d("Discovery", "Service found: ${service.serviceName}")
            if (service.serviceType == CampanaService.SERVICE_TYPE) {
                nsdManager?.resolveService(service, object : NsdManager.ResolveListener {
                    override fun onServiceResolved(resolvedService: NsdServiceInfo) {
                        val deviceInfo = DeviceInfo(
                            name = resolvedService.serviceName,
                            ip = resolvedService.host.hostAddress ?: "",
                            port = resolvedService.port
                        )
                        discoveredServices[deviceInfo.name] = deviceInfo
                        broadcastDiscovery()
                    }

                    override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                        Log.e("Discovery", "Resolve failed: $errorCode")
                    }
                })
            }
        }

        override fun onDiscoveryStarted(regType: String) {
            Log.d("Discovery", "Discovery started: $regType")
        }

        override fun onDiscoveryStopped(serviceType: String) {
            Log.d("Discovery", "Discovery stopped: $serviceType")
        }

        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.e("Discovery", "Start discovery failed: $errorCode")
            nsdManager?.stopServiceDiscovery(this)
        }

        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.e("Discovery", "Stop discovery failed: $errorCode")
            nsdManager?.stopServiceDiscovery(this)
        }

        override fun onServiceLost(service: NsdServiceInfo) {
            Log.d("Discovery", "Service lost: ${service.serviceName}")
            discoveredServices.remove(service.serviceName)
            broadcastDiscovery()
        }
    }

    private fun broadcastDiscovery() {
        val intent = Intent(ACTION_DEVICES_UPDATED).apply {
            putParcelableArrayListExtra(EXTRA_DEVICES, ArrayList(discoveredServices.values))
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (nsdManager == null) {
            nsdManager = getSystemService(Context.NSD_SERVICE) as NsdManager
            startDiscovery()
        }
        return START_STICKY
    }

    private fun startDiscovery() {
        try {
            nsdManager?.discoverServices(CampanaService.SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        } catch (e: Exception) {
            Log.e("Discovery", "Failed to start discovery: ${e.message}")
        }
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onDestroy() {
        try {
            nsdManager?.stopServiceDiscovery(discoveryListener)
        } catch (e: Exception) {
            Log.e("Discovery", "Error stopping discovery: ${e.message}")
        }
        super.onDestroy()
    }
}

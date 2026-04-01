package com.example.monitordecuidados.communication

import android.util.Log
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

class RateLimiter {
    private val requestCounts = ConcurrentHashMap<String, CopyOnWriteArrayList<Long>>()
    private val MAX_REQUESTS = 100
    private val TIME_WINDOW_MS = 60000 // 1 minute

    fun isAllowed(ip: String): Boolean {
        val now = System.currentTimeMillis()
        val requests = requestCounts.getOrPut(ip) { CopyOnWriteArrayList<Long>() }

        // Remove old requests
        synchronized(requests) {
            val iterator = requests.iterator()
            while (iterator.hasNext()) {
                if (now - iterator.next() > TIME_WINDOW_MS) {
                    requests.remove(iterator.next())
                }
            }
        }
        
        // Simpler way with CopyOnWriteArrayList and synchronized for the size check
        synchronized(requests) {
            requests.removeAll { now - it > TIME_WINDOW_MS }
            
            if (requests.size >= MAX_REQUESTS) {
                Log.w("RateLimiter", "Rate limit exceeded for $ip")
                return false
            }

            requests.add(now)
            return true
        }
    }
}

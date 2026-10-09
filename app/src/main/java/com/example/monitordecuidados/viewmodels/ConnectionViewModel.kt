package com.example.monitordecuidados.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.monitordecuidados.cloud.FirebaseService
import com.example.monitordecuidados.models.ConnectionState
import com.example.monitordecuidados.models.PairingInfo
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ListenerRegistration

class ConnectionViewModel : ViewModel() {
    private val _connectionState = MutableLiveData<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: LiveData<ConnectionState> = _connectionState

    private val _pairingsList = MutableLiveData<List<PairingInfo>>(emptyList())
    val pairingsList: LiveData<List<PairingInfo>> = _pairingsList

    private val db by lazy { FirebaseService.db }
    private val auth by lazy { FirebaseService.auth }
    private var pairingsListenerRegistration: ListenerRegistration? = null

    fun getPairingsList() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.w("ConnectionVM", "getPairingsList: auth.currentUser is null, skipping query")
            return
        }
        
        // Remove previous listener to avoid duplicates
        pairingsListenerRegistration?.remove()
        
        pairingsListenerRegistration = db.collection("pairings")
            .whereEqualTo("monitorId", userId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("ConnectionVM", "Error fetching pairings: ${e.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(PairingInfo::class.java)?.copy(id = doc.id)
                    }
                    _pairingsList.value = list
                }
            }
    }

    fun addPairing(terminalId: String, terminalName: String = "Terminal", connectionType: String = "internet", onSuccess: (() -> Unit)? = null) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.w("ConnectionVM", "addPairing: auth.currentUser is null, skipping Firestore write")
            onSuccess?.invoke()
            return
        }
        val pairing = PairingInfo(
            monitorId = userId,
            terminalId = terminalId,
            name = terminalName,
            connectionType = connectionType,
            lastSeen = Timestamp.now()
        )
        db.collection("pairings").add(pairing)
            .addOnSuccessListener {
                Log.d("ConnectionVM", "Pairing added successfully: ${it.id}")
                getPairingsList()
                onSuccess?.invoke()
            }
            .addOnFailureListener { e ->
                Log.e("ConnectionVM", "Error adding pairing: ${e.message}")
                onSuccess?.invoke()
            }
    }

    fun removePairing(pairingId: String) {
        db.collection("pairings").document(pairingId).delete()
            .addOnSuccessListener {
                getPairingsList()
            }
    }

    fun updateConnectionStatus(status: String) {
        when (status) {
            "connected" -> _connectionState.value = ConnectionState.Connected
            "disconnected" -> _connectionState.value = ConnectionState.Disconnected
            "reconnecting" -> _connectionState.value = ConnectionState.Reconnecting
            else -> _connectionState.value = ConnectionState.Error("Unknown status: $status")
        }
    }

    fun getConnectionType(): String {
        // This would ideally come from a connectivity manager or similar
        return "internet"
    }

    override fun onCleared() {
        super.onCleared()
        pairingsListenerRegistration?.remove()
    }
}

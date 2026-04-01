# WORKITEMS v15.0 — 5 BUGS DE TESTING EN DISPOSITIVO (Abril 1, 2026)

> **ACTUALIZADO**: Abril 1, 2026
> **ESTADO**: T14-T56 ✅ (detalles consolidados en TESTING_CHECKLISTS.md). Nuevas tareas T57-T61.
> **PRIORIDAD**: T57 > T60 > T58 > T59 > T61
> **NOTA**: Sonido de campana ya reemplazado (bell_chime.wav → bell_chime.mp3). Copilot lo ejecutó directo (Regla #12).

---

## HISTÓRICO COMPLETADO (código + build ✅)

| Tarea | Dispositivo |
|-------|-------------|
| T14-T27 | ✅ |
| T28: Lockscreen sonido + overlay + click | ✅ |
| T29: Monitor terminales + RECORD_AUDIO + logging | ❌ pendiente |
| T30: Alarmas repetición + reschedule boot | ❌ pendiente |
| T31: Quitar sonido detector de voz | ✅ |
| T32: Fix vinculación QR race condition | ✅ |
| T32-FIX: Regresión QR scanner | ✅ |
| T33: Servicios apagados sin permisos | ✅ |
| T34: Verificar permiso CAMERA | ✅ |
| T35: Dashboard SharedPrefs fallback | ❌ pendiente |
| T36: TerminalDetailActivity | ✅ |
| T37: Rediseño card terminal | ✅ |
| T38: Fix notificaciones Monitor (user_role) | ❌ pendiente |
| T39: GET /status endpoint + estado real | ❌ pendiente |
| T39-FIX: Switches no disparan HTTP innecesario | ❌ pendiente |
| T40: VideoActivity IP fix (intent > EncryptedPrefs) | ❌ pendiente |
| T41: Refresh servicios por Handler directo | ❌ pendiente |
| T42: TerminalQRFragment sync switches remotos | ❌ pendiente |
| T43: Polling periódico TerminalDetailActivity | ✅ |
| T44: CapabilitiesAssessment routing (3 entry points) | ✅ |
| T45: Walkie-talkie in-place (btnCall toggle) | ✅ |
| T46: CampanaService startAudioCall/stopAudioCall | ✅ |
| T46-FIX: Nullable String? en startAudioCall | ✅ |
| T47: Migración OPUS (CallManager 16kHz MediaCodec) | ✅ |
| T48: call_history Firestore (start/end) | ✅ |
| T49: CalibrationDialog en SettingsFragment | ✅ |
| T50: VoiceAlertAdapter 6 bindings completos | ✅ |
| T51: AlertAdapter consolidado (NotificationAlertAdapter) | ✅ |
| T52: remote_ip fix en serverListener callbacks | ✅ |
| T53: Battery monitoring (≤15% alerta al Monitor) | ✅ |
| T54: Exponential backoff (5s→10s→30s→60s) | ✅ |
| T55: Internet fallback + NetworkUtils expanded | ✅ |
| T56: Role-switch cleanup (bell + notificación persist) | ✅ |

---

## TAREAS PENDIENTES — Bugs de testing en dispositivo real

---

## T57: Monitor NO recibe notificaciones (campana, voz, shake)

**Problema**: Después de aparear Terminal y Monitor, al tocar campana en Terminal, al detectar voz, o al agitar — el Monitor NO muestra notificación. La cadena HTTP Terminal→Monitor está rota.

**Causa raíz** (investigación Copilot):
1. Terminal lee `paired_monitor_ip` de SharedPreferences `monitordecuidados_prefs` (línea 136 de CampanaService). Este valor se guarda cuando Monitor envía POST `/confirm_pairing` con su IP. **SI el pairing HTTP no llegó al Terminal**, `paired_monitor_ip` será null y TODAS las alertas se pierden silenciosamente (solo se loggea warning).
2. Los 3 métodos de envío tienen la MISMA lógica null-check:
   - `handleAudioCallCommand()` (bell, línea 413): `val monitorIp = prefs.getString("paired_monitor_ip", null)`
   - `sendShakeAlert()` (línea 344): `val monitorIp = prefs.getString("paired_monitor_ip", null)`
   - `sendVoiceAlert()` (línea 372): `val monitorIp = prefs.getString("paired_monitor_ip", null)`
3. Si `monitorIp == null` → se loggea `"paired_monitor_ip is null, alert NOT sent"` y la función retorna sin enviar nada.

**Diagnóstico requerido**: Verificar mediante logs (FileLogger o Logcat) si `paired_monitor_ip` se guarda correctamente tras el pairing. Si el valor ES null tras aparear, el problema está en QRScannerActivity.confirmPairingToTerminal() — puede que el POST `/confirm_pairing` no llegue, o que el Terminal no parsee correctamente el `monitorIp` del JSON body.

### SNAPSHOT: Cadena completa de pairing → alerta

**Pairing** (Monitor → Terminal):
```
1. QRScannerActivity detecta QR con IP del Terminal
2. QRScannerActivity.confirmPairingToTerminal() hace POST a http://terminalIp:8080/confirm_pairing
   Body: { "monitorName": "...", "monitorIp": "..." }
3. CampanaHttpServer recibe → listener.onPairingConfirmed(effectiveIp, monitorName)
4. CampanaService.onPairingConfirmed() guarda en SharedPrefs: "paired_monitor_ip" = sourceIp
```

**Alerta** (Terminal → Monitor):
```
1. Terminal: triggerBell/sendShakeAlert/sendVoiceAlert
2. Lee paired_monitor_ip de SharedPrefs → SI null → ABORTA (bug aquí si pairing falló)
3. HTTP POST a monitorIp:8080/trigger_bell (o /alert/shake, /trigger_voice)
4. Monitor: CampanaHttpServer recibe → listener.onBellTriggered(sourceIp)
5. CampanaService.onBellTriggered() → NotificationHelper.notifyAlert()
6. NotificationHelper verifica userRole == ROLE_MONITOR → SI no es monitor, return
7. Crea heads-up notification
```

### CAMBIOS REQUERIDOS:

**1. Añadir log de diagnóstico en CampanaService.onPairingConfirmed()** (línea ~134-137):
```kotlin
override fun onPairingConfirmed(sourceIp: String, monitorName: String) {
    FileLogger.logInfo(TAG, "onPairingConfirmed: sourceIp=$sourceIp, monitorName=$monitorName")
    getSharedPreferences("monitordecuidados_prefs", MODE_PRIVATE).edit()
        .putString("paired_monitor_ip", sourceIp)
        .putString("paired_monitor_name", monitorName)
        .apply()
    // Verificar que se guardó
    val saved = getSharedPreferences("monitordecuidados_prefs", MODE_PRIVATE)
        .getString("paired_monitor_ip", null)
    FileLogger.logInfo(TAG, "paired_monitor_ip saved as: $saved")
}
```

**2. Añadir log de diagnóstico en los 3 métodos de envío** para que en Logcat veamos si `monitorIp` es null o no, y si el POST tiene éxito o error.

**3. EN QRScannerActivity.confirmPairingToTerminal()**: Verificar que el `monitorIp` enviado en el JSON body es la IP local real del Monitor (no "0.0.0.0" ni empty). Usar `NetworkUtils.getLocalIpAddress()` si no se está usando ya.

### ARCHIVOS A MODIFICAR:
- `CampanaService.kt` (onPairingConfirmed + logs en sendShakeAlert/sendVoiceAlert/triggerBell)
- `QRScannerActivity.kt` (verificar que monitorIp en JSON body es la IP real)

### CONSERVAR:
- Lógica de envío HTTP (solo añadir logs, no cambiar flujo)
- NotificationHelper.notifyAlert() — NO TOCAR
- CampanaHttpServer.kt — NO TOCAR

### NO TOCAR:
- NotificationHelper.kt
- CampanaHttpServer.kt
- BellActivity.kt

### VERIFICACIÓN:
- [ ] Build pasa
- [ ] Aparear dispositivos → Logcat muestra "paired_monitor_ip saved as: <IP real>"
- [ ] Tocar campana en Terminal → Monitor recibe notificación 🔔
- [ ] Agitar Terminal → Monitor recibe notificación ⚠️
- [ ] Voz detectada → Monitor recibe notificación 🎙️

---

## T58: Llamada walkie-talkie sin audio bidireccional

**Problema**: En TerminalDetailActivity, al tocar "Llamar" el botón cambia a "Colgar" (UI correcta) pero NO hay comunicación de audio entre el Monitor y el Terminal. Monitor abre CallManager(terminalIp, 5060) pero el Terminal NUNCA abre su propio CallManager de vuelta.

**Causa raíz** (investigación Copilot):
1. Monitor crea `CallManager(terminalIp, 5060)` → abre DatagramSocket en puerto 5060, inicia sendThread (graba→OPUS→UDP) y receiveThread (UDP→OPUS→speaker).
2. Monitor envía paquetes UDP a `terminalIp:5060` → pero Terminal NO tiene socket abierto en 5060. Paquetes se pierden.
3. Terminal TIENE código para crear CallManager en `CampanaService.startAudioCall()` pero eso solo se invoca cuando Terminal recibe Intent con `call_action=start`. Monitor NUNCA envía esa señal.
4. **Flujo faltante**: Monitor necesita enviar HTTP POST a Terminal `/request_call` ANTES de crear su CallManager. Terminal recibe `/request_call` → `onCallRequested()` actualmente lanza VideoActivity (incorrecto para walkie-talkie) — debe crear CallManager apuntando a Monitor IP.

### CAMBIOS REQUERIDOS:

**1. En TerminalDetailActivity.kt** — btnCall click handler (donde crea CallManager):
ANTES de `callManager = CallManager(ip, 5060)`, enviar HTTP al Terminal para que abra su lado:
```kotlin
binding.btnCall.setOnClickListener {
    val ip = remoteIp ?: return@setOnClickListener
    if (!isCallActive) {
        // Señalar al Terminal que inicie su CallManager
        Thread {
            try {
                val url = java.net.URL("http://$ip:8080/request_call")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "POST"
                conn.connectTimeout = 3000
                conn.doOutput = true
                conn.outputStream.write("source=monitor".toByteArray())
                conn.responseCode
                conn.disconnect()
            } catch (e: Exception) {
                Log.e(TAG, "Error notifying terminal of call", e)
            }
        }.start()
        // Iniciar lado Monitor
        callManager = CallManager(ip, 5060)
        callManager?.startCall()
        isCallActive = true
        // ... UI update ya existente ...
```

Al colgar, enviar HTTP al Terminal para que cierre su CallManager:
```kotlin
    } else {
        // Señalar al Terminal que termine su CallManager
        Thread {
            try {
                val url = java.net.URL("http://${remoteIp}:8080/command")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "POST"
                conn.connectTimeout = 3000
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true
                conn.outputStream.write("{\"command\":\"STOP_AUDIO_CALL\"}".toByteArray())
                conn.responseCode
                conn.disconnect()
            } catch (e: Exception) {
                Log.e(TAG, "Error notifying terminal to stop call", e)
            }
        }.start()
        callManager?.stopCall()
        // ... resto ya existente ...
```

**2. En CampanaService.kt** — Cambiar `onCallRequested()` (línea ~100):
Actualmente lanza VideoActivity. Para walkie-talkie, debe crear CallManager apuntando al Monitor:
```kotlin
override fun onCallRequested(sourceIp: String) {
    FileLogger.logInfo(TAG, "onCallRequested from $sourceIp — starting walkie-talkie")
    callManager?.stopCall()
    callManager = CallManager(sourceIp, 5060)
    callManager?.startCall()
    isAudioCallActive = true
    updateNotification()
}
```

**3. En CampanaService.kt** — Manejar comando "STOP_AUDIO_CALL" en `onCommandReceived()`:
Añadir un case en el `when(command)` block (línea ~120):
```kotlin
"STOP_AUDIO_CALL" -> {
    callManager?.stopCall()
    callManager = null
    isAudioCallActive = false
    updateNotification()
}
```

### ARCHIVOS A MODIFICAR:
- `TerminalDetailActivity.kt` (btnCall handler: añadir HTTP POST antes de CallManager + HTTP al colgar)
- `CampanaService.kt` (onCallRequested: crear CallManager en vez de VideoActivity + manejar STOP_AUDIO_CALL)

### CONSERVAR:
- CallManager.kt — NO TOCAR
- TerminalDetailActivity: btnMonitor, switches, polling — NO TOCAR
- CampanaService: onMonitorRequested, onBellTriggered, etc. — NO TOCAR

### NO TOCAR:
- CallManager.kt
- CampanaHttpServer.kt (ya tiene endpoint `/request_call`)
- VideoActivity.kt

### VERIFICACIÓN:
- [ ] Build pasa
- [ ] Monitor toca "Llamar" → Terminal Logcat muestra "onCallRequested from <monitorIP>"
- [ ] Audio del Monitor se escucha en Terminal
- [ ] Audio del Terminal se escucha en Monitor
- [ ] Monitor toca "Colgar" → ambos CallManagers se cierran

---

## T59: Botón Monitorear no muestra video/audio del Terminal

**Problema**: Al tocar "Monitorear" en TerminalDetailActivity, se lanza VideoActivity en mode="monitor". Monitor abre UDP socket en puerto 9001 esperando frames de video. Pero NUNCA le dice al Terminal que empiece a hacer streaming. Resultado: pantalla negra sin video ni audio.

**Causa raíz** (investigación Copilot):
1. Monitor abre VideoActivity(mode="monitor") → `isVideoCallActive=false`
2. En `onCreate()`, para rol "monitor": ejecuta `startReceivingVideo()` (abre UDP 9001) ✅
3. Pero el código que envía comandos al Terminal (`sendCommandToTerminal("ENABLE_VIDEO_CALL")`) está dentro de `if (isVideoCallActive)` — que es `false` para mode="monitor". Monitor NUNCA le dice al Terminal que empiece.
4. Aunque Monitor enviara HTTP al Terminal, `CampanaService.onMonitorRequested()` (línea ~108) lanza VideoActivity en Terminal PERO sin `auto_accept=true` → Terminal no llama `videoManager?.startStreaming()` automáticamente.

### CAMBIOS REQUERIDOS:

**1. En VideoActivity.kt** — Para mode="monitor", enviar HTTP al Terminal para que inicie streaming:
En el bloque `if (currentRol == "monitor")` de `onCreate()` (línea ~96), DESPUÉS de `startReceivingVideo()`, añadir:
```kotlin
if (currentRol == "monitor") {
    if (!isAudioOnly) startReceivingVideo()
    
    if (isAudioOnly) {
        startAudioMode()
    } else if (isVideoCallActive) {
        callViewModel.initiateCall(remoteIp ?: "unknown")
        sendCommandToTerminal("ENABLE_AUDIO")
        sendCommandToTerminal("ENABLE_VIDEO_CALL")
    } else {
        // mode="monitor" (silent monitoring) — pedir al Terminal que inicie streaming
        sendMonitorRequest()
    }
}
```

**2. En VideoActivity.kt** — Añadir método `sendMonitorRequest()`:
```kotlin
private fun sendMonitorRequest() {
    if (remoteIp == null) return
    Thread {
        try {
            val url = java.net.URL("http://$remoteIp:8080/request_monitor")
            val conn = url.openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 3000
            conn.doOutput = true
            conn.outputStream.write("source=monitor".toByteArray())
            conn.responseCode
            conn.disconnect()
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting monitor streaming", e)
        }
    }.start()
}
```

**3. En CampanaService.kt** — Cambiar `onMonitorRequested()` (línea ~108):
Añadir `auto_accept=true` al Intent para que Terminal auto-inicie cámara:
```kotlin
override fun onMonitorRequested(sourceIp: String) {
    val intent = Intent(this@CampanaService, VideoActivity::class.java).apply {
        putExtra("mode", "monitor")
        putExtra("remote_ip", sourceIp)
        putExtra("auto_accept", true)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    startActivity(intent)
}
```

**4. En VideoActivity.kt** — Bloque Terminal con `auto_accept` (línea ~108):
Actualmente el bloque `else` (Terminal side) solo entra a `auto_accept` si `isAudioOnly` o no. Para monitoring silencioso, debe iniciar streaming sin audio:
```kotlin
} else {
    // Terminal side
    if (intent.getBooleanExtra("auto_accept", false)) {
        if (isAudioOnly) {
            startAudioMode()
        } else {
            videoManager?.startStreaming()
            if (isVideoCallActive) {
                callManager?.startCall()
            }
        }
    }
}
```
Este bloque YA funciona correctamente si `auto_accept=true` — `videoManager?.startStreaming()` se ejecutará para mode="monitor" porque `isAudioOnly=false`. Solo necesita el cambio en paso 3 para que `auto_accept` se pase.

### ARCHIVOS A MODIFICAR:
- `VideoActivity.kt` (añadir `sendMonitorRequest()` + llamarlo en mode="monitor")
- `CampanaService.kt` (onMonitorRequested: añadir `auto_accept=true`)

### CONSERVAR:
- VideoActivity: startReceivingVideo(), setupUI(), toggleBetweenSilentAndVideoCall() — NO TOCAR
- CampanaService: onCallRequested, onBellTriggered, etc. — NO TOCAR
- VideoManager.kt — NO TOCAR (startStreaming ya funciona)

### NO TOCAR:
- VideoManager.kt
- CampanaHttpServer.kt (ya tiene /request_monitor endpoint)
- TerminalDetailActivity.kt

### VERIFICACIÓN:
- [ ] Build pasa
- [ ] Monitor toca "Monitorear" → Terminal Logcat muestra "onMonitorRequested"
- [ ] Terminal inicia cámara automáticamente → frames JPEG viajan por UDP 9001
- [ ] Monitor ve video del Terminal en la pantalla
- [ ] Cerrar monitoreo → Terminal deja de hacer streaming

---

## T60: Botón toggle videollamada/silencioso CIERRA la interfaz

**Problema**: En VideoActivity, el botón `btnToggleMode` debería alternar entre "video call" y "monitoreo silencioso" SIN cerrar la Activity. Pero al toglear de videocall→silencioso, `callViewModel.endCall("monitor_toggled_off")` emite `CallState.Ended`, y el observer en `setupObservers()` ejecuta `finish()` — cerrando toda la interfaz.

**Causa raíz** (investigación Copilot):
1. `toggleBetweenSilentAndVideoCall()` (línea ~210): cuando `isVideoCallActive` pasa a `false`:
   ```kotlin
   callManager?.stopCall()
   callViewModel.endCall("monitor_toggled_off")
   ```
2. `setupObservers()` (línea ~127):
   ```kotlin
   callViewModel.callState.observe(this) { state ->
       when (state) {
           is CallState.Ended -> { finish() }  // ← ESTO CIERRA TODO
   ```
3. `endCall()` emite `CallState.Ended` → observer ejecuta `finish()` → Activity se cierra.

### CAMBIOS REQUERIDOS:

**1. En VideoActivity.kt** — Añadir flag para distinguir toggle vs hangup real:
```kotlin
private var isTogglingMode = false
```

**2. En toggleBetweenSilentAndVideoCall()** — Setear flag antes de endCall:
```kotlin
private fun toggleBetweenSilentAndVideoCall() {
    isAudioOnly = false
    isVideoCallActive = !isVideoCallActive
    if (isVideoCallActive) {
        // ... código existente sin cambios ...
    } else {
        binding.cardLocalVideo.visibility = View.GONE
        sendCommandToTerminal("DISABLE_AUDIO")
        sendCommandToTerminal("DISABLE_VIDEO_CALL")
        callManager?.stopCall()
        isTogglingMode = true
        callViewModel.endCall("monitor_toggled_off")
        isTogglingMode = false
    }
    updateStatusText()
    updateToggleButtonStyle()
}
```

**3. En setupObservers()** — Respetar el flag:
```kotlin
callViewModel.callState.observe(this) { state ->
    when (state) {
        is CallState.Ended -> {
            if (!isTogglingMode) {
                finish()
            }
        }
        is CallState.Connected -> {
            updateStatusText()
        }
        else -> {}
    }
}
```

**ALTERNATIVA MÁS LIMPIA**: En vez de `callViewModel.endCall()` en el toggle, simplemente detener el CallManager sin notificar al ViewModel. El ViewModel solo debe enterarse cuando el usuario realmente cuelga (hangUpAll). Cambio:
```kotlin
// En toggleBetweenSilentAndVideoCall(), bloque else:
    } else {
        binding.cardLocalVideo.visibility = View.GONE
        sendCommandToTerminal("DISABLE_AUDIO")
        sendCommandToTerminal("DISABLE_VIDEO_CALL")
        callManager?.stopCall()
        // NO llamar callViewModel.endCall() — solo toggle, no hangup
    }
```

**Usa la alternativa más limpia** — simplemente eliminar la línea `callViewModel.endCall("monitor_toggled_off")` del bloque toggle-off. El endCall solo debe llamarse desde `hangUpAll()`.

### ARCHIVOS A MODIFICAR:
- `VideoActivity.kt` (toggleBetweenSilentAndVideoCall: eliminar callViewModel.endCall del bloque else)

### CONSERVAR:
- hangUpAll() — NO TOCAR (ese SÍ debe terminar la call)
- setupObservers() CallState.Ended → finish() — NO TOCAR (correcto para hangup real)
- toggleBetweenSilentAndVideoCall() bloque if (isVideoCallActive=true) — NO TOCAR

### NO TOCAR:
- CallViewModel.kt
- CampanaService.kt
- TerminalDetailActivity.kt

### VERIFICACIÓN:
- [ ] Build pasa
- [ ] Monitor en Video Call → tap toggle → cambia a monitoreo silencioso SIN cerrar Activity
- [ ] Monitor en monitoreo silencioso → tap toggle → cambia a Video Call
- [ ] Monitor tap "Colgar" → SÍ cierra Activity (hangUpAll funciona)
- [ ] Toggle múltiples veces no crashea

---

## T61: Batería del Terminal no se actualiza en Dashboard del Monitor

**Problema**: Después de aparear, el porcentaje de batería del Terminal que se muestra en la interfaz del Monitor se actualiza UNA sola vez y no se va actualizando conforme sube o baja la batería del Terminal. Debe actualizarse continuamente.

**Causa raíz** (investigación Copilot):
1. TerminalDetailActivity tiene polling correcto con Handler/Runnable cada 5s — ahí SÍ se actualiza ✅
2. DashboardFragment llama `queryTerminalStatuses()` UNA sola vez (línea ~113) cuando `pairingsList` cambia en el observer. El método hace GET /status, parsea batteryLevel, actualiza el adapter — pero la función se ejecuta una sola vez y el Thread termina sin reschedule.
3. **FALTA**: DashboardFragment no tiene Handler/Runnable para polling periódico como sí lo tiene TerminalDetailActivity.

### SNAPSHOT (DashboardFragment.kt — queryTerminalStatuses, líneas ~120-159):
```kotlin
private fun queryTerminalStatuses(terminals: List<TerminalInfo>) {
    for (terminal in terminals) {
        val terminalIp = EncryptedPreferencesHelper.getString(requireContext(), "paired_terminal_ip")
        if (terminalIp == null) continue
        Thread {
            try {
                val url = java.net.URL("http://$terminalIp:8080/status")
                // ... fetch battery once, update adapter ...
            } catch (e: Exception) { }
        }.start()  // ← Thread termina, no hay reschedule
    }
}
```

### CAMBIOS REQUERIDOS:

**1. En DashboardFragment.kt** — Añadir Handler + Runnable (como TerminalDetailActivity):
```kotlin
private val pollHandler = android.os.Handler(android.os.Looper.getMainLooper())
private var currentTerminals: List<TerminalInfo> = emptyList()
private val pollRunnable = object : Runnable {
    override fun run() {
        if (currentTerminals.isNotEmpty()) {
            queryTerminalStatuses(currentTerminals)
        }
        pollHandler.postDelayed(this, 10000) // Cada 10 segundos (Dashboard no necesita 5s)
    }
}
```

**2. En DashboardFragment.kt** — Guardar referencia a terminals en el observer:
En el observer de `pairingsList` (donde actualmente llama `queryTerminalStatuses(terminals)`), guardar la lista:
```kotlin
currentTerminals = terminals
```

**3. En DashboardFragment.kt** — Lifecycle binding:
```kotlin
override fun onResume() {
    super.onResume()
    connectionViewModel.getPairingsList()
    pollHandler.post(pollRunnable)
}

override fun onPause() {
    super.onPause()
    pollHandler.removeCallbacks(pollRunnable)
}
```

**4. En DashboardFragment.kt** — Limpiar en onDestroyView():
```kotlin
override fun onDestroyView() {
    pollHandler.removeCallbacks(pollRunnable)
    super.onDestroyView()
}
```

### ARCHIVOS A MODIFICAR:
- `DashboardFragment.kt` (añadir Handler/Runnable + lifecycle binding + guardar terminals)

### CONSERVAR:
- queryTerminalStatuses() método existente — NO cambiar su lógica interna, solo asegurar que se llama periódicamente
- terminalAdapter, binding, observer de pairingsList — mantener lógica existente
- setupListeners() — NO TOCAR

### NO TOCAR:
- TerminalDetailActivity.kt (su polling ya funciona)
- CampanaService.kt (onStatusRequested ya reporta batería correctamente)
- TerminalStatusAdapter.kt (display ya funciona)

### VERIFICACIÓN:
- [ ] Build pasa
- [ ] Dashboard muestra batería del Terminal
- [ ] Poner Terminal a cargar → porcentaje sube en Dashboard sin reabrir la app
- [ ] Desconectar cargador → porcentaje baja progresivamente
- [ ] Salir de Dashboard y volver → polling se reinicia
| T54: Exponential backoff (5s→10s→30s→60s) | ✅ |
| T55: Internet fallback + NetworkUtils expanded | ✅ |
| T56: Role-switch cleanup (bell + notificación persist) | ✅ |

---
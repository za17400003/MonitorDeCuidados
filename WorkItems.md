# WORKITEMS v13.0 — TAREAS PENDIENTES (Abril 2, 2026)

> **ACTUALIZADO**: Abril 2, 2026
> **ESTADO**: T14-T43 código ✅ build ✅. T44-T51 nuevas tareas de auditoría.
> **ORDEN**: T44 → T51 → T50 → T49 → T45 → T46 → T47 → T48

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

---

## TAREAS PENDIENTES

---

## T44: Conectar CapabilitiesAssessmentActivity a los 3 puntos de entrada Terminal

**Problema**: CapabilitiesAssessmentActivity (157 líneas) está completamente implementada pero NUNCA se lanza. Los 3 puntos de entrada al modo Terminal van directo a TerminalMainActivity sin pasar por la evaluación geriátrica.

**Causa raíz**: OnboardingActivity.finishOnboarding(), RoleSelectorActivity.cardPatient, y SplashActivity "terminal" case — todos hacen Intent directo a TerminalMainActivity.

**Lógica requerida**: Antes de ir a TerminalMainActivity, verificar SharedPreferences por `capabilities_status`. Si NO es "completed" ni "omitted", redirigir a CapabilitiesAssessmentActivity. Si ya fue completada u omitida, ir directo a TerminalMainActivity como hasta ahora.

### SNAPSHOT (OnboardingActivity.kt — 152 líneas):
- Archivo: `app/src/main/java/com/example/monitordecuidados/OnboardingActivity.kt`
- Líneas 59-68: `finishOnboarding()`:
```kotlin
fun finishOnboarding() {
    EncryptedPreferencesHelper.saveBoolean(this, "onboarding_completed", true)
    CampanaService.startService(this)
    val targetActivity = if (userRole == "monitor") {
        MonitorMainActivity::class.java
    } else {
        TerminalMainActivity::class.java
    }
    startActivity(Intent(this, targetActivity))
    finish()
}
```

### CAMBIO en OnboardingActivity.kt:
Reemplazar el bloque `else` que va a TerminalMainActivity. Cuando userRole == "terminal", verificar capabilities_status:

```kotlin
fun finishOnboarding() {
    EncryptedPreferencesHelper.saveBoolean(this, "onboarding_completed", true)
    CampanaService.startService(this)
    val targetActivity = if (userRole == "monitor") {
        MonitorMainActivity::class.java
    } else {
        val capStatus = EncryptedPreferencesHelper.getString(this, "capabilities_status", "")
        if (capStatus == "completed" || capStatus == "omitted") {
            TerminalMainActivity::class.java
        } else {
            CapabilitiesAssessmentActivity::class.java
        }
    }
    startActivity(Intent(this, targetActivity))
    finish()
}
```

### SNAPSHOT (RoleSelectorActivity.kt — 43 líneas):
- Archivo: `app/src/main/java/com/example/monitordecuidados/RoleSelectorActivity.kt`
- Líneas 21-27: `cardPatient` click handler:
```kotlin
binding.cardPatient.setOnClickListener {
    saveRole("terminal")
    reinitializeConnections()
    val intent = Intent(this, TerminalMainActivity::class.java)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    startActivity(intent)
    finish()
}
```

### CAMBIO en RoleSelectorActivity.kt:
Reemplazar la línea del Intent para verificar capabilities_status:

```kotlin
binding.cardPatient.setOnClickListener {
    saveRole("terminal")
    reinitializeConnections()
    val capStatus = EncryptedPreferencesHelper.getString(this, "capabilities_status", "")
    val targetClass = if (capStatus == "completed" || capStatus == "omitted") {
        TerminalMainActivity::class.java
    } else {
        CapabilitiesAssessmentActivity::class.java
    }
    val intent = Intent(this, targetClass)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    startActivity(intent)
    finish()
}
```

Añadir import arriba si no existe:
```kotlin
import com.example.monitordecuidados.CapabilitiesAssessmentActivity
```

### SNAPSHOT (SplashActivity.kt — 63 líneas):
- Archivo: `app/src/main/java/com/example/monitordecuidados/SplashActivity.kt`
- Líneas 40-42: Routing "terminal":
```kotlin
val intent = when (userRole) {
    "monitor" -> Intent(this, MonitorMainActivity::class.java)
    "terminal" -> Intent(this, TerminalMainActivity::class.java)
    else -> Intent(this, OnboardingActivity::class.java)
}
```

### CAMBIO en SplashActivity.kt:
Reemplazar la rama "terminal" para verificar capabilities_status:

```kotlin
val intent = when (userRole) {
    "monitor" -> Intent(this, MonitorMainActivity::class.java)
    "terminal" -> {
        val capStatus = EncryptedPreferencesHelper.getString(this, "capabilities_status", "")
        if (capStatus == "completed" || capStatus == "omitted") {
            Intent(this, TerminalMainActivity::class.java)
        } else {
            Intent(this, CapabilitiesAssessmentActivity::class.java)
        }
    }
    else -> Intent(this, OnboardingActivity::class.java)
}
```

Añadir import arriba si no existe:
```kotlin
import com.example.monitordecuidados.CapabilitiesAssessmentActivity
```

### ARCHIVOS A MODIFICAR:
- `OnboardingActivity.kt` (líneas 59-68)
- `RoleSelectorActivity.kt` (líneas 21-27)
- `SplashActivity.kt` (líneas 40-42)

### CONSERVAR (dentro de cada archivo):
- OnboardingActivity: TODO lo demás (fragments, setupViewPager, nextStep). Solo cambiar finishOnboarding()
- RoleSelectorActivity: cardMonitor click handler (NO TOCAR). Solo cambiar cardPatient handler
- SplashActivity: "monitor" y "else" branches (NO TOCAR). Solo cambiar "terminal" branch

### NO TOCAR:
- CapabilitiesAssessmentActivity.kt (ya funciona correctamente)
- TerminalMainActivity.kt
- MonitorMainActivity.kt

### VERIFICACIÓN:
- [ ] Build pasa
- [ ] Solo 3 archivos en git diff
- [ ] SplashActivity: usuario terminal sin capabilities_status → va a CapabilitiesAssessmentActivity
- [ ] SplashActivity: usuario terminal con capabilities_status=completed → va a TerminalMainActivity
- [ ] OnboardingActivity: terminal completa onboarding → va a CapabilitiesAssessmentActivity (primera vez)
- [ ] RoleSelectorActivity: switch a terminal sin capabilities → va a CapabilitiesAssessmentActivity

---

## T45: Walkie-talkie in-place en TerminalDetailActivity (eliminar VideoActivity para audio)

**Problema**: btnCall en TerminalDetailActivity abre VideoActivity con mode="audiocall". Per SRS, las llamadas de voz son walkie-talkie in-place — el botón "Llamar" debe cambiar a "Colgar" en la MISMA interfaz sin abrir otra Activity.

**Causa raíz**: La implementación actual redirige a VideoActivity para audio, contradiciendo la especificación SRS.

### SNAPSHOT (TerminalDetailActivity.kt — 159 líneas):
- Archivo: `app/src/main/java/com/example/monitordecuidados/TerminalDetailActivity.kt`
- Líneas 43-49: btnCall click:
```kotlin
binding.btnCall.setOnClickListener {
    startActivity(Intent(this, VideoActivity::class.java).apply {
        putExtra("mode", "audiocall")
        putExtra("remote_ip", remoteIp)
    })
}
```

### CAMBIO en TerminalDetailActivity.kt:
Reemplazar el click handler de btnCall para usar CallManager directamente en la Activity:

1. Añadir variable de instancia después de las existentes:
```kotlin
private var callManager: CallManager? = null
private var isCallActive = false
```

2. Reemplazar el click handler:
```kotlin
binding.btnCall.setOnClickListener {
    if (!isCallActive) {
        // Iniciar llamada walkie-talkie in-place
        callManager = CallManager(remoteIp, 5060)
        callManager?.startCall()
        isCallActive = true
        binding.btnCall.text = "Colgar"
        binding.btnCall.setBackgroundColor(resources.getColor(android.R.color.holo_red_dark, theme))
    } else {
        // Colgar
        callManager?.endCall()
        callManager = null
        isCallActive = false
        binding.btnCall.text = "Llamar"
        binding.btnCall.setBackgroundColor(resources.getColor(R.color.teal_700, theme))
    }
}
```

3. Añadir onDestroy para limpiar:
```kotlin
override fun onDestroy() {
    super.onDestroy()
    callManager?.endCall()
}
```

4. Añadir import:
```kotlin
import com.example.monitordecuidados.communication.CallManager
```

### ARCHIVOS A MODIFICAR:
- `TerminalDetailActivity.kt` (líneas 43-49 + nuevas variables + onDestroy)

### CONSERVAR (dentro del archivo):
- btnMonitor click handler (lanza VideoActivity con mode="monitor") — NO TOCAR
- queryTerminalStatus() — NO TOCAR
- setupSwitchListeners() — NO TOCAR
- pollHandler/pollRunnable (T43) — NO TOCAR
- onResume/onPause (T43) — NO TOCAR
- TODO el layout XML — NO TOCAR

### NO TOCAR:
- VideoActivity.kt (sigue siendo necesaria para monitoreo)
- CallManager.kt (se usa tal cual)
- activity_terminal_detail.xml

### VERIFICACIÓN:
- [ ] Build pasa
- [ ] Solo TerminalDetailActivity.kt en git diff
- [ ] Botón "Llamar" NO abre VideoActivity
- [ ] Botón "Llamar" cambia texto a "Colgar" tras tap
- [ ] Segundo tap termina la llamada y regresa texto a "Llamar"

---

## T46: Implementar startAudioCall/stopAudioCall reales en CampanaService

**Problema**: startAudioCall() y stopAudioCall() en CampanaService son STUBS — solo toggle un boolean y actualizan notificación. No hay audio real cuando el Monitor inicia una llamada hacia el Terminal.

**Causa raíz**: Funciones nunca implementadas. Sólo fachada.

### SNAPSHOT (CampanaService.kt — 474 líneas):
- Archivo: `app/src/main/java/com/example/monitordecuidados/CampanaService.kt`
- Líneas 421-427: Stubs actuales:
```kotlin
private fun startAudioCall() {
    isAudioCallActive = true
    updateNotification()
}

private fun stopAudioCall() {
    isAudioCallActive = false
    updateNotification()
}
```

### CAMBIO en CampanaService.kt:
Reemplazar los stubs con implementación real usando CallManager:

1. Añadir variable de instancia (junto a las otras variables de clase):
```kotlin
private var callManager: CallManager? = null
```

2. Reemplazar startAudioCall():
```kotlin
private fun startAudioCall() {
    isAudioCallActive = true
    updateNotification()
    val monitorIp = EncryptedPreferencesHelper.getString(this, "monitor_ip", "")
    if (monitorIp.isNotEmpty()) {
        callManager = CallManager(monitorIp, 5060)
        callManager?.startCall()
    }
}
```

3. Reemplazar stopAudioCall():
```kotlin
private fun stopAudioCall() {
    isAudioCallActive = false
    callManager?.endCall()
    callManager = null
    updateNotification()
}
```

4. Añadir import si no existe:
```kotlin
import com.example.monitordecuidados.communication.CallManager
```

### ARCHIVOS A MODIFICAR:
- `CampanaService.kt` (líneas 421-427 + nueva variable)

### CONSERVAR (dentro del archivo):
- TODO lo demás: NanoHTTPD server, VoiceCommandManager, SensorManager, notifications — NO TOCAR
- onStartCommand, onDestroy lógica existente — NO TOCAR
- updateNotification() — NO TOCAR (ya funciona)

### NO TOCAR:
- CallManager.kt
- TerminalDetailActivity.kt

### VERIFICACIÓN:
- [ ] Build pasa
- [ ] Solo CampanaService.kt en git diff
- [ ] startAudioCall() crea CallManager y llama startCall()
- [ ] stopAudioCall() llama endCall() y limpia

---

## T47: Migrar CallManager de PCM 16BIT a OPUS codec

**Problema**: CallManager usa PCM 16BIT a 8000Hz — audio crudo sin compresión. Consume excesivo ancho de banda en redes Wi-Fi débiles. OPUS provee mejor calidad a menor bitrate.

**Causa raíz**: Implementación inicial usó el formato más simple. Nunca se migró.

### SNAPSHOT (CallManager.kt — 308 líneas):
- Archivo: `app/src/main/java/com/example/monitordecuidados/communication/CallManager.kt`
- Líneas 18-23: Constantes PCM:
```kotlin
private val SAMPLE_RATE = 8000
private val CHANNEL_CONFIG_IN = AudioFormat.CHANNEL_IN_MONO
private val CHANNEL_CONFIG_OUT = AudioFormat.CHANNEL_OUT_MONO
private val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
private val BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG_IN, AUDIO_FORMAT) * 2
```
- Líneas 127-176: sendAudio() — graba PCM, envía bytes raw por UDP
- Líneas 178-230: receiveAudio() — recibe bytes raw, escribe a AudioTrack

### CAMBIO en CallManager.kt:
Usar `MediaCodec` con codec "audio/opus" para encode/decode. Cambios:

1. Aumentar SAMPLE_RATE a 16000 (OPUS funciona mejor a 16kHz):
```kotlin
private val SAMPLE_RATE = 16000
```

2. En sendAudio(): Crear MediaCodec encoder OPUS. Grabar PCM desde AudioRecord → feed al encoder → enviar compressed frames por UDP.

3. En receiveAudio(): Crear MediaCodec decoder OPUS. Recibir compressed frames por UDP → feed al decoder → escribir PCM decoded a AudioTrack.

4. Protocolo UDP: Cada paquete ahora es un frame OPUS (variable size, típicamente 40-80 bytes vs 640 bytes PCM).

**IMPORTANTE**: MediaCodec "audio/opus" está disponible desde API 21. Nuestro minSdk es 21+. No se necesita librería externa.

### ARCHIVOS A MODIFICAR:
- `CallManager.kt` (múltiples secciones: constantes, sendAudio, receiveAudio)

### CONSERVAR (dentro del archivo):
- startCall() / endCall() lógica de threads y socket — estructura igual
- KEEP_ALIVE handling en receiveAudio — mantener
- FileLogger calls — mantener todos los logs
- isCalling flag — mantener
- Constructor (remoteIp, port) — mantener

### NO TOCAR:
- CampanaService.kt
- VideoActivity.kt (usa WebRTC, no CallManager para video)
- TerminalDetailActivity.kt

### VERIFICACIÓN:
- [ ] Build pasa
- [ ] Solo CallManager.kt en git diff
- [ ] SAMPLE_RATE = 16000
- [ ] sendAudio usa MediaCodec encoder "audio/opus"
- [ ] receiveAudio usa MediaCodec decoder "audio/opus"
- [ ] UDP packets son frames OPUS (no PCM raw)

---

## T48: Implementar call_history — registro de llamadas en Firestore

**Problema**: La colección `call_history` está documentada en el SDD (Firestore Schema) pero CERO código la referencia. No se guarda historial de llamadas.

**Causa raíz**: Nunca implementado. Schema diseñado pero no codificado.

### Schema esperado (per SDD):
```
/call_history/{docId}
  - caller_id: String (userId del Monitor que inicia)
  - receiver_id: String (userId del Terminal que recibe)
  - start_time: Timestamp
  - end_time: Timestamp (nullable, null si en progreso)
  - duration_seconds: Long
  - call_type: "voice" | "video"
  - status: "completed" | "missed" | "cancelled"
```

### CAMBIO en TerminalDetailActivity.kt:
Al inicio de llamada (cuando isCallActive se pone true), escribir documento en Firestore:

```kotlin
// En el click handler de btnCall, DESPUÉS de callManager?.startCall():
val callDoc = hashMapOf(
    "caller_id" to FirebaseAuth.getInstance().currentUser?.uid,
    "receiver_id" to terminalId,
    "start_time" to FieldValue.serverTimestamp(),
    "end_time" to null,
    "duration_seconds" to 0L,
    "call_type" to "voice",
    "status" to "in_progress"
)
val callRef = FirebaseFirestore.getInstance().collection("call_history").document()
currentCallDocId = callRef.id
callRef.set(callDoc)
```

Al colgar (cuando isCallActive se pone false), actualizar documento:

```kotlin
// En el click handler de btnCall, DESPUÉS de callManager?.endCall():
currentCallDocId?.let { docId ->
    FirebaseFirestore.getInstance().collection("call_history").document(docId)
        .update(
            "end_time", FieldValue.serverTimestamp(),
            "status", "completed"
        )
    currentCallDocId = null
}
```

Añadir variable:
```kotlin
private var currentCallDocId: String? = null
```

Añadir imports:
```kotlin
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
```

### ARCHIVOS A MODIFICAR:
- `TerminalDetailActivity.kt` (dentro del click handler de btnCall de T45)

### CONSERVAR (dentro del archivo):
- Todo lo de T45 — esta tarea EXTIENDE T45, no lo reemplaza
- pollHandler, switches, queryTerminalStatus — NO TOCAR

### NO TOCAR:
- CampanaService.kt
- CallManager.kt

### DEPENDENCIA: Requiere T45 completada primero.

### VERIFICACIÓN:
- [ ] Build pasa
- [ ] Firestore collection "call_history" recibe documento al llamar
- [ ] Documento se actualiza con end_time al colgar
- [ ] currentCallDocId se limpia correctamente

---

## T49: Conectar CalibrationDialog a Settings del Terminal

**Problema**: CalibrationDialog.kt (126 líneas) es un DialogFragment funcional que calibra voz con 3 palabras (AUXILIO/AYUDA/SOCORRO), pero NADIE lo instancia. Debe ser accesible desde la sección de Sensores en Settings cuando voice_detection está habilitada.

**Causa raíz**: Código implementado pero nunca conectado al flujo de UI.

### SNAPSHOT (root_preferences.xml):
- Archivo: `app/src/main/res/xml/root_preferences.xml`
- Línea ~130: SwitchPreferenceCompat con key `voice_detection_enabled`
- Línea ~147: Categoría `terminal_sensors_cat` ("Sensores y Detección")

### CAMBIO en root_preferences.xml:
Añadir una Preference (botón) DESPUÉS del switch `voice_detection_enabled`:

```xml
<Preference
    app:key="calibrate_voice"
    app:title="Calibrar Detección de Voz"
    app:summary="Prueba las 3 palabras de emergencia"
    app:dependency="voice_detection_enabled"
    app:icon="@android:drawable/ic_btn_speak_now" />
```

`app:dependency="voice_detection_enabled"` hace que se deshabilite automáticamente si voz está off.

### CAMBIO en SettingsFragment.kt:
Añadir click listener para la nueva preferencia. En `onViewCreated()` o `onCreatePreferences()`, añadir:

```kotlin
findPreference<Preference>("calibrate_voice")?.setOnPreferenceClickListener {
    CalibrationDialog().show(parentFragmentManager, "calibration")
    true
}
```

Añadir import:
```kotlin
import com.example.monitordecuidados.dialogs.CalibrationDialog
```

### ARCHIVOS A MODIFICAR:
- `app/src/main/res/xml/root_preferences.xml` (añadir Preference después de voice_detection_enabled)
- `app/src/main/java/com/example/monitordecuidados/fragments/SettingsFragment.kt` (añadir click listener)

### CONSERVAR (dentro de cada archivo):
- root_preferences.xml: TODAS las preferences existentes — NO TOCAR ninguna. Solo AÑADIR la nueva.
- SettingsFragment.kt: TODOS los listeners existentes — NO TOCAR. Solo AÑADIR el nuevo.

### NO TOCAR:
- CalibrationDialog.kt (ya funciona correctamente)
- CampanaService.kt
- VoiceCommandManager.kt

### VERIFICACIÓN:
- [ ] Build pasa
- [ ] En Settings → Sensores y Detección: aparece "Calibrar Detección de Voz"
- [ ] Con voice_detection OFF, el botón calibrar está deshabilitado
- [ ] Con voice_detection ON, tap en calibrar abre el DialogFragment
- [ ] Dialog muestra "AUXILIO" como primera palabra

---

## T50: Completar VoiceAlertAdapter — bindings faltantes

**Problema**: VoiceAlertAdapter solo bindea 2 de 6 vistas del layout item_alert_voz.xml. Faltan: ivAlertIcon, tvTerminalName, btnCall, btnMonitor.

**Causa raíz**: Implementación parcial. Solo se conectaron mensaje y hora.

### SNAPSHOT (VoiceAlertAdapter.kt — 39 líneas):
- Archivo: `app/src/main/java/com/example/monitordecuidados/adapters/VoiceAlertAdapter.kt`
- Líneas 24-27: onBindViewHolder actual (solo 2 bindings):
```kotlin
override fun onBindViewHolder(holder: VoiceViewHolder, position: Int) {
    val alert = alerts[position]
    holder.binding.tvAlertMessage.text = alert.message
    holder.binding.tvAlertTime.text = formatTimestamp(alert.timestamp)
}
```

### SNAPSHOT (item_alert_voz.xml vistas):
- `ivAlertIcon` (ImageView) — icono micrófono
- `tvTerminalName` (TextView) — nombre del terminal
- `tvAlertMessage` (TextView) — frase detectada ✅ ya bindeada
- `tvAlertTime` (TextView) — timestamp ✅ ya bindeada
- `btnCall` (Button) — acción llamar
- `btnMonitor` (Button) — acción monitorear

### CAMBIO en VoiceAlertAdapter.kt:
1. Añadir interface para callbacks de botones (antes de la clase):
```kotlin
interface VoiceAlertListener {
    fun onCallClicked(event: Event)
    fun onMonitorClicked(event: Event)
}
```

2. Modificar constructor para recibir listener:
```kotlin
class VoiceAlertAdapter(
    private var alerts: List<Event>,
    private val listener: VoiceAlertListener? = null
) : RecyclerView.Adapter<VoiceAlertAdapter.VoiceViewHolder>() {
```

3. Completar onBindViewHolder:
```kotlin
override fun onBindViewHolder(holder: VoiceViewHolder, position: Int) {
    val alert = alerts[position]
    holder.binding.ivAlertIcon.setImageResource(R.drawable.ic_mic)
    holder.binding.tvTerminalName.text = alert.type
    holder.binding.tvAlertMessage.text = alert.message
    holder.binding.tvAlertTime.text = formatTimestamp(alert.timestamp)
    holder.binding.btnCall.setOnClickListener { listener?.onCallClicked(alert) }
    holder.binding.btnMonitor.setOnClickListener { listener?.onMonitorClicked(alert) }
}
```

4. Añadir import:
```kotlin
import com.example.monitordecuidados.R
```

### ARCHIVOS A MODIFICAR:
- `VoiceAlertAdapter.kt` (onBindViewHolder + constructor + interface)

### CONSERVAR (dentro del archivo):
- VoiceViewHolder class — NO TOCAR
- onCreateViewHolder — NO TOCAR
- getItemCount — NO TOCAR
- updateData — NO TOCAR
- formatTimestamp — NO TOCAR

### NO TOCAR:
- item_alert_voz.xml
- AlertAdapter.kt (standalone — es otro adapter)

### VERIFICACIÓN:
- [ ] Build pasa
- [ ] Solo VoiceAlertAdapter.kt en git diff
- [ ] Las 6 vistas del layout están bindeadas
- [ ] Botones tienen clickListeners funcionales

---

## T51: Consolidar AlertAdapter — Eliminar inner classes duplicadas

**Problema**: AlertLogActivity.kt (líneas 67-97) y HistoryFragment.kt (líneas 42-71) tienen inner classes `AlertAdapter` IDÉNTICAS byte por byte. Copilot ha creado `adapters/NotificationAlertAdapter.kt` como reemplazo compartido. Gemini debe eliminar las inner classes y usar el nuevo adapter.

**Causa raíz**: Copy-paste. Las dos inner classes son exactamente iguales.

### SNAPSHOT (AlertLogActivity.kt — 97 líneas):
- Archivo: `app/src/main/java/com/example/monitordecuidados/AlertLogActivity.kt`
- Líneas 67-97: Inner class AlertAdapter (ELIMINAR)
- Líneas ~25-30: Donde se instancia `AlertAdapter()` → cambiar a `NotificationAlertAdapter()`

### SNAPSHOT (HistoryFragment.kt — 71 líneas):
- Archivo: `app/src/main/java/com/example/monitordecuidados/fragments/HistoryFragment.kt`
- Líneas 42-71: Inner class AlertAdapter (ELIMINAR)
- Líneas ~20-25: Donde se instancia `AlertAdapter()` → cambiar a `NotificationAlertAdapter()`

### CAMBIO en AlertLogActivity.kt:
1. ELIMINAR la inner class `AlertAdapter` completa (líneas 67-97)
2. Cambiar todas las referencias de `AlertAdapter` a `NotificationAlertAdapter`
3. Añadir import:
```kotlin
import com.example.monitordecuidados.adapters.NotificationAlertAdapter
```
4. Cambiar declaración de variable de `AlertAdapter` a `NotificationAlertAdapter`

### CAMBIO en HistoryFragment.kt:
1. ELIMINAR la inner class `AlertAdapter` completa (líneas 42-71)
2. Cambiar todas las referencias de `AlertAdapter` a `NotificationAlertAdapter`
3. Añadir import:
```kotlin
import com.example.monitordecuidados.adapters.NotificationAlertAdapter
```
4. Cambiar declaración de variable de `AlertAdapter` a `NotificationAlertAdapter`

### ARCHIVOS A MODIFICAR:
- `AlertLogActivity.kt` (eliminar inner class, cambiar referencias)
- `fragments/HistoryFragment.kt` (eliminar inner class, cambiar referencias)

### NO TOCAR:
- `adapters/NotificationAlertAdapter.kt` (creado por Copilot, LISTO)
- `adapters/AlertAdapter.kt` (standalone para Dashboard, usa Event model — DIFERENTE)
- item_alert_campana.xml

### VERIFICACIÓN:
- [ ] Build pasa
- [ ] AlertLogActivity.kt NO tiene inner class AlertAdapter
- [ ] HistoryFragment.kt NO tiene inner class AlertAdapter
- [ ] Ambos usan NotificationAlertAdapter del paquete adapters
- [ ] Solo 2 archivos en git diff (el nuevo adapter ya fue creado por Copilot)

### CONSERVAR (no modificar):
- `queryTerminalStatus()` — lógica HTTP GET intacta, ya maneja `isUpdatingFromServer`
- `sendCommandToTerminal()` — lógica HTTP POST intacta
- `setupDefaultSwitchStates()` — fallback intacto
- `updateStatusText()` — sin cambios
- Todos los `setOnCheckedChangeListener` con `if (!isUpdatingFromServer)` — intactos
- Toolbar, botones, tvServiceStatusIndicator — sin cambios

### NO TOCAR estos archivos:
- `CampanaHttpServer.kt`
- `CampanaService.kt`
- `TerminalQRFragment.kt` (recién arreglado en T42)
- `activity_terminal_detail.xml`

### ARCHIVOS A MODIFICAR:
1. `app/src/main/java/com/example/monitordecuidados/TerminalDetailActivity.kt`

### VERIFICACIÓN:
1. Abrir Monitor → TerminalDetailActivity con Terminal vinculado
2. En Terminal, toggle switch Campanilla OFF → switch en Monitor debe cambiar a OFF en ~5 segundos
3. Toggle de vuelta ON → Monitor refleja ON en ~5 segundos
4. Repetir con los 4 switches
5. Al salir de TerminalDetailActivity (back), el polling se detiene (no consume recursos en background)
6. Al volver a entrar, el polling se reinicia automáticamente
7. Los switches del Monitor siguen enviando comandos HTTP al tocarlos manualmente (no regresión T39-FIX)

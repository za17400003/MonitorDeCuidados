# WORKITEMS v22.0 — CORRECTIVE v2: COMPLETADO (Abril 2, 2026)

> **ACTUALIZADO**: Abril 2, 2026
> **ESTADO**: T86, T87, T83b, T84b ejecutados por Gemini y VERIFICADOS por Copilot. Coherence check: 7/7 ✅. Pendiente: testing en dispositivo real.
> **PRINCIPIO ARQUITECTÓNICO**: LOCAL-FIRST — la app funciona perfectamente en WiFi local sin internet. Firestore = backup/cross-network.
> **PRIORIDAD**: T72 > T73 (features pendientes)
> **PROHIBIDO**: No hardcodear strings, keys, IPs, URLs, secrets, ni valores que deban ser configurables.

---

## HISTÓRICO COMPLETADO (código + build ✅)

| Tarea | Estado |
|-------|--------|
| T14-T56 | ✅ código + build |
| T57v2: Pairing síncrono con retry | ✅ código |
| T58: Walkie-talkie señalización HTTP | ✅ código |
| T59v2: Video diagnóstico logs | ✅ código |
| T60: Toggle videollamada/silencioso | ✅ confirmado en dispositivo |
| T61: Battery polling DashboardFragment | ✅ código |
| T62: VoiceCommandManager leak fix | ✅ código |
| T63: Pairing auto-heal healPairingIfNeeded() | ✅ aplicado |
| T64: RECORD_AUDIO permission check walkie-talkie | ✅ aplicado |
| T65: VideoActivity permission retry handler | ✅ aplicado |
| T66: CampanaService userRole refresh | ✅ aplicado |
| T67: SpeechRecognizer availability check | ✅ aplicado |
| T68: Alert end-to-end trace logs | ✅ aplicado |
| T69: DashboardFragment LOCAL-FIRST Room DB | ✅ aplicado |
| T70: DashboardFragment pase terminal_id | ✅ aplicado |
| T71: SecurityConfig KeyStore AES keys | ✅ aplicado |
| T74: Self-heal Terminal guarda Monitor IP en /status | ✅ CONFIRMADO EN DISPOSITIVO |
| T75: Periodic heal DashboardFragment /confirm_pairing | ✅ CONFIRMADO EN DISPOSITIVO |
| T76: Fix Content-Type JSON en bell/shake/voice POSTs | ✅ CONFIRMADO EN DISPOSITIVO |
| T77-T82+T79b: Bubbles + NotificationHelper + TerminalBubbleManager | ✅ aplicado |
| T83-T85: Fix notificación + agrupación + bubble intents | ✅ aplicado (parcial, bugs persistieron) |
| T83b: Fix notificación extra + canal burbujas + guard areBubblesAllowed | ✅ aplicado — verificado por Copilot |
| T84b: Fix layout alert card vertical (info arriba, botones abajo) | ✅ aplicado — verificado por Copilot |
| T86: Fix video monitoring: streaming directo desde CampanaService | ✅ aplicado — verificado por Copilot |
| T87: Fix llamada de voz: socket reuseAddress + puerto 9050 + OPUS guard | ✅ aplicado — verificado por Copilot |

---

## 🔴 ROOT CAUSE v19.0 — RESUELTO ✅

> T74-T76 corrigieron el NULL en `paired_monitor_ip` y la estandarización JSON. Alertas confirmadas funcionando en dispositivo real.

---

## 🔴 ROOT CAUSE v21.0 — BUGS POST-BUBBLES

> T77-T82+T79b implementados por Gemini. Testing en dispositivo reveló 3 bugs:
> 1. **Notificación extra**: TerminalBubbleManager crea notificación ADICIONAL (ID=200+hash) encima de la del servicio (ID=1). En API<30 el fallback crea heads-up redundante. En API 30+ si burbujas no se activan, la notificación aparece como regular.
> 2. **Dashboard spam**: EventDao.getRecentEvents(50) retorna 1 fila por evento. AlertAdapter renderiza 1 card por fila = spam de cards individuales con botones LLAMAR/MONITOREAR repetidos.
> 3. **Burbujas no aparecen**: TerminalBubbleManager apunta bubbleIntent+shortcutIntent a TerminalDetailActivity (sin allowEmbedded) en vez de BubbleRadialActivity. Android rechaza la burbuja silenciosamente. desiredHeight=600 en vez de 350.

---

## 🔴 ROOT CAUSE v22.0 — RESUELTO ✅

> T86, T87, T83b, T84b aplicados por Gemini. Verificación de código por Copilot: 7/7 coherence check passed. Pendiente testing en dispositivo real.
>
> **BUG 1 RESUELTO: Notificación extra** — T83b: deleteNotificationChannel + recrear con allowBubbles, areBubblesAllowed guard, setSuppressNotification(true) siempre. ✅
>
> **BUG 2 RESUELTO: Burbujas no aparecen** — T83b: Canal eliminado y recreado con allowBubbles(true). T85 ya apuntaba intents a BubbleRadialActivity. ✅
>
> **BUG 3 RESUELTO: Layout apretado** — T84b: Layout vertical con info arriba (full width) y botones abajo (aligned end). ✅
>
> **BUG 4 RESUELTO: Video pantalla negra** — T86: CampanaService crea VideoManager directamente sin lanzar Activity. Streaming UDP:9001 desde Service. ✅
>
> **BUG 5 RESUELTO: Llamada de voz** — T87: DatagramSocket(null) + reuseAddress before bind, puerto 5060→9050, OPUS try-catch. ✅

---

## 🎯 TAREAS PENDIENTES v22.0

| # | Tarea | Prioridad | Pipeline | Archivos | Esfuerzo |
|---|-------|-----------|----------|----------|----------|
| T72 | Alarm bidirectional Firestore real-time listener | 🟢 MEDIUM | A (feature) | CampanaService.kt, SyncManager.kt | Alto |
| T73 | CapabilitiesAssessment datos en Monitor tras QR scan | 🟢 MEDIUM | A (feature) | QRScannerActivity.kt | Alto |

---

## ~~T77-T85: IMPLEMENTADOS — VER HISTÓRICO~~ ✅

(Las especificaciones detalladas de T77-T85 se preservan abajo para referencia, pero ya están implementadas. Los bugs encontrados en testing v22.0 se abordan en T83b-T84b.)

---

## T83b: Fix notificación extra + canal burbujas (CRITICAL)

**Problema 1 — Notificación duplicada en shade**: `showBubbleNotification()` llama `nm.notify(notificationId, notification)` que SIEMPRE crea una entrada en el notification shade. Si Android no puede mostrar la burbuja (canal sin `allowBubbles` cacheado, usuario no habilitó burbujas, etc.), la notificación aparece como notificación regular → duplica la del servicio (ID=1).

**Problema 2 — Canal cacheado**: El canal `alerts` fue creado en instalaciones anteriores SIN `setAllowBubbles(true)`. `createNotificationChannel()` NO actualiza `setAllowBubbles` en canales existentes. Hay que ELIMINAR el canal y RECREARLO.

**Problema 3 — setSuppressNotification incorrecto**: El código usa `.setSuppressNotification(alertCount > 1)` que en la primera alerta es FALSE → la primera alerta SIEMPRE aparece en el shade como notificación regular (además de la burbuja, si es que aparece).

**ARCHIVO 1**: `app/src/main/java/com/example/monitordecuidados/utils/NotificationHelper.kt`

SNAPSHOT (estado actual en `createNotificationChannels`):
```kotlin
            val alertChannel = NotificationChannel(
                ALERT_CHANNEL_ID,
                "Alertas de Cuidados",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones acumuladas del servicio de Monitor"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    setAllowBubbles(true)
                }
            }
            manager.createNotificationChannel(alertChannel)
```

CAMBIO EXACTO — ANTES de crear el alertChannel, ELIMINAR el canal existente para forzar recreación con allowBubbles:
```kotlin
            // T83b: Eliminar canal existente para forzar recreación con allowBubbles=true.
            // createNotificationChannel() NO actualiza allowBubbles en canales ya existentes.
            manager.deleteNotificationChannel(ALERT_CHANNEL_ID)

            val alertChannel = NotificationChannel(
                ALERT_CHANNEL_ID,
                "Alertas de Cuidados",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones acumuladas del servicio de Monitor"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    setAllowBubbles(true)
                }
            }
            manager.createNotificationChannel(alertChannel)
```

**ARCHIVO 2**: `app/src/main/java/com/example/monitordecuidados/utils/TerminalBubbleManager.kt`

Hay 3 cambios en `showBubbleNotification()`:

### Cambio 1: Guard con areBubblesAllowed()

SNAPSHOT (estado actual — inicio de showBubbleNotification):
```kotlin
    @RequiresApi(Build.VERSION_CODES.R)
    private fun showBubbleNotification(
        context: Context,
        nm: NotificationManager,
        notificationId: Int,
        terminalId: String,
        terminalName: String,
        terminalIp: String?,
        alertTitle: String,
        alertMessage: String,
        alertCount: Int
    ) {
        val shortcutId = "terminal_$terminalId"
```

CAMBIO EXACTO — Agregar guard al inicio del método:
```kotlin
    @RequiresApi(Build.VERSION_CODES.R)
    private fun showBubbleNotification(
        context: Context,
        nm: NotificationManager,
        notificationId: Int,
        terminalId: String,
        terminalName: String,
        terminalIp: String?,
        alertTitle: String,
        alertMessage: String,
        alertCount: Int
    ) {
        // T83b: Si burbujas no están habilitadas en el sistema, NO crear notificación.
        // La notificación del servicio (ID=1) ya muestra las alertas acumuladas.
        if (!nm.areBubblesAllowed()) {
            Log.d(TAG, "Bubbles not allowed by system — skipping bubble notification for '$terminalName'")
            return
        }

        val shortcutId = "terminal_$terminalId"
```

### Cambio 2: setSuppressNotification SIEMPRE true

SNAPSHOT (estado actual):
```kotlin
        val bubbleMetadata = NotificationCompat.BubbleMetadata.Builder(
            bubblePendingIntent,
            IconCompat.createWithResource(context, R.drawable.ic_notification_bell)
        )
            .setDesiredHeight(350) // T85: Height fix
            .setAutoExpandBubble(alertCount == 1)
            .setSuppressNotification(alertCount > 1)
            .build()
```

CAMBIO EXACTO:
```kotlin
        val bubbleMetadata = NotificationCompat.BubbleMetadata.Builder(
            bubblePendingIntent,
            IconCompat.createWithResource(context, R.drawable.ic_notification_bell)
        )
            .setDesiredHeight(350)
            .setAutoExpandBubble(alertCount == 1)
            .setSuppressNotification(true) // T83b: SIEMPRE suprimir del shade. Solo mostrar como burbuja flotante.
            .build()
```

### Cambio 3: Log mejorado

SNAPSHOT (estado actual):
```kotlin
        nm.notify(notificationId, notification)
        Log.d(TAG, "Bubble for '$terminalName' (id=$notificationId, alerts=$alertCount)")
```

CAMBIO EXACTO:
```kotlin
        nm.notify(notificationId, notification)
        Log.d(TAG, "Bubble for '$terminalName' (id=$notificationId, alerts=$alertCount, suppressed=true)")
```

ARCHIVOS A MODIFICAR:
- NotificationHelper.kt (1 cambio: deleteNotificationChannel antes de crear alertChannel)
- TerminalBubbleManager.kt (3 cambios: guard areBubblesAllowed, setSuppressNotification(true), log)

NO TOCAR:
- CampanaService.kt
- BubbleRadialActivity.kt
- AndroidManifest.xml
- Los intents del bubble (ya están correctos apuntando a BubbleRadialActivity)
- alertHistory, updateServiceNotificationWithAlerts()

VERIFICACIÓN:
- [ ] Build pasa
- [ ] Desinstalar app completamente antes de probar (para limpiar canal cacheado). O bien: ir a Ajustes → Apps → Monitor de Cuidados → Notificaciones → verificar que "Alertas de Cuidados" tiene burbujas habilitadas.
- [ ] Terminal envía 3 campanas → Monitor muestra SOLO la notificación del servicio (ID=1) en el shade
- [ ] NO aparece notificación extra "Campana (+N)" en el shade
- [ ] Si burbujas están habilitadas en sistema: aparece burbuja flotante → expandir muestra menú radial
- [ ] Si burbujas NO están habilitadas: no aparece nada extra (solo servicio ID=1)
- [ ] Logcat muestra "Bubbles not allowed" o "Bubble for 'X' (suppressed=true)"

---

## T84b: Fix layout alert card — vertical con botones en fila inferior (CRITICAL)

**Problema**: El layout actual de `item_alert_campana.xml` usa orientación horizontal con icon + texto + 2 botones en la MISMA fila. Los botones "LLAMAR" + "MONITOREAR" (wrap_content, ~90dp cada uno) consumen demasiado espacio horizontal. La columna de texto (layout_weight=1) queda con ~80dp de ancho, causando que el texto se parta en líneas de 1-2 caracteres: "Termi\nnal", "Se ha\npedid\no ayu\nda d\nesde\nTermi\nnal".

**Fix**: Cambiar a layout vertical de 2 filas:
- Fila 1: icon + info (terminal name + message + timestamp) — usa todo el ancho
- Fila 2: botones LLAMAR + MONITOREAR alineados a la derecha

**ARCHIVO**: `app/src/main/res/layout/item_alert_campana.xml`

REEMPLAZAR TODO EL CONTENIDO con:
```xml
<?xml version="1.0" encoding="utf-8"?>
<com.google.android.material.card.MaterialCardView xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginVertical="4dp"
    app:cardCornerRadius="@dimen/card_corner_radius"
    app:cardElevation="@dimen/card_elevation"
    app:cardBackgroundColor="?attr/colorSurface"
    app:strokeColor="?attr/colorOutline"
    app:strokeWidth="1dp">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="@dimen/card_padding">

        <!-- Fila 1: Icon + Info -->
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:gravity="center_vertical">

            <TextView
                android:id="@+id/tvAlertIcon"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:textSize="24sp"
                tools:text="🛎️" />

            <LinearLayout
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:layout_marginStart="@dimen/spacing_md"
                android:orientation="vertical">

                <TextView
                    android:id="@+id/tvTerminalName"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    tools:text="Terminal"
                    android:textColor="@color/text_secondary"
                    android:textSize="@dimen/text_label" />

                <TextView
                    android:id="@+id/tvAlertMessage"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    tools:text="Se ha pedido ayuda desde Terminal"
                    android:textColor="@color/text_primary"
                    android:textSize="@dimen/text_body"
                    android:textStyle="bold" />

                <TextView
                    android:id="@+id/tvAlertTime"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    tools:text="13:39:12"
                    android:textColor="@color/text_hint"
                    android:textSize="@dimen/text_timestamp" />
            </LinearLayout>
        </LinearLayout>

        <!-- Fila 2: Botones alineados a la derecha -->
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:gravity="end"
            android:layout_marginTop="@dimen/spacing_sm">

            <Button
                android:id="@+id/btnCall"
                android:layout_width="wrap_content"
                android:layout_height="40dp"
                android:text="@string/monitor_call"
                android:textSize="14sp"
                android:background="@drawable/bg_action_button"
                android:textColor="@color/white"
                android:contentDescription="@string/cd_call_button"
                android:layout_marginEnd="@dimen/spacing_sm"/>

            <Button
                android:id="@+id/btnMonitor"
                android:layout_width="wrap_content"
                android:layout_height="40dp"
                android:text="@string/monitor_watch"
                android:textSize="14sp"
                android:background="@drawable/bg_action_button"
                android:textColor="@color/white"
                android:contentDescription="@string/cd_monitor_button"/>
        </LinearLayout>

    </LinearLayout>
</com.google.android.material.card.MaterialCardView>
```

ARCHIVOS A MODIFICAR:
- item_alert_campana.xml (reemplazar contenido completo)

NO TOCAR:
- AlertAdapter.kt (— ViewBinding IDs no cambian: tvAlertIcon, tvTerminalName, tvAlertMessage, tvAlertTime, btnCall, btnMonitor)
- DashboardFragment.kt
- dimens.xml

VERIFICACIÓN:
- [ ] Build pasa
- [ ] Alert card muestra texto completo sin truncar ("Se ha pedido ayuda desde Terminal" en una línea)
- [ ] Nombre del terminal visible arriba del mensaje
- [ ] Botones LLAMAR y MONITOREAR en fila inferior, alineados a la derecha
- [ ] Botón LLAMAR funciona (abre TerminalDetailActivity con auto_call)
- [ ] Botón MONITOREAR funciona (abre VideoActivity)
- [ ] Card no tiene scroll interno ni corte

---

## ~~T83-T85: APLICADOS PARCIALMENTE — VER HISTÓRICO~~ ✅

(T83: fallback eliminado ✅. T84: agrupación correcta ✅. T85: intents correctos ✅. Bugs restantes se corrigen en T83b y T84b.)

---

## ~~T83: Eliminar notificación extra — suprimir fallback + fix bubble target (CRITICAL)~~ ✅ APLICADO

**Problema**: Cuando llega una alerta, aparecen DOS notificaciones en el shade:
1. Notificación del servicio (ID=1) — CORRECTA, acumula alertas ✅
2. Notificación de TerminalBubbleManager (ID=200+hash) — REDUNDANTE ❌

En API<30, `showFallbackNotification()` crea una heads-up completa que duplica la info de la notificación del servicio. En API 30+ cuando burbujas no se activan (canal no habilitado, etc.), la notificación de burbuja cae como notificación regular.

**El usuario dijo**: "solamente tiene que acumularse las notificaciones en la del servicio del monitor osea la de abajo"

**ARCHIVO 1**: `app/src/main/java/com/example/monitordecuidados/utils/TerminalBubbleManager.kt`

SNAPSHOT (estado actual):
```kotlin
// Líneas 47-53:
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        showBubbleNotification(context, nm, notificationId, terminalId, terminalName, alertTitle, alertMessage, count)
    } else {
        showFallbackNotification(context, nm, notificationId, terminalId, terminalName, alertTitle, alertMessage, count)
    }
```

CAMBIO EXACTO:

1. **Eliminar `showFallbackNotification()` del branch API<30** — En API<30 NO crear ninguna notificación extra. La notificación del servicio (ID=1) ya muestra las alertas acumuladas. Solo incrementar el contador interno.
```kotlin
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        showBubbleNotification(context, nm, notificationId, terminalId, terminalName, alertTitle, alertMessage, count)
    }
    // API < 30: NO crear notificación extra.
    // La notificación del servicio (ID=1) ya acumula las alertas vía CampanaService.updateServiceNotificationWithAlerts().
```

2. **Eliminar el método `showFallbackNotification()` completo** (líneas 127-159). Ya no se usa.

**ARCHIVO 2**: `app/src/main/java/com/example/monitordecuidados/utils/NotificationHelper.kt`

SNAPSHOT (estado actual):
```kotlin
// Línea 29:
    private var alertNotificationId = 100  // Incrementa para cada alerta nueva (T77: deprecated by T80)
```

CAMBIO EXACTO:
- **Eliminar línea 29** (`private var alertNotificationId = 100`). Esta variable ya no se usa desde que T80 delegó a TerminalBubbleManager. Es código muerto.

ARCHIVOS A MODIFICAR:
- TerminalBubbleManager.kt (eliminar branch fallback + eliminar método showFallbackNotification)
- NotificationHelper.kt (eliminar variable alertNotificationId muerta)

NO TOCAR:
- `showBubbleNotification()` (se corrige en T85)
- CampanaService.kt
- La lógica de alertHistory en NotificationHelper
- `updateServiceNotificationWithAlerts()` — esta es la notificación CORRECTA

VERIFICACIÓN:
- [ ] Build pasa
- [ ] Terminal envía 3 campanas → Monitor muestra SOLO la notificación del servicio (ID=1) con alertas acumuladas
- [ ] NO aparece notificación extra encima (ID=200+hash eliminado en API<30)
- [ ] En API 30+ la notificación de burbuja sigue saliendo (se corrige su target en T85)

---

## T84: Fix dashboard spam — agrupar alertas por terminal en vez de 1 card por evento (CRITICAL)

**Problema**: `DashboardFragment` observa `notificationViewModel.notificationList` que viene de `EventDao.getRecentEvents(50)` → retorna hasta 50 filas individuales de Room DB. `AlertAdapter` renderiza 1 `MaterialCardView` por evento con botones LLAMAR y MONITOREAR propios. Resultado: 2 campanas = 2 cards separadas con 4 botones totales = spam visual.

**El usuario vio**: 2 cards de "Se ha pedido ayuda desde Terminal" con timestamps 09:07:39 y 09:07:43, cada una con sus propios botones.

**FIX**: Agrupar eventos por `sourceTerminalName` (o `sourceIp`). Mostrar 1 card por terminal con badge de conteo y timestamp del más reciente. Los botones LLAMAR/MONITOREAR aparecen UNA sola vez por terminal.

**ARCHIVO 1**: `app/src/main/java/com/example/monitordecuidados/data/local/EventDao.kt`

AGREGAR query nueva:
```kotlin
    /**
     * T84: Retorna el evento más reciente por cada sourceIp (= 1 fila por terminal).
     * Usado por DashboardFragment para mostrar 1 card por terminal.
     */
    @Query("""
        SELECT e.* FROM events e
        INNER JOIN (
            SELECT sourceIp, MAX(timestamp) as maxTs
            FROM events
            WHERE type IN ('bell', 'shake', 'voice')
            GROUP BY sourceIp
        ) grouped ON e.sourceIp = grouped.sourceIp AND e.timestamp = grouped.maxTs
        ORDER BY e.timestamp DESC
        LIMIT :limit
    """)
    fun getGroupedAlertsByTerminal(limit: Int = 20): LiveData<List<Event>>
```

AGREGAR query para contar alertas por terminal:
```kotlin
    /**
     * T84: Cuenta total de alertas por sourceIp (para badge).
     */
    @Query("SELECT COUNT(*) FROM events WHERE sourceIp = :sourceIp AND type IN ('bell', 'shake', 'voice')")
    fun getAlertCountByTerminal(sourceIp: String): LiveData<Int>
```

**ARCHIVO 2**: `app/src/main/java/com/example/monitordecuidados/viewmodels/NotificationViewModel.kt`

SNAPSHOT (estado actual):
```kotlin
    val notificationList: LiveData<List<Event>> = eventDao.getRecentEvents(50)
```

CAMBIO EXACTO — Cambiar a query agrupada:
```kotlin
    // T84: 1 card por terminal, no 1 card por evento
    val notificationList: LiveData<List<Event>> = eventDao.getGroupedAlertsByTerminal(20)
```

**ARCHIVO 3**: `app/src/main/java/com/example/monitordecuidados/adapters/AlertAdapter.kt`

Agregar al `onBindViewHolder()` — mostrar nombre de terminal y manejar conteo:

SNAPSHOT (estado actual):
```kotlin
    override fun onBindViewHolder(holder: AlertViewHolder, position: Int) {
        val alert = alerts[position]
        holder.binding.tvAlertMessage.text = alert.message
        val timeString = formatTimestamp(alert.timestamp)
        holder.binding.tvAlertTime.text = timeString
```

CAMBIO EXACTO — Agregar terminal name:
```kotlin
    override fun onBindViewHolder(holder: AlertViewHolder, position: Int) {
        val alert = alerts[position]
        holder.binding.tvAlertMessage.text = alert.message
        val timeString = formatTimestamp(alert.timestamp)
        holder.binding.tvAlertTime.text = timeString
        // T84: Mostrar nombre del terminal fuente
        holder.binding.tvTerminalName.text = alert.sourceTerminalName.ifBlank { alert.sourceIp }
```

Los botones LLAMAR y MONITOREAR del card necesitan funcionar. Agregar callback al adapter:

SNAPSHOT (estado actual):
```kotlin
class AlertAdapter(private var alerts: List<Event>) : RecyclerView.Adapter<AlertAdapter.AlertViewHolder>() {
```

CAMBIO EXACTO:
```kotlin
class AlertAdapter(
    private var alerts: List<Event>,
    private val onCallClick: ((Event) -> Unit)? = null,
    private val onMonitorClick: ((Event) -> Unit)? = null
) : RecyclerView.Adapter<AlertAdapter.AlertViewHolder>() {
```

Agregar click listeners en `onBindViewHolder`, DESPUÉS de la línea de accessibility:
```kotlin
        // T84: Acciones por terminal
        holder.binding.btnCall.setOnClickListener { onCallClick?.invoke(alert) }
        holder.binding.btnMonitor.setOnClickListener { onMonitorClick?.invoke(alert) }
```

**ARCHIVO 4**: `app/src/main/java/com/example/monitordecuidados/fragments/DashboardFragment.kt`

SNAPSHOT (estado actual):
```kotlin
        alertAdapter = AlertAdapter(emptyList())
```

CAMBIO EXACTO — Pasar callbacks:
```kotlin
        alertAdapter = AlertAdapter(
            alerts = emptyList(),
            onCallClick = { event -> navigateToTerminalDetail(event, autoCall = true) },
            onMonitorClick = { event -> navigateToMonitor(event) }
        )
```

AGREGAR estos dos métodos helper a DashboardFragment (antes de `onDestroyView()`):
```kotlin
    private fun navigateToTerminalDetail(event: Event, autoCall: Boolean = false) {
        val ip = EncryptedPreferencesHelper.getString(requireContext(), "paired_terminal_ip") ?: return
        startActivity(Intent(requireContext(), TerminalDetailActivity::class.java).apply {
            putExtra("terminal_id", event.sourceIp)
            putExtra("terminal_name", event.sourceTerminalName.ifBlank { "Terminal" })
            putExtra("terminal_status", "connected")
            if (autoCall) putExtra("auto_call", true)
        })
    }

    private fun navigateToMonitor(event: Event) {
        val ip = EncryptedPreferencesHelper.getString(requireContext(), "paired_terminal_ip") ?: return
        startActivity(Intent(requireContext(), com.example.monitordecuidados.VideoActivity::class.java).apply {
            putExtra("mode", "monitor")
            putExtra("remote_ip", ip)
        })
    }
```

ARCHIVOS A MODIFICAR:
- EventDao.kt (agregar 2 queries nuevas)
- NotificationViewModel.kt (cambiar a query agrupada)
- AlertAdapter.kt (agregar callbacks + terminal name binding)
- DashboardFragment.kt (pasar callbacks + agregar helpers)

NO TOCAR:
- item_alert_campana.xml — ya tiene tvTerminalName, btnCall, btnMonitor
- Event data class
- saveEventToLocalDb() en CampanaService
- NotificationHelper.kt

VERIFICACIÓN:
- [ ] Build pasa
- [ ] Terminal envía 5 campanas → Dashboard muestra 1 card por terminal (no 5 cards)
- [ ] Card muestra nombre del terminal + timestamp del más reciente
- [ ] Botón LLAMAR abre TerminalDetailActivity con auto_call=true
- [ ] Botón MONITOREAR abre VideoActivity
- [ ] tvNoAlerts se muestra cuando no hay alertas

---

## T85: Fix burbujas — apuntar intents a BubbleRadialActivity + extras correctos (CRITICAL)

**Problema**: `showBubbleNotification()` en TerminalBubbleManager.kt lanza `TerminalDetailActivity` en lugar de `BubbleRadialActivity`. `TerminalDetailActivity` NO tiene `allowEmbedded=true` ni `resizeableActivity=true` en el manifest, lo cual es REQUERIDO para burbujas. Android rechaza la burbuja silenciosamente y muestra la notificación como regular.

**ARCHIVO**: `app/src/main/java/com/example/monitordecuidados/utils/TerminalBubbleManager.kt`

SNAPSHOT (estado actual) — shortcutIntent:
```kotlin
// Líneas 78-83:
        val shortcutIntent = Intent(context, TerminalDetailActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra("terminal_id", terminalId)
            putExtra("terminal_name", terminalName)
            putExtra("terminal_status", "connected")
        }
```

CAMBIO EXACTO:
```kotlin
        val shortcutIntent = Intent(context, BubbleRadialActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra("terminal_id", terminalId)
            putExtra("terminal_name", terminalName)
            putExtra("terminal_ip", terminalIp)
            putExtra("alert_title", alertTitle)
            putExtra("alert_count", alertCount)
        }
```

SNAPSHOT (estado actual) — bubbleIntent:
```kotlin
// Líneas 94-100:
        val bubbleIntent = Intent(context, TerminalDetailActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra("terminal_id", terminalId)
            putExtra("terminal_name", terminalName)
            putExtra("terminal_status", "connected")
            putExtra("from_bubble", true)
        }
```

CAMBIO EXACTO:
```kotlin
        val bubbleIntent = Intent(context, BubbleRadialActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra("terminal_id", terminalId)
            putExtra("terminal_name", terminalName)
            putExtra("terminal_ip", terminalIp)
            putExtra("alert_title", alertTitle)
            putExtra("alert_count", alertCount)
        }
```

SNAPSHOT (estado actual) — desiredHeight:
```kotlin
// Línea 108:
            .setDesiredHeight(600)
```

CAMBIO EXACTO:
```kotlin
            .setDesiredHeight(350)
```

IMPORT — Agregar al inicio del archivo:
```kotlin
import com.example.monitordecuidados.BubbleRadialActivity
```

Y ELIMINAR import no usado:
```kotlin
import com.example.monitordecuidados.TerminalDetailActivity  // ← ELIMINAR
```

ARCHIVOS A MODIFICAR:
- TerminalBubbleManager.kt (3 cambios: shortcutIntent, bubbleIntent, desiredHeight + imports)

NO TOCAR:
- BubbleRadialActivity.kt — ya está correctamente implementada
- AndroidManifest.xml — BubbleRadialActivity ya tiene allowEmbedded+resizeableActivity
- showBubbleNotification() notification builder (solo los intents cambian)
- El cálculo de notificationId
- terminalAlertCounts

VERIFICACIÓN:
- [ ] Build pasa
- [ ] En dispositivo API 30+: campana → aparece burbuja flotante
- [ ] Al tocar burbuja → se expande mostrando BubbleRadialActivity con menú radial
- [ ] Menú radial muestra: Monitorear, Llamar, Controles
- [ ] Botón Monitorear → abre VideoActivity
- [ ] Botón Llamar → abre TerminalDetailActivity con auto_call=true
- [ ] Botón Controles → abre TerminalDetailActivity (panel de controles)
- [ ] En API <30: NO aparece burbuja NI notificación extra (solo la del servicio)

---

## ~~T77: Fix notification spam — usar ID constante para heads-up (CRITICAL)~~ ✅ IMPLEMENTADO

**Problema**: `alertNotificationId++` en NotificationHelper.kt línea 127 crea una notificación NUEVA por cada alerta (IDs 100, 101, 102...). El resultado es 10+ notificaciones separadas en el shade. La notificación del servicio (ID=1) SÍ acumula correctamente, pero queda ahogada por las heads-up individuales.

**Fix**: Usar ID constante `99` para TODAS las heads-up de alerta. Así Android REEMPLAZA la notificación anterior en lugar de crear una nueva. También incluir el conteo de alertas pendientes.

**ARCHIVO**: `app/src/main/java/com/example/monitordecuidados/utils/NotificationHelper.kt`

SNAPSHOT (estado actual):
```kotlin
// Línea 29:
    private var alertNotificationId = 100  // Incrementa para cada alerta nueva
```
```kotlin
// Líneas ~119-133:
        val notification = NotificationCompat.Builder(context, ALERT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_bell)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(alertNotificationId++, notification)
        if (alertNotificationId > 200) alertNotificationId = 100  // Reciclar IDs
```

CAMBIO EXACTO:

1. Línea 29 — Cambiar variable por constante:
```kotlin
    private const val ALERT_HEADS_UP_ID = 99  // ID fijo: reemplaza en lugar de acumular
```

2. Líneas ~113 — Cambiar PendingIntent requestCode:
```kotlin
        val pendingIntent = PendingIntent.getActivity(
            context, ALERT_HEADS_UP_ID, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
```

3. Líneas ~119-133 — Reemplazar bloque de notificación:
```kotlin
        val alertCount = alertHistory.size
        val notification = NotificationCompat.Builder(context, ALERT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_bell)
            .setContentTitle(if (alertCount > 1) "$title (+${alertCount - 1} más)" else title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setNumber(alertCount)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(ALERT_HEADS_UP_ID, notification)
```

ARCHIVOS A MODIFICAR:
- NotificationHelper.kt (líneas 29 y 110-133)

NO TOCAR:
- CampanaService.kt
- DashboardFragment.kt
- notification_custom.xml
- AlertEvent data class
- alertHistory list logic
- getAlertHistory(), resetAlertCount(), clearNotification()

VERIFICACIÓN:
- [ ] Build pasa
- [ ] Terminal envía 3 campanas → Monitor muestra UNA sola notificación heads-up (no 3 separadas)
- [ ] La notificación dice "🔔 Campana (+2 más)" en el título
- [ ] La notificación del servicio (ID=1) sigue mostrando el historial completo
- [ ] Tocar la notificación abre MonitorMainActivity

---

## T78: Bubble channel — setAllowBubbles + manifest BubbleRadialActivity (CRITICAL)

**Problema**: Para que Android muestre burbujas, necesitamos: (1) NotificationChannel con `setAllowBubbles(true)`, (2) BubbleRadialActivity con `allowEmbedded=true` y `resizeableActivity=true` en manifest (es el target del bubble, no TerminalDetailActivity).

**ARCHIVO 1**: `app/src/main/java/com/example/monitordecuidados/utils/NotificationHelper.kt`

SNAPSHOT (estado actual, líneas ~51-60):
```kotlin
            val alertChannel = NotificationChannel(
                ALERT_CHANNEL_ID,
                "Alertas de Cuidados",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones acumuladas del servicio de Monitor"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }
            manager.createNotificationChannel(alertChannel)
```

CAMBIO EXACTO — Agregar `setAllowBubbles` dentro del `.apply`:
```kotlin
            val alertChannel = NotificationChannel(
                ALERT_CHANNEL_ID,
                "Alertas de Cuidados",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones acumuladas del servicio de Monitor"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    setAllowBubbles(true)
                }
            }
            manager.createNotificationChannel(alertChannel)
```

**ARCHIVO 2**: `app/src/main/AndroidManifest.xml`

SNAPSHOT (estado actual, líneas ~72-74):
```xml
        <activity
            android:name=".TerminalDetailActivity"
            android:theme="@style/Theme.MonitorDeCuidados"
            android:exported="false" />
```

CAMBIO EXACTO — TerminalDetailActivity queda SIN cambios. Agregar NUEVA activity BubbleRadialActivity DESPUÉS de TerminalDetailActivity:
```xml
        <activity
            android:name=".TerminalDetailActivity"
            android:theme="@style/Theme.MonitorDeCuidados"
            android:exported="false" />

        <activity
            android:name=".BubbleRadialActivity"
            android:theme="@style/Theme.MonitorDeCuidados"
            android:exported="false"
            android:allowEmbedded="true"
            android:resizeableActivity="true"
            android:documentLaunchMode="always" />
```

ARCHIVOS A MODIFICAR:
- NotificationHelper.kt (dentro de createNotificationChannels, bloque alertChannel)
- AndroidManifest.xml (agregar BubbleRadialActivity con flags de burbuja)

NO TOCAR:
- Otros canales (SERVICE_CHANNEL_ID, BATTERY_CHANNEL_ID, etc.)
- TerminalDetailActivity en manifest (NO agregarle flags)
- Otras activities en manifest
- CampanaService
- TerminalDetailActivity.kt

VERIFICACIÓN:
- [ ] Build pasa
- [ ] `grep -r "setAllowBubbles"` → 1 resultado en NotificationHelper.kt
- [ ] `grep -r "allowEmbedded"` → 1 resultado en AndroidManifest.xml (BubbleRadialActivity, NO TerminalDetailActivity)

---

## T79: Crear TerminalBubbleManager.kt — gestor de burbujas per-terminal (CRITICAL)

**Problema**: Necesitamos un componente nuevo que gestione las burbujas de Android (API 30+) por terminal. Cada terminal activo debe tener su propia burbuja flotante que muestre un menú radial (BubbleRadialActivity) cuando se expande.

**ARCHIVO NUEVO**: `app/src/main/java/com/example/monitordecuidados/utils/TerminalBubbleManager.kt`

CREAR ARCHIVO CON ESTE CONTENIDO EXACTO:

```kotlin
package com.example.monitordecuidados.utils

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.example.monitordecuidados.R
import com.example.monitordecuidados.BubbleRadialActivity
import com.example.monitordecuidados.TerminalDetailActivity

/**
 * Manages per-terminal notification bubbles (API 30+).
 * Falls back to standard heads-up on API < 30.
 * Architecture: 1 Monitor : N Terminals → N Bubbles
 */
object TerminalBubbleManager {

    private const val TAG = "TerminalBubbleManager"
    private const val BUBBLE_NOTIFICATION_ID_BASE = 200
    private const val SHORTCUT_CATEGORY = "com.example.monitordecuidados.category.TERMINAL_BUBBLE"

    private val terminalAlertCounts = mutableMapOf<String, Int>()

    fun notifyTerminalAlert(
        context: Context,
        terminalId: String,
        terminalName: String,
        terminalIp: String?,
        alertTitle: String,
        alertMessage: String
    ) {
        val count = (terminalAlertCounts[terminalId] ?: 0) + 1
        terminalAlertCounts[terminalId] = count

        val notificationId = BUBBLE_NOTIFICATION_ID_BASE + terminalId.hashCode().and(0xFF)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            showBubbleNotification(context, nm, notificationId, terminalId, terminalName, alertTitle, alertMessage, count)
        } else {
            showFallbackNotification(context, nm, notificationId, terminalId, terminalName, alertTitle, alertMessage, count)
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun showBubbleNotification(
        context: Context,
        nm: NotificationManager,
        notificationId: Int,
        terminalId: String,
        terminalName: String,
        alertTitle: String,
        alertMessage: String,
        alertCount: Int
    ) {
        val shortcutId = "terminal_$terminalId"
        val person = Person.Builder()
            .setName(terminalName)
            .setKey(terminalId)
            .setImportant(true)
            .build()

        val shortcutIntent = Intent(context, TerminalDetailActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra("terminal_id", terminalId)
            putExtra("terminal_name", terminalName)
            putExtra("terminal_status", "connected")
        }

        val shortcut = ShortcutInfoCompat.Builder(context, shortcutId)
            .setShortLabel(terminalName)
            .setLongLabel("Terminal: $terminalName")
            .setIcon(IconCompat.createWithResource(context, R.drawable.ic_notification_bell))
            .setIntent(shortcutIntent)
            .setLongLived(true)
            .setPerson(person)
            .setCategories(setOf(SHORTCUT_CATEGORY))
            .build()

        ShortcutManagerCompat.pushDynamicShortcut(context, shortcut)

        val bubbleIntent = Intent(context, BubbleRadialActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra("terminal_id", terminalId)
            putExtra("terminal_name", terminalName)
            putExtra("terminal_ip", terminalIp)
            putExtra("alert_title", alertTitle)
            putExtra("alert_count", alertCount)
            putExtra("from_bubble", true)
        }
        val bubblePendingIntent = PendingIntent.getActivity(
            context, notificationId, bubbleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        val bubbleMetadata = NotificationCompat.BubbleMetadata.Builder(
            bubblePendingIntent,
            IconCompat.createWithResource(context, R.drawable.ic_notification_bell)
        )
            .setDesiredHeight(350)
            .setAutoExpandBubble(alertCount == 1)
            .setSuppressNotification(alertCount > 1)
            .build()

        val displayTitle = if (alertCount > 1) "$alertTitle (+${alertCount - 1})" else alertTitle
        val notification = NotificationCompat.Builder(context, NotificationHelper.ALERT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_bell)
            .setContentTitle(displayTitle)
            .setContentText(alertMessage)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setShortcutId(shortcutId)
            .setBubbleMetadata(bubbleMetadata)
            .addPerson(person)
            .setNumber(alertCount)
            .setAutoCancel(false)
            .setOnlyAlertOnce(alertCount > 1)
            .build()

        nm.notify(notificationId, notification)
        Log.d(TAG, "Bubble for '$terminalName' (id=$notificationId, alerts=$alertCount)")
    }

    private fun showFallbackNotification(
        context: Context,
        nm: NotificationManager,
        notificationId: Int,
        terminalId: String,
        terminalName: String,
        alertTitle: String,
        alertMessage: String,
        alertCount: Int
    ) {
        val intent = Intent(context, TerminalDetailActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("terminal_id", terminalId)
            putExtra("terminal_name", terminalName)
            putExtra("terminal_status", "connected")
        }
        val pendingIntent = PendingIntent.getActivity(
            context, notificationId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val displayTitle = if (alertCount > 1) "$alertTitle (+${alertCount - 1})" else alertTitle
        val notification = NotificationCompat.Builder(context, NotificationHelper.ALERT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_bell)
            .setContentTitle(displayTitle)
            .setContentText("$terminalName: $alertMessage")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setNumber(alertCount)
            .setOnlyAlertOnce(alertCount > 1)
            .build()

        nm.notify(notificationId, notification)
        Log.d(TAG, "Fallback for '$terminalName' (id=$notificationId, alerts=$alertCount)")
    }

    fun clearTerminalAlerts(terminalId: String) {
        terminalAlertCounts.remove(terminalId)
    }

    fun clearAllAlerts() {
        terminalAlertCounts.clear()
    }

    fun getAlertCount(terminalId: String): Int {
        return terminalAlertCounts[terminalId] ?: 0
    }
}
```

ARCHIVOS A CREAR:
- `app/src/main/java/com/example/monitordecuidados/utils/TerminalBubbleManager.kt`

NO TOCAR:
- NotificationHelper.kt (se modifica en T77 y T80)
- CampanaService.kt (se modifica en T80)
- TerminalDetailActivity.kt
- AndroidManifest.xml (ya modificado en T78)

VERIFICACIÓN:
- [ ] Build pasa (clase compila sin errores)
- [ ] No hay imports rotos

---

## T79b: Crear BubbleRadialActivity — menú radial animado en burbuja (CRITICAL)

**Problema**: Al expandir la burbuja, el usuario necesita un menú radial con 3 acciones rápidas (Monitorear, Llamar, Controles) en lugar de abrir TerminalDetailActivity directamente. BubbleRadialActivity es una Activity liviana (~120 líneas) que muestra opciones radiales animadas y ejecuta la acción seleccionada en pantalla completa.

**ARCHIVOS NUEVOS (4 archivos)**:

### ARCHIVO 1: `app/src/main/java/com/example/monitordecuidados/BubbleRadialActivity.kt`

CREAR CON ESTE CONTENIDO EXACTO:

```kotlin
package com.example.monitordecuidados

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.appcompat.app.AppCompatActivity
import com.example.monitordecuidados.databinding.ActivityBubbleRadialBinding
import com.example.monitordecuidados.utils.EncryptedPreferencesHelper

class BubbleRadialActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBubbleRadialBinding
    private var terminalId: String? = null
    private var terminalName: String = "Terminal"
    private var terminalIp: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBubbleRadialBinding.inflate(layoutInflater)
        setContentView(binding.root)

        terminalId = intent.getStringExtra("terminal_id")
        terminalName = intent.getStringExtra("terminal_name") ?: "Terminal"
        terminalIp = intent.getStringExtra("terminal_ip")
            ?: EncryptedPreferencesHelper.getString(this, "paired_terminal_ip")
        val alertTitle = intent.getStringExtra("alert_title") ?: ""
        val alertCount = intent.getIntExtra("alert_count", 1)

        // Hub central
        binding.tvBubbleName.text = terminalName
        binding.tvBubbleAlert.text = alertTitle
        if (alertCount > 1) {
            binding.tvBubbleBadge.text = alertCount.toString()
            binding.tvBubbleBadge.visibility = View.VISIBLE
        } else {
            binding.tvBubbleBadge.visibility = View.GONE
        }

        // Click handlers
        binding.btnRadialMonitor.setOnClickListener { launchMonitor() }
        binding.btnRadialCall.setOnClickListener { launchCall() }
        binding.btnRadialControls.setOnClickListener { launchControls() }

        // Animación de expansión radial
        animateExpansion()
    }

    private fun animateExpansion() {
        val hub = binding.hubContainer
        val radialButtons = listOf(binding.btnRadialMonitor, binding.btnRadialCall, binding.btnRadialControls)
        val labels = listOf(binding.tvLabelMonitor, binding.tvLabelCall, binding.tvLabelControls)

        // Hub fade in + scale
        hub.alpha = 0f
        hub.scaleX = 0.8f
        hub.scaleY = 0.8f
        val hubAnim = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(hub, View.ALPHA, 0f, 1f),
                ObjectAnimator.ofFloat(hub, View.SCALE_X, 0.8f, 1f),
                ObjectAnimator.ofFloat(hub, View.SCALE_Y, 0.8f, 1f)
            )
            duration = 200
        }

        // Botones emergen del centro
        val btnAnims = radialButtons.map { btn ->
            btn.alpha = 0f
            btn.scaleX = 0.3f
            btn.scaleY = 0.3f
            AnimatorSet().apply {
                playTogether(
                    ObjectAnimator.ofFloat(btn, View.ALPHA, 0f, 1f),
                    ObjectAnimator.ofFloat(btn, View.SCALE_X, 0.3f, 1f),
                    ObjectAnimator.ofFloat(btn, View.SCALE_Y, 0.3f, 1f)
                )
                duration = 300
                interpolator = OvershootInterpolator(1.2f)
                startDelay = 100
            }
        }

        // Labels fade in
        val lblAnims = labels.map { lbl ->
            lbl.alpha = 0f
            ObjectAnimator.ofFloat(lbl, View.ALPHA, 0f, 1f).apply {
                duration = 100
                startDelay = 400
            }
        }

        AnimatorSet().apply {
            playTogether(listOf(hubAnim) + btnAnims + lblAnims)
            start()
        }
    }

    private fun animateCollapseAndRun(action: () -> Unit) {
        val allViews = listOf(
            binding.btnRadialMonitor, binding.btnRadialCall, binding.btnRadialControls,
            binding.tvLabelMonitor, binding.tvLabelCall, binding.tvLabelControls,
            binding.hubContainer
        )
        val anims = allViews.map { v ->
            AnimatorSet().apply {
                playTogether(
                    ObjectAnimator.ofFloat(v, View.ALPHA, v.alpha, 0f),
                    ObjectAnimator.ofFloat(v, View.SCALE_X, v.scaleX, 0.3f),
                    ObjectAnimator.ofFloat(v, View.SCALE_Y, v.scaleY, 0.3f)
                )
                duration = 300
                interpolator = AccelerateInterpolator()
            }
        }
        AnimatorSet().apply {
            playTogether(anims)
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    action()
                    finish()
                }
            })
            start()
        }
    }

    private fun launchMonitor() {
        animateCollapseAndRun {
            startActivity(Intent(this, VideoActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra("mode", "monitor")
                putExtra("remote_ip", terminalIp)
            })
        }
    }

    private fun launchCall() {
        animateCollapseAndRun {
            startActivity(Intent(this, TerminalDetailActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra("terminal_id", terminalId)
                putExtra("terminal_name", terminalName)
                putExtra("terminal_status", "connected")
                putExtra("auto_call", true)
            })
        }
    }

    private fun launchControls() {
        animateCollapseAndRun {
            startActivity(Intent(this, TerminalDetailActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra("terminal_id", terminalId)
                putExtra("terminal_name", terminalName)
                putExtra("terminal_status", "connected")
            })
        }
    }
}
```

### ARCHIVO 2: `app/src/main/res/layout/activity_bubble_radial.xml`

CREAR CON ESTE CONTENIDO EXACTO:

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/background"
    android:padding="16dp">

    <!-- Punto de anclaje central (invisible) -->
    <View
        android:id="@+id/hubCenter"
        android:layout_width="0dp"
        android:layout_height="0dp"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent" />

    <!-- Hub visual central -->
    <LinearLayout
        android:id="@+id/hubContainer"
        android:layout_width="80dp"
        android:layout_height="80dp"
        android:orientation="vertical"
        android:gravity="center"
        android:background="@drawable/bg_circle_teal"
        android:padding="8dp"
        android:contentDescription="@string/cd_terminal_info_bubble"
        app:layout_constraintCircle="@id/hubCenter"
        app:layout_constraintCircleRadius="0dp"
        app:layout_constraintCircleAngle="0">

        <TextView
            android:id="@+id/tvBubbleName"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:textSize="11sp"
            android:textColor="@color/white"
            android:textStyle="bold"
            android:maxLines="1"
            android:ellipsize="end" />

        <TextView
            android:id="@+id/tvBubbleAlert"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:textSize="9sp"
            android:textColor="@color/white"
            android:maxLines="1"
            android:ellipsize="end" />

        <TextView
            android:id="@+id/tvBubbleBadge"
            android:layout_width="18dp"
            android:layout_height="18dp"
            android:gravity="center"
            android:textSize="10sp"
            android:textColor="@color/white"
            android:background="@drawable/bg_badge_red"
            android:visibility="gone" />
    </LinearLayout>

    <!-- Botón MONITOREAR (arriba, 0° en ConstraintLayout = top) -->
    <LinearLayout
        android:id="@+id/btnRadialMonitor"
        android:layout_width="72dp"
        android:layout_height="72dp"
        android:orientation="vertical"
        android:gravity="center"
        android:clickable="true"
        android:focusable="true"
        android:background="@drawable/bg_circle_teal"
        android:padding="8dp"
        android:contentDescription="@string/cd_monitor_button"
        app:layout_constraintCircle="@id/hubCenter"
        app:layout_constraintCircleRadius="100dp"
        app:layout_constraintCircleAngle="0">

        <ImageView
            android:layout_width="32dp"
            android:layout_height="32dp"
            android:src="@drawable/ic_monitor_video"
            android:contentDescription="@null" />
    </LinearLayout>

    <TextView
        android:id="@+id/tvLabelMonitor"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="@string/monitor_video_button"
        android:textSize="11sp"
        android:textColor="@color/text_primary"
        app:layout_constraintTop_toBottomOf="@id/btnRadialMonitor"
        app:layout_constraintStart_toStartOf="@id/btnRadialMonitor"
        app:layout_constraintEnd_toEndOf="@id/btnRadialMonitor"
        android:layout_marginTop="2dp" />

    <!-- Botón LLAMAR (abajo-izquierda, 240° CL) -->
    <LinearLayout
        android:id="@+id/btnRadialCall"
        android:layout_width="72dp"
        android:layout_height="72dp"
        android:orientation="vertical"
        android:gravity="center"
        android:clickable="true"
        android:focusable="true"
        android:background="@drawable/bg_circle_teal"
        android:padding="8dp"
        android:contentDescription="@string/cd_call_button"
        app:layout_constraintCircle="@id/hubCenter"
        app:layout_constraintCircleRadius="100dp"
        app:layout_constraintCircleAngle="240">

        <ImageView
            android:layout_width="32dp"
            android:layout_height="32dp"
            android:src="@drawable/ic_call_up"
            android:contentDescription="@null" />
    </LinearLayout>

    <TextView
        android:id="@+id/tvLabelCall"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="@string/call_button"
        android:textSize="11sp"
        android:textColor="@color/text_primary"
        app:layout_constraintTop_toBottomOf="@id/btnRadialCall"
        app:layout_constraintStart_toStartOf="@id/btnRadialCall"
        app:layout_constraintEnd_toEndOf="@id/btnRadialCall"
        android:layout_marginTop="2dp" />

    <!-- Botón CONTROLES (abajo-derecha, 120° CL) -->
    <LinearLayout
        android:id="@+id/btnRadialControls"
        android:layout_width="72dp"
        android:layout_height="72dp"
        android:orientation="vertical"
        android:gravity="center"
        android:clickable="true"
        android:focusable="true"
        android:background="@drawable/bg_circle_teal"
        android:padding="8dp"
        android:contentDescription="@string/cd_settings_button"
        app:layout_constraintCircle="@id/hubCenter"
        app:layout_constraintCircleRadius="100dp"
        app:layout_constraintCircleAngle="120">

        <ImageView
            android:layout_width="32dp"
            android:layout_height="32dp"
            android:src="@drawable/ic_settings"
            android:contentDescription="@null" />
    </LinearLayout>

    <TextView
        android:id="@+id/tvLabelControls"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="@string/controls_button"
        android:textSize="11sp"
        android:textColor="@color/text_primary"
        app:layout_constraintTop_toBottomOf="@id/btnRadialControls"
        app:layout_constraintStart_toStartOf="@id/btnRadialControls"
        app:layout_constraintEnd_toEndOf="@id/btnRadialControls"
        android:layout_marginTop="2dp" />

</androidx.constraintlayout.widget.ConstraintLayout>
```

### ARCHIVO 3: `app/src/main/res/drawable/bg_badge_red.xml`

CREAR CON ESTE CONTENIDO EXACTO:

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="oval">
    <solid android:color="#E53935" />
    <size android:width="18dp" android:height="18dp" />
</shape>
```

### ARCHIVO 4: `app/src/main/res/values/strings.xml` — Agregar 2 strings

SNAPSHOT (buscar bloque de content descriptions, ~línea 152):
```xml
    <string name="cd_settings_button">Abrir configuración</string>
```

CAMBIO EXACTO — Agregar DESPUÉS de `cd_settings_button`:
```xml
    <string name="cd_settings_button">Abrir configuración</string>
    <string name="controls_button">Controles</string>
    <string name="cd_terminal_info_bubble">Información del terminal</string>
```

ARCHIVOS A CREAR:
- `app/src/main/java/com/example/monitordecuidados/BubbleRadialActivity.kt`
- `app/src/main/res/layout/activity_bubble_radial.xml`
- `app/src/main/res/drawable/bg_badge_red.xml`

ARCHIVOS A MODIFICAR:
- `app/src/main/res/values/strings.xml` (agregar 2 strings)

NO TOCAR:
- TerminalBubbleManager.kt (creado en T79)
- NotificationHelper.kt (se modifica en T80)
- TerminalDetailActivity.kt (se modifica en T80)
- activity_terminal_detail.xml
- Ningún otro layout o drawable existente

---

## T86: Fix video monitoring — streaming directo desde CampanaService (CRITICAL)

**Problema**: Cuando el Monitor solicita monitoreo silencioso, `CampanaService.onMonitorRequested()` llama `startActivity(VideoActivity)` en el Terminal. En Android 12+ (API 31+), lanzar Activities desde un foreground service en background está restringido — se bloquea silenciosamente a menos que `SYSTEM_ALERT_WINDOW` esté habilitado manualmente por el usuario. Resultado: la VideoActivity NUNCA arranca en el Terminal → la cámara nunca se abre → no hay frames UDP → Monitor ve pantalla negra.

**Decisión arquitectónica**: Para monitoreo silencioso, el Terminal NO necesita UI. La cámara debe controlarse directamente desde CampanaService (que ya tiene `foregroundServiceType="microphone|camera|dataSync"` en el manifiesto). Para videollamada (donde SÍ se necesita UI bidireccional), se sigue usando VideoActivity.

### ARCHIVO 1: `app/src/main/java/com/example/monitordecuidados/CampanaService.kt`

#### Cambio 1: Agregar import de VideoManager

SNAPSHOT (buscar bloque de imports, después de CallManager):
```kotlin
import com.example.monitordecuidados.communication.CallManager
import com.example.monitordecuidados.communication.VoiceCommandManager
```

CAMBIO EXACTO:
```kotlin
import com.example.monitordecuidados.communication.CallManager
import com.example.monitordecuidados.communication.VideoManager
import com.example.monitordecuidados.communication.VoiceCommandManager
```

#### Cambio 2: Agregar propiedad videoManager al servicio

SNAPSHOT (buscar variable callManager existente):
```kotlin
    private var callManager: CallManager? = null
```

CAMBIO EXACTO — Agregar videoManager DESPUÉS de callManager:
```kotlin
    private var callManager: CallManager? = null
    private var videoManager: VideoManager? = null
```

#### Cambio 3: Reescribir onMonitorRequested — streaming directo sin Activity

SNAPSHOT (estado actual):
```kotlin
        // T59v2: Silent monitor request with auto-accept and logging
        override fun onMonitorRequested(sourceIp: String) {
            FileLogger.logInfo(TAG, "onMonitorRequested: sourceIp=$sourceIp — launching VideoActivity")
            val intent = Intent(this@CampanaService, VideoActivity::class.java).apply {
                putExtra("mode", "monitor")
                putExtra("remote_ip", sourceIp)
                putExtra("auto_accept", true)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        }
```

CAMBIO EXACTO:
```kotlin
        // T86: Silent monitor — stream directly from Service (no Activity launch needed).
        // Avoids Android 12+ background activity start restriction.
        override fun onMonitorRequested(sourceIp: String) {
            FileLogger.logInfo(TAG, "T86: onMonitorRequested from $sourceIp — starting direct video streaming")
            videoManager?.stopStreaming()
            videoManager = VideoManager(this@CampanaService, sourceIp, 9001)
            videoManager?.startStreaming()
            isMonitoringActive = true
            Log.d(TAG, "T86: VideoManager streaming to $sourceIp:9001")
        }
```

#### Cambio 4: Agregar flag isMonitoringActive

SNAPSHOT (buscar isAudioCallActive):
```kotlin
    private var isAudioCallActive = false
```

CAMBIO EXACTO:
```kotlin
    private var isAudioCallActive = false
    private var isMonitoringActive = false
```

#### Cambio 5: Agregar comando STOP_MONITOR para detener streaming

SNAPSHOT (buscar el comando STOP_AUDIO_CALL existente en el bloque when de onCommandReceived):
```kotlin
                // T58: Stop audio call command
                "STOP_AUDIO_CALL" -> {
                    callManager?.stopCall()
                    callManager = null
```

CAMBIO EXACTO — Agregar STOP_MONITOR y STOP_SESSION ANTES del bloque STOP_AUDIO_CALL:
```kotlin
                // T86: Stop video monitoring from service
                "STOP_MONITOR", "STOP_SESSION" -> {
                    videoManager?.stopStreaming()
                    videoManager = null
                    isMonitoringActive = false
                    Log.d(TAG, "T86: Video monitoring stopped via command $command")
                }
                // T58: Stop audio call command
                "STOP_AUDIO_CALL" -> {
                    callManager?.stopCall()
                    callManager = null
```

#### Cambio 6: Cleanup en onDestroy

SNAPSHOT (buscar onDestroy del servicio):
```kotlin
    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
```

CAMBIO EXACTO — Agregar cleanup de videoManager:
```kotlin
    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        // T86: Cleanup video streaming
        videoManager?.stopStreaming()
        videoManager = null
```

### ARCHIVO 2: `app/src/main/java/com/example/monitordecuidados/VideoActivity.kt`

#### Cambio: Monitor side — enviar STOP_SESSION/STOP_MONITOR al colgar

SNAPSHOT (estado actual de hangUpAll):
```kotlin
    private fun hangUpAll() {
        if (currentRol == "monitor") {
            sendCommandToTerminal("STOP_SESSION")
            if (isAudioOnly) sendCommandToTerminal("STOP_AUDIO_CALL")
        }
        callViewModel.endCall("user_hung_up")
    }
```

CAMBIO EXACTO:
```kotlin
    private fun hangUpAll() {
        if (currentRol == "monitor") {
            sendCommandToTerminal("STOP_SESSION")
            if (isAudioOnly) sendCommandToTerminal("STOP_AUDIO_CALL")
            sendCommandToTerminal("STOP_MONITOR")
        }
        callViewModel.endCall("user_hung_up")
    }
```

ARCHIVOS A MODIFICAR:
- CampanaService.kt (6 cambios: import, propiedad videoManager, onMonitorRequested rewrite, isMonitoringActive flag, STOP_MONITOR command, onDestroy cleanup)
- VideoActivity.kt (1 cambio: hangUpAll envía STOP_MONITOR)

NO TOCAR:
- VideoManager.kt (ya funciona correctamente para streaming)
- CampanaHttpServer.kt (el endpoint /request_monitor ya llama onMonitorRequested correctamente)
- AndroidManifest.xml (ya tiene foregroundServiceType="microphone|camera|dataSync")
- BellActivity.kt
- BubbleRadialActivity.kt

VERIFICACIÓN:
- [ ] Build pasa
- [ ] Monitor toca "Monitorear" → VideoActivity se abre en Monitor con "Monitoreo Silencioso"
- [ ] Terminal NO abre ninguna Activity (la pantalla del Terminal NO cambia)
- [ ] Monitor recibe video en tiempo real (NO pantalla negra)
- [ ] Logcat Terminal muestra "T86: VideoManager streaming to X.X.X.X:9001"
- [ ] Monitor toca "Colgar" → Terminal recibe STOP_MONITOR → streaming se detiene
- [ ] Logcat Terminal muestra "T86: Video monitoring stopped"
- [ ] Segunda solicitud de monitoreo funciona (no hay conflicto de socket/cámara)

---

## T87: Fix llamada de voz — socket bind, puerto, y robustez OPUS (CRITICAL)

**Problema 1 — Socket bind**: `CallManager.startCall()` crea `DatagramSocket(port)` que hace bind INMEDIATO al puerto. Luego asigna `reuseAddress = true` DESPUÉS del bind → no surte efecto. Si una llamada anterior dejó el puerto en TIME_WAIT (típico timeout ~60s), el bind falla con `BindException` y la llamada muere silenciosamente.

**Problema 2 — Puerto SIP**: Puerto 5060 es estándar SIP. Algunos carriers interceptan o bloquean tráfico en este puerto. Cambiar a 9050 (no estándar) evita conflictos.

**Problema 3 — OPUS**: `MediaCodec.createEncoderByType(MIMETYPE_AUDIO_OPUS)` puede no estar disponible en todos los dispositivos. Si falla, `sendAudio()` muere silenciosamente. Se necesita un log explícito para diagnóstico.

### ARCHIVO 1: `app/src/main/java/com/example/monitordecuidados/communication/CallManager.kt`

#### Cambio 1: Fix socket bind — crear socket SIN bind, luego reuseAddress, luego bind

SNAPSHOT (estado actual en startCall):
```kotlin
                socket?.close()
                socket = DatagramSocket(port).apply {
                    receiveBufferSize = 1024 * 64
                    reuseAddress = true
                    soTimeout = SOCKET_TIMEOUT
                }
```

CAMBIO EXACTO:
```kotlin
                socket?.close()
                // T87: Create unbound socket, set reuseAddress BEFORE binding.
                // DatagramSocket(port) binds immediately — reuseAddress set after has no effect.
                socket = DatagramSocket(null).apply {
                    reuseAddress = true
                    receiveBufferSize = 1024 * 64
                    soTimeout = SOCKET_TIMEOUT
                    bind(java.net.InetSocketAddress(port))
                }
```

#### Cambio 2: Log OPUS encoder creation failure

SNAPSHOT (estado actual en sendAudio — bloque del encoder):
```kotlin
            // T47: OPUS Encoder setup
            val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_OPUS, SAMPLE_RATE, 1)
            format.setInteger(MediaFormat.KEY_BIT_RATE, 16000)
            encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_OPUS)
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            encoder.start()
```

CAMBIO EXACTO:
```kotlin
            // T47/T87: OPUS Encoder setup — log explicit error if codec unavailable
            val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_OPUS, SAMPLE_RATE, 1)
            format.setInteger(MediaFormat.KEY_BIT_RATE, 16000)
            try {
                encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_OPUS)
                encoder!!.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                encoder!!.start()
            } catch (e: Exception) {
                FileLogger.logCritical(TAG, "T87: OPUS encoder NOT available on this device. Call audio will not work.", e)
                Log.e(TAG, "T87: OPUS encoder creation failed — codec not supported?", e)
                return
            }
```

### ARCHIVO 2: `app/src/main/java/com/example/monitordecuidados/CampanaService.kt`

#### Cambio: Actualizar puerto de CallManager de 5060 a 9050

SNAPSHOT (estado actual en onCallRequested):
```kotlin
        override fun onCallRequested(sourceIp: String) {
            FileLogger.logInfo(TAG, "onCallRequested from $sourceIp — starting walkie-talkie")
            callManager?.stopCall()
            callManager = CallManager(sourceIp, 5060)
            callManager?.startCall()
```

CAMBIO EXACTO:
```kotlin
        override fun onCallRequested(sourceIp: String) {
            FileLogger.logInfo(TAG, "onCallRequested from $sourceIp — starting walkie-talkie")
            callManager?.stopCall()
            callManager = CallManager(sourceIp, 9050)
            callManager?.startCall()
```

### ARCHIVO 3: `app/src/main/java/com/example/monitordecuidados/TerminalDetailActivity.kt`

#### Cambio: Actualizar puerto de CallManager de 5060 a 9050

SNAPSHOT (estado actual — Monitor side):
```kotlin
                callManager = CallManager(ip, 5060)
                callManager?.startCall()
```

CAMBIO EXACTO:
```kotlin
                callManager = CallManager(ip, 9050)
                callManager?.startCall()
```

ARCHIVOS A MODIFICAR:
- CallManager.kt (2 cambios: socket bind fix, OPUS try-catch)
- CampanaService.kt (1 cambio: puerto 5060→9050)
- TerminalDetailActivity.kt (1 cambio: puerto 5060→9050)

NO TOCAR:
- VideoActivity.kt (usa puerto 9000 para llamadas en modo videocall — flujo diferente)
- CampanaHttpServer.kt
- NetworkUtils.kt

VERIFICACIÓN:
- [ ] Build pasa
- [ ] Monitor toca "Llamar" en TerminalDetailActivity → audio fluye en ambas direcciones
- [ ] Logcat NO muestra "BindException" en CallManager
- [ ] Logcat Monitor muestra "Llamada iniciada hacia X.X.X.X:9050"
- [ ] Logcat Terminal muestra "onCallRequested from X.X.X.X — starting walkie-talkie"
- [ ] Si OPUS no disponible: Logcat muestra "T87: OPUS encoder NOT available" (no crash)
- [ ] Segunda llamada consecutiva funciona (reuseAddress permite re-bind)
- [ ] Colgar desde Monitor → Terminal limpia CallManager

---

## 📋 ORDEN DE EJECUCIÓN v22.0

**Ejecutar en este orden estricto:**

1. **T86** — Video monitoring fix (CampanaService + VideoActivity)
2. **T87** — Voice call fix (CallManager + CampanaService + TerminalDetailActivity)
3. **T83b** — Notificación extra + burbujas (TerminalBubbleManager + NotificationHelper)
4. **T84b** — Layout alert card (item_alert_campana.xml)
5. Build y verificar que compila sin errores
6. T72, T73 — Features pendientes (baja prioridad)

**IMPORTANTE**:
- T86 y T87 son independientes entre sí — pueden ejecutarse en cualquier orden
- T83b y T84b son independientes entre sí
- Todos los T86/T87/T83b/T84b son independientes — no hay dependencias cruzadas
- **Desinstalar la app completamente del dispositivo de prueba antes de T83b** (para limpiar canal de notificaciones cacheado)

CONSERVAR:
- bg_circle_teal.xml (se reutiliza, NO modificar)
- ic_monitor_video.xml (se reutiliza)
- ic_call_up.xml (se reutiliza)
- ic_settings.xml (se reutiliza)
- Todos los strings existentes (solo agregar nuevos)

VERIFICACIÓN:
- [ ] Build pasa
- [ ] activity_bubble_radial.xml preview muestra 3 botones en disposición circular
- [ ] ViewBinding genera `ActivityBubbleRadialBinding` sin errores
- [ ] `grep -r "BubbleRadialActivity"` → 1 en manifest, 1 en BubbleRadialActivity.kt, 1 en TerminalBubbleManager.kt

---

## T80: Integrar BubbleManager en el flujo de alertas + auto_call en TerminalDetailActivity (CRITICAL)

**Problema**: Ahora que TerminalBubbleManager existe (T79) y BubbleRadialActivity (T79b), necesitamos: (1) conectar BubbleManager al flujo de alertas delegando desde NotificationHelper, (2) agregar manejo de `auto_call=true` en TerminalDetailActivity para que la opción "Llamar" del menú radial inicie la llamada automáticamente al abrir.

**ARCHIVO 1**: `app/src/main/java/com/example/monitordecuidados/utils/NotificationHelper.kt`

SNAPSHOT (estado actual del bloque heads-up, DESPUÉS de T77 aplicado):
```kotlin
        // Only show visible heads-up if we are in Monitor role
        if (CampanaService.userRole != CampanaService.ROLE_MONITOR) return

        val intent = Intent(context, MonitorMainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, ALERT_HEADS_UP_ID, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alertCount = alertHistory.size
        val notification = NotificationCompat.Builder(context, ALERT_CHANNEL_ID)
            ...
        nm.notify(ALERT_HEADS_UP_ID, notification)
```

CAMBIO EXACTO — Reemplazar TODO el bloque desde `// Only show visible` hasta `nm.notify(ALERT_HEADS_UP_ID, notification)` (inclusive) por:

```kotlin
        // Only show visible notification if we are in Monitor role
        if (CampanaService.userRole != CampanaService.ROLE_MONITOR) return

        // T80: Delegate to BubbleManager — per-terminal bubbles on API 30+, fallback on older
        val resolvedTerminalName = resolveTerminalName(context, terminalName)
        val resolvedTerminalId = terminalName.ifBlank { "default_terminal" }
        TerminalBubbleManager.notifyTerminalAlert(
            context = context,
            terminalId = resolvedTerminalId,
            terminalName = resolvedTerminalName,
            terminalIp = terminalName.takeIf { it.matches(Regex("\\d+\\.\\d+\\.\\d+\\.\\d+")) },
            alertTitle = title,
            alertMessage = message
        )
```

TAMBIÉN agregar esta función helper DENTRO del `object NotificationHelper`, ANTES del cierre `}`:
```kotlin
    private fun resolveTerminalName(context: Context, terminalNameOrIp: String): String {
        if (terminalNameOrIp.isBlank()) return "Terminal"
        if (terminalNameOrIp.matches(Regex("\\d+\\.\\d+\\.\\d+\\.\\d+"))) {
            val prefs = context.getSharedPreferences("monitordecuidados_prefs", Context.MODE_PRIVATE)
            val savedName = prefs.getString("paired_terminal_name", null)
            return savedName ?: "Terminal ($terminalNameOrIp)"
        }
        return terminalNameOrIp
    }
```

NOTA: T80 reemplaza el bloque de heads-up de T77. La constante `ALERT_HEADS_UP_ID` y algunos imports quedan sin usarse en notifyAlert. Eliminar `ALERT_HEADS_UP_ID` SOLO SI no se usa en ningún otro lugar.

**ARCHIVO 2**: `app/src/main/java/com/example/monitordecuidados/CampanaService.kt`

SNAPSHOT (líneas ~94-97):
```kotlin
        override fun onBellTriggered(sourceIp: String) {
            NotificationHelper.notifyAlert(this@CampanaService, "🔔 Campana", "Se ha pedido ayuda desde Terminal", "bell", sourceIp)
            // T69: DashboardFragment LOCAL-FIRST — leer alertas de Room DB
            saveEventToLocalDb("bell", "Se ha pedido ayuda desde Terminal", sourceIp)
```

CAMBIO EXACTO:
```kotlin
        override fun onBellTriggered(sourceIp: String) {
            // T80: Guardar terminal name para resolver nombre en burbujas
            val prefs = getSharedPreferences("monitordecuidados_prefs", MODE_PRIVATE)
            if (prefs.getString("paired_terminal_name", null) == null) {
                prefs.edit().putString("paired_terminal_name", android.os.Build.MODEL).apply()
            }
            NotificationHelper.notifyAlert(this@CampanaService, "🔔 Campana", "Se ha pedido ayuda desde Terminal", "bell", sourceIp)
            // T69: DashboardFragment LOCAL-FIRST — leer alertas de Room DB
            saveEventToLocalDb("bell", "Se ha pedido ayuda desde Terminal", sourceIp)
```

**ARCHIVO 3**: `app/src/main/java/com/example/monitordecuidados/TerminalDetailActivity.kt`

SNAPSHOT (líneas ~64-66, dentro de onCreate, ANTES de btnCall.setOnClickListener):
```kotlin
        // Card principal — nombre + status
        binding.tvTerminalName.text = terminalName
        updateStatusText(terminalStatus)

        // T45: Walkie-talkie in-place
        binding.btnCall.setOnClickListener {
```

CAMBIO EXACTO — Agregar bloque auto_call ENTRE `updateStatusText` y `binding.btnCall.setOnClickListener`:
```kotlin
        // Card principal — nombre + status
        binding.tvTerminalName.text = terminalName
        updateStatusText(terminalStatus)

        // T80: Auto-call cuando viene desde menú radial de burbuja
        val autoCall = intent.getBooleanExtra("auto_call", false)

        // T45: Walkie-talkie in-place
        binding.btnCall.setOnClickListener {
```

TAMBIÉN agregar DESPUÉS de configurar TODOS los listeners (al FINAL de onCreate, antes del cierre `}`):
```kotlin
        // T80: Si auto_call=true, simular tap en Llamar
        if (autoCall) {
            binding.btnCall.post { binding.btnCall.performClick() }
        }
```

ARCHIVOS A MODIFICAR:
- NotificationHelper.kt (bloque heads-up → BubbleManager delegation + resolveTerminalName)
- CampanaService.kt (onBellTriggered — guardar paired_terminal_name)
- TerminalDetailActivity.kt (agregar manejo de auto_call en onCreate)

NO TOCAR:
- TerminalBubbleManager.kt (creado en T79)
- BubbleRadialActivity.kt (creado en T79b)
- DashboardFragment.kt
- CampanaHttpServer.kt
- VideoActivity.kt
- AndroidManifest.xml (ya modificado en T78)
- notification_custom.xml
- Service notification (ID=1, createServiceNotification)
- activity_terminal_detail.xml (NO cambiar layout)

VERIFICACIÓN:
- [ ] Build pasa
- [ ] Terminal toca campana → Monitor muestra BURBUJA flotante (API 30+)
- [ ] Expandir burbuja → **BubbleRadialActivity** con menú radial (3 opciones)
- [ ] Tocar "Monitorear" → VideoActivity se abre en pantalla completa
- [ ] Tocar "Llamar" → TerminalDetailActivity se abre Y inicia llamada automáticamente (auto_call)
- [ ] Tocar "Controles" → TerminalDetailActivity se abre con panel completo de switches
- [ ] Segunda alerta → burbuja se actualiza, NO crea nueva
- [ ] API < 30 → heads-up con ID fijo por terminal → tap → TerminalDetailActivity directo (sin radial)
- [ ] Service notification (ID=1) sigue mostrando historial sin cambios

---

## T81: Fix VideoActivity — CallManager/VideoManager init después de permisos en Terminal mode (HIGH)

**Problema**: En VideoActivity modo terminal, `setupUI()` crea `videoManager` y `callManager` SOLO si `remoteIp != null`. Si `remoteIp` es null al momento de `setupUI()`, ambos managers quedan null. `onRequestPermissionsResult()` (T65) intenta `videoManager?.startStreaming()` pero videoManager es null → streaming nunca se inicia.

**Fix**: En `onRequestPermissionsResult()`, si `videoManager` o `callManager` son null, intentar crearlos con la IP disponible.

**ARCHIVO**: `app/src/main/java/com/example/monitordecuidados/VideoActivity.kt`

SNAPSHOT (líneas ~390, dentro de onRequestPermissionsResult, ramal terminal):
```kotlin
                } else {
                    // Terminal side — reiniciar streaming
                    if (intent.getBooleanExtra("auto_accept", false)) {
                        if (isAudioOnly) {
                            startAudioMode()
                        } else {
```

CAMBIO EXACTO — Agregar bloque de null-check ANTES del if:
```kotlin
                } else {
                    // Terminal side — reiniciar streaming
                    // T81: Asegurar que videoManager/callManager existen post-permisos
                    if (remoteIp == null) {
                        remoteIp = EncryptedPreferencesHelper.getString(this@VideoActivity, "paired_terminal_ip")
                            ?: getSharedPreferences("monitordecuidados_prefs", MODE_PRIVATE)
                                .getString("paired_monitor_ip", null)
                    }
                    if (remoteIp != null) {
                        if (videoManager == null) videoManager = VideoManager(this@VideoActivity, remoteIp!!, 9001)
                        if (callManager == null) callManager = CallManager(remoteIp!!, 9000)
                    } else {
                        Log.e(TAG, "T81: remoteIp STILL null after permission grant — cannot start streaming")
                    }
                    if (intent.getBooleanExtra("auto_accept", false)) {
                        if (isAudioOnly) {
                            startAudioMode()
                        } else {
```

ARCHIVOS A MODIFICAR:
- VideoActivity.kt (~línea 390, dentro de onRequestPermissionsResult)

NO TOCAR:
- CallManager.kt (se modifica en T82)
- VideoManager.kt
- CampanaService.kt
- setupUI(), validatePermissions(), startReceivingVideo()

VERIFICACIÓN:
- [ ] Build pasa
- [ ] Terminal recibe request_monitor → VideoActivity abre → permisos → usuario otorga → video transmite
- [ ] Monitor recibe video (imagen visible, no pantalla negra)
- [ ] Logcat: "T81: remoteIp STILL null" si IP no disponible

---

## T82: Fix CallManager — log explícito si RECORD_AUDIO falta (HIGH)

**Problema**: `CallManager.sendAudio()` usa `@SuppressLint("MissingPermission")` para AudioRecord. Si RECORD_AUDIO no otorgado, AudioRecord queda STATE_UNINITIALIZED y retorna silenciosamente.

**Fix**: Agregar log explícito con tag identificable.

**ARCHIVO**: `app/src/main/java/com/example/monitordecuidados/communication/CallManager.kt`

SNAPSHOT (líneas ~130-134):
```kotlin
            if (recorder.state != AudioRecord.STATE_INITIALIZED) {
                FileLogger.logCritical(TAG, "AudioRecord no se pudo inicializar")
                return
            }
```

CAMBIO EXACTO:
```kotlin
            if (recorder.state != AudioRecord.STATE_INITIALIZED) {
                FileLogger.logCritical(TAG, "T82: AudioRecord no inicializado — ¿RECORD_AUDIO otorgado?")
                Log.e(TAG, "T82: AudioRecord STATE_UNINITIALIZED. Permission RECORD_AUDIO may be missing.")
                return
            }
```

ARCHIVOS A MODIFICAR:
- CallManager.kt (línea ~131)

NO TOCAR:
- VideoActivity.kt
- receiveAudio(), startCall(), stopCall()
- OPUS encoder/decoder setup

VERIFICACIÓN:
- [ ] Build pasa
- [ ] Sin RECORD_AUDIO → Logcat muestra "T82: AudioRecord STATE_UNINITIALIZED"
- [ ] Con RECORD_AUDIO → audio se transmite correctamente

---

## T72: Alarm bidirectional Firestore real-time listener (MEDIUM)

(Mantener para futuro sprint — sin cambios respecto a v19.0)

---

## T73: CapabilitiesAssessment datos mostrados al Monitor en scan QR (MEDIUM)

(Mantener para futuro sprint — sin cambios respecto a v19.0)

---

## 📋 ORDEN DE EJECUCIÓN v22.0

```
1. T83b (fix notificación extra + canal burbujas) — PRIMERO, porque es el más visible y crítico
2. T84b (fix layout alert card) — SEGUNDO, independiente, solo XML
```

> ⚠️ T83b toca 2 archivos: NotificationHelper.kt (canal) + TerminalBubbleManager.kt (guard + suppress).
> T84b solo toca item_alert_campana.xml. Son completamente independientes.
>
> 🚨 IMPORTANTE: Después de aplicar T83b, el usuario DEBE desinstalar la app y reinstalar (o borrar datos) para limpiar el canal cacheado. Alternativa: ir a Ajustes → Apps → Monitor de Cuidados → Notificaciones → verificar que "Alertas de Cuidados" permita burbujas.

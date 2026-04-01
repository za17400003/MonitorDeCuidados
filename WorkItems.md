# WORKITEMS v12.9 — TAREAS PENDIENTES (Abril 1, 2026)

> **ACTUALIZADO**: Abril 1, 2026
> **ESTADO**: T14-T42 código ✅ build ✅. T43 pendiente código. T29-T30, T35, T38-T42 dispositivo pendiente.

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

## TAREAS PENDIENTES

### T43: Monitor no refleja cambios de switches hechos en Terminal

---

## T43: TerminalDetailActivity — Polling periódico de /status para sincronizar switches

**Problema**: Cuando el usuario del Terminal cambia un switch localmente (ej: desactiva campanilla desde el propio Terminal), el Monitor (`TerminalDetailActivity`) no se entera porque `queryTerminalStatus()` solo se llama UNA vez en `onCreate`. No hay polling ni mecanismo push.

**Causa raíz**: `TerminalDetailActivity` no tiene `onResume`/`onPause`. No hay ciclo de polling. El estado se lee una vez al abrir la pantalla y nunca más.

**SNAPSHOT actual del archivo**:
- Archivo: `app/src/main/java/com/example/monitordecuidados/TerminalDetailActivity.kt`
- Línea 24: `private var isUpdatingFromServer = false`
- Línea 80: Fin de `onCreate()` → `queryTerminalStatus()`
- Líneas 92-131: `queryTerminalStatus()` → GET /status → runOnUiThread → setea switches con `isUpdatingFromServer` flag
- NO hay `onResume`, NO hay `onPause`, NO hay Handler/timer

### FIX: Añadir polling con Handler cada 5 segundos en onResume/onPause

**Paso 1**: Añadir variable Handler. DESPUÉS de `private var isUpdatingFromServer = false` (línea 24), añadir:

```kotlin
    private val pollHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val pollRunnable = object : Runnable {
        override fun run() {
            queryTerminalStatus()
            pollHandler.postDelayed(this, 5000)
        }
    }
```

**Paso 2**: Añadir `onResume` que inicia el polling. DESPUÉS de `onCreate()` (después de la llave `}` de cierre de onCreate), añadir:

```kotlin
    override fun onResume() {
        super.onResume()
        pollHandler.post(pollRunnable)
    }
```

**Paso 3**: Añadir `onPause` que detiene el polling. DESPUÉS de `onResume()`, añadir:

```kotlin
    override fun onPause() {
        super.onPause()
        pollHandler.removeCallbacks(pollRunnable)
    }
```

**Paso 4**: ELIMINAR la llamada `queryTerminalStatus()` al final de `onCreate` (ya no se necesita, `onResume` la ejecuta inmediatamente).

De:
```kotlin
        // T39-C: Consultar estado real del terminal
        queryTerminalStatus()
    }
```

A:
```kotlin
        // T39-C: Polling de estado real arranca en onResume()
    }
```

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

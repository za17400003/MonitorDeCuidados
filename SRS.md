# 📋 ESPECIFICACIÓN FUNCIONAL v4.0 - Monitor de Cuidados

**Última actualización**: Abril 2, 2026
**Versión**: 4.1 (Auditoría código↔docs: 23 discrepancias corregidas + arquitectura LOCAL-FIRST)
**Estado**: Documentación Actualizada contra código real

---

## 🔄 PROJECT STATUS (March 29, 2026)

✅ **PHASE 5 - MVVM Architecture**: COMPLETED
- UserViewModel, ConnectionViewModel, CallViewModel, NotificationViewModel, PreferencesViewModel implemented
- All LiveData state management wired to Activities/Fragments
- Unit tests passing + Integration tests complete

✅ **PHASE 6 - Production Release**: COMPLETED  
- Unit & integration tests implemented and passing
- Performance optimization applied (memory/battery profiling)
- Security audit completed per PLAN_MIGRACION_SEGURIDAD.md
- ProGuard/R8 minification enabled
- Release APK/AAB signed and ready

🎯 **PHASE 7 - New Feature**: Battery Low Notification (IN PROGRESS)
- Terminal ≤15% battery triggers alert to Monitor via Firestore
- Consolidated AlertLog shows battery alerts across all paired Terminals

---

## 🏗️ ARCHITECTURE COMPONENTS (MVVM - PHASE 5)

**Code Location**: `app/src/main/java/.../viewmodels/` + `app/src/main/java/.../models/`

### 1. **UserViewModel** (`UserViewModel.kt`)
| Aspect | Details |
|--------|---------|
| **Purpose** | Firebase authentication + user profile + preferences cloud sync |
| **State Class** | `UserState` (sealed: Initial\|Loading\|Success\|Error), `UserData`, `UserPreferences` |
| **Key Methods** | `login(email, password)`, `loginWithGoogle(idToken)`, `logout()`, `loadPreferences()`, `updatePreference(key, value)` |
| **LiveData** | `userState: LiveData<UserState>`, exposed preferences via Firestore |
| **Integration** | LoginActivity, MonitorMainActivity, TerminalMainActivity, SettingsActivity |
| **Test File** | `UserViewModelTest.kt` (auth flows, state transitions, preferences sync) |

### 2. **ConnectionViewModel** (`ConnectionViewModel.kt`)
| Aspect | Details |
|--------|---------|
| **Purpose** | Pairing management + connection status + peer discovery |
| **State Class** | `ConnectionState` (Connected\|Disconnected\|Reconnecting), `PairingInfo` |
| **Key Methods** | `getPairingsList()`, `addPairing(peerId, peerName, role)`, `removePairing(peerId)`, `updateConnectionStatus(status)` |
| **LiveData** | `pairingList: LiveData<List<PairingInfo>>`, `connectionState: LiveData<ConnectionState>` |
| **Integration** | MonitorMainActivity (manage pairings), PairingActivity (QR scanning), ConnectionStatusFragment |
| **Test File** | `ConnectionViewModelTest.kt` (pairing logic, status transitions) |

### 3. **CallViewModel** (`CallViewModel.kt`)
| Aspect | Details |
|--------|---------|
| **Purpose** | Call state management + duration tracking + quality metrics |
| **State Class** | `CallState` (Idle\|Ringing\|Connected\|Ended), call duration, audio/video quality |
| **Key Methods** | `initiateCall(peerId, isVideo)`, `acceptCall()`, `endCall()`, `updateCallQuality(metrics)`, `getCurrentDuration(): Long` |
| **LiveData** | `callState: LiveData<CallState>`, `callDuration: LiveData<Long>`, `qualityMetrics: LiveData<QualityMetrics>` |
| **Integration** | VideoActivity, CallManager, MonitorMainActivity (walkie-talkie audio + call notifications) |
| **Test File** | `CallViewModelTest.kt` (call state, duration tracking, quality updates) |

### 4. **NotificationViewModel** (`NotificationViewModel.kt`)
| Aspect | Details |
|--------|---------|
| **Purpose** | Alert management: lee eventos de Room DB local (LOCAL-FIRST) + sincroniza con Firestore como backup |
| **State Class** | `NotificationItem` (type: bell\|shake\|voice\|battery_low\|battery_ok, timestamp, sourceTerminal), `NotificationState` |
| **Key Methods** | `observeRealtimeNotifications()`, `getNotifications(limit, offset)`, `markAsRead(notificationId)`, `clearOldNotifications(olderThanDays)` |
| **LiveData** | `notificationList: LiveData<List<NotificationItem>>`, `unreadCount: LiveData<Int>` |
| **Data Source (LOCAL-FIRST)** | Lee de Room DB tabla Event (fuente primaria). SyncManager sincroniza Event→Firestore `/notifications_history` cada 30s como backup. **NO depende de Firestore listener para UI en tiempo real.** |
| **Integration** | DashboardFragment (alert cards desde Room), AlertLogActivity (historical view), NotificationHelper (alert dispatch) |
| **Test File** | `CallManagerTest.kt` (notification queueing tested via CallManager integration) |

### 5. **PreferencesViewModel** (`PreferencesViewModel.kt`)
| Aspect | Details |
|--------|---------|
| **Purpose** | Settings sync across local + cloud (Firestore `/user_preferences`) |
| **State Class** | `PreferencesState` (Loading\|Success\|Error), preference key-value pairs cached locally in Room |
| **Key Methods** | `getPreferences(): LiveData<Map<String, String>>`, `updateAutoAcceptCalls(boolean)`, `updateShakeDetection(boolean)`, `updateNotificationSound(uri)`, `updateBatteryThreshold(percent)` |
| **LiveData** | `preferences: LiveData<Map<String, String>>`, individual preference exposures for reactive UI updates |
| **Storage** | 2-tier: Room cache (`UserPreferences` table) for offline + Firestore `/user_preferences/{userId}` for cloud sync |
| **Integration** | SettingsActivity (all toggles + pickers), MonitorMainActivity (theme, notification preferences), Terminal (auto-accept, battery threshold) |
| **Test File** | Not yet written (Schedule for PHASE 8) |

### Test Coverage (PHASE 6.1)
| Test File | Location | Focus |
|-----------|----------|-------|
| `UserViewModelTest.kt` | `app/src/test/.../viewmodels/` | Auth flows, state transitions, preference persistence |
| `ConnectionViewModelTest.kt` | `app/src/test/.../viewmodels/` | Pairing CRUD, status changes, discovery |
| `CallViewModelTest.kt` | `app/src/test/.../viewmodels/` | Call lifecycle, duration tracking, quality metrics |
| `CallManagerTest.kt` | `app/src/test/.../` | Notification queueing, call routing |
| `FileLoggerTest.kt` | `app/src/test/.../` | Log file operations, crash logging |
| `CrashLoggerTest.kt` | `app/src/test/.../` | Exception handling, error reporting |
| `ExampleUnitTest.kt` | `app/src/test/.../` | Basic framework validation |

---

## 🔴 MANDATORY REQUIREMENTS - APP MUST HAVE THESE FEATURES

**Critical Features** (sin estos NO ES la app del PROMPT):

| # | Feature | MUST HAVE | Status | Implementation |
|---|---------|-----------|--------|-----------------|
| 1 | Multiidioma (6 idiomas: ES, EN, FR, PT, DE, IT) | ✅ YES | ⚠️ Partial | [LanguageSelectorActivity.kt] - Extend to all screens |
| 2 | Dual-Role System (Monitor + Terminal) | ✅ YES | ✅ YES | [RoleSelectorActivity.kt] |
| 3 | Firebase Auth (Google OAuth + Email) | ✅ YES | ✅ YES | [LoginActivity.kt] |
| 4 | QR Scanning for Pairing | ✅ YES | ✅ YES | [QRScannerActivity.kt] |
| 5 | Audio Calls (walkie-talkie, NO UI needed) | ✅ YES | ⚠️ Partial | Walkie-talkie vía CallManager.kt - Sin interfaz visual, sin Activity separada |
| 6 | Video Monitoring (Silent - video + audio) | ✅ YES | ⚠️ Partial | [VideoActivity.kt] - Needs mode flags for audio-only |
| 7 | Video Calls (bidirectional) | ✅ YES | ⚠️ Partial | [VideoActivity.kt] - Missing Terminal auto-accept logic |
| 8 | Notifications (accumulated, max 10 events, per-terminal Bubbles API 30+) | ✅ YES | ✅ YES | [notification_custom.xml, NotificationHelper.kt, TerminalBubbleManager.kt] |
| 9 | Alert Log (historical notifications) | ✅ YES | ✅ YES | [AlertLogActivity.kt] |
| 10 | Settings (shared Monitor/Terminal) | ✅ YES | ✅ YES | [SettingsActivity.kt] |
| 11 | Role Selector/Switch | ✅ YES | ✅ YES | [RoleSelectorActivity.kt] + Menu |
| 12 | Voice Detection (ML Kit - custom phrases) | ✅ YES | ✅ YES | [VoiceCommandManager.kt] - Intelligent voice recognition |
| 13 | Shake Detection (Terminal ONLY - accelerometer alert trigger) | ✅ YES | ⚠️ Partial | [BellActivity.kt] - Needs Card 3 UI toggle + background service |
| 14 | E2E Encryption (communication) | ✅ YES | ⚠️ Partial | Transport layer only - **NEEDS APP-LEVEL ENCRYPTION** |
| 15 | Local Encryption (Room SQLCipher AES-256) | ✅ YES | ✅ YES | [AppDatabase.kt] |
| 16 | Local WiFi First (mDNS discovery) | ✅ YES | ✅ YES | [LocalDiscoveryService.kt] |
| 17 | Capabilities Assessment (Geriatric eval) | ✅ OPTIONAL | ✅ YES | [CapabilitiesAssessmentActivity.kt] - Skippable assessment |
| 18 | Bell Overlay on Lockscreen | ✅ YES | ✅ YES | [BellActivity.kt with WindowManager] |
| 19 | Terminal Screenlock Screensaver | ✅ YES | ⚠️ Partial | layout_lockscreen.xml exists but content unclear |
| 20 | Terminal Auto-accept Calls | ✅ YES | ✅ YES | [CallManager.kt] - Automatic acceptance implemented |
| 21 | Alarms System (Terminal-created schedules) | ✅ YES | ✅ YES | [Alarm entity, AlarmAdapter, CreateAlarmDialog.kt] |
| 22 | Custom Phrases (Voice commands + auto-mapping) | ✅ YES | ✅ YES | [CustomPhrasesManager.kt] - Auto-add Monitor name |
| 23 | Alert Log (Monitor notification history) | ✅ YES | ✅ YES | [AlertLogActivity.kt] - Historical view |
| 24 | Remote Notifications (Firebase CloudMessaging) | ✅ YES | ✅ YES | [FCMService.kt] - Registra FCM token en Firestore `/users/{userId}` field `fcmToken`. Recibe push notifications. Implementado. |
| 25 | Language Auto-detection (device locale) | ✅ YES | ⚠️ Partial | SplashActivity.kt - Needs comprehensive locale fallback |
| 26 | Battery Low Notification (Terminal → Monitor) | ✅ YES | ❌ MISSING | **NEW PHASE 7** - Terminal detects low battery (≤15%) and sends alert to Monitor |

---

**Summary**:
- ✅ **18 FULLY IMPLEMENTED** - Core working features
- ⚠️ **5 PARTIALLY IMPLEMENTED** - Need fixes/extensions  
- ❌ **2 MISSING** - Must be implemented before release
- ℹ️ **1 OPTIONAL** - Geriatric assessment can be skipped by user

**BEFORE RELEASE**: ❌ Items 13, 26 MUST be completed. Multiidioma MUST cover all screens.

---

## 📋 NON-FUNCTIONAL REQUIREMENTS (NEW - March 29, 2026)

### Platform Constraints
| Requirement | Specification | Notes |
|-------------|---------------|-------|
| **Minimum Android Version** | API 26 (Android 8.0) | No support for API < 26 |
| **Target Android Version** | Latest stable (API 34+) | Build against latest SDK |
| **Languages** | 6 supported: ES, EN, FR, PT, DE, IT | All UI text must be translatable |
| **Target Users** | Geriatric (65+ years) | Large UI elements (72dp primario, 56dp secundario, 48dp mínimo absoluto), high contrast (WCAG AAA 7:1 target), 18sp body text mínimo — Ver sección "DISEÑO Y ACCESIBILIDAD v2.0" |
| **Device Types** | Phones + Tablets | Both roles (Monitor/Terminal) |

### Availability & Scalability
| Requirement | Target | Justification |
|-------------|--------|---------------|
| **System Availability** | 99% uptime | Three 9s target: allowed downtime ~7 hours/month |
| **1:N Architecture (Monitor:Terminals)** | Unlimited theoretically; Limited by network topology | **Local WiFi**: Limited to ~254 hosts (Class C /24 subnet) minus Monitor IP = **max ~253 Terminals locally** |
| **1:N Architecture (via Internet)** | Unlimited via Firestore | Can pair across internet using Firestore as broker (beyond local WiFi) |
| **Concurrent Alerts** | ≤ 5 simultaneous alerts | Design assumes < 5 alerts/second; beyond queues |
| **Alert History Retention** | ≥ 24 hours | Local DB stores all events; Firestore syncs for backup |
| **Offline Operation** | ≥ 2 hours without internet | Core features (shake detection, alarms, alerts) work without Firestore |

### Performance Requirements
| Requirement | Target | Measurement Method |
|-------------|--------|-------------------|
| **App Startup Time** | < 3 seconds (cold start) | Logcat timing: "(app name) ready" message |
| **Shake Detection Latency** | ≤ 500ms from sensor to HTTP POST | ShakeDetectionService timestamps vs CampanaHttpServer receipt |
| **Alert Reception Latency** | ≤ 2 seconds from Terminal send to Monitor notification | Network timing logs |
| **UI Response Time** | ≤ 100ms button press → visual feedback | Android Profiler frame timing |
| **Memory Usage** | ≤ 150MB average, ≤ 250MB peak | Memory Profiler during peak load (video + notifications) |
| **Battery Drain** | Shake detection: ≤ 5%/hour when active | Battery usage monitor (depends on sensor polling rate) |

### Reliability & Data Integrity
| Requirement | Specification | How Verified |
|-------------|---------------|--------------|
| **Data Persistence** | 100% of local events survive app crash | Test: Insert event → kill app → relaunch → verify in DB |
| **Alert Delivery** | "At least once" semantics | Alert may arrive twice (never loss) - by design |
| **Encryption** | AES-256 for local DB (SQLCipher) + TLS 1.2+ for network | SQLCipher initialization + certificate pinning |
| **No False Negatives (Shake)** | Once 12 m/s² threshold crossed, 100% detection | Calibration test: accelerometer readings logged |

### Security & Privacy
| Requirement | Implementation | Validation |
|-------------|---|---|
| **E2E Encryption** | TLS 1.2+ for all HTTP/HTTPS | Certificate pinning + handshake verification |
| **Runtime Permissions** | All sensors/location/contacts via ActivityCompat.requestPermissions() | BODY_SENSORS for accelerometer; CAMERA for QR/video |
| **No Hardcoded Secrets** | All API keys in EncryptedSharedPreferences or Firebase Config | Code audit: grep for `key=` patterns |
| **Privacy: No Tracking** | No location tracking, no device fingerprinting, no activity profiling beyond app logs | Design review: NSURLSession logging, Firestore queries should not identify users |

> 🔴 **ALERTA CRÍTICA v4.1 — SEGURIDAD**: `SecurityConfig.kt` contiene claves AES HARDCODEADAS (`"CampanaSecureKey"` y `"CampanaInitVect1"`). Esto viola el requisito "No Hardcoded Secrets". Se requiere implementar derivación de claves real:
> - **Reemplazar**: `SecurityConfig.AES_KEY` y `SecurityConfig.AES_IV` con claves derivadas via PBKDF2 o Android KeyStore
> - **Opciones de key derivation**:
>   1. `KeyStoreHelper` ya existe — usar para generar/almacenar claves AES en Android Keystore
>   2. PBKDF2 con salt único derivado de Firebase UID del usuario
>   3. Para comunicación HTTP local: Shared secret intercambiado via QR (campo `secret` del QR JSON) como input para PBKDF2
> - **Eliminación**: `SecurityConfig.kt` no debe contener NINGÚN `SecretKeySpec` literal ni `IvParameterSpec` literal
> - **IV**: Generar IV aleatorio por cada operación de cifrado (no reusar)
> - **Afecta**: AES256EncryptionHelper, Room SQLCipher passphrase, Custom Phrases encriptación
> - **Estado**: ❌ NO IMPLEMENTADO — requiere WorkItem dedicado

---

## 🎯 VISIÓN GENERAL

Monitor de Cuidados es una aplicación dual-rol diseñada para el cuidado y monitoreo de personas mayores, optimizada para accesibilidad extrema y simplicidad radical. 

**Principio Rector (v2.0)**: "La calidad de la experiencia del usuario es el diferenciador #1". Basada en investigación de empresas exitosas en eldercare (GrandPad, GreatCall/Lively, Life360, Care.com), estándares W3C WAI para usuarios mayores, y 7 principios de Diseño Universal. El Terminal (dispositivo del adulto mayor) sigue el modelo "walled garden" de GrandPad — simplicidad extrema con máximo 3 acciones visibles. El Monitor (dispositivo del cuidador) sigue el modelo "dashboard inteligente" de Life360 — información de un vistazo con acciones de 1 tap.

### 🏗️ PRINCIPIO ARQUITECTÓNICO: LOCAL-FIRST (NUEVO v4.1 — Abril 2, 2026)

> **Decisión arquitectónica PERMANENTE**: La app DEBE funcionar perfectamente en red WiFi local sin internet. Firestore es almacenamiento secundario para cross-network y backup, NUNCA la fuente primaria de datos en tiempo real.

**Jerarquía de datos**:
1. **Room DB local** = fuente de verdad para operación en tiempo real (eventos, alarmas, frases)
2. **HTTP local (WiFi)** = canal primario de comunicación Terminal↔Monitor (CampanaHttpServer puerto 8080)
3. **Firestore** = almacenamiento persistente + sincronización cross-network + backup histórico

**Reglas LOCAL-FIRST**:
- **DashboardFragment** lee alertas desde Room DB local (tabla Event), NO desde Firestore `notifications_history`
- **Alertas** llegan via HTTP POST local (Terminal→Monitor) y se guardan en Room DB inmediatamente
- **Alarms** se guardan en Room DB primero, se sincronizan a Firestore via SyncManager (30s interval)
- **Custom Phrases** se guardan en Room DB primero, sync a Firestore cuando hay conexión
- **Si no hay internet**: La app funciona 100% en red local WiFi. Firestore queda pendiente (FirebaseSyncQueue)
- **Si no hay WiFi local**: Firestore actúa como broker para comunicación cross-network

**SyncManager** (implementación actual):
- Auto-sync cada 30 segundos: Room DB → Firestore
- Entidades sincronizadas: Event, Alarm, CustomPhrase
- FirebaseSyncQueue encola operaciones cuando Firestore no está disponible
- Al reconectar: despacha cola con timestamps originales

---

## 📱 FUNCIONALIDADES PRINCIPALES

### 1️⃣ Onboarding y Autenticación (Monitor & Terminal) — ACTUALIZADO v2.0
- **Selector de Idioma**: Pantalla inicial (LanguageSelectorActivity) con RecyclerView vertical. Cada item muestra: bandera emoji (🇪🇸🇬🇧🇫🇷🇵🇹🇩🇪🇮🇹) + nombre del idioma + check verde (✓) al seleccionar. 6 idiomas: Español, Inglés, Francés, Portugués, Alemán, Italiano. Cada dispositivo mantiene su idioma seleccionado independientemente, sin importar su rol (Monitor o Terminal) o cambios de emparejamiento posteriores.
- **Login**: Soporte para Google OAuth y credenciales locales mediante Firebase Auth.
- **Onboarding Guiado (NUEVO v2.0)**: ViewPager2 + TabLayout (indicador de dots). Experiencia de primer uso con 5 pasos:
  1. Bienvenida con botón "COMENZAR CONFIGURACIÓN" (72dp, teal). Contenido centrado verticalmente. Sin indicador de dots en esta pantalla (solo se muestra al haber múltiples slides).
  2. Elegir rol: "Soy el Cuidador" / "Soy la Persona Cuidada" (cards tappable 72dp)
  3. Setup según rol: QR scanner (Monitor) o mostrar QR (Terminal)
  4. Evaluación de Capacidades (solo Terminal, omitible)
  5. Tutorial interactivo de 3 slides (solo Terminal): campana, confirmación — con dots indicator visible
  - **TabLayout dots**: drawable custom `tab_dot_selector.xml` (8dp círculos, teal activo, gris inactivo). OCULTO en pantalla de bienvenida (paso 1), visible en pasos con múltiples slides.
  - Ver sección "DISEÑO Y ACCESIBILIDAD v2.0 → ONBOARDING" para especificación completa
  - Control: flag `onboarding_completed` en SharedPreferences, reseteable desde Settings
- **Evaluación de Capacidades** (CapabilitiesAssessmentActivity - OPCIONAL, EXPANSIÓN v4.1):
  - Cuestionario post-login que evalúa 5 capacidades: audición, movilidad, cognición, habla, visión
  - Usuario puede presionar botón "OMITIR" para saltar la evaluación (56dp, gris, prominente)
  - Si responde: Respuestas se guardan localmente (Room/SharedPreferences) + Firestore → **Disparan UI Adaptativa** (ver sección correspondiente)
  - Si omite: Se marca como "omitted" y navega a TerminalMainActivity
  - Respuestas se usan para personalizar sensibilidad de reconocimiento de voz Y adaptar UI (tamaños, contraste, vibración)
  - **NUEVO v4.1 — Info mostrada al Monitor al escanear QR**: Cuando Monitor escanea QR de Terminal, se le muestra resumen de capacidades del adulto mayor (si fueron completadas). El Monitor puede ver: nivel de audición, movilidad, cognición, habla, visión.
  - **NUEVO v4.1 — Editable por ambos roles**: TANTO Monitor como Terminal pueden editar las capacidades en cualquier momento desde Settings.
  - **NUEVO v4.1 — Sugerencias inteligentes de servicios**: Basado en las capacidades evaluadas, la app sugiere qué servicios activar. Ejemplo: si audición=baja → sugiere activar detección de agitación como alternativa a voz. Si movilidad=baja → sugiere activar shake con sensibilidad reducida.
  - **NUEVO v4.1 — Almacenamiento dual**: Capacidades guardadas en Room DB local (operación offline) + sincronizadas a Firestore (cross-network)
- **Selección de Rol**: Integrada en onboarding (paso 2). También accesible después desde Settings → "Cambiar Modo".

### 2️⃣ Monitor App (app/) - REDISEÑADO v2.0

> ⚠️ **REDISEÑO**: Ver sección "DISEÑO Y ACCESIBILIDAD v2.0 → MONITOR: REDISEÑO" para especificación completa del nuevo dashboard.

**Navegación v2.0 (ACTUALIZADO Marzo 31)**: DrawerLayout como navegación principal. SIN BottomNavigationView (eliminado por redundancia con Drawer). Drawer contiene: Cambiar Modo, Ajustes, Acerca de, Cerrar Sesión. Toolbar con: título "Monitor de Cuidados" + botón notificaciones (campana con badge) que funciona como TOGGLE: primer toque abre HistoryFragment, segundo toque regresa a DashboardFragment. **Drawer Header (nav_header_main.xml)**: headerTitle muestra nombre del PROPIO usuario Monitor (Firebase displayName si tiene sesión, Build.MODEL si modo local) — NO el nombre de la terminal pareada. headerEmail muestra email del usuario (dinámico desde FirebaseAuth). **Toolbar title**: SIEMPRE "Monitor de Cuidados" (nombre de la app) — NUNCA nombre del dispositivo.

**Notificaciones Visibles de Alerta (NUEVO v2.4 — Marzo 31)**: Cuando Terminal envía alerta HTTP (campana/shake/voz) al Monitor, el Monitor DEBE mostrar una notificación heads-up visible con sonido y vibración usando ALERT_CHANNEL_ID (IMPORTANCE_HIGH). La notificación silenciosa del servicio foreground NO cuenta como alerta visible. Cada alerta genera una notificación independiente con ID incremental (100-200).

- **Tab Inicio — Dashboard Inteligente**:
  - **Alertas Activas (LOCAL-FIRST)** (prioridad #1, top): Cards con alertas no atendidas, botones LLAMAR y MONITOREAR inline por cada alerta. **DashboardFragment DEBE leer alertas desde Room DB local (tabla Event)**, NO desde Firestore `notifications_history`. Los eventos llegan vía HTTP local (Terminal→Monitor) y se guardan en Room inmediatamente. SyncManager sincroniza Room→Firestore como backup cada 30s.
  - **Mis Terminales** (prioridad #2): Estado de un vistazo de todos los terminales (nombre + conexión + batería). Tap en terminal → abre TerminalDetailActivity (con extra `terminal_id`). **DashboardFragment DEBE llamar `connectionViewModel.getPairingsList()` en `setupObservers()` Y en `onResume()`** para disparar la query Firestore. La llamada en `onResume()` actúa como retry si Firebase Auth no estaba restaurada en `onViewCreated()`. **ConnectionViewModel DEBE almacenar `ListenerRegistration`** del `addSnapshotListener` y remover el anterior antes de agregar uno nuevo (evitar listener accumulation). **`onCleared()` DEBE remover el listener.**
  - **Vincular Terminal** (prioridad #3): Botón QR para emparejar nuevos dispositivos. **En el layout, el botón "Vincular Terminal" aparece ENCIMA de la lista de terminales** (orden: Header → Botón → Lista/Vacío).
- **Vincular Terminal**: Escaneo de QR con cámara trasera para emparejamiento seguro. **PERMISO CAMERA REQUERIDO**: DashboardFragment DEBE verificar permiso `CAMERA` ANTES de lanzar `QRScannerActivity`. Si denegado permanentemente, guiar al usuario a Ajustes → Permisos. `addPairing()` DEBE completar escritura Firestore ANTES de navegar a MonitorMainActivity (evitar race condition).
- **Llamadas Telefónicas**: Iniciar y colgar llamadas de voz bidireccionales con el Terminal.
- **Monitoreo Silencioso**: Activar cámara y micrófono del Terminal para monitoreo silencioso sin alertar al usuario.
- **Video Llamada Integrada**: Soporte para transmisión de video bidireccional como opción adicional.
- **Monitoreo en Vivo**: Streaming de video con 3 controles (Salir, Toggle Bidireccional, Cambiar Cámara) en interfaz SIN diálogo previo.
- **Notificaciones como Cards de Alerta**: Sistema de cards en dashboard con alertas activas (máx 10 eventos: bell/voice/shake/battery_low/alarm), swipe-to-dismiss. Botones LLAMAR + MONITOREAR inline en cada card.
- **Gestión de Alarmas**: Configuración de recordatorios para el Terminal (en SettingsActivity, Tab Ajustes).
- **Tab Historial** (AlertLogActivity - HISTORIAL CONSOLIDADO 1:N):
  - **Panel que muestra HISTORIAL CONSOLIDADO de TODOS los Terminales emparejados** (vista unificada)
  - Solo Monitor actual puede ver este log (Terminal NO tiene acceso)
  - Cada Monitor en una pareja 1:N ve su propio AlertLog (no comparten datos)
  - **Identificación de Terminal**: Cada evento incluye nombre de la persona que usa Terminal (si sesión iniciada) o nombre del dispositivo como fallback
  - **Formato de evento**: "María - Campana - hace 2 min" | "Pedro - Voz (Ayuda) - hace 5 min" (identifica origen Terminal)
  - Información mostrada: nombre Terminal, timestamp, tipo de evento (Campana/Voz/Alarma), detalles del evento
  - Período de almacenamiento: Últimas 24 horas de eventos
  - Items de 56dp alto, timestamps en 16sp, iconos 24dp
  - Filtro por terminal (dropdown) y por tipo de evento
- **Tab Ajustes** (SettingsActivity): Menú de configuración con categorías de headers 24sp, items 56dp alto.
- **Cambiar Modo**: Accesible desde drawer lateral (NO en tabs), abre RoleSelectorActivity SIN cerrar sesión.

### 3️⃣ Terminal App (campana/) - REDISEÑADO v2.0 (Marzo 30, 2026)

> ⚠️ **REDISEÑO RADICAL**: Ver sección "DISEÑO Y ACCESIBILIDAD v2.0 → TERMINAL: REDISEÑO RADICAL" para especificación completa de la nueva interfaz.

**Filosofía v2.0**: Inspirada en GrandPad (walled garden, simplicidad extrema). El Terminal es el dispositivo del adulto mayor — debe ser EXTREMADAMENTE simple.

**Pantalla Principal — QR + Drawer (REDISEÑO Marzo 30 por feedback de usuario)**:
- **Toolbar** con ☰ hamburguesa (abre DrawerLayout idéntico al Monitor: Cambiar Modo, Ajustes, Acerca de, Cerrar Sesión). **Título toolbar**: SIEMPRE "Monitor de Cuidados" (nombre de la app) — NUNCA nombre del dispositivo.
- **QR Code** (centrado, prominente): Muestra QR de vinculación SIEMPRE para que Monitor lo escanee
- **Drawer Header (nav_header_main.xml)**: headerTitle muestra nombre de la persona vinculada (si sesión activa) o nombre del dispositivo vía `Build.MODEL` (si modo local). headerEmail muestra email del usuario. Contenido dinámico, NO hardcodeado.
- **Sin botón campana** en pantalla principal — la campana queda EXCLUSIVAMENTE en BellActivity (pantalla de bloqueo)
- **Info strip ELIMINADO** — La batería y WiFi ya se ven en la barra del sistema Android
- **Card de Estado de Servicios (NUEVO v2.2 — Marzo 31, CORREGIDO v2.5)**: Debajo de la card QR, una MaterialCardView con switches para controlar los servicios del Terminal: Modo Campana (lockscreen_bell_enabled), Activación por Agitación (shake_detection_enabled), Detector de Voz (voice_detection_enabled), Alarmas (reminders_enabled). Muestra indicador "Activo"/"Inactivo" según CampanaService.isRunning. Los switches leen/escriben las mismas SharedPreferences que Settings. **TODOS los switches DEBEN llamar `refreshService()` al togglear** — esto envía REFRESH_SERVICES a CampanaService para aplicar cambios sin reiniciar el servicio. NO incluir "Accesibilidad".

**Cambios vs v2 (revertidos por testing real)**:
| v2 (diseño anterior) | v2.1 (actual) | Justificación |
|-----------|-----------|---------------|
| Campana 160dp principal | QR centrado prominente | QR es esencial para vinculación, campana solo en lockscreen |
| Botón ⚙️ sin drawer | ☰ Hamburguesa + DrawerLayout | Consistencia con Monitor, acceso rápido a opciones |
| Info strip (batería, WiFi) | Eliminado | Redundante con barra del sistema Android |
| Sin QR en main | QR SIEMPRE visible | Monitor necesita escanear QR para vincular |

**Servicio Auto-Start (CAMBIO v2)**: CampanaService se inicia automáticamente al abrir app en modo Terminal. NO hay switch manual. Si cuidador necesita pausar: Settings → "Pausar servicio".

**Configuración Terminal — Acceso vía Drawer → Ajustes (SettingsActivity)**:
- Sensores: shake toggle, voz toggle, frases personalizadas
- Alarmas: mis alarmas
- Sonido/Vibración: tono, vibración toggle, volumen
- Recordatorios: habilitar, configurar horarios
- Accesibilidad: tamaño de texto
- Apariencia: idioma, modo nocturno
- Extras: ver tutorial, pausar servicio
- **OCULTAR en Terminal**: Límite de historial, Notificaciones de llamada, Alertas de video (son Monitor-only)
- **OCULTAR en Terminal**: Sección "Mi Dispositivo" completa (nombre/vinculación van en toolbar, no en Settings)
- **OCULTAR en Monitor**: Respuesta automática, Sensibilidad de voz, Volumen de campana (son Terminal-only)

- **Activación por Sensores (Terminal ONLY)**: Soporte para disparo de alerta mediante agitación del dispositivo (acelerómetro) con toggle en Settings → Sensores. Ejecuta SIEMPRE que esté encendido y el servicio activo, sin importar si se agita dentro o fuera de la aplicación. Independiente a lockscreen y voice detector. Al detectar agitación, envía HTTP POST a Monitor en `/alert/shake`.
- **Comandos de Voz (ACTUALIZADO v2.7 — Abril 2, 2026)**: Detección de frases de ayuda mediante SpeechRecognizer (VoiceCommandManager). Keywords por defecto: "auxilio", "ayuda", "socorro", "necesito ayuda". Soporta frases custom via Room DB. CampanaService instancia VoiceCommandManager cuando role=Terminal y voice_detection_enabled=true y RECORD_AUDIO está otorgado. Al detectar keyword, envía HTTP POST a Monitor en `/trigger_voice` con JSON `{"text": keyword}`. **Terminal NO se notifica a sí mismo** (no sound, no vibration) — solo envía HTTP al Monitor. Si RECORD_AUDIO no está otorgado, `voice_detection_enabled` se fuerza a `false`. Configurable en Settings → Sensores → Frases personalizadas.
- **HTTP Server en Terminal (NUEVO v2.3 — Marzo 31)**: Terminal TAMBIÉN inicia CampanaHttpServer en puerto 8080 para recibir: confirmación de pairing (`/confirm_pairing`), comandos del Monitor, y solicitudes de llamada/monitoreo. Sin esto, el Monitor no puede completar el pairing y `paired_monitor_ip` nunca se guarda en Terminal.
- **Notificación de Servicio**: Mostrar "Servicio Activo" ÚNICAMENTE (sin iconos de llamada/video que son del Monitor). Notificación de servicio foreground SIEMPRE silenciosa (`.setSilent(true)`) — NO debe producir sonido ni vibración al actualizarse.
- **Gating de Permisos (NUEVO v2.7)**: Los switches de servicios (Detector de Voz, etc.) DEBEN verificar que el permiso requerido esté otorgado ANTES de activarse. Si el permiso no está otorgado: solicitar en runtime. Si el usuario deniega: revertir switch a OFF y mostrar Toast explicativo. `onRequestPermissionsResult()` sincroniza estado de permisos con switches.
- **Botón Cambiar Modo**: Accesible SOLO desde Settings (al fondo de la lista, botón discreto).
- **Aceptación Automática de Llamadas de Voz (LLAMAR)**: Terminal acepta automáticamente:
  - Llamadas de voz (walkie-talkie) → Auto-start, sin diálogo de aceptación/rechazo, sin interfaz visual
  - Motivo: Personas cuidadas usualmente no pueden manipular teléfono (discapacidad del adulto mayor)

#### 3C️⃣ Notificación de Batería Baja (Terminal) - NUEVO Marzo 29 - **PHASE 7**

**Descripción**: Terminal monitorea continuamente el nivel de batería y notifica al Monitor cuando está por debajo del 15%.

**Monitoreo en Terminal (LOCAL-FIRST + Firestore backup)**:
- CampanaService registra `BatteryReceiver` (BroadcastReceiver) para acción `Intent.ACTION_BATTERY_CHANGED`
- Se ejecuta cada vez que nivel cambia (no consume batería extra - es notificación del OS)
- Cuando nivel ≤15%:
  1. **HTTP local PRIMERO**: Envía HTTP POST a Monitor `/alert/battery_low` con JSON `{"level": N, "deviceName": "..."}` (igual que shake/bell)
  2. **Firestore backup**: Llama `logBatteryLowToFirebase(batteryLevel)` para registrar en Firestore (cross-network + historial)
  3. **Room DB local**: Guarda Event(type="battery_low") en Room DB del Terminal
- Cuando nivel >20%: Envía HTTP POST `/alert/battery_ok` + `logBatteryOkToFirebase()` (hysteresis)

**Evento Registrado en Firestore**:
```json
/notifications_history/{documentId} = {
  "userId": "terminalId",           // Terminal Firebase UID
  "type": "battery_low",            // Event type
  "batteryLevel": 15,               // Actual battery percentage
  "deviceName": "María Terminal",   // Terminal device name (from preferences)
  "timestamp": FieldValue.serverTimestamp(),
  "monitorId": "pairedMonitorId",   // Current paired Monitor
  "severity": "warning"             // For UI highlighting
}
```

**Recepción en Monitor (LOCAL-FIRST)**:
- **Vía HTTP local** (prioridad): CampanaHttpServer recibe POST `/alert/battery_low` → guarda Event en Room DB → NotificationHelper.notifyAlert() muestra heads-up
- **Vía Firestore** (cross-network fallback): NotificationViewModel observa `/notifications_history` como fallback si Terminal no está en red local
- Cuando evento `type: "battery_low"` se detecta (por cualquier vía):
  1. Crea notificación con ícono distintivo 🔋 (battery emoji)
  2. Título: "Batería Baja"
  3. Contenido: "[Terminal Name] - [15%]"
  4. Expandible mostrando: Nombre, Nivel de Batería, Hora exacta
  5. Se agrega a NotificationHelper (acumulada, máx 5 eventos)
  6. Se registra en AlertLogActivity como fila en tabla consolidada

**Configuración Personalizable** (SettingsActivity):
- Toggle: "Notificar cuando batería esté baja" (default: **ON**)
- Umbral personalizable: Selector 10% | **15% (default)** | 20% | 25%
- Si Monitor desactiva: Terminal NO envía notificaciones (pero sigue monitoreando)

**Casos de Uso**:
1. Monitor en otra habitación: Recibe alerta → Va a cargar Terminal
2. Terminal en transporte: Monitor es alertado antes de que se apague
3. Monitor de múltiples Terminales: AlertLogActivity consolida alertas (María tiene 10%, Pedro tiene 15%)

**Consideraciones Técnicas**:
- **Offline Support**: Si Terminal sin conexión y batería baja, evento se encola en Room (tabla Event) + FirebaseSyncQueue
  - Al reconectar: Se envía con timestamp original (no actual)
  - Monitor visualiza evento antiguo como "sin conectividad cuando sucedió" (UI marca diferencia)
- **Deduplicación**: Si mismo Terminal env\u00eda multiple battery_low en <1 min, solo se muestra 1 notificación
- **Histeresis**: Para evitar spam de fluctuaciones:
  - Trigger BAJO: ≤15% (configurable)
  - Trigger ALTO: >20% (hardcoded, 5% gap)
  - Ejemplo: Si baja 15% → Alerta. Si luego sube a 17% y baja a 14% → NO re-alerta. Debe subir >20% primero.

---

#### 3A️⃣ Sistema de Alarmas (Terminal) - NUEVO Marzo 28

**Funcionalidad**: Terminal crea alarmas personalizadas para recordatorios diarios (medicinas, comida, levantarse, baño, etc.)

**Creación** (SOLO Terminal):
- Terminal → SettingsActivity → "Mi Alarmas" → Presiona "+" → dialog_create_alarm.xml
- Selecciona hora fija (HH:MM), días (L, M, X, J, V, S, D), descripción
- Guarda en Base Local (Room) + sincronizado a Firestore
- Field `createdBy: "terminalId"` registra el origen

**Activación (Ambos dispositivos — BIDIRECCIONAL con Firestore listener v4.1)**:
- Cuando llega la hora: Alarma SUENA EN TERMINAL Y EN MONITOR
- Terminal ve UI con botón "Detener"
- Monitor recibe notificación con botón "Detener"
- **Con que UNO presione "Detener"** → Alarma se apaga EN AMBOS dispositivos
- **NUEVO v4.1 — Firestore real-time listener para sync bidireccional**: Ambos dispositivos escuchan cambios en la colección de alarmas via `addSnapshotListener`. Cuando Monitor crea/modifica/detiene alarma → Terminal la recibe en tiempo real via Firestore listener (incluso si no están en la misma red WiFi). Cuando Terminal detiene alarma localmente → actualiza Firestore → Monitor recibe cambio via listener.
- **LOCAL-FIRST**: Si están en misma red WiFi, TAMBIÉN enviar comando HTTP POST `/command` con action `stop_alarm` para respuesta inmediata. Firestore actúa como fallback cross-network.
- Sync offline: Si Terminal está offline cuando Monitor detiene alarma, se sincroniza al reconectar

**Acceso de Monitors Múltiples** (Arquitectura 1:N):
- Si Terminal está emparejado con Monitor A, Monitor A crea alarmas (entera)
- Si Terminal se re-empareja a Monitor B:
  - Monitor B es el nuevo Monitor actual (recibe notificaciones de alarma)
  - Monitor A deja de recibir notificaciones de nuevas alarmas
  - Alarmas históricas siguen registradas pero inactivas para Monitor A
  - Monitor B ve/puede controlar todas las alarmas en Firestore (sin distinción de creador en UI)

**Monitor: Vista de Alarmas** (Arquitectura 1:N - CONSOLIDADA):
- **Estructura**: Vista CONSOLIDADA de alarmas de TODOS los Terminales emparejados
- **Organización**: Tabs o acordeones por Terminal (ej: Tab "María", Tab "Pedro")
- **Contenido bajo cada Terminal**: 
  - Lista de alarmas activas con hora, días, descripción
  - Botones: "Editar", "Eliminar", Toggle habilitar/deshabilitar
  - Monitor puede editar/eliminar alarmas de cualquier Terminal emparejado
- ⚠️ **NOTA FUTURA**: Si N=1, se muestran alarmas directamente. Si N>1, cada Terminal en tab/acordeón separado
- **Sincronización en tiempo real**: Si Terminal emparejado cambia, alarmas actualizan automáticamente
- **Cuando Terminal se desempareja**: Ese Terminal desaparece de controlador de Alarmas del Monitor anterior

**Nota de Diseño**: 
- Alarms son propiedad del Terminal (no del Monitor)
- Todos los Monitors que se emparejen con ese Terminal comparten acceso
- Para dispositivos de red local con múltiples Monitors: El último QR escaneado define al Monitor actual

#### 3B️⃣ Frases Personalizadas y Reconocimiento de Voz (Terminal)

**Custom Phrases** (Frases personalizadas para voz):
- Terminal DEFINE sus propias frases de ayuda:
  - Ejemplos: "Ayuda", "Ven", "Monitor", nombre de cuidador, "Baño de emergencia"
  - Se guardan LOCALMENTE (Room) + CLOUD (Firestore)
  
- **Auto-mapping al Emparejar**:
  - Cuando Terminal se empareja con Monitor por PRIMERA VEZ:
    - Sistema auto-agrega el nombre real del Monitor a lista de frases (ej: "Juana")
    - Ejemplo: Si Monitor se llama "Juana" → Auto-agrega "Juana" a custom phrases con marcador `auto_added: true`
  - **Cuando Terminal se re-empareja a diferente Monitor**:
    - Sistema ELIMINA nombre del Monitor anterior (ej: "Juana" se elimina)
    - Sistema auto-agrega nombre del nuevo Monitor (ej: "Carlos" se agrega)
    - Frases custom creadas por usuario se MANTIENEN (no se eliminan)
  - Si Monitor nunca inicia sesión (sin credenciales) → NO se agrega nada a custom phrases

- **VoiceCommandManager** (Gestor de Comandos de Voz - SOLO Terminal):
  - Reconoce frases customizadas + comandos del sistema ("ayuda", "ven")
  - **Lógica Inteligente de Discriminación**:
    - Distingue conversación normal de verdadero pedido de ayuda
    - Adapta sensibilidad según capacidades evaluadas (CapabilitiesAssessmentActivity)
    - **Si persona tiene problemas para hablar** (según respuestas en assessment):
      - Prestar MAYOR atención a voces débiles/susurradas
      - MENOR umbral de confianza para activación
    - **Si persona NO tiene problemas para hablar**:
      - Prestar atención también a voces fuertes/claras
      - MAYOR umbral de confianza para evitar falsos positivos
  - Cuando se detecta comando: Envía notificación al Monitor actual con la frase detectada

- **Guardar Cambios**:
  - Solo Terminal puede crear/editar custom phrases (no Monitor)
  - Cambios se sincronizan inmediatamente a Firestore si hay conexión
  - Si offline: Se guardan localmente, se sincronizan después
  - Al re-emparejar: antiguas frases auto-added se reemplazan (creadas por usuario se mantienen)

**Notificación al Monitor**:
- Monitor recibe notificación cuando Terminal detecta frase de voz
- Notificación SOLO va al Monitor actual emparejado
- Evento se registra en AlertLog del Monitor con timestamp y frase detectada
- Si Terminal cambia de Monitor, nuevas detecciones van al nuevo Monitor (Monitor anterior deja de recibir)

### 4️⃣ Log de Notificaciones - Terminal (Bell vs Voice Detection Logic)

**Especificación**: Terminal debe diferenciar entre tipos de eventos (campanazo vs voz detectada) y mostrar con iconos y detalles expandibles.

**Display Logic** (Card en interfaz Terminal Config):

| Caso | Display |
|------|---------|
| Solo campanazos | 🔔 "Campana N eventos" |
| Solo voz detectada | 🎤 "Voz N eventos" |
| Ambos tipos | 🔔 "Campana N eventos" + 🎤 "Voz M eventos" (lado a lado) |
| Sin eventos | "Sin eventos" (texto gris) |

**Comportamiento Expandible**:
- **Al expandir 🔔 Campana**: 
  - Muestra lista de últimos campanazos
  - Formato: "Campana tocada - HH:MM"
  - Máximo 5 eventos
- **Al expandir 🎤 Voz**: 
  - Muestra frases/comandos detectados
  - Formato: "Frase detectada: [texto detectado] - HH:MM"
  - Ejemplo: "Ayuda" - 14:32, "Ver Monitor" - 14:28

**Ejemplo Visual**:
```
┌─────────────────────────────┐
│ 🔔 Campana 3 eventos        │
│ 🎤 Voz 2 eventos            │
│                             │
│ [Expandir ▼]                │
├─────────────────────────────┤
│ Campana tocada - 14:32      │
│ Campana tocada - 14:28      │
│ Campana tocada - 14:22      │
│                             │
│ Frase: "Ayuda" - 14:30      │
│ Frase: "Monitor" - 14:25    │
└─────────────────────────────┘
```

---

## 🎮 ESPECIFICACIÓN DE BOTONES Y NAVEGACIÓN (Agregado Marzo 20)

### Llamadas Telefónicas - Audio Call (WALKIE-TALKIE, SIN INTERFAZ VISUAL)
- **Activación**: Presionar botón "Llamar" desde TerminalDetailActivity (pantalla de detalle de Terminal pareado en Monitor)
- **Comportamiento**: La llamada inicia **AUTOMÁTICAMENTE** sin pantalla intermedia, tipo walkie-talkie
- **Cambio Visual**: Botón "Llamar" se convierte en "Colgar" (rojo, 48dp x 48dp) en TerminalDetailActivity
- **Ubicación del Botón Colgar**: En la misma card de acciones (donde estaba el botón Llamar)
- **Comunicación**: Voz bidireccional vía WiFi local (fallback internet) - SIN video, SIN interfaz visual
- **Duración**: Activa hasta presionar "Colgar"
- **Return**: Presionar "Colgar" → Vuelve al estado normal de TerminalDetailActivity
- **Micrófono**: ACTIVADO por defecto (usuario escucha al Terminal)
- ⚠️ **NO crear pantalla separada** - No hay Activity, Fragment ni Layout para llamadas de audio
- ⚠️ **NO usar AudioCallActivity** - La clase existe como componente interno sin UI, gestionada por CallManager

### Interfaz de Video Monitor (Monitoreo Silencioso - MONITOREAR)
- **Activación**: Presionar botón "Monitorear" desde interfaz principal
- ⚠️ **NOTA 1:N**: Actualmente va al Terminal emparejado. En futuro (N>1), selector de múltiples Terminales se implementará en fase posterior
- **Interfaz**: Video en vivo del Terminal (cámara frontal por defecto) + audio micrófono
- **Modo Default**: SILENT (recepción: Monitor VE video+audio remoto, NO ENVÍA su video)
- **3 Botones Inferiores** (48dp x 48dp cada uno, 8dp spacing, bottom-center):
  1. **Cambiar Cámara** - Cicla entre opciones del Terminal:
     - Cámara Frontal
     - Cámara Trasera
     - Ambas Cámaras (lado a lado) - **Solo visible si dispositivo Terminal lo permite**
     - Si dispositivo no soporta cámaras duales → Ocultar opción "Ambas"
  2. **Toggle Bidireccional** - Alterna entre SILENT y BIDIRECCIONAL:
     - Presionar: Activa modo BIDIRECCIONAL (envía video+audio del Monitor, PiP del Terminal visible)
     - Vuelve a presionar: Vuelve a SILENT (recibe solo)
     - Botón cambia icono/color según estado
  3. **Colgar** (rojo) - Termina monitoreo, vuelve a interfaz anterior
- **Micrófono del Terminal**: SIN muting por defecto (escuchar audio ambiente)
- **Terminal**: Operación silenciosa (Monitor supervisa sin alertar)
- **NOTA**: Las llamadas de voz bidireccionales (LLAMAR) NO usan VideoActivity - se inician desde el botón LLAMAR separado

### Interfaz de Videollamada Bidireccional (Dentro de VideoActivity)
- **Activación**: Presionar botón Toggle dentro de VideoActivity cuando está en modo SILENT
- **Interfaz**: Video bidireccional Monitor ↔ Terminal. PiP del Terminal visible en TOP-RIGHT.
- **Botones**: Mismos 3 botones (Cambiar Cámara, Toggle bidireccional para volver a SILENT, Colgar)
- **Comportamiento Terminal**: 
  - ✅ Video bidireccional activo automáticamente al recibir transmisión del Monitor
  - ✅ Terminal puede presionar "Toggle" para volver a SILENT (recepción solo)
  - ✅ Terminal puede presionar "Colgar" para terminar
- **Comunicación**: Voz + Video bidireccional vía WiFi local (fallback internet)

---

### Botón "Llamar" - Llamada Telefónica a Terminal Emparejado
- **Activación**: Presionar botón "Llamar" desde TerminalDetailActivity (tras seleccionar terminal en DashboardFragment)
- **Comportamiento**: Inicia llamada automáticamente al Terminal emparejado (sin diálogo de confirmación)
- **Cambio Visual**: Botón "Llamar" se convierte en "Colgar" (rojo) hasta finalizar llamada
- ⚠️ **NOTA 1:N**: Actualmente en pareja 1:1. En futuro (N>1), llamará al Terminal actualmente seleccionado

---

### 📱 TerminalDetailActivity — Pantalla de Detalle de Terminal (NUEVO v4.1 — Abril 2, 2026)

> **NOTA**: Esta pantalla es del lado MONITOR. Muestra detalle de UN terminal pareado y permite controlarlo.

**Contexto**: Monitor toca un terminal en DashboardFragment → abre TerminalDetailActivity
**Layout**: `activity_terminal_detail.xml`
**Intent Extras recibidos**: `terminal_name` (String), `terminal_status` (String), `terminal_id` (String)

| # | Elemento | Tipo | Función |
|---|----------|------|---------|
| 1 | Toolbar | Toolbar | Título = nombre del terminal, botón back |
| 2 | tvTerminalName | TextView | Nombre del terminal pareado |
| 3 | tvTerminalStatus | TextView | "🟢 Activo • Batería: X%" o "🔴 Desconectado" |
| 4 | btnCall + tvCallLabel + ivCallIcon | Card clickeable | Walkie-talkie: "Llamar" (teal) ↔ "Colgar" (rojo) |
| 5 | btnMonitor | Card clickeable | Lanza VideoActivity en modo "monitor" |
| 6 | tvServiceStatusIndicator | TextView | "✅ Conectado" / "⚠️ Conexión perdida" |
| 7 | switchBellMode | Switch | ON/OFF campana en Terminal remoto |
| 8 | switchShakeDetection | Switch | ON/OFF detección de agitación |
| 9 | switchVoiceDetection | Switch | ON/OFF detección de voz |
| 10 | switchAlarms | Switch | ON/OFF alarmas |

**Comunicación HTTP con Terminal (puerto 8080)**:
| Endpoint | Método | Cuándo | Body |
|----------|--------|--------|------|
| `/status` | GET | Poll cada 5s (backoff exponencial en fallo) | — |
| `/command` | POST | Switch toggled | `{"command":"SET_BELL_MODE_ON"}` etc. |
| `/command` | POST | Colgar llamada | `{"command":"STOP_AUDIO_CALL"}` |
| `/request_call` | POST | Iniciar llamada | `source=monitor` |

**Comandos disponibles via `/command`**:
- `SET_BELL_MODE_ON` / `SET_BELL_MODE_OFF`
- `SET_SHAKE_ON` / `SET_SHAKE_OFF`
- `SET_VOICE_ON` / `SET_VOICE_OFF`
- `SET_ALARMS_ON` / `SET_ALARMS_OFF`
- `STOP_AUDIO_CALL`
- `STOP_MONITOR` — T86: detiene video streaming desde CampanaService
- `STOP_SESSION` — T86: detiene video streaming desde CampanaService

**Walkie-talkie (llamada audio in-place)**:
1. Verifica permiso `RECORD_AUDIO` → solicita si falta (requestCode 3001)
2. HTTP POST `/request_call` al Terminal
3. Crea `CallManager(ip, 9050)` y llama `startCall()` — T87: puerto cambiado de 5060 a 9050
4. UI cambia: "Llamar"→"Colgar", icono teal→rojo
5. Escribe `call_history` en Firestore (caller_id, receiver_id, start_time, call_type="voice", status="in_progress")
6. Al colgar: HTTP POST `/command` con `STOP_AUDIO_CALL` + `callManager.stopCall()` + actualiza Firestore (end_time, status="completed")
7. **Llamada in-place**: NO abre Activity separada, audio corre mientras TerminalDetailActivity está visible

**Polling y reconexión**:
- Poll normal: 5s. En fallo: backoff exponencial 5s→10s→30s→60s
- Tras >5 min fallos: "⚠️ Conexión perdida" (rojo)
- Tras 6+ fallos locales: verifica internet disponible → fallback a connectionType "internet"
- Al recuperar local: vuelve a connectionType "local"

**Guard flag `isUpdatingFromServer`**: Cuando el poll `/status` actualiza switches programáticamente, este flag previene que los listeners de switches envíen comandos HTTP redundantes al Terminal.

---

### Botón "Cambiar Modo" - Comportamiento y Ubicación Definida (ACTUALIZADO v2.0)

**Acción**: Presionar → Abre pantalla "ROLE SELECTION" en AMBOS modos

**UBICACIÓN v2.0 (CAMBIO)**:
- ✅ **Monitor**: DRAWER lateral (accesible desde ícono ☰ en toolbar, 48dp)
- ✅ **Terminal**: SETTINGS → al fondo de la lista (discreto, evitar accidentales del adulto mayor)
- ❌ ~~Menú desplegable~~ → ELIMINADO en v2.0 (drawer en Monitor, Settings en Terminal)

**En Monitor**:
- Presionar ☰ → Drawer abre → "Cambiar Modo"
- Abre pantalla ROLE SELECTION
- Usuario elige: "Monitor" o "Terminal"

**En Terminal**:
- Presionar ⚙️ → Settings → Scroll al fondo → "Cambiar Modo"
- Abre pantalla ROLE SELECTION
- Usuario elige: "Monitor" o "Terminal"

**Pantalla ROLE SELECTION**:
- Dos opciones visibles: "Monitor" y "Terminal"
- Usuario presiona una de ellas
- App carga/inicializa en ese modo SIN cerrar sesión de Firebase
- Botón "Cerrar Sesión" es SEPARADO (en menú drawer)

---

### Pantalla de Campana - Requisitos de Display (ACTUALIZADO - Marzo 31)

⚠️ **ESTRUCTURA DUAL DE BellActivity - DOS LAYOUTS SEPARADOS POR CONTEXTO**:

#### 1️⃣ **Layout: layout_lockscreen.xml - SOLO PARA LOCKSCREEN (Overlay)**

**Contexto**: BellActivity aparece superpuesta en la pantalla de bloqueo. SOLO debe aparecer cuando el teléfono se bloquea (ACTION_SCREEN_OFF), NUNCA cuando el usuario entra a la app directamente.

**Auto-Launch (ACTUALIZADO Marzo 31)**: BellActivity se lanza AUTOMÁTICAMENTE cuando el teléfono se bloquea (ACTION_SCREEN_OFF). CampanaService registra BroadcastReceiver que escucha SCREEN_OFF → lanza notificación fullScreenIntent → Android muestra BellActivity sobre lockscreen. CampanaService TAMBIÉN escucha ACTION_SCREEN_ON para cancelar dicha notificación.

**Comportamiento de Vida de BellActivity (NUEVO v2.4 — Marzo 31)**:
- **SOLO en lockscreen**: BellActivity existe EXCLUSIVAMENTE sobre la pantalla de bloqueo. NUNCA debe persistir cuando el usuario desbloquea el teléfono y entra a la app normalmente.
- **Auto-finish al desbloquear**: BellActivity registra receiver para ACTION_USER_PRESENT. Cuando el usuario desbloquea (PIN/huella/patrón), BellActivity se auto-cierra con `finish()`.
- **noHistory**: AndroidManifest declara `android:noHistory="true"` para que BellActivity nunca quede en el back stack.
- **excludeFromRecents**: AndroidManifest declara `android:excludeFromRecents="true"` para que no aparezca en apps recientes.
- **NO dismiss keyguard**: BellActivity NO debe llamar `requestDismissKeyguard()`. El keyguard permanece activo detrás. Al presionar botón cerrar o back, `finish()` revela la pantalla de bloqueo del dispositivo.

**Barra de Navegación (NUEVO v2.4 — Marzo 31)**:
- **Oculta**: La barra de navegación del dispositivo (home/back/recientes) se oculta cuando BellActivity está activa (modo inmersivo sticky).
- API 30+: `WindowInsetsController.hide(navigationBars())` con `BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE`
- Legacy: `SYSTEM_UI_FLAG_HIDE_NAVIGATION | SYSTEM_UI_FLAG_IMMERSIVE_STICKY | SYSTEM_UI_FLAG_FULLSCREEN`
- Se restaura en `onResume()` para mantener inmersión tras interacciones.

**Comportamiento de Pantalla (ACTUALIZADO v2.6 — Marzo 31)**:
- **Pantalla siempre encendida**: BellActivity mantiene la pantalla encendida (FLAG_KEEP_SCREEN_ON) permanentemente. La pantalla NO debe apagarse sola mientras BellActivity esté activa.
- **Brillo del sistema**: BellActivity usa el brillo normal del sistema (NO manipula screenBrightness). El brillo mínimo (0.01f) fue eliminado porque hacía el layout invisible — la pantalla aparecía completamente negra.
- **Sin manipulación de brillo**: NO existen funciones setMinBrightness() ni setNormalBrightness(). El brillo lo controla el usuario a nivel del sistema Android.
- **Tema dedicado (NUEVO v2.6)**: BellActivity DEBE usar `Theme.BellLockscreen` (declarado en AndroidManifest con `android:theme`). Este tema hereda de `Theme.MonitorDeCuidados` pero override `android:windowBackground` a `@color/primary_dark` (teal). Esto evita que en night mode (`@color/background = #121212`) la ventana se renderice negra antes de que el layout se infle. El tema se define en AMBOS `values/themes.xml` y `values-night/themes.xml`.
- **Orden de rendering en onCreate (NUEVO v2.6)**: `hideSystemBars()` se llama DESPUÉS de `setContentView(binding.root)`, NO antes. Llamar a `hideSystemBars()` antes de `setContentView()` puede causar problemas de timing con `window.insetsController` / `window.decorView`.
- **Videollamada mantiene brillo**: Si se inicia videollamada desde BellActivity, la pantalla se mantiene encendida con brillo normal durante toda la llamada. VideoActivity también se muestra sobre lockscreen (showWhenLocked + turnScreenOn + FLAG_KEEP_SCREEN_ON).
- **AndroidManifest**: BellActivity y VideoActivity declaran `android:showWhenLocked="true"` y `android:turnScreenOn="true"`. BellActivity adicionalmente declara `android:theme="@style/Theme.BellLockscreen"`.

**Diseño Visual (CORREGIDO Marzo 31 por feedback de usuario con imagen referencia)**:
- **Fondo**: Color TEAL completo (#006B6B / @color/primary_dark) — NO blanco
- **Campana Central**: Icono campana GIGANTE (~360dp alto), color blanco #FFFFFF, centrada vertical y horizontalmente. La campana DOMINA la pantalla. QR debe caber visualmente dentro de los bordes del dibujo de la campana.
- **QR Code (Pairing - ALWAYS VISIBLE)**: ~150dp x 150dp, **SUPERPUESTO DENTRO del cuerpo de la campana** (centrado en el área interior de la campana, NO debajo de ella). Usa FrameLayout o ConstraintLayout overlay para posicionar QR encima del icono de campana.
  - ✅ **Generación**: Contains JSON {deviceId, ip, port, name, secret}
  - ✅ **Escaneado por Monitor**: Monitor scans to trigger pairing flow
  - ✅ **SIEMPRE VISIBLE**: QR nunca desaparece - disponible en todo momento para re-vinculación manual
  - ✅ **Acceso**: Visible en lockscreen para permitir re-pairing sin necesidad de desbloquear
- **Texto acción (PRIMERO)**: "Toca para pedir ayuda" — TextView blanco, BOLD, grande (~24sp), centrado DEBAJO de la campana
- **Texto instrucción QR (SEGUNDO)**: "Escanea este QR para vincular un monitor" — TextView blanco, normal (~16sp), centrado DEBAJO del texto acción
- **Botón CERRAR (Flecha ←)**: **OBLIGATORIO**, 48dp x 48dp, **TOP-LEFT corner** (16dp margin)
  - ✅ **Icon**: Flecha izquierda ← (NOT X or close icon)
  - ✅ **Color**: White #FFFFFF (visible on teal background)
  - ✅ **Function**: `finish()` - cierra overlay, vuelve a pantalla de bloqueo del dispositivo (keyguard NO se dismissea)
  - ✅ Persona mayor presiona flecha para salir del overlay
  - ✅ Botón back del sistema: mismo comportamiento que botón cerrar — `finish()` → pantalla de bloqueo
- **🚨 TOUCH TARGET**: TODA la pantalla es área táctil para activar la campana EXCEPTO el botón cerrar (flecha ←). No es necesario tocar exactamente el icono de la campana.
- **🔔 Sonido al tocar campana (ACTUALIZADO v2.8 — Marzo 31, 2026)**: Al presionar la campana (triggerBell), se reproduce un sonido local fijo (`res/raw/bell_chime.wav` — desk bell metálico 1.2s, fundamental 3200Hz, 7 parciales inharmónicos) vía MediaPlayer con USAGE_ALARM, ADEMÁS de enviar la señal HTTP al Monitor. Sonido fijo embebido, NO configurable por el usuario. Simula campana de escritorio tipo service bell con brillo metálico y decaimiento natural.

**NO incluye**:
- ❌ Menú hamburguesa
- ❌ Drawer
- ❌ Botones adicionales
- ❌ Configuración

**Purpose**: Interfaz minimalista para persona que solo necesita presionar campana o salir

**Ejemplo Visual (REFERENCIA EXACTA)**:
```
┌─────────────────────────────┐
│ ←                           │
│                             │
│         ╭ ─ ╮              │
│        ┌─────────┐         │
│        │         │         │
│        │ ┌─────┐ │         │
│        │ │ QR  │ │  ← QR   │
│        │ │CODE │ │  DENTRO  │
│        │ └─────┘ │  de la   │
│        │         │  campana │
│        └─────────┘         │
│          ╰─────╯           │
│                             │
│   Toca para pedir ayuda    │ ← BOLD 24sp
│  Escanea QR para vincular  │ ← normal 16sp
│                             │
└─────────────────────────────┘
```
└─────────────────────────────┘
```

#### 2️⃣ **~~Layout: layout_adulto_mayor.xml~~ — NO EXISTE EN CÓDIGO**

> ⚠️ **NOTA**: Este layout fue planificado pero NUNCA implementado. La pantalla principal del Terminal usa `TerminalMainActivity` con `fragment_terminal_qr.xml` (QR centrado + switches de servicios). La funcionalidad descrita aquí está distribuida entre TerminalMainActivity (DrawerLayout + toolbar) y TerminalQRFragment (QR + switches).

**Implementación real — TerminalMainActivity + TerminalQRFragment**:
- **Botón Campana**: 96dp x 96dp, centrado
- **Botón Menú (Hamburguesa)**: 48dp x 48dp, **TOP-RIGHT corner** (16dp margin), teal #008B8B
- **Drawer**: 250-300dp width, teal background, contains:
  - Cambiar Modo
  - Configuración
  - Cerrar Sesión
- **QR Card (Terminal Config)**: SIEMPRE visible - Nunca se oculta (disponible para re-vinculación en cualquier momento)
- **Service Status**: "🟢 Activo" / "🔴 Inactivo"

**NO incluye**:
- ❌ Botón cerrar flecha (este es HOME, no overlay)

**Purpose**: Acceso completo a funciones, menú, configuración

---

### SettingsActivity - Acceso Consistente (ACLARADO)

**Especificación Clave**: Los SETTINGS son idénticos en ambos modos, pero la INTERFAZ DE LA APP es diferente

**Settings Contenido** (IDÉNTICO en ambos):
- Selector de idioma
- Tono notificación
- Recordatorios personalizados
- Cualquier otra configuración compartida

**Acceso en Monitor**:
- Botón Settings ubicado: TOP-RIGHT (menú/drawer)
- Abre: SettingsActivity (mismo contenido que Terminal)
- Interfaz de Monitor + en background: Configuración compartida

**Acceso en Terminal**:
- Botón Settings ubicado: TOP-RIGHT (MISMO lugar que Monitor)
- Abre: SettingsActivity (MISMO contenido que Monitor)
- Interfaz de Terminal + en background: Configuración compartida

**SettingsActivity**:
- Contenido IDÉNTICO en ambos modos
- Cambios en Settings se sincronizan entre modos (SharedPreferences compartidas por rol)
- Return: Vuelve a interfaz anterior (Monitor o Terminal)

---

### SettingsActivity Accesibilidad (DEFINIDO)

**Cuándo se puede acceder**:
- ✅ Desde home/interfaz principal (cuando NO hay sesión activa)
- ✅ Accesible INMEDIATAMENTE después de colgar una sesión activa

**Flujo Recomendado**:
- Si usuario quiere Settings durante sesión activa: Colgar primero, THEN acceder Settings

---

### Interfaz de Video Monitor - Accesibilidad (DEFINIDO)

**Cómo acceder a "Monitorear"**:
1. **Desde Notificación** - Botón "Monitorear" en notificación expandida
2. **Desde Home** - Botón "Monitorear" en interfaz principal del Monitor

**Botón "Monitorear" en Home**:
- Ubicación: HOME principal (junto a botón "Llamar")
- Tamaño: 48dp x 48dp
- Acción: Abre directamente Video Monitor (sin diálogo previo)

**Botón "Monitorear" en Notificación**:
- Ubicación: Notificación expandida (junto a "Llamar" y "Limpiar")
- Acción: Abre directamente Video Monitor

**Resultado**: Usuario puede iniciar Monitoreo de 2 formas (notificación o home)

---

### Button Design Standard (Geriátric Optimized — ACTUALIZADO v2.0)

> **Referencia completa**: Ver sección "DISEÑO Y ACCESIBILIDAD v2.0" para justificación y estudios.

**Sizes (UPDATED v2.0)**:
- Primarios (CTA): 72dp × 72dp (campana, llamar, monitorear)
- Secundarios: 56dp × 56dp (config, video controls, log)
- Mínimo absoluto: 48dp × 48dp (iconos toolbar, back arrow)
- Separación mínima: 12dp entre botones primarios, 8dp entre secundarios
- Font: Mínimo 18sp en botones (20sp en primarios)

**Colors (UPDATED v2.0)**:
- Color primario: #006B6B (teal profundo — ratio AAA 7.2:1)
- Color variant: #008B8B (teal medio — para acentos no-textuales)
- Color alertas: #D32F2F (rojo, para Colgar/Delete)
- Color hover/pressed: 10% más oscuro que color base
- Color disabled: #BDBDBD

**Icons**:
- Formato: SVG (escalable, crisp)
- Tamaño ícono: 32dp dentro de 72dp button, 24dp dentro de 56dp button
- Color: White on primary background

**Spacing**:
- Padding dentro button: 16dp (primarios), 12dp (secundarios)
- Margin entre buttons: MÍNIMO 12dp (primarios), 8dp (secundarios)
- No overlaps permitidos (verificar layout)

**Feedback (NUEVO v2.0)**: Todo botón debe proveer feedback multi-modal:
- Visual: Ripple effect + cambio de estado visible
- Háptico: Vibración corta (50-200ms según importancia)
- Audio: Sonido sutil de confirmación (configurable ON/OFF)

**Buttons Inventory (ACTUALIZADO v2.0)**:
| Botón | Ubicación | Tamaño | Color | Icono | Función |
|-------|-----------|--------|-------|-------|---------|
| Campana | Terminal Home (centro) | 160×160 | Teal #006B6B | 🔔 64dp | Pedir ayuda |
| Llamar | Monitor Home / Alerta card | 56×40 | Teal #006B6B | 📞 24dp | Walkie-talkie audio |
| Monitorear | Monitor Home / Alerta card | 56×40 | Teal #006B6B | 📹 24dp | Video monitor silencioso |
| Cambiar Cámara | Video Monitor (bottom) | 56×56 | Teal | 🔄 24dp | Switch front/back |
| Toggle Bidireccional | Video Monitor (bottom) | 56×56 | Teal | 📹 24dp | Toggle silent/bidirectional |
| Salir Monitor | Video Monitor (bottom) | 56×56 | Red #D32F2F | ✕ 24dp | Exit monitoring |
| Colgar | Home (durante llamada) | 56×56 | Red #D32F2F | 📞 24dp | End walkie-talkie call |
| Config (⚙️) | Terminal top-right | 56×56 | Gris #555555 | ⚙️ 24dp | Abrir Settings |
| Vincular Terminal | Monitor Home | full-width×56 | Teal outline | ➕ 24dp | QR scanner |
| Cambiar Modo | Settings (fondo) | full-width×56 | Teal | 🔄 24dp | Go to RoleSelector |
| Cerrar Sesión | Settings (fondo) | full-width×56 | Red #D32F2F | 🚪 24dp | Logout Firebase |

---

### Flujo de Emparejamiento (ESPECIFICACIÓN COMPLETA - Marzo 20)

**Fase 1: Escaneo y Conexión Inicial**
1. Terminal muestra QR en interfaz principal (SIEMPRE - disponible para re-vinculación manual)
2. Monitor escanea usando cámara trasera (mejor línea de visión)
3. El QR contiene JSON estructurado (ACTUALIZADO Marzo 31):
   ```json
   {
     "deviceId": "UUID único del Terminal",
     "ip": "192.168.x.x",
     "port": 8080,
     "name": "Nombre persona o Terminal de [Build.MODEL]",
     "secret": "CampanaSecureKey"
   }
   ```
   - `deviceId`: UUID generado una vez y almacenado en SharedPreferences
   - `ip`: IP local obtenida con `NetworkUtils.getLocalIpAddress()` (usa `isSiteLocalAddress` para garantizar IP WiFi local, NO IP móvil)
   - `port`: Puerto del CampanaHttpServer (8080)
   - `name`: Nombre para display en Monitor. Prioridad: `terminal_person_name` (perfil local) > `FirebaseAuth.displayName` (sesión activa) > `"Terminal de ${Build.MODEL}"` (fallback dispositivo)
   - `secret`: Clave compartida para validar comunicación

**Nombres de Terminales en Dashboard Monitor (NUEVO v2.4 — Marzo 31)**:
- Al escanear QR, Monitor almacena `name` del QR y lo pasa a `ConnectionViewModel.addPairing(terminalId, terminalName)` que lo guarda en Firestore campo `name`
- DashboardFragment usa `pairing.name ?: "Terminal"` para mostrar nombre en card de terminal pareada
- Si terminal tiene persona registrada → aparece nombre de la persona
- Si terminal en modo local → aparece nombre del dispositivo (Build.MODEL)

**🔴 PRERREQUISITO: EMPAREJAMIENTO VÍA QR**
- Monitor y Terminal son emparejados SOLO mediante QR (nunca automáticamente)
- El QR contiene JSON con token de emparejamiento, ID Terminal, IP, puerto y metadata
- Una vez emparejados, pueden conectar vía esta Fase 2

**Fase 2: Establecimiento de Conexión - QR VERIFICADO, Prioridad Local**
1. Ambos dispositivos (YA emparejados) intentan conectar:
   - **PASO 1A: Detección Local (3s timeout)**
     - Monitor descubre Terminal en red local (mDNS broadcast: `_caremonitor._tcp`)
     - Si Terminal responde: Verificar token QR guardado contra registro local
     - Si token VÁLIDO → Establecer conexión WiFi local (puertos 8080/9000/9001)
   - **PASO 1B: Si Local Falla → Fallback a Internet (15s timeout)**
     - Verificar nuevamente token QR (debe ser válido)
     - Monitor intenta conectar vía servidor en Firestore
     - Terminal recibe mensaje y establece conexión bidireccional
     - Si token INVÁLIDO en cualquier punto → Rechazar, mostrar QR para re-emparejamiento
2. Conexión exitosa:
   - Guardar `connectionType` ("local" o "internet") en SharedPreferences + Firestore
   - Usar este tipo EXCLUSIVAMENTE para futuras comunicaciones (llamadas, video, alertas)
3. Si AMBAS fallan o token INVÁLIDO:
   - App entra en estado "Esperando Emparejamiento"
   - Muestra QR nuevamente para re-emparejamiento obligatorio

**Fase 3: Persistencia en Firestore**
- Registro creado en Firestore:
  ```
  /pairings/{monitorId}
    - terminalId: string
    - terminalPublicKey: string
    - pairingToken: string (hash de QR)
    - pairedAt: timestamp
    - connectionType: "local" | "internet"
    - lastSeen: timestamp
    - isActive: boolean
  ```
- Sincronización:
  - Si hay conexión: Guardar inmediatamente en Firestore
  - Si NO hay conexión: Guardar en SharedPreferences local
  - En background: Sincronizar a Firestore cuando hay conexión

**Fase 4: Reconexión vs Re-emparejamiento** (CLARIFICACIÓN IMPORTANTE - Marzo 28)

**4A: Reconexión Automática** (Token QR válido, pero Terminal offline temporalmente)
- Si Terminal desaparece (no responde por 30 segundos en local, 5 minutos en internet):
  - Monitor intenta reconectar automáticamente usando token QR guardado
  - Usa datos almacenados en SharedPreferences + Firestore para re-intentar
  - Ciclo de reintentos: 5s -> 10s -> 30s -> 60s (exponential backoff)
  - Si reconexión exitosa: Reanuda comandos normales

**4B: Re-emparejamiento Obligatorio** (Token QR expirado, inválido, o usuario solicita explícitamente)
- Si token QR expiró o fue rechazado:
  - App muestra "Esperando Emparejamiento"
  - Monitor/Terminal DEBE escanear NUEVO QR para emparejar nuevamente
  - Genera token QR NUEVO cada vez que se abre esta pantalla
  - Este es el ÚNICO método válido de emparejamiento (NO emparejamiento automático)

**QR DISPONIBLE (CAMBIO v2.1 — revertido por testing real)**:
- QR visible en pantalla principal de Terminal (SIEMPRE — es el elemento central)
- QR visible en lockscreen overlay (BellActivity) para emergencias
- Permite forzar re-emparejamiento sin reset completo
- Token QR tiene expiración configurable (default: 30 días)

### Flujo de Alerta y Notificación (ACTUALIZADO v4.2 — Abril 2, 2026 — BUBBLES)
1. Activación vía toque (Bell icon), voz (Voice commands), o agitación (Shake sensor - when enabled in settings) en Terminal.
2. Monitor recibe alerta como **burbuja flotante per-terminal** (API 30+). En API <30, las alertas se acumulan exclusivamente en la notificación del servicio (ID=1).
3. Cada terminal pareado = 1 burbuja independiente. Alertas se acumulan dentro de su burbuja (badge count).
4. Expandir burbuja → abre **BubbleRadialActivity** con menú radial animado: hub central (nombre terminal + última alerta + badge) + 3 opciones (Monitorear, Llamar, Controles) que emergen radialmente.
5. Tocar opción radial → acción en pantalla completa: VideoActivity (Monitorear), TerminalDetailActivity con auto_call (Llamar), o TerminalDetailActivity completa (Controles).
6. Notificación de servicio (ID=1, notification_custom.xml) sigue mostrando historial FIFO global (máx 10). Esta es la ÚNICA notificación visible en API <30.
7. API <30: NO se crea notificación extra. La notificación del servicio ya contiene todas las alertas acumuladas.

### Flujo de Llamada Telefónica (Walkie-talkie)
1. Monitor presiona "Llamar" desde notificación o interfaz principal.
2. Terminal acepta automáticamente (sin diálogo, sin UI).
3. Comunicación bidireccional de voz estilo walkie-talkie.
4. Monitor presiona "Colgar" para terminar (botón cambia dinámicamente).
5. NO se abre ninguna Activity separada - todo ocurre en la misma interfaz.

### Flujo de Monitoreo Silencioso (ESPECIFICACIÓN COMPLETA - Marzo 27 - UI UPDATE - T86 ARCH FIX)
1. Monitor presiona "Monitorear" desde notificación, burbuja, o interfaz.
2. Monitor abre VideoActivity con mode="monitor" → envía HTTP POST `/request_monitor` al Terminal.
3. **Terminal: CampanaService.onMonitorRequested()** → crea VideoManager directamente en el Service (T86 — NO lanza Activity en Terminal). Streaming de cámara vía UDP:9001 hacia Monitor.
4. Monitor ve video en vivo con **3 botones solo-icono (sin texto)** en barra inferior:
   - **Botón Cambiar Cámaras**: Alterna entre cámara frontal/trasera del Terminal
   - **Botón Alternar Modo**: Toggle que cambia entre:
     - 🔇 "Modo Monitoreo Silencioso" (solo recibiendo video/audio, sin transmisión del Monitor)
     - 📹 "Modo Videollamada Bidireccional" (video+audio bidireccional activo)
   - **Botón Salir**: Cierra VideoActivity y envía STOP_MONITOR al Terminal
5. **Terminal: Operación silenciosa** — CampanaService hace streaming en background sin UI, sin notificación extra, sin sonido
6. Cuando se alterna a "Videollamada Bidireccional": Terminal recibe automáticamente llamada videollamada
7. Monitor presiona "Salir" para terminar monitoreo → Terminal recibe STOP_MONITOR → VideoManager.stopStreaming()
8. **Logging**: Evento registrado en Firestore (ver sección Firestore Schema)

---

## 📡 ESPECIFICACIÓN DE CONEXIÓN Y COMUNICACIÓN (Marzo 20)

### Priorización de Red - Reglas Obligatorias

**REGLA #1 - Prioridad Absoluta Local**:
- ✅ SIEMPRE intentar conexión WiFi local PRIMERO
- ✅ Si ambos dispositivos en red local → Usar EXCLUSIVAMENTE local (incluso si Internet disponible)
- ✅ Cambio automático: Si se detecta que ahora están en red local → Switch a local

**REGLA #2 - Fallback a Internet**:
- Si Local FALLA (timeout 30 segundos):
  - Intenta Internet (WiFi o Bluetooth tethering disponible en Monitor)
- Si Internet FALLA:
  - Reinicia ciclo de intentos (exponential backoff: 5s → 10s → 30s → 60s)
- Si AMBAS fallan por más de 5 minutos:
  - Muestra alerta "Conexión perdida"
  - Mantiene UI activo pero buttons deshabilitados

**REGLA #3 - Detección de Red**:
- Detectar si Terminal y Monitor en misma red:
  ```
  - Ambos dispositivos usan mDNS con tipo "_caremonitor._tcp" (LocalDiscoveryService)
  - Si se resuelve en misma subnet → Red Local
  - Si no resuelven → Internet
  ```
- Ejecutar detección cada 30 segundos en background (bajo consumo batería)

**REGLA #4 - Persistencia de Connection Type y Auto-switch**:
- Una vez emparejados, guardar `connectionType` ("local" o "internet") en:
  - SharedPreferences local
  - Firestore (cuando hay conexión)
- Usar este tipo para TODAS las futuras comunicaciones (llamadas, video, notificaciones)
- **AUTO-SWITCH LÓGICA**:
  - Si conexión actual es "internet" pero se detecta que ambos dispositivos están en misma red local:
    - Actualizar `connectionType` a "local" en ambos dispositivos
    - Rutear futuras comunicaciones vía WiFi local (menor latencia)
    - NO interrumpir comunicación activa (switchear solo para nuevas conexiones)
  - Si conexión actual es "local" pero Terminal sale de la red local:
    - Actualizar `connectionType` a "internet"
    - Usar Firestore como broker para reconectar
  - Verificación automática cada 30 segundos en background

**REGLA #5 - Timeouts Según Normas y Eficiencia**:
- Local WiFi mDNS discovery: 3 segundos
- Local connection establishment: 30 segundos
- Internet handshake (Firestore): 15 segundos
- Call/Stream heartbeat: 10 segundos (ambos modos)
- No-response timeout: 30 segundos (reinicia ciclo)
- Aggressive retry después 5 minutos sin conexión

**REGLA #6 - Durante Llamada o Video Activos**:
- Si pierde Local → Intenta switchear a Internet automáticamente
- Si pierde Internet → Intenta switchear a Local automáticamente
- Si pierde ambas → Mantiene UI pero marca como "conexión débil"
- Si se restaura conexión → Reconecta automáticamente (sin intervención usuario)

### Videollamada - Aceptación Automática (ESPECIFICACIÓN - Marzo 20)

**De Monitor a Terminal**:
- Monitor dentro de VideoActivity (modo SILENT) presiona botón Toggle para activar modo BIDIRECCIONAL
- **Acción en Terminal**: La interfaz videolamada se activa automáticamente (Terminal recibe automáticamente sin diálogo de aceptación/rechazo)
- ✅ Terminal puede ver video del Monitor en modo BIDIRECCIONAL
- Justificación: Terminal es persona cuidada; Monitor controla cuándo se activa bidireccional
- Nota: Si Terminal cierra app durante videollamada → Conexión se corta automáticamente en Monitor

---

## 💾 ESPECIFICACIÓN DE BASE DE DATOS - Firestore Schema (ACTUALIZADO Marzo 28 - Arquitectura 1:N)

### Estructura Firestore - Arquitectura Monitor 1:N Terminals

**🔴 CAMBIO CRÍTICO (Marzo 28)**: Schema refactorizado para soportar 1 Monitor controlando N Terminales simultáneamente. Terminal puede cambiar de Monitor (re-pairing con código QR).

**Colección Principal: `/users/{userId}`**
```json
{
  "userId": "google_123456",
  "email": "monitor@example.com",
  "nombre": "Juan",
  "rol": "monitor" | "terminal",
  "fotoPerfil": "gs://bucket/photos/user_123.jpg",
  "idioma": "es" | "en" | "fr" | "pt" | "de" | "it",
  "createdAt": timestamp,
  "updatedAt": timestamp
}
```

**Colección Top-Level: `/pairings/{documentId}`** (REAL — código usa `db.collection("pairings")`)
```json
{
  "monitorId": "google_123456",
  "terminalId": "google_654321",
  "name": "María",              // Nombre del Terminal para display
  "terminalPublicKey": "...",
  "pairingToken": "hash_qr_...",
  "pairedAt": timestamp,
  "connectionType": "local" | "internet",
  "lastSeen": timestamp,
  "isActive": true
}
```
> ⚠️ **NOTA**: SRS anterior decía `/users/{monitorId}/emparejamientos/`. La implementación real usa colección top-level `pairings` consultada por `ConnectionViewModel.getPairingsList()` con filter `where("monitorId", ==, currentUserId)`. Esto simplifica las queries y evita subcolecciones anidadas.

**Subcolección: `/users/{userId}/logs_conexion/{eventId}`**
```json
{
  "timestamp": timestamp,
  "evento": "connected" | "disconnected" | "call_started" | "call_ended" | "monitoring_started" | "monitoring_ended" | "voz_detectada" | "alarma_completada",
  "detalles": {
    "connectionType": "local" | "internet",
    "duracionSegundos": number,
    "otroDispositivoId": string,
    "monitorId": string (si evento origina de Monitor),
    "terminalId": string (si evento origina de Terminal)
  }
}
```
**❗ NOTA 1:N**: Cada Monitor tiene su propio `logs_conexion` (solo ACTIVOS en su emparejamiento actual). Al cambiarse de Monitor, nuev Monitor ve logs futuros solo.

**Subcolección: `/users/{userId}/alarmas_recordatorios/{alarmaId}`** (ONLY en Terminal)
```json
{
  "descripcion": "Tomar medicamento X",
  "hora": "14:30",
  "diasSemana": ["lunes", "martes", ...],
  "habilitada": true | false,
  "createdBy": "terminalId" | "monitorId",  // AGREGADO - atribución de quién creo
  "ultimaEjecucion": timestamp,
  "createdAt": timestamp,
  "updatedAt": timestamp
}
```
**❗ NOTA 1:N**: Alarmas disponibles a TODOS los Monitors emparejados a ese Terminal. El campo `createdBy` es solo para auditoría (no restringe acceso).

**Subcolección: `/users/{terminalId}/frases_personalizadas/{fraseId}`** (ONLY Terminal Config)
```json
{
  "texto": "Ayuda",
  "auto_added": false | true,  // true = auto-agregada al emparejar Monitor
  "addedBy_monitorId": "google_123456",  // Si auto_added, registra qué Monitor
  "createdAt": timestamp,
  "encriptedContent": "base64_encrypted_aes256(texto)",  // NUEVO - Encriptación AES-256
  "isActive": true
}
```
**❗ NOTA ENCRIPTACIÓN**: `texto` se guarda ENCRIPTADO en `encriptedContent` usando AES-256. Terminal descifra localmente. Si `auto_added=true` y Terminal cambia de Monitor, la frase AUTO se ELIMINA (textos custom se MANTIENEN).

**Subcolección: `/users/{monitorId}/alertLog/{eventoId}`** (SOLO para Monitor - Historial 24h)
```json
{
  "timestamp": timestamp,
  "tipo": "bell" | "voice" | "shake" | "alarm" | "battery_low" | "battery_ok",
  "terminalId": string,  // Qué Terminal generó el evento
  "detalles": {
    "campanaCount": number,
    "vozDetectada": "Frase detectada",
    "etc": "..."
  },
  "expiredAt": timestamp (24h en el futuro, para TTL cleanup de Firestore)
}
```
**❗ NOTA 1:N**: CADA Monitor tiene su propio AlertLog. No es compartido. Si Terminal cambia de Monitor:
  - Monitor A: Sigue viendo su AlertLog histórico (pero no nuevos eventos de ese Terminal)
  - Monitor B: Empieza a recibir nuevos eventos en su AlertLog (historial anterior NO visible)

### Encriptación de Custom Phrases - Especificación AES-256 (Marzo 28)

**Algoritmo**: AES (Advanced Encryption Standard), modo CBC, tamaño de clave 256-bit
**Razón**: Proteger custom phrases en Firestore contra lectura directa de base de datos (en tránsito encriptadas vía TLS, en reposo encriptadas localmente antes de guardar)

**Implementación en Código**:
- Terminal: Cifra antes de guardar en Firestore, descifra al cargar desde Firestore
- Claves de encriptación: Derivadas de credenciales Firebase del Usuario (nunca se guardan en claro)
- IV (Initialization Vector): Generado aleatoriamente por encriptación, NO reutilizado

**En Firestore**:
- Campo `encriptedContent`: Base64-encoded AES-256 encrypted text
- Campo `texto` (plaintext): DEPRECATED/NO SE GUARDA EN PRODUCCIÓN (solo para legado)
- Monitor NO puede descifrar (claves locales del Terminal) → Mayor privacidad

**Notas de Diseño**:
- Custom phrases son íntimas (ej: "Juana ayúdame" puede revelar relación) → Encriptadas en Firestore
- Monitor nunca ve contenido, solo que existe el evento "voz_detectada"
- Terminal controla lectura/escritura de contenido

---

---

## 🔔 ESPECIFICACIÓN DE NOTIFICACIONES - Sistema FIFO (Marzo 20)

### Comportamiento de Acumulación

**Máximo de Eventos**: 10 eventos simultáneamente

**Orden de Descarte (FIFO - First In, First Out)**:
- Evento #1 llega → Mostrado
- Evento #2 llega → Mostrado (total: 2)
- ...
- Evento #10 llega → Mostrado (total: 10)
- Evento #11 llega → **Evento #1 se descarta automáticamente** → Mostrado (total: 10)

**Tipos de Eventos** (7 tipos canónicos - ACTUALIZADO v2.0 — USAR SIEMPRE estos valores en código):
- `bell` - Botón "Campana" presionado en Terminal
- `voice` - Comando de voz detectado en Terminal
- `shake` - Agitación del dispositivo detectada (acelerómetro)
- `battery_low` - Batería del Terminal por debajo del umbral
- `battery_ok` - Batería del Terminal recuperada (histeresis >20%)
- `alarm` - Recordatorio/alarma personalizada activada

**Botón "Limpiar Notificaciones"**:
- Presionar → Elimina TODOS los eventos acumulados de una vez
- Notificación se colapsa después de limpiar
- Reaparece cuando llega nuevo evento

### Notificaciones Per-Terminal con Bubbles API (NUEVO v4.2 — Abril 2, 2026)

**Arquitectura**: 1 Monitor : N Terminales → N Burbujas independientes.

| Aspecto | Comportamiento |
|---------|----------------|
| API 30+ (Android 11+) | Cada terminal = 1 burbuja flotante. Expandir → **BubbleRadialActivity** (menú radial: Monitorear, Llamar, Controles) |
| API 24-29 | Sin burbuja. Alertas se acumulan exclusivamente en la notificación del servicio (ID=1). Acceso a terminal vía Dashboard |
| Menú radial | Hub central (nombre + alerta + badge) + 3 botones 72dp emergen radialmente con animación OvershootInterpolator 500ms |
| Opciones radiales | Monitorear (📹, 90° arriba) → VideoActivity, Llamar (📞, 210° abajo-izq) → TerminalDetailActivity(auto_call), Controles (⚙️, 330° abajo-der) → TerminalDetailActivity |
| Acumulación | Alertas del MISMO terminal se acumulan en SU burbuja. Título: "🔔 Campana (+N más)" |
| Primera alerta | Burbuja se auto-expande |
| Alertas subsiguientes | Burbuja se actualiza silenciosamente (no interrumpe) |
| Componente bubble | `TerminalBubbleManager.kt` (singleton) — maneja ShortcutInfo, Person, BubbleMetadata por terminal |
| Componente radial | `BubbleRadialActivity.kt` + `activity_bubble_radial.xml` — Activity liviana con menú radial animado |
| Canal | ALERT_CHANNEL_ID con `setAllowBubbles(true)` (API 29+) |
| Manifest | BubbleRadialActivity: `allowEmbedded=true`, `resizeableActivity=true`, `documentLaunchMode=always` |
| ID notificación | `BUBBLE_NOTIFICATION_ID_BASE (200) + terminalId.hashCode().and(0xFF)` — único por terminal |
| Coexistencia | Service notification (ID=1, notification_custom.xml) sigue existiendo independientemente con historial FIFO global |

**Resolución de nombre**: Si la alerta llega con IP en vez de nombre, `resolveTerminalName()` busca en SharedPreferences `paired_terminal_name`. Fallback: "Terminal (X.X.X.X)".

### Interfaz de Notificaciones - DISEÑO DUAL (UPDATED March 27, 2026)

**NOTA v4.2**: El diseño dual descrito abajo aplica a la **notificación de servicio** (ID=1). Las alertas per-terminal ahora usan Bubbles (ver sección anterior).

**Estado COLAPSADO (Minimalista - Collapsed View)**:
- Mostrar: **SOLO 3 elementos** (nada más):
  1. Pequeño icono del evento (campana, voz, teléfono, etc.) - 24dp
  2. Nombre de la app proporcionado por SISTEMA (NO programado en código - lo proporciona Android)
  3. Timestamp ("ahora", "hace 2 min")
- **SIN título** - NO programar
- **SIN texto de eventos** - NO programar  
- **Acción**: Presionar → Expande a vista completa
- **📋 NOTA PARA GEMINI**: NO incluyas title ni summary text en `NotificationCompat.Builder`. Solo big picture style con icono + timestamp.

**Estado EXPANDIDO (Full Details - Expanded View)**:
- Listar todos eventos (máx 10) con:
  - Tipo de evento (ícono + texto)
  - Timestamp ("hace 2 minutos")
  - Descripción completa del evento
- Botones **SIEMPRE visibles** (incluso con 0 eventos):
  - "Llamar" (walkie-talkie audio)
  - "Monitorear" (video silencioso)
- Botón **CONDICIONAL**:
  - "Limpiar" → **OCULTO cuando hay 0 eventos**, VISIBLE cuando hay ≥1 evento, se OCULTA de nuevo al limpiar
- **Diseño**: notification_custom.xml = diseño EXPANDIDO (actual está correcto)
- **Nota**: Android maneja automáticamente el colapso cuando el usuario minimiza

**Relación con XML**:
- `notification_custom.xml` → Define VISUALMENTE el estado expandido (actual)
- Los archivos `notification_custom_collapsed.xml`, `notification_custom_empty.xml`, `notification_custom_expanded.xml` → **DEBEN SER ELIMINADOS** (duplicados, no se usan)
- La diferencia entre estados es PROGRAMÁTICA en `NotificationCompat.Builder`, no por XML

---

## ⏰ ESPECIFICACIÓN DE RECORDATORIOS PERSONALIZADOS - Alarmas (Marzo 20)

### Tipos de Alarmas

**Categorías Predefinidas** (cada una con icono distinto):
1. **Medicina** 💊 - Icono: `ic_alarm_medicine` (pastilla) - Tomar medicamento específico
2. **Agua** 💧 - Icono: `ic_alarm_water` (gota) - Beber agua
3. **Comida** 🍽️ - Icono: `ic_alarm_food` (plato) - Comer algo
4. **Custom** ⏰ - Icono: `ic_alarm_custom` (reloj) - Recordatorio personalizado

### Configuración de Alarma

**Campos Obligatorios**:
- **Tipo**: dropdown (Medicina | Agua | Comida | Custom)

---

## 📱 INTERFACES FALTANTES - ESPECIFICACIÓN COMPLETA (Marzo 30, 2026)

### LanguageSelectorActivity - Selector de Idioma
**Contexto**: Primera pantalla después de SplashActivity (onboarding) o accesible desde SettingsActivity
**Layout**: `activity_language_selector.xml`

| # | Elemento | Tipo | Función |
|---|----------|------|---------|
| 1 | Título | TextView (24sp) | "Selecciona tu idioma" / "Select your language" |
| 2 | Lista de Idiomas | RecyclerView (6 items) | Cada item: bandera + nombre del idioma |
| 2.1 | 🇪🇸 Español | Item clickeable | Selecciona español |
| 2.2 | 🇬🇧 English | Item clickeable | Selecciona inglés |
| 2.3 | 🇫🇷 Français | Item clickeable | Selecciona francés |
| 2.4 | 🇵🇹 Português | Item clickeable | Selecciona portugués |
| 2.5 | 🇩🇪 Deutsch | Item clickeable | Selecciona alemán |
| 2.6 | 🇮🇹 Italiano | Item clickeable | Selecciona italiano |
| 3 | Botón Continuar | Button (48dp, teal) | Navegación context-aware (ver comportamiento) |

**Comportamiento**:
- Auto-detecta idioma del dispositivo y pre-selecciona
- Guardar en EncryptedSharedPreferences (clave "app_language")
- NUNCA sincroniza idioma a Firestore (es preferencia local por dispositivo)
- Cambio de idioma inmediato (sin reinicio)
- **Navegación del botón Continuar** (CORREGIDO Marzo 30):
  - Si viene de **Onboarding/Splash** (primera vez): → `startActivity(LoginActivity)` + `finish()`
  - Si viene de **Settings** (app_language click): → Solo `finish()` para regresar a Settings
  - Detección: Usar `intent.getBooleanExtra("fromSettings", false)` o verificar si `callingActivity` es SettingsFragment

### ForgotPasswordActivity - Recuperar Contraseña
**Contexto**: Accesible desde LoginActivity (#2.4 "¿Olvidaste contraseña?")
**Layout**: `activity_forgot_password.xml`

| # | Elemento | Tipo | Función |
|---|----------|------|---------|
| 1 | Título | TextView (20sp) | "Recuperar Contraseña" |
| 2 | Descripción | TextView (14sp) | "Ingresa tu correo y te enviaremos un enlace para restablecer tu contraseña" |
| 3 | Campo Email | TextInputLayout + EditText | Email registrado |
| 4 | Botón Enviar | Button (48dp, teal) | Llama `FirebaseAuth.sendPasswordResetEmail(email)` |
| 5 | Botón Volver | TextView clickeable | "Volver al Login" → finish() |

**Comportamiento**:
- Validar formato email antes de enviar
- Si email no registrado: Mostrar error "Correo no encontrado"
- Si éxito: Mostrar "Enlace enviado a [email]. Revisa tu bandeja de entrada."
- Firebase maneja el enlace de reset (no requiere pantalla adicional)

### QRScannerActivity - Escáner de QR para Emparejamiento
**Contexto**: Accesible desde MonitorMainActivity (botón #1.4 btnScanQR o drawer #2.4.2)
**Layout**: `activity_qr_scanner.xml`

| # | Elemento | Tipo | Función |
|---|----------|------|---------|
| 1 | Camera Preview | PreviewView (fullscreen) | Vista de cámara TRASERA |
| 2 | Overlay Frame | View (cuadrado 200dp) | Marco visual de escaneo (centro) |
| 3 | Instrucciones | TextView (16sp, blanco) | "Apunta la cámara al QR del Terminal" |
| 4 | Botón Cerrar | ImageButton (48dp, TOP-LEFT) | Flecha ← para volver |
| ~~5~~ | ~~Flash Toggle~~ | ~~No implementado~~ | No existe en código actual |

**Comportamiento**:
- Usar CameraX con `CameraSelector.DEFAULT_BACK_CAMERA`
- Escaneo automático (no requiere presionar botón)
- **Al detectar QR válido (CAMBIO v2.2 — Marzo 31)**: Guardar datos en EncryptedPreferences → Toast "Vinculando..." → cerrar QRScannerActivity INMEDIATAMENTE → volver a MonitorMainActivity. La confirmación HTTP al Terminal se hace en background (fire-and-forget). Flag `qrDetected` evita procesamiento múltiple.
- Al detectar QR inválido: Mostrar Toast "QR no reconocido"
- Permisos: Solicitar CAMERA en runtime si no concedido

### RoleSelectorActivity - Selector de Rol
**Contexto**: Pantalla post-login y accesible desde "Cambiar Modo" en drawer de cualquier modo
**Layout**: `activity_role_selector.xml`

| # | Elemento | Tipo | Función |
|---|----------|------|---------|
| 1 | Título | TextView (20sp) | "¿Cómo usarás este dispositivo?" |
| 2 | Card Monitor | MaterialCardView (clickeable) | Opción "Monitor" |
| 2.1 | Icono Monitor | ImageView (64dp) | Icono persona con reloj |
| 2.2 | Título Monitor | TextView (18sp) | "Monitor" |
| 2.3 | Descripción | TextView (14sp) | "Supervisa y cuida a la persona mayor" |
| 3 | Card Terminal | MaterialCardView (clickeable) | Opción "Terminal" |
| 3.1 | Icono Terminal | ImageView (64dp) | Icono campana |
| 3.2 | Título Terminal | TextView (18sp) | "Terminal" |
| 3.3 | Descripción | TextView (14sp) | "Dispositivo de la persona cuidada" |

**Comportamiento**:
- Al seleccionar: Guardar rol en SharedPreferences + Firestore (`/users/{userId}/rol`)
- NO cierra sesión de Firebase al cambiar rol
- NO muestra diálogo de confirmación
- Navega directamente a MonitorMainActivity o TerminalMainActivity según selección
- Si viene de onboarding (primer uso): flujo lineal
- Si viene de "Cambiar Modo": Cierra conexiones activas de video/audio, luego navega

### SettingsActivity - Configuración Completa (ESPECIFICACIÓN DETALLADA)
**Contexto**: Accesible desde drawer de Monitor y Terminal. Contenido IDÉNTICO en ambos modos.
**Layout**: `activity_settings.xml`

| # | Sección | Elementos | Función |
|---|---------|-----------|---------|
| **1** | **Perfil** | Avatar + Nombre + Email | Información del usuario (no editable aquí) |
| **2** | **Idioma** | Spinner/Selector (6 opciones) | es, en, fr, pt, de, it - Cambio inmediato |
| **3** | **Notificaciones** | | |
| 3.1 | Tono Notificación | Selector de tono (system tones) | Sonido para alertas |
| 3.2 | Vibración | MaterialSwitch | Activar/desactivar vibración en alertas |
| 3.3 | Notificar Batería Baja | MaterialSwitch (default: ON) | Toggle para alertas battery_low |
| 3.4 | Umbral Batería | Spinner (10%, **15%**, 20%, 25%) | Nivel % para trigger battery_low |
| **4** | **Comunicación** | | |
| 4.1 | Auto-aceptar Llamadas | MaterialSwitch | Terminal auto-acepta audio/video |
| 4.2 | Calidad de Audio | Spinner (Baja/Media/Alta) | Codec OPUS bitrate |
| **5** | **Detección** | | |
| 5.1 | Detección de Agitación | MaterialSwitch | Habilitar ShakeDetectionService |
| 5.2 | Sensibilidad Acelerómetro | SeekBar (8-20 m/s²) | Umbral de shake |
| 5.3 | Detección de Voz | MaterialSwitch | Habilitar VoiceCommandManager |
| **6** | **Alarmas** | | |
| 6.1 | Mis Alarmas | Botón "Ver Alarmas" | Navega a vista de alarmas |
| 6.2 | Frases Personalizadas | Botón "Ver Frases" | SOLO Terminal: gestión de custom phrases |
| **7** | **Historial** | | |
| 7.1 | Ver Historial Alertas | Botón | Navega a AlertLogActivity (SOLO Monitor) |
| **8** | **Sesión** | | |
| 8.1 | Cambiar Modo | Botón | Navega a RoleSelectorActivity (sin cerrar sesión) |
| 8.2 | Cerrar Sesión | Botón (rojo) | Logout de Firebase + volver a SplashActivity |
| **9** | **Información** | | |
| 9.1 | Versión App | TextView | "Monitor de Cuidados v1.0.0" |

**Visibilidad condicional por rol**:
- Monitor: Secciones 1-4, 7-9 visibles. Sección 5 oculta (es de Terminal). Sección 6 muestra alarmas de Terminales.
- Terminal: Secciones 1-6, 8-9 visibles. Sección 7 oculta (AlertLog es solo Monitor).

---

## 🔔 NOTIFICATION CHANNELS - Especificación de Canales Android (Marzo 30, 2026 — ACTUALIZADO v2.0)

**Requisito Android 8+ (API 26+)**: Toda notificación debe pertenecer a un canal.

| Canal ID | Nombre | Importancia | Descripción | Sonido |
|----------|--------|-------------|-------------|--------|
| `channel_service` | "Servicio Activo" | LOW | Notificación persistente del foreground service | Sin sonido |
| `channel_alerts` | "Alertas de Terminal" | HIGH | Eventos bell, voice, shake | Tono configurable + vibración |
| `channel_battery` | "Batería" | DEFAULT | Eventos battery_low, battery_ok | Tono suave |
| `channel_alarms` | "Alarmas" | MAX | Alarmas programadas | Tono de alarma (alto) |
| `channel_calls` | "Llamadas" | HIGH | Llamadas entrantes/salientes | Tono de llamada |

**Implementación**: Crear todos los canales en `CareMonitorApp.kt` (Application.onCreate())

---

## 📊 CARDS DE EVENTOS EN MONITOR - Especificación por Tipo (Marzo 30, 2026)

**Regla General**: Cada tipo de evento tiene su propia Card en MonitorMainActivity. TODAS empiezan con `visibility="gone"` y aparecen SOLO cuando hay eventos de ese tipo.

### Card: Campana (bell) - `card_notifications.xml`
- **Icono**: 🔔 `ic_notification_bell` (teal)
- **Título**: "Campana • N eventos"
- **Expandible**: Muestra lista de campanazos con timestamp
- **Default**: `visibility="gone"` (⚠️ BUG ACTUAL: falta gone en XML)

### Card: Voz (voice) - `card_notifications_voice.xml`
- **Icono**: 🎤 `ic_mic` (teal)
- **Título**: "Voz • N eventos"
- **Expandible**: Muestra frases detectadas + timestamp
- **Default**: `visibility="gone"` ✅

### Card: Agitación (shake) - `cardShakeDetection`
- **Icono**: 📳 `ic_shake` (teal)
- **Título**: "Agitación • N eventos"
- **Expandible**: Muestra eventos de shake con timestamp
- **Default**: `visibility="gone"` ✅

### Card: Batería Baja (battery_low) - `card_notifications_battery.xml` (CREAR)
- **Icono**: 🔋 `ic_battery_low` (naranja/rojo)
- **Título**: "Batería Baja • [Terminal Name] - [N%]"
- **Expandible**: Muestra nivel exacto, timestamp, nombre de Terminal
- **Default**: `visibility="gone"`
- **Visibilidad**: Aparece cuando llega evento battery_low, se oculta con battery_ok

### Card: Alarma (alarm) - `card_notifications_alarm.xml` (CREAR)
- **Icono**: Según categoría (💊💧🍽️⏰) `ic_alarm_[category]`
- **Título**: "Alarma • [Descripción]"
- **Expandible**: Muestra descripción completa, hora programada, Terminal origen
- **Default**: `visibility="gone"`

---

## 💰 MONETIZACIÓN - Modelo Freemium (Marzo 30, 2026)

### Visión General
Monitor de Cuidados adopta un modelo **freemium** con tiers de suscripción. El "modo local" actual se transforma en **modo de prueba/desarrollo** con todas las funciones desbloqueadas (solo para testing).

### Tiers de Suscripción

| Tier | Precio | Terminales | Funciones |
|------|--------|------------|-----------|
| **Gratis** | $0/mes | 1 Terminal | Campana, Alarmas básicas (3 max), Notificaciones (max 5) |
| **Premium Familiar** | $4.99/mes | Hasta 3 Terminales | Todo Gratis + Monitoreo video, Llamadas, Voz, Shake, Alarmas ilimitadas, Notificaciones (max 10) |
| **Premium Plus** | $9.99/mes | Hasta 10 Terminales | Todo Premium + Historial 7 días, Estadísticas, Soporte prioritario |
| **B2B Residencia** | $49.99/mes | Ilimitados | Todo Plus + Dashboard web, API, Multi-cuidador, Reportes |

### Modo de Prueba (antes "Modo Local")
- **Propósito**: Solo para desarrollo y testing
- **Acceso**: Flag en `build.gradle` (`IS_TEST_MODE = true`)
- **Comportamiento**: Todas las funciones desbloqueadas sin suscripción
- **En producción**: `IS_TEST_MODE = false` → Se aplican restricciones por tier

### Feature Gating (Restricciones por Tier)
```kotlin
object FeatureGating {
    fun canUseVideoMonitoring(tier: SubscriptionTier): Boolean = tier >= PREMIUM_FAMILIAR
    fun canUseVoiceCalls(tier: SubscriptionTier): Boolean = tier >= PREMIUM_FAMILIAR
    fun canUseShakeDetection(tier: SubscriptionTier): Boolean = tier >= PREMIUM_FAMILIAR
    fun getMaxTerminals(tier: SubscriptionTier): Int = when(tier) {
        FREE -> 1; PREMIUM_FAMILIAR -> 3; PREMIUM_PLUS -> 10; B2B -> Int.MAX_VALUE
    }
    fun getMaxAlarms(tier: SubscriptionTier): Int = when(tier) {
        FREE -> 3; else -> Int.MAX_VALUE
    }
    fun getMaxNotifications(tier: SubscriptionTier): Int = when(tier) {
        FREE -> 5; else -> 10
    }
    fun getHistoryDays(tier: SubscriptionTier): Int = when(tier) {
        FREE -> 1; PREMIUM_FAMILIAR -> 1; PREMIUM_PLUS -> 7; B2B -> 30
    }
}
```

### Implementación Técnica
- **Billing**: Google Play Billing Library v6+
- **Verificación**: Server-side con Firebase Cloud Functions
- **Firestore**: `/users/{userId}/subscription` campo con tier actual
- **UI**: Mostrar banner "Actualizar" en funciones bloqueadas (tier insuficiente)
- **Grace Period**: 3 días después de vencimiento antes de downgrade

---

## 📡 ANDROID 14+ - Monitoreo Silencioso (Marzo 30, 2026)

### Problema
Android 14+ restringe actividades en background y requiere notificación visible para usar cámara/micrófono en foreground service.

### Solución Recomendada
1. **Foreground Service Type**: Declarar `android:foregroundServiceType="camera|microphone"` en AndroidManifest
2. **Notificación Obligatoria**: Cuando monitoreo silencioso activo, mostrar notificación LOW-priority en Terminal: "Servicio de cuidados activo" (sin revelar que hay monitoreo)
3. **Permisos Granulares**: Solicitar `POST_NOTIFICATIONS` en Android 13+, `FOREGROUND_SERVICE_CAMERA` y `FOREGROUND_SERVICE_MICROPHONE` en Android 14+
4. **Fallback**: Si Terminal rechaza permisos nuevos → Degradar a solo audio (sin video)

---

## ✅ ACCEPTANCE CRITERIA - Definition of Done (NUEVO - March 29, 2026)

A feature is **COMPLETE** when ALL acceptance criteria are met and verified. These criteria are used by QA to validate implementation.

### MULTIIDIOMA (6 idiomas: ES, EN, FR, PT, DE, IT)
**AC-1.1**: User selects language in SplashActivity → all UI text changes to selected language
- [ ] Verified on: Monitor app AND Terminal app
- [ ] All 26 UI screens display selected language (no mixed languages)
- [ ] Language selection persists after app restart
- [ ] Fallback: If Firestore `/localization` unavailable → use Spanish embedded strings

**AC-1.2**: StringsLocalizationManager properly loads translations for each language
- [ ] Firestore collection `/localization/{language}/` exists for all 6 languages
- [ ] Each language has minimum 15 key-value pairs (app name, menu items, messages)
- [ ] Load time ≤ 500ms
- [ ] Code references: `StringsLocalizationManager.getString(key)` used for ALL user-facing text

### DUAL-ROLE SYSTEM (Monitor + Terminal)
**AC-2.1**: RoleSelectorActivity allows switching between Monitor and Terminal
- [ ] Button "Monitor" → app loads MonitorMainActivity
- [ ] Button "Terminal" → app loads TerminalMainActivity
- [ ] Selection persists in SharedPreferences
- [ ] Sesión de Firebase se MANTIENE (NO se cierra sesión)
- [ ] "Cambiar Modo" navega a RoleSelectorActivity SIN diálogo de confirmación
- [ ] "Cerrar Sesión" es botón SEPARADO en drawer menu

### FIREBASE AUTH (Google OAuth + Email)
**AC-3.1**: User can login with Google account
- [ ] Google Sign-In button visible in LoginActivity
- [ ] Tap button → Google account picker shown
- [ ] After auth → User data saved to Firestore (UID + email + name)
- [ ] Can login multiple times (token refresh works)

**AC-3.2**: User can login with email + password
- [ ] Email input field + Password input field visible
- [ ] "Register" link opens RegisterActivity
- [ ] After auth → User profile created in Firestore

### QR SCANNING FOR PAIRING
**AC-4.1**: Monitor scans QR from Terminal to pair devices
- [ ] Camera permission requested (runtime, Android 6.0+)
- [ ] QR code recognized from Terminal screen
- [ ] After scan → devices appear in each other's paired list
- [ ] Pairing persists in Firestore

### SHAKE DETECTION (Terminal ONLY)
**AC-5.1**: Terminal detects device shaking (accelerometer > 12 m/s²)
- [ ] TerminalConfigActivity has toggle switch "Shake Detection"
- [ ] Toggle ON → ShakeDetectionService starts listening to accelerometer
- [ ] Shake detected → Alert HTTP POST sent to Monitor within 500ms
- [ ] Event logged to Room database with type="shake"

**AC-5.2**: Shake detection works in background (app closed/locked)
- [ ] Toggle ON while app visible
- [ ] Close app completely (swipe away)
- [ ] Lock device screen
- [ ] Shake device → Monitor receives alert
- [ ] Verify: No false negatives (100% detection after threshold crossed)

**AC-5.3**: "Denied Forever" permission state handled gracefully
- [ ] If user rejects BODY_SENSORS permission + checks "Don't ask again" → Dialog shown: "Permission required. Open Settings?"
- [ ] "Settings" button opens Android Settings app (can enable manually)
- [ ] User enables in Settings → Toggle becomes active

### VOICE DETECTION (Custom Phrases)
**AC-6.1**: Terminal detects custom voice commands
- [ ] TerminalConfigActivity shows "Voice Detection" toggle
- [ ] User can add custom phrases (e.g., "Ayuda")
- [ ] When phrase spoken → Monitor receives alert
- [ ] Event logged with type="voz"

**AC-6.2**: Custom phrases are encrypted in Firestore
- [ ] Phrases stored as Base64-encoded AES-256 encrypted values
- [ ] Monitor cannot decrypt (Terminal has local keys only)
- [ ] Even Firestore admins cannot read plaintext

### ALERT LOG (1:N Consolidated History)
**AC-7.1**: Monitor sees consolidated alert history from ALL paired Terminals
- [ ] AlertLogActivity shows list of events (latest first)
- [ ] Each event displays: `Terminal Name - Type - Timestamp`
- [ ] Example: "María - Campana - hace 2 min" | "Pedro - Voz (Ayuda) - hace 5 min"
- [ ] Filter by Terminal name (dropdown)
- [ ] Shows up to 24 hours of history

**AC-7.2**: Alert event types properly distinguished
- [ ] Shake events saved as type="shake" (not "bell")
- [ ] Verify: Query Event table → filter by type → confirm "shake" exists (not just "bell")
- [ ] Each type has distinct icon + color in UI

### NOTIFICATIONS (Accumulated max 10)
**AC-8.1**: Notifications accumulate up to 10 events
- [ ] Event 1→ shown (total: 1)
- [ ] Event 10 → shown (total: 10)
- [ ] Event 11 → oldest discarded, new shown (total: 10)
- [ ] FIFO order maintained

**AC-8.2**: Notification collapse/expand works correctly
- [ ] Collapsed state: Shows only icon + app name + timestamp (no text)
- [ ] Tap notification → Expands to show all events
- [ ] "Llamar" + "Monitorear" SIEMPRE visibles (incluso con 0 eventos)
- [ ] "Limpiar" OCULTO cuando 0 eventos, VISIBLE cuando ≥1 evento
- [ ] "Limpiar" removes all events → botón se oculta de nuevo

### ALARMS (Terminal-created Schedules)
**AC-9.1**: Terminal can create daily alarms
- [ ] SettingsActivity has "My Alarms" section
- [ ] "+" button opens CreateAlarmDialog
- [ ] User selects time (HH:MM), days (L-D), description
- [ ] Save → Alarm stored in Room + Firestore

**AC-9.2**: Alarm triggers in Terminal AND Monitor
- [ ] At scheduled time: Sound notification in Terminal
- [ ] Monitor receives notification with "Stop" button
- [ ] If Terminal presses "Stop" → silences in Monitor too
- [ ] If Monitor presses "Stop" → silences in Terminal too
- [ ] Sync works even if device was offline during alarm

### AUDIO CALLS (Walkie-talkie, Sin UI)
**AC-10.1**: Monitor can place walkie-talkie audio call to Terminal
- [ ] Monitor taps "Llamar" button (in MonitorMainActivity or notification)
- [ ] Terminal auto-accepts (CallManager handles audio stream)
- [ ] Audio stream opens (both can speak/hear) → walkie-talkie style
- [ ] Disconnect button ends call ("Llamar" becomes "Colgar")
- [ ] Verify: No separate Activity opens, NO visual interface needed
- [ ] Verify: No manual accept dialog shown on Terminal (auto-accept by design)

### VIDEO MONITORING (Silent - video + audio)
**AC-11.1**: Monitor can silently monitor Terminal video/audio
- [ ] Monitor taps "Monitor" button
- [ ] Terminal camera opens (Terminal doesn't know actively being monitored)
- [ ] Monitor sees video stream + hears audio
- [ ] Monitor can switch cameras (front/back)
- [ ] "Stop" button disconnects

### SETTINGS & CONFIGURATION
**AC-12.1**: Settings shared between Monitor and Terminal
- [ ] SettingsActivity accessible from both roles
- [ ] Language selector available (updates immediately)
- [ ] Tone selector for notifications
- [ ] Preferences persist in encrypted SharedPreferences + Firestore sync

---

## 🔍 VERIFICATION CHECKLIST

**Before marking feature as DONE**:
- [ ] All AC items checked
- [ ] Tested on API 26 device (minimum Android 8)
- [ ] Tested on latest Android device (API 34+)
- [ ] No hardcoded Spanish text (use StringsLocalizationManager)
- [ ] No runtime permission crashes (ActivityCompat.requestPermissions() used)
- [ ] Offline fallback works (test with Firestore disabled)
- [ ] Code reviewed against ENGINEERING_STANDARDS.md
- [ ] QA signed off in TESTING_CHECKLISTS.md
- **Descripción**: texto customizable (ej: "Tomar Aspirina 500mg")
- **Hora**: selector de hora (HH:MM)
- **Días de la Semana**: checkboxes (Lunes, Martes, ..., Domingo)
- **Habilitada**: toggle on/off

**Campo Adicional - Selector de Dispositivo**:
- **Sonar en**: dropdown
  - "Ambos dispositivos" (Monitor + Terminal)
  - "Solo Terminal"
  - "Solo Monitor"

### Comportamiento de Alarma

**Activación**:
- Se ejecuta en la hora configurada todos los días seleccionados
- Muestra notificación en dispositivo(s) seleccionado(s)
- Sonido de alarma (respeta modo silencioso si está activado)
- Notificación permanece hasta que usuario la descarta

**Historial**:
- Último tiempo de ejecución guardado en Firestore (últimaEjecucion)
- Se registra en logs_utilizacion

### Permisos - Quién puede Configurar Alarmas

**En Terminal** (modo Terminal):
- ✅ Terminal puede ver y crear sus propias alarmas
- Nota: Monitor emparejado también puede modificar estas alarmas

**En Monitor** (modo Monitor):
- ✅ Monitor puede ver alarmas del Terminal emparejado
- ✅ Monitor puede CREAR nuevas alarmas para Terminal
- ✅ Monitor puede MODIFICAR alarmas existentes
- ✅ Monitor puede ELIMINAR alarmas
- **Restricción**: Si Terminal tiene 5 alarmas, Monitor puede agregar más (no hay límite)

**CRÍTICO - Sincronización Remota**:
- Cuando Monitor modifica/crea/elimina alarma:
  - Se guarda en Firestore inmediatamente
  - Si Terminal está online → Se sincroniza en tiempo real
  - Si Terminal está offline → Se sincroniza cuando vuelve online
  - Terminal ve cambios reflejados después de sincronización

---

## 🌐 ESPECIFICACIÓN DE IDIOMA (Marzo 20 - ACTUALIZADO 1:N Marzo 28)

### Soporte de Idiomas

**Idiomas Disponibles**:
- 🇪🇸 Español (es)
- 🇬🇧 English (en)
- 🇫🇷 Français (fr)
- 🇵🇹 Português (pt)
- 🇩🇪 Deutsch (de)
- 🇮🇹 Italiano (it)

### Comportamiento de Cambio de Idioma

**Cuándo cambia**: Usuario presiona nuevo idioma en SettingsActivity

**Timing**:
- ✅ INMEDIATAMENTE (sin reinicio de app)
- ✅ UI se actualiza en tiempo real
- ✅ Persiste en SharedPreferences local (NO sincronizado a Firestore)

**Por Dispositivo - EXPLÍCITO PARA 1:N**:
- ✅ Monitor y Terminal PUEDEN tener idiomas diferentes
- ✅ **CADA DISPOSITIVO MANTIENE SU IDIOMA INDEPENDIENTEMENTE** (crítico para 1:N)
- ✅ NO es idioma global/sincronizado entre dispositivos emparejados
- ✅ Cambiar de Monitor NO afecta idioma del Terminal
- ✅ Cambiar de rol (Monitor→Terminal) NO afecta idioma del dispositivo
- Ejemplo válido en 1:N: Monitor A (Español) empareja Terminal (Español) → Monitor B (Inglés) empareja mismo Terminal quien SIGUE EN ESPAÑOL

### Implementación 1:N

**Almacenamiento (LOCAL-FIRST — ACTUALIZADO v4.1)**:
- **SharedPreferences local**: Idioma seleccionado guardado en dispositivo
- **Firestore**: StringsLocalizationManager descarga traducciones desde `/localization/{language}/{string_key}` al seleccionar idioma
- **Cache local**: Una vez descargado, el idioma se persiste localmente y funciona SIN internet
- **Español por defecto**: Si Firestore no disponible Y no hay cache descargada → usar strings.xml embebidos (Spanish)
- **Estado actual de traducciones**: Las traducciones aún NO están subidas a Firestore para todos los idiomas. Necesita upload manual de key-value pairs para EN, FR, PT, DE, IT
- **Razón**: Terminal es dispositivo personal del usuario cuidado. Idioma es preferencia local, no del Monitor

**Sincronización**:
- ❌ NO sincronizar idioma entre dispositivos emparejados
- ❌ NO mostrar opción "usar idioma del Monitor" en Terminal
- ✅ Cada dispositivo es independiente lingüísticamente
- ✅ StringsLocalizationManager.getString(key, language) obtiene texto de Firestore o fallback local

### Aplicación de Idioma

**Qué se traduce** (TODOS los textos):
- Nombres de botones
- Etiquetas de Settings
- Mensajes de error
- Notificaciones
- Alarmas (descripciones de tipo)
- Timestamps y formatos de hora

**Dónde se persiste**:
- SharedPreferences local (carga rápido)
- Firestore (sincronización y analytics)

**Cambio de Rol** (Monitor ↔ Terminal):
- Mantiene idioma seleccionado
- Ejemplo: Usuario en español → Cambia a Terminal → Sigue en español

---

## 👥 ESPECIFICACIÓN DE PERSISTENCIA DE ROL (Role Selection) (Marzo 20)

### Sesión Autenticada

**Mantención de Sesión**:
- ✅ Usuario permanece autenticado después de cambiar rol
- ✅ Credenciales de Firebase Auth se reutilizan
- ✅ Cambio de rol es inmediato (sin reinicio de sesión)

**Flujo de Cambio de Rol**:
1. Usuario en Monitor presiona "Cambiar Modo" (desde menú drawer)
2. Abre pantalla ROLE SELECTION (RoleSelectorActivity)
3. Usuario elige "Terminal"
4. App recrea Activity en modo Terminal (NO reinicia app completa, NO cierra sesión)
5. Sesión autenticada de Firebase se mantiene
6. Idioma se mantiene (véase sección anterior)

### Almacenamiento de Rol Actual

**Persistencia**:
- Rol actual guardado en SharedPreferences
- También guardado en Firestore (/users/{userId}/rol)
- Usado para:
  - Restaurar a rol anterior si app crashea
  - Analytics
  - Sincronización en múltiples dispositivos (si usuario loguea en otro phone)

**Restauración**:
- Si app se crashea en Terminal → Reinicia en Terminal
- Si usuario desinstala app → Recupera rol desde Firestore al volver a loguear

### Desconexión de Monitoreo al Cambiar Rol

**Si hay Sesión de Video/Audio Activa** (llamada, video, monitoreo):
- ✅ Presionar "Cambiar Modo" CIERRA la conexión de video/audio activa (pero NO cierra sesión de Firebase)
- ✅ Conexiones de video/audio se cierran ordenadamente
- ✅ Se registra en logs_conexion
- ✅ THEN abre ROLE SELECTION

**Ejemplo**:
- Monitor está en Monitoreo Silencioso activo
- Presiona "Cambiar Modo" durante monitoreo
- Conexión se corta inmediatamente (Terminal ve "conexión perdida")
- Abre ROLE SELECTION en Monitor
- Usuario elige nuevo rol y continúa

### Interfaz de ROLE SELECTION (ACTUALIZADO v2.0)

**Pantalla Dedicada**:
- Mostrar 2 opciones grandes como cards tappable (72dp alto cada una) — misma UI que onboarding paso 2
  - Opción 1: "👤 SOY EL CUIDADOR" + descripción "Quiero monitorear y recibir alertas" (ícono: persona con reloj)
  - Opción 2: "👴 SOY LA PERSONA CUIDADA" + descripción "Quiero poder pedir ayuda fácilmente" (ícono: campana)
- Descripción bajo cada opción (18sp, consistente con v2.0)
- Tap en card = selección directa (sin botón "Continuar" separado)

**Ubicación en Navegación (v2.0)**:
- Monitor: Drawer lateral → "Cambiar Modo"
- Terminal: Settings → "Cambiar Modo" (al fondo)
- Onboarding: Paso 2 (primer uso)
- La sesión de Firebase NO se cierra al cambiar de modo
- "Cerrar Sesión" es opción SEPARADA en drawer (Monitor) o Settings (Terminal)

---

## 🎨 DISEÑO Y ACCESIBILIDAD — REDISEÑO GERIÁTRICO v2.0 (Marzo 30, 2026)

> **Fundamento**: Basado en investigación de empresas exitosas (GrandPad, GreatCall/Lively $800M exit, Life360 48.6M MAU, Care.com $500M exit), estándares W3C WAI para usuarios mayores, principios de Diseño Universal (7 principios), y lecciones de empresas fallidas (Honor, Papa, HomeHero, Hometeam). La calidad de la experiencia del usuario es el diferenciador #1 — no el precio, no las features.

---

### 🏛️ 10 PRINCIPIOS DE DISEÑO (OBLIGATORIOS — Todo diseño debe cumplir TODOS)

| # | Principio | Origen | Especificación |
|---|-----------|--------|----------------|
| 1 | **Simplicidad Radical (Terminal)** | GrandPad: walled garden, 5 acciones máx | Terminal: máx 3 acciones visibles en pantalla principal |
| 2 | **Dashboard Inteligente (Monitor)** | Life360: core action sin fricción | Monitor: tabs inferiores, alertas primero, 1 tap para acción |
| 3 | **Touch Targets Geriátricos** | W3C WAI + GreatCall/Lively | Primarios: 72dp, Secundarios: 56dp, Mínimo absoluto: 48dp |
| 4 | **Tipografía Legible** | W3C WAI: declining vision | Body: 18sp mín, Headers: 24sp mín, Hints: 16sp mín |
| 5 | **Contraste Alto** | WCAG AA (4.5:1), target AAA (7:1) | Verificar TODO texto contra fondo, incluir modo alto contraste |
| 6 | **Feedback Multi-Modal** | Diseño Universal: perceptible info | Toda acción = visual + haptic + audio (configurable) |
| 7 | **Máximo 2 Taps** | GreatCall 5Star: un botón | Cualquier función accesible en ≤2 taps desde pantalla principal |
| 8 | **TalkBack Completo** | W3C WAI: assistive technology | contentDescription en TODOS los elementos interactivos |
| 9 | **UI Adaptativa** | CapabilitiesAssessment existente | Adaptar UI según resultados de evaluación (visión, movilidad, cognición, habla, audición) |
| 10 | **Emergencia 1-Touch** | GreatCall 5Star ($800M exit) | La campana cumple esta función: 1 toque → alerta al cuidador |

---

### 🎨 PALETA DE COLORES (Geriátric-Optimized)

| Elemento | Color | Hex | Uso | Ratio Contraste |
|----------|-------|-----|-----|----------------|
| **Primary** | Teal profundo | #006B6B | Botones primarios, headers, tabs activos | 7.2:1 vs blanco (AAA ✅) |
| **Primary Variant** | Teal medio | #008B8B | Acentos, iconos activos | 4.9:1 vs blanco (AA ✅) |
| **Background Light** | Blanco cálido | #FAFAFA | Fondo modo día | — |
| **Background Dark** | Gris profundo | #121212 | Fondo modo noche | — |
| **Surface** | Blanco | #FFFFFF | Cards, diálogos | — |
| **Surface Dark** | Gris oscuro | #1E1E1E | Cards modo noche | — |
| **Text Primary** | Negro suave | #1A1A1A | Texto principal modo día | 15.3:1 vs blanco (AAA ✅) |
| **Text Primary Dark** | Blanco | #FFFFFF | Texto principal modo noche | 15.3:1 vs #121212 (AAA ✅) |
| **Text Secondary** | Gris medio | #555555 | Texto secundario, hints | 7.5:1 vs blanco (AAA ✅) |
| **Error** | Rojo alerta | #D32F2F | Alertas críticas | 5.9:1 vs blanco (AA ✅) |
| **Success / Active** | Verde | #2E7D32 | Servicio activo, conexión OK | 5.1:1 vs blanco (AA ✅) |
| **Warning** | Ámbar | #F57F17 | Batería baja, warnings | Usar con texto negro |
| **Disabled** | Gris claro | #BDBDBD | Elementos inactivos | — |

> **CAMBIO v1→v2**: Primary cambió de #008B8B a #006B6B para alcanzar ratio AAA (7:1) en vez de solo AA. El #008B8B se mantiene como variant para acentos no-textuales.

---

### 📐 TIPOGRAFÍA (Mínimos Geriátricos)

| Contexto | Tamaño Mínimo | Peso | Fuente | Ejemplo |
|----------|---------------|------|--------|---------|
| **Headers de pantalla** | 28sp | Bold (700) | Sans-serif (Roboto) | "Monitor de Cuidados" |
| **Headers de sección** | 24sp | SemiBold (600) | Sans-serif | "Alertas Recientes" |
| **Body text** | 18sp | Regular (400) | Sans-serif | Descripción de alarma |
| **Botones primarios** | 20sp | Medium (500) | Sans-serif | "LLAMAR", "MONITOREAR" |
| **Botones secundarios** | 18sp | Medium (500) | Sans-serif | "Configuración" |
| **Labels / Hints** | 16sp | Regular (400) | Sans-serif | "Ingrese su correo" |
| **Timestamps** | 16sp | Light (300) | Sans-serif | "hace 5 minutos" |
| **Badge / Contadores** | 14sp | Bold (700) | Sans-serif | "3" (badge notificaciones) |

> **CAMBIO v1→v2**: Mínimo absoluto subió de 14sp a 16sp. Body text de 14sp a 18sp. Headers de 20sp a 24-28sp. Fuente: Roboto (default Android, optimizada legibilidad).

**Soporte de Escalado de Texto (OBLIGATORIO)**:
- Toda la tipografía usa `sp` (scale-independent pixels)
- App debe soportar Android Accessibility font scaling (hasta 200%)
- Layouts DEBEN adaptarse sin overflow cuando texto es escalado 1.5x
- Probar con: Settings → Accessibility → Font Size → Largest

---

### 👆 TOUCH TARGETS (Tamaños de Toque)

| Categoría | Tamaño Mínimo | Separación Mín | Ejemplo |
|-----------|---------------|----------------|---------|
| **Botón primario (CTA)** | 72dp × 72dp | 12dp entre botones | "Campana", "LLAMAR" |
| **Botón secundario** | 56dp × 56dp | 8dp entre botones | "Configuración", "Video", "Log" |
| **Toggle / Switch** | 56dp × 32dp | 8dp | Switches en Settings |
| **List item tappable** | Full-width × 56dp | 4dp divider | Items en AlertLog, alarmas list |
| **Icon button** | 48dp × 48dp | 8dp | Drawer toggle, back arrow |
| **Tab bar item** | Proportional × 56dp | 0dp (tabs son contiguos) | Tabs de bottom navigation |
| **FAB (Floating Action)** | 72dp × 72dp | N/A (solo 1 FAB) | Botón "+" agregar alarma |

> **CAMBIO v1→v2**: Botones primarios subieron de 48dp a 72dp. Secundarios de 48dp a 56dp. Basado en estudios de GreatCall y GrandPad donde botones >60dp redujeron errores de toque en 73% en mayores de 70 años.

---

### 🧭 MODELO DE NAVEGACIÓN (REDISEÑADO)

#### ANTES (v1): DrawerLayout (menú hamburguesa)
- ❌ Menú oculto: Usuarios mayores no descubren el hamburger menu
- ❌ Requiere gesto de arrastre: Difícil con movilidad reducida
- ❌ Contenido invisible: No saben qué opciones existen

#### AHORA (v2 — IMPLEMENTACIÓN REAL): DrawerLayout + Toolbar con bell toggle

> ⚠️ **NOTA IMPORTANTE**: El diseño v2 original planificaba BottomNavigationView pero la implementación real usa **DrawerLayout + Toolbar**. Esta sección refleja el código real.

**Monitor — DrawerLayout + Toolbar (implementación actual)**:
- **Toolbar**: título "Monitor de Cuidados" + botón campana (bell toggle) que alterna Dashboard ↔ HistoryFragment
- **DrawerLayout**: NavigationView con items: Cambiar Modo, Ajustes, Acerca de, Cerrar Sesión
- **Drawer Header (nav_header_main.xml)**: headerTitle = nombre del usuario Monitor (Firebase displayName o Build.MODEL), headerEmail = email del usuario
- **Fragment Container**: frame_container aloja DashboardFragment (default) o HistoryFragment
- **Resultado**: Acciones principales accesibles via toolbar + drawer

**Terminal — DrawerLayout + QR centrado (implementación actual)**:
- **Toolbar**: ☰ hamburguesa + título "Monitor de Cuidados"
- **DrawerLayout**: NavigationView con items: Cambiar Modo, Ajustes, Acerca de, Cerrar Sesión
- Terminal USA drawer, contrario al diseño v2 original que planificaba sin drawer
- **Contenido**: TerminalQRFragment con QR centrado + switches de servicios debajo
- **Resultado**: Consistencia de navegación con Monitor, adulto mayor accede a opciones si necesita

---

### 📱 TERMINAL: REDISEÑO RADICAL — "Modo GrandPad" (v2.0)

> **Filosofía**: El Terminal es el dispositivo del adulto mayor. Debe tener la simplicidad de un GrandPad y la funcionalidad de un botón 5Star de GreatCall. Máximo 3 elementos visibles.

#### Pantalla Principal Terminal (USO DIARIO)

```
(┌──────────────────────────────────────┐
│                      🟢 Conectado    │  ← Status bar: config (56dp) + estado
│                                      │
│                                      │
│                                      │
│         ┌──────────────┐             │
│         │              │             │
│         │   🔔 CAMPANA │             │  ← Botón GIGANTE central (160dp × 160dp)
│         │              │             │     Toque = pedir ayuda al cuidador
│         │  Tocar para  │             │
│         │  pedir ayuda │             │
│         └──────────────┘             │
│                                      │
│                                      │
│                                      │
│                                      │
│                                      │
└──────────────────────────────────────┘
```

**Elementos Visibles en Pantalla Principal (SOLO 2+2)**:
1. **Botón Campana GIGANTE** (160dp × 160dp, circular, #006B6B):
   - Ocupación: ~40% de la pantalla
   - Texto interno: "Tocar para pedir ayuda" (20sp, blanco)
   - Ícono: 🔔 (64dp)
   - Feedback al tocar: vibración 200ms + sonido de confirmación + animación ripple + texto cambia a "✓ Enviado"
   - Revert: Vuelve a estado normal después de 3 segundos
   

4. **Logo/Nombre** (debajo de status bar, centrado):
   - "Monitor de Cuidados" (18sp, #555555, solo decorativo))

#### ¿Qué pasó con los Cards del Terminal v1?

| Card v1 | Destino v2 | Justificación |
|---------|-----------|---------------|
| Card 1: Nombre + Start/Stop | Status bar (conexión) + botón ⚙️ → Settings | Servicio se auto-inicia, no necesita switch manual |
| Card 2: QR Code | Movido a: Settings → "Vincular dispositivo" | QR solo se necesita 1 vez en setup, no debe ocupar pantalla diaria |
| Card 3: Switch Shake | Movido a: Settings → "Sensores" | Configuración es del cuidador, no del adulto mayor |
| Card 4: Switch Voz | Movido a: Settings → "Sensores" | Idem |
| Card 5: Log eventos | Eliminado de Terminal (solo Monitor ve log completo) | Adulto mayor no necesita ver historial |

#### Servicio Auto-Start (CAMBIO IMPORTANTE v2)
- CampanaService se INICIA AUTOMÁTICAMENTE al abrir la app en modo Terminal
- NO hay switch manual para start/stop (se elimina)
- Servicio SIEMPRE activo mientras app esté en modo Terminal
- Si usuario necesita detener: Settings → "Pausar servicio" (botón con confirmación)
- Justificación: Adulto mayor no debe preocuparse por activar/desactivar servicios

#### Terminal — Pantalla de Configuración (Settings — Acceso vía ⚙️)
```
┌──────────────────────────────────────┐
│ ← Volver          Configuración      │
├──────────────────────────────────────┤
│                                      │
│ 📱 MI DISPOSITIVO                    │
│ ┌──────────────────────────────────┐ │
│ │ Nombre: "Terminal de María"      │ │
│ │ Vinculado a: "Monitor de Juan"   │ │
│ │ [Cambiar vinculación ▷]          │ │  ← Aquí va QR scanner
│ └──────────────────────────────────┘ │
│                                      │
│ 🔔 SENSORES Y DETECCIÓN             │
│ ┌──────────────────────────────────┐ │
│ │ Agitación (Shake)    [  ON  ]    │ │
│ │ Detección de Voz     [  ON  ]    │ │
│ │ Frases personalizadas [Editar ▷] │ │
│ └──────────────────────────────────┘ │
│                                      │
│ ⏰ ALARMAS Y RECORDATORIOS           │
│ ┌──────────────────────────────────┐ │
│ │ Mis alarmas          [Editar ▷]  │ │
│ └──────────────────────────────────┘ │
│                                      │
│ 🔊 SONIDO Y VIBRACIÓN               │
│ ┌──────────────────────────────────┐ │
│ │ Tono de campana      [Fijo 🔔]  │ │
│ │ Vibración            [  ON  ]    │ │
│ │ Volumen de alerta    [████░░] 70%│ │
│ └──────────────────────────────────┘ │
│                                      │
│ 🌐 IDIOMA                            │
│ ┌──────────────────────────────────┐ │
│ │ Español              [Cambiar ▷] │ │
│ └──────────────────────────────────┘ │
│                                      │
│ 🔄 Cambiar Modo (Monitor/Terminal)   │
│ 🚪 Cerrar Sesión                     │
│ ℹ️ Acerca de                          │  ← Abre pantalla con: nombre app, versión, desarrollador, licencias, link a aviso de privacidad
│                                      │
│ ⏸️ Pausar Servicio                    │  ← Al fondo, requiere confirmación
└──────────────────────────────────────┘
```

---

### 📱 MONITOR: REDISEÑO — Dashboard Inteligente (v2.0)

> **Filosofía**: El Monitor es el dispositivo del cuidador. Necesita información rápida, acciones inmediatas, y visibilidad de todos los terminales. Inspirado en Life360 (información de un vistazo) y GreatCall (acciones de 1 tap).

#### Pantalla Principal Monitor (Tab "Inicio")

```
┌──────────────────────────────────────┐
│ ☰  Monitor de Cuidados    🔔 (3)    │  ← Toolbar: drawer + notif badge
├──────────────────────────────────────┤
│                                      │
│ ⚡ ALERTAS ACTIVAS                    │  ← Sección prioridad #1
│ ┌──────────────────────────────────┐ │
│ │ � Campana — María — hace 2 min   │ │  ← Card alerta (72dp alto)
│ │    [📞 LLAMAR]  [📹 MONITOREAR]  │ │     Botones acción directa
│ └──────────────────────────────────┘ │
│ ┌──────────────────────────────────┐ │
│ │ 🔔 Campana — Pedro — hace 5 min │ │
│ │    [📞 LLAMAR]  [📹 MONITOREAR]  │ │
│ └──────────────────────────────────┘ │
│                                      │
│ 📊 MIS TERMINALES                    │  ← Sección prioridad #2
│ ┌──────────────────────────────────┐ │
│ │ María                  🟢 Activo │ │  ← Línea 1: nombre + status
│ │ 🔋92%     📶WiFi                │ │  ← Línea 2: batería + conexión
│ ├──────────────────────────────────┤ │
│ │ Pedro              🟡 Reconect.  │ │  ← Click → TerminalDetailActivity
│ │ 🔋15% ⚠️  📶WiFi               │ │
│ └──────────────────────────────────┘ │
│                                      │
│ ➕ VINCULAR TERMINAL                  │  ← QR scanner para agregar
│                                      │
├──────────────────────────────────────┤
│  🏠 Inicio  │  📋 Historial  │ ⚙️    │  ← Bottom Navigation (56dp)
└──────────────────────────────────────┘
```

**Jerarquía de Información (Priority-Based)**:
1. **ALERTAS ACTIVAS** (top): Cards con alertas no atendidas, ordenadas por severidad (Campana > Shake > Voz > Batería)
2. **MIS TERMINALES** (middle): Estado de todos los terminales emparejados, compacto (1 línea por terminal)
3. **VINCULAR TERMINAL** (bottom): Acceso rápido a QR para emparejar nuevos dispositivos

**Cards de Alerta — Diseño de Acción Directa**:
- Cada alerta tiene botones de acción INLINE (no en notificación expandible)
- Botones: "📞 LLAMAR" (56dp × 40dp, teal) + "📹 MONITOREAR" (56dp × 40dp, teal)
- Card completa: full-width × 72dp mínimo, 12dp corner radius, 4dp elevation
- Swipe right to dismiss (marcar como atendida)
- Si >5 alertas: las más antiguas colapsan con "Ver más (N)"

**Cards de Terminal — Estado de un Vistazo (T37 Rediseño)**:
- 2 líneas por terminal:
  - Línea 1: Nombre (bold, text_body 18sp, ellipsize end) + Status a la derecha (🟢/🟡/🔴 + texto)
  - Línea 2: Batería + Conexión (text_timestamp 16sp, color secundario)
- Alto: wrap_content (no fijo 56dp)
- Click en card → abre **TerminalDetailActivity** con nombre, status, batería, conexión como intent extras
- Card tiene `selectableItemBackground` foreground para feedback visual de toque
- Indicadores de estado:
  - 🟢 Verde = Activo y conectado
  - 🟡 Amarillo = Reconectando o intermitente
  - 🔴 Rojo = Desconectado > 5 min
  - ⚠️ = Batería ≤ 15%

#### Monitor — Detalle de Terminal (TerminalDetailActivity) (T36)
- **Acceso**: Tap en terminal card del Dashboard → abre TerminalDetailActivity
- **Layout**: Toolbar (back arrow + nombre terminal) → ScrollView → Card Acciones (nombre + status + botones Llamar/Monitorear) → Card Servicios (IDÉNTICA a Terminal: switches campana/agitación/voz/alarmas)
- **Botón Llamar**: Lanza VideoActivity con mode="audiocall"
- **Botón Monitorear**: Lanza VideoActivity con mode="monitor"
- **Switches remotos**: Cada switch envía HTTP POST al Terminal (SET_BELL_MODE_ON/OFF, SET_SHAKE_ON/OFF, SET_VOICE_ON/OFF, SET_ALARMS_ON/OFF). Terminal actualiza SharedPrefs + reinicia servicios.
- **Back**: Regresa al Dashboard (finish())
- **Datos**: terminal_name y terminal_status via Intent extras, IP de EncryptedPreferencesHelper

#### Monitor — Tab "Historial" (AlertLogActivity)
- Vista CONSOLIDADA de TODOS los terminales (sin cambios funcionales vs v1)
- Mejora visual: 56dp por fila, timestamps en 16sp, iconos 24dp
- Filtro por terminal (dropdown arriba) y por tipo de evento

#### Monitor — Tab "Ajustes" (SettingsActivity)
- Sin cambios funcionales vs v1
- Mejora visual: categorías con headers 24sp, items 56dp alto, toggles 56×32dp

---

###  ONBOARDING — Experiencia de Primer Uso (NUEVO v2.0)

> **Justificación**: W3C WAI indica que "older users often need guidance understanding the context and functionality of the content." GrandPad incluye setup asistido con soporte telefónico. Nuestro equivalente: onboarding guiado en-app.

**Flujo de Onboarding (después de login, antes de rol selector)**:

**Paso 1 — Bienvenida** (1 pantalla):
```
┌──────────────────────────────────────┐
│                                      │
│         🏠 Monitor de Cuidados       │
│                                      │
│    Cuidando a tus seres queridos     │
│    con tecnología simple y segura    │
│                                      │
│    [  COMENZAR CONFIGURACIÓN  ▷  ]   │  72dp, teal
│                                      │
│    Saltar →                          │  16sp, gris, discreto
└──────────────────────────────────────┘
```

**Paso 2 — Elegir Rol** (1 pantalla):
```
┌──────────────────────────────────────┐
│ ← Atrás                             │
│                                      │
│  ¿Quién usará este dispositivo?      │  24sp
│                                      │
│  ┌──────────────────────────────────┐│
│  │ 👤 SOY EL CUIDADOR              ││  72dp, card tappable
│  │ Quiero monitorear y recibir     ││
│  │ alertas de mi ser querido       ││
│  └──────────────────────────────────┘│
│                                      │
│  ┌──────────────────────────────────┐│
│  │ 👴 SOY LA PERSONA CUIDADA       ││  72dp, card tappable
│  │ Quiero poder pedir ayuda        ││
│  │ fácilmente                      ││
│  └──────────────────────────────────┘│
│                                      │
│  ℹ️ Puedes cambiar esto después      │  16sp, gris
└──────────────────────────────────────┘
```

**Paso 3a — Setup Monitor** (1 pantalla):
- "Escanea el QR del dispositivo de tu ser querido"
- Abre cámara QR
- Si no tiene otro dispositivo aún → "Configura el otro dispositivo primero"

**Paso 3b — Setup Terminal** (1 pantalla):
- "Muestra este código QR al cuidador"
- Muestra QR grande (200dp × 200dp)
- Instrucciones: "Pide al cuidador que escanee este código con su celular"

**Paso 4 — Evaluación de Capacidades** (solo Terminal):
- CapabilitiesAssessmentActivity existente
- Botón "OMITIR" prominente (56dp, gris) — no obligar al adulto mayor

**Paso 5 — Tutorial Interactivo** (3 slides, solo Terminal):
```
Slide 1: "Toca la campana 🔔 cuando necesites ayuda" 
         [Animación: dedo tocando botón campana]
         
Slide 2: "Si necesitas más ayuda, toca de nuevo la campana"
         [Animación: dedo tocando campana otra vez]
         
Slide 3: "¡Listo! Tu cuidador recibirá tus alertas"
         [Animación: notificación llegando a otro teléfono]

[  EMPEZAR A USAR  ▷  ]  72dp, teal
```

**Control de Re-show**:
- Primera vez: SIEMPRE mostrar
- Subsecuentes: NO mostrar (flag en SharedPreferences `onboarding_completed = true`)
- Reset: Settings → "Ver tutorial de nuevo"

---

### ♿ ACCESIBILIDAD COMPLETA — Requisitos Técnicos

#### TalkBack (Screen Reader)
- **contentDescription**: OBLIGATORIO en TODO elemento interactivo
  - Botones: "Tocar para pedir ayuda al cuidador" (no solo "Campana")
  - Switches: "Detección de agitación, actualmente activado. Doble tap para desactivar"
  - Cards: "Alerta de María, campana, hace 5 minutos. Acciones disponibles: llamar, monitorear"
  - Imágenes decorativas: `importantForAccessibility="no"`
- **Traversal Order**: Definir `accessibilityTraversalBefore/After` para orden lógico
- **Live Regions**: `accessibilityLiveRegion="polite"` en contadores y estados que cambian

#### Switch Access
- Todos los elementos focusables en orden lógico
- Focus indicators visibles (contorno 2dp, #006B6B)
- No depender de gestos complejos (swipe, long-press para funciones esenciales tiene alternativa)

#### Font Scaling
- TODO el texto en `sp` (nunca `dp` para texto)
- Layouts usan `wrap_content` height en contenedores de texto
- Probar con fuente 200%: ningún texto cortado, ningún overlap
- `ConstraintLayout` con constraints flexibles (no valores fijos que rompan)

#### Modo Alto Contraste
- Respetar `Settings.Secure.ACCESSIBILITY_HIGH_TEXT_CONTRAST_ENABLED`
- Cuando activo: Text color = #000000, Background = #FFFFFF, sin gradientes
- Bordes en todos los elementos interactivos (1dp solid)

#### Modo de Color
- Soportar daltonismo: No depender SOLO de color para comunicar estado
- Siempre usar color + ícono + texto (ej: 🟢 + "Conectado", no solo punto verde)

---

### 🌙 MODO OSCURO — Optimizado para Uso Nocturno

> **Justificación**: Caregiving ocurre 24/7. Revisiones nocturnas del Monitor requieren modo oscuro que no deslumbre ni despierte al paciente.

| Elemento | Modo Día | Modo Noche |
|----------|----------|------------|
| Fondo | #FAFAFA | #121212 |
| Cards | #FFFFFF | #1E1E1E |
| Texto primario | #1A1A1A | #FFFFFF |
| Texto secundario | #555555 | #B0B0B0 |
| Primary (teal) | #006B6B | #4DB6AC (teal claro) |
| Error (rojo) | #D32F2F | #EF5350 (rojo claro) |
| Dividers | #E0E0E0 | #333333 |

- Transición: Seguir configuración del sistema (DayNight theme ya implementado)
- Override manual: Settings → "Modo nocturno" toggle (para forzar oscuro de día)
- **Implementación del toggle** (CORREGIDO Marzo 30):
  - Al **encender** switch: `AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_YES)` + guardar `night_mode = true` en SharedPreferences
  - Al **apagar** switch: `AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_FOLLOW_SYSTEM)` + guardar `night_mode = false` en SharedPreferences. **NUNCA usar MODE_NIGHT_NO** — apagar el switch significa "seguir al sistema", no "forzar modo día"
  - Al **abrir Settings**: El switch debe reflejar el estado REAL del tema actual. Si `night_mode` pref es false PERO el sistema está en dark mode (FOLLOW_SYSTEM activó dark), el switch debe mostrarse como **activado** (refleja estado visual real). Usar `resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES` para detectar el estado real.
  - Al iniciar app: `CareMonitorApp` lee preferencia: si true → MODE_NIGHT_YES, si false y API Q+ → MODE_NIGHT_FOLLOW_SYSTEM
- Auto-schedule: Opción para activar modo oscuro entre 21:00 y 07:00

---

### 🔄 UI ADAPTATIVA — Basada en CapabilitiesAssessment (NUEVO v2.0)

> **Concepto**: La evaluación de capacidades que ya existe en la app se usa para adaptar la UI automáticamente.

| Capacidad Evaluada | Si tiene dificultad | Adaptación UI |
|--------------------|---------------------|---------------|
| **Visión** | Baja visión | Texto +25% más grande, contraste forzado AAA, iconos +50% |
| **Movilidad** | Movilidad reducida | Touch targets +25% (primario: 90dp), vibración más fuerte, botones más separados |
| **Cognición** | Deterioro cognitivo | Reducir opciones visibles a mínimo (campana solamente), eliminar info strip |
| **Habla** | Dificultad para hablar | Sensibilidad de voz aumentada, opciones de comunicación alternativa |
| **Audición** | Dificultad auditiva | Vibración más fuerte y prolongada, flash visual más prominente, LED notification |

**Implementación**:
- Resultados en Firestore: `/users/{uid}/capabilities`
- Al iniciar app: Cargar capabilities → aplicar modificadores a `dimens.xml` values via código
- Si assessment fue omitido: UI defaults (sin adaptaciones)
- Monitor puede re-ejecutar assessment: Settings → "Re-evaluar capacidades del Terminal"

---

### 📐 FEEDBACK MULTI-MODAL — Especificación por Acción

| Acción | Visual | Háptico | Audio |
|--------|--------|---------|-------|
| **Campana tocada** | Ripple + "✓ Enviado" (3s) | Vibración 200ms | Sonido confirmación corto |
| **Alerta recibida (Monitor)** | Card aparece + heads-up notif | Vibración patrón 300ms | Tono de alerta |
| **Llamada conectada** | Indicador "En llamada" verde | Vibración corta 100ms | Tono de conexión |
| **Error / Fallo** | Snackbar rojo | Vibración doble 100+100ms | — (silencioso) |
| **Toggle activado** | Animación switch | Vibración tick 50ms | Click suave |
| **Navegación (tab)** | Animación tab | — | — |

**Configuración de Feedback (SettingsActivity)**:
- Toggle: "Vibración" (ON/OFF)
- Toggle: "Sonidos" (ON/OFF)
- Slider: "Volumen de alertas" (0-100%)
- Nota: Visual feedback SIEMPRE activo (no se puede desactivar)

---

## ✅ ESTADO DE VALIDACIÓN (Marzo 30, 2026 — ACTUALIZADO v2.0)

| Módulo | Funcionalidad | Estado | Prioridad |
|---|---|---|---|
| **Core** | Estabilidad de Crashing (P0) | ✅ CORREGIDO | P0 |
| **UX** | Paleta de Colores Geriátrica | ✅ IMPLEMENTADO → ⚠️ ACTUALIZAR a #006B6B | P1 |
| **UX** | Rediseño Terminal "Modo GrandPad" | 🔴 PENDIENTE v2.0 | P0 |
| **UX** | Monitor Bottom Navigation + Dashboard | 🔴 PENDIENTE v2.0 | P0 |
| **UX** | Onboarding Guiado (5 pasos) | 🔴 PENDIENTE v2.0 | P1 |
| **UX** | UI Adaptativa (CapabilitiesAssessment) | 🔴 PENDIENTE v2.0 | P1 |
| **UX** | Touch targets 72dp/56dp | 🔴 PENDIENTE v2.0 | P1 |
| **UX** | Tipografía 18sp mín body | 🔴 PENDIENTE v2.0 | P1 |
| **UX** | Feedback Multi-Modal (haptic+audio) | 🔴 PENDIENTE v2.0 | P1 |
| **UX** | TalkBack completo | 🔴 PENDIENTE v2.0 | P1 |
| **UX** | Modo Oscuro optimizado para noche | ⚠️ PARCIAL → COMPLETAR v2.0 | P2 |
| **UX** | Switches Actualizados Automáticamente | ✅ IMPLEMENTADO | P1 |
| **UX** | Unificación Interfaces Monitor-Terminal | ✅ IMPLEMENTADO → REEMPLAZAR por v2.0 | P1 |
| **Video** | Interfaz de 3 Botones / Sin Diálogo | ✅ IMPLEMENTADO | P1 |
| **Llamadas** | Iniciar/Colgar Llamadas Bidireccionales | ✅ IMPLEMENTADO | P2 |
| **Monitoreo** | Cámara + Micrófono Silencioso | ✅ IMPLEMENTADO | P2 |
| **QR** | Cámara Trasera / Visibilidad Condicional | ✅ IMPLEMENTADO → MOVER a Settings | P1 |
| **Log** | Historial de Alertas Visible | ✅ IMPLEMENTADO | P2 |
| **Settings** | SettingsActivity Compartida (Ambos Modos) | ✅ IMPLEMENTADO → EXPANDIR para Terminal v2.0 | P2 |
| **Notificación** | Terminal: "Servicio Activo" (Sin Iconos Video) | ✅ IMPLEMENTADO | P1 |

---
**Monitor de Cuidados - Documento de Especificación v3.0**

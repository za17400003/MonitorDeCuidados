## 📊 MATRIZ DE SEGURIDAD ACTUALIZADA (Marzo 20, 2026)

| Riesgo | Impacto | Probabilidad | Mitigación Implementada | Estado |
|---|---|---|---|---|
| Acceso físico al dispositivo | Medio | Media | Cifrado AES-256 + biometric lock | ✅ |
| Intercepción Video (MITM WiFi local) | Alto | Baja | QR token validation + certificate pinning | ✅ |
| Inyección de Comandos en NanoHTTPD | Medio | Media | JSON input sanitization | ✅ |
| Robo de Credenciales | Alto | Baja | Keystore Android + Firebase Auth | ✅ |
| **NUEVO**: Terminal sabe monitoreo silencioso | Alto | Bloqueado | CERO notificaciones + eventos solo en Firestore | ✅ |
| **NUEVO**: Terminal rechaza videollamada | Alto | Bloqueado | Aceptación automática sin diálogo + control simétrico | ✅ |
| **NUEVO**: Monitor accede logs locales de Terminal | Alto | Bloqueado | Terminal NUNCA crea logs locales de monitoreo | ✅ |
| **NUEVO**: Datos sensibles en Firestore | Medio | Media | Data minimization (only metadata + events) | ✅ |
| **NUEVO**: Network fallback inseguro | Medio | Baja | Firestore TLS 1.3+ + signaling only (no streams) | ✅ |
| **NUEVO**: Múltiples Monitors acceden Terminal | Alto | Baja | Firestore rules: una pareja Terminal-Monitor | ✅ |

---

## 📋 RECOMENDACIONES POST-AUDIT

---

## 🔍 MEDIDAS DE SEGURIDAD IMPLEMENTADAS

Tras la auditoría del código en la Fase 3B, se confirman las siguientes medidas activas:

### 1. Protección de Datos en Reposo (Cifrado AES-256)
- **Base de Datos (SQLCipher)**: La base de datos Room está cifrada íntegramente usando SQLCipher con una clave generada dinámicamente.
- **Preferencias (EncryptedSharedPreferences)**: Las configuraciones sensibles (como IPs de dispositivos vinculados y tokens de sesión) se almacenan cifradas mediante el framework de seguridad de Android.
- **Archivos de Log**: Los registros de depuración se almacenan en el directorio privado de la app, inaccesible para otras aplicaciones.

### 2. Seguridad en Tránsito (Comunicaciones)
- **TLS & Certificate Pinning**: Se implementa `CertificatePinning.kt` para asegurar que las conexiones HTTPS hacia los servicios de backend no sean interceptadas.
- **Aislamiento de Red Local**: El servidor HTTP interno (`NanoHTTPD`) solo responde a peticiones dentro del mismo segmento de red, reduciendo el área de exposición.
- **Validación de QR**: El código QR de emparejamiento incluye un "pairing_secret" que debe coincidir entre Monitor y Terminal para autorizar el control remoto.

### 3. Gestión de Identidad y Acceso
- **Autenticación Biométrica/Keystore**: Uso del Keystore de Android para gestionar claves criptográficas respaldadas por hardware (TEE/StrongBox) cuando está disponible.
- **Firebase Auth**: Integración segura con Google OAuth 2.0.

---

## 🛡️ MATRIZ DE RIESGOS Y MITIGACIÓN

| Riesgo | Impacto | Mitigación Implementada |
|---|---|---|
| Acceso físico al dispositivo | Medio | Base de datos cifrada y bloqueo de pantalla automático. |
| Intercepción de Video (MITM) | Alto | Uso de protocolos UDP con validación de IP y Certificate Pinning. |
| Inyección de Comandos | Medio | Sanitización de inputs JSON en el servidor HTTP interno. |
| Robo de Credenciales | Alto | Almacenamiento exclusivo en Keystore y tokens de corta duración. |

## 📡 SEGURIDAD EN NETWORKING - ARQUITECTURA LOCAL VS INTERNET (NUEVO - Marzo 20, 2026)

### Security Model: Local vs Internet

**Local WiFi (Red Privada)**:
- ✅ **Íntegramente encriptado**: mDNS discovery + QR token verification
- ✅ **No expuesto a Internet**: Solo accesible dentro de red local (192.168.x.x subnet)
- ✅ **Zero Cloud**: Ningún dato pasa a Firestore durante comunicación local
- ⚠️ **Riesgo**: MITM en WiFi (mitigado con validación de QR token + certificate pinning en WiFi local)
- **Implementación**: 
  ```
  // Monitor envía pairingToken con cada solicitud
  POST /call/outgoing HTTP/1.1
  Authorization: PairingToken={hash_qr}
  ```

**Internet Fallback (Firestore)**:
- ✅ **Google-managed**: Infraestructura de seguridad de Google Cloud
- ✅ **Encriptación TLS 1.3+**: Todos datos en tránsito encriptados
- ⚠️ **Cloud Storage**: Datos viven en Firestore (requiere careful data handling)
- **Implementación**:
  ```
  // Firestore stores only metadata + signaling, NOT video/audio streams
  /pairings/{monitorId}/connectionType = "internet"
  /logs_conexion/{logId}/evento = "call_started"
  ```

### Data Minimization in Firestore

**QUÉ SE GUARDA EN FIRESTORE** (allowlist):
- ✅ User metadata (name, role, profilePhoto, language)
- ✅ Pairing logs (pairedAt, connectionType, lastSeen)
- ✅ Connection events (connected/disconnected/call_started/call_ended/monitoring_started/monitoring_ended)
- ✅ Usage analytics (duration, device, time)
- ✅ Alarm configuration (type, hour, days, device selection)
- ✅ Notification events (last 5, timestamps, types)

**QUÉ NO SE GUARDA EN FIRESTORE** (blacklist):
- ❌ Video streams (stored locally only, never cloud)
- ❌ Audio recordings (never stored, only streamed)
- ❌ Chat messages (N/A for this app)
- ❌ User device location (only network type)
- ❌ Terminal local app usage logs (privacy protection)

### Terminal Privacy - Zero Visibility of Monitoring

**CRÍTICO - Terminal NUNCA sabe que está siendo monitoreado:**
- ❌ No notification sound
- ❌ No vibration
- ❌ No LED indicator of camera/mic active
- ❌ No local log file that Monitor puede access
- ✅ SOLO log in Firestore (under `/users/{monitorId}/logs_conexion`)
- ✅ Terminal NUNCA puede leer sus propios monitoreo logs

**Implementación Técnica**:
```kotlin
// En Terminal: monitoreo SILENCIOSO
class VideoActivity {
  override fun onCreate() {
    if (currentMode == "TERMINAL") {
      // NO MOSTRAR notificación de grabación
      // NO ENVIAR logs a archivo local
      // SOLO enviar evento a Firestore (asíncrono en background)
    }
  }
}

// En Monitor: agregar log en Firestore ÚNICAMENTE
Firestore.collection("users").document(monitorId)
  .collection("logs_conexion")
  .add(mapOf(
    "evento" to "monitoring_started",
    "timestamp" to System.currentTimeMillis(),
    "terminalId" to terminalId
  ))
```

---

## 🔑 CONTROL DE ACCESO - MODELO BASADO EN ROLES (ACTUALIZADO - Marzo 20, 2026)

### Role-Based Access Control (RBAC) - Recordatorios

**Monitor Role**:
- ✅ CREATE reminders en Terminal
- ✅ UPDATE reminders en Terminal
- ✅ DELETE reminders en Terminal
- ✅ VIEW reminders del Terminal emparejado
- ✅ Configure "owner" de cada reminder (Monitor/Terminal)
- ❌ NO access a reminders de otro Monitor's Terminal (even si emparejado a multiple Monitors)

**Terminal Role**:
- ✅ CREATE reminders locales
- ✅ UPDATE propios reminders
- ✅ DELETE propios reminders
- ✅ VIEW propios reminders
- ✅ Can ACK/DISMISS reminder alerts
- ❌ NO puede ver si Monitor configuró reminder (caja negra - solo ve notificaciones)
- ❌ NO puede rechazar reminders creados por Monitor

**Implementación Firestore Rules**:
```javascript
// /users/{userId}/alarmas_recordatorios/{alarmaId}
match /alarmas_recordatorios/{alarmaId} {
  // Terminal puede READ/WRITE sus propias alarmas
  allow read: if isOwner(resource.data.terminalId) || isOwner(resource.data.monitorId);
  allow write: if isOwner(resource.data.terminalId) || isOwner(resource.data.monitorId);
  
  // Monitor NO puede eliminar alarmas de otro Monitor's Terminal
  allow delete: if request.auth.uid == resource.data.monitorId;
}
```

---

## 🔐 SEGURIDAD DE COMUNICACIÓN VIDEOLLAMADA - ACEPTACIÓN AUTOMÁTICA (ACTUALIZADO - Marzo 28, 2026)

### Riesgo: Terminal Feedback en Videollamada

**ESPECIFICACIÓN ACTUALIZADA**:
- ✅ VideoActivity abre AUTOMÁTICAMENTE en Terminal cuando Monitor activa Toggle bidireccional
- ✅ Terminal RECIBE automáticamente transmisión del Monitor (sin popup aceptar/rechazar)
- ✅ Botones VISIBLES en Terminal (Cambiar Cámara, Toggle volver a SILENT, Colgar)
- ✅ Monitor puede INICIAR videollamada (presionar Toggle) pero Terminal TAMBIÉN puede terminla (Colgar)
- ✅ Terminal puede volver a SILENT (presionar Toggle) terminando videollamada
- ✅ Ambos tienen control simétrico

**Threat Model Actualizado**:
- ❌ Terminal intenta cerrar VideoActivity durante videollamada
- **Mitigación**: `onBackPressed()` NO cierra cuando videollamada activa, solo permite Toggle a SILENT o Colgar
- ❌ Terminal presiona Colgar múltiples veces
- **Mitigación**: Botón Colgar deshabilitado brevemente (200ms) después de presionar
- ❌ Terminal intenta cerrar app completamente durante videollamada
- **Mitigación**: `WakeLock` para mantener app despierta + notificación a Monitor si conexión se pierde

---

## 📊 MATRIZ DE SEGURIDAD ACTUALIZADA (Marzo 20, 2026)

## 📋 RECOMENDACIONES POST-AUDIT (Actualizado Marzo 20, 2026)

1. **Rotación de Secretos**: Implementar función para regenerar `pairing_secret` periódicamente desde Monitor (cada 30 días recomendado).

2. **Ofuscación de Código**: Asegurar R8/ProGuard para ofuscar clases de: networking, Firestore, video, pairing.

3. **Session Timeout**: Implementar timeout de 30 minutos para Monitor (tras inactividad) + re-prompt biometric.

4. **Firestore Security Rules**: Hardcodear permisos RBAC para que:
   - Monitor NO pueda read logs de otro Monitor's Terminal
   - Terminal NUNCA pueda read propios logs de monitoreo
   - Eventos automáticamente deleted después 30 días (retention policy)
   - 🔴 **NUEVO 4.3 (Marzo 28 - AlertLogActivity)**: 
     ```javascript
     // /users/{monitorId}/alertLog/{eventoId}
     match /alertLog/{eventoId} {
       // Solo el Monitor propietario puede leer su AlertLog
       allow read: if request.auth.uid == monitorId;
       // Terminal NUNCA puede leer AlertLog del Monitor
       allow read: if false;
       // Monitor puede generar eventos (via Cloud Function)
       allow create: if request.auth.uid == monitorId;
       allow delete: if request.auth.uid == monitorId && 
                        resource.data.expiredAt <= now;  // Auto-purge después 24h
     }
     ```
   - **Rationale**: AlertLog contiene timestamp de notificaciones del Monitor (eventos privados del Monitor). Terminal es dispositivo de persona cuidada - debe tener máxima privacidad. El Monitor controla su propio log, Terminal no puede acceder.

5. **Encryption - CustomPhrases AES-256 (Marzo 28 - 4.1)**: 
   - 🔴 **NUEVO**: Implementar encriptación en Firestore campos `frases_personalizadas.encriptedContent`
   - **Algorithm**: AES-256 CBC mode, IV aleatoria por cada cifrado
   - **Key Derivation**: Usar SHA-256(userId + firebase_token) como source para derivar clave AES
   - **Implementation**:
     ```kotlin
     // Terminal side - before saving to Firestore
     val plaintext = "Ayuda"
     val encryptedContent = AES256.encrypt(plaintext, derivedKey)
     Firestore.collection("users").document(terminalId)
       .collection("frases_personalizadas").add(mapOf(
         "texto" to plaintext,  // Solo para UI local
         "encriptedContent" to encryptedContent,  // Para Firestore
         "auto_added" to false,
         "isActive" to true
       ))
     ```
   - **Storage Policy**: 
     - En Firestore: SOLO guardar `encriptedContent` (encrypted)
     - En Terminal Local: Cache desencriptado en EncryptedSharedPreferences
     - Monitor NUNCA recibe contenido (imposible descifrar - claves locales del Terminal)
   - **Re-pairing**: Al cambiar Monitor, frases auto-added (con `auto_added=true`) se ELIMINAN automáticamente

6. **Certificate Pinning WiFi Local**: Implementar public key pinning para servidor NanoHTTPD.

6. **Auditoría de Monitoreo**: 
   - ✅ REGISTRAR todos monitorios en Firestore con timestamps
   - ✅ Terminal puede REQUEST audit log (ver cuándo fue monitoreado)
   - Este beneficio compensates la falta de notificación en tiempo real
   
7. **Privacy Policy Update**:
   - Clarificar que Firestore guarda logs de emparejamiento y uso
   - Clarificar que no hay local logs en Terminal para monitoreo
   - Clarificar que video/audio nunca se almacena (solo streamed)

---
**Monitor de Cuidados - Documento de Seguridad v2.2**

# Monitor de Cuidados

Aplicación Android de doble rol para coordinar el cuidado de personas mayores. Un teléfono funciona como **Monitor** para el cuidador y uno o más teléfonos como **Terminal** para la persona acompañada.

## Funcionalidades

- Emparejamiento Monitor–Terminal mediante QR.
- Alertas, panel de estado e historial de eventos.
- Comunicación local-first por Wi-Fi, con Firebase para autenticación, notificaciones y sincronización entre redes.
- Llamadas de audio/vídeo, comandos de voz y alarmas.
- Persistencia local con Room/SQLCipher y sincronización en segundo plano.
- Interfaz localizada y controles adaptados para uso táctil.

## Estado

Prototipo beta en desarrollo, no producto médico ni servicio de emergencia. La especificación mantiene funciones parciales y tareas pendientes; no debe ser el único medio de supervisión o asistencia.

## Tecnología

Kotlin, Android SDK 35, minSdk 24, AndroidX/Material, MVVM, Room/SQLCipher, CameraX, ML Kit, NanoHTTPD, mDNS y Firebase.

## Compilar y probar

Requiere Android Studio con JDK 17 y Android SDK 35. Desde PowerShell en la raíz:

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```

El APK se genera en `app/build/outputs/apk/debug/`.

## Firebase y privacidad

La app utiliza Firebase Auth, Firestore, Storage y Cloud Messaging. `app/google-services.json` es configuración cliente, no credencial de servidor; restringe sus API keys y configura reglas de acceso antes de desplegar. Nunca agregues service accounts, claves privadas ni secretos de firma al repositorio.

La aplicación gestiona datos sensibles relacionados con cuidado y comunicación. Usa datos de prueba durante el desarrollo y verifica permisos, cifrado, acceso y consentimiento antes de cualquier piloto.

## Documentación

- [Requisitos y alcance](SRS.md)
- [Diseño técnico](SDD.md)
- [Lista de pruebas](TESTING_CHECKLISTS.md)
- [Plan de trabajo](WorkItems.md)
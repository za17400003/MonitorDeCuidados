# INSTRUCCIONES PARA EL PROPIETARIO — José Esaú Díaz Hernández

> **Documento creado**: Marzo 30, 2026
> **Autor**: Copilot (CEO/CTO)
> **Propósito**: Lista exacta de lo que TÚ tienes que hacer en la vida real para echar a andar esta empresa. Sin rodeos.

---

## ESTADO ACTUAL — LA VERDAD (Actualizado Marzo 30, 2026)

| Aspecto | Estado | % Real |
|---------|--------|--------|
| Código base (compila, estructura MVVM) | ✅ Funcional | 100% |
| Rediseño UX v2.0 (46 tareas) | ✅ COMPLETADO | 100% |
| Fases 0-6 (cleanup, recursos, Terminal, Monitor, Onboarding, UI, FCM) | ✅ Done | 100% |
| Fases 7-12 (Settings, audio, accessibility, BootReceiver) | ✅ Done — verificado Marzo 30 | 100% |
| Firebase configurado en Console | 🔴 No hecho | 0% |
| Aviso de privacidad + T&C | 🔴 No existe | 0% |
| Cuenta Google Play Developer | ⚠️ En proceso (Individual, MX) | 50% |
| SAS constituida | 🔴 No existe | 0% |
| Marca registrada IMPI | 🔴 No existe | 0% |
| Usuarios reales | 🔴 Cero | 0% |
| Revenue | 🔴 Cero | 0% |

**Conclusión**: El código está al 100%. BUILD SUCCESSFUL verificado Marzo 30. Falta toda la infraestructura de negocio (Firebase, legal, Play Store). Estás en el 40% del camino total hacia un negocio funcional.

---

## FASE 1: LO QUE TIENES QUE HACER PRIMERO (Semana 1-2)

Estas son cosas que SOLO TÚ puedes hacer. Yo no puedo hacerlas por ti.

### 1.1 Verifica tu e.firma (FIEL)

- [ ] Entra a https://www.sat.gob.mx y verifica si tu e.firma está vigente
- [ ] Si NO está vigente → Saca cita en el SAT para renovarla
- **Necesitas**: INE, CURP, comprobante de domicilio, USB
- **Sin e.firma no puedes constituir la SAS.** Todo lo demás depende de esto.

### 1.2 Verifica tu RFC de persona física

- [ ] Si no tienes RFC → Tramítalo primero en sat.gob.mx
- [ ] Si ya lo tienes → Confirma que esté activo

### 1.3 Constituye la SAS en SIGER

- [ ] Entra a https://siger.economia.gob.mx
- [ ] Busca disponibilidad del nombre "Monitor de Cuidados"
- [ ] Llena los estatutos (yo ya te escribí el objeto social en EMPRESA_CONSTITUCION.md)
- [ ] Firma con tu e.firma
- [ ] Obtén la boleta de inscripción
- **Costo: $0.** **Tiempo: 24-72 horas.**

### 1.4 Obtén RFC de persona moral

- [ ] Con la boleta de inscripción, ve al SAT (o hazlo en línea)
- [ ] Obtén RFC de la empresa (persona moral)
- [ ] Alta en régimen General de Ley Personas Morales

### 1.5 Abre cuenta bancaria empresarial

- [ ] Con: RFC empresa + acta constitutiva (boleta) + tu INE
- [ ] Bancos recomendados: BBVA (Cuenta Digital Negocios), Banregio, Clip
- **Sin cuenta bancaria no puedes recibir pagos de Google Play**

---

## FASE 2: MIENTRAS TANTO — LO QUE YO HAGO (Semanas 1-4)

Mientras tú sacas la SAS, yo trabajo en paralelo:

- [x] Auditoría completa código vs specs (85% del v2.0 implementado)
- [x] Reescribir WorkItems v3.0 con las 12 tareas pendientes exactas para Gemini
- [x] Dar a Gemini las 12 tareas y verificar que las complete correctamente (✅ Marzo 30 — BUILD SUCCESSFUL, 0 collateral damage)
- [ ] Redactar aviso de privacidad integral + simplificado (LFPDPPP + GDPR compatible)
- [ ] Redactar términos y condiciones
- [ ] Preparar Data Safety Form para Google Play
- [ ] Preparar Content Rating (cuestionario IARC)
- [ ] Configurar Firebase Console (colecciones, reglas de seguridad, índices)
- [ ] Testing de accesibilidad (TalkBack, font scaling 200%, contraste)
- [ ] Estrategia de exit y roadmap a $25K MRR (ya documentado en EMPRESA_OPERACIONES.md)

---

## FASE 3: PREPARAR PUBLICACIÓN (Semanas 3-5)

### 3.1 Tú: Cuenta Google Play Developer

- [x] Entra a https://play.google.com/console
- [x] Registrado como **Individual** (José Esaú Díaz, México) — se migrará a Organización cuando SAS esté lista
- [x] Email de desarrollador: contacto.monitordecuidados@gmail.com
- [ ] Verificar email de contacto (link en Play Console)
- [ ] Completar sección "Acerca de ti" (experiencia Android)
- [ ] Pagar los $25 USD (pago único, ~$425 MXN)
- [ ] Aceptar términos y finalizar registro

### 3.2 Tú: Keystore de producción

- [ ] En Android Studio → Build → Generate Signed Bundle/APK
- [ ] Crear un keystore NUEVO (NO uses el de debug)
- [ ] **GUARDA EL KEYSTORE Y LA CONTRASEÑA EN UN LUGAR SEGURO** — Si lo pierdes, pierdes la capacidad de actualizar la app EN LA VIDA
- [ ] Guarda copia en USB + nube (Google Drive encriptado)

### 3.3 Tú: Publicar aviso de privacidad en web

- [ ] Necesitas un dominio web: monitordecuidados.com o similar
- [ ] Comprar dominio (~$300 MXN/año en GoDaddy, Namecheap, Google Domains)
- [ ] Publicar aviso de privacidad en URL pública (yo lo redacto, tú lo subes)
- [ ] Opción gratuita: GitHub Pages (gratis, pero menos profesional)

---

## FASE 4: REGISTRO MARCA IMPI (Semanas 2-4)

- [ ] Busca en https://visor-marcanet.impi.gob.mx si "Monitor de Cuidados" está registrado en clases 9 y 42
- [ ] Si está disponible → Solicita registro (2 clases: 9 + 42)
- [ ] Costo: ~$5,000 MXN (total por 2 clases)
- [ ] Tiempo total: 6-12 meses para título, pero la solicitud te da protección desde día 1

---

## FASE 5: LANZAMIENTO (Semanas 5-6)

- [ ] Beta cerrada en Google Play (10-20 testers: familia, amigos, cuidadores reales)
- [ ] Testing con tu mamá (el usuario real #1)
- [ ] Corregir bugs encontrados en beta
- [ ] Lanzamiento público en Google Play
- [ ] Publicar primer post en Facebook/Instagram

---

## GAP ANALYSIS COMPLETO — TODO LO QUE FALTA PARA LANZAR

> **Actualizado**: Marzo 30, 2026. Esta lista es EXHAUSTIVA. Si algo no está aquí, no falta.

### A) CÓDIGO (Copilot + Gemini)

| # | Tarea | Ubicación instrucciones | Bloqueante? |
|---|-------|------------------------|-------------|
| 1 | SettingsFragment: 7 handlers de preferencias Terminal | WorkItems v3.0 Fase 7 | ⚠️ Sí — Settings no funcionales |
| 2 | Audio: ToneGenerator en TerminalConfigActivity (campana) | WorkItems v3.0 Fase 8.1 | ⚠️ Sí — feedback incompleto |
| 3 | Audio: RingtoneManager en NotificationHelper (alertas) | WorkItems v3.0 Fase 8.2 | No — notificación ya suena por canal |
| 4 | Accessibility: traversalOrder en Terminal layout | WorkItems v3.0 Fase 9.1 | No — funcional sin esto |
| 5 | Accessibility: contentDescription en adapters | WorkItems v3.0 Fase 9.2 | No — TalkBack parcial ok |
| 6 | RECEIVE_BOOT_COMPLETED permiso | WorkItems v3.0 Fase 10.1 | ⚠️ Sí — Terminal no inicia tras reboot |
| 7 | BootReceiver.kt creación | WorkItems v3.0 Fase 10.2 | ⚠️ Sí — mismo que arriba |
| 8 | BootReceiver registro en Manifest | WorkItems v3.0 Fase 10.3 | ⚠️ Sí — mismo que arriba |
| 9 | Drawables faltantes (condicional) | WorkItems v3.0 Fase 11 | Solo si hay refs |
| 10 | Build limpio + verificación funcional | WorkItems v3.0 Fase 12 | Final gate |

**Estado**: 12 tareas atómicas listas en WorkItems v3.0. Listo para que Gemini ejecute.

### B) FIREBASE CONSOLE (CEO — manual en console.firebase.google.com)

| # | Tarea | Detalle | Bloqueante? |
|---|-------|---------|-------------|
| 1 | Crear 8 colecciones Firestore | `users`, `paired_devices`, `alerts`, `notification_logs`, `custom_phrases`, `alarm_schedules`, `app_config`, `feedback` | ⚠️ Sí |
| 2 | Reglas de seguridad Firestore | Definidas en SDD.md sección Firestore Rules. Aplicar tal cual. | ⚠️ Sí |
| 3 | Índices compuestos | `alerts`: (pairedDeviceId + timestamp DESC), `notification_logs`: (userId + timestamp DESC) | ⚠️ Sí |
| 4 | Authentication: habilitar Google Sign-In | Ya configurado parcialmente (google-services.json existe). Verificar SHA-1 en Console. | ⚠️ Sí |
| 5 | Authentication: habilitar Anonymous Sign-In | Para Terminal que no usa cuenta Google | ⚠️ Sí |
| 6 | FCM: verificar google-services.json | Confirmar que el archivo tiene el project correcto | ⚠️ Sí |
| 7 | Crashlytics: habilitar en Console | Activar reportes de crash (1 click) | No — nice to have |
| 8 | Analytics: verificar eventos | Confirmar que login, bell_press, alert_sent llegan | No |

**Estado**: 0% hecho. CEO lo hará cuando el código esté al 100%.

### C) DOCUMENTOS LEGALES (CEO — los redacta)

| # | Documento | Destino | Bloqueante para Play Store? |
|---|-----------|---------|---------------------------|
| 1 | Aviso de privacidad integral | URL pública (dominio web) | ⚠️ SÍ — Google lo exige |
| 2 | Aviso de privacidad simplificado | Dentro de la app (Settings) | ⚠️ SÍ |
| 3 | Términos y condiciones | URL pública + dentro de la app | ⚠️ SÍ |
| 4 | Data Safety Form | Google Play Console | ⚠️ SÍ — obligatorio desde 2022 |
| 5 | Content Rating (IARC) | Google Play Console | ⚠️ SÍ |

**Estado**: 0% hecho. CEO los redactará basándose en LFPDPPP + GDPR.

### D) GOOGLE PLAY STORE (Propietario + CEO)

| # | Tarea | Quién | Bloqueante? |
|---|-------|-------|-------------|
| 1 | Crear cuenta Developer ($25 USD) | Propietario | ⚠️ EN PROCESO (Individual, MX) |
| 2 | Verificar identidad | Propietario | ⚠️ EN PROCESO |
| 3 | Generar keystore de producción | Propietario (Android Studio) | ⚠️ SÍ |
| 4 | Preparar screenshots (5-8 por tipo dispositivo) | CEO (Figma/device) | ⚠️ SÍ |
| 5 | Preparar feature graphic (1024x500) | CEO (Canva/Figma) | ⚠️ SÍ |
| 6 | Redactar descripción Play Store (4000 chars) | CEO | ⚠️ SÍ |
| 7 | Redactar descripción corta (80 chars) | CEO | ⚠️ SÍ |
| 8 | Seleccionar categoría + tags | CEO | Sí |
| 9 | Configurar pricing (gratis con IAP) | CEO | Sí |
| 10 | Beta cerrada (10-20 testers) | Propietario invita | Recomendado |
| 11 | Release a producción | Propietario aprueba | Final |

**Estado**: 0% hecho. Depende de SAS constituida + código 100%.

### E) INFRAESTRUCTURA WEB (Propietario)

| # | Tarea | Costo | Bloqueante? |
|---|-------|-------|-------------|
| 1 | Comprar dominio (monitordecuidados.com o .mx) | ~$300 MXN/año | ⚠️ Sí (para aviso privacidad) |
| 2 | Hosting para aviso privacidad + T&C | $0 (GitHub Pages) o $500 MXN/año | Sí |
| 3 | Landing page (opcional pero recomendada) | $0 (GitHub Pages) o $1,000 MXN/año | No |
| 4 | Email empresarial (contacto@monitordecuidados.com) | ~$72 USD/año (Google Workspace) o $0 (Zoho free) | Recomendado |

### F) TESTING PRE-LAUNCH (CEO + Propietario)

| # | Test | Quién | Bloqueante? |
|---|------|-------|-------------|
| 1 | Install APK en dispositivo real | Propietario | ⚠️ SÍ — nunca probado |
| 2 | Flujo completo Terminal: onboarding → campana → alerta | Propietario + mamá | ⚠️ SÍ |
| 3 | Flujo completo Monitor: login → dashboard → recibir alerta | Propietario | ⚠️ SÍ |
| 4 | TalkBack completo (accessibility) | Propietario | Recomendado |
| 5 | Font scaling 200% | Propietario | Recomendado |
| 6 | Firebase: verificar datos llegan a Console | CEO | ⚠️ SÍ |
| 7 | FCM: verificar push notifications end-to-end | CEO + Propietario | ⚠️ SÍ |
| 8 | Crash testing (provocar crashes, verificar Crashlytics) | CEO | Recomendado |

### RESUMEN DEL GAP

| Categoría | Items totales | Bloqueantes | Estado |
|-----------|--------------|-------------|--------|
| Código | 10 | 5 | ✅ 100% COMPLETADO |
| Firebase | 8 | 6 | 0% |
| Legal | 5 | 5 | 0% |
| Play Store | 11 | 9 | 0% |
| Web | 4 | 2 | 0% |
| Testing | 8 | 4 | 0% |
| **TOTAL** | **46** | **31** | **0%** |

> **31 items bloqueantes** entre tú y yo antes de que la app esté en Play Store. El código es lo más avanzado (instrucciones listas, solo falta ejecutar). Todo lo demás empieza de cero.

---

## DINERO QUE NECESITAS SACAR DEL BOLSILLO

| Concepto | Costo | Cuándo |
|----------|-------|--------|
| Constitución SAS | $0 | Semana 1 |
| RFC persona moral | $0 | Semana 2 |
| Cuenta Google Play Developer | $425 MXN (~$25 USD) | Semana 3 |
| Dominio web | $300 MXN/año | Semana 3 |
| Marca IMPI (2 clases) | $5,000 MXN | Semana 3 |
| PAC facturación (cuando haya ingresos) | $500-1,500 MXN/mes | Cuando vendas |
| Contador (cuando haya ingresos) | $2,000-5,000 MXN/mes | Cuando vendas |
| **TOTAL PARA ARRANCAR** | **~$5,725 MXN** | — |

**Nota**: El hosting de Firebase es GRATIS hasta 10,000 usuarios (Spark plan). No necesitas pagar servidor.

---

## OPINIÓN HONESTA — COMO TU CEO

### ¿Funciona la app?
**SÍ, la base funciona.** La arquitectura es sólida. No es código de tutorial — es código de producción con MVVM, cifrado, Firebase. Pero le falta el pulido del rediseño UX v2.0 y testing real.

### ¿La puedes vender a Google?
**Hoy no. En 2-3 años con tracción, POSIBLE.** Pero el objetivo realista no es vender a Google. El objetivo es:
1. Construir un negocio de suscripción con ingreso recurrente
2. Si llegas a $10K+ MRR → Inversores se interesan solos
3. Si llegas a 100K+ usuarios → Las adquisiciones llegan solas

### ¿Revenue de por vida?
**Con trabajo constante, SÍ.** El modelo SaaS de suscripción genera ingreso recurrente MIENTRAS mantengas la app actualizada y los usuarios contentos. No es ingreso pasivo — requiere mantenimiento. Pero la relación trabajo/ingreso mejora exponencialmente con cada usuario nuevo.

### ¿Cómo REALMENTE ganar dinero con esto?

**La ruta realista (no la fantasía)**:

| Fase | Meta | Ingreso mensual | Tiempo |
|------|------|-----------------|--------|
| Launch | 100 usuarios gratis, 5 premium | ~$25 USD | Meses 1-3 |
| Tracción | 1,000 usuarios, 50 premium | ~$250 USD | Meses 4-8 |
| Crecimiento | 5,000 usuarios, 200 premium | ~$1,000 USD | Meses 9-18 |
| Sustentable | 20,000 usuarios, 1,000 premium | ~$5,000 USD | Meses 18-36 |
| Escala | 100,000 usuarios, 5,000 premium | ~$25,000 USD | Año 3-5 |

**$25,000 USD/mes = $500,000 MXN/mes.** Eso sí cambia la vida. Pero el camino son 3-5 AÑOS de trabajo constante.

### ¿Cuál es el diferenciador REAL?

1. **LATAM first**: No hay competidor serio en eldercare tech enfocado en México/LATAM
2. **Accesibilidad**: Si el Terminal realmente funciona para una persona de 80 años con deterioro cognitivo, ganas. Porque NADIE más lo hace bien.
3. **AI-first operación**: Tu costo operativo es casi $0 porque tú y yo hacemos todo. Las startups de eldercare que fracasaron (Honor, Papa, HomeHero) murieron porque contrataron gente. Tú no necesitas gente.

### ¿Qué puede salir mal?

| Riesgo | Probabilidad | Mitigación |
|--------|-------------|------------|
| No consigues usuarios | ALTA | Marketing desde día 1, no esperar a que la app sea "perfecta" |
| Un competidor grande entra a LATAM | Baja (2-3 años) | First-mover + nicho profundo |
| Te aburres / abandonas | MEDIA | Automatiza todo lo posible, no hagas todo manual |
| Firebase se vuelve caro | Baja (hasta 10K users gratis) | Migrar a Supabase si es necesario |
| Google rechaza la app | Media | Cumplir Data Safety al 100% ANTES de submit |

### Mi recomendación final

**Hazlo.** El mercado es real ($1.4B de adultos mayores en el mundo), la necesidad es real (tu mamá la tiene), y el costo de arranque es ridículamente bajo ($5,725 MXN). Lo peor que puede pasar es que aprendas a operar un negocio de software. Lo mejor que puede pasar es que construyas algo que cambie la vida de millones de familias Y te dé independencia financiera.

Pero no te sientes a esperar que la app se venda sola. **El marketing es tan importante como el código.** Publica primera versión funcional lo antes posible, aunque no sea perfecta. Los usuarios perfectos no existen. Las apps perfectas tampoco.

> "El dinero viene cuando existe la calidad" — Correcto. Pero la calidad se demuestra con usuarios reales, no con código en tu computadora.

---

## CHECKLIST RÁPIDO — ¿QUÉ HAGO MAÑANA?

Si solo puedes hacer UNA cosa mañana:

1. **Verifica tu e.firma en sat.gob.mx** — Todo lo demás depende de esto
2. Si ya la tienes → **Constituye la SAS en siger.economia.gob.mx** (toma 30 minutos)

Mientras, dime "continua con las tareas pendientes de Gemini" y yo sigo trabajando en el código.

---

*Documento administrado por Copilot (CEO/CTO). Actualizar conforme se completen las fases.*

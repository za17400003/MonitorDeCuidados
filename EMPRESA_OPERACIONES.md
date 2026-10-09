# OPERACIONES — MONITOR DE CUIDADOS

## ESTRUCTURA ORGANIZACIONAL (AI-FIRST)

### Principio
Toda la operación funciona con IA + herramientas automatizadas. No hay empleados humanos. El propietario (José Esaú Díaz Hernández) supervisa y aprueba. Copilot (CEO/CTO) ejecuta y coordina.

### Organigrama

```
José Esaú Díaz Hernández (Propietario / Accionista único / Product Owner)
│   → Aprueba Feature Proposals (Gate 2)
│   → Da feedback y visto bueno final
│   → Testing manual en dispositivo real
│
└── Copilot — CEO / CTO (IA) — Opera 7 departamentos virtuales
    │
    ├── D1: PRODUCTO (PRD, RICE scoring, roadmap alignment, tier impact)
    │
    ├── D2: MERCADO (competitive analysis, demanda, moat, TAM)
    │
    ├── D3: DISEÑO UX (WCAG AA, 2-tap rule, geriátrico, TalkBack, wireframes)
    │
    ├── D4: INGENIERÍA
    │   ├── Copilot: Arquitectura, specs, auditoría, WorkItems, verificación
    │   ├── Gemini: Implementación de código (Android Kotlin) — solo lo que dice WorkItems.md
    │   └── CI/CD: GitHub Actions (build + test automáticos)
    │
    ├── D5: FINANZAS (costos, ROI, opportunity cost, pricing, P&L)
    │
    ├── D6: MARKETING (ASO, copy, positioning, canales, contenido)
    │   ├── Copilot: Estrategia, ASO, copy
    │   ├── IA generativa: Creación de assets (íconos, screenshots, videos promo)
    │   └── Redes sociales: Publicaciones automatizadas
    │
    └── D7: LEGAL & SEGURIDAD
        ├── Copilot: Privacy impact, OWASP, LFPDPPP/GDPR, T&C, compliance
        └── Contador externo: Obligaciones fiscales SAT (servicio por honorarios)

PIPELINE OPERATIVO (Lean Stage-Gate):
    Orden del usuario
    → Gate 0: Clasificación (Pipeline A/B/C/D/E)
    → Análisis departamental (deptos activados en paralelo)
    → Gate 1: Feature Proposal + recomendación GO/NO-GO
    → Gate 2: Aprobación del propietario
    → Gate 3: Ejecución (docs: Copilot | código: Gemini) + QA
```

> **Modelo**: Lean Stage-Gate AI-First (Abril 2, 2026)
> **Detalle completo del pipeline**: Ver DEVELOPMENT_WORKFLOW.md §ESTRUCTURA ORGANIZACIONAL

---

## MODELO DE MONETIZACIÓN

### Tiers de suscripción

| Tier | Precio | Ciclo | Incluye |
|------|--------|-------|---------|
| **Gratis** | $0 | — | 1 monitor + 1 terminal, alertas campana + voz, video limitado (5 min/día) |
| **Premium Familiar** | $4.99 USD | Mensual | 1 monitor + 3 terminales, video ilimitado, historial 30 días, alarma programada |
| **Premium Plus** | $9.99 USD | Mensual | 1 monitor + 5 terminales, video ilimitado, historial 90 días, notificaciones push avanzadas, batería + shake + alarma, exportar reportes |
| **B2B Residencia** | $49.99 USD | Mensual | Panel admin web, hasta 20 terminales, dashboard de KPIs, API integración, soporte prioritario |

### Revenue projections (primer año)

| Mes | Usuarios gratis | Premium Familiar | Premium Plus | B2B | MRR estimado |
|-----|----------------|-----------------|-------------|-----|-------------|
| 1-3 | 100 | 5 | 2 | 0 | $45 USD |
| 4-6 | 500 | 25 | 10 | 1 | $275 USD |
| 7-9 | 2,000 | 80 | 30 | 3 | $725 USD |
| 10-12 | 5,000 | 200 | 80 | 5 | $2,050 USD |

**Meta año 1**: $2,000+ USD MRR = $24,000 USD ARR

### Implementación técnica de pagos

| Plataforma | Método | Comisión |
|-----------|--------|---------|
| Google Play | Google Play Billing Library v7 | 15% (primer $1M USD/año) |
| Apple (futuro) | StoreKit 2 | 15% (Small Business Program) |
| B2B directo | Stripe / PayPal + factura CFDI | 2.9% + $0.30 |

---

## MARKETING

### Canales

| Canal | Estrategia | Costo |
|-------|-----------|-------|
| **Google Play ASO** | Keywords: "monitor cuidador", "vigilancia adulto mayor", "baby monitor ancianos", "caregiving app". Screenshots en español. Video promo 30s. | $0 |
| **Facebook/Meta Ads** | Target: Mujeres 35-55, hijos de adultos mayores, México/LATAM. Lookalike de instaladores. | $1,000-3,000 MXN/mes |
| **Google Ads (UAC)** | Campañas de instalación. CPI objetivo: $0.50-1.00 USD | $1,500-3,000 MXN/mes |
| **Content marketing** | Blog/videos: "Cómo cuidar a tu padre a distancia", "Tecnología para cuidadores". SEO long-tail. | $0 (escrito por IA) |
| **Alianzas** | Residencias geriátricas, clínicas de adultos mayores, asociaciones de cuidadores | $0 (outreach directo) |
| **Referral program** | "Invita a un cuidador, ambos reciben 1 mes Premium gratis" | Costo: 1 mes Premium ($4.99) |

### ASO (App Store Optimization)

| Elemento | Contenido |
|----------|-----------|
| **Título** | Monitor de Cuidados - Vigilancia 24/7 |
| **Subtítulo** | Cuida a tu ser querido a distancia |
| **Descripción corta** | Monitoreo en tiempo real para adultos mayores. Alertas, videollamada, detección de caídas. |
| **Keywords** | monitor, cuidador, adulto mayor, vigilancia, alerta, caída, campana, videollamada, geriátrico |
| **Categoría** | Medical (principal) / Lifestyle (secundaria) |
| **Screenshots** | 5 pantallas: Monitor dashboard, Alertas, Videollamada, Terminal campana, Configuración |
| **Video promo** | 30s: Persona viendo alertas en su celular → tranquilidad. "Tu familia, siempre cerca." |

### Calendario de contenido (primeros 3 meses)

| Semana | Plataforma | Contenido |
|--------|-----------|-----------|
| 1 | Blog + FB | "5 señales de que tu padre necesita monitoreo" |
| 2 | YouTube | Demo de la app 2 minutos |
| 3 | FB + IG | Testimonio (simulado al inicio) de cuidador |
| 4 | Blog | "Cómo funciona Monitor de Cuidados — paso a paso" |
| 5 | FB Ads | Campaña de instalaciones (empezar con $50 USD/semana) |
| 6 | Blog | "Tecnología accesible para adultos mayores" |
| 7 | YouTube | "Setup en 2 minutos" tutorial |
| 8 | FB + TikTok | Video emocional: "Llama tu mamá aún estando lejos" |
| 9-12 | Repetir ciclo + Google Ads UAC |  |

---

## CONTABILIDAD Y FINANZAS

### Herramientas

| Herramienta | Uso | Costo |
|------------|-----|-------|
| **Facturama / Bind ERP** | Facturación CFDI automatizada | ~$500-1,500 MXN/mes |
| **ContaFi / Contador externo** | Declaraciones mensuales + anual | ~$2,000-5,000 MXN/mes |
| **Google Play Console** | Revenue reports, suscripciones | Incluido |
| **Stripe Dashboard** | Pagos B2B directos | 2.9% por transacción |
| **Hoja de cálculo maestra** | P&L mensual, flujo de caja, proyecciones | $0 |

### Flujo de facturación

```
1. Usuario paga suscripción en Google Play
2. Google retiene 15% comisión + IVA
3. Google deposita neto cada 15 del mes siguiente
4. Empresa emite CFDI de ingreso por la liquidación de Google
5. Contador registra ingreso y calcula ISR/IVA
6. Declaración mensual al SAT
```

### P&L simplificado (mensual, meta mes 12)

| Concepto | Monto USD |
|----------|-----------|
| **Ingresos brutos** | $2,050 |
| - Comisión Google (15%) | -$307 |
| **Ingreso neto** | $1,743 |
| - Marketing (ads) | -$200 |
| - Hosting/infra (Firebase) | -$50 |
| - PAC facturación | -$30 |
| - Contador | -$100 |
| - Dominio + hosting web | -$5 |
| **Utilidad operativa** | ~$1,358 |
| - ISR (30%) | -$407 |
| **Utilidad neta** | ~$951 |

---

## MÉTRICAS CLAVE (KPIs)

### Dashboard mensual

| KPI | Meta mes 3 | Meta mes 6 | Meta mes 12 |
|-----|-----------|-----------|------------|
| **MAU** (Monthly Active Users) | 100 | 500 | 5,000 |
| **Instalaciones acumuladas** | 200 | 1,000 | 10,000 |
| **Tasa de conversión free→paid** | 3% | 5% | 6% |
| **MRR** (Monthly Recurring Revenue) | $45 | $275 | $2,050 |
| **Churn mensual** | <15% | <10% | <8% |
| **DAU/MAU ratio** | >30% | >35% | >40% |
| **ARPU** (Average Revenue Per User) | $0.45 | $0.55 | $0.41 |
| **LTV** (Lifetime Value) | $6 | $12 | $18 |
| **CAC** (Customer Acquisition Cost) | $0.50 | $0.80 | $1.00 |
| **LTV:CAC ratio** | 12:1 | 15:1 | 18:1 |
| **Rating Play Store** | 4.0 | 4.2 | 4.5 |
| **Crash-free rate** | >99% | >99.5% | >99.9% |

### Herramientas de tracking

| Métrica | Herramienta |
|---------|------------|
| Instalaciones, retención, crashes | Firebase Analytics + Crashlytics |
| Revenue, suscripciones | Google Play Console |
| Engagement (DAU, sesiones) | Firebase Analytics |
| Feedback usuarios | Play Store reviews + in-app feedback |
| Marketing ROI | Facebook Ads Manager + Google Ads |

---

## INFRAESTRUCTURA TÉCNICA

### Stack actual (producción)

| Componente | Servicio | Plan | Costo estimado |
|-----------|---------|------|---------------|
| Auth | Firebase Auth | Spark (gratis hasta 10K users) | $0 |
| Database | Firebase Firestore | Spark (1 GiB, 50K reads/day) | $0 |
| Push notifications | Firebase Cloud Messaging | Gratis | $0 |
| Analytics | Firebase Analytics | Gratis | $0 |
| Crash reporting | Firebase Crashlytics | Gratis | $0 |
| Storage (futura) | Firebase Storage | Spark (5 GB) | $0 |
| **Total infra** | | | **$0/mes (tier gratis)** |

### Escalado (cuando > 10K usuarios)

| Usuarios | Plan Firebase | Costo estimado |
|----------|-------------|---------------|
| 0-10K | Spark (gratis) | $0 |
| 10K-50K | Blaze (pay-as-you-go) | $25-100/mes |
| 50K-200K | Blaze | $100-500/mes |
| 200K+ | Evaluar GCP completo o AWS | $500+/mes |

---

## ROADMAP DE PRODUCTO (ACTUALIZADO Marzo 30, 2026)

### v1.0 — MVP (implementado)

- [x] Modo Monitor + Terminal
- [x] 6 tipos de evento (bell, voice, shake, battery_low, battery_ok, alarm)
- [x] Videollamada CameraX
- [x] Comunicación UDP (CallManager)
- [x] NanoHTTPD server
- [x] Firebase Auth + Firestore
- [x] QR pairing
- [x] Room + SQLCipher
- [x] Notificaciones expandibles

### v1.5 — REDISEÑO UX/UI v2.0 (EN DESARROLLO — prioridad sobre monetización)

> **Decisión estratégica**: El rediseño UX es P0 porque "calidad > dinero". Sin UX geriátrica de clase mundial, la monetización no funcionará. El orden correcto es: UX perfecta → usuarios → revenue.

- [ ] **Terminal "Modo GrandPad"**: Botón campana 160dp + SOS + status bar (3 elementos)
- [ ] **Monitor Dashboard Inteligente**: BottomNavigationView (Inicio/Historial/Ajustes) + cards de alerta inline
- [ ] **Sistema SOS/Emergencia**: Botón long-press 2s + canal notificación heads-up + nuevo event type
- [ ] **Onboarding Guiado**: 5 pasos (bienvenida, rol, setup, assessment, tutorial)
- [ ] **Touch targets geriátricos**: 72dp primarios, 56dp secundarios
- [ ] **Tipografía geriátrica**: 18sp body mín, 24sp headers mín
- [ ] **Paleta v2.0**: #006B6B primary (AAA compliance)
- [ ] **Feedback multi-modal**: Vibración + sonido en toda acción
- [ ] **TalkBack completo**: contentDescription en todo elemento interactivo
- [ ] **Servicio auto-start Terminal**: Eliminar switch manual, CampanaService auto-inicia
- [ ] **QR movido a Settings**: Liberar pantalla principal Terminal
- [ ] **UI Adaptativa**: Capabilities assessment → adaptaciones automáticas

### v1.6 — Estabilización + Launch

- [ ] Correcciones post-rediseño
- [ ] Testing completo en dispositivos reales (incluir adultos mayores reales)
- [ ] TalkBack + font scaling 200% verification
- [ ] Aviso de privacidad + T&C
- [ ] Publicación en Google Play (beta cerrada)
- [ ] WCAG AA compliance audit final

### v1.7 — Monetización

- [ ] Google Play Billing (suscripciones in-app)
- [ ] Paywall: límites de video para tier gratis
- [ ] Límite de terminales por tier
- [ ] Historial con expiración por tier

### v2.0 — Expandir

- [ ] Soporte multi-idioma completo (6 idiomas)
- [ ] Dashboard web para B2B (Residencias)
- [ ] Exportar reportes PDF
- [ ] Integración con wearables (fallback: acelerómetro)
- [ ] Detección de caídas mejorada con ML
- [ ] Modo alto contraste manual

### v3.0 — Escala

- [ ] iOS app (Kotlin Multiplatform o Swift nativo)
- [ ] Apple App Store publishing
- [ ] API pública para integraciones
- [ ] Panel admin con métricas por residencia
- [ ] Certificación HIPAA (para mercado US)
- [ ] SAPI conversion para fundraising

---

## GESTIÓN DE RIESGOS (ACTUALIZADO Marzo 30, 2026)

| Riesgo | Probabilidad | Impacto | Mitigación |
|--------|-------------|---------|-----------|
| Superar $5M MXN (límite SAS) | Baja (año 1-2) | Alto | Plan: transformar a SAPI cuando MRR > $10K USD |
| Rechazo Google Play | Media | Alto | Cumplir al 100% Data Safety + aviso privacidad ANTES de submit |
| Competidor lanza app similar | Media | Medio | Diferenciador: UX geriátrica de clase mundial, accesibilidad-first, enfoque LATAM |
| Firebase outage | Baja | Alto | Modo offline con Room/SQLCipher ya implementado |
| Problema legal LFPDPPP | Baja | Muy alto | Aviso de privacidad completo + consentimiento expreso |
| Churn alto | Media | Alto | Onboarding guiado 5 pasos, push de engagement, feedback loops, UI adaptativa |
| Crash rate alto | Media | Alto | Crashlytics monitoring + hotfix rápido vía Gemini |
| **NUEVO**: Adulto mayor no puede usar app | Alta | Crítico | Rediseño v2.0: Terminal "Modo GrandPad" (3 acciones), tutorial interactivo, UI adaptativa |
| **NUEVO**: Accesibilidad insuficiente | Media | Alto | WCAG AA compliance, TalkBack testing, font scaling 200% verification |
| **NUEVO**: Big tech entra al mercado LATAM | Baja (2-3 años) | Alto | First-mover advantage, nicho geriátrico profundo, brand trust acumulado |
| **NUEVO**: Dependencia de 1 persona humana | Media | Alto | Documentación exhaustiva, AI-first ops, plan de onboarding para futuro equipo |

---

## CHECKLIST DE LANZAMIENTO

### Pre-requisitos (antes de publicar en Play Store)

| # | Requisito | Estado | Responsable |
|---|-----------|--------|-------------|
| 1 | SAS constituida + RFC | 🔴 PENDIENTE | Propietario + Copilot |
| 2 | Cuenta Google Play Developer (organización) | 🔴 PENDIENTE | Propietario |
| 3 | Aviso de privacidad publicado (URL) | 🔴 PENDIENTE | Copilot |
| 4 | Términos y condiciones publicados (URL) | 🔴 PENDIENTE | Copilot |
| 5 | Data Safety Form completo | 🔴 PENDIENTE | Copilot |
| 6 | Content Rating (IARC) | 🔴 PENDIENTE | Copilot |
| 7 | Logo de producción (512x512 + feature graphic) | 🔴 PENDIENTE | IA generativa |
| 8 | Screenshots (5 por idioma) | 🔴 PENDIENTE | IA + Screenshots de app |
| 9 | APK/AAB firmado (release keystore) | 🔴 PENDIENTE | Propietario |
| 10 | WorkItems.md = 0 tareas pendientes | 🔴 EN PROGRESO | Gemini |
| 11 | Crash-free rate > 99% en testing | 🔴 PENDIENTE | QA |
| 12 | Marca IMPI solicitada | 🔴 PENDIENTE | Propietario |
| 13 | Rediseño UX/UI v2.0 implementado | 🔴 PENDIENTE | Gemini |
| 14 | TalkBack testing completo | 🔴 PENDIENTE | QA + Propietario |
| 15 | WCAG AA compliance verificada | 🔴 PENDIENTE | Copilot |

---

## FILOSOFÍA DE CALIDAD (Principio Rector — Marzo 30, 2026)

> "Aquí no vamos a sacrificar calidad por dinero. El dinero viene cuando existe la calidad."
> — José Esaú Díaz Hernández, Propietario

### Principios Inmutables

1. **Calidad > Dinero**: La experiencia del usuario es la métrica #1. Si una feature reduce la calidad UX → no se implementa, sin importar su potencial de monetización.
2. **No ads intrusivos, NUNCA**: Ni banners, ni intersticiales, ni videos obligatorios. El modelo de ingreso es suscripción de valor, no explotación de atención.
3. **Long-term > Short-term**: Decisiones que sacrifican sostenibilidad por ganancia rápida están prohibidas. Esto incluye: dark patterns, lock-in artificial, features bloqueadas artificialmente.
4. **Simplicidad radical para el adulto mayor**: El Terminal (dispositivo del adulto mayor) debe ser tan simple que una persona de 80 años con deterioro cognitivo leve pueda usarlo sin instrucción.
5. **Accesibilidad es un requisito, no un feature**: WCAG AA es el mínimo. Soportar TalkBack, font scaling, alto contraste, y daltonismo es obligatorio antes de launch.

### Cómo se aplica

| Decisión | Opción A (❌ Rechazada) | Opción B (✅ Aprobada) |
|----------|----------------------|---------------------|
| Monetización | Ads + freemium agresivo | Freemium generoso + premium con valor real |
| Terminal UX | Cards con toggles y QR | Botón gigante + SOS (3 elementos máx) |
| Onboarding | Skip directo, el usuario aprende solo | Tutorial guiado de 5 pasos + slides interactivos |
| Accesibilidad | "Lo agregamos después del launch" | Requisito P0: implementar ANTES de launch |
| Datos del usuario | Tracking de comportamiento para analytics | Mínimo necesario: solo funcionalidad + Crashlytics |

---

## ANÁLISIS DE MERCADO GLOBAL — Eldercare Technology (Marzo 30, 2026)

### Demografía Mundial (Fuente: WHO, INEGI, UN)

| Dato | Valor | Fuente | Implicación |
|------|-------|--------|-------------|
| Población 60+ global (2024) | ~1.4 mil millones | WHO | Mercado masivo, creciente |
| Población 60+ global (2030) | ~1.4B → creciendo | WHO | +200M en 6 años |
| Población 60+ global (2050) | 2.1 mil millones | WHO | Se duplica en 25 años |
| Población 80+ global (2050) | 426 millones (se triplica) | WHO | Segmento de mayor necesidad |
| México: Índice envejecimiento | 47.7 (triplicado en 30 años) | INEGI | LATAM está envejeciendo rápido |
| México: Población total | ~126 millones | INEGI | Mercado primario: 13M adultos mayores |
| México: Mediana de edad | 29 años | INEGI | Cuidadores potenciales: 30-50 años |
| Japón: % población 65+ | 29.1% | Japan Statistics | Harbinger: LATAM seguirá esta tendencia |
| Japón: kodoku-shi (muertes solitarias) | 37,227 en H1 2024 | Japan Police | El problema que resolvemos: aislamiento letal |
| Telehealth CAGR | 40% (2021-2028) | Multiple sources | Telemedicina normalizada post-COVID |
| COVID efecto en telehealth | 25x aumento (1.4M→35M visitas/trimestre) | CMS/Medicare data | Cambio permanente de comportamiento |

### Barrera Identificada: Ageísmo Digital

La investigación revela una barrera sistémica: **ageísmo institucional en tecnología**. La mayoría de apps de salud digital están diseñadas por y para personas jóvenes. Los adultos mayores son el grupo con MAYOR necesidad pero MENOR atención en diseño.

**Nuestra oportunidad**: Diseñar PRIMERO para el adulto mayor, no adaptar después. Esto es lo que hicieron GrandPad y GreatCall ($800M exit) y es la razón de su éxito.

---

## ANÁLISIS DE EMPRESAS — Exitosas vs Fallidas (Marzo 30, 2026)

### Empresas Exitosas

| Empresa | Vertical | Funding | Exit / Status | MAU/Revenue | Lección Clave |
|---------|----------|---------|---------------|-------------|---------------|
| **Care.com** | Marketplace cuidadores | $111M | **$500M (IAC, 2020)** | 32M miembros | Trust & safety es crítico. FTC multó $8.5M por background check failures → Compliance desde día 1 |
| **Life360** | Localización familiar | $90M | ASX IPO, **$228M ARR** | **48.6M MAU** | Freemium PLG funciona. Core action (ubicación) GRATIS e infinitamente simple. Premium = features avanzadas |
| **GreatCall/Lively** | Teléfonos + salud senior | No revelado | **$800M (Best Buy, 2018)** | Millones | UX radical: botones ENORMES + 5Star emergency button. Modelo HW+servicio. Benchmark de simplicidad geriátrica |
| **GrandPad** | Tablet simplificada senior | Inversión estratégica | Privada, creciendo | No público | "Walled garden" extremo: máx 5-6 features visibles, soporte 24/7, eliminaron TODO lo confuso. Modelo a seguir para Terminal |

### Empresas Fallidas

| Empresa | Vertical | Funding | Status | Lección del Fracaso |
|---------|----------|---------|--------|---------------------|
| **Honor Technology** | Home care tech platform | ~$255M | Merged con Home Instead | Capital-intensive: contratar cuidadores humanos NO escala con VC money |
| **Papa** | Companionship senior | ~$240M | Layoffs masivos | Over-scaled modelo de labor: más personas ≠ mejor servicio. Unidades económicas nunca cerraron |
| **HomeHero** | Marketplace cuidadores | ~$23M | **Cerrado 2017** | Reclasificación de contratistas → costos laborales destruyeron modelo |
| **Hometeam** | Home care premium | ~$27M | **Fracaso** | High burn rate + unit economics rotas = muerte por cash |

### Patrones de Éxito vs Fracaso

| Variable | Exitosas | Fallidas |
|----------|----------|---------|
| Modelo | Technology-first (software/hardware) | Labor-intensive (contratar personas) |
| Escalabilidad | Software escala infinitamente | Personas no escalan linealmente |
| UX Focus | Obsesión con simplicidad | Features > experiencia |
| Revenue Model | Freemium → Premium conversión | Subsidiar servicio con VC money |
| Unit Economics | Positivas desde temprano | Negativas esperando escala |
| Risk | Bajo (software) | Alto (regulación laboral, personas) |

**Conclusión**: Monitor de Cuidados sigue el patrón de EXITOSAS — software puro, sin dependencia de labor humana, freemium, simplicidad radical.

---

## FRAMEWORK DE VARIABLES PONDERADAS DE ÉXITO (Marzo 30, 2026)

### Modelo de Evaluación (12 Variables, 100 puntos total)

| # | Variable | Peso | Monitor de Cuidados | Score | Justificación |
|---|----------|------|---------------------|-------|---------------|
| 1 | **UX/Simplicidad del producto** | 15 | Rediseño v2.0: Terminal 3 acciones, Monitor dashboard | 13/15 | GrandPad-inspired. Falta implementar, pero diseñado |
| 2 | **Product-Market Fit** | 14 | Eldercare + LATAM gap + envejecimiento acelerado | 12/14 | 13M adultos mayores en México, sin competencia directa |
| 3 | **Modelo de Revenue sostenible** | 12 | Freemium 4 tiers, no ads, no labor-dependent | 11/12 | Life360 model, unit economics positivas desde mes 1 |
| 4 | **Escalabilidad técnica** | 10 | Software puro, Firebase infra auto-scaling | 10/10 | Sin bottleneck humano, sin labor cost variable |
| 5 | **Accesibilidad (Inclusividad)** | 10 | WCAG AA target, TalkBack, font scaling, adaptativa | 6/10 | Diseñado pero NO implementado aún |
| 6 | **Trust & Safety** | 8 | AES-256, SQLCipher, no tracking, LFPDPPP compliance | 7/8 | Care.com lesson applied: compliance desde día 1 |
| 7 | **Retention/Engagement** | 8 | Notification system, daily check-in patterns, alarms | 6/8 | Necesita onboarding + push engagement strategy |
| 8 | **Diferenciación competitiva** | 7 | Unica app dual-rol LATAM, accesibilidad first | 6/7 | No hay competidor directo en México |
| 9 | **Regulación & Compliance** | 5 | LFPDPPP, SAT, Google Play, aviso privacidad | 3/5 | Todo diseñado, nada ejecutado aún |
| 10 | **Equipo & Ejecución** | 5 | AI-first (Copilot+Gemini), owner testing | 4/5 | Ejecución rápida, pero depende de 1 persona humana |
| 11 | **Timing de mercado** | 3 | Post-COVID telehealth normalizado, LATAM envejeciendo | 3/3 | Timing perfecto: window abierta |
| 12 | **Capital efficiency** | 3 | ~$6,500 MXN total cost to launch | 3/3 | Casi zero-cost con AI workforce |
| | **TOTAL** | **100** | | **84/100** | |

### Interpretación

| Rango | Clasificación | Nuestro Score |
|-------|---------------|---------------|
| 90-100 | Unicornio potencial — execute immediately | — |
| 80-89 | **Startup fuerte — high chance of success** | **84 ✅** |
| 70-79 | Viable — needs focus areas | — |
| 60-69 | At risk — significant gaps | — |
| <60 | Red flag — pivot o kill | — |

**Áreas de mejora prioritarias** (para subir de 84 a 90+):
1. Implementar accesibilidad (6→10, +4 puntos) — P0
2. Ejecutar compliance legal (3→5, +2 puntos) — requiere SAS formation
3. Implementar onboarding + retention strategy (6→8, +2 puntos) — P1

---

## PREDICCIONES DE MERCADO 2026-2030 (Marzo 30, 2026)

### Tendencias Confirmadas

| Tendencia | Datos | Impacto en Monitor de Cuidados |
|-----------|-------|-------------------------------|
| **Telehealth normalizado** | 40% CAGR, COVID causó cambio permanente de comportamiento | Usuarios ya aceptan herramientas digitales de salud. Menor resistencia a adopción |
| **IoMT (Internet of Medical Things)** | Convergencia wearables + health apps + cloud | Oportunidad futura: integración con pulseras, glucómetros, etc. |
| **AI en eldercare** | Computer vision, NLP, fall detection ML | Nuestra voice detection + shake detection es fase 1. Fase 2: ML personalizado |
| **Aging in Place** | 90% de adultos mayores prefieren envejecer en casa vs residencia | Validación total del concepto: apoyo para cuidado en casa |
| **Loneliness epidemic** | Japan kodoku-shi 37K+/año, WHO declara epidemia soledad | SOS button + check-in patterns pueden detectar aislamiento |
| **LATAM aging acceleration** | México: índice envejecimiento triplicado en 30 años | Mercado creciendoexponencialmente, sin soluciones locales |

### Predicciones Específicas

**2026-2027 (Corto plazo)**:
- Google y Apple intensifican features de salud (Health Connect, HealthKit) → API para integrar
- Regulación data privacy se endurece en LATAM → nuestra postura privacy-first es ventaja
- Competidores norteamericanos NO van a LATAM aún → ventana de oportunidad

**2028-2029 (Mediano plazo)**:
- Wearables asequibles para elderly (sub-$30 USD) → integrar como peripheral
- 5G ubiquo → video monitoring mejora dramáticamente (latencia <10ms)
- AI voice assistants bilingual (ES/EN) maduran → potenciar voice commands

**2030+ (Largo plazo)**:
- México alcanza punto de inflexión demográfico (~18% de población 60+)
- Demand for eldercare tech EXPLOTA
- First-mover advantage si estamos en mercado desde 2026 = 4 años de ventaja

---

## ESTRATEGIA COMPETITIVA — Diferenciación UX (Marzo 30, 2026)

### Moat (Foso Competitivo)

| Diferenciador | Descripción | Dificultad para Copiar |
|---------------|-------------|----------------------|
| **UX geriátrica de clase mundial** | Terminal "Modo GrandPad" (3 acciones máx), Monitor dashboard inteligente, WCAG AA+ | Alta: requiere research + iteración + testing con adultos mayores reales |
| **UI Adaptativa por capacidades** | Assessment de 5 capacidades → UI se adapta automáticamente | Alta: nadie más tiene esto |
| **Dual-rol en misma app** | Monitor + Terminal en 1 APK, cambio de rol sin reinstalar | Media: concepto simple pero ejecución compleja |
| **SOS Emergency pattern** | Botón de emergencia estilo GreatCall 5Star, in-app | Baja: fácil de copiar pero requiere ecosistema |
| **Privacy-first architecture** | AES-256, no tracking, no ads, LFPDPPP compliance | Media: architectural decision from day 1 |
| **LATAM-first** | Diseño para México primero, luego LATAM, soporte 6 idiomas | Media: nicho que big players ignoran |
| **AI-operated company** | Costo operativo near-zero, velocidad de iteración extrema | Alta: requiere expertise AI + automation |

### Posicionamiento Competitivo

```
                    SIMPLE ────────────── COMPLEJO
                    │                          │
     GRATIS ────────┼──────────────────────────┤
                    │                          │
                    │  📱 Monitor de Cuidados   │
                    │  (v2.0: GrandPad-simple,  │
                    │   Life360-freemium)        │
                    │                          │
     PREMIUM ───────┤                          │
                    │           GrandPad ($$$)  │
                    │           GreatCall ($$$) │
                    │                          │
```

**Nuestro sweet spot**: Simplicidad de GrandPad + accesibilidad de Freemium. Premium para features avanzadas, no para funcionalidad básica.

### Competitive Response Matrix

| Si competidor... | Nuestra respuesta |
|------------------|-------------------|
| Big tech lanza feature similar | Profundizar nicho: más accesibilidad, más personalización, mejor UX geriátrica |
| App clone aparece en LATAM | First-mover advantage + brand trust + reviews positivas |
| GrandPad/GreatCall expande a LATAM | Diferenciador: gratis + no requiere hardware especial |
| Gobierno lanza programa de eldercare | Integrar como complemento al programa, no competir |

---

## ESTRATEGIA DE FUNDRAISING (Futuro — Post-Launch)

### Readiness Score: 5/10 (Pre-launch)

| Requisito | Estado | Blocker |
|-----------|--------|---------|
| MVP funcionando | ⚠️ En desarrollo | Rediseño v2.0 pendiente |
| Usuarios reales | 🔴 No | Necesita launch |
| Revenue | 🔴 No | Necesita suscriptores |
| Métricas de retention | 🔴 No | Necesita datos |
| SAS constituida | 🔴 Pendiente | Propietario debe tramitar |
| Team | ✅ AI-first operacional | — |
| Market size validated | ✅ WHO + INEGI data | — |
| Competitive analysis | ✅ Completado | — |

### Plan de Levantamiento (cuando sea apropiado)

**Fase Pre-Seed (cuando haya tracción)**:
- Convertir SAS → SAPI (Sociedad Anónima Promotora de Inversión)
- Requisito: tener al menos 100 usuarios activos + revenue positivo
- Target: $50K-$100K USD para: Android + iOS, marketing, legal
- Investors target: ángeles en eldercare/healthtech LATAM (ej: 500 Global LATAM, Kaszek)

**Fase Seed (PMF confirmado)**:
- Target: $500K-$1M USD para: expansión LATAM, team humano (1-2 personas)
- Requisito: 1,000+ MAU, MRR >$5K USD, retention >60% a 90 días
- Estructura: SAPI con class-B shares para fundador (control voting)

### Nota Legal
Conversión SAS → SAPI debe planificarse con anticipación. Ver EMPRESA_CONSTITUCION.md para timeline legal.

---

## ESTRATEGIA DE EXIT / ADQUISICIÓN (Marzo 30, 2026)

### ¿Son suficientes las estrategias actuales para llegar a vender?

**RESPUESTA DIRECTA: SÍ, pero con condiciones.** Las estrategias de producto, pricing y mercado son correctas. Lo que necesita la empresa para ser adquirible es TRACCIÓN demostrable.

### Qué busca un adquiriente (qué necesitamos tener)

| Métrica | Umbral mínimo adquisición | Umbral ideal | Estado actual |
|---------|--------------------------|-------------|---------------|
| MAU | 50,000+ | 200,000+ | 0 |
| MRR | $10,000+ USD | $50,000+ USD | $0 |
| Retention 90d | >40% | >60% | N/A |
| Churn mensual | <10% | <5% | N/A |
| Revenue growth | 15%+ mes/mes | 30%+ mes/mes | N/A |
| LTV:CAC ratio | >3:1 | >5:1 | N/A |
| NPS | >30 | >60 | N/A |

### 3 rutas de exit posibles

**Ruta A: Adquisición por empresa de salud digital (Más probable — 3-5 años)**
- **Compradores**: Lively/GreatCall (Best Buy), Honor Technology, Care.com (IAC), Papa Inc
- **Qué les interesa**: Base de usuarios LATAM + tecnología de UX geriátrica + data de uso
- **Valuación estimada**: 3-8x ARR → $300K-$2.4M USD con $100K ARR
- **Requisito**: Demostrar que funciona en LATAM con retención alta

**Ruta B: Adquisición por telecom/BigTech LATAM (Menos probable — 5-7 años)**
- **Compradores**: América Móvil (Telcel), Mercado Libre, Rappi, Nubank
- **Qué les interesa**: Feature para su ecosistema, penetración en segmento senior
- **Valuación estimada**: 5-15x ARR → $500K-$7.5M USD con $500K ARR
- **Requisito**: >200K usuarios, marca reconocida en LATAM

**Ruta C: IPO en bolsa mexicana o venta a VC (Ruta larga — 7-10 años)**
- **Cómo**: Convertir a SAPI → levantar Series A/B → IPO en BMV o ASX
- **Valuación estimada**: 10-20x ARR → $2.5M-$10M USD con $250K ARR
- **Requisito**: Equipo, IP registrada, SAPI conversion, auditorías financieras

### Plan de acción para llegar a la venta

| Año | Meta | Acción CEO |
|-----|------|------------|
| 2026 | MVP + primeros 1,000 usuarios | Lanzar, iterar, obtener reviews 4.5+ |
| 2027 | 5,000 MAU + $2K MRR | Facebook Ads + partnerships con residencias geriátricas |
| 2028 | 20,000 MAU + $10K MRR | Convertir SAS→SAPI, buscar pre-seed, lanzar iOS |
| 2029 | 100,000 MAU + $50K MRR | Series A, equipo de 3-5, expansión Colombia/Argentina |
| 2030 | 200,000+ MAU + $100K+ MRR | Exit-ready: iniciar conversaciones con adquirientes |

### Lo que hace VENDIBLE a esta empresa

1. **IP registrada** (marca IMPI + software INDAUTOR) → activo protegido
2. **Revenue recurrente demostrable** → no depende de un contrato
3. **Crecimiento orgánico** → menor CAC = mejores unit economics
4. **AI-operated** → comprador asume operación con costo mínimo
5. **Base de usuarios LATAM** → mercado subatendido que BigTech NO tiene

---

## PLAN DE REVENUE SOSTENIBLE — INGRESO DE POR VIDA (Marzo 30, 2026)

### Modelo: SaaS recurrente con expansión natural

El ingreso "de por vida" en software NO es pasivo. Es RECURRENTE — mientras existan usuarios pagando, hay revenue. La diferencia con un empleo es que escala: 100 usuarios pagan lo mismo con 0 horas extra de trabajo.

### Flujo de ingreso mensual perpetuo

```
Usuarios gratis (boca en boca + ASO)
    │
    ▼
Trial Premium (7 días gratis)
    │
    ▼
Suscripción mensual $4.99-$9.99
    │
    ▼
Revenue recurrente mensual (MRR)
    │  - Google retiene 15%
    │  - Deposita cada 15 del mes
    ▼
Ingreso neto en cuenta bancaria SAS
    │
    ▼
Reinvertir 30% en marketing → más usuarios → más MRR
```

### Qué se necesita para que el revenue sea SOSTENIBLE

| Pilar | Qué es | Cómo lo hacemos |
|-------|--------|-----------------|
| **Retención** | Que los que pagan sigan pagando | App útil CADA DÍA (alertas, check-in, video). Si no da valor diario → cancel |
| **Adquisición** | Nuevos usuarios constantes | ASO + Facebook Ads + content marketing + referrals |
| **Expansión** | Que paguen más con el tiempo | Free→Familiar→Plus→B2B. Upgrade natural por más terminales |
| **Baja fricción de pago** | Que pagar sea automático | Google Play Billing (auto-renewal, sin facturar manual) |

### Mantenimiento requerido para sostener revenue (trabajo del CEO)

| Tarea | Frecuencia | Tiempo estimado | Herramienta |
|-------|-----------|-----------------|-------------|
| Responder reviews Play Store | Diario | 10 min/día | Play Console |
| Monitorear crashes | Diario | 5 min/día | Crashlytics |
| Actualizar app (bugs, OS updates) | Mensual | 1-2 días/mes | Gemini + Copilot |
| Nuevo feature menor | Trimestral | 3-5 días | Gemini + Copilot |
| Marketing content | Semanal | 30 min/semana | IA generativa |
| Declaraciones fiscales | Mensual | Delegado a contador | PAC + Contador |
| Análisis de métricas | Semanal | 15 min/semana | Play Console + Firebase |

**Tiempo total**: ~5-7 horas/semana para mantener un negocio que genera ingreso recurrente.

### Proyección de ingreso a 5 años (escenario conservador)

| Año | MAU | Suscriptores | MRR | ARR | Ingreso neto anual (post-costos) |
|-----|-----|-------------|-----|-----|----------------------------------|
| 2026 (H2) | 500 | 25 | $175 | $1,050 | -$2,000 (inversión inicial) |
| 2027 | 3,000 | 150 | $1,050 | $12,600 | $6,000 |
| 2028 | 15,000 | 750 | $5,250 | $63,000 | $35,000 |
| 2029 | 50,000 | 2,500 | $17,500 | $210,000 | $130,000 |
| 2030 | 150,000 | 7,500 | $52,500 | $630,000 | $400,000 |

**Año 2030: $400K USD neto = ~$8M MXN/año = ~$666K MXN/mes**

### Riesgos al revenue sostenible y mitigación

| Riesgo | Mitigación |
|--------|-----------|
| Usuarios cancelan suscripción | Mejorar retención: features nuevos, engagement notifications |
| Google sube comisión | Diversificar: Stripe para B2B directo (2.9% vs 15%) |
| Competidor ofrece gratis | Diferenciador: UX geriátrica, accesibilidad, LATAM |
| Propietario se cansa | Automatizar TODO. 5h/semana es sostenible |
| Firebase se encarece | Migrar a Supabase/AWS cuando >50K users |

---

## ROADMAP MAESTRO — DE HOY A $25,000 USD MRR (Marzo 30, 2026)

### Fase 0: CIMIENTOS (Abril 2026) — Semanas 1-4

**Meta**: App publicada + empresa constituida

| Semana | Propietario (vida real) | CEO/Copilot (desarrollo) |
|--------|------------------------|--------------------------|
| 1 | Verificar e.firma, RFC | Completar 12 tareas pendientes WorkItems.md |
| 2 | Constituir SAS en SIGER | Redactar aviso de privacidad + T&C |
| 3 | Obtener RFC empresa, abrir banco | Configurar Firebase Console (8 colecciones + security rules) |
| 4 | Crear cuenta Google Play ($25 USD), comprar dominio | Generar APK producción, preparar Data Safety Form |

**Entregable**: App en Google Play (beta cerrada), SAS constituida

### Fase 1: LAUNCH (Mayo 2026) — Semanas 5-8

**Meta**: 100 usuarios, primeras reviews

| Acción | Responsable | Detalle |
|--------|-------------|---------|
| Beta cerrada (20 testers) | Propietario | Familia, amigos, conocidos con adulto mayor |
| Testing con adulto mayor real | Propietario | Instalar en dispositivo de su mamá |
| Corregir bugs de beta | CEO + Gemini | Hotfix rápidos, iterar |
| Publicar app (open beta) | CEO | Play Store listing completo |
| Primera campaña Facebook | CEO | $50 USD/semana, target: mujeres 35-55, México |
| Registrar marca IMPI | Propietario | Clases 9 + 42 |

**Entregable**: 100 instalaciones, 4.0+ rating, 0 crashes críticos

### Fase 2: TRACCIÓN (Jun-Ago 2026) — Meses 2-4

**Meta**: 500 MAU, 25 suscriptores, $175 MRR

| Acción | Detalle |
|--------|---------|
| Implementar Google Play Billing | Paywall: 7 días trial → $4.99/mes |
| Content marketing | Blog: "Cómo cuidar a tu padre a distancia" (SEO) |
| Referral program | "Invita amigo → 1 mes gratis ambos" |
| Optimizar onboarding | Reducir drop-off rate con analytics |
| Iterar UX con feedback real | Cambios basados en reviews + usage data |
| Programa alianzas | Contactar 10 residencias geriátricas en la región del propietario |

**Entregable**: Producto-mercado fit validado (retention 90d >40%)

### Fase 3: CRECIMIENTO (Sep 2026 - Mar 2027) — Meses 5-10

**Meta**: 3,000 MAU, 150 suscriptores, $1,050 MRR

| Acción | Detalle |
|--------|---------|
| Escalar Facebook Ads | $200/mes → optimizar CPI <$1.00 |
| Google UAC campaigns | Campañas de instalación automatizadas |
| YouTube tutorials | "Setup en 2 minutos", "Demo completa" |
| Tier Premium Plus ($9.99) | Features avanzados: historial 90d, reportes, 5 terminales |
| Primer contrato B2B | 1 residencia geriátrica → $49.99/mes |

**Entregable**: Revenue covereing operating costs (break-even)

### Fase 4: ESCALA (Abr-Dic 2027) — Meses 11-20

**Meta**: 15,000 MAU, 750 suscriptores, $5,250 MRR

| Acción | Detalle |
|--------|---------|
| Lanzar iOS app | Kotlin Multiplatform o Swift nativo |
| Expandir a Colombia | Segundo mercado LATAM |
| Dashboard web B2B | Panel admin para residencias geriátricas |
| Convertir SAS → SAPI | Preparar para inversión |
| Buscar pre-seed | $50K-100K USD para marketing + equipo |

**Entregable**: iOS + Android, 2 países, SAPI lista

### Fase 5: ACELERACIÓN (2028) — Meses 21-32

**Meta**: 50,000 MAU, 2,500 suscriptores, $17,500 MRR

| Acción | Detalle |
|--------|---------|
| Levantar Seed round | $500K-1M USD |
| Contratar 2-3 personas | Marketing + customer success + dev |
| Expandir a Chile, Argentina, Perú | 5 países LATAM |
| Integrar wearables | Pulseras de detección de caídas |
| Certificación HIPAA | Abrir mercado hispano en USA |

### Fase 6: $25K MRR (2029) — Meses 33-44

**Meta**: 100,000+ MAU, 5,000+ suscriptores, $25,000+ MRR

| Acción | Detalle |
|--------|---------|
| API pública | Integraciones con hospitales y clínicas |
| ML fall detection | Detección de caídas con machine learning |
| Programa de gobierno | Alianza con secretarías de salud LATAM |
| Evaluación exit | Iniciar conversaciones con potenciales adquirientes |

---

### Hitos clave del roadmap

```
[Abr 2026]     [May 2026]     [Ago 2026]     [Mar 2027]     [Dic 2027]     [2028]        [2029]
    │              │              │              │              │              │              │
 Lanzar        100 users      PMF             Break-even     $5K MRR       $17K MRR      $25K MRR
 SAS + App     + reviews      validado        revenue ≥      iOS +         Seed +        EXIT-READY
                               retention      costos         SAPI          equipo
                               >40%
```

### Indicadores de que vamos por buen camino (checkpoints)

| Checkpoint | Cuándo | Señal verde ✅ | Señal roja 🔴 |
|-----------|--------|----------------|----------------|
| App published | Mes 1 | Rating 4.0+, 0 crashes | Rating <3.5, crashes frecuentes |
| First paying user | Mes 2-3 | Alguien paga sin ser amigo/familia | Nadie convierte tras 500 installs |
| Retention | Mes 3 | >40% a 30 días | <20% a 30 días → problema de UX |
| PMF | Mes 4 | Usuarios refieren a otros sin pedirlo | Solo crecimiento pagado → no hay PMF |
| Break-even | Mes 10 | Revenue cubre costos operativos | Revenue no cubre ni marketing |
| Growth inflection | Mes 18 | MRR crece >15% mes/mes | MRR estancado → cambiar estrategia |

---

## DIFERENCIADORES UX/UI — Ventaja Competitiva por Diseño (Marzo 30, 2026)

### Lo que nos hace únicos (basado en investigación)

| Aspecto | Competencia Global | Monitor de Cuidados v2.0 |
|---------|-------------------|--------------------------|
| **Terminal UX** | Apps genéricas con muchos botones/configuración | "Modo GrandPad": 3 acciones máx, botón campana 160dp |
| **Emergency** | Requiere desbloquear telefono + abrir app + buscar función | SOS: 1 long-press, funciona desde pantalla principal |
| **Accesibilidad** | WCAG como afterthought | WCAG AA desde diseño, UI Adaptativa por capacidades |
| **Pricing** | Requiere hardware ($300+) o suscripción obligatoria | Freemium: core gratis, premium con valor real |  
| **LATAM focus** | Diseñado para USA/Europe | Diseñado para México + 6 idiomas |
| **Privacy** | Data harvesting, tracking, ads | Zero tracking, no ads, AES-256, mínimo necesario |
| **Cuidador UX** | Dashboards complicados, muchos menus | Dashboard inteligente: alertas→acciones→historia, 1 tap |
| **Onboarding** | "Figure it out yourself" | Tutorial guiado 5 pasos + slides interactivos |
| **Dark mode** | Cosmético/estético | Funcional: optimizado para revisiones nocturnas |

### Proyección de Impacto

Si implementamos el rediseño v2.0 completo:
- **Conversión freemium→premium**: Estimación 8-12% (vs industria 3-5%) por UX superior
- **Retention 90 días**: Target 70%+ (vs industria 25-35%) por simplicidad + valor diario
- **NPS**: Target 60+ (vs industria 30-40) por accesibilidad-first
- **Time to first value**: <5 minutos (onboarding → campana funcional) vs competencia 15-30 min

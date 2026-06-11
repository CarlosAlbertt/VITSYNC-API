# AUDITORÍA INICIAL DE SEGURIDAD — VITSYNC-API

> **Fecha:** 2026-06-11
> **Alcance:** Código fuente completo (rama `master`, commit `7336b3f`), configuración, historial git.
> **Contexto:** Backend Spring Boot 3.2.5 / Java 21 que trata **datos de salud (categoría especial, Art. 9 RGPD)**.

---

## 1.1 ESCANEO DE SEGURIDAD

### 1.1.1 Credenciales hardcodeadas

**Estado actual (HEAD): limpio.** `application.properties` (raíz y `src/main/resources`) solo contienen placeholders `${VAR}`. El raíz está en `.gitignore`.

**⚠️ CRÍTICO — Historial git contaminado.** El commit anterior a `befd485` ("remove hardcoded secrets") contiene credenciales **reales** todavía recuperables con `git show`:

| Credencial | Valor expuesto | Riesgo |
|---|---|---|
| Password PostgreSQL (Neon) | `npg_4Ouo...` (visible en historial) | Acceso total a BD con datos de salud |
| `jwt.secret` (HS256) | hex de 64 chars en historial | Forja de tokens de cualquier usuario/rol |
| Password Gmail (app password) | visible en historial | Envío de correo suplantando al sistema |

**Acción obligatoria (no la puede hacer el código):**
1. **Rotar las 3 credenciales YA** (Neon, regenerar JWT_SECRET, revocar app password de Gmail).
2. Si el repo es/fue público: considerar la BD comprometida (notificación AEPD en 72h si hubo acceso, Art. 33 RGPD).
3. Opcional: reescribir historial con `git filter-repo` (insuficiente por sí solo: las claves ya deben rotarse).

Otros hallazgos menores:
- `src/test/resources/application.properties`: secreto de test ficticio — aceptable.
- `uploads/cd7723e6-2185-42a9-bb4a-46ae388c5faf.jpg` **comiteado en git**: avatar real = dato personal versionado. Eliminar del repo e ignorar `uploads/**`.

### 1.1.2 Endpoints públicos vs protegidos (`SecurityConfig.java`)

**Públicos (permitAll):**
| Endpoint | Justificación | Problema |
|---|---|---|
| `OPTIONS /**` | Preflight CORS | OK |
| `/api/auth/**` | Login/registro/verify | Sin rate limiting |
| `GET /api/hospitales/**`, `/api/horarios/**`, `/api/medicos/**` | Reserva pública de citas | `GET /api/medicos/admin` queda público (ver abajo) |
| `POST /api/citas/**` | Reserva pública | **Cualquiera crea citas sin autenticar** y dispara emails |
| `/ws/**` | WebSocket | **Sin autenticación STOMP**: spoofing de senderId, suscripción a colas ajenas |
| `/uploads/**` | Servir ficheros | **Documentos/fotos accesibles sin autenticación** (solo protege el UUID) |
| `/error` | Página error Spring | OK |

**Protegidos:**
- `hasRole("ADMIN")`: `/api/usuarios/**`, escritura en `/api/especialidades/**` y `/api/medicos/**`.
- `authenticated()`: `GET /api/especialidades/**`, `/VitSync-app/**`, `/api/relationships/**`, `anyRequest()`.

**🐛 Bug de orden de reglas:** `GET /api/medicos/**` se declara `permitAll` (línea 49) ANTES que `GET /api/medicos/admin → hasRole("ADMIN")` (línea 57). Spring Security evalúa en orden ⇒ la regla de admin es **código muerto**: `GET /api/medicos/admin` es **público** y expone médicos inactivos.

**🐛 Autorización solo por rol, nunca por propiedad (IDOR sistémico):** ningún endpoint verifica que el `{id}` de la URL pertenezca al usuario autenticado. Ver inventario 1.3.

### 1.1.3 Endpoints sin validación (`@Valid` ausente)

| Endpoint | Body | Estado |
|---|---|---|
| `POST /api/auth/verify` | `VerifyRequest` | Sin `@Valid` y el DTO no tiene anotaciones |
| `POST /api/citas` | `CitaRequest` | Sin `@Valid`; parsea `Map<String,Object>` sin validar; `Long.parseLong` sin control |
| `POST /VitSync-app` | **Entidad `User` cruda** | Sin `@Valid`; permite setear `role`, `isVerified`, `password` directamente |
| `PUT /VitSync-app/api/users/{id}/profile` | `Map<String,Object>` | Sin DTO ni validación; casts sin control |
| `PATCH .../avatar` | `Map<String,String>` | Sin validación de URL |
| `PUT /api/informes/{id}/notes` | `Map<String,String>` | Sin validación ni sanitización (XSS almacenado) |
| `@MessageMapping("/chat")` | Entidad `Mensaje` | senderId lo decide el cliente |

Con `@Valid` correcto: `login`, `register`, CRUD de `especialidades`, `medicos`, `usuarios` (admin).

Deficiencias en los DTOs validados:
- Password: regex exige solo 8 chars letras+números (la política sanitaria razonable: ≥12, mayúscula, número, especial).
- NIF: regex de formato sin **validación del dígito de control**.
- `UserUpdateRequest`: sin regex en nif/teléfono/nombres.

### 1.1.4 Riesgos N+1 / JPA

| Ubicación | Problema |
|---|---|
| `PacienteMedico` | `@ManyToOne(fetch = EAGER)` en **ambas** relaciones → N+1 al listar relaciones; arrastra herencia JOINED (joins a `users`+`pacientes`+`medicos`) |
| `AdminUserService.findByRole()` | `findAll()` y filtra **en memoria** — carga toda la tabla |
| `MedicoController.getAllMedicos` | Filtro por especialidad en memoria tras cargar todos |
| Todos los listados | **Sin paginación** (`findAll()` → `List`): `/api/citas`, `/api/informes`, `/api/usuarios`, `/VitSync-app`, mensajes |
| Herencia `JOINED` en `User` | Cada carga polimórfica de usuario hace LEFT JOIN con las 3 subtablas |
| `Cita.paciente/medico`, `Informe.paciente/medico` | LAZY ✅ pero serializados a JSON directamente → riesgo `LazyInitializationException` / N+1 en serialización |

### 1.1.5 `uploads/` comiteado

Sí: `uploads/cd7723e6-2185-42a9-bb4a-46ae388c5faf.jpg` está trackeado. Además el directorio vive en `user.dir` (efímero en Render: los ficheros **se pierden en cada deploy**) y se sirve públicamente.

### 1.1.6 Algoritmos débiles

| Uso | Algoritmo | Veredicto |
|---|---|---|
| Firma JWT (`JwtUtil`) | **HS256** simétrico | Insuficiente para datos sanitarios: una sola clave firma y verifica; fuga = forja total. Migrar a RS256 |
| Código verificación email | **`java.util.Random`** | **Predecible** (semilla recuperable). Usar `SecureRandom` |
| Comparación código verificación | `String.equals` | No constant-time (riesgo teórico de timing) |
| Passwords | BCrypt (fuerza por defecto 10) | ✅ Correcto |
| Cifrado en reposo | **Inexistente** | Ver 1.2 / RGPD Art. 32 |
| `secretKey.getBytes()` | Sin charset explícito | Menor, pero corregir |

No hay usos de MD5/SHA-1/DES.

### 1.1.7 Manejo de errores en `@Service`

- `AuthService`: lanza `RuntimeException` genérica (mensaje distinto para "usuario no encontrado" vs "contraseña incorrecta" → **enumeración de usuarios**).
- `PacienteMedicoService`: lanza `Exception` cruda.
- `CitaService.getCitaById/cancelCita`: devuelven `null`/silencio si no existe.
- `EmailService`: traga excepciones con `printStackTrace` + `System.out.println`; sin logger, sin reintentos, el llamador nunca sabe si el email falló.
- `CitaController`: `catch (Exception)` + `ex.printStackTrace()` + **devuelve `ex.getMessage()` al cliente** (fuga de información interna).
- `AuthController./validate`: refleja `e.getMessage()` dentro de JSON construido por concatenación (inyección JSON / fuga).
- `UserController.updateUserProfile`: devuelve `e.getMessage()` en 500.

---

## 1.2 CUMPLIMIENTO LEGAL (ESPAÑA)

### RGPD (UE 2016/679)

| Artículo | Requisito | Estado |
|---|---|---|
| **Art. 5.1.c** (minimización) | Exponer solo datos necesarios | ❌ `GET /VitSync-app` devuelve **entidad User completa con hash de password y código de verificación**; `/api/relationships/**` devuelve pacientes con alergias/condiciones a cualquier autenticado |
| **Art. 5.1.f** (integridad y confidencialidad) | Protección contra acceso no autorizado | ❌ IDOR sistémico; chat legible por cualquiera; uploads públicos |
| **Art. 9** (categorías especiales) | Base jurídica + garantías reforzadas | ❌ Sin cifrado, sin control de acceso granular, sin trazabilidad. No consta recogida de consentimiento explícito |
| **Art. 25** (privacidad desde el diseño) | Por defecto, mínimo acceso | ❌ Endpoints "temporales" mock, entidades serializadas crudas |
| **Art. 30** (registro de actividades) | Registro de tratamiento | ❌ Inexistente (entidad `HistorialAcceso` creada pero **sin uso**; endpoint devuelve lista vacía mock) |
| **Art. 32** (seguridad del tratamiento) | Cifrado, pseudonimización, resiliencia | ❌ Sin cifrado en reposo de campos clínicos; JWT HS256 24h sin revocación; sin rate limiting |
| **Art. 33/34** (brechas) | Notificación 72h | ⚠️ Sin procedimiento; **ya existe incidente potencial** (credenciales en historial git) |
| **Arts. 15/17/20** (acceso/olvido/portabilidad) | Endpoints de derechos | ❌ No implementados |
| **Art. 35** (DPIA) | Evaluación de impacto — **obligatoria** para tratamiento a gran escala de datos de salud | ❌ No documentada |

### LOPDGDD (LO 3/2018)

- **Art. 9**: tratamiento de salud amparado en DA 17ª — exige medidas del Anexo II ENS como referencia y trazabilidad de accesos. ❌ Sin trazabilidad.
- **Art. 28**: obligación de medidas según riesgo. ❌ Ver Art. 32 RGPD.
- **Art. 77**: régimen sancionador para **entidades públicas** — no aplica directamente a SaaS privada, pero sí si presta servicio a administraciones sanitarias.
- DA 17ª.2.f: derecho a saber **quién accedió** a la historia clínica → requiere audit log por usuario. ❌

### Ley 41/2002 (Autonomía del Paciente)

- **Art. 7** (confidencialidad): ❌ violado por diseño — `GET /api/informes` y `GET /api/citas` devuelven **todos** los informes/citas a cualquier usuario autenticado; chat accesible por terceros.
- **Art. 16.7** (acceso a historia clínica con trazabilidad): ❌ sin registro de accesos.
- **Art. 17** (conservación): la documentación clínica debe conservarse ≥5 años → el borrado físico de `AdminUserService.delete()` puede ser ilegal; se requiere **anonimización** + conservación (coherente con Fase 3.3).

### ENS (RD 311/2022)

Aplica **solo si** la SaaS presta servicio a entidades del sector público sanitario (categoría MEDIA/ALTA por datos de salud). Si solo clientes privados: no obligatorio, pero su Anexo II es la referencia que la AEPD espera (LOPDGDD DA 1ª). Estado: ❌ no se cumpliría ninguna familia de medidas (op.acc, mp.info, op.exp).

### NIS2 (Directiva UE 2022/2555)

Sector sanitario es Anexo I (alta criticidad). Una SaaS de gestión sanitaria puede calificar como **entidad esencial/importante** según tamaño (≥50 empleados o ≥10M€). Transposición española en tramitación (Anteproyecto Ley de Coordinación y Gobernanza de la Ciberseguridad). Obligaciones previsibles: gestión de riesgos, notificación de incidentes 24/72h, responsabilidad del órgano de dirección. Estado: ⚠️ prematuro pero el audit log + gestión de incidentes de las Fases 2-3 lo encaminan.

---

## 1.3 INVENTARIO DE VULNERABILIDADES

| # | Vulnerabilidad | CVSS (aprox) | Archivo afectado | Solución propuesta |
|---|---|---|---|---|
| V01 | Hash BCrypt de password + `verificationCode` + datos completos serializados en respuestas (`GET /VitSync-app`, `PUT .../profile`, `/api/relationships/**`) | **9.1 (Crítica)** | `User.java`, `UserController.java`, `PacienteMedicoController.java` | `@JsonIgnore` en password/verificationCode + DTOs de respuesta (Fase 2.6) |
| V02 | Credenciales reales en historial git (BD Neon, JWT secret, Gmail) | **9.8 (Crítica)** | historial git | Rotar credenciales + filter-repo (Fase 2.2, acción manual) |
| V03 | IDOR sistémico: perfil, avatar, chat (`/messages/{s}/{r}`), relaciones, notas de informes — sin check de propiedad | **8.8 (Alta)** | `UserController`, `ChatController`, `InformeController`, `PacienteMedicoController` | Comparar principal vs id; `@PreAuthorize` (Fase 2.6) |
| V04 | `GET /api/informes` y `GET /api/citas` devuelven TODOS los informes/citas a cualquier autenticado | **8.5 (Alta)** | `InformeController`, `CitaController` | Restringir a ADMIN/MEDICO con relación + paginación (Fases 2.6/4.1) |
| V05 | Sin cifrado en reposo de datos clínicos (alergias, condiciones, grupo sanguíneo, contacto emergencia, notas informe, mensajes) | **7.5 (Alta)** + incumplimiento Art. 32 | `Paciente.java`, `Informe.java`, `Mensaje.java` | `SensitiveDataConverter` AES-256-GCM (Fase 2.4) |
| V06 | Sin rate limiting: brute force de login y de código de verificación (6 dígitos, `java.util.Random` predecible, intentos ilimitados → takeover de cuenta) | **8.1 (Alta)** | `AuthController`, `AuthService` | Bucket4j + `SecureRandom` (Fase 2.5) |
| V07 | JWT HS256, 24h, sin refresh ni revocación; token emitido en registro **antes** de verificar email | **7.4 (Alta)** | `JwtUtil.java`, `AuthService.java` | RS256 + access 15min + refresh en BD (Fase 2.3) |
| V08 | WebSocket sin autenticación STOMP: spoofing de `senderId`, lectura de colas de otros usuarios; origen `*` | **7.3 (Alta)** | `WebSocketConfig.java`, `ChatController.java` | ChannelInterceptor JWT + senderId del principal (Fase 2.6) |
| V09 | `/uploads/**` público + ficheros médicos en disco efímero + fichero real comiteado | **6.5 (Media)** | `WebConfig.java`, `FileUploadController.java`, git | Servir autenticado, almacenamiento externo, gitignore (Fases 2.2/4.3) |
| V10 | Upload sin validación MIME/tamaño; extensión derivada del nombre del cliente; `StringIndexOutOfBounds` si no hay punto | **6.3 (Media)** | `FileUploadController.java` | Apache Tika + límites (Fase 4.3) |
| V11 | `POST /api/citas` sin autenticación: spam de citas + envío de emails arbitrarios | **6.5 (Media)** | `SecurityConfig`, `CitaController` | Exigir autenticación o captcha + rate limit (Fase 2.5/2.6) |
| V12 | Regla `GET /api/medicos/admin` muerta por orden de matchers → endpoint admin público | **5.3 (Media)** | `SecurityConfig.java:49,57` | Reordenar reglas (Fase 2.6) |
| V13 | Enumeración de usuarios: mensajes distintos en login/registro; `/validate` refleja excepciones | **5.3 (Media)** | `AuthService`, `AuthController` | Mensaje genérico "credenciales inválidas" (Fase 2.6) |
| V14 | Sin cabeceras de seguridad (HSTS, X-Content-Type-Options, CSP, Referrer-Policy) | **5.0 (Media)** | `SecurityConfig.java` | Configurar `.headers()` (Fase 2.6) |
| V15 | Fugas de detalle interno: `ex.getMessage()` al cliente, `printStackTrace`, JSON por concatenación | **4.3 (Media)** | `CitaController`, `UserController`, `AuthController` | GlobalExceptionHandler ampliado (Fase 4.2) |
| V16 | XSS almacenado: notas de informe, mensajes de chat y campos de perfil sin sanitizar | **5.4 (Media)** | `InformeController`, `ChatController`, `UserController` | Sanitización HTML en entrada (Fase 2.6) |
| V17 | `POST /VitSync-app` acepta entidad `User` cruda: escalada de privilegios (setear `role=ADMIN`, `isVerified=true`) | **8.1 (Alta)** | `UserController.java:34` | Eliminar endpoint o DTO + ADMIN only (Fase 2.6) |
| V18 | Sin registro de auditoría de accesos a datos clínicos (Art. 30 RGPD, Ley 41/2002) | Cumplimiento | todo el proyecto | Fase 3.1 |
| V19 | N+1 / sin paginación / EAGER en `PacienteMedico` | Rendimiento | modelos/servicios | Fase 4.1 |
| V20 | CORS: `allowCredentials(true)` + orígenes localhost hardcodeados en prod; `@CrossOrigin("*")` en `HorarioController` | **4.0 (Baja)** | `SecurityConfig`, `HorarioController` | Solo orígenes de entorno (Fase 2.6) |
| V21 | Endpoints mock activos: 2FA, suspensión, access-history, horarios | Funcional | `UserController`, `HorarioController` | Implementar o devolver 501 (Fases 3-4) |

### Priorización de remediación

1. **Inmediato (manual):** rotar credenciales (V02).
2. **Fase 2:** V01, V17, V03, V06, V07, V05, V12, V13, V14, V16 — parches de código.
3. **Fase 3:** V18 + derechos RGPD.
4. **Fase 4:** V04 (paginación), V09, V10, V15, V19.

---

## Notas para fases siguientes

- El esquema usa `ddl-auto=validate` en prod ⇒ las tablas nuevas (`refresh_tokens`, `audit_logs`) **requieren migración SQL manual** en Neon antes de desplegar. Se generarán scripts en `scripts/sql/`.
- El cifrado en reposo (Fase 2.4) **no es retrocompatible** con datos ya almacenados en claro: se necesitará migración de datos (script incluido) y los índices sobre columnas cifradas dejan de ser útiles.
- `Informe` no tiene los campos `contenido/diagnostico/tratamiento` que asume el plan: los campos clínicos reales a cifrar son `notasPersonales` (+ `alergias`, `condicionesPrevias`, `grupoSanguineo`, `contactoEmergencia`, `historialClinicoId` en `Paciente`, y `content` en `Mensaje`).

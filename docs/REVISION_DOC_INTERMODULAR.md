# Revisión de la memoria "Proyecto Intermodular – VitSync"

> Documento de trabajo. Compara la memoria académica (PDF entregable) con el
> **estado real del código** (verificado en `VITSYNC-API` / `VITSYNC-WebApp` a
> 2026-06-21) y aporta, apartado por apartado, el **texto corregido listo para
> pegar** + la justificación del cambio.
>
> **Contexto del desfase:** la memoria está firmada el 30/04/2026. Después de esa
> fecha se hizo un *hardening* de seguridad completo (auditoría) y mejoras de
> perfil/RGPD (junio 2026). Por eso la memoria **se queda corta en seguridad**
> (describe la versión vieja) y a la vez **da por entregados** módulos que en el
> backend todavía no existen. Leyenda: ✅ correcto · ✏️ texto corregido · 🔎 motivo.

---

## Resumen ejecutivo de los desajustes

**A) La memoria describe seguridad obsoleta (ya está mejorada en el código):**

| La memoria dice… | La realidad del código es… |
|---|---|
| JWT **HS512**, validez **24 h**, en **localStorage** | **RS256** (par de claves), access **15 min** + **refresh opaco 7 días** (hash en BD, rotación); access token **solo en memoria** del SPA; refresh en **cookie httpOnly `Secure; SameSite=None`** |
| 2FA "estructurado pero **no activo**" | **2FA por email ACTIVO** (login en 2 pasos, código de un solo uso) |
| Rate limiting "**pendiente**" | **Bucket4j** activo en login/registro/verify/export/recuperación |
| Cifrado: "Neon cifra en reposo" + BCrypt | Además **cifrado AES-256-GCM a nivel de aplicación** en campos clínicos (`SensitiveDataConverter`) |
| BCrypt "factor de coste **12**" | `BCryptPasswordEncoder` por defecto → **coste 10** |
| Sin módulo RGPD descrito | **GdprController** (acceso, portabilidad, supresión/anonimización) + **auditoría `audit_logs`** append-only |
| — | **Preguntas de recuperación de contraseña** + reseteo con doble factor (junio 2026) |

**B) La memoria sobre-declara módulos que el backend no implementa:**

| La memoria da por entregado… | Estado real |
|---|---|
| Módulo de salud `/api/salud` (RF-05, Anexo B) | **No existe backend.** Es una vista de frontend con **datos de ejemplo** |
| Módulo de enfermedades `/api/enfermedades` (RF-10, Anexo B) | **No existe backend.** Catálogo de frontend (admin incluido) sin persistencia propia |
| Informes en **PDF** (`/api/informes/{id}/pdf`, PDFBox) | **No implementado.** El controlador solo lista informes y permite **notas del paciente** |
| Médico crea/firma informes (`POST/PUT /api/informes`) | **No expuesto.** `InformeController` = `GET /me`, `GET`, `PUT /{id}/notes` |

**C) Errores concretos de nomenclatura/rutas:**

- Enum `Gender` = **`HOMBRE / MUJER / OTRO`** (no `MALE/FEMALE/OTHER`).
- Self-service de usuario cuelga de **`/VitSync-app`** (no `/api/users`).
- Administración de usuarios = **`/api/usuarios`** (no `/api/admin/users`).
- RGPD = **`/api/users/{id}/my-data | my-data/export | gdpr-delete`**.
- Existen módulos no documentados: **`/api/hospitales`, `/api/horarios`, `/api/relationships`**.

---

## Análisis del proyecto

### 2. Análisis DAFO — Debilidades

✏️ **Sustituir el segundo punto** ("2FA… no se encuentra activa") por:

> - Algunos módulos funcionales (seguimiento de salud y catálogo de
>   enfermedades/tratamientos) están implementados en el frontend con datos de
>   ejemplo, pero todavía **no disponen de persistencia propia en el backend**.
> - La generación de informes clínicos en **PDF** está planificada pero aún no
>   implementada.

🔎 El 2FA **ya está activo** (por email), así que listarlo como debilidad es
incorrecto. Las debilidades reales hoy son el alcance backend de salud/enfermedades
y el PDF de informes. Se mantienen las otras debilidades (naturaleza académica,
equipo reducido).

✏️ **Fortalezas — sustituir el punto del sistema de autenticación** por:

> - Sistema de autenticación de **alta robustez**: JWT firmado con **RS256**
>   (clave asimétrica), *access token* de vida corta (15 min) + *refresh token*
>   opaco de 7 días con rotación y almacenamiento *hasheado* en BD, **verificación
>   en dos pasos (2FA) por correo**, recuperación de contraseña por preguntas de
>   seguridad + código por email, *hashing* BCrypt, **cifrado AES-256-GCM en
>   reposo** de los datos clínicos y **limitación de tasa (rate limiting)** en los
>   endpoints sensibles.

---

### 3. Requisitos funcionales

#### RF-02. Registro y autenticación

✏️ **Reemplazar los puntos 3.º y 4.º** (token 24 h en localStorage) por:

> - El inicio de sesión se realiza mediante **NIF y contraseña**. Si las
>   credenciales son válidas, el sistema emite un **access token JWT firmado con
>   RS256 y validez de 15 minutos** y un **refresh token opaco de 7 días**.
> - El **access token reside únicamente en memoria** del cliente (nunca en
>   `localStorage`/`sessionStorage`) y el **refresh token viaja en una cookie
>   `HttpOnly; Secure; SameSite=None`** inaccesible a JavaScript. La sesión se
>   renueva de forma transparente mediante rotación del refresh token.
> - Si el usuario tiene activada la **verificación en dos pasos (2FA)**, el login
>   no emite tokens directamente: el sistema envía un **código de un solo uso al
>   correo** y exige un segundo paso de verificación para abrir la sesión.
> - El usuario puede **recuperar el acceso si olvida la contraseña** respondiendo
>   a sus **preguntas de seguridad** y confirmando con un **código enviado por
>   email** (doble factor: conocimiento + posesión).

🔎 Corrige el modelo de tokens (RS256/refresh/cookie, no HS512/24h/localStorage),
añade el 2.º paso de 2FA y el flujo de recuperación, todos ya implementados.

#### RF-03. Gestión de perfil de usuario

✏️ **Añadir** estos puntos:

> - El usuario puede **cambiar su contraseña** (verificando la actual y aplicando
>   la política de complejidad) y gestionar su **seguridad avanzada**: activar/
>   desactivar el **2FA por email** y configurar sus **preguntas de recuperación**.
> - El usuario puede consultar y **cerrar sus sesiones activas** (dispositivo, IP
>   y última actividad), una a una o todas las demás salvo la actual.
> - El correo electrónico es de **solo lectura** desde el perfil.

🔎 Refleja el trabajo de perfil/seguridad (Fase 1 y 2) ya entregado.

#### RF-04. Sistema de citas

✏️ **Ajustar** a lo realmente expuesto por la API:

> - El paciente puede **solicitar** una cita (`POST`), **consultar sus citas**
>   (`GET /me`) y **cancelarlas** (`PUT /{id}/cancel`) verificando la propiedad de
>   la cita. Al confirmarse una cita se envía un **email de confirmación** al
>   paciente.

🔎 La gestión avanzada por médico/administrador (reasignación, confirmación
multiestado) está parcialmente cubierta; conviene no describirla como completa. El
endpoint real de cancelación es `PUT /api/citas/{id}/cancel`.

#### RF-05. Módulo de seguimiento de salud

✏️ **Encabezar el requisito con una nota de estado:**

> *(Estado: implementado en el frontend con datos de ejemplo; la persistencia en
> backend de los indicadores de salud queda como trabajo futuro.)*

🔎 No existe `SaludController` ni entidad de mediciones en el backend; las gráficas
y porcentajes se renderizan en el cliente con datos *mock*.

#### RF-06. Informes clínicos

✏️ **Reescribir** para reflejar lo implementado:

> - Los pacientes pueden **consultar sus informes clínicos** (`GET /me`), **añadir
>   notas personales** (`PUT /{id}/notes`) y marcarlos como favoritos. El acceso a
>   los archivos está **protegido por autenticación y rol**.
> - *(Trabajo futuro: creación/firma de informes por el médico y descarga en
>   formato PDF.)*

🔎 No hay endpoint de creación por médico ni de PDF; `InformeController` solo
expone listado + notas del paciente.

#### RF-07. Comunicación en tiempo real

✅ Correcto (WebSocket STOMP/SockJS + persistencia + no leídos + TalkJS).

#### RF-08. Panel de administración

✏️ **Corregir la base de rutas** y matizar:

> - El backoffice realiza operaciones CRUD sobre **usuarios** (`/api/usuarios`),
>   **médicos** (`/api/medicos`) y **especialidades** (`/api/especialidades`). La
>   gestión de **enfermedades/tratamientos** se realiza de momento en el frontend.
> - El administrador puede **activar/verificar y suspender** cuentas
>   (`PATCH /api/usuarios/{id}/verificar`, campo `suspended`).

#### RF-10. Gestión de enfermedades y tratamientos

✏️ **Nota de estado:**

> *(Estado: catálogo gestionado en el frontend; sin backend de persistencia
> dedicado por el momento.)*

🔎 No existe `EnfermedadController` ni entidad correspondiente.

---

### 4. Requisitos no funcionales

#### RNF-02. Seguridad y privacidad — **reescritura recomendada**

✏️ Reemplazar la lista por:

> - **Contraseñas**: almacenadas siempre como hash **BCrypt** (implementación por
>   defecto, coste 10). Política de complejidad ≥12 caracteres con mayúscula,
>   minúscula, dígito y carácter especial.
> - **Tokens**: JWT firmado con **RS256** (clave asimétrica pública/privada),
>   *access* de 15 min; **refresh token opaco** de 7 días *hasheado* en BD con
>   rotación y revocación; entregado en **cookie `HttpOnly; Secure; SameSite=None`**.
> - **Cifrado en reposo**: los campos clínicos sensibles (datos del paciente,
>   informes, mensajes) se cifran con **AES-256-GCM** a nivel de aplicación
>   (`SensitiveDataConverter`), además del cifrado de disco de Neon.
> - **Verificación en dos pasos (2FA)** por correo y **recuperación de contraseña**
>   con preguntas de seguridad + código por email.
> - **Limitación de tasa (rate limiting)** con **Bucket4j** en los endpoints
>   sensibles (login 5/15 min, registro 3/h, verificación 10/h, exportación RGPD
>   1/24 h, recuperación 5/h) → respuesta **429** con `Retry-After`.
> - **Control de acceso**: RBAC con Spring Security (`@PreAuthorize`) y prevención
>   de **IDOR** (`SecurityUtils.requireSelfOrAdmin`) para que cada usuario solo
>   acceda a sus propios datos.
> - **Auditoría**: registro append-only en `audit_logs` (`@Auditable` +
>   `AuditAspect`) de operaciones críticas (login, accesos a datos, exportación,
>   supresión), conforme a RGPD Art. 30 y Ley 41/2002.
> - **Validación**: `@Valid` + DTOs en el backend (espejo en el frontend),
>   incluido validador de **NIF** con letra de control; **saneado de MIME** de los
>   ficheros subidos con **Apache Tika**.
> - **Cabeceras de seguridad**: HSTS, `X-Content-Type-Options`, anti-clickjacking
>   (frame deny), `Referrer-Policy` y **CSP** restrictiva; **CORS** limitado a los
>   dominios oficiales con `allowCredentials`.
> - **Frontend**: saneado de HTML con **DOMPurify**, sin secretos en el bundle, y
>   eliminación de `console.*` en *build*.
> - **Gestión de secretos**: claves y credenciales **solo** como variables de
>   entorno (RSA, `ENCRYPTION_KEY`, `RESEND_API_KEY`), nunca en el código.

🔎 La redacción original (HS512, 24 h, "Vue escapa XSS" como única medida)
infravalora enormemente la seguridad real implementada.

#### RNF-04. Arquitectura — matiz sobre "sin estado"

✏️ Añadir matiz:

> El *access token* es **stateless** (verificable sin consultar la BD), mientras
> que los *refresh tokens* son **stateful** (almacenados *hasheados* y revocables),
> lo que permite cerrar sesiones e invalidar credenciales tras un cambio de
> contraseña.

---

### 6. Casos de uso — CU-03 Inicio de sesión

✏️ **Corregir la postcondición**:

> - Postcondición: el *access token* se mantiene **en memoria** del cliente y el
>   *refresh token* en **cookie HttpOnly**; si el usuario tiene 2FA activo, se
>   requiere un segundo paso con código por email antes de abrir la sesión.
>   Redirección al área correspondiente a su rol.

🔎 Elimina la mención a `localStorage`.

---

## Diseño

### 2. Modelo de datos

✏️ **`gender`**: el enumerado real es **`HOMBRE, MUJER, OTRO`** (no
`MALE/FEMALE/OTHER`).

✏️ **Atributos de `users` a añadir/precisar** (todos presentes en el modelo):

> - `twoFactorEnabled`, `twoFactorCode` (hash), `twoFactorCodeExpiry`
> - `securityQuestion1/2` (texto), `securityAnswer1/2` (**hash BCrypt**)
> - `passwordResetCode` (hash), `passwordResetCodeExpiry`
> - Los campos de código/secreto y los hashes **no se serializan** en las
>   respuestas JSON (`@JsonIgnore` / `WRITE_ONLY`).

✏️ **El modelo NO son "tres entidades":** además de `users`, `medicos`,
`pacientes` y `administradores` (herencia JOINED), existen: `Cita`, `Especialidad`,
`Hospital`, `Informe`, `Mensaje`, `ChatMessage`, `PacienteMedico`, **`RefreshToken`**,
**`AuditLog`** e **`HistorialAcceso`**. Conviene listarlas.

🔎 Datos clínicos de `pacientes` (`grupoSanguineo`, `alergias`,
`condicionesPrevias`, `contactoEmergencia`) se almacenan **cifrados (AES-256-GCM)**.

### 3. Tecnologías utilizadas

✏️ **Backend — completar:**

> - Seguridad: Spring Security 6 + **JWT RS256** (jjwt 0.12.6), **Bucket4j**
>   (rate limit), **Apache Tika** (validación MIME).
> - Email transaccional: **Resend**.
> - Tiempo real: **WebSocket STOMP + SockJS**.
> - Calidad: **JUnit 5 + Mockito**, **JaCoCo** (umbral 80 % en `service/` y
>   `util/`), **H2** en memoria para tests.

✏️ **Frontend — añadir:** **DOMPurify** (saneado XSS), **Vitest** (pruebas),
`@stomp/stompjs` + `sockjs-client`.

✏️ **Eliminar/aclarar PDFBox:** se menciona en el Sprint 5 pero **no se usa** (la
exportación PDF de informes no está implementada).

### 4. Notas de diseño — "JWT sin estado"

✏️ Matizar igual que en RNF-04: *access* sin estado (RS256) + *refresh* con estado
(hash en BD, rotación/revocación). Es un modelo híbrido, más seguro que JWT puro.

---

## Plan de despliegue

### 1. Entorno local

✏️ **Arranque del frontend** — el flujo de desarrollo es:

> ```
> cd VITSYNC-WebApp
> npm install
> npm run dev          # servidor Vite en http://localhost:5173
> ```
> (También existe `docker compose` para levantarlo en contenedor con nginx.)

🔎 `docker compose up -d` no es el flujo de desarrollo con HMR; el comando real es
`npm run dev`.

✏️ **Variables de entorno del backend** — precisar (no es "un secreto JWT"):

> `DATABASE_URL/USERNAME/PASSWORD`, `CORS_ALLOWED_ORIGINS`, **`JWT_PRIVATE_KEY` y
> `JWT_PUBLIC_KEY`** (par RS256 en base64 DER), **`ENCRYPTION_KEY`** (32 bytes
> base64, AES-256), `RESEND_API_KEY`; opcionales `JWT_ACCESS_EXPIRATION`,
> `JWT_REFRESH_EXPIRATION`, `MAIL_FROM_ADDRESS`, `UPLOAD_DIR`, **`DDL_AUTO`**
> (testing=`update`, producción=`validate`). El script `scripts/setup-env.sh`
> valida el entorno y puede **generar las claves RSA + AES**.

🔎 RS256 requiere par de claves (no un secreto simétrico); falta `ENCRYPTION_KEY`.

✏️ **Esquema de BD (producción):** `ddl-auto=validate` + ejecución de los scripts
de `scripts/sql/` (V2 refresh_tokens, V3 cifrado, V4 audit_logs, V5 metadatos de
sesión, V6 2FA, **V7 preguntas de recuperación**) antes de desplegar.

---

## Evaluación y pruebas

✏️ **Pruebas unitarias del frontend — Login.vue / Test 1:** corregir la
comprobación:

> - Comprobaciones: el *access token* queda **en memoria** (no en `localStorage`);
>   el refresh viaja en cookie HttpOnly gestionada por el backend; el usuario es
>   redirigido a la página principal; no se muestra error.

✏️ **Backend (JUnit):** actualizar las cifras y el alcance:

> La suite del backend cuenta con **172 pruebas** (JUnit 5 + Mockito) cubriendo
> autenticación, recuperación de cuenta, validación de NIF, JWT y saneado HTML,
> con cobertura medida por **JaCoCo** (umbral 80 % en `service/` y `util/`).

✏️ **E2E:** la memoria cita **Cypress** en la pirámide; en el estado actual las
pruebas E2E (Cypress/Playwright) están **pendientes**. Recomendado: marcarlo como
trabajo futuro o retirar la fila para no sobre-declarar.

🔎 Coherente con `docs/TESTING.md` y el `CLAUDE.md` (E2E pendiente).

---

## Plan de sostenibilidad

✏️ **Protección de datos en reposo — ampliar:**

> Además del cifrado de disco de Neon, VitSync aplica **cifrado AES-256-GCM a
> nivel de aplicación** sobre los campos clínicos (paciente, informes, mensajes),
> de modo que ni siquiera con acceso a la BD se exponen en claro.

✏️ **Derechos RGPD — añadir apartado** (no figura y está implementado):

> El sistema implementa los derechos del interesado mediante `GdprController`:
> **acceso** (`GET /my-data`), **portabilidad** (`GET /my-data/export`) y
> **supresión** (`DELETE /gdpr-delete`, por anonimización), junto con la
> **auditoría** append-only de accesos (`audit_logs`) exigida por la Ley 41/2002 y
> el RGPD Art. 30.

---

## Producto final

### 2. Fortalezas

✏️ Sustituir el punto de seguridad por la redacción ampliada de RNF-02 (RS256 +
refresh httpOnly + AES-256-GCM + 2FA + rate limiting + auditoría + RGPD).

### 3. Debilidades — **reescritura** (las actuales ya no aplican)

✏️ Reemplazar por las debilidades reales de hoy:

> - **Backend de "Mi Salud" y "Enfermedades" pendiente:** ambos módulos funcionan
>   en el frontend con datos de ejemplo, sin persistencia propia en la API.
> - **Informes en PDF pendientes** y creación/firma de informes por el médico no
>   expuesta.
> - **Dashboard del médico limitado** (gestión de agenda parcial).
> - **Almacenamiento de avatares efímero** (disco de Render; se pierden al
>   redeploy) → pendiente de almacenamiento externo (S3/Cloudinary).

🔎 Eliminar "rate limiting pendiente" y "2FA no activo": **ambos están
implementados**.

---

## Trabajo futuro

✏️ **Eliminar** de "funcionalidades pendientes" lo que ya está hecho:

- ~~2FA~~ → **implementado** (por email; evolución futura: TOTP/app autenticadora).
- ~~Rate limiting~~ → **implementado** (Bucket4j).
- ~~Notificaciones por email de citas~~ → **implementado** (confirmación de cita).

✏️ **Mantener / añadir** como trabajo futuro real:

> - Persistencia en backend del módulo de salud y del catálogo de enfermedades.
> - Generación de informes en **PDF** y creación/firma por el médico.
> - Dashboard del médico completo (agenda, disponibilidad, franjas).
> - Almacenamiento de avatares/ficheros externo (S3/Cloudinary).
> - Trabajo de cumplimiento RGPD avanzado: **DPIA (Art. 35)**, contratos de
>   encargo (Art. 28) y **job de anonimización automática a 30 días**.
> - 2FA por **TOTP/app**, vídeo-consulta integrada, app móvil, HL7 FHIR/EHDS y
>   recetas electrónicas.

---

## Bibliografía

✏️ **Añadir** las fuentes de lo realmente usado:

> - jjwt (Java JWT) – RFC 7519 / firma RS256.
> - Bucket4j – *rate limiting* (token bucket).
> - Apache Tika – detección de tipos MIME.
> - Resend – API de email transaccional.
> - OWASP ASVS / OWASP Top Ten (referencia del *hardening*).

---

## Anexo B — Tabla de endpoints (versión corregida y completa)

### Autenticación — base `/api/auth`
| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| POST | `/login` | Público | Login (NIF+contraseña); si hay 2FA devuelve `twoFactorRequired` |
| POST | `/login/2fa` | Público | 2.º paso 2FA: verifica el código del email y abre sesión |
| POST | `/register` | Público | Registro de paciente; envía código de verificación |
| POST | `/verify` | Público | Verifica la cuenta con el código del email |
| POST | `/refresh` | Cookie | Renueva el access token (rotación del refresh) |
| POST | `/logout` | Cookie | Cierra la sesión actual (revoca refresh) |
| POST | `/logout-all` | Autenticado | Cierra todas las sesiones del usuario |
| GET | `/validate` | Autenticado | Valida el access token (firma/expiración) |
| GET | `/sessions` | Autenticado | Lista las sesiones activas (dispositivo/IP) |
| DELETE | `/sessions/{id}` | Autenticado | Cierra una sesión concreta |
| POST | `/sessions/revoke-others` | Autenticado | Cierra el resto de sesiones |
| POST | `/recovery/questions` | Público | Devuelve las preguntas de seguridad por NIF |
| POST | `/recovery/verify` | Público | Verifica respuestas y envía código por email |
| POST | `/recovery/reset` | Público | Resetea la contraseña con el código |

### Perfil / self-service — base `/VitSync-app`
| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| GET | `/{id}` | Propietario/Admin | Datos del usuario |
| PUT | `/api/users/{id}/profile` | Propietario/Admin | Actualiza datos del perfil |
| PATCH | `/api/users/{id}/avatar` | Propietario/Admin | Actualiza la URL del avatar |
| PATCH | `/api/users/{id}/password` | Propietario/Admin | Cambia la contraseña |
| PUT | `/api/users/{id}/security/2fa` | Propietario/Admin | Activa/desactiva el 2FA |
| POST | `/api/users/{id}/security/questions` | Propietario/Admin | Configura las preguntas de recuperación |

### RGPD — base `/api/users/{id}`
| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| GET | `/my-data` | Propietario/Admin | Acceso a los datos personales (Art. 15) |
| GET | `/my-data/export` | Propietario/Admin | Portabilidad/exportación (Art. 20) |
| DELETE | `/gdpr-delete` | Propietario/Admin | Supresión por anonimización (Art. 17) |

### Citas — base `/api/citas`
| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| GET | `/` | Autenticado | Listado de citas |
| GET | `/me` | Autenticado | Citas del usuario autenticado |
| POST | `/` | Autenticado | Solicitar cita (envía email de confirmación) |
| PUT | `/{id}/cancel` | Autenticado | Cancelar cita (verifica propiedad) |

### Informes — base `/api/informes`
| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| GET | `/me` | Paciente | Informes del paciente autenticado |
| GET | `/` | Autenticado | Listado de informes |
| PUT | `/{id}/notes` | Paciente | Notas personales del paciente |

### Especialidades — base `/api/especialidades`
| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| GET | `/` · `/{id}` · `/slug/{slug}` · `/tipo/{tipo}` | Público/Auth | Consulta de especialidades |
| GET | `/admin` | Admin | Listado completo (incl. inactivas) |
| POST · PUT `/{id}` · DELETE `/{id}` · PATCH `/{id}/toggle-activo` | Admin | CRUD |

### Médicos — base `/api/medicos`
| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| GET | `/` · `/{id}` · `/especialidad/{id}` | Público | Consulta del cuadro médico |
| GET | `/admin` | Admin | Listado completo (incl. inactivos) |
| POST · PUT `/{id}` · DELETE `/{id}` · PATCH `/{id}/toggle-activo` | Admin | CRUD |

### Administración de usuarios — base `/api/usuarios`
| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| GET | `/` · `/{id}` · `/rol/{rol}` | Admin | Consulta de usuarios |
| PUT | `/{id}` | Admin | Actualizar usuario |
| DELETE | `/{id}` | Admin | Eliminar usuario |
| PATCH | `/{id}/verificar` | Admin | Verificar/activar cuenta |

### Otros módulos
| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| GET | `/api/hospitales` · `/api/horarios` | Público (GET) | Catálogo para reserva de citas |
| POST | `/api/upload/avatar` | Autenticado | Subida de avatar (validación MIME con Tika) |
| POST | `/api/relationships/assign` | Autenticado | Asignar relación paciente–médico |
| GET | `/api/relationships/paciente/{id}/medicos` · `/medico/{id}/pacientes` | Autenticado | Consultar relaciones |
| WS (STOMP) | `/app/chat` → `/queue/messages` | Autenticado | Chat en tiempo real |
| GET | `/api/messages/{senderId}/{recipientId}` | Autenticado | Historial de chat |

> **No existen** en el backend (frontend con datos de ejemplo): `/api/salud/*` y
> `/api/enfermedades/*`. La descarga de informes en PDF (`/api/informes/{id}/pdf`)
> tampoco está implementada.

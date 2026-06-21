# 🩺 VitSync — Resumen completo del proyecto

> Documento de referencia (vivo). Última actualización: 2026-06-18.
> Existe copia idéntica en ambos repos (VITSYNC-API y VITSYNC-WebApp).
> Reglas de Git: ver [GIT_WORKFLOW.md](GIT_WORKFLOW.md).

## 1. Qué es
SaaS sanitario: backend REST + SPA para que pacientes gestionen **citas, historial
médico, informes, chat con especialistas y sus datos (RGPD)**. Trata **datos de
salud (categoría especial, Art. 9 RGPD)** → máximas garantías técnicas y legales.

## 2. Repos y rutas
| Repo | Ruta local | Stack | GitHub |
|---|---|---|---|
| **VITSYNC-API** | `C:\Users\carlo_pjou9vc\VITSYNC-API` | Spring Boot 3.2.5 / Java 21 / Maven | `CarlosAlbertt/VITSYNC-API` |
| **VITSYNC-WebApp** | `C:\Users\carlo_pjou9vc\VITSYNC-WebApp` | Vue 3 + Vite + Tailwind v4 | `CarlosAlbertt/VITSYNC-WebApp` |

## 3. Stack
- **API**: Spring Boot 3.2.5 · Java 21 · Spring Security 6 · **JWT RS256** (jjwt 0.12.6) ·
  JPA/Hibernate 6 · **PostgreSQL 15 (Neon)** · WebSocket STOMP+SockJS (chat) ·
  Resend (email) · Bucket4j (rate limit) · Apache Tika (MIME) · JaCoCo · H2 (tests) · Lombok.
- **WebApp**: Vue 3.5 · Vite 7 · Tailwind v4 · Vue Router · axios · @stomp/stompjs + sockjs ·
  DOMPurify · Vitest (54 tests).

## 4. Arquitectura del API
Paquetes: `controller, service, repository, model, dto, config (+ratelimit), converter,
audit, enums, exception, util, validation`.

### Endpoints (base `@RequestMapping` → rutas)
- `/api/auth`: login, **login/2fa**, register, verify, refresh, logout, logout-all,
  validate, **sessions** (GET · DELETE `{id}` · POST `revoke-others`).
- `/VitSync-app` (self-service): `{id}` (GET), `api/users/{id}/profile` (PUT),
  `.../avatar` (PATCH), `.../password` (PATCH), `.../security/2fa` (PUT).
- `/api/citas`: GET, `/me`, POST, **`{id}/cancel`** (PUT).
- `/api/medicos`, `/api/especialidades`, `/api/hospitales`, `/api/horarios`,
  `/api/informes`, `/api/relationships`, `/api/upload`, `/api/usuarios` (admin),
  `/api/gdpr` (my-data, export, gdpr-delete), chat.

### Seguridad (ya implementada)
- Auth RS256 access 15 min + **refresh opaco 7d** (hash en BD, rotación, cookie
  httpOnly `Secure; SameSite=None`).
- **Cifrado en reposo AES-256-GCM** (`SensitiveDataConverter`) en campos clínicos de
  Paciente/Informe/Mensaje (clave `ENCRYPTION_KEY`).
- IDOR: `SecurityUtils.requireSelfOrAdmin`. Validación `@Valid` + DTOs + `@ValidNif`.
- Auditoría `@Auditable` + `AuditAspect` → `audit_logs` (append-only). RGPD:
  `GdprService`/`GdprController`. Rate limit Bucket4j.

### Modelo de datos
User (herencia JOINED) → Paciente, Medico, Administrador; Cita, Especialidad,
Hospital, Informe, Mensaje, ChatMessage, PacienteMedico, RefreshToken, AuditLog,
HistorialAcceso.

## 5. Frontend
- **Rutas**: `/` (home, requiere auth), `/especialidades`, `/especialidad/:id`,
  `/cuadro-medico`, `/enfermedades-tratamientos`, `/agendar-cita`, `/perfil`,
  `/privacidad`, `/mi-salud/:categoria`, `/comunicacion`, `/login`, `/register`,
  `/verify`, `/admin/*`, not-found.
- **Estructura**: `pages/`, `components/` (booking, chat, profile/sections, admin),
  `store/` (módulos reactivos), `services/` (api.js, profileService, gdprService,
  websocket), `utils/` (validators, sanitize), `tests/`.
- **Auth cliente**: access token **solo en memoria**; refresh en **cookie httpOnly**;
  interceptor con refresh single-flight.
- **Estilo**: minimalista profesional (sin gradientes/serif decorativo/texturas).

## 6. Despliegue
| Entorno | Frontend (Vercel) | API (Render) | Rama | BD (Neon) |
|---|---|---|---|---|
| **Producción** | vitsync.es / vitsync-web-app | **api.vitsync.es** | `master` | prod |
| **Testing** | vitsync-web-app-testing.vercel.app | **vitsync-api-testing.onrender.com** | `develop` | testing |

- **Env vars API**: `DATABASE_URL/USERNAME/PASSWORD`, `CORS_ALLOWED_ORIGINS`,
  `JWT_PRIVATE_KEY`, `JWT_PUBLIC_KEY` (base64 DER), `ENCRYPTION_KEY` (32 bytes base64),
  `RESEND_API_KEY`; opcionales `JWT_ACCESS_EXPIRATION`, `JWT_REFRESH_EXPIRATION`,
  `PORT`, `MAIL_FROM_ADDRESS`, `UPLOAD_DIR`, **`DDL_AUTO`** (testing=`update`, prod=`validate`).
- **Env vars Frontend**: `VITE_API_URL`, `VITE_TALKJS_APP_ID`.
- **Esquema BD**: prod = `validate` + scripts `scripts/sql/` (V2 refresh_tokens, V3
  cifrado, V4 audit_logs, V5 metadatos sesión, V6 2FA). Testing = `update` (auto).
- **Avatares**: disco efímero de Render (se pierden al redeploy) → pendiente S3/Cloudinary.

## 7. Estado del trabajo
**Hecho** (en `develop`):
- Rediseño minimalista (Login/Home/Especialidades/Register/toast).
- Fase 1 — Perfil: preview de avatar, guardado arreglado (campos médicos cifrados,
  email read-only), eliminar con “ELIMINAR”, avatar roto → iniciales (fetch autenticado).
- Fase 2 (parcial): cambio de contraseña, **sesiones activas reales**, **2FA por email**.
- Citas: asociación al paciente, mapeo de campos, cancelar con ownership, email real.

**Pendiente**:
- Fase 2 → **preguntas de recuperación** (front listo; falta backend).
- Fase 3 (RGPD/email): export con verificación (contraseña + código email), desactivar
  cuenta (email + enlace reactivación + bloqueo login), eliminar cuenta (anonimización
  real + email).
- Fase 4: Mi Salud (entidades de mediciones + gráficas reales).
- Fase 5: informes en PDF.
- Avatar persistente (almacenamiento externo).

## 8. ⚠️ Temporales a revertir antes de mergear API a master
1. `src/main/resources/application.properties`: `ddl-auto=${DDL_AUTO:update}` → `${DDL_AUTO:validate}`.
2. `GlobalExceptionHandler`: el 500 expone la causa (“DEBUG … root: …”) → mensaje genérico.
3. Aplicar scripts SQL V5/V6 en la BD de producción.

## 9. Build / test
- **API**: `export JAVA_HOME=~/.jdks/temurin-21.0.10 && ./mvnw -o compile` · `./mvnw -o test` · cobertura `./mvnw verify`.
- **WebApp**: `npm run build` · `npm run test`.

## 10. Documentación
`docs/`: AUDITORIA_INICIAL, SECURITY, GDPR_COMPLIANCE, GDPR_PROCEDURES, ENCRYPTION,
DATA_FLOWS, API_REFERENCE, TESTING, PROGRESS, INCIDENCIAS_Y_CORRECCIONES,
RESUMEN_PROYECTO (este), GIT_WORKFLOW.

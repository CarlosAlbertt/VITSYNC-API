# API REFERENCE — VITSYNC-API

Base URL: `https://<host>` · Autenticación: `Authorization: Bearer <access_token>`
salvo endpoints públicos. Errores en formato común:
`{ "timestamp", "status", "error", "message" }` (o `fieldErrors` en validación).

## Autenticación — `/api/auth`

### POST /api/auth/login (público, rate 5/15min)
Request: `{ "nif": "12345678Z", "password": "..." }`
Response 200: `{ "token", "refreshToken", "id", "nif", "email", "role", "message" }`
Errores: 401 credenciales inválidas · 400 no verificada/suspendida · 429 rate limit.

### POST /api/auth/register (público, rate 3/h)
Request: RegisterRequest (name, firstName, secondName, nif, email, password≥12,
gender, role, birthDate, phone ES, address, postCode 5 díg, country).
Response 201: `{ "id", "nif", "email", "role", "message" }` (sin tokens).
Errores: 400 validación · 400 NIF/email duplicado · 429.

### POST /api/auth/verify (público, rate 10/h)
Request: `{ "email", "code": "123456" }` → 200 / 400.

### POST /api/auth/refresh (público)
Request: `{ "refreshToken": "..." }` → 200 nuevo par de tokens (rota el anterior) / 400.

### POST /api/auth/logout (público)
Request: `{ "refreshToken": "..." }` → 200 (idempotente).

### POST /api/auth/logout-all (autenticado)
→ 200 `{ "message", "sessionsRevoked" }`.

### GET /api/auth/validate
Header Authorization → 200 `{ "valid": true, "nif", "role" }` / 401.

## Usuarios (self) — `/VitSync-app`

| Método | Ruta | Acceso | Notas |
|---|---|---|---|
| GET | `/VitSync-app` | ADMIN | Lista usuarios |
| GET | `/VitSync-app/{id}` | self/ADMIN | IDOR check |
| PUT | `/VitSync-app/api/users/{id}/profile` | self/ADMIN | ProfileUpdateRequest (parcial) |
| PATCH | `/VitSync-app/api/users/{id}/avatar` | self/ADMIN | `{ "avatarUrl" }` |
| PUT | `/VitSync-app/api/users/security/2fa` | autenticado | 501 (no implementado) |
| PUT | `/VitSync-app/api/users/status` | autenticado | 501 |

## Admin usuarios — `/api/usuarios` (ADMIN)

| Método | Ruta | Notas |
|---|---|---|
| GET | `/api/usuarios?page=&size=&sort=` | `Page<UserResponse>` |
| GET | `/api/usuarios/{id}` | 200/404 |
| GET | `/api/usuarios/rol/{rol}?page=` | ADMIN/MEDICO/PACIENTE |
| PUT | `/api/usuarios/{id}` | UserUpdateRequest · 409 si duplicado |
| DELETE | `/api/usuarios/{id}` | 204 |
| PATCH | `/api/usuarios/{id}/verificar?verified=` | 200 |

## Derechos RGPD — `/api/users/{id}` (self/ADMIN)

| Método | Ruta | Notas |
|---|---|---|
| GET | `/my-data` | JSON con perfil, citas, informes, mensajes |
| GET | `/my-data/export` | ZIP (json+txt) · rate 1/24h · 429+Retry-After |
| DELETE | `/gdpr-delete` | 202 + `scheduledAnonymizationDate` |

## Clínico

| Método | Ruta | Acceso | Notas |
|---|---|---|---|
| GET | `/api/informes/me` | autenticado | Informes propios |
| GET | `/api/informes` | ADMIN/MEDICO | Todos |
| PUT | `/api/informes/{id}/notes` | dueño | Sanitiza HTML · 403/404 |
| GET | `/api/citas/me` | autenticado | Citas propias |
| GET | `/api/citas` | autenticado | |
| POST | `/api/citas` | autenticado | Crea cita + email |
| GET | `/messages/{senderId}/{recipientId}` | participante/ADMIN | Historial chat |

## Catálogo público (GET)

`/api/hospitales`, `/api/horarios`, `/api/medicos`, `/api/medicos/{id}`,
`/api/especialidades/**` (algunas autenticado), `/api/medicos/admin` (ADMIN).

## Subida — `/api/upload` (autenticado)

### POST /api/upload/avatar
multipart `file` (jpeg/png/webp ≤2MB) → 200 `{ "url" }` · 400/413/415/500.

## WebSocket — `/ws` (STOMP, auth en CONNECT)
- CONNECT con header `Authorization: Bearer <token>`.
- SEND `/app/chat` → push a `/user/{id}/queue/messages`.

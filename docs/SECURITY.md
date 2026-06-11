# SEGURIDAD — VITSYNC-API

Modelo de amenazas y controles implementados. Complementa
`docs/AUDITORIA_INICIAL.md` (hallazgos) y `docs/GDPR_COMPLIANCE.md` (mapeo legal).

## Modelo de amenazas (resumen STRIDE)

| Amenaza | Vector | Control |
|---|---|---|
| **Spoofing** | Forja de tokens, suplantación en chat | JWT RS256 (firma asimétrica); WebSocket autenticado por interceptor STOMP; senderId derivado del principal |
| **Tampering** | Modificación de datos cifrados en BD | AES-256-GCM (tag de autenticación: descifrado falla si se altera) |
| **Repudiation** | Negar accesos a historia clínica | Audit log append-only (`audit_logs`) vía AOP |
| **Information disclosure** | Fuga de password/datos, IDOR | password WRITE_ONLY + verificationCode @JsonIgnore; cifrado en reposo; `requireSelfOrAdmin`; cabeceras de seguridad |
| **Denial of service** | Brute force, spam | Rate limiting Bucket4j (login/registro/verify/export) |
| **Elevation of privilege** | Setear role=ADMIN | Eliminado endpoint que aceptaba `User` crudo; rol solo editable por ADMIN |

## Controles implementados

### Autenticación y sesión
- JWT **RS256**, access token 15 min.
- Refresh tokens opacos (256 bits SecureRandom), 7 días, **solo hash SHA-256 en BD**, revocables, con **rotación** (replay → rechazo).
- Endpoints `/refresh`, `/logout`, `/logout-all`.
- Cuentas no verificadas o suspendidas rechazadas en el filtro JWT.
- Passwords con BCrypt; política ≥12 con mayúscula/minúscula/número/especial.

### Autorización
- `@EnableMethodSecurity` + reglas en `SecurityConfig` (orden corregido).
- Prevención IDOR centralizada en `SecurityUtils.requireSelfOrAdmin`.
- Datos clínicos (`/api/informes`, listados) restringidos por rol.

### Datos en reposo
- AES-256-GCM en campos de categoría especial. Ver `docs/ENCRYPTION.md`.

### Entrada
- `@Valid` en todos los `@RequestBody`; DTOs con validación; NIF con dígito de control; sanitización HTML anti-XSS.

### Transporte y cabeceras
- HSTS (1 año, includeSubDomains), X-Content-Type-Options, frame-options DENY, Referrer-Policy, CSP restrictiva.
- CORS limitado a orígenes de entorno (sin localhost en prod).

### Abuso
- Rate limiting: login 5/15min, registro 3/h, verify 10/h, export RGPD 1/24h. `Retry-After` en 429.

### Subida de ficheros
- MIME real (Tika), allow-list, límite de tamaño, nombres UUID, almacenamiento externo, acceso autenticado.

### Trazabilidad
- Audit log de 14 acciones (login, accesos clínicos, citas, export, borrado).

## Pendiente / mejoras recomendadas

- **Rotar credenciales del historial git** (incidente abierto, V02).
- Rate limiting distribuido (bucket4j-redis) si se escala horizontalmente.
- Job programado para ejecutar la anonimización a los 30 días.
- 2FA (campo `twoFactorEnabled` existe; flujo sin implementar).
- Renderizar PDF en la exportación RGPD.
- Migración de esquema gestionada (Flyway/Liquibase) en lugar de scripts manuales.
- Penetration test antes de producción.

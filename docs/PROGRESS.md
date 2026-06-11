# PROGRESO DEL HARDENING — VITSYNC-API

## Fase 1 — Auditoría inicial ✅ (2026-06-11)

**Qué se hizo:** auditoría completa de código, configuración e historial git. Informe en `docs/AUDITORIA_INICIAL.md` con 21 vulnerabilidades inventariadas (CVSS), análisis RGPD/LOPDGDD/Ley 41-2002/ENS/NIS2.

**Archivos modificados:** ninguno (fase de solo lectura). Creado `docs/AUDITORIA_INICIAL.md`.

**Decisiones:**
- Las credenciales reales encontradas en el historial git (BD Neon, JWT secret, Gmail) exigen **rotación manual inmediata** — no automatizable desde código.
- `Informe` no tiene `contenido/diagnostico/tratamiento`: el cifrado de Fase 2.4 se aplicará a los campos clínicos reales (`notasPersonales`, `alergias`, `condicionesPrevias`, `grupoSanguineo`, `contactoEmergencia`, `historialClinicoId`, `Mensaje.content`).
- `ddl-auto=validate` en prod ⇒ toda tabla nueva llevará script SQL de migración en `scripts/sql/`.

**Pendiente para Fase 2:** parches en orden 2.1 → 2.6.

## Fase 2 — Parches de seguridad ✅ (2026-06-11)

**2.1 Dependencias** (`pom.xml`): jjwt 0.11.5→0.12.6, +actuator, +bucket4j-core 8.10.1, +spring-boot-starter-aop, +tika-core 2.9.2, +jacoco plugin (umbral 80% en service/util). Cada bloque comentado.

**2.2 Secretos**: `application.properties` (raíz y resources) solo placeholders (ahora con claves RSA/AES/upload). `.gitignore` amplía a prod/local properties, `uploads/**`, claves `*.pem/*.key`. Avatar real `git rm --cached` + `uploads/.gitkeep`. `scripts/setup-env.sh` valida entorno y genera claves. `.example` actualizado.

**2.3 JWT RS256 + refresh** (`JwtUtil` reescrito a RS256/jjwt 0.12; nuevas `RefreshToken`, `RefreshTokenRepository`, `RefreshTokenService` con rotación y hash SHA-256; `AuthService`/`AuthController` con `/refresh`, `/logout`, `/logout-all`; access 15min, refresh 7d; `@EnableScheduling` para purga; `scripts/sql/V2__refresh_tokens.sql`; README actualizado con httpOnly cookie). Login ya no filtra enumeración; registro no emite tokens; `JwtAuthenticationFilter` rechaza cuentas no verificadas/suspendidas.

**2.4 Cifrado AES-256-GCM** (`SensitiveDataConverter`, `EncryptionConfig`, `SensitiveDataKeyHolder`): campos cifrados en `Paciente` (alergias, condicionesPrevias, grupoSanguineo, contactoEmergencia, historialClinicoId), `Informe.notasPersonales`, `Mensaje.content`. IV aleatorio por valor, null-safe. `scripts/sql/V3__encrypt_sensitive_columns.sql` (ampliar columnas + nota de migración de datos).

**2.5 Rate limiting** (`RateLimitService` + `RateLimitFilter`, Bucket4j en memoria): login 5/15min, register 3/h, verify 10/h por IP; `Retry-After` en 429. (GDPR_EXPORT 1/24h preparado para Fase 3.)

**2.6 Validación + IDOR**: `@ValidNif` con dígito de control (mod-23, NIE), password ≥12 fuerte, teléfono español, longitudes, en `RegisterRequest`/`UserUpdateRequest`/`VerifyRequest`/`ProfileUpdateRequest`. `HtmlSanitizer` (anti-XSS) en notas e informes y chat. `User.password` WRITE_ONLY + `verificationCode` @JsonIgnore (V01). `UserController` reescrito: IDOR vía `SecurityUtils.requireSelfOrAdmin`, eliminado POST User crudo (V17) y DELETE roto, mocks→501. `InformeController`/`ChatController` con checks de propiedad. `WebSocketAuthInterceptor` (auth STOMP JWT, anti-spoofing V08). `SecurityConfig`: orden de reglas (V12), cabeceras seguridad (V14), citas/uploads autenticados (V09/V11), CORS sin localhost en prod (V20), 401/403 limpios. `GlobalExceptionHandler`: +AccessDenied 403, +BadCredentials 401.

**Decisiones clave:**
- Rate limit en memoria (1 instancia Render). Escalar → bucket4j-redis.
- Refresh token opaco (no JWT), solo hash en BD; rotación detecta replay.
- Converter usa holder estático porque Hibernate instancia los converters, no Spring.
- Las tablas nuevas exigen ejecutar `scripts/sql/V2`/`V3` en Neon antes del deploy (ddl-auto=validate).

**Pendiente:** tests rotos por cambio HS256→RS256 (se arreglan en Fase 6). Fase 3 a continuación.

## Fase 3 — Auditoría y trazabilidad RGPD ✅ (2026-06-11)

**3.1 Audit log AOP**: `AuditAction` (14 acciones), `AuditLog`/`audit_logs` (append-only), `AuditLogRepository`, `@Auditable` + `AuditAspect` (success/failure, no traga excepciones), `AuditService` (REQUIRES_NEW, IP vía X-Forwarded-For). Instrumentado: login success/failure, register, logout (explícito en AuthService); VIEW_MEDICAL_REPORT, VIEW_CHAT, VIEW_PATIENT_DATA, CREATE/CANCEL_APPOINTMENT (@Auditable). `scripts/sql/V4__audit_logs.sql`.

**3.2 Acceso/portabilidad**: `GdprService.collectUserData` + `exportAsZip` (JSON+TXT), `GdprController` `/my-data` y `/my-data/export` con IDOR + rate limit 1/24h (GDPR_EXPORT).

**3.3 Derecho al olvido**: `requestDeletion` (suspende, revoca sesiones, cancela citas futuras + notifica médico, email con fecha +30d, audita) y `anonymizeUser` (seudónimo irreversible, borra texto clínico, re-apunta audit logs). `docs/GDPR_PROCEDURES.md` con el proceso completo.

**Decisiones:** borrado = anonimización (conservación legal Ley 41/2002 + audit append-only). Job de ejecución a 30d queda como pendiente de operativizar (expuesto para admin/scheduled).

## Fase 4 — Arquitectura y rendimiento ✅ (2026-06-11)

**4.1 JPA**: `PacienteMedico` EAGER→LAZY + `@EntityGraph` en repo (evita N+1 sobre subtablas JOINED, V19). `findByRole` ahora filtra en BD y pagina (antes findAll()+memoria). `AdminUserController` paginado (`Pageable`→`Page<UserResponse>`).

**4.2 GlobalExceptionHandler**: +DataIntegrityViolation 409, +MaxUploadSizeExceeded 413, +IllegalState (cifrado) 500 sin stacktrace, +AccessDenied 403, +BadCredentials 401. `server.error.include-stacktrace=never`. `CitaController` ya no hace printStackTrace ni filtra `ex.getMessage()`.

**4.3 Upload seguro**: `FileUploadController` con Apache Tika (MIME real por contenido), allow-list jpeg/png/webp, límite 2MB avatar, nombres UUID, anti path-traversal, directorio externo `vitsync.upload.dir`. `WebConfig` sirve desde ese directorio; `/uploads/**` y `/api/upload/**` exigen autenticación.

**Nota:** paginar TODOS los listados rompería el contrato del frontend; se paginaron los listados admin (grandes) y se documentó. Los catálogos públicos (medicos/especialidades/hospitales) son pequeños y se dejaron como List.

## Fase 5 — Documentación ✅ (2026-06-11)

**Qué se hizo:** documentación de arquitectura y cumplimiento en `docs/`: `SECURITY.md` (modelo de amenazas y controles), `ENCRYPTION.md` (AES-256-GCM, gestión de claves), `GDPR_COMPLIANCE.md` (mapeo Arts. RGPD → implementación), `GDPR_PROCEDURES.md` (proceso de olvido/anonimización), `DATA_FLOWS.md` (flujos de datos sensibles), `API_REFERENCE.md` (endpoints con ejemplos). Javadoc en inglés y comentarios inline en español aplicados durante las Fases 2-4 al reescribir cada clase.

**Pendiente para Fase 6:** suite de tests completa (los existentes estaban rotos por la migración HS256→RS256).

## Fase 6 — Testing completo ✅ (2026-06-11)

**6.1 Unitarios** (sin Spring, Mockito): `JwtUtilTest` (RS256, claims, expirado/manipulado), `SensitiveDataConverterTest` (round-trip, null-safe, IV aleatorio, Unicode), `NifValidatorTest` (dígito control NIF/NIE), `AuthServiceTest` (login/registro/verificación), `AuditAspectTest` (success/failure), `HtmlSanitizerTest` (anti-XSS), `SimpleServicesTest` (User/Cita/Informe/Chat), `AdminMedicoEspecialidadServiceTest`, `GdprServiceTest` (acceso Art. 15, export ZIP Art. 20, borrado/anonimización Art. 17, pseudónimo determinista), `RefreshTokenServiceTest` (emisión con hash, rotación, replay, caducidad, purga), `PacienteMedicoServiceTest` (asignación y lookups).

**6.2 Integración** (`@SpringBootTest` + H2 + MockMvc): `AuthControllerIntegrationTest` (login 400/401, register 201/400 NIF, rate limit 429 con Retry-After) y `SecurityGdprIntegrationTest` (refresh 200 + replay rechazado, logout revoca, validate 401/200, `/api/usuarios` 403 PACIENTE / 200 ADMIN, IDOR 403 en `/VitSync-app/{id}`, respuesta propia sin password/verificationCode, `/my-data` 200 propio / 403 ajeno, `/gdpr-delete` 202 + cuenta suspendida).

**6.3 Cobertura:** JaCoCo con gate al 80% LINE en `service/` y `util/` (falla el build si baja). Resultado: **service 91% / util 96%, 158 tests verdes**. Reporte en `target/site/jacoco/index.html`; cómo verlo documentado en `docs/TESTING.md`.

**Decisiones:**
- `EmailService` excluido del cómputo JaCoCo (plantillas HTML + HTTP a Resend, no unit-testeable de forma significativa) — comentado en `pom.xml`.
- Surefire fuerza `spring.config.location=classpath:` en tests: el `application.properties` de la raíz (file:./, con `${DATABASE_URL}` sin resolver) tenía mayor precedencia y rompía el contexto de los `@SpringBootTest` (bug pre-existente).
- Cada test de integración usa una IP `X-Forwarded-For` única para aislar los buckets del rate limit.
- En `SecurityGdprIntegrationTest` se mockea `EmailService` (`@MockBean`): el borrado GDPR dispara emails y los tests no deben llamar al API externo.

**Pendientes conocidos (fuera del plan de fases):** rotar credenciales del historial git (incidente abierto, AUDITORIA_INICIAL §1.1.1), DPIA (Art. 35), contratos de encargo (Art. 28), job de anonimización a 30 días, 2FA, PDF en export.

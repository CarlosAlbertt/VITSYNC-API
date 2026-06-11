# CLAUDE.md — Contexto para Claude Code

Backend REST sanitario (SaaS) que trata **datos de salud (categoría especial,
Art. 9 RGPD)**. Máximas garantías legales y técnicas.

## Stack
- Spring Boot 3.2.5, Java 21, Maven (usar `./mvnw`).
- Spring Security 6 + **JWT RS256** (jjwt 0.12.6).
- Spring Data JPA + Hibernate 6 + PostgreSQL 15 (Neon serverless).
- WebSocket STOMP + SockJS (chat). Lombok. Resend (email). Bucket4j (rate limit).
- Apache Tika (MIME). JaCoCo (cobertura). H2 (solo tests).
- Deploy: Render (prod/testing). Frontend: Vercel.

## Build / test
- **JDK 21 obligatorio.** Si `java -version` muestra 17, exportar JAVA_HOME al JDK 21
  antes de Maven (en este equipo: `~/.jdks/temurin-21.0.10`).
- Compilar: `./mvnw compile`. Tests: `./mvnw test`. Cobertura: `./mvnw verify`
  → informe en `target/site/jacoco/index.html` (umbral 80% en service/ y util/).

## Convenciones de código
- Javadoc en **inglés** técnico. Comentarios inline en **español** (dominio sanitario).
- Variables/métodos en inglés camelCase; endpoints en kebab-case; constantes UPPER_SNAKE_CASE.
- `@Transactional` solo en services, nunca en controllers.
- Nunca lanzar `Exception`/`RuntimeException` genéricas: usar `BusinessException`,
  `ResourceNotFoundException`, `AccessDeniedException`, `DataIntegrityViolationException`.
- Nunca exponer stack traces ni `ex.getMessage()` interno al cliente.
- Nunca cifrar con algoritmos deprecated (DES/3DES/RSA PKCS1v1.5). Nada de `Math.random()`
  ni `SecureRandom` sin sembrar para criptografía.

## Arquitectura de seguridad (ya implementada)
- Auth: RS256 access 15min + refresh token opaco 7d (hash en BD, rotación). Endpoints
  `/api/auth/{login,register,verify,refresh,logout,logout-all,validate}`.
- Cifrado en reposo AES-256-GCM: `SensitiveDataConverter` (campos clínicos de
  Paciente/Informe/Mensaje). Clave en `ENCRYPTION_KEY`.
- IDOR: `SecurityUtils.requireSelfOrAdmin`. Validación: `@Valid` + DTOs + `@ValidNif`.
- Auditoría: `@Auditable` + `AuditAspect` → tabla `audit_logs` (append-only).
- RGPD: `GdprService`/`GdprController` (my-data, export, gdpr-delete=anonimización).
- Rate limit: `RateLimitFilter`/`RateLimitService` (Bucket4j en memoria).

## Configuración
- `application.properties` (raíz y resources): SOLO placeholders `${VAR}`. NUNCA secretos.
- Variables requeridas: ver `scripts/setup-env.sh` (valida entorno; `--generate-keys`
  genera RSA + AES).
- `ddl-auto=validate` en prod: tablas nuevas requieren ejecutar los scripts de
  `scripts/sql/` (V2 refresh_tokens, V3 cifrado, V4 audit_logs) en Neon antes de desplegar.

## Pendientes conocidos
- **Rotar credenciales del historial git** (incidente abierto — ver AUDITORIA_INICIAL §1.1.1).
- DPIA (Art. 35), contratos de encargo (Art. 28), job de anonimización a 30 días, 2FA, PDF en export.

## Documentación
`docs/`: AUDITORIA_INICIAL, SECURITY, GDPR_COMPLIANCE, GDPR_PROCEDURES, ENCRYPTION,
DATA_FLOWS, API_REFERENCE, TESTING, PROGRESS.

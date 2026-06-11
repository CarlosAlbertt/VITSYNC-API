# PROCEDIMIENTOS RGPD — VITSYNC-API

Operativa técnica de los derechos de los interesados y de la trazabilidad.
Marco legal: RGPD (UE 2016/679), LOPDGDD (LO 3/2018), Ley 41/2002.

## 1. Registro de actividad (Art. 30 RGPD)

- Entidad `AuditLog` → tabla `audit_logs` (append-only, evidencia legal).
- Acciones en enum `AuditAction`: LOGIN_SUCCESS, LOGIN_FAILURE, LOGOUT,
  REGISTER, PASSWORD_CHANGE, VIEW_PATIENT_DATA, VIEW_MEDICAL_REPORT,
  CREATE_APPOINTMENT, MODIFY_APPOINTMENT, CANCEL_APPOINTMENT, VIEW_CHAT,
  EXPORT_DATA, ADMIN_ACCESS, DATA_DELETION_REQUEST.
- Captura automática vía Spring AOP: anotación `@Auditable` + aspecto
  `AuditAspect`. El aspecto registra `success=true` al retornar y
  `success=false` (con el tipo de excepción) si el método lanza, sin tragar
  el error.
- Login success/failure se registran explícitamente en `AuthService` porque
  son acciones con dos resultados distintos del mismo flujo.
- `AuditService.record` corre en transacción `REQUIRES_NEW`: la fila de
  auditoría se confirma aunque la transacción de negocio haga rollback.
- Cada registro guarda: actor (NIF), acción, targetId, success, IP
  (`X-Forwarded-For` tras el proxy de Render), detalle no sensible, timestamp.
- **Prohibido** almacenar contenido clínico en el campo `details`.

## 2. Derecho de acceso y portabilidad (Arts. 15 y 20)

- `GET /api/users/{id}/my-data` → JSON estructurado con perfil, datos
  clínicos (descifrados al vuelo), citas, informes y mensajes.
- `GET /api/users/{id}/my-data/export` → ZIP con `datos.json`
  (legible por máquina, Art. 20) y `datos.txt` (resumen legible por persona).
  - Límite: **1 exportación / 24 h por usuario** (Bucket4j, política
    `GDPR_EXPORT`). Devuelve 429 + `Retry-After` si se excede.
  - Mejora futura: renderizar además un PDF (p. ej. OpenPDF/flying-saucer);
    el ZIP ya está preparado para añadir la entrada.
- Control IDOR: `requireSelfOrAdmin` — el usuario solo accede a lo suyo.

## 3. Derecho de supresión / al olvido (Art. 17)

El borrado es **anonimización**, no borrado físico, porque:
- La documentación clínica debe conservarse (Ley 41/2002 art. 17, mínimo 5
  años desde el alta de cada proceso asistencial).
- Los `audit_logs` son evidencia legal append-only.

### Flujo

1. **Solicitud** — `DELETE /api/users/{id}/gdpr-delete`
   (`GdprService.requestDeletion`):
   - Suspende la cuenta (`suspended=true`) y revoca todos los refresh tokens.
   - Cancela las citas **futuras** y notifica por email al médico de cada una.
   - Envía email al usuario con la **fecha de anonimización programada**
     (hoy + 30 días).
   - Registra `DATA_DELETION_REQUEST` en auditoría.
   - Periodo de espera de **30 días**: ventana para cancelar la solicitud
     (contactando con soporte) y requisito de confirmación. Responde
     `202 Accepted`.
2. **Confirmación por email** — el correo enviado pide confirmar; si el
   usuario no reconoce la solicitud, soporte la revierte antes de la fecha.
3. **Ejecución** — pasados los 30 días (job programado o acción de admin)
   se invoca `GdprService.anonymizeUser`:
   - Reemplaza nombre, apellidos, NIF, email, teléfono, dirección y CP por un
     seudónimo irreversible (`ANON-<sha256(nif:id)[0..16]>`).
   - Borra el texto clínico libre del paciente (alergias, condiciones,
     contacto de emergencia, grupo sanguíneo, id de historial), manteniendo la
     fila por integridad referencial con citas/informes retenidos.
   - Re-apunta las filas de `audit_logs` del NIF original al seudónimo
     (`pseudonymizeActor`): el rastro legal sobrevive, disociado de la persona.

### Pendiente de operativización

- El job programado de ejecución a los 30 días no está activado por defecto:
  hoy `anonymizeUser` se expone para ejecución por admin/tarea. Para
  automatizarlo, persistir la fecha programada (tabla `deletion_requests`) y
  añadir un `@Scheduled` que procese las vencidas. Documentar el responsable
  del tratamiento que supervisa el proceso.

## 4. Cifrado de datos de categoría especial (Art. 9 + Art. 32)

Ver `docs/ENCRYPTION.md`. Campos cifrados en reposo con AES-256-GCM:
`Paciente` (alergias, condicionesPrevias, grupoSanguineo, contactoEmergencia,
historialClinicoId), `Informe.notasPersonales`, `Mensaje.content`.

## 5. Notificación de brechas (Arts. 33/34)

- Plazo: 72 h a la AEPD desde el conocimiento; a los interesados si hay alto
  riesgo.
- **Incidente abierto detectado en la auditoría inicial**: credenciales reales
  (BD, JWT, email) presentes en el historial de git. Acción: rotar todas las
  credenciales y evaluar si hubo acceso no autorizado para decidir la
  notificación. Ver `docs/AUDITORIA_INICIAL.md` §1.1.1.

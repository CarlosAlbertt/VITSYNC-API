# Incidencias y correcciones — VITSYNC-API

> Fecha: 2026-06-16 · Autor: revisión asistida (Claude Code)
> Alcance: diagnóstico del cierre inesperado de Claude Code y corrección de
> errores detectados en el backend. Documento complementario a
> [AUDITORIA_INICIAL.md](AUDITORIA_INICIAL.md) y [SECURITY.md](SECURITY.md).

---

## 1. Cierre inesperado de la sesión de Claude Code (causa raíz)

### Síntoma
Tras un rato de uso, Claude Code lanzaba un error y cerraba la interfaz.

### Causa
El repositorio versionaba **dos copias** del archivo `contexto_tarea.md`
(~398 KB cada una):

| Ruta | Problema |
|------|----------|
| `contexto_tarea.md` (raíz) | Volcado del *transcript* de una sesión anterior de Claude Code (texto con marcos ANSI), no es código. |
| `src/main/java/com/ejemplo/vitsync/contexto_tarea.md` | **Dentro del árbol de fuentes Java**, donde nada que no sea `.java` debería existir. |

Cada archivo equivale a ~130 K tokens. Cuando una búsqueda, un sub-agente o el
propio bucle de la herramienta leía el fichero (especialmente el ubicado en
`src/main/java`, que cae en cualquier exploración del código fuente), el
contexto crecía de forma desmedida hasta agotar la memoria del proceso y
provocar el cierre de la interfaz.

### Corrección aplicada
- Eliminados ambos ficheros del control de versiones (`git rm --cached`) y del
  working tree.
- Añadido a `.gitignore` un patrón para evitar que vuelvan a versionarse:

  ```gitignore
  ### Volcados de sesión de herramientas / transcripts (NUNCA versionar) ###
  contexto_tarea.md
  **/contexto_tarea.md
  ```

### Recomendaciones para evitar recaídas
- No guardar transcripts/volcados de sesión dentro del repo (y mucho menos en
  `src/`). Si se necesitan, dejarlos fuera del proyecto o en una carpeta
  ignorada.
- Mantener `src/main/java` libre de cualquier fichero que no sea código fuente.

---

## 2. Módulo `relationships` (paciente ↔ médico)

Afecta a `PacienteMedicoController`, `PacienteMedicoService` y al contrato con
el frontend (`services/relationships.js`).

### 2.1 Mismatch de contrato API (rompía la asignación)  — CORREGIDO
- **Frontend** (cambio ya presente en el WebApp): `assignPatientToProfessional`
  pasó a enviar un **body JSON** `{ patientId, medicoId }`.
- **Backend**: el endpoint `POST /api/relationships/assign` seguía esperando
  **`@RequestParam`** (query string) → la llamada habría fallado con
  *"Required request parameter 'patientId' is not present"*.
- **Fix**: el controlador acepta ahora `@Valid @RequestBody`
  [`AssignRelationshipRequest`](../src/main/java/com/ejemplo/vitsync/dto/AssignRelationshipRequest.java)
  (`@NotNull` en ambos identificadores). Además, mover los IDs del query string
  al body evita que queden registrados en logs de acceso / historial del
  navegador.

### 2.2 Fuga de detalle interno de excepciones  — CORREGIDO
El controlador hacía `catch (Exception e)` y devolvía `e.getMessage()` al
cliente, violando la convención del proyecto *"Nunca exponer stack traces ni
`ex.getMessage()` interno al cliente"*. Cualquier `NullPointerException` o error
de base de datos se filtraba al exterior.
- **Fix**: eliminado el `try/catch`. Las excepciones se delegan al
  `GlobalExceptionHandler`, que ya traduce a respuestas HTTP genéricas y seguras.

### 2.3 Excepciones genéricas en el service  — CORREGIDO
`PacienteMedicoService` lanzaba `throws Exception`, `new Exception(...)` y
`new RuntimeException(...)`, violando *"Nunca lanzar `Exception`/`RuntimeException`
genéricas"*.
- **Fix**:
  - Recurso inexistente → `ResourceNotFoundException` (→ HTTP 404).
  - Relación duplicada → `BusinessException` (→ HTTP 400).
  - Eliminado `throws Exception` de la firma.
- Los tests existentes siguen verdes (las nuevas excepciones extienden
  `RuntimeException`/`Exception` y conservan los mensajes comprobados).

---

## 3. Hallazgos pendientes (documentados, NO corregidos en esta pasada)

Se documentan para decisión del equipo por requerir definir la regla de negocio
o por riesgo menor:

### 3.1 Autorización / IDOR en `/api/relationships/**`  — PENDIENTE (medio-alto)
En `SecurityConfig` estos endpoints están como `.authenticated()`, sin
restricción de rol ni de propiedad. Un usuario autenticado podría:
- asignar **cualquier** paciente a **cualquier** médico (`POST /assign`), o
- listar pacientes de un médico arbitrario (`GET /medico/{id}/pacientes`) —
  acceso a **datos de salud de terceros (Art. 9 RGPD)**.

**Recomendación**: aplicar `SecurityUtils.requireSelfOrAdmin` (el paciente solo
se asigna a sí mismo; el médico solo consulta sus propios pacientes) o exigir
rol `ADMIN`, según la regla de negocio deseada.

### 3.2 Fuga de mensajes en `MedicoController` y `EspecialidadController` — PENDIENTE (bajo)
Ambos devuelven `e.getMessage()` de `IllegalArgumentException` en `create`/`update`.
El mensaje proviene del propio dominio (mensajes controlados), por lo que el
riesgo es bajo, pero por coherencia conviene migrar a `BusinessException` /
`ResourceNotFoundException` y dejar que el `GlobalExceptionHandler` responda.

---

## 4. Verificación

| Comprobación | Resultado |
|--------------|-----------|
| `./mvnw compile` | ✅ BUILD SUCCESS |
| `./mvnw test -Dtest=PacienteMedicoServiceTest,GlobalExceptionHandlerTest` | ✅ 11/11 |
| WebApp `npm run build` | ✅ OK |
| WebApp `npm run test` (vitest) | ✅ 54/54 |

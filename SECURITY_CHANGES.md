# VITSYNC API — Registro de cambios de seguridad

**Fecha:** 2026-04-27  
**Rama:** `feature/mi-salud-sprint-Javier`  
**Autor:** Javier Crespo Moll  

---

## Contexto

Auditoría manual de seguridad sobre el código fuente de la API. Se identificaron 12 vulnerabilidades distribuidas entre los controllers, servicios, repositorios y utilidades. Los problemas más graves eran vulnerabilidades IDOR (Insecure Direct Object Reference) que permitían a cualquier usuario autenticado acceder y modificar datos de otros usuarios sin ningún tipo de comprobación de propiedad.

---

## Fichero nuevo: `SecurityUtil.java`

**Ruta:** `src/main/java/com/ejemplo/vitsync/util/SecurityUtil.java`

### Propósito

Antes de este cambio no existía ningún mecanismo centralizado para verificar que el usuario autenticado tiene derecho a acceder a un recurso concreto. Cada controller tenía que reimplementar la lógica de extracción del principal del `SecurityContext`, lo que favorecía los olvidos.

`SecurityUtil` es un `@Component` de Spring que encapsula tres operaciones:

- `getCurrentNif()` — extrae el NIF del token JWT activo desde `SecurityContextHolder`.
- `getCurrentUser()` — carga la entidad `User` completa desde la base de datos usando ese NIF. Lanza `401 Unauthorized` si no se encuentra.
- `assertOwnerOrAdmin(Long resourceOwnerId)` — compara el ID del usuario autenticado con el ID del dueño del recurso. Si no coinciden **y** el usuario no tiene rol `ADMIN`, lanza `403 Forbidden`. Este método es la pieza central de todas las correcciones IDOR.

---

## Vulnerabilidades corregidas

---

### 1. IDOR en `UserController` — acceso a perfil ajeno

**Severidad:** Crítica  
**Endpoint afectado:** `GET /VitSync-app/{id}`

**Vulnerabilidad:**  
El endpoint recibía un `{id}` por path variable y devolvía el perfil del usuario correspondiente sin ninguna comprobación. Cualquier usuario autenticado podía consultar los datos personales de cualquier otro usuario simplemente cambiando el ID en la URL.

**Corrección:**  
Se añade `securityUtil.assertOwnerOrAdmin(id)` al inicio del método. Si el ID solicitado no coincide con el del usuario autenticado y este no es ADMIN, la petición se rechaza con `403 Forbidden` antes de tocar la base de datos.

```java
// Antes
public User findById(@PathVariable Long id) {
    return userService.findById(id);  // sin comprobación
}

// Después
public ResponseEntity<UserResponse> findById(@PathVariable Long id) {
    securityUtil.assertOwnerOrAdmin(id);   // lanza 403 si no es el dueño ni ADMIN
    User user = userService.findById(id);
    if (user == null) return ResponseEntity.notFound().build();
    return ResponseEntity.ok(UserResponse.fromEntity(user));
}
```

---

### 2. IDOR en `UserController` — borrado de usuarios ajenos

**Severidad:** Crítica  
**Endpoint afectado:** `DELETE /VitSync-app/{id}`

**Vulnerabilidad:**  
Cualquier usuario autenticado podía eliminar la cuenta de cualquier otro usuario enviando su ID. No había ninguna verificación de propiedad ni de rol.

**Corrección:**  
Se añade `securityUtil.assertOwnerOrAdmin(id)`. Un usuario solo puede eliminarse a sí mismo; solo ADMIN puede eliminar a terceros.

---

### 3. IDOR en `UserController` — cambio de avatar ajeno

**Severidad:** Crítica  
**Endpoint afectado:** `PATCH /VitSync-app/api/users/{id}/avatar`

**Vulnerabilidad:**  
El endpoint recibía un `{id}` y actualizaba el campo `avatarUrl` del usuario correspondiente sin verificar que el solicitante sea el propietario de esa cuenta.

**Corrección:**  
Se añade `securityUtil.assertOwnerOrAdmin(id)` antes de realizar la actualización. Además, el método ahora devuelve `UserResponse` en lugar de una respuesta vacía, evitando exponer la entidad `User` directa.

---

### 4. Exposición de datos sensibles en `UserController`

**Severidad:** Alta  
**Endpoints afectados:** `GET /VitSync-app` y `GET /VitSync-app/{id}`

**Vulnerabilidad:**  
Ambos endpoints devolvían directamente la entidad `User` de JPA, que incluye campos sensibles como `password` (hash BCrypt), `verificationCode` y otros campos internos. Aunque el hash no permite obtener la contraseña original, su exposición innecesaria aumenta la superficie de ataque y viola el principio de mínima exposición.

**Corrección:**  
Todos los endpoints de `UserController` ahora devuelven `UserResponse`, un DTO que únicamente expone los campos que el frontend necesita, excluyendo explícitamente `password` y `verificationCode`.

---

### 5. Falta de restricción de rol en `GET /VitSync-app` y `POST /VitSync-app`

**Severidad:** Alta  
**Endpoints afectados:** `GET /VitSync-app` (listado de todos los usuarios), `POST /VitSync-app` (creación directa de usuario)

**Vulnerabilidad:**  
- `GET /VitSync-app` listaba todos los usuarios del sistema para cualquier usuario autenticado (paciente, médico, admin). Un paciente no debe poder ver la lista completa de usuarios.  
- `POST /VitSync-app` permitía a cualquier usuario autenticado crear usuarios directamente pasando una entidad `User` cruda, incluyendo el campo `role`. Un atacante podía asignarse el rol `ADMIN`. Además, la contraseña no se hasheaba por este camino (a diferencia de `/api/auth/register`).

**Corrección:**  
Se añade `@PreAuthorize("hasRole('ADMIN')")` en ambos métodos. El listado de usuarios y la creación directa quedan restringidos exclusivamente a administradores.

---

### 6. IDOR en `CitaController` — acceso a citas de otros usuarios

**Severidad:** Crítica  
**Endpoint afectado:** `GET /api/citas`

**Vulnerabilidad:**  
El endpoint devolvía **todas** las citas de la base de datos (`citaRepository.findAll()`) a cualquier usuario autenticado. Un paciente podía ver las citas de todos los demás pacientes y médicos del sistema.

**Corrección:**  
Se introduce lógica de filtrado por rol usando `SecurityUtil.getCurrentUser()`:

- `ADMIN` → recibe todas las citas (comportamiento original, ahora explícito).
- `MEDICO` → recibe solo las citas donde él es el médico asignado (`findByMedicoId`).
- `PACIENTE` → recibe solo sus propias citas (`findByPacienteId`).

Para soportar estas consultas se añadieron `findByPacienteId` y `findByMedicoId` a `CitaRepository` e igual en `CitaService`.

---

### 7. IDOR en `CitaController` — cancelación de citas ajenas

**Severidad:** Crítica  
**Endpoint afectado:** `PUT /api/citas/{id}/cancel`

**Vulnerabilidad:**  
Cualquier usuario autenticado podía cancelar cualquier cita del sistema conociendo su ID, sin ninguna verificación de que le pertenezca.

**Corrección:**  
Antes de cancelar se comprueba que el usuario autenticado sea el paciente de la cita, el médico de la cita, o un ADMIN. En caso contrario se devuelve `403 Forbidden`. Se añade control de null en `pacienteId` y `medicoId` para evitar NPE si alguno de los campos está sin asignar.

---

### 8. IDOR en `InformeController` — acceso a informes de otros pacientes

**Severidad:** Crítica  
**Endpoint afectado:** `GET /api/informes`

**Vulnerabilidad:**  
Idéntica a la de citas: `informeRepository.findAll()` devolvía todos los informes médicos del sistema a cualquier usuario autenticado. Un paciente podía ver los informes clínicos de otros pacientes.

**Corrección:**  
Mismo patrón que citas: filtrado por rol con `findByPacienteId` y `findByMedicoId` añadidos a `InformeRepository` e `InformeService`.

---

### 9. IDOR en `InformeController` — edición de notas en informes ajenos

**Severidad:** Crítica  
**Endpoint afectado:** `PUT /api/informes/{id}/notes`

**Vulnerabilidad:**  
Cualquier usuario autenticado podía modificar las notas personales de cualquier informe médico del sistema.

**Corrección:**  
Se verifica que el solicitante sea el paciente o el médico del informe, o un ADMIN. Se añade control de null en los IDs para evitar NPE.

---

### 10. Falta de autorización en `PacienteMedicoController`

**Severidad:** Alta  
**Endpoints afectados:** `POST /api/relationships/assign`, `GET /api/relationships/paciente/{id}/medicos`, `GET /api/relationships/medico/{id}/pacientes`

**Vulnerabilidad:**  
- Cualquier usuario autenticado podía asignar cualquier paciente a cualquier médico (`POST /assign`), operación que debería estar reservada a ADMIN o al propio médico.  
- Cualquier usuario podía consultar los médicos de cualquier paciente y los pacientes de cualquier médico, exponiendo relaciones clínicas privadas.

**Corrección:**  
- `POST /assign` → `@PreAuthorize("hasRole('ADMIN') or hasRole('MEDICO')")`.  
- `GET /paciente/{id}/medicos` → `assertOwnerOrAdmin(id)`: solo el propio paciente o ADMIN.  
- `GET /medico/{id}/pacientes` → `assertOwnerOrAdmin(id)`: solo el propio médico o ADMIN.

---

### 11. Defensa en profundidad: `@PreAuthorize` en controllers de administración

**Severidad:** Media  
**Ficheros afectados:** `AdminUserController`, `MedicoController`, `EspecialidadController`

**Vulnerabilidad:**  
Estos controllers dependían exclusivamente de las reglas definidas en `SecurityConfig` para restringir el acceso a operaciones de escritura (`POST`, `PUT`, `DELETE`, `PATCH`). Una refactorización accidental del `SecurityConfig` (por ejemplo, reordenar reglas o ampliar un patrón de URL) podría dejar estos endpoints desprotegidos sin que el código del controller diera ninguna pista.

**Corrección:**  
Se añade `@PreAuthorize("hasRole('ADMIN')")` en todos los métodos de escritura y en los endpoints `/admin` de cada controller. De esta forma la autorización está declarada en dos lugares independientes (SecurityConfig + anotación), y un fallo en uno no compromete el sistema.

---

### 12. Validación de ficheros en `FileUploadController`

**Severidad:** Alta  
**Endpoint afectado:** `POST /api/upload/avatar`

**Vulnerabilidades:**

**a) Sin whitelist de extensiones:** El endpoint aceptaba cualquier tipo de fichero. Un atacante podía subir archivos `.html`, `.svg` con contenido XSS, o ficheros con extensiones ejecutables.

**b) Sin límite de tamaño en código:** No había validación del tamaño del fichero en el propio endpoint (más allá de la configuración global de Spring, que puede ser alterada o no estar configurada).

**c) NPE si el nombre de fichero no tiene extensión:** El código original hacía `filename.substring(filename.lastIndexOf("."))`. Si el filename no contiene un punto, `lastIndexOf` devuelve `-1` y `substring(-1)` lanza `StringIndexOutOfBoundsException`.

**Corrección:**

```java
private static final Set<String> ALLOWED_EXTENSIONS =
    Set.of(".jpg", ".jpeg", ".png", ".gif", ".webp");
private static final long MAX_SIZE_BYTES = 5 * 1024 * 1024; // 5 MB

// 1. Comprobación de tamaño
if (file.getSize() > MAX_SIZE_BYTES) {
    return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
        .body("El archivo supera el límite de 5 MB");
}

// 2. Comprobación de nombre y extensión válida
if (originalFilename == null || !originalFilename.contains(".")) {
    return ResponseEntity.badRequest().body("Nombre de archivo inválido");
}
String extension = originalFilename
    .substring(originalFilename.lastIndexOf(".")).toLowerCase();
if (!ALLOWED_EXTENSIONS.contains(extension)) {
    return ResponseEntity.badRequest()
        .body("Tipo de archivo no permitido. Se aceptan: JPG, JPEG, PNG, GIF, WEBP");
}
```

---

### 13. Exposición de datos personales en logs

**Severidad:** Alta  
**Fichero afectado:** `AuthController`

**Vulnerabilidad:**  
El controller logueaba el NIF del usuario a nivel `INFO` en los intentos de login y registro:

```java
logger.info("Intento de login para usuario: {}", request.getNif());
logger.info("Login exitoso para usuario: {}", request.getNif());
logger.info("Intento de registro para NIF: {}", request.getNif());
logger.info("Registro exitoso para NIF: {}", request.getNif());
```

El NIF es un dato personal identificativo (equivalente a un número de identidad). Su presencia en logs de aplicación implica que queda registrado en sistemas de log agregados (Render logs, etc.), accesibles por personal técnico y potencialmente por terceros, violando la mínima exposición de datos personales.

**Corrección:**  
Se eliminan los identificadores de usuario de todos los mensajes de log de `AuthController`. Los eventos de negocio se siguen registrando pero sin datos personales:

```java
logger.info("Intento de login recibido");
logger.info("Login exitoso");
logger.info("Intento de registro recibido");
logger.info("Registro exitoso");
```

---

### 14. `UserResponse` incompleto

**Severidad:** Baja (calidad de datos)  
**Fichero afectado:** `UserResponse.java`

**Problema:**  
El DTO `UserResponse` no incluía los campos `avatarUrl` ni `suspended`, que son necesarios para el frontend (mostrar la foto de perfil y el estado de la cuenta). Esto forzaba a devolver la entidad `User` completa en algunos endpoints, exponiendo campos sensibles.

**Corrección:**  
Se añaden `avatarUrl` y `suspended` al DTO y a su método `fromEntity()`.

---

## Resumen de ficheros modificados

| Fichero | Tipo de cambio |
|---------|---------------|
| `util/SecurityUtil.java` | **Nuevo** — utilidad central de autorización |
| `controller/UserController.java` | IDOR fix, restricción de rol, uso de DTO |
| `controller/CitaController.java` | IDOR fix, filtrado por rol |
| `controller/InformeController.java` | IDOR fix, filtrado por rol |
| `controller/PacienteMedicoController.java` | IDOR fix, restricción de rol en assign |
| `controller/AdminUserController.java` | `@PreAuthorize` defensa en profundidad |
| `controller/MedicoController.java` | `@PreAuthorize` defensa en profundidad |
| `controller/EspecialidadController.java` | `@PreAuthorize` defensa en profundidad |
| `controller/FileUploadController.java` | Whitelist extensiones + límite tamaño + fix NPE |
| `controller/AuthController.java` | Sanitización de logs |
| `dto/UserResponse.java` | Añadidos `avatarUrl` y `suspended` |
| `repository/CitaRepository.java` | `findByPacienteId`, `findByMedicoId` |
| `repository/InformeRepository.java` | `findByPacienteId`, `findByMedicoId` |
| `service/CitaService.java` | Métodos de consulta filtrada por usuario |
| `service/InformeService.java` | Métodos de consulta filtrada por usuario |

---

## Vulnerabilidades pendientes (fuera del alcance de este PR)

Las siguientes mejoras fueron identificadas pero requieren más trabajo de diseño o dependencias externas:

| # | Problema | Recomendación |
|---|----------|---------------|
| 1 | **Rate limiting en `/api/auth`** | Implementar con Bucket4j o Spring's `HandlerInterceptor` para limitar intentos de login por IP |
| 2 | **`POST /VitSync-app` no hashea contraseñas** | El endpoint de creación directa (solo ADMIN) no pasa por `AuthService`. Añadir `passwordEncoder.encode()` o eliminarlo en favor de `/api/auth/register` |
| 3 | **CORS con localhost hardcodeado** | `SecurityConfig` añade `localhost:5173`, `localhost:3000`, `localhost:4000` explícitamente como orígenes permitidos. En producción solo debería leerse de `CORS_ALLOWED_ORIGINS` |
| 4 | **Almacenamiento de avatares en disco local** | Los uploads en `/uploads/` no persisten en Render (plan gratuito). Migrar a S3 o Cloudinary |
| 5 | **2FA sin implementar** | El campo `twoFactorEnabled` existe en el modelo pero la lógica no está implementada |
| 6 | **Doble consulta redundante en `AuthService.verifyAccount`** | `userRepository.findByEmail(email)` se llama y su resultado se ignora antes de `existsByEmail`. Menor, pero indica código no revisado |

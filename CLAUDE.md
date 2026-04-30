# VITSYNC API — CLAUDE.md

## ¿Qué es este proyecto?
API REST del sistema médico VitSync. Gestiona autenticación, usuarios, médicos, citas, informes y chat en tiempo real. Desplegada en Render, BD en Neon (PostgreSQL serverless).

- **URL Producción:** https://vitsync-api.onrender.com
- **URL Testing:** https://vitsync-api-testing.onrender.com
- **Deploy:** Render (auto-deploy desde rama `master` → prod, `develop` → testing)

---

## Stack técnico
- **Framework:** Spring Boot 3.2.5
- **Lenguaje:** Java 17
- **Base de datos:** PostgreSQL (Neon serverless) vía JPA/Hibernate
- **Autenticación:** JWT (jjwt 0.11.5) + Spring Security
- **ORM:** Spring Data JPA + Hibernate (`ddl-auto=update`)
- **Email:** Resend API
- **WebSocket:** STOMP sobre WebSocket
- **Build:** Maven (mvnw)
- **Lombok:** Sí (genera getters/setters/constructores)

---

## Estructura del proyecto
```
src/main/java/com/ejemplo/vitsync/
├── VitSyncApplication.java
├── config/
│   ├── JwtAuthenticationFilter.java  → Filtro JWT en cada request
│   ├── SecurityConfig.java           → Spring Security config + rutas públicas/privadas
│   ├── WebConfig.java                → CORS config (leer desde env CORS_ALLOWED_ORIGINS)
│   └── WebSocketConfig.java          → Config STOMP WebSocket
├── controller/
│   ├── AuthController.java           → /api/auth/* (login, register, verify)
│   ├── UserController.java           → /VitSync-app/* (CRUD usuarios + endpoints perfil)
│   ├── MedicoController.java         → /api/medicos/*
│   ├── CitaController.java           → /api/citas/*
│   ├── InformeController.java        → /api/informes/*
│   ├── EspecialidadController.java   → /api/especialidades/*
│   ├── AdminUserController.java      → /api/admin/users/*
│   ├── ChatController.java           → WebSocket chat
│   ├── FileUploadController.java     → /api/upload/*
│   └── PacienteMedicoController.java → /api/paciente-medico/*
├── model/
│   ├── User.java          → Entidad base (herencia JOINED)
│   ├── Paciente.java      → Extiende User (rol PATIENT)
│   ├── Medico.java        → Extiende User (rol MEDICO)
│   ├── Administrador.java → Extiende User (rol ADMIN)
│   ├── Cita.java
│   ├── Informe.java
│   ├── Especialidad.java
│   ├── ChatMessage.java
│   ├── Mensaje.java
│   ├── PacienteMedico.java → Relación many-to-many
│   └── HistorialAcceso.java
├── dto/                   → DTOs para request/response
├── service/               → Lógica de negocio
├── repository/            → Interfaces JPA
├── enums/
│   ├── Role.java          → PATIENT, MEDICO, ADMIN
│   └── Gender.java
├── exception/
│   └── GlobalExceptionHandler.java
├── util/
│   └── JwtUtil.java
└── validation/
    └── UserValidation.java
```

---

## Variables de entorno (todas obligatorias)
```properties
DATABASE_URL=jdbc:postgresql://...neon.tech/vitsync-prod
DATABASE_USERNAME=...
DATABASE_PASSWORD=...
JWT_SECRET=clave_muy_larga_minimo_256_bits
JWT_EXPIRATION=86400000
CORS_ALLOWED_ORIGINS=https://vitsync.es
RESEND_API_KEY=re_...
MAIL_FROM_ADDRESS=VitSync <no-reply@vitsync.es>
PORT=8080
```

Para desarrollo local: copiar `application-dev.properties.example` a `application-dev.properties` y rellenar.
Ejecutar con: `./mvnw spring-boot:run -Dspring.profiles.active=dev`

---

## Modelo de datos clave

### User (tabla base `Users`, herencia JOINED)
```java
id, name, firstName, secondName, nif (único), email (único),
password (bcrypt), gender (MALE/FEMALE/OTHER), role (PATIENT/MEDICO/ADMIN),
birthDate, phone, address, postCode, country,
verificationCode, isVerified, twoFactorEnabled, suspended, avatarUrl
```

### Herencia
- `Paciente` → extiende `User` con campos médicos específicos
- `Medico` → extiende `User` con especialidad, horario, etc.
- `Administrador` → extiende `User`

---

## Endpoints implementados

### Auth (públicos)
```
POST /api/auth/login              → { nif, password } → { token, role, id, ... }
POST /api/auth/register           → RegisterRequest → 201
POST /api/auth/verify             → { email, code }
GET  /api/auth/validate           → valida token JWT
```

### Usuarios
```
GET    /VitSync-app               → todos los usuarios (ADMIN)
GET    /VitSync-app/{id}          → perfil usuario por id
POST   /VitSync-app               → crear usuario
DELETE /VitSync-app/{id}          → eliminar usuario
PATCH  /VitSync-app/api/users/{id}/avatar → { avatarUrl } actualizar foto perfil
PUT    /VitSync-app/api/users/profile     → TODO: implementar update real
PUT    /VitSync-app/api/users/security/2fa → TODO: implementar 2FA
```

### Citas
```
GET  /api/citas             → todas las citas
PUT  /api/citas/{id}/cancel → cancelar cita
```

### Informes
```
GET /api/informes            → todos los informes
PUT /api/informes/{id}/notes → { notasPersonales } actualizar notas
```

### Archivos
```
POST /api/upload/avatar → multipart/form-data → { url }
```

### Especialidades y Médicos
```
GET /api/especialidades
GET /api/medicos
```

---

## Sprint actual — Tareas a hacer

### 1. Implementar update de perfil real (prioridad alta)
El frontend tiene mock, necesita endpoint real:
```java
PUT /api/users/{id}/profile
Body: { name, firstName, secondName, email, phone, address, postCode, country, gender }
→ Actualizar campos del User en BD
→ Devolver el User actualizado
```

### 2. Endpoint GET citas por usuario
Actualmente `GET /api/citas` devuelve todas. Filtrar por el usuario autenticado:
```java
GET /api/citas/me  → citas del usuario del token JWT
```

### 3. Endpoint GET informes por usuario
Igual que citas:
```java
GET /api/informes/me → informes del usuario del token JWT
```

### 4. Generación/descarga de informe en PDF
```java
GET /api/informes/{id}/pdf → genera y devuelve el informe como PDF (Content-Type: application/pdf)
```

### 5. Nueva funcionalidad: Indicadores de Salud ("Mi Salud")
Sistema de métricas de salud por categoría para el paciente. Cada categoría tiene un porcentaje de salud calculado a partir de sus indicadores:

Categorías: CARDIO, METABOLISMO, ACTIVIDAD, NUTRICION, AUDICION, PULMONES

Endpoints necesarios:
```java
GET  /api/salud/resumen          → { categorias: [{ nombre, porcentaje, variacion }] }
GET  /api/salud/{categoria}      → detalle de categoría con indicadores y gráfico temporal
POST /api/salud/{categoria}/indicador → añadir/actualizar un indicador
```

Modelo sugerido para los indicadores:
```java
// IndicadorSalud.java
id, pacienteId, categoria (enum), nombre, valor, unidad, fecha, estado (NORMAL/ALERTA/CRITICO)
```

### 6. Mejoras de seguridad (prioridad alta — usar Semgrep MCP para auditar)
- **Validar que cada usuario solo accede a sus propios datos** — actualmente `GET /VitSync-app/{id}` no verifica que el id del token coincida con el id solicitado
- **Implementar autorización por rol** en todos los endpoints (actualmente SecurityConfig puede tener rutas mal protegidas)
- **Rate limiting** en endpoints de auth para prevenir fuerza bruta
- **Validar tamaño y tipo de archivo** en `FileUploadController` (actualmente no valida extensión ni tamaño)
- **Sanitizar logs** — el logger imprime el NIF del usuario, datos sensibles no deben loguearse
- **Añadir `@PreAuthorize`** en controllers para endpoints de admin
- **Hashear contraseñas** con BCrypt si no está ya implementado (verificar en AuthService)
- **CORS** — verificar que `CORS_ALLOWED_ORIGINS` solo permite los dominios necesarios

### 7. 2FA (Two Factor Authentication)
El modelo `User` ya tiene campo `twoFactorEnabled`. Implementar lógica real:
- Al hacer login con 2FA activado → enviar código por email (Resend)
- Endpoint de verificación del código

---

## Convenciones de código
- Entidades con anotaciones Lombok (`@Data`, `@Builder`, etc.)
- DTOs separados de entidades para request/response
- Services contienen lógica de negocio, Controllers solo delegan
- `@Valid` en todos los `@RequestBody`
- `GlobalExceptionHandler` para errores centralizados
- Logs con SLF4J — **no loggear passwords, tokens ni datos sensibles**
- Nombres de endpoints en inglés, mensajes de respuesta en español

## Comandos útiles
```bash
./mvnw spring-boot:run                                    # local sin perfil
./mvnw spring-boot:run -Dspring.profiles.active=dev       # local con BD testing
./mvnw clean package                                       # build JAR
./mvnw test                                                # tests
```

## Notas importantes
- `ddl-auto=update` → Hibernate actualiza el esquema automáticamente. Cuidado con cambios en entidades en producción.
- La carpeta `uploads/` en el proyecto almacena avatares localmente — en producción esto no persiste en Render (usar S3 o Cloudinary en el futuro)
- Render en plan gratuito duerme después de inactividad — el primer request puede tardar ~30s

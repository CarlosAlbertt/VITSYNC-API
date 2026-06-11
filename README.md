# 🏥 VITSYNC-API — Backend REST

> API REST del ecosistema VitSync, construida con Spring Boot 3.2.5 y Java 21. Gestiona la autenticación, usuarios, citas médicas, informes, especialidades, chat en tiempo real y panel de administración.

---

## 📋 Índice

- [Requisitos Previos](#-requisitos-previos)
- [Instalación y Arranque](#-instalación-y-arranque)
- [Stack Tecnológico](#-stack-tecnológico)
- [Arquitectura del Proyecto](#-arquitectura-del-proyecto)
- [Modelo de Datos](#-modelo-de-datos)
- [Endpoints de la API](#-endpoints-de-la-api)
- [Seguridad y Autenticación](#-seguridad-y-autenticación)
- [Chat en Tiempo Real](#-chat-en-tiempo-real)
- [Testing](#-testing)
- [Despliegue](#-despliegue)

---

## 🔧 Requisitos Previos

| Herramienta | Versión mínima | Descripción |
|---|---|---|
| **Java JDK** | 21 | Lenguaje principal. [Descargar](https://www.oracle.com/java/technologies/downloads/#java21) |
| **Maven** | 3.9+ | Gestor de dependencias (incluido como Maven Wrapper `mvnw`) |
| **PostgreSQL** | 15+ | Base de datos. Usamos [Neon](https://neon.tech/) (serverless) |
| **IDE** | IntelliJ IDEA / VS Code | Recomendado IntelliJ para desarrollo Java |

> **Nota**: No necesitas instalar Maven globalmente. El proyecto incluye `mvnw` (Maven Wrapper) que descarga la versión correcta automáticamente.

---

## 🚀 Instalación y Arranque

### 1. Clonar el repositorio

```bash
git clone https://github.com/CarlosAlbertt/VITSYNC-API.git
cd VITSYNC-API
```

### 2. Configurar credenciales

```bash
# Copiar la plantilla de configuración
cp src/main/resources/application-dev.properties.example src/main/resources/application-dev.properties

# Editar con tus credenciales (BD, JWT, Resend)
# Ver sección de Configuración de Entornos para más detalles
```

### 3. Arrancar en modo desarrollo

```bash
# Con Maven Wrapper (recomendado):
./mvnw spring-boot:run -Dspring.profiles.active=dev

# Con IntelliJ:
# Edit Configurations > VM options: -Dspring.profiles.active=dev
# Run > VitSyncApplication
```

### 4. Verificar que funciona

```bash
# El servidor arranca en http://localhost:8080
curl http://localhost:8080/api/auth/validate
# Debería devolver: {"valid": false, "error": "Token no proporcionado"}
```

---

## 🛠 Stack Tecnológico

### ¿Qué es cada cosa?

| Tecnología | Versión | ¿Para qué sirve? |
|---|---|---|
| **Java 21** | 21 | Lenguaje de programación. La versión 21 es LTS (soporte a largo plazo) |
| **Spring Boot** | 3.2.5 | Framework que simplifica la creación de aplicaciones web en Java. Configura automáticamente servidores, seguridad, conexiones a BD, etc. |
| **Spring Security** | 6.x | Módulo de seguridad. Gestiona quién puede acceder a qué (autenticación y autorización) |
| **Spring Data JPA** | 3.x | Capa de acceso a datos. Convierte objetos Java ↔ tablas SQL automáticamente (ORM) |
| **Hibernate** | 6.x | Implementación de JPA. Es el "motor" que traduce entre Java y PostgreSQL |
| **Spring WebSocket** | - | Permite comunicación bidireccional en tiempo real (para el chat) |
| **JWT (jjwt)** | 0.11.5 | Tokens de autenticación. Cada usuario recibe un "pase" firmado digitalmente |
| **PostgreSQL** | 15+ | Base de datos relacional donde se almacena toda la información |
| **Lombok** | 1.18.36 | Genera automáticamente getters, setters, constructores, etc. Reduce código repetitivo |
| **H2** | - | Base de datos en memoria usada solo para tests automatizados |
| **Maven** | 3.9+ | Gestiona las dependencias del proyecto (como npm para Java) |

### Dependencias en `pom.xml`

El archivo `pom.xml` es el equivalente a `package.json` en JavaScript. Define:
- **`<dependencies>`**: Librerías que usa el proyecto
- **`<build>`**: Cómo se compila y empaqueta
- **`<plugins>`**: Herramientas adicionales (compilador, Lombok, etc.)

---

## 🏗 Arquitectura del Proyecto

### Patrón: Arquitectura en Capas

```
📥 Petición HTTP del cliente (frontend)
        │
        ▼
┌─────────────────────────┐
│   🎯 CONTROLLER         │  ← Recibe la petición, valida formato, delega al servicio
│   (@RestController)      │     Ejemplo: AuthController.java
├─────────────────────────┤
│   ⚙️ SERVICE             │  ← Lógica de negocio (reglas, cálculos, validaciones)
│   (@Service)             │     Ejemplo: AuthService.java
├─────────────────────────┤
│   💾 REPOSITORY          │  ← Acceso a base de datos (queries SQL automáticas)
│   (@Repository)          │     Ejemplo: UserRepository.java
├─────────────────────────┤
│   📊 MODEL (Entity)      │  ← Representación de las tablas como clases Java
│   (@Entity)              │     Ejemplo: User.java, Cita.java
└─────────────────────────┘
        │
        ▼
    🗄️ PostgreSQL (Neon)
```

### ¿Por qué capas?

- **Separación de responsabilidades**: Cada capa hace una sola cosa
- **Testabilidad**: Puedes testear cada capa de forma aislada (con mocks)
- **Mantenibilidad**: Cambiar la BD no afecta a los controladores

### Estructura de carpetas

```
src/main/java/com/ejemplo/vitsync/
├── VitSyncApplication.java         # Punto de entrada de la app
├── config/                          # Configuración de Spring
│   ├── SecurityConfig.java          # Reglas de seguridad, CORS, rutas protegidas
│   ├── JwtAuthenticationFilter.java # Filtro que valida el JWT en cada petición
│   ├── WebConfig.java               # Configuración MVC general
│   └── WebSocketConfig.java         # Configuración del chat (STOMP + SockJS)
├── controller/                      # 12 controladores REST
│   ├── AuthController.java          # Login, registro, verificación, validate
│   ├── AdminUserController.java     # CRUD usuarios (solo ADMIN)
│   ├── MedicoController.java        # CRUD médicos
│   ├── EspecialidadController.java   # CRUD especialidades
│   ├── CitaController.java          # Gestión de citas
│   ├── InformeController.java       # Informes médicos
│   ├── HospitalController.java      # Centros médicos
│   ├── ChatController.java          # Mensajería en tiempo real
│   ├── UserController.java          # Perfil de usuario
│   ├── PacienteMedicoController.java # Relaciones paciente-médico
│   ├── HorarioController.java       # Horarios disponibles
│   └── FileUploadController.java    # Subida de archivos (avatar, docs)
├── dto/                             # Data Transfer Objects
│   ├── LoginRequest.java            # Datos que envía el frontend para login
│   ├── RegisterRequest.java         # Datos de registro
│   ├── AuthResponse.java            # Respuesta de login/registro (incluye JWT)
│   ├── UserResponse.java            # Datos de usuario sin exponer password
│   ├── MedicoRequest.java           # Datos para crear/editar médico
│   ├── MedicoResponse.java          # Datos de médico para el frontend
│   └── ...                          # Más DTOs
├── enums/                           # Enumeraciones
│   ├── Gender.java                  # HOMBRE, MUJER, OTRO
│   └── Role.java                    # PACIENTE, MEDICO, ADMIN
├── exception/                       # Manejo de errores centralizado
│   ├── GlobalExceptionHandler.java  # Traduce excepciones Java → HTTP
│   ├── ResourceNotFoundException.java # Error 404
│   └── BusinessException.java       # Error 400 (lógica de negocio)
├── model/                           # Entidades JPA (= tablas de la BD)
│   ├── User.java                    # Tabla base "users"
│   ├── Paciente.java                # Extiende User (herencia JOINED)
│   ├── Medico.java                  # Extiende User (herencia JOINED)
│   ├── Especialidad.java            # Especialidades médicas
│   ├── Cita.java                    # Citas médicas (con relaciones JPA)
│   ├── Informe.java                 # Informes médicos
│   ├── Hospital.java                # Centros médicos
│   ├── Mensaje.java                 # Mensajes del chat
│   └── PacienteMedico.java          # Tabla intermedia N:M
├── repository/                      # Interfaces de acceso a datos
│   └── ...                          # Spring genera la implementación automáticamente
├── service/                         # Lógica de negocio
│   ├── AuthService.java             # Login, registro, verificación
│   ├── AdminUserService.java        # CRUD admin de usuarios
│   ├── MedicoService.java           # Lógica de médicos
│   ├── EmailService.java            # Envío de emails (Resend API)
│   ├── ChatService.java             # Persistencia de mensajes
│   └── ...
├── util/
│   └── JwtUtil.java                 # Generación y validación de tokens JWT
└── validation/
    └── UserValidation.java          # Validaciones de negocio para usuarios
```

### ¿Qué son los DTOs?

**DTO = Data Transfer Object**. Son clases que definen qué datos viajan entre el frontend y el backend.

```
Frontend envía:  LoginRequest { nif: "12345678A", password: "..." }
                        │
                        ▼
Backend procesa: AuthService.login(request) → genera JWT
                        │
                        ▼
Backend responde: AuthResponse { token: "eyJ...", role: "PACIENTE", ... }
```

**¿Por qué no usar directamente `User`?**
- `User` tiene el campo `password` — nunca queremos enviar eso al frontend
- El DTO controla exactamente qué campos se exponen

---

## 📊 Modelo de Datos

### Herencia JPA — Estrategia JOINED

```
          ┌──────────┐
          │  User    │  ← Tabla "users" con los campos comunes
          │──────────│
          │ id       │
          │ name     │
          │ nif      │
          │ email    │
          │ password │
          │ role     │
          │ ...      │
          └─────┬────┘
                │ hereda (JOINED)
        ┌───────┴───────┐
        ▼               ▼
┌──────────────┐ ┌──────────────┐
│  Paciente    │ │  Medico      │
│──────────────│ │──────────────│
│ grupoSang.   │ │ numColegiado │
│ alergias     │ │ bio          │
│ condiciones  │ │ fotoUrl      │
│ contactoEmg. │ │ activo       │
└──────────────┘ │ especialidad │
                 └──────────────┘
```

**¿Qué significa JOINED?** Hibernate crea una tabla separada para cada clase hija (`pacientes`, `medicos`), con una Foreign Key al `id` de `users`. Así:
- Todos comparten la tabla `users` para los campos comunes
- Cada tipo tiene su propia tabla para campos específicos
- `SELECT` con `JOIN` automático cuando cargas un `Paciente` o `Medico`

### Relaciones principales

| Relación | Tipo | Descripción |
|---|---|---|
| Medico → Especialidad | `@ManyToOne` | Cada médico pertenece a una especialidad |
| Cita → Paciente | `@ManyToOne` | Cada cita la solicita un paciente |
| Cita → Medico | `@ManyToOne` | Cada cita la atiende un médico |
| Informe → Paciente | `@ManyToOne` | Cada informe pertenece a un paciente |
| Informe → Medico | `@ManyToOne` | Cada informe lo emite un médico |
| Paciente ↔ Medico | `@ManyToMany` | Via tabla intermedia `PacienteMedico` |

---

## 🌐 Endpoints de la API

### Autenticación (`/api/auth`)

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| `POST` | `/api/auth/login` | 🌍 Público | Login con NIF + contraseña → JWT |
| `POST` | `/api/auth/register` | 🌍 Público | Registro de nuevo usuario |
| `POST` | `/api/auth/verify` | 🌍 Público | Verificar cuenta con código email |
| `GET` | `/api/auth/validate` | 🔐 Token | Validar JWT (decodifica y verifica firma) |

### Usuarios (`/VitSync-app`)

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| `GET` | `/VitSync-app/{id}` | 🔐 Token | Obtener perfil por ID |
| `DELETE` | `/VitSync-app/{id}` | 🔐 Token | Eliminar cuenta |
| `PUT` | `/VitSync-app/api/users/{id}/profile` | 🔐 Token | Actualizar perfil |
| `PATCH` | `/VitSync-app/api/users/{id}/avatar` | 🔐 Token | Actualizar avatar |
| `PATCH` | `/VitSync-app/api/users/{id}/password` | 🔐 Token | Cambiar contraseña |

### Admin (`/api/admin`)

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| `GET` | `/api/admin/users` | 🔒 ADMIN | Listar todos los usuarios |
| `PUT` | `/api/admin/users/{id}` | 🔒 ADMIN | Editar usuario |
| `DELETE` | `/api/admin/users/{id}` | 🔒 ADMIN | Eliminar usuario |
| `PUT` | `/api/admin/users/{id}/verify` | 🔒 ADMIN | Verificar/suspender usuario |

### Médicos, Especialidades, Citas, Informes, Hospitales

> Consultar los controladores en `src/main/java/.../controller/` para endpoints detallados.

---

## 🔐 Seguridad y Autenticación

### Flujo de autenticación (RS256 + refresh tokens)

```
1. Usuario envía POST /api/auth/login { nif, password }
        │
2. AuthService verifica credenciales (BCrypt) + cuenta verificada
        │
3. Se emiten DOS tokens:
   ├── Access token  → JWT firmado con RS256 (clave privada RSA), expira en 15 min
   └── Refresh token → opaco, 7 días, almacenado HASHEADO en BD (revocable)
        │
4. Frontend guarda:
   ├── Access token  → SOLO en memoria JS (variable, nunca localStorage)
   └── Refresh token → SOLO en cookie httpOnly (ver advertencia abajo)
        │
5. Cada petición incluye: Authorization: Bearer <access token>
        │
6. Cuando el access token caduca (15 min):
   POST /api/auth/refresh { refreshToken } → nuevo par de tokens
   (el refresh token usado se revoca: rotación)
        │
7. Logout:
   ├── POST /api/auth/logout      → revoca ese refresh token
   └── POST /api/auth/logout-all  → revoca TODAS las sesiones del usuario
```

> ⚠️ **OBLIGATORIO para el frontend**: el refresh token DEBE almacenarse en
> una **cookie httpOnly** (`Set-Cookie: ...; HttpOnly; Secure; SameSite=Strict`),
> **NUNCA en localStorage ni sessionStorage**. localStorage es legible por
> cualquier script de la página: un solo XSS robaría una sesión de 7 días con
> acceso a datos sanitarios. El access token puede vivir en memoria JS porque
> caduca en 15 minutos y no es revocable.

### ¿Qué es JWT?

**JSON Web Token** = Un string de 3 partes separadas por puntos:

```
eyJhbGciOiJSUzI1NiJ9.eyJyb2xlIjoiUEFDSUVOVEUiLCJzdWIiOiIxMjM0NTY3OEEiLCJpYXQiOjE3MTU5...
|___ Header (alg=RS256) |___ Payload (role, nif, exp)                                    |___ Firma
```

- **Header**: Algoritmo de firma (**RS256**: RSA asimétrico — la clave privada firma, la pública verifica)
- **Payload**: Datos del usuario (NIF, rol, fecha expiración)
- **Firma**: Garantiza que nadie ha manipulado el token. Con RS256, quien verifica tokens no puede falsificarlos (a diferencia de HS256, donde el mismo secreto firma y verifica)

Las claves se configuran con las variables de entorno `JWT_PRIVATE_KEY` y
`JWT_PUBLIC_KEY` (base64 DER). Generación: `bash scripts/setup-env.sh --generate-keys`.

### BCrypt — Hashing de contraseñas

Las contraseñas **nunca** se guardan en texto plano. Se usa BCrypt:

```java
// Al registrar: convierte "Password123" → "$2a$10$xN9b8K..."
passwordEncoder.encode("Password123")

// Al hacer login: compara sin desencriptar
passwordEncoder.matches("Password123", "$2a$10$xN9b8K...")  // → true
```

---

## 💬 Chat en Tiempo Real

### Tecnologías: STOMP sobre WebSocket via SockJS

```
Frontend (Vue)                    Backend (Spring)
─────────────────                ─────────────────
SockJS Client  ◄──── WebSocket ────►  /ws endpoint
STOMP Client   ◄──── STOMP msgs ───►  @MessageMapping
                                      SimpMessagingTemplate
```

| Concepto | ¿Qué es? |
|---|---|
| **WebSocket** | Conexión bidireccional permanente (a diferencia de HTTP que es request-response) |
| **SockJS** | Fallback: si el navegador no soporta WebSocket, usa HTTP polling |
| **STOMP** | Protocolo de mensajería sobre WebSocket. Organiza los mensajes en "destinos" (como rutas) |

### Flujo de un mensaje

```
1. Usuario A escribe "Hola Doctor" y envía a /app/chat
2. ChatController.processMessage() recibe el mensaje
3. ChatService.save() lo persiste en la BD
4. messagingTemplate.convertAndSendToUser() → /user/{doctorId}/queue/messages
5. El Doctor (suscrito a /user/queue/messages) recibe el mensaje en tiempo real
```

---

## 🧪 Testing

> Documentación detallada en [docs/TESTING.md](docs/TESTING.md)

### Ejecutar tests

```bash
# Todos los tests
./mvnw test

# Solo unitarios (rápidos, sin Spring context)
./mvnw test -Dtest="JwtUtilTest,AuthServiceTest,EntityModelTest"

# Solo integración (levantan H2 + Spring)
./mvnw test -Dtest="GlobalExceptionHandlerTest"
```

---

## 🚀 Despliegue

> Documentación detallada en [docs/CONFIGURACION_ENTORNOS.md](docs/CONFIGURACION_ENTORNOS.md)

| Entorno | Servidor | BD | Arranque |
|---|---|---|---|
| **Local** | `localhost:8080` | Neon Testing | `./mvnw spring-boot:run -Dspring.profiles.active=dev` |
| **Testing** | Render (testing) | Neon Testing | Auto-deploy desde `develop` |
| **Producción** | Render (prod) | Neon Prod | Auto-deploy desde `master` |

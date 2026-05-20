# ⚙️ Configuración de Entornos — VitSync

> Guía completa para configurar y entender los 3 entornos del proyecto: Local, Testing y Producción.

---

## 🌐 Visión General

VitSync opera en **3 entornos** independientes, cada uno con su propia base de datos y configuración:

```
┌─────────────────────────────────────────────────────────────────┐
│  Tu PC (Local)         Testing (Render)      Producción (Render) │
│  ─────────────         ────────────────      ─────────────────── │
│  localhost:8080        api-testing.render     api.render          │
│  localhost:5173        webapp-testing.vercel  vitsync.es          │
│  Neon Testing DB       Neon Testing DB       Neon Prod DB        │
│                                                                   │
│  Rama: feature/*       Rama: develop         Rama: master         │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🏠 Entorno Local

### ¿Cuándo se usa?

Para **desarrollar** en tu máquina. Cada desarrollador tiene su propio entorno local.

### Configuración Backend

**Archivo**: `src/main/resources/application-dev.properties` (no versionado)

```properties
# Base de datos de Testing (compartida entre desarrolladores)
spring.datasource.url=jdbc:postgresql://TU_HOST_NEON/vitsync-testing?sslmode=require
spring.datasource.username=TU_USUARIO
spring.datasource.password=TU_PASSWORD

# Hibernate: auto-actualiza las tablas al arrancar
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

# CORS: permite peticiones desde el frontend local
app.cors.allowed-origins=http://localhost:5173,http://localhost:8080

# JWT: clave para firmar los tokens (mínimo 64 caracteres)
jwt.secret=TU_SECRET_MINIMO_64_CARACTERES
jwt.expiration=86400000

# Email: para enviar correos de verificación
resend.api.key=TU_API_KEY_RESEND
```

**Arrancar**:
```bash
./mvnw spring-boot:run -Dspring.profiles.active=dev
# O en IntelliJ: VM options → -Dspring.profiles.active=dev
```

### Configuración Frontend

**Archivo**: `.env` (no versionado, copiar desde `.env.example`)

```bash
VITE_API_URL=http://localhost:8080
VITE_TALKJS_APP_ID=tu_talkjs_id
```

**Arrancar**:
```bash
npm install   # Solo la primera vez o si cambia package.json
npm run dev   # Arranca en http://localhost:5173
```

---

## 🧪 Entorno Testing

### ¿Cuándo se usa?

Para **probar** cambios antes de que lleguen a producción. Se despliega automáticamente cuando haces push a `develop`.

### URLs

| Servicio | URL |
|---|---|
| Frontend | `https://vitsync-web-app-testing.vercel.app` |
| Backend | `https://vitsync-api-testing.onrender.com` |
| BD | Neon Testing (misma que local) |

### Deploy automático

```bash
# El frontend se despliega automáticamente en Vercel al pushear a develop
git push origin develop

# El backend se despliega automáticamente en Render al pushear a develop
```

### Variables de entorno en Render (Backend)

Configurar en el panel de Render (`Dashboard > Environment`):

| Variable | Valor |
|---|---|
| `DATABASE_URL` | `jdbc:postgresql://...neon.tech/vitsync-testing?sslmode=require` |
| `DATABASE_USERNAME` | Usuario de Neon |
| `DATABASE_PASSWORD` | Contraseña de Neon |
| `JWT_SECRET` | Secret de al menos 64 caracteres |
| `JWT_EXPIRATION` | `86400000` (24h en milisegundos) |
| `RESEND_API_KEY` | API key de Resend |
| `CORS_ALLOWED_ORIGINS` | `https://vitsync-web-app-testing.vercel.app` |

### Variables de entorno en Vercel (Frontend)

Configurar en Vercel (`Settings > Environment Variables`):

| Variable | Valor |
|---|---|
| `VITE_API_URL` | `https://vitsync-api-testing.onrender.com` |
| `VITE_TALKJS_APP_ID` | ID de TalkJS |

---

## 🚀 Entorno Producción

### ¿Cuándo se usa?

Es el entorno **real** que usan los usuarios finales. Solo se despliega desde `master`.

### URLs

| Servicio | URL |
|---|---|
| Frontend | `https://vitsync.es` |
| Backend | `https://vitsync-api.onrender.com` |
| BD | Neon Producción (separada de testing) |

### Diferencias con Testing

| Aspecto | Testing | Producción |
|---|---|---|
| `ddl-auto` | `update` | `validate` (no modifica esquema) |
| `show-sql` | `true` | `false` |
| BD | Neon Testing | Neon Producción (datos reales) |
| CORS | URLs de testing | `https://vitsync.es` |

### ⚠️ Regla de oro

> **NUNCA** hagas cambios directos en producción. Todo debe pasar por: `feature/* → develop → master`

---

## 🔄 Flujo de trabajo Git

```
feature/mi-cambio  →  develop (Testing)  →  master (Producción)
       │                    │                      │
  Desarrollo local    Auto-deploy en          Auto-deploy en
  + tests locales     Render Testing +        Render Prod +
                      Vercel Testing          Vercel Prod
```

### Pasos para un nuevo cambio

```bash
# 1. Crear rama desde develop
git checkout develop
git pull origin develop
git checkout -b feature/mi-nueva-funcionalidad

# 2. Desarrollar y testear localmente
# ... hacer cambios ...
./mvnw test   # Verificar que los tests pasan

# 3. Push y Pull Request a develop
git push origin feature/mi-nueva-funcionalidad
# → Crear PR en GitHub hacia develop

# 4. Tras revisión y aprobación, merge a develop
# → Se despliega automáticamente en Testing

# 5. Verificar en Testing que funciona

# 6. Merge de develop a master (solo cuando esté listo para producción)
```

---

## 📋 Checklist de nuevo desarrollador

- [ ] Clonar ambos repos (`VITSYNC-API` y `VITSYNC-WebApp`)
- [ ] Instalar Java 21 y Node.js 20+
- [ ] Copiar `application-dev.properties.example` → `application-dev.properties`
- [ ] Copiar `.env.example` → `.env`
- [ ] Pedir credenciales al equipo (BD Neon, Resend, JWT Secret)
- [ ] Arrancar backend: `./mvnw spring-boot:run -Dspring.profiles.active=dev`
- [ ] Arrancar frontend: `npm install && npm run dev`
- [ ] Verificar: abrir `http://localhost:5173` y hacer login
- [ ] Ejecutar tests: `./mvnw test`

# 🧪 Guía de Testing — VITSYNC-API

> Documentación completa sobre la estrategia de testing del backend: cómo funcionan los tests, qué herramientas se usan, y cómo crear nuevos tests.

---

## 📋 Índice

- [¿Por qué hacer tests?](#-por-qué-hacer-tests)
- [Herramientas de Testing](#-herramientas-de-testing)
- [Tipos de Tests](#-tipos-de-tests)
- [Estructura de Tests](#-estructura-de-tests)
- [Cómo Ejecutar los Tests](#-cómo-ejecutar-los-tests)
- [Cobertura con JaCoCo](#-cobertura-con-jacoco)
- [Guía: Crear un Nuevo Test](#-guía-crear-un-nuevo-test)
- [Tests Existentes](#-tests-existentes)
- [Buenas Prácticas](#-buenas-prácticas)

---

## 💡 ¿Por qué hacer tests?

Los tests automatizados son código que **verifica que tu código funciona correctamente**. Sin tests:

- ❌ Cada cambio puede romper algo sin que te enteres
- ❌ Hacer refactoring da miedo (¿y si rompo algo?)
- ❌ Los bugs se descubren en producción (cuando los usuarios ya los sufren)

Con tests:
- ✅ Detectas errores **antes** de hacer deploy
- ✅ Puedes cambiar código con confianza
- ✅ Los tests sirven como **documentación** de cómo funciona cada método

---

## 🛠 Herramientas de Testing

### JUnit 5 (Jupiter)

**¿Qué es?** El framework de testing estándar de Java. Proporciona las anotaciones y assertions para escribir tests.

```java
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MiTest {
    @Test                          // ← Marca este método como un test
    void sumaFunciona() {
        assertEquals(4, 2 + 2);    // ← Si no es 4, el test falla
    }
}
```

**Anotaciones principales:**

| Anotación | ¿Qué hace? |
|---|---|
| `@Test` | Marca un método como test |
| `@BeforeEach` | Se ejecuta **antes de cada** test (para preparar datos) |
| `@DisplayName("...")` | Nombre legible del test (aparece en los resultados) |
| `@Nested` | Agrupa tests relacionados en una clase interna |

**Assertions principales:**

| Assertion | ¿Qué verifica? |
|---|---|
| `assertEquals(esperado, actual)` | Que dos valores sean iguales |
| `assertTrue(condición)` | Que una condición sea verdadera |
| `assertFalse(condición)` | Que una condición sea falsa |
| `assertNotNull(objeto)` | Que un objeto no sea null |
| `assertThrows(Excepcion.class, () -> ...)` | Que se lance una excepción específica |
| `assertDoesNotThrow(() -> ...)` | Que NO se lance ninguna excepción |

---

### Mockito

**¿Qué es?** Librería para crear **objetos falsos** (mocks) de las dependencias de una clase. Así puedes testear una clase sin necesitar la BD, el servidor de email, etc.

**Problema sin mocks:**
```
Para testear AuthService.login() necesitarías:
  → Una BD PostgreSQL con datos de prueba
  → Un servidor de email funcionando
  → JwtUtil configurado con un secret real
  → Todo arrancado y conectado
```

**Solución con mocks:**
```java
@Mock UserRepository userRepository;       // ← Falso: simula respuestas de la BD
@Mock PasswordEncoder passwordEncoder;     // ← Falso: simula la verificación de contraseña
@Mock JwtUtil jwtUtil;                     // ← Falso: devuelve un token inventado
@Mock EmailService emailService;           // ← Falso: no envía emails de verdad

@InjectMocks AuthService authService;      // ← Real: usa los mocks como dependencias
```

**Cómo configurar respuestas:**

```java
// "Cuando alguien llame a findByNif con '12345678A', devuelve este usuario"
when(userRepository.findByNif("12345678A")).thenReturn(Optional.of(testUser));

// "Cuando alguien llame a matches con estas contraseñas, devuelve true"
when(passwordEncoder.matches("Password123", hash)).thenReturn(true);
```

**Cómo verificar que algo se llamó:**

```java
// Verificar que se guardó el usuario en la BD
verify(userRepository).save(any(User.class));

// Verificar que NUNCA se envió un email (porque hubo error antes)
verify(emailService, never()).sendVerificationEmail(anyString(), anyString());
```

---

### MockMvc (Tests de integración)

**¿Qué es?** Simula peticiones HTTP sin levantar un servidor real. Útil para testear que los endpoints responden correctamente.

```java
@SpringBootTest                  // ← Levanta el contexto de Spring completo
@AutoConfigureMockMvc            // ← Configura MockMvc automáticamente
class MiControllerTest {

    @Autowired MockMvc mockMvc;  // ← Cliente HTTP simulado

    @Test
    void login_conBodyVacio_devuelve400() throws Exception {
        mockMvc.perform(post("/api/auth/login")                 // Simula POST
                .contentType(MediaType.APPLICATION_JSON)         // Content-Type: JSON
                .content("{}"))                                  // Body vacío
            .andExpect(status().isBadRequest())                  // Espera HTTP 400
            .andExpect(jsonPath("$.fieldErrors").exists());       // Espera errores de campo
    }
}
```

### H2 — BD en memoria para tests

Los tests de integración no usan PostgreSQL. Usan **H2**, una BD que:
- Vive **solo en la RAM** (no necesitas instalar nada)
- Se **crea y destruye** con cada test (`ddl-auto=create-drop`)
- Es compatible con la mayoría de queries PostgreSQL

Configurado en `src/test/resources/application.properties`:
```properties
spring.datasource.url=jdbc:h2:mem:testdb
spring.jpa.hibernate.ddl-auto=create-drop
```

---

## 📂 Tipos de Tests

### Test Unitario (rápido, sin Spring)

**¿Cuándo usarlo?** Para testear lógica pura de una clase individual.

| Característica | Valor |
|---|---|
| **Velocidad** | ⚡ Milisegundos |
| **Necesita Spring** | ❌ No |
| **Necesita BD** | ❌ No |
| **Usa mocks** | ✅ Sí (Mockito) |
| **Ejemplo** | `AuthServiceTest`, `JwtUtilTest`, `EntityModelTest` |

### Test de Integración (más lento, con Spring + H2)

**¿Cuándo usarlo?** Para testear que los componentes funcionan **juntos** (controller + service + repository).

| Característica | Valor |
|---|---|
| **Velocidad** | 🐢 Segundos |
| **Necesita Spring** | ✅ Sí (`@SpringBootTest`) |
| **Necesita BD** | ✅ H2 en memoria |
| **Usa mocks** | ❌ No (usa componentes reales) |
| **Ejemplo** | `GlobalExceptionHandlerTest` |

---

## 📁 Estructura de Tests

```
src/test/
├── java/com/ejemplo/vitsync/
│   ├── ApplicationTests.java                  ← Verifica que Spring arranca sin errores
│   ├── audit/
│   │   └── AuditAspectTest.java               ← Audit log AOP (success/failure)
│   ├── converter/
│   │   └── SensitiveDataConverterTest.java    ← Cifrado AES-256-GCM round-trip
│   ├── exception/
│   │   └── GlobalExceptionHandlerTest.java    ← Respuestas HTTP de error
│   ├── integration/
│   │   ├── AuthControllerIntegrationTest.java ← Login/registro/rate limit (HTTP real)
│   │   └── SecurityGdprIntegrationTest.java   ← Refresh, logout, validate, RBAC, IDOR, GDPR
│   ├── model/
│   │   └── EntityModelTest.java               ← Entidades, herencia, relaciones
│   ├── service/
│   │   ├── AuthServiceTest.java               ← Login, registro, verificación
│   │   ├── AdminMedicoEspecialidadServiceTest.java
│   │   ├── GdprServiceTest.java               ← Acceso, exportación, anonimización
│   │   ├── PacienteMedicoServiceTest.java
│   │   ├── RefreshTokenServiceTest.java       ← Emisión, rotación, revocación
│   │   └── SimpleServicesTest.java            ← User/Cita/Informe/Chat
│   ├── util/
│   │   ├── HtmlSanitizerTest.java             ← Anti-XSS
│   │   └── JwtUtilTest.java                   ← Generación y validación JWT RS256
│   └── validation/
│       └── NifValidatorTest.java              ← Dígito de control NIF/NIE
└── resources/
    └── application.properties                 ← Configuración para tests (H2, claves de test)
```

---

## 🏃 Cómo Ejecutar los Tests

```bash
# ═══════════════════════════════════════════════════
# EJECUTAR TODOS LOS TESTS
# ═══════════════════════════════════════════════════
./mvnw test

# ═══════════════════════════════════════════════════
# EJECUTAR UNA CLASE ESPECÍFICA
# ═══════════════════════════════════════════════════
./mvnw test -Dtest="JwtUtilTest"
./mvnw test -Dtest="AuthServiceTest"

# ═══════════════════════════════════════════════════
# EJECUTAR VARIAS CLASES
# ═══════════════════════════════════════════════════
./mvnw test -Dtest="JwtUtilTest,AuthServiceTest,EntityModelTest"

# ═══════════════════════════════════════════════════
# EJECUTAR UN MÉTODO ESPECÍFICO
# ═══════════════════════════════════════════════════
./mvnw test -Dtest="AuthServiceTest#login_withValidCredentials_returnsAuthResponse"

# ═══════════════════════════════════════════════════
# EJECUTAR CON LOGS DETALLADOS
# ═══════════════════════════════════════════════════
./mvnw test -Dtest="JwtUtilTest" -X
```

### Leer los resultados

```
[INFO] Tests run: 28, Failures: 0, Errors: 0, Skipped: 0
                  ^^             ^            ^            ^
                  │              │            │            └─ Tests ignorados
                  │              │            └─ Errores inesperados (excepciones)
                  │              └─ Assertions que fallaron
                  └─ Total de tests ejecutados
```

---

## 📈 Cobertura con JaCoCo

La cobertura mide **qué porcentaje del código ejecutan los tests**. Está configurada con el plugin JaCoCo en `pom.xml`.

### Generar y ver el reporte

```bash
# Ejecuta todos los tests + genera el reporte + comprueba el umbral
./mvnw verify

# El reporte HTML queda en:
target/site/jacoco/index.html
```

Abre ese archivo en el navegador: muestra la cobertura por paquete → clase → método → línea (verde = cubierto, rojo = sin cubrir).

### Umbral obligatorio (quality gate)

El build **falla** si la cobertura de líneas baja del **80%** en estos paquetes:

| Paquete | Por qué se exige |
|---|---|
| `com.ejemplo.vitsync.service` | Toda la lógica de negocio (auth, GDPR, tokens) |
| `com.ejemplo.vitsync.util` | Criptografía y sanitización (JWT, anti-XSS) |

**Exclusión documentada:** `EmailService` queda fuera del cómputo — son plantillas HTML + llamadas HTTP al API externo de Resend, sin lógica unit-testeable significativa.

Si `./mvnw verify` falla con `Coverage checks have not been met`, abre el reporte HTML, localiza las líneas rojas del paquete afectado y añade tests que las ejerciten.

---

## 📝 Guía: Crear un Nuevo Test

### Ejemplo: Test unitario para `MedicoService.create()`

**Paso 1**: Crear archivo en la carpeta correcta:

```
src/test/java/com/ejemplo/vitsync/service/MedicoServiceTest.java
```

**Paso 2**: Estructura básica:

```java
package com.ejemplo.vitsync.service;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)           // Activa Mockito
@DisplayName("MedicoService — Gestión de Médicos")
class MedicoServiceTest {

    @Mock MedicoRepository medicoRepository;   // Mock de la BD
    @Mock EspecialidadRepository espRepo;       // Mock de especialidades
    @Mock PasswordEncoder passwordEncoder;     // Mock del encoder

    @InjectMocks MedicoService medicoService;  // Clase bajo test

    @Test
    @DisplayName("Crear médico con datos válidos")
    void create_withValidData_savesMedico() {
        // ARRANGE (preparar)
        MedicoRequest request = new MedicoRequest();
        request.setName("Dr. Test");
        request.setNumeroColegiado("COL-99999");
        request.setPassword("Password123");

        when(medicoRepository.existsByNumeroColegiado("COL-99999")).thenReturn(false);
        when(passwordEncoder.encode("Password123")).thenReturn("$2a$10$hash");

        // ACT (ejecutar)
        medicoService.create(request);

        // ASSERT (verificar)
        verify(medicoRepository).save(any(Medico.class));
    }
}
```

### Patrón AAA (Arrange-Act-Assert)

Todo test sigue este patrón:

1. **Arrange** (preparar): Crear datos de prueba y configurar mocks
2. **Act** (ejecutar): Llamar al método que estás testeando
3. **Assert** (verificar): Comprobar que el resultado es el esperado

---

## 📊 Tests Existentes

### JwtUtilTest (10 tests)

| Test | Qué verifica |
|---|---|
| `generateToken_returnsNonEmptyString` | Token tiene formato JWT (3 partes) |
| `generateToken_containsNifAsSubject` | NIF se guarda como subject |
| `generateToken_containsRole` | Rol se guarda en los claims |
| `extractExpiration_returnsValidDate` | Expiración es en el futuro |
| `extractRole_distinguishesRoles` | Diferencia PACIENTE/MEDICO/ADMIN |
| `validateToken_withCorrectNif_returnsTrue` | NIF correcto → válido |
| `validateToken_withWrongNif_returnsFalse` | NIF incorrecto → inválido |
| `validateToken_withExpiredToken_throwsException` | Token expirado → excepción |
| `validateToken_withMalformedToken_throwsException` | Token roto → excepción |
| `validateToken_withTamperedToken_throwsException` | Token manipulado → excepción |

### AuthServiceTest (10 tests)

| Grupo | Test | Qué verifica |
|---|---|---|
| **Login** | `withValidCredentials` | Login correcto devuelve JWT |
| **Login** | `withUnknownNif` | NIF inexistente → error |
| **Login** | `withWrongPassword` | Contraseña incorrecta → error |
| **Login** | `withUnverifiedAccount` | Cuenta sin verificar → error |
| **Registro** | `withValidData` | Registro guarda usuario y envía email |
| **Registro** | `withDuplicateNif` | NIF duplicado → error |
| **Registro** | `withDuplicateEmail` | Email duplicado → error |
| **Verificación** | `withCorrectCode` | Código correcto → verificado |
| **Verificación** | `withWrongCode` | Código incorrecto → error |
| **Verificación** | `withUnknownEmail` | Email inexistente → error |

### EntityModelTest (8 tests)

| Test | Qué verifica |
|---|---|
| `user_allFieldsAreSetAndRetrieved` | Todos los campos de User funcionan |
| `user_birthDateIsLocalDate` | birthDate es LocalDate (no String) |
| `paciente_inheritsUserFields` | Paciente hereda campos de User |
| `medico_inheritsUserAndHasOwnFields` | Medico tiene campos propios |
| `cita_hasJpaRelationships` | Cita tiene @ManyToOne a Paciente/Medico |
| `informe_hasJpaRelationships` | Informe tiene @ManyToOne correctos |
| `hospital_usesSpanishFieldNames` | Hospital usa campos en español |
| `especialidad_slugAndCodeWork` | Slug y código de Especialidad funcionan |

---

## ✅ Buenas Prácticas

1. **Un test = una cosa**: Cada test verifica UNA sola funcionalidad
2. **Nombres descriptivos**: `login_withWrongPassword_throwsException` > `testLogin2`
3. **Independencia**: Los tests no deben depender del orden de ejecución
4. **No testees frameworks**: No testees que Spring funciona, testea TU código
5. **Tests rápidos**: Usa tests unitarios siempre que puedas (milisegundos vs segundos)
6. **`@DisplayName`**: Siempre añade un nombre legible en español

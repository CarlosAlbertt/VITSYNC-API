# CIFRADO — VITSYNC-API

## Qué se cifra (datos de categoría especial, Art. 9 RGPD)

| Entidad | Campos cifrados en reposo |
|---|---|
| `Paciente` | alergias, condicionesPrevias, grupoSanguineo, contactoEmergencia, historialClinicoId |
| `Informe` | notasPersonales |
| `Mensaje` | content (chat médico-paciente) |

Las contraseñas NO se cifran: se hashean con BCrypt (no reversible). Los
refresh tokens se almacenan como hash SHA-256.

## Algoritmo

- **AES-256-GCM** (`AES/GCM/NoPadding`), implementado con `javax.crypto`
  (sin librerías externas).
- **GCM** = cifrado autenticado: además de confidencialidad, integra un tag
  de 128 bits. Si el ciphertext se altera en BD, el descifrado **falla** en
  vez de devolver datos corruptos. Se eligió frente a CBC (vulnerable a
  padding oracle y sin integridad).
- **IV** aleatorio de 12 bytes (96 bits) por cada valor, generado con
  `SecureRandom`. Nunca se reutiliza un IV con la misma clave (requisito de
  GCM). Consecuencia: cifrar el mismo texto dos veces produce ciphertexts
  distintos.

## Formato almacenado

```
base64( IV(12 bytes) || ciphertext || tag(16 bytes) )
```

El IV no es secreto y se antepone para poder descifrar. Implementado en
`SensitiveDataConverter` (`@Converter` JPA con `@Convert` en cada campo).

## Gestión de claves

- Clave AES de 32 bytes (256 bits) en base64, vía variable de entorno
  `ENCRYPTION_KEY`. Generación: `openssl rand -base64 32`.
- Validada al arranque por `EncryptionConfig` (falla rápido si no son 32
  bytes). Publicada al converter por `SensitiveDataKeyHolder` (holder estático
  porque Hibernate, no Spring, instancia los converters).
- **Rotación de clave**: cambiar `ENCRYPTION_KEY` exige re-cifrar todos los
  datos existentes (descifrar con la antigua, cifrar con la nueva). No hay
  soporte de doble clave automático; planificar ventana de mantenimiento.

## Impacto en consultas

Las columnas cifradas contienen base64 opaco: **no se pueden indexar** ni usar
en `WHERE`/`LIKE`/`ORDER BY` de SQL. Cualquier búsqueda sobre estos campos
debe cargar y descifrar en la aplicación. Por eso solo se cifran campos que no
se filtran a nivel de BD (texto clínico libre), no identificadores de búsqueda
como NIF o email.

## JWT (en tránsito / firma)

- Firma **RS256** (RSA 2048+ / SHA-256). Claves vía `JWT_PRIVATE_KEY`
  (PKCS#8) y `JWT_PUBLIC_KEY` (X.509), base64 DER. Ver `JwtUtil`.

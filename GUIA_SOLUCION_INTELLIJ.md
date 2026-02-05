# Guía de Solución: Error "TypeTag :: UNKNOWN" en IntelliJ

Este error NO es de tu código. Ocurre porque IntelliJ está usando un compilador interno desactualizado o una configuración de Lombok que choca con Java 21.

Sigue estos 4 pasos exactos para arreglarlo definitivamente.

---

## PASO 1: Activar el Procesado de Anotaciones
Lombok necesita esto para generar el código (getters, setters) "al vuelo".

1.  Ve a `Settings` (o `Preferences`) > `Build, Execution, Deployment` > `Compiler` > `Annotation Processors`.
2.  Marca la casilla **Enable annotation processing**.
3.  Asegúrate de que "Obtain processors from project classpath" esté seleccionado.

## PASO 2: Recargar Maven (CRUCIAL)
IntelliJ necesita leer el cambio que hicimos en `pom.xml` donde forzamos la versión compatible de Lombok.

1.  Abre la pestaña **Maven** (usualmente a la derecha de la ventana).descuento al tramitar
2.  Haz clic en el icono de **"Reload All Maven Projects"** (dos flechas azules formando un círculo).
3.  Espera a que termine la barra de progreso inferior de "Resolving dependencies".

## PASO 3: Invalidar Cachés
A veces IntelliJ se queda con "basura" de la configuración anterior.

1.  Ve al menú `File` > `Invalidate Caches...`.
2.  Marca las casillas (especialmente "Clear file system cache and Local History").
3.  Haz clic en **"Invalidate and Restart"**.
4.  IntelliJ se cerrará y volverá a abrir. Espera a que re-indexe todo.

## PASO 4: Delegar la compilación a Maven (Solución definitiva)
Si lo anterior falla, dile a IntelliJ que use Maven para compilar (que sabemos que funciona) en lugar de su compilador interno.

1.  Ve a `Settings` > `Build, Execution, Deployment` > `Build Tools` > `Maven` > `Runner`.
2.  Marca la casilla **Delegate IDE build/run actions to Maven**.
3.  Dale a `Apply` y `OK`.


---

## PASO 5: Configurar Base de Datos (Error JDBC)
Si al arrancar te sale un error rojo gigante que dice:
`Driver org.postgresql.Driver claims to not accept jdbcUrl, ${DATABASE_URL}`

Es porque te falta configurar la base de datos. ¡Ya tienes un archivo preparado para esto!

1.  En IntelliJ, arriba a la derecha, verás "VitSyncApplication" (o el nombre de tu ejecución). Haz clic y dale a **Edit Configurations...**
2.  Busca el campo **"Active profiles"** (si no sale, pulsa "Modify options").
3.  Escribe: `dev`
4.  Dale a **OK** y vuelve a dar al **Play**.

Esto cargará el archivo `application-dev.properties` que ya tiene la conexión a NeonDB configurada.

---

### ¿Cómo sé si ya funciona?
Después de reiniciar, abre `PacienteMedicoController.java`.
*   Si los `import` ya no están en rojo.
*   Si puedes hacer clic derecho en `getMedicosDePaciente` y no da error.
*   **Si al dar al Play la aplicación arranca y dice `Started VitSyncApplication in...`**

¡Inténtalo en este orden y debería desaparecer el problema!

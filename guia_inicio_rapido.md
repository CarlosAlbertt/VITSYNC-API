# 🚀 Guía "Para Tontos": Cómo Iniciar VitSync en Local

Sigue estos pasos EXACTOS para no tener problemas.

## 1. Backend (API) - Desde IntelliJ IDEA

El objetivo es arrancar el servidor Java (`localhost:8080`) conectado a la base de datos de pruebas.

1.  Abre el proyecto `VITSYNC-API` en IntelliJ.
2.  En la barra superior derecha, al lado del botón verde de "Play" ▶️, verás un desplegable (puede poner "VitSyncApplication" o "Add Configuration..."). Haz clic y selecciona **"Edit Configurations..."**.
3.  Si no tienes ninguna configuración creada, pulsa el **+** (arriba a la izquierda) -> **Application**.
4.  Rellena estos campos (los demás déjalos igual):
    *   **Name**: `VitSync API (DEV)`
    *   **Main class**: Busca o escribe `com.ejemplo.vitsync.VitSyncApplication`
    *   **VM options** (¡IMPORTANTE!): Haz clic en "Modify options" (texto azul a la derecha) -> "Add VM options". En la casilla que aparece, pega esto:
        ```
        -Dspring.profiles.active=dev
        ```
        *(Esto le dice a IntelliJ que use tu archivo `application-dev.properties` con las claves)*.
5.  Dale a **OK**.
6.  ¡Listo! Ahora dale al botón **Play Verde ▶️** arriba a la derecha.

> **¿Cómo sé si funciona?**
> En la consola de abajo verás muchas letras. Si al final pone algo como:
> `Started VitSyncApplication in 5.432 seconds (JVM running for 6.123)`
> ... ¡está vivo!

---

## 2. Frontend (Web) - Desde Terminal (Antigravity / VS Code)

El objetivo es arrancar la web (`localhost:5173`) para verla en tu navegador.

1.  Abre una terminal (en VS Code: `Terminal` -> `New Terminal`, o usa la de Antigravity si prefieres).
2.  Asegúrate de estar en la carpeta del frontend. Escribe:
    ```bash
    cd VITSYNC-WebApp
    ```
3.  (Solo la primera vez) Instala las dependencias:
    ```bash
    npm install
    ```
4.  Arranca el servidor de desarrollo:
    ```bash
    npm run dev
    ```
5.  Verás algo como:
    ```
      VITE v5.2.11  ready in 345 ms

      ➜  Local:   http://localhost:5173/
      ➜  Network: use --host to expose
    ```
6.  Haz **Ctrl + Clic** en el enlace `http://localhost:5173/` (o cópialo y pégalo en Chrome).

---

## ¡Y ya está! 🎉
Ahora tienes:
*   Tu API corriendo en el puerto 8080.
*   Tu Web corriendo en el puerto 5173 (y conectándose al 8080).
*   Ambos conectados a la base de datos Neon de pruebas (en la nube).

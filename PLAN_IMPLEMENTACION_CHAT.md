# Plan de Implementación: Chat Médico <-> Paciente

Este documento detalla la arquitectura, el flujo de datos y los pasos necesarios para implementar un chat en tiempo real entre médicos y pacientes en VitSync.

## 1. Diagrama de Flujo (Arquitectura)

A continuación se muestra el flujo de interacción entre el Paciente, el Servidor y el Médico.

**(Diagrama de Texto)**

```text
[ PACIENTE ]                     [ SERVIDOR ]                      [ MÉDICO ]
(Frontend)                  (Spring Boot / WS)                    (Frontend)
     |                               |                                 |
     | --- 1. Conexión WS ---------> |                                 |
     | <--- Conectado (STOMP) ------ |                                 |
     |                               |                                 |
     | --- 2. Suscribir queue -----> |                                 |
     |     (/user/queue/messages)    |                                 |
     |                               | <--- 1. Conexión WS ----------- |
     |                               | ------ Conectado (STOMP) -----> |
     |                               |                                 |
     |                               | <--- 2. Suscribir queue ------- |
     |                               |     (/user/queue/messages)      |
     |                               |                                 |
     | === FASE DE MENSAJERÍA ======================================== |
     |                               |                                 |
     | --- 3. Enviar Mensaje ------> |                                 |
     |     (Dest: /app/chat)         |                                 |
     |     (Body: "Hola Dr.")        |                                 |
     |                               | --- [Guardar en BD]             |
     |                               |                                 |
     |                               | --- 4. Push Mensaje ----------> |
     |                               |     Dest: /user/{idM}/queue...  |
     |                               |     Body: "Hola Dr."            |
     |                               |                                 |
     |                               | <--- 5. Responder Mensaje ----- |
     |                               |      (Dest: /app/chat)          |
     |                               |      (Body: "Hola, dígame")     |
     |                               | --- [Guardar en BD]             |
     |                               |                                 |
     | <--- 6. Push Mensaje -------- |                                 |
     |      Dest: /user/{idP}/queue..|                                 |
     |      Body: "Hola, dígame"     |                                 |
     |                               |                                 |
```

## 2. Tecnologías Requeridas

*   **Backend**: Spring Boot Starter WebSocket (`spring-boot-starter-websocket`).
*   **Protocolo**: STOMP (Simple Text Oriented Messaging Protocol) sobre WebSocket.
*   **Frontend**: Librería cliente STOMP (ej. `@stomp/stompjs`).
*   **Base de Datos**: Tabla `mensajes` para historial.

## 3. Pasos de Implementación (Guía Paso a Paso)

### A. Backend (VITSYNC-API)

#### 1. Agregar Dependencias
En tu `pom.xml`, añade la dependencia de WebSocket:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>
```

#### 2. Configuración de WebSocket (`WebSocketConfig.java`)
Crear una clase para habilitar el broker.
*   Anotar con `@Configuration` y `@EnableWebSocketMessageBroker`.
*   Implementar `WebSocketMessageBrokerConfigurer`.
*   **Endpoints**:
    *   `/ws`: Endpoint de conexión.
    *   `/app`: Prefijo destino aplicación.
    *   `/user`: Prefijo destino usuario específico.

#### 3. Modelo de Datos (`ChatMessage.java`)
Entidad JPA:
*   `id` (Long)
*   `senderId` (Long)
*   `recipientId` (Long)
*   `content` (String)
*   `timestamp` (LocalDateTime)

#### 4. Controlador (`ChatController.java`)
*   Recibir mensajes en `@MessageMapping("/chat")`.
*   Guardar mensaje usando un servicio (`ChatService`).
*   Enviar al destinatario:
    ```java
    messagingTemplate.convertAndSendToUser(
        destinatarioId, 
        "/queue/messages", 
        mensaje
    );
    ```

---

### B. Frontend (VITSYNC-WebApp)

#### 1. Instalar Dependencias
```bash
npm install @stomp/stompjs
```

#### 2. Lógica de Conexión
*   Crear cliente STOMP apuntando a `http://localhost:8080/ws`.
*   Al conectar, suscribirse a `/user/queue/messages` para recibir lo que te envían.
*   Para enviar, publicar en `/app/chat` con un JSON `{ to: idDestino, content: "texto" }`.

---
**Nota**: Si sigues viendo problemas con el formato, este archivo es texto plano estándar (Markdown) y debería ser legible en cualquier editor de código.

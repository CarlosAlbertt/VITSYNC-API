# Documentación del Flujo de Chat (WebSocket)

Este documento detalla el flujo de comunicación en tiempo real entre Paciente y Médico utilizando WebSockets.

## 1. Diagrama de Secuencia (Técnico)
Este diagrama representa el paso a paso exacto de los mensajes a través de los controladores y servicios de Spring Boot.

```mermaid
sequenceDiagram
    participant Paciente as 👤 Cliente 1 (Emisor)
    participant Server as ⚙️ API (Spring Boot)
    participant DB as 🗄️ Base de Datos
    participant Medico as 🩺 Cliente 2 (Receptor)

    Note over Paciente, Medico: Fase 1: Conexión Inicial
    Paciente->>Server: Conexión WebSocket (/ws)
    Medico->>Server: Conexión WebSocket (/ws)
    Paciente->>Server: Suscripción a su canal (/user/queue/messages)
    Medico->>Server: Suscripción a su canal (/user/queue/messages)

    Note over Paciente, Medico: Fase 2: Envío de Mensaje
    Paciente->>Server: SEND /app/chat (Payload: ChatMessage)
    
    rect rgb(240, 248, 255)
    Note right of Server: ChatController.processMessage()
    Server->>DB: repository.save(mensaje)
    DB-->>Server: Mensaje Guardado (OK)
    end
    
    Note over Server, Medico: Fase 3: Entrega Push
    Server->>Medico: SEND /user/{id}/queue/messages
    Medico-->>Medico: Actualizar UI (Mostrar mensaje)
```

## 2. Diagrama de Arquitectura (Visual)
Este diagrama es más conceptual, ideal para presentaciones o visión general.

```mermaid
graph LR
    P[👤 Paciente] -- "1. Envía (/app/chat)" --> S(☁️ Servidor WebSocket)
    S -- "2. Persiste" --> D[(🗄️ Base de Datos)]
    S -- "3. Notifica (/user/queue)" --> M[🩺 Médico]
    
    style P fill:#e1f5fe,stroke:#01579b,stroke-width:2px
    style M fill:#e8f5e9,stroke:#1b5e20,stroke-width:2px
    style S fill:#fff3e0,stroke:#e65100,stroke-width:2px
    style D fill:#f3e5f5,stroke:#4a148c,stroke-width:2px
```

## Referencia de Código

*   **Configuración**: `WebSocketConfig.java` define los endpoints `/ws` y el broker `/user`.
*   **Controlador**: `ChatController.java` maneja el mensaje entrante en `@MessageMapping("/chat")` y lo reenvía con `simpMessagingTemplate.convertAndSendToUser()`.
*   **Modelo de Datos**: `ChatMessage` contiene el `senderId`, `recipientId` y `content`.

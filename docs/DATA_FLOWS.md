# FLUJOS DE DATOS SENSIBLES — VITSYNC-API

Diagramas ASCII de cómo circulan los datos de categoría especial.

## 1. Autenticación

```
Frontend                 AuthController        AuthService            BD
   │  POST /login            │                     │                  │
   │───{nif,password}───────▶│ @Valid              │                  │
   │                         │──login(req)────────▶│ findByNif        │
   │                         │                     │─────────────────▶│
   │                         │                     │ BCrypt.matches   │
   │                         │                     │ JWT RS256 (15m)  │
   │                         │                     │ refresh (hash)──▶│ refresh_tokens
   │                         │                     │ audit LOGIN_*───▶│ audit_logs
   │◀──{token, refreshToken}─│◀────────────────────│                  │
   │  token→memoria JS                                                 │
   │  refreshToken→cookie httpOnly                                     │
```

## 2. Lectura de datos clínicos (informe)

```
Frontend          JwtFilter      InformeController   InformeService(@Auditable)   BD
   │ GET /informes/me  │              │                    │                      │
   │──Bearer token────▶│ valida RS256 │                    │                      │
   │                   │ set principal│                    │                      │
   │                   │─────────────▶│ getInformesByNif   │                      │
   │                   │              │───────────────────▶│ findByPaciente_Nif   │
   │                   │              │                    │─────────────────────▶│
   │                   │              │                    │ converter DESCIFRA   │ (AES-256-GCM)
   │                   │              │                    │ audit VIEW_REPORT───▶│ audit_logs
   │◀──informes────────┼──────────────┼────────────────────│                      │
```

## 3. Escritura cifrada (mensaje de chat)

```
Frontend(STOMP)   WsAuthInterceptor   ChatController        ChatService     BD
   │ CONNECT+Bearer    │                  │                    │            │
   │──────────────────▶│ valida token     │                    │            │
   │                   │ set user(NIF)    │                    │            │
   │ SEND /app/chat    │                  │                    │            │
   │──{content}───────────────────────────▶│ senderId=principal│            │
   │                   │                  │ sanitize HTML       │            │
   │                   │                  │───────save────────▶│ converter  │
   │                   │                  │                    │ CIFRA──────▶│ mensajes (ciphertext)
   │                   │                  │ convertAndSendToUser│            │
   │◀══push notification (al destinatario)═╪════════════════════│            │
```

## 4. Exportación RGPD (Art. 20)

```
Frontend         GdprController(IDOR+rate)   GdprService                 BD
   │ GET /my-data/export │                       │                       │
   │────────────────────▶│ requireSelfOrAdmin    │                       │
   │                     │ rate limit 1/24h      │                       │
   │                     │──────────────────────▶│ collectUserData       │
   │                     │                       │ (perfil+citas+        │
   │                     │                       │  informes+mensajes)──▶│ (descifra)
   │                     │                       │ ZIP(json,txt)         │
   │                     │                       │ audit EXPORT_DATA────▶│ audit_logs
   │◀──application/zip────│◀──────────────────────│                       │
```

## Encargados del tratamiento (datos salen del sistema)

```
VitSync API ──TLS──▶ Neon (PostgreSQL, datos cifrados en reposo)
            ──TLS──▶ Resend (emails: NO contienen datos clínicos)
            ──host─▶ Render (cómputo + disco efímero)
```

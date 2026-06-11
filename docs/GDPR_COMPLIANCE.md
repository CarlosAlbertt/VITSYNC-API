# CUMPLIMIENTO RGPD — VITSYNC-API

Mapeo de cada artículo relevante con su implementación técnica. Operativa de
derechos en `docs/GDPR_PROCEDURES.md`.

## Mapeo Artículo → Implementación

| Artículo | Requisito | Implementación |
|---|---|---|
| **Art. 5.1.c** Minimización | Exponer solo lo necesario | password WRITE_ONLY, verificationCode @JsonIgnore, DTOs de respuesta, listados restringidos por rol |
| **Art. 5.1.f** Integridad/confidencialidad | Protección frente a acceso no autorizado | Cifrado AES-256-GCM, IDOR checks, chat autenticado, uploads autenticados |
| **Art. 9** Categorías especiales | Garantías reforzadas | Cifrado en reposo de campos clínicos + control de acceso + trazabilidad |
| **Art. 15** Acceso | Copia de los datos | `GET /api/users/{id}/my-data` |
| **Art. 17** Supresión | Borrado / olvido | `DELETE /api/users/{id}/gdpr-delete` → anonimización (conservando lo exigido por ley) |
| **Art. 20** Portabilidad | Formato estructurado | `GET /api/users/{id}/my-data/export` (ZIP con JSON) |
| **Art. 25** Privacidad desde el diseño | Mínimo por defecto | Stateless, sin endpoints mock activos (501), validación estricta |
| **Art. 30** Registro de actividades | Registro de tratamiento | `audit_logs` + `@Auditable`/`AuditAspect` |
| **Art. 32** Seguridad del tratamiento | Cifrado, resiliencia | AES-256-GCM, RS256, rate limiting, BCrypt, cabeceras, refresh revocable |
| **Art. 33/34** Brechas | Notificación 72h | Procedimiento en GDPR_PROCEDURES §5; incidente abierto (credenciales en git) por resolver |
| **Art. 35** DPIA | Evaluación de impacto | **Pendiente**: obligatoria para tratamiento a gran escala de datos de salud |

## LOPDGDD (LO 3/2018)

- **Art. 9 / DA 17ª**: tratamiento de salud con trazabilidad de accesos →
  `audit_logs` registra VIEW_PATIENT_DATA / VIEW_MEDICAL_REPORT / VIEW_CHAT.
- **Art. 28**: medidas según riesgo → ver `docs/SECURITY.md`.

## Ley 41/2002 (Autonomía del Paciente)

- **Art. 7** Confidencialidad: acceso a informes/citas restringido al titular
  (y a ADMIN/MEDICO con justificación).
- **Art. 16.7** Trazabilidad de acceso a historia clínica: audit log.
- **Art. 17** Conservación: el "borrado" es anonimización; la documentación
  clínica se conserva el periodo legal.

## Registro de Actividades de Tratamiento (Art. 30) — ficha resumen

- **Responsable**: VitSync (completar identificación y DPO).
- **Categorías de interesados**: pacientes, profesionales sanitarios,
  administradores.
- **Categorías de datos**: identificativos, contacto, **datos de salud
  (categoría especial)**.
- **Finalidad**: gestión de citas, informes y comunicación clínica.
- **Base jurídica**: ejecución de contrato / consentimiento explícito (Art.
  9.2.a) / fines de medicina (Art. 9.2.h) — **confirmar y documentar**.
- **Destinatarios**: Neon (BD, encargado), Render (hosting, encargado), Resend
  (email, encargado) — **firmar contratos de encargo (Art. 28)**.
- **Transferencias internacionales**: verificar ubicación de Neon/Render/Resend
  y garantías (cláusulas tipo) — **pendiente**.
- **Plazos de conservación**: documentación clínica según Ley 41/2002;
  audit_logs según política.
- **Medidas de seguridad**: ver `docs/SECURITY.md` y `docs/ENCRYPTION.md`.

## Pendientes de cumplimiento (no técnicos)

- DPIA (Art. 35), contratos de encargo (Art. 28), verificación de
  transferencias internacionales, designación de DPO, política de
  conservación formal, gestión del incidente de credenciales.

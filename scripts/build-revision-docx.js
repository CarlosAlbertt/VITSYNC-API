/*
 * Genera el .docx "Revisión y actualización de la memoria del Proyecto
 * Intermodular – VitSync" a partir del contenido de docs/REVISION_DOC_INTERMODULAR.md.
 * Uso: node scripts/build-revision-docx.js <ruta_salida.docx>
 */
const fs = require('fs');
const {
  Document, Packer, Paragraph, TextRun, Table, TableRow, TableCell,
  AlignmentType, LevelFormat, HeadingLevel, BorderStyle, WidthType, ShadingType,
} = require('docx');

const out = process.argv[2] || 'Revision-Memoria-VitSync.docx';
const CONTENT_W = 9360;

// ── Helpers ──────────────────────────────────────────────────────────────
const H1 = (t) => new Paragraph({ heading: HeadingLevel.HEADING_1, children: [new TextRun(t)] });
const H2 = (t) => new Paragraph({ heading: HeadingLevel.HEADING_2, children: [new TextRun(t)] });
const H3 = (t) => new Paragraph({ heading: HeadingLevel.HEADING_3, children: [new TextRun(t)] });
const P = (t) => new Paragraph({ spacing: { after: 120 }, children: [new TextRun(t)] });
// Párrafo con etiqueta en negrita al inicio (p.ej. "Corregir:" / "Motivo:")
const PL = (label, rest) => new Paragraph({
  spacing: { after: 120 },
  children: [new TextRun({ text: label + ' ', bold: true }), new TextRun(rest)],
});
const BUL = (t) => new Paragraph({ numbering: { reference: 'bul', level: 0 }, spacing: { after: 40 }, children: [new TextRun(t)] });
// Cita / texto propuesto (sangrado, en cursiva)
const QUOTE = (t) => new Paragraph({
  spacing: { after: 80 }, indent: { left: 480 },
  children: [new TextRun({ text: t, italics: true })],
});

const border = { style: BorderStyle.SINGLE, size: 1, color: 'CCCCCC' };
const borders = { top: border, bottom: border, left: border, right: border };
function cell(text, w, { head = false, bold = false } = {}) {
  return new TableCell({
    borders,
    width: { size: w, type: WidthType.DXA },
    margins: { top: 60, bottom: 60, left: 110, right: 110 },
    shading: head ? { fill: 'D5E8F0', type: ShadingType.CLEAR } : undefined,
    children: [new Paragraph({ children: [new TextRun({ text, bold: head || bold, size: 19 })] })],
  });
}
function table(widths, rows) {
  return new Table({
    width: { size: CONTENT_W, type: WidthType.DXA },
    columnWidths: widths,
    rows: rows.map((r, i) =>
      new TableRow({ children: r.map((c) => cell(c, widths[r.indexOf(c)] ?? widths[0], { head: i === 0 })) })),
  });
}
// Tabla robusta (índices explícitos, evita problemas con celdas repetidas)
function tbl(widths, header, data) {
  const rows = [];
  rows.push(new TableRow({ children: header.map((h, i) => cell(h, widths[i], { head: true })) }));
  for (const r of data) rows.push(new TableRow({ children: r.map((c, i) => cell(c, widths[i])) }));
  return new Table({ width: { size: CONTENT_W, type: WidthType.DXA }, columnWidths: widths, rows });
}

// ── Contenido ────────────────────────────────────────────────────────────
const children = [];

children.push(new Paragraph({
  spacing: { after: 60 },
  children: [new TextRun({ text: 'VitSync', bold: true, size: 48 })],
}));
children.push(new Paragraph({
  spacing: { after: 240 },
  children: [new TextRun({ text: 'Revisión y actualización de la memoria del Proyecto Intermodular', size: 28, color: '555555' })],
}));
children.push(P('Documento de trabajo. Compara la memoria académica (PDF entregable) con el estado real del código verificado en los repositorios VITSYNC-API y VITSYNC-WebApp (a 2026-06-21) y aporta, apartado por apartado, el texto corregido listo para pegar más la justificación de cada cambio.'));
children.push(PL('Contexto del desfase:', 'la memoria está firmada el 30/04/2026. Después de esa fecha se realizó un hardening de seguridad completo y mejoras de perfil/RGPD (junio 2026). Por eso la memoria se queda corta en seguridad (describe la versión vieja) y a la vez da por entregados módulos que en el backend todavía no existen.'));

// Resumen ejecutivo
children.push(H1('Resumen ejecutivo de los desajustes'));
children.push(H2('A) Seguridad obsoleta en la memoria (ya está mejorada en el código)'));
children.push(tbl([4680, 4680], ['La memoria dice…', 'La realidad del código es…'], [
  ['JWT HS512, validez 24 h, en localStorage', 'RS256 (clave asimétrica), access 15 min + refresh opaco 7 días (hash en BD, rotación); access token solo en memoria; refresh en cookie HttpOnly; Secure; SameSite=None'],
  ['2FA "estructurado pero no activo"', '2FA por email ACTIVO (login en 2 pasos, código de un solo uso)'],
  ['Rate limiting "pendiente"', 'Bucket4j activo en login/registro/verify/export/recuperación'],
  ['Cifrado: "Neon cifra en reposo" + BCrypt', 'Además cifrado AES-256-GCM a nivel de aplicación en campos clínicos (SensitiveDataConverter)'],
  ['BCrypt "factor de coste 12"', 'BCryptPasswordEncoder por defecto → coste 10'],
  ['Sin módulo RGPD descrito', 'GdprController (acceso, portabilidad, supresión/anonimización) + auditoría audit_logs append-only'],
  ['—', 'Preguntas de recuperación de contraseña + reseteo con doble factor (junio 2026)'],
]));
children.push(new Paragraph({ spacing: { after: 120 }, children: [] }));
children.push(H2('B) Módulos sobre-declarados (la memoria los da por hechos y el backend no los implementa)'));
children.push(tbl([4680, 4680], ['La memoria da por entregado…', 'Estado real'], [
  ['Módulo de salud /api/salud (RF-05, Anexo B)', 'No existe backend. Vista de frontend con datos de ejemplo'],
  ['Módulo de enfermedades /api/enfermedades (RF-10)', 'No existe backend. Catálogo de frontend sin persistencia propia'],
  ['Informes en PDF (/api/informes/{id}/pdf, PDFBox)', 'No implementado. El controlador solo lista informes y permite notas del paciente'],
  ['Médico crea/firma informes (POST/PUT /api/informes)', 'No expuesto. InformeController = GET /me, GET, PUT /{id}/notes'],
]));
children.push(new Paragraph({ spacing: { after: 120 }, children: [] }));
children.push(H2('C) Errores concretos de nomenclatura/rutas'));
[
  'Enum Gender = HOMBRE / MUJER / OTRO (no MALE/FEMALE/OTHER).',
  'Self-service de usuario cuelga de /VitSync-app (no /api/users).',
  'Administración de usuarios = /api/usuarios (no /api/admin/users).',
  'RGPD = /api/users/{id}/my-data | my-data/export | gdpr-delete.',
  'Existen módulos no documentados: /api/hospitales, /api/horarios, /api/relationships.',
].forEach((t) => children.push(BUL(t)));

// Análisis
children.push(H1('Análisis del proyecto'));
children.push(H2('2. DAFO — Debilidades'));
children.push(PL('Corregir:', 'sustituir el punto "2FA… no se encuentra activa" por:'));
children.push(QUOTE('Algunos módulos funcionales (seguimiento de salud y catálogo de enfermedades/tratamientos) están implementados en el frontend con datos de ejemplo, pero todavía no disponen de persistencia propia en el backend.'));
children.push(QUOTE('La generación de informes clínicos en PDF está planificada pero aún no implementada.'));
children.push(PL('Motivo:', 'el 2FA ya está activo (por email); las debilidades reales hoy son el alcance backend de salud/enfermedades y el PDF de informes.'));
children.push(PL('Fortalezas — sustituir el punto de autenticación por:', ''));
children.push(QUOTE('Sistema de autenticación de alta robustez: JWT firmado con RS256, access token de 15 min + refresh token opaco de 7 días con rotación y almacenamiento hasheado en BD, verificación en dos pasos (2FA) por correo, recuperación de contraseña por preguntas de seguridad + código por email, hashing BCrypt, cifrado AES-256-GCM en reposo de los datos clínicos y limitación de tasa (rate limiting) en los endpoints sensibles.'));

children.push(H2('3. Requisitos funcionales'));
children.push(H3('RF-02. Registro y autenticación'));
children.push(PL('Reemplazar los puntos de token (24 h en localStorage) por:', ''));
children.push(QUOTE('El inicio de sesión se realiza mediante NIF y contraseña. Si las credenciales son válidas, el sistema emite un access token JWT firmado con RS256 y validez de 15 minutos y un refresh token opaco de 7 días.'));
children.push(QUOTE('El access token reside únicamente en memoria del cliente (nunca en localStorage/sessionStorage) y el refresh token viaja en una cookie HttpOnly; Secure; SameSite=None inaccesible a JavaScript. La sesión se renueva por rotación del refresh token.'));
children.push(QUOTE('Si el usuario tiene activada la verificación en dos pasos (2FA), el login no emite tokens: el sistema envía un código de un solo uso al correo y exige un segundo paso para abrir la sesión.'));
children.push(QUOTE('El usuario puede recuperar el acceso si olvida la contraseña respondiendo a sus preguntas de seguridad y confirmando con un código enviado por email (doble factor: conocimiento + posesión).'));

children.push(H3('RF-03. Gestión de perfil de usuario'));
children.push(PL('Añadir:', ''));
children.push(QUOTE('El usuario puede cambiar su contraseña (verificando la actual y aplicando la política de complejidad) y gestionar su seguridad avanzada: activar/desactivar el 2FA por email y configurar sus preguntas de recuperación.'));
children.push(QUOTE('El usuario puede consultar y cerrar sus sesiones activas (dispositivo, IP y última actividad), una a una o todas las demás salvo la actual. El correo electrónico es de solo lectura.'));

children.push(H3('RF-04. Sistema de citas'));
children.push(PL('Ajustar a lo expuesto por la API:', 'el paciente puede solicitar una cita (POST), consultar sus citas (GET /me) y cancelarlas (PUT /{id}/cancel) verificando la propiedad. Al confirmarse una cita se envía email de confirmación. La gestión avanzada por médico/administrador es parcial; conviene no describirla como completa.'));

children.push(H3('RF-05. Módulo de seguimiento de salud'));
children.push(PL('Nota de estado a añadir:', ''));
children.push(QUOTE('(Estado: implementado en el frontend con datos de ejemplo; la persistencia en backend de los indicadores de salud queda como trabajo futuro.)'));
children.push(PL('Motivo:', 'no existe SaludController ni entidad de mediciones; las gráficas se renderizan en el cliente con datos mock.'));

children.push(H3('RF-06. Informes clínicos'));
children.push(PL('Reescribir:', ''));
children.push(QUOTE('Los pacientes pueden consultar sus informes clínicos (GET /me), añadir notas personales (PUT /{id}/notes) y marcarlos como favoritos. El acceso a los archivos está protegido por autenticación y rol. (Trabajo futuro: creación/firma de informes por el médico y descarga en PDF.)'));

children.push(H3('RF-08 y RF-10. Administración y enfermedades'));
children.push(PL('RF-08 — corregir base de rutas:', 'CRUD sobre usuarios (/api/usuarios), médicos (/api/medicos) y especialidades (/api/especialidades). Activar/verificar y suspender cuentas (PATCH /api/usuarios/{id}/verificar, campo suspended). La gestión de enfermedades es de frontend.'));
children.push(PL('RF-10 — nota de estado:', '(catálogo gestionado en el frontend; sin backend de persistencia dedicado por el momento). No existe EnfermedadController.'));

children.push(H2('4. Requisitos no funcionales'));
children.push(H3('RNF-02. Seguridad y privacidad — reescritura recomendada'));
[
  'Contraseñas: hash BCrypt (por defecto, coste 10). Política ≥12 caracteres con mayúscula, minúscula, dígito y carácter especial.',
  'Tokens: JWT RS256 (clave asimétrica), access 15 min; refresh opaco 7 días hasheado en BD con rotación y revocación; entregado en cookie HttpOnly; Secure; SameSite=None.',
  'Cifrado en reposo: campos clínicos (paciente, informes, mensajes) cifrados con AES-256-GCM a nivel de aplicación (SensitiveDataConverter), además del cifrado de disco de Neon.',
  'Verificación en dos pasos (2FA) por correo y recuperación de contraseña con preguntas de seguridad + código por email.',
  'Rate limiting con Bucket4j (login 5/15 min, registro 3/h, verificación 10/h, exportación RGPD 1/24 h, recuperación 5/h) → 429 con Retry-After.',
  'Control de acceso: RBAC con Spring Security (@PreAuthorize) y prevención de IDOR (SecurityUtils.requireSelfOrAdmin).',
  'Auditoría: registro append-only en audit_logs (@Auditable + AuditAspect), conforme a RGPD Art. 30 y Ley 41/2002.',
  'Validación: @Valid + DTOs (espejo en el frontend), validador de NIF con letra de control; saneado de MIME con Apache Tika.',
  'Cabeceras de seguridad: HSTS, X-Content-Type-Options, anti-clickjacking, Referrer-Policy y CSP restrictiva; CORS limitado a dominios oficiales.',
  'Frontend: saneado HTML con DOMPurify, sin secretos en el bundle, eliminación de console.* en build.',
  'Gestión de secretos: claves y credenciales solo como variables de entorno (RSA, ENCRYPTION_KEY, RESEND_API_KEY), nunca en el código.',
].forEach((t) => children.push(BUL(t)));
children.push(H3('RNF-04. Arquitectura — matiz sobre "sin estado"'));
children.push(QUOTE('El access token es stateless (verificable sin consultar la BD), mientras que los refresh tokens son stateful (almacenados hasheados y revocables), lo que permite cerrar sesiones e invalidar credenciales tras un cambio de contraseña.'));

children.push(H2('6. Casos de uso — CU-03 Inicio de sesión'));
children.push(PL('Corregir la postcondición:', 'el access token se mantiene en memoria del cliente y el refresh token en cookie HttpOnly; si el usuario tiene 2FA activo, se requiere un segundo paso con código por email antes de abrir la sesión.'));

// Diseño
children.push(H1('Diseño'));
children.push(H2('2. Modelo de datos'));
children.push(PL('gender:', 'el enumerado real es HOMBRE, MUJER, OTRO (no MALE/FEMALE/OTHER).'));
children.push(PL('Atributos de users a añadir/precisar (todos presentes):', ''));
[
  'twoFactorEnabled, twoFactorCode (hash), twoFactorCodeExpiry.',
  'securityQuestion1/2 (texto), securityAnswer1/2 (hash BCrypt).',
  'passwordResetCode (hash), passwordResetCodeExpiry.',
  'Los códigos/secretos y los hashes no se serializan en las respuestas JSON (@JsonIgnore / WRITE_ONLY).',
].forEach((t) => children.push(BUL(t)));
children.push(PL('El modelo NO son "tres entidades":', 'además de users, medicos, pacientes y administradores (herencia JOINED), existen Cita, Especialidad, Hospital, Informe, Mensaje, ChatMessage, PacienteMedico, RefreshToken, AuditLog e HistorialAcceso. Los datos clínicos del paciente se almacenan cifrados (AES-256-GCM).'));

children.push(H2('3. Tecnologías utilizadas'));
children.push(PL('Backend — completar:', 'Spring Security 6 + JWT RS256 (jjwt 0.12.6), Bucket4j (rate limit), Apache Tika (MIME), Resend (email), WebSocket STOMP + SockJS, JUnit 5 + Mockito, JaCoCo (umbral 80 % en service/ y util/), H2 en tests.'));
children.push(PL('Frontend — añadir:', 'DOMPurify (saneado XSS), Vitest (pruebas), @stomp/stompjs + sockjs-client.'));
children.push(PL('Eliminar/aclarar PDFBox:', 'se menciona en el Sprint 5 pero no se usa (la exportación PDF de informes no está implementada).'));

// Despliegue
children.push(H1('Plan de despliegue'));
children.push(H2('1. Entorno local'));
children.push(PL('Arranque del frontend:', 'el flujo de desarrollo es npm install && npm run dev (servidor Vite en http://localhost:5173). docker compose es una alternativa en contenedor, no el flujo con HMR.'));
children.push(PL('Variables de entorno del backend (precisar):', 'DATABASE_URL/USERNAME/PASSWORD, CORS_ALLOWED_ORIGINS, JWT_PRIVATE_KEY y JWT_PUBLIC_KEY (par RS256 base64 DER), ENCRYPTION_KEY (32 bytes base64, AES-256), RESEND_API_KEY; opcionales JWT_ACCESS_EXPIRATION, JWT_REFRESH_EXPIRATION, MAIL_FROM_ADDRESS, UPLOAD_DIR, DDL_AUTO (testing=update, prod=validate). El script scripts/setup-env.sh valida el entorno y genera las claves RSA + AES.'));
children.push(PL('Esquema de BD (producción):', 'ddl-auto=validate + ejecutar los scripts de scripts/sql/ (V2 refresh_tokens, V3 cifrado, V4 audit_logs, V5 metadatos de sesión, V6 2FA, V7 preguntas de recuperación) antes de desplegar.'));

// Pruebas
children.push(H1('Evaluación y pruebas'));
children.push(PL('Login.vue / Test 1 — corregir la comprobación:', 'el access token queda en memoria (no en localStorage); el refresh viaja en cookie HttpOnly; el usuario es redirigido a la página principal; no se muestra error.'));
children.push(PL('Backend (JUnit) — actualizar cifras:', 'la suite cuenta con 172 pruebas (JUnit 5 + Mockito) cubriendo autenticación, recuperación de cuenta, validación de NIF, JWT y saneado HTML, con cobertura medida por JaCoCo (umbral 80 % en service/ y util/).'));
children.push(PL('E2E:', 'las pruebas E2E (Cypress/Playwright) están pendientes; recomendado marcarlo como trabajo futuro en lugar de listarlo como realizado.'));

// Sostenibilidad
children.push(H1('Plan de sostenibilidad'));
children.push(PL('Protección de datos en reposo — ampliar:', 'además del cifrado de disco de Neon, VitSync aplica cifrado AES-256-GCM a nivel de aplicación sobre los campos clínicos.'));
children.push(PL('Derechos RGPD — añadir apartado:', 'el sistema implementa acceso (GET /my-data), portabilidad (GET /my-data/export) y supresión por anonimización (DELETE /gdpr-delete) mediante GdprController, junto con la auditoría append-only de accesos (audit_logs) exigida por la Ley 41/2002 y el RGPD Art. 30.'));

// Producto final
children.push(H1('Producto final'));
children.push(PL('Fortalezas:', 'sustituir el punto de seguridad por la redacción ampliada de RNF-02 (RS256 + refresh HttpOnly + AES-256-GCM + 2FA + rate limiting + auditoría + RGPD).'));
children.push(PL('Debilidades — reescribir (las actuales ya no aplican):', ''));
[
  'Backend de "Mi Salud" y "Enfermedades" pendiente: funcionan en el frontend con datos de ejemplo, sin persistencia propia en la API.',
  'Informes en PDF pendientes y creación/firma por el médico no expuesta.',
  'Dashboard del médico limitado (gestión de agenda parcial).',
  'Almacenamiento de avatares efímero (disco de Render; se pierden al redeploy) → pendiente de almacenamiento externo (S3/Cloudinary).',
].forEach((t) => children.push(BUL(t)));
children.push(PL('Eliminar:', '"rate limiting pendiente" y "2FA no activo": ambos están implementados.'));

// Trabajo futuro
children.push(H1('Trabajo futuro'));
children.push(PL('Eliminar de pendientes (ya hechos):', '2FA por email, rate limiting (Bucket4j) y notificaciones por email de citas.'));
children.push(PL('Mantener / añadir como trabajo futuro real:', ''));
[
  'Persistencia en backend del módulo de salud y del catálogo de enfermedades.',
  'Generación de informes en PDF y creación/firma por el médico.',
  'Dashboard del médico completo (agenda, disponibilidad, franjas).',
  'Almacenamiento de avatares/ficheros externo (S3/Cloudinary).',
  'Cumplimiento RGPD avanzado: DPIA (Art. 35), contratos de encargo (Art. 28) y job de anonimización automática a 30 días.',
  '2FA por TOTP/app, vídeo-consulta integrada, app móvil, HL7 FHIR/EHDS y recetas electrónicas.',
].forEach((t) => children.push(BUL(t)));

// Anexo B
children.push(H1('Anexo B — Tabla de endpoints (versión corregida y completa)'));
const EW = [1050, 3550, 1650, 3110];
const EH = ['Método', 'Ruta', 'Acceso', 'Descripción'];
children.push(H3('Autenticación — base /api/auth'));
children.push(tbl(EW, EH, [
  ['POST', '/login', 'Público', 'Login (NIF+contraseña); si hay 2FA devuelve twoFactorRequired'],
  ['POST', '/login/2fa', 'Público', '2.º paso 2FA: verifica el código del email y abre sesión'],
  ['POST', '/register', 'Público', 'Registro de paciente; envía código de verificación'],
  ['POST', '/verify', 'Público', 'Verifica la cuenta con el código del email'],
  ['POST', '/refresh', 'Cookie', 'Renueva el access token (rotación del refresh)'],
  ['POST', '/logout', 'Cookie', 'Cierra la sesión actual (revoca refresh)'],
  ['POST', '/logout-all', 'Autenticado', 'Cierra todas las sesiones del usuario'],
  ['GET', '/validate', 'Autenticado', 'Valida el access token (firma/expiración)'],
  ['GET', '/sessions', 'Autenticado', 'Lista las sesiones activas (dispositivo/IP)'],
  ['DELETE', '/sessions/{id}', 'Autenticado', 'Cierra una sesión concreta'],
  ['POST', '/sessions/revoke-others', 'Autenticado', 'Cierra el resto de sesiones'],
  ['POST', '/recovery/questions', 'Público', 'Devuelve las preguntas de seguridad por NIF'],
  ['POST', '/recovery/verify', 'Público', 'Verifica respuestas y envía código por email'],
  ['POST', '/recovery/reset', 'Público', 'Resetea la contraseña con el código'],
]));
children.push(new Paragraph({ spacing: { after: 100 }, children: [] }));
children.push(H3('Perfil / self-service — base /VitSync-app'));
children.push(tbl(EW, EH, [
  ['GET', '/{id}', 'Propietario/Admin', 'Datos del usuario'],
  ['PUT', '/api/users/{id}/profile', 'Propietario/Admin', 'Actualiza datos del perfil'],
  ['PATCH', '/api/users/{id}/avatar', 'Propietario/Admin', 'Actualiza la URL del avatar'],
  ['PATCH', '/api/users/{id}/password', 'Propietario/Admin', 'Cambia la contraseña'],
  ['PUT', '/api/users/{id}/security/2fa', 'Propietario/Admin', 'Activa/desactiva el 2FA'],
  ['POST', '/api/users/{id}/security/questions', 'Propietario/Admin', 'Configura las preguntas de recuperación'],
]));
children.push(new Paragraph({ spacing: { after: 100 }, children: [] }));
children.push(H3('RGPD — base /api/users/{id}'));
children.push(tbl(EW, EH, [
  ['GET', '/my-data', 'Propietario/Admin', 'Acceso a los datos personales (Art. 15)'],
  ['GET', '/my-data/export', 'Propietario/Admin', 'Portabilidad/exportación (Art. 20)'],
  ['DELETE', '/gdpr-delete', 'Propietario/Admin', 'Supresión por anonimización (Art. 17)'],
]));
children.push(new Paragraph({ spacing: { after: 100 }, children: [] }));
children.push(H3('Citas / Informes'));
children.push(tbl(EW, EH, [
  ['GET', '/api/citas · /api/citas/me', 'Autenticado', 'Listado / citas del usuario'],
  ['POST', '/api/citas', 'Autenticado', 'Solicitar cita (envía email de confirmación)'],
  ['PUT', '/api/citas/{id}/cancel', 'Autenticado', 'Cancelar cita (verifica propiedad)'],
  ['GET', '/api/informes/me · /api/informes', 'Paciente/Auth', 'Informes del paciente / listado'],
  ['PUT', '/api/informes/{id}/notes', 'Paciente', 'Notas personales del paciente'],
]));
children.push(new Paragraph({ spacing: { after: 100 }, children: [] }));
children.push(H3('Catálogos y administración'));
children.push(tbl(EW, EH, [
  ['GET', '/api/especialidades (+/{id},/slug,/tipo)', 'Público/Auth', 'Consulta de especialidades'],
  ['CRUD', '/api/especialidades (+/admin, toggle-activo)', 'Admin', 'Gestión de especialidades'],
  ['GET', '/api/medicos (+/{id},/especialidad/{id})', 'Público', 'Cuadro médico'],
  ['CRUD', '/api/medicos (+/admin, toggle-activo)', 'Admin', 'Gestión de médicos'],
  ['GET/PUT/DELETE/PATCH', '/api/usuarios (+/{id},/rol,/verificar)', 'Admin', 'Administración de usuarios'],
  ['GET', '/api/hospitales · /api/horarios', 'Público (GET)', 'Catálogo para reserva de citas'],
  ['POST', '/api/upload/avatar', 'Autenticado', 'Subida de avatar (validación MIME con Tika)'],
  ['POST/GET', '/api/relationships/*', 'Autenticado', 'Relaciones paciente–médico'],
  ['WS/GET', '/app/chat → /queue/messages · /api/messages/{a}/{b}', 'Autenticado', 'Chat en tiempo real e historial'],
]));
children.push(new Paragraph({
  spacing: { before: 120 },
  children: [new TextRun({ text: 'No existen en el backend (frontend con datos de ejemplo): /api/salud/* y /api/enfermedades/*. La descarga de informes en PDF (/api/informes/{id}/pdf) tampoco está implementada.', italics: true, size: 19 })],
}));

// ── Documento ──────────────────────────────────────────────────────────────
const doc = new Document({
  styles: {
    default: { document: { run: { font: 'Arial', size: 22 } } },
    paragraphStyles: [
      { id: 'Heading1', name: 'Heading 1', basedOn: 'Normal', next: 'Normal', quickFormat: true,
        run: { size: 30, bold: true, font: 'Arial', color: '0D9488' },
        paragraph: { spacing: { before: 320, after: 160 }, outlineLevel: 0 } },
      { id: 'Heading2', name: 'Heading 2', basedOn: 'Normal', next: 'Normal', quickFormat: true,
        run: { size: 25, bold: true, font: 'Arial', color: '0F172A' },
        paragraph: { spacing: { before: 220, after: 120 }, outlineLevel: 1 } },
      { id: 'Heading3', name: 'Heading 3', basedOn: 'Normal', next: 'Normal', quickFormat: true,
        run: { size: 22, bold: true, font: 'Arial', color: '334155' },
        paragraph: { spacing: { before: 160, after: 80 }, outlineLevel: 2 } },
    ],
  },
  numbering: {
    config: [
      { reference: 'bul', levels: [{ level: 0, format: LevelFormat.BULLET, text: '•', alignment: AlignmentType.LEFT,
        style: { paragraph: { indent: { left: 600, hanging: 280 } } } }] },
    ],
  },
  sections: [{
    properties: { page: { size: { width: 12240, height: 15840 }, margin: { top: 1440, right: 1440, bottom: 1440, left: 1440 } } },
    children,
  }],
});

Packer.toBuffer(doc).then((buf) => { fs.writeFileSync(out, buf); console.log('OK ->', out); });

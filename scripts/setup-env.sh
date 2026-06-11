#!/usr/bin/env bash
# ==============================================================
# setup-env.sh — Validación del entorno de VitSync API
# ==============================================================
# Uso:
#   bash scripts/setup-env.sh                  → valida variables requeridas
#   bash scripts/setup-env.sh --generate-keys  → genera par RSA + clave AES
#
# Falla (exit 1) si falta alguna variable requerida, para que el
# arranque en Render/local aborte ANTES de levantar Spring con una
# configuración incompleta.
# ==============================================================
set -u

REQUIRED_VARS=(
  DATABASE_URL
  DATABASE_USERNAME
  DATABASE_PASSWORD
  CORS_ALLOWED_ORIGINS
  JWT_PRIVATE_KEY
  JWT_PUBLIC_KEY
  ENCRYPTION_KEY
  RESEND_API_KEY
)

OPTIONAL_VARS=(
  PORT
  JWT_ACCESS_EXPIRATION
  JWT_REFRESH_EXPIRATION
  MAIL_FROM_ADDRESS
  UPLOAD_DIR
)

generate_keys() {
  command -v openssl >/dev/null 2>&1 || { echo "ERROR: se necesita openssl"; exit 1; }
  TMPDIR_KEYS=$(mktemp -d)
  trap 'rm -rf "$TMPDIR_KEYS"' EXIT

  # RSA 2048 para desarrollo. En producción usar 4096:
  #   openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:4096 ...
  openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 \
      -out "$TMPDIR_KEYS/private.pem" 2>/dev/null
  openssl pkey -in "$TMPDIR_KEYS/private.pem" -pubout -out "$TMPDIR_KEYS/public.pem" 2>/dev/null

  # DER + base64 en una sola línea (formato que espera JwtUtil)
  PRIV_B64=$(openssl pkcs8 -topk8 -nocrypt -in "$TMPDIR_KEYS/private.pem" -outform DER 2>/dev/null | openssl base64 -A)
  PUB_B64=$(openssl pkey -pubin -in "$TMPDIR_KEYS/public.pem" -outform DER 2>/dev/null | openssl base64 -A)
  AES_B64=$(openssl rand -base64 32)

  echo "# Copia estas variables a tu entorno (Render → Environment, o tu shell):"
  echo "JWT_PRIVATE_KEY=$PRIV_B64"
  echo "JWT_PUBLIC_KEY=$PUB_B64"
  echo "ENCRYPTION_KEY=$AES_B64"
  echo ""
  echo "# ADVERTENCIA: no comitees estos valores. Guarda la clave privada"
  echo "# y ENCRYPTION_KEY en un gestor de secretos."
  exit 0
}

[ "${1:-}" = "--generate-keys" ] && generate_keys

missing=0
for var in "${REQUIRED_VARS[@]}"; do
  if [ -z "${!var:-}" ]; then
    echo "FALTA (requerida): $var"
    missing=1
  fi
done

for var in "${OPTIONAL_VARS[@]}"; do
  if [ -z "${!var:-}" ]; then
    echo "opcional sin definir: $var (se usará el valor por defecto)"
  fi
done

# Validaciones de formato básicas
if [ -n "${ENCRYPTION_KEY:-}" ]; then
  decoded_len=$(printf '%s' "$ENCRYPTION_KEY" | openssl base64 -d -A 2>/dev/null | wc -c | tr -d ' ')
  if [ "$decoded_len" != "32" ]; then
    echo "ERROR: ENCRYPTION_KEY debe ser 32 bytes en base64 (actual: $decoded_len bytes)"
    missing=1
  fi
fi

if [ -n "${DATABASE_URL:-}" ] && ! printf '%s' "$DATABASE_URL" | grep -q '^jdbc:postgresql://'; then
  echo "ERROR: DATABASE_URL debe empezar por jdbc:postgresql://"
  missing=1
fi

if [ "$missing" -eq 1 ]; then
  echo ""
  echo "Entorno INCOMPLETO. Aborta el arranque."
  exit 1
fi

echo "Entorno OK: todas las variables requeridas están definidas."

#!/bin/bash
# Genera el par de llaves Ed25519 con el que identidad-service firma los JWT y lo deja en formato de UNA línea (los
# saltos de línea del PEM se escriben como \n), listo para pegar en .env o en el gestor de secretos.
#
#   ./scripts/generar-claves-jwt.sh           imprime IDENTIDAD_JWT_CLAVE_PRIVADA=... e IDENTIDAD_JWT_CLAVE_PUBLICA=...
#   ./scripts/generar-claves-jwt.sh >> .env   las agrega al .env
#
# La privada es un secreto: solo identidad-service la recibe. La pública no es secreta y la usan los demás servicios.
# Para ROTAR: genera un par nuevo, despliega primero la pública nueva en todos los servicios y al final la privada.
# Requiere openssl.
set -euo pipefail

temporal="$(mktemp -d)"
trap 'rm -rf "$temporal"' EXIT

openssl genpkey -algorithm ed25519 -out "$temporal/privada.pem" 2>/dev/null
openssl pkey -in "$temporal/privada.pem" -pubout -out "$temporal/publica.pem" 2>/dev/null

# Une las líneas del PEM escribiendo "\n" literal entre ellas (y quita los \r de Windows).
en_una_linea() { tr -d '\r' < "$1" | paste -sd '|' - | sed 's/|/\\n/g'; }

echo "IDENTIDAD_JWT_CLAVE_PRIVADA=$(en_una_linea "$temporal/privada.pem")"
echo "IDENTIDAD_JWT_CLAVE_PUBLICA=$(en_una_linea "$temporal/publica.pem")"

#!/usr/bin/env bash
# Corre docker compose con la configuración de un ambiente: env/<ambiente>.env (versionado, sin secretos) más
# env/<ambiente>.secretos.env (ignorado por git, con las contraseñas y claves).
#
# Uso:    ./scripts/compose.sh <desarrollo|pruebas|produccion> <argumentos de docker compose>
# Ejemplo: ./scripts/compose.sh pruebas up -d
#          ./scripts/compose.sh produccion ps
set -euo pipefail

cd "$(dirname "$0")/.."

ambiente="${1:-}"
case "$ambiente" in
  desarrollo|pruebas|produccion) shift ;;
  *)
    echo "Uso: $0 <desarrollo|pruebas|produccion> <argumentos de docker compose>" >&2
    exit 2
    ;;
esac

config="env/${ambiente}.env"
secretos="env/${ambiente}.secretos.env"

if [ ! -f "$secretos" ]; then
  echo "Falta ${secretos}. Cópialo de env/secretos.env.example y completa los valores:" >&2
  echo "  cp env/secretos.env.example ${secretos}" >&2
  exit 1
fi

# El segundo --env-file gana si una variable aparece en los dos.
exec docker compose --env-file "$config" --env-file "$secretos" "$@"

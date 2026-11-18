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

# Banderas opcionales (true/false) en env/<ambiente>.env o en el archivo de secretos: cada una activa el perfil de
# Compose que levanta sus contenedores. Sin la bandera, esos contenedores no existen y los servicios funcionan igual.
valor() { grep -hE "^$1=" "$config" "$secretos" 2>/dev/null | tail -1 | cut -d= -f2- | tr -d '\r'; }
perfiles=()
[ "$(valor OBSERVABILIDAD_HABILITADA)" = "true" ] && perfiles+=(--profile observabilidad)
[ "$(valor OBSERVABILIDAD_HOST_HABILITADA)" = "true" ] && perfiles+=(--profile host)

# El segundo --env-file gana si una variable aparece en los dos.
exec docker compose --env-file "$config" --env-file "$secretos" "${perfiles[@]+"${perfiles[@]}"}" "$@"

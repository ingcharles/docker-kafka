#!/usr/bin/env bash
# Corre docker compose del stack de Postgres con la configuración de un ambiente: los mismos archivos que usan los
# servicios (../facturacion-electronica-ec/env/<ambiente>.env y .secretos.env), de modo que las contraseñas no se duplican.
# Las banderas del ambiente se traducen a perfiles de Compose:
#   PGBOUNCER_HABILITADO=true                 -> pgbouncer
#   REPLICACION_HABILITADA=true               -> replica
#   RESPALDO_ALMACEN_LOCAL_HABILITADO=true    -> almacen-s3 (solo si RESPALDO_HABILITADO=true y RESPALDO_REPO=s3)
#
# Uso:     ./compose.sh <desarrollo|pruebas|produccion> <argumentos de docker compose>
# Ejemplo: ./compose.sh desarrollo up -d --build
#          ./compose.sh desarrollo --profile migrar run --rm migraciones
set -euo pipefail

cd "$(dirname "$0")"

ambiente="${1:-}"
case "$ambiente" in
  desarrollo) proyecto="postgres" ;;                        # conserva el volumen de datos de siempre
  pruebas) proyecto="facturacion-pruebas" ;;
  produccion) proyecto="facturacion-produccion" ;;
  *)
    echo "Uso: $0 <desarrollo|pruebas|produccion> <argumentos de docker compose>" >&2
    exit 2
    ;;
esac
shift

config="../facturacion-electronica-ec/env/${ambiente}.env"
secretos="../facturacion-electronica-ec/env/${ambiente}.secretos.env"
if [ ! -f "$secretos" ]; then
  echo "Falta ${secretos}. Cópialo de ../facturacion-electronica-ec/env/secretos.env.example y completa los valores." >&2
  exit 1
fi

valor() { grep -hE "^$1=" "$config" "$secretos" 2>/dev/null | tail -1 | cut -d= -f2- | tr -d '\r' || true; }
perfiles=()
[ "$(valor PGBOUNCER_HABILITADO)" = "true" ] && perfiles+=(--profile pgbouncer)
[ "$(valor REPLICACION_HABILITADA)" = "true" ] && perfiles+=(--profile replica)
if [ "$(valor RESPALDO_HABILITADO)" = "true" ] && [ "$(valor RESPALDO_REPO)" = "s3" ] \
   && [ "$(valor RESPALDO_ALMACEN_LOCAL_HABILITADO)" = "true" ]; then
  perfiles+=(--profile almacen-s3)
fi

# El segundo --env-file gana si una variable aparece en los dos.
exec docker compose -p "$proyecto" --env-file "$config" --env-file "$secretos" "${perfiles[@]+"${perfiles[@]}"}" "$@"

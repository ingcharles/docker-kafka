#!/bin/sh
# Aplica con Flyway las migraciones V*.sql de cada microservicio sobre su
# esquema, usando el rol de ese servicio. Deja el mismo flyway_schema_history
# que dejaría el propio servicio al arrancar, así que el servicio después ve
# todo como ya aplicado y no hay conflicto. Idempotente: se puede correr
# las veces que quieras (solo aplica lo pendiente).
#
# Se ejecuta dentro del contenedor `migraciones` (ver docker-compose.yml):
#   docker compose --profile migrar run --rm migraciones
set -e

BASE=/proyecto
URL="jdbc:postgresql://${DB_HOST:-facturacion-db}:5432/${POSTGRES_DB:-facturacion}"

# directorio-del-servicio  esquema  rol  variable-de-contraseña
SERVICIOS="
inventario-service               inventario         inventario_service     INVENTARIO_DB_PASSWORD
firma-electronica-service        firma_electronica  firma_service          FIRMA_DB_PASSWORD
comprobantes-service             comprobantes       comprobantes_service   COMPROBANTES_DB_PASSWORD
sri-conector-service             sri                sri_service            SRI_DB_PASSWORD
notificacion-service             notificacion       notificacion_service   NOTIFICACION_DB_PASSWORD
facturacion-orchestrator-service orquestacion       orquestacion_service   ORQUESTACION_DB_PASSWORD
identidad-service                identidad          identidad_service      IDENTIDAD_DB_PASSWORD
"

echo "$SERVICIOS" | while read -r dir esquema rol var; do
  [ -z "$dir" ] && continue
  loc="$BASE/$dir/src/main/resources/db/migration"
  if [ ! -d "$loc" ]; then
    echo "!! No existe $loc — ¿FACTURACION_PROYECTO_DIR apunta bien?" >&2
    exit 1
  fi
  eval "pass=\${$var:?$var must be set}"
  echo "==> $dir -> esquema $esquema (rol $rol)"
  flyway -url="$URL" -user="$rol" -password="$pass" \
    -schemas="$esquema" -defaultSchema="$esquema" \
    -locations="filesystem:$loc" -connectRetries=10 migrate
done
echo "Listo: tablas creadas en todos los esquemas."

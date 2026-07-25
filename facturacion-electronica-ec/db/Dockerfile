# Postgres precargado con el esquema-por-servicio de la base de datos
# facturacion, para que el stack arranque con el rol y el esquema de cada
# microservicio ya listos antes de que Flyway corra dentro de cada uno.
FROM postgres:16-alpine

COPY init/ /docker-entrypoint-initdb.d/

HEALTHCHECK --interval=10s --timeout=5s --start-period=10s --retries=5 \
  CMD pg_isready -U "$POSTGRES_USER" -d "$POSTGRES_DB" || exit 1

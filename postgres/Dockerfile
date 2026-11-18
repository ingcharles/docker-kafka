# Postgres precargado con el esquema-por-servicio de la base de datos
# facturacion, para que el stack arranque con el rol y el esquema de cada
# microservicio ya listos antes de que Flyway corra dentro de cada uno.
FROM postgres:16-alpine

COPY init/ /docker-entrypoint-initdb.d/
# Normaliza CRLF -> LF: si el repo se clona en Windows con autocrlf, un
# `#!/bin/sh^M` hace que Postgres no pueda ejecutar el script y se salte la init.
RUN sed -i 's/\r$//' /docker-entrypoint-initdb.d/*.sh && chmod +x /docker-entrypoint-initdb.d/*.sh

HEALTHCHECK --interval=10s --timeout=5s --start-period=10s --retries=5 \
  CMD pg_isready -U "$POSTGRES_USER" -d "$POSTGRES_DB" || exit 1

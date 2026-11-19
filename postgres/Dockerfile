# Postgres precargado con el esquema-por-servicio de la base de datos
# facturacion, para que el stack arranque con el rol y el esquema de cada
# microservicio ya listos antes de que Flyway corra dentro de cada uno.
# Además trae pgBackRest (respaldo continuo y restauración a un instante), apagado salvo RESPALDO_HABILITADO=true, y un
# envoltorio del entrypoint que prepara la replicación y el modo réplica según banderas (ver contenedor/entrypoint.sh).
FROM postgres:16-alpine

RUN apk add --no-cache pgbackrest tzdata openssl && mkdir -p /etc/pgbackrest

COPY init/ /docker-entrypoint-initdb.d/
COPY contenedor/ /usr/local/share/facturacion-postgres/
# Normaliza CRLF -> LF: si el repo se clona en Windows con autocrlf, un
# `#!/bin/sh^M` hace que Postgres no pueda ejecutar el script y se salte la init.
RUN sed -i 's/\r$//' /docker-entrypoint-initdb.d/*.sh /usr/local/share/facturacion-postgres/*.sh \
 && chmod +x /docker-entrypoint-initdb.d/*.sh /usr/local/share/facturacion-postgres/*.sh \
 && ln -s /usr/local/share/facturacion-postgres/entrypoint.sh /usr/local/bin/facturacion-entrypoint.sh

HEALTHCHECK --interval=10s --timeout=5s --start-period=10s --retries=5 \
  CMD pg_isready -U "$POSTGRES_USER" -d "$POSTGRES_DB" || exit 1

ENTRYPOINT ["facturacion-entrypoint.sh"]
CMD ["postgres"]

# PgBouncer: agrupa las conexiones de los 7 microservicios para que Postgres atienda muchas menos (modo transaction).
# Opcional: se levanta con PGBOUNCER_HABILITADO=true (ver ../README.md).
FROM alpine:3.24

RUN apk add --no-cache pgbouncer && addgroup -S pgbouncer && adduser -S -D -H -G pgbouncer pgbouncer

COPY entrypoint.sh /usr/local/bin/pgbouncer-entrypoint.sh
RUN sed -i 's/\r$//' /usr/local/bin/pgbouncer-entrypoint.sh && chmod +x /usr/local/bin/pgbouncer-entrypoint.sh

USER pgbouncer
EXPOSE 6432
HEALTHCHECK --interval=10s --timeout=3s --start-period=5s --retries=3 CMD nc -z 127.0.0.1 6432 || exit 1
ENTRYPOINT ["pgbouncer-entrypoint.sh"]

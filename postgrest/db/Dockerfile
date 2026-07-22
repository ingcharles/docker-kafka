# Postgres pre-loaded with the schema/roles PostgREST expects, so the
# stack comes up ready to serve instead of needing a manual migration step.
FROM postgres:16-alpine

COPY init/ /docker-entrypoint-initdb.d/

HEALTHCHECK --interval=10s --timeout=5s --start-period=10s --retries=5 \
  CMD pg_isready -U "$POSTGRES_USER" -d "$POSTGRES_DB" || exit 1

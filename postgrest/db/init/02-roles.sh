#!/bin/sh
# Sets up the two roles PostgREST needs (see the "Standard Role" section
# of the PostgREST docs). Runs once, on first container start, against
# an empty data directory — that's how the official postgres image's
# /docker-entrypoint-initdb.d mechanism works.
set -e

: "${AUTHENTICATOR_PASSWORD:?AUTHENTICATOR_PASSWORD must be set}"

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    -- Role PostgREST switches into for every request by default.
    -- No login, no privileges beyond what's granted below.
    do \$\$
    begin
      if not exists (select from pg_roles where rolname = 'web_anon') then
        create role web_anon nologin;
      end if;
    end
    \$\$;

    grant usage on schema api to web_anon;
    grant select on api.todos to web_anon;

    -- Role PostgREST actually authenticates as. It has no privileges
    -- of its own; it can only act as web_anon (or a JWT-derived role).
    do \$\$
    begin
      if not exists (select from pg_roles where rolname = 'authenticator') then
        create role authenticator noinherit login password '${AUTHENTICATOR_PASSWORD}';
      end if;
    end
    \$\$;

    grant web_anon to authenticator;
EOSQL

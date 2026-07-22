# PostgREST stack

PostgreSQL 16 + PostgREST, exposing a REST API generated straight from
the database schema. Two services:

- **db** — `postgres:16-alpine`, pre-loaded on first start with an `api`
  schema and the two roles PostgREST needs (`db/init/`).
- **postgrest** — `postgrest/postgrest`, authenticates against Postgres
  as `authenticator` and switches into `web_anon` for every request
  (PostgREST's standard security model).

## Security model

- `db/init/01-schema.sql` creates schema `api` with one example table
  (`api.todos`), row-level security **on**, and a permissive select
  policy — replace/extend this with your real schema and policies.
- `db/init/02-roles.sh` creates:
  - `web_anon` — no login, granted only what's explicitly listed
    (`usage` on `api`, `select` on `api.todos`).
  - `authenticator` — the only role PostgREST logs in as; has no
    privileges of its own beyond `web_anon` (or whatever role a JWT
    claim maps to).
- PostgREST only ever sees schema `api` (`PGRST_DB_SCHEMAS`) — nothing
  outside it is reachable through the API, by construction.
- Secrets (DB password, authenticator password, JWT secret) are passed
  as environment variables at container start, never baked into an
  image layer.

## Run standalone

```sh
cp .env.example .env      # then edit the values
docker compose up -d --build
curl http://localhost:3000/todos
```

Interactive API docs (OpenAPI) are served at the same root, e.g.
`http://localhost:3000/` returns the schema description PostgREST
generates automatically.

## Configuration

All variables are in `.env.example`. Required (no default, compose
refuses to start without them): `POSTGRES_PASSWORD`,
`AUTHENTICATOR_PASSWORD`, `PGRST_JWT_SECRET` (32+ chars).

## Notes

- `db/init/` only runs once, against an empty `postgres_data` volume.
  To change the schema after that, write a real migration and apply it
  with `psql`/your migration tool — editing the init scripts won't
  affect an already-initialized volume.
- The `postgrest/postgrest` image ships as a static binary with no
  shell (there's nothing to `docker exec` into), so config is 100%
  environment-variable driven rather than a mounted `postgrest.conf`.
- Add authenticated (non-anonymous) access by setting `PGRST_JWT_SECRET`
  to match whatever issues your JWTs, and add a `role` claim that maps
  to a Postgres role you `GRANT`.

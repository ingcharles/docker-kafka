# Docker

Two independent stacks, each runnable on its own or together.

- [`postgrest/`](postgrest/README.md) — PostgreSQL + PostgREST (auto-generated REST API).
- [`kafka/`](kafka/README.md) — Kafka (KRaft) + topic bootstrap + Kafka UI.

## Run independently

Each folder is a self-contained Compose project:

```sh
cd postgrest && cp .env.example .env   # edit values, then:
docker compose up -d --build

cd ../kafka && cp .env.example .env    # edit values, then:
docker compose up -d --build
```

## Run together

The root `docker-compose.yml` includes both stacks without duplicating
their service definitions (Compose's `include:`, requires Compose
v2.20+). Both `.env` files still need to exist first:

```sh
cp postgrest/.env.example postgrest/.env   # edit values
cp kafka/.env.example kafka/.env           # edit values, see kafka/README.md
docker compose up -d --build
```

This is the same two stacks, same networks/volumes, no shared state
between them — `include:` is purely a convenience for bringing both up
with one command.

## Verified

Both stacks (standalone and combined) were built and run end-to-end
against a live Docker daemon while putting this together: PostgREST
serving seeded rows through the `api` schema/RLS policy, and Kafka
with a topic auto-created and a message produced/consumed through it.

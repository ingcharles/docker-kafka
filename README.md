# Docker

Dos stacks, cada uno se puede correr por separado o los dos juntos.

- [`kafka/`](kafka/README.md) — Kafka (modo KRaft) + creación automática de tópicos + Kafka UI.
- [`ecommerce/`](ecommerce/README.md) — 6 microservicios Quarkus que implementan una Saga orquestada (transacción distribuida) para el flujo de e-commerce de orden a entrega, con su propio Postgres (un esquema por servicio) y reutilizando el broker de Kafka de `kafka/`.

## Correr cada stack por separado

Cada carpeta es un proyecto de Compose autosuficiente. `ecommerce/` es la única excepción parcial: necesita que el broker de `kafka/` ya esté corriendo (se conecta a la red `kafka_net` de ese stack en vez de levantar un segundo broker):

```sh
cd kafka && cp .env.example .env       # edita los valores, luego:
docker compose up -d --build

cd ../ecommerce && cp .env.example .env   # edita los valores, ver ecommerce/README.md
docker compose up -d --build              # requiere que kafka/ ya esté arriba
```

## Correr todo junto

El `docker-compose.yml` de la raíz incluye los dos stacks sin duplicar sus definiciones de servicios (usando `include:` de Compose, requiere Compose v2.20 o superior). Los dos archivos `.env` deben existir de antemano:

```sh
cp kafka/.env.example kafka/.env           # edita los valores, ver kafka/README.md
cp ecommerce/.env.example ecommerce/.env   # edita los valores, ver ecommerce/README.md
```

Los servicios de `ecommerce/` se conectan a la red del broker de `kafka/` como red *externa* (`kafka_net`) — en un entorno completamente nuevo (donde esa red todavía no existe en ningún lado), Compose valida que las redes externas ya existan **antes** de crear nada, así que la primerísima vez que se levanta todo junto hacen falta dos comandos, no uno solo:

```sh
docker compose up -d broker      # crea kafka_net como efecto secundario
docker compose up -d             # ahora la red externa ya existe
```

Cualquier corrida después de esa primera vez — como `kafka_net` ya existe — es simplemente `docker compose up -d --build`.

Son los mismos dos stacks, las mismas redes/volúmenes — `include:` es puramente una comodidad para levantar todo con un solo comando. `ecommerce/` comparte Kafka (el broker de `kafka/`), pero tiene su propio Postgres, totalmente separado.

## Verificado

Los dos stacks (por separado y combinados) se construyeron y se corrieron de punta a punta contra un daemon de Docker real mientras se armaba todo esto: Kafka con un tópico creado automáticamente y un mensaje producido/consumido a través de él, y la saga completa de e-commerce ejercitada con tráfico HTTP + Kafka real — el camino feliz (orden → confirmada) y cada uno de los caminos de compensación (stock insuficiente, pago rechazado, envío fallido) terminando cada uno en el estado final correcto. Ver [`ecommerce/README.md`](ecommerce/README.md) y [`ecommerce/ARQUITECTURA.md`](ecommerce/ARQUITECTURA.md) para el detalle.

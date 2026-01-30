# Local Development – RabbitMQ

Run RabbitMQ locally for development. Start it before any services that use message queues.

## Start

```bash
# From project root
docker compose up -d
```

Wait ~10 seconds for RabbitMQ to be ready.

## Access

| Purpose   | URL / Port | Credentials |
|-----------|------------|-------------|
| AMQP     | `localhost:5672` | guest / guest |
| Management UI | [http://localhost:15672](http://localhost:15672) | guest / guest |

## Configuration

Optional: copy `.env.example` to `.env` and adjust if needed.

| Variable | Default | Description |
|----------|---------|-------------|
| `RABBITMQ_USERNAME` | guest | Broker username |
| `RABBITMQ_PASSWORD` | guest | Broker password |
| `RABBITMQ_VIRTUAL_HOST` | / | Virtual host |

## Stop

```bash
docker compose down
```

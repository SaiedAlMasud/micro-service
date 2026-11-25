# micro-service

## Run the whole project locally with Docker Compose

This local setup starts the three PostgreSQL databases, all backend services,
and the frontend behind an Nginx API gateway. Install Docker Engine and the
Docker Compose plugin, then run these commands from the repository root:

```bash
cp deployment/local-compose.env.example deployment/local-compose.env
docker compose --env-file deployment/local-compose.env -f deployment/docker-compose.local.yml up --build
```

Open [http://localhost:8080](http://localhost:8080). The gateway routes
`/api/v1/auth` and `/api/v1/users` to User Service, product and category API
requests to Product Service, and cart API requests to Cart Service.

To run it in the background, add `-d` to the `up --build` command. View the
service status or logs with:

```bash
docker compose --env-file deployment/local-compose.env -f deployment/docker-compose.local.yml ps
docker compose --env-file deployment/local-compose.env -f deployment/docker-compose.local.yml logs -f
```

Stop the stack with:

```bash
docker compose --env-file deployment/local-compose.env -f deployment/docker-compose.local.yml down
```

The database data remains in Docker volumes after `down`. Do not add `-v`
unless you want to delete that data. Change the local settings in
`deployment/local-compose.env`; keep that file private and use the same
`JWT_SECRET` for all services.

# Two-EC2 Docker deployment

This deployment keeps the three application databases independent:

```text
Browser
  |
  | HTTP now; HTTPS after certificates are installed
  v
Your EC2: Nginx + frontend + User Service + user PostgreSQL
  |
  +--> Friend EC2: restricted Nginx ingress
                         |
                         +--> Product Service + product_db
                         +--> Cart Service + cart_db
                                      |
                                      +--> http://product-service:8082
```

The friend ingress is the only public entry point for Product and Cart on the
friend's instance. Product and Cart application ports and PostgreSQL are not
published. Until TLS certificates and DNS are configured, the example uses
temporary HTTP between the two instances; HTTP is not secure for production.

## Prerequisites

Install Docker Engine and the Compose plugin on both Ubuntu 24.04 instances.
Use Docker's official installation instructions, then verify:

```bash
docker --version
docker compose version
```

Use Elastic IPs for both instances. Do not rely on the current public IPs,
because EC2 public addresses can change after a stop/start.

## Build, tag, and push images

Run these commands from the repository root. The frontend is built with empty
API base URLs so the browser uses the same-origin Nginx routes.

```bash
docker build -t saiedalmasud1/user-service:1.0.0 user-service
docker build -t saiedalmasud1/product-service:1.0.0 product-service
docker build -t saiedalmasud1/cart-service:1.0.0 cart-service
docker build \
  --build-arg VITE_USER_API_URL= \
  --build-arg VITE_PRODUCT_API_URL= \
  --build-arg VITE_CART_API_URL= \
  -t saiedalmasud1/ecommerce-frontend:1.0.0 frontend

docker push saiedalmasud1/user-service:1.0.0
docker push saiedalmasud1/product-service:1.0.0
docker push saiedalmasud1/cart-service:1.0.0
docker push saiedalmasud1/ecommerce-frontend:1.0.0
```

The Docker Hub repositories are public, so images must never contain secrets.
The deployment `.env` files are not copied into images.

## Prepare deployment environment files

On each EC2 instance:

```bash
cp deployment/your-ec2/.env.example deployment/your-ec2/.env
# or on the friend instance:
cp deployment/friends-ec2/.env.example deployment/friends-ec2/.env
chmod 600 deployment/*/.env
```

Replace every `replace-with-...` value. Generate a shared JWT secret with:

```bash
openssl rand -base64 48
```

The same JWT secret must be used by User, Product, and Cart. Do not put that
secret in GitHub, Dockerfiles, Docker build arguments, or Docker Hub.

## Deploy your EC2

Set `FRIEND_PRODUCT_UPSTREAM` and `FRIEND_CART_UPSTREAM` to the friend's
restricted ingress address. The current example uses port 80. After TLS is
configured, use the same HTTPS origin for both variables.

```bash
cd deployment/your-ec2
docker compose --env-file .env pull
docker compose --env-file .env up -d
docker compose ps
curl http://localhost/actuator/health
```

The frontend is served by the same container as the public Nginx reverse
proxy. It forwards User locally and Product/Cart to the friend's ingress.
The current Nginx configuration is an HTTP testing configuration. Add a
certificate, a domain, an HTTPS server block, and an HTTP-to-HTTPS redirect
before production traffic.

## Deploy the friend's EC2

```bash
cd deployment/friends-ec2
docker compose --env-file .env pull
docker compose --env-file .env up -d
docker compose ps
curl http://localhost/api/v1/products
```

The friend EC2 security group must allow port 80 only from your EC2 Elastic
IP while HTTP is being used. It must not allow Product, Cart, or PostgreSQL
ports from the Internet. For production, terminate TLS at the friend ingress
and allow only TCP 443 from your EC2 Elastic IP.

## AWS Security Group rules

Your EC2:

- TCP 80 from `0.0.0.0/0` while HTTP testing is enabled.
- TCP 443 from `0.0.0.0/0` after TLS is configured.
- TCP 22 only from your administrator IP.
- No PostgreSQL or backend application ports.

Friend EC2:

- TCP 80 only from your EC2 Elastic IP during temporary HTTP testing.
- TCP 443 only from your EC2 Elastic IP after TLS is configured.
- TCP 22 only from the administrator IPs.
- No TCP 5432, 8082, or 8083 ingress.

## Database initialization, backup, and restore

The PostgreSQL init scripts run only when the named volume is empty. Changing
an environment password does not change an existing PostgreSQL role password;
perform a deliberate password rotation with `psql` and update the service
environment together.

Back up databases from the host:

```bash
docker compose --env-file .env exec -T user-db \
  pg_dump -U "$USER_DB_USER" "$USER_DB_NAME" > user_db.sql
```

For the friend's instance, run equivalent commands against `service-db` for
`product_db` and `cart_db`. Restore only after stopping the affected service:

```bash
cat user_db.sql | docker compose --env-file .env exec -T user-db \
  psql -U "$USER_DB_USER" "$USER_DB_NAME"
```

Store backups outside the EC2 instance and encrypt them.

## Updating and rollback

Build and push a new immutable tag, then change only the relevant image tag
in the deployment `.env`:

```bash
docker compose --env-file .env pull
docker compose --env-file .env up -d
docker compose ps
docker compose logs --tail=100 user-service
```

For rollback, restore the previous image tag in `.env`, pull it, and run
`docker compose up -d` again. Database migrations are forward-only; verify
migration compatibility before rolling back an application image.

## Troubleshooting

- `unhealthy` database: inspect `docker compose logs service-db` and verify
  the volume was initialized with the intended credentials.
- `401` from Product or Cart: confirm the same `JWT_SECRET` and `JWT_ISSUER`
  are set on all services.
- Cart cannot add products: verify Cart uses
  `PRODUCT_SERVICE_URL=http://product-service:8082` inside Compose.
- Browser API failures: verify the frontend image was built with empty
  `VITE_*_API_URL` values and inspect the Nginx upstream variables.
- Friend API timeout: verify the friend security group allows ingress only from
  your EC2 Elastic IP and that its Nginx container is running.
- CORS errors: the browser should use same-origin `/api/v1` paths. Set
  `FRONTEND_ORIGIN` to the public frontend origin for backend CORS.

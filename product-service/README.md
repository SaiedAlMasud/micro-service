# Product Service

Product catalog service for the microservice project. It owns `product_db` and
does not access another service database.

## Run locally

1. Start PostgreSQL 16 with database `product_db`.
2. Copy `.env.example` to `.env` and set the real database password and JWT secret.
3. Run `./gradlew bootRun`.

Spring Boot loads the local `.env` file automatically. The file is ignored by
Git. Alternatively, export the variables in the shell before starting.

Flyway applies the schema migration automatically. Hibernate validates the
schema and does not create tables.

## API

Public reads:

- `GET /api/v1/products?page=0&size=20&categoryId=<uuid>&q=phone`
- `GET /api/v1/products/{id}`
- `POST /api/v1/products/bulk`
- `GET /api/v1/categories`

ADMIN writes:

- `POST`, `PUT`, and `DELETE /api/v1/products[/{id}]`
- `POST`, `PUT`, and `DELETE /api/v1/categories[/{id}]`

Write requests require a locally validated HS256 JWT issued by User Service.
The token must contain issuer `user-service`, UUID `sub`, `role` (`USER` or
`ADMIN`), `iat`, and `exp` claims. Product Service maps the role claim to
`ROLE_USER` or `ROLE_ADMIN`.
The Cart Service contracts are `GET /api/v1/products/{id}` and
`POST /api/v1/products/bulk`.

## Configuration

`SERVER_PORT`, `DB_URL`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET`, and `JWT_ISSUER` are supplied
through environment variables. Secrets must not be committed.

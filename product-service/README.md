# Product Service

Product catalog service for the microservice project. It owns `product_db` and
does not access another service database.

## Run locally

1. Start PostgreSQL 16 with database `product_db`.
2. Set the variables in `.env.example` in the environment.
3. Run `./gradlew bootRun`.

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

Write requests require a locally validated JWT with a `role` claim of `ADMIN`.
The Cart Service contracts are `GET /api/v1/products/{id}` and
`POST /api/v1/products/bulk`.

## Configuration

`SERVER_PORT`, `DB_URL`, `DB_USER`, `DB_PASSWORD`, and `JWT_SECRET` are supplied
through environment variables. Secrets must not be committed.

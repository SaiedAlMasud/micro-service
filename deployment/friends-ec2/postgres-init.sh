#!/usr/bin/env bash
set -euo pipefail

psql -v ON_ERROR_STOP=1 \
  --username "$POSTGRES_USER" \
  --dbname postgres \
  --set=product_db="$PRODUCT_DB_NAME" \
  --set=product_user="$PRODUCT_DB_USER" \
  --set=product_password="$PRODUCT_DB_PASSWORD" \
  --set=cart_db="$CART_DB_NAME" \
  --set=cart_user="$CART_DB_USER" \
  --set=cart_password="$CART_DB_PASSWORD" <<'SQL'
CREATE DATABASE :"product_db";
CREATE ROLE :"product_user" LOGIN PASSWORD :'product_password';
GRANT CONNECT ON DATABASE :"product_db" TO :"product_user";
CREATE DATABASE :"cart_db";
CREATE ROLE :"cart_user" LOGIN PASSWORD :'cart_password';
GRANT CONNECT ON DATABASE :"cart_db" TO :"cart_user";
SQL

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$PRODUCT_DB_NAME" \
  --set=app_user="$PRODUCT_DB_USER" <<'SQL'
GRANT USAGE, CREATE ON SCHEMA public TO :"app_user";
SQL

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$CART_DB_NAME" \
  --set=app_user="$CART_DB_USER" <<'SQL'
GRANT USAGE, CREATE ON SCHEMA public TO :"app_user";
SQL

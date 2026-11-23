#!/usr/bin/env bash
set -euo pipefail

psql -v ON_ERROR_STOP=1 \
  --username "$POSTGRES_USER" \
  --dbname postgres \
  --set=app_db="$USER_DB_NAME" \
  --set=app_user="$USER_DB_USER" \
  --set=app_password="$USER_DB_PASSWORD" <<'SQL'
CREATE DATABASE :"app_db";
CREATE ROLE :"app_user" LOGIN PASSWORD :'app_password';
GRANT CONNECT ON DATABASE :"app_db" TO :"app_user";
SQL

psql -v ON_ERROR_STOP=1 \
  --username "$POSTGRES_USER" \
  --dbname "$USER_DB_NAME" \
  --set=app_user="$USER_DB_USER" <<'SQL'
GRANT USAGE, CREATE ON SCHEMA public TO :"app_user";
SQL

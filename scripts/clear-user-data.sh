#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
ENV_FILE="${ROOT}/.env"
SQL_FILE="${ROOT}/scripts/clear-user-data.sql"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "Missing .env — set NEON_DATABASE_URL to your Neon/Railway Postgres URL."
  exit 1
fi

NEON_DATABASE_URL="$(grep -E '^NEON_DATABASE_URL=' "$ENV_FILE" | cut -d= -f2- | tr -d '\r' | tr -d '"')"
if [[ -z "$NEON_DATABASE_URL" ]]; then
  echo "NEON_DATABASE_URL is empty in .env"
  exit 1
fi

PSQL="${PSQL:-psql}"
if ! command -v "$PSQL" >/dev/null 2>&1; then
  PSQL="/opt/homebrew/opt/postgresql@16/bin/psql"
fi

echo "Clearing all users (cascades tokens, orders, agencies, onboarding, etc.)..."
echo "Seed catalog (categories, suppliers, products) is preserved."
"$PSQL" "$NEON_DATABASE_URL" -v ON_ERROR_STOP=1 -f "$SQL_FILE"
echo "Done."

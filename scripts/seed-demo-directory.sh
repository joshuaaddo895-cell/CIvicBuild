#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
ENV_FILE="${ROOT}/.env"
SQL_FILE="${ROOT}/src/main/resources/db/migration/V10__seed_suppliers_and_delivery_demo.sql"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "Missing .env — set NEON_DATABASE_URL."
  exit 1
fi

NEON_DATABASE_URL="$(grep -E '^NEON_DATABASE_URL=' "$ENV_FILE" | cut -d= -f2- | tr -d '\r' | tr -d '"')"
PSQL="${PSQL:-psql}"
if ! command -v "$PSQL" >/dev/null 2>&1; then
  PSQL="/opt/homebrew/opt/postgresql@16/bin/psql"
fi

echo "Seeding extra suppliers + demo agency/delivery personnel..."
"$PSQL" "$NEON_DATABASE_URL" -v ON_ERROR_STOP=1 -f "$SQL_FILE"
echo "Done."

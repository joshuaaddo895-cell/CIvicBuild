# Start the full local stack (Postgres + Redis + API)
docker compose up --build -d

# Tail API logs
docker compose logs -f api

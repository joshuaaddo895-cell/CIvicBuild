# Start Postgres + Redis only (run API on host with mvnw spring-boot:run)
docker compose up -d postgres redis

Write-Host "Postgres: postgresql://civicbuild:civicbuild@localhost:5432/civicbuild"
Write-Host "Redis:    redis://localhost:6379"
Write-Host ""
Write-Host "Set these in .env, then run: .\mvnw.cmd spring-boot:run"

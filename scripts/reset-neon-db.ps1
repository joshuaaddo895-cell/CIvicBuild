# Resets the Neon Postgres database and re-applies all Flyway migrations.
# Usage: .\scripts\reset-neon-db.ps1
# Uses Maven Flyway plugin (no Docker required).

$ErrorActionPreference = "Stop"
$ProjectRoot = Split-Path -Parent $PSScriptRoot
$EnvFile = Join-Path $ProjectRoot ".env"

if (-not (Test-Path $EnvFile)) {
    throw ".env not found at $EnvFile"
}

$neonUrl = $null
Get-Content $EnvFile | ForEach-Object {
    $line = $_.Trim()
    if ($line -and -not $line.StartsWith("#") -and $line -match '^NEON_DATABASE_URL=(.+)$') {
        $neonUrl = $Matches[1].Trim()
    }
}

if (-not $neonUrl) {
    throw "NEON_DATABASE_URL not found in .env"
}

if ($neonUrl -notmatch '^postgresql://([^:]+):([^@]+)@([^/]+)/([^?]+)') {
    throw "NEON_DATABASE_URL format is invalid"
}

$user = $Matches[1]
$password = $Matches[2]
$hostPort = $Matches[3]
$database = $Matches[4]
$jdbcUrl = "jdbc:postgresql://${hostPort}/${database}?sslmode=require"

Write-Host "Resetting Neon database ($database)..." -ForegroundColor Yellow

Push-Location $ProjectRoot
try {
    $flywayArgs = @(
        "-q",
        "flyway:clean",
        "flyway:migrate",
        "-Dflyway.url=$jdbcUrl",
        "-Dflyway.user=$user",
        "-Dflyway.password=$password",
        "-Dflyway.cleanDisabled=false"
    )

    & .\mvnw.cmd @flywayArgs
    if ($LASTEXITCODE -ne 0) {
        throw "Flyway reset failed with exit code $LASTEXITCODE"
    }
} finally {
    Pop-Location
}

Write-Host "`nNeon database reset complete." -ForegroundColor Green
Write-Host "- All primary keys are UUID (gen_random_uuid()), not 1/2/3." -ForegroundColor Green
Write-Host "- Deleting a user in Neon cascades: refresh_tokens, password_reset_tokens, orders, order_items, payment_events." -ForegroundColor Green

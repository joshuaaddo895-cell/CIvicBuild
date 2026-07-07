# CivicBuild Backend

Spring Boot REST API for **CivicBuild** — a construction marketplace platform connecting customers, construction agencies, and delivery providers.

This repository contains the backend auth module: sign up, sign in, sign out, token refresh, and password reset.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Runtime | Java 21, Spring Boot 4.1 |
| Database | PostgreSQL (Neon) + Flyway migrations |
| Cache / rate limiting | Redis (Upstash) |
| Auth | JWT access tokens + hashed opaque refresh tokens (BCrypt passwords) |
| Email | Resend |
| Build | Maven |

---

## Prerequisites

- **Java 21** (JDK)
- **Maven** (or use the included `./mvnw` wrapper)
- A **`.env`** file with your credentials (see [Environment variables](#environment-variables))
- **Docker Desktop** (optional — only needed for integration tests)

---

## Quick Start

### 1. Configure environment

```bash
cp .env.example .env
```

Fill in the values in `.env`:

| Variable | Description |
|---|---|
| `NEON_DATABASE_URL` | Neon Postgres connection URL (`postgresql://user:pass@host/db?sslmode=require`) |
| `REDIS_URL` | Upstash Redis TLS URL (`rediss://...`) |
| `RESEND_API_KEY` | Resend API key for transactional email |
| `JWT_SECRET` | HS256 signing secret, min 32 bytes (`openssl rand -base64 64`) |
| `GOOGLE_WEB_CLIENT_ID` | Google OAuth Web client ID (ID-token audience verification) |
| `SERVER_PORT` | HTTP port (default `8081` — frontend typically uses 8080) |

> `.env` is git-ignored. Never commit secrets.

### 2. Run the server

```bash
cd civicbuild
./mvnw spring-boot:run        # Linux / macOS
.\mvnw.cmd spring-boot:run    # Windows
```

The API starts at **http://localhost:8081** by default.

### 3. Verify it's running

```bash
curl http://localhost:8081/api/health
```

Expected response:

```json
{
  "success": true,
  "data": { "status": "UP" },
  "timestamp": "..."
}
```

---

## API Endpoints

All responses use the standard envelope:

```json
{
  "success": true,
  "message": "optional message",
  "data": { },
  "errors": null,
  "timestamp": "2026-07-07T13:00:00Z"
}
```

### Health

| Method | Path | Auth | Description |
|---|---|---|---|
| `GET` | `/api/health` | Public | App liveness check |
| `GET` | `/actuator/health` | Public | Spring Actuator health |

### Authentication

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/api/auth/register` | Public | Create account (defaults to `CUSTOMER` role) |
| `POST` | `/api/auth/login` | Public | Sign in → access + refresh tokens |
| `POST` | `/api/auth/google` | Public | Google Sign-In → same JWT token pair as login |
| `POST` | `/api/auth/refresh` | Public | Rotate refresh token, get new access token |
| `POST` | `/api/auth/logout` | Public | Revoke refresh token |
| `POST` | `/api/auth/forgot-password` | Public | Request password reset email |
| `POST` | `/api/auth/reset-password` | Public | Reset password with email token |

### Request / response contracts

**Register** `POST /api/auth/register`

```json
{ "fullName": "Jane Doe", "email": "jane@example.com", "password": "Secret123" }
```

- Password: min 8 chars, at least one letter + one number
- `confirmPassword` is frontend-only — never send it to the API
- Role is NOT accepted at register (chosen in separate onboarding)
- Returns `201` with user info — **no auto-login**

**Login** `POST /api/auth/login`

```json
{ "email": "jane@example.com", "password": "Secret123" }
```

**Google Sign-In** `POST /api/auth/google`

```json
{ "idToken": "<Google ID token from @react-native-google-signin/google-signin>" }
```

Returns the same `AuthResponse` as login. Google-only accounts cannot use manual login (400). See [docs/GOOGLE_SIGNIN_FRONTEND.md](docs/GOOGLE_SIGNIN_FRONTEND.md) for Expo setup.

Returns:

```json
{
  "success": true,
  "data": {
    "accessToken": "<JWT>",
    "refreshToken": "<opaque token>",
    "tokenType": "Bearer",
    "expiresIn": 900
  }
}
```

**Protected routes** — include header:

```
Authorization: Bearer <accessToken>
```

---

## Postman Collection

Import the collection to test all endpoints interactively:

1. Open Postman → **Import** → select `postman/CivicBuild-Auth-API.postman_collection.json`
2. Collection variables are pre-set:
   - `baseUrl` = `http://localhost:8081`
   - `email`, `password` — update with your test account
3. Run **Register** → **Login** (auto-saves tokens) → **Refresh** / **Logout**

The Login and Refresh requests include test scripts that automatically save `accessToken` and `refreshToken` to collection variables.

---

## Testing

### Unit tests

```bash
./mvnw test
```

Runs JWT, password hashing, rate limiter, and URL parsing tests. Integration tests (Testcontainers) are skipped if Docker is not running.

### Route smoke test (live server)

With the server running on port 8081:

```powershell
.\scripts\test-routes.ps1
```

Tests all 14 auth scenarios: register, login, refresh rotation, logout, forgot/reset password, validation errors.

---

## Project Structure

```
src/main/java/backend/example/civicbuild/
├── auth/
│   ├── controller/     AuthController
│   ├── service/        AuthService, TokenService, PasswordResetService
│   ├── dto/            Request/response DTOs
│   ├── entity/         User, RefreshToken, PasswordResetToken
│   ├── repository/     JPA repositories
│   ├── security/       JwtService, SecurityConfig, filters
│   └── exception/      Domain exceptions
├── email/              Resend email service + templates
├── ratelimit/          Redis-backed rate limiter
├── common/             ApiResponse wrapper, GlobalExceptionHandler, health
└── config/             AppProperties, Neon datasource, Dotenv bootstrap

src/main/resources/
├── application.yml
└── db/migration/       Flyway SQL migrations
```

---

## Security Notes

- **Passwords** hashed with BCrypt (strength 12)
- **Refresh/reset tokens** stored as SHA-256 hashes only — raw tokens never touch the database
- **Refresh token rotation** on every `/refresh` call; old token revoked
- **Logout** revokes refresh token; access JWT expires naturally (~15 min)
- **Rate limiting** on register + login: 5 attempts / 15 min per IP + email (Redis)
- **Forgot password** always returns 200 to prevent email enumeration
- **No Spring default login screen** — JWT-only auth, no browser form login

---

## Environment Variables Reference

See `.env.example` for the full list. Optional overrides:

| Variable | Default | Description |
|---|---|---|
| `SERVER_PORT` | `8081` | HTTP port |
| `JWT_ACCESS_TTL` | `PT15M` | Access token lifetime |
| `JWT_REFRESH_TTL` | `P7D` | Refresh token lifetime |
| `PASSWORD_RESET_TTL` | `PT30M` | Reset token lifetime |
| `RATE_LIMIT_MAX_ATTEMPTS` | `5` | Max attempts per window |
| `RATE_LIMIT_WINDOW` | `PT15M` | Rate limit window |
| `EMAIL_FROM` | `CivicBuild <onboarding@resend.dev>` | Sender address |
| `PASSWORD_RESET_BASE_URL` | `http://localhost:8081/reset-password` | Reset link base URL |

---

## Roles

| Role | Description |
|---|---|
| `CUSTOMER` | Default at registration |
| `CONSTRUCTION_AGENCY` | Selected during onboarding |
| `DELIVERY_PROVIDER` | Selected during onboarding |

Verification status: `UNVERIFIED` → `PENDING` → `VERIFIED` / `REJECTED`

---

## License

Private — CivicBuild project.

# CivicBuild Backend

Spring Boot REST API for **CivicBuild** — a construction marketplace platform connecting customers, construction agencies, and delivery providers.

This repository contains the backend: authentication, Google Sign-In, orders, and Paystack payments.

**Production API:** https://civicbuild-production.up.railway.app

---

## Repository layout (important)

The Spring Boot project root is **this folder** (`civicbuild/`), which must contain `pom.xml`, `Dockerfile`, and `src/` at the **Git repository root** when you push to GitHub.

If Railway reports *"empty civicbuild/ directory"*, the repo was pushed from the wrong parent folder. Fix:

1. Run all `git` commands from `civicbuild/civicbuild` (inner folder)
2. In Railway → **Settings** → **Root Directory** → leave blank or `/` (not `civicbuild`)
3. **Builder** → **Dockerfile** (not Railpack auto-detect)

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
- **Docker Desktop** (optional — for integration tests and local Docker stack)

---

## Docker

Run the full stack locally with **Postgres**, **Redis**, and the **API** — no Neon/Upstash required.

### 1. Configure environment

```bash
cp .env.docker.example .env
```

Fill in `JWT_SECRET`, `RESEND_API_KEY`, `GOOGLE_WEB_CLIENT_ID`, and Paystack keys.  
Docker Compose overrides `NEON_DATABASE_URL` and `REDIS_URL` to use the local containers.

### 2. Start everything

```bash
docker compose up --build
```

Or on Windows:

```powershell
.\scripts\docker-up.ps1
```

API: **http://localhost:8081**

### 3. Infra only (API on host)

Start just Postgres + Redis, then run the app with Maven:

```powershell
.\scripts\docker-infra.ps1
# Set NEON_DATABASE_URL and REDIS_URL in .env to the printed local URLs
.\mvnw.cmd spring-boot:run
```

### 4. Integration tests

With Docker Desktop running:

```powershell
docker compose up -d postgres redis
.\mvnw.cmd test
```

Testcontainers will also spin up isolated Postgres/Redis per test class when Docker is available.

### 5. Production image (Railway / any host)

The `Dockerfile` builds a multi-stage image. Set all env vars on the host platform — do not bake secrets into the image.

```bash
docker build -t civicbuild-api .
docker run -p 8081:8081 --env-file .env civicbuild-api
```

---

## Railway deployment

**Live URL:** https://civicbuild-production.up.railway.app

### 1. Push from the correct folder

```powershell
cd "path\to\civicbuild\civicbuild"   # inner folder — must see pom.xml here
git add .
git commit -m "your message"
git push -u origin main
```

The GitHub repo root must contain `pom.xml`, `Dockerfile`, `src/` — not an empty nested `civicbuild/` folder.

### 2. Railway service settings

| Setting | Value |
|---------|-------|
| **Root Directory** | `/` (empty) |
| **Builder** | Dockerfile |
| **Health check** | `/api/health` |

### 3. Railway environment variables

Set in Railway → **Variables** (same as `.env`):

| Variable | Example / notes |
|----------|-----------------|
| `NEON_DATABASE_URL` | Neon Postgres URL |
| `REDIS_URL` | Upstash Redis URL |
| `JWT_SECRET` | HS256 secret (≥32 bytes) |
| `RESEND_API_KEY` | Resend API key |
| `GOOGLE_WEB_CLIENT_ID` | Google OAuth web client ID |
| `PAYSTACK_SECRET_KEY` | Paystack test/live secret |
| `PAYSTACK_PUBLIC_KEY` | Paystack test/live public key |
| `PAYSTACK_CALLBACK_URL` | `https://civicbuild-production.up.railway.app/api/payments/paystack/callback` |
| `PAYSTACK_WEBHOOK_URL` | `https://civicbuild-production.up.railway.app/api/payments/webhook` |
| `SERVER_PORT` | `8081` (or `${{PORT}}` if Railway injects PORT — see Railway docs) |

### 4. Verify deployment

```bash
curl https://civicbuild-production.up.railway.app/api/health
```

### 5. Paystack dashboard

| Field | Value |
|-------|-------|
| Test Callback URL | `https://civicbuild-production.up.railway.app/api/payments/paystack/callback` |
| Test Webhook URL | `https://civicbuild-production.up.railway.app/api/payments/webhook` |

---

## Quick Start (without Docker)

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
| `CLOUDINARY_CLOUD_NAME` | Cloudinary cloud name |
| `CLOUDINARY_API_KEY` | Cloudinary API key |
| `CLOUDINARY_API_SECRET` | Cloudinary API secret (never commit) |
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

### Orders & payments

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/api/orders/checkout` | JWT | Create order + Paystack initialize → `authorizationUrl` |
| `POST` | `/api/orders/{id}/verify` | JWT | Fallback Paystack verify (webhook is primary) |
| `GET` | `/api/orders/{id}` | JWT | Get order (owner only) |
| `GET` | `/api/orders` | JWT | List my orders |
| `POST` | `/api/payments/webhook` | Public (signed) | Paystack webhook |
| `GET` | `/api/payments/paystack/callback` | Public | Post-checkout redirect (not confirmation) |

---

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

Import `postman/CivicBuild-Auth-API.postman_collection.json`

| Variable | Default | Use |
|----------|---------|-----|
| `baseUrl` | `https://civicbuild-production.up.railway.app` | Production (Railway) |
| `baseUrlLocal` | `http://localhost:8081` | Docker / local Maven |

To test locally, change request URLs from `{{baseUrl}}` to `{{baseUrlLocal}}`, or set `baseUrl` to `http://localhost:8081`.

**Flow:** Register → Login (auto-saves tokens) → Checkout → Get Order / Verify Payment

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

Verification status: `UNVERIFIED` → `PENDING` → `VERIFIED` / `REJECTED` (cosmetic only — does not block access)

**File uploads (Cloudinary)**

| Endpoint | Auth | Purpose |
|---|---|---|
| `POST /api/verification/upload-document` | Bearer JWT | Private verification docs (`multipart/form-data`: `file` + `documentType`) |
| `GET /api/verification/{userId}/document-url?documentType=` | Bearer JWT (owner or `ADMIN`) | Short-lived signed URL for private docs |
| `POST /api/agency/portfolio/upload` | Bearer JWT (`CONSTRUCTION_AGENCY`) | Public portfolio images (`multipart/form-data`: `file`) |
| `POST /api/users/me/avatar` | Bearer JWT | Profile photo upload → `{ profilePictureUrl }` |
| `POST /api/agencies/me/products/upload-image` | Bearer JWT (`CONSTRUCTION_AGENCY`) | Product image upload → `{ imageUrl }` |

`documentType` values: `BUSINESS_REGISTRATION`, `GOVERNMENT_ID`, `PROFESSIONAL_LICENSE`. Max file size: **5MB**.

---

## Marketplace & onboarding APIs (new)

**Onboarding** — `GET/PATCH /api/users/me/onboarding`, `POST /api/users/me/onboarding/complete`

**Agencies** — `POST /api/agencies`, `GET/PATCH /api/agencies/me`, `GET /api/agencies`, `GET /api/agencies/{id}`

**Agency content** — posts (`/api/agencies/me/posts`), portfolio list/delete, personnel approve/reject

**Catalog (public read)** — `GET /api/categories`, `GET /api/suppliers`, `GET /api/products`

**Agency products** — `POST/PATCH/DELETE /api/agencies/me/products`

**Delivery** — `POST /api/delivery-providers/setup`, `GET/PATCH /api/delivery-providers/me`, jobs at `/api/delivery-providers/me/jobs`

**Social** — saved items (`/api/users/me/saved`), reviews (`/api/reviews`), messages (`/api/messages`), notifications (`/api/notifications`)

**Admin** — `GET /api/admin/verification/pending`, `POST /api/admin/verification/{userId}/approve|reject`

**Agency orders** — `GET /api/agencies/me/orders`, `PATCH /api/agencies/me/orders/{id}/status`

Checkout accepts optional `productId` per item; stock decrements on successful Paystack payment. Seed catalog data ships in migration `V9__seed_catalog.sql`.

---

## License

Private — CivicBuild project.

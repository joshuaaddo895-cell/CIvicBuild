# CivicBuild Backend

Spring Boot REST API for **CivicBuild** — a construction marketplace platform connecting customers, construction agencies, and delivery providers.

This repository contains the backend: authentication, Google Sign-In, onboarding, marketplace catalog, agencies, orders, Paystack payments, messaging, and Cloudinary file storage.

> Note: README updated for clarity by joshuaaddo895-cell (2026-07-28).

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
| `POST` | `/api/orders/checkout` | JWT | Create order + Paystack initialize → `authorizationUrl`, `orderNumber`, `totalAmount` |
| `POST` | `/api/orders/{id}/verify` | JWT | Fallback Paystack verify (webhook is primary) |
| `GET` | `/api/orders/{id}` | JWT | Get order (owner only) |
| `GET` | `/api/orders` | JWT | List my orders |
| `POST` | `/api/payments/webhook` | Public (signed) | Paystack webhook |
| `GET` | `/api/payments/paystack/callback` | Public | Post-checkout redirect (not confirmation) |

### Users & profile

| Method | Path | Auth | Description |
|---|---|---|---|
| `GET` | `/api/users/me` | JWT | Current user profile |
| `PATCH` | `/api/users/me` | JWT | Update `fullName`, `profilePictureUrl` |
| `POST` | `/api/users/me/avatar` | JWT | Upload profile photo (multipart `file`) |
| `DELETE` | `/api/account` | JWT | Delete account + cascade |

### Onboarding

| Method | Path | Auth | Description |
|---|---|---|---|
| `GET` | `/api/users/me/onboarding` | JWT | Account type, completion, `managedAgencyId`, delivery profile |
| `PATCH` | `/api/users/me/onboarding` | JWT | Set `accountType`: `customer` \| `construction` \| `delivery` |
| `POST` | `/api/users/me/onboarding/complete` | JWT | Mark onboarding complete |

### Catalog (public read)

| Method | Path | Auth | Description |
|---|---|---|---|
| `GET` | `/api/categories` | Public | Product categories (`cement`, `blocks`, …) |
| `GET` | `/api/suppliers` | Public | Supplier directory (`?q=&category=&page=&limit=`) — omit `q` to list all |
| `GET` | `/api/suppliers/{id}` | Public | Supplier detail |
| `GET` | `/api/products` | Public | Product listing (`?q=&category=&supplierId=&agencyId=&page=&limit=`) |
| `GET` | `/api/products/{id}` | Public | Product detail |

> **Catalog search:** Optional `q` uses case-insensitive name search. Omit `q` entirely (do not send empty string). Fixed in commit `784afba` (PostgreSQL `lower(bytea)` issue).

### Agencies

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/api/agencies` | JWT | Create agency (construction user) |
| `GET` | `/api/agencies/me` | JWT | My agency profile |
| `PATCH` | `/api/agencies/me` | JWT | Update agency |
| `GET` | `/api/agencies` | Public | Agency directory |
| `GET` | `/api/agencies/{id}` | Public | Agency public detail |
| `POST` | `/api/agencies/me/products` | JWT | Create agency product |
| `PATCH` | `/api/agencies/me/products/{id}` | JWT | Update product |
| `DELETE` | `/api/agencies/me/products/{id}` | JWT | Delete product |
| `POST` | `/api/agencies/me/products/upload-image` | JWT | Product image upload |
| `GET` | `/api/agencies/me/posts` | JWT | My posts |
| `POST` | `/api/agencies/me/posts` | JWT | Create post |
| `GET` | `/api/agencies/{id}/posts` | Public | Agency posts |
| `GET` | `/api/agencies/me/portfolio` | JWT | My portfolio images |
| `GET` | `/api/agencies/{id}/portfolio` | Public | Agency portfolio |
| `GET` | `/api/agencies/me/personnel` | JWT | Delivery personnel requests |
| `GET` | `/api/agencies/me/orders` | JWT | Orders containing my products |

### Delivery providers

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/api/delivery-providers/setup` | JWT | Create/update delivery profile |
| `GET` | `/api/delivery-providers/me` | JWT | My delivery profile |
| `PATCH` | `/api/delivery-providers/me` | JWT | Update profile |
| `DELETE` | `/api/delivery-providers/me/association` | JWT | Leave agency |
| `GET` | `/api/delivery-providers/me/jobs` | JWT | Assigned delivery jobs |

### Verification

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/api/verification/upload-document` | JWT | Upload verification doc (multipart `file`) |
| `GET` | `/api/verification/{userId}/document-url` | JWT | Signed private doc URL |

### Saved items

| Method | Path | Auth | Description |
|---|---|---|---|
| `GET` | `/api/users/me/saved` | JWT | List saved products, suppliers, agencies |
| `POST` | `/api/users/me/saved` | JWT | Save item `{ id, type }` — types: `product`, `supplier`, `agency` |
| `DELETE` | `/api/users/me/saved/{type}/{id}` | JWT | Remove saved item |

### Reviews

| Method | Path | Auth | Description |
|---|---|---|---|
| `GET` | `/api/reviews` | Public | List reviews (`?subjectType=product&subjectId=`) |
| `GET` | `/api/reviews/summary` | Public | Average rating + count |
| `GET` | `/api/reviews/me` | JWT | My reviews |
| `POST` | `/api/reviews` | JWT | Create review |
| `PATCH` | `/api/reviews/{id}` | JWT | Update own review |
| `DELETE` | `/api/reviews/{id}` | JWT | Delete own review |

### Messaging

| Method | Path | Auth | Description |
|---|---|---|---|
| `GET` | `/api/messages/threads` | JWT | List threads |
| `POST` | `/api/messages/threads` | JWT | Start thread `{ agencyId }` (agency-only) |
| `GET` | `/api/messages/threads/{id}` | JWT | List messages in thread |
| `POST` | `/api/messages/threads/{id}/messages` | JWT | Send message `{ text }` |
| `PATCH` | `/api/messages/threads/{id}/read` | JWT | Mark thread read |

### Notifications

| Method | Path | Auth | Description |
|---|---|---|---|
| `GET` | `/api/notifications` | JWT | List notifications |
| `PATCH` | `/api/notifications/{id}/read` | JWT | Mark one read |
| `PATCH` | `/api/notifications/read-all` | JWT | Mark all read |

### Agency orders

| Method | Path | Auth | Description |
|---|---|---|---|
| `GET` | `/api/agencies/me/orders` | JWT | Orders containing my products |
| `GET` | `/api/agencies/me/orders/{id}` | JWT | Order detail |
| `PATCH` | `/api/agencies/me/orders/{id}/status` | JWT | Update status (`?status=processing`) |

### Admin

| Method | Path | Auth | Description |
|---|---|---|---|
| `GET` | `/api/admin/verification/pending` | JWT (`ADMIN`) | Pending verifications |
| `PATCH` | `/api/admin/verification/{userId}/approve` | JWT (`ADMIN`) | Approve user |
| `PATCH` | `/api/admin/verification/{userId}/reject` | JWT (`ADMIN`) | Reject user |

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

**Auth response** (login / google / refresh):

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

**Onboarding** `PATCH /api/users/me/onboarding`

```json
{ "accountType": "construction" }
```

Values: `customer`, `construction`, `delivery`.

**Create agency** `POST /api/agencies`

```json
{
  "name": "BuildStrong Ltd",
  "category": "general-contracting",
  "tagline": "Quality construction",
  "services": ["renovation", "new builds"]
}
```

**Checkout** `POST /api/orders/checkout`

```json
{
  "items": [
    {
      "productId": "b2000001-0000-4000-8000-000000000001",
      "productName": "Dangote Cement 50kg",
      "supplierName": "BuildMart Ghana",
      "unitPrice": 88,
      "quantity": 2,
      "unit": "per bag"
    }
  ],
  "delivery": {
    "address": "12 Market Road",
    "city": "Accra",
    "region": "Greater Accra",
    "phoneNumber": "+233201234567"
  }
}
```

`productId` is optional (backward compatible). When provided, server validates price/stock from catalog.

Returns:

```json
{
  "success": true,
  "data": {
    "orderId": "<uuid>",
    "orderNumber": "CB-<uuid>",
    "paystackReference": "CB-<uuid>",
    "authorizationUrl": "https://checkout.paystack.com/...",
    "totalAmount": 176
  }
}
```

---

## Postman Collection

Import **`postman/CivicBuild-API.postman_collection.json`** (full API — 21 folders, 75+ requests).

Legacy auth-only collection: `postman/CivicBuild-Auth-API.postman_collection.json`

| Variable | Default | Use |
|----------|---------|-----|
| `baseUrl` | `https://civicbuild-production.up.railway.app` | Production (Railway) |
| `baseUrlLocal` | `http://localhost:8081` | Docker / local Maven |
| `accessToken` | (auto-set by Login) | Bearer token for protected routes |
| `productId` | `b2000001-0000-4000-8000-000000000001` | Dangote Cement — checkout / reviews / saved |
| `supplierId` | `a1000001-0000-4000-8000-000000000001` | BuildMart Ghana |
| `agencyId` | (auto-set by Create Agency) | Agency-scoped routes |
| `reviewId` | (auto-set by Create Review) | Update / delete review |
| `notificationId` | (from List Notifications) | Mark notification read |
| `threadId` | (auto-set by Start Thread) | Messages |

To test locally, set `baseUrl` to `http://localhost:8081`.

**Smoke (no auth):** App Health → List Products → List Suppliers → Get Product

**Suggested flows:**
1. **Customer:** Register → Login → List Products → Save Item → Checkout → Verify Payment → List Orders
2. **Agency:** Login → Patch onboarding `construction` → Create Agency → Create Post → Upload Portfolio → Create Product → List Agency Orders
3. **Delivery:** Login → Setup delivery profile → Agency approves personnel → List jobs
4. **Social:** Start Thread → Send Message → Create Review → List Notifications → Mark All Read

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

## Frontend integration

The **CivicBuild Expo** app (`CivicBuild-Frontend`, local path `/Users/mac/Pictures/CivicBuildFrontend`) is wired to this API as of commit **`c3faf82`**. Mock stores and constants were removed; all marketplace, agency, delivery, and social screens call the live backend.

| Document | Purpose |
|----------|---------|
| **[docs/FRONTEND_API_REFERENCE.md](docs/FRONTEND_API_REFERENCE.md)** | **Postman-style API reference** — endpoints, bodies, live Railway examples |
| **[docs/FRONTEND_INTEGRATION_PROMPT.md](docs/FRONTEND_INTEGRATION_PROMPT.md)** | Quick setup + copy-paste test calls |
| `docs/FRONTEND_MASTER_INTEGRATION_PROMPT.md` | Agent prompt for validation only |

Set `EXPO_PUBLIC_API_URL=https://civicbuild-production.up.railway.app` (or `http://localhost:8081` for local backend).

**Not on backend (do not implement in frontend):** `verify-email`, `resend-verification`, supplier message threads, WebSocket messaging.

---

## Project Structure

```
src/main/java/backend/example/civicbuild/
├── auth/           JWT auth, profile, avatar
├── onboarding/     Account type + completion persistence
├── agency/         Agencies, posts, portfolio, personnel
├── catalog/        Categories, suppliers, products
├── delivery/       Delivery providers + jobs
├── order/          Checkout, agency orders, stock
├── payment/        Paystack webhook + reconciliation
├── verification/   Private document upload
├── saved/          Favorites
├── review/         Product/supplier reviews
├── messaging/      Customer ↔ agency threads
├── notification/   In-app notifications
├── admin/          Verification review (ADMIN)
├── storage/        Cloudinary abstraction
├── email/          Resend transactional email
├── ratelimit/      Redis-backed rate limiter
├── common/         ApiResponse, pagination, errors
└── config/         AppProperties, datasource, Dotenv

src/main/resources/db/migration/   Flyway V1–V9
postman/                             Postman collections
docs/                                Frontend integration guides
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

Seed catalog data (5 products, 3 suppliers) ships in `V9__seed_catalog.sql`. Checkout accepts optional `productId`; stock decrements on successful Paystack payment.

---

## License

Private — CivicBuild project.

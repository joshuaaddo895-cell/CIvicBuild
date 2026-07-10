# CivicBuild Frontend Integration Prompt

Copy-paste this to your Expo frontend agent or team. The backend at `EXPO_PUBLIC_API_URL` now exposes real APIs for every feature that was previously mock/local-only.

**Production:** `https://civicbuild-production.up.railway.app`  
**Local:** `http://localhost:8081`

---

## Your task

Wire the Expo app to the CivicBuild backend. Replace Zustand/AsyncStorage mocks with API clients under `src/api/`. Keep existing auth/checkout calls working; add new modules below.

---

## API conventions (unchanged)

**Envelope:**
```json
{ "success": true, "message": "...", "data": { }, "errors": null, "timestamp": "..." }
```

**Auth:** `Authorization: Bearer <accessToken>` on protected routes.

**Refresh:** `POST /api/auth/refresh` with `{ "refreshToken": "..." }`.

**Multipart uploads:** field name must be `file`. Max 5MB. Types: PDF/JPG/PNG (verification), JPG/PNG (images).

**Pagination:** list endpoints accept `?page=0&limit=20`. Response:
```json
{ "items": [], "page": 0, "limit": 20, "total": 128, "hasNextPage": true }
```

---

## Migration map: what to replace

| Frontend mock / local store | Replace with |
|---|---|
| `authStore` onboarding (`accountType`, `onboardingComplete`) | `GET/PATCH /api/users/me/onboarding`, `POST .../complete` |
| `managedAgencyId` in auth store | `GET /api/users/me/onboarding` → `data.managedAgencyId` |
| `MOCK_PRODUCTS`, `mockSuppliers` | `GET /api/products`, `GET /api/suppliers`, `GET /api/categories` |
| `VERIFIED_CONSTRUCTION_AGENCIES` | `GET /api/agencies`, `GET /api/agencies/{id}` |
| `agencyPostsStore` | `GET/POST/PATCH/DELETE /api/agencies/me/posts` |
| `agencyPortfolioStore` (local URLs) | `POST /api/agency/portfolio/upload` + `GET /api/agencies/me/portfolio` |
| `deliveryPersonnelStore` | `GET /api/agencies/me/personnel`, delivery `POST /api/delivery-providers/setup` |
| `savedStore` | `GET/POST/DELETE /api/users/me/saved` |
| `mockReviews` | `GET /api/reviews`, `GET /api/reviews/summary` |
| `messagesData` | `GET/POST /api/messages/threads` |
| `MOCK_AGENCY_ORDERS` | `GET /api/agencies/me/orders` |
| Profile `imageUri` as `profilePictureUrl` | `POST /api/users/me/avatar` first, then `PATCH /api/users/me` |

---

## Phase 1 — Onboarding (do first)

### 1.1 After login, hydrate onboarding from server

```
GET /api/users/me/onboarding
```

Response `data`:
```ts
{
  accountType: "customer" | "construction" | "delivery" | null,
  onboardingComplete: boolean,
  verificationStatus: "unverified" | "pending" | "verified" | "rejected",
  managedAgencyId: string | null,          // UUID of owned agency
  deliveryProviderProfile: {
    fullName: string,
    constructionAgencyId: string | null,
    vehicleInfo: string | null,
    profileImageUrl: string | null
  } | null,
  deliveryProviderStatus: "none" | "pending" | "approved" | "rejected"
}
```

**Replace** local `accountType` / `onboardingComplete` reads with this endpoint on app start and after onboarding steps.

### 1.2 Role selection screen

```
PATCH /api/users/me/onboarding
{ "accountType": "customer" | "construction" | "delivery" }
```

This also updates backend `user.role` (`CUSTOMER`, `CONSTRUCTION_AGENCY`, `DELIVERY_PROVIDER`).

### 1.3 Finish onboarding

```
POST /api/users/me/onboarding/complete
```

Call after the last onboarding screen. Do **not** rely only on AsyncStorage.

### 1.4 Construction agency setup

After user picks **construction** and fills business info:

```
POST /api/agencies
{
  "name": "BuildStrong Ltd",
  "category": "general-contracting",
  "tagline": "...",
  "description": "...",
  "address": "...",
  "phone": "+233...",
  "hours": "Mon–Fri 8am–6pm",
  "services": ["renovation", "new builds"]
}
```

Returns `data.id` → store as `managedAgencyId` (also returned by `GET /api/users/me/onboarding`).

### 1.5 Delivery provider setup

```
POST /api/delivery-providers/setup
{
  "fullName": "...",
  "constructionAgencyId": "<uuid from agency picker>",
  "vehicleInfo": "Motorbike - Honda CB125",
  "profileImageUrl": null   // set after avatar upload
}
```

Agency owner sees pending personnel at `GET /api/agencies/me/personnel`.

---

## Phase 2 — Marketplace catalog

### Public (no auth)

```
GET /api/categories
GET /api/suppliers?q=&category=&page=&limit=
GET /api/suppliers/{supplierId}
GET /api/products?q=&category=&supplierId=&agencyId=&page=&limit=
GET /api/products/{productId}
```

**Product shape:**
```ts
{
  id, name, category, price, unit, imageUrl, description,
  supplierId, agencyId, stockQuantity, inStock, brand, spec, deliveryEstimate
}
```

**Seed data** is live in production (5 products, 3 suppliers). Use real UUIDs from API — remove hardcoded IDs like `buildstrong-ltd`.

### Product detail / marketplace screens

Replace `MOCK_PRODUCTS` imports with `productsApi.list()` / `productsApi.get(id)`.

---

## Phase 3 — Agency dashboard

### Agency profile

```
GET  /api/agencies/me
PATCH /api/agencies/me
GET  /api/agencies              // public directory
GET  /api/agencies/{agencyId}   // public detail (AgencyDetailScreen)
```

### Agency products (replaces local product form)

```
POST   /api/agencies/me/products
PATCH  /api/agencies/me/products/{productId}
DELETE /api/agencies/me/products/{productId}
POST   /api/agencies/me/products/upload-image   // multipart file → { imageUrl }
```

Create body:
```json
{
  "name": "Dangote Cement 50kg",
  "category": "cement",
  "price": 88,
  "unit": "per bag",
  "stockQuantity": 100,
  "imageUrl": "https://res.cloudinary.com/...",
  "description": "..."
}
```

**Do not** send `imageUri` (local file path). Upload image first, pass returned CDN URL.

### Agency posts (replaces `agencyPostsStore`)

```
GET    /api/agencies/me/posts
GET    /api/agencies/{agencyId}/posts     // public
POST   /api/agencies/me/posts
PATCH  /api/agencies/me/posts/{postId}
DELETE /api/agencies/me/posts/{postId}
```

Post body:
```json
{ "type": "service" | "material" | "general", "title": "...", "description": "...", "imageUrl": null }
```

### Agency portfolio

```
POST   /api/agency/portfolio/upload        // existing — multipart file
GET    /api/agencies/me/portfolio          // list with fresh deliveryUrl
GET    /api/agencies/{agencyId}/portfolio  // public
DELETE /api/agencies/me/portfolio/{imageId}
```

Upload response: `{ imageId, publicId, resourceType, deliveryUrl }`.  
List response: `{ imageId, publicId, resourceType, deliveryUrl }[]` — **do not** persist URLs long-term; refetch on screen open.

### Agency orders (replaces `MOCK_AGENCY_ORDERS`)

```
GET   /api/agencies/me/orders
GET   /api/agencies/me/orders/{orderId}
PATCH /api/agencies/me/orders/{orderId}/status?status=pending|processing|delivered|cancelled
```

### Personnel (agency approves delivery drivers)

```
GET    /api/agencies/me/personnel
POST   /api/agencies/me/personnel/{personnelId}/approve
POST   /api/agencies/me/personnel/{personnelId}/reject
DELETE /api/agencies/me/personnel/{personnelId}
```

---

## Phase 4 — Customer flows

### Checkout (enhanced)

`POST /api/orders/checkout` — add optional `productId` per item:

```json
{
  "items": [
    {
      "productId": "b2000001-0000-4000-8000-000000000001",
      "productName": "Dangote Cement 50kg",
      "supplierName": "BuildMart Ghana",
      "unitPrice": 88,
      "quantity": 3,
      "unit": "per bag"
    }
  ],
  "delivery": {
    "address": "...",
    "city": "Accra",
    "region": "Greater Accra",
    "phoneNumber": "+233201234567"
  }
}
```

When `productId` is set, server validates price/stock and links order to agency. Stock decrements on successful Paystack payment.

**Response** (updated):
```json
{
  "orderId": "...",
  "orderNumber": "CB-...",
  "paystackReference": "CB-...",
  "authorizationUrl": "https://checkout.paystack.com/...",
  "totalAmount": 264
}
```

### Saved items

```
GET    /api/users/me/saved
POST   /api/users/me/saved     { "id": "<uuid>", "type": "product" | "supplier" | "agency" }
DELETE /api/users/me/saved/{type}/{id}
```

Saved response: `{ id, type, savedAt }`. Resolve IDs against catalog APIs on Saved screen.

### Reviews

```
GET  /api/reviews?subjectType=product|supplier&subjectId=<uuid>
GET  /api/reviews/summary?subjectType=...&subjectId=...
GET  /api/reviews/me
POST /api/reviews
```

---

## Phase 5 — Profile, verification, messaging

### Avatar upload (fixes broken profile photo)

```
POST /api/users/me/avatar     // multipart file
→ { profilePictureUrl: "https://res.cloudinary.com/..." }
```

Then optionally `PATCH /api/users/me` with the returned URL.

### Verification documents (already wired — keep)

```
POST /api/verification/upload-document?documentType=BUSINESS_REGISTRATION|GOVERNMENT_ID|PROFESSIONAL_LICENSE
GET  /api/verification/{userId}/document-url?documentType=...
```

Upload sets `verificationStatus` → `pending` on first doc.

### Messaging

```
GET   /api/messages/threads
POST  /api/messages/threads              { "agencyId": "<uuid>" }
GET   /api/messages/threads/{threadId}
POST  /api/messages/threads/{threadId}/messages   { "text": "..." }
PATCH /api/messages/threads/{threadId}/read
```

### Notifications

```
GET   /api/notifications
PATCH /api/notifications/{id}/read
PATCH /api/notifications/read-all
```

Types: `order`, `verification`, `personnel`, `message`.

### Delivery jobs

```
GET   /api/delivery-providers/me/jobs
PATCH /api/delivery-providers/me/jobs/{jobId}/status?status=assigned|in_transit|delivered
```

---

## Suggested `src/api/` layout

```
src/api/
  client.ts           // fetch wrapper, Bearer token, refresh on 401
  auth.ts             // existing
  users.ts            // me, avatar
  onboarding.ts       // NEW
  agencies.ts         // NEW
  catalog.ts          // categories, suppliers, products
  orders.ts           // existing + agency orders
  delivery.ts         // NEW
  verification.ts     // existing
  portfolio.ts        // upload + list
  saved.ts            // NEW
  reviews.ts          // NEW
  messages.ts         // NEW
  notifications.ts    // NEW
```

---

## Store migration checklist

- [ ] On app launch (authenticated): `GET /api/users/me` + `GET /api/users/me/onboarding`
- [ ] Role selection → `PATCH /api/users/me/onboarding` (not local only)
- [ ] Construction onboarding → `POST /api/agencies` then `POST .../onboarding/complete`
- [ ] Delivery onboarding → `POST /api/delivery-providers/setup` then complete
- [ ] Marketplace home → `GET /api/categories` + `GET /api/products`
- [ ] Product detail → `GET /api/products/{id}` + `GET /api/reviews?...`
- [ ] Agency dashboard products → agency product CRUD APIs
- [ ] Agency posts screen → `/api/agencies/me/posts`
- [ ] Portfolio screen → upload + `GET /api/agencies/me/portfolio`
- [ ] Cart checkout → pass `productId` when available
- [ ] Saved screen → `GET /api/users/me/saved` + catalog resolve
- [ ] Messages → threads API
- [ ] Edit profile photo → `POST /api/users/me/avatar`
- [ ] Remove all `MOCK_*` constants once screens work against API

---

## Error handling

| Status | Meaning |
|---|---|
| 400 | Validation failed — check `errors[]` |
| 401 | Token expired — refresh or re-login |
| 403 | Wrong role (e.g. agency endpoint as customer) |
| 404 | Resource not found |
| 409 | Conflict (e.g. agency already exists) |
| 502 | Cloudinary upload failed |

---

## Testing

Import `postman/CivicBuild-API.postman_collection.json` for all endpoints.

**Quick smoke after deploy:**
```bash
curl https://civicbuild-production.up.railway.app/api/categories
curl https://civicbuild-production.up.railway.app/api/products?limit=3
```

---

## Not yet on backend (do not wire)

- `POST /api/auth/verify-email`
- `POST /api/auth/resend-verification`
- WebSocket real-time messaging (use REST + poll for now)

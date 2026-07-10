# CivicBuild API — Frontend Consumer Reference (Postman-style)

**Base URL (Railway):** `https://civicbuild-production.up.railway.app`  
**Env var in Expo:** `EXPO_PUBLIC_API_URL`  
**Auth header:** `Authorization: Bearer <accessToken>`

Every response uses this envelope:

```json
{
  "success": true,
  "message": "optional human-readable string",
  "data": {},
  "errors": [{ "field": "email", "message": "must be a valid email" }],
  "timestamp": "2026-07-10T18:26:07.553214475Z"
}
```

On failure: `success: false`, HTTP 4xx/5xx, `message` + optional `errors[]`.

---

## Quick start sequence

```
1. POST /api/auth/register        → 201, no token
2. POST /api/auth/login           → save accessToken + refreshToken
3. GET  /api/users/me/onboarding  → accountType, managedAgencyId
4. GET  /api/products             → browse catalog (no auth)
5. POST /api/users/me/saved       → save product (auth)
6. POST /api/orders/checkout      → Paystack URL (auth)
```

**Postman collection:** `postman/CivicBuild-API.postman_collection.json`  
Import it — `Login` auto-saves `accessToken`. Seed IDs are pre-filled.

---

## 1. Health

### GET `/api/health`
**Auth:** none

**Example response (live Railway):**
```json
{
  "success": true,
  "data": { "status": "UP" },
  "timestamp": "2026-07-10T18:26:07.553214475Z"
}
```

**Frontend:** use on app launch / settings to confirm API reachability.

---

## 2. Auth

### POST `/api/auth/register`
**Auth:** none | **Rate limit:** yes

**Request:**
```json
{
  "fullName": "Jane Doe",
  "email": "jane@example.com",
  "password": "Secret123"
}
```
Password: min 8 chars, ≥1 letter + ≥1 digit. Do **not** send `confirmPassword`.

**Response `201`:**
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "fullName": "Jane Doe",
    "email": "jane@example.com",
    "role": "CUSTOMER",
    "verificationStatus": "UNVERIFIED",
    "active": true,
    "profilePictureUrl": null,
    "createdAt": "2026-07-10T18:00:00Z"
  }
}
```
No tokens — user must login next.

---

### POST `/api/auth/login`
**Auth:** none | **Rate limit:** yes

**Request:**
```json
{ "email": "jane@example.com", "password": "Secret123" }
```

**Response `200`:**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJ...",
    "refreshToken": "opaque-string",
    "tokenType": "Bearer",
    "expiresIn": 900
  }
}
```

**Frontend:** `src/api/auth.ts` → store tokens in SecureStore; attach Bearer on all protected calls.

---

### POST `/api/auth/google`
**Request:** `{ "idToken": "<Google ID token>" }`  
**Response:** same `AuthResponse` as login.

---

### POST `/api/auth/refresh`
**Request:** `{ "refreshToken": "<opaque>" }`  
**Response:** new `AuthResponse` (rotation — old refresh token revoked).

---

### POST `/api/auth/logout`
**Request:** `{ "refreshToken": "<opaque>" }`  
**Response:** `{ "success": true, "message": "Logged out" }`

---

### POST `/api/auth/change-password` 🔒
**Request:**
```json
{ "currentPassword": "Secret123", "newPassword": "NewSecret456" }
```

---

### POST `/api/auth/forgot-password`
**Request:** `{ "email": "jane@example.com" }`  
Always returns 200 (no email enumeration).

---

### POST `/api/auth/reset-password`
**Request:** `{ "token": "<from email>", "newPassword": "NewSecret456" }`

---

## 3. Users & account

### GET `/api/users/me` 🔒
**Response `data`:**
```json
{
  "id": "uuid",
  "fullName": "Jane Doe",
  "email": "jane@example.com",
  "role": "CUSTOMER",
  "verificationStatus": "UNVERIFIED",
  "active": true,
  "profilePictureUrl": null,
  "createdAt": "2026-07-10T18:00:00Z"
}
```
**Roles:** `CUSTOMER` | `CONSTRUCTION_AGENCY` | `DELIVERY_PROVIDER` | `ADMIN`

---

### PATCH `/api/users/me` 🔒
**Request:**
```json
{
  "fullName": "Jane Doe",
  "profilePictureUrl": "https://res.cloudinary.com/..."
}
```
`profilePictureUrl` is optional. Upload avatar first (below), then PATCH with returned URL.

---

### POST `/api/users/me/avatar` 🔒
**Content-Type:** `multipart/form-data`  
**Field:** `file` (JPEG/PNG, max 5MB)

**Response:**
```json
{ "success": true, "data": { "profilePictureUrl": "https://res.cloudinary.com/..." } }
```

**Frontend flow:**
1. `uploadAvatar(file)` → get URL
2. `updateProfile({ fullName, profilePictureUrl })`

---

### DELETE `/api/account` 🔒
Deletes account and all related data.

---

## 4. Onboarding

### GET `/api/users/me/onboarding` 🔒
**Response `data`:**
```json
{
  "accountType": "customer",
  "onboardingComplete": false,
  "verificationStatus": "unverified",
  "managedAgencyId": null,
  "deliveryProviderProfile": null,
  "deliveryProviderStatus": "none"
}
```
`accountType`: `customer` | `construction` | `delivery` | `null`  
`deliveryProviderStatus`: `pending` | `approved` | `rejected` | `none`

**Frontend:** always call after login (`syncOnboardingFromServer`). Never hardcode `managedAgencyId`.

---

### PATCH `/api/users/me/onboarding` 🔒
**Request:** `{ "accountType": "construction" }`  
Sets user role: `customer`→CUSTOMER, `construction`→CONSTRUCTION_AGENCY, `delivery`→DELIVERY_PROVIDER.

---

### POST `/api/users/me/onboarding/complete` 🔒
Marks onboarding done. Requires `accountType` set first.

---

## 5. Catalog (public)

### GET `/api/categories`
**Example response (live Railway):**
```json
{
  "success": true,
  "data": [
    { "id": "cement", "name": "Cement" },
    { "id": "blocks", "name": "Blocks" },
    { "id": "steel", "name": "Steel" }
  ]
}
```

---

### GET `/api/products`
**Query params:**

| Param | Type | Notes |
|-------|------|-------|
| `page` | int | default `0` |
| `limit` | int | default `20`, max `100` |
| `q` | string | search by name — **omit entirely to list all** |
| `category` | string | e.g. `cement` |
| `supplierId` | UUID | filter by supplier |
| `agencyId` | UUID | filter by agency |

**Example:** `GET /api/products?page=0&limit=20`  
**Example:** `GET /api/products?q=cement&page=0&limit=20`

**Response `data` (live Railway):**
```json
{
  "items": [
    {
      "id": "b2000001-0000-4000-8000-000000000001",
      "name": "Dangote Cement 50kg",
      "category": "cement",
      "price": 88.0,
      "unit": "per bag",
      "imageUrl": null,
      "description": "Premium Portland cement for all construction needs.",
      "supplierId": "a1000001-0000-4000-8000-000000000001",
      "agencyId": null,
      "stockQuantity": 500,
      "inStock": true,
      "brand": "Dangote",
      "spec": "50kg bag",
      "deliveryEstimate": "Same day"
    }
  ],
  "page": 0,
  "limit": 20,
  "total": 5,
  "hasNextPage": false
}
```

**Frontend:** `src/api/catalog.ts` → `getProducts()`, `getProduct(id)`

---

### GET `/api/products/{productId}`
**Seed ID:** `b2000001-0000-4000-8000-000000000001` (Dangote Cement, 88 GHS)

---

### GET `/api/suppliers`
**Query:** `q`, `category`, `page`, `limit` (same pagination rules)

**Response `data.items[]` (live Railway):**
```json
{
  "id": "a1000001-0000-4000-8000-000000000001",
  "name": "BuildMart Ghana",
  "logoUrl": null,
  "rating": 4.7,
  "reviewCount": 128,
  "distanceKm": 2.4,
  "verified": true,
  "categoryId": "cement"
}
```

**Seed ID:** `a1000001-0000-4000-8000-000000000001`

---

### GET `/api/suppliers/{supplierId}`
Single `SupplierResponse`.

---

## 6. Agencies

### POST `/api/agencies` 🔒 (role: CONSTRUCTION_AGENCY)
**Request:**
```json
{
  "name": "BuildStrong Ltd",
  "category": "general-contracting",
  "tagline": "Quality construction",
  "description": "Full-service builder",
  "address": "12 Ring Road, Accra",
  "phone": "+233201234567",
  "hours": "Mon–Sat 8am–6pm",
  "services": ["renovation", "new builds"]
}
```
Only `name` + `category` required.

**Response `201` `data`:**
```json
{
  "id": "uuid",
  "name": "BuildStrong Ltd",
  "logoUrl": null,
  "verified": false,
  "tagline": "Quality construction",
  "description": "Full-service builder",
  "address": "12 Ring Road, Accra",
  "phone": "+233201234567",
  "hours": "Mon–Sat 8am–6pm",
  "services": ["renovation", "new builds"],
  "category": "general-contracting"
}
```
One agency per owner. `managedAgencyId` appears in onboarding after create.

---

### GET `/api/agencies` (public)
**Query:** `q`, `page`, `limit`  
Currently empty on Railway until agencies are created.

---

### GET `/api/agencies/{agencyId}` (public)
Public agency profile.

---

### GET `/api/agencies/me` 🔒
Own agency profile.

---

### PATCH `/api/agencies/me` 🔒
Partial update — all fields optional (`name`, `category`, `logoUrl`, `tagline`, etc.)

---

### Agency posts

| Method | Path | Auth |
|--------|------|------|
| GET | `/api/agencies/me/posts?page=0&limit=20` | 🔒 agency |
| GET | `/api/agencies/{agencyId}/posts?page=0&limit=20` | public |
| POST | `/api/agencies/me/posts` | 🔒 agency |
| PATCH | `/api/agencies/me/posts/{postId}` | 🔒 agency |
| DELETE | `/api/agencies/me/posts/{postId}` | 🔒 agency |

**POST body:**
```json
{
  "type": "service",
  "title": "Foundation work available",
  "description": "We handle residential foundations",
  "imageUrl": "https://res.cloudinary.com/..."
}
```
`type`: `service` | `material` | `general`

**Response `data`:**
```json
{
  "id": "uuid",
  "agencyId": "uuid",
  "type": "service",
  "title": "Foundation work available",
  "description": "We handle residential foundations",
  "imageUrl": null,
  "createdAt": "2026-07-10T18:00:00Z",
  "updatedAt": "2026-07-10T18:00:00Z"
}
```

---

### Agency portfolio

| Method | Path | Auth |
|--------|------|------|
| GET | `/api/agencies/me/portfolio` | 🔒 agency |
| GET | `/api/agencies/{agencyId}/portfolio` | public |
| POST | `/api/agency/portfolio/upload` | 🔒 agency, multipart `file` |
| DELETE | `/api/agencies/me/portfolio/{imageId}` | 🔒 agency |

**Upload response:**
```json
{
  "imageId": "uuid",
  "publicId": "civicbuild/...",
  "resourceType": "image",
  "deliveryUrl": "https://res.cloudinary.com/..."
}
```

---

### Agency personnel (delivery approvals)

| Method | Path |
|--------|------|
| GET | `/api/agencies/me/personnel` |
| POST | `/api/agencies/me/personnel/{personnelId}/approve` |
| POST | `/api/agencies/me/personnel/{personnelId}/reject` |
| DELETE | `/api/agencies/me/personnel/{personnelId}` |

**Personnel item:**
```json
{
  "id": "uuid",
  "userId": "uuid",
  "fullName": "Kwame Mensah",
  "profileImageUrl": null,
  "constructionAgencyId": "uuid",
  "vehicleInfo": "Toyota Hilux",
  "approvalStatus": "pending",
  "submittedAt": "2026-07-10T18:00:00Z",
  "handledAt": null
}
```

---

### Agency products (catalog CRUD)

| Method | Path |
|--------|------|
| POST | `/api/agencies/me/products` |
| PATCH | `/api/agencies/me/products/{productId}` |
| DELETE | `/api/agencies/me/products/{productId}` |
| POST | `/api/agencies/me/products/upload-image` (multipart `file`) |

**Create product:**
```json
{
  "name": "Premium Blocks",
  "category": "blocks",
  "price": 12.5,
  "unit": "per piece",
  "stockQuantity": 1000,
  "imageUrl": "https://res.cloudinary.com/...",
  "description": "6-inch solid blocks",
  "brand": "BuildStrong",
  "spec": "6x9x18",
  "deliveryEstimate": "2 days"
}
```

---

## 7. Agency orders

| Method | Path | Auth |
|--------|------|------|
| GET | `/api/agencies/me/orders` | 🔒 agency |
| GET | `/api/agencies/me/orders/{orderId}` | 🔒 agency |
| PATCH | `/api/agencies/me/orders/{orderId}/status?status=processing` | 🔒 agency |

**Status values:** `pending` | `processing` | `delivered` | `cancelled`

**Order item:**
```json
{
  "id": "uuid",
  "orderNumber": "CB-uuid",
  "customerId": "uuid",
  "customerName": "Jane Doe",
  "customerEmail": "jane@example.com",
  "customerPhone": "+233201234567",
  "agencyId": "uuid",
  "orderDate": "2026-07-10T18:00:00Z",
  "status": "pending",
  "deliveryAddress": "12 Market Rd, Accra",
  "totalAmount": 176.0,
  "items": [
    {
      "productId": "b2000001-0000-4000-8000-000000000001",
      "productName": "Dangote Cement 50kg",
      "quantity": 2,
      "unitPrice": 88.0,
      "unit": "per bag"
    }
  ]
}
```

**Frontend:** `src/api/agencyOrders.ts`

---

## 8. Delivery providers

### POST `/api/delivery-providers/setup` 🔒 (role: DELIVERY_PROVIDER)
**Request:**
```json
{
  "fullName": "Kwame Mensah",
  "constructionAgencyId": "agency-uuid",
  "vehicleInfo": "Toyota Hilux",
  "profileImageUrl": "https://res.cloudinary.com/..."
}
```

**Response `201`:**
```json
{
  "id": "uuid",
  "userId": "uuid",
  "fullName": "Kwame Mensah",
  "constructionAgencyId": "agency-uuid",
  "vehicleInfo": "Toyota Hilux",
  "profileImageUrl": "https://...",
  "approvalStatus": "pending",
  "submittedAt": "2026-07-10T18:00:00Z",
  "handledAt": null
}
```

---

### GET `/api/delivery-providers/me` 🔒
### PATCH `/api/delivery-providers/me` 🔒 (same body shape as setup)
### DELETE `/api/delivery-providers/me/association` 🔒

### GET `/api/delivery-providers/me/jobs` 🔒
```json
[{
  "id": "uuid",
  "orderId": "uuid",
  "orderNumber": "CB-uuid",
  "pickupAddress": null,
  "deliveryAddress": "12 Market Rd, Accra",
  "status": "assigned",
  "assignedAt": "2026-07-10T18:00:00Z"
}]
```

### PATCH `/api/delivery-providers/me/jobs/{jobId}/status?status=in_transit` 🔒
Status: `assigned` | `in_transit` | `delivered`

---

## 9. Customer orders & checkout

### POST `/api/orders/checkout` 🔒
**Request:**
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
Include `productId` — server validates price/stock from catalog.

**Response `201`:**
```json
{
  "orderId": "uuid",
  "orderNumber": "CB-uuid",
  "paystackReference": "CB-uuid",
  "authorizationUrl": "https://checkout.paystack.com/...",
  "totalAmount": 176.0
}
```
Open `authorizationUrl` in WebView/browser for payment.

---

### POST `/api/orders/{id}/verify` 🔒
Call after Paystack redirect. Server verifies with Paystack.

**Response `data` order status:** `PENDING` | `PROCESSING` | `PAID` | `FAILED` | `REFUNDED`

---

### GET `/api/orders` 🔒
List my orders (newest first, no pagination).

### GET `/api/orders/{id}` 🔒
Single order detail.

---

## 10. Saved items

### GET `/api/users/me/saved` 🔒
```json
[{ "id": "b2000001-0000-4000-8000-000000000001", "type": "product", "savedAt": "2026-07-10T18:00:00Z" }]
```

### POST `/api/users/me/saved` 🔒
```json
{ "id": "b2000001-0000-4000-8000-000000000001", "type": "product" }
```
`type`: `product` | `supplier` | `agency` — idempotent.

### DELETE `/api/users/me/saved/{type}/{id}` 🔒
Example: `DELETE /api/users/me/saved/product/b2000001-0000-4000-8000-000000000001`

**Frontend:** `src/api/saved.ts` — sync on login, no AsyncStorage persistence.

---

## 11. Reviews

### GET `/api/reviews?subjectType=product&subjectId={uuid}` (public)
Both query params required. `subjectType`: `product` | `supplier`

### GET `/api/reviews/summary?subjectType=product&subjectId={uuid}` (public)
**Live Railway:**
```json
{ "averageRating": 0.0, "totalCount": 0, "breakdown": [] }
```

### GET `/api/reviews/me` 🔒

### POST `/api/reviews` 🔒
```json
{
  "subjectType": "product",
  "subjectId": "b2000001-0000-4000-8000-000000000001",
  "rating": 5,
  "text": "Great quality cement",
  "verifiedPurchase": false,
  "orderNumber": null
}
```
`rating`: 1–5. Returns `201`.

### PATCH `/api/reviews/{reviewId}` 🔒
```json
{ "rating": 4, "text": "Updated review" }
```

### DELETE `/api/reviews/{reviewId}` 🔒

---

## 12. Messages

**Important:** threads are **agency-only**. No supplier threads.

### GET `/api/messages/threads` 🔒
```json
[{
  "id": "uuid",
  "participantName": "BuildStrong Ltd",
  "participantLogoUrl": null,
  "lastMessage": "Hello",
  "lastMessageAt": "2026-07-10T18:00:00Z",
  "unreadCount": 1
}]
```

### POST `/api/messages/threads` 🔒
```json
{ "agencyId": "agency-uuid" }
```
Returns `201`. Idempotent — returns existing thread if already started.

### GET `/api/messages/threads/{threadId}` 🔒
```json
[{
  "id": "uuid",
  "threadId": "uuid",
  "text": "I need a quote",
  "sentAt": "2026-07-10T18:00:00Z",
  "isOutgoing": true
}]
```

### POST `/api/messages/threads/{threadId}/messages` 🔒
```json
{ "text": "I need a quote for foundation work" }
```

### PATCH `/api/messages/threads/{threadId}/read` 🔒

---

## 13. Notifications

### GET `/api/notifications` 🔒
```json
[{
  "id": "uuid",
  "type": "order",
  "title": "New order",
  "body": "Order CB-xxx received",
  "read": false,
  "createdAt": "2026-07-10T18:00:00Z",
  "data": { "orderId": "uuid" }
}]
```
`type`: `order` | `verification` | `personnel` | `message`

### PATCH `/api/notifications/{id}/read` 🔒
### PATCH `/api/notifications/read-all` 🔒

---

## 14. Verification documents

### POST `/api/verification/upload-document?documentType=BUSINESS_REGISTRATION` 🔒
**Content-Type:** `multipart/form-data`  
**Field:** `file` (PDF/JPG/PNG, max 5MB)

**documentType:** `BUSINESS_REGISTRATION` | `GOVERNMENT_ID` | `PROFESSIONAL_LICENSE`

**Response:**
```json
{
  "documentId": "uuid",
  "documentType": "BUSINESS_REGISTRATION",
  "publicId": "civicbuild/...",
  "resourceType": "raw"
}
```
Sets user `verificationStatus` → `PENDING`.

### GET `/api/verification/{userId}/document-url?documentType=BUSINESS_REGISTRATION` 🔒
Owner or ADMIN only. Signed URL expires in 5 minutes.

---

## 15. Error examples

**Validation error `400`:**
```json
{
  "success": false,
  "message": "Validation failed",
  "errors": [
    { "field": "password", "message": "must contain at least one letter and one number" }
  ],
  "timestamp": "..."
}
```

**Unauthorized `401`:**
```json
{ "success": false, "message": "Unauthorized", "data": null }
```
→ refresh token or redirect to login.

**Forbidden `403`:**
```json
{ "success": false, "message": "Access denied" }
```
→ wrong role (e.g. customer hitting agency endpoint).

---

## 16. Seed data on Railway

| Entity | UUID | Price |
|--------|------|-------|
| Dangote Cement 50kg | `b2000001-0000-4000-8000-000000000001` | 88 GHS |
| Iron Rods 12mm | `b2000001-0000-4000-8000-000000000002` | 45 GHS |
| BuildMart Ghana | `a1000001-0000-4000-8000-000000000001` | — |
| Steel & More Ltd | `a1000001-0000-4000-8000-000000000002` | — |

---

## 17. Frontend file map

| API area | Expo module |
|----------|-------------|
| Auth | `src/api/auth.ts` |
| Profile / avatar | `src/api/users.ts` |
| Onboarding | `src/api/onboarding.ts` |
| Catalog | `src/api/catalog.ts` |
| Agencies | `src/api/agencies.ts` |
| Agency orders | `src/api/agencyOrders.ts` |
| Portfolio upload | `src/api/agencyPortfolio.ts` |
| Checkout / orders | `src/api/checkoutService.ts`, `src/api/orders.ts` |
| Delivery | `src/api/delivery.ts` |
| Saved | `src/api/saved.ts` |
| Reviews | `src/api/reviews.ts` |
| Messages | `src/api/messages.ts` |
| Notifications | `src/api/notifications.ts` |
| Verification | `src/api/verification.ts` |
| HTTP client | `src/api/client.ts` |
| Response unwrap | `src/api/authTypes.ts`, `src/api/apiResult.ts` |
| Checkout mapper | `src/utils/orderMappers.ts` |

---

## 18. Not implemented on backend

Do not build UI for these — they will 404:

- `POST /api/auth/verify-email`
- `POST /api/auth/resend-verification`
- Supplier message threads
- WebSocket messaging

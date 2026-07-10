# CivicBuild Expo Frontend — Complete Backend Integration Prompt

You are wiring the **CivicBuild** Expo React Native app to the production Spring Boot API. Replace every mock constant, Zustand persisted store, and AsyncStorage-only state with real HTTP calls. Implement `src/api/*` clients and update screens to use them.

---

## 0. Environment

```
EXPO_PUBLIC_API_URL=https://civicbuild-production.up.railway.app
# Local dev:
EXPO_PUBLIC_API_URL=http://localhost:8081
```

All paths below are relative to this base. Currency is **GHS**. Phone numbers should use Ghana format: `+233XXXXXXXXX`.

---

## 1. Global API contract

### 1.1 Response envelope (every endpoint)

**Success:**
```json
{
  "success": true,
  "message": "Human-readable message or null",
  "data": { },
  "errors": null,
  "timestamp": "2026-07-10T15:00:00Z"
}
```

**Error:**
```json
{
  "success": false,
  "message": "Human-readable error",
  "errors": [
    { "field": "email", "message": "Email must be a valid email address" }
  ],
  "timestamp": "2026-07-10T15:00:00Z"
}
```

`errors` is only present on validation failures (400). Other errors have `errors: null`.

### 1.2 Authentication

- Protected routes: header `Authorization: Bearer <accessToken>`
- Access token TTL: **900 seconds (15 min)**
- On **401**: call refresh once, retry original request; if refresh fails → logout + navigate to Sign In
- Refresh: `POST /api/auth/refresh` with `{ "refreshToken": "..." }` — returns new access + refresh pair (rotation: old refresh token is invalidated)

### 1.3 Pagination

List endpoints accept `?page=0&limit=20` (0-indexed page, default limit 20, max 100).

Paginated `data` shape:
```json
{
  "items": [],
  "page": 0,
  "limit": 20,
  "total": 128,
  "hasNextPage": true
}
```

Non-paginated list endpoints return `data` as a JSON array directly.

### 1.4 Multipart uploads

- Field name: **`file`** (required)
- Max size: **5 MB**
- Allowed types:
  - Verification docs: **PDF, JPG, PNG** (magic-byte validated)
  - Portfolio / avatar / product images: **JPG, PNG only**
- Never send local `file://` URIs in JSON body fields — upload first, then pass returned CDN URL

### 1.5 HTTP status codes

| Code | Meaning | Frontend action |
|------|---------|-----------------|
| 200 | OK | Use `data` |
| 201 | Created | Use `data` |
| 400 | Validation / bad request | Show `message` + field `errors` |
| 401 | Unauthorized | Refresh token or re-login |
| 403 | Forbidden (wrong role) | Show permission error |
| 404 | Not found | Show not-found UI |
| 409 | Conflict (e.g. duplicate agency) | Show conflict message |
| 502 | Storage/upload failed | Retry upload |

---

## 2. Role & account type mapping

| Frontend `accountType` | Backend `Role` (JWT + `/api/users/me`) | Set via |
|------------------------|----------------------------------------|---------|
| `customer` | `CUSTOMER` | `PATCH /api/users/me/onboarding` |
| `construction` | `CONSTRUCTION_AGENCY` | same |
| `delivery` | `DELIVERY_PROVIDER` | same |

**Verification status** (`verificationStatus` on user + onboarding): `UNVERIFIED` → `PENDING` → `VERIFIED` / `REJECTED`. Cosmetic only — **does not block** dashboard access.

**Never trust client-side `managedAgencyId`** — always read from `GET /api/users/me/onboarding` → `data.managedAgencyId`.

---

## 3. Seed catalog data (production has these UUIDs)

Use these real IDs in dev/testing — do not use string slugs like `buildstrong-ltd`.

**Categories:** `cement`, `blocks`, `gravel`, `steel`, `roofing`, `tiles`, `paint`, `plumbing`, `electrical`

**Suppliers:**
| ID | Name |
|----|------|
| `a1000001-0000-4000-8000-000000000001` | BuildMart Ghana |
| `a1000001-0000-4000-8000-000000000002` | Steel & More Ltd |
| `a1000001-0000-4000-8000-000000000003` | RoofPro Supplies |

**Products:**
| ID | Name | Price | Category |
|----|------|-------|----------|
| `b2000001-0000-4000-8000-000000000001` | Dangote Cement 50kg | 88 GHS | cement |
| `b2000001-0000-4000-8000-000000000002` | Iron Rods 12mm | 45 GHS | steel |
| `b2000001-0000-4000-8000-000000000003` | Aluzinc Roofing Sheet | 120 GHS | roofing |
| `b2000001-0000-4000-8000-000000000004` | Sandcrete Blocks 6" | 5.50 GHS | blocks |
| `b2000001-0000-4000-8000-000000000005` | Ceramic Floor Tiles | 35 GHS | tiles |

---

## 4. API client architecture

Create under `src/api/`:

```
client.ts          # fetch wrapper, JSON parse, Bearer header, 401 refresh retry
auth.ts
users.ts           # profile + avatar
onboarding.ts
catalog.ts         # categories, suppliers, products (public)
agencies.ts        # agency CRUD, posts, portfolio, personnel, orders
orders.ts          # customer checkout + order history
delivery.ts
verification.ts
saved.ts
reviews.ts
messages.ts
notifications.ts
```

**`client.ts` requirements:**
```typescript
type ApiResponse<T> = {
  success: boolean;
  message: string | null;
  data: T;
  errors: { field: string; message: string }[] | null;
  timestamp: string;
};

async function api<T>(path: string, options?: RequestInit): Promise<T> {
  // 1. Attach Authorization if accessToken exists
  // 2. Parse envelope
  // 3. If !success throw ApiError with status, message, errors
  // 4. Return data
}
```

---

## 5. Authentication endpoints

### 5.1 Register
`POST /api/auth/register` — **Public**

**Request:**
```json
{
  "fullName": "Jane Doe",
  "email": "jane@example.com",
  "password": "Secret123"
}
```

**Validation rules:**
- `fullName`: required, max 150 chars
- `email`: valid email, max 255 chars
- `password`: 8–100 chars, at least 1 letter + 1 number
- Do **NOT** send `confirmPassword` or `role`

**Response:** `201`
```json
{
  "success": true,
  "message": "Registration successful. Please sign in.",
  "data": {
    "id": "uuid",
    "fullName": "Jane Doe",
    "email": "jane@example.com",
    "role": "CUSTOMER",
    "verificationStatus": "UNVERIFIED",
    "active": true,
    "profilePictureUrl": null,
    "createdAt": "..."
  }
}
```

**Frontend:** Navigate to Sign In. **Do NOT** auto-login or store tokens.

**Validation check:** Register → expect 201, `data.role === "CUSTOMER"`, no tokens returned.

---

### 5.2 Login
`POST /api/auth/login` — **Public**

**Request:**
```json
{ "email": "jane@example.com", "password": "Secret123" }
```

**Response:** `200`
```json
{
  "data": {
    "accessToken": "eyJ...",
    "refreshToken": "opaque-string",
    "tokenType": "Bearer",
    "expiresIn": 900
  }
}
```

Store `accessToken` + `refreshToken` securely (SecureStore). Rate limited: 5 attempts / 15 min.

**Validation check:** Login → expect 200, both tokens present, `expiresIn === 900`.

---

### 5.3 Google Sign-In
`POST /api/auth/google` — **Public**

**Request:**
```json
{ "idToken": "<Google ID token from expo-auth-session>" }
```

**Response:** Same `AuthResponse` as login.

**Validation check:** Valid idToken → 200 + tokens. Google-only account + manual login → 400.

---

### 5.4 Refresh
`POST /api/auth/refresh` — **Public**

**Request:**
```json
{ "refreshToken": "<stored refresh token>" }
```

**Response:** New `accessToken` + `refreshToken` pair. Old refresh token is revoked.

**Validation check:** Refresh → new tokens differ from old. Using old refresh again → 401.

---

### 5.5 Logout
`POST /api/auth/logout` — **Public**

**Request:**
```json
{ "refreshToken": "<stored refresh token>" }
```

**Response:** `200`, `{ "success": true, "message": "Logged out successfully" }`

Clear all local tokens after success.

---

### 5.6 Forgot / Reset / Change password

`POST /api/auth/forgot-password` — `{ "email": "..." }` → always 200 (no enumeration)

`POST /api/auth/reset-password` — `{ "token": "...", "newPassword": "..." }`

`POST /api/auth/change-password` — **JWT required** — `{ "currentPassword": "...", "newPassword": "..." }`

---

## 6. User profile

### 6.1 Get profile
`GET /api/users/me` — **JWT**

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
  "createdAt": "..."
}
```

### 6.2 Update profile
`PATCH /api/users/me` — **JWT**

**Request:**
```json
{
  "fullName": "Jane Updated",
  "profilePictureUrl": "https://res.cloudinary.com/..."
}
```

Both fields optional. Email is read-only.

### 6.3 Upload avatar
`POST /api/users/me/avatar` — **JWT**, multipart `file`

**Response `data`:**
```json
{ "profilePictureUrl": "https://res.cloudinary.com/ntdimmen/image/upload/..." }
```

**Flow:** Pick image → upload avatar → PATCH profile with returned URL (or just use upload response).

**Validation check:** Upload PNG → 200, URL starts with `https://res.cloudinary.com/`. GET `/api/users/me` → `profilePictureUrl` matches.

### 6.4 Delete account
`DELETE /api/account` — **JWT** → cascades all user data.

---

## 7. Onboarding (replace local authStore onboarding)

### 7.1 Get onboarding state
`GET /api/users/me/onboarding` — **JWT**

**Response `data`:**
```json
{
  "accountType": "construction",
  "onboardingComplete": false,
  "verificationStatus": "unverified",
  "managedAgencyId": "uuid-or-null",
  "deliveryProviderProfile": {
    "fullName": "Kofi Mensah",
    "constructionAgencyId": "uuid-or-null",
    "vehicleInfo": "Motorbike",
    "profileImageUrl": null
  },
  "deliveryProviderStatus": "none"
}
```

`accountType` is `null` before role selection. `deliveryProviderProfile` is `null` if not a delivery user. `deliveryProviderStatus`: `none` | `pending` | `approved` | `rejected`.

**Call on:** app launch (if authenticated), after login, after any onboarding step.

### 7.2 Set account type
`PATCH /api/users/me/onboarding` — **JWT**

**Request:**
```json
{ "accountType": "construction" }
```

Valid values: `"customer"` | `"construction"` | `"delivery"` (lowercase strings).

**Side effect:** Updates `user.role` to `CUSTOMER` | `CONSTRUCTION_AGENCY` | `DELIVERY_PROVIDER`.

**Response:** Updated onboarding object (same shape as GET).

**Validation check:** PATCH `construction` → GET onboarding shows `accountType: "construction"`. GET `/api/users/me` → `role: "CONSTRUCTION_AGENCY"`.

### 7.3 Complete onboarding
`POST /api/users/me/onboarding/complete` — **JWT**

**Response:** `onboardingComplete: true`.

**Validation check:** After POST → GET shows `onboardingComplete === true`.

---

## 8. Agencies

### 8.1 Create agency (construction onboarding)
`POST /api/agencies` — **JWT** (role must be `CONSTRUCTION_AGENCY`)

**Request:**
```json
{
  "name": "BuildStrong Ltd",
  "category": "general-contracting",
  "tagline": "Quality builds since 2010",
  "description": "Full-service construction agency in Accra.",
  "address": "14 Independence Ave, Accra",
  "phone": "+233201234567",
  "hours": "Mon–Fri 8am–6pm",
  "services": ["renovation", "new builds", "project management"]
}
```

Required: `name` (max 200), `category` (max 50). All else optional.

**Response:** `201`
```json
{
  "data": {
    "id": "uuid",
    "name": "BuildStrong Ltd",
    "logoUrl": null,
    "verified": false,
    "tagline": "...",
    "description": "...",
    "address": "...",
    "phone": "...",
    "hours": "...",
    "services": ["renovation", "new builds", "project management"],
    "category": "general-contracting"
  }
}
```

Store `data.id` — it becomes `managedAgencyId` in onboarding.

**Validation check:** Create → 201 with UUID. GET onboarding → `managedAgencyId` equals `data.id`. Second create → 409.

### 8.2 Get / update my agency
`GET /api/agencies/me` — **JWT**

`PATCH /api/agencies/me` — **JWT** — partial update, same fields as create + `logoUrl`

### 8.3 Public agency directory
`GET /api/agencies?q=build&page=0&limit=20` — **Public**

`GET /api/agencies/{agencyId}` — **Public**

**Validation check:** Unauthenticated GET list → 200 with paginated agencies.

---

## 9. Catalog (replace MOCK_PRODUCTS / mockSuppliers)

All **public** — no auth header needed.

### 9.1 Categories
`GET /api/categories`

**Response `data`:** array of `{ "id": "cement", "name": "Cement" }`

### 9.2 Suppliers
`GET /api/suppliers?q=&category=cement&page=0&limit=20`

`GET /api/suppliers/{supplierId}`

**Supplier shape:**
```json
{
  "id": "uuid",
  "name": "BuildMart Ghana",
  "logoUrl": null,
  "rating": 4.7,
  "reviewCount": 128,
  "distanceKm": 2.4,
  "verified": true,
  "categoryId": "cement"
}
```

### 9.3 Products
`GET /api/products?q=cement&category=cement&supplierId=&agencyId=&page=0&limit=20`

`GET /api/products/{productId}`

**Product shape:**
```json
{
  "id": "uuid",
  "name": "Dangote Cement 50kg",
  "category": "cement",
  "price": 88,
  "unit": "per bag",
  "imageUrl": null,
  "description": "...",
  "supplierId": "uuid",
  "agencyId": null,
  "stockQuantity": 500,
  "inStock": true,
  "brand": "Dangote",
  "spec": "50kg bag",
  "deliveryEstimate": "Same day"
}
```

**Validation check (no auth):**
- GET categories → 9 items
- GET products → at least 5 items
- GET product `b2000001-0000-4000-8000-000000000001` → name contains "Dangote", price 88

---

## 10. Agency products (replace local agency product form)

Requires JWT + `CONSTRUCTION_AGENCY` role + agency created.

### 10.1 Upload product image first
`POST /api/agencies/me/products/upload-image` — multipart `file`

**Response `data`:** `{ "imageUrl": "https://res.cloudinary.com/..." }`

### 10.2 Create product
`POST /api/agencies/me/products` — **JWT**

**Request:**
```json
{
  "name": "Premium Cement 50kg",
  "category": "cement",
  "price": 92,
  "unit": "per bag",
  "stockQuantity": 100,
  "imageUrl": "https://res.cloudinary.com/...",
  "description": "High-quality cement",
  "brand": "Dangote",
  "spec": "50kg",
  "deliveryEstimate": "Same day"
}
```

Required: `name`, `category`, `price` (≥0), `unit`, `stockQuantity`.

### 10.3 Update / delete
`PATCH /api/agencies/me/products/{productId}` — partial update, add `"active": false` to soft-delete

`DELETE /api/agencies/me/products/{productId}`

**Validation check:** Create product → appears in `GET /api/products?agencyId={managedAgencyId}`.

---

## 11. Agency posts (replace agencyPostsStore)

`GET /api/agencies/me/posts?page=0&limit=20` — **JWT**

`POST /api/agencies/me/posts` — **JWT**
```json
{
  "type": "general",
  "title": "New project completed",
  "description": "We finished a 3-bedroom home in East Legon.",
  "imageUrl": null
}
```

`type`: `"service"` | `"material"` | `"general"`

`PATCH /api/agencies/me/posts/{postId}` — partial update

`DELETE /api/agencies/me/posts/{postId}`

`GET /api/agencies/{agencyId}/posts` — **Public**

**Post response shape:**
```json
{
  "id": "uuid",
  "agencyId": "uuid",
  "type": "general",
  "title": "...",
  "description": "...",
  "imageUrl": null,
  "createdAt": "...",
  "updatedAt": "..."
}
```

**Validation check:** Create post → list my posts includes it. Public GET by agencyId also returns it.

---

## 12. Agency portfolio (replace agencyPortfolioStore)

### Upload (existing endpoint)
`POST /api/agency/portfolio/upload` — **JWT**, multipart `file`

**Response `data`:**
```json
{
  "imageId": "uuid",
  "publicId": "agency-portfolio/userId/uuid",
  "resourceType": "image",
  "deliveryUrl": "https://res.cloudinary.com/..."
}
```

### List (do NOT persist URLs long-term — refetch on screen open)
`GET /api/agencies/me/portfolio` — **JWT**

`GET /api/agencies/{agencyId}/portfolio` — **Public**

**Response `data`:** array of `{ imageId, publicId, resourceType, deliveryUrl }`

### Delete
`DELETE /api/agencies/me/portfolio/{imageId}` — **JWT**

**Validation check:** Upload → list returns item with valid `deliveryUrl`. Delete → list no longer contains `imageId`.

---

## 13. Verification documents

`POST /api/verification/upload-document?documentType=BUSINESS_REGISTRATION` — **JWT**, multipart `file`

`documentType` values: `BUSINESS_REGISTRATION` | `GOVERNMENT_ID` | `PROFESSIONAL_LICENSE`

**Response `data`:**
```json
{
  "documentId": "uuid",
  "documentType": "BUSINESS_REGISTRATION",
  "publicId": "verification-docs/userId/uuid",
  "resourceType": "image"
}
```

**Side effect:** First upload sets `verificationStatus` → `PENDING`.

`GET /api/verification/{userId}/document-url?documentType=BUSINESS_REGISTRATION` — **JWT** (owner or ADMIN)

**Response `data`:**
```json
{
  "signedUrl": "https://res.cloudinary.com/...",
  "expiresAt": "2026-07-10T15:05:00Z"
}
```

Signed URL expires in **5 minutes** — fetch on demand, never cache long-term.

**Validation check:** Upload doc → GET `/api/users/me` shows `verificationStatus: "PENDING"`.

---

## 14. Customer checkout & orders

### 14.1 Checkout
`POST /api/orders/checkout` — **JWT**

**Request (preferred — with productId):**
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

`productId` is **optional** (nullable) — legacy name-based checkout still works. When `productId` is set:
- Server uses catalog price (ignores client `unitPrice` for billing)
- Validates stock availability
- Links order item to agency if product is agency-owned
- Decrements stock on successful Paystack payment

**Response:** `201`
```json
{
  "data": {
    "orderId": "uuid",
    "orderNumber": "CB-uuid",
    "paystackReference": "CB-uuid",
    "authorizationUrl": "https://checkout.paystack.com/...",
    "totalAmount": 176
  }
}
```

**Frontend flow:**
1. POST checkout → get `authorizationUrl`
2. Open Paystack in WebView/browser
3. On return: `POST /api/orders/{orderId}/verify` (fallback; webhook is primary)
4. Poll `GET /api/orders/{orderId}` until `status === "PAID"`

### 14.2 Order history
`GET /api/orders` — **JWT** — array of orders

`GET /api/orders/{orderId}` — **JWT** (owner only)

**Order shape:**
```json
{
  "id": "uuid",
  "status": "PAID",
  "subtotal": 176,
  "total": 176,
  "currency": "GHS",
  "deliveryAddress": "12 Market Road",
  "deliveryCity": "Accra",
  "deliveryRegion": "Greater Accra",
  "phoneNumber": "+233201234567",
  "paystackReference": "CB-uuid",
  "items": [
    {
      "id": "uuid",
      "productName": "Dangote Cement 50kg",
      "supplierName": "BuildMart Ghana",
      "unitPrice": 88,
      "quantity": 2,
      "unit": "per bag",
      "lineTotal": 176
    }
  ],
  "createdAt": "...",
  "updatedAt": "..."
}
```

**Payment statuses:** `PENDING` → `PROCESSING` → `PAID` | `FAILED` | `REFUNDED`

**Validation check:** Checkout with seed productId → 201, `totalAmount === 176` (88×2). `authorizationUrl` is valid HTTPS URL.

---

## 15. Agency orders (replace MOCK_AGENCY_ORDERS)

`GET /api/agencies/me/orders` — **JWT** (`CONSTRUCTION_AGENCY` + owns agency)

`GET /api/agencies/me/orders/{orderId}` — **JWT**

`PATCH /api/agencies/me/orders/{orderId}/status?status=processing` — **JWT**

**Fulfillment status values:** `pending` | `processing` | `delivered` | `cancelled`

**Agency order shape:**
```json
{
  "id": "uuid",
  "orderNumber": "CB-uuid",
  "customerId": "uuid",
  "customerName": "Jane Doe",
  "customerEmail": "jane@example.com",
  "customerPhone": "+233201234567",
  "agencyId": "uuid",
  "orderDate": "...",
  "status": "pending",
  "deliveryAddress": "12 Market Road, Accra, Greater Accra",
  "totalAmount": 176,
  "items": [
    {
      "productId": "uuid",
      "productName": "Dangote Cement 50kg",
      "quantity": 2,
      "unitPrice": 88,
      "unit": "per bag"
    }
  ]
}
```

**Validation check:** After customer pays for agency product → agency orders list is non-empty.

---

## 16. Delivery providers (replace deliveryPersonnelStore)

### 16.1 Setup
`POST /api/delivery-providers/setup` — **JWT** (`DELIVERY_PROVIDER`)

**Request:**
```json
{
  "fullName": "Kofi Mensah",
  "constructionAgencyId": "uuid-of-agency-from-picker",
  "vehicleInfo": "Motorbike - Honda CB125",
  "profileImageUrl": null
}
```

`fullName` required. `constructionAgencyId` from `GET /api/agencies` public directory.

**Response `data`:**
```json
{
  "id": "uuid",
  "userId": "uuid",
  "fullName": "Kofi Mensah",
  "constructionAgencyId": "uuid",
  "vehicleInfo": "Motorbike - Honda CB125",
  "profileImageUrl": null,
  "approvalStatus": "pending",
  "submittedAt": "...",
  "handledAt": null
}
```

### 16.2 Profile management
`GET /api/delivery-providers/me` — **JWT**

`PATCH /api/delivery-providers/me` — **JWT** — same body shape as setup

`DELETE /api/delivery-providers/me/association` — **JWT** — leaves agency, resets to pending

### 16.3 Jobs
`GET /api/delivery-providers/me/jobs` — **JWT**

`PATCH /api/delivery-providers/me/jobs/{jobId}/status?status=in_transit` — **JWT**

**Job status:** `assigned` | `in_transit` | `delivered`

**Validation check:** Setup with agencyId → GET onboarding shows `deliveryProviderStatus: "pending"`. Agency approves → status becomes `"approved"`.

---

## 17. Agency personnel (agency approves delivery drivers)

`GET /api/agencies/me/personnel` — **JWT**

**Response `data`:** array of:
```json
{
  "id": "uuid",
  "userId": "uuid",
  "fullName": "Kofi Mensah",
  "profileImageUrl": null,
  "constructionAgencyId": "uuid",
  "vehicleInfo": "Motorbike",
  "approvalStatus": "pending",
  "submittedAt": "...",
  "handledAt": null
}
```

`POST /api/agencies/me/personnel/{personnelId}/approve` — **JWT**

`POST /api/agencies/me/personnel/{personnelId}/reject` — **JWT**

`DELETE /api/agencies/me/personnel/{personnelId}` — **JWT**

**Validation check:** Delivery user requests join → appears in agency personnel with `pending`. Approve → `approvalStatus: "approved"`, delivery user onboarding shows `deliveryProviderStatus: "approved"`.

---

## 18. Saved items (replace savedStore)

`GET /api/users/me/saved` — **JWT**

**Response `data`:** `[{ "id": "uuid", "type": "product", "savedAt": "..." }]`

Note: `id` is the **subject ID** (product/supplier/agency UUID), not the saved-record ID.

`POST /api/users/me/saved` — **JWT**
```json
{ "id": "b2000001-0000-4000-8000-000000000001", "type": "product" }
```

`type`: `"product"` | `"supplier"` | `"agency"`

`DELETE /api/users/me/saved/{type}/{id}` — **JWT**

**Frontend Saved screen:** Fetch saved IDs → resolve against catalog/agency APIs.

**Validation check:** Save product → GET saved includes it. Delete → gone. Idempotent re-save → no error.

---

## 19. Reviews (replace mockReviews)

`GET /api/reviews?subjectType=product&subjectId={uuid}` — **Public**

`GET /api/reviews/summary?subjectType=product&subjectId={uuid}` — **Public**

**Summary `data`:**
```json
{ "averageRating": 4.6, "totalCount": 128, "breakdown": [] }
```

`GET /api/reviews/me` — **JWT**

`POST /api/reviews` — **JWT**
```json
{
  "subjectType": "product",
  "subjectId": "b2000001-0000-4000-8000-000000000001",
  "rating": 5,
  "text": "Excellent quality cement.",
  "verifiedPurchase": false,
  "orderNumber": null
}
```

`rating`: 1–5 (required). `subjectType`: `"product"` | `"supplier"`.

`PATCH /api/reviews/{reviewId}` — **JWT** — `{ "rating": 4, "text": "Updated" }`

`DELETE /api/reviews/{reviewId}` — **JWT**

---

## 20. Messaging (replace messagesData)

`GET /api/messages/threads` — **JWT**

**Thread shape:**
```json
{
  "id": "uuid",
  "participantName": "BuildStrong Ltd",
  "participantLogoUrl": null,
  "lastMessage": "Hello, I need a quote",
  "lastMessageAt": "...",
  "unreadCount": 1
}
```

`POST /api/messages/threads` — **JWT** — start or get existing thread
```json
{ "agencyId": "uuid" }
```

`GET /api/messages/threads/{threadId}` — **JWT** — returns **array of messages** (not thread metadata)

**Message shape:**
```json
{
  "id": "uuid",
  "threadId": "uuid",
  "text": "Hello, I need a quote",
  "sentAt": "...",
  "isOutgoing": true
}
```

`POST /api/messages/threads/{threadId}/messages` — **JWT**
```json
{ "text": "What is your price for 50 bags?" }
```

`PATCH /api/messages/threads/{threadId}/read` — **JWT**

**Validation check:** Customer starts thread with agency → thread appears in list. Send message → appears in thread messages. `unreadCount` decreases after mark read.

---

## 21. Notifications

`GET /api/notifications` — **JWT**

**Notification shape:**
```json
{
  "id": "uuid",
  "type": "personnel",
  "title": "New personnel request",
  "body": "Kofi Mensah requested to join your agency.",
  "read": false,
  "createdAt": "...",
  "data": { "personnelId": "uuid" }
}
```

`type`: `"order"` | `"verification"` | `"personnel"` | `"message"`

`PATCH /api/notifications/{id}/read` — **JWT**

`PATCH /api/notifications/read-all` — **JWT**

---

## 22. Admin verification (ADMIN role only)

`GET /api/admin/verification/pending` — **JWT** (ADMIN)

`POST /api/admin/verification/{userId}/approve` — **JWT**

`POST /api/admin/verification/{userId}/reject` — **JWT**

Approve sets user `verificationStatus: VERIFIED` and agency `verified: true`.

---

## 23. Complete integration flows (implement in this order)

### Flow A — New customer (marketplace + checkout)
1. `POST /api/auth/register`
2. `POST /api/auth/login` → store tokens
3. `GET /api/users/me` → confirm `role: CUSTOMER`
4. `PATCH /api/users/me/onboarding` → `{ "accountType": "customer" }`
5. `POST /api/users/me/onboarding/complete`
6. `GET /api/categories` + `GET /api/products` → render marketplace
7. `GET /api/products/b2000001-0000-4000-8000-000000000001` → product detail
8. `POST /api/users/me/saved` → save product
9. `POST /api/orders/checkout` with `productId` → open Paystack URL
10. `POST /api/orders/{id}/verify` → confirm `status: PAID`
11. `GET /api/orders` → order appears in history

### Flow B — Construction agency
1. Register + login
2. `PATCH /api/users/me/onboarding` → `"construction"`
3. `POST /api/agencies` → store `managedAgencyId`
4. Upload verification doc → `verificationStatus: PENDING`
5. `POST /api/agencies/me/products/upload-image` → get `imageUrl`
6. `POST /api/agencies/me/products` → create product
7. `POST /api/agencies/me/posts` → create announcement
8. `POST /api/agency/portfolio/upload` → upload image
9. `GET /api/agencies/me/portfolio` → list images
10. `POST /api/users/me/onboarding/complete`
11. (After customer order) `GET /api/agencies/me/orders` → see order
12. `PATCH /api/agencies/me/orders/{id}/status?status=processing`

### Flow C — Delivery provider
1. Register + login
2. `PATCH /api/users/me/onboarding` → `"delivery"`
3. `GET /api/agencies` → pick agency from public list
4. `POST /api/delivery-providers/setup` with `constructionAgencyId`
5. Agency: `GET /api/agencies/me/personnel` → see pending request
6. Agency: `POST /api/agencies/me/personnel/{id}/approve`
7. Delivery: `GET /api/users/me/onboarding` → `deliveryProviderStatus: "approved"`
8. `POST /api/users/me/onboarding/complete`

### Flow D — Messaging
1. Customer logged in
2. `POST /api/messages/threads` → `{ "agencyId": "..." }`
3. `POST /api/messages/threads/{id}/messages` → send text
4. Agency owner logged in → `GET /api/messages/threads` → sees thread
5. `PATCH /api/messages/threads/{id}/read`

---

## 24. Master validation checklist

Run these after integration. Every item must pass before shipping.

### Auth & session
- [ ] Register returns 201, no tokens, `role: CUSTOMER`
- [ ] Login returns tokens, `expiresIn: 900`
- [ ] Protected route without token → 401
- [ ] Refresh rotates tokens; old refresh fails on reuse
- [ ] Logout revokes refresh token

### Onboarding persistence
- [ ] Fresh user: `GET onboarding` → `accountType: null`, `onboardingComplete: false`
- [ ] PATCH `construction` → `GET /api/users/me` shows `CONSTRUCTION_AGENCY`
- [ ] POST complete → `onboardingComplete: true`
- [ ] Kill app / reinstall → onboarding state restored from API (not AsyncStorage)

### Catalog (no auth)
- [ ] `GET /api/categories` → 9 categories
- [ ] `GET /api/products` → ≥5 products with real UUIDs
- [ ] `GET /api/products?category=cement` → filters correctly
- [ ] Product detail shows `inStock: true` for seed data

### Agency
- [ ] Create agency → 201, UUID returned
- [ ] `GET onboarding` → `managedAgencyId` populated
- [ ] Duplicate create → 409
- [ ] `GET /api/agencies` (no auth) → includes created agency
- [ ] Create product → visible in `GET /api/products?agencyId=`
- [ ] Create post → visible in public `GET /api/agencies/{id}/posts`
- [ ] Portfolio upload → list returns `deliveryUrl`
- [ ] Portfolio delete → item removed

### Verification & avatar
- [ ] Upload verification doc → `verificationStatus: PENDING`
- [ ] Signed URL returns valid HTTPS link expiring in ~5 min
- [ ] Avatar upload → `profilePictureUrl` is Cloudinary HTTPS URL

### Checkout
- [ ] Checkout with `productId` → `totalAmount` matches server price × quantity
- [ ] Checkout returns `authorizationUrl` (HTTPS)
- [ ] Checkout response includes `orderNumber` and `paystackReference`
- [ ] Legacy checkout without `productId` still works
- [ ] `GET /api/orders` returns order after checkout

### Delivery & personnel
- [ ] Delivery setup → `approvalStatus: pending`
- [ ] Agency sees personnel in `GET /api/agencies/me/personnel`
- [ ] Approve → delivery onboarding shows `approved`
- [ ] Reject → delivery onboarding shows `rejected`

### Social
- [ ] Save product → appears in `GET /api/users/me/saved`
- [ ] Delete saved → removed
- [ ] Create review → appears in `GET /api/reviews?subjectType=product&subjectId=`
- [ ] Start message thread → send message → appears for both parties
- [ ] Notifications appear after personnel request

### Error handling
- [ ] 400 validation shows field errors in UI
- [ ] 403 on agency endpoint as CUSTOMER shows permission message
- [ ] 401 triggers refresh; failed refresh → logout
- [ ] Network error shows retry option

---

## 25. What to remove from frontend

Delete or stop using:
- `MOCK_PRODUCTS`, `mockSuppliers`, `VERIFIED_CONSTRUCTION_AGENCIES`
- `MOCK_AGENCY_ORDERS`, `mockReviews`, `messagesData`
- `productStore`, `agencyPostsStore`, `savedStore`, `deliveryPersonnelStore`, `agencyPortfolioStore` as **source of truth** (may keep as cache)
- Local-only `accountType`, `onboardingComplete`, `managedAgencyId` in `authStore`
- Sending `file://` or `content://` URIs as `profilePictureUrl` or `imageUrl` in JSON

---

## 26. Not implemented on backend (do not wire)

- `POST /api/auth/verify-email`
- `POST /api/auth/resend-verification`
- WebSocket / real-time push (poll notifications + messages on focus)

---

## 27. Production smoke commands

```bash
# Public catalog (no auth)
curl -s https://civicbuild-production.up.railway.app/api/categories
curl -s "https://civicbuild-production.up.railway.app/api/products?limit=3"

# Health
curl -s https://civicbuild-production.up.railway.app/api/health
```

Expected: all return `"success": true`.

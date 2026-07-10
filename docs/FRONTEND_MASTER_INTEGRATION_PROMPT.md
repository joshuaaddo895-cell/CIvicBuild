# CivicBuild Frontend — Master Prompt: Consume Railway Backend APIs

> **COPY THIS ENTIRE FILE** into a Cursor/Composer agent on the Expo frontend repo.

You are working on the **CivicBuild** Expo React Native app (SDK 54). The Spring Boot API is **live on Railway** and the frontend has been wired to it (commit `c3faf82` on `CivicBuild-Frontend`). Your job is to **consume, validate, polish, and extend** the integrated features against **Railway production** — not reintroduce mocks or local persistence for server-owned data.

**Railway API (use this):** `https://civicbuild-production.up.railway.app`  
**Frontend repo:** `/Users/mac/Pictures/CivicBuildFrontend`  
**Backend repo:** `/Users/mac/CivicBuildBackend`  
**Expo docs:** https://docs.expo.dev/versions/v54.0.0/

### Railway production status (verified)

All public endpoints below return `success: true` on Railway:

| Endpoint | Status |
|----------|--------|
| `GET /api/health` | UP |
| `GET /api/categories` | OK |
| `GET /api/products?page=0&limit=20` | 5 seed products |
| `GET /api/suppliers?page=0&limit=20` | 3 seed suppliers |
| `GET /api/products?q=cement` | Search works |
| `GET /api/suppliers?q=build` | Search works |
| `GET /api/products/{seedId}` | Dangote Cement 50kg — 88 GHS |
| `GET /api/suppliers/{seedId}` | BuildMart Ghana |
| `GET /api/reviews/summary?...` | OK |

`GET /api/agencies` returns empty until a construction user creates an agency — expected.

---

## 0. Environment — Railway first

```bash
# REQUIRED — Railway production (what the app should use)
EXPO_PUBLIC_API_URL=https://civicbuild-production.up.railway.app

# Optional — local backend only for backend dev
# EXPO_PUBLIC_API_URL=http://localhost:8081
```

Run the app:

```bash
cd /Users/mac/Pictures/CivicBuildFrontend
npm start
```

Typecheck before finishing:

```bash
npm run typecheck
```

---

## 0b. New Railway features to consume (post-mock removal)

These backend areas are **live on Railway** and already have API modules + wired screens. Validate they work end-to-end against production:

| Feature | Railway endpoints | Frontend module | Key screens |
|---------|-------------------|-----------------|-------------|
| **Catalog** | `GET /api/products`, `/api/suppliers`, `/api/categories` | `catalog.ts` | Home, AllSuppliers, SupplierDetail |
| **Saved items** | `GET/POST/DELETE /api/users/me/saved` | `saved.ts` | Saved, ProductDetail, AgencyDetail |
| **Reviews** | `GET/POST/PATCH/DELETE /api/reviews` | `reviews.ts` | Reviews, MyReviews, ProductDetail |
| **Messages** | `GET/POST /api/messages/threads` | `messages.ts` | Messages, ConversationDetail |
| **Notifications** | `GET/PATCH /api/notifications` | `notifications.ts` | Notifications |
| **Agency posts** | `GET/POST/PATCH/DELETE /api/agencies/me/posts` | `agencies.ts` | AgencyPosts, AgencyPostForm, AgencyDetail |
| **Agency portfolio** | `GET/POST/DELETE` portfolio + upload | `agencies.ts`, `agencyPortfolio.ts` | AgencyPortfolio, AgencyDetail |
| **Agency orders** | `GET/PATCH /api/agencies/me/orders` | `agencyOrders.ts` | AgencyOrders, AgencyOrderDetail |
| **Agency personnel** | `GET/PATCH` personnel approve/reject | `agencies.ts` | AgencyPersonnel |
| **Delivery** | `POST/GET/PATCH /api/delivery-providers/*` | `delivery.ts` | DeliveryProviderSetup, DeliveryDashboard |
| **Avatar upload** | `POST /api/users/me/avatar` | `users.ts` | EditProfile |
| **Checkout hardening** | `POST /api/orders/checkout` with `productId` | `orderMappers.ts` | Checkout (mapper only) |
| **Agency directory** | `GET /api/agencies` | `agencies.ts` | ConstructionAgencySelect, AgencyDetail |

---

## 1. Non-negotiable integration rules

1. **Never send `file://` or `content://` URIs in JSON bodies.** Upload via multipart (`field name: file`) first; use the returned HTTPS URL in PATCH/POST bodies.
2. **Never trust local `managedAgencyId`.** Always read from `GET /api/users/me/onboarding` after login and after any onboarding mutation (`syncOnboardingFromServer()`).
3. **Use `apiClient` only** (`src/api/client.ts`) — Bearer token + 401 refresh. No second HTTP client.
4. **Envelope pattern:** `{ success, message, data, errors, timestamp }`. Unwrap with `unwrapApiResponse` (`src/api/authTypes.ts`). Use `toApiResult` (`src/api/apiResult.ts`) for `{ ok, data | error }`.
5. **Pagination:** `?page=0&limit=20` → `data: { items, page, limit, total, hasNextPage }`. Omitting `q` is valid — backend precomputes LIKE patterns (do not pass empty string for search).
6. **Errors:** Show `message`; map `errors: [{ field, message }]` via `normalizeApiError` (`src/api/errors.ts`).
7. **Loading / error / empty states** on every fetch screen: `ActivityIndicator`, retry button, `EmptyState`.
8. **Server is source of truth** after mutations; optimistic UI only where rollback is trivial (saved toggle).
9. **Do not modify** Auth, Cart, Checkout, or Payment **screens** unless absolutely required. Checkout payload changes only in `src/utils/orderMappers.ts` (`productId` on each item).
10. **Messaging threads** are **agency-only**: `POST /api/messages/threads { agencyId }`. No supplier threads on backend yet.

---

## 2. API client modules (all implemented)

| Module | File | Key functions |
|--------|------|---------------|
| Auth | `src/api/auth.ts` | `login`, `register`, `googleSignIn`, `refresh`, `logout`, `forgotPassword`, `resetPassword`, `changePassword` |
| Users | `src/api/users.ts` | `getProfile`, `updateProfile`, `uploadAvatar` |
| Account | `src/api/account.ts` | `deleteAccount` |
| Onboarding | `src/api/onboarding.ts` | `getOnboarding`, `patchOnboarding`, `completeOnboarding` |
| Catalog | `src/api/catalog.ts` | `getCategories`, `getSuppliers`, `getSupplier`, `getProducts`, `getProduct`, mappers |
| Agencies | `src/api/agencies.ts` | CRUD agency, posts, portfolio, personnel, products, `listAgencies`, `getAgency` |
| Agency orders | `src/api/agencyOrders.ts` | `getAgencyOrders`, `getAgencyOrder`, `updateAgencyOrderStatus` |
| Agency portfolio upload | `src/api/agencyPortfolio.ts` | `uploadAgencyPortfolioImage` |
| Orders / checkout | `src/api/orders.ts`, `src/api/checkoutService.ts` | `checkout`, `verifyPayment`, `getOrder`, `listMyOrders` |
| Delivery | `src/api/delivery.ts` | `setupDeliveryProvider`, `getMyDeliveryProvider`, `getMyDeliveryJobs`, `updateDeliveryJobStatus` |
| Saved | `src/api/saved.ts` | `getSavedItems`, `saveItem`, `removeSavedItem` |
| Reviews | `src/api/reviews.ts` | `getReviews`, `getReviewSummary`, `getMyReviews`, `createReview`, `updateReview`, `deleteReview` |
| Messages | `src/api/messages.ts` | `getThreads`, `startThread`, `getThreadMessages`, `sendMessage`, `markThreadRead` |
| Notifications | `src/api/notifications.ts` | `getNotifications`, `markNotificationRead`, `markAllNotificationsRead` |
| Verification | `src/api/verification.ts` | document upload + signed URL |

**Types:** `src/types/*Api.ts`, `src/types/catalog.ts`, `src/types/agency.ts`, `src/types/order.ts`

---

## 3. Screen → API mapping (wired — do not regress)

| Screen | API calls |
|--------|-----------|
| `HomeScreen` | `getProducts`, `getSuppliers`, `productStore.fetchCatalog`, `savedStore.syncFromServer` |
| `AllSuppliersScreen` | `getSuppliers` (paginated + search) |
| `SupplierDetailScreen` | `getSupplier`, `findProductsBySupplier` |
| `ProductDetailScreen` | `getReviewSummary`, catalog via `productStore` |
| `AgencyDetailScreen` | `getAgency`, `getAgencyPosts`, `getAgencyPortfolio`, `startThread`, saved toggle |
| `SavedScreen` | `syncFromServer`, `resolveSavedItemDetailAsync` |
| `ReviewsScreen` | `getReviews`, `getReviewSummary` |
| `MyReviewsScreen` | `getMyReviews` + catalog resolve |
| `MessagesScreen` | `getThreads` |
| `ConversationDetailScreen` | `getThreadMessages`, `sendMessage`, `markThreadRead`, `startThread` (if `agencyId` param) |
| `ProfileScreen` | `getMyReviews`, `getAgency` (delivery agency name) |
| `EditProfileScreen` | `uploadAvatar` then `updateProfile` |
| `AgencyPostFormScreen` | `createAgencyPost` / `updateAgencyPost`, `uploadAgencyProductImage` for local images |
| `AgencyPostsScreen` | `getMyAgencyPosts`, `deleteAgencyPost` |
| `AgencyPortfolioScreen` | `getMyPortfolio`, `uploadAgencyPortfolioImage`, `deletePortfolioImage` |
| `AgencyOrdersScreen` | `getAgencyOrders` |
| `AgencyOrderDetailScreen` | `getAgencyOrder`, `updateAgencyOrderStatus` |
| `AgencyPersonnelScreen` | `getAgencyPersonnel`, approve/reject/remove |
| `AgencyDashboardScreen` | parallel fetch: agency, posts, portfolio, orders, personnel, products |
| `NotificationsScreen` | `getNotifications`, `markNotificationRead` |
| `DeliveryProviderSetupScreen` | `setupDeliveryProvider`, `uploadAvatar`, `completeOnboarding`, `syncOnboardingFromServer` |
| `DeliveryDashboardScreen` | `getMyDeliveryProvider`, `getMyDeliveryJobs`, `getAgency` |
| `ConstructionAgencySelect` | `listAgencies?q=` |
| `PendingCompanyConfirmationScreen` | `getAgency`, `syncOnboardingFromServer` on focus |

**Checkout mapper:** `src/utils/orderMappers.ts` — each `BackendOrderItem` includes `productId` from `CartItem`.

---

## 4. Stores & persistence (current state)

| Store | Behavior |
|-------|----------|
| `authStore` | Persists `user` + `isAuthenticated` only; tokens in SecureStore; onboarding from server |
| `savedStore` | In-memory cache synced from `GET /api/users/me/saved` — **not** AsyncStorage |
| `productStore` | In-memory catalog from `GET /api/products`; refresh after agency product mutations |
| `cartStore` | Local cart only (unchanged) |

**Deleted (must not reintroduce):** `agencyPostsStore`, `agencyPortfolioStore`, `deliveryPersonnelStore`, and all mock constants under `src/constants/mock*`, `constructionAgencies`, `agencyProfiles`, `messagesData`.

---

## 5. Upload flows

| Feature | Endpoint | Field name | Then |
|---------|----------|------------|------|
| Profile avatar | `POST /api/users/me/avatar` | `file` | PATCH `/api/users/me` with `profilePictureUrl` |
| Agency product image | `POST /api/agencies/me/products/upload-image` | `file` | Include `imageUrl` in product body |
| Agency portfolio | `POST /api/agency/portfolio/upload` | `file` | Refetch `GET /api/agencies/me/portfolio` |
| Verification doc | `POST /api/verification/upload-document` | `file` | Store returned `documentId` in onboarding state |
| Agency post image | Use product upload endpoint OR `imageUrl: null` | — | No dedicated post upload on backend |

Helper: `isLocalImageUri()` in `src/utils/agencyPostMappers.ts`; `buildMultipartFormData()` in `src/utils/uploadValidation.ts`.

---

## 6. authStore requirements

- `managedAgencyId` only from `syncOnboardingFromServer()` — never set locally except transient UI
- After agency create, delivery setup, onboarding complete: call `syncOnboardingFromServer()`
- `submitDeliveryProviderSetup` is a no-op stub — real setup is in `DeliveryProviderSetupScreen` → API
- On logout: `purgeLocalSession()` clears cart + saved cache

---

## 7. Seed data for Railway / Postman testing

| Entity | UUID | Notes |
|--------|------|-------|
| Product | `b2000001-0000-4000-8000-000000000001` | Dangote Cement 50kg — 88 GHS |
| Product | `b2000001-0000-4000-8000-000000000002` | Iron Rods 12mm — 45 GHS |
| Supplier | `a1000001-0000-4000-8000-000000000001` | BuildMart Ghana |

Verify on production:

```bash
curl https://civicbuild-production.up.railway.app/api/health
curl "https://civicbuild-production.up.railway.app/api/products?page=0&limit=2"
curl "https://civicbuild-production.up.railway.app/api/suppliers?page=0&limit=2"
```

---

## 8. Validation flows (must pass)

### Flow A — Customer
1. Register → Login → `syncOnboardingFromServer`
2. Home loads products + suppliers from API
3. Save product (heart) → persists via `POST /api/users/me/saved`
4. Add to cart → checkout with `productId` in payload → Paystack → verify → `GET /api/orders`

### Flow B — Agency
1. Onboarding `construction` → verification docs → `POST /api/agencies`
2. Navbar **Create Post** → `POST /api/agencies/me/posts` → visible on public agency detail
3. Upload portfolio → list on dashboard
4. Create agency product → appears in catalog
5. Customer order → `GET /api/agencies/me/orders` → update status

### Flow C — Delivery
1. Onboarding `delivery` → `listAgencies` → `POST /api/delivery-providers/setup` → `completeOnboarding`
2. Agency approves personnel → delivery dashboard shows jobs from `GET /api/delivery-providers/me/jobs`

### Flow D — Social
1. Agency detail → **Message Us** → `startThread` → send message
2. Notification appears in `GET /api/notifications`
3. Product reviews load on `ReviewsScreen`
4. Reinstall app + login → saved items restored from server

---

## 9. Polish & extension opportunities (optional)

- **Customer order history UI** — `listMyOrders` exists in `checkoutService.ts`; add dedicated screen or Profile section
- **Write review UI** on `ProductDetailScreen` / `SupplierDetailScreen` — `POST /api/reviews` after paid order
- **Supplier messaging** — backend has no supplier threads; currently navigates to Messages list
- **Categories from API** — `getCategories()` available; HomeScreen still uses static `MARKETPLACE_CATEGORIES` labels (IDs match seed)
- **Agency search on Home** — `listAgencies` for construction agencies in supplier carousel routing
- **Pull-to-refresh** on list screens (posts, orders, messages, notifications)
- **WebSocket messaging** — not on backend; REST + refetch on focus is correct for now

---

## 10. Explicit do-nots

- Do not add hardcoded agency IDs like `buildstrong-ltd`
- Do not persist onboarding RBAC in AsyncStorage
- Do not store Cloudinary signed URLs long-term — refetch portfolio on screen focus
- Do not implement `verify-email` / `resend-verification` (not on backend)
- Do not leave dead imports from deleted mock files
- Do not create a second HTTP client

---

## 11. Definition of done (regression checklist)

- [ ] `npm run typecheck` passes
- [ ] No runtime imports from deleted mock files (§4)
- [ ] `GET /api/products` and `GET /api/suppliers` work without `q` param
- [ ] Create Post from navbar persists and appears on agency detail
- [ ] Saved items survive reinstall + login
- [ ] Checkout sends `productId` per line item
- [ ] Avatar upload uses multipart, not local URI in PATCH
- [ ] `managedAgencyId` matches server after agency create

---

## 12. Reference docs

| Doc | Purpose |
|-----|---------|
| `docs/FRONTEND_INTEGRATION_PROMPT.md` | Shorter API contract + payload examples |
| `postman/CivicBuild-API.postman_collection.json` | All endpoints with seed variables |
| `README.md` | Full endpoint tables + Railway setup |

**Backend commits:** marketplace APIs `30a0b86`, docs `4826c32`, catalog search fix `784afba`  
**Frontend commit:** full wire-up `c3faf82`

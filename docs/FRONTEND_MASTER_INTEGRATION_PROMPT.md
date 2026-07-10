# CivicBuild Frontend — Master Prompt: Wire All Remaining Features to Backend

You are completing backend integration for the **CivicBuild** Expo React Native app (SDK 54). The Spring Boot API is live. Your job is to wire **every feature that still uses mock data, local Zustand persistence, or hardcoded constants** to real HTTP calls — in one cohesive pass, with consistent patterns across the app.

Read Expo v54 docs before coding: https://docs.expo.dev/versions/v54.0.0/

---

## 0. Environment & global rules

```bash
EXPO_PUBLIC_API_URL=https://civicbuild-production.up.railway.app
# local: http://localhost:8081
```

**Non-negotiable integration rules:**

1. **Never send `file://` or `content://` URIs in JSON bodies.** Upload via multipart (`field name: file`) first, then send the returned HTTPS URL.
2. **Never trust local `managedAgencyId`.** Always read from `GET /api/users/me/onboarding` after login and after any onboarding mutation.
3. **Use existing `apiClient`** (`src/api/client.ts`) — it handles Bearer tokens and 401 refresh. Do not create a second HTTP client.
4. **Use the envelope pattern:** `{ success, message, data, errors, timestamp }`. Unwrap with `unwrapApiResponse` from `src/api/authTypes.ts`. Use `toApiResult` from `src/api/apiResult.ts` for `{ ok, data | error }` results.
5. **Pagination:** `?page=0&limit=20` → `data: { items, page, limit, total, hasNextPage }`.
6. **Errors:** Show `message`; map `errors: [{ field, message }]` via existing `normalizeApiError` in `src/api/errors.ts`.
7. **Loading / error / empty states:** Every screen that fetches must show ActivityIndicator while loading, user-friendly error with retry, and existing EmptyState when list is empty.
8. **Optimistic UI only where rollback is trivial.** Prefer server response as source of truth, then update local cache.
9. **Do not break already-wired flows.** See §2 — extend, don't rewrite.
10. **User constraint:** Do **not** modify Auth, Cart, Checkout, or Payment **screens** unless absolutely required. Checkout may be updated only in `src/utils/orderMappers.ts` to include `productId` in the payload (cart already has `productId`).

---

## 1. What is ALREADY wired (do not regress)

| Module | Files | Endpoints |
|--------|-------|-----------|
| Auth | `src/api/auth.ts`, Login/Register/Forgot/Verify/ChangePassword | `/api/auth/*` |
| Users | `src/api/users.ts`, EditProfileScreen | `GET/PATCH /api/users/me` |
| Account delete | `src/api/account.ts` | `DELETE /api/account` |
| Onboarding | `src/api/onboarding.ts`, authStore, RoleSelection, Verification | `/api/users/me/onboarding` |
| Agency create | VerificationScreen → `createAgency` | `POST /api/agencies` |
| Verification docs | `src/api/verification.ts`, VerificationUploadField | `/api/verification/*` |
| Portfolio upload | `src/api/agencyPortfolio.ts`, AgencyPortfolioScreen | `POST /api/agency/portfolio/upload` |
| Catalog products (read) | `src/api/catalog.ts`, productStore.fetchCatalog, HomeScreen | `GET /api/products` |
| Agency products CRUD | `src/api/agencies.ts`, AgencyProductForm/Products | `/api/agencies/me/products/*` |
| Checkout (partial) | `src/api/orders.ts`, checkoutService | `/api/orders/checkout`, verify, get, list |

---

## 2. What is NOT wired yet (your full scope)

Wire **all** of the following in this session.

### A. Agency posts (HIGH — navbar Create Post depends on this)

**Currently:** `src/store/agencyPostsStore.ts` + `src/constants/mockAgencyPosts.ts` + AsyncStorage  
**API exists in:** `src/api/agencies.ts` (`getMyAgencyPosts`, `createAgencyPost`, `updateAgencyPost`, `deleteAgencyPost`, `getAgencyPosts`) — **screens don't use it**

**Wire these files:**
- `src/screens/agency/AgencyPostFormScreen.tsx` — create/edit via API
- `src/screens/agency/AgencyPostsScreen.tsx` — list + delete via API
- `src/screens/agency/AgencyDashboardScreen.tsx` — fetch posts from API (not store seed)
- `src/screens/main/AgencyDetailScreen.tsx` — public `GET /api/agencies/{id}/posts`

**Needs:**
- Remove `useAgencyPostsStore` from all screens
- Delete or gut `agencyPostsStore.ts` and stop importing `SEED_AGENCY_POSTS`
- Map `BackendAgencyPost.imageUrl` → UI `imageUri` if needed
- Post image: if user picks local image, upload via product image endpoint or add post image upload if backend supports it; otherwise allow `imageUrl: null`
- After create from navbar tab → `navigation.goBack()` still works

---

### B. Agency portfolio list + delete

**Currently:** Upload hits API, but list is cached in `src/store/agencyPortfolioStore.ts`  
**API exists:** `getMyPortfolio`, `getAgencyPortfolio`, `deletePortfolioImage` in `src/api/agencies.ts`

**Wire:**
- `src/screens/agency/AgencyPortfolioScreen.tsx` — on mount call `GET /api/agencies/me/portfolio`; refetch after upload/delete; do not persist signed URLs long-term
- `src/screens/main/AgencyDetailScreen.tsx` — `GET /api/agencies/{id}/portfolio` for public view
- `src/screens/agency/AgencyDashboardScreen.tsx` — portfolio preview from API

**Delete:** `agencyPortfolioStore.ts` after migration

---

### C. Agency orders

**Currently:** `src/constants/mockAgencyOrders.ts`  
**API needed:** create `src/api/agencyOrders.ts` (or extend agencies.ts):

```
GET  /api/agencies/me/orders
GET  /api/agencies/me/orders/{orderId}
PATCH /api/agencies/me/orders/{orderId}/status?status=pending|processing|delivered|cancelled
```

**Wire:**
- `src/screens/agency/AgencyOrdersScreen.tsx`
- `src/screens/agency/AgencyOrderDetailScreen.tsx`
- `src/screens/agency/AgencyDashboardScreen.tsx` (latest order preview)

**Map backend status** to frontend `OrderStatus` (`pending | processing | delivered | cancelled`).

**Delete:** `mockAgencyOrders.ts` and all imports.

---

### D. Customer order history (API exists, UI not wired)

**Currently:** `listMyOrders` in `src/api/checkoutService.ts` — no screen uses it

**Wire:**
- Add order history section to Profile or a dedicated screen if one exists
- At minimum: ensure post-checkout flow can fetch `GET /api/orders/{orderId}` (already partially wired)

---

### E. Checkout hardening (mapper only — do not edit CheckoutScreen)

**Currently:** `src/utils/orderMappers.ts` sends product names without `productId`  
**Cart already has:** `productId` in `CartItem` (`src/types/cart.ts`)

**Change:** Include `productId` in each `BackendOrderItem` when mapping cart → checkout request.

---

### F. Suppliers & construction agency directory (catalog)

**Currently mock:**
- `src/constants/mockSuppliers.ts` → `TRUSTED_SUPPLIERS` in `marketplaceData.ts`
- `src/constants/constructionAgencies.ts` → `VERIFIED_CONSTRUCTION_AGENCIES`
- `src/constants/agencyProfiles.ts` → hardcoded profile details

**API:** `src/api/catalog.ts` has `getSuppliers`, `getSupplier`; `src/api/agencies.ts` has `listAgencies`, `getAgency`

**Wire:**
- `src/screens/main/HomeScreen.tsx` — suppliers carousel from `GET /api/suppliers`
- `src/screens/main/AllSuppliersScreen.tsx`
- `src/screens/main/SupplierDetailScreen.tsx`
- `src/screens/main/AgencyDetailScreen.tsx` — agency from `GET /api/agencies/{id}` + profile fields from response (replace `getAgencyProfile` mock)
- `src/components/delivery/ConstructionAgencySelect.tsx` — `GET /api/agencies?q=` instead of `VERIFIED_CONSTRUCTION_AGENCIES`
- `src/utils/productHelpers.ts`, `src/utils/roleLabels.ts` — resolve supplier/agency names from API or passed data, not mock constants

**Categories:** optionally replace `MARKETPLACE_CATEGORIES` with `GET /api/categories` on HomeScreen / AllSuppliers.

---

### G. Delivery provider setup & jobs

**Currently:** `src/store/deliveryPersonnelStore.ts` + local authStore `submitDeliveryProviderSetup`

**Create:** `src/api/delivery.ts`

```
POST   /api/delivery-providers/setup
GET    /api/delivery-providers/me
PATCH  /api/delivery-providers/me
DELETE /api/delivery-providers/me/association
GET    /api/delivery-providers/me/jobs
PATCH  /api/delivery-providers/me/jobs/{jobId}/status?status=assigned|in_transit|delivered
```

**Wire:**
- `src/screens/onboarding/DeliveryProviderSetupScreen.tsx` — POST setup, then `completeOnboarding` API, then `syncOnboardingFromServer`
- Profile photo: upload avatar first if local URI
- `src/screens/delivery/DeliveryDashboardScreen.tsx` — GET jobs, show real assigned jobs
- `src/store/authStore.ts` — `submitDeliveryProviderSetup` should call API, not local personnel store

**Delete:** seed data in `deliveryPersonnelStore.ts` for production paths (or entire store if agency personnel also moves to API)

---

### H. Agency personnel

**Currently:** `deliveryPersonnelStore` + `AgencyPersonnelScreen`, `NotificationsScreen`

**Create in** `src/api/agencies.ts` or `src/api/personnel.ts`:

```
GET    /api/agencies/me/personnel
POST   /api/agencies/me/personnel/{personnelId}/approve
POST   /api/agencies/me/personnel/{personnelId}/reject
DELETE /api/agencies/me/personnel/{personnelId}
```

**Wire:**
- `src/screens/agency/AgencyPersonnelScreen.tsx`
- `src/screens/agency/NotificationsScreen.tsx` — show personnel pending notifications from `GET /api/notifications` (see K)

---

### I. Saved / favorites

**Currently:** `src/store/savedStore.ts` (AsyncStorage only)

**Create:** `src/api/saved.ts`

```
GET    /api/users/me/saved
POST   /api/users/me/saved        { id, type: product|supplier|agency }
DELETE /api/users/me/saved/{type}/{id}
```

**Wire every `toggleSaved` / `isSaved` usage:**
- `HomeScreen`, `ProductDetailScreen`, `SupplierDetailScreen`, `AgencyDetailScreen`, `SavedScreen`, cards

**Pattern:** Optimistic toggle with rollback on error. On SavedScreen mount: GET saved → resolve IDs via catalog/agency APIs.

**Delete:** persisted local saved store (keep in-memory cache synced from server if useful).

---

### J. Reviews

**Currently:** `src/constants/mockReviews.ts`, `mockMyReviews.ts`  
**Screens:** `ReviewsScreen`, `MyReviewsScreen`, `ProfileScreen`, `ProductDetailScreen`, `SupplierDetailScreen`

**Create:** `src/api/reviews.ts`

```
GET  /api/reviews?subjectType=product|supplier&subjectId=
GET  /api/reviews/summary?subjectType=&subjectId=
GET  /api/reviews/me
POST /api/reviews
PATCH /api/reviews/{reviewId}
DELETE /api/reviews/{reviewId}
```

**Wire all review displays to API.** Add write-review UI on ProductDetail/SupplierDetail if missing (POST after verified purchase).

**Delete:** mock review constants from runtime paths.

---

### K. Messaging

**Currently:** `src/constants/messagesData.ts`  
**Screens:** `MessagesScreen`, `ConversationDetailScreen`

**Create:** `src/api/messages.ts`

```
GET   /api/messages/threads
POST  /api/messages/threads           { agencyId }
GET   /api/messages/threads/{threadId}
POST  /api/messages/threads/{threadId}/messages   { text }
PATCH /api/messages/threads/{threadId}/read
```

**Wire both screens.** Remove local `CHAT_MESSAGES_BY_THREAD` and fake send. Refetch thread on focus.

---

### L. Notifications

**Currently:** `NotificationsScreen` is empty placeholder

**Create:** `src/api/notifications.ts`

```
GET   /api/notifications
PATCH /api/notifications/{id}/read
PATCH /api/notifications/read-all
```

**Wire:** `src/screens/agency/NotificationsScreen.tsx` — list with read/unread; types: `order | verification | personnel | message`

---

### M. Profile avatar upload

**Currently:** `EditProfileScreen` sends local URI as `profilePictureUrl` in PATCH — **broken**

**Add to** `src/api/users.ts`:

```
POST /api/users/me/avatar   (multipart file)
→ { profilePictureUrl: "https://res.cloudinary.com/..." }
```

**Wire:** `EditProfileScreen` + `ProfileAvatarEditor` — upload file first, then PATCH with returned URL.

---

### N. Product store cleanup

**Currently:** `productStore` fetches catalog but agency CRUD still mixes local cache.

**Needs:**
- After any agency product mutation, call `fetchCatalog()` or surgically update from API response
- Remove any remaining references to `MOCK_PRODUCTS`, `initialize`, `extraProducts`, `removedProductIds`, `productOverrides`
- `getPopularProducts()` must return API data only

---

## 3. New API modules to create

```
src/api/
  saved.ts          ← NEW
  reviews.ts        ← NEW
  messages.ts       ← NEW
  notifications.ts  ← NEW
  delivery.ts       ← NEW
  agencyOrders.ts   ← NEW (or extend agencies.ts)

src/types/
  savedApi.ts
  reviewsApi.ts
  messagesApi.ts
  notificationsApi.ts
  deliveryApi.ts
  agencyOrdersApi.ts
```

Follow the pattern in `src/api/onboarding.ts` and `src/api/catalog.ts`.

---

## 4. Stores & constants to remove or stop using

**Remove from runtime (delete files or leave only for tests if needed):**

| File | Reason |
|------|--------|
| `src/store/agencyPostsStore.ts` | Replaced by posts API |
| `src/store/agencyPortfolioStore.ts` | Replaced by portfolio API |
| `src/store/savedStore.ts` | Replaced by saved API |
| `src/store/deliveryPersonnelStore.ts` | Replaced by delivery + personnel API |
| `src/constants/mockAgencyOrders.ts` | Replaced by agency orders API |
| `src/constants/mockAgencyPosts.ts` | Replaced by posts API |
| `src/constants/mockReviews.ts` | Replaced by reviews API |
| `src/constants/mockMyReviews.ts` | Replaced by reviews API |
| `src/constants/messagesData.ts` | Replaced by messages API |
| `src/constants/mockSuppliers.ts` | Replaced by suppliers API |
| `src/constants/constructionAgencies.ts` | Replaced by agencies API |
| `src/constants/agencyProfiles.ts` | Replaced by `GET /api/agencies/{id}` |
| `src/constants/mockProducts.ts` | Replaced by products API |

**Keep:** `mockCheckout.ts` only when `EXPO_PUBLIC_USE_MOCK_CHECKOUT=true`.

---

## 5. authStore requirements

- `managedAgencyId` must always come from `syncOnboardingFromServer()` — never set locally except as temporary UI before server confirms
- After delivery setup, agency create, or onboarding complete: always call `syncOnboardingFromServer()`
- Remove any re-introduced `onboardingProfilesByUserId` local RBAC persistence
- `submitDeliveryProviderSetup` must call backend then refresh onboarding state

---

## 6. UI screens → API mapping (complete checklist)

| Screen | Must call |
|--------|-----------|
| AgencyPostFormScreen | POST/PATCH `/api/agencies/me/posts` |
| AgencyPostsScreen | GET/DELETE posts |
| AgencyPortfolioScreen | GET/DELETE portfolio + existing upload |
| AgencyOrdersScreen | GET agency orders |
| AgencyOrderDetailScreen | GET order + PATCH status |
| AgencyPersonnelScreen | GET personnel + approve/reject/delete |
| AgencyDashboardScreen | API for posts, orders, products, portfolio previews |
| AgencyDetailScreen (customer) | GET agency, posts, portfolio, products |
| DeliveryProviderSetupScreen | POST delivery setup + complete onboarding |
| DeliveryDashboardScreen | GET delivery jobs |
| ConstructionAgencySelect | GET `/api/agencies` |
| HomeScreen | GET products + suppliers (+ categories) |
| AllSuppliersScreen | GET suppliers |
| SupplierDetailScreen | GET supplier + reviews |
| ProductDetailScreen | GET product + reviews |
| SavedScreen | GET saved + resolve catalog |
| ReviewsScreen | GET reviews + summary |
| MyReviewsScreen | GET `/api/reviews/me` |
| MessagesScreen | GET threads |
| ConversationDetailScreen | GET messages + POST message + PATCH read |
| NotificationsScreen | GET notifications |
| EditProfileScreen | POST avatar + PATCH profile |
| ProfileScreen | GET reviews/me summary from API |

---

## 7. Backend seed UUIDs (use in tests)

**Products:**
- `b2000001-0000-4000-8000-000000000001` Dangote Cement — 88 GHS
- `b2000001-0000-4000-8000-000000000002` Iron Rods 12mm — 45 GHS

**Suppliers:**
- `a1000001-0000-4000-8000-000000000001` BuildMart Ghana

---

## 8. Implementation order (follow this sequence)

1. Create all missing `src/api/*` modules with types
2. Agency posts (navbar create flow)
3. Portfolio list/delete
4. Agency orders
5. Suppliers + agencies directory + AgencyDetail
6. Delivery setup + jobs + personnel
7. Saved items
8. Reviews
9. Messages
10. Notifications
11. Avatar upload
12. Checkout mapper `productId`
13. Delete mock stores/constants
14. `npm run typecheck` — fix all errors
15. Manual smoke test flows A–D below

---

## 9. Validation flows (must pass)

**Flow A — Customer:** Browse API products → save product → checkout with `productId` → Paystack → verify → order in history

**Flow B — Agency:** Onboarding construction → create agency → create post (navbar) → upload portfolio → list products → see agency orders

**Flow C — Delivery:** Onboarding delivery → pick agency from API → POST setup → agency approves personnel → delivery dashboard shows jobs

**Flow D — Social:** Message thread create + send → notification appears → reviews on product → saved items persist across reinstall

---

## 10. Explicit do-nots

- Do not add hardcoded agency IDs like `buildstrong-ltd`
- Do not persist onboarding RBAC in AsyncStorage
- Do not store Cloudinary signed URLs long-term — refetch on preview
- Do not modify Auth/Cart/Checkout/Payment **screens** (mapper-only exception for checkout)
- Do not implement `verify-email` / `resend-verification` (not on backend)
- Do not add WebSockets in this pass (REST + refetch on focus is fine)
- Do not leave dead imports from deleted mock files

---

## 11. Definition of done

- [ ] Zero runtime imports from mock constants listed in §4
- [ ] Zero persisted Zustand stores for posts, portfolio, saved, personnel
- [ ] All §6 screens fetch from API with loading/error/empty states
- [ ] `npm run typecheck` passes
- [ ] Create Post from navbar persists to backend and appears on agency detail for customers
- [ ] Reinstall app + login restores saved items, onboarding, and role from server

---

**Frontend repo:** `/Users/mac/Pictures/CivicBuildFrontend`  
**Backend API:** `https://civicbuild-production.up.railway.app`

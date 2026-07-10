# CivicBuild Frontend — API Integration Guide

**This is the doc the frontend team should use.**

| Document | What it is |
|----------|------------|
| **[FRONTEND_API_REFERENCE.md](./FRONTEND_API_REFERENCE.md)** | **Postman-style API reference** — every endpoint, request body, live Railway response examples |
| `postman/CivicBuild-API.postman_collection.json` | Import into Postman — runnable requests with seed variables |
| `FRONTEND_MASTER_INTEGRATION_PROMPT.md` | Agent prompt for validation/polish only (not API docs) |

---

## Setup

```bash
# .env or app.config — Railway production
EXPO_PUBLIC_API_URL=https://civicbuild-production.up.railway.app
```

All requests go through `src/api/client.ts` with `Authorization: Bearer <accessToken>`.

---

## Response envelope (every endpoint)

```json
{
  "success": true,
  "message": null,
  "data": {},
  "errors": null,
  "timestamp": "2026-07-10T18:26:07Z"
}
```

Unwrap in app: `unwrapApiResponse()` / `toApiResult()`.

---

## Copy-paste test calls (Railway)

Replace `TOKEN` after login.

### No auth
```
GET  https://civicbuild-production.up.railway.app/api/health
GET  https://civicbuild-production.up.railway.app/api/products?page=0&limit=20
GET  https://civicbuild-production.up.railway.app/api/suppliers?page=0&limit=20
GET  https://civicbuild-production.up.railway.app/api/products/b2000001-0000-4000-8000-000000000001
```

### With auth
```
GET  https://civicbuild-production.up.railway.app/api/users/me/onboarding
     Authorization: Bearer TOKEN

POST https://civicbuild-production.up.railway.app/api/users/me/saved
     Authorization: Bearer TOKEN
     Content-Type: application/json
     {"id":"b2000001-0000-4000-8000-000000000001","type":"product"}

POST https://civicbuild-production.up.railway.app/api/orders/checkout
     Authorization: Bearer TOKEN
     Content-Type: application/json
     {"items":[{"productId":"b2000001-0000-4000-8000-000000000001","productName":"Dangote Cement 50kg","supplierName":"BuildMart Ghana","unitPrice":88,"quantity":2,"unit":"per bag"}],"delivery":{"address":"12 Market Rd","city":"Accra","region":"Greater Accra","phoneNumber":"+233201234567"}}
```

Full request/response shapes: **[FRONTEND_API_REFERENCE.md](./FRONTEND_API_REFERENCE.md)**

---

## Integration status

Frontend commit `c3faf82` — all modules wired. See API reference §17 for file map.

---

## Seed UUIDs

| Entity | UUID |
|--------|------|
| Dangote Cement | `b2000001-0000-4000-8000-000000000001` |
| BuildMart Ghana | `a1000001-0000-4000-8000-000000000001` |

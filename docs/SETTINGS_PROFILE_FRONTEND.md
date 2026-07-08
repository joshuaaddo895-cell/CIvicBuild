# Settings, Change Password & Edit Profile — Expo Frontend Guide

The **backend** changes for this session are in this repo. Your **Expo frontend is a separate project** — apply these steps there.

**Production API:** `https://civicbuild-production.up.railway.app`  
**Local:** `http://localhost:8081`

---

## Item 4 — Register flow (already correct on backend)

`POST /api/auth/register` returns **201** with `UserResponse` only — **no tokens**.

```json
{
  "success": true,
  "message": "Registration successful. Please sign in.",
  "data": {
    "id": "...",
    "fullName": "...",
    "email": "...",
    "role": "CUSTOMER",
    "verificationStatus": "UNVERIFIED",
    "active": true,
    "profilePictureUrl": null,
    "createdAt": "..."
  }
}
```

### Frontend revert (check your Expo app)

Remove any auto-login after register:

```typescript
// ❌ REMOVE — do not call login() after register
// const loginRes = await api.login(email, password);

// ✅ CORRECT — navigate to Sign In
await api.register(fullName, email, password);
router.replace('/sign-in'); // or navigation.navigate('SignIn')
showToast('Account created. Please sign in.');
```

Search your frontend for: `register` then `login`, `issueTokens`, `setTokens` after signup.

---

## Item 5 — Logout (unchanged, verify intact)

Backend: `POST /api/auth/logout` with `{ refreshToken }` — revokes that refresh token.

Frontend must still:
1. Call logout API with stored refresh token
2. Clear `accessToken` + `refreshToken` from SecureStore/AsyncStorage
3. Navigate to Sign In

No backend changes were made to logout in this session.

---

## 1. Settings screen cleanup

**Remove entirely:**
- Language
- Data Permissions
- Privacy Policy
- Email Notifications toggle
- Push Notifications toggle

**Keep under Account:**
- Change Password → navigates to Change Password screen
- Log Out
- Delete Account (`DELETE /api/account`)

Delete mock state/constants only used by removed items (e.g. `notificationSettings`, `languageOptions`).

**Do NOT add Change Email** — out of scope.

---

## 2. Change Password

### API

```
POST /api/auth/change-password
Authorization: Bearer <accessToken>
Content-Type: application/json

{
  "currentPassword": "Secret123",
  "newPassword": "NewSecret456"
}
```

**Success (200):**
```json
{ "success": true, "message": "Password updated successfully" }
```

**Wrong current password (403):**
```json
{ "success": false, "message": "Current password is incorrect" }
```

**Validation error (400):** same rules as registration (8+ chars, letter + number).

**Google-only account (403):** `"This account uses Google Sign-In"`

### Security behavior

- All **refresh tokens** are revoked (same as password reset)
- Current **access token** stays valid until natural expiry (~15 min)
- User can keep using the app briefly; refresh will fail → must sign in with new password
- Other devices lose refresh ability immediately

### Frontend screen (`Settings → Account → Change Password`)

| Field | Notes |
|-------|-------|
| Current Password | show/hide toggle |
| New Password | show/hide toggle, same strength rules as Sign Up |
| Confirm New Password | show/hide toggle; client-side match validation |

**Confirm mismatch error:** `ERROR: Password do not match` (red border, same as Sign Up)

**Button:** full-width green `Update Password`, disabled until all fields filled + new/confirm match

**On submit:**
```typescript
try {
  await api.changePassword(currentPassword, newPassword);
  showToast('Password updated successfully');
  router.back(); // to Settings
} catch (e) {
  if (e.status === 403 && e.message.includes('Current password')) {
    setError('Current password is incorrect');
  } else {
    setError(e.message);
  }
}
```

---

## 3. Edit Profile (Profile screen, not Settings)

### API

```
GET /api/users/me
Authorization: Bearer <accessToken>
```

```
PATCH /api/users/me
Authorization: Bearer <accessToken>
Content-Type: application/json

{
  "fullName": "Jane Doe",
  "profilePictureUrl": "https://cdn.example.com/avatar.jpg"
}
```

**Response (200):** updated `UserResponse` in `data` — refresh auth store / context.

### Frontend screen (`Profile → Edit Profile`)

| Field | Behavior |
|-------|----------|
| Full Name | editable, pre-filled from `GET /api/users/me` |
| Profile Picture | `expo-image` picker/upload — all roles (reuse Delivery Provider upload pattern) |
| Email | **display only**, disabled/greyed out |

**Button:** full-width green `Save Changes`

```typescript
const updated = await api.patchProfile({ fullName, profilePictureUrl });
authStore.setUser(updated);
showToast('Profile updated');
router.back();
```

---

## API client additions (TypeScript)

```typescript
changePassword(currentPassword: string, newPassword: string) {
  return fetch(`${API_URL}/api/auth/change-password`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${accessToken}` },
    body: JSON.stringify({ currentPassword, newPassword }),
  });
}

getProfile() {
  return fetch(`${API_URL}/api/users/me`, {
    headers: { Authorization: `Bearer ${accessToken}` },
  });
}

updateProfile(body: { fullName: string; profilePictureUrl?: string | null }) {
  return fetch(`${API_URL}/api/users/me`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${accessToken}` },
    body: JSON.stringify(body),
  });
}

deleteAccount() {
  return fetch(`${API_URL}/api/account`, {
    method: 'DELETE',
    headers: { Authorization: `Bearer ${accessToken}` },
  });
}
```

---

## Testing checklist

- [ ] Settings shows only Account (Change Password, Log Out, Delete Account)
- [ ] Sign Up → navigates to Sign In (no auto-login)
- [ ] Change Password with wrong current → 403 inline error
- [ ] Change Password success → toast + back to Settings
- [ ] After change password, old refresh token fails; login with new password works
- [ ] Edit Profile saves name + picture; email not editable
- [ ] Logout clears tokens and requires Sign In

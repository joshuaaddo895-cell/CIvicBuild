# Google Sign-In — Expo / React Native Frontend Guide

The CivicBuild **backend** (`POST /api/auth/google`) is implemented in this repo.  
Your **Expo frontend is in a separate project** — apply these steps there.

---

## Flow (confirmed)

```
User taps "Continue with Google"
  → @react-native-google-signin/google-signin gets Google ID token
  → Frontend POSTs { idToken } to http://localhost:8081/api/auth/google
  → Backend verifies token with Google public keys (audience = WEB client ID)
  → Backend find-or-create User by email, issues OUR JWT access + refresh tokens
  → Frontend stores tokens same as manual login → same navigation (Role Selection / Dashboard)
```

The frontend **never** trusts Google's token for API calls after sign-in — only **our** JWT access token.

---

## Requirements

- **Custom dev build required** — Google Sign-In does **not** work in Expo Go.
- Run: `npx expo prebuild` then `npx expo run:android` / `npx expo run:ios`, or `eas build --profile development`.
- Other screens can still be tested in Expo Go; only the Google button needs a dev build.

---

## 1. Install

```bash
npx expo install @react-native-google-signin/google-signin
```

---

## 2. Environment variables (frontend `.env`)

Use **public** Expo env vars (never put `GOOGLE_WEB_CLIENT_SECRET` in the app):

```env
EXPO_PUBLIC_API_URL=http://localhost:8081
EXPO_PUBLIC_GOOGLE_WEB_CLIENT_ID=737938877454-i625gs5j47rb1u8dj0b84laivu19fhg1.apps.googleusercontent.com
EXPO_PUBLIC_GOOGLE_IOS_CLIENT_ID=737938877454-c1otu82hgk20cn2lo7eaiqh1d9de44ji.apps.googleusercontent.com
```

> Only `EXPO_PUBLIC_GOOGLE_WEB_CLIENT_ID` is referenced in app code via `GoogleSignin.configure()`.  
> Android/iOS client IDs are registered in Google Cloud + Expo config plugin only.

---

## 3. `app.config.js` / `app.json`

```javascript
export default {
  expo: {
    // Must match Google Cloud Console exactly:
    android: {
      package: "com.civicbuild.app",  // ← confirm your value
    },
    ios: {
      bundleIdentifier: "com.civicbuild.app",  // ← confirm your value
    },
    plugins: [
      [
        "@react-native-google-signin/google-signin",
        {
          iosUrlScheme: "com.googleusercontent.apps.737938877454-c1otu82hgk20cn2lo7eaiqh1d9de44ji"
          // Reversed iOS client ID: com.googleusercontent.apps.<id-without-suffix>
        }
      ]
    ],
  },
};
```

**iosUrlScheme** = reverse the iOS client ID:
- Client ID: `737938877454-c1otu82hgk20cn2lo7eaiqh1d9de44ji.apps.googleusercontent.com`
- URL scheme: `com.googleusercontent.apps.737938877454-c1otu82hgk20cn2lo7eaiqh1d9de44ji`

After changing config:

```bash
npx expo prebuild --clean
npx expo run:android   # or run:ios
```

---

## 4. Configure Google Sign-In (app startup)

```typescript
import { GoogleSignin } from '@react-native-google-signin/google-signin';

GoogleSignin.configure({
  webClientId: process.env.EXPO_PUBLIC_GOOGLE_WEB_CLIENT_ID,
  iosClientId: process.env.EXPO_PUBLIC_GOOGLE_IOS_CLIENT_ID, // iOS only
  offlineAccess: false,
});
```

---

## 5. Wire "Continue with Google" button

```typescript
import { GoogleSignin, statusCodes } from '@react-native-google-signin/google-signin';

async function signInWithGoogle() {
  try {
    await GoogleSignin.hasPlayServices();
    const result = await GoogleSignin.signIn();
    const idToken = result.data?.idToken;
    if (!idToken) throw new Error('No ID token from Google');

    const res = await fetch(`${process.env.EXPO_PUBLIC_API_URL}/api/auth/google`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ idToken }),
    });

    if (!res.ok) {
      const err = await res.json();
      throw new Error(err.message ?? 'Google sign-in failed');
    }

    const { data } = await res.json();
    // Store data.accessToken + data.refreshToken using your existing auth store
    // Navigate same as manual login (Role Selection if new, Dashboard if onboarded)
  } catch (error: any) {
    if (error.code === statusCodes.SIGN_IN_CANCELLED) {
      return; // user cancelled — silent, not an error
    }
    // show retry / error message
  }
}
```

---

## 6. Google Cloud checklist

| Platform | Registered in Google Cloud | Referenced in app code |
|---|---|---|
| Web | ✅ `GOOGLE_WEB_CLIENT_ID` | ✅ `webClientId` in `GoogleSignin.configure()` |
| Android | ✅ package + SHA-1 | ❌ (implicit via native SDK) |
| iOS | ✅ bundle ID | ✅ `iosUrlScheme` in config plugin |

**SHA-1 (Android dev):** `3B:DD:73:65:EA:35:E1:93:BF:5C:AE:95:91:53:F5:A4:9B:D7:16:5F`

---

## 7. Backend endpoint

```
POST /api/auth/google
Content-Type: application/json

{ "idToken": "<from Google Sign-In>" }

→ 200 { success, data: { accessToken, refreshToken, tokenType, expiresIn } }
→ 401 invalid Google token
→ 400 account inactive
```

---

## Notes

- **Linking:** same email as manual signup → same account (no duplicate).
- **Google-only accounts:** `password_hash` is null; manual login returns *"This account uses Google Sign-In"*.
- **New Google users:** `role=CUSTOMER`, `verification_status=UNVERIFIED`, same onboarding as manual signup.
- **`GOOGLE_WEB_CLIENT_SECRET`:** stored in backend `.env` for reference only — **not used** for ID-token verification.

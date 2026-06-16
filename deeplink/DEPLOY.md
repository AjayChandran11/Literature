# Deep-link room invites — deployment

Phase 2a growth feature. The app code (manifest intent filters, intent parsing, invite
UI, share sheet) is done. To make invite links **auto-open the app** on Android 12+, two
things must be hosted on the web. One is in this repo; one needs a new repo.

## The invite link

```
https://ajaychandran11.github.io/Literature/join.html?room=ABC123
```

- Host: `ajaychandran11.github.io` (matches the manifest App Link + `InviteLink.kt`).
- `join.html` is committed at the repo root → GitHub Pages serves it at `/Literature/join.html`.
  Nothing to do here beyond merging this branch; Pages already serves `master` root.

## 1. `assetlinks.json` at the domain ROOT  ✅ DONE (repo created & live)

Android verifies App Links per-host by fetching:

```
https://ajaychandran11.github.io/.well-known/assetlinks.json
```

This **must** be at the domain root. The `Literature` repo serves under `/Literature/`,
so it can't host the root path. A GitHub **user-site** repo was created for it:

- Repo: **`AjayChandran11/ajaychandran11.github.io`** (public).
- Files: `.well-known/assetlinks.json` (placeholder fingerprints), `index.html` (redirect
  to the Literature page), `.nojekyll` (so Pages serves the `.well-known/` dotfolder —
  Jekyll strips dot-folders otherwise).
- Pages: enabled (auto for `*.github.io` sites), serving from `main` root.
- Verified live: `https://ajaychandran11.github.io/.well-known/assetlinks.json` → 200,
  `application/json`.

**Only remaining step here: fill in the two real SHA-256 fingerprints** (next section) —
edit `.well-known/assetlinks.json` in that repo and replace the two placeholders.

## 2. SHA-256 fingerprints

The app uses **Play App Signing**, so there are two certs — list BOTH:

- **App signing key**: Play Console → your app → *Test and release → Setup → App integrity*
  → *App signing key certificate* → copy the **SHA-256 certificate fingerprint**.
- **Upload key**: same page → *Upload key certificate* → **SHA-256 fingerprint**.

Paste them into the `sha256_cert_fingerprints` array (replace the two placeholders),
keeping the colon-separated uppercase-hex format Play gives you.

## 3. Release the app update

The intent filters ship in a new build. Bump `versionCode`/`versionName` in
`composeApp/build.gradle.kts`, build the AAB, and upload to Play. After Google processes
it, the App Link host is auto-verified against the assetlinks file above.

## 4. Verify

- `adb shell pm get-app-links com.cards.game.literature` → host should show `verified`.
- Tap an invite link from WhatsApp/Messages with the app installed → opens the room.
- Tap with the app NOT installed → `join.html` → Play Store.
- Test the App Link directly:
  `adb shell am start -a android.intent.action.VIEW -d "https://ajaychandran11.github.io/Literature/join.html?room=ABC123"`
- Test the custom scheme:
  `adb shell am start -a android.intent.action.VIEW -d "literature://join?room=ABC123"`

## Notes

- iOS Universal Links are deferred to Phase 4 (iOS launch). The `Sharer` iOS actual exists
  so the shared module compiles; no iOS deep-link entitlement is configured yet.
- `assetlinks.json` and this file live under `deeplink/` only as the source of truth — they
  are NOT the deployed copies. The deployed copy goes in the `ajaychandran11.github.io` repo.

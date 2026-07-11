# LiveTranslate Pro

[![Android CI](https://github.com/piyushkumarexe/LiveTranslate-Pro/actions/workflows/android-ci.yml/badge.svg)](https://github.com/piyushkumarexe/LiveTranslate-Pro/actions/workflows/android-ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-635BFF.svg)](LICENSE)

A modern Android translator for live speech, two-person conversations, typed text, and camera/gallery OCR. Built with Kotlin, Jetpack Compose, Clean Architecture, Room, Firebase, and a server-side Groq adapter.

> **Security:** AI credentials are never packaged in the APK. Android calls an authenticated Firebase Callable Function; the function reads `GROQ_API_KEY` from Firebase Secret Manager.

## Highlights

- Live speech-to-text with partial subtitles and low-latency debounced translation
- Speech-to-speech, text-to-text, and text-to-voice
- Two-person interpreter mode with automatic turn switching
- Automatic language detection and dedicated Hinglish prompting
- Camera and gallery OCR with editable recognized text
- Room-backed history, search, favorites, export, settings, metadata, and response cache
- Firebase Google Authentication, Firestore sync, Crashlytics, Analytics, Storage, and FCM entry point
- Material 3 light/dark/dynamic themes, responsive layouts, animations, and accessible controls
- Offline-first writes with WorkManager sync when connectivity returns
- Modular provider boundary for replacing Groq later

## Modules

```text
app                  Composition root, navigation, FCM, application setup
core:model           Shared immutable models and language catalog
core:common          Connectivity and platform utilities
core:database        Room entities, DAOs, database, DI
core:ui              Material 3 theme and reusable UI components
domain                Repository contracts and use cases
data:auth             Firebase Auth and Firestore user/device profiles
data:translation      AI adapter, sync, settings, speech, TTS, caching
feature:auth          Onboarding and Google login
feature:home          Home and profile
feature:translate     Live, conversation, and OCR experiences
feature:history       Search, delete, favorites, export, cloud sync
feature:settings      Settings, privacy, and about
functions             Firebase Functions Groq proxy and rate limiting
```

See [Architecture](docs/ARCHITECTURE.md), [database design](docs/DATABASE.md), [authentication flow](docs/AUTHENTICATION.md), and [setup/build guide](docs/SETUP.md).

## Quick start

### Prerequisites

- Android Studio Ladybug or newer
- JDK 17
- Android SDK 35
- A Firebase project on the Blaze plan for outbound Cloud Functions calls
- Firebase CLI and Node.js 20 for backend deployment

### Android

1. Clone the repository.
2. Register the Android app `com.piyush.livetranslate` in Firebase and add both debug and release SHA-1/SHA-256 signing fingerprints.
3. Download its Firebase configuration as `app/google-services.json`.
4. Enable **Authentication → Google**, Firestore, Storage, Crashlytics, and Cloud Messaging.
5. Deploy rules and the translation function (instructions below).
6. Build:

```bash
./gradlew :app:assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

`google-services.json` is intentionally gitignored. The Google web OAuth client ID is extracted from it at build time. As a fallback, set `GOOGLE_WEB_CLIENT_ID` in untracked `local.properties` or the environment.

### Secure Groq setup

Do **not** place a Groq key in `local.properties`, Gradle fields, source code, or GitHub Actions Android build secrets. A key embedded in an APK can always be recovered.

```bash
npm install -g firebase-tools
firebase login
firebase use YOUR_FIREBASE_PROJECT_ID
firebase functions:secrets:set GROQ_API_KEY
npm --prefix functions ci
npm --prefix functions run build
firebase deploy --only functions,firestore:rules,firestore:indexes,storage
```

Optionally select a model without code changes:

```bash
firebase functions:config:set # not used for secrets
# During deploy, set the GROQ_MODEL parameter when prompted, or accept the default.
```

The callable function requires Firebase Authentication, validates input, enforces a per-user rate limit, treats source text as untrusted data, and limits runtime/instances.

## Firebase collections

| Collection | Document ID | Purpose |
|---|---|---|
| `users` | `{uid}` | Profile, created time, last login |
| `devices` | `{uid}_{deviceHash}` | Device and FCM metadata |
| `translations` | `{translationId}` | User-owned translation records |
| `favorites` | `{uid}_{translationId}` | User-owned favorite references |
| `settings` | `{uid}` | Synced preferences |
| `rateLimits` | `{uid}` | Server-only Functions rate limit state |

Firestore persistence plus Room/WorkManager provide delayed synchronization. Rules deny cross-user access and deny all unspecified paths.

## GitHub Actions and releases

- Every push/PR runs unit checks, builds the Firebase Functions TypeScript, and creates a Debug APK artifact.
- Tags matching `v*` build a minified, signed release APK and publish a GitHub Release.

Configure these GitHub Actions secrets before tagging:

- `GOOGLE_SERVICES_JSON`
- `RELEASE_KEYSTORE_BASE64`
- `RELEASE_STORE_PASSWORD`
- `RELEASE_KEY_ALIAS`
- `RELEASE_KEY_PASSWORD`

The Groq key belongs in Firebase Secret Manager, **not** GitHub's Android build workflow.

## Testing

```bash
./gradlew testDebugUnitTest :core:model:test :domain:test lintDebug
npm --prefix functions run build
```

Use Firebase Emulator Suite for rules and function development. Speech recognition, TTS voices, and Credential Manager behavior should also be tested on physical devices from multiple OEMs.

## Performance and resilience

- Compose state is Flow-backed and lifecycle-aware.
- Partial speech results are debounced before network calls.
- SHA-256 keyed results are cached for 30 days.
- WorkManager uses connected-network constraints and bounded retries.
- Room is the UI source of truth; cloud synchronization does not block browsing.
- R8 and resource shrinking are enabled for release builds.

Actual latency depends on the Android speech service, network path, Firebase region, selected Groq model, and TTS engine. Deploy Functions near the expected user base and measure end-to-end p50/p95 before defining an SLA.

## Roadmap

- Firebase App Check enforcement with Play Integrity
- Streaming backend transport for token-level translation updates
- Multi-script OCR models and document layout preservation
- Downloadable on-device translation fallback
- End-to-end sync tombstones and cursor-based pagination
- Wear OS / Android Auto companion experiences
- Macrobenchmark and baseline profiles

## Contributing

Read [CONTRIBUTING.md](CONTRIBUTING.md). Please report security issues privately as described in [SECURITY.md](SECURITY.md).

## Branding

“Made by Piyush” is displayed subtly on the splash, About page, and Settings footer.

## License

MIT © 2026 Piyush. See [LICENSE](LICENSE).

# Setup and build guide

## Local configuration

```bash
git clone https://github.com/piyushkumarexe/LiveTranslate-Pro.git
cd LiveTranslate-Pro
cp local.properties.example local.properties
# Set sdk.dir, then copy Firebase config:
cp ~/Downloads/google-services.json app/google-services.json
./gradlew :app:assembleDebug
```

If Firebase config is absent, the project still compiles for CI/review, but login, cloud sync, crash reporting, messaging, and AI translation stay unavailable at runtime.

## Firebase

```bash
firebase login
firebase use YOUR_PROJECT_ID
firebase functions:secrets:set GROQ_API_KEY
npm --prefix functions ci
npm --prefix functions run build
firebase deploy --only functions,firestore:rules,firestore:indexes,storage
```

Do not use the Groq key originally pasted into chats, logs, tickets, or commits. Revoke exposed keys and set a newly generated value directly through the Firebase CLI prompt.

## APK guide

Debug:

```bash
./gradlew clean :app:assembleDebug
```

Unsigned/minified local release:

```bash
./gradlew :app:assembleRelease
```

Signed release (environment variables):

```bash
export RELEASE_STORE_FILE=/absolute/or/root-relative/path/release.jks
export RELEASE_STORE_PASSWORD='...'
export RELEASE_KEY_ALIAS='...'
export RELEASE_KEY_PASSWORD='...'
./gradlew :app:assembleRelease
```

APK locations:

```text
app/build/outputs/apk/debug/app-debug.apk
app/build/outputs/apk/release/app-release.apk
```

Generate a signing key once and store it in a password manager/secure CI secret store:

```bash
keytool -genkeypair -v -keystore release.jks -alias livetranslate -keyalg RSA -keysize 4096 -validity 10000
```

Losing the signing key prevents updates to an existing distribution identity.

## GitHub release setup

Base64-encode the keystore and configure repository secrets listed in the README. Push a semantic tag:

```bash
git tag -a v1.0.0 -m "LiveTranslate Pro 1.0.0"
git push origin v1.0.0
```

The release workflow validates required secrets, builds the minified signed APK, uploads checksums, and creates a GitHub Release.

# Database design

## Room

```mermaid
erDiagram
  TRANSLATIONS ||--o| FAVORITES : "translationId"
  TRANSLATIONS {
    string id PK
    string userId
    string sourceText
    string translatedText
    string sourceLanguage
    string targetLanguage
    string detectedLanguage
    float confidence
    string origin
    long createdAt
    long updatedAt
    string syncState
  }
  FAVORITES {
    string translationId PK,FK
    string userId
    long createdAt
    string syncState
  }
  RECENT_LANGUAGES {
    string languageTag PK
    string displayName
    long lastUsedAt
    int useCount
  }
  USER_SETTINGS {
    string profile PK
    string themeMode
    string appLanguage
    string voiceGender
    float voiceSpeed
    boolean dynamicColor
    boolean onboardingComplete
    boolean cloudSyncEnabled
  }
  OFFLINE_METADATA {
    string key PK
    string value
    long updatedAt
  }
  CACHED_RESULTS {
    string cacheKey PK
    string sourceText
    string translatedText
    string detectedLanguage
    float confidence
    string provider
    long createdAt
    long expiresAt
  }
```

Room schema JSON is exported under `core/database/schemas` during builds. Every future schema change must include an explicit migration and migration tests before release.

## Firestore

All user data is stored in top-level collections to support direct ownership queries:

```text
users/{uid}
translations/{translationId}
favorites/{uid}_{translationId}
settings/{uid}
devices/{uid}_{hashedDeviceId}
rateLimits/{uid}                  # server-only
```

Each mutable user-owned document carries `userId`. Security rules check both existing and incoming ownership, preventing ownership transfer. The Functions Admin SDK alone accesses `rateLimits`.

## Retention

- Cached AI results expire logically after 30 days and are pruned/cleared locally.
- History persists until the user deletes it.
- Device/FCM entries should be removed with an account-deletion backend in a production deployment.

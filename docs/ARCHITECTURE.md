# Architecture

LiveTranslate Pro uses feature modules around a unidirectional UI flow. The Android app treats Room as its local source of truth and Firebase as an authenticated synchronization/backend boundary.

```mermaid
flowchart LR
  UI[Compose feature screens] --> VM[Hilt ViewModels]
  VM --> UC[Domain use cases / contracts]
  UC --> REPO[Repository implementations]
  REPO --> ROOM[(Room)]
  REPO --> FIRE[(Firestore)]
  REPO --> FUNC[Firebase Callable Function]
  FUNC --> GROQ[Groq API]
  REPO --> ML[ML Kit Language ID / OCR]
  VM --> SPEECH[Android SpeechRecognizer]
  VM --> TTS[Android TextToSpeech]
  WM[WorkManager] --> REPO
  FCM[Firebase Messaging] --> APP[Application routing]
```

## Dependency rule

- `core:model` has no Android dependency.
- `domain` depends only on shared models and coroutines.
- data modules implement domain contracts.
- feature modules consume contracts, not concrete repositories.
- `app` is the composition and navigation root.

The exception is platform UI integration required directly by a feature (Credential Manager and ML Kit OCR input). Platform engines still sit behind domain interfaces where reusable behavior is needed.

## Translation sequence

```mermaid
sequenceDiagram
  participant S as SpeechRecognizer
  participant VM as TranslateViewModel
  participant R as TranslationRepository
  participant C as Room Cache
  participant F as Firebase Function
  participant G as Groq
  S-->>VM: partial/final text
  VM->>VM: debounce partials
  VM->>R: TranslationRequest
  R->>C: cache lookup
  alt cache hit
    C-->>R: result
  else cache miss
    R->>F: authenticated callable
    F->>G: server-side request + secret
    G-->>F: structured JSON
    F-->>R: validated result
    R->>C: cache result
  end
  R-->>VM: TranslationResult
  R->>C: save final to history
```

## Synchronization

1. Save local translations/favorites with `PENDING` state.
2. Enqueue unique connected-network work.
3. Upload pending rows with ownership metadata.
4. Mark uploaded local rows `SYNCED`.
5. Pull the latest user-owned cloud snapshot and merge by stable UUID.
6. Firestore's native persistence additionally queues writes while offline.

For very large datasets, replace the bounded snapshot with cursor-based delta synchronization and explicit deletion tombstones.

## Security boundaries

- Groq secret: Firebase Secret Manager only.
- Identity: Firebase ID token on callable requests.
- Authorization: Firestore/Storage ownership rules.
- Device identifier: one-way SHA-256 hash, truncated for document addressing.
- Local database: app-private sandbox; no backup. Add SQLCipher if the threat model requires database-at-rest encryption beyond Android file-based encryption.

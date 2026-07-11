# Changelog

All notable changes follow [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and Semantic Versioning.

## [Unreleased]

### Changed
- Replaced the packaged manual text/speech translator flow with a system-wide Accessibility Service translation dashboard.
- Accessibility node text is now the primary live input, with deduplication, debounce, sensitive-field filtering, and a movable accessibility overlay.
- Added optional Android 11+ ML Kit screenshot OCR fallback for apps that expose no accessible text.
- Added prominent consent, pause/resume, stop, consent revocation, OCR controls, privacy-first history defaults, and Room migration 1→2.
- Removed microphone/camera permissions and the standalone translate feature from the application build.

### Planned
- Firebase App Check enforcement
- Streaming translation responses
- Baseline Profiles and macrobenchmarks

## [1.0.0] - 2026-07-11

### Added
- Modular Kotlin/Compose Android application
- Google/Firebase authentication and profile/device persistence
- Room history, favorites, settings, metadata, and cache
- Firestore offline synchronization with WorkManager
- Secure Firebase Functions Groq provider
- Live speech, interpreter, typed text, TTS, and OCR screens
- Material 3 light/dark/dynamic UI
- Debug and tagged-release GitHub Actions

# Security policy

Do not disclose vulnerabilities in public issues. Contact the repository owner privately through GitHub Security Advisories.

Include affected version, impact, reproduction steps, and a suggested mitigation. Do not include real API keys, access tokens, Firebase configs, screen contents, user data, or signing material.

## Accessibility and screen-data issues

Treat any of the following as high-priority security/privacy defects:

- capture of password or editable fields
- capture while consent is absent or revoked
- screenshots or extracted screen text written to storage/logs
- overlay translation continuing after Pause/Stop
- interaction with another app through clicks, gestures, typing, or scrolling
- extraction from lock/system UI, permission surfaces, keyboards, or secure windows
- screenshots uploaded to any backend

The intended design observes only visible non-editable text, performs OCR on-device as a fallback, submits recognized text only for translation, and immediately discards screenshots.

## Secret handling

Android contains no Groq key. Production translation uses Firebase Functions and Firebase Secret Manager. GitHub Actions secrets hold optional Firebase/release configuration; client and signing files remain gitignored.

If a secret is pasted into chat, a ticket, a commit, or a log, treat it as compromised: revoke it immediately, issue a replacement, and inspect audit logs. Deleting the text is not sufficient.

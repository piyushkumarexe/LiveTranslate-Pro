# Contributing

Thank you for improving LiveTranslate Pro.

1. Open an issue for substantial changes.
2. Fork and branch from `main`: `feat/short-description` or `fix/short-description`.
3. Never commit credentials, `google-services.json`, signing material, or personal data.
4. Keep dependencies flowing toward `domain`/`core`, not between unrelated features.
5. Add tests for business logic, migrations, parsers, and regressions.
6. Run `./gradlew testDebugUnitTest :core:model:test :domain:test lintDebug` and `npm --prefix functions run build`.
7. Use Conventional Commits (`feat:`, `fix:`, `docs:`, `test:`, `chore:`).
8. Complete the pull request checklist and request review.

UI changes should include screenshots or a short recording. Database changes require explicit Room migrations; security-rule changes require emulator tests. Contributors certify they have the right to submit their work under the MIT License.

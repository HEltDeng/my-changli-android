# V1 Dependency Closure Report — Scope Remediation

- Date: 2026-08-12
- Complete baseline: `9afec16ef5f8095a4c949f08ac1426bf2cd916d1`
- Branch: `release/v1-initial`

## Result

- `Lite3DMap.jar`: dependency closure remains complete; it is absent and campus map remains CLOSED.
- Payment SDK/merchant implementation: absent; real payment remains CLOSED.
- Real sports transaction path: unreachable; real reservation remains CLOSED.
- `CSUSTDataGet-1.1.4-no-sensitive-logs.aar`: no longer treated as a product requirement to be permanently absent, but it cannot be restored because its redistribution right is unproven.

Overall result: `CORE_CAMPUS_SERVICE_RELEASE_BLOCKER`.

## Why the AAR was not restored

The exact upstream source/tag/commit and local modified AAR were audited. No license or redistribution authorization was found in the tree, reachable history, source headers or AAR metadata. The AAR is a modified derivative. Inserting it into an APK/AAB would require rights that have not been evidenced.

Clean reimplementation spans SSO/CAPTCHA, redirects, cookies, school sessions, timetable, grades, exams, classrooms, MOOC, campus-card/electricity, local credential security and HTTP transport policy. That is major work and triggers the user-defined Stop Condition.

## Current safe build boundary

- Neither local binary exists in the V1 worktree.
- Existing Gradle declarations remain absent.
- The current preBuild gate still rejects either filename if it reappears; for CSUSTDataGet this is a temporary safety gate until a lawful replacement/authorization is chosen.
- Current V1 unit tests and Release compile pass without the two files.
- Current Release runtime contains no map or payment SDK.

## Corrected product boundary

The safe interim binary is not the desired full-campus product. School authentication and Study/Life queries are now documented as product-OPEN but release-BLOCKED. They must not be silently classified CLOSED or deleted as a final product decision.

## Next closure options

1. Obtain written authorization covering the upstream source, local modifications and distribution inside the App's APK/AAB; then complete transport, credential, privacy and device audits.
2. Separately authorize a clean-room school adapter and test it through normal user-driven school login without bypassing CAPTCHA, access control or rate limits.

No AAR/JAR, secret, credential or signing material was added to Git.

# V1 Release-First Test Results

- Date: 2026-08-12
- Java: Temurin 17.0.19
- Android SDK: `C:\Users\HeVoi\AppData\Local\Android\Sdk`
- Gradle result: `BUILD SUCCESSFUL`

## Automated result

Freeze command group:

```text
:app:testDebugUnitTest
:app:assembleDebug
:app:mergeDebugResources
:app:mergeReleaseResources
:app:processDebugMainManifest
:app:compileReleaseKotlin
:app:compileReleaseJavaWithJavac
:app:processReleaseMainManifest
:app:verifyReleaseEndpointSafety
```

- JUnit: 23 suites / 76 tests / 0 failures / 0 errors / 0 skipped.
- Debug assemble: PASS.
- Debug/Release resource and manifest merge: PASS.
- Release Kotlin/Java compile: PASS.
- Release endpoint fail-closed check: PASS.
- Authorized AAR hash preBuild check: PASS.
- New freeze-stage P0/build blockers: 0.
- Historical lint baseline: 0 errors / 1,378 warnings; not rerun because this freeze introduced no lint-policy or broad UI change.

## Covered boundaries

- Study catalog exposes timetable, grades, classroom, MOOC, exams, resources, CET and Mandarin.
- Life catalog exposes electricity, calendar and Lost & Found.
- School session coordinator covers single-flight login, session reuse/expiry and failure behavior.
- Package/version/privacy/own-account separation and exact network policy source boundaries.
- Authorized CSUSTDataGet present at the fixed hash; Lite3DMap and AMap absent.
- Campus map, real sports and real payment remain closed.
- No school credential/session fields appear in own-Backend service DTO boundaries.
- Release clean-surface checks cover the formal label/package, clean privacy/agreement titles, Debug/Release build flag separation, safe HTTPS update target policy and Release manifest removal of local demo Activities.

## Lint remediation

The restored code initially reproduced 174 errors. Core-library desugaring removed the Java Time minSdk failures; one API 29 getter was replaced with an API-compatible setter; custom styleable indices and a lost-item runtime layout weight were corrected; 40 AppCompat tint sites were migrated to `app:tint`; the Compose resource read now uses `LocalResources`. Final errors: **0**.

The 1,378 warnings are historical cleanup debt and are classified in `V1_RELEASE_LINT_REMEDIATION.md`; they did not block this Debug QA build.

## User acceptance and remaining non-automated release work

The user completed physical-device QA and explicitly accepted the runtime, UI, campus paths and final Launcher icon at accepted QA SHA `cda0300e7c306d98cc54d8b4a4a5f60e1e52974d`. No credential was requested or used by automation. Final signed-package smoke testing remains pending after production deployment, endpoint, filing and formal signing steps.

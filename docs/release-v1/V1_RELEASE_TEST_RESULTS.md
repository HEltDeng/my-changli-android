# V1 Release-First Test Results

- Date: 2026-08-12
- Java: Temurin 17.0.19
- Android SDK: `C:\Users\HeVoi\AppData\Local\Android\Sdk`
- Gradle result: `BUILD SUCCESSFUL`

## Automated result

Final command group:

```text
:app:testDebugUnitTest
:app:assembleDebug
:app:compileReleaseKotlin
:app:compileReleaseJavaWithJavac
:app:processReleaseMainManifest
:app:verifyReleaseEndpointSafety
:app:lintRelease
```

- JUnit: 23 suites / 74 tests / 0 failures / 0 errors / 0 skipped.
- Debug assemble: PASS.
- Release Kotlin/Java compile and merged manifest: PASS.
- Release endpoint fail-closed check: PASS.
- Authorized AAR hash preBuild check: PASS.
- Release lint: 0 errors / 1,378 warnings.

## Covered boundaries

- Study catalog exposes timetable, grades, classroom, MOOC, exams, resources, CET and Mandarin.
- Life catalog exposes electricity, calendar and Lost & Found.
- School session coordinator covers single-flight login, session reuse/expiry and failure behavior.
- Package/version/privacy/own-account separation and exact network policy source boundaries.
- Authorized CSUSTDataGet present at the fixed hash; Lite3DMap and AMap absent.
- Campus map, real sports and real payment remain closed.
- No school credential/session fields appear in own-Backend service DTO boundaries.

## Lint remediation

The restored code initially reproduced 174 errors. Core-library desugaring removed the Java Time minSdk failures; one API 29 getter was replaced with an API-compatible setter; custom styleable indices and a lost-item runtime layout weight were corrected; 40 AppCompat tint sites were migrated to `app:tint`; the Compose resource read now uses `LocalResources`. Final errors: **0**.

The 1,378 warnings are historical cleanup debt and are classified in `V1_RELEASE_LINT_REMEDIATION.md`; they did not block this Debug QA build.

## Not automated

No Android device was connected. Installation, launch smoke testing, visual regression and real school account/CAPTCHA/session E2E were not run. No user credential was requested. These items remain in `V1_CAMPUS_QA_CHECKLIST.md` and `user_acceptance` remains `PENDING`.

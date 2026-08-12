# V1 Release Test Results — Full Campus Scope Remediation

- Date: 2026-08-12
- Java: Temurin 17.0.19
- Android SDK: `C:\Users\HeVoi\AppData\Local\Android\Sdk`
- Remediation execution: stopped at `CORE_CAMPUS_SERVICE_RELEASE_BLOCKER`

## Current safe branch verification

Command group completed successfully:

```text
:app:testDebugUnitTest
:app:verifyReleaseEndpointSafety
:app:compileReleaseKotlin
:app:compileReleaseJavaWithJavac
:app:processReleaseMainManifest
```

JUnit XML: 23 suites, 70 tests, 0 failures, 0 errors, 0 skipped.

These tests validate the current dependency-reduced interim branch. They do not prove the requested school features were restored.

## Corrected-scope test matrix

| Requirement | Result | Evidence / reason |
| --- | --- | --- |
| School binding/login/wrong password | NOT RUN / BLOCKED | implementation cannot lawfully enter Release yet |
| CAPTCHA | NOT RUN / BLOCKED | same gate; no live credential testing performed |
| Session restore/logout/unbind | NOT RUN / BLOCKED | same gate |
| School timetable/grades/exams/classroom/MOOC | NOT RUN / BLOCKED | same gate |
| Electricity/campus card/school calendar | NOT RUN / BLOCKED | same gate |
| Credentials not sent to own Backend | STATIC PASS FOR CURRENT INTERIM BRANCH | school input/session absent; source boundary rejects school credential fields |
| Lost-and-found | PASS for existing unit/model/rules coverage | remains OPEN |
| Map CLOSED | PASS for current boundary | map classes/route/metadata/location absent |
| Real sports reservation CLOSED | PASS for current boundary | no route/UI/request path |
| Real payment CLOSED | PASS for current boundary | no SDK/provider/merchant path |
| Release endpoint fail-closed | PASS | `verifyReleaseEndpointSafety` |
| Current package/privacy/identity | PASS in existing suite | unchanged by this documentation-only gate run |

## Release lint

`lintRelease` ran to completion in 2m25s and the Gradle process returned success only because the project sets `abortOnError = false` and `checkReleaseBuilds = false`.

Actual lint report: **169 errors, 1,397 warnings, 3 hints**.

Error groups:

| ID | Count | Classification |
| --- | ---: | --- |
| `NewApi` | 125 | Java Time/API 26 use with minSdk 24; core-library desugaring/API policy must be fixed |
| `UseAppTint` | 40 | AppCompat tint correctness |
| `ResourceType` | 2 | custom styleable resource access |
| `Suspicious0dp` | 1 | layout correctness |
| `LocalContextGetResourceValueCall` | 1 | Compose resource access |

Warning groups include 844 unused resources, 173 hardcoded text issues, 72 content-description issues and two scoped-storage warnings caused by intentional manifest-removal markers. The complete report is at `app/build/reports/lint-results-release.html`.

Result: `COMPLETED_WITH_ERRORS` — not PASS. The errors were not repaired because user Stop Condition 1 had already required work to stop and return the core dependency decision.

## Builds and signing

- Release Kotlin/Java/Manifest compile: PASS for the current interim branch.
- Full-campus Release compile: NOT ATTEMPTED; blocked before restoration.
- `MY_CHANGLI_SIGNING_CONFIG`: absent in this task environment; no value/path/password was printed.
- No second keystore was created.
- Signed APK/AAB: not generated.

## Manual/E2E boundary

No real school account, password, CAPTCHA, cookie or token was requested or used. After the core dependency decision and lint errors are resolved, real school-service testing must be user-interactive on device. Physical-device RC acceptance remains PENDING.

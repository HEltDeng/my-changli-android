# V1 Release Test Results

- Date: 2026-08-12
- Java: Temurin 17.0.19
- Android SDK: `C:\Users\HeVoi\AppData\Local\Android\Sdk`
- Forbidden local binaries present: `NO`

## Final automated result

Command group:

```text
:app:testDebugUnitTest
:app:assembleDebug
:app:compileReleaseKotlin
:app:compileReleaseJavaWithJavac
:app:processReleaseResources
:app:processReleaseManifest
:app:verifyReleaseEndpointSafety
```

Result: `BUILD SUCCESSFUL`.

JUnit XML result: 23 suites, 70 tests, 0 failures, 0 errors, 0 skipped.

## Required regression coverage

| Requirement | Evidence | Result |
| --- | --- | --- |
| CLOSED feature routing | `V1SourceBoundaryTest`, `FunctionCatalogTest` | PASS |
| CSUSTDataGet absent build | forbidden-file task, source test, successful build without file | PASS |
| Lite3DMap absent build | forbidden-file task, source test, successful build without file | PASS |
| payment disabled | `SportsReservationRulesTest`, `V1ReleasePolicy` | PASS |
| sports real booking disabled | `SportsReservationRulesTest`, empty sports catalog, account-surface source test | PASS |
| school credential boundary | `SchoolSessionCoordinatorTest`, `CampusToolsAuthContractTest`, account-surface source test | PASS |
| Feature Flag | `FeatureAvailabilityTest`, `ServiceDynamicsFilterTest`, local hard policy | PASS |
| Deep Link CLOSED feature | Manifest/source boundary test; no `<data>` route | PASS |
| Release network fail closed | `NetworkSecurityPolicyTest`, `verifyReleaseEndpointSafety`, Release BuildConfig `.invalid` | PASS |
| package/app identity | `AppIdentityTest`, source boundary test, APK badging | PASS |
| privacy gate | source boundary test for consent/declined screens | PASS |
| OPEN lost-found path | `LostFoundRulesTest`, codec/model/filter suites, Manifest assertion | PASS |

## Build artifacts and expected gate

- Debug APK: built successfully for engineering inspection only.
- Release Kotlin/Java/resources/manifest compilation: successful.
- `assembleRelease`: intentionally failed at `validateReleaseSigning` because `MY_CHANGLI_SIGNING_CONFIG` is absent.
- Signed APK/AAB: not generated; status `WAITING_FOR_FORMAL_SIGNING`.

## Intermediate issues fixed

Two over-pruned legitimate dependencies were found by the final build: FlexboxLayout used by the OPEN lost-found publish page, and StateLayout used by the OPEN account page. Their traceable runtime dependencies were restored. An old support annotation import was migrated to AndroidX. Subsequent full verification passed.

## Skips and incomplete checks

- JUnit skipped tests: none.
- `app/src/androidTest` contains no instrumented test files, so no connected test suite existed to run.
- `lintRelease` reached `lintAnalyzeRelease` but did not finish within the bounded execution window and was stopped; no lint PASS is claimed. This should be rerun before store submission.
- Physical-device installation, notification/alarm behavior, lifecycle/back navigation, network faults and visual acceptance remain user/manual RC checks.

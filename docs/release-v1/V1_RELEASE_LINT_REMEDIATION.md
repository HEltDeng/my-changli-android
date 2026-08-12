# V1 Release Lint Remediation

- Date: 2026-08-12
- Starting evidence: 169 errors, 1,397 warnings, 3 hints
- Current result: `NOT_STARTED_AFTER_CONFIRMED_SECURITY_STOP`

## Why remediation did not proceed

The required order was campus runtime restoration followed by a complete lint repair. Source restoration stopped before code changes when `SCHOOL_CLEARTEXT_SECURITY_BLOCKER_CONFIRMED` was proven on the actual xk/pt paths. Continuing a broad 169-error source rewrite after the security Stop Condition would violate the instruction to stop and would produce lint evidence for an implementation that is not the intended full-campus runtime.

## Preserved error inventory

| Category | Count | Required future treatment |
| --- | ---: | --- |
| `NewApi` | 125 | SDK guards, AndroidX compatibility APIs, desugaring where correct and real fallbacks; no blanket suppression |
| `UseAppTint` | 40 | compatible AppCompat attributes/APIs |
| `ResourceType` | 2 | correct the resource type or accessor |
| `Suspicious0dp` | 1 | correct layout sizing/constraints |
| `LocalContextGetResourceValueCall` | 1 | use Compose-aware resource access |

Warnings remain to be classified as accepted, store-sensitive, privacy-sensitive, deprecated, performance, accessibility or future cleanup after the runtime implementation is safe.

## Gate state

The project still has `checkReleaseBuilds = false` and `abortOnError = false`; this is not accepted as a passing Release gate. Future completion must set both gates to fail normally and produce `lintRelease` with zero errors after campus restoration.

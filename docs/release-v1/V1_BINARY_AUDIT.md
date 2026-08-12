# V1 Binary Audit — Scope Remediation

- Date: 2026-08-12
- Formal signed full-campus Release binary: `NOT_AVAILABLE`
- Status: `BLOCKED — CORE_CAMPUS_SERVICE_RELEASE_BLOCKER`

## Existing engineering artifact

The previously built Debug APK remains an engineering artifact only:

`app/build/outputs/apk/debug/MyChangli_debug_v1.0.0.apk`

| Field | Existing audited value |
| --- | --- |
| applicationId | `com.mychangli.app.debug` |
| versionCode / versionName | `32` / `1.0.0` |
| label | `我的长理 Debug` |
| SHA-256 | `37174DD12D43C954E0103F6253C4E78F773D2BC6BD096A71B13AEC8FA089739E` |
| certificate | Android Debug certificate |

It is debuggable, includes the Debug localhost endpoint and is not a Release Candidate.

## Current interim binary/dependency boundary

- CSUSTDataGet and its packages are absent from the existing Debug artifact/current Release runtime.
- Lite3DMap and AMap packages are absent.
- Payment SDK/package paths are absent.
- Current merged Release Manifest contains no location, Wi-Fi control, camera, media or legacy-storage grant.
- Current Release endpoint remains `https://campus-tools.invalid/api/v1/` and fails closed.

The CSUSTDataGet absence proves the safe interim build does not distribute the unlicensed artifact. It does not satisfy the corrected requirement to restore school Study/Life features.

## Why no signed binary was produced

1. CSUSTDataGet modified-binary/App redistribution rights are unproven and clean reimplementation is major work.
2. Release lint completed with 169 errors.
3. `MY_CHANGLI_SIGNING_CONFIG` is not available in this task environment.
4. Formal production domain/endpoint is not configured.

No Debug-signed package was renamed or represented as an RC.

## Required final audit after blockers close

Build the signed APK/AAB with the existing formal signing identity and re-audit application ID, version, label/icon, certificate/hashes, merged permissions, all SDK/classes/native libraries, school hosts and packages, AAR/JAR residue, payment/map/debug packages, localhost/`.invalid`, secrets/tokens/log strings, old brand/package/emblem and school credential persistence. A true final binary audit cannot be issued before that artifact exists.

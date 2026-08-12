# CSUSTDataGet Release Audit

- Date: 2026-08-12
- Upstream: `CreaMakers/CSUSTDataGet`
- Version: `1.1.4`
- Upstream commit: `7c2dfa9384ab2117ec917ff52b7d3b6b3b2f2894`
- Integrated artifact: `app/libs/CSUSTDataGet-1.1.4-no-sensitive-logs.aar`
- Size: 291,778 bytes
- SHA-256: `9B03523FA04E9C8B3F5CAAAD49DC282B17EB2AC82E69A49E89764B21ABF71C29`

## Authorization

`RESOLVED_BY_RIGHTSHOLDER_AUTHORIZATION`

The user confirmed a project-specific authorization covering use, necessary modification, dependency builds, APK/AAB distribution and security-remediation derivatives. This record is not represented as an upstream open-source license and does not imply authorization from Changsha University of Science and Technology. See `CSUST_DATA_GET_AUTHORIZATION_RECORD.md`.

## Integration decision

The release-first pass restored the historically verified no-sensitive-logs AAR instead of undertaking a high-risk rewrite before QA. Gradle task `verifyAuthorizedCsustDataGet` runs before `preBuild` and fails if the file is missing or its SHA-256 differs from the fixed value above.

The Debug APK contains the `com.dcelysia.csust_spider` runtime classes. AAR inspection found 162 classes and no native library. The library supplies SSO/CAPTCHA/session, education, MOOC, calendar and campus-card/electricity implementations.

## Security controls and residual risk

| Item | Current control | Status |
| --- | --- | --- |
| Sensitive network/library logs | fixed derivative uses HTTP logger `NONE` and no-op `SafeLog`; App logger is BODY-free | REQUIRED CONTROL PRESENT |
| School password | app `StudentInfoManager` stores with Android Keystore + AES-GCM | PRESENT |
| Legacy password | one-time migration writes encrypted copy, verifies it, then deletes plaintext | PRESENT |
| Library credential MMKV | dependency stores are cleared and made read-only before helper initialization | MITIGATED |
| Library Cookie MMKV | dependency Cookie store is cleared and made read-only; active jar is process-local | MITIGATED |
| xk/pt transport | exact-host HTTP compatibility only; disclosed known risk | KNOWN_V1_RISK |
| Own Backend boundary | no school credential/session fields in own-Backend DTO/services | ABSENT BY STATIC AUDIT |

The AAR still contains legacy MMKV/Cookie implementation classes. Their durable writes are blocked by app initialization, but source-level removal and dedicated instrumentation tests remain `POST_V1_SECURITY_HARDENING`.

## Binary exclusions

Lite3DMap, AMap classes, payment SDKs, CampusMapActivity and SportsReservationActivity were absent in the final Debug QA APK class-descriptor scan.

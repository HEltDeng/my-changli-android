# V1 School Credential Storage Audit

## Current implementation

- `StudentInfoManager` uses project `CredentialCipher` backed by Android Keystore and AES-GCM for the school password.
- Legacy plaintext migration reads once, encrypts, verifies the encrypted readback and deletes the plaintext key without logging its value.
- `PlanetApplication.prepareTransientCsustDataGetStores()` clears the dependency `education_cache` and `csust_cookie_jar` stores and opens them read-only before helper use.
- Active library Cookie/session behavior is therefore process-local rather than a second durable plaintext copy.
- Logout/unbind clears current and legacy values and school session/cache state.

## Logging

The authorized derivative has a no-op `SafeLog` and network logger level `NONE`. The App uses BASIC metadata only in Debug and no network logging in Release, with sensitive headers redacted and no request/response body logging. Source scan found no statement that logs password, CAPTCHA, Cookie, token or Authorization values.

## Residual risk

The fixed AAR still contains historical MMKV and persistent-Cookie implementation classes. App initialization blocks their durable writes, but a future source-level integration should remove these code paths entirely and add device instrumentation proving no plaintext file survives restart, migration, logout or unbind. Classification: `POST_V1_SECURITY_HARDENING`, not a QA APK blocker under the release-first policy.

# V1 School Credential Boundary Audit

- Date: 2026-08-12
- Target flow: user -> Android client -> school authentication/academic service
- Forbidden flow: school password/CAPTCHA/cookie/token/academic record -> My Changli Backend
- Current Release state: school integration not restored due `CORE_CAMPUS_SERVICE_RELEASE_BLOCKER`

## Baseline data flow

| Data | Intended destination | Local handling in full baseline | Own Backend evidence | Result |
| --- | --- | --- | --- | --- |
| Student ID | school auth/service | stored locally; masked in settings | A future sports DTO can contain a student ID, but sports is excluded from V1; no active school-login payload to own Backend | Release boundary preserved while sports is closed |
| School password | school auth/campus-card service | `StudentInfoManager` encrypts using Android Keystore AES-GCM; dependency also tries plaintext MMKV | no own Backend API field/call found | Correct intended boundary; dependency storage needs mitigation/audit |
| CAPTCHA | school authserver only | transient login input | no own Backend payload found | Correct intended boundary |
| School cookies/session | school hosts only | dependency cookie jar; full app attempts memory-only behavior by clearing/opening its MMKV read-only | no own Backend upload found | Correct destination, persistence mitigation not device-proven |
| School/campus token | school service only | process/session state | no own Backend upload found | Correct intended boundary |
| Timetable/grades/exams/MOOC data | display/cache on user device | Room/MMKV/local UI caches | no own Backend upload found | Correct intended boundary |

## Local storage

- `CredentialCipher` uses an Android Keystore AES key with AES/GCM/NoPadding.
- `StudentInfoManager` writes the encrypted password and deletes the legacy plaintext key; save verifies the encrypted value before accepting it.
- The dependency's own `EducationData` implementation writes plaintext ID/password to an `education_cache` MMKV.
- The full app mitigates that dependency behavior by clearing and opening `education_cache` and `csust_cookie_jar` read-only before library use, while keeping the active cookie jar in memory.
- This mitigation has not been proven on the full Android process/device matrix and is not sufficient to override the missing redistribution right.

## Logs and crash paths

- Upstream source contains credential, cookie, HTML/body and exception logging.
- The fixed local AAR bytecode shows `SafeLog` methods return without logging and the HTTP logging interceptor uses `Level.NONE`.
- The app still contains unrelated `printStackTrace()` calls in legacy UI/repositories. No direct school password/cookie argument was found in those calls, but a future restored implementation must replace exception printing in school data paths and re-run a Release log scan.
- No telemetry or analytics upload was found in the dependency.

## Logout, unbind and data clear

The complete baseline contains explicit paths to:

- invalidate `SchoolSessionState`;
- call/reset the library session and cookie jar;
- clear `education_cache`, `csust_cookie_jar`, legacy `MoocCookiejar`, school content caches, timetable widget state and local course data;
- clear encrypted school binding data through `StudentInfoManager`/account data management.

These paths are currently absent from the V1 Release implementation because school binding was over-removed. They must be restored and device-tested only after the core dependency gate is closed.

## Release assertion

Current V1 code cannot send school credentials to the own Backend because there is no school credential input/session implementation in the branch, and the source-boundary tests reject school credential fields. That is a safe interim state, not completion of the corrected product scope.

Before campus features can ship, add regression/E2E evidence for wrong password, CAPTCHA, session expiry/restore, logout, unbind, process death, app-data clear and network logging. Real credentials must be entered by the user on-device and never sent through chat or test fixtures.

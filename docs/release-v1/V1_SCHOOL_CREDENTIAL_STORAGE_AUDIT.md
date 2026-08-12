# V1 School Credential Storage Audit

- Date: 2026-08-12
- Scope: full baseline `9afec16ef5f8095a4c949f08ac1426bf2cd916d1`, exact CSUSTDataGet source `7c2dfa9384ab2117ec917ff52b7d3b6b3b2f2894`, current V1 branch
- Runtime restoration result: stopped before integration at `SCHOOL_CLEARTEXT_SECURITY_BLOCKER`

## Data classification

| Class | Data | Required handling |
| --- | --- | --- |
| Secret | school password, Cookie, session token, authentication token | Keystore-backed encryption at rest or memory-only; never log; delete on logout/unbind/data clear |
| Identifier | student ID / school account identifier | Store only when binding requires it; do not treat it as an authentication secret; delete on unbind |
| Sensitive cache | timetable, grades, exams, MOOC and campus-card/electricity result | App-private storage, minimum retention, user-visible deletion path; never send to the My Changli Backend |
| Public/low-sensitivity cache | public school calendar material | App-private cache with expiry and clear-data support |

## Findings

1. The full app baseline has `StudentInfoManager`, backed by Android Keystore AES-GCM, for the school password and identifier. That is the preferred app-side credential boundary.
2. The upstream dependency writes `student_id` and `student_password` to the `education_cache` MMKV store without cryptographic protection.
3. Its `PersistentCookieJar` serializes school cookies in the `csust_cookie_jar` MMKV store without Keystore encryption.
4. The previously audited derivative AAR reduced log exposure but did not turn those dependency-owned stores into an independently acceptable secret store.
5. No school credential/session implementation exists in the current safe V1 runtime, so no new plaintext credential or cookie persistence was introduced during this task.

## Required source-integration design

- Remove dependency-owned password persistence entirely; inject credentials transiently from the app's Keystore-backed store only for user-authorized authentication.
- Keep authentication/session cookies in memory unless durable restoration is strictly necessary. If persistence is necessary, encrypt the serialized cookie payload with a Keystore-held AES-GCM key and bind deletion to logout, unbind and account-data clearing.
- Separate school-session clients from the My Changli Backend client so headers, interceptors and cookie jars cannot cross domains.
- Never place school password, CAPTCHA, Cookie, token, timetable, grades or exams in own-Backend DTOs, request bodies, headers or query parameters.

## Result

The target design is defined, but runtime implementation and device verification did not proceed because authenticated school traffic would have to use cleartext HTTP for current `xk` and `pt` endpoints. Credential Storage cannot be marked PASS until transport is safe and the source-based implementation is completed and tested.

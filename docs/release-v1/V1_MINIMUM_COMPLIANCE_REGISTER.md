# V1 Minimum Compliance Register

- Date: 2026-08-12
- Policy: Release-first, truthful disclosure, narrow technical exceptions and recorded post-V1 hardening

| Risk / gate | Current classification | V1 control | Follow-up |
| --- | --- | --- | --- |
| `xk.csust.edu.cn` authenticated HTTP | `KNOWN_V1_RISK` | exact-host allowlist only; global cleartext false; in-app disclosure | migrate when equivalent HTTPS exists |
| `pt.csust.edu.cn` authenticated HTTP | `KNOWN_V1_RISK` | exact-host allowlist only; global cleartext false; in-app disclosure | migrate when equivalent HTTPS exists |
| Historical AAR MMKV credential/Cookie classes | `POST_V1_SECURITY_HARDENING` | Keystore AES-GCM password; migration deletes plaintext; dependency stores cleared/read-only; process-local Cookie jar | source-module integration and device instrumentation |
| CSUSTDataGet upstream has no declared open-source license | RESOLVED for this project | project-specific rightsholder authorization record; fixed provenance/hash; no false open-source claim | retain private-repository and authorization evidence |
| School credential -> own Backend | `RELEASE_BOUNDARY` | separate network/data models; static scan and disclosure; no school fields in own API DTOs | repeat final binary and traffic QA |
| 1,378 lint warnings | `HISTORICAL_DEBT` | lint errors reduced to 0; warnings classified, no bulk UI rewrite | accessibility, localization, deprecation and unused-resource cleanup |
| Exact alarm permission | `STORE_SENSITIVE` | only user-enabled reminders; fallback behavior retained | declaration/store review before submission |
| Debug loopback and diagnostic SDKs | `QA_ONLY` | `.debug` package, Debug certificate, explicit NOT RELEASE label | absent/re-audited in final signed Release |
| Release own-Backend endpoint remains `.invalid` | `RELEASE_BLOCKER`, not QA blocker | Release fails closed; Debug may use local test endpoint | configure formal HTTPS domain after deployment/filing gate |
| Real school E2E not automated | `USER_ACCEPTANCE_PENDING` | no credentials in chat or automation; user checklist supplied | user-operated device QA |

## Privacy disclosure implemented

The in-app Release Information screen and school-binding notice disclose student ID, school password, CAPTCHA, school Cookie/session, timetable, grades, exams, MOOC, calendar, electricity/campus-card data; purpose and direct school-service destination; local encrypted/cache behavior; the fact that school secrets do not enter the My Changli Backend; unbind/delete controls; xk/pt HTTP risk; dependency authorization; non-official identity; and the three excluded V1 capabilities.

This register records engineering facts and accepted V1 risk. It is not a legal opinion and does not replace application-store, filing or user acceptance review.

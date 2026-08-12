# My Changli V1 Campus QA Checklist

- Date: 2026-08-12
- Artifact status: `NOT_GENERATED`
- Reason: `SCHOOL_CLEARTEXT_SECURITY_BLOCKER_CONFIRMED`
- Intended artifact class: `DEBUG ONLY / NOT RELEASE / NOT RC`

This checklist is prepared for the future build produced only after confidential school-service transport exists and the campus runtime is restored. It is not evidence that a current Campus QA APK exists.

## Artifact preflight

- [ ] Filename clearly contains `Campus-QA-debug`.
- [ ] Package is the Debug application ID, not the Release package.
- [ ] Version is `1.0.0` / versionCode `32` unless a documented monotonic increment is required.
- [ ] SHA-256, byte size and Android Debug signing identity are recorded.
- [ ] Artifact is labelled `DEBUG ONLY`, `NOT RELEASE`, `NOT RC`.
- [ ] No formal JKS password or signing secret is required.

## Transport and credential safety

- [ ] xk/pt traffic is HTTPS with valid hostname/certificate and no downgrade.
- [ ] No password, CAPTCHA, Cookie, token or session crosses HTTP.
- [ ] Global cleartext is disabled.
- [ ] School secrets never enter the My Changli Backend.
- [ ] Password and durable Cookie/session storage are Keystore-backed or intentionally memory-only.
- [ ] Legacy plaintext MMKV values are migrated and deleted, or cleared with explicit re-login.
- [ ] Debug and Release logs contain no school secret or sensitive body.

## User-operated school E2E

The user enters real credentials directly on the device. Codex must not receive or automate them.

- [ ] School account bind and account switch.
- [ ] Correct login, wrong-password error and CAPTCHA path.
- [ ] Session reuse, expiry, manual re-authentication and automatic re-authentication.
- [ ] Logout and unbind clear credentials, cookies, sessions and cached academic data.
- [ ] School timetable sync and local/manual timetable coexist.
- [ ] Grades and grade detail.
- [ ] Exams.
- [ ] Empty classrooms.
- [ ] MOOC courses, homework and tests.
- [ ] School/teaching calendar and semester discovery.
- [ ] Electricity and campus-card queries.

## Existing OPEN features

- [ ] Resource Library.
- [ ] CET.
- [ ] Mandarin.
- [ ] Lost & Found through the My Changli Backend.
- [ ] Own account, feedback, announcements and settings.

## UI and navigation

- [ ] Study/Life cards, names, icons and ordering match the established experience.
- [ ] Chinese text is valid UTF-8 with no mojibake.
- [ ] No duplicated routes, overlapping cards, clipping, invisible text or dark-mode regression.
- [ ] Loading, empty, offline and authentication-error states are understandable.
- [ ] Back navigation, process restart and account switching do not show stale user data.

## Explicit exclusions

- [ ] Campus map is inaccessible; Lite3DMap/AMap and location permission are absent.
- [ ] Real sports inventory/order/fulfilment is inaccessible.
- [ ] Real payment/refund and payment SDKs are absent.

## Acceptance boundary

Automated tests and this checklist support user acceptance but do not replace it. `user_acceptance` remains `PENDING` until the user completes the future device run.

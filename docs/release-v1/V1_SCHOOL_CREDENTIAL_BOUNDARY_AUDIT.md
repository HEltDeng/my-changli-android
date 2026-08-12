# V1 School Credential Boundary Audit

## Data classification

- Secret: school password, CAPTCHA answer, Cookie, token and session.
- Identifier: student ID/school account identifier.
- Personal campus data: timetable, grades, exams, MOOC, calendar and electricity/campus-card result.
- App account data: separate My Changli identity and Backend contract.

## Allowed flow

```text
User input / local encrypted credential
  -> Android school client
  -> applicable CSUST school host
```

The school password and CAPTCHA are used for the school SSO flow. School Cookies/tokens/sessions are used for direct school queries. They are not proxied through the My Changli Backend.

## Own Backend proof

- Own account and school account have separate UI copy, stores and network stacks.
- Static scans found no school password/CAPTCHA/Cookie/session/token fields in own-Backend DTO/service boundaries.
- No CSUSTDataGet class contains an own-Backend host.
- Source-boundary tests reject regression to mixed school/own-account handling.

This is static and unit-test evidence. Final device traffic inspection remains a QA item; no real credentials were used by Codex.

## Delete/unbind

Unbind/log-out clears encrypted school credentials, legacy plaintext keys, dependency credential/Cookie stores, session state and relevant caches. The user can separately delete My Changli account data without conflating the two identities.

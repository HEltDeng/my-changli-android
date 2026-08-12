# V1 School Network Host Audit

- Date: 2026-08-12
- Decision: release-first compatibility with the school's current service conditions
- Global cleartext: denied

| Host | Transport | Use | Sensitive state | V1 policy |
| --- | --- | --- | --- | --- |
| `authserver.csust.edu.cn` | HTTPS | SSO login, CAPTCHA, logout | ID/password/CAPTCHA/Cookies | TLS required; no cleartext exception |
| `ehall.csust.edu.cn` | HTTPS | SSO/eHall redirect and identity | ticket/Cookie/session | TLS required; no cleartext exception |
| `xk.csust.edu.cn` | HTTP | timetable, grades, exams, classrooms, teaching calendar | authenticated Cookie and academic data | exact-host cleartext compatibility; known risk |
| `pt.csust.edu.cn` | HTTP | MOOC profile/courses/homework/tests | authenticated Cookie and learning data | exact-host cleartext compatibility; known risk |
| `hxyxh5.csust.edu.cn` | HTTPS in authorized dependency | campus-card/electricity exchange | ticket/token and card/electricity data | TLS required; no cleartext exception |

No alternate working HTTPS data plane was established in the prior investigation. That evidence is retained in reports 58/59; the release-first decision does not claim HTTP is confidential.

## Android enforcement

The main Release configuration uses:

- base `cleartextTrafficPermitted="false"`;
- exact, non-subdomain exceptions only for `xk.csust.edu.cn` and `pt.csust.edu.cn`;
- system trust anchors; no trust-all, hostname bypass or disabled certificate validation.

The Debug overlay adds only exact `127.0.0.1` and `localhost` exceptions for the QA own-Backend loopback configuration, plus the same exact xk/pt rules. It no longer contains map/AMap domains or cleartext exceptions for HTTPS school hosts. This is one reason the artifact is `DEBUG QA ONLY`.

School traffic remains direct Android-to-school. The My Changli Backend is not an authentication proxy and must not receive school credentials, CAPTCHA, Cookies, tokens, sessions or academic payloads.

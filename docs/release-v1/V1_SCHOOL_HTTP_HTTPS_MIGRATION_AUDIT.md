# V1 School HTTP/HTTPS Migration Audit

- Date: 2026-08-12
- Probe policy: public endpoints only, no account, password, CAPTCHA, Cookie or token; one bounded request per check
- Decision: `SCHOOL_CLEARTEXT_SECURITY_BLOCKER_CONFIRMED`

## Source-level transport map

The exact authorized source at `7c2dfa9384ab2117ec917ff52b7d3b6b3b2f2894` defines:

| Host | Source base URL | Client state |
| --- | --- | --- |
| `authserver.csust.edu.cn` | HTTPS | shared school CookieJar; credential/CAPTCHA form endpoint |
| `ehall.csust.edu.cn` | HTTPS | shared school CookieJar; SSO/eHall redirect target |
| `xk.csust.edu.cn` | **HTTP** | education login/service clients use shared school CookieJar |
| `pt.csust.edu.cn` | **HTTP** | MOOC client uses shared school CookieJar |
| `hxyxh5.csust.edu.cn` | HTTPS | campus-card client uses shared school CookieJar |

`xk` serves timetable, grades, exams, empty-classroom and teaching-calendar operations. `pt` serves personal MOOC profile, courses, homework and tests. Both therefore combine an authenticated session with user-specific education data.

## Public HTTPS probes

System DNS was under a local synthetic-address network layer, so authoritative A records were also queried through Google DNS-over-HTTPS:

- `xk.csust.edu.cn` -> `222.247.37.5`
- `pt.csust.edu.cn` -> `222.247.225.230`

Observed evidence:

| Check | `xk.csust.edu.cn` | `pt.csust.edu.cn` |
| --- | --- | --- |
| Current HTTPS request | no usable TLS service | no usable TLS service |
| Direct actual-path request pinned to authoritative A record | connection timeout before TLS/HTTP | connection timeout before TLS/HTTP |
| Independent external port-443 certificate probe | connection timeout | connection timeout |
| HTTP request through current network path | HTTP 200 | HTTP 302 to an HTTP error page |
| Usable HTTPS redirect/certificate | not observed | not observed |

The same web environment successfully reached the school’s HTTPS authentication page and followed the eHall HTTPS redirect, so the result is not evidence that every school HTTPS endpoint is unavailable. It is specific to the two legacy service hosts in this audit. The actual-path probes covered xk SSO/grades and pt SSO/personal routes rather than relying on root paths.

## HTTPS gateway and replacement candidates

| Candidate | Evidence | Verdict |
| --- | --- | --- |
| `authserver.csust.edu.cn` | HTTPS CAS/login works; source requests `service=http://xk.csust.edu.cn/sso.jsp` | Secure credential collection, but post-auth xk handoff downgrades to HTTP |
| `ehall.csust.edu.cn` | HTTPS eHall/identity front door | No verified HTTPS proxy/API for xk or pt business paths |
| `mooc.csust.edu.cn/portal` | official public MOOC link; browser reported certificate common-name mismatch | Not a valid HTTPS replacement and not the pt MEOL service set |
| `my.csust.edu.cn` / `jwc.csust.edu.cn` | official portal/front-page candidates | No publicly verifiable, source-compatible HTTPS API replacement found |

The official 2026 notice continues to publish `http://xk.csust.edu.cn/` for user login. This corroborates the source-level HTTP service target; it does not justify sending credentials or session material over cleartext from the App.

## Cookie and authentication impact

- `EducationClientForService` attaches `totalCookieJar` to HTTP `xk` timetable/grade/exam/classroom requests.
- The MOOC client attaches the same CookieJar to HTTP `pt` profile/course/homework/test requests.
- The SSO flow obtains authenticated state over HTTPS, but the resulting service session and personal response data are then used over cleartext HTTP.
- A domain-specific cleartext allowlist would narrow Android policy scope but would not protect the session Cookie or returned academic data from network interception.

## Decision

The user-defined Stop Condition 1 is confirmed: HTTPS was not available for the two required hosts, and authenticated session material plus sensitive academic data must traverse their cleartext services. The V1 branch therefore keeps `usesCleartextTraffic="false"`, does not add a domain exception, does not restore the campus source module, and does not request real credentials.

Resolution requires the school endpoints to provide working HTTPS or an authorized school-side replacement that preserves authentication and data confidentiality. A client-side workaround cannot make an HTTP transport confidential.

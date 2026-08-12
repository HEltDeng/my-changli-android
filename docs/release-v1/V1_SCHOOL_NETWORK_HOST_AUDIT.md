# V1 School Network Host Audit

- Date: 2026-08-12
- Evidence source: full baseline plus exact CSUSTDataGet `1.1.4` source/AAR audit
- Rights gate: resolved by project-specific rightsholder authorization
- Current V1 Release behavior: no school host is compiled/reachable because restoration stopped at `SCHOOL_CLEARTEXT_SECURITY_BLOCKER_CONFIRMED`

| Host | Owner/category | HTTPS | Feature | Credential sent? | Cookie/token sent? | User data returned? | Direct Android access? | Own Backend involved? | Classification |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `authserver.csust.edu.cn` | School unified authentication | Yes | CAPTCHA, SSO login/logout | Yes: ID/password/CAPTCHA | Yes | login/session state | Yes | No | HTTPS endpoint independently observed |
| `ehall.csust.edu.cn` | School eHall | Yes | SSO redirect/login user/logout | Ticket via redirect | Yes | school identity/profile | Yes | No | HTTPS redirect to authserver independently observed |
| `xk.csust.edu.cn` | School educational administration | **No — HTTP** | timetable, grades, exams, classroom, teaching calendar | No password on normal query; school session cookie | Yes | academic records | Yes | No | Expected school host but cleartext is a security blocker |
| `pt.csust.edu.cn` | School MOOC | **No — HTTP** | courses, homework, tests, profile, logout | SSO-derived session | Yes | MOOC/learning data | Yes | No | Expected school host but cleartext is a security blocker |
| `hxyxh5.csust.edu.cn` | School campus-card/electricity service | Yes in source | CAS login, token, buildings/rooms/electricity | Token API accepts ticket as username/password after school redirect | Yes | card/electricity data | Yes | No | Source uses HTTPS; runtime chain not restored after shared transport gate |

## Endpoint classes

- HTML pages/parsing: SSO login form, educational main page, timetable, grade detail, exam/classroom lists, teaching calendar, MOOC profile/course/homework/test pages.
- JSON/form endpoints: CAPTCHA need check, campus-card token exchange, electricity selection/query.
- Redirects: authserver -> eHall / educational SSO / MOOC / campus-card.
- Cookies: a shared in-process OkHttp cookie jar supplies the authenticated redirects and service queries.

## Unknown or third-party hosts

No unknown or non-school host was found in the audited library. No My Changli Backend URL exists in the library. The V1 self-operated Backend endpoint (`campus-tools.invalid` by default) is a separate network stack and must never receive school credentials, CAPTCHA, school cookies, tokens or academic records.

## HTTPS verification on actual paths

No credentialed request was made. Google DNS-over-HTTPS returned authoritative public A records `222.247.37.5` for `xk` and `222.247.225.230` for `pt`. The following source-derived HTTPS paths were checked with correct hostname/SNI and direct authoritative-A-record pinning, without disabling certificate verification:

| HTTPS URL | Result |
| --- | --- |
| `https://xk.csust.edu.cn/sso.jsp` | connect timeout; no TLS/HTTP response |
| `https://xk.csust.edu.cn/jsxsd/kscj/cjcx_query` | connect timeout; no TLS/HTTP response |
| `https://pt.csust.edu.cn/meol/homepage/common/sso_login.jsp` | connect timeout; no TLS/HTTP response |
| `https://pt.csust.edu.cn/meol/personal.do` | connect timeout; no TLS/HTTP response |

Earlier normal-resolution probes failed TLS negotiation and an independent external port-443 certificate probe timed out for both hosts. The difference between handshake failure and timeout across vantage points does not change the security result: no valid certificate, TLS session or non-downgrading HTTPS response was obtained.

The exact source attaches one shared CookieJar to the HTTP education and MOOC clients. Consequently, those requests carry authenticated session state and return personal academic/MOOC data. A domain allowlist would narrow policy but would not provide encryption.

## Alternate secure route assessment

- `authserver.csust.edu.cn` and `ehall.csust.edu.cn` provide an HTTPS authentication/eHall front door, but no official material or observed redirect exposed an HTTPS proxy for the `xk` or `pt` business paths.
- The exact CSUSTDataGet SSO route requests `service=http://xk.csust.edu.cn/sso.jsp`, proving that the authenticated xk service handoff is deliberately HTTP in this implementation.
- The school information-platform directory identifies `pt` as the teaching platform; it does not advertise an eHall HTTPS API proxy.
- `https://mooc.csust.edu.cn/portal` failed browser certificate hostname validation and is not the same MEOL route as the required `pt` API/page set.
- `my.csust.edu.cn` and `jwc.csust.edu.cn` did not provide a publicly verifiable replacement API path; private-address/front-page existence is not evidence of a secure data-plane replacement.

Official evidence retained for re-audit:

- 2026 school notice publishing the educational system as HTTP: `https://www.csust.edu.cn/__local/2/16/D8/D2D840DB99E970A712A74A06470_964C9DA9_70B45.pdf`
- Unified identity/eHall guidance: `https://www.csust.edu.cn/xxhjs/fwzn/sfrz.htm`
- School information-platform directory: `https://www.csust.edu.cn/xxhjs/xxhjxpt.htm`

## Release conclusion

The rights issue is resolved, but the transport issue is not. `SCHOOL_CLEARTEXT_SECURITY_BLOCKER_CONFIRMED` prevents Release restoration. No real-user E2E request was made, no cleartext exception was added and the current V1 network policy remains deny-by-default.

# V1 School Network Host Audit

- Date: 2026-08-12
- Evidence source: full baseline plus exact CSUSTDataGet `1.1.4` source/AAR audit
- Rights gate: resolved by project-specific rightsholder authorization
- Current V1 Release behavior: no school host is compiled/reachable because restoration stopped at `SCHOOL_CLEARTEXT_SECURITY_BLOCKER`

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

## HTTPS verification

No credentialed request was made. Google DNS-over-HTTPS returned authoritative public A records `222.247.37.5` for `xk` and `222.247.225.230` for `pt`. HTTPS requests using both normal resolution and direct `--resolve` pinning failed during TLS handshake. An independent external port-443 probe timed out for both hosts. HTTP remained reachable through the current network path.

The exact source attaches one shared CookieJar to the HTTP education and MOOC clients. Consequently, those requests carry authenticated session state and return personal academic/MOOC data. A domain allowlist would narrow policy but would not provide encryption.

## Release conclusion

The rights issue is resolved, but the transport issue is not. `SCHOOL_CLEARTEXT_SECURITY_BLOCKER` prevents Release restoration. No real-user E2E request was made, no cleartext exception was added and the current V1 network policy remains deny-by-default.

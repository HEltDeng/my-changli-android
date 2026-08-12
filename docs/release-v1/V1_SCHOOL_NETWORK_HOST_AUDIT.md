# V1 School Network Host Audit

- Date: 2026-08-12
- Evidence source: full baseline plus exact CSUSTDataGet `1.1.4` source/AAR audit
- Current V1 Release behavior: no school host is compiled/reachable because restoration stopped at `CORE_CAMPUS_SERVICE_RELEASE_BLOCKER`

| Host | Owner/category | HTTPS | Feature | Credential sent? | Cookie/token sent? | User data returned? | Direct Android access? | Own Backend involved? | Classification |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `authserver.csust.edu.cn` | School unified authentication | Yes | CAPTCHA, SSO login/logout | Yes: ID/password/CAPTCHA | Yes | login/session state | Yes | No | Expected school host; release blocked pending rights/security |
| `ehall.csust.edu.cn` | School eHall | Yes | SSO redirect/login user/logout | Ticket via redirect | Yes | school identity/profile | Yes | No | Expected school host; release blocked pending rights/security |
| `xk.csust.edu.cn` | School educational administration | **No — HTTP** | timetable, grades, exams, classroom, teaching calendar | No password on normal query; school session cookie | Yes | academic records | Yes | No | Expected school host but cleartext is a security blocker |
| `pt.csust.edu.cn` | School MOOC | **No — HTTP** | courses, homework, tests, profile, logout | SSO-derived session | Yes | MOOC/learning data | Yes | No | Expected school host but cleartext is a security blocker |
| `hxyxh5.csust.edu.cn` | School campus-card/electricity service | Yes | CAS login, token, buildings/rooms/electricity | Token API accepts ID/password after school redirect | Yes | card/electricity data | Yes | No | Expected school host; release blocked pending rights/security |

## Endpoint classes

- HTML pages/parsing: SSO login form, educational main page, timetable, grade detail, exam/classroom lists, teaching calendar, MOOC profile/course/homework/test pages.
- JSON/form endpoints: CAPTCHA need check, campus-card token exchange, electricity selection/query.
- Redirects: authserver -> eHall / educational SSO / MOOC / campus-card.
- Cookies: a shared in-process OkHttp cookie jar supplies the authenticated redirects and service queries.

## Unknown or third-party hosts

No unknown or non-school host was found in the audited library. No My Changli Backend URL exists in the library. The V1 self-operated Backend endpoint (`campus-tools.invalid` by default) is a separate network stack and must never receive school credentials, CAPTCHA, school cookies, tokens or academic records.

## Release conclusion

The host list is explainable, but explainable is not equivalent to approved. The two HTTP hosts and the unresolved dependency redistribution right prevent Release restoration. No live requests or real-user E2E tests were performed in this audit.

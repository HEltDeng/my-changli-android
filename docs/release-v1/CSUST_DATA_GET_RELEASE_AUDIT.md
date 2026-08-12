# CSUSTDataGet Release Audit

- Audit date: 2026-08-12
- Artifact: `CSUSTDataGet-1.1.4-no-sensitive-logs.aar`
- Local artifact size: 291,778 bytes
- SHA-256: `9B03523FA04E9C8B3F5CAAAD49DC282B17EB2AC82E69A49E89764B21ABF71C29`
- Claimed upstream: `https://github.com/CreaMakers/CSUSTDataGet`
- Upstream tag: `1.1.4`
- Exact upstream commit: `7c2dfa9384ab2117ec917ff52b7d3b6b3b2f2894`
- Binary handling: local read-only audit only; not copied into the V1 worktree and not uploaded to Git

## Final decision

`CORE_CAMPUS_SERVICE_RELEASE_BLOCKER`

The exact source/tag is publicly viewable, but neither the checked tree nor reachable Git history contains a LICENSE, LICENCE, COPYING or NOTICE file, and no per-source license grant or redistribution authorization was found. The AAR itself contains only `AndroidManifest.xml`, `classes.jar`, `R.txt` and Android AAR metadata, with no license/notice. Public GitHub visibility does not establish the right to redistribute a modified derivative inside an APK/AAB. GitHub's licensing guidance states that without a license, default copyright applies and others may not reproduce, distribute or create derivatives beyond the platform terms.

Because the locally used AAR is a modified build and the school-service integration spans authentication, session/cookies, timetable, grades, exams, classrooms, MOOC, electricity and campus-card access, a clean-room reimplementation is major work. This meets user Stop Condition 1. The artifact remains excluded from V1 Release until either explicit redistribution authorization is obtained or a separately approved clean-room implementation is completed and audited.

This is an evidence-based release gate, not a final legal opinion.

## Provenance

| Question | Evidence | Result |
| --- | --- | --- |
| Source | Public Git repository and exact commit/tag resolved by Git | Identified |
| Author at exact commit | Git commit records the display name `Wang` | Identified as commit author only; email omitted from this report |
| Version | tag `1.1.4` resolves to exact commit above | Identified |
| Upstream license | No license-like file in tree or reachable history; no source license headers found | NOT FOUND |
| AAR notice/license | No embedded license/notice | NOT FOUND |
| Modification | Project records and bytecode show no-op `SafeLog` and HTTP logger level `NONE`; endpoints/auth remain | Modified derivative |
| AAR reproducibility | Hash and upstream commit are fixed, but the exact patch/build recipe is not present in the V1 branch | Not independently reproducible from V1 |
| Source redistribution | No explicit grant found | UNPROVEN |
| Binary/App redistribution | No explicit grant found | UNPROVEN |

Primary references:

- Upstream repository: <https://github.com/CreaMakers/CSUSTDataGet>
- Exact source commit: <https://github.com/CreaMakers/CSUSTDataGet/tree/7c2dfa9384ab2117ec917ff52b7d3b6b3b2f2894>
- GitHub licensing guidance: <https://docs.github.com/en/repositories/managing-your-repositorys-settings-and-features/customizing-your-repository/licensing-a-repository>

## Technical responsibilities

| Capability | Present in AAR/source | Application consumer in full baseline |
| --- | --- | --- |
| SSO login form and credential submission | Yes | binding/session coordinator |
| CAPTCHA need check and image endpoint | Yes | SSO flow (UI handling incomplete/legacy) |
| eHall/SSO redirect and login-user state | Yes | school identity/session |
| School session and cookies | Yes | all school queries |
| School logout | Yes | unbind/data clear |
| Timetable and semester start data | Yes | timetable/overview |
| Grades and grade detail | Yes | grade UI |
| Exams | Yes | exam UI/overview |
| Available classrooms | Yes | classroom UI |
| MOOC profile/courses/homework/tests | Yes | MOOC/overview |
| Electricity/campus-card lookup | Yes | electricity UI/widget |
| Campus-card CAS/ticket/token exchange | Yes | electricity chain |
| Teaching-calendar/semester metadata | Yes | calendar/timetable helpers |

## Network behavior

Only five hard-coded hosts were found. All are under `csust.edu.cn`; no My Changli Backend host, analytics host, ad host or unrelated third-party host was found in the library source/AAR.

| Host | Protocol in library | Endpoint categories | Credentials/cookies |
| --- | --- | --- | --- |
| `authserver.csust.edu.cn` | HTTPS | CAPTCHA check/image, login form, SSO POST, logout | student ID/password/CAPTCHA; SSO cookies |
| `ehall.csust.edu.cn` | HTTPS | login redirect target, login-user lookup, logout | SSO cookies/ticket |
| `xk.csust.edu.cn` | HTTP | SSO bridge, main page, timetable, grades, exams, classroom, teaching calendar | school session cookies; returned academic data |
| `pt.csust.edu.cn` | HTTP | MOOC SSO, profile, courses, homework, tests, logout | school/MOOC session cookies; returned learning data |
| `hxyxh5.csust.edu.cn` | HTTPS | campus-card CAS, OAuth token, electricity/building/room queries | ticket/token; student credentials are accepted by token API |

Redirect/cookie flow: Android requests the SSO login form, submits the user's credentials/CAPTCHA to the school auth server, follows redirects to eHall/educational/MOOC/campus-card services, and shares cookies through the library cookie jar. The library does not route this traffic through the My Changli Backend.

Security blocker: two core academic hosts use cleartext HTTP in the implementation. Even with release redistribution permission, the HTTP transport, redirect/cookie exposure and current school policy would need explicit security review before publication.

## Security behavior

| Check | Finding |
| --- | --- |
| Request/response logs | Upstream logs bodies and sensitive state; audited local AAR sets HTTP logger to `NONE` and routes library logging through a no-op `SafeLog` |
| Plaintext credentials | Library `EducationData` writes `student_id` and `student_password` to MMKV without encryption |
| Cookie persistence | Library `PersistentCookieJar` serializes cookies to `csust_cookie_jar` MMKV |
| App-side mitigation in full baseline | App initializes these library stores read-only after clearing them and stores password separately with Android Keystore-backed AES-GCM |
| Telemetry/analytics | None found |
| Hidden upload | None found |
| Extra/non-school host | None found |
| Reflection | Gson `TypeToken` only; no dynamic class loading found |
| Native code | No `.so` in AAR; no `System.loadLibrary` found |
| WebView/JavaScript execution | No WebView or JavaScript bridge in AAR; a `javascript:openWindow` string is parsed as an academic detail URL, not executed |
| Dynamic loading/JNI | None found |

The local modifications materially reduce log leakage, but they do not themselves grant redistribution rights and do not make the dependency safe in isolation. The app-side read-only-MMKV mitigation also requires device/process testing before it could be accepted.

## Clean reimplementation assessment

A clean-room adapter would need independently implemented and tested support for:

1. SSO/CAS form discovery, CAPTCHA, redirect/ticket state and logout.
2. Separate cookie jars and expiry/re-authentication behavior.
3. Timetable, term, grade, detail, exam and classroom HTML/JSON parsers.
4. MOOC profile/course/homework/test parsers.
5. Campus-card token exchange and electricity lookup.
6. Keystore-backed credential storage, memory-only sessions, clear-data/unbind semantics and log redaction.
7. Safe HTTPS policy or an explicit determination for currently HTTP-only school services.
8. Real-user, rate-limited, interactive E2E testing without sharing credentials in chat.

This is a cross-cutting authentication and data integration project, not a small substitute class. It must be separately authorized/scoped before implementation.

## Release decision matrix

| Option | Decision |
| --- | --- |
| A — ship current AAR | REJECTED until explicit source/modified-binary/App redistribution authorization and security closure exist |
| B — clean reimplementation | Technically possible in principle, but MAJOR_WORK and not authorized by this continuation after the Stop Condition |
| C — silently close all campus functions | REJECTED product interpretation |
| Current safe action | Keep AAR out of Release, record campus functions as product-OPEN but release-BLOCKED, return to user for decision |

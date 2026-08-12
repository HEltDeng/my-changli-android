# My Changli V1 Corrected Scope Matrix

- Date: 2026-08-12
- Complete baseline: `9afec16ef5f8095a4c949f08ac1426bf2cd916d1`
- Remediation start: `b015791949dc8ce6770429b3b4f9dcaeac3f7132`
- Previous scope result: `REJECTED_FOR_OVER_REMOVAL`
- Reason: `STUDY_AND_LIFE_CAMPUS_FEATURES_WERE_INCORRECTLY_REMOVED`
- Rights gate: `CORE_CAMPUS_SERVICE_RELEASE_BLOCKER = RESOLVED_BY_RIGHTSHOLDER_AUTHORIZATION`
- Current execution gate: `SCHOOL_CLEARTEXT_SECURITY_BLOCKER`

Fixed identity statement:

> 本应用由个人开发者独立开发，是面向校园生活场景的非官方辅助工具，与长沙理工大学不存在官方隶属或授权关系。

The corrected product rule is not `school-related => CLOSED`. Personal, user-authorized campus queries are product-OPEN. Only campus map, real sports reservation and real payment are explicit V1 exclusions.

`OPEN / BLOCKED` means the feature belongs in V1 but cannot yet enter a distributable Release because required authenticated school services are available only over cleartext HTTP. It is not a product reclassification to CLOSED.

## OPEN

| Capability | Product decision | Current implementation state |
| --- | --- | --- |
| School account bind/switch/unbind | OPEN | BLOCKED by cleartext school-service transport |
| SSO/CAS login, CAPTCHA, session/cookie, re-authentication and school logout | OPEN | BLOCKED by cleartext downstream session transport |
| School timetable sync/query, local timetable, course detail | OPEN | Local/manual subset present; school subset BLOCKED by HTTP `xk` |
| Grades and grade detail | OPEN | BLOCKED by HTTP `xk` |
| Exam arrangements | OPEN | BLOCKED by HTTP `xk` |
| Available classrooms | OPEN | BLOCKED by HTTP `xk` |
| MOOC courses/homework/tests | OPEN | BLOCKED by HTTP `pt` |
| Teaching/school calendar | OPEN | BLOCKED by HTTP `xk` |
| Electricity and campus-card-backed queries | OPEN | Source restoration stopped with the shared session chain; HTTPS service itself identified |
| Self-operated lost-and-found | OPEN | Present |
| Self-operated resource library | OPEN | Present |
| CET and Mandarin web utilities | OPEN | Present |
| My Changli own account | OPEN | Present and separate from school identity |
| Announcements, version/update metadata and Feature Flags | OPEN | Present; own production endpoint remains fail-closed until externally configured |
| Feedback | OPEN | Present |
| Local account book/date/week tools | OPEN | Present |
| Local timetable widgets and user-enabled reminders | OPEN | Present |
| Privacy/agreements/personal-information/account-data controls | OPEN | Present; must be expanded when school integration is restored |

## CLOSED

| Explicit V1 exclusion | Closure |
| --- | --- |
| Campus map | No Lite3DMap/AMap implementation, metadata, location permission, route or bypass |
| Real sports reservation | No venue/inventory/order/fulfillment request or reachable entry |
| Real payment/refund | No provider, SDK, merchant secret, callback or reachable payment UI |

No other baseline Study/Life capability is product-CLOSED by this matrix.

## REMOVE_FROM_RELEASE_BINARY

| Item | Release status |
| --- | --- |
| `Lite3DMap.jar` and map SDK implementation | Permanently excluded from this V1 candidate |
| Payment SDK/merchant implementation | Excluded |
| Real sports fulfillment implementation | Excluded or unreachable; no real request path |
| CSUSTDataGet source/binary | Project-specific use/modification/App-distribution authorization confirmed; still excluded from this branch because source integration stopped at the cleartext security gate |
| Debug-only SDKs | Excluded from Release runtime |

## FUTURE

- Explicit school cooperation/official authorization.
- A legally distributable campus-map replacement.
- Real sports fulfillment and inventory cooperation.
- Merchant payment/refund/settlement integration.
- Enterprise-entity-only features.
- Legacy community/Buddy/remote-skin services that do not currently map to the accepted production Backend contract; these require a separate product/contract decision and are not silently treated as one of the three exclusions.

## Stop-condition consequence

The project-specific dependency-rights gate is resolved. The attempted source-based restoration nevertheless proved that `xk.csust.edu.cn` and `pt.csust.edu.cn` do not expose a usable HTTPS service while the exact implementation attaches authenticated school-session cookies and receives personal academic/MOOC data over those hosts.

The branch therefore remains at the safe interim implementation. It does not add a domain-specific cleartext exception, does not restore the source module and does not request a real school account. Resolution requires working HTTPS or an authorized school-side replacement; until then this branch is not a full-campus V1 Release Candidate.

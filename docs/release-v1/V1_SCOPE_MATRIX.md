# My Changli V1 Corrected Scope Matrix

- Date: 2026-08-12
- Complete baseline: `9afec16ef5f8095a4c949f08ac1426bf2cd916d1`
- Remediation start: `b015791949dc8ce6770429b3b4f9dcaeac3f7132`
- Previous scope result: `REJECTED_FOR_OVER_REMOVAL`
- Reason: `STUDY_AND_LIFE_CAMPUS_FEATURES_WERE_INCORRECTLY_REMOVED`
- Current execution gate: `CORE_CAMPUS_SERVICE_RELEASE_BLOCKER`

Fixed identity statement:

> 本应用由个人开发者独立开发，是面向校园生活场景的非官方辅助工具，与长沙理工大学不存在官方隶属或授权关系。

The corrected product rule is not `school-related => CLOSED`. Personal, user-authorized campus queries are product-OPEN. Only campus map, real sports reservation and real payment are explicit V1 exclusions.

`OPEN / BLOCKED` means the feature belongs in V1 but cannot yet enter a distributable Release because the shared school integration has no proven redistribution right and a clean-room replacement is major work. It is not a product reclassification to CLOSED.

## OPEN

| Capability | Product decision | Current implementation state |
| --- | --- | --- |
| School account bind/switch/unbind | OPEN | BLOCKED |
| SSO/CAS login, CAPTCHA, session/cookie, re-authentication and school logout | OPEN | BLOCKED |
| School timetable sync/query, local timetable, course detail | OPEN | Local/manual subset present; school subset BLOCKED |
| Grades and grade detail | OPEN | BLOCKED |
| Exam arrangements | OPEN | BLOCKED |
| Available classrooms | OPEN | BLOCKED |
| MOOC courses/homework/tests | OPEN | BLOCKED |
| Teaching/school calendar | OPEN | BLOCKED |
| Electricity and campus-card-backed queries | OPEN | BLOCKED |
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
| Current modified `CSUSTDataGet` AAR | Provisionally excluded because redistribution right is unproven; this is the core blocker, not a request to delete campus product scope |
| Debug-only SDKs | Excluded from Release runtime |

## FUTURE

- Explicit school cooperation/official authorization.
- A legally distributable campus-map replacement.
- Real sports fulfillment and inventory cooperation.
- Merchant payment/refund/settlement integration.
- Enterprise-entity-only features.
- Legacy community/Buddy/remote-skin services that do not currently map to the accepted production Backend contract; these require a separate product/contract decision and are not silently treated as one of the three exclusions.

## Stop-condition consequence

The current branch remains at the safer over-removed implementation instead of inserting an unlicensed AAR. Restoring all school Study/Life features is not complete. The next user decision is either:

1. obtain explicit permission covering source modifications plus AAR/APK/AAB redistribution; or
2. authorize and scope a clean-room `SchoolServiceAdapter` project with interactive on-device testing.

Until one path is completed, this branch is not a full-campus V1 Release Candidate.

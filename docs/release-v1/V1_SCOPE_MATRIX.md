# My Changli V1 Release-First Scope Matrix

- Date: 2026-08-12
- Full baseline: `9afec16ef5f8095a4c949f08ac1426bf2cd916d1`
- Restoration start: `a3f31f5eaf12fd2bb21e0100033246d00c45dd6b`
- Policy: `RELEASE-FIRST / FUNCTION-FIRST / MINIMUM-NECESSARY-COMPLIANCE`
- CSUSTDataGet rights: `RESOLVED_BY_RIGHTSHOLDER_AUTHORIZATION`
- School HTTP decision: `KNOWN_V1_RISK`, not a feature-closure gate
- Runtime/UI user QA: `ACCEPTED` on 2026-08-12 at SHA `cda0300e7c306d98cc54d8b4a4a5f60e1e52974d`
- Freeze state: `V1_RUNTIME_UI_FROZEN / PRE_PRODUCTION_BASELINE`

The non-official identity remains fixed:

> 本应用由个人开发者独立开发，是面向校园生活场景的非官方辅助工具，与长沙理工大学不存在官方隶属或授权关系。

## OPEN, restored and user accepted

| Area | Capability | Runtime state | Acceptance state |
| --- | --- | --- | --- |
| School identity | bind/switch/unbind, student ID, password, CAPTCHA, SSO/CAS, redirects, Cookie/session, re-login and logout | RESTORED | USER_ACCEPTED on physical device |
| Study | school timetable sync, grades/details, exams/details, empty classroom, MOOC, semester and teaching calendar | RESTORED | USER_ACCEPTED on physical device |
| Study | resource library, CET, Mandarin and local/manual timetable | PRESERVED | USER_ACCEPTED on physical device |
| Life | electricity/campus-card query and school calendar | RESTORED | USER_ACCEPTED on physical device |
| Life | self-operated Lost & Found | PRESERVED | USER_ACCEPTED on physical device |
| General | My Changli account, announcements, update checks, flags, feedback, local tools, widgets and reminders | PRESERVED | runtime/UI USER_ACCEPTED; production own-Backend pending |

School passwords, CAPTCHA values, school Cookies/tokens/sessions and academic data go directly from the Android client to the applicable school service. They are not fields in the My Changli Backend contract.

## CLOSED in V1

| Capability | Enforced closure |
| --- | --- |
| Campus map | no Lite3DMap/AMap dependency, metadata, location permission, route or Activity |
| Real sports reservation | no visible shortcut, route, Activity or live inventory/order/fulfilment path |
| Real payment/refund | no SDK, merchant configuration, secret, payment route or live money path |

## Binary decisions

- `CSUSTDataGet-1.1.4-no-sensitive-logs.aar`: included under project-specific authorization; exact SHA-256 is verified by Gradle before every build.
- Lite3DMap/AMap: absent.
- Alipay/WeChat payment SDKs: absent.
- Debug diagnostic SDKs: Debug-only and explicitly make the QA APK unsuitable for store submission.
- Release user surface: formal label/package, clean legal-document titles, no reachable local demo/placeholder Activities, and no simulated update action.

## Deferred, not silently closed

- Migration from the authorized fixed AAR to a fully reproducible source module.
- Removal of all legacy credential/cache implementation classes after equivalent runtime behavior is verified.
- Migration of `xk` and `pt` to HTTPS when the school provides equivalent working endpoints.
- Final own-Backend production endpoint, filing, formal signing and store-binary QA.

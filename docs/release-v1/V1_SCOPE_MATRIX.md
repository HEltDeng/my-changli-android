# My Changli V1 Release-First Scope Matrix

- Date: 2026-08-12
- Full baseline: `9afec16ef5f8095a4c949f08ac1426bf2cd916d1`
- Restoration start: `a3f31f5eaf12fd2bb21e0100033246d00c45dd6b`
- Policy: `RELEASE-FIRST / FUNCTION-FIRST / MINIMUM-NECESSARY-COMPLIANCE`
- CSUSTDataGet rights: `RESOLVED_BY_RIGHTSHOLDER_AUTHORIZATION`
- School HTTP decision: `KNOWN_V1_RISK`, not a feature-closure gate

The non-official identity remains fixed:

> 本应用由个人开发者独立开发，是面向校园生活场景的非官方辅助工具，与长沙理工大学不存在官方隶属或授权关系。

## OPEN and restored for QA

| Area | Capability | Runtime state | Acceptance state |
| --- | --- | --- | --- |
| School identity | bind/switch/unbind, student ID, password, CAPTCHA, SSO/CAS, redirects, Cookie/session, re-login and logout | RESTORED | real-account E2E pending user QA |
| Study | school timetable sync, grades/details, exams/details, empty classroom, MOOC, semester and teaching calendar | RESTORED | real-account E2E pending user QA |
| Study | resource library, CET, Mandarin and local/manual timetable | PRESERVED | user QA pending |
| Life | electricity/campus-card query and school calendar | RESTORED | real-account E2E pending user QA |
| Life | self-operated Lost & Found | PRESERVED | own-Backend environment and user QA pending |
| General | My Changli account, announcements, update checks, flags, feedback, local tools, widgets and reminders | PRESERVED | user QA pending |

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

## Deferred, not silently closed

- Migration from the authorized fixed AAR to a fully reproducible source module.
- Removal of all legacy credential/cache implementation classes after equivalent runtime behavior is verified.
- Migration of `xk` and `pt` to HTTPS when the school provides equivalent working endpoints.
- Final own-Backend production endpoint, filing, formal signing and store-binary QA.

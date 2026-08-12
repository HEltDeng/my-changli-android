# V1 Release Permission Audit — Scope Remediation

- Date: 2026-08-12
- Evidence: generated merged Release Manifest after `processReleaseMainManifest`
- Campus feature restoration: BLOCKED before implementation by `SCHOOL_CLEARTEXT_SECURITY_BLOCKER`

## Merged Release permissions

| Permission | Current OPEN consumer | Decision |
| --- | --- | --- |
| `INTERNET` | own Backend, lost-found, feedback, resource library, announcements/version data, public web utilities | Keep; future direct school HTTPS/HTTP review uses the same permission |
| `ACCESS_NETWORK_STATE` | network failure-state handling | Keep |
| `POST_NOTIFICATIONS` | user-enabled local class reminders | Keep; runtime request must remain user initiated |
| `RECEIVE_BOOT_COMPLETED` | restore user-enabled reminders/widgets | Keep |
| `SCHEDULE_EXACT_ALARM` | exact class reminder with fallback | Keep; store declaration and device behavior remain manual gates |
| app-scoped dynamic receiver permission | AndroidX generated signature permission | Keep/generated |

## Confirmed absent from merged Release

- Coarse/fine location: absent; campus map remains CLOSED.
- Wi-Fi state/change: absent.
- Camera: absent.
- Broad media and legacy storage: absent from merged Manifest. Source-level `tools:node="remove"` markers intentionally suppress transitive legacy declarations and cause two lint warnings; they do not grant permission.
- Foreground service: absent.

## Corrected campus scope impact

School login, timetable, grades, exams, classroom, MOOC, electricity/campus-card and school calendar do not inherently require location, camera, media, Wi-Fi-control or foreground-service permissions. Their future restoration must not bring back map/location permissions.

Only `MainActivity` is exported as launcher. No business `<data>` deep link exists. The current interim branch has no school Activity/route to audit because restoration stopped at the transport gate; this is not evidence that the corrected campus scope is complete. No cleartext domain exception was added.

## Result

Current merged permission surface is minimal and explainable. Re-run after lawful school integration is restored, and retain the explicit campus-map/location exclusion.

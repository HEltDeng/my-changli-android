# V1 Permission Audit

- Date: 2026-08-12
- Evidence: final merged Release Manifest and Debug QA APK

## Release merged permissions

| Permission | Consumer / reason | Decision |
| --- | --- | --- |
| `INTERNET` | school queries, own Backend, Lost & Found, resources, feedback and public utilities | keep |
| `ACCESS_NETWORK_STATE` | offline/error state | keep |
| `POST_NOTIFICATIONS` | user-enabled class reminders | keep; runtime consent required |
| `RECEIVE_BOOT_COMPLETED` | restore user-enabled reminders/widgets | keep |
| `SCHEDULE_EXACT_ALARM` | exact class reminder with fallback | keep; store-sensitive/manual review |
| generated not-exported receiver signature permission | AndroidX runtime receiver isolation | generated/keep |

Release has no location, Wi-Fi-control, camera, media/legacy-storage, foreground-service or wake-lock permission in the final merged manifest.

## Debug QA additions

The Debug APK also has `WAKE_LOCK` and `FOREGROUND_SERVICE`, contributed by debug/runtime tooling and WorkManager dependencies. Debug additionally contains CodeLocator, LeakCanary and `libnrb.so`. These are accepted only for the explicitly non-store QA artifact and must be absent/re-audited on the final Release APK/AAB.

## Components and deep links

Business Activities are non-exported. `MainActivity` is the app launcher. No app-owned campus business deep link was found. Dependency debug tools add their own exported diagnostic components, which reinforces `DEBUG ONLY / NOT RC` status.

Campus map does not add location, Wi-Fi or camera permissions.

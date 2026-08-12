# V1 Permission Audit

- Date: 2026-08-12
- Evidence: freeze-stage merged Release Manifest and accepted Debug QA APK

## Release merged permissions

| Permission | Consumer / reason | Decision |
| --- | --- | --- |
| `INTERNET` | school queries, own Backend, Lost & Found, resources, feedback and public utilities | keep |
| `ACCESS_NETWORK_STATE` | offline/error state | keep |
| `POST_NOTIFICATIONS` | user-enabled class reminders | keep; runtime consent required |
| `RECEIVE_BOOT_COMPLETED` | restore user-enabled reminders/widgets | keep |
| `SCHEDULE_EXACT_ALARM` | exact class reminder with fallback | keep; store-sensitive/manual review |
| `WAKE_LOCK` | WorkManager scheduling/runtime support | keep; dependency-contributed, not a Debug permission |
| `FOREGROUND_SERVICE` | WorkManager system foreground service declaration | keep; dependency-contributed, not a Debug permission |
| generated not-exported receiver signature permission | AndroidX runtime receiver isolation | generated/keep |

Release has no location, Wi-Fi-control, camera, media/legacy-storage, map or payment-specific permission. `WAKE_LOCK` and `FOREGROUND_SERVICE` are contributed by WorkManager in both relevant runtime configurations; they are not Debug-tool permissions.

## Debug QA additions

Debug contains CodeLocator, LeakCanary, Compose tooling/test manifest and `libnrb.so`. These are accepted only for the explicitly non-store QA artifact. Release runtime dependency resolution confirms those Debug-only dependencies are absent; repeat the check on the final signed Release APK/AAB.

## Components and deep links

Business Activities are non-exported. `MainActivity` is the only app-owned exported launcher Activity. No app-owned campus business deep link was found. Release removes local demo/placeholder Activities through a Release manifest overlay. AndroidX WorkManager/ProfileInstaller components protected by system/signature permissions are dependency internals and are not ordinary user-openable developer menus.

Campus map does not add location, Wi-Fi or camera permissions.

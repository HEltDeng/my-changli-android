# V1 Dependency Closure Report

- Date: 2026-08-12
- Baseline: `9afec16ef5f8095a4c949f08ac1426bf2cd916d1`
- Branch: `release/v1-initial`

## Result

`CSUSTDataGet-1.1.4-no-sensitive-logs.aar` and `Lite3DMap.jar` are not V1 build dependencies. Neither file exists in the V1 worktree, neither has a Gradle/catalog declaration, and `preBuild` fails if either filename reappears.

## Closure actions

- Removed `flatDir`, local `files(...)` dependencies and the dormant CSUST catalog coordinate.
- Removed school authentication/session, education, grade, exam, MOOC and campus-card implementations from the V1 branch.
- Kept local Room timetable/manual course flows without a school session.
- Removed map/location classes, AMap privacy initialization, metadata, permissions and network exceptions.
- Removed closed UI, Route targets, Manifest declarations, widget/receiver paths, stale school credential layouts and account reservation-profile collection.
- Kept future Backend DTO/status structures only where they are unreachable from V1 UI/navigation.
- Added `V1ReleasePolicy`, source-boundary tests and the forbidden-file build task.

## Verification

| Check | Result |
| --- | --- |
| Target files present in worktree | `NO` |
| Target Gradle/catalog declarations | `NO` |
| Target package imports in main source | `NO` |
| Reflection/JNI loader for target packages | `NO` |
| Release dependency-tree matches | `NONE` |
| Debug APK target ZIP-entry matches | `0` |
| Debug APK DEX target names/packages | `NONE` |
| Build without target files | `PASS` |

The Debug APK DEX scan found no `CSUSTDataGet`, `Lite3DMap`, `com/dcelysia/csust_spider`, `com/example/csustdataget` or `com/amap` strings after user-facing audit text was made implementation-neutral.

## Remaining gate

A signed, minified Release APK/AAB does not yet exist because `MY_CHANGLI_SIGNING_CONFIG` is absent. The final formal-binary scan must be repeated after Gate A provides the existing signing identity. No replacement keystore was created.

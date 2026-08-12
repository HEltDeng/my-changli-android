# V1 Release SDK Audit

- Date: 2026-08-12
- Evidence: `releaseRuntimeClasspath` after V1 closure

## Release runtime surface

| Group | Included components | V1 use |
| --- | --- | --- |
| Android UI/runtime | AndroidX core/appcompat/activity/fragment/lifecycle, Compose BOM/UI/Material3, Material, ConstraintLayout, Navigation3 runtime | App UI and navigation |
| Persistence/reactive | Room, MMKV, RxJava/RxAndroid | Local timetable, settings, cache and state |
| Own-network stack | OkHttp, logging-interceptor, Retrofit, Gson | Accepted Backend contract and explicit public endpoints |
| Image/document UI | Glide, Coil, PhotoView, Lottie, FlexboxLayout, StateLayout, Haze | OPEN lost-found, resources, account and presentation UI |
| Local schedule UI | TimetableView, AndroidPicker Common/WheelPicker | Manual timetable/date selection |
| Internal module | `:CP_Common` | Existing shared app code |
| Profile support | AndroidX ProfileInstaller | Android runtime profile support; the stale old-package profile file itself was removed |

## Debug-only, not Release

- LeakCanary.
- CodeLocator and its RestrictionBypass native library (`libnrb.so`).
- Compose UI tooling and test manifest.

Release dependency-tree filtering returned no `leakcanary` or `codelocator` match.

## Confirmed absent from Release dependency tree

- `CSUSTDataGet` and `Lite3DMap`.
- AMap/location SDK.
- Alipay, WeChat/Weixin or any payment SDK.
- Bugly.
- WorkManager.
- uCrop, jsoup and other removed unused libraries.
- Local AAR/JAR file dependency or `flatDir` repository.

## Native-library evidence

The Debug APK contains `libmmkv.so`, `libandroidx.graphics.path.so`, and debug-only `libnrb.so`. `libmmkv.so` comes from MMKV, `libandroidx.graphics.path.so` from Compose/AndroidX graphics, and `libnrb.so` from debug-only CodeLocator's RestrictionBypass. A formal Release APK/AAB must be rescanned after signing to confirm only the Release set remains.

## Audit conclusion

The two unverified binaries, payment SDKs, map SDKs and debug SDKs are absent from `releaseRuntimeClasspath`. Runtime components that remain have an identifiable OPEN feature or Android framework purpose. This is a dependency-tree result, not final formal-binary acceptance.

# V1 Release SDK Audit — Scope Remediation

- Date: 2026-08-12
- Evidence: current `releaseRuntimeClasspath` plus local AAR/source audit
- Current binary state: safe interim dependency set; full campus target is BLOCKED

## Current Release runtime

| Group | Included | Use |
| --- | --- | --- |
| Android framework/UI | AndroidX core/appcompat/activity/fragment/lifecycle, Compose/Material, ConstraintLayout, Navigation | UI/navigation |
| Persistence/reactive | Room, MMKV, RxJava/RxAndroid | local timetable/settings/cache/state |
| Network/parsing | OkHttp, Retrofit, Gson | own Backend and explicit public endpoints |
| Image/document UI | Glide, Coil, PhotoView, Lottie, FlexboxLayout, StateLayout, Haze | lost-found/resources/account/presentation |
| Schedule UI | TimetableView, picker modules, ProfileInstaller | local schedule/reminder/runtime support |
| Internal | `:CP_Common` | existing shared app code |

## Debug-only

- LeakCanary.
- CodeLocator/RestrictionBypass and debug native `libnrb.so`.
- Compose tooling/test manifest.

They are not in `releaseRuntimeClasspath`.

## Excluded

- Lite3DMap and AMap/location SDK: absent; map is CLOSED.
- Alipay, WeChat/Weixin or other payment SDK: absent; real payment is CLOSED.
- Bugly and other dormant/debug SDKs: absent from Release.
- Real sports provider/merchant SDK: absent.

## CSUSTDataGet

The current Release dependency tree does not contain CSUSTDataGet. That absence is no longer a rights requirement; project-specific authorization is recorded. It is the safe consequence of `SCHOOL_CLEARTEXT_SECURITY_BLOCKER_CONFIRMED` before source integration.

The audited local AAR contains 162 classes and no native library. The preferred authorized design is a reproducible source module, not the opaque local derivative. That integration was not added because the source uses authenticated cleartext education/MOOC services.

## Result

The transport follow-up changed no Gradle dependency. The present dependency set passes the previously recorded interim Release compile and contains no map/payment target. It does not implement the corrected full-campus scope. Re-run `releaseRuntimeClasspath`, SDK filing classification and final binary scan after safe school-service transport exists.

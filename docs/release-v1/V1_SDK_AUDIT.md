# V1 SDK and Dependency Audit

- Date: 2026-08-12
- Evidence: freeze-stage `releaseRuntimeClasspath`, Gradle configuration, AAR inspection and accepted Debug QA APK scan

## Included release/runtime groups

- AndroidX UI/lifecycle/navigation/Room/WorkManager/ProfileInstaller and Compose/Material.
- MMKV for non-secret app state plus guarded legacy integration stores.
- OkHttp 5.3.2, Retrofit 3.0.0 and Gson for school, own-Backend and public services.
- Glide/Coil/PhotoView/Lottie and existing document/schedule UI libraries.
- `:CP_Common` internal module.
- Authorized `CSUSTDataGet-1.1.4-no-sensitive-logs.aar`, fixed by SHA-256 and containing 162 Java/Kotlin classes, no native library.
- `desugar_jdk_libs:2.1.5` through core-library desugaring so Java Time usage supports minSdk 24.

## Debug-only

- LeakCanary.
- CodeLocator/RestrictionBypass and `libnrb.so`.
- Compose tooling/test manifest.

The Debug QA APK contains three native libraries: AndroidX graphics path, MMKV and debug-only `libnrb.so`.

## Confirmed absent

- Lite3DMap and AMap SDK/classes/native libraries.
- Alipay, WeChat/Weixin or other payment SDK classes.
- Bugly.
- Real sports provider/merchant SDK.
- LeakCanary, CodeLocator/RestrictionBypass, Compose UI test manifest, MockWebServer and JUnit from `releaseRuntimeClasspath`.

The Debug class-descriptor scan confirmed CSUSTDataGet and restored school Activities are present while CampusMapActivity and SportsReservationActivity are absent.

## Release conclusion

Release dependency resolution and Release Kotlin/Java compilation pass. The Release manifest also removes retained local demo/placeholder Activities. A final store APK/AAB still requires production endpoint, formal signing use, Release binary generation and a fresh binary/SDK audit; the accepted Debug QA package is evidence, not the store artifact.

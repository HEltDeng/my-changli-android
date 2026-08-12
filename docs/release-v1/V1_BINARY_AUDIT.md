# V1 Debug QA Binary Audit

- Artifact: `MyChangli-V1.0.0-Full-QA-debug.apk`
- Type: `DEBUG QA ONLY / NOT RELEASE / NOT RC`
- Size: 33,813,056 bytes
- SHA-256: `E8F3ED318633A075E5CD36F627CF25CEA284567F4291CF588B56691F657D433C`
- Package: `com.mychangli.app.debug`
- Version: `1.0.0` / `32`
- Label: `我的长理 Debug`
- minSdk / targetSdk: 24 / 36
- Signing: Android Debug; certificate SHA-256 `8CE0AA11F57B7921BED57258461B1AAEE5D795E801DC49BCA8471CC7FAE77B15`

## Runtime proof

DEX descriptor audit confirms the authorized `com.dcelysia.csust_spider` classes plus BindingUser, grades, exams, classroom, MOOC and electricity Activities are packaged. The merged Manifest contains the restored school Activities as non-exported components.

## Exclusion proof

No class descriptor matched Lite3DMap, AMap, CampusMapActivity, SportsReservationActivity, Alipay or WeChat/Weixin payment SDK packages. No location, camera, Wi-Fi-control or legacy storage permission is granted.

## Network policy

The packaged Debug network config denies cleartext by default, permits exact xk/pt school hosts, and permits exact loopback hosts for Debug own-Backend QA. No map domain exception remains. The Release source config contains only exact xk/pt exceptions.

## Debug-only surface

The QA APK contains LeakCanary, CodeLocator/RestrictionBypass, WorkManager-generated components, `WAKE_LOCK`, `FOREGROUND_SERVICE` and `libnrb.so`. These are expected Debug diagnostics and prohibit treating the package as an RC/store binary.

## Pending final Release audit

Formal signing, production own-Backend domain/configuration, minified Release APK/AAB generation, certificate/hashes and store-binary secret/domain/class/native-lib scans remain later gates. No real credential or user data is in this artifact or report.

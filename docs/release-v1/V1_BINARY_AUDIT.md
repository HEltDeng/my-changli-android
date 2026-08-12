# V1 Binary Audit

- Date: 2026-08-12
- Formal signed Release binary: `NOT_AVAILABLE`
- Formal audit status: `PARTIAL — WAITING_FOR_FORMAL_SIGNING`

## Debug engineering artifact

Path: `app/build/outputs/apk/debug/MyChangli_debug_v1.0.0.apk`

| Field | Observed value |
| --- | --- |
| Size | 35,899,130 bytes |
| applicationId | `com.mychangli.app.debug` |
| versionCode | `32` |
| versionName | `1.0.0` |
| label | `我的长理 Debug` |
| launcher | `com.mychangli.app.core.MainActivity` |
| icon | `res/mipmap-anydpi-v26/my_changli_icon.xml` |
| min/target SDK | 24 / 36 |
| ABI | arm64-v8a |
| MD5 | `9E82ECA996A9F1F8FFB7DB311BE27C58` |
| SHA-1 | `41E32C822F854A24CA814BEE2C84889C3FB50019` |
| SHA-256 | `37174DD12D43C954E0103F6253C4E78F773D2BC6BD096A71B13AEC8FA089739E` |
| certificate | Android Debug, SHA-256 `8ce0aa11f57b7921bed57258461b1aaee5d795e801dc49bca8471cc7fae77b15` |

This artifact is debuggable, Debug-signed, uses `.debug`, and contains `127.0.0.1`/`localhost` from the Debug BuildConfig. It is not a Release Candidate and must not be renamed as one.

## Manifest and permission evidence

The Debug APK contains INTERNET, network state, notifications, boot completed, exact alarm and the AndroidX app-scoped dynamic-receiver permission. It contains no location, Wi-Fi control, camera, media or legacy storage permission after manifest merge.

The generated merged Release Manifest contains the same five app permissions plus the generated app-scoped dynamic-receiver permission, only the Launcher activity exported, no business deep link, no AMap metadata and cleartext disabled.

## Target dependency scan

| Check | Result |
| --- | --- |
| ZIP entry names matching CSUSTDataGet/Lite3DMap/AMap/payment/Bugly | 0 |
| DEX `CSUSTDataGet` | absent |
| DEX `Lite3DMap` | absent |
| DEX school dependency package paths | absent |
| DEX `com/amap` | absent |
| DEX Alipay/WeChat package paths | absent |
| DEX old school API host | absent |
| DEX migration-era old package path | absent |

## Native libraries

Debug ZIP entries are `libandroidx.graphics.path.so`, `libmmkv.so`, and `libnrb.so`. Dependency evidence attributes the first two to Release runtime AndroidX/MMKV and `libnrb.so` to debug-only CodeLocator/RestrictionBypass. The Release dependency tree contains no CodeLocator; a signed Release must confirm `libnrb.so` is absent.

## Release-side evidence without a Release binary

- Generated Release BuildConfig endpoint: `https://campus-tools.invalid/api/v1/`.
- Release endpoint task rejects localhost, loopback, `.local`, cleartext HTTP and historical school/community hosts.
- `releaseRuntimeClasspath` has no target/map/payment/debug SDK match.
- Release Kotlin, Java, resources and Manifest compile successfully without the target binaries.

## Required final rerun

After Gate A supplies the existing external `MY_CHANGLI_SIGNING_CONFIG`, build signed APK and AAB and repeat application identity, certificate, hashes, manifest, permission, SDK/class/native-library, host/IP, secret, debug/log, old package/brand, school credential and payment-configuration scans. Until then `MyChangli-V1.0.0-RC.apk` is intentionally not produced.

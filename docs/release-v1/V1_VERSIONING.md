# V1 Versioning

- User-facing version: `1.0.0`
- Android `versionCode`: `32`
- Android applicationId / namespace: `com.mychangli.app`
- Debug applicationId: `com.mychangli.app.debug`

The complete baseline used `versionCode 31` and `versionName 2.0.9`. The first public product name is reset to `1.0.0`, but `versionCode` is advanced to `32` rather than lowered to `1`, preserving Android's monotonic upgrade rule.

Future public builds must use a `versionCode` greater than 32. `versionName` may follow semantic versioning independently.

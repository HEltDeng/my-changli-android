# V1 Dependency Closure Report

- CSUSTDataGet: `AUTHORIZED + INTEGRATED + FIXED-HASH VERIFIED`
- Lite3DMap: `ABSENT`
- AMap: `ABSENT`
- Payment SDK: `ABSENT`
- Real sports runtime: `ABSENT / UNREACHABLE`

The V1 worktree builds without Lite3DMap and without any map/payment local binary. The authorized CSUSTDataGet derivative is intentionally the only restored local AAR. Its file name, size and SHA-256 are fixed, and build configuration fails on absence or mismatch.

Debug APK class-descriptor audit found the campus library and all key school runtime Activities. It found no CampusMapActivity, SportsReservationActivity, Lite3DMap, AMap, Alipay or WeChat payment classes.

The two legacy school hosts are handled by exact-host network-security rules. This is a documented compatibility risk, not an unknown dependency.

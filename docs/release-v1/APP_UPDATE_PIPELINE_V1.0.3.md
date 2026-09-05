# V1.0.3 App 内更新器设计与验收边界

状态：`Release Candidate only / Production unchanged`

V1.0.2（`versionCode=34`）是不可变正式 APK。本文件描述首次加入 App 内下载安装更新器的 V1.0.3（`versionCode=35`）候选实现；它没有覆盖、重签或替换 V1.0.2。

## 统一调用链

自动检查和“我的 → 检查更新”都调用同一个 `AppUpdateChecker`。生产 Backend 是主源；Backend 请求失败、响应不完整或下载地址不安全时，才请求同域 `https://mychangliapp.cn/releases/latest.json`。手动检查使用 `forceRefresh=true` 和 cache-busting 参数，自动检查不重复请求官网。

两条入口得到 `VersionCheckResult.Available` 后都交给 `AppUpdateCoordinator`，不再打开官网页面。Coordinator 负责下载、断点续传、校验和系统安装；Activity 只观察 `StateFlow` 并展示状态。

## 状态与生命周期

```text
IDLE → DOWNLOADING → VERIFYING → READY_TO_INSTALL
                                  ├→ NEED_INSTALL_PERMISSION → INSTALLING
                                  └→ INSTALLING
INSTALLING → COMPLETED
任意下载/校验/安装错误 → FAILED
```

下载任务使用进程级 CoroutineScope，文件写入 `filesDir/app-updates/update-<versionCode>.apk.partial`。Activity 重建、旋转或暂时离开前台不会取消任务；重新进入 Activity 会读取待安装记录，并避免重复创建 PackageInstaller session。HTTP 206 支持从已有 `.partial` 文件继续；HTTP 错误、响应长度不完整、取消和旧版本文件会被明确处理。

## 安装前安全门

下载完成后必须依次通过：

1. HTTPS 公网下载地址和 manifest 元数据完整性检查；
2. SHA-256 与 Backend/官网 manifest 一致；
3. APK `packageName=com.mychangli.app`、`versionName`、`versionCode > 当前版本` 一致；
4. 正式签名证书 SHA-256 为 `2C3B934931C26719C1A55FA707760A41E18BA15B00561F97691FE1D452F4BF3D`。

任一项失败立即删除临时 APK，不进入安装。校验通过后才提交 Android `PackageInstaller`。Android 8.0+ 若尚未授权“允许来自此来源的应用”，界面进入该应用的设置页；返回后复用已校验 APK，不要求重新下载。系统安装器仍由用户确认，应用不做静默安装、ADB、Root 或无障碍自动点击。

## 文件清理

同一时刻只保留当前目标版本的临时文件。校验失败、安装失败、安装成功或下一个版本开始下载时清理过期/失败 APK；成功升级由 Android 按同包名、同签名和递增 versionCode 覆盖安装，用户数据由系统正常保留。

## 自动化与设备边界

单元测试覆盖版本数值比较、Backend 主源/官网 fallback、手动 cache-busting、HTTP 成功/失败/断点/不完整、SHA 匹配/不匹配、包名/版本/证书拒绝和元数据缺失。`:app:compileDebugKotlin`、`:app:testDebugUnitTest`、`:app:lintRelease` 和正式签名 Release 候选构建通过。

当前没有连接 HONOR 真机或模拟器，因此系统安装器、未知来源授权、真实进度展示、升级后数据保留和真实旧版本弹窗必须标记 `PENDING_REAL_DEVICE`，不能写成设备 `PASS`。V1.0.3 在完成设备验收和完整 Release Gate 前不得发布到 Backend、官网或线上 APK 地址。

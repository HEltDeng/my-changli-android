# CSUSTDataGet 外部认证配置与 Mac 恢复

本开发基线使用 `app/libs/CSUSTDataGet-1.1.4-external-auth.aar`。AAR 不含真实认证值，库仅接收宿主 App 提供的配置；校园卡端点、请求参数和业务调用方法保持不变。

依赖源码：https://github.com/HEltDeng/CSUSTDataGet ，分支 `migration/clean-source-baseline`。这是项目所有者在当前账号下建立的安全开发恢复仓库。原 CreaMakers 也是同一所有者控制的账号；旧历史未被复制到新仓库，也未被重写或删除。许可见 `docs/dependencies/CSUSTDataGet-LICENSE.txt`。

## 新 Mac 恢复本地开发

1. `git clone --branch codex/campus-activities-preview https://github.com/HEltDeng/my-changli-android.git`。
2. 安装 JDK 17、Android SDK 36.1，配置 `ANDROID_HOME` 或本机 `local.properties` 中的 `sdk.dir`。
3. 将 `secrets.properties.example` 复制为仓库根目录的 `secrets.properties`，通过安全私密渠道填入两项值。文件已被 Git 忽略；不要放入聊天、README、命令行参数或受跟踪的 Gradle 配置。
4. 运行 `sh ./gradlew :app:testReleaseUnitTest :app:assembleDebug --no-configuration-cache --no-build-cache`。

也可通过同名环境变量提供配置，优先级高于本地 properties 文件；不使用带真实值的 `-P` 命令行参数。

本地 Debug 由 Gradle 从私密配置生成 BuildConfig，`PlanetApplication` 启动时向 `CampusCardAuthConfig.configure` 提供参数。缺少参数会在 Debug 构建时明确失败，错误信息不会输出值。库本身构建不读取私密配置，因此 AAR 可作为已扫描的源码恢复依赖入库。

## 严格分开源码与本地 Debug 制品

- 当前受 Git 跟踪源码和依赖 AAR 必须无真实认证值。
- 本地 Debug APK 和 `app/build/` 中的生成 BuildConfig **包含注入值**，只允许开发机本地使用。不得上传 GitHub、CI artifacts、Release 或公开分发。
- 关闭 configuration/build cache 的构建命令用于减少私密参数进入缓存；构建目录仍是敏感本地产物，不是备份上传目标。
- Release、candidate、benchmark 打包由明确门禁阻止，等待单独认证归属和分发审查。Release 单元测试允许编译无值配置，但不代表 App Release 获准构建/分发。
- 当前正式线上 App、版本号、签名身份和生产服务未修改。`REAL_SERVICE_AUTH_VALIDATION: PENDING`。
- `OWNER_UNKNOWN` 指认证值的管理方尚未确认，不是 GitHub 仓库所有权未知。
- 原 my-changli-android Git history 仍可能包含旧 AAR：`HISTORICAL_SECRET_REMEDIATION: PENDING`。

## 依赖重建

在独立 CSUSTDataGet 工作目录，按其 README 构建 `:csust_spider:assembleRelease` 和单元测试。先扫描源码、解包 AAR/class/字符串并确认无值，再更新 Android 内的 AAR 和 `verifyAuthorizedCsustDataGet` 的 SHA-256。不得直接从旧含值制品复制或仅改文件名。

## 本次依赖构建身份

- Library clean root commit: 890a353ff3e49d420e76492ef42e7a1dad6333ba
- AAR SHA-256: 9bb1e69c98cf1d9e0e484774bdae48cac0025d0b5d6e5ab3176ffd583a94061c
- Library 配置测试：5 / 5 PASS；解包 AAR 已知认证值扫描与 Gitleaks：PASS。

## 2026-10-11 Owner 个人手机联网验收

Owner 本轮明确要求正式后端联网、正式签名 Release 仅安装自己的手机，不官网分发。增加 privateDeviceAcceptance 显式本机构建例外，默认公开 Release/candidate/benchmark 门禁不解除。此例外不把认证值归属标为已确认、不授予公开分发权限；APK 仅本机及 Owner 手机保存，禁止上传。详见 campus-activities-directed-release.md。

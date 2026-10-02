# 源码身份校验与受控恢复

`tools/source-identity.json` 是可移植身份规则的唯一来源：产品、canonical GitHub owner/repository、正式基线和组件标记。不要写入机器绝对路径。`Test-MyChangliSourceIdentity.ps1` 是唯一检查实现；项目外 04_Tools 同名文件只是转发器，旧 04_Tools/source-identity.json 仅为指针。

本机 `04_Tools/local-source-entrypoints.json` 登记三个日常入口和依赖源码位置。日常模式只接受登记目录；新机器应明确建立自己的登记。Validation 模式要求显式目录，并继续核验真实 Git 根、origin、产品、正式基线祖先关系和禁止目录。它不是 SkipIdentity，也不切分支/reset/修改源码。

```powershell
# 日常固定入口（本机外部转发器）
& '<项目根>/04_Tools/Test-MyChangliSourceIdentity.ps1' -Component android
# 受控全新克隆根目录，显式核验
& ./tools/Test-MyChangliSourceIdentity.ps1 -Mode Validation -Repository .
# 仅当前进程启用受控构建；结束后清除
$env:MYCHANGLI_SOURCE_MODE='Validation'
try { ./gradlew.bat assembleDebug testDebugUnitTest --no-daemon --no-build-cache --rerun-tasks } finally { Remove-Item Env:MYCHANGLI_SOURCE_MODE }
```

HTTPS、git@github.com:owner/repo.git、ssh://git@github.com/owner/repo.git 识别为同一 owner/repo；其他 host、owner、repo 拒绝。基线后继可继续前进；浅克隆不能证明祖先关系时应取得授权的完整历史，不能绕过。相对 Repository 相对当前工作目录，登记内相对路径相对登记文件；目录联接/符号链接必须使用真实目录。

覆盖范围：Windows gradlew.bat 启动 Gradle 前检查；现用签名构建通过 04_Tools 转发器执行 Development 检查。直接 IDE/Gradle Tooling API、POSIX gradlew、直接 gradle/JVM 没有自动钩子，必须先显式预检，不能称全部入口受保护。Backend/Website 同样显式检查，普通 Python/pnpm 不是自动守卫入口。

测试：`python tools/test_source_identity.py --scratch <新的隔离夹具目录>`，15 项正反例；仅在指定夹具中写入。此本地夹具测试不冒充远端恢复证明，远端验收见 FINAL_HANDOVER。

正式 APK 对应 d56fc5e 归档，基线校验不证明重建 APK 哈希相同。发布仍需审核 dirty、签名、产物、用户验收与批准。

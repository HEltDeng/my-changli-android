# 我的长理 android 来源与开发入口

仓库：HEltDeng/my-changli-android；main 持续向前，不可 reset 回历史提交。组件基线：`d56fc5efc8f1584309a7bc348035780e2c816c27`。
本机固定目录：`D:\Codex\01_App-应用开发\001_MyChangLi-我的长理\01_Source-当前源码\01_Android`。本机统一导航在项目根 SOURCE_OF_TRUTH.md，本文件仅说明本组件。

便携身份规则与唯一实现位于 Android tools/source-identity.json、Test-MyChangliSourceIdentity.ps1；本机路径在项目外 local-source-entrypoints.json。受控克隆显式使用 Validation，仍检查 origin、产品和基线。
正式 V1.0.5 和生产部署不能被验证产物替换。历史说明不是默认开发入口。

发布基线 d56fc5efc8f1584309a7bc348035780e2c816c27；Tag v1.0.5-release-20261001，详细证据见 docs/releases/v1.0.5-20261001。依赖恢复见 tools/DEPENDENCY_RECOVERY.md。Windows wrapper 自动检查；IDE/POSIX/直接 Gradle 先显式预检。

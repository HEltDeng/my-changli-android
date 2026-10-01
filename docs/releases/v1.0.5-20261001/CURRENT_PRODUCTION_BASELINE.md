# CURRENT PRODUCTION BASELINE

记录日期：2026-10-01（用户已完成人工验收；本轮执行正式发布及发布一致性核对）

## 当前正式生产身份

- 我的长理当前正式发布版本：`V1.0.5`
- versionName：`1.0.5`
- versionCode：`37`
- Android正式源码checkout：`02_Worktrees/my-changli-v1.0.4`（目录名不是版本身份）
- Production Git Tag：`v1.0.5-release-20261001`
- Production Source Commit：`d56fc5efc8f1584309a7bc348035780e2c816c27`
- 正式 APK SHA-256：`936223F8A7D7E40DBB1929433190CE325073AE689E1BA36C4B90B7E56B538E0A`
- 包名：`com.mychangli.app`；大小：7163474 bytes
- 官网APK：`https://mychangliapp.cn/downloads/我的长理-V1.0.5-20261001.apk`
- GitHub Release：`https://github.com/HEltDeng/my-changli-android/releases/tag/v1.0.5-release-20261001`
- Website部署源码：`88d610779d4b821afe883ee610f89484fa78252f`；部署脚本修复归档：`d266af348be942847a5805dfbe3b905d225cb503`
- Backend/Admin部署源码：`30a1c5fb37e7803ecadb2fd43b5b05c34f61eb37`
- 普通更新：updateEnabled=true，forceUpdateEnabled=false；minimumSupportedVersionCode仍36，旧DTO有效minimum=1。
- 发布通知：`v1.0.5-release-20261001`，一条、系统通知允许、额外弹窗关闭；不代表所有用户已收到。
- 旧v1.0.5 Tag/草稿Release、旧8C8BB9EE...制品及撤回证据继续保留，不能作为正式来源。
- `02_Worktrees/my-changli-production-v1.0.4`继续只读保留为历史回滚参考，不是当前线上版本。
- 正式签名证书 SHA-256：`2C3B934931C26719C1A55FA707760A41E18BA15B00561F97691FE1D452F4BF3D`

## 保护范围

`V1.0.0`、`V1.0.1`、`V1.0.2`、`V1.0.3`、`V1.0.4` 的 Git 历史、Tag/Branch、正式 APK、Hash、签名、Release metadata、官网历史、发布报告和 rollback 证据均属于受保护正式历史。完整证据链见：

`03_Docs/local-cleanup-audit-2026-09-08/01_正式版本证据链审计.md`

## 重要警告

仓库当前 checkout 的 branch 不等于线上 Production。当前本地仍保留 `feature/sicau-v1-adaptation` 的 SICAU 开发状态；本轮不 checkout、不 reset、不覆盖它。任何 Codex 任务不得仅通过当前 checkout、`current`/`final`/`new` 目录名、文件时间或 versionName/versionCode 数字大小判断当前正式版本。

正式生产身份只能通过本文件与可验证的 Git Tag、Commit、APK Hash、签名和官网发布证据共同确认。

项目身份总索引见：`00_Project-项目说明/SOURCE_OF_TRUTH.md`。

本次未重新构建APK。构建基点c01915ca加已验收未提交改动，归档为d56fc5e；文档归档提交不改变已验收二进制。公网、GitHub附件、冻结本地制品一致。详细结果见05_Output/v1.0.5-release-20261001及Android docs/releases/v1.0.5-20261001。

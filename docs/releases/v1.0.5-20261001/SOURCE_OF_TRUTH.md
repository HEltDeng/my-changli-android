# SOURCE OF TRUTH

建立日期：2026-09-08；正式基线更新：2026-10-01

本文件是“我的长理”项目身份判断的最高优先级说明。它解决 Git 母库当前 checkout、Production、SICAU/V2、官网、Backend 和历史恢复源混在一起时的误判问题。

## 1. 当前正式 Android Production

- Repository checkout：`02_Worktrees/my-changli-v1.0.4`；branch：`codex/v1.0.5-dynamic-config-android`
- 当前线上正式版本：`V1.0.5`
- 历史只读Reference：`02_Worktrees/my-changli-production-v1.0.4`（仅旧版）
- Production Tag：`v1.0.5-release-20261001`
- Production Source Commit：`d56fc5efc8f1584309a7bc348035780e2c816c27`
- versionName：`1.0.5`
- versionCode：`37`
- applicationId：`com.mychangli.app`
- 正式 APK SHA-256：`936223F8A7D7E40DBB1929433190CE325073AE689E1BA36C4B90B7E56B538E0A`；7163474字节
- 正式签名证书 SHA-256：`2C3B934931C26719C1A55FA707760A41E18BA15B00561F97691FE1D452F4BF3D`

注意：母库的四川农业大学适配checkout不是线上Production，不得reset/checkout。查看当前正式源码应使用上述V1.0.5 Tag或现有候选checkout。用户已人工验收；未重建APK，正式Tag归档了构建时的未提交UI修改。旧v1.0.5 Tag属于撤回历史，不得移动。

## 2. 当前官网

- Repository：`01_Repositories/my-changli-website`
- Git remote：`https://github.com/HEltDeng/my-changli-website.git`
- V1.0.5页面部署commit：`88d610779d4b821afe883ee610f89484fa78252f`；部署脚本修复commit：`d266af348be942847a5805dfbe3b905d225cb503`
- 当前线上页面：
  - `https://mychangliapp.cn/`
  - `https://mychangliapp.cn/download.html`
  - `https://mychangliapp.cn/updates.html`
  - `https://mychangliapp.cn/releases/latest.json`
- 当前线上发布身份：V1.0.5 / versionCode 37；下载地址为/downloads/我的长理-V1.0.5-20261001.apk。latest.json不包含force/minimum策略。
- 当前 UI 图片优先看官网仓库实际引用的 `assets/`，不看 `qa/screenshots` 或历史交付 ZIP。

## 3. 当前 Backend

- Repository：`01_Repositories/CodexWorkspace-projects/my-changli-backend`
- Git remote：`https://github.com/HEltDeng/my-changli-backend.git`
- V1.0.5 production release commit：`30a1c5fb37e7803ecadb2fd43b5b05c34f61eb37`
- 当前本地 branch：`codex/v1.0.5-dynamic-config-backend`
- 生产当前指针37；普通更新开、强更关；动态配置为默认V1，未投放测试配置，业务flags保持原状。
- 说明：Backend 同时承载当前 V1 生产能力和体育预约/V2 兼容合同；不能只因包含 V2 兼容字段就将整个 Backend 视为废弃实验。

## 4. V2 / 实验身份

- 索引文件：`06_V2-Experiments/README.md`
- SICAU/体育预约 Android 工程：`01_Repositories/my-changli-android-current`
- SICAU branch：`feature/sicau-adaptation`
- SICAU HEAD：`d78817966d470b9577d9adacbce43ccb364a428c`
- 体育预约源文件：`SportsReservationActivity.kt`、`SportsReservationRepository.kt`
- 体育预约规则测试：`SportsReservationRulesTest.kt`
- 体育预约代表 APK：`05_Output/CodexWorkspace-builds/my-changli/account-sports-library/`
- 体育预约验收材料：`03_Docs/CodexWorkspace-logs/my-changli/account-sports-library/`

SICAU、体育预约、未来实验和代表 APK 都不是当前 V1 Production，不能作为当前“我的长理”UI 或正式母版判断依据。

## 5. 历史与恢复资产

- V1.0.0–V1.0.4 Git、APK、签名、官网 release history、release metadata、回滚证据和法务/软著材料：`KEEP_PROTECTED`。
- PreGitRecovery、迁移冲突快照、Website recovery 和历史 ZIP：按 `07_历史ZIP与备份冗余审计.md` 分类，未获确认前不删除。
- 当前 Production 参考只由本文件、CURRENT_PRODUCTION_BASELINE、Git Tag/Commit、Release metadata、正式 APK SHA-256 和官网 release history 共同确定。

## 6. Codex 默认判断优先级

以后任何任务判断“当前正式版本/当前 UI/当前源码”时，严格按以下顺序：

1. 本文件 `SOURCE_OF_TRUTH.md`；
2. `00_Project-项目说明/CURRENT_PRODUCTION_BASELINE.md`；
3. Production Reference 的 Git Tag / Commit；
4. 正式 Release metadata；
5. 正式 APK 的 applicationId、versionName、versionCode、SHA-256 和签名；
6. 官网 release history 与 `latest.json`。

禁止单独依据：

- 文件夹名称；
- `current`、`final`、`new`、`old`；
- 修改时间；
- versionName/versionCode 数字大小；
- 当前 checkout branch；
- APK 文件名；
- README 自称；
- 截图数量。

## 7. 开发与发布规则

历史Production Reference只读使用。未来开发以正式Tag `v1.0.5-release-20261001`为身份基线，默认复用现有正式checkout；只有Owner明确要求时才创建worktree。不得建立current2/final2/new2等副本。品牌长期规范见Android仓库AGENTS.md及docs/design/UI_GUIDELINES.md。

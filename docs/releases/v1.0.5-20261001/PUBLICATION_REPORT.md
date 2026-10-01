# V1.0.5 正式发布结果 — 2026-10-01

用户已完成人工验收；本轮执行正式发布及发布一致性核对。没有重新构建APK，没有再次控制手机。

## 正式状态

- Source Backup：Android、Backend/Admin、Website均已提交推送；各自保持原有仓库可见性。
- GitHub Release：正式、非draft、非prerelease，ID 400893202。
- Production Deployment：官网、Backend/Admin、版本化APK及普通更新已上线。
- Store Release：未执行，不把官网/GitHub发布称为商店上架。

## 发行物与源码

- Android源代码：d56fc5efc8f1584309a7bc348035780e2c816c27；tag v1.0.5-release-20261001。
- 构建基点：c01915ca1b1164c548f8322218baa41d7846506b，加当时未提交的已验收改动；d56fc5e归档这些输入。报告/基线后续提交不改变构建输入。
- Backend部署：30a1c5fb37e7803ecadb2fd43b5b05c34f61eb37。
- Website页面部署：88d610779d4b821afe883ee610f89484fa78252f；部署根目录限制修复：d266af348be942847a5805dfbe3b905d225cb503。
- APK：1.0.5/code37/com.mychangli.app；7163474 bytes。
- SHA-256：936223F8A7D7E40DBB1929433190CE325073AE689E1BA36C4B90B7E56B538E0A。
- 证书SHA-256：2C3B934931C26719C1A55FA707760A41E18BA15B00561F97691FE1D452F4BF3D。
- 下载：https://mychangliapp.cn/downloads/我的长理-V1.0.5-20261001.apk
- Release：https://github.com/HEltDeng/my-changli-android/releases/tag/v1.0.5-release-20261001
- GitHub为现有私有仓库；官网APK无需GitHub登录。GitHub APK和校验文件已通过认证下载校验；官网APK匿名下载验证字节一致。

## 发布一致性核对

- 本地冻结APK、官网公网APK和GitHub附件哈希/大小一致；没有替换已验收包。
- 官网首页、下载页、更新日志、统一site-config、latest.json正常地址回读为V1.0.5。首页HTML含6处当前V1.0.5标识，未遗留V1.0.0/1.0.4当前标识；历史日志保留原版本。
- latest.json不含force/minimum策略。两个静态JSON路径使用no-store/no-cache；API更新决策也返回no-store。
- 客户端code0/35/36：OPTIONAL；code37/38：NONE；全部force=false、旧DTO minimum=1。
- 策略updateEnabled=true、forceUpdateEnabled=false；minimumSupportedVersionCode维持36，未提高门槛。
- 后端58项本地单测通过；生产副本上现有迁移重复两次通过；部署过程测试、健康/ready及关键公共API通过。
- 生产service active、127.0.0.1:8787监听、SQLite integrity=ok、外键违规0，最终检查近期error级日志0。
- 动态配置默认revision1/configVersion1；业务remote_flags前后完全一致，未投放实验配置或新增开放功能。

## 通知与历史

固定通知ID v1.0.5-release-20261001，仅创建一次；notificationEnabled=true，popupEnabled=false，未清除任何用户已读/关闭状态。通知已发布可供客户端拉取，不声称所有用户已收到。

历史tag v1.0.5仍指向1bea4af3299fc3c719ea737ba558fbc52797f8b8；旧Release 383992859保持撤回draft/prerelease。旧code37/8C8BB9EE...原始记录在服务器备份及admin_audit_logs中完整保留，准备阶段DISABLED并记录V105_RELEASE_PREPARED_20261001，再由正式发布流程激活新制品。旧公开文件不覆盖；新URL有日期后缀。

## 回滚位置与方法

- 发布前完整官网、版本元数据、SQLite在线快照及迁移副本：/var/backups/mychangli/v1.0.5-release-20261001。
- 后端原代码/配置：/var/backups/mychangli/backend-release-20261001-111315-722975bcee38（原生产SHA d58b0ad7bab9745c29d2ec05f99c7055e094307c）。
- 官网部署备份：/var/backups/mychangli/website-20261001-111431。
- 激活前版本指针备份：/var/backups/mychangli/backend-version-20261001-111553。
- Nginx官网配置原件：v1.0.5-release-20261001/nginx-website-before.conf。

如需回滚：停用本次发布通知，定向恢复旧版本/策略指针和官网/服务代码。保留新APK和审计；不得整库恢复覆盖发布后的业务写入，不卸载/降级Owner手机。

## 长期规范及验收边界

AGENTS.md引用docs/design/UI_GUIDELINES.md；两张原始目标图保存在docs/design/references，当前参数由d56fc5e归档，无发布前组件重构。

此前动态配置同APK V1→V2→Rollback实机演示缺口仍如实保留，不将本次Owner品牌视觉验收追溯写成该演示PASS。正式发布授权以2026-10-01 Owner明确指令为准。

## 安全与未纳入内容

Secrets uploaded: NO；Production DB uploaded: NO；Private keys uploaded: NO；Keystore uploaded: NO。

官网已有qa/TEST_REPORT.md修改及4张旧截图删除保留未提交；本次不擅自处理。APK通过Release附件及官网分发，不将新APK塞入Git源码历史。未改四川农业大学适配分支、旧正式参考checkout或通用官网模板。

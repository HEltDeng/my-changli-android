# 我的长理 V1.0.7 正式发布报告

正式发布完成。版本 1.0.7 / 39，产品所有者最终验收通过。未重新构建、签名或改动已验收 APK。

## 发布时间与入口

- APK 独立发行路径首次公网验证：2026-10-04T14:27:04+08:00。
- GitHub 正式 Release：2026-10-04T06:36:00Z（UTC）。
- 后端普通更新激活：2026-10-04T14:40:32+08:00（Asia/Singapore）。官网日期为实际上线日 2026-10-04。
- 首页：https://mychangliapp.cn/
- 下载页：https://mychangliapp.cn/download.html
- 更新日志：https://mychangliapp.cn/updates.html
- 正式 APK：https://mychangliapp.cn/downloads/%E6%88%91%E7%9A%84%E9%95%BF%E7%90%86-V1.0.7.apk
- 客户端实际使用清单：https://mychangliapp.cn/releases/latest.json
- 正式更新接口：https://api.mychangliapp.cn/api/v1/public/version/android
- GitHub Release：https://github.com/HEltDeng/my-changli-android/releases/tag/v1.0.7-release-20261004。正式、非 Draft、非 Prerelease；保持仓库 private，官网不依赖 GitHub 登录。

## 发行身份

| 项目 | 核对结果 |
|---|---|
| 包名 / 名称 | com.mychangli.app / 我的长理 |
| 版本 | versionName=1.0.7，versionCode=39 |
| 构建 | release，debuggable=false |
| 生产 API | https://api.mychangliapp.cn/api/v1/ |
| 文件大小 | 7,175,674 bytes / 6.84 MiB |
| APK SHA-256 | D932142B5A07756F1FEEA557AD2C70B8B0A555BBD070CC945236B531645A9E1D |
| 签名证书 SHA-256 | 2C3B934931C26719C1A55FA707760A41E18BA15B00561F97691FE1D452F4BF3D |
| 官网下载校验 | HTTP 200，APK ZIP 格式，非 HTML；aapt2 / apksigner 核对通过 |
| 同一文件 | 本人验收原件、手机已安装 base.apk、公网下载、GitHub 附件，四者 SHA-256 一致 |

验收原件：`D:/Codex/01_App-应用开发/001_MyChangLi-我的长理/05_Output/v107-controls-webmenu-20261004/我的长理-V1.0.7-release.apk`。
正式本地归档：`D:/Codex/01_App-应用开发/001_MyChangLi-我的长理/02_Releases-正式发布/V1.0.7/我的长理-V1.0.7-release.apk`。
GitHub 附件为同字节 `MyChangli-v1.0.7.apk`，并附 SHA256SUMS.txt、RELEASE_NOTES.md、release-manifest.json 和本报告。

## 源码、分支与生产对应

| 组件 | 实际目录（项目根下） | 正式身份与远端情况 |
|---|---|---|
| Android | 01_Source-当前源码/01_Android | 验收分支 codex/v107-ui-identity：e3dd29fef08ed1367433442f7dedb1bdd25a8981，已推送；main 从此前正式主线正常快进至同一提交，正式源码整合提交也是 e3dd29f；发行说明提交 e420bb87e42dd6e587433895fa0eb500184013dc 已推送。本报告为后续纯文档提交，不改变 APK 源码身份。 |
| Website | 01_Source-当前源码/03_Website | main：b5cc46787434f906835ef8d63b9db1175ef756ea，已推送并部署；包含 0ce15d8768f53ae0c0cf22eadf6f4220a3a36463 的版本/日志/下载修改及发布公告一次展示修复。公网 release-deployment.json 对应该提交，49 个静态文件完成远端哈希核对。 |
| Backend/Admin | 01_Source-当前源码/02_Backend-Admin | 正式 main 与生产仍为 2e26a502ff4d20571ffe2f2a89e674fa361d154a，远端一致；生产 server.py 规范化内容 SHA 与该提交一致。本次无后端代码修改、无空提交、无后端重启。 |

Android Tag `v1.0.7-release-20261004` 精确指向验收提交 `e3dd29fef08ed1367433442f7dedb1bdd25a8981`，未移动既有标签。
Backend 工作检出仍为 codex/v107-candidate-sync-reminders / 71238c79d1d1aae81d549312976e470dc7a10610，已在远端保存但未部署。Website 对应候选政策分支 5c4af6138a87e9d33e7bcaaf8d1a6f62a45c51cc 保留，未合入生产。
当前正式基线更新为 V1.0.7 / 39；旧版身份、Tag、APK、更新历史均保留。

## 更新配置与通知

使用既有版本表 app_versions / app_release_versions 发布 39；39 发行记录仅一条。仅修改发布元数据，未执行业务数据库迁移或清理。
实时 app_update_policies 全行与发布前完全一致：update_enabled=1、force_update_enabled=0、minimum_supported_version_code=36、policy_revision=1。
版本 36、38 的真实公网请求得到 OPTIONAL；39、40 得到 NONE；mandatoryUpdate=false。旧客户端嵌套兼容字段保留，forceUpdate=false；兼容 minimum_version_code=1 是既有关闭强更时的中性响应，不代表修改了策略表中的 36。
清单继续只提供发行元数据，未加入强制更新状态。实际客户端使用 `/releases/latest.json`；根 `/latest.json` 原本不存在，未虚构新清单替代现行链路。

公告固定身份：`release-v1.0.7-39`。分别写入 App announcements 与 website_announcements；各仅一条，两个渠道独立。重复执行后无新增、原时间戳不变。
App 公告：enabled / service_visible / notification_enabled / popup_enabled 均启用；通过既有 public/bootstrap 前台拉取，由客户端既有持久化 ID 去重及本地通知/弹窗流程处理。未接入新的远程推送服务。
官网发布公告：按固定 release ID 保存展示状态，在自动展示前持久化；关闭后刷新没有再次自动弹出，仍可主动打开公告。相同 ID 的文字修改不会重新触发自动展示。
App 主动检查更新与公告已读状态独立。没有全体用户送达或升级证据，不宣称全体已收到或已升级。
未启用新自动采集生产规则；正式构建 AUTOMATIC_SYNC_POLICY_ENABLED=false 保持不变。

## P01–P12 验证

| 编号 | 状态与实际证据 |
|---|---|
| P01 | 已验证：真实浏览器首页标题、主介绍、版本标识和页脚显示 V1.0.7。 |
| P02 | 已验证：真实下载页日期 2026-10-04、6.84 MiB、完整 SHA 与正式链接正确。 |
| P03 | 已验证：公网 APK 下载哈希、字节数、包名、版本与正式签名全部匹配。 |
| P04 | 已验证：生产更新接口返回 1.0.7 / 39 和正确下载信息。 |
| P05 | 已验证：官网实际清单、后端、网页配置与 APK 一致，兼容字段保留。 |
| P06 | 已验证（旧版请求）：36、38 返回 OPTIONAL，未强制。未进行真实 38→39 手机覆盖升级。 |
| P07 | 已真机验证：已安装 APK 哈希与验收原件相同，1.0.7 点击“检查更新”显示“当前已是最新版本”。未卸载、清数据或降级。 |
| P08 | 已验证：更新日志及两个公告渠道上线；公告脚本重复执行无重复记录；官网关闭/刷新去重通过。App 公告去重沿用验收源码，未声称所有设备通知实际送达。 |
| P09 | 已验证：main/验收分支已推送；Tag 指向准确；正式 Release 与附件完整，附件下载哈希一致。 |
| P10 | 已验证：三仓实际分支与远端提交已记录；后台生产沿用 2e26a50，无伪造新提交。 |
| P11 | 已验证：策略表未变化，nginx/backend active、nginx 配置检查及公网 health 正常；发布期间后台日志 ERROR/CRITICAL/Traceback 计数 0。其他项目未改动；未做其他项目全业务回归。 |
| P12 | 已验证：不带临时版本查询参数的公网首页、下载页、日志、清单及配置再次读取一致；JSON 既有 no-store/no-cache 头有效，新前端资源使用内容哈希。未清空全站缓存。 |

网站自动化验证：12 页面、13 功能与配置检查通过，Vite 正式构建成功。USB 真机在最终核对后保留“当前已是最新版本”结果供本人查看。
先前报告中未完整实测的时序提醒、完整系统授权组合、多窗口及进程恢复边界保持，不因产品验收而改写为全部 PASS。

## 备份、故障与恢复

- 网站仅备份实际变更的原文件：`/var/backups/mychangli/v107-release-site-0ce15d8768`、`/var/backups/mychangli/v107-release-site-b5cc467874`。旧 APK 与旧哈希资源保留。
- 版本配置仅备份相关版本/策略行：`/var/lib/mychangli/release-backups/v107-20261004/version-before.json`，服务账号私有权限；未复制完整生产数据库、未导出真实用户资料。
- 初次辅助命令发现非登录 shell 不包含 sbin 路径，改用 /usr/sbin/nginx、/usr/sbin/runuser 后通过。失败发生在版本写入前，未造成业务数据修改。
- 根 latest.json 预检查返回既有 404，核对客户端真实地址后使用原 releases/latest.json；未创建无人使用的替代清单。
- 无错误发行文件激活，无数据库回滚，无版本降级，无强更扩大。无剩余发布阻塞。

## GitHub 归档与安全

Android/Website 源码推送及远端 HEAD 核对完成。Backend 正式提交与远端、生产一致，无需无意义提交或部署。
Website 既有 qa/TEST_REPORT.md 修改、四张 QA 截图删除及未跟踪 V1.0.5 文件保留，未混入本次提交。
Secrets uploaded: NO；Production DB uploaded: NO；Private keys uploaded: NO；Keystore uploaded: NO。
原始手机截图/UI dump 仅在本地工作输出，不上传公开发行附件。发布报告不包含个人账号、令牌或敏感日志。

最终状态：源码同步完成；GitHub 正式归档完成；官网正式发布完成；生产普通更新生效；发布公告按既有渠道启用；未进行应用商店发布。

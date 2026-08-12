# 我的长理 V1 初代首发版功能范围矩阵

- 审计日期：2026-08-12
- 完整版基线：`9afec16ef5f8095a4c949f08ac1426bf2cd916d1`
- V1 分支：`release/v1-initial`
- 产品身份：个人开发者独立开发的非官方校园生活辅助工具
- 固定声明：本应用由个人开发者独立开发，是面向校园生活场景的非官方辅助工具，与长沙理工大学不存在官方隶属或授权关系。

## 判定规则

- `OPEN`：V1 保留用户入口和实现；依赖自有 Backend 的能力在正式 HTTPS endpoint 未注入时安全失败。
- `CLOSED`：V1 用户不可进入或触发真实业务；远端配置不能覆盖本地硬关闭。
- `REMOVE_FROM_V1_BINARY`：完整历史由 main 与保护 tag 保存，相应实现或二进制不进入 V1 APK/AAB。
- `FUTURE`：仅保留清晰 DTO、状态模型或抽象，等待授权、合作、商户与基础设施条件。

## OPEN

| 能力 | V1 边界 | 代码依据 |
| --- | --- | --- |
| 隐私首启门禁、非官方声明、隐私/协议/SDK/个人信息页 | 首次同意前不执行非必要联网 | `PrivacyConsentManager`、`MainActivity`、`ReleaseInformationV1Activity` |
| Guest-first 首页、服务页、设置页 | 本地能力和公共页面不要求全局登录；云写操作再要求自有账号 | `MainScreen`、`AccountAccess` |
| 我的长理自有账号 | 手机号账号与学校身份分离；V1 账号页不收集学校凭据或预约学号资料 | `CampusToolsSession`、`AccountCenterActivity`、auth contract tests |
| 公告、服务更新、Feature Flags、版本检查与更新提示 | 仅走自有 Backend；默认 `https://campus-tools.invalid/api/v1/` 在 DNS 前 fail closed | `RemoteConfigurationStore`、`ServiceEndpointPolicy`、`AppUpdateChecker` |
| 用户反馈 | 自有账号写入自有 Backend；不提供体育预约反馈类别 | `FeedbackActivity`、`POST /feedback` |
| 失物招领 | 自有账号、用户内容、举报和后台治理；不依赖学校身份 | `LostFoundRepository`、`LostFoundActivity`、accepted Backend contract |
| 自有资料库 | 公共浏览按 Backend flag；收藏/投诉等写操作按账号策略 | `ResourceLibraryRepository`、`ResourceLibraryActivity` |
| 本地手动课表 | Room 与手动添加；不登录教务、不拉学校课表 | `CoursesDataBase`、`TimeTableActivity`、`AddCourseActivity` |
| 课表组件与本地课程提醒 | 只读本地课表；通知/精确闹钟由用户主动启用并可降级 | `TimeTableAppWidget`、`NextCourseAppWidget`、`CourseReminderScheduler` |
| 本地记账、日期/周次工具 | 不依赖学校授权接口 | ledger、`CourseScheduleEngine` |
| 四六级、普通话及资料库公开网页 | 明确为外部网页，不代表学校合作，不提交学校统一认证凭据 | `CetActivity`、`MandeActivity`、`ResourceCatalog` |
| 设置、关于、账号与数据清理、默认本地主题 | 保留自有账号和本地数据控制，无学校绑定入口 | profile/settings/privacy |

## CLOSED

| 能力 | V1 关闭方式 | 原因 |
| --- | --- | --- |
| 学校账号绑定与学校凭据输入 | Activity、布局、导航、账号页入口和会话实现从 V1 分支移除；只保留升级清理旧数据 | 学校凭据与未授权依赖边界 |
| 学校统一认证、教务、选课、MOOC | 不编译实现，不后台恢复，不直连学校接口 | 来源/授权和 HTTP/会话风险未闭合 |
| 学校课表同步、成绩、考试、空教室、MOOC 作业 | Route、Manifest、UI、Repository/ViewModel 实现移除；本地课表独立保留 | 同上 |
| 学校网络校历 | 网络 Repository、缓存预取和 UI 入口移除 | 不依赖学校接口的本地日期工具已足够首发 |
| 宿舍电量/校园卡与电量小组件 | Receiver、Repository、UI、资源和入口移除 | 依赖不明学校二进制 |
| 校园地图与定位 | Route、Activity、AMap 实现、metadata、location permission 和网络例外移除 | 地图二进制来源/授权未闭合 |
| 体育真实预约、库存、履约 | 本地 policy 硬关闭；无页面、路由、服务分区或账号资料表单 | 无学校合作与真实履约 |
| 真实支付、模拟支付按钮、退款 UI | 无付款页面和支付 SDK；仅允许未来 DTO/状态模型保留 | 首发不接真实支付 |
| 旧社区/Buddy/旧新闻 API | 网络实现、页面、路由与导航移除；不属于 accepted Backend contract | 合同边界不成立 |
| 远程皮肤下载 | 下载 UI/API 与导航移除；默认本地主题保留 | 生产 tools endpoint 未建立 |
| Deep Link/Intent/Shortcut/Widget/通知 action 旁路 | CLOSED 目标没有 Route/Manifest/deep link；只有 Launcher Activity 导出 | 防止仅隐藏按钮 |

## REMOVE_FROM_V1_BINARY

| 项目 | 处置 |
| --- | --- |
| `CSUSTDataGet-1.1.4-no-sensitive-logs.aar` | Gradle 和 catalog 声明删除；文件缺失是受测前提；preBuild 遇到文件出现立即失败 |
| `Lite3DMap.jar` | Gradle 声明删除；文件缺失是受测前提；preBuild 遇到文件出现立即失败 |
| `com.dcelysia.csust_spider.*` / `com.example.csustdataget.*` | 调用实现从 V1 分支删除，源码边界测试禁止重新出现 |
| `com.amap.*` | 调用、Activity、隐私初始化、metadata、权限和网络例外从 V1 分支删除 |
| 学校凭据输入/持久化/静默恢复 | 不进入 V1 classes/resources；遗留数据只能清理，不能读取或发送 |
| 体育预约/支付 UI 与 Repository | 不进入 V1 classes；Backend DTO/未来状态模型不构成用户入口 |
| 迁移前 baseline profile | 旧包名 profile 从 V1 分支移除；需以后按正式包名重新采集 |

## FUTURE

| 能力 | 恢复条件 |
| --- | --- |
| 教务/MOOC/校园卡 Adapter | 合作或合法授权、HTTPS、隐私与凭据链路专项审计完成 |
| 校园地图 | SDK 来源、版本、许可证、隐私字段和再分发权均可证明，或合法自研替代 |
| 体育预约 | 场馆/库存/履约合作和规则真实成立 |
| 支付 Provider | 适格主体、商户合同、密钥托管、退款/对账和专项合规完成 |
| 正式覆盖更新 | 正式域名/TLS、同一 signing identity、size/hash/applicationId/签名校验完成 |
| Production endpoint | 域名、备案、TLS 和部署完成后从仓库外注入 |

## 结论

失物招领为 `OPEN`，体育真实预约、真实支付、学校正式接口、学校账号绑定和校园地图为本地硬 `CLOSED`。远端 Feature Flag 只能进一步关闭 OPEN 能力，不能打开这些首发禁区。

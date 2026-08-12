# 我的长理 V1 初代首发版功能范围矩阵

- 审计日期：2026-08-12
- Android 完整版基线：`9afec16ef5f8095a4c949f08ac1426bf2cd916d1`
- V1 分支：`release/v1-initial`
- 产品身份：个人开发者独立开发的非官方校园生活辅助工具
- 固定声明：本应用由个人开发者独立开发，是面向校园生活场景的非官方辅助工具，与长沙理工大学不存在官方隶属或授权关系。

## 判定规则

- `OPEN`：V1 二进制保留入口和实现；需要自有 Backend 的能力仍必须通过正式 HTTPS endpoint 注入，默认 `.invalid` 时安全关闭。
- `CLOSED`：V1 用户无法进入或触发真实业务；远端 Feature Flag 不得覆盖本地 V1 硬关闭策略。
- `REMOVE_FROM_V1_BINARY`：源码历史可以由完整 main/tag 保存，但相应实现或二进制不得进入 V1 APK/AAB。
- `FUTURE`：仅保留清晰的数据模型、合同或抽象，等待授权、合作、正式商户或生产基础设施。

## OPEN

| 能力 | V1 边界 | 真实代码依据 |
| --- | --- | --- |
| 隐私首启门禁、非官方声明、隐私/协议/SDK/个人信息页面 | 首次同意前不做非必要联网和敏感权限工作 | `PrivacyConsentManager`、`MainActivity`、`ReleaseInformationActivity` |
| 无全局登录墙的首页、服务页、设置页 | 游客可进入本地页面；云写操作再要求“我的长理”账号 | `MainActivity` 直接渲染 `MainScreen`；`AccountAccess` 只保护具体云操作 |
| “我的长理”自有账号 | 手机号账号与学校身份完全分离；学校密码等不得进入请求模型 | `CampusToolsSession`、Backend auth contract、`CampusToolsAuthContractTest` |
| Public bootstrap、公告、服务更新、Feature Flags | 仅访问自有 Backend；未注入正式域名时 `.invalid` 在 DNS 前 fail closed | `RemoteConfigurationStore`、`ServiceEndpointPolicy` |
| 版本检查与更新提示 | 仅下发版本元数据和提示；V1 不执行未签名/未校验 APK 的真实安装 | `AppUpdateChecker`、`AppUpdatePrompt` |
| 用户反馈 | 需“我的长理”账号；内容进入自有 Backend | `FeedbackActivity`、`POST /feedback` |
| 失物招领 | 保留并 OPEN；需“我的长理”账号，支持发布、修改、删除、认领、举报，Backend/Admin 可治理；不使用学校身份 | `LostFoundRepository`、`LostFoundActivity`、Backend contract |
| 自有资料库 | 浏览按 Backend guest flag 或账号策略；收藏/投诉等写操作需账号 | `ResourceLibraryRepository`、`ResourceLibraryActivity`、Backend contract |
| 本地手动课表 | 仅设备本地 Room 数据和用户手动添加；不登录教务、不拉取学校课表 | `CoursesDataBase`、`TimeTableActivity`、`AddCourseActivity` |
| 课表桌面组件与本地课程提醒 | 只读取本地课表；通知/精确闹钟由用户主动启用并可降级 | `TimeTableAppWidget`、`NextCourseAppWidget`、`CourseReminderScheduler` |
| 本地记账、校历缓存和日期/周次工具 | 不依赖学校授权接口；网络校历预取从 V1 移除，仅保留内置/本地数据 | ledger、calendar、`CourseScheduleEngine` |
| 四六级、普通话等公开外部查询入口 | 明确为外部页面，不代表学校合作；不提交学校统一认证凭据 | `CetActivity`、`MandeActivity` |
| 设置、关于、账号与数据清理、默认本地主题 | 保留自有账号和本地数据控制；不展示学校绑定入口 | profile/settings/privacy 代码 |

## CLOSED

| 能力 | V1 关闭方式 | 原因 |
| --- | --- | --- |
| 学校账号绑定、学号/教务密码输入 | 移除入口、Navigation、Manifest Activity 和编译实现；升级时只允许清理遗留缓存 | 学校凭据与未授权依赖边界 |
| 学校统一认证、教务、选课、MOOC 会话 | 不编译实现，不允许后台静默恢复或直连学校接口 | `CSUSTDataGet` 来源/授权未闭合，且含 HTTP/学校会话风险 |
| 学校课表在线同步 | 本地课表仍可用；刷新只读本地数据 | 同上 |
| 成绩查询、考试安排、空教室、MOOC 作业 | 移除所有可达入口和实现 | 同上 |
| 宿舍电量/校园卡查询及电量小组件 | 移除 Receiver、Repository、UI 入口和实现 | 直接依赖 `CampusCardHelper` |
| 校园地图与定位 | 移除 Manifest key、定位权限、地图入口和 AMap 实现 | `Lite3DMap.jar` 来源/授权未闭合 |
| 体育真实预约、真实库存、取消/履约 | 本地 V1 硬关闭，不能被远端 flag 打开；移除入口与 Manifest Activity | 无学校合作/履约；现有 Backend 为模拟业务 |
| 真实支付、模拟支付按钮、真实退款 | 不存在可达付款页面或支付 SDK；仅保留 DTO 状态字段 | 首发不接真实支付，不应形成付款或预约结果 |
| 旧校园社区/Buddy/旧社区 API | `.invalid` fail closed，并从 V1 导航移除 | 不属于已 ACCEPT Production Backend contract |
| 远程皮肤下载 | 不进入 V1 对外功能，仅保留本地默认主题 | 依赖未配置的 tools endpoint |
| 通过 Deep Link/Intent/Shortcut 进入 CLOSED 能力 | 所有 CLOSED Activity `exported=false` 仍不足；V1 还将删除 Manifest 声明和 Route 目标 | 防止按钮隐藏但旁路可达 |

## REMOVE_FROM_V1_BINARY

| 项目 | 二进制处置 |
| --- | --- |
| `CSUSTDataGet-1.1.4-no-sensitive-logs.aar` | 删除 Gradle active dependency；V1 worktree 在文件缺失条件下构建；不得复制、下载或伪造替代二进制 |
| `Lite3DMap.jar` | 删除 Gradle active dependency、Manifest metadata、定位权限和地图实现 |
| `com.dcelysia.csust_spider.*` / `com.example.csustdataget.*` 实现调用 | 从 V1 source set 与最终 classes 中排除或改为自有本地实现边界 |
| `com.amap.*` 实现调用 | 从 V1 source set 与最终 classes 中移除 |
| 学校凭据输入、持久化和静默会话实现 | 不进入 V1 classes；只保留无凭据读取能力的遗留数据清理 |
| 体育预约/模拟支付 UI 与 Repository 实现 | 不进入 V1 classes；只保留 Backend DTO/未来抽象所需的数据结构 |

## FUTURE

| 能力 | 恢复条件 |
| --- | --- |
| 学校教务/MOOC/校园卡 Adapter | 获得明确合作或合法授权、HTTPS/隐私审计闭环，并独立评审学校凭据链路 |
| 校园地图 | 使用来源、版本、许可证、隐私字段和分发权均可证明的 SDK 或合法自研替代 |
| 体育预约 | 学校场馆/库存/履约合作、规则和用户责任边界正式成立 |
| 支付 Provider | 企业/商户主体、支付合同、密钥托管、退款/对账与专项合规完成 |
| 正式 APK 下载与覆盖更新 | 正式域名/TLS、同一 signing identity、size/SHA-256/applicationId/签名校验完成 |
| Production endpoint | 域名、备案、TLS、部署和 Gate A/B/C 完成后从仓库外注入 |

## 入口冻结检查

V1 收敛必须同时检查首页、服务页、设置页、Route、Manifest、Deep Link、Intent、Shortcut、Widget、通知 action、Receiver、后台任务和远端 Feature Flag。当前基线只有 Launcher Activity 导出，没有业务 Deep Link；这不替代移除 CLOSED Activity 声明和 Route 目标。

## 失物招领结论

`OPEN`。它是“我的长理”自有用户内容服务：使用自有 Backend、自有账号、用户发布、认领/举报、后台管理和内容治理，不依赖学校官方身份或两个授权不明二进制。Backend 合同当前要求 Bearer，因此 V1 可浏览/操作前需要“我的长理”账号；这不是学校账号绑定。

## 本轮不可由远端覆盖的硬关闭

`sports_reservation`、school official APIs、school credential binding、real payment 和 campus map 必须由本地 V1 policy 固定为 CLOSED。Backend flag 只能进一步关闭 OPEN 能力，不能打开这些首发禁区。

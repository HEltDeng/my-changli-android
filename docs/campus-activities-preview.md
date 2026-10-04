# 校园活动预览 Android

母版：当前正式工程 e306adad1484078c21073d9db1c2b8d416953d14；本地开发分支 codex/campus-activities-preview。

- FeatureFragment 在现行服务页的「学习」分组加入校园活动。现行页面分为学习、生活两组，未为本轮重新合并或改变一级导航。
- OverviewScreen 增加 RecentActivities，最多三条可报名活动，与活动中心使用同一 Repository/API。
- CampusActivitiesActivity、ActivitiesViewModel 实现发现/我的活动、详情、身份确认、电话确认、报名成功反馈、取消二次确认、空态、错误重试和服务器状态按钮。
- ActivityRepository 调用现行学校 SSO，姓名/学号/学院只读确认；学校组织层级统一为最后一级学院。不上传学校密码或 Cookie。
- AndroidManifest 注册 mychangli://activity/{public_id} 路由。

正式默认 API 不变。仅显式 Gradle 参数 `-PactivityPreview=true` 的 release 将活动 API 指向 http://127.0.0.1:8794/api/v1/，并加载 src/activityPreview 的 loopback 网络配置。其余业务 API、正式更新通道不变。预览活动的安装身份会话与正式平台会话分开，不能覆盖现行登录 Token。预览版本名 1.0.7-activities-preview.1，versionCode 保持 39，包名 com.mychangli.app。

使用既有 DPAPI 正式签名脚本构建 release（ORG_GRADLE_PROJECT_activityPreview=true）。签名必须校验为 2C3B934931C26719C1A55FA707760A41E18BA15B00561F97691FE1D452F4BF3D。仅 adb install -r 覆盖安装，不卸载、不清数据。活动预览依赖本机服务及 adb reverse tcp:8794 tcp:8794。

最终自动化：132 项 Android 单元测试通过。V1SourceBoundaryTest 更新活动 Deep Link 白名单和预览版本表达式；两个既有过期文案断言与当前正式母版文案对齐，没有改变对应产品文案。

正式发布、latest.json、最低版本、强制更新、官网、GitHub Release/Tag 均未修改。用户 UI/业务验收仍待进行。完整数据库、权限、接口和回滚说明位于 Backend 仓库 docs/campus-activities-preview.md。

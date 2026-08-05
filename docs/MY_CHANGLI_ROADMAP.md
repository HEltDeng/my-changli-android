# “我的长理”改造与发布说明

## 2026-07 产品方向收敛

“我的长理”后续只发展高频校园工具。校园频道、普通帖子、评论、点赞、搭子社、校园活动内容流、校巴和广告扩张已取消。当前保留教务工具、提醒与桌面组件方向、体育场馆预约、独立失物招领，以及后续必要的版本检测、公告、远程配置和用户反馈。

下文社区内容仅作为历史实现与兼容风险记录，不再是待开发路线。既有本地失物数据会通过独立失物模块继续读取，不得因社区下线而删除。

## 已完成的第一阶段

- 品牌名称改为“我的长理”。
- 根据长沙理工大学官网公开校徽图制作 Android legacy 与 adaptive launcher icon。
- 主色切换为学校蓝色系，服务页旧星球头图替换为校徽品牌头图。
- 底部“情报站”改为“校园”，并把仓库中未接入的帖子能力挂回主导航。
- 校园频道增加首次加载、搜索、热门排序、话题录入与展示、错误页、发布刷新、图片预览和评论跳转。
- 资料库从占位提示升级为可搜索、可分类的可信资源目录。
- 首页、校园频道、资料库预留 SDK 无关的广告位，默认完全关闭。
- 停用上游 Bugly 项目和上游 APK 自动更新，移除电话权限并关闭应用数据备份。
- 移除上游用户域名/IP 的启动预连接，以及未使用的 Bugly、腾讯 HTTPDNS 依赖；工具/皮肤服务默认指向 `.invalid`。
- 应用自有的学号密码缓存改用 Android Keystore AES-GCM 加密，并通过 Room 10→11 迁移删除课程表中的重复密码列；升级时会清理依赖库遗留的明文密码与 Cookie。
- 高德定位改为在校园地图页取得明确同意后才启用；FileProvider 仅授权拍照、发帖图片和更新缓存子目录。
- Release 网络日志关闭，Debug 仅保留 BASIC 且脱敏认证与 Cookie 请求头。

## 当前版本仅用于开发预览

App 当前使用基于上游 `CSUSTDataGet 1.1.4` 精确提交的本地安全构建：依赖内部的 Android 日志、HTTP BODY 日志和 `printStackTrace()` 已关闭，端点、请求参数和认证流程未改；应用仍会在进程启动时清理旧存储并以只读方式打开依赖的凭据/Cookie MMKV，使活动会话只保留在内存。来源提交、补丁边界和 AAR 校验值见 `docs/csustdataget-local-security-build.md`。

这仍是开发预览措施。精确上游标签中未发现独立 LICENSE/NOTICE，根项目 MIT 许可不能自动覆盖这个单独依赖；公开分发修改后的 AAR 前，必须向上游确认许可或取得授权，并完成正式安全与会话回归审计。

## 历史社区实现（已停止）

上游社区账号体系已经从当前 App 主流程移除，旧社区服务也不可依赖。默认构建使用 `.invalid` 域名，因此不会把新应用的帖子或用户数据发往原项目服务器。

开发环境可在 `local.properties` 中配置自有服务（所有 Retrofit Base URL 必须以 `/` 结尾）：

```properties
community.apiBaseUrl=https://api.example.com/app/
community.commentsBaseUrl=https://api.example.com/app/fresh_news/
community.userBaseUrl=https://api.example.com/users/
```

也可以使用环境变量：

- `MY_CHANGLI_COMMUNITY_API_BASE_URL`
- `MY_CHANGLI_COMMUNITY_COMMENTS_BASE_URL`
- `MY_CHANGLI_COMMUNITY_USER_BASE_URL`

可选工具/皮肤服务使用 `tools.apiBaseUrl` 或环境变量 `MY_CHANGLI_TOOLS_API_BASE_URL`；未配置时保持关闭。

以下内容不再列入产品计划，仅保留为历史安全审计记录：

1. 服务端签发的匿名设备账号或自有登录账号；校园身份认证只能使用学校授权方式，不应把教务密码上传到自建服务。
2. 帖子、评论、点赞、收藏、图片上传与分页信息流。
3. 服务端从 token 推导 userId 和地区；客户端不应提交可信身份或 IP。
4. 举报、作者删除、管理员隐藏、封禁、限流、敏感内容审核和申诉。
5. 信息流直接返回作者资料和当前用户互动状态，避免逐帖请求用户资料。
6. 明确的 `channelSlug`，用于“求助、二手、失物、组队”等频道；现有自由文本 tags 只适合作为过渡。

## 资料库扩展

集中目录位于：

`app/src/main/java/com/creamaker/changli_planet_app/feature/resources/ResourceCatalog.kt`

当前链接采用 HTTPS 与主机白名单。后续若改为服务端目录，应继续校验 URL、来源、版权、文件哈希和下架状态，并把资源上传、审核、举报与版本管理放在服务端。

## 广告接入

广告入口位于 `core/ads`，目前使用 No-op renderer。开发占位可通过下列任一配置开启：

```properties
ads.enabled=true
```

或环境变量 `ADS_ENABLED=true`。

接入真实 SDK 前必须完成隐私政策、第三方 SDK 清单、用户同意/撤回、未成年人保护、频控与远程开关。广告卡必须明确标注“广告”或“赞助”，不能伪装为普通帖子，也不能影响课表、成绩等核心功能。

## 独立发布清单

- [x] 将正式 `applicationId` 固定为 `com.denghe.mychangli`，Debug 使用独立的 `.debug` 后缀，并同步更新 Baseline Profile 目标包名。
- [x] 建立仓库外离线保管的独立发布签名方案；正式发布前仍需在高德控制台登记新包名与正式签名，并建立应用商店记录。
- [ ] 部署自有社区、资源和更新服务，盘点并逐步替换其余上游域名。
- [ ] 向 `CSUSTDataGet` 上游确认修改版 AAR 的再分发许可或取得授权，并验证正式构建中的教务密码、Cookie 与会话不会进入日志或明文文件。
- [ ] 将 `planet.apiBaseUrl.debug/release` 换为自有配置/地图服务，或在隐私政策中明确仍依赖的上游服务。
- [ ] 提供自己的反馈渠道、官网、隐私政策、用户协议和账号注销入口。
- [ ] 取得长沙理工大学校名/校徽用于第三方应用及广告场景的授权；未获官方认可时持续显著标注“非官方”。
- [ ] 保留根目录 MIT License 中 `Copyright (c) 2026 CreaMakers` 与完整许可文本。
- [ ] 为社区增加举报、审核、封禁、删除和内容安全后台。
- [ ] 使用自有统计/崩溃平台并在取得同意后初始化；不得复用上游项目账号。
- [ ] 运行 `:app:assembleDebug`、lint、关键页面真机测试和发布签名验证。

官方校徽说明与下载来源：<https://www.csust.edu.cn/xywh/jswh/xhxm.htm>

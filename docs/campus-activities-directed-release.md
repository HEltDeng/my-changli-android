# 1.0.8 活动联网定向验收（2026-10-11）

Owner 本轮确认上次活动预览通过，要求 Backend/Admin 接入正式网络，正式签名 Release 仅安装个人手机，不官网分发、不推送现有用户。

版本 1.0.8 / 40；活动与其他校园服务均使用 https://api.mychangliapp.cn/api/v1/，不启用 activityPreview 或 candidate。

当前依赖仍为无内置认证值的 external-auth AAR。公开发行的 blockUnauditedCampusCardDistribution 门禁保留。新增显式 `privateDeviceAcceptance=true` 仅用于 Owner 明确要求的个人设备本机 Release 验收；默认 Release、candidate、benchmark 仍禁止打包。私密配置来自已忽略的本机文件/环境，不写源码/AAR。

该本机 APK 含本机校园卡接口配置，与本地 Debug 产物一样不得上传 GitHub、CI、官网或公共下载。这不是认证值归属确认或公开分发授权，OWNER_UNKNOWN 与正式分发审查仍保留。要公开分发必须另行完成归属、安全与分发审查，不能复用本次定向包。

构建必须禁用 configuration cache 与 build cache。本轮使用既有 DPAPI 签名脚本，子进程设置 ORG_GRADLE_PROJECT_privateDeviceAcceptance=true、ORG_GRADLE_PROJECT_activityPreview=false、GRADLE_OPTS=-Dorg.gradle.configuration-cache=false -Dorg.gradle.caching=false。不能在命令参数或文档填写任何真实认证值。

Backend/Admin 部署 b8fab58，沿用现有生产数据库和账号，迁移 20261004_campus_activities_v1。测试活动/账号不导入生产。完整部署、备份、证书、APK 哈希与真机验证记录保存在项目 Output/campus-activities-directed-release-20261011。

官网、latest.json、最低版本、更新策略、GitHub Release/Tag 本轮不变。公开当前版本记录不改写为 1.0.8。

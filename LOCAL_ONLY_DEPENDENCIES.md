# 本地专用依赖说明（不上传 GitHub）

以下文件为本地构建所需依赖或授权未确认资源，**不复制到 GitHub 导出目录**，仍在原本地项目中保留。正式构建前需要将其放置到对应位置：

| 本地文件名 | 原位置（仅说明类别） | 用途 | 授权状态 |
| --- | --- | --- | --- |
| CSUSTDataGet-1.1.4-no-sensitive-logs.aar | app/libs | 教务数据获取本地 AAR | 上游未发现独立 LICENSE/NOTICE，再分发授权未确认；仅允许本地开发/私有测试 |
| csust_spider-release.aar | app/libs | 上游解析库（build.gradle 中已注释引用） | 未确认 |
| Lite3DMap.jar | app/libs | 地图/3D 相关 | 未确认 |
| develocity-gradle-plugin-3.17.6.jar | app/libs | Gradle 构建插件 | 随构建生态分发，建议从官方渠道获取 |
| skin_dark.apk | app/src/main/assets | 本地皮肤资源 | 未确认来源，未复制 |
| csust_emblem.png | app/src/main/res/drawable-nodpi | 启动画面校徽 | 学校视觉，未获授权前不得作为正式产品图标或上传 |

如导出目录因缺少上述文件而无法独立构建，状态为“安全导出成功，但尚不能独立构建”。请不要通过擅自复制授权不明文件来强行构建。

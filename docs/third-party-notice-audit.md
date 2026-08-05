# 第三方组件与许可审计

审计日期：2026-07-22。本文记录当前私有开发预览的许可边界，不替代法律意见，也不是完整的依赖许可证清单。

## 根项目许可

仓库根目录 `LICENSE` 为 MIT License，并保留 `Copyright (c) 2026 CreaMakers`。该许可只覆盖其有权授权的根项目代码，不能自动覆盖独立仓库、二进制依赖、学校名称与校徽或其他第三方素材。当前根目录没有独立 `NOTICE` 文件；不得因此推定不存在其他依赖义务。

## CSUSTDataGet 本地 AAR

- 活动文件：`app/libs/CSUSTDataGet-1.1.4-no-sensitive-logs.aar`
- SHA-256：`9B03523FA04E9C8B3F5CAAAD49DC282B17EB2AC82E69A49E89764B21ABF71C29`
- 项目记录所指上游：<https://github.com/CreaMakers/CSUSTDataGet>
- 项目记录所指版本：标签 `1.1.4`，对应提交 `7c2dfa9384ab2117ec917ff52b7d3b6b3b2f2894`

本地 AAR 内只有 Android 资源/Manifest、`classes.jar` 和 AAR 元数据，未包含 LICENSE、LICENCE、COPYING、NOTICE、POM、来源或可独立证明版本的元数据。上游标签的文件树中也未发现独立 LICENSE、LICENCE、COPYING 或 NOTICE。公开可见的源码仓库本身不等于授予修改或再分发权；文件名中的版本与来源依赖现有项目构建记录，不能只靠二进制自行证明。

该 AAR 是为关闭依赖内部敏感日志而制作的本地安全构建。相关修改边界见 `docs/csustdataget-local-security-build.md`；这份安全说明不是再分发授权，也不证明二进制可由当前仓库完整复现。

结论：在上游补充明确许可证或项目所有者取得书面再分发授权之前，包含该 AAR 的构建只允许用于本机开发、Debug 构建与私有测试。公开 APK、应用商店发布、对外分发或上传包含该 AAR 的代码包均处于阻塞状态。

## 其他第三方依赖

Gradle/Maven 依赖分别受其上游许可证约束，正式发布前仍需生成并人工复核完整清单。仓库还包含 `Lite3DMap.jar`，以及当前注释停用的 `csust_spider-release.aar`；本次未完成这两个二进制的独立再分发授权审计，不能默认继承根项目 MIT License。

普通用户个人中心当前不显示“开源和许可”入口，但根项目 `LICENSE`、本审计文档与现有版权声明仍保留在仓库中。正式面向用户分发前，应根据每项许可证要求恢复合规的应用内或随包展示路径。

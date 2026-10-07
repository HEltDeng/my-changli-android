# 我的长理

> 2026-10-07 开发恢复：当前分支使用不含真实认证值的 CSUSTDataGet AAR。Mac clone 后须创建 ignored 私密配置，再进行本地 Debug 构建。完整步骤及禁止分发边界见 [认证配置与 Mac 恢复](docs/csustdataget-debug-auth-recovery.md)。

“我的长理”是一款面向长沙理工大学师生的非官方 Android 校园工具，优先提供稳定、快速、低维护成本的课表、成绩、考试、体育预约与失物招领能力。

本项目基于 [CreaMakers/changli-planet-app](https://github.com/CreaMakers/changli-planet-app) 二次开发，继续遵循根目录中的 MIT License，并保留原作者版权声明。

## 当前状态

- 项目名称：我的长理
- Android 包名：`com.mychangli.app`
- 当前正式发布：V1.0.5（versionCode 37）；正式 Tag：`v1.0.5-release-20261001`。后续沿当前正确版本线连续开发。
- 本应用不是长沙理工大学官方应用
- 第一版为个人独立开发的免费校园生活辅助工具
- 真实支付和学校合作尚未接入
- 已部署官网：`https://mychangliapp.cn`；Backend/Admin：`https://api.mychangliapp.cn`。
- 固定开发目录和发布来源见 [SOURCE_OF_TRUTH.md](SOURCE_OF_TRUTH.md)。构建前执行项目 `04_Tools/Test-MyChangliSourceIdentity.ps1`。

## 产品方向

- 主导航：首页、服务、我的
- 校园频道、普通帖子、评论、搭子社和校园活动不再作为正式用户功能
- 失物招领独立为生活工具
- 体育预约保留为本地模拟，不连接真实场馆、订单或支付系统
- 资料库支持可搜索、可分类扩展

## 构建

环境要求：

- 完整 JDK 17（需包含 `jlink`）
- Android SDK（compileSdk 36）

Windows：

```powershell
.\gradlew.bat :app:assembleDebug
```

可选本地配置放在不提交版本库的 `local.properties` 中：

```properties
sdk.dir=C\:\\Users\\you\\AppData\\Local\\Android\\Sdk
amap.apiKey=YOUR_AMAP_KEY
ads.enabled=false
```

## 本地依赖与授权

当前安全 CSUSTDataGet AAR 已纳入本仓库并由构建脚本校验 SHA-256；完整本地补丁源码位置见 SOURCE_OF_TRUTH.md。其他二进制依赖的授权与导出边界仍以 `LOCAL_ONLY_DEPENDENCIES.md` 为准，不因源码整理视为已完成授权。

## 品牌与合规

长沙理工大学校名、校徽及视觉标识不属于本项目的 MIT 授权范围。用于带广告或公开分发的第三方应用前，应取得学校相关授权，并明确标注非官方身份。广告功能必须在隐私政策、第三方 SDK 清单和用户同意机制完备后再启用。

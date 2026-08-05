# 我的长理

“我的长理”是一款面向长沙理工大学师生的非官方 Android 校园工具，优先提供稳定、快速、低维护成本的课表、成绩、考试、体育预约与失物招领能力。

本项目基于 [CreaMakers/changli-planet-app](https://github.com/CreaMakers/changli-planet-app) 二次开发，继续遵循根目录中的 MIT License，并保留原作者版权声明。

## 当前状态

- 项目名称：我的长理
- Android 包名：`com.mychangli.app`
- 当前阶段：发布前本地候选
- 本应用不是长沙理工大学官方应用
- 第一版为个人独立开发的免费校园生活辅助工具
- 真实支付和学校合作尚未接入
- 生产域名和正式云环境尚未配置
- 本地调试使用 `127.0.0.1` 与 ADB reverse；Release 配置使用 `.invalid` 占位地址

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

部分本地二进制依赖（CSUSTDataGet AAR、Lite3DMap 等）授权未确认，不包含在本仓库导出目录中。详见 `LOCAL_ONLY_DEPENDENCIES.md`。

## 品牌与合规

长沙理工大学校名、校徽及视觉标识不属于本项目的 MIT 授权范围。用于带广告或公开分发的第三方应用前，应取得学校相关授权，并明确标注非官方身份。广告功能必须在隐私政策、第三方 SDK 清单和用户同意机制完备后再启用。

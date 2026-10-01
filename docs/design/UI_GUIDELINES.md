# 我的长理 UI 规范

## 白底品牌卡片（2026-10-01 Owner 人工验收基线）

白色图标、品牌卡片或需要突出的白色主体放在白色/近白色页面时，应通过适当圆角、留白及轻微柔和的浅色阴影形成独立感。不是给所有白色元素统一加阴影。

优先使用本次淡蓝/淡蓝灰阴影：低存在感但边界可辨、过渡柔和；禁止黑色重投影、脏灰边、厚描边、强蓝光晕及厚重拟物按钮。图标原图必须完整清晰，禁止模糊主体冒充阴影。

## 已验收实际参数与位置

| 位置 | 主体 | 阴影 |
|---|---|---|
| PrivacyConsentScreen | 104dp，圆角26dp | Compose dropShadow：radius14dp、spread1dp、ARGB 0x387B9FCB、offset(0,6dp) |
| AccountGateScreen | 72dp，圆角18dp | Compose dropShadow：radius10dp、spread1dp、ARGB 0x387B9FCB、offset(0,4dp) |
| Android12+系统启动 | my_changli_splash_icon → splash_card → 1296px rounded母版 | 外层四向23%安全留白；卡片内左/右6%、上5%、下7%；splash_shadow采用18层0x037B9FCB柔化圆角轮廓 |
| Android11及以下启动 | 216dp居中卡片，纯白背景 | 复用splash_card与splash_shadow |

以上参数记录当前实现，不是所有未来页面的固定尺寸。保持已验收页面布局、按钮、协议区域及交互。可复用现有资源/样式；不得仅为抽象组件重构已验收代码。

外层须留阴影空间，先绘制阴影再裁剪主体，不能把阴影一起clip。检查透明留白造成的可见尺寸变化，不能只扩大容器。系统启动层存在放大和遮罩，必须保留完整品牌曲线及文字，不能新增长等待或重复中间页。

## 参考资源与验收归属

- references/brand-card-target.png：Owner提供的启动品牌卡片目标图。
- references/welcome-target.png：Owner提供的欢迎页品牌卡片层次参考。
- app/src/main/res/drawable-nodpi/my_changli_brand_logo.png：现有高清品牌母版。
- app/src/main/res/drawable-nodpi/my_changli_brand_logo_rounded.png：已验收系统启动圆角母版。

参考图是视觉设计依据，不是实机截图。用户于2026-10-01通过USB真机人工验收最终APK（SHA-256：936223F8A7D7E40DBB1929433190CE325073AE689E1BA36C4B90B7E56B538E0A），随后授权正式发布。验收不是自动截图结论。

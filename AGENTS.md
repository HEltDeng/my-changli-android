> 当前入口（2026-10-01 第二阶段整理）：Android `D:\Codex\01_App-应用开发\001_MyChangLi-我的长理\01_Source-当前源码\01_Android`；Backend/Admin `D:\Codex\01_App-应用开发\001_MyChangLi-我的长理\01_Source-当前源码\02_Backend-Admin`；Website `D:\Codex\01_App-应用开发\001_MyChangLi-我的长理\01_Source-当前源码\03_Website`。
> 以项目根目录 SOURCE_OF_TRUTH.md 与 04_Tools/source-identity.json 为现行导航。下文旧路径、候选状态、V1.0.4/2.0.9 环境验证属于历史记录，不得作为新的开发起点。

# 我的长理 Android 开发约束

- 修改前核对仓库、分支、HEAD 与未提交改动；保留已有工作。
- 品牌及白底主体样式遵循 [UI_GUIDELINES](docs/design/UI_GUIDELINES.md)。V1.0.5 的已验收实现是基准，禁止未经新需求重新调整参数。
- 系统启动图与应用内品牌图分别验证。不得将 adaptive-icon 直接传入 Compose painterResource。
- 不以构建或截图替代 Owner 最终视觉验收；不得循环冷启动抓帧、清空手机数据或改变手机网络。
- 发布身份及不可变制品见 docs/releases/v1.0.5-20261001/RELEASE_NOTES.md；历史撤回 v1.0.5 tag 不得移动或覆盖。

涉及 Backend/Admin 或 Website 时，必须显式读取对应 AGENTS.md 和 SOURCE_OF_TRUTH.md，不得假定附加目录指令自动加载。身份检查用法见 tools/README.md。

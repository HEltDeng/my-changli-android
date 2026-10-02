> 当前入口（2026-10-01 第二阶段整理）：Android `D:\Codex\01_App-应用开发\001_MyChangLi-我的长理\01_Source-当前源码\01_Android`；Backend/Admin `D:\Codex\01_App-应用开发\001_MyChangLi-我的长理\01_Source-当前源码\02_Backend-Admin`；Website `D:\Codex\01_App-应用开发\001_MyChangLi-我的长理\01_Source-当前源码\03_Website`。
> 以项目根目录 SOURCE_OF_TRUTH.md 与 04_Tools/source-identity.json 为现行导航。下文旧路径、候选状态、V1.0.4/2.0.9 环境验证属于历史记录，不得作为新的开发起点。

# 我的长理 Android 开发约束

- 修改前核对仓库、分支、HEAD 与未提交改动；保留已有工作。
- 品牌及白底主体样式遵循 [UI_GUIDELINES](docs/design/UI_GUIDELINES.md)。V1.0.5 的已验收实现是基准，禁止未经新需求重新调整参数。
- 系统启动图与应用内品牌图分别验证。不得将 adaptive-icon 直接传入 Compose painterResource。
- 不以构建或截图替代 Owner 最终视觉验收；不得循环冷启动抓帧、清空手机数据或改变手机网络。
- 发布身份及不可变制品见 docs/releases/v1.0.5-20261001/RELEASE_NOTES.md；历史撤回 v1.0.5 tag 不得移动或覆盖。

涉及 Backend/Admin 或 Website 时，必须显式读取对应 AGENTS.md 和 SOURCE_OF_TRUTH.md，不得假定附加目录指令自动加载。身份检查用法见 tools/README.md。

## 后续任务入口与主线延续

- 本仓固定目录是 Android 日常开发入口；任务开始先显式指定本仓工作目录，读取现行 `SOURCE_OF_TRUTH.md`，核验实际 Git 根、origin、分支、HEAD、暂存/未暂存及未跟踪差异。
- 使用已有 `tools/Test-MyChangliSourceIdentity.ps1 -Component android -Repository .` 执行 Development 身份预检，不新增重复系统。路径不可访问、权限不足或预检失败时报告具体问题，不猜旧目录、不自动切分支或丢弃修改。
- 沿正式 V1.0.5 基线 `d56fc5efc8f1584309a7bc348035780e2c816c27` 的当前正确主线及有效后继开发；不将 HEAD 固定在整理完成点，不因新任务 reset 回基线，不覆盖已完成成果。
- 未经明确授权，不从旧原型、旧测试版、V2/SICAU 历史工程复制或合并业务代码。历史归档、恢复快照和 `04_Tools/Git-Administration` 是历史/恢复/管理材料，不是开发目录。
- 保护既有修改，只提交当次任务明确修改并审阅过的文件。涉及官网时，既有 QA 修改、截图删除状态及未跟踪正式 APK 不得混入新任务提交。
- 初始会话仍指向 C 盘时，允许显式使用本仓开展已授权任务；这不代表 Codex 界面绑定已修复。源码整理已结束，不因新功能任务重启清理或全量审计。

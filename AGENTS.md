# 我的长理 Android 开发约束

- 修改前核对仓库、分支、HEAD 与未提交改动；保留已有工作。
- 品牌及白底主体样式遵循 [UI_GUIDELINES](docs/design/UI_GUIDELINES.md)。V1.0.5 的已验收实现是基准，禁止未经新需求重新调整参数。
- 系统启动图与应用内品牌图分别验证。不得将 adaptive-icon 直接传入 Compose painterResource。
- 不以构建或截图替代 Owner 最终视觉验收；不得循环冷启动抓帧、清空手机数据或改变手机网络。
- 发布身份及不可变制品见 docs/releases/v1.0.5-20261001/RELEASE_NOTES.md；历史撤回 v1.0.5 tag 不得移动或覆盖。

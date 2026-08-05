# CSUSTDataGet 本地安全构建说明

应用当前使用 `app/libs/CSUSTDataGet-1.1.4-no-sensitive-logs.aar`，替代运行时直接解析的远程 `CSUSTDataGet:1.1.4`。

- 上游仓库：`https://github.com/CreaMakers/CSUSTDataGet`
- 上游标签：`1.1.4`
- 上游提交：`7c2dfa9384ab2117ec917ff52b7d3b6b3b2f2894`
- AAR SHA-256：`9B03523FA04E9C8B3F5CAAAD49DC282B17EB2AC82E69A49E89764B21ABF71C29`

本地构建只关闭了依赖内部的 Android 日志输出、HTTP BODY 日志和 `printStackTrace()`。教务端点、请求参数、认证流程、Cookie 行为和返回值语义未改动。

这样处理是为了避免账号、密码、Cookie、Token 或登录页面内容进入 Debug 日志。重新构建该 AAR 时，必须从上述精确提交开始，并重新进行源码日志扫描、AAR 字节码审计和 SHA-256 记录；不得把任何测试凭据写入源码或构建配置。

原项目与第三方依赖的许可证、NOTICE 和版权声明仍按仓库现有文件保留。本说明不替代许可证文本。

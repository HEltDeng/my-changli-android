# V1.0.7 服务网页盘点

来源：现行 FunctionCatalog、Route、ResourceCatalog、后台驱动资料链接及原学校认证/课程网页。未新增学校服务。只读参考我的川农 08acecf93a513ea51507ee49eabcbb9e480a112b 的原生外层、分域适配及历史管理思路，未修改或合并川农工程。

| 入口 | 当前地址/已知域名 | 容器及本轮处理 | 登录/网络 | 实测范围 |
|---|---|---|---|---|
| 普通话 | https://zwfw.moe.gov.cn/mandarin/；实际仍为 zwfw.moe.gov.cn | MandeActivity → ServiceWebActivity，白圆返回、更多菜单；仅该 HTTPS 路径注入窄屏表单规则，验证码保持比例 | 查询需本人信息/验证码；公开页可访问 | App 实际加载；宽度/输入/软键盘/按钮可达；未提交真实查询 |
| 四六级 | https://cjcx.neea.edu.cn/html1/folder/21033/653-1.htm | CetActivity → 同一容器；保持官方页面，不注入通用 CSS | 真实查询需本人资料；公开页 | 地址与页面核对，真实结果待验 |
| 我的长理官网/公告入口 | https://mychangliapp.cn/ 及既有受限配置 URL | OfficialWebsiteActivity 统一顶部及更多菜单，既有安全导航保留 | 公开页面 | 原路径保持；不修改正式站发布内容 |
| 学校图书馆 | https://www.csust.edu.cn/tsg/index.htm；浏览器实际仍为该地址 | 已确认 HTTPS 学校链接默认进入统一原生网页容器；文章图片/表格局部规则 | 公开页可访问，馆内数据库另有授权限制 | 浏览器入口加载；全馆页面手机布局与馆藏业务待验 |
| 馆藏检索 | https://opac.csust.edu.cn/index；实际域名未改变 | 已确认学校域内嵌；无强行 CSS | 当前网络返回“您的访问请求已被拦截”；借阅/个人信息另需学校权限 | BLOCKED_BY_ORIGIN；没有绕过访问限制 |
| 教务处通知 | https://www.csust.edu.cn/jwc/index/tzgg.htm；实际域名未改变 | 学校网页统一外层，正文图片等比例、表格单独滚动 | 公开通知页已加载；教务系统另需账号/网络 | 浏览器访问通过，未验证全部通知附件 |
| 在校学生服务/学校官网链接 | https://www.csust.edu.cn/fldh/zxxs1.htm；实际域名未改变 | 已确认学校网页默认内嵌，后续协议跳转有明确外部确认 | 汇总页已加载；子服务可能需要校园网/认证 | 浏览器入口通过，子服务逐项登录未验证，未宣称全部原生化 |
| 往年试题 GitHub | https://github.com/MedivhTirisfal/csust_test_database | 既有外部浏览器入口；不把下载/仓库页面重写成自有功能 | 网络受实际连接影响 | 非学校业务页面，无新增采集 |
| 待提交作业 → 课程网页 | http://pt.csust.edu.cn/meol/jpk/course/layout/newpage/index.jsp?courseId=… | MoocCoursePageActivity 保留真实会话/文件选择；Compose 原生统一顶部，刷新改为明确确认后的 GET | 学校登录；可能依赖校园网 | 容器构建通过；真实课程访问待学校账号 |
| 学校绑定 → 网页登录 | https://authserver.csust.edu.cn/authserver/login?service=https%3A%2F%2Fehall.csust.edu.cn%2Flogin | 原 SSOWebviewDialog 专用认证流程保留；不复制到公共 ServiceWebActivity | 学校账号；回调 ehall.csust.edu.cn；既有课程域还有 cas.csust.edu.cn、xk.csust.edu.cn | 无真实账号，完整 SSO 跳转待验 |
| 后台资料库动态外链 | 实际 resource.externalUrl | HTTPS www.csust.edu.cn/opac.csust.edu.cn 内嵌，其他链接沿用显式浏览器打开 | 由资料来源决定 | 不把可配置链接称为已逐站验证 |

通用容器不读取表单值、不上传第三方结果、不开放原生 JavaScript 桥、不绕过证书。仅主文档失败显示错误，保留等待/重试；刷新不自动重发 POST；回退跳过认证跳转历史，保存 WebView 状态。视图恢复和多站真实业务仍需真机/实际账号确认。

“原生外层已统一”“网页可加载”“真实业务已完成”是三个不同结论。本清单保留待验项，不能将有外壳或构建成功记成全部网页移动适配通过。

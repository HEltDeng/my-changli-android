# 第三方 SDK / 服务清单（发布前草案）

> 状态：DRAFT。只列出经客户端代码确认可能真正接收数据的第三方/外部服务。普通 UI、数据库和工具库不因被依赖就被虚构为数据接收方。

| 第三方/外部服务 | 用途 | 触发方式 | 可能接收的数据 | 地址/说明 | 状态 |
| --- | --- | --- | --- | --- | --- |
| 高德相关服务（Lite3DMap / AMapLocation） | 校园地图、用户选择后的定位 | App-level 同意后，用户打开地图并单独同意定位 | 粗略/精确位置、IP、设备与网络信息；准确字段待成品审计 | `amap.com`、`autonavi.com`、`alicdn.com`、`aliyuncs.com` 等 | `Lite3DMap.jar` 来源、版本、LICENSE、NOTICE 与分发授权仍为 BLOCKER |
| 长沙理工大学统一身份认证/办事服务 | SSO 登录和学校服务 | 用户主动绑定/登录 | 学号、教务密码、验证码、Cookie/session、IP | `authserver.csust.edu.cn`、`ehall.csust.edu.cn`、`cas.csust.edu.cn` 等 | 学校外部服务，非本 App 后端 |
| 长沙理工大学教务/选课/MOOC | 课表、成绩、考试、MOOC | 用户主动查询；已绑定用户同意后可静默恢复学校会话 | 学号、密码/会话、查询参数、课程与教育数据、IP | `pt.csust.edu.cn`、`xk.csust.edu.cn` 等 | 部分 HTTP 明文风险；`CSUSTDataGet` 授权仍为 BLOCKER |
| 学校校园卡/电量等服务 | 电量/校园工具查询 | 用户主动输入并查询 | 宿舍楼、房间、查询参数、IP | `yktwd.csust.edu.cn` 等，具体依赖内部地址待复核 | 待完整本地依赖审计 |
| 学校官网/图书馆 | 外部资源页面 | 用户点击 | IP、浏览器标识、页面交互 | `www.csust.edu.cn`、`opac.csust.edu.cn` 等 | 外部 Web 页面 |
| 全国大学英语四六级查询页面 | 成绩查询入口 | 用户点击并主动填写 | IP、浏览器标识、用户填写内容 | `cjcx.neea.edu.cn` | 外部 Web 页面 |
| 教育政务普通话页面 | 普通话查询入口 | 用户点击并主动填写 | IP、浏览器标识、用户填写内容 | `zwfw.moe.gov.cn` | 外部 Web 页面 |
| GitHub | 开源说明、源码/资源入口 | 用户点击 | IP、浏览器标识 | `github.com` | 外部 Web 页面 |

## 不进入 Release 的项目

- LeakCanary、ByteDance CodeLocator、Compose tooling：Debug-only。
- 腾讯 Bugly：只有版本目录项，未作为 active dependency；初始化为 no-op。
- 广告、Analytics、推送、第三方登录 SDK：当前未接入；`ADS_ENABLED=false`。

## 本地二进制

- `CSUSTDataGet-1.1.4-no-sensitive-logs.aar`：教务/校园数据访问，active 声明但 Git 基线缺失；来源和分发授权待确认。
- `Lite3DMap.jar`：高德轻量地图/定位，active 声明但 Git 基线缺失；来源、SDK 版本、隐私字段和分发授权待确认。
- 其他历史 AAR/JAR/APK 不进入当前安全基线；若恢复必须另审，禁止上传 GitHub。

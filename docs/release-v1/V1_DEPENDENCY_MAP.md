# 我的长理 V1 不明本地二进制依赖地图

- 审计日期：2026-08-12
- 完整版基线：`9afec16ef5f8095a4c949f08ac1426bf2cd916d1`
- 范围：Gradle、catalog、imports、直接/反射/JNI 引用、Service、Repository、ViewModel、UI、Route、Manifest、Receiver、权限、网络与资源
- 当前文件状态：V1 worktree 不存在 `app/libs/CSUSTDataGet-1.1.4-no-sensitive-logs.aar` 或 `app/libs/Lite3DMap.jar`

## 总览

| Binary | 完整版基线 active declaration | Package/Class | V1 declaration | V1 结论 |
| --- | --- | --- | --- | --- |
| `CSUSTDataGet-1.1.4-no-sensitive-logs.aar` | `implementation(files(...))` | `com.dcelysia.csust_spider.*`、`com.example.csustdataget.CampusCard.CampusCardHelper` | 无；catalog 坐标亦删除 | `CORE_CAMPUS_SERVICE_RELEASE_BLOCKER`；当前暂不进入 binary |
| `Lite3DMap.jar` | `implementation(files(...))` | `com.amap.api.location.*`、`com.amap.api.maps.*` | 无 | `REMOVE_FROM_V1_BINARY` |

V1 另有 `verifyV1ForbiddenBinariesAbsent` preBuild task；任一目标文件重新出现在 `app/libs` 时，所有普通 build 在编译前失败。

## CSUSTDataGet dependency map

### Binary → Package/Class

- 学校认证与基础：`AuthService`、`Resource`、`RetrofitUtils`。
- 教务：`EducationHelper`、`EducationRepository`、`CourseScheduleData`、`Course`、`CourseGrade`、`GradeDetail`、`Campus`、`DayOfWeek`、`EduHelperError`。
- 考试：`ExamArrangeService`、`ExamArrange`。
- MOOC：`MoocRepository`、`MoocHomework`、`MoocTest`、`PendingAssignmentCourse`。
- 校园卡：`CampusCardHelper`。

### Package/Class → Service/Repository → ViewModel → UI → Navigation

| 依赖链 | 完整版真实路径 | V1 处置 |
| --- | --- | --- |
| 学校认证 | `AuthService` → `SchoolSessionCoordinator`/`UserStore` → `BindingUserActivity` | 实现、Activity、输入布局、Manifest 与 Route 移除；升级代码仅删除旧密钥/缓存 |
| 学校课表 | `EducationHelper` → `TimeTableViewModel`/`OverviewRepository` → timetable/overview | 网络链删除；Room、手动添加、组件与提醒保留 |
| 成绩 | `EducationHelper` → `ScoreInquiryViewModel` → `ScoreInquiryActivity` → `Route` | 整链移除 |
| 考试 | `ExamArrangeService` → `ExamArrangementViewModel`/cache → Activity/overview | 整链移除 |
| 空教室 | `EducationRepository` → `AvailableClassroomViewModel` → `ClassInfoActivity` | 整链移除 |
| MOOC | `MoocRepository` → `MoocDataHelper`/ViewModel → MOOC pages | 整链移除 |
| 校园卡/电量 | `CampusCardHelper` → `ElectricityRepository`/ViewModel → Activity/Widget | 整链、Receiver 与资源移除 |

### Permission → Network → Feature

| 项目 | 完整版关系 | V1 结果 |
| --- | --- | --- |
| `INTERNET` | 学校与自有服务共同需要 | 为自有 Backend/公开网页保留 |
| Wi-Fi 状态/变更 | 历史学校能力残留 | Release 移除 |
| 学校 cleartext allowlist | `pt.csust.edu.cn`、`xk.csust.edu.cn`、校园卡等 | 全部移除；base cleartext 为 false |
| 学校凭据 | 学号、密码、验证码、Cookie/session | 无输入 UI、无学校会话类、无自有 Backend payload 字段 |
| Feature | 学校同步、成绩、考试、空教室、MOOC、电量 | 产品目标 `OPEN`；当前因 dependency/rights gate `BLOCKED` |

## Lite3DMap dependency map

### Binary → Package/Class

- 定位：`AMapLocation`、`AMapLocationClient`、`AMapLocationClientOption`、`AMapLocationListener`。
- 地图：`AMap`、`MapView`、`CameraUpdateFactory`、`LocationSource`。
- 模型：`CameraPosition`、`LatLng`、`MyLocationStyle`、`Polygon`、`PolygonOptions`。

### Package/Class → Service/Repository → ViewModel → UI → Navigation

| 层 | 完整版真实路径 | V1 结果 |
| --- | --- | --- |
| 隐私初始化 | `PrivacyConsentManager` / `PlanetApplication` 调 AMap privacy API | 调用移除，只保留 App 自身 consent |
| Repository/ViewModel | `CampusMapRepository` / `CampusMapViewModel` | 实现移除 |
| UI/Navigation | `CampusMapActivity` → `Route.goCampusMap` / FunctionCatalog | Activity、Route 和目录项移除 |
| Manifest | AMap key metadata | 移除 |
| Permission | coarse/fine location | 移除 |
| Network | AMap domain 与 HTTP webapi 例外 | 移除 |
| Background/Deep Link | 基线未发现相关 Worker/Service/Receiver/deep link | V1 仍全局扫描确认无业务 deep link |

## Reflection / JNI / embedded residue

- 未发现针对两个目标的 `Class.forName`、`DexClassLoader`、`PathClassLoader`、`System.loadLibrary` 或 `external fun`。
- 未发现目标文件、解包 classes、目标 native `.so` 或旧 APK 提取副本。
- Release runtime dependency tree 对 `CSUSTDataGet|Lite3DMap|amap|alipay|wechat|weixin|bugly|leakcanary|codelocator` 的匹配为空。
- Debug APK ZIP entry 名称无两个目标；DEX 包路径无 `com/dcelysia/csust_spider`、`com/example/csustdataget` 或 `com/amap`。

## 闭环状态

1. Lite3DMap 的 Gradle、本地二进制和地图入口闭环保持完成。
2. CSUSTDataGet 的当前声明仍删除，但其角色已从“永久排除”纠偏为“发布权未证明导致的核心校园服务 blocker”。
3. 学校/校园卡实现目前仍未进入 V1；这是 Stop Condition 后的安全中间态，不是最终产品 CLOSED 决策。
4. 本地课表、失物招领、自有账号和自有 Backend 能力独立保留。
5. Manifest 的地图入口、metadata、location 权限和网络例外保持移除；学校 hosts 只有在合法集成恢复后才能重新评估。
6. 目标文件缺失条件下，70 项单测与 Release compile 再次成功。
7. 正式 signed APK/AAB 同时受校园服务 dependency gate、外置 signing config 和正式 endpoint Gate 阻塞。

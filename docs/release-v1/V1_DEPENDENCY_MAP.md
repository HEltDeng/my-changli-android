# 我的长理 V1 不明本地二进制依赖地图

- 审计日期：2026-08-12
- 基线 SHA：`9afec16ef5f8095a4c949f08ac1426bf2cd916d1`
- 审计范围：Gradle、Version Catalog、import、直接类引用、reflection、JNI、Service、Repository、ViewModel、UI、Navigation、Intent、Widget、Receiver、Manifest、permission、network security、resources
- 文件状态：V1 worktree 中 `app/libs` 不存在；目标 AAR/JAR 均不存在

## 总览

| Binary | Active Gradle declaration | 实际包/类 | 反射/JNI | V1 结论 |
| --- | --- | --- | --- | --- |
| `CSUSTDataGet-1.1.4-no-sensitive-logs.aar` | `implementation(files("libs/CSUSTDataGet-1.1.4-no-sensitive-logs.aar"))` | `com.dcelysia.csust_spider.*`、`com.example.csustdataget.CampusCard.CampusCardHelper` | 未发现 | `REMOVE_FROM_V1_BINARY` |
| `Lite3DMap.jar` | `implementation(files("libs/Lite3DMap.jar"))` | `com.amap.api.location.*`、`com.amap.api.maps.*` | 未发现 | `REMOVE_FROM_V1_BINARY` |

`gradle/libs.versions.toml` 还保留 `com.github.CreaMakers:CSUSTDataGet:1.1.4` catalog 坐标，但当前未启用。V1 不允许通过取消注释 Maven 坐标来绕过授权问题。

## CSUSTDataGet dependency map

### Binary → Package/Class

- 教务/MOOC 基础：`Resource`、`RetrofitUtils`。
- 学校认证：`AuthService`。
- 教务：`EducationHelper`、`EducationRepository`、`CourseScheduleData`、教务 `Course`、`CourseGrade`、`GradeDetail`、`Campus`、`DayOfWeek`、`EduHelperError`。
- 考试：`ExamArrangeService`、`ExamArrange`。
- MOOC：`MoocRepository`、`MoocHomework`、`MoocTest`、`PendingAssignmentCourse`。
- 校园卡/电量：`CampusCardHelper`。

### Package/Class → Service/Repository

| 依赖类 | Service / Repository / Store |
| --- | --- |
| `AuthService`、`MoocRepository`、`RetrofitUtils` | `SchoolSessionCoordinator`、`UserStore`、`AccountDataManager` |
| `EducationHelper`、`CourseScheduleData` | `TimeTableViewModel`、`CommonInfo`、`OverviewRepository` |
| `EducationRepository`、`AuthService` | `AvailableClassroomViewModel` |
| `ExamArrangeService`、`ExamArrange` | `ExamArrangementViewModel`、`OverviewRepository`、`ExamArrangementCache` |
| `EducationHelper`、`CourseGrade`、`GradeDetail` | `ScoreInquiryViewModel` |
| `MoocRepository` 与 MOOC DTO | `MoocDataHelper`、`MoocViewModel`、MOOC UI state |
| `CampusCardHelper` | `ElectricityRepository`、`ElectronicAppWidget` |

### Repository/Store → ViewModel/UI

| 层 | 受影响文件/页面 |
| --- | --- |
| 学校会话 | `SchoolSessionCoordinator.kt`、`UserStore.kt`、`BindingUserActivity.kt` |
| 课表网络同步 | `TimeTableViewModel.kt` → `TimeTableActivity.kt`；本地 Room/手动添加可独立保留 |
| 首页聚合 | `OverviewRepository.kt` → `OverviewViewModel` → `OverviewScreen` |
| 成绩 | `ScoreInquiryViewModel.kt` → `ScoreInquiryActivity.kt` |
| 考试 | `ExamArrangementViewModel.kt` → `ExamArrangementActivity.kt` |
| 空教室 | `AvailableClassroomViewModel.kt` → `ClassInfoActivity.kt` |
| MOOC | `MoocDataHelper` / `MoocViewModel` → `MoocActivity` / `MoocCoursePageActivity` / `MoocScreen` |
| 电量 | `ElectricityRepository` / `ElectronicViewModel` → `ElectronicActivity` / `ElectronicScreen` / `ElectronicAppWidget` |

### UI → Navigation / Background

- `FunctionCatalog` 暴露 `ScoreInquiry`、`Classroom`、`Homework`、`Electronic`、`ExamArrangement`；`Route` 直接构造这些 Activity Intent。
- 首页 `MainScreen` / `OverviewFragment` 还有成绩、电量和学校绑定的直接动作。
- 设置页 `ProfileSettingsRoute` 暴露学校绑定/切换学号。
- Manifest 注册学校绑定、成绩、考试、空教室、MOOC、电量 Activity，以及 `ElectronicAppWidget` Receiver。
- `ElectronicAppWidget` 的点击 `PendingIntent` 可绕过普通菜单，因此必须同时移除 Receiver 与实现。
- 未发现学校业务 `android.intent.action.VIEW` Deep Link、dynamic shortcut 或 exported Activity；不能据此只隐藏按钮。

### Permission / Network / Feature

| 项目 | 依赖关系 |
| --- | --- |
| `INTERNET` | 学校认证/教务/MOOC/电量需要；V1 自有 Backend 与公开外部页面仍需要，不能随依赖删除 |
| `ACCESS_WIFI_STATE` / `CHANGE_WIFI_STATE` | 基线无法证明 OPEN 功能使用；历史审计因缺失 AAR/JAR暂留。依赖移除后重新以 merged manifest/代码核验 |
| cleartext allowlist | `pt.csust.edu.cn`、`xk.csust.edu.cn`、`yktwd.csust.edu.cn` 等服务学校会话；学校能力 CLOSED 后应移除不再需要的明文例外 |
| 学校凭据 | 学号、密码、Cookie/session 由 `StudentInfoManager` / school session 链使用；Backend contract 明确禁止这些字段 |
| V1 feature | 在线学校课表、成绩、考试、空教室、MOOC、电量、学校绑定全部 `CLOSED`；本地手动课表另行保留为 `OPEN` |

## Lite3DMap dependency map

### Binary → Package/Class

- 定位：`AMapLocation`、`AMapLocationClient`、`AMapLocationClientOption`、`AMapLocationListener`。
- 地图：`AMap`、`MapView`、`CameraUpdateFactory`、`LocationSource`。
- 地图模型：`CameraPosition`、`LatLng`、`MyLocationStyle`、`Polygon`、`PolygonOptions`。

### Package/Class → Service/Repository/ViewModel/UI

| 层 | 真实代码 |
| --- | --- |
| 隐私初始化 | `PrivacyConsentManager` 调用 `AMapLocationClient.updatePrivacyShow/updatePrivacyAgree` |
| Application | `PlanetApplication.initAMapPrivacy()` 在已有 App consent 时同步 SDK 隐私状态 |
| Repository/ViewModel | `CampusMapRepository` / `CampusMapViewModel` 提供校园坐标数据；本身不证明 JAR 来源合法 |
| UI | `CampusMapActivity` 创建 `MapView`、配置地图、polygon、定位 source 和 `AMapLocationClient` |
| Navigation | `FunctionCatalog.CampusMap` → `Route.goCampusMap()`；Manifest 注册 `CampusMapActivity` |

### Manifest / Permission / Network / Resource

| 项目 | 真实关系 |
| --- | --- |
| Manifest metadata | `com.amap.api.v2.apikey=${amapApiKey}`；key 从 `local.properties`/`AMAP_API_KEY` 注入 |
| Permissions | `ACCESS_COARSE_LOCATION`、`ACCESS_FINE_LOCATION` 只找到校园地图定位用途 |
| Network | `netconfig.xml` 为历史 `liteAmap.html` 放行 `http://webapi.amap.com`，并允许 `amap.com` 等域 |
| Resources | 地图 UI、坐标/校园 polygon 资源与文案；不等于 SDK 授权证据 |
| Background/Deep Link | 未发现地图 Worker、Service、Receiver、Deep Link、JNI 或 reflection 入口 |
| V1 feature | 校园地图与定位 `CLOSED`；移除 Activity/Route/metadata/permissions/cleartext 例外 |

## Reflection / JNI / embedded residue result

- `Class.forName` 仅出现在项目通用 startup initializer，读取应用自有 startup class 名；未发现动态加载上述二进制类名。
- 未发现 `System.loadLibrary`、`external fun`、`jniLibs`、`DexClassLoader`、`PathClassLoader` 与两个目标相关的路径。
- 未发现目标 AAR/JAR、其解包 classes、native `.so` 或从旧 APK 提取的副本。

## V1 闭环验证计划

1. 删除两个 Gradle active dependency，并保持目标文件实际不存在。
2. 将学校网络/凭据/校园卡实现和 AMap 实现从 V1 编译输入移除；本地课表改为自有 Room-only 路径。
3. 同时移除所有对应 UI、Navigation、Manifest、Widget/Receiver、权限、SDK metadata 和 cleartext 例外。
4. 新增本地 V1 hard policy 测试，证明远端 Feature Flag 不能打开 sports/school/payment/map。
5. 在无目标文件环境执行 Gradle configuration、Debug compile/build、Release compile、测试。
6. 对实际 APK/AAB 扫描 class、resource、manifest、domain、permission 和字符串，证明两个依赖无 residue。

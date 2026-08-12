# V1 Dependency Map — Release-First Restoration

## CSUSTDataGet

```text
CSUSTDataGet-1.1.4-no-sensitive-logs.aar
  -> com.dcelysia.csust_spider core / education / mooc / campus-card classes
  -> SchoolSessionCoordinator, StudentInfoManager, AccountDataManager
  -> Education/MOOC/calendar/electricity repositories and services
  -> ViewModels for timetable, grades, exams, classrooms, MOOC, calendar, electricity
  -> BindingUserActivity and Study/Life Activities
  -> FunctionCatalog / Overview / Route / Manifest
  -> INTERNET only; no map/location permission
  -> HTTPS authserver/ehall/card services; exact HTTP compatibility for xk/pt
  -> school account, timetable, grades, exams, classrooms, MOOC, calendar, electricity/card
```

Key packages include `com.dcelysia.csust_spider.education`, `.mooc` and the campus-card helper used by electricity queries. The fixed AAR is verified at preBuild and is present in the QA APK.

## Lite3DMap / map SDK

```text
Lite3DMap.jar or AMap dependency
  -> ABSENT
  -> no CampusMap repository/ViewModel/UI
  -> no route or manifest Activity/metadata
  -> no location/Wi-Fi/camera permission
  -> Campus Map CLOSED
```

## Sports and payment

Future DTO/model/abstraction code may remain, but the runtime Activity, shortcut, route, live inventory/order/fulfilment and payment SDK/provider/merchant paths are absent. Sports and payment remain production-default OFF.

## Own Backend boundary

School-password/CAPTCHA/Cookie/token/session fields are absent from own-Backend DTO/service boundaries. Lost & Found, resources, feedback and My Changli account continue to use the separate Backend stack.

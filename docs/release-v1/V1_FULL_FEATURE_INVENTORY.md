# V1 Full Feature Inventory

- Inventory date: 2026-08-12
- Read-only source: original `main` at `9afec16ef5f8095a4c949f08ac1426bf2cd916d1`
- Remediation start: `b015791949dc8ce6770429b3b4f9dcaeac3f7132`
- Source surfaces checked: `FunctionCatalog`, Overview, `FeatureFragment`, `Route`, Manifest, profile/settings, repositories, ViewModels, widgets and receivers

This is an inventory of the complete product baseline, not a claim that every item is currently present in the V1 Release branch. The previous V1 scope result is formally recorded as `REJECTED_FOR_OVER_REMOVAL` because study and life campus features were incorrectly removed.

## Home

| Baseline feature | Source / route | Corrected V1 product decision | Current remediation state |
| --- | --- | --- | --- |
| Guest-first overview | `OverviewFragment`, `OverviewScreen` | OPEN | Present |
| School account binding prompt | `Route.goBindingUser` | OPEN | BLOCKED by core school-service dependency gate |
| Today/tomorrow timetable | `OverviewRepository`, timetable Room data | OPEN | Local-only subset present; school sync blocked |
| Pending MOOC homework/tests | `MoocDataHelper`, Overview | OPEN | BLOCKED |
| Exam arrangement summary | `ExamArrangeService`, Overview | OPEN | BLOCKED |
| Grade/electricity metric cards | Overview quick routes | OPEN | BLOCKED |
| Primary shortcuts | schedule, grade, classroom, homework, electric, exam, calendar | OPEN | Only dependency-independent subset present |
| Announcements, service updates and version metadata | accepted own Backend contract | OPEN | Present; Release endpoint fails closed until configured |

## Services -> Study

The authoritative baseline `learningFunctionShortcuts()` contains eight entries.

| ID | User-facing capability | Baseline chain | Corrected decision | Current state |
| --- | --- | --- | --- | --- |
| `schedule` | School timetable sync/query plus local timetable | `EducationHelper` -> `TimeTableViewModel` -> `TimeTableActivity` | OPEN | Local/manual timetable present; school sync BLOCKED |
| `grade` | Grade list and grade detail | `EducationHelper` -> `ScoreInquiryViewModel` -> `ScoreInquiryActivity` | OPEN | BLOCKED |
| `classroom` | Available classroom query | `EducationRepository` -> `AvailableClassroomViewModel` -> `ClassInfoActivity` | OPEN | BLOCKED |
| `homework` | MOOC courses, assignments and tests | `MoocRepository` -> `MoocDataHelper`/`MoocViewModel` -> MOOC pages | OPEN | BLOCKED |
| `exam` | Exam terms and arrangements | `ExamArrangeService` -> ViewModel/cache -> `ExamArrangementActivity` | OPEN | BLOCKED |
| `document` | Self-operated resource library | own Backend -> `ResourceLibraryActivity` | OPEN | Present |
| `cet` | CET information/web utility | `CetActivity` | OPEN | Present |
| `mandarin` | Mandarin test information/web utility | `MandeActivity` | OPEN | Present |

Supporting study capabilities found outside the eight visible cards:

- School ID/password binding and switching.
- SSO login form, CAPTCHA check/image path, SSO/eHall redirect and school logout.
- Process-local session restoration and explicit re-authentication.
- Term discovery, semester start date and teaching-calendar parsing.
- Course details, manual course addition, local Room cache, widgets and user-enabled reminders.
- Grade detail components, exam cache, MOOC course detail/homework/test pages.

## Services -> Life

The authoritative baseline `lifeFunctionShortcuts()` contains three entries.

| ID | User-facing capability | Baseline chain | Corrected decision | Current state |
| --- | --- | --- | --- | --- |
| `electric` | Dorm electricity and campus-card-backed lookup | `CampusCardHelper` -> `ElectricityRepository` -> Activity/widget | OPEN | BLOCKED |
| `calendar` | Network school/teaching calendar | `SemesterCalendarRepository` -> calendar screens | OPEN | BLOCKED |
| `lost_found` | Self-operated lost-and-found | accepted own Backend -> `LostFoundActivity` | OPEN | Present |

Other campus-life/support capabilities found in the complete baseline:

- Electricity home-screen widget.
- Campus-card SSO/ticket/token exchange used for electricity data.
- User feedback, account/data deletion controls, local account book and local date/week tools.
- Self-operated resource library and account-gated cloud writes.
- Campus community, Buddy, old news/comments and remote skins exist in history, but are outside the authoritative Services -> Study/Life lists and do not match the accepted production Backend contract. They require a separate product/contract decision; this audit does not silently reclassify them as one of the three explicit exclusions.

## My / Settings

| Baseline item | Decision / status |
| --- | --- |
| My Changli account center | OPEN; present |
| Bind/switch school account | OPEN; BLOCKED by core school-service dependency gate |
| Clear local tool cache | OPEN; present |
| Class reminder and timetable widgets | OPEN; present for local timetable |
| Theme settings / remote skin download | Local theme present; remote download depends on an unconfigured endpoint |
| Feedback | OPEN; present |
| About My Changli and update check | OPEN; present |
| Privacy policy, user agreement, third-party list, personal-information list | OPEN; present and must be expanded when school integration is actually restored |
| Account and data management / school unbind | Own-account controls present; school unbind implementation BLOCKED |

## Background / hidden entry points

Read-only inventory found:

- Non-exported business Activities for account, feedback, calendar, sports, lost-found, resource library, MOOC, map, classroom, grades, timetable, school binding, exam, electricity, community and skins.
- App widgets: timetable, next course and electricity.
- Receivers: course reminder, boot/date/time/timezone/package-change schedule refresh and widget refresh.
- Internal Routes for all visible features plus publishing, comments, user home, Buddy, map, sports, binding, privacy documents and WebView.
- Only `MainActivity` is exported as the launcher; the baseline has no business `<data>` deep link.

For the corrected V1 target, campus map, real sports reservation and real payment must remain unreachable through every route, Activity, widget, notification, receiver, worker, shortcut and debug entry. School study/life features are product-OPEN but cannot be restored into a distributable binary until the dependency gate is closed.

## Inventory counts used by handoff

- Study cards found: 8.
- Study cards currently usable: 3 fully (`document`, `cet`, `mandarin`) plus the local/manual subset of `schedule`.
- Life cards found: 3.
- Life cards currently usable: 1 (`lost_found`); `electric` and network `calendar` are blocked.
- Explicit V1 exclusions: exactly 3 product classes — campus map, real sports reservation and real payment.

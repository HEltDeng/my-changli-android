# V1 Full Feature Inventory

- Date: 2026-08-12
- Source reference: original full baseline `9afec16ef5f8095a4c949f08ac1426bf2cd916d1`
- Restoration policy: selective restoration; no wholesale reset or merge

## School account and authentication

The V1 branch contains the school binding Activity, student-ID/password inputs, CAPTCHA detection/form path, SSO/CAS redirects, Cookie/session coordination, login-state observation, account switching, re-authentication, logout and unbind cleanup. App-side password persistence uses Android Keystore-backed AES-GCM. Library credential and Cookie MMKV stores are cleared and opened read-only so runtime sessions remain process-local.

No real school account was used by Codex. Runtime behavior requiring a real account is `RESTORED / USER E2E PENDING`, not self-declared PASS.

## Services -> Study

The authoritative list again exposes all eight original entries:

| ID | Capability | Route/UI | Data/runtime |
| --- | --- | --- | --- |
| `schedule` | school sync plus local/manual timetable | RESTORED | school education helper + local Room/cache |
| `grade` | grades and details | RESTORED | education repository/service/ViewModel |
| `classroom` | available classrooms | RESTORED | education repository/ViewModel |
| `homework` | MOOC courses/homework/tests | RESTORED | MOOC repository/helper/screens |
| `exam` | exams and details | RESTORED | exam service/cache/ViewModel |
| `document` | resource library | PRESERVED | My Changli Backend |
| `cet` | CET utility | PRESERVED | existing public utility flow |
| `mandarin` | Mandarin utility | PRESERVED | existing public utility flow |

Semester discovery, teaching week/calendar parsing, overview cards, loading/error states, timetable widgets and user-enabled reminders were restored or preserved with their original structure.

## Services -> Life

| ID | Capability | State |
| --- | --- | --- |
| `electric` | electricity/campus-card-backed query and widget | RESTORED / USER E2E PENDING |
| `calendar` | network school/teaching calendar | RESTORED / USER E2E PENDING |
| `lost_found` | self-operated Lost & Found | PRESERVED / own-Backend QA pending |

Other baseline non-map and non-transaction features were retained where their code and route belonged to the existing product. The release-first pass did not redesign visual structure.

## Explicit exclusions

- Campus map: CLOSED; map dependency, Activity, route, metadata and location permission absent.
- Real sports reservation: CLOSED; no runtime Activity/route or live request path.
- Real payment: CLOSED; no SDK/provider/merchant/real-money path.

## QA inventory result

- Study cards: 8/8 present in the catalog.
- Life cards: 3/3 present in the catalog.
- School binding, timetable, grades, exams, classroom, MOOC and electricity Activities are present in the merged manifest and Debug APK.
- Automated build/static verification passed; install, UI and real-account E2E remain user acceptance work.

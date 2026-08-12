# My Changli V1 Full Campus QA Checklist

- Artifact: `MyChangli-V1.0.0-Full-QA-debug.apk`
- Type: `DEBUG QA ONLY / NOT RELEASE / NOT RC`
- Package: `com.mychangli.app.debug`
- Version: `1.0.0` (`32`)
- Size: 33,813,056 bytes (32.25 MiB)
- SHA-256: `E8F3ED318633A075E5CD36F627CF25CEA284567F4291CF588B56691F657D433C`
- Signing: Android Debug, certificate SHA-256 `8CE0AA11F57B7921BED57258461B1AAEE5D795E801DC49BCA8471CC7FAE77B15`

## Install and launch

- [ ] Install on a QA device; confirm it is the `.debug` package.
- [ ] Launch, splash, icon, app name and first-run privacy gate.
- [ ] Confirm no immediate crash, blank page or repeated consent loop.

## School account (user-operated only)

- [ ] Open school binding; check non-official and data-use notice.
- [ ] Login with the user's own account; exercise wrong-password and CAPTCHA states.
- [ ] Verify successful login, session reuse, expiry/re-login and account switch.
- [ ] Logout and unbind; confirm credentials/session and school cache are cleared.

## Study

- [ ] All eight cards exist in the expected order: timetable, grades, classroom, MOOC, exams, resources, CET, Mandarin.
- [ ] School timetable sync plus local/manual timetable.
- [ ] Grades and grade details.
- [ ] Exams and exam details.
- [ ] Empty-classroom query.
- [ ] MOOC courses, homework and tests.
- [ ] Semester/teaching week/network calendar.

## Life and existing features

- [ ] Electricity and campus-card query.
- [ ] School calendar entry.
- [ ] Lost & Found.
- [ ] Resource Library, CET and Mandarin.
- [ ] Own account, feedback, announcements, settings, widgets/reminders.

## UI regression

- [ ] No Chinese mojibake, missing/duplicate cards, overlap, clipping or invisible text.
- [ ] Icons, color, typography and layout match the established product.
- [ ] Loading, empty, offline and error states are understandable.
- [ ] Back navigation, dark mode and process restart behave normally.

## Closed features

- [ ] Campus map cannot be reached and no location permission is requested.
- [ ] Real sports reservation cannot create inventory/order/fulfilment.
- [ ] Real payment/refund cannot be initiated.

## Network notice

`xk.csust.edu.cn` and `pt.csust.edu.cn` use exact-host HTTP compatibility because the current school service does not offer an equivalent working HTTPS data plane. The Debug QA package also permits exact loopback hosts for the own-Backend test configuration. Do not treat this package as a store candidate.

Final acceptance belongs to the user; automated results do not complete these checkboxes.

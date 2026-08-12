# V1 School HTTP/HTTPS Compatibility Audit

Prior non-invasive investigation established that the real `xk` and `pt` service paths used by the complete product did not expose an equivalent working HTTPS data plane. The evidence and probe results remain preserved in reports 58 and 59.

## V1 decision

- `authserver`, `ehall` and campus-card/electricity HTTPS hosts remain TLS-only.
- `xk.csust.edu.cn` and `pt.csust.edu.cn` receive exact, non-subdomain HTTP exceptions.
- Base policy remains `cleartextTrafficPermitted=false`.
- No global cleartext, trust-all certificate manager, hostname bypass, TLS downgrade code, own-Backend credential proxy or CAPTCHA bypass exists.
- Debug additionally permits exact loopback hosts for the own-Backend QA configuration; Release does not.

## Risk

The xk/pt paths can carry authenticated Cookie/session state and personal academic/MOOC data over unencrypted HTTP. The app discloses this fact. This is `KNOWN_V1_RISK` accepted by the explicit release-first product decision; it is not described as secure transport.

## Migration trigger

When the school provides equivalent working HTTPS endpoints, replace the service base URLs, validate certificate/hostname/redirect/Cookie behavior, remove both exceptions, rerun parser/session tests and produce a new binary audit.

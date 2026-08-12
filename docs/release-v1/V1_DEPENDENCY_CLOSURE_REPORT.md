# V1 Dependency Closure Report — Scope Remediation

- Date: 2026-08-12
- Complete baseline: `9afec16ef5f8095a4c949f08ac1426bf2cd916d1`
- Branch: `release/v1-initial`

## Result

- `Lite3DMap.jar`: dependency closure remains complete; it is absent and campus map remains CLOSED.
- Payment SDK/merchant implementation: absent; real payment remains CLOSED.
- Real sports transaction path: unreachable; real reservation remains CLOSED.
- CSUSTDataGet: project-specific use, modification, build and APK/AAB distribution authorization is confirmed; no open-source license is asserted.

Rights result: `CORE_CAMPUS_SERVICE_RELEASE_BLOCKER = RESOLVED_BY_RIGHTSHOLDER_AUTHORIZATION`.

Runtime result: `SCHOOL_CLEARTEXT_SECURITY_BLOCKER`.

## Why no CSUSTDataGet implementation was restored

The exact upstream source/tag/commit and local modified AAR were audited. The user then confirmed project-specific rightsholder authorization, resolving the rights gate. The preferred integration changed to a reproducible, auditable source module rather than reinserting the opaque derivative.

Before source integration, public transport probes and source inspection established that `xk.csust.edu.cn` and `pt.csust.edu.cn` lack usable HTTPS while authenticated school-session cookies and personal academic/MOOC data use those hosts. This triggers the user-defined cleartext Stop Condition.

## Current safe build boundary

- Neither local binary exists in the V1 worktree.
- Existing Gradle declarations remain absent.
- The current preBuild gate still rejects the old opaque AAR filename and Lite3DMap. A future authorized source module would be a distinct, auditable dependency only after transport safety is resolved.
- Current V1 unit tests and Release compile pass without the two files.
- Current Release runtime contains no map or payment SDK.

## Corrected product boundary

The safe interim binary is not the desired full-campus product. School authentication and Study/Life queries are now documented as product-OPEN but release-BLOCKED. They must not be silently classified CLOSED or deleted as a final product decision.

## Next closure options

1. The school provides working HTTPS endpoints for the legacy education and MOOC services; then complete the authorized source module, credential/privacy and device audits.
2. The school provides an authorized HTTPS replacement contract. No Android-only configuration can make authenticated HTTP confidential.

No AAR/JAR, secret, credential or signing material was added to Git.

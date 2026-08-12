# CSUSTDataGet Project-Specific Authorization Record

- Record date: 2026-08-12
- Project: My Changli / 我的长理
- Dependency: `CreaMakers/CSUSTDataGet`
- Upstream version: `1.1.4`
- Exact upstream commit: `7c2dfa9384ab2117ec917ff52b7d3b6b3b2f2894`
- Prior locally audited derivative SHA-256: `9B03523FA04E9C8B3F5CAAAD49DC282B17EB2AC82E69A49E89764B21ABF71C29`

## Authorization basis

The user stated in the current Codex task that the original rightsholder explicitly authorized the My Changli project to:

- use CSUSTDataGet;
- make changes necessary for this project;
- build it as a project dependency;
- distribute and publish it as part of the My Changli Android APK/AAB; and
- create security-hardened derivative builds for this project.

Targeted filename searches in the project documentation, task-record and release-audit archives did not locate a separate non-sensitive authorization attachment. This record therefore preserves the user's explicit project instruction and scope; it does not claim independent legal verification of private correspondence.

## Classification

- Authorization type: project-specific rightsholder authorization.
- Open-source license: none asserted or fabricated.
- Upstream repository license state: unchanged.
- School authorization: **not granted or implied**. Authorization from the dependency rightsholder is not authorization from Changsha University of Science and Technology.
- Resolved gate: `CORE_CAMPUS_SERVICE_RELEASE_BLOCKER = RESOLVED_BY_RIGHTSHOLDER_AUTHORIZATION`.

## Remaining independent gates

Authorization resolves the dependency-rights question only. Credential storage, logging, HTTPS transport, school-session handling, privacy disclosure, tests and binary review remain independent release requirements. During the same restoration attempt, the HTTP transport audit triggered `SCHOOL_CLEARTEXT_SECURITY_BLOCKER`; no authorized source or binary was therefore added to the V1 branch.

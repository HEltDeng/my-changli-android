# V1 Release Lint Remediation

- Initial complete-runtime result: 174 errors / 1,377 warnings / 3 hints.
- Final result: **0 errors / 1,378 warnings**.
- Gradle task: `lintRelease` completed successfully.

## Error closure

| Category | Initial | Final | Remediation |
| --- | ---: | ---: | --- |
| `NewApi` | 130 | 0 | core-library desugaring 2.1.5 plus API-compatible TextView setter |
| `UseAppTint` | 40 | 0 | compatible `app:tint` attributes; no visual redesign |
| `ResourceType` | 2 | 0 | declared `SkinTabLayout` styleable and generated indices |
| `Suspicious0dp` | 1 | 0 | fixed runtime `android:layout_weight` in lost-item layout |
| `LocalContextGetResourceValueCall` | 1 | 0 | `LocalResources` access in Compose |

## Warning classification

Warnings are dominated by unused resources, hardcoded/localization debt, accessibility content descriptions, RTL compatibility, deprecations and performance/style suggestions. Scoped-storage warnings originate from explicit manifest removal markers, not granted storage permissions.

The warning count increased by one while the complete runtime and final resource surface were restored; none is classified as a Debug QA build/install/crash blocker. Accessibility, localization, deprecated API and resource cleanup remain post-QA work. No warning was mass-suppressed to manufacture the result.

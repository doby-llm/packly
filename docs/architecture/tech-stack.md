# Packly Technical Stack Blueprint

## Recommendation

Use a modern Android-only stack centered on Jetpack Compose, Material 3, unidirectional state flow, and JSON-backed persistence. The best architectural pattern is Clean Architecture with a light Hexagonal boundary around persistence.

Layers:

- UI: Compose screens, reusable Material 3 components, navigation, and UI state rendering only.
- Presentation: ViewModels expose immutable StateFlow screen state and accept explicit user events.
- Domain: models, validation, seed-data rules, migrations contracts, and use cases independent of Android UI APIs.
- Data: repositories, JSON serializers, DataStore adapter, migrations, and seed bootstrapping.

This keeps Packly small enough for fast delivery while preserving seams for future sync, import/export, backup, or database migration.

## Framework choices

Use Jetpack Compose with Material 3.

Rationale:

- The UX is screen-state driven: grouped lists, chips, bottom sheets, dialogs, snackbars, progress, and checklists.
- Material 3 gives accessible defaults for color roles, typography, touch targets, state layers, and motion.
- Compose keeps state ownership clear and pairs well with ViewModel + StateFlow.
- Compose theming makes palette swaps easier than custom views.

Avoid custom views unless a later spike proves a Compose component cannot satisfy accessibility or performance needs.

## Version baseline

Version facts were checked from Maven/Gradle metadata during this task. Prefer stable releases, not alpha/beta/RC versions, unless GitHub Actions proves a compatibility issue.

| Tool/library | Recommended baseline | Notes |
| --- | --- | --- |
| Gradle Wrapper | 9.5.1 | Current Gradle release from services.gradle.org. Commit wrapper once the Android skeleton is created. |
| Android Gradle Plugin | 9.2.1 | Latest stable metadata observed; avoid 9.3 alpha line. |
| Kotlin Android plugin | 2.3.21 | Latest stable metadata observed; avoid 2.4 RC line. |
| Compose BOM | 2026.05.01 | Use BOM instead of pinning individual Compose artifacts. |
| Material 3 Compose | from Compose BOM | Use androidx.compose.material3:material3. |
| Navigation Compose | 2.9.8 | Latest stable metadata observed; avoid 2.10 alpha line. |
| Lifecycle ViewModel Compose | 2.10.0 | Lifecycle-aware ViewModel and state collection support. |
| DataStore | 1.2.1 | Backing store for the JSON document. |
| Kotlinx Serialization JSON | 1.11.0 | JSON encoding/decoding and migrations. |
| Kotlinx Coroutines Android | 1.11.0 | ViewModel async work and DataStore flows. |
| JDK on CI | Temurin 17 initially | Raise only if selected AGP requires it. |

## Package layout

Play Store applicationId: com.gusanitolabs.packly

Source namespace/package: com.dobyllm.packly. The Android Gradle `namespace`
controls generated code and source imports, while `applicationId` is the Play
Store package identity installed on devices. Keep the source namespace stable
unless the team intentionally schedules a broad Kotlin package rename.

```text
app/src/main/java/com/dobyllm/packly/
  PacklyApp.kt
  MainActivity.kt
  core/
    model/
    time/
    validation/
  data/
    json/
    migration/
    repository/
    seed/
  domain/
    repository/
    usecase/
  feature/
    home/
    items/
    lists/
    trips/
    packing/
  navigation/
  ui/
    component/
    theme/
    token/
```

Boundary rules:

- feature/* can depend on domain, core, navigation, and ui, but not directly on JSON files or DataStore.
- domain/* can depend on core/model but not Compose, Android UI, DataStore, or serializers.
- data/* implements domain/repository interfaces and owns JSON serialization details.
- ui/theme and ui/token own colors, typography, shape, spacing, motion, and category style mapping.
- Navigation routes should use stable IDs and route objects, not raw string concatenation scattered through screens.

## Separation of concerns

- Screens render state and send events only.
- ViewModels combine repository flows, local filters, and form drafts into immutable screen state.
- Use cases enforce business rules: trip snapshot creation, duplicate warnings, item archival, validation, and reset packing state.
- Repositories are the only writers to persisted app state.
- Theme/category token objects centralize all palette, icon-key, spacing, shape, and motion values.

## Tech stack alignment

This stack fits the long-term goal because:

- Compose + Material 3 supports modern, smooth, accessible, retheme-ready UI without maintaining custom views.
- StateFlow gives predictable refreshes for grouped lists, checklist toggles, progress indicators, and undo flows.
- JSON persistence keeps local session state readable and migration-friendly across app updates.
- Clean boundaries allow later sync, backup, sharing, import/export, or Room migration without rewriting UI.
- GitHub Actions-only validation honors the project rule that Android compile/build/lint/test must not run locally.

## Explicit non-goals for the first implementation

- No network sync.
- No local database unless JSON/DataStore proves insufficient.
- No multiplatform abstraction.
- No custom design system outside Material 3 tokens.
- No drag/reorder until core packing flows are stable.

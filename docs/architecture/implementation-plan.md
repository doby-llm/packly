# Packly Implementation Plan

> **For Hermes:** Use subagent-driven-development skill to implement this plan task-by-task with Codex-backed workers only. Do not use OpenHands, OpenCode, or weaker fallback models.

**Goal:** Build the fresh Packly Android app as a modern Jetpack Compose + Material 3 packing companion with JSON-style local persistence.

**Architecture:** Use Clean Architecture with a light Hexagonal persistence boundary: Compose UI and ViewModels call domain use cases/repository interfaces; DataStore JSON implementations live behind those interfaces. Trips snapshot list entries so packing sessions remain stable after item/list edits.

**Tech Stack:** Android, Kotlin, Jetpack Compose, Material 3, Navigation Compose, Lifecycle ViewModel, Coroutines, DataStore, Kotlinx Serialization JSON, GitHub Actions-only validation.

---

## Non-negotiable project rules

- Repository: /home/mflova/packly only.
- Ignore packly-deprecated; do not copy code from it.
- Never compile/build locally.
- Do not run ./gradlew assemble*, build, lint, test, connectedAndroidTest, or Android compilation locally.
- Validation must happen through GitHub Actions only.
- Workers cannot push; commit locally and block with review-required: needs git push.
- Use Codex-backed workflows only.
- Keep UI modern, colorful, smooth, performant, accessible, and easy to retheme.

## Task 1: Create Android project skeleton

**Objective:** Add a minimal Android app project structure without implementing feature screens.

**Files:**

- Create: settings.gradle.kts
- Create: build.gradle.kts
- Create: gradle/libs.versions.toml
- Create: app/build.gradle.kts
- Create: app/src/main/AndroidManifest.xml
- Create: app/src/main/java/com/dobyllm/packly/MainActivity.kt

**Steps:**

1. Add Gradle version catalog using stable baselines from docs/architecture/tech-stack.md.
2. Configure Compose, Kotlin serialization plugin, Material 3, Navigation Compose, Lifecycle ViewModel Compose, DataStore, Coroutines, and Kotlinx Serialization JSON.
3. Add a placeholder MainActivity that renders “Packly” and does not depend on unfinished feature code.
4. Verify locally only with git status --short and git diff --check.
5. Commit: git commit -m "chore: add android project skeleton".

## Task 2: Add core domain models

**Objective:** Define serializable domain models for categories, items, lists, trips, settings, and session state.

**Files:**

- Create: app/src/main/java/com/dobyllm/packly/core/model/Ids.kt
- Create: app/src/main/java/com/dobyllm/packly/core/model/Category.kt
- Create: app/src/main/java/com/dobyllm/packly/core/model/Item.kt
- Create: app/src/main/java/com/dobyllm/packly/core/model/PackingList.kt
- Create: app/src/main/java/com/dobyllm/packly/core/model/Trip.kt
- Create: app/src/main/java/com/dobyllm/packly/core/model/Settings.kt
- Create: app/src/main/java/com/dobyllm/packly/core/model/PacklyAppDocument.kt

**Steps:**

1. Write model classes from docs/architecture/data-model.md.
2. Keep core/model independent of Compose and Android UI packages.
3. Verify locally only with git status --short and git diff --check.
4. Commit: git commit -m "feat: add packly domain models".

## Task 3: Add theme tokens and design system layer

**Objective:** Implement the Material 3 theme and category token map from UX docs.

**Files:**

- Create: app/src/main/java/com/dobyllm/packly/ui/theme/Color.kt
- Create: app/src/main/java/com/dobyllm/packly/ui/theme/Theme.kt
- Create: app/src/main/java/com/dobyllm/packly/ui/theme/Type.kt
- Create: app/src/main/java/com/dobyllm/packly/ui/token/CategoryTokens.kt
- Create: app/src/main/java/com/dobyllm/packly/ui/token/Spacing.kt
- Create: app/src/main/java/com/dobyllm/packly/ui/token/Motion.kt

**Steps:**

1. Create semantic Material 3 color tokens from docs/ux/design-system.md.
2. Map stable category keys to icon keys, accent colors, and soft colors.
3. Add PacklyTheme with system light/dark support; keep dynamic color optional for later.
4. Verify locally only with git status --short and git diff --check.
5. Commit: git commit -m "feat: add packly theme tokens".

## Task 4: Add seed data provider

**Objective:** Create default categories, 24+ seed items, and starter lists.

**Files:**

- Create: app/src/main/java/com/dobyllm/packly/data/seed/SeedDataProvider.kt

**Steps:**

1. Define category keys from docs/architecture/data-model.md.
2. Define seed items from docs/ux/packly-ux-concept.md with category IDs, icon keys, and default quantity.
3. Add starter lists: Weekend, Beach day, Business trip, Camping, Family visit.
4. Verify locally only with git status --short and git diff --check.
5. Commit: git commit -m "feat: add default packly seed data".

## Task 5: Add JSON persistence adapter

**Objective:** Implement versioned JSON persistence behind repository interfaces.

**Files:**

- Create: app/src/main/java/com/dobyllm/packly/domain/repository/PacklyRepository.kt
- Create: app/src/main/java/com/dobyllm/packly/data/json/PacklyJson.kt
- Create: app/src/main/java/com/dobyllm/packly/data/json/PacklySerializer.kt
- Create: app/src/main/java/com/dobyllm/packly/data/migration/PacklyJsonMigration.kt
- Create: app/src/main/java/com/dobyllm/packly/data/repository/DataStorePacklyRepository.kt

**Steps:**

1. Define repository interface with flows and transactional update methods.
2. Define JSON serializer with ignoreUnknownKeys, encodeDefaults, explicitNulls = false.
3. Seed empty documents on first launch.
4. Verify locally only with git status --short and git diff --check.
5. Commit: git commit -m "feat: add json datastore repository".

## Task 6: Add navigation shell

**Objective:** Wire app-level navigation and placeholder screens for top-level destinations.

**Files:**

- Create: app/src/main/java/com/dobyllm/packly/navigation/PacklyRoute.kt
- Create: app/src/main/java/com/dobyllm/packly/navigation/PacklyNavHost.kt
- Create: app/src/main/java/com/dobyllm/packly/feature/home/HomeScreen.kt
- Create: app/src/main/java/com/dobyllm/packly/feature/items/ItemsScreen.kt
- Create: app/src/main/java/com/dobyllm/packly/feature/lists/ListsScreen.kt
- Create: app/src/main/java/com/dobyllm/packly/feature/trips/TripsScreen.kt
- Modify: app/src/main/java/com/dobyllm/packly/MainActivity.kt

**Steps:**

1. Define typed routes with stable IDs for detail routes.
2. Add PacklyNavHost for Home, Items, Lists, Trips, TripDetail, ListDetail, and PackingMode placeholders.
3. Connect MainActivity and wrap navigation in PacklyTheme.
4. Verify locally only with git status --short and git diff --check.
5. Commit: git commit -m "feat: add compose navigation shell".

## Task 7: Build shared UI components

**Objective:** Add reusable Material 3 components before feature-specific screens.

**Files:**

- Create: app/src/main/java/com/dobyllm/packly/ui/component/CategoryHeader.kt
- Create: app/src/main/java/com/dobyllm/packly/ui/component/ItemRow.kt
- Create: app/src/main/java/com/dobyllm/packly/ui/component/ListCard.kt
- Create: app/src/main/java/com/dobyllm/packly/ui/component/TripCard.kt
- Create: app/src/main/java/com/dobyllm/packly/ui/component/EmptyState.kt
- Create: app/src/main/java/com/dobyllm/packly/ui/component/PacklyProgress.kt

**Steps:**

1. Implement stateless components that receive state and callbacks.
2. Add accessibility labels; checklist rows announce item name, category, quantity, and packed state.
3. Verify locally only with git status --short and git diff --check.
4. Commit: git commit -m "feat: add shared compose components".

## Task 8: Implement Items feature

**Objective:** Add item browsing, category filtering, add/edit form, duplicate warning, and archive behavior.

**Files:**

- Create: app/src/main/java/com/dobyllm/packly/feature/items/ItemsViewModel.kt
- Create: app/src/main/java/com/dobyllm/packly/feature/items/ItemsState.kt
- Create: app/src/main/java/com/dobyllm/packly/feature/items/EditItemSheet.kt
- Modify: app/src/main/java/com/dobyllm/packly/feature/items/ItemsScreen.kt

**Steps:**

1. Add state/events for loading, search, selected category, grouped sections, dialogs, and errors.
2. Add ViewModel that reads repository flow, applies filters, and handles add/edit/archive events.
3. Add Compose screen with category chips, grouped LazyColumn, FAB, bottom sheet, and Snackbar undo.
4. Validate through GitHub Actions after orchestrator push; do not run local Gradle commands.
5. Commit: git commit -m "feat: implement items feature".

## Task 9: Implement Lists feature

**Objective:** Add packing list overview, detail editor, duplication, and list-entry management.

**Files:**

- Create: app/src/main/java/com/dobyllm/packly/feature/lists/ListsViewModel.kt
- Create: app/src/main/java/com/dobyllm/packly/feature/lists/ListsState.kt
- Create: app/src/main/java/com/dobyllm/packly/feature/lists/ListDetailScreen.kt
- Create: app/src/main/java/com/dobyllm/packly/feature/lists/AddItemsToListSheet.kt
- Modify: app/src/main/java/com/dobyllm/packly/feature/lists/ListsScreen.kt

**Steps:**

1. Add list overview state with item count and top category icons.
2. Add list detail editor grouped by category with add/remove/quantity updates.
3. Preserve snapshot semantics in list entries.
4. Validate through GitHub Actions after orchestrator push; do not run local Gradle commands.
5. Commit: git commit -m "feat: implement lists feature".

## Task 10: Implement Trips and Packing mode

**Objective:** Add trip creation, trip overview/detail, focused packing checklist, progress, and undo.

**Files:**

- Create: app/src/main/java/com/dobyllm/packly/feature/trips/TripsViewModel.kt
- Create: app/src/main/java/com/dobyllm/packly/feature/trips/TripsState.kt
- Create: app/src/main/java/com/dobyllm/packly/feature/trips/CreateTripScreen.kt
- Create: app/src/main/java/com/dobyllm/packly/feature/trips/TripDetailScreen.kt
- Create: app/src/main/java/com/dobyllm/packly/feature/packing/PackingModeScreen.kt
- Create: app/src/main/java/com/dobyllm/packly/feature/packing/PackingViewModel.kt
- Modify: app/src/main/java/com/dobyllm/packly/feature/trips/TripsScreen.kt

**Steps:**

1. Add trip creation use case that snapshots selected list entries into trip entries.
2. Add trip overview/detail with active trips first, completed collapsed, progress, and primary action.
3. Add packing mode with stable keys, category grouping, filters, progress text, checkbox rows, and Snackbar undo.
4. Add “Everything is packed” completion state with review/finish actions.
5. Validate through GitHub Actions after orchestrator push; do not run local Gradle commands.
6. Commit: git commit -m "feat: implement trips and packing mode".

## Task 11: Enable GitHub Actions workflow

**Objective:** Add the active Android CI workflow only after the Gradle project exists.

**Files:**

- Create: .github/workflows/android-ci.yml
- Modify: README.md

**Steps:**

1. Create workflow from docs/ci/github-actions-plan.md, adjusting task names only if actual Gradle project files require it.
2. Add APK retrieval instructions to README.
3. Commit: git commit -m "ci: enable android github actions".
4. Block for orchestrator push with review-required: needs git push.

## Bite-sized coder task checklist

Use these as the actual dispatch units if a worker needs smaller steps than the feature tasks above. Each unit should end with git diff --check and a local commit; Android validation still happens only in GitHub Actions after push.

1. Add settings.gradle.kts and root build.gradle.kts.
2. Add gradle/libs.versions.toml with stable versions from tech-stack.md.
3. Add app/build.gradle.kts and AndroidManifest.xml.
4. Add placeholder MainActivity and PacklyTheme wrapper.
5. Add ID value classes/type aliases.
6. Add Category and Item models.
7. Add PackingList and PacklyListEntry models.
8. Add Trip and TripEntry models.
9. Add Settings, SessionState, and PacklyAppDocument models.
10. Add semantic light/dark color tokens.
11. Add category token map from design-system.md.
12. Add spacing, shape, and motion constants.
13. Add SeedDataProvider categories.
14. Add SeedDataProvider item library seeds.
15. Add SeedDataProvider starter lists.
16. Add PacklyRepository interface.
17. Add JSON configuration and serializer shell.
18. Add migration interface and schemaVersion handling.
19. Add DataStore repository implementation.
20. Add typed route contract.
21. Add PacklyNavHost with placeholder destinations.
22. Add Home placeholder screen with destination cards.
23. Add Items placeholder screen and state/event contracts.
24. Add Lists placeholder screen and state/event contracts.
25. Add Trips placeholder screen and state/event contracts.
26. Add shared CategoryHeader component.
27. Add shared ItemRow component with accessibility text.
28. Add shared ListCard and TripCard components.
29. Add EmptyState and progress components.
30. Implement Items ViewModel filtering/grouping.
31. Implement add/edit item bottom sheet.
32. Implement item archive/delete confirmation and undo.
33. Implement list overview cards.
34. Implement list detail grouped editor.
35. Implement add-items-to-list sheet.
36. Implement create-trip use case that snapshots list entries.
37. Implement trips overview/detail.
38. Implement packing mode filtering and stable-key checklist.
39. Implement packed toggle undo and completion panel.
40. Enable GitHub Actions workflow after Gradle project exists.
41. Update README with CI/APK retrieval instructions.

## Acceptance criteria for coder handoff

- Architecture docs are committed before implementation starts.
- App code uses Compose + Material 3.
- Package boundaries match docs/architecture/tech-stack.md.
- JSON persistence and migrations match docs/architecture/persistence.md.
- Trips snapshot list entries.
- Theme/category tokens are centralized.
- Grouped lists use stable keys and avoid avoidable recomposition.
- Local workers never run Android build/lint/test/assemble commands.
- GitHub Actions becomes the only build/test/APK validation path once app code exists.

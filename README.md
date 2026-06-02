# Packly

Packly is a fresh Android packing companion built with Jetpack Compose, Material 3, and JSON-style local persistence. It helps people manage reusable items, build packing list templates, and turn those templates into trip-specific packing checklists.

## What is implemented

- Home / main menu with three top-level options: Edit items, Item Lists, and Trips.
- Seeded category system with Material-style icons, colors, and 27 default items.
- Seeded starter lists: Weekend, Beach day, Business trip, Camping, and Family visit.
- Items screen with search, grouped category sections, add-item form, and soft-delete/archive behavior.
- Lists screen with create/delete, list detail editing, and checklist-based item selection.
- Trips screen with trip creation from a list and/or isolated items, duplicate ignoring, progress, trip details, reset, and focused packing mode.
- Versioned `PacklyAppDocument` persisted through DataStore with Kotlinx Serialization JSON.
- Centralized theme, category, spacing, and motion tokens for retheming.

## Project rules

Do not run local Android compilation, lint, tests, APK assembly, or connected tests for this repository. Validation is intentionally performed by GitHub Actions only.

Forbidden local commands include:

- `./gradlew build`
- `./gradlew assemble*`
- `./gradlew lint*`
- `./gradlew test*`
- `./gradlew connectedAndroidTest`

Local contributors may inspect files, review diffs, and run Git-only checks such as `git diff --check`.

## CI / APK validation

The active workflow is `.github/workflows/android-ci.yml`.

It runs on GitHub-hosted runners and performs:

1. Gradle Wrapper validation.
2. Android lint.
3. JVM unit tests.
4. Debug APK assembly on pull requests, pushes to `main`, and `workflow_dispatch`.
5. Upload of the debug APK artifact named `packly-debug-apk`.

After a successful workflow run with an uploaded APK:

```bash
gh run list --workflow android-ci.yml --limit 10
gh run download <RUN_ID> --name packly-debug-apk --dir ./artifacts/apk
```

## Architecture docs

See:

- `docs/architecture/tech-stack.md`
- `docs/architecture/data-model.md`
- `docs/architecture/navigation-and-state.md`
- `docs/architecture/persistence.md`
- `docs/ux/packly-ux-concept.md`
- `docs/ux/design-system.md`

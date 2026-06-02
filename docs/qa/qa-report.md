# Packly QA Report

Task: `t_7b66f9b9`
Repository: `/home/mflova/packly` (`doby-llm/packly`)
Reviewed commit: `52dd0b6 feat: implement fresh Packly Android app`
Date: 2026-06-02

## Validation boundaries

- Local Android compilation/build/lint/test commands were not run, per project rule.
- Local checks performed: file inspection, static code review, `git diff --check`, repository status/log inspection, and GitHub Actions metadata lookup through the public GitHub API.
- GitHub Actions run inspected: `26801748076` (`Android CI`, push to `main`, head `52dd0b6`) at https://github.com/doby-llm/packly/actions/runs/26801748076.

## Executive verdict

Status: FAIL / BLOCKED.

The app meets several product-direction requirements on paper: it is a fresh Jetpack Compose + Material 3 Android app, has JSON-style DataStore persistence, includes 27 seeded items, uses 10 category tokens with icons/colors, and implements the main Items, Lists, Trips, and Packing flows.

However, merge/release should remain blocked because GitHub Actions failed in `Run Android lint`, unit tests were skipped, and the debug APK job was skipped. There are also functional UX gaps versus the architecture/UX requirements: item editing is wired to a no-op, duplicate handling/warnings are missing for user-created items/lists/trips, destructive actions do not ask for confirmation or offer undo, persistence corruption/migration behavior can silently reset data, and list/trip creation only exposes the first 12 items.

## GitHub Actions / CI results

| Check | Result | Evidence |
| --- | --- | --- |
| Workflow triggered on push to `main` | PASS | Run `26801748076`, event `push`, branch `main`, head `52dd0b6`. |
| Gradle Wrapper validation | PASS | Job `79009729667`, step `Validate Gradle Wrapper`, conclusion `success`. |
| Android lint | FAIL | Job `79009729667`, step `Run Android lint`, conclusion `failure`, process exit code 1 on GitHub page. |
| JVM unit tests | NOT RUN | Step `Run JVM unit tests` was skipped because lint failed first. |
| Debug APK assembly/artifact | NOT RUN | Job `79009809887 Build debug APK` was skipped because `quality` failed. |
| APK artifact availability | FAIL | No successful APK job/artifact is available from this run. |

Note: unauthenticated API access exposed workflow/job/step conclusions, but not job logs (`actions/jobs/79009729667/logs` returned 403). A remediation worker with GitHub auth should inspect the failing lint log before patching.

## Requirements checklist

| Requirement | Result | Notes |
| --- | --- | --- |
| Use repository `/home/mflova/packly`; ignore deprecated repo | PASS | Review stayed in `/home/mflova/packly`; no deprecated paths inspected or copied. |
| Fresh start, no copied deprecated code | PASS by inspection | No evidence of deprecated references in inspected app/docs. |
| Never compile/build locally | PASS | No Gradle/Android build/lint/test commands were run locally. |
| Prefer Jetpack Compose unless reason not to | PASS | UI is Compose (`MainActivity`, `PacklyNavHost`, feature screens), docs explain Compose + Material 3. |
| Modern Material Design, colorful, retheme-ready | PARTIAL | Uses Material 3, theme colors, category tokens, spacing/motion tokens. Some UX polish is missing: no destructive confirmations, undo snackbars, edit flows, or responsive max-width treatment. |
| JSON-style persistence suitable for sessions/app updates | PARTIAL | DataStore + Kotlinx JSON exists, but serializer silently falls back to default seeds on any read error and migration runner is unused/no-op. Session state model exists but no meaningful session state is saved. |
| Pretty category icons and 20-30 default seed items | PASS | 10 Material Rounded category icons and 27 default items are present. |
| Grouping by categories | PASS | Items and packing mode group entries by sorted categories. |
| List/trip flows | PARTIAL | Lists can be created, opened, archived, and used for trips; trips can be created, opened, reset, archived, and packed. Missing rename/edit metadata, confirmation, and full-library item selection. |
| Packing mode | PASS with gaps | Focused checklist, progress, All/Unpacked/Packed filters, stable entry keys, completion panel. Missing undo and optimistic/error handling. |
| Quantities | PASS | Item defaults, list entry quantities, and trip entry quantities are snapshotted and displayed. No UI to edit quantities after creation. |
| Duplicate handling | PARTIAL/FAIL | Trip creation deduplicates source list + isolated selected items, but no duplicate warning; add item/create list/create trip allow duplicate names. |
| Compose performance | PARTIAL | LazyColumn and stable item keys are used. Filtering/grouping is recomputed in composables on every recomposition, all state is a full-document flow, and DataStore writes the whole document for each packing toggle. Acceptable for MVP size, risky as data grows. |
| Theming extensibility | PASS with gaps | Theme/category/spacing/motion tokens are centralized. Category colors are in tokens, not individual screens. Dark scheme is less complete than light scheme. |
| Docs alignment | PARTIAL | README matches current implementation and CI intent. Some architecture docs describe future/ideal boundaries (feature ViewModels/use cases/typed routes/migrations/undo) that are not fully implemented. |

## Critical blockers

1. CI is failing before unit tests/APK assembly.
   - Evidence: GitHub Actions run `26801748076`, `Lint and unit tests` job failed at `Run Android lint`; `Run JVM unit tests` skipped; `Build debug APK` skipped.
   - Impact: no validated build, no test signal, no APK artifact.
   - Required remediation: authenticated devops/coder worker must inspect the lint log and patch the failing issue, then push and re-run CI. Do not run local Gradle tasks.

2. Persistence read errors silently replace user data with seed defaults.
   - Evidence: `PacklySerializer.readFrom` catches any exception and returns `defaultValue`; `PacklyJsonMigration` exists but is not invoked.
   - Impact: corrupted or future-version JSON could appear as data loss with no backup/error path, conflicting with `docs/architecture/persistence.md` lines 83-90.
   - Required remediation: introduce corruption handling/migration flow that preserves bad data when possible and surfaces a recoverable UI state, or explicitly document MVP limitation.

3. Item editing is advertised but not implemented.
   - Evidence: `ItemsScreen` passes `onEdit = {}` to `ItemRow`; README says Items screen has add-item form and soft-delete/archive behavior, while design/architecture expect add/edit/archive.
   - Impact: users see edit icons that do nothing.
   - Required remediation: either implement edit item behavior or remove/disable edit affordance until implemented.

## Major functional gaps

- Duplicate handling/warnings are incomplete.
  - `createTrip` deduplicates list + isolated items via `distinctBy`, but it silently ignores duplicates without warning.
  - `addItem`, `createList`, and `createTrip` do not reject or warn on duplicate names.

- Destructive actions lack confirmation and undo.
  - Item delete/archive, list delete/archive, trip delete/archive, and reset packing happen immediately.
  - This conflicts with design-system requirements for destructive dialogs and snackbar undo.

- Selection sheets expose only the first 12 items.
  - `CreateListSheet` and `CreateTripSheet` use `doc.items.filterNot { it.isArchived }.take(12)`.
  - Users with more than 12 items cannot select the rest from those flows, despite seeded data containing 27 items.

- Quantity editing is missing from user flows.
  - Quantities are stored and displayed, but list/trip creation only snapshots item defaults; users cannot adjust list or trip quantities in the UI.

- Architecture boundaries are thinner than documented.
  - `PacklyAppViewModel` centralizes all feature mutations; feature ViewModel files are placeholders.
  - There are no use cases for validation, duplicate warnings, trip snapshot creation, or reset confirmation.
  - This is acceptable for a small MVP if documented, but it does not fully match the architecture blueprint.

## Edge case audit

- Empty/blank names: UI disables blank list/trip saves and item sheet likely validates, but ViewModel methods do not enforce blank-name validation if invoked from another caller.
- Duplicate item/list/trip names: allowed with no warning, causing ambiguous chips/cards and duplicate list/trip creation.
- Archived source items: existing list/trip snapshots remain visible, which is good; adding archived items is prevented by UI filters.
- Archived source list used by route/backstack: detail screen can still render an archived list if navigated by ID; overview hides it.
- Corrupt JSON: resets to seed defaults with no user-facing warning or backup.
- Future schema versions/unknown fields: JSON config likely ignores unknown keys, but migrations are not applied and unsupported required-shape changes fall into silent reset.
- Rapid packing toggles: every tap writes the full document to DataStore; no optimistic UI, debounce, snackbar undo, or error recovery.
- Large item library: grouping/filtering is recomputed in composables and list/trip creation sheets only show first 12 items.
- Zero-entry trip: possible when creating a blank trip with no items; progress avoids divide-by-zero in cards but packing/detail UX may feel odd.
- Route IDs with special characters: generated IDs are safe; route helpers do not encode IDs, so future imported/custom IDs could break navigation.
- Accessibility: packing rows expose useful content descriptions; destructive/reset actions lack confirmation semantics, and progress should be verified by TalkBack after CI/build is green.

## Testing strategy

Unit tests to add/run in CI:
- Seed data invariants: category count/order, item count 20-30, all seeded entries reference real items/categories, stable IDs.
- Trip creation use case: source-list snapshots, isolated items, duplicate item handling, quantities/notes snapshots, zero-entry trip policy.
- Validation rules: blank names, duplicate names, invalid quantities, archived item/list behavior.
- JSON serializer/migration: unknown keys, defaults, schema version detection, corrupt JSON handling, no silent data loss.
- Packing progress: toggle packed/unpacked, reset all, packedAt timestamp behavior.

Integration tests to add/run in CI:
- Repository DataStore update transactions for add/archive/create/toggle flows.
- Persistence round-trip for a populated document with items/lists/trips/session settings.
- Navigation route tests for list detail/trip detail/packing mode with missing IDs and encoded IDs.

E2E/UI tests to add/run in GitHub Actions only:
- Fresh launch shows seeded counts and starter categories/lists.
- Add item -> appears under category -> archive item -> hidden from active lists.
- Create list from a selected item set -> open detail -> toggle membership.
- Create trip from list + isolated items -> duplicate ignored with visible warning/notice.
- Packing mode: filter All/Unpacked/Packed, toggle items, reset with confirmation, completion panel.
- Accessibility smoke: TalkBack labels for packing rows, progress text, destructive dialogs.

## Static analysis / formatting recommendations

Minimum checks before merge in GitHub Actions:
- Android lint: keep `./gradlew --no-daemon lintDebug` as required and fix current failure.
- JVM unit tests: keep `./gradlew --no-daemon testDebugUnitTest` as required.
- Kotlin formatting: add ktlint or Spotless with ktlint and enforce trailing commas/line wrapping/import ordering.
- Static analysis: add Detekt with Compose-friendly rules; enable complexity, magic number, long method, too many functions, and coroutine rules with practical thresholds.
- Dependency/version checks: add Gradle dependency lock or version catalog review; consider Dependabot for Gradle and GitHub Actions.
- Secrets scan: add gitleaks or GitHub secret scanning if available.
- Markdown lint: run markdownlint on docs to keep QA/architecture/UX docs consistent.

Suggested Detekt focus:
- `LongMethod`, `ComplexMethod`, `NestedBlockDepth` for ViewModel and screen composables.
- `TooGenericExceptionCaught` for serializer/corruption paths.
- `MagicNumber` with excludes for Compose dp/token files.
- `UnusedPrivateMember` and `ForbiddenComment`.
- Coroutines rules for ViewModel launches and dispatcher usage.

Suggested ktlint/Spotless focus:
- Official Kotlin style.
- No wildcard imports except where explicitly allowed by project convention.
- Max line length with Compose-friendly exceptions.
- Consistent trailing commas in multiline declarations.

## CI/CD merge gate recommendations

Do not merge or release until:
1. Android CI is green on `main` or the target PR branch.
2. `lintDebug` succeeds.
3. `testDebugUnitTest` succeeds and includes more than seed-only tests.
4. `:app:assembleDebug` succeeds on GitHub Actions and uploads `packly-debug-apk`.
5. The current lint failure is understood from authenticated logs and documented in the remediation handoff.
6. No local Gradle/Android validation has been substituted for CI.

Recommended workflow additions:
- A separate docs/static job for markdownlint and YAML validation.
- Branch protection requiring the Android CI workflow before merge.
- Artifact retention confirmation and checksum publication for downloaded APKs.
- Dependabot updates for Gradle libs and GitHub Actions.

## Suggested remediation tasks

Coder remediation:
- Inspect authenticated CI lint logs for run `26801748076` and fix the lint failure without local Gradle validation.
- Implement or remove the no-op item edit affordance.
- Add duplicate-name validation/warnings for items, lists, and trips; make silent trip duplicate removal visible to users.
- Replace `.take(12)` selectors with searchable/full-list selectors for list/trip creation.
- Add destructive confirmations and/or snackbar undo for archive/delete/reset actions.
- Add quantity editing for list/trip entries or document it as out of MVP scope.
- Improve persistence corruption/migration behavior so user data is not silently reset.

DevOps remediation:
- Ensure GitHub auth/log access is available to workers that must diagnose failed Actions logs.
- Re-run Android CI after fixes and confirm lint, unit tests, debug APK assembly, and artifact upload.
- Add branch protection / required status checks once CI is green.
- Add markdown/static-analysis jobs after the Android pipeline is stable.

## Final QA decision

Blocked. The implementation is a promising MVP foundation, but the failed GitHub Actions run and the user-visible gaps above must be remediated before Packly can be considered ready for merge/release validation.

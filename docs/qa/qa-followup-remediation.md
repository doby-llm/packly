# Packly QA Follow-up After Remediation

Task: `t_3e46207c`
Repository: `/home/mflova/packly` (`doby-llm/packly`)
Remediation commit reviewed: `f2c4e6a fix: unblock Android CI and QA blockers`
GitHub Actions run: `26802512340` (`Android CI`, push to `main`, head `f2c4e6a02c7edc022e10ddea396713f9d3ef5b57`)
Run URL: https://github.com/doby-llm/packly/actions/runs/26802512340

## Validation boundaries

- Local Android compilation/build/lint/test commands were not run, per project rule.
- Local checks performed: repository/file inspection, remediation diff review, `git diff --check`, public GitHub Actions API lookup, public GitHub Actions job page/annotation inspection, and Gradle distribution URL HEAD check.
- GitHub Actions logs could not be downloaded without admin/authenticated repository access; the public job page exposes only annotations and step conclusions.

## Executive verdict

Status: FAIL / BLOCKED.

The remediation commit fixes or meaningfully addresses several QA blockers from `docs/qa/qa-report.md`: the malformed Gradle wrapper URL is corrected, item editing is wired, list/trip item selectors now search the full active library, duplicate-name validation/warnings exist for primary create/edit flows, duplicate trip-source overlap is surfaced, and high-risk archive/reset actions now require confirmation. The persistence-corruption behavior is not fixed in code, but the MVP limitation is now explicitly documented.

However, the new GitHub Actions run still fails in `Run Android lint`; JVM unit tests are skipped and debug APK assembly/artifact upload are skipped. Release/merge validation remains blocked until an authenticated worker inspects the lint logs, fixes the current lint failure, pushes the fix, and confirms lint, unit tests, and debug APK jobs all pass in GitHub Actions.

## GitHub Actions / CI results

| Check | Result | Evidence |
| --- | --- | --- |
| Workflow triggered on push to `main` | PASS | Run `26802512340`, event `push`, branch `main`, head `f2c4e6a02c7edc022e10ddea396713f9d3ef5b57`. |
| Gradle wrapper URL syntax | PASS by inspection | `gradle/wrapper/gradle-wrapper.properties` now uses `distributionUrl=https://services.gradle.org/distributions/gradle-9.5.1-bin.zip`; a local HEAD check followed redirects to HTTP 200. |
| Gradle Wrapper validation step | PASS | Job `79012119390`, step `Validate Gradle Wrapper`, conclusion `success`. |
| Android lint | FAIL | Job `79012119390`, step `Run Android lint`, conclusion `failure`; public annotation says `Process completed with exit code 1.` |
| JVM unit tests | NOT RUN | Step `Run JVM unit tests` was skipped because lint failed first. |
| Debug APK assembly/artifact | NOT RUN | Job `79012267807 Build debug APK` was skipped because the `quality` job failed. |
| APK artifact availability | FAIL | Public artifacts API returned `total_count: 0`. |
| Workflow platform health | WARNING | Public annotation reports Node.js 20 actions deprecation for `actions/checkout@v4`, `actions/setup-java@v4`, `gradle/actions/setup-gradle@v4`, and `gradle/actions/wrapper-validation@v4`. |

## Remediation review against prior QA blockers

| Prior blocker / gap | Follow-up status | Evidence / notes |
| --- | --- | --- |
| Gradle wrapper URL failure | PASS for the known URL bug | The previous `https\\://...` value is now `https://...`; the distribution URL resolves through redirects to HTTP 200. CI now reaches the lint command, but the lint command still exits 1 for a currently unknown reason. |
| Item editing no-op | PASS by static review | `ItemsScreen` now tracks `itemToEdit`, opens `EditItemSheet` with the selected item, and passes `onUpdate = vm::updateItem`; `PacklyAppViewModel.updateItem` updates name/category/default quantity/notes. |
| Full-library selectors | PASS by static review | `CreateListSheet` and `CreateTripSheet` no longer use `.take(12)` and instead filter all active items through a search field. |
| Duplicate validation/warnings | PARTIAL/PASS for UI flows | Add/edit item, create list, and create trip now show duplicate-name supporting text and disable save. ViewModel methods also guard against active duplicate names. Trip creation now warns when selected isolated items already exist in the selected source list. Remaining caveat: direct ViewModel duplicate attempts silently no-op instead of returning a domain validation error to the UI. |
| Destructive confirmations / undo | PARTIAL | Item/list/trip archive and trip reset now use `AlertDialog` confirmation. Undo snackbars are still deferred, and list-detail item removal/toggle remains immediate. |
| Persistence corruption/migration behavior | DOCUMENTED, NOT CODE-FIXED | `docs/architecture/persistence.md` now states the current MVP limitation: `PacklySerializer` still falls back to the seeded document on decode failure and does not preserve the bad payload or show a recovery banner. The serializer code still returns `defaultValue` on read failure. |
| Quantity editing after list/trip creation | REMAINS OPEN | Item default quantity can be edited, but list/trip entry quantities are still snapshots with no post-creation editing UI. |
| Architecture boundary mismatch | REMAINS ACCEPTED MVP GAP | `PacklyAppViewModel` remains the central mutation surface; feature ViewModels/use cases are still not implemented. |

## Remaining blockers

1. CI is still red after remediation.
   - Evidence: run `26802512340` concludes `failure`; `Run Android lint` exits 1; unit tests and APK job are skipped.
   - Impact: no successful lint signal, no unit test signal, and no APK artifact.
   - Required action: authenticated coder/devops worker must inspect the lint logs for job `79012119390`, patch the issue without local Gradle validation, push, and monitor the next Actions run.

2. Persistence corruption handling is documented but not safe enough for release.
   - Evidence: `PacklySerializer.readFrom` still uses `runCatching { ... }.getOrElse { defaultValue }`.
   - Impact: corrupt/future-incompatible data can still appear as silent data loss.
   - Required action: implement a recoverable corruption path with backup/preserve behavior and user-facing warning, or keep release blocked until the limitation is explicitly accepted.

3. Undo and some destructive/toggle affordances are incomplete.
   - Evidence: confirmation dialogs were added for archive/reset, but no snackbar undo exists, and list-detail item toggles can remove entries immediately.
   - Impact: accidental destructive edits are less risky than before but still not fully recoverable.

4. CI workflow has a Node.js 20 action deprecation warning.
   - Evidence: GitHub public annotation in job `79012119390` warns that the JavaScript actions in use are running on Node.js 20 and GitHub will force Node.js 24 by default starting 2026-06-16.
   - Impact: not the current failure, but it is a near-term CI reliability risk.

## Recommended next worker tasks

1. Coder remediation: inspect authenticated lint logs for run `26802512340` / job `79012119390`, fix the lint failure without local Gradle/Android commands, commit locally, and block with `review-required: needs git push`.
2. DevOps/orchestrator follow-up: after the lint fix is pushed, monitor the new GitHub Actions run and confirm `lintDebug`, `testDebugUnitTest`, `:app:assembleDebug`, and `packly-debug-apk` artifact upload all pass.
3. Coder/product follow-up before release: implement persistence corruption recovery with bad-payload preservation and user-visible recovery messaging, or capture an explicit product decision accepting the documented MVP limitation.
4. UX/coder follow-up: add snackbar undo for archive/reset flows and decide whether list-detail item removal needs confirmation, undo, or clearer copy.
5. DevOps follow-up: address the GitHub Actions Node.js 20 deprecation warning by updating actions or opting into Node.js 24 once compatibility is confirmed.

## Final QA decision

Blocked. Remediation improved multiple functional QA blockers and appears to resolve the original malformed Gradle wrapper URL issue, but the replacement CI run still fails at Android lint and does not produce unit-test or APK validation.

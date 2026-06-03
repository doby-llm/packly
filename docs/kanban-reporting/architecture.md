# Strict Kanban Reporting Architecture

## Goal

Replace prose-only Packly Kanban status updates with a deterministic reporting pipeline that emits:

1. a strict machine-readable JSON report for the `packly-fresh` board;
2. a polished PDF rendered from the same JSON contract;
3. the existing human summary, with the PDF attached by the cron job.

The design should be reusable from `/home/mflova/packly`, run with `uv`, and avoid hidden state in ad-hoc `~/.hermes` scripts.

## Recommendation

Use deterministic extraction from the Hermes Kanban board plus Pydantic models and JSON Schema validation. Do not introduce Pydantic AI for the first version.

Preferred pattern: Clean Architecture with a small Hexagonal boundary around Hermes/Kanban IO.

```text
Cron job / Hermes prompt
  -> scripts/kanban_report/collect_board.py       # adapter: board/session/cron inputs
  -> packly_tools/kanban_reporting/models.py      # Pydantic contract
  -> packly_tools/kanban_reporting/generator.py   # deterministic aggregation
  -> packly_tools/kanban_reporting/pdf.py         # PDF renderer
  -> reports/kanban/YYYYMMDD-HHMM/                # JSON + PDF artifacts
```

This keeps the LLM out of the correctness path. The LLM may still write the short narrative summary after reading the validated JSON, but it should not invent report data.

## Why not Pydantic AI initially

Pydantic AI is not needed for the core report because the inputs are already structured enough: Kanban task rows, events, comments, run handoffs, git commits, CI status, and artifact metadata. The high-value requirement is repeatability, not semantic interpretation.

Use deterministic extraction plus Pydantic models because it provides:

- stable schema enforcement and generated JSON Schema;
- predictable diffs between report runs;
- easy cron/script testing without model calls;
- lower latency and no LLM failure mode on Raspberry Pi/Linux;
- lower cost and no prompt-injection surface from task comments.

Consider Pydantic AI later only for an optional `insights` enrichment stage, for example clustering vague risks from free-form comments. If added, it must write into a separate optional field such as `llm_insights` and never be required to produce valid counts, blockers, CI status, commits, or artifact links.

## Storage location

Recommended: keep the reusable reporting package in the Packly repository.

```text
/home/mflova/packly/
  pyproject.toml                         # uv-managed Python tool project
  packly_tools/
    kanban_reporting/
      __init__.py
      models.py                          # Pydantic models + JSON Schema export
      generator.py                       # aggregation from normalized inputs
      pdf.py                             # ReportLab rendering
      cli.py                             # `uv run python -m ...`
  scripts/
    kanban_report/
      generate.py                        # cron-friendly entrypoint
  docs/
    kanban-reporting/
      architecture.md                    # this spec
      schema.md                          # optional generated schema docs
  reports/
    kanban/                              # gitignored runtime artifacts
```

Tradeoffs:

| Location | Pros | Cons | Decision |
| --- | --- | --- | --- |
| Packly repo | Versioned with the app, reviewable, testable, reusable by CI/cron, easy to evolve with board conventions | Adds Python tooling to an Android repo | Preferred |
| `~/.hermes/scripts` | Quick to wire into one cron job | Hidden from repo review, hard to reproduce, profile-specific, easy to drift | Avoid except for a tiny launcher if cron cannot call repo scripts directly |
| Hermes skill | Good for reusable agent procedure | Bad place for production report code or artifacts; skills are procedural memory, not app-owned tooling | Document process only, do not store generator here |

Add `reports/kanban/` to `.gitignore`; commit sample fixtures under `tests/fixtures/kanban_reporting/` instead of committing generated reports.

## Separation of concerns

```text
packly_tools/kanban_reporting/
  models.py
    BoardReport, TaskSummary, StatusCounts, ChangedSinceLastUpdate,
    HumanAction, CiStatus, ArtifactRef, CommitRef, Risk, Blocker

  extractors/
    kanban.py
      Reads Hermes Kanban task/export data and normalizes it.
    git.py
      Reads commits since a baseline ref/time.
    ci.py
      Reads GitHub Actions status/artifacts via `gh` or provided JSON.

  generator.py
    Pure transformation from normalized inputs to BoardReport.
    No file IO except through explicit parameters.

  pdf.py
    Pure renderer from BoardReport to PDF path.

  cli.py
    Argument parsing, path handling, JSON/PDF write orchestration.
```

Boundary rules:

- `models.py` owns schema and validation only; no shell calls.
- Extractors depend on external tools (`hermes`, `gh`, `git`) but return typed intermediate objects.
- `generator.py` contains aggregation policy and is covered by fixture tests.
- `pdf.py` does presentation only; it must not recalculate counts.
- The cron job calls the CLI and attaches artifacts; it does not parse board data itself.

## Strict JSON schema shape

The canonical schema should be generated from Pydantic models and committed as a generated artifact only if useful for review. Use JSON Schema draft 2020-12 and forbid additional properties on all objects.

Top-level shape:

```json
{
  "schema_version": "1.0.0",
  "generated_at": "2026-06-03T15:20:00+02:00",
  "board": {
    "name": "packly-fresh",
    "source": "hermes-kanban",
    "repo_path": "/home/mflova/packly",
    "git_branch": "main",
    "git_head": "abc1234",
    "report_window": {
      "since": "2026-06-03T14:40:00+02:00",
      "until": "2026-06-03T15:20:00+02:00"
    }
  },
  "status_counts": {
    "triage": 0,
    "todo": 3,
    "ready": 2,
    "running": 1,
    "blocked": 1,
    "done": 8,
    "archived": 0,
    "total_active": 7
  },
  "changed_since_last_update": {
    "new_tasks": [],
    "completed_tasks": [],
    "blocked_tasks": [],
    "unblocked_tasks": [],
    "status_changes": [],
    "new_comments": [],
    "new_commits": []
  },
  "running": [],
  "recently_done": [],
  "pending_next": [],
  "blockers": [],
  "human_action_required": [],
  "ci": {
    "provider": "github-actions",
    "workflow": "android-ci.yml",
    "latest_run_id": null,
    "status": "unknown",
    "conclusion": null,
    "url": null,
    "checked_at": "2026-06-03T15:20:00+02:00"
  },
  "artifacts": [],
  "commits": [],
  "risks": [],
  "summary_metrics": {
    "completion_delta": 0,
    "blocked_delta": 0,
    "tasks_touched": 0,
    "requires_human": true
  }
}
```

Required model details:

- `schema_version`: semantic version string. Increment minor for additive optional fields, major for breaking changes.
- `generated_at`: timezone-aware ISO-8601 string; use Europe/Zurich for human reports.
- `board`: board metadata, source, repo path, branch/head, and report window.
- `status_counts`: one integer per Kanban status plus `total_active` excluding archived.
- `changed_since_last_update`: arrays of task/event references since the previous report window.
- `running`: tasks currently claimed/running, including assignee, started time, latest heartbeat, and stale flag.
- `recently_done`: tasks completed inside the report window with summary and changed files if provided.
- `pending_next`: ready/todo tasks ordered by priority and dependency readiness.
- `blockers`: blocked tasks with reason, owner/assignee, age, and whether human input is required.
- `human_action_required`: normalized action list for Manu/reviewer/orchestrator, such as review, push, unblock, credential, or decision.
- `ci`: latest workflow status, conclusion, URL, checked time, and warnings.
- `artifacts`: generated APK/report artifacts with path/name/url/type/created_at.
- `commits`: commits in the report window with sha, subject, author, timestamp, and touched paths.
- `risks`: deterministic risk list with severity, source, description, mitigation, and related task IDs.

Representative Pydantic model skeleton:

```python
from datetime import datetime
from enum import StrEnum
from pydantic import BaseModel, ConfigDict, Field

class StrictModel(BaseModel):
    model_config = ConfigDict(extra="forbid")

class KanbanStatus(StrEnum):
    triage = "triage"
    todo = "todo"
    ready = "ready"
    running = "running"
    blocked = "blocked"
    done = "done"
    archived = "archived"

class TaskRef(StrictModel):
    id: str = Field(pattern=r"^t_[0-9a-f]+$")
    title: str
    status: KanbanStatus
    assignee: str | None = None
    priority: int | None = None
    url: str | None = None

class BoardMetadata(StrictModel):
    name: str
    source: str = "hermes-kanban"
    repo_path: str
    git_branch: str
    git_head: str
    report_window: dict[str, datetime]

class BoardReport(StrictModel):
    schema_version: str = "1.0.0"
    generated_at: datetime
    board: BoardMetadata
    status_counts: dict[KanbanStatus, int]
    changed_since_last_update: dict[str, list[TaskRef]]
    running: list[TaskRef]
    recently_done: list[TaskRef]
    pending_next: list[TaskRef]
    blockers: list[dict]
    human_action_required: list[dict]
    ci: dict
    artifacts: list[dict]
    commits: list[dict]
    risks: list[dict]
    summary_metrics: dict[str, int | bool]
```

The production implementation should replace generic `dict` fields with dedicated strict models before release. The skeleton above is only to show boundaries and naming.

## PDF library recommendation

Use ReportLab for the first implementation.

Rationale:

- Pure Python wheel availability is good on Linux/aarch64; it is easier to run via `uv` on Raspberry Pi than browser-backed or system-library-heavy renderers.
- Tables, headings, page breaks, colors, and simple charts are enough for a Kanban status report.
- It avoids WeasyPrint's Cairo/Pango/native dependency chain and avoids headless Chromium.
- It is deterministic and works in cron without a display server.

Alternatives:

| Library | Fit | Concern |
| --- | --- | --- |
| ReportLab | Best first choice | Imperative layout API, less CSS-like |
| WeasyPrint | Best visual HTML/CSS output | Native deps can be painful on Raspberry Pi; larger cron surface |
| xhtml2pdf | Simple HTML-to-PDF | Lower fidelity, older CSS support, frequent layout surprises |
| Playwright/Chromium print | High fidelity | Heavy install, browser lifecycle, not ideal for small Pi cron |
| FPDF/fpdf2 | Lightweight | Weaker table/page-flow primitives for polished operational reports |

PDF sections should mirror the JSON contract: Executive Summary, Human Action Required, Board Counts, Changed Since Last Update, Running Work, Recently Done, Pending Next, Blockers/Risks, CI/Artifacts, Commits, Appendix.

## Cron/script contract

The cron job should become a thin orchestrator. It should call a repo-owned script and then include both artifacts in its final response.

Recommended command:

```bash
cd /home/mflova/packly
uv run python -m packly_tools.kanban_reporting.cli \
  --board packly-fresh \
  --since-state .hermes/kanban-report-state.json \
  --out-dir reports/kanban/$(date +%Y%m%d-%H%M%S) \
  --format json,pdf
```

The CLI should print a strict one-line JSON envelope to stdout:

```json
{
  "ok": true,
  "schema_version": "1.0.0",
  "json_path": "/home/mflova/packly/reports/kanban/20260603-152000/report.json",
  "pdf_path": "/home/mflova/packly/reports/kanban/20260603-152000/report.pdf",
  "requires_human": true,
  "headline": "1 running, 2 ready, 1 blocked; reviewer action required"
}
```

Contract rules:

- Exit code `0`: JSON and PDF were written and schema validation passed.
- Exit code `2`: input/board extraction failed; cron should report failure and not invent data.
- Exit code `3`: schema validation failed; cron should attach logs if available and block for investigation.
- Exit code `4`: PDF rendering failed after valid JSON; cron should send JSON summary and flag PDF failure.
- stdout must be the envelope only; logs go to stderr.
- The cron final answer may summarize the envelope, but any attachment path must come from `pdf_path`.
- The generator updates the `--since-state` file only after successful JSON and PDF writes.

## Migration path for cron job `6b9b2fb0665a`

1. Add and commit this architecture spec.
2. Implement `pyproject.toml`, Pydantic models, fixture tests, generator, ReportLab renderer, and CLI in the Packly repo using `uv` only.
3. Add `.gitignore` entries for `reports/kanban/` and `.hermes/kanban-report-state.json` if they are not meant to be committed.
4. Run local non-Android validation only:
   - `uv run pytest tests/kanban_reporting -q`
   - `uv run python -m packly_tools.kanban_reporting.cli --fixture tests/fixtures/kanban_reporting/sample_board.json --out-dir /tmp/packly-kanban-report --format json,pdf`
   - `git diff --check`
5. Update cron job `6b9b2fb0665a` so its prompt says: run the report CLI first, read the validated JSON, write the normal human summary from the JSON, and include the generated PDF path in the delivered message.
6. Keep the cron schedule and workdir unchanged initially: every 40 minutes, workdir `/home/mflova/packly`, board `packly-fresh`.
7. Run the cron job once manually and verify delivery includes both prose and PDF.
8. After two successful runs, remove any old prose-only extraction instructions from the cron prompt.

Because `cronjob(action="list")` from this profile returned no jobs during this spec task, the implementation worker or orchestrator should confirm whether job `6b9b2fb0665a` lives in another Hermes profile before editing it.

## Scalability and bottlenecks

- Board size: use pagination/export snapshots instead of repeatedly rendering full event history in prompts. The generator should calculate deltas from stored state.
- Data volume: keep raw snapshots separate from compact reports; prune or archive `reports/kanban/` by age.
- Concurrency: write reports into timestamped directories and update the since-state file with an atomic rename to avoid overlapping cron runs corrupting state.
- CI/API limits: cache latest `gh run` response per report run; do not call GitHub once per task.
- PDF growth: cap free-form comment excerpts and move long details to appendix pages.
- LLM reliability: the LLM consumes validated JSON only; it does not determine counts or blocker state.

## Implementation plan

1. Create the Python tool skeleton and uv config.
2. Add strict Pydantic models and export JSON Schema.
3. Add fixtures representing empty, active, blocked, and recently-completed boards.
4. Build deterministic generator tests before implementation.
5. Add Kanban/git/CI extractors behind interfaces.
6. Add ReportLab PDF renderer with golden smoke checks: file exists, non-empty, at least one page.
7. Add CLI and cron envelope behavior.
8. Update cron job `6b9b2fb0665a` only after a fixture run succeeds.

## Recommended committed paths and modules

- `pyproject.toml`
- `packly_tools/kanban_reporting/models.py`
- `packly_tools/kanban_reporting/generator.py`
- `packly_tools/kanban_reporting/pdf.py`
- `packly_tools/kanban_reporting/cli.py`
- `scripts/kanban_report/generate.py`
- `tests/kanban_reporting/test_models.py`
- `tests/kanban_reporting/test_generator.py`
- `tests/kanban_reporting/test_cli_fixture.py`
- `tests/fixtures/kanban_reporting/sample_board.json`
- `docs/kanban-reporting/architecture.md`
- optional generated schema: `docs/kanban-reporting/schema/board-report.schema.json`

# Packly Kanban PDF Report Design

## Purpose

Create a Telegram-friendly PDF companion to the Packly Kanban status summary. The PDF should let Manu quickly answer four questions from a phone screen:

1. What changed since the last report?
2. What is blocked or risky right now?
3. Which work is ready, running, or done?
4. What should happen next?

The prose Telegram message remains the fast notification. The PDF is the structured, scannable artifact for deeper review and forwarding.

## Design principles

- Mobile-first reading: assume the PDF opens in Telegram's viewer on a narrow phone screen.
- Skimmable before detailed: top-level health, blockers, and next actions must appear before full task tables.
- Statuses must work without color: every badge includes text, and risk is reinforced by icons or labels.
- Dense but not cramped: prefer short rows, grouped sections, and page breaks over tiny type.
- Reusable schema: every visual section maps to explicit required JSON fields so the generator can fail fast when data is missing.

## Page format

- Page size: A4 portrait.
- Margins: 18 mm top, 16 mm left/right, 18 mm bottom.
- Header height: 26-32 mm on page 1; 12-16 mm on continuation pages.
- Footer: page number, board name, report timestamp, and compact confidentiality label if needed.
- Recommended maximum: 6 pages for routine reports. If more than 6 pages are needed, collapse done/archived task detail into summary counts and include only the most recent completed items.

## Visual treatment

### Typography

Use a clean sans-serif stack that renders reliably in PDF generation:

- Primary: Inter, Plus Jakarta Sans, Noto Sans, or DejaVu Sans.
- Title: 20-22 pt, 700 weight, line-height 1.2.
- Section heading: 13-15 pt, 700 weight, line-height 1.25.
- Body/table text: 9.5-10.5 pt, 400 weight, line-height 1.35.
- Small metadata: 8-9 pt, 500 weight, line-height 1.3.
- Badge text: 8-9 pt, 700 weight, uppercase or title case.

Avoid body text below 9 pt. Telegram's PDF preview compresses pages; tiny type becomes unreadable quickly.

### Color palette

Base colors should feel aligned with Packly's vibrant minimal design while remaining readable in print/PDF:

| Token | Use | Color | Text |
| --- | --- | --- | --- |
| `ink` | Primary text | `#191C1D` | n/a |
| `muted_ink` | Secondary text | `#52615C` | n/a |
| `surface` | Page background | `#F8F9FA` | `#191C1D` |
| `card` | Section cards | `#FFFFFF` | `#191C1D` |
| `outline` | Rules/borders | `#D6E1DD` | n/a |
| `brand` | Primary accent | `#006B57` | `#FFFFFF` |
| `brand_soft` | Soft primary chip | `#DDFBF3` | `#005141` |
| `info` | Informational | `#006591` | `#FFFFFF` |
| `info_soft` | Soft info chip | `#E2F3FF` | `#004C6E` |
| `warning` | At risk / needs attention | `#B26A00` | `#FFFFFF` |
| `warning_soft` | Soft warning chip | `#FFF1D6` | `#6B4100` |
| `danger` | Blocked / failed | `#BA1A1A` | `#FFFFFF` |
| `danger_soft` | Soft danger chip | `#FFDAD6` | `#93000A` |
| `success` | Done / healthy | `#0B6B35` | `#FFFFFF` |
| `success_soft` | Soft success chip | `#DDF8E7` | `#084B27` |

### Status badges

Badges should be rounded pills with 4-5 px vertical padding and 8-10 px horizontal padding. Always include readable text. Do not rely on color alone.

| Status | Label | Background | Text color | Optional icon |
| --- | --- | --- | --- | --- |
| `triage` | Triage | `#E8DDFF` | `#310082` | `?` |
| `todo` | Todo | `#E1E3E4` | `#3B4A44` | `•` |
| `ready` | Ready | `#E2F3FF` | `#004C6E` | `→` |
| `running` | Running | `#DDFBF3` | `#005141` | `▶` |
| `blocked` | Blocked | `#FFDAD6` | `#93000A` | `!` |
| `done` | Done | `#DDF8E7` | `#084B27` | `✓` |
| `archived` | Archived | `#F0F1F2` | `#52615C` | `×` |

Risk/severity badges:

- `critical`: solid `danger`, white text, `CRITICAL`.
- `high`: `danger_soft`, `#93000A`, `HIGH`.
- `medium`: `warning_soft`, `#6B4100`, `MEDIUM`.
- `low`: `info_soft`, `#004C6E`, `LOW`.
- `none`: `success_soft`, `#084B27`, `OK`.

## Report hierarchy

### 1. Cover summary: report header + current health

Goal: communicate the board state in the first 10 seconds.

Layout:

- Left: title block.
- Right: generated timestamp and run metadata.
- Under title: 3-5 KPI cards in a single row on desktop/PDF page; allow wrapping to two rows.
- Health banner below KPI cards: one sentence summary with severity styling.

Required fields:

- `report.title`
- `report.board_name`
- `report.generated_at_local`
- `report.timezone`
- `report.window_label` such as `Since previous 40m run` or `Daily summary`
- `report.job_id`
- `report.run_id` or stable generation identifier
- `health.level`: `ok | attention | blocked | critical`
- `health.summary`
- `metrics.total_tasks`
- `metrics.running_count`
- `metrics.blocked_count`
- `metrics.ready_count`
- `metrics.done_since_last_report_count`
- `metrics.changed_since_last_report_count`

KPI card layout:

| Card | Primary number | Secondary label | Styling |
| --- | --- | --- | --- |
| Board total | `total_tasks` | `tasks tracked` | neutral |
| Running | `running_count` | `in progress` | brand soft |
| Blocked | `blocked_count` | `need input` | danger if > 0, neutral if 0 |
| Ready | `ready_count` | `ready to pick up` | info soft |
| Done | `done_since_last_report_count` | `completed this window` | success soft |

### 2. Executive summary

Goal: provide the human-readable digest that pairs with the Telegram prose.

Layout:

- Card with 3-5 bullets.
- Each bullet starts with a label: `Progress`, `Risk`, `Decision`, `Next`, or `Note`.
- Keep bullets to one or two lines each.

Required fields:

- `summary.bullets[]`
  - `label`
  - `text`
  - `severity`: `info | success | warning | danger`
- `summary.next_update_at_local` if the cron job has another scheduled run.

### 3. Blockers and decisions needed

Goal: make human intervention obvious. This section appears even when empty.

Layout:

- If blocked items exist: a table grouped by severity, highest first.
- If none: a quiet empty-state card: `No blocked tasks right now.`
- Each blocker row should use generous vertical spacing because block reasons can be long.

Table columns:

| Column | Width guidance | Content |
| --- | --- | --- |
| Task | 22% | Short title, task ID in muted text below |
| Owner | 14% | Assignee/profile |
| Block reason | 36% | One-sentence reason, wrap up to 3 lines |
| Age | 10% | Time blocked or time since last event |
| Needed next | 18% | Decision/action required |

Required fields:

- `blockers.items[]`
  - `task_id`
  - `title`
  - `assignee`
  - `status`
  - `severity`
  - `blocked_reason`
  - `blocked_since_local` or `blocked_age_label`
  - `needed_next`
  - `last_comment_excerpt` optional but recommended

Empty state fields:

- `blockers.empty_message`

### 4. Changes since last report

Goal: answer what changed during the cron window.

Layout:

- Timeline-style list for up to 12 high-signal events.
- If there are more, include a compact `+N more low-signal events` footer.
- Use event-type chips to distinguish completed, blocked, unblocked, created, reassigned, and failed.

Required fields:

- `changes.window_start_local`
- `changes.window_end_local`
- `changes.items[]`
  - `timestamp_local`
  - `event_type`: `created | claimed | completed | blocked | unblocked | failed | reassigned | commented | archived`
  - `task_id`
  - `title`
  - `assignee`
  - `summary`
  - `severity`
- `changes.omitted_count`

### 5. Active work table

Goal: show current running/ready/todo work in a compact operational view.

Layout:

- Sort order: blocked, running, ready, todo, triage.
- Split into separate subsections if there are more than 18 active rows.
- Repeat header row after page breaks.
- Keep task titles to two lines; include details in the notes column only when critical.

Table columns:

| Column | Width guidance | Content |
| --- | --- | --- |
| Status | 13% | Badge |
| Task | 30% | Title + task ID below |
| Assignee | 14% | Profile/person |
| Last update | 13% | Relative age, e.g. `12m ago` |
| Current signal | 30% | Latest summary/comment/error excerpt |

Required fields:

- `active.items[]`
  - `task_id`
  - `title`
  - `status`
  - `assignee`
  - `last_update_local`
  - `last_update_age_label`
  - `current_signal`
  - `priority` optional
  - `parent_ids` optional
  - `child_ids` optional

### 6. Completed work

Goal: celebrate progress while keeping the report short.

Layout:

- Table of tasks completed during the reporting window.
- Limit to the most recent 10 by default.
- If no completed work: compact empty state.

Table columns:

| Column | Width guidance | Content |
| --- | --- | --- |
| Done at | 16% | Local completion time |
| Task | 30% | Title + ID |
| Assignee | 14% | Profile/person |
| Result | 40% | Completion summary, max 3 lines |

Required fields:

- `completed.items[]`
  - `task_id`
  - `title`
  - `assignee`
  - `completed_at_local`
  - `summary`
  - `metadata_highlights[]` optional, max 3 short strings
- `completed.omitted_count`
- `completed.empty_message`

### 7. Risks, aging, and SLA cues

Goal: surface tasks that may not be blocked yet but need attention.

Layout:

- 2-column card grid if few items; table if many.
- Include clear thresholds in the section intro so users understand why an item appears.

Suggested risk rules:

- Running for more than 60 minutes without heartbeat: high.
- Ready for more than 2 reporting windows: medium.
- Blocked for more than 1 hour: high.
- Failed/crashed/timed out in latest run: high or critical.
- Parent done but child not ready: medium.

Required fields:

- `risks.thresholds[]`
  - `label`
  - `description`
- `risks.items[]`
  - `task_id`
  - `title`
  - `risk_level`
  - `risk_reason`
  - `age_label`
  - `recommended_action`

### 8. Dependency snapshot

Goal: show whether the board flow is logically moving.

Layout:

- Small summary row with counts: root tasks, tasks with children, waiting on parents.
- Optional compact dependency table for tasks that are blocked by parent/child relationships.
- Avoid full graph rendering in the PDF unless the board is tiny; graph images become unreadable on mobile.

Required fields:

- `dependencies.root_count`
- `dependencies.with_children_count`
- `dependencies.waiting_on_parents_count`
- `dependencies.items[]`
  - `task_id`
  - `title`
  - `waiting_on_task_ids[]`
  - `unblocked_when`

### 9. Appendix: raw board counts

Goal: give the schema/generator a predictable place for complete counts without cluttering the main narrative.

Layout:

- Compact table by status and assignee.
- Include only if there is enough data or if debug mode is enabled.

Required fields:

- `appendix.status_counts[]`
  - `status`
  - `count`
- `appendix.assignee_counts[]`
  - `assignee`
  - `todo`
  - `ready`
  - `running`
  - `blocked`
  - `done`
  - `total`

## Mobile readability rules

- Use portrait pages and single-column content except for KPI cards and small risk cards.
- Avoid landscape tables. If a table needs more columns, remove lower-value columns or move details into a second line inside the same cell.
- Keep line length around 55-75 characters in prose blocks.
- Use row striping with subtle contrast (`#FFFFFF` / `#F3F7F5`) for tables over 6 rows.
- Repeat table headers after page breaks.
- Never split a task row across pages if the PDF library supports `keepTogether`/`page-break-inside: avoid`.
- Keep first page self-contained: the reader should understand overall health without scrolling.
- Put IDs in muted smaller text below titles, not as the primary label.

## Accessibility and readability constraints

- Text contrast should meet WCAG AA: 4.5:1 for body text and 3:1 for large/bold text.
- Badge foreground/background pairs must meet at least 3:1, with text labels so color is not the only cue.
- Do not encode status by icon alone.
- Use semantic headings in the PDF generation layer if the renderer supports tagged PDFs.
- Include document metadata: title, author/tool, creation date, and board name.
- Avoid red/green-only comparisons; pair status with labels like `Blocked` and `Done`.
- Avoid dense all-caps outside short badges.
- Ensure every generated PDF has a text layer, not screenshot-only pages.
- Use local timezone labels explicitly, e.g. `Europe/Zurich`, so report timing is unambiguous.

## Visual states for the generator

### Loading state

The cron job should send the text summary first only if PDF generation is expected to take more than a few seconds. Otherwise send summary and PDF together.

Suggested temporary message:

`Preparing Packly Kanban PDF report… summary will follow with the attachment.`

### Empty board state

If no tasks exist:

- Header health: `ok`.
- KPI cards show zero counts.
- Executive summary states: `No Kanban tasks are currently tracked on board packly-fresh.`
- Hide blockers, changes, active, completed, risks, and dependencies behind compact empty cards.

### No-change state

If the board exists but nothing changed since the last report:

- Keep KPI cards and active work table.
- Changes section empty card: `No task events during this reporting window.`
- Telegram summary should still state the next scheduled run.

### Error state

If report generation fails:

- Do not attach a broken or partial PDF.
- Telegram text should include: board name, timestamp, failure reason category, and fallback prose summary if available.
- Save the failing JSON payload and generator logs locally for debugging when safe.

Required error fields for a fallback summary:

- `error.generated_at_local`
- `error.board_name`
- `error.error_type`
- `error.error_message_safe`
- `error.fallback_summary`
- `error.debug_artifact_path` optional local path only, never a Telegram MEDIA tag unless intentionally attaching a safe log.

### Success state

Telegram delivery should include:

- concise prose summary
- PDF attachment
- filename that makes the board and run window obvious
- note about the next scheduled run when relevant

## Suggested PDF filename

Use a stable, sortable, human-readable filename:

`packly-kanban-{board_slug}-{YYYYMMDD-HHMM}-{timezone_slug}.pdf`

Example:

`packly-kanban-packly-fresh-20260603-1730-Europe-Zurich.pdf`

Rules:

- Use local report time, not UTC, unless the board explicitly operates in UTC.
- Replace `/` in timezone names with `-`.
- Keep filename under 80 characters for Telegram readability.
- If multiple reports can be generated in the same minute, append a short run ID: `-r{run_id}`.

## Suggested Telegram message with attachment

Short format:

`Packly Kanban report: 2 running, 1 blocked, 3 ready, 4 done since last update. Main risk: packaging QA is blocked on release approval. Next run around 18:10 Europe/Zurich.`

With PDF attachment:

`MEDIA:/path/to/packly-kanban-packly-fresh-20260603-1730-Europe-Zurich.pdf`

In the cron implementation, the MEDIA line should be included only on messaging platforms that intercept `MEDIA:` attachments. For CLI logs or non-media channels, send the plain file path instead.

Required Telegram summary fields:

- `telegram.summary_line`
- `telegram.primary_risk_line`
- `telegram.next_run_line`
- `telegram.attachment_path`
- `telegram.filename`

## Recommended JSON coverage checklist

The schema worker should ensure the payload can populate these top-level objects:

- `report`
- `health`
- `metrics`
- `summary`
- `blockers`
- `changes`
- `active`
- `completed`
- `risks`
- `dependencies`
- `appendix`
- `telegram`

Minimum viable PDF requires: `report`, `health`, `metrics`, `summary`, `blockers`, `changes`, `active`, and `telegram`.

## Acceptance criteria for the coder

- The PDF first page includes header metadata, health banner, KPI cards, executive summary, and blockers or blocker empty state.
- The generator maps every section to explicit JSON fields and fails with a clear validation error when minimum viable fields are missing.
- Status and risk badges use the labels and color tokens above, with non-color text/icon cues.
- Tables repeat headers across page breaks and avoid splitting task rows when supported by the PDF library.
- Routine reports remain readable in Telegram mobile preview: body text is at least 9 pt, margins are not excessive, and important content appears on page 1.
- Empty, no-change, error, and success states are handled intentionally rather than producing blank sections.
- PDF filename follows `packly-kanban-{board_slug}-{YYYYMMDD-HHMM}-{timezone_slug}.pdf` and the Telegram summary references the attachment clearly.
- Generated PDFs include a real text layer and basic document metadata.
- The implementation uses `uv` for Python dependency management and running scripts.
- The cron job can send the normal prose summary and attach the generated PDF when the target platform supports MEDIA attachments.

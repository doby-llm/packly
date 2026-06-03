# Kanban reporting

This directory documents the project-agnostic Hermes Kanban reporting mini-project. The implementation lives in the neutral `kanban_reporting/` Python package and intentionally avoids Packly-specific branding, fields, or Android assumptions.

## Generate from the sample fixture

Use `uv` for every Python command:

```bash
uv run pytest tests/kanban_reporting -q
uv run python -m kanban_reporting.cli \
  --fixture tests/fixtures/kanban_reporting/sample_board.json \
  --out-dir /tmp/kanban-report-fixture \
  --format json,pdf
```

The CLI prints a one-line JSON envelope on stdout. On success it contains:

- `json_path`: absolute path to the validated report JSON.
- `pdf_path`: absolute path to the rendered PDF.
- `delivery_text`: concise text suitable for the normal status summary.
- `media_tag`: `MEDIA:/absolute/path.pdf` for Telegram or other Hermes media-capable gateways.

## Generate JSON Schema

```bash
uv run python -m kanban_reporting.cli \
  --schema \
  --out-dir docs/kanban-reporting/schema
```

The schema is generated from strict Pydantic models with `extra="forbid"` so unknown fields fail fast.

## Input contract

The current CLI accepts either:

- `--fixture <snapshot.json>`: a project-agnostic board snapshot containing board metadata, task rows, and change events; or
- `--input-report <report.json>`: an already validated `BoardReport` JSON payload to render/write.

Live Kanban facts must be normalized into explicit JSON before the renderer runs. The package should not invent counts, blockers, statuses, CI state, commits, or artifact paths.

## Exit codes

- `0`: JSON/PDF generation succeeded.
- `2`: input or board extraction failed.
- `3`: strict schema validation failed.
- `4`: report generation or PDF rendering failed after validation.

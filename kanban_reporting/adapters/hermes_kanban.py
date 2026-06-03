from __future__ import annotations

import json
from pathlib import Path
from typing import Any


def load_snapshot(path: str | Path) -> dict[str, Any]:
    """Load an explicit Hermes Kanban snapshot JSON file.

    Live board extraction should normalize into the fixture shape consumed by
    kanban_reporting.generator before validation/rendering.
    """
    return json.loads(Path(path).read_text(encoding="utf-8"))

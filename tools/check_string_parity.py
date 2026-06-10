#!/usr/bin/env python3
"""Validate Packly Android string-resource parity across supported locales."""

from __future__ import annotations

import sys
import xml.etree.ElementTree as ET
from pathlib import Path

RESOURCE_DIRS = ("values", "values-es", "values-de")
STRINGS_FILE = "strings.xml"


def read_string_names(path: Path) -> set[str]:
    root = ET.parse(path).getroot()
    return {
        element.attrib["name"]
        for element in root.findall("string")
        if "name" in element.attrib
    }


def main() -> int:
    repo_root = Path(__file__).resolve().parents[1]
    res_root = repo_root / "app" / "src" / "main" / "res"
    keys_by_locale = {
        locale: read_string_names(res_root / locale / STRINGS_FILE)
        for locale in RESOURCE_DIRS
    }

    reference_locale = RESOURCE_DIRS[0]
    reference_keys = keys_by_locale[reference_locale]
    has_errors = False

    for locale in RESOURCE_DIRS[1:]:
        missing = sorted(reference_keys - keys_by_locale[locale])
        extra = sorted(keys_by_locale[locale] - reference_keys)
        if missing or extra:
            has_errors = True
            print(f"{locale} does not match {reference_locale}:", file=sys.stderr)
            if missing:
                print(f"  missing: {', '.join(missing)}", file=sys.stderr)
            if extra:
                print(f"  extra: {', '.join(extra)}", file=sys.stderr)

    if has_errors:
        return 1

    key_count = len(reference_keys)
    locales = ", ".join(RESOURCE_DIRS)
    print(f"String parity OK: {key_count} keys across {locales}.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

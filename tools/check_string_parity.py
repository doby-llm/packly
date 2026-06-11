#!/usr/bin/env python3
"""Validate Packly Android string/plural i18n coverage without Gradle."""

from __future__ import annotations

import re
import sys
import xml.etree.ElementTree as ET
from dataclasses import dataclass
from pathlib import Path

RESOURCE_DIRS = ("values", "values-es", "values-de")
STRINGS_FILE = "strings.xml"
PLACEHOLDER_RE = re.compile(r"%(?:(\d+)\$)?([dsf])")
COUNT_STRING_KEY_RE = re.compile(r"count", re.IGNORECASE)
COUNT_VALUE_RE = re.compile(r"%(?:\d+\$)?d.*\b(items?|trips?|lists?)\b|\b(items?|trips?|lists?)\b.*%(?:\d+\$)?d", re.IGNORECASE)
PAREN_PLURAL_RE = re.compile(r"\((?:s|es)\)", re.IGNORECASE)

# Count-bearing strings that intentionally do not require plural grammar.
COUNT_STRING_ALLOWLIST = {
    "all_categories_count",  # The variable is the category count shown in parentheses.
    "category_count_label",  # Category name + numeric badge; no noun is inflected.
    "filter_unpacked_count",
    "filter_packed_count",
    "percent_packed",
    "percent_packed_label",
    "a11y_packing_progress",  # Progress phrase; no item/list/trip noun.
}

ALLOWED_LOCALIZED_ENGLISH_TERMS = (
    "Packly",
    "Google",
    "Google Drive",
    "Android",
    "URL",
    "Cloud",  # Common loanword in German sync copy.
)
FORBIDDEN_LOCALIZED_ENGLISH = (
    re.compile(r"\bitem\(s\)|\bitems?\b", re.IGNORECASE),
    re.compile(r"\btrips?\b", re.IGNORECASE),
    re.compile(r"\blists?\b", re.IGNORECASE),
    re.compile(r"\bPack\s*by\b", re.IGNORECASE),
    re.compile(r"\bCreate\b", re.IGNORECASE),
    re.compile(r"\bSearch\b", re.IGNORECASE),
    re.compile(r"\bSelect\b", re.IGNORECASE),
    re.compile(r"\bRename\b", re.IGNORECASE),
    re.compile(r"\bDuplicate\b", re.IGNORECASE),
    re.compile(r"\bArchive\b", re.IGNORECASE),
    re.compile(r"\bUnpacked\b|\bPacked\b", re.IGNORECASE),
    re.compile(r"\bBackup\b|\bRestore\b", re.IGNORECASE),
)
UI_LITERAL_PATTERNS = (
    re.compile(r'\bText\s*\(\s*"[^"]*[A-Za-z][^"]*"'),
    re.compile(r'\b(contentDescription|stateDescription)\s*=\s*"[^"]*[A-Za-z][^"]*"'),
    re.compile(r'\b(title|body|actionLabel|label|placeholder|supportingText|percentLabel|metadata)\s*=\s*"[^"]*[A-Za-z][^"]*"'),
    re.compile(r'\bshowSnackbar\s*\(\s*"[^"]*[A-Za-z][^"]*"'),
)
ALLOWED_NON_UI_LITERAL_MARKERS = (
    "DateTimeFormatter.ofPattern",
    "item(key =",
    "const val",
    'getString("',
    'Regex("',
    "JsonPrimitive",
    "schemaVersion",
    "@Suppress",
    "CategoryToken(",
    "normalizedForDedupe",
    "stableRequestCode",
    'Text("$',
    "packing-progress",
)
FORBIDDEN_SEED_SNIPPETS_BY_FILE = {
    "app/src/main/java/com/dobyllm/packly/feature/trips/TripDetailScreen.kt": (
        "Text(entry.nameSnapshot", "itemName = entry.nameSnapshot", "a11y_remove_named, entry.nameSnapshot",
        "item.name.contains(normalizedQuery", ".label.contains(normalizedQuery", "list.name.contains(normalizedQuery",
        "list.description.contains(normalizedQuery", "label = category.label", "a11y_remove_list_from_trip, list.name",
        "a11y_add_list_to_trip, list.name", "Text(list.name", "a11y_remove_named, list.name",
        "a11y_add_named, list.name", "a11y_remove_item_from_trip, item.name", "a11y_add_named, item.name",
        "Text(item.name", "text = category.label", "sourceList?.name?.let",
    ),
    "app/src/main/java/com/dobyllm/packly/feature/lists/ListDetailScreen.kt": ("title = item.name", "category?.label", "text = list.name", "text = list.description"),
    "app/src/main/java/com/dobyllm/packly/feature/lists/AddItemsToListSheet.kt": ("it.id to it.label", "item.name.contains(query", "label = category.label", "title = item.name", "category?.label", "sortedBy { it.name.lowercase()"),
    "app/src/main/java/com/dobyllm/packly/feature/lists/ListsScreen.kt": ("list_duplicated_snackbar, list.name", "archive_list_title, list.name", "rename_list_title, list.name", "it.id to it.label", "item.name.contains(itemQuery", "label = category.label", "title = item.name", "category?.label", "sortedBy { it.name.lowercase()"),
    "app/src/main/java/com/dobyllm/packly/feature/items/ItemsScreen.kt": ("archive_item_title, item.name", "category_count_label, category.label", "name.contains(query"),
    "app/src/main/java/com/dobyllm/packly/feature/items/EditItemSheet.kt": ("label = category.label",),
    "app/src/main/java/com/dobyllm/packly/feature/trips/CreateTripScreen.kt": ("item.name.contains(itemQuery", "category_count_label, list.name", "selected_list_remove_label, list.name", "item_already_included_label, item.name", "TripReviewItem(itemId, entry.itemNameSnapshot", "TripReviewItem(item.id, item.name", "?.label ?: stringResource(R.string.unknown_category)"),
    "app/src/main/java/com/dobyllm/packly/feature/packing/PackingModeScreen.kt": ("entry.nameSnapshot", "text = category?.label", "category?.label ?: stringResource(R.string.category_other)"),
}


@dataclass(frozen=True)
class Resources:
    strings: dict[str, str]
    plurals: dict[str, dict[str, str]]


def placeholders(value: str) -> tuple[str, ...]:
    return tuple(f"{position or ''}${kind}" for position, kind in PLACEHOLDER_RE.findall(value))


def read_resources(path: Path) -> Resources:
    root = ET.parse(path).getroot()
    strings = {
        element.attrib["name"]: "".join(element.itertext())
        for element in root.findall("string")
        if "name" in element.attrib
    }
    plurals: dict[str, dict[str, str]] = {}
    for plural in root.findall("plurals"):
        name = plural.attrib.get("name")
        if not name:
            continue
        quantities: dict[str, str] = {}
        for item in plural.findall("item"):
            quantity = item.attrib.get("quantity")
            if quantity:
                quantities[quantity] = "".join(item.itertext())
        plurals[name] = quantities
    return Resources(strings=strings, plurals=plurals)


def strip_allowed_terms(value: str) -> str:
    cleaned = value
    for term in ALLOWED_LOCALIZED_ENGLISH_TERMS:
        cleaned = cleaned.replace(term, "")
    return cleaned


def validate_parity(resources_by_locale: dict[str, Resources]) -> list[str]:
    errors: list[str] = []
    reference = resources_by_locale[RESOURCE_DIRS[0]]
    reference_string_keys = set(reference.strings)
    reference_plural_keys = set(reference.plurals)

    for locale in RESOURCE_DIRS[1:]:
        resources = resources_by_locale[locale]
        missing_strings = sorted(reference_string_keys - set(resources.strings))
        extra_strings = sorted(set(resources.strings) - reference_string_keys)
        missing_plurals = sorted(reference_plural_keys - set(resources.plurals))
        extra_plurals = sorted(set(resources.plurals) - reference_plural_keys)
        if missing_strings or extra_strings:
            errors.append(f"{locale} string keys differ from values: missing={missing_strings}, extra={extra_strings}")
        if missing_plurals or extra_plurals:
            errors.append(f"{locale} plural keys differ from values: missing={missing_plurals}, extra={extra_plurals}")

        for key in sorted(reference_plural_keys & set(resources.plurals)):
            reference_quantities = set(reference.plurals[key])
            quantities = set(resources.plurals[key])
            if reference_quantities != quantities:
                errors.append(f"{locale}:plurals/{key} quantities differ: {sorted(quantities)} != {sorted(reference_quantities)}")
    return errors


def validate_placeholders(resources_by_locale: dict[str, Resources]) -> list[str]:
    errors: list[str] = []
    reference = resources_by_locale[RESOURCE_DIRS[0]]
    for locale in RESOURCE_DIRS[1:]:
        resources = resources_by_locale[locale]
        for key, value in reference.strings.items():
            if key in resources.strings and placeholders(value) != placeholders(resources.strings[key]):
                errors.append(f"{locale}:string/{key} placeholder mismatch {placeholders(resources.strings[key])} != {placeholders(value)}")
        for key, quantities in reference.plurals.items():
            for quantity, value in quantities.items():
                localized = resources.plurals.get(key, {}).get(quantity)
                if localized is not None and placeholders(value) != placeholders(localized):
                    errors.append(f"{locale}:plurals/{key}[{quantity}] placeholder mismatch {placeholders(localized)} != {placeholders(value)}")
    return errors


def validate_default_count_resources(default: Resources) -> list[str]:
    errors: list[str] = []
    plural_keys = set(default.plurals)
    for key, value in default.strings.items():
        if PAREN_PLURAL_RE.search(value):
            errors.append(f"values:string/{key} uses parenthetical plural copy: {value}")
        countish_key = COUNT_STRING_KEY_RE.search(key) and "%" in value and key not in COUNT_STRING_ALLOWLIST
        countish_value = COUNT_VALUE_RE.search(value)
        if (countish_key or countish_value) and key not in COUNT_STRING_ALLOWLIST and key not in plural_keys:
            errors.append(f"values:string/{key} looks count-bearing; use <plurals> or add an explicit allowlist entry")
    return errors


def validate_localized_english(resources_by_locale: dict[str, Resources]) -> list[str]:
    errors: list[str] = []
    for locale in RESOURCE_DIRS[1:]:
        resources = resources_by_locale[locale]
        entries = [(f"string/{key}", value) for key, value in resources.strings.items()]
        entries += [
            (f"plurals/{key}[{quantity}]", value)
            for key, quantities in resources.plurals.items()
            for quantity, value in quantities.items()
        ]
        for label, value in entries:
            if PAREN_PLURAL_RE.search(value):
                errors.append(f"{locale}:{label} uses parenthetical plural copy: {value}")
            stripped = strip_allowed_terms(value)
            for pattern in FORBIDDEN_LOCALIZED_ENGLISH:
                if pattern.search(stripped):
                    errors.append(f"{locale}:{label} appears to contain untranslated English: {value}")
                    break
    return errors


def validate_source_i18n(repo_root: Path) -> list[str]:
    errors: list[str] = []
    source_root = repo_root / "app" / "src" / "main" / "java" / "com" / "dobyllm" / "packly"
    for path in source_root.rglob("*.kt"):
        if path.as_posix().endswith("ui/i18n/SeedDisplayNames.kt"):
            continue
        relative = path.relative_to(source_root).as_posix()
        for line_number, line in enumerate(path.read_text().splitlines(), start=1):
            stripped = line.strip()
            if any(marker in stripped for marker in ALLOWED_NON_UI_LITERAL_MARKERS):
                continue
            if any(pattern.search(line) for pattern in UI_LITERAL_PATTERNS):
                errors.append(f"{relative}:{line_number} visible copy must use resources: {stripped}")

    for relative_path, forbidden_snippets in FORBIDDEN_SEED_SNIPPETS_BY_FILE.items():
        source = (repo_root / relative_path).read_text()
        for snippet in forbidden_snippets:
            if snippet in source:
                errors.append(f"{relative_path} should use seed display helpers; found `{snippet}`")
    return errors


def main() -> int:
    repo_root = Path(__file__).resolve().parents[1]
    res_root = repo_root / "app" / "src" / "main" / "res"
    resources_by_locale = {
        locale: read_resources(res_root / locale / STRINGS_FILE)
        for locale in RESOURCE_DIRS
    }

    errors: list[str] = []
    errors.extend(validate_parity(resources_by_locale))
    errors.extend(validate_placeholders(resources_by_locale))
    errors.extend(validate_default_count_resources(resources_by_locale["values"]))
    errors.extend(validate_localized_english(resources_by_locale))
    errors.extend(validate_source_i18n(repo_root))

    if errors:
        print("i18n resource validation failed:", file=sys.stderr)
        for error in errors:
            print(f"  - {error}", file=sys.stderr)
        return 1

    default = resources_by_locale["values"]
    locales = ", ".join(RESOURCE_DIRS)
    print(
        f"String/plural parity OK: {len(default.strings)} strings and "
        f"{len(default.plurals)} plurals across {locales}."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

#!/usr/bin/env python3
"""Validate Packly Android string/plural i18n coverage without Gradle."""

from __future__ import annotations

import hashlib
import re
import struct
import sys
import xml.etree.ElementTree as ET
from dataclasses import dataclass
from pathlib import Path

RESOURCE_DIRS = ("values", "values-es", "values-de")
STRINGS_FILE = "strings.xml"
APP_BUILD_GRADLE_FILE = Path("app/build.gradle.kts")
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


def strip_kotlin_comments(source: str) -> str:
    """Remove Kotlin comments while preserving quoted strings for static Gradle checks."""
    result: list[str] = []
    i = 0
    in_string = False
    in_triple_string = False
    while i < len(source):
        if in_triple_string:
            if source.startswith('\"\"\"', i):
                result.append('\"\"\"')
                i += 3
                in_triple_string = False
            else:
                result.append(source[i])
                i += 1
            continue

        if in_string:
            result.append(source[i])
            if source[i] == "\\" and i + 1 < len(source):
                result.append(source[i + 1])
                i += 2
                continue
            if source[i] == '\"':
                in_string = False
            i += 1
            continue

        if source.startswith('\"\"\"', i):
            result.append('\"\"\"')
            i += 3
            in_triple_string = True
        elif source[i] == '\"':
            result.append(source[i])
            i += 1
            in_string = True
        elif source.startswith("//", i):
            newline = source.find("\n", i)
            if newline == -1:
                break
            result.append("\n")
            i = newline + 1
        elif source.startswith("/*", i):
            end = source.find("*/", i + 2)
            comment = source[i : len(source) if end == -1 else end + 2]
            result.append("\n" * comment.count("\n"))
            i = len(source) if end == -1 else end + 2
        else:
            result.append(source[i])
            i += 1
    return "".join(result)


def find_named_block(source: str, name: str, start: int = 0) -> tuple[int, int] | None:
    pattern = re.compile(rf"(?<![A-Za-z0-9_]){re.escape(name)}\s*\{{")
    match = pattern.search(source, start)
    if not match:
        return None

    open_brace = source.find("{", match.start())
    depth = 0
    for index in range(open_brace, len(source)):
        if source[index] == "{":
            depth += 1
        elif source[index] == "}":
            depth -= 1
            if depth == 0:
                return open_brace + 1, index
    return None


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


def validate_incoming_trip_summary_plural(resources_by_locale: dict[str, Resources]) -> list[str]:
    """Keep the home hero on Android plurals so exactly-one trips read singular."""
    errors: list[str] = []
    repo_root = Path(__file__).resolve().parents[1]
    for locale, resources in resources_by_locale.items():
        home_summary = resources.plurals.get("home_summary")
        if not home_summary:
            errors.append(f"{locale}:plurals/home_summary is required for upcoming-trip copy")
            continue
        missing = {"one", "other"} - set(home_summary)
        if missing:
            errors.append(f"{locale}:plurals/home_summary missing quantities: {sorted(missing)}")
        for quantity in ("one", "other"):
            value = home_summary.get(quantity, "")
            if "%1$d" not in value:
                errors.append(f"{locale}:plurals/home_summary[{quantity}] must include %1$d")

    home_screen = repo_root / "app/src/main/java/com/dobyllm/packly/feature/home/HomeScreen.kt"
    source = home_screen.read_text()
    if "pluralStringResource(" not in source or "R.plurals.home_summary" not in source:
        errors.append("HomeScreen.kt must render upcoming-trip summary with pluralStringResource(R.plurals.home_summary)")
    return errors


def read_ttf_table_directory(font_path: Path) -> dict[str, tuple[int, int]]:
    """Return SFNT table offsets/lengths without depending on fontTools in CI."""
    data = font_path.read_bytes()
    if len(data) < 12:
        raise ValueError("file is too small to be a TTF")
    num_tables = struct.unpack_from(">H", data, 4)[0]
    tables: dict[str, tuple[int, int]] = {}
    for index in range(num_tables):
        record_offset = 12 + index * 16
        if record_offset + 16 > len(data):
            raise ValueError("table directory is truncated")
        tag = data[record_offset : record_offset + 4].decode("ascii", errors="replace")
        offset, length = struct.unpack_from(">II", data, record_offset + 8)
        if offset + length > len(data):
            raise ValueError(f"table {tag} points outside the file")
        tables[tag] = (offset, length)
    return tables


def read_ttf_weight_class(font_path: Path, tables: dict[str, tuple[int, int]]) -> int:
    os2 = tables.get("OS/2")
    if os2 is None:
        raise ValueError("missing OS/2 table")
    offset, length = os2
    if length < 6:
        raise ValueError("OS/2 table is too short")
    return struct.unpack_from(">H", font_path.read_bytes(), offset + 4)[0]


def read_ttf_name(font_path: Path, tables: dict[str, tuple[int, int]], name_id: int) -> str | None:
    name_table = tables.get("name")
    if name_table is None:
        raise ValueError("missing name table")
    data = font_path.read_bytes()
    table_offset, length = name_table
    if length < 6:
        raise ValueError("name table is too short")
    _format, count, strings_offset = struct.unpack_from(">HHH", data, table_offset)
    records_offset = table_offset + 6
    string_storage_offset = table_offset + strings_offset
    for index in range(count):
        record_offset = records_offset + index * 12
        if record_offset + 12 > table_offset + length:
            raise ValueError("name record is truncated")
        platform_id, _encoding_id, _language_id, record_name_id, value_length, value_offset = struct.unpack_from(
            ">HHHHHH",
            data,
            record_offset,
        )
        if record_name_id != name_id:
            continue
        value_start = string_storage_offset + value_offset
        value = data[value_start : value_start + value_length]
        return value.decode("utf-16-be" if platform_id == 3 else "mac_roman", errors="replace")
    return None


def validate_plus_jakarta_font_files(font_dir: Path) -> list[str]:
    errors: list[str] = []
    hashes: dict[int, str] = {}
    expected_subfamilies = {
        400: "Regular",
        500: "Medium",
        600: "SemiBold",
        700: "Bold",
        800: "ExtraBold",
    }
    for weight, subfamily in expected_subfamilies.items():
        font_path = font_dir / f"plus_jakarta_sans_{weight}.ttf"
        if not font_path.exists():
            errors.append(f"app/src/main/res/font/plus_jakarta_sans_{weight}.ttf is required")
            continue
        try:
            tables = read_ttf_table_directory(font_path)
            actual_weight = read_ttf_weight_class(font_path, tables)
            actual_subfamily = read_ttf_name(font_path, tables, 2)
        except ValueError as exc:
            errors.append(f"{font_path.relative_to(font_dir.parent.parent.parent.parent)} is not a readable static TTF: {exc}")
            continue
        hashes[weight] = hashlib.sha256(font_path.read_bytes()).hexdigest()
        if actual_weight != weight:
            errors.append(f"{font_path.name} OS/2 usWeightClass {actual_weight} must match suffix {weight}")
        if actual_subfamily != subfamily:
            errors.append(f"{font_path.name} name subfamily {actual_subfamily!r} must be {subfamily!r}")
        variable_tables = sorted({"fvar", "gvar"} & set(tables))
        if variable_tables:
            errors.append(f"{font_path.name} must be a static TTF; found variable tables {variable_tables}")
    duplicate_hashes = {digest for digest in hashes.values() if list(hashes.values()).count(digest) > 1}
    for digest in sorted(duplicate_hashes):
        duplicate_weights = [weight for weight, font_hash in hashes.items() if font_hash == digest]
        errors.append(f"Plus Jakarta Sans static weights must not be byte-identical: {duplicate_weights} share {digest}")
    return errors


def validate_plus_jakarta_typography(repo_root: Path) -> list[str]:
    """Guard the deterministic bundled-font rollout from ad-hoc font overrides."""
    errors: list[str] = []
    theme_type = repo_root / "app/src/main/java/com/dobyllm/packly/ui/theme/Type.kt"
    theme_source = theme_type.read_text()
    required_snippets = (
        "val PlusJakartaSans = FontFamily(",
        "R.font.plus_jakarta_sans_400",
        "R.font.plus_jakarta_sans_500",
        "R.font.plus_jakarta_sans_600",
        "R.font.plus_jakarta_sans_700",
        "R.font.plus_jakarta_sans_800",
        "fontFamily = PlusJakartaSans",
        "bodyLarge = packlyTextStyle(16, 24, FontWeight.Normal)",
        "bodyMedium = packlyTextStyle(16, 24, FontWeight.Normal)",
        "labelLarge = packlyTextStyle(12, 16, FontWeight.SemiBold, 0.6f)",
        "labelMedium = packlyTextStyle(12, 16, FontWeight.SemiBold, 0.6f)",
    )
    for snippet in required_snippets:
        if snippet not in theme_source:
            errors.append(f"{theme_type.relative_to(repo_root)} missing Plus Jakarta typography snippet `{snippet}`")

    theme = repo_root / "app/src/main/java/com/dobyllm/packly/ui/theme/Theme.kt"
    if "typography = PacklyTypography" not in theme.read_text():
        errors.append(f"{theme.relative_to(repo_root)} must install PacklyTypography in MaterialTheme")

    font_dir = repo_root / "app/src/main/res/font"
    errors.extend(validate_plus_jakarta_font_files(font_dir))

    source_root = repo_root / "app/src/main/java/com/dobyllm/packly"
    allowed_font_family_file = "ui/theme/Type.kt"
    for path in source_root.rglob("*.kt"):
        relative = path.relative_to(source_root).as_posix()
        if relative == allowed_font_family_file:
            continue
        source = path.read_text()
        if "fontFamily" in source or "FontFamily" in source:
            errors.append(f"{relative} must not bypass PacklyTypography with direct fontFamily/FontFamily usage")
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


def validate_duplicate_copy_name_localized(repo_root: Path) -> list[str]:
    errors: list[str] = []
    view_model_path = repo_root / "app/src/main/java/com/dobyllm/packly/PacklyAppViewModel.kt"
    nav_host_path = repo_root / "app/src/main/java/com/dobyllm/packly/navigation/PacklyNavHost.kt"
    view_model_source = view_model_path.read_text()
    nav_host_source = nav_host_path.read_text()

    forbidden_view_model_snippets = (
        '"$baseName copy"',
        '"$copyName $suffix"',
        '" copy"',
    )
    for snippet in forbidden_view_model_snippets:
        if snippet in view_model_source:
            errors.append(f"{view_model_path.relative_to(repo_root)} hard-codes duplicate-list copy name snippet `{snippet}`")

    required_flow_snippets = (
        "copyNameTemplates = copyNameTemplates",
        "R.string.list_duplicate_copy_name",
        "R.string.list_duplicate_copy_name_numbered",
    )
    combined_source = view_model_source + "\n" + nav_host_source
    for snippet in required_flow_snippets:
        if snippet not in combined_source:
            errors.append(f"duplicate-list copy naming must flow through localized resources; missing `{snippet}`")
    return errors


def validate_aab_language_split_disabled(repo_root: Path) -> list[str]:
    build_gradle_path = repo_root / APP_BUILD_GRADLE_FILE
    source = strip_kotlin_comments(build_gradle_path.read_text())

    android_block = find_named_block(source, "android")
    if not android_block:
        return [f"{APP_BUILD_GRADLE_FILE} must declare an android block"]

    android_source = source[android_block[0] : android_block[1]]
    bundle_block = find_named_block(android_source, "bundle")
    if not bundle_block:
        return [
            f"{APP_BUILD_GRADLE_FILE} must disable AAB language splits with "
            "android { bundle { language { enableSplit = false } } }"
        ]

    bundle_source = android_source[bundle_block[0] : bundle_block[1]]
    language_block = find_named_block(bundle_source, "language")
    if not language_block:
        return [f"{APP_BUILD_GRADLE_FILE} must declare android.bundle.language for Play language delivery"]

    language_source = bundle_source[language_block[0] : language_block[1]]
    if not re.search(r"(?<![A-Za-z0-9_])enableSplit\s*=\s*false(?![A-Za-z0-9_])", language_source):
        return [f"{APP_BUILD_GRADLE_FILE} must set android.bundle.language.enableSplit = false"]
    return []


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
    errors.extend(validate_incoming_trip_summary_plural(resources_by_locale))
    errors.extend(validate_plus_jakarta_typography(repo_root))
    errors.extend(validate_source_i18n(repo_root))
    errors.extend(validate_duplicate_copy_name_localized(repo_root))
    errors.extend(validate_aab_language_split_disabled(repo_root))

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

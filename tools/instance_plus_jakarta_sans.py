# /// script
# requires-python = ">=3.12"
# dependencies = [
#     "fonttools>=4.63.0",
# ]
# ///
"""Generate deterministic static Plus Jakarta Sans weight instances for Android.

The repository intentionally keeps the source variable font in the 400 slot until
this script replaces every bundled weight with a static, weight-specific TTF.
Run with: uv run tools/instance_plus_jakarta_sans.py
"""

from __future__ import annotations

from io import BytesIO
from pathlib import Path

from fontTools.ttLib import TTFont
from fontTools.varLib.instancer import instantiateVariableFont

FONT_DIR = Path("app/src/main/res/font")
WEIGHTS = {
    400: "Regular",
    500: "Medium",
    600: "SemiBold",
    700: "Bold",
    800: "ExtraBold",
}


def set_name(font: TTFont, name_id: int, value: str) -> None:
    """Update all existing name records for name_id to keep Android metadata sane."""
    for record in font["name"].names:
        if record.nameID == name_id:
            record.string = value.encode(record.getEncoding(), errors="replace")


def set_static_names(font: TTFont, weight: int, subfamily: str) -> None:
    set_name(font, 2, subfamily)
    set_name(font, 3, f"2.071;TOKO;PlusJakartaSans-{subfamily}")
    set_name(font, 4, f"Plus Jakarta Sans {subfamily}")
    set_name(font, 6, f"PlusJakartaSans-{subfamily}")
    font["OS/2"].usWeightClass = weight


def main() -> int:
    source_path: Path | None = None
    source_bytes: bytes | None = None
    for candidate_weight in WEIGHTS:
        candidate = FONT_DIR / f"plus_jakarta_sans_{candidate_weight}.ttf"
        if candidate.exists():
            with TTFont(candidate) as candidate_font:
                if "fvar" in candidate_font:
                    source_path = candidate
                    source_bytes = candidate.read_bytes()
                    break
    if source_path is None or source_bytes is None:
        raise SystemExit("No bundled Plus Jakarta Sans variable font with an fvar table was found")

    for weight, subfamily in WEIGHTS.items():
        font = TTFont(BytesIO(source_bytes))
        instance = instantiateVariableFont(font, {"wght": weight}, inplace=False)
        set_static_names(instance, weight, subfamily)
        destination = FONT_DIR / f"plus_jakarta_sans_{weight}.ttf"
        instance.save(destination)
        print(f"wrote {destination} as {subfamily} ({weight})")

    # Keep accidental no-op runs obvious in local output.
    if (FONT_DIR / "plus_jakarta_sans_400.ttf").read_bytes() == source_bytes:
        raise SystemExit("400 output remained byte-identical to the variable source")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

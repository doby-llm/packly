#!/usr/bin/env python3
# /// script
# requires-python = ">=3.12"
# dependencies = [
#     "pillow>=12.2.0",
# ]
# ///

from __future__ import annotations

import argparse
from pathlib import Path
from PIL import Image


MAX_SIZE_BYTES = 1_000_000
TARGET_SIZE = (512, 512)


def center_crop_square(image: Image.Image) -> Image.Image:
    """Crop the image to a centered square.

    Args:
        image: Input PIL image.

    Returns:
        A square PIL image.
    """
    width, height = image.size
    side = min(width, height)

    left = (width - side) // 2
    top = (height - side) // 2
    right = left + side
    bottom = top + side

    return image.crop((left, top, right, bottom))


def process_icon(input_path: Path, output_path: Path) -> None:
    """Create a compliant app icon image.

    The output image will be:
    - PNG
    - 512 x 512 px
    - No metadata
    - Up to 1 MB when possible

    Args:
        input_path: Path to the source image.
        output_path: Path where the processed icon will be saved.

    Raises:
        ValueError: If the final file is larger than 1 MB.
    """
    with Image.open(input_path) as image:
        image = image.convert("RGBA")

        image = center_crop_square(image)
        image = image.resize(TARGET_SIZE, Image.Resampling.LANCZOS)

        # Save without metadata.
        image.save(
            output_path,
            format="PNG",
            optimize=True,
            compress_level=9,
        )

    final_size = output_path.stat().st_size

    if final_size > MAX_SIZE_BYTES:
        raise ValueError(
            f"Output file is too large: {final_size / 1024:.1f} KB. "
            "Try using a simpler image with fewer details or colors."
        )


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Prepare an app icon as PNG, 512x512 px, max 1 MB."
    )
    parser.add_argument(
        "input",
        type=Path,
        help="Input image path.",
    )
    parser.add_argument(
        "-o",
        "--output",
        type=Path,
        default=Path("app_icon.png"),
        help="Output icon path. Defaults to app_icon.png.",
    )

    args = parser.parse_args()

    process_icon(args.input, args.output)

    size_kb = args.output.stat().st_size / 1024
    print(f"Icon created: {args.output}")
    print(f"Size: {size_kb:.1f} KB")
    print("Format: PNG")
    print("Dimensions: 512x512 px")


if __name__ == "__main__":
    main()

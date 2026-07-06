#!/usr/bin/env python3
"""CI-safe Packly Google Drive sync config checker.

This script intentionally does not call Gradle or Google APIs. It verifies that
credential placeholders/config keys are present in source and reminds operators
which Google Cloud values must be supplied out-of-band.
"""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
REQUIRED_SOURCE_MARKERS = {
    "app/build.gradle.kts": [
        "PACKLY_GOOGLE_ANDROID_CLIENT_ID",
        "PACKLY_DRIVE_SYNC_ENABLED",
        "packly.google.androidClientId",
        "packly.driveSyncEnabled",
    ],
    "docs/google-drive-cloud-sync.md": [
        "com.gusanitolabs.packly",
        "https://www.googleapis.com/auth/drive.appdata",
        "appDataFolder:/packly/",
    ],
}
REQUIRED_HUMAN_VALUES = [
    "Google Cloud Project ID",
    "OAuth consent publishing state",
    "Support/developer contact email",
    "Debug Android OAuth client ID for com.gusanitolabs.packly",
    "Release/Play Android OAuth client ID for com.gusanitolabs.packly",
    "Registered SHA-1 fingerprint for each OAuth client",
    "Google Drive API enabled",
    "Drive appDataFolder scope added",
]

missing = []
for relative_path, markers in REQUIRED_SOURCE_MARKERS.items():
    text = (ROOT / relative_path).read_text(encoding="utf-8")
    for marker in markers:
        if marker not in text:
            missing.append(f"{relative_path}: missing {marker}")

if missing:
    print("Cloud sync config scaffold check failed:")
    for issue in missing:
        print(f"- {issue}")
    raise SystemExit(1)

print("Cloud sync config scaffold check passed.")
print("Remaining human Google Cloud values:")
for value in REQUIRED_HUMAN_VALUES:
    print(f"- {value}")

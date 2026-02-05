#!/usr/bin/env python3
"""Validate expected asset files listed in app/src/main/assets/annotations.json.

Run:
  python tools/validate_assets.py
"""
from pathlib import Path
import json

REQUIRED_FIELDS = ("file", "animal", "color", "type", "description")
ALLOWED_TYPES = {"static", "idle_sheet"}

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app" / "src" / "main" / "assets"
ANNOTATIONS = ASSETS / "annotations.json"

if not ANNOTATIONS.exists():
    raise SystemExit("Missing app/src/main/assets/annotations.json")

items = json.loads(ANNOTATIONS.read_text())
missing = []
invalid = []

for idx, item in enumerate(items, start=1):
    filename = item.get("file")
    if not filename:
        invalid.append(f"Entry {idx}: missing file")
        continue
    for field in REQUIRED_FIELDS:
        if not item.get(field):
            invalid.append(f"{filename}: missing {field}")
            break
    else:
        asset_type = item.get("type")
        if asset_type not in ALLOWED_TYPES:
            invalid.append(f"{filename}: invalid type '{asset_type}'")
        color = item.get("color")
        animal = item.get("animal")
        expected = (
            f"{animal}_{asset_type}.png"
            if color == "default"
            else f"{animal}_{color}_{asset_type}.png"
        )
        if filename != expected:
            invalid.append(f"{filename}: expected '{expected}'")
    base64_path = ASSETS / f"{filename}.base64"
    png_path = ASSETS / filename
    if not base64_path.exists() and not png_path.exists():
        missing.append(filename)

if invalid:
    print("Invalid annotation entries:")
    for entry in invalid:
        print(f"- {entry}")
    raise SystemExit(1)

if missing:
    print("Missing asset files:")
    for name in missing:
        print(f"- {name} (.base64 or .png)")
    raise SystemExit(1)

print("All annotated assets are present.")

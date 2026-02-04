#!/usr/bin/env python3
"""Generate PNG assets from base64 placeholders.

Run:
  python tools/materialize_assets.py
"""
from pathlib import Path
import base64

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "assets"

for base64_path in ASSETS.rglob("*.png.base64"):
    png_path = base64_path.with_suffix("")
    data = base64_path.read_text().strip()
    png_path.write_bytes(base64.b64decode(data))
    print(f"Wrote {png_path.relative_to(ROOT)}")

#!/usr/bin/env python3
"""Run asset materialization and validation in one step.

Usage:
  python tools/prepare_assets.py
"""
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def run(cmd: list[str]) -> None:
    subprocess.run(cmd, check=True, cwd=ROOT)


def main() -> None:
    run(["python", "tools/materialize_assets.py"])
    run(["python", "tools/validate_assets.py"])


if __name__ == "__main__":
    main()

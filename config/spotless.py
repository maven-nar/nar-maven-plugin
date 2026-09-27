#!/usr/bin/env python3
"""Run the standalone Spotless configuration on files added since a Git base."""

import argparse
from pathlib import Path
import subprocess
import tempfile


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("action", choices=("check", "apply"))
    parser.add_argument("--base", default="origin/master", help="Git base (default: origin/master)")
    parser.add_argument("--mvn", default="mvn", help="Maven executable")
    args = parser.parse_args()
    root = Path(__file__).resolve().parent.parent
    base = subprocess.check_output(
        ["git", "merge-base", args.base, "HEAD"], cwd=root, text=True
    ).strip()
    # Compare with the working tree to include staged additions and subsequent edits.
    names = subprocess.check_output(
        ["git", "diff", "--diff-filter=A", "--name-only", "-z", base, "--"], cwd=root
    ).decode("utf-8").split("\0")
    files = [name for name in names if name and (root / name).is_file()]
    if not files:
        print("No added files to format.")
        return 0
    # Spotless matches absolute paths with Java regexes and splits patterns on commas.
    patterns = []
    for name in files:
        path = str(root / name)
        pattern = "".join("\\" + c if c in r"\.^$|?*+()[]{}" else c for c in path)
        patterns.append(pattern.replace(",", r"\x2c"))
    # A root-level POM makes source paths work without overriding read-only Maven parameters.
    with tempfile.NamedTemporaryFile(prefix=".spotless-", suffix=".xml", dir=root, delete=False) as pom:
        pom.write((root / "config/spotless-pom.xml").read_bytes())
    try:
        return subprocess.call(
            [args.mvn, "-B", "-f", pom.name, "-DspotlessFiles=" + ",".join(patterns),
             "spotless:" + args.action], cwd=root
        )
    finally:
        Path(pom.name).unlink()


if __name__ == "__main__":
    raise SystemExit(main())

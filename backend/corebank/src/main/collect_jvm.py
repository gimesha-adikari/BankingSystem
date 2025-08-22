#!/usr/bin/env python3
"""
Concatenate all .java/.kt/.kts files under a root directory into a single text file.

- Preserves code exactly (no reformatting).
- Writes UTF-8 output.
- Skips common noise folders (.git, __pycache__, venv, node_modules, build, dist, etc.).
"""

from __future__ import annotations
import argparse, os, sys
from pathlib import Path

DEFAULT_EXCLUDES = {
    ".git", ".hg", ".svn", "__pycache__", ".mypy_cache",
    ".pytest_cache", ".venv", "venv", "env", "build", "dist",
    "out", ".gradle", ".idea", ".cache", "node_modules"
}
EXTS = {".java", ".kt", ".kts"}
HEADER_LINE = "=" * 80

def iter_files(root: Path, exts: set[str], exclude_dirs: set[str]) -> list[Path]:
    files: list[Path] = []
    for dirpath, dirnames, filenames in os.walk(root):
        dirnames[:] = [d for d in dirnames if d not in exclude_dirs]
        for fn in filenames:
            if Path(fn).suffix.lower() in exts:
                files.append(Path(dirpath) / fn)
    files.sort(key=lambda p: p.as_posix().lower())
    return files

def write_header(out, rel_path: Path, size_bytes: int | None):
    out.write(f"{HEADER_LINE}\n")
    out.write(f"FILE: {rel_path.as_posix()}")
    if size_bytes is not None:
        out.write(f"  (size: {size_bytes} bytes)")
    out.write("\n")
    out.write(f"{HEADER_LINE}\n")

def concat_sources(root: Path, out_path: Path, exclude_dirs: set[str], include_headers: bool = True) -> int:
    root = root.resolve(); out_path = out_path.resolve()
    if not root.is_dir():
        print(f"Error: root '{root}' is not a directory.", file=sys.stderr); return 2

    files = iter_files(root, EXTS, exclude_dirs)
    if not files:
        print("No Java/Kotlin files found.", file=sys.stderr); return 1

    with out_path.open("w", encoding="utf-8", newline="\n") as out:
        for f in files:
            rel = f.relative_to(root)
            try:
                content = f.read_text(encoding="utf-8")
            except UnicodeDecodeError:
                # Fallback if some file isn't UTF-8
                content = f.read_text(encoding="latin-1")
            except Exception as e:
                print(f"[WARN] Skipping {rel}: {e}", file=sys.stderr)
                continue

            if include_headers:
                try: size_bytes = f.stat().st_size
                except Exception: size_bytes = None
                write_header(out, rel, size_bytes)

            out.write(content)
            if not content.endswith("\n"): out.write("\n")
            if include_headers: out.write("\n")
    print(f"Done. Wrote {len(files)} files into '{out_path}'.")
    return 0

def main():
    ap = argparse.ArgumentParser(description="Concatenate all Java/Kotlin files under ROOT into OUTPUT (UTF-8).")
    ap.add_argument("root", type=Path, help="Root directory to scan")
    ap.add_argument("output", type=Path, help="Output .txt file path")
    ap.add_argument("--no-headers", action="store_true", help="Do not add file path headers between files")
    ap.add_argument("--exclude", nargs="*", default=[], help="Additional directory names to exclude")
    args = ap.parse_args()

    exclude_dirs = set(DEFAULT_EXCLUDES) | set(args.exclude or [])
    rc = concat_sources(args.root, args.output, exclude_dirs, include_headers=not args.no_headers)
    sys.exit(rc)

if __name__ == "__main__":
    main()

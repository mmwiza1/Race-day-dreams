#!/usr/bin/env python3
"""Run pinned build-time formatters; never part of the Android application.

Only the architecture surfaces listed in config/formatting.json are targeted. Tool
downloads require the repository-pinned SHA-256 and a valid JAR, then are cached locally.
Neither --check nor --list rewrites source. Network is unnecessary with a warm cache.
"""

from __future__ import annotations

import argparse
import difflib
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import tempfile
import time
import urllib.request
import zipfile

ROOT = Path(__file__).resolve().parent.parent
CONFIG = ROOT / "config/formatting.json"
MAX_JAR_BYTES = 128 * 1024 * 1024
MAX_FORMAT_PASSES = 4
JAVA_EXPORTS = [
    "--add-exports=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
    "--add-exports=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED",
    "--add-exports=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED",
    "--add-exports=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED",
    "--add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED",
    "--add-exports=jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED",
]


def targets(root: Path, patterns: list[str]) -> list[Path]:
    selected: set[Path] = set()
    for pattern in patterns:
        if Path(pattern).is_absolute() or ".." in Path(pattern).parts:
            raise ValueError("Formatter targets must stay inside the repository")
        for path in root.glob(pattern):
            if path.is_symlink() or not path.resolve().is_relative_to(root.resolve()):
                raise ValueError(f"Unsafe formatter target: {path}")
            if path.is_file() and path.suffix in (".java", ".kt"):
                selected.add(path)
    return sorted(selected)


def fetch(url: str, maximum: int) -> bytes:
    if not url.startswith("https://repo.maven.apache.org/maven2/"):
        raise ValueError("Formatter download must use the pinned Maven Central host")
    # Honour the environment's proxy and normal CA validation. Never bypass either.
    started = time.monotonic()
    with urllib.request.urlopen(url, timeout=30) as response:
        result = bytearray()
        while True:
            block = response.read(min(32768, maximum + 1 - len(result)))
            if not block:
                return bytes(result)
            result.extend(block)
            if len(result) > maximum or time.monotonic() - started > 60:
                raise ValueError("Formatter download exceeded its size/time limit")


def validate_jar(path: Path, expected: str) -> None:
    if not re.fullmatch(r"[a-fA-F0-9]{64}", expected):
        raise ValueError("Publisher did not provide a valid SHA-256")
    if not path.is_file() or path.stat().st_size > MAX_JAR_BYTES:
        raise ValueError("Formatter JAR missing or too large")
    if hashlib.sha256(path.read_bytes()).hexdigest() != expected.lower():
        raise ValueError("Formatter JAR SHA-256 mismatch")
    with zipfile.ZipFile(path) as jar:
        if "META-INF/MANIFEST.MF" not in jar.namelist() or jar.testzip() is not None:
            raise ValueError("Formatter download is not an intact executable JAR")


def tool_jar(language: str, tool: dict, cache: Path) -> Path:
    jar = cache / f"{language}-{tool['version']}.jar"
    checksum = jar.with_suffix(".sha256")
    expected = tool["sha256"]
    if not isinstance(expected, str) or not re.fullmatch(r"[a-fA-F0-9]{64}", expected):
        raise ValueError("Formatter configuration must pin a SHA-256")
    if jar.exists():
        # Cache metadata cannot override the reviewed repository content lock.
        validate_jar(jar, expected)
        return jar
    cache.mkdir(parents=True, exist_ok=True)
    with tempfile.NamedTemporaryFile(dir=cache, suffix=".jar", delete=False) as temp:
        temporary = Path(temp.name)
        try:
            temp.write(fetch(tool["url"], MAX_JAR_BYTES))
            temp.flush()
            validate_jar(temporary, expected)
            temporary.replace(jar)
            checksum.write_text(expected.lower() + "\n", encoding="ascii")
        finally:
            temporary.unlink(missing_ok=True)
    return jar


def command(language: str, tool: dict, jar: Path, paths: list[Path], apply: bool) -> list[str]:
    args = [os.environ.get("JAVA", "java")]
    if language == "java":
        args.extend(JAVA_EXPORTS)
    args.extend(["-jar", str(jar), *tool.get("arguments", [])])
    if language == "kotlin":
        args.extend([] if apply else ["--dry-run", "--set-exit-if-changed"])
    else:
        args.extend(["--replace"] if apply else ["--dry-run", "--set-exit-if-changed"])
    return args + [str(path) for path in paths]


def difference_patch(root: Path, formatted: Path, paths: list[Path]) -> str:
    parts = []
    for path in paths:
        relative = path.relative_to(root)
        original = path.read_text(encoding="utf-8")
        changed = (formatted / relative).read_text(encoding="utf-8")
        if original != changed:
            for line in difflib.unified_diff(
                original.splitlines(keepends=True), changed.splitlines(keepends=True),
                fromfile="a/" + relative.as_posix(), tofile="b/" + relative.as_posix(),
            ):
                parts.append(line if line.endswith("\n") else line + "\n\\ No newline at end of file\n")
    return "".join(parts)


def apply_until_stable(language: str, tool: dict, jar: Path, paths: list[Path]) -> int:
    # Long-string wrapping can expose another layout change on a subsequent pass.
    # Compare actual bytes, not the formatter's success code, and bound the work.
    for _ in range(MAX_FORMAT_PASSES):
        before = [path.read_bytes() for path in paths]
        result = subprocess.run(command(language, tool, jar, paths, True), check=False)
        if result.returncode:
            return result.returncode
        if all(path.read_bytes() == original for path, original in zip(paths, before)):
            return 0
    raise ValueError(f"{language} formatter did not converge after {MAX_FORMAT_PASSES} passes")


def check_with_patch(config: dict, paths: list[Path], cache: Path, output: Path) -> int:
    # Run the actual tools against isolated copies: producing diagnostics must
    # never mutate application sources or weaken the check's exit code.
    with tempfile.TemporaryDirectory(prefix="rdd-format-") as directory:
        formatted = Path(directory)
        for path in paths:
            destination = formatted / path.relative_to(ROOT)
            destination.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(path, destination)
        for language, suffix in (("kotlin", ".kt"), ("java", ".java")):
            selected = [formatted / path.relative_to(ROOT) for path in paths if path.suffix == suffix]
            if not selected:
                continue
            tool = config["tools"][language]
            jar = tool_jar(language, tool, cache)
            status = apply_until_stable(language, tool, jar, selected)
            if status:
                return status
        patch = difference_patch(ROOT, formatted, paths)
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(patch, encoding="utf-8")
    if patch:
        print(f"Formatting differences written to {output}; source files were not changed.")
        return 1
    print(f"Formatting checked: {len(paths)} scoped source files.")
    return 0


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    mode = parser.add_mutually_exclusive_group(required=True)
    mode.add_argument("--check", action="store_true", help="Fail on unformatted source")
    mode.add_argument("--apply", action="store_true", help="Format only configured source")
    mode.add_argument("--list", action="store_true", help="List targets without downloading tools")
    parser.add_argument("--patch-output", type=Path,
                        help="With --check, write a git-compatible patch without modifying source")
    args = parser.parse_args(argv)
    if args.patch_output is not None and not args.check:
        parser.error("--patch-output requires --check")
    config = json.loads(CONFIG.read_text(encoding="utf-8"))
    if config.get("version") != 1:
        raise ValueError("Unsupported formatting configuration")
    paths = targets(ROOT, config["targets"])
    if not paths:
        raise ValueError("Formatter scope is empty")
    if args.list:
        print("\n".join(str(path.relative_to(ROOT)) for path in paths))
        return 0
    cache = Path(os.environ.get("RDD_FORMATTER_CACHE", ROOT / ".gradle/rdd-formatters"))
    if args.patch_output is not None:
        return check_with_patch(config, paths, cache, args.patch_output)
    for language, suffix in (("kotlin", ".kt"), ("java", ".java")):
        selected = [path for path in paths if path.suffix == suffix]
        if not selected:
            continue
        tool = config["tools"][language]
        jar = tool_jar(language, tool, cache)
        if args.apply:
            status = apply_until_stable(language, tool, jar, selected)
        else:
            status = subprocess.run(command(language, tool, jar, selected, False), check=False).returncode
        if status:
            return status
        print(f"{language} {tool['version']}: {len(selected)} files {'formatted' if args.apply else 'checked'}")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except (OSError, ValueError, zipfile.BadZipFile) as failure:
        print(f"Formatting failed: {failure}", file=sys.stderr)
        raise SystemExit(1)

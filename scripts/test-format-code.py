#!/usr/bin/env python3
"""Offline contract tests for pinned formatter selection, integrity and check mode."""

import hashlib
import importlib.util
import io
import urllib.error
import json
from pathlib import Path
import subprocess
import tempfile
import unittest
from unittest.mock import patch
import zipfile

spec = importlib.util.spec_from_file_location("format_code", Path(__file__).with_name("format-code.py"))
formatter = importlib.util.module_from_spec(spec)
spec.loader.exec_module(formatter)


class FormattingTest(unittest.TestCase):
    def test_configuration_pins_versions_and_only_known_host(self):
        config = json.loads(formatter.CONFIG.read_text())
        self.assertEqual({"kotlin", "java"}, set(config["tools"]))
        self.assertEqual("0.54", config["tools"]["kotlin"]["version"])
        self.assertEqual("1.24.0", config["tools"]["java"]["version"])
        for tool in config["tools"].values():
            self.assertTrue(tool["url"].startswith("https://repo.maven.apache.org/maven2/"))
            self.assertRegex(tool["sha256"], r"^[a-f0-9]{64}$")

    def test_scope_is_bounded_deduplicated_and_source_only(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / "one.kt").write_text("class One")
            (root / "two.java").write_text("class Two {}")
            (root / "notes.md").write_text("not source")
            selected = formatter.targets(root, ["*", "*.kt"])
            self.assertEqual(["one.kt", "two.java"], [p.name for p in selected])
            for invalid in ("../*.java", "/tmp/*.kt"):
                with self.assertRaises(ValueError):
                    formatter.targets(root, [invalid])

    def test_scope_rejects_symlinks(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / "source.kt").write_text("class One")
            (root / "link.kt").symlink_to(root / "source.kt")
            with self.assertRaises(ValueError):
                formatter.targets(root, ["*.kt"])

    def test_cached_jar_requires_matching_hash_and_executable_manifest(self):
        with tempfile.TemporaryDirectory() as directory:
            jar = Path(directory) / "tool.jar"
            with zipfile.ZipFile(jar, "w") as archive:
                archive.writestr("META-INF/MANIFEST.MF", "Manifest-Version: 1.0\nMain-Class: Test\n")
            digest = hashlib.sha256(jar.read_bytes()).hexdigest()
            formatter.validate_jar(jar, digest)
            for invalid in ("0" * 64, "not a hash"):
                with self.assertRaises(ValueError):
                    formatter.validate_jar(jar, invalid)
            jar.write_bytes(b"partial download")
            with self.assertRaises((ValueError, zipfile.BadZipFile)):
                formatter.validate_jar(jar, digest)

    def test_cache_metadata_cannot_override_the_repository_content_lock(self):
        with tempfile.TemporaryDirectory() as directory:
            cache = Path(directory)
            jar = cache / "java-test.jar"
            with zipfile.ZipFile(jar, "w") as archive:
                archive.writestr("META-INF/MANIFEST.MF", "Manifest-Version: 1.0\nMain-Class: Test\n")
            digest = hashlib.sha256(jar.read_bytes()).hexdigest()
            tool = {"version": "test", "sha256": digest}
            with patch.object(formatter.urllib.request, "urlopen") as network:
                self.assertEqual(jar, formatter.tool_jar("java", tool, cache))
                network.assert_not_called()
            with zipfile.ZipFile(jar, "a") as archive:
                archive.writestr("changed.txt", "Different executable content")
            jar.with_suffix(".sha256").write_text(hashlib.sha256(jar.read_bytes()).hexdigest())
            with patch.object(formatter.urllib.request, "urlopen") as network:
                with self.assertRaises(ValueError):
                    formatter.tool_jar("java", tool, cache)
                network.assert_not_called()

    def test_unexpected_download_host_is_rejected_without_network(self):
        with patch.object(formatter.urllib.request, "urlopen") as network:
            with self.assertRaises(ValueError):
                formatter.fetch("https://example.com/formatter.jar", 100)
            network.assert_not_called()

    def test_throttled_download_honours_retry_after_and_returns_exact_bytes(self):
        url = "https://repo.maven.apache.org/maven2/tool.jar"
        failed_body = io.BytesIO(b"throttled, not executable content")
        error = urllib.error.HTTPError(url, 429, "Too Many Requests", {"Retry-After": "3"}, failed_body)
        with patch.object(formatter.urllib.request, "urlopen", side_effect=[error, io.BytesIO(b"exact bytes")]) as network, \
                patch.object(formatter.time, "sleep") as sleep, \
                patch.object(formatter.time, "monotonic", return_value=0):
            self.assertEqual(b"exact bytes", formatter.fetch(url, 100))
        self.assertTrue(failed_body.closed)
        self.assertEqual(2, network.call_count)
        sleep.assert_called_once_with(3)

    def test_retry_after_http_date_is_respected(self):
        url = "https://repo.maven.apache.org/maven2/tool.jar"
        error = urllib.error.HTTPError(url, 503, "Unavailable", {"Retry-After": "Thu, 01 Jan 1970 00:00:03 GMT"}, None)
        with patch.object(formatter.urllib.request, "urlopen", side_effect=[error, io.BytesIO(b"tool")]), \
                patch.object(formatter.time, "sleep") as sleep, \
                patch.object(formatter.time, "time", return_value=0), \
                patch.object(formatter.time, "monotonic", return_value=0):
            self.assertEqual(b"tool", formatter.fetch(url, 100))
        sleep.assert_called_once_with(3)

    def test_download_retries_are_bounded_and_permanent_or_long_backoff_fails_closed(self):
        url = "https://repo.maven.apache.org/maven2/tool.jar"
        for code, headers, calls in [(404, {}, 1), (429, {"Retry-After": "120"}, 1), (503, {}, 3)]:
            with self.subTest(code=code, headers=headers):
                error = urllib.error.HTTPError(url, code, "Unavailable", headers, None)
                with patch.object(formatter.urllib.request, "urlopen", side_effect=error) as network, \
                        patch.object(formatter.time, "sleep") as sleep, \
                        patch.object(formatter.time, "monotonic", return_value=0):
                    with self.assertRaises(urllib.error.HTTPError):
                        formatter.fetch(url, 100)
                self.assertEqual(calls, network.call_count)
                self.assertEqual([1, 2] if calls == 3 else [], [c.args[0] for c in sleep.call_args_list])
        # A server delay must fit the ORIGINAL budget, not a new budget for each attempt.
        error = urllib.error.HTTPError(url, 429, "Throttled", {"Retry-After": "2"}, None)
        with patch.object(formatter.urllib.request, "urlopen", side_effect=error) as network, \
                patch.object(formatter.time, "sleep") as sleep, \
                patch.object(formatter.time, "monotonic", side_effect=[0, 0, 59]):
            with self.assertRaises(urllib.error.HTTPError):
                formatter.fetch(url, 100)
        self.assertEqual(1, network.call_count)
        sleep.assert_not_called()

    def test_size_and_time_limit_failures_are_not_retried(self):
        url = "https://repo.maven.apache.org/maven2/tool.jar"
        for data, clock in [(b"12345", [0, 0, 0]), (b"ok", [0, 0, 61])]:
            with self.subTest(data=data, clock=clock), \
                    patch.object(formatter.urllib.request, "urlopen", return_value=io.BytesIO(data)) as network, \
                    patch.object(formatter.time, "sleep") as sleep, \
                    patch.object(formatter.time, "monotonic", side_effect=clock):
                with self.assertRaises(ValueError):
                    formatter.fetch(url, 4)
                self.assertEqual(1, network.call_count)
                sleep.assert_not_called()

    def test_check_commands_never_request_in_place_replacement(self):
        jar = Path("tool.jar")
        source = [Path("sample.kt")]
        kotlin = formatter.command("kotlin", {"arguments": ["--kotlinlang-style"]}, jar, source, False)
        java = formatter.command("java", {}, jar, [Path("Sample.java")], False)
        for command in (kotlin, java):
            self.assertIn("--dry-run", command)
            self.assertIn("--set-exit-if-changed", command)
            self.assertNotIn("--replace", command)
        self.assertIn("--replace", formatter.command("java", {}, jar, source, True))

    def test_patch_check_returns_failure_without_changing_original(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            source = root / "One.kt"
            source.write_text("class One{}\n")
            output = root / "build/format.patch"

            def run(command, check):
                Path(command[-1]).write_text("class One {}\n")
                return subprocess.CompletedProcess(command, 0)

            with patch.object(formatter, "ROOT", root), \
                    patch.object(formatter, "tool_jar", return_value=root / "tool.jar"), \
                    patch.object(formatter.subprocess, "run", side_effect=run):
                status = formatter.check_with_patch({"tools": {"kotlin": {}}}, [source], root, output)
            self.assertEqual(1, status)
            self.assertEqual("class One{}\n", source.read_text())
            self.assertIn("--- a/One.kt\n+++ b/One.kt\n", output.read_text())
            self.assertIn("+class One {}", output.read_text())

    def test_patch_check_passes_only_when_real_tool_output_matches(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            source = root / "One.java"
            source.write_text("class One {}\n")
            output = root / "format.patch"
            with patch.object(formatter, "ROOT", root), \
                    patch.object(formatter, "tool_jar", return_value=root / "tool.jar"), \
                    patch.object(formatter.subprocess, "run", return_value=subprocess.CompletedProcess([], 0)):
                status = formatter.check_with_patch({"tools": {"java": {}}}, [source], root, output)
            self.assertEqual(0, status)
            self.assertEqual("", output.read_text())

    def test_generated_patch_applies_even_when_original_has_no_final_newline(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            original = root / "source"
            formatted = root / "formatted"
            original.mkdir()
            formatted.mkdir()
            source = original / "One.kt"
            source.write_text("class One{}")
            (formatted / "One.kt").write_text("class One {}\n")
            result = formatter.difference_patch(original, formatted, [source])
            patch_file = root / "format.patch"
            patch_file.write_text(result)
            subprocess.run(["git", "init", "--quiet", str(original)], check=True)
            subprocess.run(["git", "-C", str(original), "apply", str(patch_file)], check=True)
            self.assertEqual("class One {}\n", source.read_text())

    def test_patch_check_formats_to_a_byte_stable_result_before_writing_diagnostics(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            source = root / "One.java"
            source.write_text("first\n")
            output = root / "format.patch"

            def run(command, check):
                target = Path(command[-1])
                target.write_text({"first\n": "middle\n", "middle\n": "final\n",
                                   "final\n": "final\n"}[target.read_text()])
                return subprocess.CompletedProcess(command, 0)

            with patch.object(formatter, "ROOT", root), \
                    patch.object(formatter, "tool_jar", return_value=root / "tool.jar"), \
                    patch.object(formatter.subprocess, "run", side_effect=run) as process:
                status = formatter.check_with_patch({"tools": {"java": {}}}, [source], root, output)
            self.assertEqual(1, status)
            self.assertEqual(3, process.call_count)
            self.assertEqual("first\n", source.read_text())
            self.assertIn("+final\n", output.read_text())
            self.assertNotIn("middle", output.read_text())

    def test_apply_formats_the_requested_files_until_the_bytes_are_stable(self):
        with tempfile.TemporaryDirectory() as directory:
            source = Path(directory) / "One.kt"
            source.write_text("first\n")

            def run(command, check):
                target = Path(command[-1])
                target.write_text({"first\n": "middle\n", "middle\n": "final\n",
                                   "final\n": "final\n"}[target.read_text()])
                return subprocess.CompletedProcess(command, 0)

            with patch.object(formatter.subprocess, "run", side_effect=run) as process:
                status = formatter.apply_until_stable("kotlin", {}, Path("tool.jar"), [source])
            self.assertEqual(0, status)
            self.assertEqual(3, process.call_count)
            self.assertEqual("final\n", source.read_text())

    def test_nonconverging_patch_check_is_bounded_and_preserves_the_original(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            source = root / "One.java"
            source.write_text("first\n")
            output = root / "format.patch"

            def run(command, check):
                target = Path(command[-1])
                target.write_text(target.read_text() + " ")
                return subprocess.CompletedProcess(command, 0)

            with patch.object(formatter, "ROOT", root), \
                    patch.object(formatter, "tool_jar", return_value=root / "tool.jar"), \
                    patch.object(formatter.subprocess, "run", side_effect=run) as process:
                with self.assertRaisesRegex(ValueError, "did not converge after 4 passes"):
                    formatter.check_with_patch({"tools": {"java": {}}}, [source], root, output)
            self.assertEqual(formatter.MAX_FORMAT_PASSES, process.call_count)
            self.assertEqual("first\n", source.read_text())
            self.assertFalse(output.exists())

    def test_tool_failure_cannot_be_mistaken_for_unchanged_output(self):
        with tempfile.TemporaryDirectory() as directory:
            source = Path(directory) / "One.kt"
            source.write_text("unchanged\n")
            with patch.object(formatter.subprocess, "run",
                              return_value=subprocess.CompletedProcess([], 7)) as process:
                status = formatter.apply_until_stable("kotlin", {}, Path("tool.jar"), [source])
            self.assertEqual(7, status)
            self.assertEqual(1, process.call_count)
            self.assertEqual("unchanged\n", source.read_text())


if __name__ == "__main__":
    unittest.main(verbosity=2)

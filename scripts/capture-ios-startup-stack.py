#!/usr/bin/env python3
"""Get a native stack for an early simulator launch exit on the current CI runner."""

import re
import subprocess
import sys


def main(device):
    try:
        launch = subprocess.run(
            [
                "xcrun", "simctl", "launch", "--wait-for-debugger",
                device, "org.racedaydreams.ios",
            ],
            check=True,
            capture_output=True,
            text=True,
            timeout=60,
        )
        print(launch.stdout, end="", flush=True)
        pid = re.search(r"org\.racedaydreams\.ios:\s*(\d+)\s*$", launch.stdout)
        if pid is None:
            raise ValueError("Simulator launch did not report a process ID")
        return subprocess.run(
            [
                "xcrun", "lldb", "--batch", "-p", pid.group(1),
                "-o", "process continue",
                "-k", "thread backtrace all",
                "-k", "image list -o -f",
            ],
            timeout=90,
        ).returncode
    except (subprocess.SubprocessError, ValueError) as error:
        print(f"Debugger capture failed: {error}", file=sys.stderr)
        if isinstance(error, subprocess.CalledProcessError) and error.stderr:
            print(error.stderr, file=sys.stderr)
        return 1


if __name__ == "__main__":
    sys.exit(main(sys.argv[1]))

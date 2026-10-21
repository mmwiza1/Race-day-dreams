#!/bin/sh
# Runs real domain tests offline, without Android, Gradle or downloaded libraries.
set -eu
project_dir=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
output_dir="$project_dir/domain/build/offline-tests"
mkdir -p "$output_dir"
find "$project_dir/domain/src/main/java" "$project_dir/domain/src/test/java" -name '*.java' -print > "$output_dir/sources.txt"
if command -v javac >/dev/null 2>&1; then
    javac -encoding UTF-8 -source 17 -target 17 -d "$output_dir" @"$output_dir/sources.txt"
else
    java -m jdk.compiler/com.sun.tools.javac.Main -encoding UTF-8 -source 17 -target 17 -d "$output_dir" @"$output_dir/sources.txt"
fi
find "$project_dir/domain/src/test/java" -name '*TestSuite.java' -print | sort | while IFS= read -r suite; do
    relative=${suite#"$project_dir/domain/src/test/java/"}
    class_name=$(printf '%s' "${relative%.java}" | tr / .)
    java -ea -cp "$output_dir:$project_dir/domain/src/test/resources" "$class_name"
done

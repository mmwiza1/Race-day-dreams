#!/bin/sh
# Runs the retained real domain suites against the shared Kotlin/JVM implementation.
set -eu
project_dir=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
output_dir="$project_dir/domain/build/offline-tests"
mkdir -p "$output_dir"
# Kotlin/JVM shared classes and the existing Java suites use one resolved classpath.
# No Android emulator or device is involved. Warm Gradle dependencies allow offline use.
"$project_dir/gradlew" --no-daemon :domain:prepareOfflineTests
find "$project_dir/domain/src/test/java" -name '*TestSuite.java' -print | sort | while IFS= read -r suite; do
    relative=${suite#"$project_dir/domain/src/test/java/"}
    class_name=$(printf '%s' "${relative%.java}" | tr / .)
    java -ea -cp "$output_dir:$project_dir/domain/src/test/resources" "$class_name"
done

#!/usr/bin/env bash
# Compiles the application and runs the Person 3 self-check.
# Usage: bash tests/run-person3-tests.sh [submissionsFolder]   (default: submissions)
set -euo pipefail
cd "$(dirname "$0")/.."
bash compile.sh
mkdir -p test-classes
javac -encoding UTF-8 -cp classes -d test-classes tests/Person3Test.java
java -cp "classes:test-classes" Person3Test "${1:-submissions}"

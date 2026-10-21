#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
mkdir -p classes
# Compile all Java source files in the scaffold, including unreferenced modules.
find src -name '*.java' -print0 | xargs -0 javac -d classes
printf 'Compilation successful.\n'

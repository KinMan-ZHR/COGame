#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"
JAVA_BIN="${JAVA_HOME:-/home/kinman/.local/opt/temurin-17}/bin/java"
BUILD_JAVA_HOME="${JAVA_HOME:-/home/kinman/.local/opt/temurin-17}"
MAVEN_BIN="/home/kinman/.local/opt/apache-maven-3.9.16/bin/mvn"
JAR_PATH="$ROOT_DIR/cogame-client/target/cogame-client-2.3.0.jar"
CLASS_MARKER="$ROOT_DIR/cogame-ai/target/classes/person/kinman/cogame/ai/cli/LlmBattleCli.class"

if [ ! -f "$JAR_PATH" ] || [ ! -f "$CLASS_MARKER" ]; then
    JAVA_HOME="$BUILD_JAVA_HOME" "$MAVEN_BIN" -f "$ROOT_DIR/pom.xml" package -DskipTests -q
fi

RUNTIME_JAR="$(mktemp /tmp/cogame-llm-XXXXXX.jar)"
cp "$JAR_PATH" "$RUNTIME_JAR"
trap 'rm -f "$RUNTIME_JAR"' EXIT

"$JAVA_BIN" -cp "$RUNTIME_JAR" person.kinman.cogame.ai.cli.LlmBattleCli "$@"

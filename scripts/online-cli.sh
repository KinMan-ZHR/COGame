#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"
JAVA_BIN="${JAVA_HOME:-/home/kinman/.local/opt/temurin-17}/bin/java"
JAR_PATH="$ROOT_DIR/cogame-client/target/cogame-client-2.3.0.jar"

if [ ! -f "$JAR_PATH" ]; then
    echo "未检测到已编译的 Fat-JAR，正在自动构建..."
    /home/kinman/.local/opt/apache-maven-3.9.16/bin/mvn -f "$ROOT_DIR/pom.xml" install -DskipTests -q
fi

"$JAVA_BIN" -cp "$JAR_PATH" person.kinman.cogame.client.cli.OnlineCli "$@"

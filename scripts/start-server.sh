#!/usr/bin/env bash
set -euo pipefail

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
JAR="$DIR/cogame-server/target/cogame-server-2.0.0.jar"
PID_FILE="$DIR/logs/server.pid"
LOG_FILE="$DIR/logs/server.log"
JAVA_BIN="${JAVA_HOME:-/home/kinman/.local/opt/temurin-17}/bin/java"

if [[ -f "$PID_FILE" ]]; then
    PID="$(cat "$PID_FILE")"
    if kill -0 "$PID" 2>/dev/null; then
        echo "COGame 服务端已在运行中 (PID: $PID)"
        exit 0
    else
        rm -f "$PID_FILE"
    fi
fi

if [[ ! -f "$JAR" ]]; then
    echo "未找到服务端 JAR 包: $JAR，请先执行 mvn package" >&2
    exit 1
fi

echo "正在启动 COGame 联机服务端 (tmux 守护会话: cogame-server, 端口: 8088)..."
tmux new-session -d -s cogame-server "$JAVA_BIN -jar \"$JAR\" 8088 >> \"$LOG_FILE\" 2>&1"
sleep 1

if tmux has-session -t cogame-server 2>/dev/null; then
    echo "COGame 服务端启动成功！(tmux session: cogame-server)"
    echo "查看实时控制台: tmux attach -t cogame-server"
    echo "查看日志: tail -f $LOG_FILE"
else
    echo "启动失败，请检查日志: $LOG_FILE" >&2
    exit 1
fi

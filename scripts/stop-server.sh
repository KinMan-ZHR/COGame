#!/usr/bin/env bash
set -euo pipefail

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PID_FILE="$DIR/logs/server.pid"

if tmux has-session -t cogame-server 2>/dev/null; then
    echo "正在停止 COGame 服务端 (tmux session: cogame-server)..."
    tmux kill-session -t cogame-server
    rm -f "$PID_FILE"
    echo "已成功停止"
    exit 0
fi

if [[ ! -f "$PID_FILE" ]]; then
    echo "COGame 服务端未运行"
    exit 0
fi

PID="$(cat "$PID_FILE")"
if kill -0 "$PID" 2>/dev/null; then
    echo "正在停止 COGame 服务端 (PID: $PID)..."
    kill "$PID"
    rm -f "$PID_FILE"
    echo "已成功停止"
else
    echo "进程已不存在，清除残留 PID 文件"
    rm -f "$PID_FILE"
fi

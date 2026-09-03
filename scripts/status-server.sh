#!/usr/bin/env bash
set -euo pipefail

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PID_FILE="$DIR/logs/server.pid"

if tmux has-session -t cogame-server 2>/dev/null; then
    echo "COGame 服务端状态: [运行中] (tmux session: cogame-server)"
    exit 0
fi

if [[ -f "$PID_FILE" ]]; then
    PID="$(cat "$PID_FILE")"
    if kill -0 "$PID" 2>/dev/null; then
        echo "COGame 服务端状态: [运行中] (PID: $PID)"
        exit 0
    fi
fi

echo "COGame 服务端状态: [未运行]"
exit 1

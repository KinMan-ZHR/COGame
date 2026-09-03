#!/usr/bin/env bash
set -euo pipefail

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PID_FILE="$DIR/logs/server.pid"

if tmux has-session -t cogame-server 2>/dev/null; then
    echo "正在停止 COGame 服务端 (tmux session: cogame-server)..."
    tmux kill-session -t cogame-server
fi

pkill -f "cogame-server.*jar" 2>/dev/null || true
rm -f "$PID_FILE"

# 等待端口 8088 彻底释放
for i in {1..10}; do
    if ! ss -lnt | grep -q ":8088 "; then
        break
    fi
    pkill -9 -f "cogame-server.*jar" 2>/dev/null || true
    sleep 0.3
done

echo "已成功停止"
exit 0

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

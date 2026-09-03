#!/usr/bin/env bash
set -euo pipefail

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DIST_DIR="$DIR/distribution"
PORT=8080
LOG_FILE="$DIR/logs/web.log"

if tmux has-session -t cogame-web 2>/dev/null; then
    echo "COGame 分发服务已在运行中 (tmux session: cogame-web, 端口: $PORT)"
    exit 0
fi

echo "正在启动 COGame 客户端分发 Web 站点 (tmux 守护会话: cogame-web, 端口: $PORT)..."
tmux new-session -d -s cogame-web "cd \"$DIST_DIR\" && python3 -m http.server $PORT >> \"$LOG_FILE\" 2>&1"
sleep 1

if tmux has-session -t cogame-web 2>/dev/null; then
    echo "分发 Web 站点启动成功！"
    echo "浏览器访问地址: http://127.0.0.1:$PORT/ (或宿主机局域网/公网IP:$PORT)"
else
    echo "分发 Web 站点启动失败，请检查日志: $LOG_FILE" >&2
    exit 1
fi

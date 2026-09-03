#!/usr/bin/env bash
set -euo pipefail

if tmux has-session -t cogame-web 2>/dev/null; then
    echo "正在停止 COGame 分发 Web 站点 (tmux session: cogame-web)..."
    tmux kill-session -t cogame-web
    echo "已成功停止"
else
    echo "COGame 分发 Web 站点未运行"
fi

#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CLI="$SCRIPT_DIR/battle-cli.sh"

echo "=========================================================================="
echo "★ COGame 巅峰对决锦标赛：Antigravity (策略均衡) vs Codex (极限压迫)"
echo "★ 规则版本：对称起手能量修正版 (小盘回3/蓄5，大盘回6/蓄11)"
echo "=========================================================================="
echo ""

echo "##########################################################################"
echo "【第一阶段】经典小盘 6 × 6 对决 (三局两胜交替先手)"
echo "##########################################################################"
echo ""
echo ">>> [小盘第1战] Antigravity(P1先手·青) vs Codex(P2后手·金)"
"$CLI" --size 6 --p1 antigravity --p2 codex --games 1

echo ""
echo ">>> [小盘第2战] Codex(P1先手·青) vs Antigravity(P2后手·金)"
"$CLI" --size 6 --p1 codex --p2 antigravity --games 1

echo ""
echo ">>> [小盘第3战] Antigravity(P1先手·青) vs Codex(P2后手·金)"
"$CLI" --size 6 --p1 antigravity --p2 codex --games 1

echo ""
echo "##########################################################################"
echo "【第二阶段】战略大盘 12 × 12 对决 (迷宫战场，约16%中立墙，三局交替先手)"
echo "##########################################################################"
echo ""
echo ">>> [大盘第1战] Antigravity(P1先手·青) vs Codex(P2后手·金)"
"$CLI" --size 12 --p1 antigravity --p2 codex --games 1

echo ""
echo ">>> [大盘第2战] Codex(P1先手·青) vs Antigravity(P2后手·金)"
"$CLI" --size 12 --p1 codex --p2 antigravity --games 1

echo ""
echo ">>> [大盘第3战] Antigravity(P1先手·青) vs Codex(P2后手·金)"
"$CLI" --size 12 --p1 antigravity --p2 codex --games 1

echo ""
echo "=========================================================================="
echo "★ 锦标赛全战罢！"
echo "=========================================================================="

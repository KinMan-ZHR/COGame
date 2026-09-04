#!/usr/bin/env python3
import sys, json

def solve():
    raw = sys.stdin.read()
    if not raw.strip():
        print(json.dumps({"choice": 0}))
        return

    data = json.loads(raw)
    candidates = data.get("candidates", [])
    if not candidates:
        print(json.dumps({"choice": 0}))
        return

    turn = data.get("turn", 1)
    my_energy = data.get("myEnergy", 3)

    best_choice = 0
    best_score = -1e9

    for c in candidates:
        outcome = c.get("outcome", "NORMAL")
        if outcome == "WIN":
            print(json.dumps({"choice": c["id"]}))
            return
        if outcome == "LOSS":
            continue

        my_t = c.get("myTerritory", 18)
        opp_t = c.get("oppTerritory", 18)
        steps = c.get("steps", 0)
        dist = c.get("distance", 0)
        target = c.get("target", [0, 0])
        tr, tc = target[0], target[1]
        opp_deg = c.get("oppDegree", 4)
        my_deg = c.get("myDegree", 4)
        my_e = c.get("myEdges", 0)
        opp_e = c.get("oppEdges", 0)

        # 核心领地控制权
        score = (my_t - opp_t) * 45.0

        # 最短路隔断拉伸
        score += dist * 16.0

        # 中路压制 (靠近 2.5, 2.5)
        center_dist = abs(tr - 2.5) + abs(tc - 2.5)
        score += (5.0 - center_dist) * 8.0

        # 开局前 3 回合抢中路，禁止保守缩壳
        if turn <= 3:
            score += steps * 6.0
        else:
            energy_rem = my_energy - steps
            score += energy_rem * 3.0

        # 出口封堵
        score += (4 - opp_deg) * 18.0
        if outcome == "PRESSURE":
            score += 70.0
        if outcome == "FRAGILE":
            score -= 120.0

        score += my_deg * 8.0
        score += (my_e - opp_e) * 1.5

        if score > best_score:
            best_score = score
            best_choice = c["id"]

    print(json.dumps({"choice": best_choice}))

if __name__ == "__main__":
    solve()

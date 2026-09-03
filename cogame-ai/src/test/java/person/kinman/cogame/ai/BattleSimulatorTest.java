package person.kinman.cogame.ai;

import org.junit.jupiter.api.Test;
import person.kinman.cogame.core.action.GameAction;
import person.kinman.cogame.core.model.Board;
import person.kinman.cogame.core.model.Direction;
import person.kinman.cogame.core.model.GameState;
import person.kinman.cogame.core.model.PlayerState;
import person.kinman.cogame.core.rule.GameEngine;
import person.kinman.cogame.core.rule.GameEvaluator;

import java.util.*;

/**
 * 实战对局模拟器：Antigravity (策略均衡型) vs Codex (极限压迫型)
 */
public class BattleSimulatorTest {

    public static class MatchTelemetry {
        public String matchName;
        public int boardSize;
        public String p1Name;
        public String p2Name;
        public int totalTurns;
        public String winner;
        public String winReason;
        public int p1Territory;
        public int p2Territory;

        public List<Integer> p1Steps = new ArrayList<>();
        public List<Integer> p2Steps = new ArrayList<>();
        public List<Integer> p1EnergyAtStart = new ArrayList<>();
        public List<Integer> p2EnergyAtStart = new ArrayList<>();
        public int p1CapHits = 0;
        public int p2CapHits = 0;
        public int p1ZeroStepTurns = 0;
        public int p2ZeroStepTurns = 0;
        public int p1SprintTurns = 0;
        public int p2SprintTurns = 0;
        public List<String> turnLogs = new ArrayList<>();
    }

    public interface CombatantAi {
        String getName();
        AiDecision planTurn(GameState state, int playerId);
    }

    /**
     * Antigravity AI: 宏观拓扑割裂与能量统筹型 (宏观领地占优 + 节能高效)
     */
    public static class AntigravityAi implements CombatantAi {
        @Override
        public String getName() {
            return "Antigravity";
        }

        @Override
        public AiDecision planTurn(GameState state, int myId) {
            PlayerState me = state.getPlayer(myId);
            PlayerState opp = state.getPlayer(myId == 1 ? 2 : 1);
            Board board = state.getBoard();

            int maxSteps = Math.max(1, me.getEnergy());
            Map<Long, List<Direction>> reachablePaths = getReachable(board, me.getR(), me.getC(), opp.getR(), opp.getC(), maxSteps);

            double bestScore = -Double.MAX_VALUE;
            List<GameAction> bestActions = new ArrayList<>();
            String desc = "原地锁边";

            for (Map.Entry<Long, List<Direction>> entry : reachablePaths.entrySet()) {
                long key = entry.getKey();
                int r = GameEvaluator.decodeR(key);
                int c = GameEvaluator.decodeC(key);
                List<Direction> path = entry.getValue();

                for (Direction dir : Direction.values()) {
                    if (!board.isConnected(r, c, dir)) continue;

                    Board simBoard = board.copy();
                    simBoard.lockEdge(r, c, dir, myId);

                    double score = evaluate(simBoard, r, c, opp.getR(), opp.getC(), path.size(), me.getEnergy());
                    if (score > bestScore) {
                        bestScore = score;
                        desc = String.format("走%d步至(%d,%d)锁[%s]", path.size(), r, c, dir.getName());
                        bestActions = buildActions(me.getDirection(), path, dir);
                    }
                }
            }

            if (bestActions.isEmpty()) {
                bestActions.add(GameAction.lock());
            }
            return new AiDecision(bestActions, bestScore, desc);
        }

        private double evaluate(Board simBoard, int myR, int myC, int oppR, int oppC, int steps, int currentEnergy) {
            boolean connected = GameEvaluator.hasPath(simBoard, myR, myC, oppR, oppC);
            if (!connected) {
                Set<Long> myTerr = GameEvaluator.getConnectedComponent(simBoard, myR, myC);
                Set<Long> oppTerr = GameEvaluator.getConnectedComponent(simBoard, oppR, oppC);
                int diff = myTerr.size() - oppTerr.size();
                return diff > 0 ? (1000000.0 + diff * 1000.0) : (-1000000.0 + diff * 1000.0);
            }

            // 1. Voronoi 领地划分
            int voronoi = calculateVoronoi(simBoard, myR, myC, oppR, oppC);
            // 2. 空间距离拉伸
            List<int[]> path = GameEvaluator.findPath(simBoard, myR, myC, oppR, oppC);
            int pathLen = path.isEmpty() ? 0 : path.size();
            // 3. 节能偏好：非必要不进行长途马拉松消耗，保持战术储备
            double energyReserveScore = (currentEnergy - steps) * 4.0;

            return voronoi * 25.0 + pathLen * 8.0 + energyReserveScore;
        }
    }

    /**
     * Codex AI: 极限前线压迫与窒息包夹型 (猛扑对手咽喉 + 强封出口)
     */
    public static class CodexAi implements CombatantAi {
        @Override
        public String getName() {
            return "Codex";
        }

        @Override
        public AiDecision planTurn(GameState state, int myId) {
            PlayerState me = state.getPlayer(myId);
            PlayerState opp = state.getPlayer(myId == 1 ? 2 : 1);
            Board board = state.getBoard();

            int maxSteps = Math.max(1, me.getEnergy());
            Map<Long, List<Direction>> reachablePaths = getReachable(board, me.getR(), me.getC(), opp.getR(), opp.getC(), maxSteps);

            double bestScore = -Double.MAX_VALUE;
            List<GameAction> bestActions = new ArrayList<>();
            String desc = "原地锁边";

            for (Map.Entry<Long, List<Direction>> entry : reachablePaths.entrySet()) {
                long key = entry.getKey();
                int r = GameEvaluator.decodeR(key);
                int c = GameEvaluator.decodeC(key);
                List<Direction> path = entry.getValue();

                for (Direction dir : Direction.values()) {
                    if (!board.isConnected(r, c, dir)) continue;

                    Board simBoard = board.copy();
                    simBoard.lockEdge(r, c, dir, myId);

                    double score = evaluate(simBoard, r, c, opp.getR(), opp.getC(), path.size());
                    if (score > bestScore) {
                        bestScore = score;
                        desc = String.format("冲刺%d步至(%d,%d)突袭锁[%s]", path.size(), r, c, dir.getName());
                        bestActions = buildActions(me.getDirection(), path, dir);
                    }
                }
            }

            if (bestActions.isEmpty()) {
                bestActions.add(GameAction.lock());
            }
            return new AiDecision(bestActions, bestScore, desc);
        }

        private double evaluate(Board simBoard, int myR, int myC, int oppR, int oppC, int steps) {
            boolean connected = GameEvaluator.hasPath(simBoard, myR, myC, oppR, oppC);
            if (!connected) {
                Set<Long> myTerr = GameEvaluator.getConnectedComponent(simBoard, myR, myC);
                Set<Long> oppTerr = GameEvaluator.getConnectedComponent(simBoard, oppR, oppC);
                int diff = myTerr.size() - oppTerr.size();
                return diff > 0 ? (1000000.0 + diff * 1000.0) : (-1000000.0 + diff * 1000.0);
            }

            // 1. 对手出口封堵窒息度 (极高偏好)
            int oppDegree = simBoard.getOpenDirections(oppR, oppC).size();
            double suffocation = (4 - oppDegree) * 45.0;

            // 2. 贴脸压迫：曼哈顿逼近
            int manhattan = Math.abs(myR - oppR) + Math.abs(myC - oppC);
            double proximityScore = -manhattan * 6.0;

            // 3. Voronoi 领地
            int voronoi = calculateVoronoi(simBoard, myR, myC, oppR, oppC);

            return voronoi * 15.0 + suffocation + proximityScore;
        }
    }

    private static Map<Long, List<Direction>> getReachable(Board board, int startR, int startC, int oppR, int oppC, int maxSteps) {
        Map<Long, List<Direction>> paths = new HashMap<>();
        paths.put(GameEvaluator.encode(startR, startC), new ArrayList<>());

        Queue<int[]> queue = new ArrayDeque<>();
        queue.add(new int[]{startR, startC, 0});

        while (!queue.isEmpty()) {
            int[] curr = queue.poll();
            int r = curr[0];
            int c = curr[1];
            int d = curr[2];
            long currKey = GameEvaluator.encode(r, c);
            List<Direction> curPath = paths.get(currKey);

            if (d >= maxSteps) continue;

            for (Direction dir : Direction.values()) {
                if (board.isConnected(r, c, dir)) {
                    int nr = r + dir.getDr();
                    int nc = c + dir.getDc();
                    if (nr == oppR && nc == oppC) continue; // 身位刚性阻挡

                    long nextKey = GameEvaluator.encode(nr, nc);
                    if (!paths.containsKey(nextKey)) {
                        List<Direction> nextPath = new ArrayList<>(curPath);
                        nextPath.add(dir);
                        paths.put(nextKey, nextPath);
                        queue.add(new int[]{nr, nc, d + 1});
                    }
                }
            }
        }
        return paths;
    }

    private static int calculateVoronoi(Board board, int p1R, int p1C, int p2R, int p2C) {
        int rows = board.getRows();
        int cols = board.getCols();
        int[][] d1 = bfsDist(board, p1R, p1C);
        int[][] d2 = bfsDist(board, p2R, p2C);

        int p1Count = 0;
        int p2Count = 0;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int dist1 = d1[r][c];
                int dist2 = d2[r][c];
                if (dist1 != -1 && (dist2 == -1 || dist1 < dist2)) {
                    p1Count++;
                } else if (dist2 != -1 && (dist1 == -1 || dist2 < dist1)) {
                    p2Count++;
                }
            }
        }
        return p1Count - p2Count;
    }

    private static int[][] bfsDist(Board board, int startR, int startC) {
        int rows = board.getRows();
        int cols = board.getCols();
        int[][] dist = new int[rows][cols];
        for (int[] row : dist) Arrays.fill(row, -1);

        Queue<int[]> queue = new ArrayDeque<>();
        queue.add(new int[]{startR, startC});
        dist[startR][startC] = 0;

        while (!queue.isEmpty()) {
            int[] curr = queue.poll();
            int r = curr[0];
            int c = curr[1];
            int d = dist[r][c];

            for (Direction dir : Direction.values()) {
                if (board.isConnected(r, c, dir)) {
                    int nr = r + dir.getDr();
                    int nc = c + dir.getDc();
                    if (dist[nr][nc] == -1) {
                        dist[nr][nc] = d + 1;
                        queue.add(new int[]{nr, nc});
                    }
                }
            }
        }
        return dist;
    }

    private static List<GameAction> buildActions(Direction curDir, List<Direction> path, Direction lockDir) {
        List<GameAction> actions = new ArrayList<>();
        Direction d = curDir;
        for (Direction step : path) {
            actions.add(GameAction.changeDirMove(step));
            d = step;
        }
        while (d != lockDir) {
            actions.add(GameAction.rotate());
            d = d.clockwise();
        }
        actions.add(GameAction.lock());
        return actions;
    }

    public static MatchTelemetry runMatch(int boardSize, CombatantAi ai1, CombatantAi ai2, String matchName) {
        GameState state = new GameState(boardSize);
        state.getP1().setName(ai1.getName());
        state.getP2().setName(ai2.getName());

        MatchTelemetry tele = new MatchTelemetry();
        tele.matchName = matchName;
        tele.boardSize = boardSize;
        tele.p1Name = ai1.getName();
        tele.p2Name = ai2.getName();

        int maxEnergy = state.getMaxEnergy();
        int sprintThreshold = (boardSize == 6) ? 4 : 6;

        int turnCounter = 0;
        int maxTurns = 200;

        while (!state.isOver() && turnCounter < maxTurns) {
            turnCounter++;
            int currPlayerId = state.getCurrentTurn();
            CombatantAi currentAi = (currPlayerId == 1) ? ai1 : ai2;
            PlayerState currPlayer = state.getCurrentPlayer();

            int energyBeforeTurn = currPlayer.getEnergy();
            boolean isCapHit = (energyBeforeTurn == maxEnergy);

            AiDecision decision = currentAi.planTurn(state, currPlayerId);

            // 执行决策动作
            for (GameAction act : decision.getActions()) {
                GameEngine.executeAction(state, currPlayerId, act);
            }

            int stepsMoved = 0;
            for (GameAction act : decision.getActions()) if (act.getType() == GameAction.Type.CHANGE_DIR_MOVE || act.getType() == GameAction.Type.MOVE) stepsMoved++; // 结算前已走步数
            // 记录遥测
            if (currPlayerId == 1) {
                tele.p1Steps.add(stepsMoved);
                tele.p1EnergyAtStart.add(energyBeforeTurn);
                if (isCapHit) tele.p1CapHits++;
                if (stepsMoved == 0) tele.p1ZeroStepTurns++;
                if (stepsMoved >= sprintThreshold) tele.p1SprintTurns++;
            } else {
                tele.p2Steps.add(stepsMoved);
                tele.p2EnergyAtStart.add(energyBeforeTurn);
                if (isCapHit) tele.p2CapHits++;
                if (stepsMoved == 0) tele.p2ZeroStepTurns++;
                if (stepsMoved >= sprintThreshold) tele.p2SprintTurns++;
            }

            String log = String.format("  [第%02d回合 | %s(P%d)] 开局能量:%d/%d | 走%d步 | 决策: %s | 当前位置:(%d,%d)",
                    turnCounter, currentAi.getName(), currPlayerId, energyBeforeTurn, maxEnergy,
                    stepsMoved, decision.getDescription(), currPlayer.getR(), currPlayer.getC());
            tele.turnLogs.add(log);
        }

        tele.totalTurns = turnCounter;
        tele.p1Territory = state.getP1Territory();
        tele.p2Territory = state.getP2Territory();
        tele.winner = (state.getWinner() == 1) ? ai1.getName() : (state.getWinner() == 2 ? ai2.getName() : "平局");
        tele.winReason = state.getWinReason();

        return tele;
    }

    @Test
    public void runEmpiricalMatches() {
        System.out.println("==========================================================================");
        System.out.println("★ 开始实战对抗实验：Antigravity (策略均衡) vs Codex (极限压迫)");
        System.out.println("==========================================================================");

        AntigravityAi ag = new AntigravityAi();
        CodexAi cx = new CodexAi();

        // 1. 小盘 6x6 对决三局
        System.out.println("\n##########################################################################");
        System.out.println("【第一阶段】经典小盘 6 × 6 对局 (纯净模式，无预置墙，回3/蓄5)");
        System.out.println("##########################################################################");
        MatchTelemetry s1 = runMatch(6, ag, cx, "小盘第1局: Antigravity(P1先手) vs Codex(P2后手)");
        printMatchReport(s1);

        MatchTelemetry s2 = runMatch(6, cx, ag, "小盘第2局: Codex(P1先手) vs Antigravity(P2后手)");
        printMatchReport(s2);

        MatchTelemetry s3 = runMatch(6, ag, cx, "小盘第3局: Antigravity(P1先手) vs Codex(P2后手)");
        printMatchReport(s3);

        // 2. 大盘 12x12 对决三局
        System.out.println("\n##########################################################################");
        System.out.println("【第二阶段】战略大盘 12 × 12 对局 (迷宫进阶，~16%中立迷宫墙，回6/蓄11)");
        System.out.println("##########################################################################");
        MatchTelemetry l1 = runMatch(12, ag, cx, "大盘第1局: Antigravity(P1先手) vs Codex(P2后手)");
        printMatchReport(l1);

        MatchTelemetry l2 = runMatch(12, cx, ag, "大盘第2局: Codex(P1先手) vs Antigravity(P2后手)");
        printMatchReport(l2);

        MatchTelemetry l3 = runMatch(12, ag, cx, "大盘第3局: Antigravity(P1先手) vs Codex(P2后手)");
        printMatchReport(l3);
    }

    private void printMatchReport(MatchTelemetry m) {
        System.out.println("\n>>> " + m.matchName);
        System.out.println("--------------------------------------------------------------------------");
        for (String log : m.turnLogs) {
            System.out.println(log);
        }
        System.out.println("--------------------------------------------------------------------------");
        System.out.printf("对局结果: 胜者【%s】 | 领地对比: P1(%s)=%d格, P2(%s)=%d格 | 终局判定: %s | 总回合数: %d\n",
                m.winner, m.p1Name, m.p1Territory, m.p2Name, m.p2Territory, m.winReason, m.totalTurns);

        double p1AvgSteps = m.p1Steps.stream().mapToInt(Integer::intValue).average().orElse(0.0);
        double p2AvgSteps = m.p2Steps.stream().mapToInt(Integer::intValue).average().orElse(0.0);
        int p1MaxStep = m.p1Steps.stream().mapToInt(Integer::intValue).max().orElse(0);
        int p2MaxStep = m.p2Steps.stream().mapToInt(Integer::intValue).max().orElse(0);

        System.out.printf("数据统计 P1(%s): 均步=%.2f, 最大单回合冲刺=%d步, 满能量开局=%d次, 0步原地锁边=%d次, 爆发冲刺(>=阈值)=%d次\n",
                m.p1Name, p1AvgSteps, p1MaxStep, m.p1CapHits, m.p1ZeroStepTurns, m.p1SprintTurns);
        System.out.printf("数据统计 P2(%s): 均步=%.2f, 最大单回合冲刺=%d步, 满能量开局=%d次, 0步原地锁边=%d次, 爆发冲刺(>=阈值)=%d次\n",
                m.p2Name, p2AvgSteps, p2MaxStep, m.p2CapHits, m.p2ZeroStepTurns, m.p2SprintTurns);
    }
}

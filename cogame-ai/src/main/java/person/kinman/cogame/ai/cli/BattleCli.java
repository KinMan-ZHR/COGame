package person.kinman.cogame.ai.cli;

import person.kinman.cogame.ai.AiDecision;
import person.kinman.cogame.core.action.GameAction;
import person.kinman.cogame.core.model.Board;
import person.kinman.cogame.core.model.Direction;
import person.kinman.cogame.core.model.GameState;
import person.kinman.cogame.core.model.PlayerState;
import person.kinman.cogame.core.rule.GameEngine;
import person.kinman.cogame.core.rule.GameEvaluator;

import java.util.*;

/**
 * 端脑 AI 终端竞技场 CLI：支持 Antigravity 与 Codex 多轮博弈演练
 */
public class BattleCli {

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_GRAY = "\u001B[90m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_BOLD = "\u001B[1m";

    public interface CombatantAi {
        String getName();
        AiDecision planTurn(GameState state, int playerId);
    }

    /**
     * Antigravity AI: 宏观拓扑割裂与能量统筹型
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

            int voronoi = calculateVoronoi(simBoard, myR, myC, oppR, oppC);
            List<int[]> path = GameEvaluator.findPath(simBoard, myR, myC, oppR, oppC);
            int pathLen = path.isEmpty() ? 0 : path.size();
            double energyReserveScore = (currentEnergy - steps) * 4.0;

            return voronoi * 25.0 + pathLen * 8.0 + energyReserveScore;
        }
    }

    /**
     * Codex AI: 极限前线压迫与近身窒息型
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

            int oppDegree = simBoard.getOpenDirections(oppR, oppC).size();
            double suffocation = (4 - oppDegree) * 45.0;
            int manhattan = Math.abs(myR - oppR) + Math.abs(myC - oppC);
            double proximityScore = -manhattan * 6.0;
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
                    if (nr == oppR && nc == oppC) continue;

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

    public static void main(String[] args) {
        int size = 6;
        String p1Type = "antigravity";
        String p2Type = "codex";
        int games = 1;
        boolean render = false;
        int delayMs = 0;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--size", "-s" -> { if (i + 1 < args.length) size = Integer.parseInt(args[++i]); }
                case "--p1" -> { if (i + 1 < args.length) p1Type = args[++i].toLowerCase(); }
                case "--p2" -> { if (i + 1 < args.length) p2Type = args[++i].toLowerCase(); }
                case "--games", "-g" -> { if (i + 1 < args.length) games = Integer.parseInt(args[++i]); }
                case "--render", "-r" -> render = true;
                case "--delay", "-d" -> { if (i + 1 < args.length) delayMs = Integer.parseInt(args[++i]); }
                case "--help", "-h" -> {
                    printHelp();
                    return;
                }
            }
        }

        System.out.println(ANSI_CYAN + ANSI_BOLD + "================================================================" + ANSI_RESET);
        System.out.println(ANSI_CYAN + ANSI_BOLD + "       COGame - 端脑 AI 终端竞技场 (AI Battle Arena CLI)        " + ANSI_RESET);
        System.out.println(ANSI_CYAN + ANSI_BOLD + "================================================================" + ANSI_RESET);
        System.out.printf("棋盘规格: %d × %d | 对决局数: %d | 渲染模式: %s\n", size, size, games, render ? "终端地图动画" : "精简快报");
        System.out.printf("P1 (先手·青): %s | P2 (后手·金): %s\n\n", p1Type.toUpperCase(), p2Type.toUpperCase());

        int p1Wins = 0;
        int p2Wins = 0;
        int draws = 0;

        for (int g = 1; g <= games; g++) {
            CombatantAi ai1 = "codex".equals(p1Type) ? new CodexAi() : new AntigravityAi();
            CombatantAi ai2 = "antigravity".equals(p2Type) ? new AntigravityAi() : new CodexAi();

            System.out.println(ANSI_BOLD + String.format(">>> 【第 %d / %d 局开始】", g, games) + ANSI_RESET);
            GameState state = new GameState(size);
            state.getP1().setName(ai1.getName());
            state.getP2().setName(ai2.getName());

            int turnCount = 0;
            while (!state.isOver() && turnCount < 200) {
                turnCount++;
                int currId = state.getCurrentTurn();
                CombatantAi currAi = (currId == 1) ? ai1 : ai2;
                PlayerState p = state.getCurrentPlayer();
                int energyBefore = p.getEnergy();

                AiDecision decision = currAi.planTurn(state, currId);

                int stepsMoved = 0;
                for (GameAction act : decision.getActions()) {
                    if (act.getType() == GameAction.Type.CHANGE_DIR_MOVE || act.getType() == GameAction.Type.MOVE) {
                        stepsMoved++;
                    }
                    GameEngine.executeAction(state, currId, act);
                }

                String pColor = (currId == 1) ? ANSI_CYAN : ANSI_YELLOW;
                System.out.printf("  %s[T%02d | %s(P%d)]%s 开局能量:%d/%d | 走%d步 | 决策: %s | 坐标:(%d,%d)\n",
                        pColor, turnCount, currAi.getName(), currId, ANSI_RESET,
                        energyBefore, state.getMaxEnergy(), stepsMoved, decision.getDescription(), p.getR(), p.getC());

                if (render) {
                    renderBoard(state);
                    if (delayMs > 0) {
                        try { Thread.sleep(delayMs); } catch (InterruptedException ignored) {}
                    }
                }
            }

            String winnerName = (state.getWinner() == 1) ? ai1.getName() : (state.getWinner() == 2 ? ai2.getName() : "平局");
            if (state.getWinner() == 1) p1Wins++;
            else if (state.getWinner() == 2) p2Wins++;
            else draws++;

            System.out.println(ANSI_GREEN + ANSI_BOLD + String.format("  ★ 第 %d 局结束: 胜者【%s】 | 领地: P1=%d格, P2=%d格 | 总回合: %d\n",
                    g, winnerName, state.getP1Territory(), state.getP2Territory(), turnCount) + ANSI_RESET);
        }

        System.out.println("================================================================");
        System.out.println(ANSI_BOLD + "★ 竞技场对决最终战报总结:" + ANSI_RESET);
        System.out.printf("P1 (%s) 胜场: %d  (胜率: %.1f%%)\n", p1Type.toUpperCase(), p1Wins, (p1Wins * 100.0 / games));
        System.out.printf("P2 (%s) 胜场: %d  (胜率: %.1f%%)\n", p2Type.toUpperCase(), p2Wins, (p2Wins * 100.0 / games));
        if (draws > 0) System.out.printf("平局场次: %d\n", draws);
        System.out.println("================================================================");
    }

    private static void renderBoard(GameState state) {
        Board board = state.getBoard();
        int rows = board.getRows();
        int cols = board.getCols();
        PlayerState p1 = state.getP1();
        PlayerState p2 = state.getP2();

        System.out.println("   " + ANSI_GRAY + "+---".repeat(cols) + "+" + ANSI_RESET);
        for (int r = 0; r < rows; r++) {
            StringBuilder line = new StringBuilder("   |");
            for (int c = 0; c < cols; c++) {
                String mark = "   ";
                if (p1.getR() == r && p1.getC() == c) mark = ANSI_CYAN + " 1 " + ANSI_RESET;
                else if (p2.getR() == r && p2.getC() == c) mark = ANSI_YELLOW + " 2 " + ANSI_RESET;
                line.append(mark);

                if (c < cols - 1) {
                    boolean open = board.isConnected(r, c, Direction.RIGHT);
                    int locker = board.getEdgeLocker(r, c, Direction.RIGHT);
                    if (open) line.append(ANSI_GRAY + " " + ANSI_RESET);
                    else if (locker == 1) line.append(ANSI_CYAN + "|" + ANSI_RESET);
                    else if (locker == 2) line.append(ANSI_YELLOW + "|" + ANSI_RESET);
                    else if (locker == 3) line.append(ANSI_GRAY + "#" + ANSI_RESET);
                    else line.append(ANSI_RED + "|" + ANSI_RESET);
                } else {
                    line.append(ANSI_GRAY + "|" + ANSI_RESET);
                }
            }
            System.out.println(line);

            if (r < rows - 1) {
                StringBuilder hLine = new StringBuilder("   +");
                for (int c = 0; c < cols; c++) {
                    boolean open = board.isConnected(r, c, Direction.DOWN);
                    int locker = board.getEdgeLocker(r, c, Direction.DOWN);
                    if (open) hLine.append(ANSI_GRAY + "   +" + ANSI_RESET);
                    else if (locker == 1) hLine.append(ANSI_CYAN + "---+" + ANSI_RESET);
                    else if (locker == 2) hLine.append(ANSI_YELLOW + "---+" + ANSI_RESET);
                    else if (locker == 3) hLine.append(ANSI_GRAY + "###+" + ANSI_RESET);
                    else hLine.append(ANSI_RED + "---+" + ANSI_RESET);
                }
                System.out.println(hLine);
            }
        }
        System.out.println("   " + ANSI_GRAY + "+---".repeat(cols) + "+" + ANSI_RESET);
    }

    private static void printHelp() {
        System.out.println("用法: battle-cli [选项]");
        System.out.println("选项:");
        System.out.println("  --size, -s <6|9|12>        棋盘规格 (默认: 6)");
        System.out.println("  --p1 <antigravity|codex>   先手 P1 策略 (默认: antigravity)");
        System.out.println("  --p2 <antigravity|codex>   后手 P2 策略 (默认: codex)");
        System.out.println("  --games, -g <数量>         连续对战局数 (默认: 1)");
        System.out.println("  --render, -r               在终端实时打印彩色棋盘地图");
        System.out.println("  --delay, -d <毫秒>         每回合动画渲染延迟毫秒数 (默认: 0)");
        System.out.println("  --help, -h                 显示本帮助信息");
    }
}

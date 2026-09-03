package person.kinman.cogame.ai;

import person.kinman.cogame.core.action.GameAction;
import person.kinman.cogame.core.model.Board;
import person.kinman.cogame.core.model.Direction;
import person.kinman.cogame.core.model.GameState;
import person.kinman.cogame.core.model.PlayerState;
import person.kinman.cogame.core.rule.GameEvaluator;

import java.util.*;

/**
 * Codex 极限压迫流 AI：
 * 核心特征：曼哈顿贴身突袭、死死封锁对手通道出口、高压窒息刺刀战
 */
public class CodexStrategy implements AiStrategy {

    @Override
    public AiDecision computeTurn(GameState state, int aiPlayerId) {
        PlayerState me = state.getPlayer(aiPlayerId);
        PlayerState opp = state.getPlayer(aiPlayerId == 1 ? 2 : 1);
        Board board = state.getBoard();

        int maxSteps = Math.max(1, me.getEnergy());
        Map<Long, List<Direction>> reachablePaths = getReachablePaths(board, me.getR(), me.getC(), opp.getR(), opp.getC(), maxSteps);

        double bestScore = -Double.MAX_VALUE;
        List<GameAction> bestActions = new ArrayList<>();
        String bestDescription = "原地突袭封锁";

        for (Map.Entry<Long, List<Direction>> entry : reachablePaths.entrySet()) {
            long key = entry.getKey();
            int r = GameEvaluator.decodeR(key);
            int c = GameEvaluator.decodeC(key);
            List<Direction> path = entry.getValue();

            for (Direction dir : Direction.values()) {
                if (!board.isConnected(r, c, dir)) continue;

                Board simBoard = board.copy();
                simBoard.lockEdge(r, c, dir, aiPlayerId);

                double score = evaluate(simBoard, r, c, opp.getR(), opp.getC());
                if (score > bestScore) {
                    bestScore = score;
                    bestDescription = String.format("冲刺至(%d,%d) 锁死[%s], 评估分: %.1f", r, c, dir.getName(), score);
                    bestActions = buildActions(me.getDirection(), path, dir);
                }
            }
        }

        if (bestActions.isEmpty()) {
            bestActions.add(GameAction.lock());
        }
        return new AiDecision(bestActions, bestScore, bestDescription);
    }

    private double evaluate(Board simBoard, int myR, int myC, int oppR, int oppC) {
        boolean connected = GameEvaluator.hasPath(simBoard, myR, myC, oppR, oppC);
        if (!connected) {
            Set<Long> myTerr = GameEvaluator.getConnectedComponent(simBoard, myR, myC);
            Set<Long> oppTerr = GameEvaluator.getConnectedComponent(simBoard, oppR, oppC);
            int diff = myTerr.size() - oppTerr.size();
            return diff > 0 ? (1000000.0 + diff * 1000.0) : (-1000000.0 + diff * 1000.0);
        }

        // 自身安全保障：避免落子后自身被困在单出口
        int myDegree = simBoard.getOpenDirections(myR, myC).size();
        if (myDegree <= 1) return -5000.0;

        // 核心亮点：压迫对手出口，让对手窒息
        int oppDegree = simBoard.getOpenDirections(oppR, oppC).size();
        double suffocation = (4 - oppDegree) * 45.0;

        // 核心亮点：曼哈顿贴身刺刀战（越近分越高）
        int manhattan = Math.abs(myR - oppR) + Math.abs(myC - oppC);
        double proximity = -manhattan * 6.0;

        int voronoi = calculateVoronoi(simBoard, myR, myC, oppR, oppC);

        return voronoi * 15.0 + suffocation + proximity + (myDegree * 4.0);
    }

    private Map<Long, List<Direction>> getReachablePaths(Board board, int startR, int startC, int oppR, int oppC, int maxSteps) {
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

    private int calculateVoronoi(Board board, int p1R, int p1C, int p2R, int p2C) {
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

    private int[][] bfsDist(Board board, int startR, int startC) {
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

    private List<GameAction> buildActions(Direction curDir, List<Direction> path, Direction lockDir) {
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
}

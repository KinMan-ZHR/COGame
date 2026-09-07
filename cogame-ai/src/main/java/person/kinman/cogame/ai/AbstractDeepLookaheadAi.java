package person.kinman.cogame.ai;

import person.kinman.cogame.core.action.GameAction;
import person.kinman.cogame.core.model.Board;
import person.kinman.cogame.core.model.Direction;
import person.kinman.cogame.core.model.GameState;
import person.kinman.cogame.core.model.PlayerState;
import person.kinman.cogame.core.rule.GameEngine;
import person.kinman.cogame.core.rule.GameEvaluator;

import java.util.*;

/**
 * 深度推演博弈 AI 基类：
 * 具备 3~10 档前瞻深搜能力、一击必杀斩杀探查、防对手单步偷袭剪枝 (Anti-Blunder) 与束搜索 (Beam Search)
 */
public abstract class AbstractDeepLookaheadAi implements AiStrategy {

    protected final int searchDepth;

    public AbstractDeepLookaheadAi(int searchDepth) {
        this.searchDepth = Math.max(3, Math.min(10, searchDepth));
    }

    public int getSearchDepth() {
        return searchDepth;
    }

    public abstract String getStrategistName();

    /**
     * 子类特定流派的叶子节点盘面评估启发函数
     */
    protected abstract double evaluateLeaf(GameState state, int aiPlayerId, int stepsUsed);

    public static class CandidateMove {
        public final int r;
        public final int c;
        public final List<Direction> path;
        public final Direction lockDir;
        public double score;

        public CandidateMove(int r, int c, List<Direction> path, Direction lockDir) {
            this.r = r;
            this.c = c;
            this.path = path;
            this.lockDir = lockDir;
            this.score = 0.0;
        }
    }

    @Override
    public AiDecision computeTurn(GameState state, int aiPlayerId) {
        PlayerState me = state.getPlayer(aiPlayerId);
        PlayerState opp = state.getPlayer(aiPlayerId == 1 ? 2 : 1);
        int oppId = opp.getId();

        List<CandidateMove> candidates = generateCandidateMoves(state, aiPlayerId);
        if (candidates.isEmpty()) {
            return new AiDecision(Collections.singletonList(GameAction.lock()), 0.0, "无可行动作，原地锁边");
        }

        CandidateMove bestMove = null;
        double bestScore = -Double.MAX_VALUE;

        // 1. 首层评估：一击必杀探查与防对手单步反绝杀检测
        List<CandidateMove> viableCandidates = new ArrayList<>();

        for (CandidateMove cand : candidates) {
            GameState sim = state.copy();
            for (Direction d : cand.path) {
                GameEngine.executeAction(sim, aiPlayerId, GameAction.changeDirMove(d));
            }
            GameEngine.executeAction(sim, aiPlayerId, GameAction.lock(cand.lockDir));

            // 终局检测
            if (sim.isOver()) {
                if (sim.getWinner() == aiPlayerId) {
                    // 一击必杀绝杀切断！立即采纳
                    String desc = String.format("【%s · 绝杀斩】(深度 %d) 移至(%d,%d) 封锁[%s], 终局绝杀胜！",
                            getStrategistName(), searchDepth, cand.r, cand.c, cand.lockDir.getName());
                    return new AiDecision(buildActions(me.getDirection(), cand.path, cand.lockDir), 10000000.0, desc);
                } else {
                    // 自杀切断，严格剔除
                    cand.score = -10000000.0;
                    continue;
                }
            }

            // 防对手下回合一击必杀反切 (Anti-Blunder 核心机制)
            if (checkOpponentImmediateWin(sim, oppId)) {
                cand.score = -5000000.0;
                continue;
            }

            // 基础启发分
            cand.score = evaluateLeaf(sim, aiPlayerId, cand.path.size());
            viableCandidates.add(cand);
        }

        if (viableCandidates.isEmpty()) {
            // 若全部被剪枝，退回原候选集中评分最高者保底
            viableCandidates = candidates;
        }

        // 2. 深度前瞻搜索 (Minimax with Beam Pruning)
        viableCandidates.sort((a, b) -> Double.compare(b.score, a.score));

        // 束搜索带宽：根据深度动态调节保证毫秒级响应
        int beamWidth = (searchDepth >= 8) ? 3 : (searchDepth >= 6 ? 4 : 5);
        List<CandidateMove> searchPool = viableCandidates.subList(0, Math.min(viableCandidates.size(), beamWidth));

        for (CandidateMove cand : searchPool) {
            GameState sim = state.copy();
            for (Direction d : cand.path) {
                GameEngine.executeAction(sim, aiPlayerId, GameAction.changeDirMove(d));
            }
            GameEngine.executeAction(sim, aiPlayerId, GameAction.lock(cand.lockDir));

            double score;
            if (searchDepth > 1) {
                score = minimax(sim, searchDepth - 1, false, aiPlayerId, -Double.MAX_VALUE, Double.MAX_VALUE);
            } else {
                score = cand.score;
            }

            if (score > bestScore) {
                bestScore = score;
                bestMove = cand;
            }
        }

        if (bestMove == null) {
            bestMove = viableCandidates.get(0);
            bestScore = bestMove.score;
        }

        String description = String.format("【%s · 深算 %d层】移至(%d,%d) 封锁[%s]边 (预判势能: %.1f)",
                getStrategistName(), searchDepth, bestMove.r, bestMove.c, bestMove.lockDir.getName(), bestScore);

        return new AiDecision(buildActions(me.getDirection(), bestMove.path, bestMove.lockDir), bestScore, description);
    }

    private double minimax(GameState state, int depth, boolean isAiTurn, int aiPlayerId, double alpha, double beta) {
        if (state.isOver()) {
            if (state.getWinner() == aiPlayerId) {
                return 10000000.0 + depth * 10000.0;
            } else if (state.getWinner() == 3) {
                return 0.0;
            } else {
                return -10000000.0 - depth * 10000.0;
            }
        }
        if (depth <= 0) {
            return evaluateLeaf(state, aiPlayerId, 0);
        }

        int currPlayerId = isAiTurn ? aiPlayerId : (aiPlayerId == 1 ? 2 : 1);
        List<CandidateMove> moves = generateCandidateMoves(state, currPlayerId);
        if (moves.isEmpty()) {
            return evaluateLeaf(state, aiPlayerId, 0);
        }

        // 轻量快评为束剪枝排序
        for (CandidateMove m : moves) {
            Board simB = state.getBoard().copy();
            simB.lockEdge(m.r, m.c, m.lockDir, currPlayerId);
            int oppId = (currPlayerId == 1 ? 2 : 1);
            PlayerState opp = state.getPlayer(oppId);
            boolean conn = GameEvaluator.hasPath(simB, m.r, m.c, opp.getR(), opp.getC());
            if (!conn) {
                Set<Long> myT = GameEvaluator.getConnectedComponent(simB, m.r, m.c);
                Set<Long> oppT = GameEvaluator.getConnectedComponent(simB, opp.getR(), opp.getC());
                m.score = (myT.size() - oppT.size()) > 0 ? 500000.0 : -500000.0;
            } else {
                m.score = calculateVoronoi(simB, m.r, m.c, opp.getR(), opp.getC());
            }
        }

        moves.sort((a, b) -> Double.compare(b.score, a.score));
        int limit = (depth >= 5) ? 2 : 3;
        List<CandidateMove> pruned = moves.subList(0, Math.min(moves.size(), limit));

        if (isAiTurn) {
            double maxEval = -Double.MAX_VALUE;
            for (CandidateMove cand : pruned) {
                GameState next = state.copy();
                for (Direction d : cand.path) GameEngine.executeAction(next, currPlayerId, GameAction.changeDirMove(d));
                GameEngine.executeAction(next, currPlayerId, GameAction.lock(cand.lockDir));
                double eval = minimax(next, depth - 1, false, aiPlayerId, alpha, beta);
                maxEval = Math.max(maxEval, eval);
                alpha = Math.max(alpha, eval);
                if (beta <= alpha) break;
            }
            return maxEval;
        } else {
            double minEval = Double.MAX_VALUE;
            for (CandidateMove cand : pruned) {
                GameState next = state.copy();
                for (Direction d : cand.path) GameEngine.executeAction(next, currPlayerId, GameAction.changeDirMove(d));
                GameEngine.executeAction(next, currPlayerId, GameAction.lock(cand.lockDir));
                double eval = minimax(next, depth - 1, true, aiPlayerId, alpha, beta);
                minEval = Math.min(minEval, eval);
                beta = Math.min(beta, eval);
                if (beta <= alpha) break;
            }
            return minEval;
        }
    }

    private boolean checkOpponentImmediateWin(GameState state, int oppId) {
        if (state.isOver()) return false;
        PlayerState opp = state.getPlayer(oppId);
        PlayerState me = state.getPlayer(oppId == 1 ? 2 : 1);
        Board board = state.getBoard();

        int maxSteps = Math.max(1, opp.getEnergy());
        Map<Long, List<Direction>> reachable = getReachablePaths(board, opp.getR(), opp.getC(), me.getR(), me.getC(), maxSteps);

        for (Map.Entry<Long, List<Direction>> entry : reachable.entrySet()) {
            long key = entry.getKey();
            int r = GameEvaluator.decodeR(key);
            int c = GameEvaluator.decodeC(key);

            for (Direction dir : Direction.values()) {
                if (!board.isConnected(r, c, dir)) continue;
                Board simBoard = board.copy();
                simBoard.lockEdge(r, c, dir, oppId);

                if (!GameEvaluator.hasPath(simBoard, r, c, me.getR(), me.getC())) {
                    Set<Long> oppTerr = GameEvaluator.getConnectedComponent(simBoard, r, c);
                    Set<Long> myTerr = GameEvaluator.getConnectedComponent(simBoard, me.getR(), me.getC());
                    if (oppTerr.size() > myTerr.size()) {
                        return true;
                    }
                    if (oppTerr.size() == myTerr.size()) {
                        int oppEdges = GameEvaluator.countConnectedEdges(simBoard, oppTerr);
                        int myEdges = GameEvaluator.countConnectedEdges(simBoard, myTerr);
                        if (oppEdges > myEdges) return true;
                    }
                }
            }
        }
        return false;
    }

    protected List<CandidateMove> generateCandidateMoves(GameState state, int playerId) {
        PlayerState player = state.getPlayer(playerId);
        PlayerState opponent = state.getPlayer(playerId == 1 ? 2 : 1);
        Board board = state.getBoard();

        int maxSteps = Math.max(1, player.getEnergy());
        Map<Long, List<Direction>> reachablePaths = getReachablePaths(
                board, player.getR(), player.getC(), opponent.getR(), opponent.getC(), maxSteps);

        List<CandidateMove> result = new ArrayList<>();
        for (Map.Entry<Long, List<Direction>> entry : reachablePaths.entrySet()) {
            long key = entry.getKey();
            int r = GameEvaluator.decodeR(key);
            int c = GameEvaluator.decodeC(key);
            List<Direction> path = entry.getValue();

            for (Direction dir : Direction.values()) {
                if (board.isConnected(r, c, dir)) {
                    result.add(new CandidateMove(r, c, path, dir));
                }
            }
        }
        return result;
    }

    protected Map<Long, List<Direction>> getReachablePaths(Board board, int startR, int startC, int oppR, int oppC, int maxSteps) {
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

    protected int calculateVoronoi(Board board, int p1R, int p1C, int p2R, int p2C) {
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

    protected int[][] bfsDist(Board board, int startR, int startC) {
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

    protected List<GameAction> buildActions(Direction curDir, List<Direction> path, Direction lockDir) {
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
        actions.add(GameAction.lock(lockDir));
        return actions;
    }
}

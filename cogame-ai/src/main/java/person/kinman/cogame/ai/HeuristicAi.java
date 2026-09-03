package person.kinman.cogame.ai;

import person.kinman.cogame.core.action.GameAction;
import person.kinman.cogame.core.model.Board;
import person.kinman.cogame.core.model.Direction;
import person.kinman.cogame.core.model.GameState;
import person.kinman.cogame.core.model.PlayerState;
import person.kinman.cogame.core.rule.GameEvaluator;

import java.util.*;

/**
 * 启发式评估 AI：基于《端脑》割裂连通、领地势能与最短路径阻断
 */
public class HeuristicAi implements AiStrategy {

    private final int maxSearchDistance;

    public HeuristicAi() {
        this(3); // 默认每回合最大移动步数探索范围为3步
    }

    public HeuristicAi(int maxSearchDistance) {
        this.maxSearchDistance = maxSearchDistance;
    }

    @Override
    public AiDecision computeTurn(GameState state, int aiPlayerId) {
        PlayerState me = state.getPlayer(aiPlayerId);
        PlayerState opp = state.getPlayer(aiPlayerId == 1 ? 2 : 1);
        Board board = state.getBoard();

        // 1. BFS 寻找从当前位置出发、在限制步数内能到达的所有格子及具体路径
        Map<Long, List<Direction>> reachablePaths = getReachablePaths(board, me.getR(), me.getC(), maxSearchDistance);

        double bestScore = -Double.MAX_VALUE;
        List<GameAction> bestActions = new ArrayList<>();
        String bestDescription = "原地尝试锁边";

        // 2. 遍历所有可到达格子及其可锁边的方向
        for (Map.Entry<Long, List<Direction>> entry : reachablePaths.entrySet()) {
            long key = entry.getKey();
            int r = GameEvaluator.decodeR(key);
            int c = GameEvaluator.decodeC(key);
            List<Direction> pathToCell = entry.getValue();

            for (Direction dir : Direction.values()) {
                if (!board.isConnected(r, c, dir)) continue;

                // 模拟锁边
                Board simBoard = board.copy();
                simBoard.lockEdge(r, c, dir);

                double score = evaluateMove(simBoard, r, c, opp.getR(), opp.getC(), pathToCell.size());

                if (score > bestScore) {
                    bestScore = score;
                    bestDescription = String.format("移动到(%d,%d) 并封锁[%s]方向边, 得分: %.1f", r, c, dir.getName(), score);

                    // 构造动作链
                    bestActions = buildActionSequence(me.getDirection(), pathToCell, dir);
                }
            }
        }

        // 保底：若无可行动作，则尝试原地锁边
        if (bestActions.isEmpty()) {
            bestActions.add(GameAction.lock());
        }

        return new AiDecision(bestActions, bestScore, bestDescription);
    }

    private double evaluateMove(Board simBoard, int myR, int myC, int oppR, int oppC, int moveSteps) {
        boolean stillConnected = GameEvaluator.hasPath(simBoard, myR, myC, oppR, oppC);

        if (!stillConnected) {
            // 绝杀时刻：双方已被隔断！
            Set<Long> myTerr = GameEvaluator.getConnectedComponent(simBoard, myR, myC);
            Set<Long> oppTerr = GameEvaluator.getConnectedComponent(simBoard, oppR, oppC);

            int diff = myTerr.size() - oppTerr.size();
            if (diff > 0) {
                // 必胜落子！
                return 1000000.0 + diff * 10000.0;
            } else if (diff < 0) {
                // 会导致自己输掉的阻断，极力避免
                return -1000000.0 + diff * 10000.0;
            } else {
                // 格子数相同，比较剩余边数
                int myEdges = GameEvaluator.countConnectedEdges(simBoard, myTerr);
                int oppEdges = GameEvaluator.countConnectedEdges(simBoard, oppTerr);
                return (myEdges - oppEdges) > 0 ? 500000.0 : -500000.0;
            }
        }

        // 仍连通：评估博弈势能
        // 1. 最短路径长度：距离对手越长，对手被压缩的概率越大
        List<int[]> path = GameEvaluator.findPath(simBoard, myR, myC, oppR, oppC);
        int pathLen = path.isEmpty() ? 0 : path.size();

        // 2. 对手行动自由度（周围未封锁的边数量）
        int oppDegree = simBoard.getOpenDirections(oppR, oppC).size();

        // 3. 自身领地可达格数量
        int myTerritorySize = GameEvaluator.getConnectedComponent(simBoard, myR, myC).size();

        // 4. 移动消耗
        int movePenalty = moveSteps * 5;

        return (pathLen * 25.0) - (oppDegree * 15.0) + (myTerritorySize * 8.0) - movePenalty;
    }

    private List<GameAction> buildActionSequence(Direction initialDir, List<Direction> pathDirs, Direction targetLockDir) {
        List<GameAction> actions = new ArrayList<>();
        Direction currentDir = initialDir;

        // 沿着路径移动
        for (Direction stepDir : pathDirs) {
            actions.add(GameAction.changeDirMove(stepDir));
            currentDir = stepDir;
        }

        // 调整朝向以面向目标锁边方向
        while (currentDir != targetLockDir) {
            actions.add(GameAction.rotate());
            currentDir = currentDir.clockwise();
        }

        // 执行锁边
        actions.add(GameAction.lock());
        return actions;
    }

    private Map<Long, List<Direction>> getReachablePaths(Board board, int startR, int startC, int maxDepth) {
        Map<Long, List<Direction>> result = new HashMap<>();
        long startKey = GameEvaluator.encode(startR, startC);
        result.put(startKey, new ArrayList<>());

        Queue<long[]> queue = new ArrayDeque<>();
        queue.add(new long[]{startKey, 0});

        while (!queue.isEmpty()) {
            long[] item = queue.poll();
            long key = item[0];
            int depth = (int) item[1];
            if (depth >= maxDepth) continue;

            int r = GameEvaluator.decodeR(key);
            int c = GameEvaluator.decodeC(key);
            List<Direction> currentPath = result.get(key);

            for (Direction dir : Direction.values()) {
                if (board.isConnected(r, c, dir)) {
                    int nr = r + dir.getDr();
                    int nc = c + dir.getDc();
                    long nextKey = GameEvaluator.encode(nr, nc);

                    if (!result.containsKey(nextKey)) {
                        List<Direction> nextPath = new ArrayList<>(currentPath);
                        nextPath.add(dir);
                        result.put(nextKey, nextPath);
                        queue.add(new long[]{nextKey, depth + 1});
                    }
                }
            }
        }
        return result;
    }
}

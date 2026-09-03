package person.kinman.cogame.ai;

import person.kinman.cogame.core.action.GameAction;
import person.kinman.cogame.core.model.Board;
import person.kinman.cogame.core.model.Direction;
import person.kinman.cogame.core.model.GameState;
import person.kinman.cogame.core.model.PlayerState;
import person.kinman.cogame.core.rule.GameEvaluator;

import java.util.*;

/**
 * 高级启发式博弈 AI：基于 Voronoi 领地控制图、最短路径切断阻断、对手出口压制与自保规避
 */
public class HeuristicAi implements AiStrategy {

    private final int searchDepth;

    public HeuristicAi() {
        this(3);
    }

    public HeuristicAi(int searchDepth) {
        this.searchDepth = searchDepth;
    }

    @Override
    public AiDecision computeTurn(GameState state, int aiPlayerId) {
        PlayerState me = state.getPlayer(aiPlayerId);
        PlayerState opp = state.getPlayer(aiPlayerId == 1 ? 2 : 1);
        Board board = state.getBoard();

        int maxSteps = Math.max(1, me.getEnergy());

        // 1. BFS 寻找从当前位置出发、在当前可用能量步数内能到达的所有格子及具体路径 (避开对手身位)
        Map<Long, List<Direction>> reachablePaths = getReachablePaths(board, me.getR(), me.getC(), opp.getR(), opp.getC(), maxSteps);

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

                // 模拟锁边 (记录为 AI 锁边)
                Board simBoard = board.copy();
                simBoard.lockEdge(r, c, dir, aiPlayerId);

                double score = evaluateMove(simBoard, r, c, opp.getR(), opp.getC(), pathToCell.size());

                if (score > bestScore) {
                    bestScore = score;
                    bestDescription = String.format("移至(%d,%d) 封锁[%s]边, 评估分: %.1f", r, c, dir.getName(), score);
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

    /**
     * 深度评估某一着法的战略势能
     */
    private double evaluateMove(Board simBoard, int myR, int myC, int oppR, int oppC, int moveSteps) {
        boolean stillConnected = GameEvaluator.hasPath(simBoard, myR, myC, oppR, oppC);

        // 1. 终局绝杀状态判断
        if (!stillConnected) {
            Set<Long> myTerr = GameEvaluator.getConnectedComponent(simBoard, myR, myC);
            Set<Long> oppTerr = GameEvaluator.getConnectedComponent(simBoard, oppR, oppC);

            int diff = myTerr.size() - oppTerr.size();
            if (diff > 0) {
                // 必胜绝杀！
                return 10000000.0 + diff * 10000.0;
            } else if (diff < 0) {
                // 致命自杀禁手，严厉禁止！
                return -10000000.0 + diff * 10000.0;
            } else {
                // 格子数相同，比较未封锁边数量
                int myEdges = GameEvaluator.countConnectedEdges(simBoard, myTerr);
                int oppEdges = GameEvaluator.countConnectedEdges(simBoard, oppTerr);
                return (myEdges - oppEdges) > 0 ? 5000000.0 : -5000000.0;
            }
        }

        // 2. Voronoi 领地势力分布 (核心：谁能先到达谁就控制该格子)
        int voronoiAdvantage = calculateVoronoiAdvantage(simBoard, myR, myC, oppR, oppC);

        // 3. 最短路径距离拉伸 (迫使对手走向死角或大迂回)
        List<int[]> path = GameEvaluator.findPath(simBoard, myR, myC, oppR, oppC);
        int pathLen = path.isEmpty() ? 0 : path.size();

        // 4. 对手出口压制 (迫使对手行动受限)
        int oppDegree = simBoard.getOpenDirections(oppR, oppC).size();
        double oppSuffocation = switch (oppDegree) {
            case 1 -> 1500.0; // 对手只剩1个出口，极易被困死！
            case 2 -> 500.0;  // 对手在窄道中
            case 3 -> 0.0;
            default -> -200.0; // 对手太自由
        };

        // 5. 自身安全防护 (绝不让自己陷入单一死角出口)
        int myDegree = simBoard.getOpenDirections(myR, myC).size();
        double selfSafety = (myDegree <= 1) ? -4000.0 : (myDegree * 120.0);

        // 6. 移动消耗惩罚
        double movePenalty = moveSteps * 8.0;

        return (voronoiAdvantage * 220.0) + (pathLen * 45.0) + oppSuffocation + selfSafety - movePenalty;
    }

    /**
     * 计算双源 Voronoi 领地势力范围差值
     */
    private int calculateVoronoiAdvantage(Board board, int myR, int myC, int oppR, int oppC) {
        int rows = board.getRows();
        int cols = board.getCols();
        int[][] myDist = getBfsDistances(board, myR, myC);
        int[][] oppDist = getBfsDistances(board, oppR, oppC);

        int aiTerritory = 0;
        int oppTerritory = 0;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int d1 = myDist[r][c];
                int d2 = oppDist[r][c];
                if (d1 < 0 && d2 < 0) continue;
                if (d1 >= 0 && (d2 < 0 || d1 < d2)) {
                    aiTerritory++;
                } else if (d2 >= 0 && (d1 < 0 || d2 < d1)) {
                    oppTerritory++;
                }
            }
        }
        return aiTerritory - oppTerritory;
    }

    /**
     * 计算从指定起点到全图所有格子的最短距离矩阵
     */
    private int[][] getBfsDistances(Board board, int startR, int startC) {
        int rows = board.getRows();
        int cols = board.getCols();
        int[][] dist = new int[rows][cols];
        for (int[] row : dist) {
            Arrays.fill(row, -1);
        }

        Queue<int[]> q = new ArrayDeque<>();
        q.add(new int[]{startR, startC});
        dist[startR][startC] = 0;

        while (!q.isEmpty()) {
            int[] curr = q.poll();
            int r = curr[0];
            int c = curr[1];
            int d = dist[r][c];

            for (Direction dir : Direction.values()) {
                if (board.isConnected(r, c, dir)) {
                    int nr = r + dir.getDr();
                    int nc = c + dir.getDc();
                    if (board.isValidCoord(nr, nc) && dist[nr][nc] == -1) {
                        dist[nr][nc] = d + 1;
                        q.add(new int[]{nr, nc});
                    }
                }
            }
        }
        return dist;
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

    private Map<Long, List<Direction>> getReachablePaths(Board board, int startR, int startC, int oppR, int oppC, int maxDepth) {
        Map<Long, List<Direction>> result = new HashMap<>();
        long startKey = GameEvaluator.encode(startR, startC);
        result.put(startKey, new ArrayList<>());

        Queue<long[]> queue = new ArrayDeque<>();
        queue.add(new long[]{startKey, 0});

        while (!queue.isEmpty()) {
            long[] curr = queue.poll();
            long currKey = curr[0];
            int depth = (int) curr[1];

            if (depth >= maxDepth) continue;

            int r = GameEvaluator.decodeR(currKey);
            int c = GameEvaluator.decodeC(currKey);
            List<Direction> currentPath = result.get(currKey);

            for (Direction dir : Direction.values()) {
                if (board.isConnected(r, c, dir)) {
                    int nr = r + dir.getDr();
                    int nc = c + dir.getDc();
                    if (nr == oppR && nc == oppC) continue; // 对手身位阻挡

                    long nextKey = GameEvaluator.encode(nr, nc);

                    if (!result.containsKey(nextKey)) {
                        List<Direction> newPath = new ArrayList<>(currentPath);
                        newPath.add(dir);
                        result.put(nextKey, newPath);
                        queue.add(new long[]{nextKey, depth + 1});
                    }
                }
            }
        }
        return result;
    }
}

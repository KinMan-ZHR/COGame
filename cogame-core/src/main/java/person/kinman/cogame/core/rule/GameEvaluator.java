package person.kinman.cogame.core.rule;

import person.kinman.cogame.core.model.Board;
import person.kinman.cogame.core.model.Direction;
import person.kinman.cogame.core.model.GameState;

import java.util.*;

/**
 * 拓扑图论算法与胜负判定（连通性检测、最短路径、领地与边数计算）
 */
public class GameEvaluator {

    /**
     * 检查两个坐标之间是否存在连通路径（BFS）
     */
    public static boolean hasPath(Board board, int r1, int c1, int r2, int c2) {
        if (r1 == r2 && c1 == c2) return true;
        boolean[][] visited = new boolean[board.getRows()][board.getCols()];
        Queue<int[]> queue = new ArrayDeque<>();
        queue.add(new int[]{r1, c1});
        visited[r1][c1] = true;

        while (!queue.isEmpty()) {
            int[] curr = queue.poll();
            int r = curr[0];
            int c = curr[1];

            if (r == r2 && c == c2) return true;

            for (Direction dir : Direction.values()) {
                if (board.isConnected(r, c, dir)) {
                    int nr = r + dir.getDr();
                    int nc = c + dir.getDc();
                    if (!visited[nr][nc]) {
                        visited[nr][nc] = true;
                        queue.add(new int[]{nr, nc});
                    }
                }
            }
        }
        return false;
    }

    /**
     * 获取从 (r1, c1) 到 (r2, c2) 的最短路径坐标序列（若无路径返回空列表）
     */
    public static List<int[]> findPath(Board board, int r1, int c1, int r2, int c2) {
        List<int[]> path = new ArrayList<>();
        if (!board.isValidCoord(r1, c1) || !board.isValidCoord(r2, c2)) return path;

        int rows = board.getRows();
        int cols = board.getCols();
        int[][] prevR = new int[rows][cols];
        int[][] prevC = new int[rows][cols];
        boolean[][] visited = new boolean[rows][cols];

        for (int[] row : prevR) Arrays.fill(row, -1);
        for (int[] row : prevC) Arrays.fill(row, -1);

        Queue<int[]> queue = new ArrayDeque<>();
        queue.add(new int[]{r1, c1});
        visited[r1][c1] = true;

        boolean found = false;
        while (!queue.isEmpty()) {
            int[] curr = queue.poll();
            int r = curr[0];
            int c = curr[1];
            if (r == r2 && c == c2) {
                found = true;
                break;
            }

            for (Direction dir : Direction.values()) {
                if (board.isConnected(r, c, dir)) {
                    int nr = r + dir.getDr();
                    int nc = c + dir.getDc();
                    if (!visited[nr][nc]) {
                        visited[nr][nc] = true;
                        prevR[nr][nc] = r;
                        prevC[nr][nc] = c;
                        queue.add(new int[]{nr, nc});
                    }
                }
            }
        }

        if (!found) return path;

        int currR = r2;
        int currC = c2;
        while (currR != -1 && currC != -1) {
            path.add(0, new int[]{currR, currC});
            if (currR == r1 && currC == c1) break;
            int pr = prevR[currR][currC];
            int pc = prevC[currR][currC];
            currR = pr;
            currC = pc;
        }
        return path;
    }

    /**
     * 计算指定起点所在连通分量内的所有格子坐标集合
     */
    public static Set<Long> getConnectedComponent(Board board, int startR, int startC) {
        Set<Long> visited = new HashSet<>();
        if (!board.isValidCoord(startR, startC)) return visited;

        Queue<int[]> queue = new ArrayDeque<>();
        queue.add(new int[]{startR, startC});
        visited.add(encode(startR, startC));

        while (!queue.isEmpty()) {
            int[] curr = queue.poll();
            int r = curr[0];
            int c = curr[1];

            for (Direction dir : Direction.values()) {
                if (board.isConnected(r, c, dir)) {
                    int nr = r + dir.getDr();
                    int nc = c + dir.getDc();
                    long key = encode(nr, nc);
                    if (!visited.contains(key)) {
                        visited.add(key);
                        queue.add(new int[]{nr, nc});
                    }
                }
            }
        }
        return visited;
    }

    /**
     * 计算指定连通分量内的未封锁边数量（即未隔断的通路数）
     */
    public static int countConnectedEdges(Board board, Set<Long> component) {
        int edgeCount = 0;
        for (long key : component) {
            int r = decodeR(key);
            int c = decodeC(key);
            // 只向右、向下统计，避免无向边重复计数
            if (board.isConnected(r, c, Direction.RIGHT)) {
                long neighborKey = encode(r, c + 1);
                if (component.contains(neighborKey)) {
                    edgeCount++;
                }
            }
            if (board.isConnected(r, c, Direction.DOWN)) {
                long neighborKey = encode(r + 1, c);
                if (component.contains(neighborKey)) {
                    edgeCount++;
                }
            }
        }
        return edgeCount;
    }

    /**
     * 判定终局并计算结算数据
     */
    public static void evaluateGameOver(GameState state) {
        Board board = state.getBoard();
        int r1 = state.getP1().getR();
        int c1 = state.getP1().getC();
        int r2 = state.getP2().getR();
        int c2 = state.getP2().getC();

        boolean pathExists = hasPath(board, r1, c1, r2, c2);
        if (pathExists) {
            state.setOver(false);
            return;
        }

        // 双方不连通，游戏结束！
        state.setOver(true);

        Set<Long> comp1 = getConnectedComponent(board, r1, c1);
        Set<Long> comp2 = getConnectedComponent(board, r2, c2);

        int t1 = comp1.size();
        int t2 = comp2.size();
        int e1 = countConnectedEdges(board, comp1);
        int e2 = countConnectedEdges(board, comp2);

        state.setP1Territory(t1);
        state.setP2Territory(t2);
        state.setP1UnblockedEdges(e1);
        state.setP2UnblockedEdges(e2);

        // 胜负判定（优先看格子数；格子数相同看剩余边数）
        String winnerText;
        if (t1 > t2) {
            state.setWinner(1);
            winnerText = state.getP1().getName() + " 胜利！";
        } else if (t2 > t1) {
            state.setWinner(2);
            winnerText = state.getP2().getName() + " 胜利！";
        } else {
            if (e1 > e2) {
                state.setWinner(1);
                winnerText = state.getP1().getName() + " 胜利(边数领先)！";
            } else if (e2 > e1) {
                state.setWinner(2);
                winnerText = state.getP2().getName() + " 胜利(边数领先)！";
            } else {
                state.setWinner(3);
                winnerText = "平局！";
            }
        }

        state.setWinReason(String.format("%s [格子: %d vs %d | 边数: %d vs %d]",
                winnerText, t1, t2, e1, e2));
    }

    public static long encode(int r, int c) {
        return (((long) r) << 32) | (c & 0xFFFFFFFFL);
    }

    public static int decodeR(long key) {
        return (int) (key >> 32);
    }

    public static int decodeC(long key) {
        return (int) key;
    }
}

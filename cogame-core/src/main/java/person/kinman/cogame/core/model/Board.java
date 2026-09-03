package person.kinman.cogame.core.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 矩阵棋盘与边连通/封锁状态管理 (支持记录锁边归属玩家)
 */
public class Board {
    public static final int DEFAULT_ROWS = 6;
    public static final int DEFAULT_COLS = 6;

    private final int rows;
    private final int cols;

    // 水平边: hEdge[r][c] 表示 (r, c) 与 (r, c+1) 之间的边 (size: rows x (cols-1))
    // true 表示通路未封锁，false 表示已封锁
    private boolean[][] hEdge;
    // 记录水平边锁边者: 0=未锁, 1=P1, 2=P2
    private int[][] hEdgeOwner;

    // 垂直边: vEdge[r][c] 表示 (r, c) 与 (r+1, c) 之间的边 (size: (rows-1) x cols)
    // true 表示通路未封锁，false 表示已封锁
    private boolean[][] vEdge;
    // 记录垂直边锁边者: 0=未锁, 1=P1, 2=P2
    private int[][] vEdgeOwner;

    public Board() {
        this(DEFAULT_ROWS, DEFAULT_COLS);
    }

    public Board(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        reset();
    }

    /**
     * 重置棋盘所有边为畅通状态
     */
    public void reset() {
        this.hEdge = new boolean[rows][cols - 1];
        this.hEdgeOwner = new int[rows][cols - 1];
        for (int r = 0; r < rows; r++) {
            Arrays.fill(hEdge[r], true);
        }

        this.vEdge = new boolean[rows - 1][cols];
        this.vEdgeOwner = new int[rows - 1][cols];
        for (int r = 0; r < rows - 1; r++) {
            Arrays.fill(vEdge[r], true);
        }
    }

    public int getRows() {
        return rows;
    }

    public int getCols() {
        return cols;
    }

    public boolean isValidCoord(int r, int c) {
        return r >= 0 && r < rows && c >= 0 && c < cols;
    }

    /**
     * 检查 (r, c) 朝指定方向的边是否连通
     */
    public boolean isConnected(int r, int c, Direction dir) {
        if (!isValidCoord(r, c)) return false;
        int nr = r + dir.getDr();
        int nc = c + dir.getDc();
        if (!isValidCoord(nr, nc)) return false;

        return switch (dir) {
            case UP -> vEdge[r - 1][c];
            case DOWN -> vEdge[r][c];
            case LEFT -> hEdge[r][c - 1];
            case RIGHT -> hEdge[r][c];
        };
    }

    /**
     * 获取指定边是由哪位玩家封锁的 (0=未封锁或外边界, 1=P1, 2=P2)
     */
    public int getEdgeLocker(int r, int c, Direction dir) {
        if (!isValidCoord(r, c)) return 0;
        int nr = r + dir.getDr();
        int nc = c + dir.getDc();
        if (!isValidCoord(nr, nc)) return 0;

        return switch (dir) {
            case UP -> vEdgeOwner[r - 1][c];
            case DOWN -> vEdgeOwner[r][c];
            case LEFT -> hEdgeOwner[r][c - 1];
            case RIGHT -> hEdgeOwner[r][c];
        };
    }

    /**
     * 封锁 (r, c) 朝指定方向的边 (默认记录为玩家1)
     */
    public boolean lockEdge(int r, int c, Direction dir) {
        return lockEdge(r, c, dir, 1);
    }

    /**
     * 封锁 (r, c) 朝指定方向的边，并记录归属玩家
     * @param playerId 锁边的玩家ID (1 or 2)
     * @return 若成功封锁返回 true；若已封锁或属于外边界返回 false
     */
    public boolean lockEdge(int r, int c, Direction dir, int playerId) {
        if (!isValidCoord(r, c)) return false;
        int nr = r + dir.getDr();
        int nc = c + dir.getDc();
        if (!isValidCoord(nr, nc)) return false;

        switch (dir) {
            case UP -> {
                if (vEdge[r - 1][c]) {
                    vEdge[r - 1][c] = false;
                    vEdgeOwner[r - 1][c] = playerId;
                    return true;
                }
            }
            case DOWN -> {
                if (vEdge[r][c]) {
                    vEdge[r][c] = false;
                    vEdgeOwner[r][c] = playerId;
                    return true;
                }
            }
            case LEFT -> {
                if (hEdge[r][c - 1]) {
                    hEdge[r][c - 1] = false;
                    hEdgeOwner[r][c - 1] = playerId;
                    return true;
                }
            }
            case RIGHT -> {
                if (hEdge[r][c]) {
                    hEdge[r][c] = false;
                    hEdgeOwner[r][c] = playerId;
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 获取 (r, c) 当前所有畅通的方向
     */
    public List<Direction> getOpenDirections(int r, int c) {
        List<Direction> list = new ArrayList<>();
        for (Direction d : Direction.values()) {
            if (isConnected(r, c, d)) {
                list.add(d);
            }
        }
        return list;
    }

    /**
     * 深拷贝棋盘副本
     */
    public Board copy() {
        Board copy = new Board(this.rows, this.cols);
        for (int r = 0; r < rows; r++) {
            System.arraycopy(this.hEdge[r], 0, copy.hEdge[r], 0, cols - 1);
            System.arraycopy(this.hEdgeOwner[r], 0, copy.hEdgeOwner[r], 0, cols - 1);
        }
        for (int r = 0; r < rows - 1; r++) {
            System.arraycopy(this.vEdge[r], 0, copy.vEdge[r], 0, cols);
            System.arraycopy(this.vEdgeOwner[r], 0, copy.vEdgeOwner[r], 0, cols);
        }
        return copy;
    }

    public boolean[][] gethEdge() {
        return hEdge;
    }

    public boolean[][] getvEdge() {
        return vEdge;
    }

    public int[][] gethEdgeOwner() {
        return hEdgeOwner;
    }

    public int[][] getvEdgeOwner() {
        return vEdgeOwner;
    }
}

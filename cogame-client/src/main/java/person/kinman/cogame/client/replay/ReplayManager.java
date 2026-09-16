package person.kinman.cogame.client.replay;

import person.kinman.cogame.core.model.Board;
import person.kinman.cogame.core.model.Direction;
import person.kinman.cogame.core.model.GameState;
import person.kinman.cogame.core.model.PlayerState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * 战局复盘管理器：
 * 1. 自动高保真捕获对局每一步的移动轨迹、封锁边、能量消耗与盘面深拷贝快照；
 * 2. 终局后提供完整的逐手复盘推演、步骤跳转与快捷键导航系统。
 */
public class ReplayManager {
    private final List<TurnSnapshot> snapshots = new ArrayList<>();
    private final List<Consumer<ReplayManager>> listeners = new ArrayList<>();

    private GameState lastCommittedState = null;
    private boolean replayMode = false;
    private int currentStep = 0;

    public ReplayManager() {}

    public synchronized void reset(GameState initialState) {
        snapshots.clear();
        replayMode = false;
        currentStep = 0;

        if (initialState != null) {
            GameState copy = initialState.copy();
            TurnSnapshot initSnap = new TurnSnapshot(
                    0, 0, "双方起手",
                    -1, -1, -1, -1, 0,
                    -1, -1, null,
                    "开局初始盘面（双方就位，等待先手出招）",
                    copy,
                    copy.getP1Territory(), copy.getP2Territory(),
                    false, ""
            );
            snapshots.add(initSnap);
            this.lastCommittedState = copy;
        } else {
            this.lastCommittedState = null;
        }
        notifyListeners();
    }

    /**
     * 响应控制器状态更新：自动识别新落子锁边、回合切换与终局结算
     */
    public synchronized void onStateChanged(GameState newState) {
        if (newState == null) return;

        // 如果从未初始化，或检测到全新开局重置
        if (lastCommittedState == null) {
            reset(newState);
            return;
        }

        int lastWalls = countPlayerLockedEdges(lastCommittedState.getBoard());
        int currWalls = countPlayerLockedEdges(newState.getBoard());

        if (currWalls < lastWalls) {
            // 玩家主动重置开新局
            reset(newState);
            return;
        }

        // 寻找相较于上一次提交状态，新封锁的边
        List<LockedEdgeDiff> diffs = findLockedEdgeDiffs(lastCommittedState.getBoard(), newState.getBoard());

        if (!diffs.isEmpty()) {
            for (LockedEdgeDiff diff : diffs) {
                int moveIdx = snapshots.size();
                int moverId = diff.owner > 0 ? diff.owner : lastCommittedState.getCurrentTurn();
                PlayerState moverOld = lastCommittedState.getPlayer(moverId);
                PlayerState moverNew = newState.getPlayer(moverId);

                int fromR = moverOld.getR();
                int fromC = moverOld.getC();
                int toR = moverNew.getR();
                int toC = moverNew.getC();
                int steps = lastCommittedState.getCurrentTurnSteps();
                if (steps == 0 && (fromR != toR || fromC != toC)) {
                    steps = Math.abs(fromR - toR) + Math.abs(fromC - toC);
                }

                int lockedR = diff.r;
                int lockedC = diff.c;
                Direction lockedDir = diff.direction;

                // 若该边与玩家到达的格子 (toR, toC) 相邻，优先以玩家站位视角表达（如位于 (r, c+1) 格的 LEFT 边）
                if (diff.direction == Direction.RIGHT) {
                    if (toR == diff.r && toC == diff.c + 1) {
                        lockedR = toR;
                        lockedC = toC;
                        lockedDir = Direction.LEFT;
                    }
                } else if (diff.direction == Direction.DOWN) {
                    if (toR == diff.r + 1 && toC == diff.c) {
                        lockedR = toR;
                        lockedC = toC;
                        lockedDir = Direction.UP;
                    }
                }

                String roleName = (moverId == 1) ? "P1 先手" : "P2 后手";
                String playerName = moverOld.getName();
                String desc = String.format("第 %d 手: [%s] %s 从 (%d,%d) 移至 (%d,%d)，耗能 %d 步 · 封锁 [%s %s] 边",
                        moveIdx, roleName, playerName, fromR, fromC, toR, toC, steps, lockedDir.getSymbol(), lockedDir.getName());

                boolean isOver = newState.isOver();
                String winReason = isOver ? newState.getWinReason() : "";

                TurnSnapshot snap = new TurnSnapshot(
                        moveIdx, moverId, playerName,
                        fromR, fromC, toR, toC, steps,
                        lockedR, lockedC, lockedDir,
                        desc, newState.copy(),
                        newState.getP1Territory(), newState.getP2Territory(),
                        isOver, winReason
                );
                snapshots.add(snap);
            }
            this.lastCommittedState = newState.copy();
            if (newState.isOver()) {
                this.currentStep = Math.max(0, snapshots.size() - 1);
            }
            notifyListeners();
        } else if (newState.isOver() && !lastCommittedState.isOver()) {
            // 没有新增边但游戏非正常结束（如认输、超时或直接判定终局）
            int moveIdx = snapshots.size();
            TurnSnapshot snap = new TurnSnapshot(
                    moveIdx, 0, "终局判定",
                    -1, -1, -1, -1, 0,
                    -1, -1, null,
                    "终局结算: " + newState.getWinReason(),
                    newState.copy(),
                    newState.getP1Territory(), newState.getP2Territory(),
                    true, newState.getWinReason()
            );
            snapshots.add(snap);
            this.lastCommittedState = newState.copy();
            this.currentStep = Math.max(0, snapshots.size() - 1);
            notifyListeners();
        }
    }

    public synchronized boolean isReplayMode() {
        return replayMode;
    }

    public synchronized void setReplayMode(boolean replayMode) {
        this.replayMode = replayMode;
        if (replayMode && !snapshots.isEmpty()) {
            this.currentStep = snapshots.size() - 1; // 默认跳到最后一手
        }
        notifyListeners();
    }

    public synchronized int getCurrentStep() {
        return currentStep;
    }

    public synchronized int getTotalSteps() {
        return Math.max(0, snapshots.size() - 1);
    }

    public synchronized int getSnapshotCount() {
        return snapshots.size();
    }

    public synchronized TurnSnapshot getCurrentSnapshot() {
        if (snapshots.isEmpty()) return null;
        int idx = Math.max(0, Math.min(snapshots.size() - 1, currentStep));
        return snapshots.get(idx);
    }

    public synchronized List<TurnSnapshot> getAllSnapshots() {
        return Collections.unmodifiableList(snapshots);
    }

    public synchronized void jumpTo(int step) {
        if (snapshots.isEmpty()) return;
        int clamped = Math.max(0, Math.min(snapshots.size() - 1, step));
        if (this.currentStep != clamped) {
            this.currentStep = clamped;
            notifyListeners();
        }
    }

    public synchronized void next() {
        jumpTo(currentStep + 1);
    }

    public synchronized void prev() {
        jumpTo(currentStep - 1);
    }

    public synchronized void first() {
        jumpTo(0);
    }

    public synchronized void last() {
        jumpTo(snapshots.size() - 1);
    }

    public synchronized void addListener(Consumer<ReplayManager> listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    private void notifyListeners() {
        for (Consumer<ReplayManager> l : listeners) {
            try {
                l.accept(this);
            } catch (Exception ignored) {}
        }
    }

    // ==========================================
    // 内部边状态差分计算
    // ==========================================

    private static class LockedEdgeDiff {
        final int r;
        final int c;
        final Direction direction;
        final int owner;

        LockedEdgeDiff(int r, int c, Direction direction, int owner) {
            this.r = r;
            this.c = c;
            this.direction = direction;
            this.owner = owner;
        }
    }

    private static List<LockedEdgeDiff> findLockedEdgeDiffs(Board oldBoard, Board newBoard) {
        List<LockedEdgeDiff> diffs = new ArrayList<>();
        if (oldBoard == null || newBoard == null) return diffs;

        int rows = newBoard.getRows();
        int cols = newBoard.getCols();

        // 1. 检查水平边
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols - 1; c++) {
                boolean wasOpen = oldBoard.isConnected(r, c, Direction.RIGHT);
                boolean isOpen = newBoard.isConnected(r, c, Direction.RIGHT);
                if (wasOpen && !isOpen) {
                    int owner = newBoard.getEdgeLocker(r, c, Direction.RIGHT);
                    diffs.add(new LockedEdgeDiff(r, c, Direction.RIGHT, owner));
                }
            }
        }

        // 2. 检查垂直边
        for (int r = 0; r < rows - 1; r++) {
            for (int c = 0; c < cols; c++) {
                boolean wasOpen = oldBoard.isConnected(r, c, Direction.DOWN);
                boolean isOpen = newBoard.isConnected(r, c, Direction.DOWN);
                if (wasOpen && !isOpen) {
                    int owner = newBoard.getEdgeLocker(r, c, Direction.DOWN);
                    diffs.add(new LockedEdgeDiff(r, c, Direction.DOWN, owner));
                }
            }
        }

        return diffs;
    }

    public static int countPlayerLockedEdges(Board board) {
        if (board == null) return 0;
        int count = 0;
        int[][] hOwner = board.gethEdgeOwner();
        int[][] vOwner = board.getvEdgeOwner();

        for (int r = 0; r < board.getRows(); r++) {
            for (int c = 0; c < board.getCols() - 1; c++) {
                int owner = hOwner[r][c];
                if (owner == 1 || owner == 2) {
                    count++;
                }
            }
        }

        for (int r = 0; r < board.getRows() - 1; r++) {
            for (int c = 0; c < board.getCols(); c++) {
                int owner = vOwner[r][c];
                if (owner == 1 || owner == 2) {
                    count++;
                }
            }
        }

        return count;
    }
}

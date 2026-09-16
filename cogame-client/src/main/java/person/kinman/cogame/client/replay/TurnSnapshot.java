package person.kinman.cogame.client.replay;

import person.kinman.cogame.core.model.Direction;
import person.kinman.cogame.core.model.GameState;

/**
 * 单手对局历史快照：忠实记录对局中具体某一手的完整数据（行动方、起止轨迹、封锁边、盘面深拷贝与局势指标）
 */
public class TurnSnapshot {
    private final int moveIndex; // 0 = 开局初始, 1, 2, ...
    private final int playerId;  // 0 = 开局, 1 = P1, 2 = P2
    private final String playerName;
    private final int fromR;
    private final int fromC;
    private final int toR;
    private final int toC;
    private final int stepsUsed;
    private final int lockedEdgeR;
    private final int lockedEdgeC;
    private final Direction lockedDirection;
    private final String description;
    private final GameState stateSnapshot;
    private final int p1Territory;
    private final int p2Territory;
    private final boolean gameOverMove;
    private final String winReason;

    public TurnSnapshot(int moveIndex, int playerId, String playerName,
                        int fromR, int fromC, int toR, int toC, int stepsUsed,
                        int lockedEdgeR, int lockedEdgeC, Direction lockedDirection,
                        String description, GameState stateSnapshot,
                        int p1Territory, int p2Territory,
                        boolean gameOverMove, String winReason) {
        this.moveIndex = moveIndex;
        this.playerId = playerId;
        this.playerName = (playerName != null) ? playerName : ("P" + playerId);
        this.fromR = fromR;
        this.fromC = fromC;
        this.toR = toR;
        this.toC = toC;
        this.stepsUsed = stepsUsed;
        this.lockedEdgeR = lockedEdgeR;
        this.lockedEdgeC = lockedEdgeC;
        this.lockedDirection = lockedDirection;
        this.description = description;
        this.stateSnapshot = (stateSnapshot != null) ? stateSnapshot.copy() : null;
        this.p1Territory = p1Territory;
        this.p2Territory = p2Territory;
        this.gameOverMove = gameOverMove;
        this.winReason = (winReason != null) ? winReason : "";
    }

    public int getMoveIndex() {
        return moveIndex;
    }

    public int getPlayerId() {
        return playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public int getFromR() {
        return fromR;
    }

    public int getFromC() {
        return fromC;
    }

    public int getToR() {
        return toR;
    }

    public int getToC() {
        return toC;
    }

    public boolean hasMoved() {
        return fromR >= 0 && fromC >= 0 && toR >= 0 && toC >= 0 && (fromR != toR || fromC != toC);
    }

    public int getStepsUsed() {
        return stepsUsed;
    }

    public int getLockedEdgeR() {
        return lockedEdgeR;
    }

    public int getLockedEdgeC() {
        return lockedEdgeC;
    }

    public Direction getLockedDirection() {
        return lockedDirection;
    }

    public boolean hasLockedEdge() {
        return lockedEdgeR >= 0 && lockedEdgeC >= 0 && lockedDirection != null;
    }

    public String getDescription() {
        return description;
    }

    public GameState getStateSnapshot() {
        return stateSnapshot;
    }

    public int getP1Territory() {
        return p1Territory;
    }

    public int getP2Territory() {
        return p2Territory;
    }

    public boolean isGameOverMove() {
        return gameOverMove;
    }

    public String getWinReason() {
        return winReason;
    }

    /**
     * 获取紧凑型步骤说明，供 HUD 面板展示
     */
    public String getActionSummary() {
        if (moveIndex == 0) {
            return "开局双方起手就位";
        }
        StringBuilder sb = new StringBuilder();
        if (hasMoved()) {
            sb.append(String.format("位移: (%d,%d)➔(%d,%d)[%d步]", fromR, fromC, toR, toC, stepsUsed));
        } else {
            sb.append("原地未移动");
        }
        if (hasLockedEdge()) {
            sb.append(String.format(" · 封锁 [%s] 边", lockedDirection.getSymbol()));
        }
        return sb.toString();
    }
}

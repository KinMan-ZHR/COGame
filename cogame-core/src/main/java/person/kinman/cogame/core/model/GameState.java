package person.kinman.cogame.core.model;

import person.kinman.cogame.core.rule.GameEvaluator;

/**
 * 完整对战游戏状态（可序列化为网络 JSON 快照）
 */
public class GameState {
    private int rows = 6;
    private int cols = 6;
    private Board board;
    private PlayerState p1;
    private PlayerState p2;
    private int currentTurn; // 1 or 2
    private boolean over;
    private int winner; // 0=未结束, 1=P1胜, 2=P2胜, 3=平局
    private String winReason;

    private int p1Territory;
    private int p2Territory;
    private int p1UnblockedEdges;
    private int p2UnblockedEdges;

    public GameState() {
        this(6, 6);
    }

    private int turnStartR = 0;
    private int turnStartC = 0;
    private int currentTurnSteps = 0;

    public GameState(int size) {
        this(size, size);
    }

    public GameState(int rows, int cols) {
        this.rows = Math.max(6, Math.min(13, rows));
        this.cols = Math.max(6, Math.min(13, cols));
        reset();
    }

    /**
     * 能量上限：棋盘边长 - 1
     */
    public int getMaxEnergy() {
        return cols - 1;
    }

    /**
     * 每回合恢复能量：边长一半向下取整
     */
    public int getEnergyRegen() {
        return cols / 2;
    }

    public void reset() {
        this.board = new Board(rows, cols);
        this.p1 = new PlayerState(1, "我", 0, 0, Direction.DOWN);
        this.p2 = new PlayerState(2, "对手", rows - 1, cols - 1, Direction.UP);

        // 初始能量等于单回合恢复量（保证首回合攻守对称）
        int initEnergy = getEnergyRegen();
        this.p1.setEnergy(initEnergy);
        this.p2.setEnergy(initEnergy);

        this.currentTurn = 1;
        this.turnStartR = 0;
        this.turnStartC = 0;
        this.currentTurnSteps = 0;
        this.over = false;
        this.winner = 0;
        this.winReason = "";
        this.p1Territory = 0;
        this.p2Territory = 0;
        this.p1UnblockedEdges = 0;
        this.p2UnblockedEdges = 0;

        // 中大盘进阶模式：初始化预置中立隔断墙 (6x6为0; 9x9为0.12; 12x12为0.16)
        double ratio = (rows >= 12) ? 0.16 : (rows >= 9 ? 0.12 : 0.0);
        if (ratio > 0.0) {
            GameEvaluator.setupNeutralBarriers(this.board, ratio, System.currentTimeMillis());
        }
    }

    public PlayerState getPlayer(int id) {
        return (id == 1) ? p1 : p2;
    }

    public PlayerState getCurrentPlayer() {
        return getPlayer(currentTurn);
    }

    public PlayerState getOpponentPlayer() {
        return getPlayer(currentTurn == 1 ? 2 : 1);
    }

    public void switchTurn() {
        this.currentTurn = (this.currentTurn == 1) ? 2 : 1;
        PlayerState curr = getCurrentPlayer();
        // 获得恢复能量，但不超过上限
        curr.setEnergy(Math.min(getMaxEnergy(), curr.getEnergy() + getEnergyRegen()));
        this.turnStartR = curr.getR();
        this.turnStartC = curr.getC();
        this.currentTurnSteps = 0;
    }

    public GameState copy() {
        GameState copy = new GameState(this.rows, this.cols);
        copy.board = this.board.copy();
        copy.p1 = this.p1.copy();
        copy.p2 = this.p2.copy();
        copy.currentTurn = this.currentTurn;
        copy.turnStartR = this.turnStartR;
        copy.turnStartC = this.turnStartC;
        copy.currentTurnSteps = this.currentTurnSteps;
        copy.over = this.over;
        copy.winner = this.winner;
        copy.winReason = this.winReason;
        copy.p1Territory = this.p1Territory;
        copy.p2Territory = this.p2Territory;
        copy.p1UnblockedEdges = this.p1UnblockedEdges;
        copy.p2UnblockedEdges = this.p2UnblockedEdges;
        return copy;
    }

    public int getRows() {
        return rows;
    }

    public void setRows(int rows) {
        this.rows = rows;
    }

    public int getCols() {
        return cols;
    }

    public void setCols(int cols) {
        this.cols = cols;
    }

    public Board getBoard() {
        return board;
    }

    public void setBoard(Board board) {
        this.board = board;
    }

    public PlayerState getP1() {
        return p1;
    }

    public void setP1(PlayerState p1) {
        this.p1 = p1;
    }

    public PlayerState getP2() {
        return p2;
    }

    public void setP2(PlayerState p2) {
        this.p2 = p2;
    }

    public int getCurrentTurn() {
        return currentTurn;
    }

    public void setCurrentTurn(int currentTurn) {
        this.currentTurn = currentTurn;
    }

    public boolean isOver() {
        return over;
    }

    public void setOver(boolean over) {
        this.over = over;
    }

    public int getWinner() {
        return winner;
    }

    public void setWinner(int winner) {
        this.winner = winner;
    }

    public String getWinReason() {
        return winReason;
    }

    public void setWinReason(String winReason) {
        this.winReason = winReason;
    }

    public int getP1Territory() {
        return p1Territory;
    }

    public void setP1Territory(int p1Territory) {
        this.p1Territory = p1Territory;
    }

    public int getP2Territory() {
        return p2Territory;
    }

    public void setP2Territory(int p2Territory) {
        this.p2Territory = p2Territory;
    }

    public int getP1UnblockedEdges() {
        return p1UnblockedEdges;
    }

    public void setP1UnblockedEdges(int p1UnblockedEdges) {
        this.p1UnblockedEdges = p1UnblockedEdges;
    }

    public int getP2UnblockedEdges() {
        return p2UnblockedEdges;
    }

    public void setP2UnblockedEdges(int p2UnblockedEdges) {
        this.p2UnblockedEdges = p2UnblockedEdges;
    }

    public int getTurnStartR() {
        return turnStartR;
    }

    public void setTurnStartR(int turnStartR) {
        this.turnStartR = turnStartR;
    }

    public int getTurnStartC() {
        return turnStartC;
    }

    public void setTurnStartC(int turnStartC) {
        this.turnStartC = turnStartC;
    }

    public int getCurrentTurnSteps() {
        return currentTurnSteps;
    }

    public void setCurrentTurnSteps(int currentTurnSteps) {
        this.currentTurnSteps = currentTurnSteps;
    }

    public int getRemainingSteps() {
        return Math.max(0, getCurrentPlayer().getEnergy() - currentTurnSteps);
    }
}

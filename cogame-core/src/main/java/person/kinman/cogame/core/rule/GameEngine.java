package person.kinman.cogame.core.rule;

import person.kinman.cogame.core.action.GameAction;
import person.kinman.cogame.core.model.Board;
import person.kinman.cogame.core.model.Direction;
import person.kinman.cogame.core.model.GameState;
import person.kinman.cogame.core.model.PlayerState;

/**
 * 核心游戏规则引擎：动作执行、状态转移与回合切换
 */
public class GameEngine {

    public static boolean executeAction(GameState state, int playerId, GameAction action) {
        if (action == null || action.getType() == null) return false;

        // 重置局随时可执行
        if (action.getType() == GameAction.Type.RESET) {
            state.reset();
            return true;
        }

        // 终局后不再接收走子动作
        if (state.isOver()) return false;

        // 校验是否为该玩家的回合
        if (playerId != 0 && playerId != state.getCurrentTurn()) {
            return false;
        }

        PlayerState player = state.getCurrentPlayer();
        PlayerState opponent = state.getOpponentPlayer();
        Board board = state.getBoard();

        switch (action.getType()) {
            case MOVE -> {
                Direction dir = player.getDirection();
                if (!board.isConnected(player.getR(), player.getC(), dir)) {
                    return false;
                }
                int nr = player.getR() + dir.getDr();
                int nc = player.getC() + dir.getDc();

                // 机制 1: 身位阻挡 (禁止与对手站立在同一格)
                if (nr == opponent.getR() && nc == opponent.getC()) {
                    return false;
                }

                // 机制 2: 动态能量行动力限制 (当前格与该回合起始格距离 <= 当前可用能量)
                int maxAllowed = player.getEnergy();
                int dist = GameEvaluator.getDistanceAvoidingOpponent(
                        board, state.getTurnStartR(), state.getTurnStartC(), nr, nc, opponent.getR(), opponent.getC());
                if (dist < 0 || dist > maxAllowed) {
                    return false;
                }

                player.setR(nr);
                player.setC(nc);
                state.setCurrentTurnSteps(dist);
                return true;
            }
            case ROTATE -> {
                player.setDirection(player.getDirection().clockwise());
                return true;
            }
            case CHANGE_DIR_MOVE -> {
                Direction dir = action.getDirection();
                if (dir == null) return false;
                player.setDirection(dir);
                if (!board.isConnected(player.getR(), player.getC(), dir)) {
                    return true;
                }
                int nr = player.getR() + dir.getDr();
                int nc = player.getC() + dir.getDc();

                // 机制 1: 身位阻挡
                if (nr == opponent.getR() && nc == opponent.getC()) {
                    return true; // 仅转向，不可进入对手格子
                }

                // 机制 2: 动态能量行动力限制
                int maxAllowed = player.getEnergy();
                int dist = GameEvaluator.getDistanceAvoidingOpponent(
                        board, state.getTurnStartR(), state.getTurnStartC(), nr, nc, opponent.getR(), opponent.getC());
                if (dist < 0 || dist > maxAllowed) {
                    return true; // 仅转向，不可超出可用能量步数
                }

                player.setR(nr);
                player.setC(nc);
                state.setCurrentTurnSteps(dist);
                return true;
            }
            case LOCK -> {
                if (action.getDirection() != null) {
                    player.setDirection(action.getDirection());
                }
                Direction dir = player.getDirection();
                boolean locked = board.lockEdge(player.getR(), player.getC(), dir, player.getId());
                if (locked) {
                    // 真实结算能量消耗 (等于落点距该回合起始点的位移步数)
                    int stepsUsed = state.getCurrentTurnSteps();
                    player.setEnergy(Math.max(0, player.getEnergy() - stepsUsed));

                    // 成功封锁后，检查是否满足终局（双方不再连通）
                    GameEvaluator.evaluateGameOver(state);
                    if (!state.isOver()) {
                        state.switchTurn();
                    }
                    return true;
                }
                return false;
            }
            default -> {
                return false;
            }
        }
    }
}

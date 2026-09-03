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
        Board board = state.getBoard();

        switch (action.getType()) {
            case MOVE -> {
                Direction dir = player.getDirection();
                if (board.isConnected(player.getR(), player.getC(), dir)) {
                    player.setR(player.getR() + dir.getDr());
                    player.setC(player.getC() + dir.getDc());
                    return true;
                }
                return false;
            }
            case ROTATE -> {
                player.setDirection(player.getDirection().clockwise());
                return true;
            }
            case CHANGE_DIR_MOVE -> {
                Direction dir = action.getDirection();
                if (dir == null) return false;
                player.setDirection(dir);
                if (board.isConnected(player.getR(), player.getC(), dir)) {
                    player.setR(player.getR() + dir.getDr());
                    player.setC(player.getC() + dir.getDc());
                    return true;
                }
                return true; // 即使被阻挡，方向已经调整
            }
            case LOCK -> {
                Direction dir = player.getDirection();
                boolean locked = board.lockEdge(player.getR(), player.getC(), dir);
                if (locked) {
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

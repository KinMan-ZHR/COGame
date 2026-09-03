package person.kinman.cogame.client.controller;

import person.kinman.cogame.core.action.GameAction;
import person.kinman.cogame.core.model.GameState;
import person.kinman.cogame.core.rule.GameEngine;

import java.util.function.Consumer;

/**
 * 单机双人控制器：同一台设备、同一键盘交替轮流行动
 */
public class LocalController implements GameController {
    private final GameState state = new GameState();
    private Consumer<GameState> onStateChanged;
    private Consumer<String> onNotification;

    @Override
    public void handleUserAction(GameAction action) {
        if (state.isOver() && action.getType() != GameAction.Type.RESET) {
            return;
        }
        int currentTurn = state.getCurrentTurn();
        boolean ok = GameEngine.executeAction(state, currentTurn, action);
        if (ok && onStateChanged != null) {
            onStateChanged.accept(state);
        }
    }

    @Override
    public void resetGame() {
        state.reset();
        if (onStateChanged != null) {
            onStateChanged.accept(state);
        }
    }

    @Override
    public GameState getGameState() {
        return state;
    }

    @Override
    public int getMyPlayerId() {
        return 0; // 0 代表本地双人，双方均可在本设备操作
    }

    @Override
    public String getModeName() {
        return "单机双人对战";
    }

    @Override
    public void setOnStateChanged(Consumer<GameState> listener) {
        this.onStateChanged = listener;
    }

    @Override
    public void setOnNotification(Consumer<String> listener) {
        this.onNotification = listener;
    }

    @Override
    public void close() {}
}

package person.kinman.cogame.client.controller;

import person.kinman.cogame.core.action.GameAction;
import person.kinman.cogame.core.model.GameState;

import java.util.function.Consumer;

/**
 * 游戏控制器接口：统领单机双人、人机对战与网络联机
 */
public interface GameController {
    void handleUserAction(GameAction action);
    void resetGame();
    GameState getGameState();
    int getMyPlayerId(); // 0表示本地双人，1表示P1，2表示P2
    String getModeName();
    void setOnStateChanged(Consumer<GameState> listener);
    void setOnNotification(Consumer<String> listener);
    default void resetGameWithPreference(person.kinman.cogame.core.model.TurnOrderPreference pref) {
        resetGame();
    }
    default void swapTurnOrder() {
        resetGame();
    }
    default person.kinman.cogame.core.model.TurnOrderPreference getCurrentPreference() {
        return person.kinman.cogame.core.model.TurnOrderPreference.RANDOM;
    }
    default void start() {}
    void close();
}

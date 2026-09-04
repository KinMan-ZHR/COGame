package person.kinman.cogame.client.controller;

import person.kinman.cogame.core.action.GameAction;
import person.kinman.cogame.core.model.GameState;
import person.kinman.cogame.core.model.TurnOrderPreference;
import person.kinman.cogame.core.rule.GameEngine;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * 单机双人控制器：同一台设备、同一键盘交替轮流行动
 */
public class LocalController implements GameController {
    private final GameState state;
    private TurnOrderPreference preference;
    private final List<Consumer<GameState>> stateListeners = new CopyOnWriteArrayList<>();
    private final List<Consumer<String>> notificationListeners = new CopyOnWriteArrayList<>();

    public LocalController() {
        this(6, TurnOrderPreference.FIRST);
    }

    public LocalController(int boardSize) {
        this(boardSize, TurnOrderPreference.FIRST);
    }

    public LocalController(int boardSize, TurnOrderPreference preference) {
        this.preference = (preference != null) ? preference : TurnOrderPreference.FIRST;
        this.state = new GameState(boardSize);
        initPlayerNames();
    }

    private void initPlayerNames() {
        boolean p1IsFirst = true;
        if (preference == TurnOrderPreference.SECOND) {
            p1IsFirst = false;
        } else if (preference == TurnOrderPreference.RANDOM) {
            p1IsFirst = new java.util.Random().nextBoolean();
        }
        String myName = person.kinman.cogame.client.profile.ProfileManager.getDisplayName();
        if (p1IsFirst) {
            this.state.getP1().setName(myName + " (先手)");
            this.state.getP2().setName("对手 (后手)");
        } else {
            this.state.getP1().setName("对手 (先手)");
            this.state.getP2().setName(myName + " (后手)");
        }
    }

    @Override
    public void handleUserAction(GameAction action) {
        if (state.isOver() && action.getType() != GameAction.Type.RESET) {
            return;
        }
        int currentTurn = state.getCurrentTurn();
        boolean ok = GameEngine.executeAction(state, currentTurn, action);
        if (ok) {
            notifyState();
        }
    }

    private void notifyState() {
        for (Consumer<GameState> l : stateListeners) {
            try { l.accept(state); } catch (Exception ignored) {}
        }
    }

    private void notifyNotification(String msg) {
        for (Consumer<String> l : notificationListeners) {
            try { l.accept(msg); } catch (Exception ignored) {}
        }
    }

    @Override
    public void resetGame() {
        resetGameWithPreference(this.preference);
    }

    @Override
    public void resetGameWithPreference(TurnOrderPreference pref) {
        if (pref != null) {
            this.preference = pref;
        }
        state.reset();
        initPlayerNames();
        notifyState();
        notifyNotification("⚔️ 本地双人已开新局 (当前先手: " + state.getP1().getName() + ")");
    }

    @Override
    public void swapTurnOrder() {
        TurnOrderPreference newPref = (preference == TurnOrderPreference.FIRST) ? TurnOrderPreference.SECOND : TurnOrderPreference.FIRST;
        resetGameWithPreference(newPref);
    }

    @Override
    public TurnOrderPreference getCurrentPreference() {
        return preference;
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
        if (listener != null) {
            this.stateListeners.add(listener);
        }
    }

    @Override
    public void setOnNotification(Consumer<String> listener) {
        if (listener != null) {
            this.notificationListeners.add(listener);
        }
    }

    @Override
    public void close() {}
}

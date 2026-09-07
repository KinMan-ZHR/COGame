package person.kinman.cogame.client.controller;

import person.kinman.cogame.ai.AiDecision;
import person.kinman.cogame.ai.AiPlaystyle;
import person.kinman.cogame.ai.AiStrategy;
import person.kinman.cogame.core.action.GameAction;
import person.kinman.cogame.core.model.GameState;
import person.kinman.cogame.core.model.TurnOrderPreference;
import person.kinman.cogame.core.rule.GameEngine;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * 人机对战控制器：支持自由分先 (玩家执先 / AI执先 / 随机)，内置多流派算法对局
 */
public class AiController implements GameController {
    private final GameState state;
    private final AiStrategy aiStrategy;
    private final AiPlaystyle playstyle;
    private final int searchDepth;
    private TurnOrderPreference preference;
    private int myPlayerId; // 1 (玩家先手 P1) 或 2 (玩家后手 P2)
    private int aiPlayerId; // 2 或 1
    private final ExecutorService aiExecutor = Executors.newSingleThreadExecutor();

    private final List<Consumer<GameState>> stateListeners = new CopyOnWriteArrayList<>();
    private final List<Consumer<String>> notificationListeners = new CopyOnWriteArrayList<>();
    private volatile boolean aiThinking = false;

    public AiController() {
        this(6, AiPlaystyle.CE_TIAN, 5, TurnOrderPreference.FIRST);
    }

    public AiController(int boardSize) {
        this(boardSize, AiPlaystyle.CE_TIAN, 5, TurnOrderPreference.FIRST);
    }

    public AiController(int boardSize, AiPlaystyle playstyle) {
        this(boardSize, playstyle, 5, TurnOrderPreference.FIRST);
    }

    public AiController(int boardSize, AiPlaystyle playstyle, TurnOrderPreference preference) {
        this(boardSize, playstyle, 5, preference);
    }

    public AiController(int boardSize, AiPlaystyle playstyle, int searchDepth) {
        this(boardSize, playstyle, searchDepth, TurnOrderPreference.FIRST);
    }

    public AiController(int boardSize, AiPlaystyle playstyle, int searchDepth, TurnOrderPreference preference) {
        this.playstyle = (playstyle != null) ? playstyle : AiPlaystyle.CE_TIAN;
        this.searchDepth = Math.max(3, Math.min(10, searchDepth));
        this.preference = (preference != null) ? preference : TurnOrderPreference.FIRST;
        this.aiStrategy = this.playstyle.createStrategy(this.searchDepth);
        this.state = new GameState(boardSize);

        int resolvedHuman = 1;
        if (this.preference == TurnOrderPreference.SECOND) {
            resolvedHuman = 2;
        } else if (this.preference == TurnOrderPreference.RANDOM) {
            resolvedHuman = new java.util.Random().nextBoolean() ? 1 : 2;
        }
        this.myPlayerId = resolvedHuman;
        this.aiPlayerId = (myPlayerId == 1) ? 2 : 1;

        initPlayerNames();
    }

    private void initPlayerNames() {
        String humanName = person.kinman.cogame.client.profile.ProfileManager.getDisplayName();
        String aiName = this.playstyle.getPlayerName();
        if (myPlayerId == 1) {
            state.getP1().setName(humanName);
            state.getP2().setName(aiName);
        } else {
            state.getP1().setName(aiName);
            state.getP2().setName(humanName);
        }
    }

    public AiPlaystyle getPlaystyle() {
        return playstyle;
    }

    public int getSearchDepth() {
        return searchDepth;
    }

    public TurnOrderPreference getPreference() {
        return preference;
    }

    @Override
    public void start() {
        checkAndTriggerAiFirstTurn();
    }

    private void checkAndTriggerAiFirstTurn() {
        if (!state.isOver() && state.getCurrentTurn() == aiPlayerId && !aiThinking) {
            triggerAiTurn();
        }
    }

    @Override
    public void handleUserAction(GameAction action) {
        if (state.isOver() && action.getType() != GameAction.Type.RESET) {
            return;
        }
        if (aiThinking) {
            return; // AI 思考/移动中不接收玩家输入
        }
        if (state.getCurrentTurn() != myPlayerId && action.getType() != GameAction.Type.RESET) {
            return;
        }

        boolean ok = GameEngine.executeAction(state, myPlayerId, action);
        if (ok) {
            notifyState();

            // 若行动后切换到了 AI 的回合且未结束，触发 AI 思考
            if (!state.isOver() && state.getCurrentTurn() == aiPlayerId) {
                triggerAiTurn();
            }
        }
    }

    private void triggerAiTurn() {
        aiThinking = true;
        notifyNotification(playstyle.getPlayerName() + " [深度 " + searchDepth + "] 正在推演局势...");

        aiExecutor.submit(() -> {
            try {
                Thread.sleep(350); // 适度停顿模拟思考
                AiDecision decision = aiStrategy.computeTurn(state, aiPlayerId);

                for (GameAction act : decision.getActions()) {
                    Thread.sleep(200); // 每步动作动画延迟
                    GameEngine.executeAction(state, aiPlayerId, act);
                    notifyState();
                }

                notifyNotification(playstyle.getPlayerName() + " 行动完毕: " + decision.getDescription());
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                aiThinking = false;
            }
        });
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
        int resolvedHuman = 1;
        if (this.preference == TurnOrderPreference.SECOND) {
            resolvedHuman = 2;
        } else if (this.preference == TurnOrderPreference.RANDOM) {
            resolvedHuman = new java.util.Random().nextBoolean() ? 1 : 2;
        }
        this.myPlayerId = resolvedHuman;
        this.aiPlayerId = (myPlayerId == 1) ? 2 : 1;

        state.reset();
        initPlayerNames();
        aiThinking = false;
        notifyState();
        String roleText = (myPlayerId == 1) ? "您执先手 (P1)" : playstyle.getPlayerName() + " 执先 (P1)";
        notifyNotification("⚔️ 新一局开战！分先结果: " + roleText);
        checkAndTriggerAiFirstTurn();
    }

    @Override
    public void swapTurnOrder() {
        TurnOrderPreference newPref = (myPlayerId == 1) ? TurnOrderPreference.SECOND : TurnOrderPreference.FIRST;
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
        return myPlayerId;
    }

    @Override
    public String getModeName() {
        String turnStr = (myPlayerId == 1) ? "玩家先手" : "AI先手";
        return "人机流派挑战 (" + turnStr + " · " + playstyle.getPlayerName() + " · 深度" + searchDepth + ")";
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
    public void close() {
        aiExecutor.shutdownNow();
    }
}

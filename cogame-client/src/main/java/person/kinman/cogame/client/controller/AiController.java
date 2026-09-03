package person.kinman.cogame.client.controller;

import person.kinman.cogame.ai.AiDecision;
import person.kinman.cogame.ai.HeuristicAi;
import person.kinman.cogame.core.action.GameAction;
import person.kinman.cogame.core.model.GameState;
import person.kinman.cogame.core.rule.GameEngine;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * 人机对战控制器：玩家作为P1，内置启发式算法作为P2
 */
public class AiController implements GameController {
    private final GameState state = new GameState();
    private final HeuristicAi ai = new HeuristicAi(3);
    private final ExecutorService aiExecutor = Executors.newSingleThreadExecutor();

    private Consumer<GameState> onStateChanged;
    private Consumer<String> onNotification;
    private volatile boolean aiThinking = false;

    public AiController() {
        state.getP2().setName("端脑AI");
    }

    @Override
    public void handleUserAction(GameAction action) {
        if (state.isOver() && action.getType() != GameAction.Type.RESET) {
            return;
        }
        if (aiThinking) {
            return; // AI 思考/移动中不接收玩家输入
        }
        if (state.getCurrentTurn() != 1 && action.getType() != GameAction.Type.RESET) {
            return;
        }

        boolean ok = GameEngine.executeAction(state, 1, action);
        if (ok) {
            notifyState();

            // 若行动后切换到了 AI (P2) 的回合且未结束，触发 AI 思考
            if (!state.isOver() && state.getCurrentTurn() == 2) {
                triggerAiTurn();
            }
        }
    }

    private void triggerAiTurn() {
        aiThinking = true;
        if (onNotification != null) {
            onNotification.accept("AI 正在深度思考连通策略...");
        }

        aiExecutor.submit(() -> {
            try {
                Thread.sleep(300); // 适度停顿模拟思考
                AiDecision decision = ai.computeTurn(state, 2);

                for (GameAction act : decision.getActions()) {
                    Thread.sleep(200); // 每步动作动画延迟
                    GameEngine.executeAction(state, 2, act);
                    notifyState();
                }

                if (onNotification != null) {
                    onNotification.accept("AI 行动完毕: " + decision.getDescription());
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                aiThinking = false;
            }
        });
    }

    private void notifyState() {
        if (onStateChanged != null) {
            onStateChanged.accept(state);
        }
    }

    @Override
    public void resetGame() {
        state.reset();
        state.getP2().setName("端脑AI");
        aiThinking = false;
        notifyState();
    }

    @Override
    public GameState getGameState() {
        return state;
    }

    @Override
    public int getMyPlayerId() {
        return 1; // 玩家固定为 P1
    }

    @Override
    public String getModeName() {
        return "人机挑战模式 (vs 端脑AI)";
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
    public void close() {
        aiExecutor.shutdownNow();
    }
}

package person.kinman.cogame.client.cli.agent;

import person.kinman.cogame.core.action.GameAction;
import person.kinman.cogame.core.model.GameState;

import java.util.List;

/**
 * 统一参赛实体接口：无论是人类交互、内置AI、外部LLM还是第三方程序，均作为一等公民参赛
 */
public interface PlayerAgent extends AutoCloseable {

    String getName();

    /**
     * 针对当前局面做出本回合的行动决策（复合动作序列：移动 + 转向 + 锁边）
     */
    List<GameAction> act(GameState state, int myPlayerId) throws Exception;

    default void onTurnCompleted(GameState state, int myPlayerId, List<GameAction> actions) {}

    default void onGameOver(GameState state, int myPlayerId) {}

    @Override
    default void close() throws Exception {}
}

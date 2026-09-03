package person.kinman.cogame.ai;

import person.kinman.cogame.core.model.GameState;

public interface AiStrategy {
    /**
     * 计算当前回合 AI 玩家的最佳行动序列
     * @param state 当前棋局状态
     * @param aiPlayerId AI玩家ID (通常为 2)
     * @return 决策方案（动作列表包含移动和最终的锁定）
     */
    AiDecision computeTurn(GameState state, int aiPlayerId);
}

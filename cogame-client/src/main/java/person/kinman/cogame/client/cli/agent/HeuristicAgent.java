package person.kinman.cogame.client.cli.agent;

import person.kinman.cogame.ai.AiDecision;
import person.kinman.cogame.ai.AiStrategy;
import person.kinman.cogame.core.action.GameAction;
import person.kinman.cogame.core.model.GameState;

import java.util.List;
import java.util.Objects;

/**
 * 本地启发式算法 Agent（包装 Antigravity, Codex, Classic 等规则引擎）
 */
public class HeuristicAgent implements PlayerAgent {

    private final String name;
    private final AiStrategy strategy;

    public HeuristicAgent(String name, AiStrategy strategy) {
        this.name = Objects.requireNonNull(name, "name");
        this.strategy = Objects.requireNonNull(strategy, "strategy");
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public List<GameAction> act(GameState state, int myPlayerId) {
        AiDecision decision = strategy.computeTurn(state, myPlayerId);
        return decision.getActions();
    }
}

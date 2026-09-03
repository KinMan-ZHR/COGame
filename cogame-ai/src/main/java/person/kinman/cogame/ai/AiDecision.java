package person.kinman.cogame.ai;

import person.kinman.cogame.core.action.GameAction;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 决策结果：包含一系列要执行的动作（移动、旋转、锁边）和评分
 */
public class AiDecision {
    private final List<GameAction> actions;
    private final double score;
    private final String description;

    public AiDecision(List<GameAction> actions, double score, String description) {
        this.actions = actions != null ? actions : new ArrayList<>();
        this.score = score;
        this.description = description;
    }

    public List<GameAction> getActions() {
        return actions;
    }

    public double getScore() {
        return score;
    }

    public String getDescription() {
        return description;
    }
}

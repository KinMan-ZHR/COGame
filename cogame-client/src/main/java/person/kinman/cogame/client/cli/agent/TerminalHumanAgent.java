package person.kinman.cogame.client.cli.agent;

import person.kinman.cogame.ai.cli.LlmTurnAdvisor;
import person.kinman.cogame.core.action.GameAction;
import person.kinman.cogame.core.model.GameState;
import person.kinman.cogame.core.model.PlayerState;

import java.util.List;
import java.util.Scanner;

/**
 * 终端交互式人类玩家 Agent：通过命令行提示选择候选动作或快捷操作
 */
public class TerminalHumanAgent implements PlayerAgent {

    private final String name;
    private final Scanner scanner = new Scanner(System.in);

    public TerminalHumanAgent(String name) {
        this.name = (name != null && !name.isBlank()) ? name : "HumanPlayer";
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public List<GameAction> act(GameState state, int myPlayerId) {
        PlayerState me = state.getPlayer(myPlayerId);
        List<LlmTurnAdvisor.Candidate> candidates = LlmTurnAdvisor.enumerateCandidates(state);
        if (candidates.isEmpty()) {
            System.out.println("⚠️ 当前无可行动作，自动锁边！");
            return List.of(GameAction.lock());
        }

        System.out.printf("\n👉 【轮到您行动 (%s | P%d)】当前能量: %d/%d | 当前位置: (%d,%d)\n",
                name, myPlayerId, me.getEnergy(), state.getMaxEnergy(), me.getR(), me.getC());
        System.out.println("请选择要执行的行动策略候选序号 (输入 0 ~ " + (candidates.size() - 1) + ")：");

        // 最多展示前 8 个候选推荐
        int showCount = Math.min(8, candidates.size());
        for (int i = 0; i < showCount; i++) {
            LlmTurnAdvisor.Candidate c = candidates.get(i);
            System.out.printf("  [%d] 移动%d步至(%d,%d) 锁[%s] | 状态:%s | 领地预演:己%d:敌%d\n",
                    c.id(), c.path().size(), c.row(), c.col(), c.lockDirection(), c.outcome(),
                    c.myTerritory(), c.opponentTerritory());
        }
        if (candidates.size() > showCount) {
            System.out.printf("  ... (共 %d 个合法候选，默认回车选择 [0])\n", candidates.size());
        }

        System.out.print("请输入编号 [默认 0]: ");
        String line = scanner.nextLine().trim();
        int choice = 0;
        if (!line.isEmpty()) {
            try {
                choice = Integer.parseInt(line);
            } catch (Exception ignored) {}
        }
        if (choice < 0 || choice >= candidates.size()) {
            choice = 0;
        }

        LlmTurnAdvisor.Candidate selected = candidates.get(choice);
        return LlmTurnAdvisor.buildCandidateActions(me, selected);
    }
}

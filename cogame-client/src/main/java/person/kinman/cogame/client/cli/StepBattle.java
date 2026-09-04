package person.kinman.cogame.client.cli;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import person.kinman.cogame.ai.AiDecision;
import person.kinman.cogame.ai.AiStrategy;
import person.kinman.cogame.ai.AntigravityStrategy;
import person.kinman.cogame.ai.CodexStrategy;
import person.kinman.cogame.ai.HeuristicAi;
import person.kinman.cogame.ai.cli.LlmTurnAdvisor;
import person.kinman.cogame.core.action.GameAction;
import person.kinman.cogame.core.model.Board;
import person.kinman.cogame.core.model.Direction;
import person.kinman.cogame.core.model.GameState;
import person.kinman.cogame.core.model.PlayerState;
import person.kinman.cogame.core.rule.GameEngine;

import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class StepBattle {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static class StepSave {
        public GameState state;
        public String engineType;
        public int turn;
    }

    public static void start(int size, String engineType, String filePath) throws Exception {
        GameState state = new GameState(size);
        state.getP1().setName("Antigravity (Agent)");
        state.getP2().setName("Engine (" + engineType + ")");

        StepSave save = new StepSave();
        save.state = state;
        save.engineType = engineType;
        save.turn = 1;

        saveToFile(save, filePath);
        printTurnStatus(save);
    }

    public static void move(String filePath, int choice) throws Exception {
        StepSave save = loadFromFile(filePath);
        GameState state = save.state;
        int p1Id = 1;

        List<LlmTurnAdvisor.Candidate> candidates = LlmTurnAdvisor.enumerateCandidates(state);
        if (candidates.isEmpty()) {
            System.out.println("❌ P1 无合法候选，自动弃权！");
            return;
        }

        if (choice < 0 || choice >= candidates.size()) {
            System.out.printf("❌ 非法选择编号 %d (合法范围: 0 ~ %d)\n", choice, candidates.size() - 1);
            return;
        }

        // 1. 执行 P1 (Agent) 的选择
        LlmTurnAdvisor.Candidate selected = candidates.get(choice);
        PlayerState p1 = state.getPlayer(p1Id);
        List<GameAction> p1Actions = LlmTurnAdvisor.buildCandidateActions(p1, selected);
        for (GameAction act : p1Actions) {
            GameEngine.executeAction(state, p1Id, act);
        }

        System.out.printf("\n▶ 【P1: Antigravity (Agent)】选择候选 [%d]：走 %d 步至 (%d,%d) 锁 [%s] (预期: %s)\n",
                selected.id(), selected.path().size(), selected.row(), selected.col(), selected.lockDirection(), selected.outcome());

        if (state.isOver()) {
            printGameOver(state);
            return;
        }

        // 2. 执行 P2 (内置引擎) 的决策
        save.turn++;
        int p2Id = 2;
        AiStrategy strategy = createStrategy(save.engineType);
        AiDecision p2Decision = strategy.computeTurn(state, p2Id);
        PlayerState p2 = state.getPlayer(p2Id);

        int p2Steps = 0;
        Direction p2Lock = null;
        for (GameAction act : p2Decision.getActions()) {
            if (act.getType() == GameAction.Type.CHANGE_DIR_MOVE || act.getType() == GameAction.Type.MOVE) {
                p2Steps++;
            } else if (act.getType() == GameAction.Type.LOCK) {
                p2Lock = p2.getDirection();
            }
            GameEngine.executeAction(state, p2Id, act);
        }

        System.out.printf("▶ 【P2: 内置引擎 (%s)】反击：走 %d 步至 (%d,%d) 锁 [%s]\n",
                save.engineType, p2Steps, p2.getR(), p2.getC(), p2Lock != null ? p2Lock.getName() : "-");

        if (state.isOver()) {
            printGameOver(state);
            return;
        }

        save.turn++;
        saveToFile(save, filePath);
        printTurnStatus(save);
    }

    private static AiStrategy createStrategy(String type) {
        if (type == null) return new AntigravityStrategy();
        return switch (type.toLowerCase()) {
            case "codex" -> new CodexStrategy();
            case "classic", "guard" -> new HeuristicAi(3);
            default -> new AntigravityStrategy();
        };
    }

    private static void printTurnStatus(StepSave save) {
        GameState state = save.state;
        Board board = state.getBoard();
        PlayerState p1 = state.getP1();
        PlayerState p2 = state.getP2();

        System.out.println("\n" + "=".repeat(64));
        System.out.printf("【第 %d 回合】当前盘面 (6x6)\n", save.turn);
        System.out.printf("P1(青): (%d,%d) 朝向:%s 能量:%d/%d\n", p1.getR(), p1.getC(), p1.getDirection(), p1.getEnergy(), state.getMaxEnergy());
        System.out.printf("P2(金): (%d,%d) 朝向:%s 能量:%d/%d\n", p2.getR(), p2.getC(), p2.getDirection(), p2.getEnergy(), state.getMaxEnergy());
        System.out.println("=".repeat(64));

        renderBoard(state);

        List<LlmTurnAdvisor.Candidate> candidates = LlmTurnAdvisor.enumerateCandidates(state);
        System.out.println("\n👉 请为 P1 选择行动候选 (传入 --choice <编号>)：");
        int count = Math.min(12, candidates.size());
        for (int i = 0; i < count; i++) {
            LlmTurnAdvisor.Candidate c = candidates.get(i);
            System.out.printf("  [%d] 路径:%-6s 落点:(%d,%d) 锁:%-5s 判定:%-8s 领地预演:己%d:敌%d (开放边:己%d:敌%d)\n",
                    c.id(), LlmTurnAdvisor.pathText(c.path()), c.row(), c.col(), c.lockDirection(), c.outcome(),
                    c.myTerritory(), c.opponentTerritory(), c.myEdges(), c.opponentEdges());
        }
        if (candidates.size() > count) {
            System.out.printf("  ... (共 %d 个候选可选)\n", candidates.size());
        }
    }

    private static void printGameOver(GameState state) {
        System.out.println("\n" + "#".repeat(64));
        String winner = state.getWinner() == 1 ? state.getP1().getName() : (state.getWinner() == 2 ? state.getP2().getName() : "平局");
        System.out.printf("🏆 终局对决结束！胜者: 【%s】\n", winner);
        System.out.printf("判定原因: %s\n", state.getWinReason());
        System.out.printf("领地比分: P1(%s)=%d 格, P2(%s)=%d 格\n",
                state.getP1().getName(), state.getP1Territory(),
                state.getP2().getName(), state.getP2Territory());
        System.out.println("#".repeat(64));
        renderBoard(state);
    }

    private static void renderBoard(GameState state) {
        Board board = state.getBoard();
        int rows = board.getRows();
        int cols = board.getCols();
        PlayerState p1 = state.getP1();
        PlayerState p2 = state.getP2();

        System.out.println("   +---".repeat(cols) + "+");
        for (int r = 0; r < rows; r++) {
            StringBuilder line = new StringBuilder("   |");
            for (int c = 0; c < cols; c++) {
                String mark = " . ";
                if (p1.getR() == r && p1.getC() == c) mark = " 1 ";
                else if (p2.getR() == r && p2.getC() == c) mark = " 2 ";
                line.append(mark);

                if (c < cols - 1) {
                    boolean open = board.isConnected(r, c, Direction.RIGHT);
                    int locker = board.getEdgeLocker(r, c, Direction.RIGHT);
                    if (open) line.append(" ");
                    else if (locker == 1) line.append("|");
                    else if (locker == 2) line.append(":");
                    else line.append("#");
                } else {
                    line.append("|");
                }
            }
            System.out.println(line);

            if (r < rows - 1) {
                StringBuilder hLine = new StringBuilder("   +");
                for (int c = 0; c < cols; c++) {
                    boolean open = board.isConnected(r, c, Direction.DOWN);
                    int locker = board.getEdgeLocker(r, c, Direction.DOWN);
                    if (open) hLine.append("   +");
                    else if (locker == 1) hLine.append("---+");
                    else if (locker == 2) hLine.append("===+");
                    else hLine.append("###+");
                }
                System.out.println(hLine);
            }
        }
        System.out.println("   +---".repeat(cols) + "+");
    }

    private static void saveToFile(StepSave save, String path) throws Exception {
        try (FileWriter fw = new FileWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(save, fw);
        }
    }

    private static StepSave loadFromFile(String path) throws Exception {
        try (FileReader fr = new FileReader(path, StandardCharsets.UTF_8)) {
            return GSON.fromJson(fr, StepSave.class);
        }
    }
}

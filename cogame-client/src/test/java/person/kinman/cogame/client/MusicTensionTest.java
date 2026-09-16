package person.kinman.cogame.client;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import person.kinman.cogame.client.ui.GameCanvas;
import person.kinman.cogame.core.model.Board;
import person.kinman.cogame.core.model.Direction;
import person.kinman.cogame.core.model.GameState;
import person.kinman.cogame.core.rule.GameEvaluator;

public class MusicTensionTest {

    @Test
    public void testOpeningPhaseIsNotTenseOnAllBoardSizes() {
        // 6x6 标准盘开局
        GameState state6 = new GameState(6, 6);
        Assertions.assertEquals(0, GameCanvas.countPlayerLockedEdges(state6.getBoard()));
        Assertions.assertFalse(GameCanvas.isTenseSituation(state6), "6x6 开局首步不应触发紧张态");

        // 9x9 中盘开局（包含预置中立障碍）
        GameState state9 = new GameState(9, 9);
        GameEvaluator.setupNeutralBarriers(state9.getBoard(), 0.12, 12345L);
        Assertions.assertEquals(0, GameCanvas.countPlayerLockedEdges(state9.getBoard()), "中立障碍不应计入玩家锁边");
        Assertions.assertFalse(GameCanvas.isTenseSituation(state9), "9x9 开局即使预置中立障碍也不应触发紧张态");

        // 12x12 大盘开局（包含 16% 中立障碍）
        GameState state12 = new GameState(12, 12);
        GameEvaluator.setupNeutralBarriers(state12.getBoard(), 0.16, 54321L);
        Assertions.assertEquals(0, GameCanvas.countPlayerLockedEdges(state12.getBoard()), "中立障碍不应计入玩家锁边");
        Assertions.assertFalse(GameCanvas.isTenseSituation(state12), "12x12 大盘开局不应触发紧张态");
    }

    @Test
    public void testSuffocationCrisisTriggersTension() {
        GameState state = new GameState(6, 6);
        Board board = state.getBoard();

        // P1 在 (0,0)，初始出度为 2（RIGHT, DOWN）
        // 玩家 2 封锁 P1 右侧边，让 P1 只剩 1 个出口 (DOWN)
        board.lockEdge(0, 0, Direction.RIGHT, 2);

        Assertions.assertEquals(1, board.getOpenDirections(0, 0).size());
        Assertions.assertTrue(GameCanvas.isTenseSituation(state),
                "唯一保留触发场景：任一方出度 <= 1 陷入死胡同时，必须触发紧迫变奏");
    }

    @Test
    public void testOtherScenariosCancelledFromTriggeringTense() {
        GameState state = new GameState(6, 6);
        Board board = state.getBoard();

        // 1. 测试近距离接触场景（已取消紧迫触发）
        // 双方贴脸相距仅 1 步 (P1在2,2, P2在2,3)，且已有玩家锁边，但双方出度 >= 2
        state.getP1().setR(2);
        state.getP1().setC(2);
        state.getP2().setR(2);
        state.getP2().setC(3);
        board.lockEdge(0, 0, Direction.RIGHT, 1); // 放置一条普通玩家锁边

        Assertions.assertTrue(board.getOpenDirections(2, 2).size() >= 2);
        Assertions.assertTrue(board.getOpenDirections(2, 3).size() >= 2);
        Assertions.assertFalse(GameCanvas.isTenseSituation(state),
                "近身接触场景已被取消，只要出度 >= 2 就不得切入紧迫状态");

        // 2. 测试高锁边数/残局饱和度场景（已取消紧迫触发）
        // 棋盘上放置大量墙体（如 16 条），但双方依然自由（出度 >= 2）
        for (int i = 0; i < 4; i++) {
            board.lockEdge(i, 0, Direction.RIGHT, 1);
            board.lockEdge(5 - i, 5, Direction.LEFT, 2);
            board.lockEdge(0, i, Direction.DOWN, 1);
            board.lockEdge(5, 5 - i, Direction.UP, 2);
        }
        Assertions.assertTrue(GameCanvas.countPlayerLockedEdges(board) >= 12);
        Assertions.assertFalse(GameCanvas.isTenseSituation(state),
                "残局高饱和度与割边场景已被取消，只要未陷入出度 <= 1 死胡同，持续保持平和曲");
    }

    @Test
    public void testHysteresisAndAntiJitter() {
        person.kinman.cogame.client.controller.LocalController controller = new person.kinman.cogame.client.controller.LocalController(6);
        GameCanvas canvas = new GameCanvas(controller);
        GameState state = controller.getGameState();

        // 初始开局：非紧张态
        Assertions.assertFalse(canvas.isInTenseMode(), "初始开局应处于平和模式");

        // 制造绝境出度危机（P1 出度 <= 1）
        state.getBoard().lockEdge(0, 0, Direction.RIGHT, 2);
        canvas.updateMusicForTesting(state);
        Assertions.assertTrue(canvas.isInTenseMode(), "出度 <= 1 危机发生后应进入紧张变奏模式");

        // 解除直接危机（出度恢复）但未重新开局：应当单向锁定紧张变奏，防止反复横跳
        state.getBoard().unlockEdge(0, 0, Direction.RIGHT);
        // 放置一条普通玩家锁边保证 playerWalls > 0
        state.getBoard().lockEdge(3, 3, Direction.RIGHT, 1);
        canvas.updateMusicForTesting(state);
        Assertions.assertTrue(canvas.isInTenseMode(), "单向升级机制应保持紧张变奏，避免反复横跳破坏听感");

        // 游戏终局
        state.setOver(true);
        canvas.updateMusicForTesting(state);
        Assertions.assertFalse(canvas.isInTenseMode(), "终局后重置紧张态，进入终局和弦");
    }
}

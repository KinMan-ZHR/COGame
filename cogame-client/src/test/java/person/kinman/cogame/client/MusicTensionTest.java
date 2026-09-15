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
    public void testSuffocationTriggerTension() {
        GameState state = new GameState(6, 6);
        Board board = state.getBoard();

        // P1 在 (0,0)，初始出度为 2（RIGHT, DOWN）
        // 玩家 2 封锁 P1 右侧边，让 P1 只剩 1 个出口 (DOWN)
        board.lockEdge(0, 0, Direction.RIGHT, 2);

        Assertions.assertEquals(1, board.getOpenDirections(0, 0).size());
        Assertions.assertTrue(GameCanvas.isTenseSituation(state), "玩家出度 <= 1 陷入死胡同时必须立即触发紧张博弈变奏");
    }

    @Test
    public void testProximityHandToHandCombat() {
        GameState state = new GameState(6, 6);
        Board board = state.getBoard();

        // 将双方移动至近身肉搏距离（例如 P1 在 2,2，P2 在 2,4，最短距离为 2 步）
        state.getP1().setR(2);
        state.getP1().setC(2);
        state.getP2().setR(2);
        state.getP2().setC(4);
        board.lockEdge(0, 0, Direction.RIGHT, 1); // 至少有一条玩家放置的边

        Assertions.assertTrue(GameCanvas.isTenseSituation(state), "双方物理距离 <= 3 步近身接触时应当触发紧张变奏");
    }

    @Test
    public void testCriticalBridgeDetection() {
        GameState state = new GameState(6, 6);
        Board board = state.getBoard();

        // 将双方置于相距 4 步的位置
        state.getP1().setR(1);
        state.getP1().setC(1);
        state.getP2().setR(1);
        state.getP2().setC(5);

        // 封锁其他可能的分支，使 (1,2)-(1,3) 成为连接两人的关键割边 (Bridge)
        // 只要封锁整行上下通道
        for (int c = 0; c < 6; c++) {
            board.lockEdge(0, c, Direction.DOWN, 1);
            board.lockEdge(1, c, Direction.DOWN, 2);
        }

        Assertions.assertTrue(GameCanvas.isTenseSituation(state), "威胁距离内存在一击必杀的割边（Bridge）时应触发紧张态");
    }

    @Test
    public void testHysteresisAndAntiJitter() {
        person.kinman.cogame.client.controller.LocalController controller = new person.kinman.cogame.client.controller.LocalController(6);
        GameCanvas canvas = new GameCanvas(controller);
        GameState state = controller.getGameState();

        // 初始开局：非紧张态
        Assertions.assertFalse(canvas.isInTenseMode(), "初始开局应处于平和模式");

        // 制造紧张危机（P1 出度 <= 1）
        state.getBoard().lockEdge(0, 0, Direction.RIGHT, 2);
        canvas.updateMusicForTesting(state);
        Assertions.assertTrue(canvas.isInTenseMode(), "危机发生后应进入紧张变奏模式");

        // 解除直接危机但未重新开局：应当单向锁定紧张变奏，防止忽上忽下
        state.getBoard().unlockEdge(0, 0, Direction.RIGHT);
        // 放置一条普通玩家锁边使 playerWalls > 0
        state.getBoard().lockEdge(3, 3, Direction.RIGHT, 1);
        canvas.updateMusicForTesting(state);
        Assertions.assertTrue(canvas.isInTenseMode(), "单向升级机制应保持紧张变奏，避免反复横跳破坏听感");

        // 游戏终局
        state.setOver(true);
        canvas.updateMusicForTesting(state);
        Assertions.assertFalse(canvas.isInTenseMode(), "终局后重置紧张态，进入终局和弦");
    }
}

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
    public void testSmallBoardOpeningPacingNotPremature() {
        // 验证小棋盘前几回合开局保护期：即使双方在开局阶段身位接近，也不过早切歌
        GameState state = new GameState(6, 6);
        Board board = state.getBoard();

        // 模拟开局第 2 回合：双方走到中央探路，物理距离仅相距 2 步
        state.getP1().setR(2);
        state.getP1().setC(2);
        state.getP2().setR(2);
        state.getP2().setC(4);

        // 双方各自只放置了 1 道起始墙 (总玩家墙体 = 2 < 8)
        board.lockEdge(0, 0, Direction.RIGHT, 1);
        board.lockEdge(5, 5, Direction.LEFT, 2);

        Assertions.assertEquals(2, GameCanvas.countPlayerLockedEdges(board));
        Assertions.assertFalse(GameCanvas.isTenseSituation(state),
                "小棋盘开局仅 2 条锁边且棋盘空旷，即使身位接近也必须处于平和期，留足沉思听感");
    }

    @Test
    public void testSuffocationAmbushTrigger() {
        GameState state = new GameState(6, 6);
        Board board = state.getBoard();

        // P1 在 (0,0)，被困在死胡同（出度 = 1）
        board.lockEdge(0, 0, Direction.RIGHT, 2);
        board.lockEdge(1, 1, Direction.RIGHT, 1);
        board.lockEdge(2, 2, Direction.RIGHT, 1);
        board.lockEdge(3, 3, Direction.RIGHT, 1); // 累积 4 条墙

        // 若对手在千里之外 (5,5)，无直接威胁，不开紧迫曲
        state.getP2().setR(5);
        state.getP2().setC(5);
        Assertions.assertFalse(GameCanvas.isTenseSituation(state), "对手在棋盘远端无法关门斩杀，不应虚报紧张态");

        // 当对手已近身至 (1,0) (距离 1 步，随时可关门截杀)
        state.getP2().setR(1);
        state.getP2().setC(0);
        Assertions.assertTrue(GameCanvas.isTenseSituation(state), "绝境死胡同且对手逼近 <= 3 步具备斩杀能力时，必须切入紧迫变奏");
    }

    @Test
    public void testMidEndgameHandToHandCombat() {
        GameState state = new GameState(6, 6);
        Board board = state.getBoard();

        // 进入中盘（已放置 8 条以上玩家墙体）
        for (int i = 0; i < 4; i++) {
            board.lockEdge(i, 0, Direction.RIGHT, 1);
            board.lockEdge(5 - i, 5, Direction.LEFT, 2);
        }
        Assertions.assertTrue(GameCanvas.countPlayerLockedEdges(board) >= 8);

        // 双方在窄道狭路相逢（相距 2 步，出度受限 <= 2）
        state.getP1().setR(2);
        state.getP1().setC(2);
        state.getP2().setR(2);
        state.getP2().setC(4);
        board.lockEdge(2, 2, Direction.UP, 1); // 限制出度

        Assertions.assertTrue(GameCanvas.isTenseSituation(state), "中盘白热化阶段，狭路相逢且出度受限必须触发紧张变奏");
    }

    @Test
    public void testCriticalBridgeDetectionInMidEndgame() {
        GameState state = new GameState(6, 6);
        Board board = state.getBoard();

        // 铺设 8 条玩家墙体，模拟中后盘格局
        for (int c = 0; c < 6; c++) {
            board.lockEdge(0, c, Direction.DOWN, 1);
            board.lockEdge(1, c, Direction.DOWN, 2);
        }
        Assertions.assertTrue(GameCanvas.countPlayerLockedEdges(board) >= 8);

        // 双方在 (1,1) 与 (1,4) 相距 3 步对峙，中间存在一锁即绝杀的关键割边
        state.getP1().setR(1);
        state.getP1().setC(1);
        state.getP2().setR(1);
        state.getP2().setC(4);

        Assertions.assertTrue(GameCanvas.isTenseSituation(state), "中后盘威胁距离内存在一击必杀的割边（Bridge）时应触发紧张态");
    }

    @Test
    public void testHysteresisAndAntiJitter() {
        person.kinman.cogame.client.controller.LocalController controller = new person.kinman.cogame.client.controller.LocalController(6);
        GameCanvas canvas = new GameCanvas(controller);
        GameState state = controller.getGameState();

        // 初始开局：非紧张态
        Assertions.assertFalse(canvas.isInTenseMode(), "初始开局应处于平和模式");

        // 制造致命伏击危机（P1 出度 <= 1，P2 近身 1 步，墙体 >= 4）
        state.getBoard().lockEdge(0, 0, Direction.RIGHT, 2);
        state.getBoard().lockEdge(1, 1, Direction.RIGHT, 1);
        state.getBoard().lockEdge(2, 2, Direction.RIGHT, 1);
        state.getBoard().lockEdge(3, 3, Direction.RIGHT, 1);
        state.getP2().setR(1);
        state.getP2().setC(0);

        canvas.updateMusicForTesting(state);
        Assertions.assertTrue(canvas.isInTenseMode(), "致命伏击危机发生后应进入紧张变奏模式");

        // 解除直接危机但未重新开局：应当单向锁定紧张变奏，防止反复横跳
        state.getP2().setR(4);
        state.getP2().setC(4);
        canvas.updateMusicForTesting(state);
        Assertions.assertTrue(canvas.isInTenseMode(), "单向升级机制应保持紧张变奏，避免反复横跳破坏听感");

        // 游戏终局
        state.setOver(true);
        canvas.updateMusicForTesting(state);
        Assertions.assertFalse(canvas.isInTenseMode(), "终局后重置紧张态，进入终局和弦");
    }
}

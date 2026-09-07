package person.kinman.cogame.client;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import person.kinman.cogame.ai.AiPlaystyle;
import person.kinman.cogame.client.controller.AiController;
import person.kinman.cogame.client.ui.GameCanvas;
import person.kinman.cogame.core.model.Board;
import person.kinman.cogame.core.model.Direction;
import person.kinman.cogame.core.rule.GameEvaluator;

import java.util.List;

public class MouseInteractionTest {

    @Test
    public void testMouseDirectionQuadrantCalculation() {
        // 验证象限向量至方向的精确判定
        Assertions.assertEquals(Direction.UP, GameCanvas.getDirectionFromOffset(0, -20));
        Assertions.assertEquals(Direction.DOWN, GameCanvas.getDirectionFromOffset(0, 20));
        Assertions.assertEquals(Direction.LEFT, GameCanvas.getDirectionFromOffset(-20, 0));
        Assertions.assertEquals(Direction.RIGHT, GameCanvas.getDirectionFromOffset(20, 0));

        // 对角偏向测试
        Assertions.assertEquals(Direction.RIGHT, GameCanvas.getDirectionFromOffset(25, -10));
        Assertions.assertEquals(Direction.UP, GameCanvas.getDirectionFromOffset(10, -25));
        Assertions.assertEquals(Direction.LEFT, GameCanvas.getDirectionFromOffset(-25, 10));
        Assertions.assertEquals(Direction.DOWN, GameCanvas.getDirectionFromOffset(-10, 25));
    }

    @Test
    public void testAiControllerSearchDepthClamping() {
        AiController c1 = new AiController(6, AiPlaystyle.CE_TIAN, 5);
        Assertions.assertEquals(5, c1.getSearchDepth());

        AiController c2 = new AiController(6, AiPlaystyle.JUE_YING, 10);
        Assertions.assertEquals(10, c2.getSearchDepth());

        // 越界自动钳位至 [3, 10]
        AiController cMin = new AiController(6, AiPlaystyle.CE_TIAN, 1);
        Assertions.assertEquals(3, cMin.getSearchDepth());

        AiController cMax = new AiController(6, AiPlaystyle.JUE_YING, 99);
        Assertions.assertEquals(10, cMax.getSearchDepth());
    }

    @Test
    public void testMousePathfindingAvoidingOpponent() {
        Board board = new Board(6, 6);
        // 起点 (0,0), 终点 (2,0), 对手站在 (1,0) 阻挡通路
        List<Direction> path = GameEvaluator.findPathAvoidingOpponent(board, 0, 0, 2, 0, 1, 0);
        Assertions.assertFalse(path.isEmpty(), "应当能绕过对手身位寻路");
        // 绕行应当为: (0,0) -> (0,1) -> (1,1) -> (2,1) -> (2,0)
        Assertions.assertEquals(4, path.size(), "绕过身位步数应当为 4");
    }
}

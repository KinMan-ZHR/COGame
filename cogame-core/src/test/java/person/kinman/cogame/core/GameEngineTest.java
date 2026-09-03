package person.kinman.cogame.core;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import java.util.List;
import person.kinman.cogame.core.action.GameAction;
import person.kinman.cogame.core.model.Board;
import person.kinman.cogame.core.model.Direction;
import person.kinman.cogame.core.model.GameState;
import person.kinman.cogame.core.rule.GameEngine;
import person.kinman.cogame.core.rule.GameEvaluator;

public class GameEngineTest {

    @Test
    public void testInitialStateAndMovement() {
        GameState state = new GameState();
        Assertions.assertEquals(1, state.getCurrentTurn());
        Assertions.assertEquals(0, state.getP1().getR());
        Assertions.assertEquals(0, state.getP1().getC());

        // P1 moves down
        boolean moved = GameEngine.executeAction(state, 1, GameAction.move());
        Assertions.assertTrue(moved);
        Assertions.assertEquals(1, state.getP1().getR());
        Assertions.assertEquals(0, state.getP1().getC());
        // Turn should still be P1
        Assertions.assertEquals(1, state.getCurrentTurn());
    }

    @Test
    public void testLockAndTurnSwitch() {
        GameState state = new GameState();
        // P1 starts facing DOWN, locks edge between (0,0) and (1,0)
        boolean locked = GameEngine.executeAction(state, 1, GameAction.lock());
        Assertions.assertTrue(locked);
        Assertions.assertFalse(state.getBoard().isConnected(0, 0, Direction.DOWN));
        // Turn should switch to P2
        Assertions.assertEquals(2, state.getCurrentTurn());
    }

    @Test
    public void testEdgeLockerTracking() {
        GameState state = new GameState();
        // Turn 1: P1 locks edge DOWN of (0,0)
        boolean locked = GameEngine.executeAction(state, 1, GameAction.lock());
        Assertions.assertTrue(locked);
        Assertions.assertEquals(1, state.getBoard().getEdgeLocker(0, 0, Direction.DOWN));
        Assertions.assertEquals(1, state.getBoard().getEdgeLocker(1, 0, Direction.UP));

        // Turn 2: P2 locks edge UP of (5,5)
        boolean locked2 = GameEngine.executeAction(state, 2, GameAction.lock());
        Assertions.assertTrue(locked2);
        Assertions.assertEquals(2, state.getBoard().getEdgeLocker(5, 5, Direction.UP));
        Assertions.assertEquals(2, state.getBoard().getEdgeLocker(4, 5, Direction.DOWN));
    }

    @Test
    public void testDisconnectGameOver() {
        GameState state = new GameState();
        // Disconnect P1 (at 0,0) from the rest of the board:
        // Lock right edge of (0,0): between (0,0) and (0,1)
        state.getBoard().lockEdge(0, 0, Direction.RIGHT);
        // Lock down edge of (0,0): between (0,0) and (1,0)
        state.getBoard().lockEdge(0, 0, Direction.DOWN);

        GameEvaluator.evaluateGameOver(state);
        Assertions.assertTrue(state.isOver());
        Assertions.assertEquals(1, state.getP1Territory()); // only (0,0)
        Assertions.assertEquals(35, state.getP2Territory()); // remaining 35 cells
        Assertions.assertEquals(2, state.getWinner()); // P2 wins
    }

    @Test
    public void testLargeBoard13x13() {
        GameState state = new GameState(13);
        Assertions.assertEquals(13, state.getRows());
        Assertions.assertEquals(13, state.getCols());
        Assertions.assertEquals(0, state.getP1().getR());
        Assertions.assertEquals(0, state.getP1().getC());
        Assertions.assertEquals(12, state.getP2().getR());
        Assertions.assertEquals(12, state.getP2().getC());

        // Test path exists between (0,0) and (12,12)
        Assertions.assertTrue(GameEvaluator.hasPath(state.getBoard(), 0, 0, 12, 12));

        // Test moving and locking on 13x13
        List<Direction> openDirs = state.getBoard().getOpenDirections(0, 0);
        Assertions.assertFalse(openDirs.isEmpty());
        Direction moveDir = openDirs.get(0);
        boolean moved = GameEngine.executeAction(state, 1, GameAction.changeDirMove(moveDir));
        Assertions.assertTrue(moved);

        List<Direction> nextOpen = state.getBoard().getOpenDirections(state.getP1().getR(), state.getP1().getC());
        Assertions.assertFalse(nextOpen.isEmpty());
        state.getP1().setDirection(nextOpen.get(0));

        boolean locked = GameEngine.executeAction(state, 1, GameAction.lock());
        Assertions.assertTrue(locked);
        Assertions.assertEquals(2, state.getCurrentTurn());
    }

    @Test
    public void testSolidCollisionBlocking() {
        GameState state = new GameState();
        // Place P2 at (1, 0) right in front of P1 (0, 0)
        state.getP2().setR(1);
        state.getP2().setC(0);

        // P1 attempts to move into P2's cell (1, 0)
        boolean moved = GameEngine.executeAction(state, 1, GameAction.move());
        Assertions.assertFalse(moved, "P1 无法踩入对手 P2 所在的格子（身位阻挡）");
        Assertions.assertEquals(0, state.getP1().getR());
        Assertions.assertEquals(0, state.getP1().getC());
    }

    @Test
    public void testMax3StepsPerTurn() {
        GameState state = new GameState();
        Assertions.assertEquals(0, state.getCurrentTurnSteps());

        // Step 1: (0,0) -> (1,0)
        boolean s1 = GameEngine.executeAction(state, 1, GameAction.changeDirMove(Direction.DOWN));
        Assertions.assertTrue(s1);
        Assertions.assertEquals(1, state.getCurrentTurnSteps());

        // Step 2: (1,0) -> (2,0)
        boolean s2 = GameEngine.executeAction(state, 1, GameAction.changeDirMove(Direction.DOWN));
        Assertions.assertTrue(s2);
        Assertions.assertEquals(2, state.getCurrentTurnSteps());

        // Step 3: (2,0) -> (3,0)
        boolean s3 = GameEngine.executeAction(state, 1, GameAction.changeDirMove(Direction.DOWN));
        Assertions.assertTrue(s3);
        Assertions.assertEquals(3, state.getCurrentTurnSteps());

        // Step 4: (3,0) -> (4,0) -> MUST FAIL (超出3步限制)
        boolean s4 = GameEngine.executeAction(state, 1, GameAction.move());
        Assertions.assertFalse(s4, "单回合移动不可超过 3 步");
        Assertions.assertEquals(3, state.getP1().getR());

        // Step back: (3,0) -> (2,0) -> MUST SUCCEED (回退，距离缩减为2)
        boolean back = GameEngine.executeAction(state, 1, GameAction.changeDirMove(Direction.UP));
        Assertions.assertTrue(back);
        Assertions.assertEquals(2, state.getP1().getR());
        Assertions.assertEquals(2, state.getCurrentTurnSteps());

        // Step branch: (2,0) -> (2,1) -> MUST SUCCEED (距离为3)
        boolean branch = GameEngine.executeAction(state, 1, GameAction.changeDirMove(Direction.RIGHT));
        Assertions.assertTrue(branch);
        Assertions.assertEquals(2, state.getP1().getR());
        Assertions.assertEquals(1, state.getP1().getC());
        Assertions.assertEquals(3, state.getCurrentTurnSteps());

        // Lock edge: ends turn and switches to P2
        boolean locked = GameEngine.executeAction(state, 1, GameAction.lock());
        Assertions.assertTrue(locked);
        Assertions.assertEquals(2, state.getCurrentTurn());
        // In P2's turn, steps count resets to 0 and turn start is P2's position
        Assertions.assertEquals(0, state.getCurrentTurnSteps());
        Assertions.assertEquals(state.getP2().getR(), state.getTurnStartR());
        Assertions.assertEquals(state.getP2().getC(), state.getTurnStartC());
    }

    @Test
    public void testEnergyAccumulationAndBurst() {
        GameState state = new GameState(6);
        Assertions.assertEquals(3, state.getP1().getEnergy());
        Assertions.assertEquals(5, state.getMaxEnergy());

        // P1 takes 0 steps, locks edge facing DOWN directly
        boolean p1Locked = GameEngine.executeAction(state, 1, GameAction.lock());
        Assertions.assertTrue(p1Locked);
        // P1 used 0 steps, so retained 3 energy
        Assertions.assertEquals(3, state.getP1().getEnergy());

        // P2's turn: initial 3 + 3 (capped at 5) = 5 energy
        Assertions.assertEquals(5, state.getP2().getEnergy());
        // P2 takes 1 step down and locks
        GameEngine.executeAction(state, 2, GameAction.changeDirMove(Direction.UP));
        GameEngine.executeAction(state, 2, GameAction.lock());
        // P2 used 1 step, retained 4 energy
        Assertions.assertEquals(4, state.getP2().getEnergy());

        // P1's turn: 3 retained + 3 regen = 6, capped at 5!
        Assertions.assertEquals(5, state.getP1().getEnergy());
        // P1 now has 5 energy and can sprint up to 5 steps!
        Assertions.assertEquals(5, state.getRemainingSteps());
    }

    @Test
    public void testMediumBoard9x9WithPreSetBarriers() {
        GameState state = new GameState(9);
        Assertions.assertEquals(9, state.getRows());
        Assertions.assertEquals(9, state.getCols());
        Assertions.assertEquals(8, state.getMaxEnergy()); // 9 - 1 = 8
        Assertions.assertEquals(4, state.getEnergyRegen()); // 9 / 2 = 4
        Assertions.assertEquals(4, state.getP1().getEnergy());
        Assertions.assertEquals(4, state.getP2().getEnergy());

        // 连通性必须完好
        Assertions.assertTrue(GameEvaluator.hasPath(state.getBoard(), 0, 0, 8, 8));

        // 检查是否存在中立预置墙 (locker == 3)
        boolean hasNeutralBarrier = false;
        Board board = state.getBoard();
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                if (board.getEdgeLocker(r, c, Direction.RIGHT) == 3 ||
                    board.getEdgeLocker(r, c, Direction.DOWN) == 3) {
                    hasNeutralBarrier = true;
                    break;
                }
            }
        }
        Assertions.assertTrue(hasNeutralBarrier, "9x9 战术中盘应当成功生成中立要塞废墟墙");
    }

    @Test
    public void testLargeBoard12x12WithPreSetBarriers() {
        GameState state = new GameState(12);
        Assertions.assertEquals(12, state.getRows());
        Assertions.assertEquals(12, state.getCols());
        Assertions.assertEquals(11, state.getMaxEnergy()); // 12 - 1 = 11
        Assertions.assertEquals(6, state.getEnergyRegen()); // 12 / 2 = 6
        Assertions.assertEquals(6, state.getP1().getEnergy());
        Assertions.assertEquals(6, state.getP2().getEnergy());

        // 连通性必须完好
        Assertions.assertTrue(GameEvaluator.hasPath(state.getBoard(), 0, 0, 11, 11));

        // 检查是否存在中立预置墙 (locker == 3)
        boolean hasNeutralBarrier = false;
        Board board = state.getBoard();
        for (int r = 0; r < 12; r++) {
            for (int c = 0; c < 12; c++) {
                if (board.getEdgeLocker(r, c, Direction.RIGHT) == 3 ||
                    board.getEdgeLocker(r, c, Direction.DOWN) == 3) {
                    hasNeutralBarrier = true;
                    break;
                }
            }
        }
        Assertions.assertTrue(hasNeutralBarrier, "12x12 战略大盘应当成功生成中立迷宫隔断墙");
    }
}

package person.kinman.cogame.core;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import person.kinman.cogame.core.action.GameAction;
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
        boolean moved = GameEngine.executeAction(state, 1, GameAction.changeDirMove(Direction.RIGHT));
        Assertions.assertTrue(moved);
        Assertions.assertEquals(0, state.getP1().getR());
        Assertions.assertEquals(1, state.getP1().getC());

        boolean locked = GameEngine.executeAction(state, 1, GameAction.lock());
        Assertions.assertTrue(locked);
        Assertions.assertEquals(2, state.getCurrentTurn());
    }
}

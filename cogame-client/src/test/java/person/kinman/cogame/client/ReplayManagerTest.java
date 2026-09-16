package person.kinman.cogame.client;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import person.kinman.cogame.client.replay.ReplayManager;
import person.kinman.cogame.client.replay.TurnSnapshot;
import person.kinman.cogame.core.action.GameAction;
import person.kinman.cogame.core.model.Direction;
import person.kinman.cogame.core.model.GameState;
import person.kinman.cogame.core.rule.GameEngine;

public class ReplayManagerTest {

    @Test
    public void testInitialStateSnapshot() {
        GameState state = new GameState(6, 6);
        ReplayManager rm = new ReplayManager();
        rm.reset(state);

        Assertions.assertEquals(1, rm.getSnapshotCount());
        Assertions.assertEquals(0, rm.getTotalSteps());
        Assertions.assertEquals(0, rm.getCurrentStep());

        TurnSnapshot initSnap = rm.getCurrentSnapshot();
        Assertions.assertNotNull(initSnap);
        Assertions.assertEquals(0, initSnap.getMoveIndex());
        Assertions.assertEquals(0, initSnap.getPlayerId());
        Assertions.assertFalse(initSnap.hasMoved());
        Assertions.assertFalse(initSnap.hasLockedEdge());
        Assertions.assertTrue(initSnap.getDescription().contains("开局初始盘面"));
    }

    @Test
    public void testMoveAndLockEdgeTracking() {
        GameState state = new GameState(6, 6);
        ReplayManager rm = new ReplayManager();
        rm.reset(state);

        // 第 1 手：P1 (0,0) 向下移动到 (1,0)，然后锁定 RIGHT 边
        GameEngine.executeAction(state, 1, GameAction.changeDirMove(Direction.DOWN));
        GameEngine.executeAction(state, 1, GameAction.lock(Direction.RIGHT));

        // 触发状态变更通知
        rm.onStateChanged(state);

        Assertions.assertEquals(2, rm.getSnapshotCount());
        Assertions.assertEquals(1, rm.getTotalSteps());

        TurnSnapshot move1 = rm.getAllSnapshots().get(1);
        Assertions.assertEquals(1, move1.getMoveIndex());
        Assertions.assertEquals(1, move1.getPlayerId());
        Assertions.assertEquals(0, move1.getFromR());
        Assertions.assertEquals(0, move1.getFromC());
        Assertions.assertEquals(1, move1.getToR());
        Assertions.assertEquals(0, move1.getToC());
        Assertions.assertEquals(1, move1.getStepsUsed());
        Assertions.assertEquals(Direction.RIGHT, move1.getLockedDirection());
        Assertions.assertTrue(move1.hasMoved());
        Assertions.assertTrue(move1.hasLockedEdge());

        // 第 2 手：P2 从 (5,5) 向上移动到 (4,5)，然后锁定 LEFT 边
        GameEngine.executeAction(state, 2, GameAction.changeDirMove(Direction.UP));
        GameEngine.executeAction(state, 2, GameAction.lock(Direction.LEFT));
        rm.onStateChanged(state);

        Assertions.assertEquals(3, rm.getSnapshotCount());
        Assertions.assertEquals(2, rm.getTotalSteps());

        TurnSnapshot move2 = rm.getAllSnapshots().get(2);
        Assertions.assertEquals(2, move2.getMoveIndex());
        Assertions.assertEquals(2, move2.getPlayerId());
        Assertions.assertEquals(5, move2.getFromR());
        Assertions.assertEquals(5, move2.getFromC());
        Assertions.assertEquals(4, move2.getToR());
        Assertions.assertEquals(5, move2.getToC());
        Assertions.assertEquals(Direction.LEFT, move2.getLockedDirection());
        Assertions.assertTrue(move2.hasMoved());
        Assertions.assertTrue(move2.hasLockedEdge());
    }

    @Test
    public void testReplayNavigationAndClamping() {
        GameState state = new GameState(6, 6);
        ReplayManager rm = new ReplayManager();
        rm.reset(state);

        // 模拟进行 3 手
        GameEngine.executeAction(state, 1, GameAction.changeDirMove(Direction.DOWN));
        GameEngine.executeAction(state, 1, GameAction.lock(Direction.RIGHT));
        rm.onStateChanged(state);

        GameEngine.executeAction(state, 2, GameAction.changeDirMove(Direction.UP));
        GameEngine.executeAction(state, 2, GameAction.lock(Direction.LEFT));
        rm.onStateChanged(state);

        GameEngine.executeAction(state, 1, GameAction.changeDirMove(Direction.RIGHT));
        GameEngine.executeAction(state, 1, GameAction.lock(Direction.DOWN));
        rm.onStateChanged(state);

        Assertions.assertEquals(4, rm.getSnapshotCount()); // 0, 1, 2, 3

        // 进入复盘模式
        rm.setReplayMode(true);
        Assertions.assertTrue(rm.isReplayMode());
        Assertions.assertEquals(3, rm.getCurrentStep(), "进入复盘模式应默认定位至最后一手");

        // 首手与末手导航
        rm.first();
        Assertions.assertEquals(0, rm.getCurrentStep());
        Assertions.assertEquals(0, rm.getCurrentSnapshot().getMoveIndex());

        rm.next();
        Assertions.assertEquals(1, rm.getCurrentStep());
        Assertions.assertEquals(1, rm.getCurrentSnapshot().getMoveIndex());

        rm.prev();
        Assertions.assertEquals(0, rm.getCurrentStep());
        rm.prev(); // 越界下限测试
        Assertions.assertEquals(0, rm.getCurrentStep());

        rm.last();
        Assertions.assertEquals(3, rm.getCurrentStep());
        rm.next(); // 越界上限测试
        Assertions.assertEquals(3, rm.getCurrentStep());

        // 指定跳转
        rm.jumpTo(2);
        Assertions.assertEquals(2, rm.getCurrentStep());
        Assertions.assertEquals(2, rm.getCurrentSnapshot().getMoveIndex());
    }

    @Test
    public void testResetRestartsSnapshotHistory() {
        GameState state = new GameState(6, 6);
        ReplayManager rm = new ReplayManager();
        rm.reset(state);

        GameEngine.executeAction(state, 1, GameAction.changeDirMove(Direction.DOWN));
        GameEngine.executeAction(state, 1, GameAction.lock(Direction.RIGHT));
        rm.onStateChanged(state);
        Assertions.assertEquals(2, rm.getSnapshotCount());

        // 模拟开新局重置
        state.reset();
        rm.onStateChanged(state);

        Assertions.assertEquals(1, rm.getSnapshotCount());
        Assertions.assertEquals(0, rm.getCurrentStep());
        Assertions.assertFalse(rm.isReplayMode());
    }
}

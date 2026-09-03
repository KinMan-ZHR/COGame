package person.kinman.cogame.client;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import person.kinman.cogame.client.controller.OnlineController;
import person.kinman.cogame.core.action.GameAction;
import person.kinman.cogame.core.model.Direction;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class OnlineMatchIntegrationTest {

    @Test
    public void testTwoPlayersConnectAndPlay() throws Exception {
        String serverUrl = "ws://127.0.0.1:8088";
        String roomId = "test-integration-" + System.currentTimeMillis();

        CountDownLatch startLatch = new CountDownLatch(2);
        CountDownLatch moveLatch = new CountDownLatch(1);

        // Player 1 (KinMan)
        OnlineController p1 = new OnlineController(serverUrl, roomId, "KinMan");
        p1.setOnNotification(msg -> {
            if (msg.contains("游戏开始")) {
                startLatch.countDown();
            }
        });

        // Player 2 (Challenger)
        OnlineController p2 = new OnlineController(serverUrl, roomId, "Challenger");
        p2.setOnNotification(msg -> {
            if (msg.contains("游戏开始")) {
                startLatch.countDown();
            }
        });

        // 等待双方连接并匹配成功
        long deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < deadline) {
            if (p1.isGameStarted() && p2.isGameStarted()) {
                break;
            }
            Thread.sleep(50);
        }
        Assertions.assertTrue(p1.isGameStarted() && p2.isGameStarted(), "双方未能在5秒内成功匹配开局");

        // 验证一个为1，一个为2
        Assertions.assertTrue((p1.getMyPlayerId() == 1 && p2.getMyPlayerId() == 2)
                           || (p1.getMyPlayerId() == 2 && p2.getMyPlayerId() == 1),
                "双方应分别被分配为先手(1)和后手(2)");

        OnlineController firstPlayer = (p1.getMyPlayerId() == 1) ? p1 : p2;
        OnlineController secondPlayer = (p1.getMyPlayerId() == 1) ? p2 : p1;

        // 后手玩家监听状态同步
        secondPlayer.setOnStateChanged(state -> {
            if (state.getP1().getR() == 1 && state.getP1().getC() == 0) {
                moveLatch.countDown();
            }
        });

        // 先手执行向下移动
        firstPlayer.handleUserAction(GameAction.changeDirMove(Direction.DOWN));

        boolean synced = moveLatch.await(5, TimeUnit.SECONDS);
        Assertions.assertTrue(synced, "先手行动未能实时同步给后手");

        p1.close();
        p2.close();
    }
}
